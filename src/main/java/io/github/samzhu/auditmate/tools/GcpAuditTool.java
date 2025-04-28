package io.github.samzhu.auditmate.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.serviceusage.v1.ServiceUsage;
import com.google.api.services.serviceusage.v1.model.ListServicesResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.compute.v1.Firewall;
import com.google.cloud.compute.v1.FirewallsClient;
import com.google.cloud.compute.v1.ListFirewallsRequest;
import com.google.cloud.kms.v1.CryptoKey;
import com.google.cloud.kms.v1.CryptoKeyVersion;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyRing;
import com.google.cloud.kms.v1.ListCryptoKeysRequest;
import com.google.cloud.kms.v1.ListKeyRingsRequest;
import com.google.cloud.location.ListLocationsRequest;
import com.google.cloud.location.Location;
import com.google.cloud.resourcemanager.v3.ProjectName;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.iam.v1.Policy;

import io.github.samzhu.auditmate.dto.GcpAuditResult;
import lombok.extern.slf4j.Slf4j;

/**
 * GCP Audit Tool class
 * Provides self-audit functionality for GCP projects and outputs results to an Excel file.
 */
@Slf4j
@Component
public class GcpAuditTool {

    private static final String KMS_SERVICE_NAME = "cloudkms.googleapis.com";
    private static final List<String> REQUIRED_SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/cloud-platform");
    private static final String AUTH_REQUIRED_STATUS = "AUTH_REQUIRED";
    private static final String FAILED_STATUS = "FAILED";
    private static final String SUCCESS_STATUS = "SUCCESS";

    /**
     * Execute GCP audit
     *
     * @param projectId GCP project ID
     * @param year      Year
     * @param quarter   Quarter (H1 or H2)
     * @return Audit result message
     */
    @Tool(name = "performAudit", description = "Perform GCP self-audit and generate a report, checking IAM permissions, BYOK, and firewall rules.")
    public String performAudit(
            @ToolParam(required = true, description = "GCP project ID to audit") String projectId,
            @ToolParam(required = true, description = "Audit year, e.g. 2025") String year,
            @ToolParam(required = true, description = "Audit quarter, H1 for first half, H2 for second half") String quarter) {

        GcpAuditResult result = initializeAuditResult(projectId, year, quarter);
        String responseMessage = "";

        try {
            GoogleCredentials credentials = getCredentials();
            executeAudit(result, projectId, year, quarter, credentials);
        } catch (IOException e) {
            handleIoException(result, e);
        } catch (Exception e) {
            handleGenericException(result, e);
        } finally {
            // 確保在 finally 區塊中回傳結果
            responseMessage = formatAuditResultMessage(result);
        }

        return responseMessage;
    }

    /**
     * Initialize audit result object
     */
    private GcpAuditResult initializeAuditResult(String projectId, String year, String quarter) {
        GcpAuditResult result = new GcpAuditResult();
        result.setProjectId(projectId);
        result.setYear(year);
        result.setQuarter(quarter);
        result.setAuditTime(LocalDateTime.now());
        return result;
    }

    /**
     * Execute audit and generate report
     */
    private void executeAudit(GcpAuditResult result, String projectId, String year, String quarter, GoogleCredentials credentials) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(baos, "GcpAuditTool", "1.0")) {
            // Perform IAM audit
            showIAM(workbook, projectId);

            // Perform BYOK audit
            showBYOK(workbook, projectId);

            // Perform firewall rules audit
            inventoryFirewallRules(workbook, projectId);

            // Complete workbook
            workbook.finish();
            
            // Generate report file name and save report
            String fileName = String.format("CloudSelfAudit_%s%s_GCP_%s.xlsx", year, quarter, projectId);
            String filePath = saveWorkbookToFile(baos, fileName);
            
            result.setReportFilePath(filePath);
            result.setStatus(SUCCESS_STATUS);
        }
    }

    /**
     * Save Excel workbook to file system
     */
    private String saveWorkbookToFile(ByteArrayOutputStream baos, String fileName) throws IOException {
        byte[] data = baos.toByteArray();
        
        // Try writing to home directory
        try {
            File homeDir = new File(System.getProperty("user.home"));
            validateDirectory(homeDir);
            
            File outputFile = new File(homeDir, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                fileOut.write(data);
                return outputFile.getAbsolutePath();
            }
        } catch (IOException homeException) {
            // If writing to home directory fails, try writing to temp directory
            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                ensureDirectoryExists(tempDir);
                validateDirectory(tempDir);
                
                File outputFile = new File(tempDir, fileName);
                try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                    fileOut.write(data);
                    return outputFile.getAbsolutePath();
                }
            } catch (IOException tempException) {
                // If writing to file system fails, return Base64 encoded string
                return createBase64EncodedFile(data);
            }
        }
    }

    /**
     * Validate if directory exists and is writable
     */
    private void validateDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Directory does not exist: " + directory.getAbsolutePath());
        }
        
        if (!directory.canWrite()) {
            throw new IOException("Directory does not have write permission: " + directory.getAbsolutePath());
        }
    }

    /**
     * Ensure directory exists, create if not
     */
    private void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Create Base64 encoded file content
     */
    private String createBase64EncodedFile(byte[] data) {
        String base64Content = Base64.getEncoder().encodeToString(data);
        return "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," + base64Content;
    }

    /**
     * Handle IO exception
     */
    private void handleIoException(GcpAuditResult result, IOException e) {
        if (e instanceof java.io.FileNotFoundException) {
            result.setStatus(FAILED_STATUS);
            result.setErrorMessage("Unable to write file: " + e.getMessage() + 
                    "\nPlease ensure the application has permission to write to the file, or try running the program again.");
        } else {
            result.setStatus(AUTH_REQUIRED_STATUS);
            result.setErrorMessage("Please use the following command to get GCP authorization:\n\n" +
                    "gcloud auth application-default login\n\n" +
                    "Or use headless mode:\n\n" +
                    "gcloud auth application-default login --no-browser\n\n" +
                    "After authorization is completed, please re-run the audit.");
        }
        result.setStackTrace(getStackTraceAsString(e));
    }

    /**
     * Handle generic exception
     */
    private void handleGenericException(GcpAuditResult result, Exception e) {
        result.setStatus(FAILED_STATUS);
        result.setErrorMessage(e.getMessage());
        result.setStackTrace(getStackTraceAsString(e));
    }

    /**
     * Format audit result message
     */
    private String formatAuditResultMessage(GcpAuditResult result) {
        if (SUCCESS_STATUS.equals(result.getStatus())) {
            return String.format("""
                    ✅ GCP audit succeeded!

                    • Project ID: %s
                    • Year / Quarter: %s / %s
                    • Audit time: %s
                    • Report location: %s

                    If you need to open the report, please manually open the above Excel file
                    """, result.getProjectId(), result.getYear(), result.getQuarter(),
                    result.getAuditTime(), result.getReportFilePath());
        } else {
            return String.format("""
                    ❌ GCP audit failed!

                    • Project ID: %s
                    • Year / Quarter: %s / %s
                    • Audit time: %s
                    • Error message: %s
                    • Stack trace: %s
                    """, result.getProjectId(), result.getYear(), result.getQuarter(),
                    result.getAuditTime(), result.getErrorMessage(), result.getStackTrace());
        }
    }

    /**
     * Get Google application default credentials
     */
    private GoogleCredentials getCredentials() throws IOException {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

            if (credentials.createScopedRequired()) {
                credentials = credentials.createScoped(REQUIRED_SCOPES);
            }

            return credentials;
        } catch (IOException e) {
            throw new IOException("GCP authorization is required. Please run the 'gcloud auth application-default login' command to get credentials.", e);
        }
    }

    /**
     * Check if KMS API is enabled
     */
    private boolean isKmsApiEnabled(String projectId) throws IOException {
        try {
            ServiceUsage serviceUsage = new ServiceUsage.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault()))
                    .setApplicationName("service-usage-check")
                    .build();

            ListServicesResponse response = serviceUsage.services()
                    .list("projects/" + projectId)
                    .setFilter("state:ENABLED")
                    .execute();

            return response.getServices().stream()
                    .anyMatch(service -> service.getName().contains(KMS_SERVICE_NAME));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Query and record firewall rules
     */
    private void inventoryFirewallRules(Workbook workbook, String projectId) throws Exception {
        Worksheet sheet = workbook.newWorksheet("Network Rules");
        setupFirewallSheetHeader(sheet);

        try (FirewallsClient firewallsClient = FirewallsClient.create()) {
            ListFirewallsRequest listFirewallsRequest = ListFirewallsRequest.newBuilder()
                    .setProject(projectId)
                    .build();

            int rowNum = 1;
            boolean hasRules = false;

            for (Firewall firewall : firewallsClient.list(listFirewallsRequest).iterateAll()) {
                hasRules = true;
                populateFirewallRow(sheet, rowNum++, firewall);
            }

            if (!hasRules) {
                createEmptyFirewallRow(sheet);
            }
        } catch (Exception e) {
            createEmptyFirewallRow(sheet);
            throw new Exception("Unable to retrieve firewall rule information: " + e.getMessage(), e);
        }
    }

    /**
     * Set firewall sheet header
     */
    private void setupFirewallSheetHeader(Worksheet sheet) {
        sheet.value(0, 0, "Direction");
        sheet.value(0, 1, "Source Ranges");
        sheet.value(0, 2, "Destination Ranges");
        sheet.value(0, 3, "Name");
        sheet.value(0, 4, "Purpose");

        sheet.width(0, 15);
        sheet.width(1, 30);
        sheet.width(2, 30);
        sheet.width(3, 30);
        sheet.width(4, 40);
    }

    /**
     * Populate firewall rule row
     */
    private void populateFirewallRow(Worksheet sheet, int rowNum, Firewall firewall) {
        sheet.value(rowNum, 0, firewall.getDirection().toString());
        sheet.value(rowNum, 1, String.join(", ", firewall.getSourceRangesList()));
        sheet.value(rowNum, 2, String.join(", ", firewall.getDestinationRangesList()));
        sheet.value(rowNum, 3, firewall.getName());
        sheet.value(rowNum, 4, firewall.getDescription());
    }

    /**
     * Create empty firewall rule row
     */
    private void createEmptyFirewallRow(Worksheet sheet) {
        sheet.value(1, 0, "None");
        sheet.value(1, 1, "None");
        sheet.value(1, 2, "None");
        sheet.value(1, 3, "None");
        sheet.value(1, 4, "None");
    }

    /**
     * Query and record self-carry keys (BYOK)
     */
    private void showBYOK(Workbook workbook, String projectId) throws IOException {
        Worksheet sheet = workbook.newWorksheet("BYOK");
        setupBYOKSheetHeader(sheet);

        if (!isKmsApiEnabled(projectId)) {
            createEmptyBYOKRow(sheet);
            return;
        }

        try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
            int rowNum = 1;
            boolean hasImportedKeys = false;

            ListLocationsRequest listLocationsRequest = ListLocationsRequest.newBuilder()
                    .setName(String.format("projects/%s", projectId))
                    .build();

            for (Location location : client.listLocations(listLocationsRequest).iterateAll()) {
                String locationId = location.getLocationId();
                hasImportedKeys = processKeysInLocation(client, projectId, locationId, sheet, rowNum, hasImportedKeys);
                if (hasImportedKeys) {
                    rowNum++;
                }
            }

            if (!hasImportedKeys) {
                createEmptyBYOKRow(sheet);
            }
        } catch (com.google.api.gax.rpc.PermissionDeniedException e) {
            sheet.value(1, 0, "Insufficient permissions to access Cloud KMS service");
            sheet.value(1, 1, "None");
            sheet.value(1, 2, "None");
            sheet.value(1, 3, "None");
            throw new IOException("Unable to access Cloud KMS service: " + e.getMessage(), e);
        }
    }

    /**
     * Set BYOK sheet header
     */
    private void setupBYOKSheetHeader(Worksheet sheet) {
        sheet.value(0, 0, "Key Name");
        sheet.value(0, 1, "Type (ex. RSA-2048)");
        sheet.value(0, 2, "Lifecycle");
        sheet.value(0, 3, "Manager");

        sheet.width(0, 50);
        sheet.width(1, 20);
        sheet.width(2, 15);
        sheet.width(3, 15);
    }

    /**
     * Process keys in a specific location
     */
    private boolean processKeysInLocation(KeyManagementServiceClient client, String projectId, 
            String locationId, Worksheet sheet, int rowNum, boolean hasImportedKeys) {
        
        ListKeyRingsRequest listKeyRingsRequest = ListKeyRingsRequest.newBuilder()
                .setParent(String.format("projects/%s/locations/%s", projectId, locationId))
                .build();

        for (KeyRing keyRing : client.listKeyRings(listKeyRingsRequest).iterateAll()) {
            hasImportedKeys = processKeysInKeyRing(client, keyRing, sheet, rowNum, hasImportedKeys);
        }
        
        return hasImportedKeys;
    }

    /**
     * Process keys in a specific key ring
     */
    private boolean processKeysInKeyRing(KeyManagementServiceClient client, 
            KeyRing keyRing, Worksheet sheet, int rowNum, boolean hasImportedKeys) {
        
        ListCryptoKeysRequest listCryptoKeysRequest = ListCryptoKeysRequest.newBuilder()
                .setParent(keyRing.getName())
                .build();

        for (CryptoKey cryptoKey : client.listCryptoKeys(listCryptoKeysRequest).iterateAll()) {
            if (cryptoKey.getImportOnly()) {
                hasImportedKeys = true;
                sheet.value(rowNum, 0, cryptoKey.getName());
                sheet.value(rowNum, 1, cryptoKey.getPurpose().toString());
                sheet.value(rowNum, 2, getKeyLifecycle(client, cryptoKey));
                sheet.value(rowNum, 3, getKeyManager(cryptoKey));
            }
        }
        
        return hasImportedKeys;
    }

    /**
     * Create empty BYOK row
     */
    private void createEmptyBYOKRow(Worksheet sheet) {
        sheet.value(1, 0, "None");
        sheet.value(1, 1, "None");
        sheet.value(1, 2, "None");
        sheet.value(1, 3, "None");
    }

    /**
     * Query and record IAM permissions
     */
    private void showIAM(Workbook workbook, String projectId) throws IOException {
        Worksheet sheet = workbook.newWorksheet("IAM");
        setupIAMSheetHeader(sheet);

        try (ProjectsClient projectsClient = ProjectsClient.create()) {
            ProjectName projectName = ProjectName.of(projectId);
            Policy policy = projectsClient.getIamPolicy(projectName);

            Map<String, IAM> iamMap = generateIAMMap(policy);
            populateIAMSheet(sheet, new ArrayList<>(iamMap.values()));
        } catch (Exception e) {
            sheet.value(1, 0, "Unable to retrieve IAM permission information");
            sheet.value(1, 1, "None");
            throw new IOException("Unable to retrieve IAM permission information: " + e.getMessage(), e);
        }
    }

    /**
     * Set IAM sheet header
     */
    private void setupIAMSheetHeader(Worksheet sheet) {
        sheet.value(0, 0, "User/Group");
        sheet.value(0, 1, "Permissions");

        sheet.width(0, 20);
        sheet.width(1, 50);
    }

    /**
     * Generate IAM mapping
     */
    private Map<String, IAM> generateIAMMap(Policy policy) {
        Map<String, IAM> iamMap = new HashMap<>();

        policy.getBindingsList().forEach(binding -> {
            String role = binding.getRole();
            binding.getMembersList().forEach(member -> {
                IAM iam = iamMap.computeIfAbsent(member, IAM::new);
                iam.addRole(role);
            });
        });

        return iamMap;
    }

    /**
     * Populate IAM sheet
     */
    private void populateIAMSheet(Worksheet sheet, List<IAM> iamList) {
        int rowNum = 1;
        for (IAM iam : iamList) {
            if (iam.getName().startsWith("user") || iam.getName().startsWith("group")) {
                sheet.value(rowNum, 0, iam.getName());
                sheet.value(rowNum, 1, String.join("\n", iam.getRoles()));
                sheet.style(rowNum, 1).wrapText(true).set();
                rowNum++;
            }
        }
    }

    /**
     * IAM class, used to store IAM name and role list
     */
    private static class IAM {
        private final String name;
        private final List<String> roles;

        public IAM(String name) {
            this.name = name;
            this.roles = new ArrayList<>();
        }

        public void addRole(String role) {
            roles.add(role);
        }

        public String getName() {
            return name;
        }

        public List<String> getRoles() {
            return roles;
        }
    }

    /**
     * Get key lifecycle status
     */
    private String getKeyLifecycle(KeyManagementServiceClient client, CryptoKey cryptoKey) {
        String primaryVersionName = cryptoKey.getPrimary().getName();
        CryptoKeyVersion primaryVersion = client.getCryptoKeyVersion(primaryVersionName);
        return primaryVersion.getState().toString();
    }

    /**
     * Get key manager
     */
    private String getKeyManager(CryptoKey cryptoKey) {
        return cryptoKey.getLabelsMap().getOrDefault("manager", "Unknown");
    }

    /**
     * Convert exception stack trace to string
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
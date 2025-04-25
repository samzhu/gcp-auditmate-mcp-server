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
 * GCP 查核工具類
 * 提供 GCP 專案的自我查核功能，並輸出結果到 Excel 檔案
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
     * 執行 GCP 查核
     * 
     * @param projectId GCP 專案 ID
     * @param year      年份
     * @param quarter   季度（H1 或 H2）
     * @return 查核結果訊息
     */
    @Tool(name = "performAudit", description = "執行 GCP 自我查核並生成報告，檢查 IAM 權限、自攜金鑰(BYOK)和防火牆規則")
    public String performAudit(
            @ToolParam(required = true, description = "要查核的 GCP 專案 ID") String projectId,
            @ToolParam(required = true, description = "查核年份，例如 2025") String year,
            @ToolParam(required = true, description = "查核季度，H1 表示上半年，H2 表示下半年") String quarter) {

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
     * 初始化查核結果物件
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
     * 執行查核並生成報告
     */
    private void executeAudit(GcpAuditResult result, String projectId, String year, String quarter, GoogleCredentials credentials) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(baos, "GcpAuditTool", "1.0")) {
            // 進行 IAM 查核
            showIAM(workbook, projectId);

            // 進行 BYOK 查核
            showBYOK(workbook, projectId);

            // 進行防火牆規則查核
            inventoryFirewallRules(workbook, projectId);

            // 完成工作簿
            workbook.finish();
            
            // 生成報表檔案名稱和保存報表
            String fileName = String.format("雲端自行查核_%s%s_GCP_%s.xlsx", year, quarter, projectId);
            String filePath = saveWorkbookToFile(baos, fileName);
            
            result.setReportFilePath(filePath);
            result.setStatus(SUCCESS_STATUS);
        }
    }

    /**
     * 儲存 Excel 工作簿到檔案系統
     */
    private String saveWorkbookToFile(ByteArrayOutputStream baos, String fileName) throws IOException {
        byte[] data = baos.toByteArray();
        
        // 嘗試寫入到家目錄
        try {
            File homeDir = new File(System.getProperty("user.home"));
            validateDirectory(homeDir);
            
            File outputFile = new File(homeDir, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                fileOut.write(data);
                return outputFile.getAbsolutePath();
            }
        } catch (IOException homeException) {
            // 家目錄寫入失敗，嘗試寫入到臨時目錄
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
                // 如果檔案系統寫入都失敗，則返回 Base64 編碼字串
                return createBase64EncodedFile(data);
            }
        }
    }

    /**
     * 校驗目錄是否存在且可寫入
     */
    private void validateDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IOException("目錄不存在: " + directory.getAbsolutePath());
        }
        
        if (!directory.canWrite()) {
            throw new IOException("目錄無寫入權限: " + directory.getAbsolutePath());
        }
    }

    /**
     * 確保目錄存在，若不存在則創建
     */
    private void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * 創建 Base64 編碼的檔案內容
     */
    private String createBase64EncodedFile(byte[] data) {
        String base64Content = Base64.getEncoder().encodeToString(data);
        return "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," + base64Content;
    }

    /**
     * 處理 IO 異常
     */
    private void handleIoException(GcpAuditResult result, IOException e) {
        if (e instanceof java.io.FileNotFoundException) {
            result.setStatus(FAILED_STATUS);
            result.setErrorMessage("無法寫入檔案: " + e.getMessage() + 
                    "\n請確認應用程式有權限寫入文件，或是嘗試重新執行程式。");
        } else {
            result.setStatus(AUTH_REQUIRED_STATUS);
            result.setErrorMessage("請使用以下指令取得 GCP 授權:\n\n" +
                    "gcloud auth application-default login\n\n" +
                    "或使用無瀏覽器模式:\n\n" +
                    "gcloud auth application-default login --no-browser\n\n" +
                    "授權完成後，請重新執行查核。");
        }
        result.setStackTrace(getStackTraceAsString(e));
    }

    /**
     * 處理一般異常
     */
    private void handleGenericException(GcpAuditResult result, Exception e) {
        result.setStatus(FAILED_STATUS);
        result.setErrorMessage(e.getMessage());
        result.setStackTrace(getStackTraceAsString(e));
    }

    /**
     * 格式化查核結果訊息
     */
    private String formatAuditResultMessage(GcpAuditResult result) {
        if (SUCCESS_STATUS.equals(result.getStatus())) {
            return String.format("""
                    ✅ GCP 查核成功！

                    • 專案 ID：%s
                    • 年度 / 季度：%s / %s
                    • 查核時間：%s
                    • 報告位置：%s

                    若需開啟報告，請手動點開上方 Excel 檔案
                    """, result.getProjectId(), result.getYear(), result.getQuarter(),
                    result.getAuditTime(), result.getReportFilePath());
        } else {
            return String.format("""
                    ❌ GCP 查核失敗！

                    • 專案 ID：%s
                    • 年度 / 季度：%s / %s
                    • 查核時間：%s
                    • 錯誤訊息：%s
                    • 堆疊追蹤：%s
                    """, result.getProjectId(), result.getYear(), result.getQuarter(),
                    result.getAuditTime(), result.getErrorMessage(), result.getStackTrace());
        }
    }

    /**
     * 取得 Google 應用程式默認憑證
     */
    private GoogleCredentials getCredentials() throws IOException {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

            if (credentials.createScopedRequired()) {
                credentials = credentials.createScoped(REQUIRED_SCOPES);
            }

            return credentials;
        } catch (IOException e) {
            throw new IOException("需要 GCP 授權。請執行 'gcloud auth application-default login' 命令獲取憑證。", e);
        }
    }

    /**
     * 檢查 KMS API 是否已啟用
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
     * 查詢並記錄防火牆規則
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
            throw new Exception("無法取得防火牆規則資訊: " + e.getMessage(), e);
        }
    }

    /**
     * 設置防火牆表格標題
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
     * 填充防火牆規則行
     */
    private void populateFirewallRow(Worksheet sheet, int rowNum, Firewall firewall) {
        sheet.value(rowNum, 0, firewall.getDirection().toString());
        sheet.value(rowNum, 1, String.join(", ", firewall.getSourceRangesList()));
        sheet.value(rowNum, 2, String.join(", ", firewall.getDestinationRangesList()));
        sheet.value(rowNum, 3, firewall.getName());
        sheet.value(rowNum, 4, firewall.getDescription());
    }

    /**
     * 創建空的防火牆規則行
     */
    private void createEmptyFirewallRow(Worksheet sheet) {
        sheet.value(1, 0, "無");
        sheet.value(1, 1, "無");
        sheet.value(1, 2, "無");
        sheet.value(1, 3, "無");
        sheet.value(1, 4, "無");
    }

    /**
     * 查詢並記錄自攜金鑰(BYOK)
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
            sheet.value(1, 0, "權限不足，無法存取 Cloud KMS 服務");
            sheet.value(1, 1, "無");
            sheet.value(1, 2, "無");
            sheet.value(1, 3, "無");
            throw new IOException("無法存取 Cloud KMS 服務: " + e.getMessage(), e);
        }
    }

    /**
     * 設置 BYOK 表格標題
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
     * 處理特定位置的金鑰
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
     * 處理特定金鑰環中的金鑰
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
     * 創建空的 BYOK 行
     */
    private void createEmptyBYOKRow(Worksheet sheet) {
        sheet.value(1, 0, "無");
        sheet.value(1, 1, "無");
        sheet.value(1, 2, "無");
        sheet.value(1, 3, "無");
    }

    /**
     * 查詢並記錄 IAM 權限
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
            sheet.value(1, 0, "無法取得 IAM 權限資訊");
            sheet.value(1, 1, "無");
            throw new IOException("無法取得 IAM 權限資訊: " + e.getMessage(), e);
        }
    }

    /**
     * 設置 IAM 表格標題
     */
    private void setupIAMSheetHeader(Worksheet sheet) {
        sheet.value(0, 0, "User/Group");
        sheet.value(0, 1, "Permissions");

        sheet.width(0, 20);
        sheet.width(1, 50);
    }

    /**
     * 生成 IAM 映射
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
     * 填充 IAM 表格
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
     * IAM 類，用於存儲 IAM 名稱和角色列表
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
     * 獲取金鑰的生命週期狀態
     */
    private String getKeyLifecycle(KeyManagementServiceClient client, CryptoKey cryptoKey) {
        String primaryVersionName = cryptoKey.getPrimary().getName();
        CryptoKeyVersion primaryVersion = client.getCryptoKeyVersion(primaryVersionName);
        return primaryVersion.getState().toString();
    }

    /**
     * 獲取金鑰的管理人員
     */
    private String getKeyManager(CryptoKey cryptoKey) {
        return cryptoKey.getLabelsMap().getOrDefault("manager", "Unknown");
    }

    /**
     * 將異常的堆疊追蹤轉換為字符串
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
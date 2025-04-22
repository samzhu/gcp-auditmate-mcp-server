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

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    // 靜態初始化塊，確保 POI 相關類在使用前已初始化
    static {
        try {
            // 初始化 POI 相關類
            Class.forName("org.apache.poi.ss.formula.udf.IndexedUDFFinder");
            Class.forName("org.apache.poi.ss.formula.udf.UDFFinder");
            Class.forName("org.apache.poi.ss.formula.udf.DefaultUDFFinder");
            Class.forName("org.apache.poi.ss.formula.udf.AggregatingUDFFinder");
            Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook");
            // 加載 CTWorkbook 相關類
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookImpl");
            // 加載 CTWorkbookPr 相關類，解決類型轉換問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookPr");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookPrImpl");
            // 加載 CTBookViews 相關類，解決類型轉換問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookViews");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookView");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewImpl");
            // 加載 CTSheets 相關類，解決類型轉換問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheets");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetImpl");
            // 加載 SstDocument 相關類，解決共享字串表問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.SstDocument");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.SstDocumentImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSst");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSstImpl");
            // 加載 StyleSheetDocument 相關類，解決樣式表問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.StyleSheetDocument");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.StyleSheetDocumentImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTStylesheet");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTStylesheetImpl");
            // 加載 CTFont 相關類，解決字體問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFonts");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontsImpl");
            // 加載 CTFontSize 和其他字體屬性相關類
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontSize");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSizeImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontName");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontNameImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontFamily");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontFamilyImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColorImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontScheme");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSchemeImpl");
            // 加載 CTFill 相關類，解決填充樣式問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFills");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPatternFillImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STPatternTypeImpl");
            // 加載 CTBorder 相關類，解決邊框樣式問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorders");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBordersImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderPrImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STBorderStyleImpl");
            // 加載 CTXf 相關類，解決單元格格式樣式問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTXfImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellXfs");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellXfsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyleXfs");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellStyleXfsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDxf");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDxfImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmt");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTNumFmtImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmts");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTNumFmtsImpl");
            // 加載 CTRow 相關類，解決行結構問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRowImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetData");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetDataImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColImpl");
            // 加載 CTRst 相關類，解決富文本字符串問題
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRstImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRElt");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTREltImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRPrElt");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRPrEltImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontFamily");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STFontFamily");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STFontFamilyImpl");
            // 加載 STXstring 相關類，解決共享類型字符串問題
            Class.forName("org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STXstring");
            Class.forName("org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.impl.STXstringImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STCellTypeImpl");
            // 加載單元格類型枚舉相關類，解決枚舉轉換問題
            try {
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType$Enum");
                Class.forName("org.apache.xmlbeans.StringEnumAbstractBase");
                Class.forName("org.apache.xmlbeans.StringEnumAbstractBase$Table");
                Class.forName("org.apache.xmlbeans.impl.values.StringEnumValue");
                Class.forName("org.apache.xmlbeans.impl.values.JavaStringEnumerationHolderEx");
                // 加載其他常用枚舉類型
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle$Enum");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType$Enum");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STFontFamily$Enum");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STVerticalAlignment$Enum");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STHorizontalAlignment$Enum");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STVerticalAlignment");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STVerticalAlignmentImpl");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.STHorizontalAlignment");
                Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STHorizontalAlignmentImpl");
            } catch (NoClassDefFoundError e) {
                log.warn("無法找到枚舉相關類: {}", e.getMessage());
            }
            // 加載其他常用 Office XML 類，預防可能的錯誤
            // DrawingML 相關類（圖表、圖形等）
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextParagraphImpl");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextBodyImpl");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.impl.CTRegularTextRunImpl");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties");
            Class.forName("org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextCharacterPropertiesImpl");
            // 表格相關類
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTTableImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTTableColumnsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTTableColumnImpl");
            // 工作表相關類
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHeaderFooter");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTHeaderFooterImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetView");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetViewImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetViewsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetFormatPr");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetFormatPrImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetProtectionImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageMargins");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPageMarginsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageSetup");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPageSetupImpl");
            // 單元格相關類
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellAlignment");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellAlignmentImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCell");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTMergeCellImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCells");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTMergeCellsImpl");
            // 其他功能相關類
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHyperlink");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTHyperlinkImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHyperlinks");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTHyperlinksImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataValidation");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDataValidationImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataValidations");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDataValidationsImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDefinedName");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDefinedNameImpl");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDefinedNames");
            Class.forName("org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDefinedNamesImpl");
        } catch (ClassNotFoundException e) {
            log.warn("無法初始化 POI 相關類: {}", e.getMessage());
        }
    }

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
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 進行 IAM 查核
            showIAM(workbook, projectId);

            // 進行 BYOK 查核
            showBYOK(workbook, projectId);

            // 進行防火牆規則查核
            inventoryFirewallRules(workbook, projectId);

            // 生成報表檔案名稱和保存報表
            String fileName = String.format("雲端自行查核_%s%s_GCP_%s.xlsx", year, quarter, projectId);
            String filePath = saveWorkbookToFile(workbook, fileName);
            
            result.setReportFilePath(filePath);
            result.setStatus(SUCCESS_STATUS);
        }
    }

    /**
     * 儲存 Excel 工作簿到檔案系統
     */
    private String saveWorkbookToFile(XSSFWorkbook workbook, String fileName) throws IOException {
        // 嘗試寫入到家目錄
        try {
            File homeDir = new File(System.getProperty("user.home"));
            validateDirectory(homeDir);
            
            File outputFile = new File(homeDir, fileName);
            writeWorkbookToFile(workbook, outputFile);
            return outputFile.getAbsolutePath();
        } catch (IOException homeException) {
            // 家目錄寫入失敗，嘗試寫入到臨時目錄
            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                ensureDirectoryExists(tempDir);
                validateDirectory(tempDir);
                
                File outputFile = new File(tempDir, fileName);
                writeWorkbookToFile(workbook, outputFile);
                return outputFile.getAbsolutePath();
            } catch (IOException tempException) {
                // 如果檔案系統寫入都失敗，則返回 Base64 編碼字串
                return createBase64EncodedFile(workbook);
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
     * 將工作簿寫入檔案
     */
    private void writeWorkbookToFile(XSSFWorkbook workbook, File outputFile) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
    }

    /**
     * 創建 Base64 編碼的檔案內容
     */
    private String createBase64EncodedFile(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] bytes = bos.toByteArray();
        String base64Content = Base64.getEncoder().encodeToString(bytes);
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
    private void inventoryFirewallRules(XSSFWorkbook workbook, String projectId) throws Exception {
        XSSFSheet sheet = workbook.createSheet("Network Rules");
        setupFirewallSheetHeader(sheet);

        try (FirewallsClient firewallsClient = FirewallsClient.create()) {
            ListFirewallsRequest listFirewallsRequest = ListFirewallsRequest.newBuilder()
                    .setProject(projectId)
                    .build();

            int rowNum = 1;
            boolean hasRules = false;

            for (Firewall firewall : firewallsClient.list(listFirewallsRequest).iterateAll()) {
                hasRules = true;
                XSSFRow row = sheet.createRow(rowNum++);
                populateFirewallRow(row, firewall);
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
    private void setupFirewallSheetHeader(XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Direction");
        headerRow.createCell(1).setCellValue("Source Ranges");
        headerRow.createCell(2).setCellValue("Destination Ranges");
        headerRow.createCell(3).setCellValue("Name");
        headerRow.createCell(4).setCellValue("Purpose");

        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 30 * 256);
        sheet.setColumnWidth(3, 30 * 256);
        sheet.setColumnWidth(4, 40 * 256);
    }

    /**
     * 填充防火牆規則行
     */
    private void populateFirewallRow(XSSFRow row, Firewall firewall) {
        row.createCell(0).setCellValue(firewall.getDirection().toString());
        row.createCell(1).setCellValue(String.join(", ", firewall.getSourceRangesList()));
        row.createCell(2).setCellValue(String.join(", ", firewall.getDestinationRangesList()));
        row.createCell(3).setCellValue(firewall.getName());
        row.createCell(4).setCellValue(firewall.getDescription());
    }

    /**
     * 創建空的防火牆規則行
     */
    private void createEmptyFirewallRow(XSSFSheet sheet) {
        XSSFRow row = sheet.createRow(1);
        row.createCell(0).setCellValue("無");
        row.createCell(1).setCellValue("無");
        row.createCell(2).setCellValue("無");
        row.createCell(3).setCellValue("無");
        row.createCell(4).setCellValue("無");
    }

    /**
     * 查詢並記錄自攜金鑰(BYOK)
     */
    private void showBYOK(XSSFWorkbook workbook, String projectId) throws IOException {
        XSSFSheet sheet = workbook.createSheet("BYOK");
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
            XSSFRow row = sheet.createRow(1);
            row.createCell(0).setCellValue("權限不足，無法存取 Cloud KMS 服務");
            row.createCell(1).setCellValue("無");
            row.createCell(2).setCellValue("無");
            row.createCell(3).setCellValue("無");
            throw new IOException("無法存取 Cloud KMS 服務: " + e.getMessage(), e);
        }
    }

    /**
     * 設置 BYOK 表格標題
     */
    private void setupBYOKSheetHeader(XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Key Name");
        headerRow.createCell(1).setCellValue("Type (ex. RSA-2048)");
        headerRow.createCell(2).setCellValue("Lifecycle");
        headerRow.createCell(3).setCellValue("Manager");

        sheet.setColumnWidth(0, 50 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 15 * 256);
        sheet.setColumnWidth(3, 15 * 256);
    }

    /**
     * 處理特定位置的金鑰
     */
    private boolean processKeysInLocation(KeyManagementServiceClient client, String projectId, 
            String locationId, XSSFSheet sheet, int rowNum, boolean hasImportedKeys) {
        
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
            KeyRing keyRing, XSSFSheet sheet, int rowNum, boolean hasImportedKeys) {
        
        ListCryptoKeysRequest listCryptoKeysRequest = ListCryptoKeysRequest.newBuilder()
                .setParent(keyRing.getName())
                .build();

        for (CryptoKey cryptoKey : client.listCryptoKeys(listCryptoKeysRequest).iterateAll()) {
            if (cryptoKey.getImportOnly()) {
                hasImportedKeys = true;
                XSSFRow row = sheet.createRow(rowNum);
                row.createCell(0).setCellValue(cryptoKey.getName());
                row.createCell(1).setCellValue(cryptoKey.getPurpose().toString());
                row.createCell(2).setCellValue(getKeyLifecycle(client, cryptoKey));
                row.createCell(3).setCellValue(getKeyManager(cryptoKey));
            }
        }
        
        return hasImportedKeys;
    }

    /**
     * 創建空的 BYOK 行
     */
    private void createEmptyBYOKRow(XSSFSheet sheet) {
        XSSFRow row = sheet.createRow(1);
        row.createCell(0).setCellValue("無");
        row.createCell(1).setCellValue("無");
        row.createCell(2).setCellValue("無");
        row.createCell(3).setCellValue("無");
    }

    /**
     * 查詢並記錄 IAM 權限
     */
    private void showIAM(XSSFWorkbook workbook, String projectId) throws IOException {
        XSSFSheet sheet = workbook.createSheet("IAM");
        setupIAMSheetHeader(sheet);

        try (ProjectsClient projectsClient = ProjectsClient.create()) {
            ProjectName projectName = ProjectName.of(projectId);
            Policy policy = projectsClient.getIamPolicy(projectName);

            Map<String, IAM> iamMap = generateIAMMap(policy);
            populateIAMSheet(sheet, new ArrayList<>(iamMap.values()), workbook);
        } catch (Exception e) {
            XSSFRow row = sheet.createRow(1);
            row.createCell(0).setCellValue("無法取得 IAM 權限資訊");
            row.createCell(1).setCellValue("無");
            throw new IOException("無法取得 IAM 權限資訊: " + e.getMessage(), e);
        }
    }

    /**
     * 設置 IAM 表格標題
     */
    private void setupIAMSheetHeader(XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("User/Group");
        headerRow.createCell(1).setCellValue("Permissions");

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 50 * 256);
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
    private void populateIAMSheet(XSSFSheet sheet, List<IAM> iamList, XSSFWorkbook workbook) {
        int rowNum = 1;
        for (IAM iam : iamList) {
            if (iam.getName().startsWith("user") || iam.getName().startsWith("group")) {
                XSSFRow row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(iam.getName());
                row.createCell(1).setCellValue(String.join("\n", iam.getRoles()));
            }
        }

        XSSFCellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        for (int i = 1; i < rowNum; i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                XSSFCell cell = row.getCell(1);
                if (cell != null) {
                    cell.setCellStyle(wrapStyle);
                }
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
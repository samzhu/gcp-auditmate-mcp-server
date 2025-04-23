package io.github.samzhu.auditmate.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.extern.slf4j.Slf4j;

/**
 * POI 在 Native Image 環境中的輔助類
 * 避免直接使用可能導致問題的 POIXMLProperties 類
 */
@Slf4j
public class PoiNativeImageHelper {

    static {
        setupEnvironment();
    }

    /**
     * 設置 POI 環境屬性
     */
    private static void setupEnvironment() {
        log.debug("設置 POI Native Image 環境");
        
        // XMLBeans 相關配置
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", 
                "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        System.setProperty("POIXMLDocumentPart.read.contentTypes.validation", "false");
        System.setProperty("org.apache.poi.ooxml.strict", "false");
        System.setProperty("poi.keep.scoped.context", "true");
        
        // 禁用 XML 驗證和內存優化
        System.setProperty("javax.xml.stream.XMLInputFactory", 
                "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        System.setProperty("javax.xml.stream.XMLOutputFactory", 
                "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
        System.setProperty("javax.xml.stream.XMLEventFactory", 
                "com.sun.xml.internal.stream.events.XMLEventFactoryImpl");
    }

    /**
     * 安全創建工作簿，避免 POIXMLProperties 相關問題
     * 
     * @return 新的 XSSFWorkbook 實例
     */
    public static Workbook createWorkbook() {
        setupEnvironment();
        return new XSSFWorkbook();
    }

    /**
     * 將工作簿寫入輸出流
     * 
     * @param workbook 工作簿
     * @param output 輸出流
     * @throws IOException 如果寫入過程中發生錯誤
     */
    public static void writeWorkbook(Workbook workbook, OutputStream output) throws IOException {
        setupEnvironment();
        try {
            workbook.write(output);
        } catch (Exception e) {
            log.error("寫入工作簿時發生錯誤: {}", e.getMessage());
            throw new IOException("寫入工作簿時發生錯誤", e);
        }
    }

    /**
     * 將工作簿轉換為字節數組
     * 
     * @param workbook 工作簿
     * @return 字節數組
     * @throws IOException 如果轉換過程中發生錯誤
     */
    public static byte[] workbookToBytes(Workbook workbook) throws IOException {
        setupEnvironment();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("轉換工作簿為字節數組時發生錯誤: {}", e.getMessage());
            throw new IOException("轉換工作簿為字節數組時發生錯誤", e);
        }
    }

    /**
     * 創建並設置工作表
     * 
     * @param workbook 工作簿
     * @param sheetName 工作表名
     * @return 新創建的工作表
     */
    public static Sheet createSheet(Workbook workbook, String sheetName) {
        setupEnvironment();
        return workbook.createSheet(sheetName);
    }

    /**
     * 安全設置單元格值，處理各種數據類型
     * 
     * @param cell 要設置的單元格
     * @param value 要設置的值（支持 String, Number, Boolean, Date 等類型）
     */
    public static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 創建包含標題行的新表
     * 
     * @param workbook 工作簿
     * @param sheetName 表名
     * @param headers 標題數組
     * @return 創建的表
     */
    public static Sheet createSheetWithHeaders(Workbook workbook, String sheetName, String[] headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        return sheet;
    }

    /**
     * 關閉工作簿並處理異常
     * 
     * @param workbook 要關閉的工作簿
     */
    public static void closeWorkbook(Workbook workbook) {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                log.warn("關閉工作簿時發生警告: {}", e.getMessage());
            }
        }
    }
} 
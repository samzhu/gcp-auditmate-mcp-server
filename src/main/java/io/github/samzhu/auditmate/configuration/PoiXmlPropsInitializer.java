package io.github.samzhu.auditmate.configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * 專門用於初始化 POIXMLProperties 相關類的配置
 * 解決在 GraalVM Native Image 下 "Could not initialize class org.apache.poi.ooxml.POIXMLProperties" 錯誤
 */
@Slf4j
@Configuration
public class PoiXmlPropsInitializer {
    
    @PostConstruct
    public void initializePoiXmlProps() {
        log.info("開始初始化 POIXMLProperties 相關類");
        
        try {
            // 創建一個臨時 XLSX 文件並寫入一些內容，觸發 POIXMLProperties 類的初始化
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                workbook.createSheet("Sheet1");
                
                // 嘗試訪問 POIXMLProperties 對象
                POIXMLProperties props = workbook.getProperties();
                if (props != null) {
                    log.info("成功初始化 POIXMLProperties 對象: {}", props.getClass().getName());
                    
                    // 嘗試訪問核心屬性
                    POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();
                    if (coreProps != null) {
                        log.info("成功初始化 CoreProperties 對象: {}", coreProps.getClass().getName());
                    }
                    
                    // 嘗試訪問擴展屬性
                    POIXMLProperties.ExtendedProperties extProps = props.getExtendedProperties();
                    if (extProps != null) {
                        log.info("成功初始化 ExtendedProperties 對象: {}", extProps.getClass().getName());
                    }
                    
                    // 嘗試訪問自定義屬性
                    POIXMLProperties.CustomProperties customProps = props.getCustomProperties();
                    if (customProps != null) {
                        log.info("成功初始化 CustomProperties 對象: {}", customProps.getClass().getName());
                    }
                }
                
                // 將工作簿寫入字節數組，觸發更多屬性相關類的初始化
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                bos.close();
                
                log.info("POIXMLProperties 相關類初始化完成");
            }
        } catch (IOException | RuntimeException e) {
            log.error("初始化 POIXMLProperties 時發生錯誤", e);
        }
        
        // 嘗試顯式加載 POIXMLProperties 類及其內部類
        try {
            Class.forName("org.apache.poi.ooxml.POIXMLProperties");
            Class.forName("org.apache.poi.ooxml.POIXMLProperties$CoreProperties");
            Class.forName("org.apache.poi.ooxml.POIXMLProperties$ExtendedProperties");
            Class.forName("org.apache.poi.ooxml.POIXMLProperties$CustomProperties");
            log.info("成功顯式加載 POIXMLProperties 類及其內部類");
        } catch (ClassNotFoundException e) {
            log.error("加載 POIXMLProperties 類失敗", e);
        }
    }
} 
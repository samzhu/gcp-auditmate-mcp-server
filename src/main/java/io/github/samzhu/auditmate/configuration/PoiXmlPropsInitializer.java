package io.github.samzhu.auditmate.configuration;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Native Image 環境中 POI 相關配置
 * 避免直接使用 POIXMLProperties 類
 */
@Slf4j
@Configuration
public class PoiXmlPropsInitializer {
    
    static {
        // 設置關鍵系統屬性
        disableXmlBeansValidation();
    }
    
    @PostConstruct
    public void initializePoiXmlProps() {
        log.info("設置 POI Native Image 相容性配置");
        
        // 再次確保設置所有屬性
        disableXmlBeansValidation();
    }
    
    /**
     * 設置關鍵系統屬性，禁用 XMLBeans 驗證機制
     */
    private static void disableXmlBeansValidation() {
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
} 
package io.github.samzhu.auditmate.configuration;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * 專門用於初始化 POIXMLProperties 相關類的配置
 * 解決在 GraalVM Native Image 下 "Could not initialize class org.apache.poi.ooxml.POIXMLProperties" 錯誤
 */
@Slf4j
@Configuration
public class PoiXmlPropsInitializer {
    
    static {
        // 設置系統屬性，禁用某些 XMLBeans 功能以避免 Native Image 問題
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        System.setProperty("POIXMLDocumentPart.read.contentTypes.validation", "false");
        System.setProperty("org.apache.poi.ooxml.strict", "false");
    }
    
    @PostConstruct
    public void initializePoiXmlProps() {
        log.info("初始化 POI 相關類配置");
        
        try {
            // 顯式加載關鍵類但不嘗試初始化
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.poi.ooxml.POIXMLProperties");
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.poi.ooxml.POIXMLProperties$CoreProperties");
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.poi.ooxml.POIXMLProperties$ExtendedProperties");
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.poi.ooxml.POIXMLProperties$CustomProperties");
            
            // 加載 XMLBeans 關鍵類
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.xmlbeans.XmlObject");
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.xmlbeans.impl.values.XmlComplexContentImpl");
            
            // 加載 Office OpenXML 文檔屬性相關類
            Thread.currentThread().getContextClassLoader().loadClass("org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperties");
            Thread.currentThread().getContextClassLoader().loadClass("org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.CTProperties");
            
            log.info("POI 相關類加載完成");
        } catch (ClassNotFoundException e) {
            log.error("加載 POI 相關類失敗", e);
        }
    }
} 
package io.github.samzhu.auditmate.configuration;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Apache POI 在 GraalVM Native Image 環境下的配置類
 * 設置一些必要的系統屬性，避免使用不兼容的功能
 */
@Slf4j
@Configuration
public class PoiNativeImageConfig {
    
    @PostConstruct
    public void init() {
        log.info("初始化 POI Native Image 配置");
        
        // 禁用 POI 的 Sheet 自動調整列寬功能，避免使用 AWT 字體渲染
        System.setProperty("org.apache.poi.ss.disableAutoColumnWidth", "true");
        
        // 禁用顏色格式化，避免使用 AWT 顏色對象
        System.setProperty("org.apache.poi.ss.format.disable.color", "true");
        
        // 禁用 Java2D Disposer 線程（這是導致 GraalVM native-image 編譯錯誤的原因之一）
        System.setProperty("sun.java2d.disable", "true");
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.d3d", "false");
        
        // 對於 Mac 系統，禁用 AWT 字體查找和 Java2D 相關功能
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.setProperty("apple.awt.headless", "true");
            System.setProperty("apple.awt.UIElement", "true");
        }
        
        // 禁用 AWT
        System.setProperty("java.awt.headless", "true");
    }
} 
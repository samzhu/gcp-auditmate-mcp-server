package io.github.samzhu.auditmate.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import lombok.extern.slf4j.Slf4j;

/**
 * Apache POI 相關類的 RuntimeHints 配置
 * 解決在 GraalVM Native Image 執行時 IndexedUDFFinder 初始化問題
 */
@Slf4j
@ImportRuntimeHints(PoiRuntimeHintsConfig.PoiResourcesRegistrar.class)
@Configuration
public class PoiRuntimeHintsConfig {

    /**
     * 註冊 Apache POI 相關的類和資源
     */
    static class PoiResourcesRegistrar implements RuntimeHintsRegistrar {

        // POI 核心類 - 這些類應該都存在
        List<String> poiClassNames = Arrays.asList(
            "org.apache.poi.ss.formula.udf.IndexedUDFFinder",
            "org.apache.poi.ss.formula.udf.UDFFinder",
            "org.apache.poi.ss.formula.udf.DefaultUDFFinder",
            "org.apache.poi.ss.formula.udf.AggregatingUDFFinder",
            "org.apache.poi.ss.formula.functions.FreeRefFunction",
            "org.apache.poi.xssf.usermodel.XSSFWorkbook",
            "org.apache.poi.xssf.usermodel.XSSFSheet",
            "org.apache.poi.xssf.usermodel.XSSFRow",
            "org.apache.poi.xssf.usermodel.XSSFCell",
            "org.apache.poi.xssf.usermodel.XSSFCellStyle",
            "org.apache.poi.ooxml.POIXMLProperties",
            "org.apache.poi.ooxml.POIXMLDocument"
        );
        
        // OpenXML 關鍵類
        private static final List<String> OPENXML_CLASSES = Arrays.asList(
            // 明確知道存在的類
            "org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.PropertiesDocumentImpl",
            "org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.PropertiesDocumentImpl",
            "org.openxmlformats.schemas.officeDocument.x2006.customProperties.PropertiesDocument",
            "org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.PropertiesDocument",
            // 添加關鍵 CTWorkbook 相關類
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.WorkbookDocument",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.WorkbookDocumentImpl",
            // 添加 CTWorkbookPr 相關類
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookPr",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookPrImpl",
            // 添加 CTBookViews 相關類
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookViews",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewsImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookView",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewImpl",
            // 添加 CTSheets 相關類 - 解決類型轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheets",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetsImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetImpl",
            // 添加 Sst Document 相關類 - 解決共享字串表轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.SstDocument",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.SstDocumentImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSst",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSstImpl",
            // 添加 StyleSheetDocument 相關類 - 解決樣式表轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.StyleSheetDocument",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.StyleSheetDocumentImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTStylesheet",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTStylesheetImpl",
            // 添加 CTFont 相關類 - 解決字體轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFonts",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontsImpl",
            // 添加 CTFontSize 和其他字體屬性相關類
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontSize",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSizeImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontName",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontNameImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontFamily",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontFamilyImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColorImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontScheme",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSchemeImpl",
            // 添加 CTFill 相關類 - 解決填充樣式轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFills",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillsImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPatternFillImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STPatternTypeImpl",
            // 添加 CTBorder 相關類 - 解決邊框樣式轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorders",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBordersImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderPrImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STBorderStyleImpl",
            // 添加 CTXf 相關類 - 解決單元格格式樣式轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTXfImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellXfs",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellXfsImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyleXfs",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellStyleXfsImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDxf",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDxfImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmt",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTNumFmtImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmts",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTNumFmtsImpl",
            // 添加 CTRow 相關類 - 解決行結構轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRowImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetData",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetDataImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColsImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColImpl",
            // 添加 CTRst 相關類 - 解決富文本字符串轉換問題
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRstImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRElt",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTREltImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRPrElt",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRPrEltImpl",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontFamily",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.STFontFamily",
            "org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STFontFamilyImpl"
        );
        
        // XMLBeans 基礎類 - 這些類應該都存在
        private static final List<String> XMLBEANS_CORE_CLASSES = Arrays.asList(
            "org.apache.xmlbeans.XmlObject",
            "org.apache.xmlbeans.XmlOptions",
            "org.apache.xmlbeans.SchemaType"
        );
        
        // AWT 相關類
        private static final List<String> AWT_CLASSES = Arrays.asList(
            "java.awt.Color",
            "java.awt.Font",
            "java.awt.font.FontRenderContext",
            "java.awt.geom.AffineTransform",
            "java.awt.RenderingHints",
            "java.awt.font.GlyphVector"
        );

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            registerPoiResources(hints);
            registerPoiClasses(hints);
            registerOpenXmlClasses(hints);
            registerXmlBeansClasses(hints);
            registerAwtClasses(hints);
            registerServiceLoader(hints);
        }

        /**
         * 註冊 POI 相關資源模式
         */
        private void registerPoiResources(RuntimeHints hints) {
            hints.resources()
                .registerPattern("META-INF/poi-ooxml/*")
                .registerPattern("META-INF/xmlgraphics-commons/*")
                .registerPattern("ORG/openxmlformats/schemas/*")
                .registerPattern("ORG/apache/poi/schemas/*")
                .registerPattern("org/apache/poi/ss/formula/function/*")
                .registerPattern("org/apache/poi/ss/usermodel/*")
                .registerPattern("org/openxmlformats/schemas/officeDocument/x2006/customProperties/*")
                .registerPattern("org/openxmlformats/schemas/officeDocument/x2006/extendedProperties/*")
                .registerPattern("org/openxmlformats/schemas/spreadsheetml/x2006/main/*")
                .registerPattern("org/apache/xmlbeans/**")
                .registerPattern("schemaorg_apache_xmlbeans/**")
                .registerPattern("META-INF/services/*")
                .registerPattern("META-INF/services/org.apache.poi.ss.formula.udf.UDFFinder")
                .registerPattern("META-INF/services/org.apache.xmlbeans.*")
                .registerPattern("META-INF/native/macosx/*");  // 用於 macOS 的本地庫
        }

        /**
         * 註冊 POI 相關類
         */
        private void registerPoiClasses(RuntimeHints hints) {
            poiClassNames.forEach(className -> registerClassByName(hints, className));
            
            // 註冊需要在運行時初始化的類
            tryDirectStringRegister(hints, "org.apache.poi.ss.util.SheetUtil");
            tryDirectStringRegister(hints, "org.apache.poi.ss.format.CellFormatPart");
            tryDirectStringRegister(hints, "org.apache.poi.util.RandomSingleton");
            tryDirectStringRegister(hints, "org.apache.poi.hpsf.PropertySet");
            tryDirectStringRegister(hints, "org.apache.poi.util.ChainedProperties");
        }
        
        /**
         * 註冊 OpenXML 相關類
         */
        private void registerOpenXmlClasses(RuntimeHints hints) {
            // 註冊明確的 OpenXML 類
            OPENXML_CLASSES.forEach(className -> registerClassByName(hints, className));
            
            // 其餘類的字串描述直接註冊，不進行類加載
            tryDirectStringRegister(hints, "org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.PropertiesDocumentImpl");
            tryDirectStringRegister(hints, "org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.PropertiesDocumentImpl");
        }
        
        /**
         * 註冊 XMLBeans 相關類
         */
        private void registerXmlBeansClasses(RuntimeHints hints) {
            // 只註冊核心類
            XMLBEANS_CORE_CLASSES.forEach(className -> registerClassByName(hints, className));
            
            // 這些類可能不存在，直接用字串描述註冊
            tryDirectStringRegister(hints, "org.apache.xmlbeans.impl.schema.SchemaTypeImpl");
            tryDirectStringRegister(hints, "org.apache.xmlbeans.impl.schema.SchemaTypeLoaderImpl");
            tryDirectStringRegister(hints, "org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl");
            tryDirectStringRegister(hints, "org.apache.xmlbeans.impl.values.XmlObjectBase");
            tryDirectStringRegister(hints, "org.apache.xmlbeans.impl.values.XmlComplexContentImpl");
        }
        
        /**
         * 註冊 AWT 相關類
         */
        private void registerAwtClasses(RuntimeHints hints) {
            // 註冊 AWT 類
            AWT_CLASSES.forEach(className -> registerClassByName(hints, className));
        }

        /**
         * 註冊 ServiceLoader 相關配置
         */
        private void registerServiceLoader(RuntimeHints hints) {
            hints.proxies().registerJdkProxy(
                TypeReference.of("org.apache.poi.ss.formula.udf.UDFFinder"),
                TypeReference.of("java.io.Serializable")
            );
            
            // 註冊 XMLBeans 相關代理
            hints.proxies().registerJdkProxy(
                TypeReference.of("org.apache.xmlbeans.XmlObject"),
                TypeReference.of("java.io.Serializable")
            );
        }

        /**
         * 根據類名註冊類的反射提示
         * 安全處理，處理類不存在的情況
         */
        private void registerClassByName(RuntimeHints hints, String className) {
            try {
                Class<?> clazz = Class.forName(className);
                hints.reflection().registerType(
                    TypeReference.of(clazz), 
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.PUBLIC_FIELDS
                );
                log.debug("已註冊反射提示: {}", className);
            } catch (Throwable e) {
                log.warn("無法註冊類 (跳過): {}, 錯誤: {}", className, e.getMessage());
                // 發生異常時不再使用 TypeReference.of(className)
            }
        }
        
        /**
         * 嘗試直接使用字符串註冊，不加載類
         */
        private void tryDirectStringRegister(RuntimeHints hints, String className) {
            try {
                // 使用直接字符串方式註冊，避免類型引用問題
                hints.reflection().registerType(TypeReference.of(className),
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.PUBLIC_FIELDS
                );
                log.debug("已使用字符串註冊反射提示: {}", className);
            } catch (Throwable e) {
                log.warn("無法使用字符串註冊類: {}, 錯誤: {}", className, e.getMessage());
            }
        }
    }
} 
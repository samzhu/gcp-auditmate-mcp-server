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
 * 日誌系統相關類的 RuntimeHints 配置
 * 解決在 GraalVM Native Image 執行時 SLF4J 和 Logback 初始化問題
 */
@Slf4j
@ImportRuntimeHints(LoggingRuntimeHintsConfig.LoggingResourcesRegistrar.class)
@Configuration
public class LoggingRuntimeHintsConfig {

    /**
     * 註冊日誌系統相關的類和資源
     */
    static class LoggingResourcesRegistrar implements RuntimeHintsRegistrar {

        List<String> loggingClassNames = Arrays.asList(
            "ch.qos.logback.classic.Logger",
            "ch.qos.logback.classic.LoggerContext",
            "ch.qos.logback.core.Appender",
            "ch.qos.logback.classic.spi.ILoggingEvent",
            "ch.qos.logback.core.ConsoleAppender",
            "ch.qos.logback.core.FileAppender",
            "ch.qos.logback.core.spi.AppenderAttachable",
            "org.slf4j.LoggerFactory",
            "org.slf4j.impl.StaticLoggerBinder",
            "org.apache.logging.slf4j.SLF4JLogger",
            "org.apache.logging.log4j.spi.AbstractLogger"
        );

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            registerLoggingResources(hints);
            registerLoggingClasses(hints);
            registerProxies(hints);
        }

        /**
         * 註冊日誌相關資源模式
         */
        private void registerLoggingResources(RuntimeHints hints) {
            hints
                .resources()
                .registerPattern("logback.xml")
                .registerPattern("logback-test.xml")
                .registerPattern("logback-spring.xml")
                .registerPattern("logback-spring-test.xml")
                .registerPattern("META-INF/services/org.slf4j.spi.SLF4JServiceProvider")
                .registerPattern("META-INF/services/ch.qos.logback.classic.spi.Configurator");
        }

        /**
         * 註冊日誌相關類
         */
        private void registerLoggingClasses(RuntimeHints hints) {
            loggingClassNames.forEach(className -> registerClassByName(hints, className));
        }
        
        /**
         * 註冊代理類
         */
        private void registerProxies(RuntimeHints hints) {
            hints.proxies().registerJdkProxy(
                TypeReference.of("org.slf4j.Logger"),
                TypeReference.of("java.io.Serializable")
            );
        }

        /**
         * 根據類名註冊類的反射提示
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
            } catch (ClassNotFoundException e) {
                log.warn("無法註冊類: {}, 錯誤: {}", className, e.getMessage());
                // 如果類無法在編譯時找到，仍然嘗試註冊類型引用
                hints.reflection().registerType(
                    TypeReference.of(className),
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.PUBLIC_FIELDS
                );
            }
        }
    }
} 
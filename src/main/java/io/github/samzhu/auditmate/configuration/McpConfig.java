package io.github.samzhu.auditmate.configuration;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.samzhu.auditmate.tools.GcpAuditTool;

/**
 * Spring AI MCP (Model Control Protocol) 工具配置類
 * 用於註冊和配置所有 AI 工具回調
 * 
 * <p>
 * 此配置類負責：
 * <ul>
 * <li>註冊所有工具類為 Spring Bean</li>
 * <li>配置工具回調提供者</li>
 * <li>整合 Spring AI 與工具類的互動</li>
 * </ul>
 */
@Configuration
public class McpConfig {

    /**
     * 配置工具回調提供者
     * 將所有工具類註冊到 Spring AI 系統中
     * 
     * @param gcpAuditTool       GCP 查核工具
     * @return ToolCallbackProvider 工具回調提供者實例
     */
    @Bean
    ToolCallbackProvider fileSystemToolProvider(GcpAuditTool gcpAuditTool) {
        // 使用建構器模式建立工具回調提供者
        // 將所有工具類註冊為可被 AI 模型調用的工具
        return MethodToolCallbackProvider.builder()
                .toolObjects(gcpAuditTool)
                .build();
    }
}
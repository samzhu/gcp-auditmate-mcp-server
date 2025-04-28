package io.github.samzhu.auditmate.configuration;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.samzhu.auditmate.tools.GcpAuditTool;

/**
 * Spring AI MCP (Model Control Protocol) tool configuration class.
 * Used to register and configure all AI tool callbacks.
 *
 * <p>
 * This configuration class is responsible for:
 * <ul>
 * <li>Registering all tool classes as Spring Beans</li>
 * <li>Configuring tool callback providers</li>
 * <li>Integrating Spring AI with tool class interactions</li>
 * </ul>
 */
@Configuration
public class McpConfig {

    /**
     * Configure the tool callback provider.
     * Register all tool classes into the Spring AI system.
     *
     * @param gcpAuditTool       GCP audit tool
     * @return ToolCallbackProvider instance
     */
    @Bean
    ToolCallbackProvider fileSystemToolProvider(GcpAuditTool gcpAuditTool) {
        // Use builder pattern to create the tool callback provider
        // Register all tool classes as tools callable by the AI model
        return MethodToolCallbackProvider.builder()
                .toolObjects(gcpAuditTool)
                .build();
    }
}
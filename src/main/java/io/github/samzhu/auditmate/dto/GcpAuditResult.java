package io.github.samzhu.auditmate.dto;

import java.time.LocalDateTime;

import org.springframework.ai.tool.annotation.ToolParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;

/**
 * GCP audit result data transfer object.
 * This class is used to return the results of the GCP audit tool execution, including the report file path and related metadata.
 */
@Data
public class GcpAuditResult {
    
    /**
     * Generated report file path
     * Contains the absolute path of the generated Excel report file
     */
    @ToolParam(description = "GCP project ID")
    @JsonProperty("reportFilePath")
    @JsonPropertyDescription("The absolute path of the generated Excel report file")
    private String reportFilePath;
    
    /**
     * Audit time
     * The timestamp when the audit was executed
     */
    @JsonProperty("auditTime")
    @JsonPropertyDescription("The timestamp when the audit was executed")
    private LocalDateTime auditTime;
    
    /**
     * Project ID
     * The GCP project ID being audited
     */
    @JsonProperty("projectId")
    @JsonPropertyDescription("The GCP project ID being audited")
    private String projectId;
    
    /**
     * Year
     * The year of the audit, e.g. 2025
     */
    @JsonProperty("year")
    @JsonPropertyDescription("The year of the audit, e.g. 2025")
    private String year;
    
    /**
     * Quarter (H1 or H2)
     * H1 means the first half of the year, H2 means the second half
     */
    @JsonProperty("quarter")
    @JsonPropertyDescription("The quarter of the audit, H1 means the first half, H2 means the second half")
    private String quarter;
    
    /**
     * Audit status
     * SUCCESS means the audit succeeded, FAILED means the audit failed
     */
    @JsonProperty("status")
    @JsonPropertyDescription("Audit status, SUCCESS means succeeded, FAILED means failed")
    private String status;
    
    /**
     * Error message (if any)
     * The error message when the audit fails
     */
    @JsonProperty("errorMessage")
    @JsonPropertyDescription("The error message when the audit fails")
    private String errorMessage;
    
    /**
     * Stack trace (if any)
     * The full stack trace information when the audit fails
     */
    @JsonProperty("stackTrace")
    @JsonPropertyDescription("The full stack trace information when the audit fails")
    private String stackTrace;
} 
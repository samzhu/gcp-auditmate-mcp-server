package io.github.samzhu.auditmate.dto;

import java.time.LocalDateTime;

import org.springframework.ai.tool.annotation.ToolParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;

/**
 * GCP 查核結果數據傳輸對象
 * 此類用於返回 GCP 查核工具執行的結果，包含查核報告的檔案路徑和相關元數據
 */
@Data
public class GcpAuditResult {
    
    /**
     * 生成的報告檔案路徑
     * 包含生成的 Excel 報告檔案的絕對路徑
     */
    @ToolParam(description = "GCP 專案 ID")
    @JsonProperty("reportFilePath")
    @JsonPropertyDescription("生成的 Excel 報告檔案的絕對路徑")
    private String reportFilePath;
    
    /**
     * 查核時間
     * 執行查核的時間戳記
     */
    @JsonProperty("auditTime")
    @JsonPropertyDescription("執行查核的時間戳記")
    private LocalDateTime auditTime;
    
    /**
     * 專案 ID
     * 被查核的 GCP 專案 ID
     */
    @JsonProperty("projectId")
    @JsonPropertyDescription("被查核的 GCP 專案 ID")
    private String projectId;
    
    /**
     * 年份
     * 查核的年份，例如 2025
     */
    @JsonProperty("year")
    @JsonPropertyDescription("查核的年份，例如 2025")
    private String year;
    
    /**
     * 季度（H1 或 H2）
     * H1 表示上半年，H2 表示下半年
     */
    @JsonProperty("quarter")
    @JsonPropertyDescription("查核的季度，H1 表示上半年，H2 表示下半年")
    private String quarter;
    
    /**
     * 查核狀態
     * SUCCESS 表示查核成功，FAILED 表示查核失敗
     */
    @JsonProperty("status")
    @JsonPropertyDescription("查核狀態，SUCCESS 表示查核成功，FAILED 表示查核失敗")
    private String status;
    
    /**
     * 錯誤訊息（如果有）
     * 當查核失敗時的錯誤訊息
     */
    @JsonProperty("errorMessage")
    @JsonPropertyDescription("當查核失敗時的錯誤訊息")
    private String errorMessage;
    
    /**
     * 堆疊追蹤（如果有）
     * 當查核失敗時的完整堆疊追蹤信息
     */
    @JsonProperty("stackTrace")
    @JsonPropertyDescription("當查核失敗時的完整堆疊追蹤信息")
    private String stackTrace;
} 
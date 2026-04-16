package com.compliance.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewTaskRespVO {

    private Long id;
    private Long tenantId;
    private Long submittedBy;
    private String contentType;
    private String productType;
    private String channel;
    private String originalContent;
    private String fileUrl;
    private String overallVerdict;
    private Integer riskScore;
    private String riskLevel;
    private String reviewStatus;
    private String llmModel;
    private Integer llmLatencyMs;
    private Integer totalLatencyMs;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private List<ReviewResultRespVO> results;
    private List<ReviewMissingElementRespVO> missingElements;
}

package com.compliance.module.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_task")
public class ReviewTaskDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "product_type", length = 50)
    private String productType;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "original_content", columnDefinition = "LONGTEXT")
    private String originalContent;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "parsed_segments", columnDefinition = "JSON")
    private String parsedSegments;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "overall_verdict", length = 20)
    private String overallVerdict;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @Column(name = "llm_raw_prompt", columnDefinition = "LONGTEXT")
    private String llmRawPrompt;

    @Column(name = "llm_raw_response", columnDefinition = "LONGTEXT")
    private String llmRawResponse;

    @Column(name = "llm_latency_ms")
    private Integer llmLatencyMs;

    @Column(name = "total_latency_ms")
    private Integer totalLatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reviewStatus == null) {
            reviewStatus = "pending";
        }
        if (riskScore == null) {
            riskScore = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

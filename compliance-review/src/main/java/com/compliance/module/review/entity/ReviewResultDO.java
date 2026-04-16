package com.compliance.module.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_result")
public class ReviewResultDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "segment_index", nullable = false)
    private Integer segmentIndex;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "text_offset")
    private Integer textOffset;

    @Column(name = "text_length")
    private Integer textLength;

    @Column(name = "verdict", nullable = false, length = 20)
    private String verdict;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "issue_type", length = 100)
    private String issueType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "law_article_id")
    private Long lawArticleId;

    @Column(name = "cited_article_code", length = 100)
    private String citedArticleCode;

    @Column(name = "cited_law_name", length = 500)
    private String citedLawName;

    @Column(name = "citation_status", length = 20)
    private String citationStatus;

    @Column(name = "verified_original_text", columnDefinition = "TEXT")
    private String verifiedOriginalText;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (citationStatus == null) {
            citationStatus = "pending";
        }
    }
}

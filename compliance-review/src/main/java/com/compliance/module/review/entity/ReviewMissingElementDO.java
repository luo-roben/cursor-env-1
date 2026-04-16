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
@Table(name = "review_missing_element")
public class ReviewMissingElementDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "element", nullable = false, length = 200)
    private String element;

    @Column(name = "requirement", columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "law_article_id")
    private Long lawArticleId;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (severity == null) {
            severity = "major";
        }
    }
}

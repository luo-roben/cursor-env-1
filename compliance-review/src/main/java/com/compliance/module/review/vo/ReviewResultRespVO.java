package com.compliance.module.review.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewResultRespVO {

    private Long id;
    private Integer segmentIndex;
    private String originalText;
    private String verdict;
    private BigDecimal confidence;
    private String issueType;
    private String severity;
    private String description;
    private Long lawArticleId;
    private String citedArticleCode;
    private String citedLawName;
    private String citationStatus;
    private String verifiedOriginalText;
    private String suggestion;
}

package com.compliance.module.review.vo;

import lombok.Data;

@Data
public class ReviewMissingElementRespVO {

    private Long id;
    private String element;
    private String requirement;
    private Long lawArticleId;
    private String severity;
    private String suggestion;
}

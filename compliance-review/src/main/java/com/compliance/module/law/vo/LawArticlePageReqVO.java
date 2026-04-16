package com.compliance.module.law.vo;

import lombok.Data;

@Data
public class LawArticlePageReqVO {

    private String lawName;
    private String normType;
    private String status;
    private Integer authorityLevel;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}

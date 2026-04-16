package com.compliance.module.review.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewSubmitReqVO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "提交人ID不能为空")
    private Long submittedBy;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    private String productType;
    private String channel;

    @NotBlank(message = "审查内容不能为空")
    private String originalContent;

    private String fileUrl;
}

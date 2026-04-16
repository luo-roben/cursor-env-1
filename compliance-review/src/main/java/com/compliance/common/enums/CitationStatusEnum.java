package com.compliance.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CitationStatusEnum {

    VERIFIED("verified", "已验证"),
    CORRECTED("corrected", "已修正"),
    UNVERIFIED("unverified", "未验证"),
    PENDING("pending", "待验证");

    private final String value;
    private final String label;
}

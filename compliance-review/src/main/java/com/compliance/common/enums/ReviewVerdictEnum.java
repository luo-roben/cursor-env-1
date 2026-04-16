package com.compliance.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewVerdictEnum {

    VIOLATION("violation", "违规"),
    COMPLIANT("compliant", "合规"),
    NEEDS_REVIEW("needs_review", "需人工复核");

    private final String value;
    private final String label;
}

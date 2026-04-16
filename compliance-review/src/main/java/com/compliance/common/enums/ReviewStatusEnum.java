package com.compliance.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStatusEnum {

    PENDING("pending", "待审查"),
    REVIEWING("reviewing", "审查中"),
    COMPLETED("completed", "已完成"),
    HUMAN_REVIEWED("human_reviewed", "人工已复核");

    private final String value;
    private final String label;
}

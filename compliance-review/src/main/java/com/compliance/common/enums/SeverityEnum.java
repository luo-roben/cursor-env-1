package com.compliance.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeverityEnum {

    CRITICAL("critical", "严重", 40),
    MAJOR("major", "重要", 20),
    MINOR("minor", "轻微", 5),
    INFO("info", "提示", 0);

    private final String value;
    private final String label;
    private final int score;
}

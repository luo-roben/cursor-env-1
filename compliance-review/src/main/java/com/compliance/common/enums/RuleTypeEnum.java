package com.compliance.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RuleTypeEnum {

    BANNED_WORD("banned_word", "禁用词"),
    REQUIRED_STATEMENT("required_statement", "必备声明"),
    PRODUCT_INFO("product_info", "产品信息"),
    NL_RULE("nl_rule", "自然语言规则");

    private final String value;
    private final String label;
}

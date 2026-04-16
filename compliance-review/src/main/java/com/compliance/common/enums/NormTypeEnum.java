package com.compliance.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NormTypeEnum {

    PROHIBITION("禁止", "Prohibition"),
    OBLIGATION("义务", "Obligation"),
    RIGHT("权利", "Right"),
    DEFINITION("定义", "Definition"),
    PROCEDURE("程序", "Procedure");

    private final String value;
    private final String label;
}

package com.compliance.common.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final int code;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }
}

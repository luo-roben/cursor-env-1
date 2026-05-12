package com.compliance.common.exception;

import com.compliance.common.result.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceException e) {
        log.warn("Business exception: code={}, msg={}", e.getCode(), e.getMessage());
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("Validation exception: {}", message);
        return CommonResult.error(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public CommonResult<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        log.warn("Bind exception: {}", message);
        return CommonResult.error(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return CommonResult.error(ErrorCode.INTERNAL_ERROR);
    }
}

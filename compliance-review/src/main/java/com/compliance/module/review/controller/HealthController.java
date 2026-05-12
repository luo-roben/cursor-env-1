package com.compliance.module.review.controller;

import com.compliance.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "健康检查", description = "System health check")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "健康检查", description = "Check system health status")
    @GetMapping("/health")
    public CommonResult<Map<String, Object>> health() {
        return CommonResult.success(Map.of(
                "status", "UP",
                "service", "compliance-review",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

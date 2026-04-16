package com.compliance.module.tenant.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import com.compliance.module.tenant.service.TenantCustomRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "企业自定义规则", description = "Tenant custom rule management")
@RestController
@RequestMapping("/api/v1/tenant/rules")
@RequiredArgsConstructor
public class TenantCustomRuleController {

    private final TenantCustomRuleService ruleService;

    @Operation(summary = "创建自定义规则", description = "Create a custom compliance rule for a tenant")
    @PostMapping
    public CommonResult<TenantCustomRuleDO> create(@RequestBody Map<String, Object> body) {
        Long tenantId = Long.valueOf(body.get("tenantId").toString());
        String ruleType = (String) body.get("ruleType");
        String content = (String) body.get("content");
        Integer priority = body.get("priority") != null ? Integer.valueOf(body.get("priority").toString()) : null;
        String applicableContentTypes = body.get("applicableContentTypes") != null ?
                body.get("applicableContentTypes").toString() : null;
        String matchMode = (String) body.get("matchMode");
        String severityIfTriggered = (String) body.get("severityIfTriggered");

        return CommonResult.success(ruleService.create(tenantId, ruleType, content,
                priority, applicableContentTypes, matchMode, severityIfTriggered));
    }

    @Operation(summary = "查询租户规则列表", description = "List custom rules for a tenant")
    @GetMapping
    public CommonResult<List<TenantCustomRuleDO>> list(
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        return CommonResult.success(ruleService.listByTenantId(tenantId));
    }

    @Operation(summary = "删除自定义规则", description = "Delete a custom rule")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        ruleService.delete(id, tenantId);
        return CommonResult.success();
    }
}

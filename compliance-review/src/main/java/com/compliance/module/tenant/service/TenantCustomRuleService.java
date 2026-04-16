package com.compliance.module.tenant.service;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import com.compliance.module.tenant.repository.TenantCustomRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCustomRuleService {

    private final TenantCustomRuleRepository ruleRepository;

    @Transactional
    public TenantCustomRuleDO create(Long tenantId, String ruleType, String content,
                                      Integer priority, String applicableContentTypes,
                                      String matchMode, String severityIfTriggered) {
        TenantCustomRuleDO rule = TenantCustomRuleDO.builder()
                .tenantId(tenantId)
                .ruleType(ruleType)
                .content(content)
                .priority(priority != null ? priority : 0)
                .applicableContentTypes(applicableContentTypes)
                .matchMode(matchMode != null ? matchMode : "exact")
                .severityIfTriggered(severityIfTriggered != null ? severityIfTriggered : "major")
                .enabled(true)
                .build();

        rule = ruleRepository.save(rule);
        log.info("Created custom rule: id={}, tenantId={}, ruleType={}", rule.getId(), tenantId, ruleType);
        return rule;
    }

    public List<TenantCustomRuleDO> listByTenantId(Long tenantId) {
        return ruleRepository.findByTenantIdOrderByPriorityDesc(tenantId);
    }

    @Transactional
    public void delete(Long id, Long tenantId) {
        TenantCustomRuleDO rule = ruleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ServiceException(ErrorCode.TENANT_RULE_NOT_FOUND));
        ruleRepository.delete(rule);
        log.info("Deleted custom rule: id={}, tenantId={}", id, tenantId);
    }
}

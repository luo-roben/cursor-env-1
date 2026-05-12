package com.compliance.module.tenant.repository;

import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantCustomRuleRepository extends JpaRepository<TenantCustomRuleDO, Long> {

    /** Find all enabled rules for a tenant - tenant_id for isolation */
    List<TenantCustomRuleDO> findByTenantIdAndEnabledTrueOrderByPriorityDesc(Long tenantId);

    /** Find rule by id and tenant_id - tenant isolation */
    Optional<TenantCustomRuleDO> findByIdAndTenantId(Long id, Long tenantId);

    /** Find all rules for a tenant */
    List<TenantCustomRuleDO> findByTenantIdOrderByPriorityDesc(Long tenantId);
}

package com.compliance.module.context.dto;

import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssembledContext {

    private List<ContentSegment> segments;
    private List<LawArticleDO> relevantArticles;
    private List<TenantCustomRuleDO> customRules;
    private String assembledPrompt;
}

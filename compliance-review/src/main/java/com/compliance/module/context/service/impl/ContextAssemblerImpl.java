package com.compliance.module.context.service.impl;

import com.compliance.module.ai.prompt.PromptTemplateManager;
import com.compliance.module.context.dto.AssembledContext;
import com.compliance.module.context.dto.ContentSegment;
import com.compliance.module.context.service.ContextAssembler;
import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import com.compliance.module.tenant.repository.TenantCustomRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAssemblerImpl implements ContextAssembler {

    private final LawArticleRepository lawArticleRepository;
    private final TenantCustomRuleRepository tenantCustomRuleRepository;
    private final PromptTemplateManager promptTemplateManager;

    @Override
    public AssembledContext assemble(String content, String contentType, String productType, Long tenantId) {
        List<ContentSegment> segments = parseSegments(content);

        Set<LawArticleDO> articleSet = new LinkedHashSet<>();

        if (contentType != null) {
            articleSet.addAll(lawArticleRepository.findPublishedByContentType(contentType));
        }
        if (productType != null) {
            articleSet.addAll(lawArticleRepository.findPublishedByProductType(productType));
        }

        if (articleSet.isEmpty()) {
            articleSet.addAll(lawArticleRepository.findByStatus("published"));
        }

        List<LawArticleDO> relevantArticles = new ArrayList<>(articleSet);
        log.info("Found {} relevant law articles for contentType={}, productType={}", relevantArticles.size(), contentType, productType);

        List<TenantCustomRuleDO> customRules = tenantId != null
                ? tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(tenantId)
                : Collections.emptyList();
        log.info("Found {} custom rules for tenantId={}", customRules.size(), tenantId);

        String lawArticlesText = relevantArticles.stream()
                .map(a -> String.format("【%s %s】%s", a.getLawName(), a.getArticleId(), a.getOriginalText()))
                .collect(Collectors.joining("\n"));

        String customRulesText = customRules.stream()
                .map(r -> String.format("[%s] %s", r.getRuleType(), r.getContent()))
                .collect(Collectors.joining("\n"));

        Map<String, String> variables = new HashMap<>();
        variables.put("lawArticles", lawArticlesText.isEmpty() ? "无相关法规" : lawArticlesText);
        variables.put("customRules", customRulesText.isEmpty() ? "无自定义规则" : customRulesText);
        variables.put("contentType", contentType);
        variables.put("productType", productType != null ? productType : "未指定");
        variables.put("content", content);

        String assembledPrompt = promptTemplateManager.buildReviewPrompt(variables);

        return AssembledContext.builder()
                .segments(segments)
                .relevantArticles(relevantArticles)
                .customRules(customRules)
                .assembledPrompt(assembledPrompt)
                .build();
    }

    private List<ContentSegment> parseSegments(String content) {
        List<ContentSegment> segments = new ArrayList<>();
        String[] sentences = content.split("(?<=[。！？\\n])");

        int offset = 0;
        int index = 1;
        StringBuilder buffer = new StringBuilder();
        int bufferStart = 0;

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                offset += sentence.length();
                continue;
            }

            if (buffer.isEmpty()) {
                bufferStart = offset;
            }

            buffer.append(sentence);

            if (buffer.length() >= 50 || offset + sentence.length() >= content.length()) {
                segments.add(ContentSegment.builder()
                        .index(index++)
                        .text(buffer.toString().trim())
                        .offset(bufferStart)
                        .length(buffer.length())
                        .build());
                buffer = new StringBuilder();
            }

            offset += sentence.length();
        }

        if (!buffer.isEmpty()) {
            segments.add(ContentSegment.builder()
                    .index(index)
                    .text(buffer.toString().trim())
                    .offset(bufferStart)
                    .length(buffer.length())
                    .build());
        }

        if (segments.isEmpty()) {
            segments.add(ContentSegment.builder()
                    .index(1)
                    .text(content)
                    .offset(0)
                    .length(content.length())
                    .build());
        }

        return segments;
    }
}

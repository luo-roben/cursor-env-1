package com.compliance.module.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MVP implementation using a mock chat model.
 * Analyzes input text for compliance-sensitive keywords and generates realistic review results.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelFactoryImpl implements AiModelFactory {

    private final ObjectMapper objectMapper;

    private static final Map<String, String> VIOLATION_KEYWORDS = new HashMap<>();

    static {
        VIOLATION_KEYWORDS.put("保本", "承诺保本保收益");
        VIOLATION_KEYWORDS.put("保收益", "承诺保本保收益");
        VIOLATION_KEYWORDS.put("收益率", "使用具体收益率数字进行宣传");
        VIOLATION_KEYWORDS.put("预期收益", "使用预期收益误导投资者");
        VIOLATION_KEYWORDS.put("稳赚", "使用夸大宣传用语");
        VIOLATION_KEYWORDS.put("零风险", "虚假宣传无风险");
        VIOLATION_KEYWORDS.put("最高", "使用极端用语进行宣传");
        VIOLATION_KEYWORDS.put("最佳", "使用极端用语进行宣传");
        VIOLATION_KEYWORDS.put("最好", "使用极端用语进行宣传");
        VIOLATION_KEYWORDS.put("业绩最优", "不当业绩比较");
    }

    @Override
    public Object getChatModel(String modelName) {
        return "MockChatModel-" + modelName;
    }

    @Override
    public String generate(String modelName, String prompt) {
        log.info("MockChatModel generating response for model: {}", modelName);

        try {
            String content = extractContentFromPrompt(prompt);
            List<String> segments = splitIntoSegments(content);

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> segmentResults = new ArrayList<>();
            List<Map<String, Object>> missingElements = new ArrayList<>();

            boolean hasViolation = false;
            int segIdx = 1;

            for (String segment : segments) {
                Map<String, Object> result = new HashMap<>();
                result.put("segmentIndex", segIdx);
                result.put("originalText", segment);

                String matchedKeyword = null;
                String matchedDesc = null;
                for (Map.Entry<String, String> entry : VIOLATION_KEYWORDS.entrySet()) {
                    if (segment.contains(entry.getKey())) {
                        matchedKeyword = entry.getKey();
                        matchedDesc = entry.getValue();
                        break;
                    }
                }

                if (matchedKeyword != null) {
                    hasViolation = true;
                    result.put("verdict", "violation");
                    result.put("confidence", new BigDecimal("0.92"));
                    result.put("issueType", "misleading_promotion");
                    result.put("severity", "major");
                    result.put("description", "检测到违规关键词\"" + matchedKeyword + "\": " + matchedDesc);
                    result.put("citedArticleCode", "第二十条第一款");
                    result.put("citedLawName", "证券期货投资者适当性管理办法");
                    result.put("suggestion", "建议删除\"" + matchedKeyword + "\"相关表述，改用合规话术");
                } else {
                    result.put("verdict", "compliant");
                    result.put("confidence", new BigDecimal("0.95"));
                    result.put("issueType", null);
                    result.put("severity", null);
                    result.put("description", "该段落未发现合规问题");
                    result.put("citedArticleCode", null);
                    result.put("citedLawName", null);
                    result.put("suggestion", null);
                }

                segmentResults.add(result);
                segIdx++;
            }

            if (!content.contains("风险提示") && !content.contains("风险揭示")) {
                Map<String, Object> missing = new HashMap<>();
                missing.put("element", "风险提示声明");
                missing.put("requirement", "基金宣传推介材料必须包含风险提示");
                missing.put("severity", "critical");
                missing.put("suggestion", "请在材料末尾添加：\"基金有风险，投资需谨慎\"等风险提示语");
                missingElements.add(missing);
            }

            response.put("segments", segmentResults);
            response.put("missingElements", missingElements);
            response.put("overallVerdict", hasViolation ? "violation" : "compliant");

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Mock model generation failed", e);
            return "{\"segments\":[],\"missingElements\":[],\"overallVerdict\":\"needs_review\"}";
        }
    }

    private String extractContentFromPrompt(String prompt) {
        int contentIdx = prompt.indexOf("## 待审查内容");
        if (contentIdx >= 0) {
            String afterHeader = prompt.substring(contentIdx);
            int nextSection = afterHeader.indexOf("## 输出要求");
            if (nextSection > 0) {
                return afterHeader.substring(0, nextSection).trim();
            }
            return afterHeader.trim();
        }
        return prompt;
    }

    private List<String> splitIntoSegments(String content) {
        List<String> segments = new ArrayList<>();
        String[] lines = content.split("[。！？\\n]+");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (current.length() + trimmed.length() > 200 && !current.isEmpty()) {
                segments.add(current.toString());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("。");
            }
            current.append(trimmed);
        }

        if (!current.isEmpty()) {
            segments.add(current.toString());
        }

        if (segments.isEmpty()) {
            segments.add(content);
        }

        return segments;
    }
}

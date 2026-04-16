package com.compliance.module.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Loads prompt templates from application.yml configuration.
 * Templates use {placeholder} syntax for variable substitution.
 */
@Slf4j
@Component
public class PromptTemplateManager {

    @Value("${compliance.prompt.review-template}")
    private String reviewTemplate;

    public String buildReviewPrompt(Map<String, String> variables) {
        String prompt = reviewTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            prompt = prompt.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "无");
        }
        log.debug("Built review prompt, length={}", prompt.length());
        return prompt;
    }

    public String getReviewTemplate() {
        return reviewTemplate;
    }
}

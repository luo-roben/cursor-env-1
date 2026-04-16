package com.compliance.module.verification.service.impl;

import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.RiskScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RiskScoreCalculatorImpl implements RiskScoreCalculator {

    private static final Map<String, Integer> SEVERITY_SCORES = Map.of(
            "critical", 40,
            "major", 20,
            "minor", 5,
            "info", 0
    );

    @Override
    public int calculateRiskScore(List<ReviewResultDO> results, List<ReviewMissingElementDO> missingElements) {
        int totalScore = 0;

        for (ReviewResultDO result : results) {
            if ("violation".equals(result.getVerdict()) && result.getSeverity() != null) {
                totalScore += SEVERITY_SCORES.getOrDefault(result.getSeverity(), 5);
            }
        }

        for (ReviewMissingElementDO missing : missingElements) {
            if (missing.getSeverity() != null) {
                totalScore += SEVERITY_SCORES.getOrDefault(missing.getSeverity(), 10);
            }
        }

        int capped = Math.min(totalScore, 100);
        log.debug("Risk score calculated: raw={}, capped={}", totalScore, capped);
        return capped;
    }

    @Override
    public String calculateRiskLevel(int riskScore) {
        if (riskScore >= 60) return "high";
        if (riskScore >= 30) return "medium";
        return "low";
    }
}

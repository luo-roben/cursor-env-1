package com.compliance.module.verification.service;

import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewMissingElementDO;

import java.util.List;

public interface RiskScoreCalculator {

    /**
     * Calculate the overall risk score for a review task based on individual results.
     * Two-level scoring: issue severity → task score (capped at 100).
     */
    int calculateRiskScore(List<ReviewResultDO> results, List<ReviewMissingElementDO> missingElements);

    /**
     * Determine risk level based on risk score.
     */
    String calculateRiskLevel(int riskScore);
}

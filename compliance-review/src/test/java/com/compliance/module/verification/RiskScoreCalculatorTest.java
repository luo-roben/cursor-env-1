package com.compliance.module.verification;

import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.impl.RiskScoreCalculatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskScoreCalculatorTest {

    private RiskScoreCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculatorImpl();
    }

    @Test
    void testEmptyResults() {
        int score = calculator.calculateRiskScore(Collections.emptyList(), Collections.emptyList());
        assertEquals(0, score);
        assertEquals("low", calculator.calculateRiskLevel(score));
    }

    @Test
    void testCriticalViolation() {
        List<ReviewResultDO> results = new ArrayList<>();
        ReviewResultDO result = new ReviewResultDO();
        result.setVerdict("violation");
        result.setSeverity("critical");
        results.add(result);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(40, score);
        assertEquals("medium", calculator.calculateRiskLevel(score));
    }

    @Test
    void testMixedSeverities() {
        List<ReviewResultDO> results = new ArrayList<>();

        ReviewResultDO r1 = new ReviewResultDO();
        r1.setVerdict("violation");
        r1.setSeverity("critical");
        results.add(r1);

        ReviewResultDO r2 = new ReviewResultDO();
        r2.setVerdict("violation");
        r2.setSeverity("major");
        results.add(r2);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(60, score);
        assertEquals("high", calculator.calculateRiskLevel(score));
    }

    @Test
    void testScoreCapping() {
        List<ReviewResultDO> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ReviewResultDO r = new ReviewResultDO();
            r.setVerdict("violation");
            r.setSeverity("critical");
            results.add(r);
        }

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(100, score);
    }

    @Test
    void testMissingElementsContribution() {
        List<ReviewMissingElementDO> missingElements = new ArrayList<>();
        ReviewMissingElementDO me = new ReviewMissingElementDO();
        me.setSeverity("critical");
        missingElements.add(me);

        int score = calculator.calculateRiskScore(Collections.emptyList(), missingElements);
        assertEquals(40, score);
    }

    @Test
    void testCompliantResultsNoScore() {
        List<ReviewResultDO> results = new ArrayList<>();
        ReviewResultDO r = new ReviewResultDO();
        r.setVerdict("compliant");
        r.setSeverity("info");
        results.add(r);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(0, score);
    }

    @Test
    void testRiskLevels() {
        assertEquals("low", calculator.calculateRiskLevel(0));
        assertEquals("low", calculator.calculateRiskLevel(29));
        assertEquals("medium", calculator.calculateRiskLevel(30));
        assertEquals("medium", calculator.calculateRiskLevel(59));
        assertEquals("high", calculator.calculateRiskLevel(60));
        assertEquals("high", calculator.calculateRiskLevel(100));
    }
}

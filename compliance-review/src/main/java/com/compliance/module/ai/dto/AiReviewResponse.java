package com.compliance.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponse {

    private List<SegmentResult> segments;
    private List<MissingElement> missingElements;
    private String overallVerdict;
    private String rawResponse;
    private long latencyMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentResult {
        private int segmentIndex;
        private String originalText;
        private String verdict;
        private BigDecimal confidence;
        private String issueType;
        private String severity;
        private String description;
        private String citedArticleCode;
        private String citedLawName;
        private String suggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingElement {
        private String element;
        private String requirement;
        private String severity;
        private String suggestion;
    }
}

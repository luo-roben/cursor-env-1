package com.compliance.module.ai.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.module.ai.dto.AiReviewRequest;
import com.compliance.module.ai.dto.AiReviewResponse;
import com.compliance.module.ai.model.AiModelFactory;
import com.compliance.module.ai.service.AiReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewServiceImpl implements AiReviewService {

    private final AiModelFactory aiModelFactory;
    private final ObjectMapper objectMapper;

    @Value("${compliance.llm.default-model:mock}")
    private String defaultModel;

    @Value("${compliance.llm.max-retries:3}")
    private int maxRetries;

    @Override
    public AiReviewResponse review(AiReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String rawResponse = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                rawResponse = aiModelFactory.generate(defaultModel, request.getAssembledPrompt());
                AiReviewResponse response = parseResponse(rawResponse);
                response.setRawResponse(rawResponse);
                response.setLatencyMs(System.currentTimeMillis() - startTime);
                return response;
            } catch (Exception e) {
                log.warn("AI review attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    throw new ServiceException(ErrorCode.AI_SERVICE_ERROR);
                }
            }
        }

        throw new ServiceException(ErrorCode.AI_SERVICE_ERROR);
    }

    @SuppressWarnings("unchecked")
    private AiReviewResponse parseResponse(String rawResponse) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(rawResponse,
                    new TypeReference<Map<String, Object>>() {});

            List<AiReviewResponse.SegmentResult> segments = new ArrayList<>();
            List<Map<String, Object>> segmentList = (List<Map<String, Object>>) responseMap.get("segments");
            if (segmentList != null) {
                for (Map<String, Object> seg : segmentList) {
                    AiReviewResponse.SegmentResult sr = AiReviewResponse.SegmentResult.builder()
                            .segmentIndex(getInt(seg, "segmentIndex"))
                            .originalText(getString(seg, "originalText"))
                            .verdict(getString(seg, "verdict"))
                            .confidence(getBigDecimal(seg, "confidence"))
                            .issueType(getString(seg, "issueType"))
                            .severity(getString(seg, "severity"))
                            .description(getString(seg, "description"))
                            .citedArticleCode(getString(seg, "citedArticleCode"))
                            .citedLawName(getString(seg, "citedLawName"))
                            .suggestion(getString(seg, "suggestion"))
                            .build();
                    segments.add(sr);
                }
            }

            List<AiReviewResponse.MissingElement> missingElements = new ArrayList<>();
            List<Map<String, Object>> missingList = (List<Map<String, Object>>) responseMap.get("missingElements");
            if (missingList != null) {
                for (Map<String, Object> me : missingList) {
                    AiReviewResponse.MissingElement elem = AiReviewResponse.MissingElement.builder()
                            .element(getString(me, "element"))
                            .requirement(getString(me, "requirement"))
                            .severity(getString(me, "severity"))
                            .suggestion(getString(me, "suggestion"))
                            .build();
                    missingElements.add(elem);
                }
            }

            return AiReviewResponse.builder()
                    .segments(segments)
                    .missingElements(missingElements)
                    .overallVerdict(getString(responseMap, "overallVerdict"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", rawResponse, e);
            throw new ServiceException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return new BigDecimal(val.toString());
        }
        return null;
    }
}

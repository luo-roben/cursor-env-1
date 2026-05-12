package com.compliance.module.ai.service;

import com.compliance.module.ai.dto.AiReviewRequest;
import com.compliance.module.ai.dto.AiReviewResponse;

public interface AiReviewService {

    /**
     * Perform AI-powered compliance review on the given content.
     */
    AiReviewResponse review(AiReviewRequest request);
}

package com.compliance.module.verification.service;

import com.compliance.module.review.entity.ReviewResultDO;

import java.util.List;

public interface CitationVerifier {

    /**
     * Verify LLM-cited law articles by looking up article_id + law_name in the knowledge base.
     * Updates citation_status and verified_original_text on each result.
     */
    void verifyCitations(List<ReviewResultDO> results);
}

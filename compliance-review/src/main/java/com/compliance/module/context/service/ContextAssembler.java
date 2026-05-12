package com.compliance.module.context.service;

import com.compliance.module.context.dto.AssembledContext;

public interface ContextAssembler {

    /**
     * Assemble the full context for a compliance review:
     * 1. Parse content into segments
     * 2. Query relevant law articles (by content_type and product_type)
     * 3. Query tenant custom rules
     * 4. Build the final prompt from template
     */
    AssembledContext assemble(String content, String contentType, String productType, Long tenantId);
}

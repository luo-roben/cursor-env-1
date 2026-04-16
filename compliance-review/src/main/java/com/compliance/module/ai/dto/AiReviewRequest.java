package com.compliance.module.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiReviewRequest {

    private String content;
    private String contentType;
    private String productType;
    private String channel;
    private List<String> segments;
    private String assembledPrompt;
}

package com.compliance.module.parser;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContentParseResult {
    private String extractedText;
    private List<String> segments;
    private String fileType;
    private String parseMethod;
    private Map<String, Object> metadata;
}

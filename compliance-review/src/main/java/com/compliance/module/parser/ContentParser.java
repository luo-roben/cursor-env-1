package com.compliance.module.parser;

public interface ContentParser {
    ContentParseResult parse(byte[] fileBytes, String fileName, String contentType);
}

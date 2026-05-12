package com.compliance.module.ai.model;

public interface AiModelFactory {

    /**
     * Get a chat model by name. For MVP, only "mock" is supported.
     * Returns an Object that can generate text responses.
     */
    Object getChatModel(String modelName);

    /**
     * Generate a response from the specified model given a prompt.
     */
    String generate(String modelName, String prompt);
}

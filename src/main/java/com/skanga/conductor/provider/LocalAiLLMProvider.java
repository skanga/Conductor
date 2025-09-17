package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.localai.LocalAiChatModel;

public class LocalAiLLMProvider extends AbstractLLMProvider {

    private final LocalAiChatModel model;

    public LocalAiLLMProvider(String baseUrl, String modelName) {
        super("localai");
        this.model = LocalAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    public LocalAiLLMProvider(String baseUrl, String modelName, RetryPolicy retryPolicy) {
        super("localai", retryPolicy);
        this.model = LocalAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }
}

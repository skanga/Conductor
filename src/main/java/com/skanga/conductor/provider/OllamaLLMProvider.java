package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class OllamaLLMProvider extends AbstractLLMProvider {

    private final OllamaChatModel model;

    public OllamaLLMProvider(String baseUrl, String modelName) {
        super("ollama");
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    public OllamaLLMProvider(String baseUrl, String modelName, RetryPolicy retryPolicy) {
        super("ollama", retryPolicy);
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }
}

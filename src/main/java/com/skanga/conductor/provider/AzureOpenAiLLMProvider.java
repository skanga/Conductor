package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;

public class AzureOpenAiLLMProvider extends AbstractLLMProvider {

    private final AzureOpenAiChatModel model;

    public AzureOpenAiLLMProvider(String apiKey, String endpoint, String deploymentName) {
        super("azure-openai");
        this.model = AzureOpenAiChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deploymentName)
                .build();
    }

    public AzureOpenAiLLMProvider(String apiKey, String endpoint, String deploymentName, RetryPolicy retryPolicy) {
        super("azure-openai", retryPolicy);
        this.model = AzureOpenAiChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deploymentName)
                .build();
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }
}

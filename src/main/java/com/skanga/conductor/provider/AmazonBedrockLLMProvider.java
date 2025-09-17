package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import software.amazon.awssdk.regions.Region;

public class AmazonBedrockLLMProvider extends AbstractLLMProvider {

    private final BedrockChatModel model;

    public AmazonBedrockLLMProvider(String modelId, String region) {
        super("amazon-bedrock");
        this.model = BedrockChatModel.builder()
                .modelId(modelId)
                .region(Region.of(region))
                .build();
    }

    public AmazonBedrockLLMProvider(String modelName, String region, RetryPolicy retryPolicy) {
        super("amazon-bedrock", retryPolicy);
        this.model = BedrockChatModel.builder()
                .modelId(modelName)
                .region(Region.of(region))
                .build();
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }
}
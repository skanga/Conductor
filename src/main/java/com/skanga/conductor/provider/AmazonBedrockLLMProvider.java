package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import software.amazon.awssdk.regions.Region;

/**
 * Amazon Bedrock LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with Amazon Bedrock, AWS's fully managed service for
 * foundation models. Bedrock provides access to high-performing models from
 * leading AI companies like Anthropic, Cohere, Meta, Stability AI, and Amazon
 * through a single API. It handles authentication, scaling, and infrastructure
 * management automatically.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Access to multiple foundation models (Claude, Jurassic, Titan, etc.)</li>
 * <li>Fully managed AWS service with automatic scaling</li>
 * <li>Built-in security and compliance (VPC, encryption, IAM)</li>
 * <li>Regional deployment options for data residency</li>
 * <li>Pay-per-use pricing with no upfront costs</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Integration with AWS IAM for authentication</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * BedrockChatModel implementation is thread-safe.
 * </p>
 * <p>
 * Usage Example:
 * </p>
 * <pre>
 * // Using Claude model in US East region
 * AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
 *     "anthropic.claude-v2", "us-east-1");
 * String response = provider.generate("Explain cloud computing");
 *
 * // Using Amazon Titan model in Europe
 * AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
 *     "amazon.titan-text-express-v1", "eu-west-1");
 * </pre>
 * <p>
 * <strong>Authentication:</strong> This provider uses AWS SDK default credential chain.
 * Ensure AWS credentials are configured via environment variables, IAM roles,
 * or AWS credentials file.
 * </p>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class AmazonBedrockLLMProvider extends AbstractLLMProvider {

    private final BedrockChatModel model;
    private final String modelId;
    private final String region;

    /**
     * Creates a new Amazon Bedrock LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with the specified model ID and AWS region.
     * Authentication is handled automatically through the AWS SDK default
     * credential chain. Retry behavior is configured based on application
     * configuration settings.
     * </p>
     *
     * @param modelId the Bedrock model ID to use (e.g., "anthropic.claude-v2",
     *                "amazon.titan-text-express-v1", "cohere.command-text-v14")
     * @param region the AWS region where Bedrock is available (e.g., "us-east-1", "eu-west-1")
     */
    public AmazonBedrockLLMProvider(String modelId, String region) {
        super(standardizeProviderName("amazon-bedrock"), standardizeModelName(modelId, "anthropic.claude-v2"));
        this.modelId = modelId;
        this.region = region;
        this.model = createBedrockModel();
    }

    /**
     * Creates a new Amazon Bedrock LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed
     * for different Bedrock models or regions.
     * </p>
     *
     * @param modelId the Bedrock model ID to use (e.g., "anthropic.claude-v2",
     *                "amazon.titan-text-express-v1", "cohere.command-text-v14")
     * @param region the AWS region where Bedrock is available (e.g., "us-east-1", "eu-west-1")
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public AmazonBedrockLLMProvider(String modelId, String region, RetryPolicy retryPolicy) {
        super(standardizeProviderName("amazon-bedrock"), standardizeModelName(modelId, "anthropic.claude-v2"), retryPolicy);
        this.modelId = modelId;
        this.region = region;
        this.model = createBedrockModel();
    }

    /**
     * Generates text using the Amazon Bedrock model.
     * <p>
     * This method sends the prompt to the specified Bedrock foundation model,
     * returning the generated response. The communication is handled through
     * AWS APIs with automatic authentication using the configured AWS credentials.
     * </p>
     *
     * @param prompt the text prompt to send to the Bedrock model
     * @return the generated text response from the foundation model
     * @throws Exception if the Bedrock request fails due to authentication issues,
     *                   throttling, model not available, insufficient permissions,
     *                   or other AWS service-related problems
     */
    /**
     * Creates the Amazon Bedrock chat model using the template method pattern.
     *
     * @return configured Amazon Bedrock chat model
     */
    private BedrockChatModel createBedrockModel() {
        return createModel(
            BedrockChatModel::builder,
            builder -> builder
                .modelId(modelId)
                .region(Region.of(region))
        );
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Builder for creating Amazon Bedrock LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<AmazonBedrockLLMProvider, Builder> {
        private String modelId;
        private String region;

        public Builder() {
            super("amazon-bedrock");
        }

        /**
         * Sets the Bedrock model ID.
         *
         * @param modelId the model ID
         * @return this builder for method chaining
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Sets the AWS region.
         *
         * @param region the AWS region
         * @return this builder for method chaining
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        @Override
        public AmazonBedrockLLMProvider build() {
            if (modelId == null || region == null) {
                throw new IllegalArgumentException("Model ID and region are required");
            }

            if (retryPolicy != null) {
                return new AmazonBedrockLLMProvider(modelId, region, retryPolicy);
            } else {
                return new AmazonBedrockLLMProvider(modelId, region);
            }
        }
    }

    /**
     * Creates a new builder for Amazon Bedrock LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
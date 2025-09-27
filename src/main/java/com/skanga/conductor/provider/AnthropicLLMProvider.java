package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.anthropic.AnthropicChatModel;

/**
 * Anthropic LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with Anthropic's Claude API through the
 * LangChain4j library, providing access to Claude models like Claude-3,
 * Claude-3.5, and other Anthropic language models. It handles API
 * communication, authentication, and response processing with automatic
 * retry logic.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Support for various Claude model versions</li>
 * <li>Configurable API key authentication</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Synchronous text generation</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * AnthropicChatModel implementation is thread-safe.
 * </p>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class AnthropicLLMProvider extends AbstractLLMProvider {

    private final AnthropicChatModel model;
    private final String apiKey;
    private final String modelName;

    /**
     * Creates a new Anthropic LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with API key and model name. Retry behavior
     * is configured based on application configuration settings.
     * </p>
     *
     * @param apiKey the Anthropic API key for authentication
     * @param modelName the name of the model to use (e.g., "claude-3-5-sonnet-20241022", "claude-3-haiku-20240307")
     */
    public AnthropicLLMProvider(String apiKey, String modelName) {
        super(standardizeProviderName("anthropic"), getEffectiveModelName(modelName));
        this.apiKey = apiKey;
        this.modelName = getEffectiveModelName(modelName);
        this.model = createAnthropicModel();
    }

    /**
     * Creates a new Anthropic LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed.
     * </p>
     *
     * @param apiKey the Anthropic API key for authentication
     * @param modelName the name of the model to use
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public AnthropicLLMProvider(String apiKey, String modelName, RetryPolicy retryPolicy) {
        super(standardizeProviderName("anthropic"), getEffectiveModelName(modelName), retryPolicy);
        this.apiKey = apiKey;
        this.modelName = getEffectiveModelName(modelName);
        this.model = createAnthropicModel();
    }

    /**
     * Creates the Anthropic chat model using the template method pattern.
     *
     * @return configured Anthropic chat model
     */
    private AnthropicChatModel createAnthropicModel() {
        // Handle null or empty API key by using a placeholder for testing
        String effectiveApiKey = (apiKey == null || apiKey.trim().isEmpty()) ? "test-api-key" : apiKey;

        return createModel(
            AnthropicChatModel::builder,
            builder -> builder
                .apiKey(effectiveApiKey)
                .modelName(modelName)
        );
    }

    /**
     * Performs the actual Anthropic LLM generation call.
     * <p>
     * This method handles the core Anthropic API interaction without retry logic,
     * which is managed by the abstract base class.
     * </p>
     *
     * @param prompt the text prompt to send to the Anthropic model
     * @return the generated text response from Anthropic
     * @throws Exception if the Anthropic API call fails
     */
    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Gets the effective model name, providing a default if null or blank.
     * For Anthropic, model names should be preserved exactly for API compatibility.
     */
    private static String getEffectiveModelName(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return "claude-3-sonnet";
        }
        return modelName.trim();
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    /**
     * Determines if an exception from Anthropic API calls should be retried.
     * <p>
     * This method provides Anthropic-specific exception classification logic
     * in addition to the common patterns handled by the base class.
     * </p>
     *
     * @param exception the exception to check
     * @return true if the exception appears to be transient and retryable
     */
    @Override
    protected boolean isRetryableException(Exception exception) {
        // First check common patterns from base class
        if (super.isRetryableException(exception)) {
            return true;
        }

        // Anthropic-specific patterns
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // Anthropic specific error patterns
        return lowerMessage.contains("anthropic") && (
               lowerMessage.contains("rate_limit_error") ||
               lowerMessage.contains("overloaded_error") ||
               lowerMessage.contains("api_error") ||
               lowerMessage.contains("service_unavailable") ||
               lowerMessage.contains("claude") && lowerMessage.contains("busy") ||
               lowerMessage.contains("claude") && lowerMessage.contains("overloaded")
        );
    }

    /**
     * Builder for creating Anthropic LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<AnthropicLLMProvider, Builder> {
        private String apiKey;
        private String anthropicModelName;

        public Builder() {
            super("anthropic");
        }

        /**
         * Sets the Anthropic API key.
         *
         * @param apiKey the API key
         * @return this builder for method chaining
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the Anthropic model name.
         *
         * @param modelName the model name
         * @return this builder for method chaining
         */
        public Builder anthropicModelName(String modelName) {
            this.anthropicModelName = modelName;
            return this;
        }

        @Override
        public AnthropicLLMProvider build() {
            if (apiKey == null || anthropicModelName == null) {
                throw new IllegalArgumentException("API key and model name are required");
            }

            if (retryPolicy != null) {
                return new AnthropicLLMProvider(apiKey, anthropicModelName, retryPolicy);
            } else {
                return new AnthropicLLMProvider(apiKey, anthropicModelName);
            }
        }
    }

    /**
     * Creates a new builder for Anthropic LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
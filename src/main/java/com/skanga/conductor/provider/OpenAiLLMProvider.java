package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * OpenAI LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with OpenAI's chat completion API through the
 * LangChain4j library, providing access to models like GPT-3.5, GPT-4, and
 * other OpenAI language models. It handles API communication, authentication,
 * and response processing with automatic retry logic.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Support for custom OpenAI API endpoints (including Azure OpenAI)</li>
 * <li>Configurable model selection</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Synchronous text generation</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * OpenAiChatModel implementation is thread-safe.
 * </p>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class OpenAiLLMProvider extends AbstractLLMProvider {

    private final OpenAiChatModel model;

    private final String apiKey;
    private final String openAiBase;

    /**
     * Creates a new OpenAI LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with custom API endpoint, API key, and model name.
     * This constructor supports both standard OpenAI endpoints and custom endpoints
     * such as Azure OpenAI Service. Retry behavior is configured based on application
     * configuration settings.
     * </p>
     *
     * @param apiKey the OpenAI API key for authentication
     * @param modelName the name of the model to use (e.g., "gpt-4", "gpt-3.5-turbo")
     * @param openAiBase the base URL for the OpenAI API endpoint
     */
    public OpenAiLLMProvider(String apiKey, String modelName, String openAiBase) {
        super(standardizeProviderName("openai"), standardizeModelName(modelName, "gpt-3.5-turbo"));
        this.apiKey = apiKey;
        this.openAiBase = openAiBase;
        this.model = createOpenAiModel();
    }

    /**
     * Creates a new OpenAI LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed.
     * </p>
     *
     * @param apiKey the OpenAI API key for authentication
     * @param modelName the name of the model to use
     * @param openAiBase the base URL for the OpenAI API endpoint
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public OpenAiLLMProvider(String apiKey, String modelName, String openAiBase, RetryPolicy retryPolicy) {
        super(standardizeProviderName("openai"), standardizeModelName(modelName, "gpt-3.5-turbo"), retryPolicy);
        this.apiKey = apiKey;
        this.openAiBase = openAiBase;
        this.model = createOpenAiModel();
    }

    /**
     * Creates the OpenAI model instance using the template method pattern.
     * <p>
     * This method eliminates duplication in model creation logic by using
     * the common pattern provided by AbstractLLMProvider.
     * </p>
     *
     * @return configured OpenAI chat model
     */
    private OpenAiChatModel createOpenAiModel() {
        // Handle null/empty baseUrl by providing a default
        String effectiveBaseUrl = (openAiBase == null || openAiBase.trim().isEmpty())
            ? "https://api.openai.com/v1" : openAiBase;

        return createModel(
            OpenAiChatModel::builder,
            builder -> builder
                .baseUrl(effectiveBaseUrl)
                .apiKey(apiKey)
                .modelName(getModelName())
        );
    }

    /**
     * Performs the actual OpenAI LLM generation call.
     * <p>
     * This method handles the core OpenAI API interaction without retry logic,
     * which is managed by the abstract base class.
     * </p>
     *
     * @param prompt the text prompt to send to the OpenAI model
     * @return the generated text response from OpenAI
     * @throws Exception if the OpenAI API call fails
     */
    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Determines if an exception from OpenAI API calls should be retried.
     * <p>
     * This method provides OpenAI-specific exception classification logic
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

        // OpenAI-specific patterns
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // OpenAI specific error patterns
        return lowerMessage.contains("openai") && (
               lowerMessage.contains("overloaded") ||
               lowerMessage.contains("capacity") ||
               lowerMessage.contains("model overloaded") ||
               lowerMessage.contains("engine overloaded")
        );
    }

    /**
     * Builder for creating OpenAI LLM providers with fluent configuration.
     * <p>
     * This builder provides a more convenient way to create OpenAI providers
     * and eliminates constructor duplication.
     * </p>
     */
    public static class Builder extends AbstractLLMProvider.Builder<OpenAiLLMProvider, Builder> {
        private String apiKey;
        private String openAiBase;

        public Builder() {
            super("openai");
        }

        /**
         * Sets the OpenAI API key.
         *
         * @param apiKey the API key
         * @return this builder for method chaining
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the OpenAI base URL.
         *
         * @param openAiBase the base URL
         * @return this builder for method chaining
         */
        public Builder baseUrl(String openAiBase) {
            this.openAiBase = openAiBase;
            return this;
        }

        /**
         * Creates a new builder instance.
         *
         * @return a new OpenAI provider builder
         */
        public static Builder create() {
            return new Builder();
        }

        @Override
        public OpenAiLLMProvider build() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("API key is required for OpenAI provider");
            }
            if (openAiBase == null || openAiBase.isBlank()) {
                openAiBase = "https://api.openai.com/v1/";
            }

            if (retryPolicy != null) {
                return new OpenAiLLMProvider(apiKey, modelName, openAiBase, retryPolicy);
            } else {
                return new OpenAiLLMProvider(apiKey, modelName, openAiBase);
            }
        }
    }

    /**
     * Creates a new builder for configuring an OpenAI provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}

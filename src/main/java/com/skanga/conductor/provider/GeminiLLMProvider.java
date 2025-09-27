package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

/**
 * Google Gemini LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with Google's Gemini AI API through the
 * LangChain4j library, providing access to Gemini models like Gemini Pro,
 * Gemini Pro Vision, and other Google AI language models. It handles API
 * communication, authentication, and response processing with automatic
 * retry logic.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Support for various Gemini model versions</li>
 * <li>Configurable API key authentication</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Synchronous text generation</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * GoogleAiGeminiChatModel implementation is thread-safe.
 * </p>
 * <p>
 * <strong>Note:</strong> This implementation is currently disabled due to
 * LangChain4j API compatibility issues. The Google AI Gemini model interface
 * may require further investigation to determine the correct method signatures
 * for text generation.
 * </p>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class GeminiLLMProvider extends AbstractLLMProvider {
    private final GoogleAiGeminiChatModel model;
    private final String apiKey;
    private final String modelName;

    /**
     * Creates a new Gemini LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with API key and model name. Retry behavior
     * is configured based on application configuration settings.
     * </p>
     *
     * @param apiKey the Google AI API key for authentication
     * @param modelName the name of the model to use (e.g., "gemini-pro", "gemini-pro-vision")
     */
    public GeminiLLMProvider(String apiKey, String modelName) {
        super(standardizeProviderName("gemini"), standardizeModelName(modelName, "gemini-pro"));
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.model = createGeminiModel();
    }

    /**
     * Creates a new Gemini LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed.
     * </p>
     *
     * @param apiKey the Google AI API key for authentication
     * @param modelName the name of the model to use
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public GeminiLLMProvider(String apiKey, String modelName, RetryPolicy retryPolicy) {
        super(standardizeProviderName("gemini"), standardizeModelName(modelName, "gemini-pro"), retryPolicy);
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.model = createGeminiModel();
    }

    /**
     * Creates the Google AI Gemini chat model using the template method pattern.
     *
     * @return configured Google AI Gemini chat model
     */
    private GoogleAiGeminiChatModel createGeminiModel() {
        return createModel(
            GoogleAiGeminiChatModel::builder,
            builder -> builder
                .apiKey(apiKey)
                .modelName(modelName)
        );
    }

    /**
     * Performs the actual Gemini LLM generation call.
     * <p>
     * This method handles the core Google AI API interaction without retry logic,
     * which is managed by the abstract base class.
     * </p>
     *
     * @param prompt the text prompt to send to the Gemini model
     * @return the generated text response from Gemini
     * @throws Exception if the Gemini API call fails
     */
    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Determines if an exception from Gemini API calls should be retried.
     * <p>
     * This method provides Gemini-specific exception classification logic
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

        // Gemini-specific patterns
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // Google AI / Gemini specific error patterns
        return lowerMessage.contains("google") && (
               lowerMessage.contains("quota exceeded") ||
               lowerMessage.contains("resource exhausted") ||
               lowerMessage.contains("backend error") ||
               lowerMessage.contains("deadline exceeded") ||
               lowerMessage.contains("unavailable") ||
               lowerMessage.contains("gemini") && lowerMessage.contains("busy") ||
               lowerMessage.contains("gemini") && lowerMessage.contains("overloaded") ||
               lowerMessage.contains("ai platform") && lowerMessage.contains("error")
        );
    }

    /**
     * Builder for creating Google Gemini LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<GeminiLLMProvider, Builder> {
        private String apiKey;
        private String geminiModelName;

        public Builder() {
            super("gemini");
        }

        /**
         * Sets the Google AI API key.
         *
         * @param apiKey the API key
         * @return this builder for method chaining
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the Gemini model name.
         *
         * @param modelName the model name
         * @return this builder for method chaining
         */
        public Builder geminiModelName(String modelName) {
            this.geminiModelName = modelName;
            return this;
        }

        @Override
        public GeminiLLMProvider build() {
            if (apiKey == null || geminiModelName == null) {
                throw new IllegalArgumentException("API key and model name are required");
            }

            if (retryPolicy != null) {
                return new GeminiLLMProvider(apiKey, geminiModelName, retryPolicy);
            } else {
                return new GeminiLLMProvider(apiKey, geminiModelName);
            }
        }
    }

    /**
     * Creates a new builder for Gemini LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
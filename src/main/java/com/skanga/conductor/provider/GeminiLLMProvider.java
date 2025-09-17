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
        super("gemini");
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
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
        super("gemini", retryPolicy);
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
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
}
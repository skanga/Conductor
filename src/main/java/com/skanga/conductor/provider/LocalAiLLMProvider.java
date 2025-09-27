package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.localai.LocalAiChatModel;

/**
 * LocalAI LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with LocalAI, an OpenAI-compatible API that allows
 * running large language models locally or on-premises. LocalAI supports a wide
 * variety of model formats and provides an OpenAI-compatible REST API for text
 * generation, making it ideal for private deployments and edge computing scenarios.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>OpenAI-compatible API for seamless integration</li>
 * <li>Support for multiple model formats (GGML, GGUF, etc.)</li>
 * <li>Local deployment with no external API dependencies</li>
 * <li>Configurable base URL for custom LocalAI deployments</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Synchronous text generation</li>
 * <li>No API key required (local deployment)</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * LocalAiChatModel implementation is thread-safe.
 * </p>
 * <p>
 * Usage Example:
 * </p>
 * <pre>
 * // Connect to local LocalAI instance
 * LocalAiLLMProvider provider = new LocalAiLLMProvider("http://localhost:8080", "gpt-3.5-turbo");
 * String response = provider.generate("Explain quantum computing");
 *
 * // Custom LocalAI deployment
 * LocalAiLLMProvider provider = new LocalAiLLMProvider("http://localai.internal:8080", "llama-7b");
 * </pre>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class LocalAiLLMProvider extends AbstractLLMProvider {

    private final LocalAiChatModel model;
    private final String baseUrl;
    private final String modelName;

    /**
     * Creates a new LocalAI LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with custom base URL and model name. The base URL
     * should point to a running LocalAI instance. The model name should match
     * one of the models configured in your LocalAI deployment. Retry behavior
     * is configured based on application configuration settings.
     * </p>
     *
     * @param baseUrl the base URL of the LocalAI instance (e.g., "http://localhost:8080")
     * @param modelName the name of the model to use (e.g., "gpt-3.5-turbo", "llama-7b")
     */
    public LocalAiLLMProvider(String baseUrl, String modelName) {
        super(standardizeProviderName("localai"), standardizeModelName(modelName, "gpt-3.5-turbo"));
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.model = createLocalAiModel();
    }

    /**
     * Creates a new LocalAI LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed
     * for local vs remote LocalAI deployments.
     * </p>
     *
     * @param baseUrl the base URL of the LocalAI instance (e.g., "http://localhost:8080")
     * @param modelName the name of the model to use (e.g., "gpt-3.5-turbo", "llama-7b")
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public LocalAiLLMProvider(String baseUrl, String modelName, RetryPolicy retryPolicy) {
        super(standardizeProviderName("localai"), standardizeModelName(modelName, "gpt-3.5-turbo"), retryPolicy);
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.model = createLocalAiModel();
    }

    /**
     * Generates text using the LocalAI model.
     * <p>
     * This method sends the prompt to the configured LocalAI instance and model,
     * returning the generated response. The communication uses the OpenAI-compatible
     * API provided by LocalAI, handled by the underlying LangChain4j LocalAiChatModel.
     * </p>
     *
     * @param prompt the text prompt to send to the LocalAI model
     * @return the generated text response from the model
     * @throws Exception if the LocalAI request fails due to network issues,
     *                   model not found, insufficient resources, or other service-related problems
     */
    /**
     * Creates the LocalAI chat model using the template method pattern.
     *
     * @return configured LocalAI chat model
     */
    private LocalAiChatModel createLocalAiModel() {
        return createModel(
            LocalAiChatModel::builder,
            builder -> builder
                .baseUrl(baseUrl)
                .modelName(modelName)
        );
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Builder for creating LocalAI LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<LocalAiLLMProvider, Builder> {
        private String baseUrl;
        private String localAiModelName;

        public Builder() {
            super("localai");
        }

        /**
         * Sets the LocalAI base URL.
         *
         * @param baseUrl the base URL
         * @return this builder for method chaining
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the LocalAI model name.
         *
         * @param modelName the model name
         * @return this builder for method chaining
         */
        public Builder localAiModelName(String modelName) {
            this.localAiModelName = modelName;
            return this;
        }

        @Override
        public LocalAiLLMProvider build() {
            if (baseUrl == null || localAiModelName == null) {
                throw new IllegalArgumentException("Base URL and model name are required");
            }

            if (retryPolicy != null) {
                return new LocalAiLLMProvider(baseUrl, localAiModelName, retryPolicy);
            } else {
                return new LocalAiLLMProvider(baseUrl, localAiModelName);
            }
        }
    }

    /**
     * Creates a new builder for LocalAI LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}

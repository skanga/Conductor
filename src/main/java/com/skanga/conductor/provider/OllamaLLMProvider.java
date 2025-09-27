package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.ollama.OllamaChatModel;

/**
 * Ollama LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with Ollama, which allows running large language models
 * locally. Ollama provides a simple API for running models like Llama, Codellama,
 * Mistral, and other open-source language models on local hardware. It handles
 * API communication and response processing with automatic retry logic.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Support for locally hosted Ollama instances</li>
 * <li>Configurable base URL for custom Ollama deployments</li>
 * <li>Support for various open-source models (Llama, Mistral, CodeLlama, etc.)</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Synchronous text generation</li>
 * <li>No API key required (local deployment)</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * OllamaChatModel implementation is thread-safe.
 * </p>
 * <p>
 * Usage Example:
 * </p>
 * <pre>
 * // Connect to local Ollama instance
 * OllamaLLMProvider provider = new OllamaLLMProvider("http://localhost:11434", "llama2");
 * String response = provider.generate("Explain machine learning");
 *
 * // Custom Ollama deployment
 * OllamaLLMProvider provider = new OllamaLLMProvider("http://ollama.company.com:11434", "codellama");
 * </pre>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class OllamaLLMProvider extends AbstractLLMProvider {

    private final OllamaChatModel model;
    private final String baseUrl;
    private final String modelName;

    /**
     * Creates a new Ollama LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with custom base URL and model name. The base URL
     * should point to a running Ollama instance. Retry behavior is configured
     * based on application configuration settings.
     * </p>
     *
     * @param baseUrl the base URL of the Ollama instance (e.g., "http://localhost:11434")
     * @param modelName the name of the model to use (e.g., "llama2", "mistral", "codellama")
     */
    public OllamaLLMProvider(String baseUrl, String modelName) {
        super(standardizeProviderName("ollama"), standardizeModelName(modelName, "llama2"));
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.model = createOllamaModel();
    }

    /**
     * Creates a new Ollama LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed
     * for local vs remote Ollama deployments.
     * </p>
     *
     * @param baseUrl the base URL of the Ollama instance (e.g., "http://localhost:11434")
     * @param modelName the name of the model to use (e.g., "llama2", "mistral", "codellama")
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public OllamaLLMProvider(String baseUrl, String modelName, RetryPolicy retryPolicy) {
        super(standardizeProviderName("ollama"), standardizeModelName(modelName, "llama2"), retryPolicy);
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.model = createOllamaModel();
    }

    /**
     * Generates text using the Ollama model.
     * <p>
     * This method sends the prompt to the configured Ollama instance and model,
     * returning the generated response. The actual HTTP communication is handled
     * by the underlying LangChain4j OllamaChatModel.
     * </p>
     *
     * @param prompt the text prompt to send to the Ollama model
     * @return the generated text response from the model
     * @throws Exception if the Ollama request fails due to network issues,
     *                   model not found, or other service-related problems
     */
    /**
     * Creates the Ollama chat model using the template method pattern.
     *
     * @return configured Ollama chat model
     */
    private OllamaChatModel createOllamaModel() {
        return createModel(
            OllamaChatModel::builder,
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
     * Builder for creating Ollama LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<OllamaLLMProvider, Builder> {
        private String baseUrl;
        private String ollamaModelName;

        public Builder() {
            super("ollama");
        }

        /**
         * Sets the Ollama base URL.
         *
         * @param baseUrl the base URL
         * @return this builder for method chaining
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the Ollama model name.
         *
         * @param modelName the model name
         * @return this builder for method chaining
         */
        public Builder ollamaModelName(String modelName) {
            this.ollamaModelName = modelName;
            return this;
        }

        @Override
        public OllamaLLMProvider build() {
            if (baseUrl == null || ollamaModelName == null) {
                throw new IllegalArgumentException("Base URL and model name are required");
            }

            if (retryPolicy != null) {
                return new OllamaLLMProvider(baseUrl, ollamaModelName, retryPolicy);
            } else {
                return new OllamaLLMProvider(baseUrl, ollamaModelName);
            }
        }
    }

    /**
     * Creates a new builder for Ollama LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}

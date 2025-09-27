package com.skanga.conductor.provider;

import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;

/**
 * Azure OpenAI LLM provider implementation using LangChain4j integration.
 * <p>
 * This provider integrates with Azure OpenAI Service, Microsoft's cloud-based
 * offering of OpenAI's language models. Azure OpenAI provides enterprise-grade
 * security, compliance, and regional availability for OpenAI models like GPT-4,
 * GPT-3.5, and other foundation models. It offers enhanced data privacy and
 * governance compared to the standard OpenAI API.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Enterprise-grade security and compliance (SOC 2, HIPAA, etc.)</li>
 * <li>Regional deployment options for data residency requirements</li>
 * <li>Integration with Azure Active Directory for authentication</li>
 * <li>Virtual network support and private endpoints</li>
 * <li>Content filtering and responsible AI features</li>
 * <li>SLA-backed availability and performance guarantees</li>
 * <li>Automatic error handling and retry logic</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * AzureOpenAiChatModel implementation is thread-safe.
 * </p>
 * <p>
 * Usage Example:
 * </p>
 * <pre>
 * // Connect to Azure OpenAI deployment
 * AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(
 *     "your-api-key",
 *     "https://your-resource.openai.azure.com",
 *     "your-gpt4-deployment");
 * String response = provider.generate("Explain Azure services");
 *
 * // Using environment-specific deployment
 * AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(
 *     System.getenv("AZURE_OPENAI_API_KEY"),
 *     System.getenv("AZURE_OPENAI_ENDPOINT"),
 *     "production-gpt35-turbo");
 * </pre>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class AzureOpenAiLLMProvider extends AbstractLLMProvider {

    private final AzureOpenAiChatModel model;
    private final String apiKey;
    private final String endpoint;
    private final String deploymentName;

    /**
     * Creates a new Azure OpenAI LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with API key, endpoint, and deployment name.
     * The endpoint should be the full URL to your Azure OpenAI resource,
     * and the deployment name should match a model deployment in your resource.
     * Retry behavior is configured based on application configuration settings.
     * </p>
     *
     * @param apiKey the Azure OpenAI API key for authentication
     * @param endpoint the Azure OpenAI endpoint URL (e.g., "https://your-resource.openai.azure.com")
     * @param deploymentName the name of your model deployment (e.g., "gpt-4", "gpt-35-turbo")
     */
    public AzureOpenAiLLMProvider(String apiKey, String endpoint, String deploymentName) {
        super(standardizeProviderName("azure-openai"), standardizeModelName(deploymentName, "gpt-35-turbo"));
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.deploymentName = deploymentName;
        this.model = createAzureOpenAiModel();
    }

    /**
     * Creates a new Azure OpenAI LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed
     * for different Azure regions or deployment configurations.
     * </p>
     *
     * @param apiKey the Azure OpenAI API key for authentication
     * @param endpoint the Azure OpenAI endpoint URL (e.g., "https://your-resource.openai.azure.com")
     * @param deploymentName the name of your model deployment (e.g., "gpt-4", "gpt-35-turbo")
     * @param retryPolicy the retry policy to use for LLM calls
     */
    public AzureOpenAiLLMProvider(String apiKey, String endpoint, String deploymentName, RetryPolicy retryPolicy) {
        super(standardizeProviderName("azure-openai"), standardizeModelName(deploymentName, "gpt-35-turbo"), retryPolicy);
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.deploymentName = deploymentName;
        this.model = createAzureOpenAiModel();
    }

    /**
     * Generates text using the Azure OpenAI model.
     * <p>
     * This method sends the prompt to the specified Azure OpenAI deployment,
     * returning the generated response. The communication is handled through
     * Azure's secure endpoints with automatic authentication and content filtering.
     * </p>
     *
     * @param prompt the text prompt to send to the Azure OpenAI model
     * @return the generated text response from the model
     * @throws Exception if the Azure OpenAI request fails due to authentication issues,
     *                   rate limiting, content filtering, deployment not found,
     *                   or other Azure service-related problems
     */
    /**
     * Creates the Azure OpenAI chat model using the template method pattern.
     *
     * @return configured Azure OpenAI chat model
     */
    private AzureOpenAiChatModel createAzureOpenAiModel() {
        return createModel(
            AzureOpenAiChatModel::builder,
            builder -> builder
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deploymentName)
        );
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Builder for creating Azure OpenAI LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<AzureOpenAiLLMProvider, Builder> {
        private String apiKey;
        private String endpoint;
        private String deploymentName;

        public Builder() {
            super("azure-openai");
        }

        /**
         * Sets the Azure OpenAI API key.
         *
         * @param apiKey the API key
         * @return this builder for method chaining
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the Azure OpenAI endpoint URL.
         *
         * @param endpoint the endpoint URL
         * @return this builder for method chaining
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Sets the Azure OpenAI deployment name.
         *
         * @param deploymentName the deployment name
         * @return this builder for method chaining
         */
        public Builder deploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        @Override
        public AzureOpenAiLLMProvider build() {
            if (apiKey == null || endpoint == null || deploymentName == null) {
                throw new IllegalArgumentException("API key, endpoint, and deployment name are required");
            }

            String modelName = this.modelName != null ? this.modelName : deploymentName;

            if (retryPolicy != null) {
                return new AzureOpenAiLLMProvider(apiKey, endpoint, deploymentName, retryPolicy);
            } else {
                return new AzureOpenAiLLMProvider(apiKey, endpoint, deploymentName);
            }
        }
    }

    /**
     * Creates a new builder for Azure OpenAI LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}

package com.skanga.conductor.provider;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiChatModel;

import java.io.IOException;

/**
 * Oracle Cloud Infrastructure (OCI) Generative AI LLM provider implementation.
 * <p>
 * This provider integrates with Oracle Cloud Infrastructure's Generative AI service,
 * which provides access to large language models hosted on Oracle's cloud platform.
 * OCI Generative AI offers enterprise-grade security, compliance, and performance
 * with models optimized for various use cases including text generation, summarization,
 * and conversation.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Enterprise-grade security and compliance on Oracle Cloud</li>
 * <li>Integration with OCI Identity and Access Management (IAM)</li>
 * <li>Support for custom fine-tuned models</li>
 * <li>Regional deployment options for data sovereignty</li>
 * <li>High-performance inference with dedicated compute</li>
 * <li>Automatic error handling and retry logic</li>
 * <li>Cost-effective pricing with flexible billing options</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying
 * OciGenAiChatModel implementation is thread-safe.
 * </p>
 * <p>
 * Usage Example:
 * </p>
 * <pre>
 * // Using OCI Generative AI with default config
 * OracleLLMProvider provider = new OracleLLMProvider(
 *     "ocid1.compartment.oc1..your-compartment-id",
 *     "cohere.command");
 * String response = provider.generate("Explain Oracle Cloud services");
 *
 * // Using environment-specific compartment
 * OracleLLMProvider provider = new OracleLLMProvider(
 *     System.getenv("OCI_COMPARTMENT_ID"),
 *     "meta.llama-2-70b-chat");
 * </pre>
 * <p>
 * <strong>Authentication:</strong> This provider uses OCI SDK default configuration
 * from ~/.oci/config file. Ensure OCI CLI is properly configured with valid
 * credentials and the required IAM policies are in place for Generative AI access.
 * </p>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 * @see LLMProvider
 */
public class OracleLLMProvider extends AbstractLLMProvider {

    private final OciGenAiChatModel model;
    private final String compartmentId;
    private final String modelName;
    private final AuthenticationDetailsProvider authProvider;

    /**
     * Creates a new Oracle Cloud Generative AI LLM provider with the specified configuration.
     * <p>
     * Initializes the provider with compartment ID and model name. Authentication
     * is handled automatically through the OCI SDK default configuration file
     * (~/.oci/config). Retry behavior is configured based on application
     * configuration settings.
     * </p>
     *
     * @param compartmentId the OCID of the OCI compartment containing the Generative AI resources
     * @param modelName the name of the model to use (e.g., "cohere.command", "meta.llama-2-70b-chat")
     * @throws IOException if the OCI configuration file cannot be read or is invalid
     */
    public OracleLLMProvider(String compartmentId, String modelName) throws IOException {
        super(standardizeProviderName("oracle"), standardizeModelName(modelName, "cohere.command"));
        this.compartmentId = compartmentId;
        this.modelName = modelName;
        // Create an authentication provider using the default configuration profile (~/.oci/config)
        this.authProvider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
        this.model = createOracleModel();
    }

    public OracleLLMProvider(String compartmentId, String modelName, AuthenticationDetailsProvider authProvider) throws IOException {
        super(standardizeProviderName("oracle"), standardizeModelName(modelName, "cohere.command"));
        this.compartmentId = compartmentId;
        this.modelName = modelName;
        this.authProvider = authProvider;
        this.model = createOracleModel();
    }

    /**
     * Creates a new Oracle Cloud Generative AI LLM provider with custom retry policy.
     * <p>
     * This constructor allows full control over the retry behavior,
     * useful for testing or when different retry strategies are needed
     * for different OCI regions or model configurations.
     * </p>
     *
     * @param compartmentId the OCID of the OCI compartment containing the Generative AI resources
     * @param modelName the name of the model to use (e.g., "cohere.command", "meta.llama-2-70b-chat")
     * @param retryPolicy the retry policy to use for LLM calls
     * @throws IOException if the OCI configuration file cannot be read or is invalid
     */
    public OracleLLMProvider(String compartmentId, String modelName, RetryPolicy retryPolicy) throws IOException {
        super(standardizeProviderName("oracle"), standardizeModelName(modelName, "cohere.command"), retryPolicy);
        this.compartmentId = compartmentId;
        this.modelName = modelName;
        // Create an authentication provider using the default configuration profile (~/.oci/config)
        this.authProvider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
        this.model = createOracleModel();
    }

    /**
     * Generates text using the Oracle Cloud Generative AI model.
     * <p>
     * This method sends the prompt to the specified OCI Generative AI model,
     * returning the generated response. The communication is handled through
     * Oracle Cloud APIs with automatic authentication using the configured
     * OCI credentials and IAM policies.
     * </p>
     *
     * @param prompt the text prompt to send to the OCI Generative AI model
     * @return the generated text response from the foundation model
     * @throws Exception if the OCI request fails due to authentication issues,
     *                   insufficient IAM permissions, model not available, service limits,
     *                   compartment not found, or other Oracle Cloud service-related problems
     */
    /**
     * Creates the Oracle OCI Generative AI chat model using the template method pattern.
     *
     * @return configured Oracle OCI Generative AI chat model
     */
    private OciGenAiChatModel createOracleModel() {
        return createModel(
            OciGenAiChatModel::builder,
            builder -> builder
                .compartmentId(compartmentId)
                .modelName(modelName)
                .authProvider(authProvider)
        );
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }

    /**
     * Builder for creating Oracle OCI Generative AI LLM providers with fluent configuration.
     */
    public static class Builder extends AbstractLLMProvider.Builder<OracleLLMProvider, Builder> {
        private String compartmentId;
        private String oracleModelName;
        private AuthenticationDetailsProvider authProvider;

        public Builder() {
            super("oracle");
        }

        /**
         * Sets the OCI compartment ID.
         *
         * @param compartmentId the compartment ID
         * @return this builder for method chaining
         */
        public Builder compartmentId(String compartmentId) {
            this.compartmentId = compartmentId;
            return this;
        }

        /**
         * Sets the Oracle model name.
         *
         * @param modelName the model name
         * @return this builder for method chaining
         */
        public Builder oracleModelName(String modelName) {
            this.oracleModelName = modelName;
            return this;
        }

        /**
         * Sets a custom authentication provider.
         *
         * @param authProvider the authentication provider
         * @return this builder for method chaining
         */
        public Builder authProvider(AuthenticationDetailsProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        @Override
        public OracleLLMProvider build() {
            if (compartmentId == null || oracleModelName == null) {
                throw new IllegalArgumentException("Compartment ID and model name are required");
            }

            try {
                if (retryPolicy != null) {
                    return new OracleLLMProvider(compartmentId, oracleModelName, retryPolicy);
                } else {
                    return new OracleLLMProvider(compartmentId, oracleModelName);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create Oracle LLM provider", e);
            }
        }
    }

    /**
     * Creates a new builder for Oracle LLM provider.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}

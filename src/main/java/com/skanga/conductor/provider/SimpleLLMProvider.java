package com.skanga.conductor.provider;

import java.util.UUID;

/**
 * Simple base class for LLM providers without built-in retry logic.
 * <p>
 * This class provides utility methods for provider and model name standardization
 * without forcing retry behavior on all implementations. Providers can extend this
 * class for basic functionality and optionally wrap themselves with
 * {@link RetryableLLMProvider} for retry behavior.
 * </p>
 * <p>
 * This approach follows the Single Responsibility Principle by separating
 * core provider functionality from retry logic, allowing providers to opt-in
 * to retry behavior via decoration rather than inheritance.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * // Create a simple provider without retry
 * public class MyLLMProvider extends SimpleLLMProvider {
 *     public MyLLMProvider(String apiKey) {
 *         super("my-provider", "default-model");
 *     }
 *
 *     @Override
 *     public String generate(String prompt) throws ConductorException.LLMProviderException {
 *         // Implement LLM call without retry logic
 *         return callMyLLM(prompt);
 *     }
 * }
 *
 * // Use without retry
 * LLMProvider provider = new MyLLMProvider(apiKey);
 *
 * // Or wrap with retry decorator
 * LLMProvider retryableProvider = new RetryableLLMProvider(provider);
 * }</pre>
 * <p>
 * Thread Safety: This class is thread-safe. Concrete implementations should
 * also ensure thread safety.
 * </p>
 *
 * @since 1.1.0
 * @see LLMProvider
 * @see RetryableLLMProvider
 * @see AbstractLLMProvider
 */
public abstract class SimpleLLMProvider implements LLMProvider {

    private final String providerName;
    private final String modelName;

    /**
     * Creates a new simple LLM provider.
     *
     * @param providerName the name of this provider (used for logging and metrics)
     * @param modelName the default model name for this provider
     */
    protected SimpleLLMProvider(String providerName, String modelName) {
        this.providerName = generateProviderName(providerName);
        this.modelName = standardizeModelName(modelName, "default");
    }

    /**
     * Returns the name of this provider.
     *
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Returns the model name for this provider.
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Generates a provider name, creating a unique one if the input is null/blank.
     *
     * @param name the raw provider name
     * @return the generated or standardized provider name
     */
    private static String generateProviderName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "llm-provider-" + UUID.randomUUID().toString();
        }
        return standardizeProviderName(name);
    }

    /**
     * Helper method for standardizing provider names.
     * <p>
     * Ensures consistent provider name formatting across all implementations.
     * Converts to lowercase, replaces spaces and special characters with hyphens.
     * </p>
     *
     * @param name the raw provider name
     * @return the standardized provider name
     */
    protected static String standardizeProviderName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be null or blank");
        }
        return name.toLowerCase()
                  .trim()
                  .replaceAll("[^a-z0-9]+", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }

    /**
     * Helper method for standardizing model names.
     * <p>
     * Ensures consistent model name formatting and provides default values.
     * </p>
     *
     * @param modelName the raw model name
     * @param defaultModel the default model to use if modelName is null/blank
     * @return the standardized model name
     */
    protected static String standardizeModelName(String modelName, String defaultModel) {
        if (modelName == null || modelName.isBlank()) {
            return defaultModel != null ? defaultModel : "default";
        }
        return modelName.toLowerCase()
                  .trim()
                  .replaceAll("[^a-z0-9]+", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}

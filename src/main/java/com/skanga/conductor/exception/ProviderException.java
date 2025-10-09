package com.skanga.conductor.exception;

/**
 * Exception for LLM provider-related errors.
 * <p>
 * This exception is thrown when an LLM provider (OpenAI, Anthropic, etc.)
 * encounters an error. It uses the simplified error category system to
 * classify errors and determine retryability.
 * </p>
 * <p>
 * Common scenarios:
 * </p>
 * <ul>
 * <li>AUTH_ERROR: Invalid API key, expired credentials</li>
 * <li>RATE_LIMITED: Rate limit or quota exceeded</li>
 * <li>TIMEOUT: Request timeout, connection timeout</li>
 * <li>SERVICE_ERROR: Service unavailable, server error</li>
 * <li>INVALID_INPUT: Invalid model name, malformed prompt</li>
 * </ul>
 *
 * @since 1.1.0
 * @see ErrorCategory
 * @see SimpleConductorException
 */
public class ProviderException extends SimpleConductorException {

    private final String providerName;
    private final String modelName;

    /**
     * Creates a new provider exception.
     *
     * @param category the error category
     * @param message the detail message
     * @param providerName the name of the provider (e.g., "openai", "anthropic")
     */
    public ProviderException(ErrorCategory category, String message, String providerName) {
        this(category, message, null, providerName, null);
    }

    /**
     * Creates a new provider exception with cause.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause
     * @param providerName the name of the provider
     */
    public ProviderException(ErrorCategory category, String message,
                            Throwable cause, String providerName) {
        this(category, message, cause, providerName, null);
    }

    /**
     * Creates a new provider exception with full context.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause (may be null)
     * @param providerName the name of the provider
     * @param modelName the name of the model (may be null)
     */
    public ProviderException(ErrorCategory category, String message,
                            Throwable cause, String providerName, String modelName) {
        super(category, message, cause, buildContext(providerName, modelName));
        this.providerName = providerName;
        this.modelName = modelName;
    }

    /**
     * Returns the provider name.
     *
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Returns the model name, if specified.
     *
     * @return the model name, or null
     */
    public String getModelName() {
        return modelName;
    }

    private static String buildContext(String providerName, String modelName) {
        if (modelName != null) {
            return "provider=" + providerName + ", model=" + modelName;
        }
        return "provider=" + providerName;
    }

    /**
     * Creates a provider exception from another exception with automatic categorization.
     *
     * @param message the detail message
     * @param cause the underlying exception
     * @param providerName the provider name
     * @return a new ProviderException
     */
    public static ProviderException from(String message, Exception cause, String providerName) {
        ErrorCategory category = ErrorCategory.fromException(cause);
        return new ProviderException(category, message, cause, providerName);
    }
}

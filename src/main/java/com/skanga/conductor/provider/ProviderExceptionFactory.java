package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException.LLMProviderException;
import com.skanga.conductor.exception.ErrorCodes;
import com.skanga.conductor.exception.ExceptionBuilder;
import com.skanga.conductor.exception.ExceptionContext;

import java.util.concurrent.TimeoutException;

/**
 * Standardized factory for creating provider exceptions with rich context information.
 * <p>
 * This factory provides a consistent way to create provider exceptions across all
 * LLM provider implementations, ensuring proper error categorization, context information,
 * and recovery hints. It encapsulates the complex logic of mapping various provider
 * errors to standardized exception types.
 * </p>
 * <p>
 * Key Features:
 * </p>
 * <ul>
 * <li>Standardized error code mapping for all providers</li>
 * <li>Rich context information including provider details</li>
 * <li>Automatic recovery hint generation</li>
 * <li>Type-safe exception creation methods</li>
 * <li>Support for provider-specific metadata</li>
 * </ul>
 *
 * @since 1.0.0
 * @see LLMProviderException
 * @see ExceptionBuilder
 * @see ErrorCodes
 */
public final class ProviderExceptionFactory {

    private ProviderExceptionFactory() {
        // Utility class - no instantiation
    }

    /**
     * Helper method to safely add attempt information to the builder
     */
    private static ExceptionBuilder addAttemptInfo(ExceptionBuilder builder, ProviderContext context) {
        if (context.getAttemptNumber() != null && context.getMaxAttempts() != null) {
            return builder.attempt(context.getAttemptNumber(), context.getMaxAttempts());
        }
        return builder;
    }

    /**
     * Helper method to safely add duration information to the builder
     */
    private static ExceptionBuilder addDuration(ExceptionBuilder builder, ProviderContext context) {
        if (context.getDuration() != null) {
            return builder.duration(context.getDuration());
        }
        return builder;
    }

    /**
     * Helper method to create a base exception builder with common context information
     */
    private static ExceptionBuilder createBaseBuilder(String message, String errorCode, ProviderContext context) {
        ExceptionBuilder builder = ExceptionBuilder.llmProvider(message)
                .errorCode(errorCode)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .correlationId(context.getCorrelationId());

        builder = addDuration(builder, context);
        builder = addAttemptInfo(builder, context);

        return builder;
    }

    /**
     * Provider-specific information for exception context
     */
    public static class ProviderContext {
        private final String providerName;
        private final String modelName;
        private final String operation;
        private final Long duration;
        private final Integer attemptNumber;
        private final Integer maxAttempts;
        private final String correlationId;

        private ProviderContext(Builder builder) {
            this.providerName = builder.providerName;
            this.modelName = builder.modelName;
            this.operation = builder.operation;
            this.duration = builder.duration;
            this.attemptNumber = builder.attemptNumber;
            this.maxAttempts = builder.maxAttempts;
            this.correlationId = builder.correlationId;
        }

        public static Builder builder(String providerName) {
            return new Builder(providerName);
        }

        public static class Builder {
            private final String providerName;
            private String modelName;
            private String operation;
            private Long duration;
            private Integer attemptNumber;
            private Integer maxAttempts;
            private String correlationId;

            private Builder(String providerName) {
                this.providerName = providerName;
            }

            public Builder model(String modelName) {
                this.modelName = modelName;
                return this;
            }

            public Builder operation(String operation) {
                this.operation = operation;
                return this;
            }

            public Builder duration(long duration) {
                this.duration = duration;
                return this;
            }

            public Builder attempt(int attemptNumber, int maxAttempts) {
                this.attemptNumber = attemptNumber;
                this.maxAttempts = maxAttempts;
                return this;
            }

            public Builder correlationId(String correlationId) {
                this.correlationId = correlationId;
                return this;
            }

            public ProviderContext build() {
                return new ProviderContext(this);
            }
        }

        // Getters
        public String getProviderName() { return providerName; }
        public String getModelName() { return modelName; }
        public String getOperation() { return operation; }
        public Long getDuration() { return duration; }
        public Integer getAttemptNumber() { return attemptNumber; }
        public Integer getMaxAttempts() { return maxAttempts; }
        public String getCorrelationId() { return correlationId; }
    }

    // Authentication Exceptions

    /**
     * Creates an authentication exception for invalid API keys.
     */
    public static LLMProviderException invalidApiKey(ProviderContext context, String keyInfo) {
        return (LLMProviderException) createBaseBuilder("Invalid API key", ErrorCodes.AUTH_FAILED, context)
                .metadata("key_info", keyInfo)
                .checkCredentials()
                .build();
    }

    /**
     * Creates an authentication exception for expired API keys.
     */
    public static LLMProviderException expiredApiKey(ProviderContext context, String expirationInfo) {
        return (LLMProviderException) createBaseBuilder("API key has expired", ErrorCodes.AUTH_FAILED, context)
                .metadata("expiration_info", expirationInfo)
                .checkCredentials()
                .build();
    }

    /**
     * Creates an authentication exception for missing API keys.
     */
    public static LLMProviderException missingApiKey(ProviderContext context) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("API key is missing")
                .errorCode(ErrorCodes.AUTH_FAILED)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("required_env_var", context.getProviderName().toUpperCase() + "_API_KEY")
                .checkCredentials()
                .build();
    }

    /**
     * Creates an authentication exception for insufficient permissions.
     */
    public static LLMProviderException insufficientPermissions(ProviderContext context, String resource) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("Insufficient permissions")
                .errorCode(ErrorCodes.AUTH_FAILED)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("required_resource", resource)
                .checkCredentials()
                .build();
    }

    // Rate Limit Exceptions

    /**
     * Creates a rate limit exception for exceeded request limits.
     */
    public static LLMProviderException rateLimitExceeded(ProviderContext context, long resetTimeSeconds) {
        return (LLMProviderException) createBaseBuilder("Rate limit exceeded", ErrorCodes.RATE_LIMIT_EXCEEDED, context)
                .rateLimitReset(resetTimeSeconds)
                .metadata("retry_after_seconds", resetTimeSeconds)
                .retryWithBackoff()
                .recoveryDetails("Wait " + resetTimeSeconds + " seconds before retrying")
                .build();
    }

    /**
     * Creates a quota exceeded exception.
     */
    public static LLMProviderException quotaExceeded(ProviderContext context, String quotaType) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("Quota exceeded")
                .errorCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("quota_type", quotaType)
                .userActionRequired()
                .recoveryDetails("Upgrade plan or wait for quota reset")
                .build();
    }

    // Timeout Exceptions

    /**
     * Creates a request timeout exception.
     */
    public static LLMProviderException requestTimeout(ProviderContext context, long timeoutMs) {
        return (LLMProviderException) createBaseBuilder("Request timeout", ErrorCodes.TIMEOUT, context)
                .metadata("timeout_ms", timeoutMs)
                .retryWithBackoff()
                .recoveryDetails("Consider increasing timeout or reducing prompt size")
                .build();
    }

    /**
     * Creates a connection timeout exception.
     */
    public static LLMProviderException connectionTimeout(ProviderContext context, String endpoint) {
        ExceptionBuilder builder = ExceptionBuilder.llmProvider("Connection timeout")
                .errorCode(ErrorCodes.TIMEOUT)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .correlationId(context.getCorrelationId())
                .metadata("endpoint", endpoint)
                .retryWithBackoff();

        builder = addDuration(builder, context);
        builder = addAttemptInfo(builder, context);

        return (LLMProviderException) builder.build();
    }

    // Network Exceptions

    /**
     * Creates a network connection failed exception.
     */
    public static LLMProviderException networkConnectionFailed(ProviderContext context, Throwable cause) {
        ExceptionBuilder builder = ExceptionBuilder.llmProvider("Network connection failed")
                .errorCode(ErrorCodes.NETWORK_ERROR)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .correlationId(context.getCorrelationId())
                .cause(cause)
                .retryWithBackoff();

        builder = addDuration(builder, context);
        builder = addAttemptInfo(builder, context);

        return (LLMProviderException) builder.build();
    }

    /**
     * Creates a DNS resolution exception.
     */
    public static LLMProviderException dnsResolutionFailed(ProviderContext context, String hostname) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("DNS resolution failed")
                .errorCode(ErrorCodes.NETWORK_ERROR)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("hostname", hostname)
                .retryWithBackoff()
                .recoveryDetails("Check network connectivity and DNS configuration")
                .build();
    }

    // Service Exceptions

    /**
     * Creates a service unavailable exception.
     */
    public static LLMProviderException serviceUnavailable(ProviderContext context, int httpStatus) {
        ExceptionBuilder builder = ExceptionBuilder.llmProvider("Service unavailable")
                .errorCode(ErrorCodes.SERVICE_UNAVAILABLE)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .correlationId(context.getCorrelationId())
                .httpStatus(httpStatus)
                .retryWithBackoff();

        builder = addDuration(builder, context);
        builder = addAttemptInfo(builder, context);

        return (LLMProviderException) builder.build();
    }

    /**
     * Creates a service maintenance exception.
     */
    public static LLMProviderException serviceMaintenance(ProviderContext context, String maintenanceWindow) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("Service under maintenance")
                .errorCode(ErrorCodes.SERVICE_UNAVAILABLE)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("maintenance_window", maintenanceWindow)
                .retryWithBackoff()
                .recoveryDetails("Service is under maintenance. Try again later.")
                .build();
    }

    // Request/Response Exceptions

    /**
     * Creates an invalid request format exception.
     */
    public static LLMProviderException invalidRequestFormat(ProviderContext context, String validationError) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("Invalid request format")
                .errorCode(ErrorCodes.INVALID_FORMAT)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("validation_error", validationError)
                .fixConfiguration()
                .recoveryDetails("Fix request format: " + validationError)
                .build();
    }

    /**
     * Creates a request too large exception.
     */
    public static LLMProviderException requestTooLarge(ProviderContext context, int actualSize, int maxSize) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("Request too large")
                .errorCode(ErrorCodes.SIZE_EXCEEDED)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("actual_size", actualSize)
                .metadata("max_size", maxSize)
                .fixConfiguration()
                .recoveryDetails("Reduce prompt size or split into smaller requests")
                .build();
    }

    /**
     * Creates an invalid model exception.
     */
    public static LLMProviderException invalidModel(ProviderContext context, String availableModels) {
        return (LLMProviderException) ExceptionBuilder.llmProvider("Invalid model specified")
                .errorCode(ErrorCodes.NOT_FOUND)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .metadata("available_models", availableModels)
                .fixConfiguration()
                .recoveryDetails("Use one of the available models: " + availableModels)
                .build();
    }

    // Helper method to determine error type from exception
    public static LLMProviderException fromException(ProviderContext context, Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        String className = exception.getClass().getSimpleName().toLowerCase();

        // Timeout exceptions
        if (exception instanceof TimeoutException ||
            message.contains("timeout") ||
            className.contains("timeout")) {
            ExceptionBuilder builder = createBaseBuilder("Request timeout", ErrorCodes.TIMEOUT, context)
                    .metadata("timeout_ms", context.getDuration() != null ? context.getDuration() : 30000)
                    .cause(exception)
                    .retryWithBackoff()
                    .recoveryDetails("Consider increasing timeout or reducing prompt size");
            return (LLMProviderException) builder.build();
        }

        // Network exceptions
        if (message.contains("connection") ||
            message.contains("network") ||
            className.contains("connect")) {
            return networkConnectionFailed(context, exception);
        }

        // Rate limit exceptions
        if (message.contains("rate limit") ||
            message.contains("too many requests") ||
            message.contains("429")) {
            long resetTimeSeconds = 60; // Default 60 seconds
            ExceptionBuilder builder = createBaseBuilder("Rate limit exceeded", ErrorCodes.RATE_LIMIT_EXCEEDED, context)
                    .rateLimitReset(resetTimeSeconds)
                    .metadata("retry_after_seconds", resetTimeSeconds)
                    .cause(exception)
                    .retryWithBackoff()
                    .recoveryDetails("Wait " + resetTimeSeconds + " seconds before retrying");
            return (LLMProviderException) builder.build();
        }

        // Authentication exceptions
        if (message.contains("authentication") ||
            message.contains("unauthorized") ||
            message.contains("401")) {
            ExceptionBuilder builder = createBaseBuilder("Invalid API key", ErrorCodes.AUTH_FAILED, context)
                    .metadata("key_info", "Check API key validity")
                    .cause(exception)
                    .checkCredentials();
            return (LLMProviderException) builder.build();
        }

        // Service unavailable
        if (message.contains("service unavailable") ||
            message.contains("503") ||
            message.contains("502") ||
            message.contains("504")) {
            int httpStatus = 503;
            ExceptionBuilder builder = ExceptionBuilder.llmProvider("Service unavailable")
                    .errorCode(ErrorCodes.SERVICE_UNAVAILABLE)
                    .operation(context.getOperation())
                    .provider(context.getProviderName(), context.getModelName())
                    .correlationId(context.getCorrelationId())
                    .httpStatus(httpStatus)
                    .cause(exception)
                    .retryWithBackoff();
            builder = addDuration(builder, context);
            builder = addAttemptInfo(builder, context);
            return (LLMProviderException) builder.build();
        }

        // Generic provider exception with enhanced context
        ExceptionBuilder builder = ExceptionBuilder.llmProvider(exception.getMessage())
                .errorCode(ErrorCodes.SERVICE_UNAVAILABLE)
                .operation(context.getOperation())
                .provider(context.getProviderName(), context.getModelName())
                .correlationId(context.getCorrelationId())
                .cause(exception)
                .retryWithBackoff();

        builder = addDuration(builder, context);
        builder = addAttemptInfo(builder, context);

        return (LLMProviderException) builder.build();
    }
}
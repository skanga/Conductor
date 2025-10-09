package com.skanga.conductor.exception;

/**
 * Simplified error categories for the Conductor framework.
 * <p>
 * This enum replaces the complex error code system with a simple categorization
 * that focuses on what matters for error handling: whether the error is retryable
 * and what general category it falls into.
 * </p>
 * <p>
 * Each category includes guidance on whether errors are typically retryable
 * and what action should be taken.
 * </p>
 *
 * @since 1.1.0
 */
public enum ErrorCategory {

    /**
     * Authentication or authorization failures.
     * <p>
     * Examples: Invalid API key, expired credentials, insufficient permissions
     * </p>
     * <p>
     * Retryable: NO - requires fixing credentials
     * </p>
     */
    AUTH_ERROR(false, "Authentication/Authorization failure"),

    /**
     * Rate limiting or quota exceeded.
     * <p>
     * Examples: Rate limit exceeded, quota exhausted, too many requests
     * </p>
     * <p>
     * Retryable: YES - with exponential backoff
     * </p>
     */
    RATE_LIMITED(true, "Rate limit or quota exceeded"),

    /**
     * Timeout during operation.
     * <p>
     * Examples: Connection timeout, read timeout, request timeout
     * </p>
     * <p>
     * Retryable: YES - temporary network issue
     * </p>
     */
    TIMEOUT(true, "Operation timed out"),

    /**
     * Service unavailable or temporary failure.
     * <p>
     * Examples: Service down, maintenance mode, server overloaded
     * </p>
     * <p>
     * Retryable: YES - temporary service issue
     * </p>
     */
    SERVICE_ERROR(true, "Service temporarily unavailable"),

    /**
     * Invalid input or request.
     * <p>
     * Examples: Invalid parameters, malformed request, unsupported operation
     * </p>
     * <p>
     * Retryable: NO - requires fixing the input
     * </p>
     */
    INVALID_INPUT(false, "Invalid input or request"),

    /**
     * Configuration error.
     * <p>
     * Examples: Missing configuration, invalid settings, misconfigured component
     * </p>
     * <p>
     * Retryable: NO - requires fixing configuration
     * </p>
     */
    CONFIGURATION_ERROR(false, "Configuration error"),

    /**
     * Resource not found.
     * <p>
     * Examples: Model not found, tool not found, file not found
     * </p>
     * <p>
     * Retryable: NO - resource doesn't exist
     * </p>
     */
    NOT_FOUND(false, "Resource not found"),

    /**
     * Internal error or unexpected failure.
     * <p>
     * Examples: Unexpected exception, system error, programming error
     * </p>
     * <p>
     * Retryable: MAYBE - depends on specific error
     * </p>
     */
    INTERNAL_ERROR(false, "Internal error");

    private final boolean defaultRetryable;
    private final String description;

    ErrorCategory(boolean defaultRetryable, String description) {
        this.defaultRetryable = defaultRetryable;
        this.description = description;
    }

    /**
     * Returns whether errors in this category are typically retryable.
     *
     * @return true if errors are typically retryable
     */
    public boolean isDefaultRetryable() {
        return defaultRetryable;
    }

    /**
     * Returns a human-readable description of this error category.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Determines error category from an exception message.
     * <p>
     * This is a simple heuristic-based classifier that looks for keywords
     * in exception messages to categorize them.
     * </p>
     *
     * @param message the exception message
     * @return the most likely error category
     */
    public static ErrorCategory fromMessage(String message) {
        if (message == null) {
            return INTERNAL_ERROR;
        }

        String lower = message.toLowerCase();

        // Check for authentication/authorization
        if (lower.contains("auth") || lower.contains("credential") ||
            lower.contains("permission") || lower.contains("unauthorized") ||
            lower.contains("forbidden") || lower.contains("api key")) {
            return AUTH_ERROR;
        }

        // Check for rate limiting
        if (lower.contains("rate limit") || lower.contains("quota") ||
            lower.contains("too many requests") || lower.contains("throttl")) {
            return RATE_LIMITED;
        }

        // Check for timeouts
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return TIMEOUT;
        }

        // Check for service errors
        if (lower.contains("unavailable") || lower.contains("service error") ||
            lower.contains("server error") || lower.contains("503") ||
            lower.contains("502") || lower.contains("504") ||
            lower.contains("maintenance") || lower.contains("overload")) {
            return SERVICE_ERROR;
        }

        // Check for invalid input
        if (lower.contains("invalid") || lower.contains("malformed") ||
            lower.contains("bad request") || lower.contains("400")) {
            return INVALID_INPUT;
        }

        // Check for not found
        if (lower.contains("not found") || lower.contains("404") ||
            lower.contains("does not exist")) {
            return NOT_FOUND;
        }

        // Check for configuration
        if (lower.contains("config") || lower.contains("setting") ||
            lower.contains("property")) {
            return CONFIGURATION_ERROR;
        }

        // Default to internal error
        return INTERNAL_ERROR;
    }

    /**
     * Determines error category from an exception type and message.
     *
     * @param exception the exception to categorize
     * @return the most likely error category
     */
    public static ErrorCategory fromException(Exception exception) {
        // Check exception type first
        String exceptionType = exception.getClass().getSimpleName().toLowerCase();

        if (exceptionType.contains("timeout")) {
            return TIMEOUT;
        }
        if (exceptionType.contains("auth")) {
            return AUTH_ERROR;
        }
        if (exceptionType.contains("notfound") || exceptionType.contains("missing")) {
            return NOT_FOUND;
        }
        if (exceptionType.contains("config")) {
            return CONFIGURATION_ERROR;
        }

        // Fall back to message-based classification
        return fromMessage(exception.getMessage());
    }
}

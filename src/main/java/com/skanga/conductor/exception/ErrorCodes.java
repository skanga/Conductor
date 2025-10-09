package com.skanga.conductor.exception;

/**
 * Streamlined error codes for the Conductor framework.
 * <p>
 * This class provides a minimal set of ~20 core error codes organized by
 * {@link ErrorCategory} for consistent error classification and handling.
 * The design focuses on actionability rather than granularity.
 * </p>
 * <p>
 * Each error code maps to a specific {@link ErrorCategory} which determines
 * whether the error is retryable and what recovery action should be taken.
 * </p>
 * <p>
 * Error Code Categories:
 * </p>
 * <ul>
 * <li>AUTH_* - Authentication/authorization failures (not retryable)</li>
 * <li>RATE_LIMIT_* - Rate limiting (retryable with backoff)</li>
 * <li>TIMEOUT_* - Operation timeouts (retryable)</li>
 * <li>SERVICE_* - Service availability issues (retryable)</li>
 * <li>INVALID_* - Input validation failures (not retryable)</li>
 * <li>CONFIG_* - Configuration errors (not retryable)</li>
 * <li>NOT_FOUND_* - Resource not found (not retryable)</li>
 * <li>INTERNAL_* - Internal/system errors (sometimes retryable)</li>
 * </ul>
 *
 * @since 2.0.0
 * @see ErrorCategory
 */
public final class ErrorCodes {

    // ========== Authentication/Authorization Errors (AUTH_ERROR) ==========

    /**
     * Authentication failed - invalid credentials, expired token, or missing API key.
     * <p>Category: AUTH_ERROR | Retryable: NO</p>
     */
    public static final String AUTH_FAILED = "AUTH_FAILED";

    // ========== Rate Limiting Errors (RATE_LIMITED) ==========

    /**
     * Rate limit exceeded - too many requests in a time window.
     * <p>Category: RATE_LIMITED | Retryable: YES (with backoff)</p>
     */
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    // ========== Timeout Errors (TIMEOUT) ==========

    /**
     * Operation timed out - request, connection, or read timeout.
     * <p>Category: TIMEOUT | Retryable: YES</p>
     */
    public static final String TIMEOUT = "TIMEOUT";

    // ========== Service Availability Errors (SERVICE_ERROR) ==========

    /**
     * Service temporarily unavailable - maintenance, overload, or transient failure.
     * <p>Category: SERVICE_ERROR | Retryable: YES</p>
     */
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

    // ========== Input Validation Errors (INVALID_INPUT) ==========

    /**
     * Invalid input - malformed request, type mismatch, or validation failure.
     * <p>Category: INVALID_INPUT | Retryable: NO</p>
     */
    public static final String INVALID_INPUT = "INVALID_INPUT";

    /**
     * Invalid request format - JSON/YAML parsing error or structural issue.
     * <p>Category: INVALID_INPUT | Retryable: NO</p>
     */
    public static final String INVALID_FORMAT = "INVALID_FORMAT";

    /**
     * Request size exceeded - payload too large or context length exceeded.
     * <p>Category: INVALID_INPUT | Retryable: NO</p>
     */
    public static final String SIZE_EXCEEDED = "SIZE_EXCEEDED";

    /**
     * Content policy violation - blocked by safety filters or content moderation.
     * <p>Category: INVALID_INPUT | Retryable: NO</p>
     */
    public static final String CONTENT_BLOCKED = "CONTENT_BLOCKED";

    // ========== Configuration Errors (CONFIGURATION_ERROR) ==========

    /**
     * Configuration error - missing, invalid, or out-of-range configuration value.
     * <p>Category: CONFIGURATION_ERROR | Retryable: NO</p>
     */
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";

    // ========== Resource Not Found Errors (NOT_FOUND) ==========

    /**
     * Resource not found - model, tool, file, or endpoint doesn't exist.
     * <p>Category: NOT_FOUND | Retryable: NO</p>
     */
    public static final String NOT_FOUND = "NOT_FOUND";

    // ========== Internal/System Errors (INTERNAL_ERROR) ==========

    /**
     * Execution failed - tool execution, workflow stage, or operation failure.
     * <p>Category: INTERNAL_ERROR | Retryable: MAYBE</p>
     */
    public static final String EXECUTION_FAILED = "EXECUTION_FAILED";

    /**
     * Database operation failed - connection, query, or transaction error.
     * <p>Category: INTERNAL_ERROR | Retryable: MAYBE</p>
     */
    public static final String DATABASE_ERROR = "DATABASE_ERROR";

    /**
     * Resource exhausted - memory, storage, or connection pool exhausted.
     * <p>Category: INTERNAL_ERROR | Retryable: MAYBE</p>
     */
    public static final String RESOURCE_EXHAUSTED = "RESOURCE_EXHAUSTED";

    /**
     * Network error - connection failed, DNS resolution, or SSL/TLS error.
     * <p>Category: INTERNAL_ERROR | Retryable: YES</p>
     */
    public static final String NETWORK_ERROR = "NETWORK_ERROR";

    /**
     * Internal error - unexpected exception or system error.
     * <p>Category: INTERNAL_ERROR | Retryable: NO</p>
     */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
        // Utility class - no instantiation
    }

    /**
     * Maps an error code to its corresponding {@link ErrorCategory}.
     * <p>
     * This method provides a simple, maintainable mapping from error codes
     * to categories. The categorization determines retry behavior and recovery hints.
     * </p>
     *
     * @param errorCode the error code
     * @return the corresponding error category
     */
    public static ErrorCategory toCategory(String errorCode) {
        if (errorCode == null) {
            return ErrorCategory.INTERNAL_ERROR;
        }

        switch (errorCode) {
            case AUTH_FAILED:
                return ErrorCategory.AUTH_ERROR;

            case RATE_LIMIT_EXCEEDED:
                return ErrorCategory.RATE_LIMITED;

            case TIMEOUT:
                return ErrorCategory.TIMEOUT;

            case SERVICE_UNAVAILABLE:
                return ErrorCategory.SERVICE_ERROR;

            case INVALID_INPUT:
            case INVALID_FORMAT:
            case SIZE_EXCEEDED:
            case CONTENT_BLOCKED:
                return ErrorCategory.INVALID_INPUT;

            case CONFIGURATION_ERROR:
                return ErrorCategory.CONFIGURATION_ERROR;

            case NOT_FOUND:
                return ErrorCategory.NOT_FOUND;

            case EXECUTION_FAILED:
            case DATABASE_ERROR:
            case RESOURCE_EXHAUSTED:
            case NETWORK_ERROR:
            case INTERNAL_ERROR:
            default:
                return ErrorCategory.INTERNAL_ERROR;
        }
    }

    /**
     * Maps an error code to {@link ExceptionContext.ErrorCategory} for backwards compatibility.
     * <p>
     * This bridges the old ExceptionContext.ErrorCategory enum with the new simplified
     * ErrorCategory enum.
     * </p>
     *
     * @param errorCode the error code
     * @return the corresponding ExceptionContext.ErrorCategory
     */
    public static ExceptionContext.ErrorCategory toExceptionContextCategory(String errorCode) {
        if (errorCode == null) {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        }

        switch (errorCode) {
            case AUTH_FAILED:
                return ExceptionContext.ErrorCategory.AUTHENTICATION;

            case RATE_LIMIT_EXCEEDED:
                return ExceptionContext.ErrorCategory.RATE_LIMIT;

            case TIMEOUT:
                return ExceptionContext.ErrorCategory.TIMEOUT;

            case SERVICE_UNAVAILABLE:
            case NETWORK_ERROR:
                return ExceptionContext.ErrorCategory.NETWORK;

            case INVALID_INPUT:
            case INVALID_FORMAT:
            case SIZE_EXCEEDED:
            case CONTENT_BLOCKED:
                return ExceptionContext.ErrorCategory.VALIDATION;

            case CONFIGURATION_ERROR:
                return ExceptionContext.ErrorCategory.CONFIGURATION;

            case NOT_FOUND:
                return ExceptionContext.ErrorCategory.RESOURCE_UNAVAILABLE;

            case EXECUTION_FAILED:
            case DATABASE_ERROR:
            case RESOURCE_EXHAUSTED:
            case INTERNAL_ERROR:
            default:
                return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        }
    }

    /**
     * Determines if an error code represents a retryable error.
     * <p>
     * This is a convenience method that delegates to the error category's
     * default retryability.
     * </p>
     *
     * @param errorCode the error code
     * @return true if the error is typically retryable
     */
    public static boolean isRetryable(String errorCode) {
        return toCategory(errorCode).isDefaultRetryable();
    }

    /**
     * Gets a human-readable description of an error code.
     *
     * @param errorCode the error code
     * @return a description of what the error means
     */
    public static String getDescription(String errorCode) {
        if (errorCode == null) {
            return "Unknown error";
        }

        switch (errorCode) {
            case AUTH_FAILED:
                return "Authentication or authorization failed";
            case RATE_LIMIT_EXCEEDED:
                return "Rate limit exceeded - too many requests";
            case TIMEOUT:
                return "Operation timed out";
            case SERVICE_UNAVAILABLE:
                return "Service temporarily unavailable";
            case INVALID_INPUT:
                return "Invalid input provided";
            case INVALID_FORMAT:
                return "Invalid data format";
            case SIZE_EXCEEDED:
                return "Request size limit exceeded";
            case CONTENT_BLOCKED:
                return "Content blocked by policy";
            case CONFIGURATION_ERROR:
                return "Configuration error";
            case NOT_FOUND:
                return "Resource not found";
            case EXECUTION_FAILED:
                return "Execution failed";
            case DATABASE_ERROR:
                return "Database operation failed";
            case RESOURCE_EXHAUSTED:
                return "Resource exhausted";
            case NETWORK_ERROR:
                return "Network error";
            case INTERNAL_ERROR:
            default:
                return "Internal error";
        }
    }

}
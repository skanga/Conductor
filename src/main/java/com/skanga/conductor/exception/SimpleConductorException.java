package com.skanga.conductor.exception;

/**
 * Simplified base exception for the Conductor framework.
 * <p>
 * This exception replaces the complex exception builder system with a simple,
 * straightforward approach using error categories and basic context information.
 * </p>
 * <p>
 * Key simplifications:
 * </p>
 * <ul>
 * <li>Uses {@link ErrorCategory} enum instead of 500+ error codes</li>
 * <li>Simple constructors instead of builder pattern</li>
 * <li>Minimal context - only what's actually needed</li>
 * <li>Clear guidance on retryability</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * // Simple usage
 * throw new SimpleConductorException(
 *     ErrorCategory.RATE_LIMITED,
 *     "Rate limit exceeded: 100 requests/minute"
 * );
 *
 * // With cause
 * throw new SimpleConductorException(
 *     ErrorCategory.SERVICE_ERROR,
 *     "LLM service unavailable",
 *     originalException
 * );
 *
 * // With additional context
 * throw new SimpleConductorException(
 *     ErrorCategory.TIMEOUT,
 *     "Request timed out after 30 seconds",
 *     null,
 *     "operation=generate_text, provider=openai, model=gpt-4"
 * );
 * }</pre>
 *
 * @since 1.1.0
 * @see ErrorCategory
 */
public class SimpleConductorException extends Exception {

    private final ErrorCategory category;
    private final String context;
    private final boolean retryable;

    /**
     * Creates a new exception with the specified category and message.
     *
     * @param category the error category
     * @param message the detail message
     */
    public SimpleConductorException(ErrorCategory category, String message) {
        this(category, message, null, null);
    }

    /**
     * Creates a new exception with the specified category, message, and cause.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause
     */
    public SimpleConductorException(ErrorCategory category, String message, Throwable cause) {
        this(category, message, cause, null);
    }

    /**
     * Creates a new exception with full context information.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause (may be null)
     * @param context additional context string (may be null)
     */
    public SimpleConductorException(ErrorCategory category, String message,
                                   Throwable cause, String context) {
        super(formatMessage(category, message, context), cause);
        this.category = category;
        this.context = context;
        this.retryable = category.isDefaultRetryable();
    }

    /**
     * Creates a new exception with custom retryability.
     * <p>
     * Use this constructor when you need to override the default retryability
     * for a specific error category.
     * </p>
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause (may be null)
     * @param context additional context string (may be null)
     * @param retryable whether this specific error should be retried
     */
    public SimpleConductorException(ErrorCategory category, String message,
                                   Throwable cause, String context, boolean retryable) {
        super(formatMessage(category, message, context), cause);
        this.category = category;
        this.context = context;
        this.retryable = retryable;
    }

    /**
     * Returns the error category for this exception.
     *
     * @return the error category
     */
    public ErrorCategory getCategory() {
        return category;
    }

    /**
     * Returns the additional context string, if any.
     *
     * @return the context string, or null if not provided
     */
    public String getContext() {
        return context;
    }

    /**
     * Returns whether this error should be retried.
     *
     * @return true if the error is retryable
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Formats the exception message with category and context.
     */
    private static String formatMessage(ErrorCategory category, String message, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(category.name()).append("] ");
        sb.append(message);
        if (context != null && !context.isBlank()) {
            sb.append(" (").append(context).append(")");
        }
        return sb.toString();
    }

    /**
     * Creates an exception from another exception with automatic categorization.
     *
     * @param message the detail message
     * @param cause the underlying exception
     * @return a new SimpleConductorException with auto-detected category
     */
    public static SimpleConductorException from(String message, Exception cause) {
        ErrorCategory category = ErrorCategory.fromException(cause);
        return new SimpleConductorException(category, message, cause);
    }

    /**
     * Creates an exception from another exception, preserving its message.
     *
     * @param cause the underlying exception
     * @return a new SimpleConductorException with auto-detected category
     */
    public static SimpleConductorException from(Exception cause) {
        ErrorCategory category = ErrorCategory.fromException(cause);
        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        return new SimpleConductorException(category, message, cause);
    }
}

package com.skanga.conductor.exception;

import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.LLMPlanMaker;
import com.skanga.conductor.orchestration.PlannerOrchestrator;
import com.skanga.conductor.tools.Tool;

/**
 * Base exception class for the Conductor framework.
 * <p>
 * This exception serves as the root of the exception hierarchy for all
 * Conductor-specific errors. It provides a consistent way to handle
 * framework-level exceptions and includes specialized subclasses for
 * different types of failures.
 * </p>
 * <p>
 * Following the framework's exception strategy, ConductorException is a checked
 * exception used for recoverable business logic failures that calling code should
 * handle with recovery strategies such as retries, fallbacks, or alternative approaches.
 * </p>
 * <p>
 * Enhanced Context Support:
 * </p>
 * <ul>
 * <li>Structured error codes and categories via {@link ExceptionContext}</li>
 * <li>Recovery hints and operational metadata</li>
 * <li>Timing information and attempt tracking</li>
 * <li>Correlation IDs for distributed tracing</li>
 * </ul>
 * <p>
 * Subclasses include:
 * </p>
 * <ul>
 * <li>{@link LLMProviderException} - LLM service related errors (retryable)</li>
 * <li>{@link ToolExecutionException} - Tool execution failures (can fallback)</li>
 * <li>{@link PlannerException} - Planning and orchestration errors (can retry)</li>
 * <li>{@link com.skanga.conductor.workflow.approval.ApprovalException} - Human approval failures (can re-prompt)</li>
 * </ul>
 * <p>
 * For programming errors and system failures, use {@link ConductorRuntimeException} subclasses instead.
 * </p>
 *
 * @since 1.0.0
 * @see LLMProviderException
 * @see ToolExecutionException
 * @see PlannerException
 * @see com.skanga.conductor.workflow.approval.ApprovalException
 * @see ConductorRuntimeException
 * @see ExceptionStrategy
 * @see ExceptionContext
 */
public class ConductorException extends Exception {

    private final ExceptionContext context;
    /**
     * Constructs a new ConductorException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ConductorException(String message) {
        super(message);
        this.context = null;
    }

    /**
     * Constructs a new ConductorException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public ConductorException(String message, Throwable cause) {
        super(message, cause);
        this.context = null;
    }

    /**
     * Constructs a new ConductorException with enhanced context information.
     *
     * @param message the detail message explaining the cause of the exception
     * @param context the enhanced context information
     */
    public ConductorException(String message, ExceptionContext context) {
        super(enhanceMessage(message, context));
        this.context = context;
    }

    /**
     * Constructs a new ConductorException with enhanced context information and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     * @param context the enhanced context information
     */
    public ConductorException(String message, Throwable cause, ExceptionContext context) {
        super(enhanceMessage(message, context), cause);
        this.context = context;
    }

    /**
     * Gets the enhanced context information associated with this exception.
     *
     * @return the exception context, or null if not available
     */
    public ExceptionContext getContext() {
        return context;
    }

    /**
     * Checks if this exception has enhanced context information.
     *
     * @return true if context is available
     */
    public boolean hasContext() {
        return context != null;
    }

    /**
     * Gets the error code from the context, if available.
     *
     * @return the error code, or null if not available
     */
    public String getErrorCode() {
        return context != null ? context.getErrorCode() : null;
    }

    /**
     * Gets the error category from the context, if available.
     *
     * @return the error category, or null if not available
     */
    public ExceptionContext.ErrorCategory getErrorCategory() {
        return context != null ? context.getCategory() : null;
    }

    /**
     * Checks if this exception is retryable based on context information.
     *
     * @return true if the exception suggests retry is appropriate
     */
    public boolean isRetryable() {
        return context != null && context.isRetryable();
    }

    /**
     * Checks if this exception suggests using a fallback mechanism.
     *
     * @return true if fallback is suggested
     */
    public boolean suggestsFallback() {
        return context != null && context.suggestsFallback();
    }

    /**
     * Gets a detailed error summary including context information.
     *
     * @return formatted error summary
     */
    public String getDetailedSummary() {
        if (context != null) {
            return context.getSummary() + ": " + getMessage();
        }
        return getMessage();
    }

    /**
     * Enhances the exception message with context information.
     *
     * @param message the base message
     * @param context the exception context
     * @return enhanced message
     */
    private static String enhanceMessage(String message, ExceptionContext context) {
        if (context == null) {
            return message;
        }
        StringBuilder enhanced = new StringBuilder();
        if (context.getErrorCode() != null) {
            enhanced.append("[").append(context.getErrorCode()).append("] ");
        }
        enhanced.append(message);
        if (context.getOperation() != null) {
            enhanced.append(" (operation: ").append(context.getOperation()).append(")");
        }
        if (context.getAttemptNumber() != null && context.getMaxAttempts() != null) {
            enhanced.append(" [attempt ").append(context.getAttemptNumber())
                    .append("/").append(context.getMaxAttempts()).append("]");
        }
        return enhanced.toString();
    }

    /**
     * Unchecked exception used by {@link MemoryStore} to wrap SQL related errors.
     * <p>
     * It extends {@link ConductorRuntimeException} following the framework's exception
     * strategy. Infrastructure failures like database connection issues represent
     * system errors that calling code typically cannot recover from meaningfully.
     * The original {@link java.sql.SQLException} is retained as the cause for
     * debugging and logging purposes.
     * </p>
     *
     * @since 1.0.0
     * @see MemoryStore
     * @see ConductorRuntimeException
     */
    public static class MemoryStoreException extends ConductorRuntimeException {
        /**
         * Constructs a new MemoryStoreException with the specified detail message.
         *
         * @param message the detail message explaining the memory store error
         */
        public MemoryStoreException(String message) {
            super(message);
        }

        /**
         * Constructs a new MemoryStoreException with the specified detail message and cause.
         *
         * @param message the detail message explaining the memory store error
         * @param cause the underlying cause, typically a {@link java.sql.SQLException}
         */
        public MemoryStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when LLM provider operations fail.
     * <p>
     * This exception covers various LLM-related failures including network issues,
     * authentication problems, rate limiting, timeouts, and service errors.
     * Specialized subclasses provide more specific error handling for common
     * LLM provider issues.
     * </p>
     * <p>
     * Enhanced with provider-specific context including:
     * </p>
     * <ul>
     * <li>Provider name and model information</li>
     * <li>Request/response metadata</li>
     * <li>Rate limit and quota details</li>
     * <li>Retry recommendations</li>
     * </ul>
     *
     * @since 1.0.0
     * @see LLMAuthenticationException
     * @see LLMRateLimitException
     * @see LLMTimeoutException
     */
    public static class LLMProviderException extends ConductorException {
        /**
         * Constructs a new LLMProviderException with the specified detail message.
         *
         * @param message the detail message explaining the LLM provider error
         */
        public LLMProviderException(String message) {
            super(message);
        }

        /**
         * Constructs a new LLMProviderException with the specified detail message and cause.
         *
         * @param message the detail message explaining the LLM provider error
         * @param cause the underlying cause of the LLM provider failure
         */
        public LLMProviderException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new LLMProviderException with enhanced context.
         *
         * @param message the detail message explaining the LLM provider error
         * @param context the enhanced context information
         */
        public LLMProviderException(String message, ExceptionContext context) {
            super(message, context);
        }

        /**
         * Constructs a new LLMProviderException with enhanced context and cause.
         *
         * @param message the detail message explaining the LLM provider error
         * @param cause the underlying cause of the LLM provider failure
         * @param context the enhanced context information
         */
        public LLMProviderException(String message, Throwable cause, ExceptionContext context) {
            super(message, cause, context);
        }

        /**
         * Gets the provider name from context metadata.
         *
         * @return provider name, or null if not available
         */
        public String getProviderName() {
            return hasContext() ? getContext().getMetadata("provider.name", String.class) : null;
        }

        /**
         * Gets the model name from context metadata.
         *
         * @return model name, or null if not available
         */
        public String getModelName() {
            return hasContext() ? getContext().getMetadata("provider.model", String.class) : null;
        }

        /**
         * Gets the HTTP status code from context metadata.
         *
         * @return HTTP status code, or null if not available
         */
        public Integer getHttpStatusCode() {
            return hasContext() ? getContext().getMetadata("http.status", Integer.class) : null;
        }

        /**
         * Gets the rate limit reset time from context metadata.
         *
         * @return rate limit reset time in seconds, or null if not available
         */
        public Long getRateLimitResetTime() {
            return hasContext() ? getContext().getMetadata("rate_limit.reset_time", Long.class) : null;
        }
    }

    /**
     * Exception thrown when LLM provider authentication fails.
     * <p>
     * This exception is thrown when API keys are invalid, expired, or missing,
     * or when authentication with the LLM service fails for any reason.
     * </p>
     *
     * @since 1.0.0
     */
    public static class LLMAuthenticationException extends LLMProviderException {
        /**
         * Constructs a new LLMAuthenticationException with the specified detail message.
         *
         * @param message the detail message explaining the authentication failure
         */
        public LLMAuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when LLM provider rate limits are exceeded.
     * <p>
     * This exception is thrown when the application exceeds the allowed
     * request rate or quota limits imposed by the LLM service provider.
     * </p>
     *
     * @since 1.0.0
     */
    public static class LLMRateLimitException extends LLMProviderException {
        /**
         * Constructs a new LLMRateLimitException with the specified detail message.
         *
         * @param message the detail message explaining the rate limit violation
         */
        public LLMRateLimitException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when LLM provider requests time out.
     * <p>
     * This exception is thrown when requests to the LLM service take longer
     * than the configured timeout period or when the service is unresponsive.
     * </p>
     *
     * @since 1.0.0
     */
    public static class LLMTimeoutException extends LLMProviderException {
        /**
         * Constructs a new LLMTimeoutException with the specified detail message.
         *
         * @param message the detail message explaining the timeout condition
         */
        public LLMTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when tool execution fails.
     * <p>
     * This exception is thrown when tools encounter errors during execution,
     * such as invalid input parameters, external service failures, or
     * system resource limitations.
     * </p>
     * <p>
     * Enhanced with tool-specific context including:
     * </p>
     * <ul>
     * <li>Tool name and type information</li>
     * <li>Input parameters and validation details</li>
     * <li>Execution environment metadata</li>
     * <li>Fallback tool recommendations</li>
     * </ul>
     *
     * @since 1.0.0
     * @see Tool
     */
    public static class ToolExecutionException extends ConductorException {
        /**
         * Constructs a new ToolExecutionException with the specified detail message.
         *
         * @param message the detail message explaining the tool execution failure
         */
        public ToolExecutionException(String message) {
            super(message);
        }

        /**
         * Constructs a new ToolExecutionException with the specified detail message and cause.
         *
         * @param message the detail message explaining the tool execution failure
         * @param cause the underlying cause of the tool execution failure
         */
        public ToolExecutionException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new ToolExecutionException with enhanced context.
         *
         * @param message the detail message explaining the tool execution failure
         * @param context the enhanced context information
         */
        public ToolExecutionException(String message, ExceptionContext context) {
            super(message, context);
        }

        /**
         * Constructs a new ToolExecutionException with enhanced context and cause.
         *
         * @param message the detail message explaining the tool execution failure
         * @param cause the underlying cause of the tool execution failure
         * @param context the enhanced context information
         */
        public ToolExecutionException(String message, Throwable cause, ExceptionContext context) {
            super(message, cause, context);
        }

        /**
         * Gets the tool name from context metadata.
         *
         * @return tool name, or null if not available
         */
        public String getToolName() {
            return hasContext() ? getContext().getMetadata("tool.name", String.class) : null;
        }

        /**
         * Gets the tool type from context metadata.
         *
         * @return tool type, or null if not available
         */
        public String getToolType() {
            return hasContext() ? getContext().getMetadata("tool.type", String.class) : null;
        }

        /**
         * Gets the suggested fallback tool from context metadata.
         *
         * @return fallback tool name, or null if not available
         */
        public String getFallbackTool() {
            return hasContext() ? getContext().getMetadata("tool.fallback", String.class) : null;
        }

        /**
         * Gets the input validation error from context metadata.
         *
         * @return validation error message, or null if not available
         */
        public String getValidationError() {
            return hasContext() ? getContext().getMetadata("validation.error", String.class) : null;
        }
    }

    /**
     * Exception thrown when planning and orchestration operations fail.
     * <p>
     * This exception is thrown during workflow planning, task decomposition,
     * or orchestration failures where the system cannot create or execute
     * a valid execution plan.
     * </p>
     *
     * @since 1.0.0
     * @see LLMPlanMaker
     * @see PlannerOrchestrator
     */
    public static class PlannerException extends ConductorException {
        /**
         * Constructs a new PlannerException with the specified detail message.
         *
         * @param message the detail message explaining the planning failure
         */
        public PlannerException(String message) {
            super(message);
        }

        /**
         * Constructs a new PlannerException with the specified detail message and cause.
         *
         * @param message the detail message explaining the planning failure
         * @param cause the underlying cause of the planning failure
         */
        public PlannerException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new PlannerException with enhanced context.
         *
         * @param message the detail message explaining the planning failure
         * @param context the enhanced context information
         */
        public PlannerException(String message, ExceptionContext context) {
            super(message, context);
        }

        /**
         * Constructs a new PlannerException with enhanced context and cause.
         *
         * @param message the detail message explaining the planning failure
         * @param cause the underlying cause of the planning failure
         * @param context the enhanced context information
         */
        public PlannerException(String message, Throwable cause, ExceptionContext context) {
            super(message, cause, context);
        }
    }
}

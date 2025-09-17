package com.skanga.conductor.exception;

import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.LLMPlanner;
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
 * Subclasses include:
 * </p>
 * <ul>
 * <li>{@link LLMProviderException} - LLM service related errors</li>
 * <li>{@link ToolExecutionException} - Tool execution failures</li>
 * <li>{@link PlannerException} - Planning and orchestration errors</li>
 * <li>{@link MemoryStoreException} - Memory persistence errors (unchecked)</li>
 * </ul>
 *
 * @since 1.0.0
 * @see LLMProviderException
 * @see ToolExecutionException
 * @see PlannerException
 * @see MemoryStoreException
 */
public class ConductorException extends Exception {
    /**
     * Constructs a new ConductorException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ConductorException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConductorException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public ConductorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Unchecked exception used by {@link MemoryStore} to wrap SQL related errors.
     * <p>
     * It extends {@link RuntimeException} so callers are not forced to catch it,
     * but the original {@link java.sql.SQLException} is retained as the cause.
     * This allows for cleaner client code while still preserving error details
     * for debugging and logging purposes.
     * </p>
     *
     * @since 1.0.0
     * @see MemoryStore
     */
    public static class MemoryStoreException extends RuntimeException {
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
     * @see LLMPlanner
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
    }
}

package com.skanga.conductor.exception;

/**
 * Exception Strategy Guidelines for Conductor Framework
 *
 * This class documents the official strategy for when to use checked vs unchecked exceptions
 * in the Conductor framework, providing clear guidelines for developers.
 *
 * CLASSIFICATION PRINCIPLES:
 *
 * 1. UNCHECKED EXCEPTIONS (RuntimeException)
 *    Use for programming errors, system failures, and unrecoverable conditions:
 *    - Configuration errors during startup
 *    - Invalid parameters/arguments
 *    - Infrastructure failures (database, network)
 *    - Data format/parsing errors
 *    - Internal state corruption
 *
 * 2. CHECKED EXCEPTIONS (Exception)
 *    Use for recoverable business logic failures that calling code should handle:
 *    - External service communication failures (can retry)
 *    - Tool execution failures (can fallback to alternative)
 *    - Human approval/interaction timeouts (can prompt again)
 *    - Temporary resource unavailability (can wait and retry)
 *
 * RATIONALE:
 * - Unchecked exceptions represent programming errors or system issues that
 *   the calling code typically cannot recover from meaningfully
 * - Checked exceptions represent expected failure conditions in business logic
 *   that calling code should explicitly handle with recovery strategies
 * - This creates a clear distinction between "should not happen" vs "might happen"
 *   failure scenarios
 *
 * IMPLEMENTATION GUIDELINES:
 * - All custom exceptions should extend either ConductorException (checked)
 *   or ConductorRuntimeException (unchecked) for consistency
 * - Provide clear error messages with actionable information
 * - Include root cause exceptions when wrapping
 * - Log at appropriate levels: ERROR for business failures, WARN for retryable issues
 * - Document recovery strategies in exception javadoc
 */
public final class ExceptionStrategy {

    /**
     * Exception Classification Categories
     */
    public enum Category {
        /**
         * Programming errors and system failures - use unchecked exceptions
         * Examples: configuration errors, invalid arguments, infrastructure failures
         */
        PROGRAMMING_ERROR,

        /**
         * Recoverable business logic failures - use checked exceptions
         * Examples: external service timeouts, tool failures, approval timeouts
         */
        BUSINESS_FAILURE,

        /**
         * Data format and parsing errors - use unchecked exceptions
         * Examples: malformed JSON, invalid file formats, corrupted data
         */
        DATA_FORMAT_ERROR,

        /**
         * Infrastructure and resource errors - use unchecked exceptions
         * Examples: database connection failures, file system errors, memory issues
         */
        INFRASTRUCTURE_ERROR
    }

    /**
     * Recommended Exception Hierarchy:
     *
     * ConductorException (checked) - base for all checked exceptions
     * ├── LLMProviderException - external service failures (retryable)
     * ├── ToolExecutionException - tool failures (can fallback)
     * └── ApprovalException - human interaction failures (can re-prompt)
     *
     * ConductorRuntimeException (unchecked) - base for all unchecked exceptions
     * ├── ConfigurationException - startup/config errors
     * ├── MemoryStoreException - database/infrastructure failures
     * ├── JsonProcessingRuntimeException - data format errors
     * └── ValidationException - parameter/state validation errors
     */
    private ExceptionStrategy() {
        // Utility class - no instantiation
    }
}
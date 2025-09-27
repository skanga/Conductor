package com.skanga.conductor.execution;

/**
 * Unified immutable data transfer object representing the result of execution operations.
 * <p>
 * This record replaces both TaskResult and ToolResult to provide a consistent API
 * across all execution contexts in the Conductor framework. It encapsulates the
 * outcome of task execution, tool execution, or any other execution operations.
 * </p>
 * <p>
 * The success field indicates whether the execution completed successfully without
 * errors. The output field contains the main result content such as generated text,
 * processed data, file contents, command output, or error messages. The metadata
 * field can contain additional information about the execution such as performance
 * metrics, execution context, file sizes, exit codes, or other operation-specific data.
 * </p>
 * <p>
 * As a record, this class is immutable and provides automatic implementations of
 * equals(), hashCode(), toString(), and accessor methods.
 * </p>
 *
 * @param success true if the execution completed successfully, false if it failed
 * @param output the main result content from execution, or error message if failed
 * @param metadata optional additional execution information or metrics
 *
 * @since 1.0.0
 * @see ExecutionInput
 */
public record ExecutionResult(boolean success, String output, Object metadata) {

    /**
     * Creates a successful ExecutionResult with output and no metadata.
     *
     * @param output the result content from successful execution
     * @return new ExecutionResult instance marked as successful
     */
    public static ExecutionResult success(String output) {
        return new ExecutionResult(true, output, null);
    }

    /**
     * Creates a successful ExecutionResult with output and metadata.
     *
     * @param output the result content from successful execution
     * @param metadata additional execution information or metrics
     * @return new ExecutionResult instance marked as successful
     */
    public static ExecutionResult success(String output, Object metadata) {
        return new ExecutionResult(true, output, metadata);
    }

    /**
     * Creates a failed ExecutionResult with error message and no metadata.
     *
     * @param errorMessage the error message describing what failed
     * @return new ExecutionResult instance marked as failed
     */
    public static ExecutionResult failure(String errorMessage) {
        return new ExecutionResult(false, errorMessage, null);
    }

    /**
     * Creates a failed ExecutionResult with error message and metadata.
     *
     * @param errorMessage the error message describing what failed
     * @param metadata additional error context or execution information
     * @return new ExecutionResult instance marked as failed
     */
    public static ExecutionResult failure(String errorMessage, Object metadata) {
        return new ExecutionResult(false, errorMessage, metadata);
    }

    /**
     * Returns true if this result represents a successful execution.
     *
     * @return true if execution was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns true if this result represents a failed execution.
     *
     * @return true if execution failed, false otherwise
     */
    public boolean isFailure() {
        return !success;
    }
}
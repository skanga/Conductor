package com.skanga.conductor.execution;

/**
 * Unified immutable data transfer object representing input for execution operations.
 * <p>
 * This record replaces both TaskInput and ToolInput to provide a consistent API
 * across all execution contexts in the Conductor framework. It encapsulates the
 * information needed to execute tasks, tools, or any other execution operations.
 * </p>
 * <p>
 * The content field contains the main text instruction, query, or data that should
 * be processed. The metadata field can contain any additional contextual information
 * such as execution parameters, workflow context, configuration data, or tool-specific
 * settings.
 * </p>
 * <p>
 * As a record, this class is immutable and provides automatic implementations of
 * equals(), hashCode(), toString(), and accessor methods.
 * </p>
 *
 * @param content the main text content, instruction, or data to be processed
 * @param metadata optional additional context data, configuration, or parameters
 *
 * @since 1.0.0
 * @see ExecutionResult
 */
public record ExecutionInput(String content, Object metadata) {

    /**
     * Creates an ExecutionInput with only content and no metadata.
     *
     * @param content the main text content to be processed
     * @return new ExecutionInput instance with null metadata
     */
    public static ExecutionInput of(String content) {
        return new ExecutionInput(content, null);
    }

    /**
     * Creates an ExecutionInput with content and metadata.
     *
     * @param content the main text content to be processed
     * @param metadata additional context or configuration data
     * @return new ExecutionInput instance
     */
    public static ExecutionInput of(String content, Object metadata) {
        return new ExecutionInput(content, metadata);
    }
}
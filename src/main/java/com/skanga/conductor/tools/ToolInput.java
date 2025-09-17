package com.skanga.conductor.tools;

/**
 * Immutable data transfer object representing input for tool execution.
 * <p>
 * This record encapsulates the information needed to execute a tool within the
 * Conductor framework, including the primary text input and optional metadata
 * for additional context or configuration.
 * </p>
 * <p>
 * The text field contains the main input data that the tool should process,
 * such as file paths for file operations, commands for execution, queries for
 * searches, or content for processing. The metadata field can contain any
 * additional configuration or context data specific to the tool's operation.
 * </p>
 * <p>
 * As a record, this class is immutable and provides automatic implementations of
 * equals(), hashCode(), toString(), and accessor methods.
 * </p>
 *
 * @param text the main text input for the tool to process
 * @param metadata optional additional configuration or context data, can be null
 *
 * @since 1.0.0
 * @see Tool#run(ToolInput)
 * @see ToolResult
 */
public record ToolInput(String text, Object metadata) {
}
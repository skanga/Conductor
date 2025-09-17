package com.skanga.conductor.tools;

/**
 * Immutable data transfer object representing the result of tool execution.
 * <p>
 * This record encapsulates the outcome of a tool execution within the Conductor
 * framework, including the success status, generated output, and optional metadata.
 * It provides a standardized way for tools to return execution results to agents
 * and other components.
 * </p>
 * <p>
 * The success field indicates whether the tool execution completed successfully
 * or encountered an error. The output field contains the main result of the tool
 * execution, such as file contents, command output, search results, or error messages.
 * The metadata field can contain additional information about the execution, such as
 * performance metrics, file sizes, exit codes, or other tool-specific data.
 * </p>
 * <p>
 * As a record, this class is immutable and provides automatic implementations of
 * equals(), hashCode(), toString(), and accessor methods.
 * </p>
 *
 * @param success true if the tool execution completed successfully, false if it failed
 * @param output the main result content from tool execution, or error message if failed
 * @param metadata optional additional execution information, can be null
 *
 * @since 1.0.0
 * @see Tool#run(ToolInput)
 * @see ToolInput
 */
public record ToolResult(boolean success, String output, Object metadata) {
}
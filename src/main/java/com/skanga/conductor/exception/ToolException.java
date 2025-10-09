package com.skanga.conductor.exception;

/**
 * Exception for tool execution errors.
 * <p>
 * This exception is thrown when a tool execution fails. Tools can fail
 * for various reasons including invalid input, missing resources, or
 * execution errors.
 * </p>
 * <p>
 * Common scenarios:
 * </p>
 * <ul>
 * <li>NOT_FOUND: Tool not found in registry</li>
 * <li>INVALID_INPUT: Invalid tool parameters</li>
 * <li>CONFIGURATION_ERROR: Tool not properly configured</li>
 * <li>INTERNAL_ERROR: Tool execution failed</li>
 * </ul>
 *
 * @since 1.1.0
 * @see ErrorCategory
 * @see SimpleConductorException
 */
public class ToolException extends SimpleConductorException {

    private final String toolName;

    /**
     * Creates a new tool exception.
     *
     * @param category the error category
     * @param message the detail message
     * @param toolName the name of the tool
     */
    public ToolException(ErrorCategory category, String message, String toolName) {
        this(category, message, null, toolName);
    }

    /**
     * Creates a new tool exception with cause.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause
     * @param toolName the name of the tool
     */
    public ToolException(ErrorCategory category, String message,
                        Throwable cause, String toolName) {
        super(category, message, cause, "tool=" + toolName);
        this.toolName = toolName;
    }

    /**
     * Returns the tool name.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Creates a tool exception from another exception with automatic categorization.
     *
     * @param message the detail message
     * @param cause the underlying exception
     * @param toolName the tool name
     * @return a new ToolException
     */
    public static ToolException from(String message, Exception cause, String toolName) {
        ErrorCategory category = ErrorCategory.fromException(cause);
        return new ToolException(category, message, cause, toolName);
    }
}

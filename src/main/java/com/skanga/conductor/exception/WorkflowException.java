package com.skanga.conductor.exception;

/**
 * Exception for workflow execution errors.
 * <p>
 * This exception is thrown when a workflow execution fails. This includes
 * errors in workflow orchestration, stage execution, and workflow validation.
 * </p>
 * <p>
 * Common scenarios:
 * </p>
 * <ul>
 * <li>CONFIGURATION_ERROR: Invalid workflow configuration</li>
 * <li>INVALID_INPUT: Invalid workflow input or context</li>
 * <li>NOT_FOUND: Workflow or stage not found</li>
 * <li>INTERNAL_ERROR: Workflow execution failed</li>
 * </ul>
 *
 * @since 1.1.0
 * @see ErrorCategory
 * @see SimpleConductorException
 */
public class WorkflowException extends SimpleConductorException {

    private final String workflowName;
    private final String stageName;

    /**
     * Creates a new workflow exception.
     *
     * @param category the error category
     * @param message the detail message
     * @param workflowName the name of the workflow (may be null)
     */
    public WorkflowException(ErrorCategory category, String message, String workflowName) {
        this(category, message, null, workflowName, null);
    }

    /**
     * Creates a new workflow exception with cause.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause
     * @param workflowName the name of the workflow (may be null)
     */
    public WorkflowException(ErrorCategory category, String message,
                            Throwable cause, String workflowName) {
        this(category, message, cause, workflowName, null);
    }

    /**
     * Creates a new workflow exception with full context.
     *
     * @param category the error category
     * @param message the detail message
     * @param cause the underlying cause (may be null)
     * @param workflowName the name of the workflow (may be null)
     * @param stageName the name of the stage (may be null)
     */
    public WorkflowException(ErrorCategory category, String message,
                            Throwable cause, String workflowName, String stageName) {
        super(category, message, cause, buildContext(workflowName, stageName));
        this.workflowName = workflowName;
        this.stageName = stageName;
    }

    /**
     * Returns the workflow name, if specified.
     *
     * @return the workflow name, or null
     */
    public String getWorkflowName() {
        return workflowName;
    }

    /**
     * Returns the stage name, if specified.
     *
     * @return the stage name, or null
     */
    public String getStageName() {
        return stageName;
    }

    private static String buildContext(String workflowName, String stageName) {
        if (workflowName != null && stageName != null) {
            return "workflow=" + workflowName + ", stage=" + stageName;
        } else if (workflowName != null) {
            return "workflow=" + workflowName;
        } else if (stageName != null) {
            return "stage=" + stageName;
        }
        return null;
    }

    /**
     * Creates a workflow exception from another exception with automatic categorization.
     *
     * @param message the detail message
     * @param cause the underlying exception
     * @param workflowName the workflow name
     * @return a new WorkflowException
     */
    public static WorkflowException from(String message, Exception cause, String workflowName) {
        ErrorCategory category = ErrorCategory.fromException(cause);
        return new WorkflowException(category, message, cause, workflowName);
    }
}

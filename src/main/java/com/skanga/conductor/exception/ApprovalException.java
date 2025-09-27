package com.skanga.conductor.exception;

/**
 * Exception thrown when there's an error during the approval process.
 * <p>
 * This exception represents recoverable business logic failures related to human
 * approval interactions. Common scenarios include approval timeouts, user rejections,
 * and communication failures with approval handlers.
 * </p>
 * <p>
 * Following the framework's exception strategy, ApprovalException extends ConductorException
 * (checked) because approval failures are expected business conditions that calling code
 * should handle with recovery strategies such as re-prompting the user or falling back
 * to alternative approval mechanisms.
 * </p>
 * <p>
 * Enhanced with approval-specific context including:
 * </p>
 * <ul>
 * <li>Approval request details and timeout information</li>
 * <li>User interaction metadata</li>
 * <li>Handler type and configuration</li>
 * <li>Retry and escalation recommendations</li>
 * </ul>
 *
 * @see ExceptionStrategy
 * @see ExceptionContext
 * @since 1.0.0
 */
public class ApprovalException extends ConductorException {

    public ApprovalException(String message) {
        super(message);
    }

    public ApprovalException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ApprovalException with enhanced context.
     *
     * @param message the detail message explaining the approval failure
     * @param context the enhanced context information
     */
    public ApprovalException(String message, ExceptionContext context) {
        super(message, context);
    }

    /**
     * Constructs a new ApprovalException with enhanced context and cause.
     *
     * @param message the detail message explaining the approval failure
     * @param cause the underlying cause of the approval failure
     * @param context the enhanced context information
     */
    public ApprovalException(String message, Throwable cause, ExceptionContext context) {
        super(message, cause, context);
    }

    /**
     * Gets the approval request ID from context metadata.
     *
     * @return approval request ID, or null if not available
     */
    public String getApprovalRequestId() {
        return hasContext() ? getContext().getMetadata("approval.request_id", String.class) : null;
    }

    /**
     * Gets the approval handler type from context metadata.
     *
     * @return handler type, or null if not available
     */
    public String getHandlerType() {
        return hasContext() ? getContext().getMetadata("approval.handler_type", String.class) : null;
    }

    /**
     * Gets the timeout duration from context metadata.
     *
     * @return timeout in milliseconds, or null if not available
     */
    public Long getTimeoutMs() {
        return hasContext() ? getContext().getMetadata("approval.timeout_ms", Long.class) : null;
    }

    /**
     * Gets the user ID from context metadata.
     *
     * @return user ID, or null if not available
     */
    public String getUserId() {
        return hasContext() ? getContext().getMetadata("approval.user_id", String.class) : null;
    }

    /**
     * Gets the escalation contact from context metadata.
     *
     * @return escalation contact, or null if not available
     */
    public String getEscalationContact() {
        return hasContext() ? getContext().getMetadata("approval.escalation_contact", String.class) : null;
    }
}
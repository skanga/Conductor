package com.skanga.conductor.exception;

/**
 * Base unchecked exception class for the Conductor framework.
 * <p>
 * This exception serves as the root for all unchecked exceptions in the Conductor
 * framework, representing programming errors, configuration issues, and system
 * failures that calling code typically cannot recover from meaningfully.
 * </p>
 * <p>
 * Following the established exception strategy:
 * - Use ConductorRuntimeException for programming errors and system failures
 * - Use ConductorException for recoverable business logic failures
 * </p>
 * <p>
 * Enhanced Context Support:
 * </p>
 * <ul>
 * <li>Structured error codes and categories via {@link ExceptionContext}</li>
 * <li>Diagnostic metadata for system errors</li>
 * <li>Configuration details for setup failures</li>
 * <li>Validation details for parameter errors</li>
 * </ul>
 * <p>
 * Subclasses should include:
 * </p>
 * <ul>
 * <li>ConfigurationException - Configuration and startup errors</li>
 * <li>ValidationException - Parameter and state validation errors</li>
 * <li>JsonProcessingRuntimeException - Data format and parsing errors</li>
 * <li>MemoryStoreException - Database and infrastructure failures</li>
 * </ul>
 *
 * @since 1.0.0
 * @see ConductorException
 * @see ExceptionStrategy
 * @see ExceptionContext
 */
public class ConductorRuntimeException extends RuntimeException {

    private final ExceptionContext context;

    /**
     * Constructs a new ConductorRuntimeException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ConductorRuntimeException(String message) {
        super(message);
        this.context = null;
    }

    /**
     * Constructs a new ConductorRuntimeException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public ConductorRuntimeException(String message, Throwable cause) {
        super(message, cause);
        this.context = null;
    }

    /**
     * Constructs a new ConductorRuntimeException with the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public ConductorRuntimeException(Throwable cause) {
        super(cause);
        this.context = null;
    }

    /**
     * Constructs a new ConductorRuntimeException with enhanced context information.
     *
     * @param message the detail message explaining the cause of the exception
     * @param context the enhanced context information
     */
    public ConductorRuntimeException(String message, ExceptionContext context) {
        super(enhanceMessage(message, context));
        this.context = context;
    }

    /**
     * Constructs a new ConductorRuntimeException with enhanced context information and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     * @param context the enhanced context information
     */
    public ConductorRuntimeException(String message, Throwable cause, ExceptionContext context) {
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
        return enhanced.toString();
    }
}
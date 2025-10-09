package com.skanga.conductor.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for creating exceptions with rich context information.
 * <p>
 * This class provides a convenient way to create exceptions with comprehensive
 * context data, making error handling and debugging more effective. It supports
 * both checked and unchecked exceptions with consistent API patterns.
 * </p>
 * <p>
 * Usage examples:
 * </p>
 * <pre>
 * // LLM Provider Exception with context
 * throw ExceptionBuilder.llmProvider("OpenAI request failed")
 *     .errorCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
 *     .operation("generate_completion")
 *     .provider("openai", "gpt-4")
 *     .httpStatus(429)
 *     .rateLimitReset(3600)
 *     .retryWithBackoff()
 *     .build();
 *
 * // Tool Execution Exception with fallback
 * throw ExceptionBuilder.toolExecution("File read failed")
 *     .errorCode(ErrorCodes.EXECUTION_FAILED)
 *     .tool("file_reader", "io")
 *     .fallbackTool("backup_reader")
 *     .validationError("File not found: " + filename)
 *     .useFallback()
 *     .build();
 *
 * // Configuration Exception
 * throw ExceptionBuilder.configuration("Invalid database URL")
 *     .errorCode(ErrorCodes.CONFIGURATION_ERROR)
 *     .operation("database_initialization")
 *     .metadata("url", invalidUrl)
 *     .fixConfiguration()
 *     .build();
 * </pre>
 *
 * @since 1.0.0
 * @see ExceptionContext
 * @see ErrorCodes
 */
public final class ExceptionBuilder {

    private final String message;
    private final ExceptionType type;
    private Throwable cause;
    private String errorCode;
    private ExceptionContext.ErrorCategory category;
    private String operation;
    private Instant timestamp;
    private String correlationId;
    private ExceptionContext.RecoveryHint recoveryHint;
    private String recoveryDetails;
    private Long duration;
    private Integer attemptNumber;
    private Integer maxAttempts;
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * Exception types supported by the builder
     */
    private enum ExceptionType {
        LLM_PROVIDER,
        TOOL_EXECUTION,
        PLANNER,
        APPROVAL,
        CONFIGURATION,
        MEMORY_STORE,
        JSON_PROCESSING,
        VALIDATION
    }

    private ExceptionBuilder(String message, ExceptionType type) {
        this.message = message;
        this.type = type;
    }

    // Factory methods for different exception types

    /**
     * Creates a builder for LLMProviderException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder llmProvider(String message) {
        return new ExceptionBuilder(message, ExceptionType.LLM_PROVIDER);
    }

    /**
     * Creates a builder for ToolExecutionException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder toolExecution(String message) {
        return new ExceptionBuilder(message, ExceptionType.TOOL_EXECUTION);
    }

    /**
     * Creates a builder for PlannerException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder planner(String message) {
        return new ExceptionBuilder(message, ExceptionType.PLANNER);
    }

    /**
     * Creates a builder for ApprovalException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder approval(String message) {
        return new ExceptionBuilder(message, ExceptionType.APPROVAL);
    }

    /**
     * Creates a builder for ConfigurationException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder configuration(String message) {
        return new ExceptionBuilder(message, ExceptionType.CONFIGURATION);
    }

    /**
     * Creates a builder for MemoryStoreException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder memoryStore(String message) {
        return new ExceptionBuilder(message, ExceptionType.MEMORY_STORE);
    }

    /**
     * Creates a builder for JsonProcessingRuntimeException.
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder jsonProcessing(String message) {
        return new ExceptionBuilder(message, ExceptionType.JSON_PROCESSING);
    }

    /**
     * Creates a builder for validation errors (using ConductorRuntimeException).
     *
     * @param message the error message
     * @return new exception builder
     */
    public static ExceptionBuilder validation(String message) {
        return new ExceptionBuilder(message, ExceptionType.VALIDATION);
    }

    // Context building methods

    /**
     * Sets the error code and automatically determines category and recovery hint.
     *
     * @param errorCode the structured error code
     * @return this builder
     */
    public ExceptionBuilder errorCode(String errorCode) {
        this.errorCode = errorCode;
        this.category = ErrorCodes.toExceptionContextCategory(errorCode);
        return this;
    }

    /**
     * Sets the operation that was being performed when the error occurred.
     *
     * @param operation the operation name
     * @return this builder
     */
    public ExceptionBuilder operation(String operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Sets the underlying cause of the exception.
     *
     * @param cause the root cause
     * @return this builder
     */
    public ExceptionBuilder cause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    /**
     * Sets the correlation ID for distributed tracing.
     *
     * @param correlationId the correlation ID
     * @return this builder
     */
    public ExceptionBuilder correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Sets the duration of the failed operation.
     *
     * @param duration the duration in milliseconds
     * @return this builder
     */
    public ExceptionBuilder duration(long duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Sets the attempt information for retry scenarios.
     *
     * @param attemptNumber the current attempt number
     * @param maxAttempts the maximum number of attempts
     * @return this builder
     */
    public ExceptionBuilder attempt(int attemptNumber, int maxAttempts) {
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
        return this;
    }

    /**
     * Adds custom metadata.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder
     */
    public ExceptionBuilder metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    // LLM Provider specific methods

    /**
     * Sets provider information for LLM exceptions.
     *
     * @param providerName the provider name (e.g., "openai", "anthropic")
     * @param modelName the model name (e.g., "gpt-4", "claude-3")
     * @return this builder
     */
    public ExceptionBuilder provider(String providerName, String modelName) {
        return metadata("provider.name", providerName)
                .metadata("provider.model", modelName);
    }

    /**
     * Sets HTTP status code for network-related LLM errors.
     *
     * @param statusCode the HTTP status code
     * @return this builder
     */
    public ExceptionBuilder httpStatus(int statusCode) {
        return metadata("http.status", statusCode);
    }

    /**
     * Sets rate limit reset time for rate limit errors.
     *
     * @param resetTimeSeconds the reset time in seconds
     * @return this builder
     */
    public ExceptionBuilder rateLimitReset(long resetTimeSeconds) {
        return metadata("rate_limit.reset_time", resetTimeSeconds);
    }

    // Tool Execution specific methods

    /**
     * Sets tool information for tool execution exceptions.
     *
     * @param toolName the tool name
     * @param toolType the tool type
     * @return this builder
     */
    public ExceptionBuilder tool(String toolName, String toolType) {
        return metadata("tool.name", toolName)
                .metadata("tool.type", toolType);
    }

    /**
     * Sets fallback tool recommendation.
     *
     * @param fallbackTool the fallback tool name
     * @return this builder
     */
    public ExceptionBuilder fallbackTool(String fallbackTool) {
        return metadata("tool.fallback", fallbackTool);
    }

    /**
     * Sets validation error details for tool input/output errors.
     *
     * @param validationError the validation error message
     * @return this builder
     */
    public ExceptionBuilder validationError(String validationError) {
        return metadata("validation.error", validationError);
    }

    // Approval specific methods

    /**
     * Sets approval request information.
     *
     * @param requestId the approval request ID
     * @param handlerType the handler type
     * @return this builder
     */
    public ExceptionBuilder approvalRequest(String requestId, String handlerType) {
        return metadata("approval.request_id", requestId)
                .metadata("approval.handler_type", handlerType);
    }

    /**
     * Sets approval timeout information.
     *
     * @param timeoutMs the timeout in milliseconds
     * @return this builder
     */
    public ExceptionBuilder approvalTimeout(long timeoutMs) {
        return metadata("approval.timeout_ms", timeoutMs);
    }

    /**
     * Sets user information for approval exceptions.
     *
     * @param userId the user ID
     * @return this builder
     */
    public ExceptionBuilder userId(String userId) {
        return metadata("approval.user_id", userId);
    }

    /**
     * Sets escalation contact for approval failures.
     *
     * @param escalationContact the escalation contact
     * @return this builder
     */
    public ExceptionBuilder escalationContact(String escalationContact) {
        return metadata("approval.escalation_contact", escalationContact);
    }

    // Recovery hint methods

    /**
     * Sets retry with backoff as the recovery hint.
     *
     * @return this builder
     */
    public ExceptionBuilder retryWithBackoff() {
        this.recoveryHint = ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;
        return this;
    }

    /**
     * Sets use fallback as the recovery hint.
     *
     * @return this builder
     */
    public ExceptionBuilder useFallback() {
        this.recoveryHint = ExceptionContext.RecoveryHint.USE_FALLBACK;
        return this;
    }

    /**
     * Sets fix configuration as the recovery hint.
     *
     * @return this builder
     */
    public ExceptionBuilder fixConfiguration() {
        this.recoveryHint = ExceptionContext.RecoveryHint.FIX_CONFIGURATION;
        return this;
    }

    /**
     * Sets check credentials as the recovery hint.
     *
     * @return this builder
     */
    public ExceptionBuilder checkCredentials() {
        this.recoveryHint = ExceptionContext.RecoveryHint.CHECK_CREDENTIALS;
        return this;
    }

    /**
     * Sets user action required as the recovery hint.
     *
     * @return this builder
     */
    public ExceptionBuilder userActionRequired() {
        this.recoveryHint = ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED;
        return this;
    }

    /**
     * Sets custom recovery details.
     *
     * @param details the recovery details
     * @return this builder
     */
    public ExceptionBuilder recoveryDetails(String details) {
        this.recoveryDetails = details;
        return this;
    }

    /**
     * Builds the exception with the configured context.
     *
     * @return the constructed exception
     */
    public Exception build() {
        ExceptionContext context = ExceptionContext.builder()
                .errorCode(errorCode)
                .category(category)
                .operation(operation)
                .timestamp(timestamp)
                .correlationId(correlationId)
                .recoveryHint(recoveryHint)
                .recoveryDetails(recoveryDetails)
                .duration(duration)
                .attempt(attemptNumber, maxAttempts)
                .metadata(metadata)
                .build();

        switch (type) {
            case LLM_PROVIDER:
                return cause != null
                        ? new ConductorException.LLMProviderException(message, cause, context)
                        : new ConductorException.LLMProviderException(message, context);

            case TOOL_EXECUTION:
                return cause != null
                        ? new ConductorException.ToolExecutionException(message, cause, context)
                        : new ConductorException.ToolExecutionException(message, context);

            case PLANNER:
                return cause != null
                        ? new ConductorException.PlannerException(message, cause, context)
                        : new ConductorException.PlannerException(message, context);

            case APPROVAL:
                return cause != null
                        ? new ApprovalException(message, cause, context)
                        : new ApprovalException(message, context);

            case CONFIGURATION:
                return cause != null
                        ? new ConfigurationException(message, cause)
                        : new ConfigurationException(message);

            case MEMORY_STORE:
                return cause != null
                        ? new ConductorException.MemoryStoreException(message, cause)
                        : new ConductorException.MemoryStoreException(message);

            case JSON_PROCESSING:
                return cause != null
                        ? new com.skanga.conductor.exception.JsonProcessingException(message, cause)
                        : new com.skanga.conductor.exception.JsonProcessingException(message);

            case VALIDATION:
                return cause != null
                        ? new ConductorRuntimeException(message, cause, context)
                        : new ConductorRuntimeException(message, context);

            default:
                throw new IllegalStateException("Unknown exception type: " + type);
        }
    }
}
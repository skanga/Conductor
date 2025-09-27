package com.skanga.conductor.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Enhanced context information for exceptions in the Conductor framework.
 * <p>
 * This class provides structured context data that helps with debugging,
 * monitoring, and recovery logic. It includes timing information, operation
 * details, recovery hints, and custom metadata.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Structured error codes and categories</li>
 * <li>Timing and performance context</li>
 * <li>Operation-specific metadata</li>
 * <li>Recovery hints and suggestions</li>
 * <li>Correlation IDs for distributed tracing</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class ExceptionContext {

    /**
     * Error categories for classification and handling
     */
    public enum ErrorCategory {
        /** Authentication and authorization failures */
        AUTHENTICATION,
        /** Network and connectivity issues */
        NETWORK,
        /** Rate limiting and quota violations */
        RATE_LIMIT,
        /** Timeout and latency issues */
        TIMEOUT,
        /** Invalid input or parameters */
        VALIDATION,
        /** Configuration and setup errors */
        CONFIGURATION,
        /** Resource unavailability */
        RESOURCE_UNAVAILABLE,
        /** Data format and parsing errors */
        DATA_FORMAT,
        /** Internal system errors */
        SYSTEM_ERROR,
        /** Business logic violations */
        BUSINESS_LOGIC,
        /** Human interaction failures */
        USER_INTERACTION
    }

    /**
     * Recovery suggestions for error handling
     */
    public enum RecoveryHint {
        /** Retry the operation with exponential backoff */
        RETRY_WITH_BACKOFF,
        /** Retry immediately (transient failure) */
        RETRY_IMMEDIATE,
        /** Use fallback mechanism */
        USE_FALLBACK,
        /** Fix configuration and restart */
        FIX_CONFIGURATION,
        /** Validate and correct input */
        VALIDATE_INPUT,
        /** Check authentication credentials */
        CHECK_CREDENTIALS,
        /** Wait for rate limit reset */
        WAIT_RATE_LIMIT,
        /** Increase timeout values */
        INCREASE_TIMEOUT,
        /** Contact system administrator */
        CONTACT_ADMIN,
        /** User intervention required */
        USER_ACTION_REQUIRED,
        /** No automatic recovery possible */
        NO_RECOVERY
    }

    private final String errorCode;
    private final ErrorCategory category;
    private final String operation;
    private final Instant timestamp;
    private final String correlationId;
    private final RecoveryHint recoveryHint;
    private final String recoveryDetails;
    private final Long duration;
    private final Integer attemptNumber;
    private final Integer maxAttempts;
    private final Map<String, Object> metadata;

    private ExceptionContext(Builder builder) {
        this.errorCode = builder.errorCode;
        this.category = builder.category;
        this.operation = builder.operation;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.correlationId = builder.correlationId;
        this.recoveryHint = builder.recoveryHint;
        this.recoveryDetails = builder.recoveryDetails;
        this.duration = builder.duration;
        this.attemptNumber = builder.attemptNumber;
        this.maxAttempts = builder.maxAttempts;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Creates a new builder for ExceptionContext.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a minimal context with just error code and category.
     *
     * @param errorCode the error code
     * @param category the error category
     * @return new ExceptionContext instance
     */
    public static ExceptionContext of(String errorCode, ErrorCategory category) {
        return builder()
                .errorCode(errorCode)
                .category(category)
                .build();
    }

    /**
     * Creates a context with error code, category, and operation.
     *
     * @param errorCode the error code
     * @param category the error category
     * @param operation the operation that failed
     * @return new ExceptionContext instance
     */
    public static ExceptionContext of(String errorCode, ErrorCategory category, String operation) {
        return builder()
                .errorCode(errorCode)
                .category(category)
                .operation(operation)
                .build();
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public ErrorCategory getCategory() { return category; }
    public String getOperation() { return operation; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public RecoveryHint getRecoveryHint() { return recoveryHint; }
    public String getRecoveryDetails() { return recoveryDetails; }
    public Long getDuration() { return duration; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @return the metadata value, or null if not present
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Gets a metadata value by key with type casting.
     *
     * @param key the metadata key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the metadata value cast to the specified type, or null if not present or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Checks if this error is retryable based on category and recovery hint.
     *
     * @return true if the error suggests retry is appropriate
     */
    public boolean isRetryable() {
        if (recoveryHint == null) return false;
        return recoveryHint == RecoveryHint.RETRY_WITH_BACKOFF ||
               recoveryHint == RecoveryHint.RETRY_IMMEDIATE ||
               recoveryHint == RecoveryHint.WAIT_RATE_LIMIT;
    }

    /**
     * Checks if this error suggests using a fallback mechanism.
     *
     * @return true if fallback is suggested
     */
    public boolean suggestsFallback() {
        return recoveryHint == RecoveryHint.USE_FALLBACK;
    }

    /**
     * Gets a human-readable summary of the error context.
     *
     * @return formatted error summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error ").append(errorCode != null ? errorCode : "UNKNOWN");
        if (category != null) {
            sb.append(" (").append(category).append(")");
        }
        if (operation != null) {
            sb.append(" in operation '").append(operation).append("'");
        }
        if (attemptNumber != null && maxAttempts != null) {
            sb.append(" [attempt ").append(attemptNumber).append("/").append(maxAttempts).append("]");
        }
        if (duration != null) {
            sb.append(" after ").append(duration).append("ms");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ExceptionContext{" +
                "errorCode='" + errorCode + '\'' +
                ", category=" + category +
                ", operation='" + operation + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                ", recoveryHint=" + recoveryHint +
                ", duration=" + duration +
                ", attemptNumber=" + attemptNumber +
                ", maxAttempts=" + maxAttempts +
                ", metadata=" + metadata +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExceptionContext that = (ExceptionContext) o;
        return Objects.equals(errorCode, that.errorCode) &&
                category == that.category &&
                Objects.equals(operation, that.operation) &&
                Objects.equals(correlationId, that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, category, operation, correlationId);
    }

    /**
     * Builder for creating ExceptionContext instances with fluent API.
     * <p>
     * This builder is thread-safe and can be used concurrently by multiple threads.
     * All methods are synchronized to ensure safe concurrent access.
     * </p>
     */
    public static class Builder {
        private volatile String errorCode;
        private volatile ErrorCategory category;
        private volatile String operation;
        private volatile Instant timestamp;
        private volatile String correlationId;
        private volatile RecoveryHint recoveryHint;
        private volatile String recoveryDetails;
        private volatile Long duration;
        private volatile Integer attemptNumber;
        private volatile Integer maxAttempts;
        private final Map<String, Object> metadata = new java.util.concurrent.ConcurrentHashMap<>();

        public synchronized Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public synchronized Builder category(ErrorCategory category) {
            this.category = category;
            return this;
        }

        public synchronized Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public synchronized Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public synchronized Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public synchronized Builder recoveryHint(RecoveryHint hint) {
            this.recoveryHint = hint;
            return this;
        }

        public synchronized Builder recoveryDetails(String details) {
            this.recoveryDetails = details;
            return this;
        }

        public synchronized Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public synchronized Builder attempt(Integer attemptNumber, Integer maxAttempts) {
            this.attemptNumber = attemptNumber;
            this.maxAttempts = maxAttempts;
            return this;
        }

        public synchronized Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public synchronized Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public synchronized ExceptionContext build() {
            return new ExceptionContext(this);
        }
    }
}
package com.skanga.conductor.exception;

/**
 * Exception thrown when an approval request times out without receiving a response.
 * <p>
 * This exception is thrown when human approval requests exceed the configured timeout
 * period without receiving a user response. It includes the timeout duration for
 * diagnostic purposes and recovery logic.
 * </p>
 * <p>
 * This is a recoverable business condition - calling code can implement retry logic,
 * extend timeouts, or fall back to alternative approval mechanisms.
 * </p>
 *
 * @since 1.0.0
 */
public class ApprovalTimeoutException extends ApprovalException {

    private final long timeoutMs;

    public ApprovalTimeoutException(long timeoutMs) {
        super("Approval request timed out after " + timeoutMs + "ms");
        this.timeoutMs = timeoutMs;
    }

    public ApprovalTimeoutException(String message, long timeoutMs) {
        super(message);
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Long getTimeoutMs() {
        return timeoutMs;
    }
}
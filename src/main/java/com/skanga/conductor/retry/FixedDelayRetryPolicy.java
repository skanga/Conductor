package com.skanga.conductor.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.io.IOException;

/**
 * A retry policy that waits a fixed amount of time between retry attempts.
 * <p>
 * This policy implements a simple retry strategy where each retry attempt
 * is preceded by the same fixed delay. It's suitable for scenarios where
 * the failure cause is expected to resolve quickly or when you want
 * predictable retry timing.
 * </p>
 * <p>
 * The policy includes built-in logic for determining which exceptions
 * are retryable, focusing on transient failures like network timeouts
 * and temporary service unavailability.
 * </p>
 *
 * @since 1.0.0
 * @see RetryPolicy
 * @see ExponentialBackoffRetryPolicy
 */
public class FixedDelayRetryPolicy implements RetryPolicy {

    private final int maxAttempts;
    private final Duration delay;
    private final Duration maxDuration;
    private final Set<Class<? extends Throwable>> retryableExceptions;

    /**
     * Creates a new fixed delay retry policy with default settings.
     *
     * @param maxAttempts the maximum number of attempts (including initial attempt)
     * @param delay the fixed delay between retry attempts
     * @throws IllegalArgumentException if maxAttempts is less than 1 or delay is negative
     */
    public FixedDelayRetryPolicy(int maxAttempts, Duration delay) {
        this(maxAttempts, delay, Duration.ofMinutes(5), getDefaultRetryableExceptions());
    }

    /**
     * Creates a new fixed delay retry policy with custom settings.
     *
     * @param maxAttempts the maximum number of attempts (including initial attempt)
     * @param delay the fixed delay between retry attempts
     * @param maxDuration the maximum total time to spend on retries
     * @param retryableExceptions the set of exception types that should trigger retries
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public FixedDelayRetryPolicy(int maxAttempts, Duration delay, Duration maxDuration,
                                Set<Class<? extends Throwable>> retryableExceptions) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, got: " + maxAttempts);
        }
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay cannot be negative: " + delay);
        }
        if (maxDuration.isNegative()) {
            throw new IllegalArgumentException("maxDuration cannot be negative: " + maxDuration);
        }

        this.maxAttempts = maxAttempts;
        this.delay = delay;
        this.maxDuration = maxDuration;
        this.retryableExceptions = Set.copyOf(Objects.requireNonNull(retryableExceptions,
            "retryableExceptions cannot be null"));
    }

    @Override
    public boolean shouldRetry(RetryContext context) {
        // Don't retry if we've reached the maximum number of attempts
        if (context.getAttemptCount() >= maxAttempts) {
            return false;
        }

        // Don't retry if we've exceeded the maximum duration
        if (context.getElapsedTime().compareTo(maxDuration) >= 0) {
            return false;
        }

        // Don't retry if the last exception is not retryable
        Throwable lastException = context.getLastException();
        if (lastException != null && !isRetryableException(lastException)) {
            return false;
        }

        return true;
    }

    @Override
    public Duration getRetryDelay(RetryContext context) {
        return delay;
    }

    @Override
    public boolean isRetryableException(Throwable exception) {
        if (exception == null) {
            return false;
        }

        // Check if the exact exception type is retryable
        if (retryableExceptions.contains(exception.getClass())) {
            return true;
        }

        // Check if any parent class is retryable
        for (Class<? extends Throwable> retryableType : retryableExceptions) {
            if (retryableType.isAssignableFrom(exception.getClass())) {
                return true;
            }
        }

        // Special handling for common retryable patterns
        return isCommonRetryableException(exception);
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public Duration getMaxDuration() {
        return maxDuration;
    }

    /**
     * Checks for common retryable exception patterns based on message content.
     *
     * @param exception the exception to check
     * @return true if the exception appears to be retryable based on its message
     */
    private boolean isCommonRetryableException(Throwable exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // Common patterns that indicate transient failures
        return lowerMessage.contains("timeout") ||
               lowerMessage.contains("connection reset") ||
               lowerMessage.contains("connection refused") ||
               lowerMessage.contains("temporary failure") ||
               lowerMessage.contains("service unavailable") ||
               lowerMessage.contains("rate limit") ||
               lowerMessage.contains("too many requests") ||
               lowerMessage.contains("server error") ||
               lowerMessage.contains("internal error") ||
               lowerMessage.contains("network is unreachable");
    }

    /**
     * Returns the default set of retryable exception types.
     *
     * @return a set of exception classes that are considered retryable by default
     */
    private static Set<Class<? extends Throwable>> getDefaultRetryableExceptions() {
        return Set.of(
            IOException.class,
            SocketTimeoutException.class,
            ConnectException.class,
            TimeoutException.class
        );
    }

    @Override
    public String toString() {
        return String.format("FixedDelayRetryPolicy{maxAttempts=%d, delay=%s, maxDuration=%s}",
            maxAttempts, delay, maxDuration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FixedDelayRetryPolicy that = (FixedDelayRetryPolicy) obj;
        return maxAttempts == that.maxAttempts &&
               Objects.equals(delay, that.delay) &&
               Objects.equals(maxDuration, that.maxDuration) &&
               Objects.equals(retryableExceptions, that.retryableExceptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxAttempts, delay, maxDuration, retryableExceptions);
    }
}
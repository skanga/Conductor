package com.skanga.conductor.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.io.IOException;

/**
 * A retry policy that implements exponential backoff with optional jitter.
 * <p>
 * This policy increases the delay between retry attempts exponentially,
 * which helps reduce load on failing services while providing reasonable
 * retry behavior. Jitter can be added to prevent the "thundering herd"
 * problem when multiple clients retry simultaneously.
 * </p>
 * <p>
 * The delay calculation follows the formula:
 * delay = min(initialDelay * (multiplier ^ attemptNumber), maxDelay)
 * </p>
 * <p>
 * With jitter enabled, the actual delay is randomized within a range
 * around the calculated delay to avoid synchronized retries.
 * </p>
 *
 * @since 1.0.0
 * @see RetryPolicy
 * @see FixedDelayRetryPolicy
 */
public class ExponentialBackoffRetryPolicy implements RetryPolicy {

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double multiplier;
    private final Duration maxDuration;
    private final boolean jitterEnabled;
    private final double jitterFactor;
    private final Set<Class<? extends Throwable>> retryableExceptions;
    private final Random random;

    /**
     * Creates a new exponential backoff retry policy with default settings.
     *
     * @param maxAttempts the maximum number of attempts (including initial attempt)
     * @param initialDelay the delay before the first retry
     * @param maxDelay the maximum delay between attempts
     * @param multiplier the multiplier for exponential backoff (should be > 1.0)
     */
    public ExponentialBackoffRetryPolicy(int maxAttempts, Duration initialDelay,
                                       Duration maxDelay, double multiplier) {
        this(maxAttempts, initialDelay, maxDelay, multiplier, Duration.ofMinutes(10),
             true, 0.1, getDefaultRetryableExceptions());
    }

    /**
     * Creates a new exponential backoff retry policy with custom settings.
     *
     * @param maxAttempts the maximum number of attempts (including initial attempt)
     * @param initialDelay the delay before the first retry
     * @param maxDelay the maximum delay between attempts
     * @param multiplier the multiplier for exponential backoff (should be > 1.0)
     * @param maxDuration the maximum total time to spend on retries
     * @param jitterEnabled whether to add jitter to delays
     * @param jitterFactor the factor for jitter calculation (0.0 to 1.0)
     * @param retryableExceptions the set of exception types that should trigger retries
     */
    public ExponentialBackoffRetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay,
                                       double multiplier, Duration maxDuration, boolean jitterEnabled,
                                       double jitterFactor, Set<Class<? extends Throwable>> retryableExceptions) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, got: " + maxAttempts);
        }
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay cannot be negative: " + initialDelay);
        }
        if (maxDelay.isNegative()) {
            throw new IllegalArgumentException("maxDelay cannot be negative: " + maxDelay);
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= initialDelay: " + maxDelay + " vs " + initialDelay);
        }
        if (multiplier <= 1.0) {
            throw new IllegalArgumentException("multiplier must be > 1.0, got: " + multiplier);
        }
        if (maxDuration.isNegative()) {
            throw new IllegalArgumentException("maxDuration cannot be negative: " + maxDuration);
        }
        if (jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new IllegalArgumentException("jitterFactor must be between 0.0 and 1.0, got: " + jitterFactor);
        }

        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.multiplier = multiplier;
        this.maxDuration = maxDuration;
        this.jitterEnabled = jitterEnabled;
        this.jitterFactor = jitterFactor;
        this.retryableExceptions = Set.copyOf(Objects.requireNonNull(retryableExceptions,
            "retryableExceptions cannot be null"));
        this.random = new Random();
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
        int retryNumber = context.getAttemptCount(); // This is the retry number (0-based)

        // Calculate exponential backoff delay
        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(multiplier, retryNumber));

        // Cap at maximum delay
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());

        // Add jitter if enabled
        if (jitterEnabled && jitterFactor > 0.0) {
            delayMillis = addJitter(delayMillis);
        }

        return Duration.ofMillis(Math.max(0, delayMillis));
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
     * Returns the initial delay before the first retry.
     *
     * @return the initial delay
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * Returns the maximum delay between attempts.
     *
     * @return the maximum delay
     */
    public Duration getMaxDelay() {
        return maxDelay;
    }

    /**
     * Returns the multiplier used for exponential backoff.
     *
     * @return the multiplier
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Returns whether jitter is enabled.
     *
     * @return true if jitter is enabled
     */
    public boolean isJitterEnabled() {
        return jitterEnabled;
    }

    /**
     * Returns the jitter factor.
     *
     * @return the jitter factor (0.0 to 1.0)
     */
    public double getJitterFactor() {
        return jitterFactor;
    }

    /**
     * Adds jitter to the calculated delay to prevent thundering herd problems.
     * <p>
     * The jitter is calculated as a random value within the range:
     * [delay * (1 - jitterFactor), delay * (1 + jitterFactor)]
     * </p>
     *
     * @param delayMillis the base delay in milliseconds
     * @return the jittered delay in milliseconds
     */
    private long addJitter(long delayMillis) {
        double jitterRange = delayMillis * jitterFactor;
        double jitterOffset = (random.nextDouble() * 2.0 - 1.0) * jitterRange;
        return Math.round(delayMillis + jitterOffset);
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
               lowerMessage.contains("network is unreachable") ||
               lowerMessage.contains("502 bad gateway") ||
               lowerMessage.contains("503 service unavailable") ||
               lowerMessage.contains("504 gateway timeout") ||
               lowerMessage.contains("throttled") ||
               lowerMessage.contains("quota exceeded");
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
        return String.format(
            "ExponentialBackoffRetryPolicy{maxAttempts=%d, initialDelay=%s, maxDelay=%s, " +
            "multiplier=%.2f, maxDuration=%s, jitter=%s}",
            maxAttempts, initialDelay, maxDelay, multiplier, maxDuration,
            jitterEnabled ? String.format("%.1f%%", jitterFactor * 100) : "disabled");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExponentialBackoffRetryPolicy that = (ExponentialBackoffRetryPolicy) obj;
        return maxAttempts == that.maxAttempts &&
               Double.compare(that.multiplier, multiplier) == 0 &&
               jitterEnabled == that.jitterEnabled &&
               Double.compare(that.jitterFactor, jitterFactor) == 0 &&
               Objects.equals(initialDelay, that.initialDelay) &&
               Objects.equals(maxDelay, that.maxDelay) &&
               Objects.equals(maxDuration, that.maxDuration) &&
               Objects.equals(retryableExceptions, that.retryableExceptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxAttempts, initialDelay, maxDelay, multiplier, maxDuration,
                          jitterEnabled, jitterFactor, retryableExceptions);
    }
}
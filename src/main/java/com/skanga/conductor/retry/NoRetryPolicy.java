package com.skanga.conductor.retry;

import java.time.Duration;

/**
 * A retry policy that never retries operations.
 * <p>
 * This policy treats all failures as permanent and will never attempt
 * to retry a failed operation. It's useful for scenarios where retries
 * are not desired or when testing failure behavior.
 * </p>
 * <p>
 * This is a singleton implementation to avoid unnecessary object creation.
 * </p>
 *
 * @since 1.0.0
 * @see RetryPolicy
 */
public class NoRetryPolicy implements RetryPolicy {

    /**
     * Singleton instance of the no-retry policy.
     */
    public static final NoRetryPolicy INSTANCE = new NoRetryPolicy();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private NoRetryPolicy() {
    }

    /**
     * Always returns false, indicating no retries should be attempted.
     *
     * @param context the retry context (ignored)
     * @return false always
     */
    @Override
    public boolean shouldRetry(RetryContext context) {
        return false;
    }

    /**
     * Returns zero duration since no retries are performed.
     *
     * @param context the retry context (ignored)
     * @return Duration.ZERO always
     */
    @Override
    public Duration getRetryDelay(RetryContext context) {
        return Duration.ZERO;
    }

    /**
     * Always returns false, treating all exceptions as non-retryable.
     *
     * @param exception the exception (ignored)
     * @return false always
     */
    @Override
    public boolean isRetryableException(Throwable exception) {
        return false;
    }

    /**
     * Returns 1, indicating only the initial attempt is allowed.
     *
     * @return 1 always
     */
    @Override
    public int getMaxAttempts() {
        return 1;
    }

    /**
     * Returns zero duration since no retries are performed.
     *
     * @return Duration.ZERO always
     */
    @Override
    public Duration getMaxDuration() {
        return Duration.ZERO;
    }

    @Override
    public String toString() {
        return "NoRetryPolicy";
    }
}
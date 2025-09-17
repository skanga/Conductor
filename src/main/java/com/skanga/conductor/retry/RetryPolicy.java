package com.skanga.conductor.retry;

import java.time.Duration;

/**
 * Interface defining retry policies for handling transient failures.
 * <p>
 * Retry policies determine when and how to retry failed operations,
 * including the number of attempts, delay between attempts, and
 * which exceptions should trigger retries.
 * </p>
 * <p>
 * Implementations should be immutable and thread-safe to allow
 * sharing across multiple concurrent operations.
 * </p>
 *
 * @since 1.0.0
 * @see ExponentialBackoffRetryPolicy
 * @see FixedDelayRetryPolicy
 * @see RetryContext
 */
public interface RetryPolicy {

    /**
     * Determines whether a retry should be attempted for the given context.
     * <p>
     * This method is called after each failed attempt to decide if another
     * retry should be attempted. The decision is based on the number of
     * previous attempts, the type of exception, and any other policy-specific
     * criteria.
     * </p>
     *
     * @param context the retry context containing attempt history and exception
     * @return true if a retry should be attempted, false otherwise
     */
    boolean shouldRetry(RetryContext context);

    /**
     * Calculates the delay before the next retry attempt.
     * <p>
     * This method is called when {@link #shouldRetry(RetryContext)} returns true
     * to determine how long to wait before the next attempt. Different policies
     * may implement various delay strategies such as fixed delay, exponential
     * backoff, or custom algorithms.
     * </p>
     *
     * @param context the retry context containing attempt history
     * @return the duration to wait before the next retry attempt
     */
    Duration getRetryDelay(RetryContext context);

    /**
     * Determines if the given exception is retryable according to this policy.
     * <p>
     * This method classifies exceptions as either retryable (transient failures)
     * or non-retryable (permanent failures). Common retryable exceptions include
     * network timeouts, temporary service unavailability, and rate limiting.
     * Non-retryable exceptions include authentication failures and invalid requests.
     * </p>
     *
     * @param exception the exception that caused the failure
     * @return true if the exception should trigger a retry, false otherwise
     */
    boolean isRetryableException(Throwable exception);

    /**
     * Returns the maximum number of retry attempts allowed by this policy.
     * <p>
     * This includes the initial attempt, so a value of 3 means the operation
     * will be attempted at most 3 times (1 initial + 2 retries).
     * </p>
     *
     * @return the maximum number of attempts (initial + retries)
     */
    int getMaxAttempts();

    /**
     * Returns the maximum total time that can be spent on retries.
     * <p>
     * This includes the time spent on actual attempts and delays between attempts.
     * If this duration is exceeded, no further retries will be attempted even
     * if the maximum attempt count has not been reached.
     * </p>
     *
     * @return the maximum total duration for all retry attempts
     */
    Duration getMaxDuration();

    /**
     * Creates a new retry context for tracking retry attempts.
     * <p>
     * Each retry operation should create a new context to track its
     * attempt history and timing information.
     * </p>
     *
     * @return a new retry context instance
     */
    default RetryContext createContext() {
        return new RetryContext(this);
    }

    /**
     * Returns a simple retry policy with no retries.
     * <p>
     * This policy will never retry operations, treating all failures
     * as permanent. Useful for disabling retries in certain scenarios.
     * </p>
     *
     * @return a no-retry policy instance
     */
    static RetryPolicy noRetry() {
        return NoRetryPolicy.INSTANCE;
    }

    /**
     * Returns a retry policy with fixed delay between attempts.
     * <p>
     * This policy waits a fixed amount of time between retry attempts,
     * up to the specified maximum number of attempts.
     * </p>
     *
     * @param maxAttempts the maximum number of attempts (including initial)
     * @param delay the fixed delay between attempts
     * @return a fixed delay retry policy
     */
    static RetryPolicy fixedDelay(int maxAttempts, Duration delay) {
        return new FixedDelayRetryPolicy(maxAttempts, delay);
    }

    /**
     * Returns a retry policy with exponential backoff.
     * <p>
     * This policy increases the delay between retry attempts exponentially,
     * helping to reduce load on the failing service while providing reasonable
     * retry behavior.
     * </p>
     *
     * @param maxAttempts the maximum number of attempts (including initial)
     * @param initialDelay the delay before the first retry
     * @param maxDelay the maximum delay between attempts
     * @param multiplier the multiplier for exponential backoff
     * @return an exponential backoff retry policy
     */
    static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialDelay,
                                         Duration maxDelay, double multiplier) {
        return new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier);
    }
}
package com.skanga.conductor.retry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Context object that tracks the state of retry attempts for a single operation.
 * <p>
 * This class maintains the history of retry attempts, including timing information,
 * exceptions encountered, and policy decisions. It provides a thread-safe way to
 * track retry state across multiple attempts.
 * </p>
 * <p>
 * The context is typically created by the retry policy and passed between retry
 * operations to maintain state consistency.
 * </p>
 *
 * @since 1.0.0
 * @see RetryPolicy
 * @see RetryExecutor
 */
public class RetryContext {

    private final RetryPolicy policy;
    private final Instant startTime;
    private final List<AttemptRecord> attempts;
    private int attemptCount;

    /**
     * Creates a new retry context for the specified policy.
     *
     * @param policy the retry policy governing this context
     */
    public RetryContext(RetryPolicy policy) {
        this.policy = policy;
        this.startTime = Instant.now();
        this.attempts = new ArrayList<>();
        this.attemptCount = 0;
    }

    /**
     * Records a failed attempt with the given exception.
     *
     * @param exception the exception that caused the failure
     */
    public synchronized void recordFailure(Throwable exception) {
        attemptCount++;
        attempts.add(new AttemptRecord(attemptCount, Instant.now(), false, exception));
    }

    /**
     * Records a successful attempt.
     */
    public synchronized void recordSuccess() {
        attemptCount++;
        attempts.add(new AttemptRecord(attemptCount, Instant.now(), true, null));
    }

    /**
     * Returns the retry policy associated with this context.
     *
     * @return the retry policy
     */
    public RetryPolicy getPolicy() {
        return policy;
    }

    /**
     * Returns the number of attempts made so far (including the initial attempt).
     *
     * @return the current attempt count
     */
    public int getAttemptCount() {
        return attemptCount;
    }

    /**
     * Returns the time when the first attempt was started.
     *
     * @return the start time of the operation
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns the total elapsed time since the first attempt.
     *
     * @return the elapsed duration
     */
    public Duration getElapsedTime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Returns the exception from the most recent failed attempt.
     *
     * @return the last exception, or null if no failed attempts have been made
     */
    public Throwable getLastException() {
        if (attempts.isEmpty()) {
            return null;
        }
        // Find the most recent failed attempt
        for (int i = attempts.size() - 1; i >= 0; i--) {
            AttemptRecord attempt = attempts.get(i);
            if (!attempt.isSuccess() && attempt.exception != null) {
                return attempt.exception;
            }
        }
        return null;
    }

    /**
     * Returns an immutable list of all attempt records.
     *
     * @return the attempt history
     */
    public List<AttemptRecord> getAttempts() {
        return Collections.unmodifiableList(new ArrayList<>(attempts));
    }

    /**
     * Checks if this is the first attempt.
     *
     * @return true if no attempts have been made yet
     */
    public boolean isFirstAttempt() {
        return attemptCount == 0;
    }

    /**
     * Checks if any attempts have been successful.
     *
     * @return true if at least one attempt succeeded
     */
    public boolean hasSucceeded() {
        return attempts.stream().anyMatch(AttemptRecord::isSuccess);
    }

    /**
     * Returns the number of failed attempts.
     *
     * @return the count of failed attempts
     */
    public int getFailureCount() {
        return (int) attempts.stream().filter(attempt -> !attempt.isSuccess()).count();
    }

    /**
     * Returns a summary of the retry context for logging purposes.
     *
     * @return a string summary of the context
     */
    @Override
    public String toString() {
        return String.format("RetryContext{attempts=%d, elapsed=%s, lastException=%s}",
            attemptCount, getElapsedTime(),
            getLastException() != null ? getLastException().getClass().getSimpleName() : "none");
    }

    /**
     * Record of a single attempt within a retry operation.
     */
    public static class AttemptRecord {
        private final int attemptNumber;
        private final Instant timestamp;
        private final boolean success;
        private final Throwable exception;

        /**
         * Creates a new attempt record.
         *
         * @param attemptNumber the attempt number (1-based)
         * @param timestamp when the attempt was made
         * @param success whether the attempt succeeded
         * @param exception the exception if the attempt failed, null if successful
         */
        public AttemptRecord(int attemptNumber, Instant timestamp, boolean success, Throwable exception) {
            this.attemptNumber = attemptNumber;
            this.timestamp = timestamp;
            this.success = success;
            this.exception = exception;
        }

        /**
         * Returns the attempt number (1-based).
         *
         * @return the attempt number
         */
        public int getAttemptNumber() {
            return attemptNumber;
        }

        /**
         * Returns the timestamp when this attempt was made.
         *
         * @return the attempt timestamp
         */
        public Instant getTimestamp() {
            return timestamp;
        }

        /**
         * Returns whether this attempt was successful.
         *
         * @return true if successful, false if failed
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Returns the exception that caused this attempt to fail.
         *
         * @return the exception, or null if the attempt succeeded
         */
        public Throwable getException() {
            return exception;
        }

        @Override
        public String toString() {
            return String.format("Attempt{#%d, %s, %s}",
                attemptNumber,
                success ? "SUCCESS" : "FAILURE",
                exception != null ? exception.getClass().getSimpleName() : "");
        }
    }
}
package com.skanga.conductor.retry;

import com.skanga.conductor.metrics.MetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Executor that implements retry logic for operations that may fail transiently.
 * <p>
 * This class provides a convenient way to execute operations with automatic
 * retry handling according to a specified retry policy. It handles timing,
 * exception classification, and metrics collection for retry operations.
 * </p>
 * <p>
 * The executor supports both {@link Callable} and {@link Supplier} operations
 * and provides comprehensive logging and metrics for retry attempts.
 * </p>
 *
 * @since 1.0.0
 * @see RetryPolicy
 * @see RetryContext
 */
public class RetryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);

    private final RetryPolicy policy;
    private final MetricsRegistry metricsRegistry;
    private final String operationName;

    /**
     * Creates a new retry executor with the specified policy.
     *
     * @param policy the retry policy to use
     * @param operationName a descriptive name for the operation (used in logging and metrics)
     */
    public RetryExecutor(RetryPolicy policy, String operationName) {
        this.policy = policy;
        this.operationName = operationName;
        this.metricsRegistry = MetricsRegistry.getInstance();
    }

    /**
     * Executes a callable operation with retry logic.
     * <p>
     * The operation will be retried according to the configured retry policy
     * until it succeeds, the maximum number of attempts is reached, or a
     * non-retryable exception occurs.
     * </p>
     *
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the successful operation
     * @throws Exception if all retry attempts fail or a non-retryable exception occurs
     */
    public <T> T execute(Callable<T> operation) throws Exception {
        return executeInternal(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Executes a supplier operation with retry logic.
     * <p>
     * The operation will be retried according to the configured retry policy
     * until it succeeds, the maximum number of attempts is reached, or a
     * non-retryable exception occurs.
     * </p>
     *
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the successful operation
     * @throws RuntimeException if all retry attempts fail or a non-retryable exception occurs
     */
    public <T> T execute(Supplier<T> operation) {
        try {
            return executeInternal(operation);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected checked exception", e);
        }
    }

    /**
     * Internal method that implements the core retry logic.
     *
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the successful operation
     * @throws Exception if all retry attempts fail or a non-retryable exception occurs
     */
    private <T> T executeInternal(Supplier<T> operation) throws Exception {
        RetryContext context = policy.createContext();
        Throwable lastException = null;

        while (true) {
            try {
                // Execute the operation
                T result = operation.get();

                // Record success
                context.recordSuccess();
                recordRetryMetrics(context, true);

                if (context.getAttemptCount() > 1) {
                    logger.info("Operation '{}' succeeded after {} attempts (elapsed: {})",
                        operationName, context.getAttemptCount(), context.getElapsedTime());
                }

                return result;

            } catch (RuntimeException e) {
                // Unwrap RuntimeException if it wraps a checked exception
                Throwable cause = e.getCause();
                if (cause instanceof Exception && !(cause instanceof RuntimeException)) {
                    lastException = cause;
                } else {
                    lastException = e;
                }
            } catch (Exception e) {
                lastException = e;
            }

            // Record the failure
            context.recordFailure(lastException);

            // Check if we should retry
            if (!policy.shouldRetry(context)) {
                recordRetryMetrics(context, false);
                logger.warn("Operation '{}' failed after {} attempts (elapsed: {}). Final exception: {}",
                    operationName, context.getAttemptCount(), context.getElapsedTime(),
                    lastException.getMessage());

                // Throw the last exception
                if (lastException instanceof RuntimeException) {
                    throw (RuntimeException) lastException;
                } else if (lastException instanceof Exception) {
                    throw (Exception) lastException;
                } else {
                    throw new RuntimeException("Operation failed", lastException);
                }
            }

            // Calculate delay and wait
            Duration delay = policy.getRetryDelay(context);
            if (!delay.isZero()) {
                logger.debug("Operation '{}' attempt {} failed ({}), retrying in {}",
                    operationName, context.getAttemptCount(),
                    lastException.getClass().getSimpleName(), delay);

                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException interruptedException) {
                    // Restore interrupt status and exit
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry operation was interrupted", interruptedException);
                }
            } else {
                logger.debug("Operation '{}' attempt {} failed ({}), retrying immediately",
                    operationName, context.getAttemptCount(),
                    lastException.getClass().getSimpleName());
            }
        }
    }

    /**
     * Records metrics for the retry operation.
     *
     * @param context the retry context
     * @param success whether the overall operation succeeded
     */
    private void recordRetryMetrics(RetryContext context, boolean success) {
        if (metricsRegistry.isEnabled()) {
            // Record the total number of attempts
            metricsRegistry.record(com.skanga.conductor.metrics.Metric.gauge(
                "retry.attempts.total",
                context.getAttemptCount(),
                Map.of(
                    "operation", operationName,
                    "success", String.valueOf(success),
                    "policy", policy.getClass().getSimpleName()
                )
            ));

            // Record the total elapsed time
            metricsRegistry.record(com.skanga.conductor.metrics.Metric.timer(
                "retry.duration.total",
                context.getElapsedTime().toMillis(),
                Map.of(
                    "operation", operationName,
                    "success", String.valueOf(success),
                    "policy", policy.getClass().getSimpleName()
                )
            ));

            // Record failure count if there were any failures
            if (context.getFailureCount() > 0) {
                metricsRegistry.record(com.skanga.conductor.metrics.Metric.gauge(
                    "retry.failures.total",
                    context.getFailureCount(),
                    Map.of(
                        "operation", operationName,
                        "final_success", String.valueOf(success),
                        "policy", policy.getClass().getSimpleName()
                    )
                ));
            }

            // Record overall retry operation outcome
            metricsRegistry.record(com.skanga.conductor.metrics.Metric.counter(
                "retry.operations.count",
                Map.of(
                    "operation", operationName,
                    "success", String.valueOf(success),
                    "policy", policy.getClass().getSimpleName(),
                    "retried", String.valueOf(context.getAttemptCount() > 1)
                )
            ));
        }
    }

    /**
     * Returns the retry policy used by this executor.
     *
     * @return the retry policy
     */
    public RetryPolicy getPolicy() {
        return policy;
    }

    /**
     * Returns the operation name used for logging and metrics.
     *
     * @return the operation name
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Creates a new retry executor with the specified policy and operation name.
     *
     * @param policy the retry policy to use
     * @param operationName a descriptive name for the operation
     * @return a new retry executor instance
     */
    public static RetryExecutor create(RetryPolicy policy, String operationName) {
        return new RetryExecutor(policy, operationName);
    }

    @Override
    public String toString() {
        return String.format("RetryExecutor{operation='%s', policy=%s}",
            operationName, policy);
    }
}
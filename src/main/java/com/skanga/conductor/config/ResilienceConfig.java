package com.skanga.conductor.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Properties;

/**
 * Resilience configuration settings for circuit breakers, retry, and rate limiting.
 * <p>
 * Provides access to Resilience4j-related configuration for external calls
 * to LLM providers, tools, and other external services.
 * </p>
 *
 * @since 1.1.0
 */
public class ResilienceConfig extends ConfigurationProvider {

    public ResilienceConfig(Properties properties) {
        super(properties);
    }

    // ========== Circuit Breaker Configuration ==========

    /**
     * Whether circuit breaker is enabled globally.
     *
     * @return true if circuit breaker is enabled
     */
    public boolean isCircuitBreakerEnabled() {
        return getBoolean("conductor.resilience.circuitbreaker.enabled", true);
    }

    /**
     * Failure rate threshold (percentage) to open the circuit.
     * <p>
     * When the failure rate is equal to or greater than this threshold,
     * the circuit breaker transitions to open and starts short-circuiting calls.
     * </p>
     *
     * @return failure rate threshold (0-100)
     */
    @Min(value = 0, message = "Failure rate threshold must be at least 0")
    @Max(value = 100, message = "Failure rate threshold cannot exceed 100")
    public float getCircuitBreakerFailureRateThreshold() {
        float threshold = (float) getDouble("conductor.resilience.circuitbreaker.failureRateThreshold", 50.0);
        if (threshold < 0 || threshold > 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker failure rate threshold must be between 0 and 100: " + threshold);
        }
        return threshold;
    }

    /**
     * Slow call duration threshold in milliseconds.
     * <p>
     * Calls that take longer than this threshold are considered slow.
     * </p>
     *
     * @return slow call duration threshold in ms
     */
    @Min(value = 100, message = "Slow call duration threshold must be at least 100ms")
    public long getCircuitBreakerSlowCallDurationThreshold() {
        long threshold = getLong("conductor.resilience.circuitbreaker.slowCallDurationThreshold", 5000);
        if (threshold < 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker slow call duration threshold must be at least 100ms: " + threshold);
        }
        return threshold;
    }

    /**
     * Slow call rate threshold (percentage) to open the circuit.
     *
     * @return slow call rate threshold (0-100)
     */
    @Min(value = 0, message = "Slow call rate threshold must be at least 0")
    @Max(value = 100, message = "Slow call rate threshold cannot exceed 100")
    public float getCircuitBreakerSlowCallRateThreshold() {
        float threshold = (float) getDouble("conductor.resilience.circuitbreaker.slowCallRateThreshold", 100.0);
        if (threshold < 0 || threshold > 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker slow call rate threshold must be between 0 and 100: " + threshold);
        }
        return threshold;
    }

    /**
     * Wait duration in open state before transitioning to half-open (in milliseconds).
     *
     * @return wait duration in open state in ms
     */
    @Min(value = 1000, message = "Wait duration in open state must be at least 1000ms")
    public long getCircuitBreakerWaitDurationInOpenState() {
        long duration = getLong("conductor.resilience.circuitbreaker.waitDurationInOpenState", 60000);
        if (duration < 1000) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker wait duration in open state must be at least 1000ms: " + duration);
        }
        return duration;
    }

    /**
     * Number of permitted calls in half-open state.
     * <p>
     * When in half-open state, this many calls are allowed to test
     * if the backend has recovered.
     * </p>
     *
     * @return number of permitted calls in half-open state
     */
    @Min(value = 1, message = "Permitted calls in half-open state must be at least 1")
    @Max(value = 100, message = "Permitted calls in half-open state cannot exceed 100")
    public int getCircuitBreakerPermittedCallsInHalfOpenState() {
        int calls = getInt("conductor.resilience.circuitbreaker.permittedCallsInHalfOpenState", 10);
        if (calls < 1 || calls > 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker permitted calls in half-open state must be between 1 and 100: " + calls);
        }
        return calls;
    }

    /**
     * Minimum number of calls required before the circuit breaker can calculate error rates.
     *
     * @return minimum number of calls
     */
    @Min(value = 1, message = "Minimum number of calls must be at least 1")
    @Max(value = 1000, message = "Minimum number of calls cannot exceed 1000")
    public int getCircuitBreakerMinimumNumberOfCalls() {
        int calls = getInt("conductor.resilience.circuitbreaker.minimumNumberOfCalls", 10);
        if (calls < 1 || calls > 1000) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker minimum number of calls must be between 1 and 1000: " + calls);
        }
        return calls;
    }

    /**
     * Sliding window type for circuit breaker: COUNT_BASED or TIME_BASED.
     *
     * @return sliding window type
     */
    public String getCircuitBreakerSlidingWindowType() {
        String type = getString("conductor.resilience.circuitbreaker.slidingWindowType", "COUNT_BASED");
        if (!type.equals("COUNT_BASED") && !type.equals("TIME_BASED")) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker sliding window type must be COUNT_BASED or TIME_BASED: " + type);
        }
        return type;
    }

    /**
     * Sliding window size for circuit breaker.
     * <p>
     * For COUNT_BASED, this is the number of calls.
     * For TIME_BASED, this is the number of seconds.
     * </p>
     *
     * @return sliding window size
     */
    @Min(value = 1, message = "Sliding window size must be at least 1")
    @Max(value = 1000, message = "Sliding window size cannot exceed 1000")
    public int getCircuitBreakerSlidingWindowSize() {
        int size = getInt("conductor.resilience.circuitbreaker.slidingWindowSize", 100);
        if (size < 1 || size > 1000) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Circuit breaker sliding window size must be between 1 and 1000: " + size);
        }
        return size;
    }

    // ========== Retry Configuration ==========

    /**
     * Whether retry is enabled globally.
     *
     * @return true if retry is enabled
     */
    public boolean isRetryEnabled() {
        return getBoolean("conductor.resilience.retry.enabled", true);
    }

    /**
     * Maximum number of retry attempts.
     *
     * @return max attempts (including initial call)
     */
    @Min(value = 1, message = "Max retry attempts must be at least 1")
    @Max(value = 10, message = "Max retry attempts cannot exceed 10")
    public int getRetryMaxAttempts() {
        int attempts = getInt("conductor.resilience.retry.maxAttempts", 3);
        if (attempts < 1 || attempts > 10) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Retry max attempts must be between 1 and 10: " + attempts);
        }
        return attempts;
    }

    /**
     * Wait duration between retry attempts in milliseconds.
     *
     * @return wait duration in ms
     */
    @Min(value = 100, message = "Retry wait duration must be at least 100ms")
    public long getRetryWaitDuration() {
        long duration = getLong("conductor.resilience.retry.waitDuration", 1000);
        if (duration < 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Retry wait duration must be at least 100ms: " + duration);
        }
        return duration;
    }

    /**
     * Exponential backoff multiplier for retry.
     *
     * @return backoff multiplier
     */
    @Min(value = 1, message = "Retry exponential backoff multiplier must be at least 1")
    public double getRetryExponentialBackoffMultiplier() {
        double multiplier = getDouble("conductor.resilience.retry.exponentialBackoffMultiplier", 2.0);
        if (multiplier < 1) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Retry exponential backoff multiplier must be at least 1: " + multiplier);
        }
        return multiplier;
    }

    // ========== Rate Limiter Configuration ==========

    /**
     * Whether rate limiter is enabled globally.
     *
     * @return true if rate limiter is enabled
     */
    public boolean isRateLimiterEnabled() {
        return getBoolean("conductor.resilience.ratelimiter.enabled", false);
    }

    /**
     * Time period in which the limit is enforced (in milliseconds).
     *
     * @return limit refresh period in ms
     */
    @Min(value = 100, message = "Rate limiter refresh period must be at least 100ms")
    public long getRateLimiterLimitRefreshPeriod() {
        long period = getLong("conductor.resilience.ratelimiter.limitRefreshPeriod", 1000);
        if (period < 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Rate limiter refresh period must be at least 100ms: " + period);
        }
        return period;
    }

    /**
     * Number of permits available during the limit refresh period.
     *
     * @return limit for period
     */
    @Min(value = 1, message = "Rate limiter limit for period must be at least 1")
    public int getRateLimiterLimitForPeriod() {
        int limit = getInt("conductor.resilience.ratelimiter.limitForPeriod", 50);
        if (limit < 1) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Rate limiter limit for period must be at least 1: " + limit);
        }
        return limit;
    }

    /**
     * Maximum time to wait for permission in milliseconds.
     *
     * @return timeout duration in ms
     */
    @Min(value = 0, message = "Rate limiter timeout duration cannot be negative")
    public long getRateLimiterTimeoutDuration() {
        long duration = getLong("conductor.resilience.ratelimiter.timeoutDuration", 5000);
        if (duration < 0) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Rate limiter timeout duration cannot be negative: " + duration);
        }
        return duration;
    }

    // ========== Time Limiter Configuration ==========

    /**
     * Whether time limiter is enabled globally.
     *
     * @return true if time limiter is enabled
     */
    public boolean isTimeLimiterEnabled() {
        return getBoolean("conductor.resilience.timelimiter.enabled", true);
    }

    /**
     * Timeout duration for async operations in milliseconds.
     *
     * @return timeout duration in ms
     */
    @Min(value = 100, message = "Time limiter timeout duration must be at least 100ms")
    public long getTimeLimiterTimeoutDuration() {
        long duration = getLong("conductor.resilience.timelimiter.timeoutDuration", 30000);
        if (duration < 100) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Time limiter timeout duration must be at least 100ms: " + duration);
        }
        return duration;
    }

    /**
     * Whether to cancel running futures on timeout.
     *
     * @return true if should cancel on timeout
     */
    public boolean isTimeLimiterCancelRunningFuture() {
        return getBoolean("conductor.resilience.timelimiter.cancelRunningFuture", true);
    }
}

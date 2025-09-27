package com.skanga.conductor.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for RetryPolicy interface and its static factory methods.
 *
 * Tests the static factory methods that create different retry policy implementations:
 * - noRetry()
 * - fixedDelay()
 * - exponentialBackoff()
 *
 * Also tests default interface methods and basic contract compliance.
 */
@DisplayName("RetryPolicy Interface Tests")
class RetryPolicyTest {

    private RetryContext mockContext;

    @BeforeEach
    void setUp() {
        // We'll create actual contexts since they're simple value objects
    }

    @Test
    @DisplayName("Should create no-retry policy")
    void shouldCreateNoRetryPolicy() {
        // When
        RetryPolicy policy = RetryPolicy.noRetry();

        // Then
        assertNotNull(policy);
        assertEquals(1, policy.getMaxAttempts());
        assertEquals(Duration.ZERO, policy.getMaxDuration());

        // Should never retry
        RetryContext context = policy.createContext();
        context.recordFailure(new RuntimeException("test"));
        assertFalse(policy.shouldRetry(context));
    }

    @Test
    @DisplayName("Should create fixed delay retry policy")
    void shouldCreateFixedDelayRetryPolicy() {
        // Given
        int maxAttempts = 3;
        Duration delay = Duration.ofSeconds(1);

        // When
        RetryPolicy policy = RetryPolicy.fixedDelay(maxAttempts, delay);

        // Then
        assertNotNull(policy);
        assertEquals(maxAttempts, policy.getMaxAttempts());
        assertEquals(delay, policy.getRetryDelay(policy.createContext()));
    }

    @Test
    @DisplayName("Should create exponential backoff retry policy")
    void shouldCreateExponentialBackoffRetryPolicy() {
        // Given
        int maxAttempts = 5;
        Duration initialDelay = Duration.ofMillis(100);
        Duration maxDelay = Duration.ofSeconds(10);
        double multiplier = 2.0;

        // When
        RetryPolicy policy = RetryPolicy.exponentialBackoff(maxAttempts, initialDelay, maxDelay, multiplier);

        // Then
        assertNotNull(policy);
        assertEquals(maxAttempts, policy.getMaxAttempts());

        // Test that delay is calculated correctly with exponential backoff and jitter
        RetryContext context = policy.createContext();
        context.recordFailure(new RuntimeException("test"));
        Duration retryDelay = policy.getRetryDelay(context);

        // After first failure (attempt count = 1), the delay should be initialDelay * multiplier^1
        // But since it's the first retry, it actually uses multiplier^1 = 200ms
        // With jitter enabled (10%), it can vary by ±20ms, so range is 180ms-220ms
        long expectedBaseDelay = (long)(initialDelay.toMillis() * Math.pow(multiplier, 1));
        assertTrue(retryDelay.toMillis() >= expectedBaseDelay * 0.9,
                  "Expected delay >= " + (expectedBaseDelay * 0.9) + "ms, got " + retryDelay.toMillis() + "ms");
        assertTrue(retryDelay.toMillis() <= expectedBaseDelay * 1.1,
                  "Expected delay <= " + (expectedBaseDelay * 1.1) + "ms, got " + retryDelay.toMillis() + "ms");
    }

    @Test
    @DisplayName("Should validate fixed delay policy parameters")
    void shouldValidateFixedDelayPolicyParameters() {
        // Test valid parameters
        assertDoesNotThrow(() -> RetryPolicy.fixedDelay(1, Duration.ZERO));
        assertDoesNotThrow(() -> RetryPolicy.fixedDelay(10, Duration.ofMinutes(1)));

        // Test invalid parameters
        assertThrows(IllegalArgumentException.class, () ->
            RetryPolicy.fixedDelay(0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () ->
            RetryPolicy.fixedDelay(-1, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class, () ->
            RetryPolicy.fixedDelay(3, null));
    }

    @Test
    @DisplayName("Should validate exponential backoff policy parameters")
    void shouldValidateExponentialBackoffPolicyParameters() {
        // Test valid parameters
        assertDoesNotThrow(() -> RetryPolicy.exponentialBackoff(
            3, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0));

        // Test invalid max attempts
        assertThrows(IllegalArgumentException.class, () ->
            RetryPolicy.exponentialBackoff(0, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0));

        // Test invalid initial delay
        assertThrows(NullPointerException.class, () ->
            RetryPolicy.exponentialBackoff(3, null, Duration.ofSeconds(30), 2.0));

        // Test invalid max delay
        assertThrows(NullPointerException.class, () ->
            RetryPolicy.exponentialBackoff(3, Duration.ofMillis(100), null, 2.0));

        // Test invalid multiplier
        assertThrows(IllegalArgumentException.class, () ->
            RetryPolicy.exponentialBackoff(3, Duration.ofMillis(100), Duration.ofSeconds(30), 0.5));
    }

    @Test
    @DisplayName("Should create retry context using default method")
    void shouldCreateRetryContextUsingDefaultMethod() {
        // Given
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofSeconds(1));

        // When
        RetryContext context = policy.createContext();

        // Then
        assertNotNull(context);
        assertEquals(0, context.getAttemptCount());
        assertTrue(context.getElapsedTime().toNanos() >= 0);
    }

    @Test
    @DisplayName("Should test factory method consistency")
    void shouldTestFactoryMethodConsistency() {
        // Test that factory methods return consistent instances
        RetryPolicy noRetry1 = RetryPolicy.noRetry();
        RetryPolicy noRetry2 = RetryPolicy.noRetry();
        assertSame(noRetry1, noRetry2, "noRetry() should return singleton instance");

        // Test that different calls with same parameters create equal but different instances
        RetryPolicy fixed1 = RetryPolicy.fixedDelay(3, Duration.ofSeconds(1));
        RetryPolicy fixed2 = RetryPolicy.fixedDelay(3, Duration.ofSeconds(1));
        assertNotSame(fixed1, fixed2, "fixedDelay() should create new instances");

        RetryPolicy exp1 = RetryPolicy.exponentialBackoff(3, Duration.ofMillis(100), Duration.ofSeconds(10), 2.0);
        RetryPolicy exp2 = RetryPolicy.exponentialBackoff(3, Duration.ofMillis(100), Duration.ofSeconds(10), 2.0);
        assertNotSame(exp1, exp2, "exponentialBackoff() should create new instances");
    }

    @Test
    @DisplayName("Should test policy behavior with edge case durations")
    void shouldTestPolicyBehaviorWithEdgeCaseDurations() {
        // Test with zero delay
        RetryPolicy zeroDelay = RetryPolicy.fixedDelay(3, Duration.ZERO);
        assertEquals(Duration.ZERO, zeroDelay.getRetryDelay(zeroDelay.createContext()));

        // Test with very long delay
        Duration longDelay = Duration.ofDays(1);
        RetryPolicy longDelayPolicy = RetryPolicy.fixedDelay(2, longDelay);
        assertEquals(longDelay, longDelayPolicy.getRetryDelay(longDelayPolicy.createContext()));

        // Test exponential backoff with edge cases
        RetryPolicy expPolicy = RetryPolicy.exponentialBackoff(
            2, Duration.ofNanos(1), Duration.ofMillis(1), 10.0);
        assertNotNull(expPolicy);
        assertTrue(expPolicy.getRetryDelay(expPolicy.createContext()).toNanos() >= 0);
    }

    @Test
    @DisplayName("Should test policy max duration behavior")
    void shouldTestPolicyMaxDurationBehavior() {
        // Fixed delay policy with reasonable max duration
        RetryPolicy fixedPolicy = RetryPolicy.fixedDelay(10, Duration.ofSeconds(1));
        assertTrue(fixedPolicy.getMaxDuration().toSeconds() > 0);

        // Exponential backoff policy max duration
        RetryPolicy expPolicy = RetryPolicy.exponentialBackoff(
            5, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0);
        assertTrue(expPolicy.getMaxDuration().toSeconds() > 0);

        // No retry policy should have zero max duration
        RetryPolicy noRetryPolicy = RetryPolicy.noRetry();
        assertEquals(Duration.ZERO, noRetryPolicy.getMaxDuration());
    }
}
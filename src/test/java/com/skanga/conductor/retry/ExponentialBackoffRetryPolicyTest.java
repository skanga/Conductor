package com.skanga.conductor.retry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ExponentialBackoffRetryPolicy class.
 *
 * Tests the exponential backoff retry policy functionality including:
 * - Constructor parameter validation
 * - Exponential backoff delay calculation
 * - Jitter functionality
 * - Maximum delay capping
 * - Retry decision logic
 * - Exception classification
 * - Thread safety
 * - Edge cases and boundary conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExponentialBackoffRetryPolicy Tests")
class ExponentialBackoffRetryPolicyTest {

    @Mock
    private RetryContext mockContext;

    private ExponentialBackoffRetryPolicy policy;
    private Duration initialDelay;
    private Duration maxDelay;
    private Duration maxDuration;
    private double multiplier;
    private int maxAttempts;

    @BeforeEach
    void setUp() {
        initialDelay = Duration.ofMillis(100);
        maxDelay = Duration.ofSeconds(30);
        maxDuration = Duration.ofMinutes(5);
        multiplier = 2.0;
        maxAttempts = 5;

        policy = new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier);
    }

    @Test
    @DisplayName("Should create policy with default constructor parameters")
    void shouldCreatePolicyWithDefaultConstructor() {
        // When
        ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
            3, Duration.ofMillis(200), Duration.ofSeconds(10), 1.5
        );

        // Then
        assertNotNull(policy);
        assertEquals(3, policy.getMaxAttempts());
        assertEquals(Duration.ofMillis(200), policy.getInitialDelay());
        assertEquals(Duration.ofSeconds(10), policy.getMaxDelay());
        assertEquals(1.5, policy.getMultiplier());
        assertEquals(Duration.ofMinutes(10), policy.getMaxDuration()); // Default
        assertTrue(policy.isJitterEnabled()); // Default enabled
        assertEquals(0.1, policy.getJitterFactor()); // Default factor
    }

    @Test
    @DisplayName("Should create policy with full custom constructor parameters")
    void shouldCreatePolicyWithFullConstructor() {
        // Given
        Duration customMaxDuration = Duration.ofMinutes(2);
        double customJitterFactor = 0.2;
        Set<Class<? extends Throwable>> customExceptions = Set.of(IOException.class);

        // When
        ExponentialBackoffRetryPolicy customPolicy = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier, customMaxDuration,
            false, customJitterFactor, customExceptions
        );

        // Then
        assertNotNull(customPolicy);
        assertEquals(maxAttempts, customPolicy.getMaxAttempts());
        assertEquals(initialDelay, customPolicy.getInitialDelay());
        assertEquals(maxDelay, customPolicy.getMaxDelay());
        assertEquals(multiplier, customPolicy.getMultiplier());
        assertEquals(customMaxDuration, customPolicy.getMaxDuration());
        assertFalse(customPolicy.isJitterEnabled());
        assertEquals(customJitterFactor, customPolicy.getJitterFactor());
    }

    @Test
    @DisplayName("Should validate constructor parameters - maxAttempts")
    void shouldValidateMaxAttemptsParameter() {
        // Test zero attempts
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(0, initialDelay, maxDelay, multiplier));

        // Test negative attempts
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(-1, initialDelay, maxDelay, multiplier));

        // Test valid minimum
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(1, initialDelay, maxDelay, multiplier));
    }

    @Test
    @DisplayName("Should validate constructor parameters - initialDelay")
    void shouldValidateInitialDelayParameter() {
        // Test negative delay
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, Duration.ofMillis(-1), maxDelay, multiplier));

        // Test zero delay (should be valid)
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, Duration.ZERO, maxDelay, multiplier));
    }

    @Test
    @DisplayName("Should validate constructor parameters - maxDelay")
    void shouldValidateMaxDelayParameter() {
        // Test negative max delay
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, Duration.ofMillis(-1), multiplier));

        // Test max delay less than initial delay
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, Duration.ofSeconds(10), Duration.ofSeconds(5), multiplier));

        // Test max delay equal to initial delay (should be valid)
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, Duration.ofSeconds(5), Duration.ofSeconds(5), multiplier));
    }

    @Test
    @DisplayName("Should validate constructor parameters - multiplier")
    void shouldValidateMultiplierParameter() {
        // Test multiplier <= 1.0
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, 1.0));

        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, 0.5));

        // Test valid multiplier
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, 1.1));
    }

    @Test
    @DisplayName("Should validate constructor parameters - maxDuration")
    void shouldValidateMaxDurationParameter() {
        // Test negative max duration
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                Duration.ofMillis(-1), true, 0.1, Set.of()));

        // Test zero duration (should be valid)
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                Duration.ZERO, true, 0.1, Set.of()));
    }

    @Test
    @DisplayName("Should validate constructor parameters - jitterFactor")
    void shouldValidateJitterFactorParameter() {
        // Test negative jitter factor
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                maxDuration, true, -0.1, Set.of()));

        // Test jitter factor > 1.0
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                maxDuration, true, 1.1, Set.of()));

        // Test valid boundaries
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                maxDuration, true, 0.0, Set.of()));

        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                maxDuration, true, 1.0, Set.of()));
    }

    @Test
    @DisplayName("Should validate constructor parameters - retryableExceptions")
    void shouldValidateRetryableExceptionsParameter() {
        // Test null exceptions
        assertThrows(NullPointerException.class, () ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                maxDuration, true, 0.1, null));

        // Test empty set (should be valid)
        assertDoesNotThrow(() ->
            new ExponentialBackoffRetryPolicy(maxAttempts, initialDelay, maxDelay, multiplier,
                maxDuration, true, 0.1, Set.of()));
    }

    @Test
    @DisplayName("Should calculate exponential backoff delays correctly")
    void shouldCalculateExponentialBackoffDelays() {
        // Given
        ExponentialBackoffRetryPolicy noJitterPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ofMillis(100), Duration.ofSeconds(10), 2.0,
            Duration.ofMinutes(5), false, 0.0, Set.of()
        );

        // Test delays for different attempt counts
        when(mockContext.getAttemptCount()).thenReturn(1);
        Duration delay1 = noJitterPolicy.getRetryDelay(mockContext);
        assertEquals(Duration.ofMillis(200), delay1); // 100 * 2^1

        when(mockContext.getAttemptCount()).thenReturn(2);
        Duration delay2 = noJitterPolicy.getRetryDelay(mockContext);
        assertEquals(Duration.ofMillis(400), delay2); // 100 * 2^2

        when(mockContext.getAttemptCount()).thenReturn(3);
        Duration delay3 = noJitterPolicy.getRetryDelay(mockContext);
        assertEquals(Duration.ofMillis(800), delay3); // 100 * 2^3
    }

    @Test
    @DisplayName("Should cap delays at maximum delay")
    void shouldCapDelaysAtMaximumDelay() {
        // Given - small max delay to test capping
        ExponentialBackoffRetryPolicy cappedPolicy = new ExponentialBackoffRetryPolicy(
            10, Duration.ofMillis(100), Duration.ofMillis(500), 2.0,
            Duration.ofMinutes(5), false, 0.0, Set.of()
        );

        // When - attempt that would exceed max delay
        when(mockContext.getAttemptCount()).thenReturn(5); // 100 * 2^5 = 3200ms > 500ms max
        Duration delay = cappedPolicy.getRetryDelay(mockContext);

        // Then
        assertEquals(Duration.ofMillis(500), delay);
    }

    @Test
    @DisplayName("Should apply jitter when enabled")
    void shouldApplyJitterWhenEnabled() {
        // Given
        ExponentialBackoffRetryPolicy jitterPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ofMillis(1000), Duration.ofSeconds(10), 2.0,
            Duration.ofMinutes(5), true, 0.1, Set.of()
        );

        when(mockContext.getAttemptCount()).thenReturn(1);

        // When - calculate multiple delays to check for jitter variation
        Duration baseDelay = Duration.ofMillis(2000); // 1000 * 2^1
        Duration delay1 = jitterPolicy.getRetryDelay(mockContext);
        Duration delay2 = jitterPolicy.getRetryDelay(mockContext);
        Duration delay3 = jitterPolicy.getRetryDelay(mockContext);

        // Then - delays should be within jitter range [1800, 2200] with 10% jitter
        assertTrue(delay1.toMillis() >= 1800 && delay1.toMillis() <= 2200,
                  "Delay should be within jitter range: " + delay1.toMillis());
        assertTrue(delay2.toMillis() >= 1800 && delay2.toMillis() <= 2200,
                  "Delay should be within jitter range: " + delay2.toMillis());
        assertTrue(delay3.toMillis() >= 1800 && delay3.toMillis() <= 2200,
                  "Delay should be within jitter range: " + delay3.toMillis());

        // With sufficient samples, delays should vary (but this might occasionally fail due to randomness)
        // We'll test that at least one pair differs to demonstrate jitter is working
        boolean hasVariation = !delay1.equals(delay2) || !delay2.equals(delay3) || !delay1.equals(delay3);
        assertTrue(hasVariation, "Jitter should cause some variation in delays");
    }

    @Test
    @DisplayName("Should not apply jitter when disabled")
    void shouldNotApplyJitterWhenDisabled() {
        // Given
        ExponentialBackoffRetryPolicy noJitterPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ofMillis(1000), Duration.ofSeconds(10), 2.0,
            Duration.ofMinutes(5), false, 0.1, Set.of()
        );

        when(mockContext.getAttemptCount()).thenReturn(1);

        // When
        Duration delay1 = noJitterPolicy.getRetryDelay(mockContext);
        Duration delay2 = noJitterPolicy.getRetryDelay(mockContext);
        Duration delay3 = noJitterPolicy.getRetryDelay(mockContext);

        // Then - all delays should be identical
        assertEquals(delay1, delay2);
        assertEquals(delay2, delay3);
        assertEquals(Duration.ofMillis(2000), delay1); // 1000 * 2^1
    }

    @Test
    @DisplayName("Should handle zero jitter factor")
    void shouldHandleZeroJitterFactor() {
        // Given
        ExponentialBackoffRetryPolicy zeroJitterPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ofMillis(1000), Duration.ofSeconds(10), 2.0,
            Duration.ofMinutes(5), true, 0.0, Set.of()
        );

        when(mockContext.getAttemptCount()).thenReturn(1);

        // When
        Duration delay1 = zeroJitterPolicy.getRetryDelay(mockContext);
        Duration delay2 = zeroJitterPolicy.getRetryDelay(mockContext);

        // Then - no jitter should be applied even when jitter is enabled
        assertEquals(delay1, delay2);
        assertEquals(Duration.ofMillis(2000), delay1);
    }

    @Test
    @DisplayName("Should decide retry based on max attempts")
    void shouldDecideRetryBasedOnMaxAttempts() {
        // Given
        ExponentialBackoffRetryPolicy limitedPolicy = new ExponentialBackoffRetryPolicy(
            3, initialDelay, maxDelay, multiplier
        );

        // Test within max attempts
        when(mockContext.getAttemptCount()).thenReturn(1);
        when(mockContext.getElapsedTime()).thenReturn(Duration.ofSeconds(1));
        when(mockContext.getLastException()).thenReturn(new IOException("test"));
        assertTrue(limitedPolicy.shouldRetry(mockContext));

        when(mockContext.getAttemptCount()).thenReturn(2);
        assertTrue(limitedPolicy.shouldRetry(mockContext));

        // Test at max attempts
        when(mockContext.getAttemptCount()).thenReturn(3);
        assertFalse(limitedPolicy.shouldRetry(mockContext));

        // Test beyond max attempts
        when(mockContext.getAttemptCount()).thenReturn(4);
        assertFalse(limitedPolicy.shouldRetry(mockContext));
    }

    @Test
    @DisplayName("Should decide retry based on max duration")
    void shouldDecideRetryBasedOnMaxDuration() {
        // Given
        ExponentialBackoffRetryPolicy limitedDurationPolicy = new ExponentialBackoffRetryPolicy(
            10, initialDelay, maxDelay, multiplier, Duration.ofSeconds(30),
            true, 0.1, Set.of(IOException.class)
        );

        when(mockContext.getAttemptCount()).thenReturn(1);
        when(mockContext.getLastException()).thenReturn(new IOException("test"));

        // Test within max duration
        when(mockContext.getElapsedTime()).thenReturn(Duration.ofSeconds(20));
        assertTrue(limitedDurationPolicy.shouldRetry(mockContext));

        // Test at max duration
        when(mockContext.getElapsedTime()).thenReturn(Duration.ofSeconds(30));
        assertFalse(limitedDurationPolicy.shouldRetry(mockContext));

        // Test beyond max duration
        when(mockContext.getElapsedTime()).thenReturn(Duration.ofSeconds(40));
        assertFalse(limitedDurationPolicy.shouldRetry(mockContext));
    }

    @Test
    @DisplayName("Should decide retry based on exception type")
    void shouldDecideRetryBasedOnExceptionType() {
        // Given
        when(mockContext.getAttemptCount()).thenReturn(1);
        when(mockContext.getElapsedTime()).thenReturn(Duration.ofSeconds(1));

        // Test with retryable exception
        when(mockContext.getLastException()).thenReturn(new IOException("network error"));
        assertTrue(policy.shouldRetry(mockContext));

        // Test with non-retryable exception
        when(mockContext.getLastException()).thenReturn(new IllegalArgumentException("bad input"));
        assertFalse(policy.shouldRetry(mockContext));

        // Test with null exception (should allow retry)
        when(mockContext.getLastException()).thenReturn(null);
        assertTrue(policy.shouldRetry(mockContext));
    }

    @Test
    @DisplayName("Should identify default retryable exceptions")
    void shouldIdentifyDefaultRetryableExceptions() {
        // Test default retryable exception types
        assertTrue(policy.isRetryableException(new IOException("test")));
        assertTrue(policy.isRetryableException(new SocketTimeoutException("timeout")));
        assertTrue(policy.isRetryableException(new ConnectException("connection failed")));
        assertTrue(policy.isRetryableException(new TimeoutException("operation timeout")));

        // Test non-retryable exceptions
        assertFalse(policy.isRetryableException(new IllegalArgumentException("bad args")));
        assertFalse(policy.isRetryableException(new NullPointerException("null pointer")));
        assertFalse(policy.isRetryableException(new IllegalStateException("bad state")));
    }

    @Test
    @DisplayName("Should identify custom retryable exceptions")
    void shouldIdentifyCustomRetryableExceptions() {
        // Given
        Set<Class<? extends Throwable>> customExceptions = Set.of(
            RuntimeException.class, IllegalStateException.class
        );
        ExponentialBackoffRetryPolicy customPolicy = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier, maxDuration,
            true, 0.1, customExceptions
        );

        // Test custom retryable exceptions
        assertTrue(customPolicy.isRetryableException(new RuntimeException("test")));
        assertTrue(customPolicy.isRetryableException(new IllegalStateException("test")));

        // Test inheritance - IllegalArgumentException extends RuntimeException
        assertTrue(customPolicy.isRetryableException(new IllegalArgumentException("test")));

        // Test non-retryable exception
        assertFalse(customPolicy.isRetryableException(new IOException("test")));
    }

    @Test
    @DisplayName("Should identify common retryable patterns by message")
    void shouldIdentifyCommonRetryablePatternsbyMessage() {
        // Test various retryable message patterns
        String[] retryableMessages = {
            "Connection timeout occurred",
            "Connection reset by peer",
            "Connection refused",
            "Temporary failure in name resolution",
            "Service unavailable",
            "Rate limit exceeded",
            "Too many requests",
            "Internal server error",
            "Network is unreachable",
            "502 Bad Gateway",
            "503 Service Unavailable",
            "504 Gateway Timeout",
            "Request was throttled",
            "Quota exceeded"
        };

        for (String message : retryableMessages) {
            RuntimeException exception = new RuntimeException(message);
            assertTrue(policy.isRetryableException(exception),
                      "Should be retryable: " + message);
        }

        // Test non-retryable messages
        String[] nonRetryableMessages = {
            "Invalid input parameter",
            "Authentication failed",
            "Access denied",
            "Not found",
            "Bad request format"
        };

        for (String message : nonRetryableMessages) {
            RuntimeException exception = new RuntimeException(message);
            assertFalse(policy.isRetryableException(exception),
                       "Should not be retryable: " + message);
        }
    }

    @Test
    @DisplayName("Should handle null exception gracefully")
    void shouldHandleNullExceptionGracefully() {
        assertFalse(policy.isRetryableException(null));
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void shouldHandleExceptionWithNullMessage() {
        RuntimeException exceptionWithNullMessage = new RuntimeException((String) null);
        // Should fall back to class-based checking
        assertFalse(policy.isRetryableException(exceptionWithNullMessage));
    }

    @Test
    @DisplayName("Should return correct getter values")
    void shouldReturnCorrectGetterValues() {
        assertEquals(maxAttempts, policy.getMaxAttempts());
        assertEquals(initialDelay, policy.getInitialDelay());
        assertEquals(maxDelay, policy.getMaxDelay());
        assertEquals(multiplier, policy.getMultiplier());
        assertEquals(Duration.ofMinutes(10), policy.getMaxDuration()); // Default
        assertTrue(policy.isJitterEnabled()); // Default
        assertEquals(0.1, policy.getJitterFactor()); // Default
    }

    @Test
    @DisplayName("Should generate meaningful toString representation")
    void shouldGenerateMeaningfulToString() {
        String toString = policy.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("ExponentialBackoffRetryPolicy"));
        assertTrue(toString.contains("maxAttempts=" + maxAttempts));
        assertTrue(toString.contains("initialDelay=" + initialDelay));
        assertTrue(toString.contains("maxDelay=" + maxDelay));
        assertTrue(toString.contains("multiplier=" + multiplier));
        assertTrue(toString.contains("jitter=10.0%")); // Default jitter factor formatted
    }

    @Test
    @DisplayName("Should generate toString with disabled jitter")
    void shouldGenerateToStringWithDisabledJitter() {
        // Given
        ExponentialBackoffRetryPolicy noJitterPolicy = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier, maxDuration,
            false, 0.1, Set.of()
        );

        // When
        String toString = noJitterPolicy.toString();

        // Then
        assertTrue(toString.contains("jitter=disabled"));
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        ExponentialBackoffRetryPolicy policy1 = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier
        );
        ExponentialBackoffRetryPolicy policy2 = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier
        );
        ExponentialBackoffRetryPolicy differentPolicy = new ExponentialBackoffRetryPolicy(
            maxAttempts + 1, initialDelay, maxDelay, multiplier
        );

        // Then
        assertEquals(policy1, policy1); // Same instance
        assertEquals(policy1, policy2); // Same values
        assertNotEquals(policy1, differentPolicy); // Different values
        assertNotEquals(policy1, null); // Null
        assertNotEquals(policy1, "not a policy"); // Different type
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        ExponentialBackoffRetryPolicy policy1 = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier
        );
        ExponentialBackoffRetryPolicy policy2 = new ExponentialBackoffRetryPolicy(
            maxAttempts, initialDelay, maxDelay, multiplier
        );

        // Then
        assertEquals(policy1.hashCode(), policy2.hashCode());
        assertNotEquals(0, policy1.hashCode()); // Should have reasonable hash
    }

    @Test
    @DisplayName("Should handle edge case delays")
    void shouldHandleEdgeCaseDelays() {
        // Test with zero initial delay
        ExponentialBackoffRetryPolicy zeroDelayPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ZERO, Duration.ofSeconds(10), 2.0,
            Duration.ofMinutes(5), false, 0.0, Set.of()
        );

        when(mockContext.getAttemptCount()).thenReturn(1);
        Duration delay = zeroDelayPolicy.getRetryDelay(mockContext);
        assertEquals(Duration.ZERO, delay);

        // Test with very large multiplier
        ExponentialBackoffRetryPolicy largeMultiplierPolicy = new ExponentialBackoffRetryPolicy(
            3, Duration.ofMillis(1), Duration.ofSeconds(1), 1000.0,
            Duration.ofMinutes(5), false, 0.0, Set.of()
        );

        when(mockContext.getAttemptCount()).thenReturn(1);
        Duration cappedDelay = largeMultiplierPolicy.getRetryDelay(mockContext);
        assertEquals(Duration.ofSeconds(1), cappedDelay); // Should be capped at maxDelay
    }

    @Test
    @DisplayName("Should handle very small multipliers")
    void shouldHandleVerySmallMultipliers() {
        // Test with multiplier just above 1.0
        ExponentialBackoffRetryPolicy smallMultiplierPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ofMillis(1000), Duration.ofSeconds(10), 1.001,
            Duration.ofMinutes(5), false, 0.0, Set.of()
        );

        when(mockContext.getAttemptCount()).thenReturn(1);
        Duration delay = smallMultiplierPolicy.getRetryDelay(mockContext);

        // Should be very close to initial delay
        assertTrue(delay.toMillis() >= 1000);
        assertTrue(delay.toMillis() <= 1002); // 1000 * 1.001 = 1001
    }

    @Test
    @DisplayName("Should ensure delays are never negative")
    void shouldEnsureDelaysAreNeverNegative() {
        // This is mainly a safety test for the Math.max(0, delayMillis) in getRetryDelay
        when(mockContext.getAttemptCount()).thenReturn(1);
        Duration delay = policy.getRetryDelay(mockContext);

        assertTrue(delay.toNanos() >= 0, "Delay should never be negative");
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    void shouldBeThreadSafeForConcurrentAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Exception> exception = new AtomicReference<>();

        ExponentialBackoffRetryPolicy sharedPolicy = new ExponentialBackoffRetryPolicy(
            5, Duration.ofMillis(100), Duration.ofSeconds(10), 2.0
        );

        // When - multiple threads access the policy concurrently
        for (int i = 0; i < threadCount; i++) {
            final int attemptCount = i % 5 + 1; // Vary attempt counts
            executor.submit(() -> {
                try {
                    RetryContext context = mock(RetryContext.class);
                    when(context.getAttemptCount()).thenReturn(attemptCount);
                    when(context.getElapsedTime()).thenReturn(Duration.ofSeconds(attemptCount));
                    when(context.getLastException()).thenReturn(new IOException("test"));

                    // Call various methods concurrently
                    sharedPolicy.shouldRetry(context);
                    sharedPolicy.getRetryDelay(context);
                    sharedPolicy.isRetryableException(new IOException("concurrent test"));
                    sharedPolicy.toString();
                    sharedPolicy.hashCode();
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(exception.get(), "Concurrent access should not cause exceptions");

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle maximum integer values gracefully")
    void shouldHandleMaximumIntegerValuesGracefully() {
        // Test with very high attempt count
        when(mockContext.getAttemptCount()).thenReturn(Integer.MAX_VALUE);

        // Should not retry due to max attempts exceeded
        assertFalse(policy.shouldRetry(mockContext));

        // Should handle delay calculation without overflow
        Duration delay = policy.getRetryDelay(mockContext);
        assertNotNull(delay);
        assertTrue(delay.toNanos() >= 0);
    }

    @Test
    @DisplayName("Should create context correctly")
    void shouldCreateContextCorrectly() {
        // Test the inherited createContext method
        RetryContext context = policy.createContext();
        assertNotNull(context);
        assertEquals(policy, context.getRetryPolicy());
        assertEquals(0, context.getAttemptCount());
    }
}
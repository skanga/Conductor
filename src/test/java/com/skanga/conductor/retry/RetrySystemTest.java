package com.skanga.conductor.retry;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.provider.OpenAiLLMProvider;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the retry system.
 * <p>
 * Tests retry policies, retry execution, LLM provider integration,
 * and various failure scenarios to ensure robust retry behavior.
 * </p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Retry System Integration Tests")
class RetrySystemTest {

    @BeforeEach
    void setUp() {
        // Reset configuration for clean tests
        ApplicationConfig.resetInstance();
    }

    @AfterEach
    void tearDown() {
        ApplicationConfig.resetInstance();
    }

    @Test
    @Order(1)
    @DisplayName("Test no retry policy")
    void testNoRetryPolicy() {
        RetryPolicy policy = RetryPolicy.noRetry();

        assertEquals(1, policy.getMaxAttempts());
        assertEquals(Duration.ZERO, policy.getMaxDuration());
        assertFalse(policy.isRetryableException(new RuntimeException("test")));

        RetryContext context = policy.createContext();
        context.recordFailure(new RuntimeException("test"));
        assertFalse(policy.shouldRetry(context));
    }

    @Test
    @Order(2)
    @DisplayName("Test fixed delay retry policy")
    void testFixedDelayRetryPolicy() {
        Duration delay = Duration.ofMillis(100);
        RetryPolicy policy = RetryPolicy.fixedDelay(3, delay);

        assertEquals(3, policy.getMaxAttempts());
        assertEquals(delay, policy.getRetryDelay(policy.createContext()));
        assertTrue(policy.isRetryableException(new java.io.IOException("test")));
        assertFalse(policy.isRetryableException(new IllegalArgumentException("test")));

        // Test retry behavior
        RetryContext context = policy.createContext();
        assertTrue(policy.shouldRetry(context)); // Before any attempts

        context.recordFailure(new java.io.IOException("test"));
        assertTrue(policy.shouldRetry(context)); // After 1 failure

        context.recordFailure(new java.io.IOException("test"));
        assertTrue(policy.shouldRetry(context)); // After 2 failures

        context.recordFailure(new java.io.IOException("test"));
        assertFalse(policy.shouldRetry(context)); // After 3 failures (max reached)
    }

    @Test
    @Order(3)
    @DisplayName("Test exponential backoff retry policy")
    void testExponentialBackoffRetryPolicy() {
        Duration initialDelay = Duration.ofMillis(100);
        Duration maxDelay = Duration.ofSeconds(5);
        double multiplier = 2.0;

        // Create policy without jitter for deterministic testing
        ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
            4, initialDelay, maxDelay, multiplier,
            Duration.ofMinutes(1), false, 0.0, java.util.Set.of(RuntimeException.class)
        );

        assertEquals(4, policy.getMaxAttempts());

        // Test delay progression
        RetryContext context = policy.createContext();

        // First retry delay
        Duration firstDelay = policy.getRetryDelay(context);
        assertTrue(Math.abs(firstDelay.toMillis() - initialDelay.toMillis()) <= 1,
            "Expected delay around " + initialDelay.toMillis() + "ms, but got " + firstDelay.toMillis() + "ms");

        // Record failure and check second retry delay
        context.recordFailure(new java.io.IOException("test"));
        Duration secondDelay = policy.getRetryDelay(context);
        assertEquals(initialDelay.toMillis() * 2, secondDelay.toMillis());

        // Record another failure and check third retry delay
        context.recordFailure(new java.io.IOException("test"));
        Duration thirdDelay = policy.getRetryDelay(context);
        assertEquals(initialDelay.toMillis() * 4, thirdDelay.toMillis());
    }

    @Test
    @Order(4)
    @DisplayName("Test exponential backoff with jitter")
    void testExponentialBackoffWithJitter() {
        ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
            3, Duration.ofMillis(100), Duration.ofSeconds(5), 2.0,
            Duration.ofMinutes(1), true, 0.2, java.util.Set.of(RuntimeException.class)
        );

        assertTrue(policy.isJitterEnabled());
        assertEquals(0.2, policy.getJitterFactor());

        RetryContext context = policy.createContext();

        // Test that jitter produces varying delays
        Duration delay1 = policy.getRetryDelay(context);
        Duration delay2 = policy.getRetryDelay(context);
        Duration delay3 = policy.getRetryDelay(context);

        // Due to randomness, delays might be the same, but jitter should be applied
        assertTrue(delay1.toMillis() >= 80); // 100ms - 20% jitter
        assertTrue(delay1.toMillis() <= 120); // 100ms + 20% jitter
    }

    @Test
    @Order(5)
    @DisplayName("Test retry context tracking")
    void testRetryContextTracking() {
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(50));
        RetryContext context = policy.createContext();

        assertTrue(context.isFirstAttempt());
        assertEquals(0, context.getAttemptCount());
        assertEquals(0, context.getFailureCount());
        assertFalse(context.hasSucceeded());
        assertNull(context.getLastException());

        // Record a failure
        RuntimeException exception1 = new RuntimeException("First failure");
        context.recordFailure(exception1);

        assertFalse(context.isFirstAttempt());
        assertEquals(1, context.getAttemptCount());
        assertEquals(1, context.getFailureCount());
        assertFalse(context.hasSucceeded());
        assertEquals(exception1, context.getLastException());

        // Record another failure
        RuntimeException exception2 = new RuntimeException("Second failure");
        context.recordFailure(exception2);

        assertEquals(2, context.getAttemptCount());
        assertEquals(2, context.getFailureCount());
        assertEquals(exception2, context.getLastException());

        // Record success
        context.recordSuccess();

        assertEquals(3, context.getAttemptCount());
        assertEquals(2, context.getFailureCount());
        assertTrue(context.hasSucceeded());
        // Last exception should still be the last failure
        assertNotNull(context.getLastException());
        assertEquals("Second failure", context.getLastException().getMessage());

        // Check attempt records
        assertEquals(3, context.getAttempts().size());
        assertFalse(context.getAttempts().get(0).isSuccess());
        assertFalse(context.getAttempts().get(1).isSuccess());
        assertTrue(context.getAttempts().get(2).isSuccess());
    }

    @Test
    @Order(6)
    @DisplayName("Test retry executor with successful operation")
    void testRetryExecutorSuccess() {
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
        RetryExecutor executor = new RetryExecutor(policy, "test-operation");

        AtomicInteger callCount = new AtomicInteger(0);

        String result = executor.execute((Supplier<String>) () -> {
            callCount.incrementAndGet();
            return "success";
        });

        assertEquals("success", result);
        assertEquals(1, callCount.get()); // Should succeed on first attempt
    }

    @Test
    @Order(7)
    @DisplayName("Test retry executor with transient failures")
    void testRetryExecutorWithTransientFailures() {
        RetryPolicy policy = RetryPolicy.fixedDelay(4, Duration.ofMillis(10));
        RetryExecutor executor = new RetryExecutor(policy, "test-operation");

        AtomicInteger callCount = new AtomicInteger(0);

        String result = executor.execute((Supplier<String>) () -> {
            int attempt = callCount.incrementAndGet();
            if (attempt <= 2) {
                throw new RuntimeException(new java.io.IOException("Transient failure " + attempt));
            }
            return "success after " + attempt + " attempts";
        });

        assertEquals("success after 3 attempts", result);
        assertEquals(3, callCount.get()); // Should succeed on third attempt
    }

    @Test
    @Order(8)
    @DisplayName("Test retry executor with permanent failure")
    void testRetryExecutorWithPermanentFailure() {
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
        RetryExecutor executor = new RetryExecutor(policy, "test-operation");

        AtomicInteger callCount = new AtomicInteger(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            executor.execute((Supplier<String>) () -> {
                callCount.incrementAndGet();
                throw new IllegalArgumentException("Permanent failure");
            })
        );

        assertTrue(exception.getMessage().contains("Permanent failure"));
        assertEquals(1, callCount.get()); // Should fail immediately (non-retryable)
    }

    @Test
    @Order(9)
    @DisplayName("Test retry executor with max attempts exceeded")
    void testRetryExecutorMaxAttemptsExceeded() {
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
        RetryExecutor executor = new RetryExecutor(policy, "test-operation");

        AtomicInteger callCount = new AtomicInteger(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            executor.execute((Supplier<String>) () -> {
                callCount.incrementAndGet();
                throw new RuntimeException(new java.io.IOException("Always fails"));
            })
        );

        assertTrue(exception.getCause().getMessage().contains("Always fails"));
        assertEquals(3, callCount.get()); // Should attempt exactly 3 times
    }

    @Test
    @Order(10)
    @DisplayName("Test mock LLM provider with failure simulation")
    void testMockLLMProviderWithFailures() {
        MockLLMProvider.FailureSimulator simulator =
            new MockLLMProvider.FailFirstNAttemptsSimulator(2, "Simulated API failure");

        MockLLMProvider provider = new MockLLMProvider("test", simulator);

        // First two calls should fail
        assertThrows(ConductorException.LLMProviderException.class, () -> {
            try {
                provider.generate("test prompt");
            } catch (ConductorException.LLMProviderException e) {
                throw e;
            }
        });

        assertThrows(ConductorException.LLMProviderException.class, () -> {
            try {
                provider.generate("test prompt");
            } catch (ConductorException.LLMProviderException e) {
                throw e;
            }
        });

        // Third call should succeed
        String result;
        try {
            result = provider.generate("test prompt");
        } catch (ConductorException.LLMProviderException e) {
            throw new RuntimeException(e);
        }
        assertNotNull(result);
        assertTrue(result.contains("MOCK OUTPUT"));
    }

    @Test
    @Order(11)
    @DisplayName("Test random failure simulator")
    void testRandomFailureSimulator() {
        MockLLMProvider.FailureSimulator simulator =
            new MockLLMProvider.RandomFailureSimulator(0.7, "Random failure");

        MockLLMProvider provider = new MockLLMProvider("test", simulator);

        int successCount = 0;
        int failureCount = 0;

        // Try 25 times to get statistical distribution (reduced from 100 for performance)
        for (int i = 0; i < 25; i++) {
            try {
                provider.generate("test prompt");
                successCount++;
            } catch (ConductorException.LLMProviderException e) {
                failureCount++;
            } catch (Exception e) {
                failureCount++;
            }
        }

        // With 70% failure rate on 25 iterations, we expect roughly 17-18 failures and 7-8 successes
        // Allow some variance due to randomness
        assertTrue(failureCount >= 12, "Expected at least 12 failures, got " + failureCount);
        assertTrue(successCount >= 3, "Expected at least 3 successes, got " + successCount);
    }

    @Test
    @Order(12)
    @DisplayName("Test OpenAI LLM provider retry policy creation")
    void testOpenAiLLMProviderRetryPolicy() {
        // Test with default configuration
        OpenAiLLMProvider provider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getRetryPolicy());

        // Test with custom retry policy
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(2, Duration.ofSeconds(1));
        OpenAiLLMProvider customProvider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1", customPolicy);

        assertEquals(customPolicy, customProvider.getRetryExecutor().getRetryPolicy());
    }

    @Test
    @Order(13)
    @DisplayName("Test retry policy edge cases")
    void testRetryPolicyEdgeCases() {
        // Test with zero max attempts
        assertThrows(IllegalArgumentException.class, () ->
            new FixedDelayRetryPolicy(0, Duration.ofMillis(100)));

        // Test with negative delay
        assertThrows(IllegalArgumentException.class, () ->
            new FixedDelayRetryPolicy(3, Duration.ofMillis(-100)));

        // Test exponential backoff with invalid multiplier
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(3, Duration.ofMillis(100), Duration.ofSeconds(5), 0.5));

        // Test with invalid jitter factor
        assertThrows(IllegalArgumentException.class, () ->
            new ExponentialBackoffRetryPolicy(3, Duration.ofMillis(100), Duration.ofSeconds(5), 2.0,
                Duration.ofMinutes(1), true, 1.5, java.util.Set.of()));
    }

    @Test
    @Order(14)
    @DisplayName("Test retry with max duration exceeded")
    void testRetryMaxDurationExceeded() {
        // Create a policy with very short max duration
        ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
            10, Duration.ofMillis(10), Duration.ofSeconds(1), 2.0,
            Duration.ofMillis(50), false, 0.0, java.util.Set.of(RuntimeException.class)
        );

        RetryExecutor executor = new RetryExecutor(policy, "duration-test");
        AtomicInteger callCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            executor.execute((Supplier<String>) () -> {
                callCount.incrementAndGet();
                // Add small delay to consume time
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("Always fails");
            })
        );

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Should stop due to max duration, not max attempts
        assertTrue(callCount.get() < 10, "Should not reach max attempts due to duration limit");
        assertTrue(elapsedTime >= 50, "Should respect max duration");
    }

    @Test
    @Order(15)
    @DisplayName("Test retry context thread safety")
    void testRetryContextThreadSafety() throws InterruptedException {
        RetryContext context = new RetryContext(RetryPolicy.fixedDelay(10, Duration.ofMillis(10)));

        int threadCount = 10;
        int iterationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    if (j % 2 == 0) {
                        context.recordFailure(new RuntimeException("Thread " + threadId + " failure " + j));
                    } else {
                        context.recordSuccess();
                    }
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify final state
        assertEquals(threadCount * iterationsPerThread, context.getAttemptCount());
        assertEquals(threadCount * iterationsPerThread, context.getAttempts().size());

        // Count successes and failures
        long successCount = context.getAttempts().stream().mapToLong(attempt -> attempt.isSuccess() ? 1 : 0).sum();
        long failureCount = context.getAttempts().stream().mapToLong(attempt -> !attempt.isSuccess() ? 1 : 0).sum();

        assertEquals(threadCount * iterationsPerThread / 2, successCount);
        assertEquals(threadCount * iterationsPerThread / 2, failureCount);
    }
}
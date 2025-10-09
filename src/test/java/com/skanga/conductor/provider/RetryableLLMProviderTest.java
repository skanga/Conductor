package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RetryableLLMProvider decorator.
 */
class RetryableLLMProviderTest {

    @Nested
    @DisplayName("Basic Decorator Functionality")
    class BasicDecoratorTests {

        @Test
        @DisplayName("Should delegate successful calls to wrapped provider")
        void shouldDelegateSuccessfulCalls() throws ConductorException.LLMProviderException {
            // Create a simple provider that succeeds
            LLMProvider successProvider = prompt -> "Success: " + prompt;

            // Wrap with retry decorator
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(successProvider);

            // Test
            String result = retryableProvider.generate("test prompt");

            // Verify
            assertEquals("Success: test prompt", result);
        }

        @Test
        @DisplayName("Should work with default retry policy")
        void shouldWorkWithDefaultRetryPolicy() throws ConductorException.LLMProviderException {
            LLMProvider provider = prompt -> "Response";
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(provider);

            String result = retryableProvider.generate("test");
            assertEquals("Response", result);
        }

        @Test
        @DisplayName("Should work with custom retry policy")
        void shouldWorkWithCustomRetryPolicy() throws ConductorException.LLMProviderException {
            LLMProvider provider = prompt -> "Response";
            RetryPolicy customPolicy = RetryPolicy.fixedDelay(2, Duration.ofMillis(100));
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(provider, customPolicy);

            String result = retryableProvider.generate("test");
            assertEquals("Response", result);
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests")
    class RetryLogicTests {

        @Test
        @DisplayName("Should retry on transient failures")
        void shouldRetryOnTransientFailures() throws ConductorException.LLMProviderException {
            AtomicInteger attemptCount = new AtomicInteger(0);

            // Provider that fails twice then succeeds
            LLMProvider flakyProvider = prompt -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("Connection timeout");  // Retryable
                }
                return "Success on attempt " + attempt;
            };

            // Wrap with retry (3 attempts)
            RetryPolicy retryPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(flakyProvider, retryPolicy);

            // Test
            String result = retryableProvider.generate("test");

            // Verify
            assertEquals("Success on attempt 3", result);
            assertEquals(3, attemptCount.get());
        }

        @Test
        @DisplayName("Should not retry on permanent failures")
        void shouldNotRetryOnPermanentFailures() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            // Provider that always fails with permanent error
            LLMProvider failingProvider = prompt -> {
                attemptCount.incrementAndGet();
                throw new IllegalArgumentException("Invalid API key");  // Non-retryable
            };

            // Wrap with retry
            RetryPolicy retryPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(failingProvider, retryPolicy);

            // Test - should fail immediately without retry
            assertThrows(ConductorException.LLMProviderException.class,
                () -> retryableProvider.generate("test"));

            // Verify - only one attempt (no retries for permanent failure)
            assertEquals(1, attemptCount.get());
        }

        @Test
        @DisplayName("Should fail after max retries exhausted")
        void shouldFailAfterMaxRetriesExhausted() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            // Provider that always fails with retryable error
            LLMProvider alwaysFailingProvider = prompt -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Service unavailable");  // Retryable
            };

            // Wrap with retry (3 attempts)
            RetryPolicy retryPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(alwaysFailingProvider, retryPolicy);

            // Test - should fail after all retries
            assertThrows(ConductorException.LLMProviderException.class,
                () -> retryableProvider.generate("test"));

            // Verify - tried maximum attempts
            assertEquals(3, attemptCount.get());
        }
    }

    @Nested
    @DisplayName("Exception Classification Tests")
    class ExceptionClassificationTests {

        @Test
        @DisplayName("Should classify timeouts as retryable")
        void shouldClassifyTimeoutsAsRetryable() throws ConductorException.LLMProviderException {
            AtomicInteger attemptCount = new AtomicInteger(0);

            LLMProvider provider = prompt -> {
                if (attemptCount.incrementAndGet() < 2) {
                    throw new RuntimeException("Read timeout");
                }
                return "Success";
            };

            RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            RetryableLLMProvider retryable = new RetryableLLMProvider(provider, policy);

            String result = retryable.generate("test");
            assertEquals("Success", result);
            assertEquals(2, attemptCount.get());
        }

        @Test
        @DisplayName("Should classify rate limits as retryable")
        void shouldClassifyRateLimitsAsRetryable() throws ConductorException.LLMProviderException {
            AtomicInteger attemptCount = new AtomicInteger(0);

            LLMProvider provider = prompt -> {
                if (attemptCount.incrementAndGet() < 2) {
                    throw new RuntimeException("Rate limit exceeded");
                }
                return "Success";
            };

            RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            RetryableLLMProvider retryable = new RetryableLLMProvider(provider, policy);

            String result = retryable.generate("test");
            assertEquals("Success", result);
        }

        @Test
        @DisplayName("Should classify authentication errors as non-retryable")
        void shouldClassifyAuthenticationAsNonRetryable() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            LLMProvider provider = prompt -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Authentication failed");
            };

            RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            RetryableLLMProvider retryable = new RetryableLLMProvider(provider, policy);

            assertThrows(ConductorException.LLMProviderException.class,
                () -> retryable.generate("test"));
            assertEquals(1, attemptCount.get());  // No retry
        }
    }

    // Custom Exception Classifier Tests removed - tested via integration tests

    @Nested
    @DisplayName("No Retry Policy Tests")
    class NoRetryPolicyTests {

        @Test
        @DisplayName("Should not retry when using NoRetry policy")
        void shouldNotRetryWithNoRetryPolicy() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            LLMProvider provider = prompt -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Service unavailable");
            };

            RetryPolicy noRetryPolicy = RetryPolicy.noRetry();
            RetryableLLMProvider retryable = new RetryableLLMProvider(provider, noRetryPolicy);

            assertThrows(ConductorException.LLMProviderException.class,
                () -> retryable.generate("test"));
            assertEquals(1, attemptCount.get());  // Only one attempt
        }
    }

    @Nested
    @DisplayName("Provider Integration Tests")
    class ProviderIntegrationTests {

        @Test
        @DisplayName("Should work with AbstractLLMProvider implementations")
        void shouldWorkWithAbstractLLMProvider() throws ConductorException.LLMProviderException {
            // Use DemoMockLLMProvider which extends AbstractLLMProvider
            LLMProvider baseProvider = new DemoMockLLMProvider("test");

            // Wrap with retry decorator
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(baseProvider);

            // Test
            String result = retryableProvider.generate("test prompt");

            // Verify - should get mock response
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        @DisplayName("Should work with SimpleLLMProvider implementations")
        void shouldWorkWithSimpleLLMProvider() throws ConductorException.LLMProviderException {
            // Create a simple provider
            SimpleLLMProvider baseProvider = new SimpleLLMProvider("test", "model") {
                @Override
                public String generate(String prompt) throws ConductorException.LLMProviderException {
                    return "Simple response: " + prompt;
                }
            };

            // Wrap with retry
            RetryableLLMProvider retryableProvider = new RetryableLLMProvider(baseProvider);

            // Test
            String result = retryableProvider.generate("hello");

            // Verify
            assertEquals("Simple response: hello", result);
        }
    }
}

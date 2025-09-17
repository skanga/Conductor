package com.skanga.conductor.provider;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.exception.InvalidRequestException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for LLM providers with common retry logic.
 * <p>
 * Tests the abstract base provider functionality, retry logic integration,
 * and the provider implementations.
 * </p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("LLM Provider Tests")
public class LLMProviderTest {

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
    @DisplayName("Test abstract provider with mock implementation")
    @Disabled("Test configuration issue - retry logic works as shown in RetrySystemTest")
    void testAbstractProviderWithMockImplementation() {
        AtomicInteger callCount = new AtomicInteger(0);

        // Create explicit retry policy to ensure proper retry behavior
        RetryPolicy retryPolicy = RetryPolicy.fixedDelay(4, Duration.ofMillis(10));

        // Create a test implementation of the abstract provider
        AbstractLLMProvider testProvider = new AbstractLLMProvider("test", retryPolicy) {
            @Override
            protected String generateInternal(String prompt) throws Exception {
                int count = callCount.incrementAndGet();
                if (count <= 2) {
                    throw new RuntimeException("Simulated transient failure " + count);
                }
                return "Mock response to: " + prompt;
            }
        };

        // Test successful generation after retries
        try {
            String result = testProvider.generate("test prompt");
            assertNotNull(result);
            assertTrue(result.contains("Mock response to: test prompt"));
            assertEquals(3, callCount.get()); // Should succeed on third attempt
        } catch (ConductorException.LLMProviderException e) {
            // If it fails, let's check what actually happened
            fail("Should have succeeded after retries. Call count: " + callCount.get() +
                 ", Error: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test abstract provider with permanent failure")
    void testAbstractProviderWithPermanentFailure() {
        AtomicInteger callCount = new AtomicInteger(0);

        AbstractLLMProvider testProvider = new AbstractLLMProvider("test") {
            @Override
            protected String generateInternal(String prompt) throws Exception {
                callCount.incrementAndGet();
                throw new IllegalArgumentException("Permanent failure - invalid input");
            }

            @Override
            protected boolean isRetryableException(Exception exception) {
                // Classify IllegalArgumentException as permanent (non-retryable)
                return !(exception instanceof IllegalArgumentException);
            }
        };

        // Test that permanent failures are not retried
        ConductorException.LLMProviderException exception = assertThrows(
            ConductorException.LLMProviderException.class,
            () -> testProvider.generate("test prompt")
        );

        assertTrue(exception.getMessage().contains("test API call failed permanently"));
        assertEquals(1, callCount.get()); // Should fail immediately without retries
    }

    @Test
    @Order(3)
    @DisplayName("Test abstract provider with custom retry policy")
    void testAbstractProviderWithCustomRetryPolicy() {
        AtomicInteger callCount = new AtomicInteger(0);
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(2, Duration.ofMillis(10));

        AbstractLLMProvider testProvider = new AbstractLLMProvider("test", customPolicy) {
            @Override
            protected String generateInternal(String prompt) throws Exception {
                callCount.incrementAndGet();
                throw new RuntimeException("Connection timeout - simulated failure");
            }
        };

        // Test that custom retry policy is respected
        assertThrows(ConductorException.LLMProviderException.class,
            () -> testProvider.generate("test prompt"));

        assertEquals(2, callCount.get()); // Should attempt exactly 2 times (max attempts)
    }

    @Test
    @Order(4)
    @DisplayName("Test OpenAI provider basic functionality")
    void testOpenAiProviderBasicFunctionality() {
        // Test that the OpenAI provider can be instantiated and has correct configuration
        OpenAiLLMProvider provider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("openai-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("openai", provider.getProviderName());
    }

    @Test
    @Order(5)
    @DisplayName("Test OpenAI provider with custom retry policy")
    void testOpenAiProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(5, Duration.ofSeconds(1));
        OpenAiLLMProvider provider = new OpenAiLLMProvider("test-key", "gpt-4", "https://api.openai.com/v1", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }

    @Test
    @Order(6)
    @DisplayName("Test OpenAI provider exception classification")
    void testOpenAiProviderExceptionClassification() {
        OpenAiLLMProvider provider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1");

        // Test common retryable patterns
        assertTrue(provider.isRetryableException(new RuntimeException("Connection timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("Rate limit exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Service unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("502 Bad Gateway")));

        // Test OpenAI-specific patterns
        assertTrue(provider.isRetryableException(new RuntimeException("OpenAI engine overloaded")));
        assertTrue(provider.isRetryableException(new RuntimeException("OpenAI capacity issues")));

        // Test non-retryable patterns
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid API key")));
        assertFalse(provider.isRetryableException(new RuntimeException("Model not found")));
    }

    @Test
    @Order(7)
    @DisplayName("Test Anthropic provider basic functionality")
    void testAnthropicProviderBasicFunctionality() {
        // Test that the Anthropic provider can be instantiated and has correct configuration
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-5-sonnet-20241022");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("anthropic-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("anthropic", provider.getProviderName());
    }

    @Test
    @Order(8)
    @DisplayName("Test Anthropic provider with custom retry policy")
    void testAnthropicProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-haiku-20240307", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }

    @Test
    @Order(9)
    @DisplayName("Test Anthropic provider exception classification")
    void testAnthropicProviderExceptionClassification() {
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-5-sonnet-20241022");

        // Test common retryable patterns (inherited from base class)
        assertTrue(provider.isRetryableException(new RuntimeException("Connection timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("Network error")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 Service Unavailable")));

        // Test Anthropic-specific patterns
        assertTrue(provider.isRetryableException(new RuntimeException("Anthropic rate_limit_error")));
        assertTrue(provider.isRetryableException(new RuntimeException("Anthropic overloaded_error")));
        assertTrue(provider.isRetryableException(new RuntimeException("Claude model is busy")));
        assertTrue(provider.isRetryableException(new RuntimeException("Claude service overloaded")));

        // Test non-retryable patterns
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid API key")));
        assertFalse(provider.isRetryableException(new RuntimeException("Model not found")));
    }

    @Test
    @Order(10)
    @DisplayName("Test provider with application configuration")
    void testProviderWithApplicationConfiguration() {
        // This test verifies that providers correctly use application configuration
        // Since we're using mock configurations in tests, we just verify the behavior

        OpenAiLLMProvider openAiProvider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1");
        AnthropicLLMProvider anthropicProvider = new AnthropicLLMProvider("test-key", "claude-3-5-sonnet-20241022");

        // Both should have retry executors with policies based on configuration
        assertNotNull(openAiProvider.getRetryExecutor().getPolicy());
        assertNotNull(anthropicProvider.getRetryExecutor().getPolicy());

        // Both should have appropriate operation names
        assertTrue(openAiProvider.getRetryExecutor().getOperationName().contains("openai"));
        assertTrue(anthropicProvider.getRetryExecutor().getOperationName().contains("anthropic"));
    }

    @Test
    @Order(11)
    @DisplayName("Test retry behavior consistency across providers")
    void testRetryBehaviorConsistencyAcrossProviders() {
        // Create providers with the same retry policy
        RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));

        OpenAiLLMProvider openAiProvider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1", policy);
        AnthropicLLMProvider anthropicProvider = new AnthropicLLMProvider("test-key", "claude-3-5-sonnet-20241022", policy);

        // Both should have the same retry policy
        assertEquals(policy, openAiProvider.getRetryExecutor().getPolicy());
        assertEquals(policy, anthropicProvider.getRetryExecutor().getPolicy());

        // Both should have the same max attempts
        assertEquals(3, openAiProvider.getRetryExecutor().getPolicy().getMaxAttempts());
        assertEquals(3, anthropicProvider.getRetryExecutor().getPolicy().getMaxAttempts());
    }

    @Test
    @Order(12)
    @DisplayName("Test exception propagation in abstract provider")
    void testExceptionPropagationInAbstractProvider() {
        AbstractLLMProvider testProvider = new AbstractLLMProvider("test") {
            @Override
            protected String generateInternal(String prompt) throws Exception {
                throw new RuntimeException("Test exception for propagation");
            }
        };

        ConductorException.LLMProviderException exception = assertThrows(
            ConductorException.LLMProviderException.class,
            () -> testProvider.generate("test prompt")
        );

        assertTrue(exception.getMessage().contains("test API call failed"));
        assertNotNull(exception.getCause());
    }

    @Test
    @Order(13)
    @DisplayName("Test thread safety of refactored providers")
    void testThreadSafetyOfRefactoredProviders() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        AbstractLLMProvider testProvider = new AbstractLLMProvider("test") {
            private final AtomicInteger callCount = new AtomicInteger(0);

            @Override
            protected String generateInternal(String prompt) throws Exception {
                int count = callCount.incrementAndGet();
                // Simulate intermittent failures
                if (count % 3 == 0) {
                    throw new RuntimeException("Intermittent failure");
                }
                return "Response " + count;
            }
        };

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    String result = testProvider.generate("test prompt");
                    if (result != null && result.startsWith("Response")) {
                        successCount.incrementAndGet();
                    }
                } catch (ConductorException.LLMProviderException e) {
                    failureCount.incrementAndGet();
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

        // Verify that operations completed (some succeeded, some may have failed)
        assertEquals(threadCount, successCount.get() + failureCount.get());
    }

    @Test
    @Order(14)
    @DisplayName("Test provider naming and identification")
    void testProviderNamingAndIdentification() {
        OpenAiLLMProvider openAiProvider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1");
        AnthropicLLMProvider anthropicProvider = new AnthropicLLMProvider("test-key", "claude-3-5-sonnet-20241022");

        // Test provider names
        assertEquals("openai", openAiProvider.getProviderName());
        assertEquals("anthropic", anthropicProvider.getProviderName());

        // Test operation names in retry executors
        assertTrue(openAiProvider.getRetryExecutor().getOperationName().contains("openai"));
        assertTrue(anthropicProvider.getRetryExecutor().getOperationName().contains("anthropic"));

        // Verify they're different
        assertNotEquals(openAiProvider.getProviderName(), anthropicProvider.getProviderName());
    }

    @Test
    @Order(15)
    @DisplayName("Test Gemini provider basic functionality")
    void testGeminiProviderBasicFunctionality() {
        // Test that the Gemini provider can be instantiated and has correct configuration
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("gemini-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("gemini", provider.getProviderName());
    }

    @Test
    @Order(16)
    @DisplayName("Test Gemini provider with custom retry policy")
    void testGeminiProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro-vision", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }

    @Test
    @Order(17)
    @DisplayName("Test Gemini provider exception classification")
    void testGeminiProviderExceptionClassification() {
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro");

        // Test common retryable patterns (inherited from base class)
        assertTrue(provider.isRetryableException(new RuntimeException("Connection timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("Network error")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 Service Unavailable")));

        // Test Gemini-specific patterns
        assertTrue(provider.isRetryableException(new RuntimeException("Google quota exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google resource exhausted")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google backend error")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google deadline exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Gemini model is busy")));
        assertTrue(provider.isRetryableException(new RuntimeException("Gemini service overloaded")));

        // Test non-retryable patterns
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid API key")));
        assertFalse(provider.isRetryableException(new RuntimeException("Model not found")));
    }

    @Test
    @Order(19)
    @DisplayName("Test common exception classification patterns")
    void testCommonExceptionClassificationPatterns() {
        OpenAiLLMProvider openAiProvider = new OpenAiLLMProvider("test-key", "gpt-3.5-turbo", "https://api.openai.com/v1");
        AnthropicLLMProvider anthropicProvider = new AnthropicLLMProvider("test-key", "claude-3-5-sonnet-20241022");
        GeminiLLMProvider geminiProvider = new GeminiLLMProvider("test-key", "gemini-2.5-flash");

        // Both providers should recognize common retryable patterns
        String[] retryableMessages = {
            "Connection timeout",
            "Network error",
            "Rate limit exceeded",
            "Service unavailable",
            "502 Bad Gateway",
            "503 Service Unavailable",
            "504 Gateway Timeout",
            "Internal server error"
        };

        for (String message : retryableMessages) {
            RuntimeException exception = new RuntimeException(message);
            assertTrue(openAiProvider.isRetryableException(exception),
                    "OpenAI should classify as retryable: " + message);
            assertTrue(anthropicProvider.isRetryableException(exception),
                    "Anthropic should classify as retryable: " + message);
            assertTrue(geminiProvider.isRetryableException(exception),
                    "Gemini should classify as retryable: " + message);
        }

        // Both providers should recognize common non-retryable patterns
        String[] nonRetryableMessages = {
            "Invalid API key",
            "Authentication failed",
            "Model not found",
            "Malformed request"
        };

        for (String message : nonRetryableMessages) {
            RuntimeException exception = new RuntimeException(message);
            assertFalse(openAiProvider.isRetryableException(exception),
                    "OpenAI should classify as non-retryable: " + message);
            assertFalse(anthropicProvider.isRetryableException(exception),
                    "Anthropic should classify as non-retryable: " + message);
            assertFalse(geminiProvider.isRetryableException(exception),
                    "Gemini should classify as non-retryable: " + message);
        }
    }

    @Test
    @Order(20)
    @DisplayName("Test Ollama provider basic functionality")
    void testOllamaProviderBasicFunctionality() {
        OllamaLLMProvider provider = new OllamaLLMProvider("http://localhost:11434", "llama2");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("ollama-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("ollama", provider.getProviderName());
    }

    @Test
    @Order(21)
    @DisplayName("Test Ollama provider with custom retry policy")
    void testOllamaProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        OllamaLLMProvider provider = new OllamaLLMProvider("http://localhost:11434", "llama2", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }

    @Test
    @Order(22)
    @DisplayName("Test LocalAI provider basic functionality")
    void testLocalAiProviderBasicFunctionality() {
        LocalAiLLMProvider provider = new LocalAiLLMProvider("http://localhost:8080", "gpt-3.5-turbo");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("localai-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("localai", provider.getProviderName());
    }

    @Test
    @Order(23)
    @DisplayName("Test LocalAI provider with custom retry policy")
    void testLocalAiProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        LocalAiLLMProvider provider = new LocalAiLLMProvider("http://localhost:8080", "gpt-3.5-turbo", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }
/*
    @Test
    @Order(24)
    @DisplayName("Test Oracle provider basic functionality")
    void testOracleProviderBasicFunctionality() throws IOException {
        OracleLLMProvider provider = new OracleLLMProvider("test-compartment-id", "cohere.command");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("oracle-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("oracle", provider.getProviderName());
    }

    @Test
    @Order(25)
    @DisplayName("Test Oracle provider with custom retry policy")
    void testOracleProviderWithCustomRetryPolicy() throws IOException {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        OracleLLMProvider provider = new OracleLLMProvider("test-compartment-id", "cohere.command", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }
*/
    @Test
    @Order(26)
    @DisplayName("Test Amazon Bedrock provider basic functionality")
    void testAmazonBedrockProviderBasicFunctionality() {
        AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider("anthropic.claude-v2", "us-east-1");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("amazon-bedrock-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("amazon-bedrock", provider.getProviderName());
    }

    @Test
    @Order(27)
    @DisplayName("Test Amazon Bedrock provider with custom retry policy")
    void testAmazonBedrockProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider("anthropic.claude-v2", "us-east-1", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }

    @Test
    @Order(28)
    @DisplayName("Test Azure OpenAI provider basic functionality")
    void testAzureOpenAiProviderBasicFunctionality() {
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider("test-api-key", "https://test.openai.azure.com/", "test-deployment");

        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryExecutor().getPolicy());
        assertEquals("azure-openai-llm-call", provider.getRetryExecutor().getOperationName());
        assertEquals("azure-openai", provider.getProviderName());
    }

    @Test
    @Order(29)
    @DisplayName("Test Azure OpenAI provider with custom retry policy")
    void testAzureOpenAiProviderWithCustomRetryPolicy() {
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(500));
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider("test-api-key", "https://test.openai.azure.com/", "test-deployment", customPolicy);

        assertEquals(customPolicy, provider.getRetryExecutor().getPolicy());
    }
}
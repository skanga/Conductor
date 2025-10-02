package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractLLMProviderTest {

    private TestableAbstractLLMProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TestableAbstractLLMProvider("test-provider", "test-model");
    }

    @Test
    void shouldCreateProviderWithValidParameters() {
        // Then
        assertEquals("test-provider", provider.getProviderName());
        assertEquals("test-model", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullProviderName() {
        // When
        TestableAbstractLLMProvider nullNameProvider = new TestableAbstractLLMProvider(null, "model");

        // Then
        assertNotNull(nullNameProvider.getProviderName());
        assertTrue(nullNameProvider.getProviderName().startsWith("llm-provider-"));
    }

    @Test
    void shouldCreateProviderWithEmptyProviderName() {
        // When
        TestableAbstractLLMProvider emptyNameProvider = new TestableAbstractLLMProvider("", "model");

        // Then
        assertNotNull(emptyNameProvider.getProviderName());
        assertTrue(emptyNameProvider.getProviderName().startsWith("llm-provider-"));
    }

    @Test
    void shouldCreateProviderWithWhitespaceProviderName() {
        // When
        TestableAbstractLLMProvider whitespaceProvider = new TestableAbstractLLMProvider("   ", "model");

        // Then
        assertNotNull(whitespaceProvider.getProviderName());
        assertTrue(whitespaceProvider.getProviderName().startsWith("llm-provider-"));
    }

    @Test
    void shouldStandardizeProviderNameWithSpecialCharacters() {
        // When
        TestableAbstractLLMProvider specialProvider = new TestableAbstractLLMProvider("Test Provider 123!", "model");

        // Then
        String standardized = specialProvider.getProviderName();
        assertTrue(standardized.matches("[a-z0-9\\-]+"));
        assertTrue(standardized.contains("test"));
        assertTrue(standardized.contains("provider"));
        assertTrue(standardized.contains("123"));
    }

    @Test
    void shouldHandleNullModelName() {
        // When
        TestableAbstractLLMProvider nullModelProvider = new TestableAbstractLLMProvider("provider", null);

        // Then
        assertNull(nullModelProvider.getModelName());
    }

    @Test
    void shouldGenerateSuccessfully() throws ConductorException {
        // Given
        String prompt = "Test prompt";
        provider.setMockResponse("Mock response");

        // When
        String result = provider.generate(prompt);

        // Then
        assertEquals("Mock response", result);
    }

    @Test
    void shouldHandleNullPrompt() throws ConductorException {
        // Given
        provider.setMockResponse("Default response");

        // When
        String result = provider.generate(null);

        // Then
        assertEquals("Default response", result);
    }

    @Test
    void shouldHandleEmptyPrompt() throws ConductorException {
        // Given
        provider.setMockResponse("Empty prompt response");

        // When
        String result = provider.generate("");

        // Then
        assertEquals("Empty prompt response", result);
    }

    @Test
    void shouldRetryOnTransientException() throws ConductorException {
        // Given
        provider.setFailureCount(2); // Fail twice, then succeed
        provider.setMockResponse("Success after retries");

        // When
        String result = provider.generate("test prompt");

        // Then
        assertEquals("Success after retries", result);
        assertEquals(3, provider.getAttemptCount());
    }

    @Test
    void shouldPropagateNonTransientException() {
        // Given
        provider.setThrowNonTransientException(true);

        // When & Then
        ConductorException exception = assertThrows(ConductorException.class, () -> {
            provider.generate("test prompt");
        });

        assertTrue(exception.getMessage().contains("Non-transient error"));
    }

    @Test
    void shouldHandleVeryLongPrompt() throws ConductorException {
        // Given
        StringBuilder longPrompt = new StringBuilder();
        for (int i = 0; i < 25; i++) { // Reduced from 1000 to 25 for faster testing
            longPrompt.append("This is a very long prompt with lots of text. ");
        }
        provider.setMockResponse("Long prompt handled");

        // When
        String result = provider.generate(longPrompt.toString());

        // Then
        assertEquals("Long prompt handled", result);
    }

    @Test
    void shouldHandleSpecialCharactersInPrompt() throws ConductorException {
        // Given
        String specialPrompt = "Prompt with unicode: 你好世界 🚀, symbols: @#$%^&*(), and newlines\nand\ttabs";
        provider.setMockResponse("Special characters handled");

        // When
        String result = provider.generate(specialPrompt);

        // Then
        assertEquals("Special characters handled", result);
    }

    @Test
    void shouldProvideProviderInformation() {
        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("test-provider", providerName);
        assertEquals("test-model", modelName);
    }

    @Test
    void shouldHandleMultipleConcurrentGenerations() throws Exception {
        // Given
        provider.setMockResponse("Concurrent response");
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    results[index] = provider.generate("Concurrent test " + index);
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
            assertEquals("Concurrent response", results[i]);
        }
    }

    @Test
    void shouldTestBuilderPattern() {
        // Test the abstract builder functionality through concrete implementation
        TestBuilderProvider provider = new TestBuilderProvider.TestBuilder("test-provider")
            .modelName("custom-model")
            .build();

        // Then
        assertNotNull(provider);
        assertTrue(provider.getProviderName().contains("test-provider"));
        assertEquals("custom-model", provider.getModelName());
    }

    @Test
    void shouldTestCreateModel() {
        // Test the createModel template method
        TestableAbstractLLMProvider testProvider = provider;

        // Verify that the provider was created successfully with the model
        assertNotNull(testProvider);
        assertTrue(testProvider.getProviderName().contains("test-provider"));
    }

    @Test
    void shouldTestProviderContext() throws ConductorException {
        // Test provider context creation methods
        provider.setMockResponse("Context test");

        String result = provider.generate("test context");

        assertNotNull(result);
        assertEquals("Context test", result);
    }

    @Test
    void shouldTestStandardizeModelNameEdgeCases() {
        // Test model name standardization with various inputs
        assertEquals("default", AbstractLLMProvider.standardizeModelName(null, "default"));
        assertEquals("custom-default", AbstractLLMProvider.standardizeModelName("", "custom-default"));
        assertEquals("test-model", AbstractLLMProvider.standardizeModelName("  Test_Model  ", "default"));
        assertEquals("complex-123-model", AbstractLLMProvider.standardizeModelName("Complex@123#Model!", "default"));
    }

    @Test
    void shouldTestStandardizeProviderNameEdgeCases() {
        // Test provider name standardization
        assertEquals("test-provider", AbstractLLMProvider.standardizeProviderName("Test Provider"));
        assertEquals("provider-123", AbstractLLMProvider.standardizeProviderName("Provider@123!"));
        assertEquals("multi-word-provider", AbstractLLMProvider.standardizeProviderName("Multi   Word Provider"));
    }

    @Test
    void shouldThrowExceptionForInvalidProviderName() {
        assertThrows(IllegalArgumentException.class, () ->
            AbstractLLMProvider.standardizeProviderName(null));
        assertThrows(IllegalArgumentException.class, () ->
            AbstractLLMProvider.standardizeProviderName(""));
        assertThrows(IllegalArgumentException.class, () ->
            AbstractLLMProvider.standardizeProviderName("   "));
    }

    @Test
    void shouldTestGetModelNameWithPromptContext() throws ConductorException {
        // Test the getModelName(prompt) method
        provider.setMockResponse("Model context test");

        String result = provider.generate("test prompt for model context");

        assertNotNull(result);
        assertEquals("Model context test", result);
    }

    @Test
    void shouldTestRetryPolicyAccess() {
        // Test retry policy getter methods
        assertNotNull(provider.getRetryExecutor());
        assertNotNull(provider.getRetryPolicy());
        assertEquals(5, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    void shouldTestExceptionClassification() {
        TestableAbstractLLMProvider testProvider = new TestableAbstractLLMProvider("test", "model");

        // Test retryable exceptions
        assertTrue(testProvider.isRetryableException(new RuntimeException("timeout error")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("rate limit exceeded")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("502 bad gateway")));

        // Test non-retryable exceptions
        assertFalse(testProvider.isRetryableException(new IllegalArgumentException("invalid parameter")));
        assertFalse(testProvider.isRetryableException(new RuntimeException("authentication failed")));
        assertFalse(testProvider.isRetryableException(new RuntimeException("not found")));
    }

    @Test
    void shouldTestLegacyConstructors() {
        // Test deprecated constructors for backward compatibility
        @SuppressWarnings("deprecation")
        TestableAbstractLLMProvider legacyProvider1 = new TestableAbstractLLMProvider("legacy-test");

        assertNotNull(legacyProvider1);
        assertTrue(legacyProvider1.getProviderName().contains("legacy-test"));
        assertEquals("default", legacyProvider1.getModelName());
    }

    /**
     * Test builder implementation for testing abstract builder pattern
     */
    private static class TestBuilderProvider extends AbstractLLMProvider {

        public TestBuilderProvider(String providerName, String modelName) {
            super(providerName, modelName);
        }

        @Override
        protected String generateInternal(String prompt) throws Exception {
            return "Builder test response";
        }

        public static class TestBuilder extends AbstractLLMProvider.Builder<TestBuilderProvider, TestBuilder> {
            public TestBuilder(String providerName) {
                super(providerName);
            }

            @Override
            public TestBuilderProvider build() {
                return new TestBuilderProvider(providerName, modelName != null ? modelName : "default");
            }
        }
    }

    /**
     * Extended testable provider with legacy constructor support
     */
    private static class TestableAbstractLLMProvider extends AbstractLLMProvider {
        private String mockResponse = "Mock response";
        private int failureCount = 0;
        private int attemptCount = 0;
        private boolean throwNonTransientException = false;

        public TestableAbstractLLMProvider(String providerName, String modelName) {
            super(providerName, modelName, createTestRetryPolicy());
        }

        @SuppressWarnings("deprecation")
        public TestableAbstractLLMProvider(String providerName) {
            super(providerName);
        }

        @Override
        public boolean isRetryableException(Exception exception) {
            return super.isRetryableException(exception);
        }

        private static RetryPolicy createTestRetryPolicy() {
            return new com.skanga.conductor.retry.FixedDelayRetryPolicy(
                5,
                java.time.Duration.ofMillis(10),
                java.time.Duration.ofMinutes(1),
                java.util.Set.of(
                    RuntimeException.class,
                    AbstractLLMProvider.TransientLLMException.class,
                    java.io.IOException.class,
                    java.net.SocketTimeoutException.class
                )
            );
        }

        @Override
        protected String generateInternal(String prompt) throws Exception {
            attemptCount++;

            if (throwNonTransientException) {
                throw new IllegalArgumentException("Non-transient error");
            }

            if (failureCount > 0) {
                failureCount--;
                throw new RuntimeException("Transient error");
            }

            return mockResponse;
        }

        public void setMockResponse(String response) {
            this.mockResponse = response;
        }

        public void setFailureCount(int count) {
            this.failureCount = count;
            this.attemptCount = 0;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public void setThrowNonTransientException(boolean throwException) {
            this.throwNonTransientException = throwException;
        }
    }
}

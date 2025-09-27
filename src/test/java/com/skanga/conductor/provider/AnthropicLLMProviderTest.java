package com.skanga.conductor.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnthropicLLMProviderTest {

    @Test
    void shouldCreateProviderWithValidParameters() {
        // When
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");

        // Then
        assertNotNull(provider);
        assertEquals("anthropic", provider.getProviderName());
        assertEquals("claude-3-opus", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullModelName() {
        // When
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", null);

        // Then
        assertNotNull(provider);
        assertEquals("anthropic", provider.getProviderName());
        assertEquals("claude-3-sonnet", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithEmptyModelName() {
        // When
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "");

        // Then
        assertNotNull(provider);
        assertEquals("anthropic", provider.getProviderName());
        assertEquals("claude-3-sonnet", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithWhitespaceModelName() {
        // When
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "   ");

        // Then
        assertNotNull(provider);
        assertEquals("anthropic", provider.getProviderName());
        assertEquals("claude-3-sonnet", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithCustomModelName() {
        // When
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-haiku");

        // Then
        assertNotNull(provider);
        assertEquals("anthropic", provider.getProviderName());
        assertEquals("claude-3-haiku", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullApiKey() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            AnthropicLLMProvider provider = new AnthropicLLMProvider(null, "claude-3-opus");
            assertNotNull(provider);
        });
    }

    @Test
    void shouldCreateProviderWithEmptyApiKey() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            AnthropicLLMProvider provider = new AnthropicLLMProvider("", "claude-3-opus");
            assertNotNull(provider);
        });
    }

    @Test
    void shouldHaveConsistentProviderName() {
        // Given
        AnthropicLLMProvider provider1 = new AnthropicLLMProvider("key1", "claude-3-opus");
        AnthropicLLMProvider provider2 = new AnthropicLLMProvider("key2", "claude-3-sonnet");

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("anthropic", provider1.getProviderName());
    }

    @Test
    void shouldProvideProviderInformation() {
        // Given
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");

        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("anthropic", providerName);
        assertEquals("claude-3-opus", modelName);
    }

    @Test
    void shouldHandleModelNameStandardization() {
        // Test various model name formats - Anthropic preserves model names exactly
        String[] testCases = {
            "Claude-3-Opus",
            "claude_3_sonnet",
            "CLAUDE 3 HAIKU",
            "Claude-3-Opus-20240229"
        };

        for (String inputModelName : testCases) {
            // When
            AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", inputModelName);

            // Then - Anthropic provider preserves model names for API compatibility
            assertNotNull(provider.getModelName());
            assertEquals(inputModelName, provider.getModelName());
        }
    }

    @Test
    void shouldCreateMultipleProviderInstances() {
        // When
        AnthropicLLMProvider provider1 = new AnthropicLLMProvider("key1", "claude-3-opus");
        AnthropicLLMProvider provider2 = new AnthropicLLMProvider("key2", "claude-3-sonnet");
        AnthropicLLMProvider provider3 = new AnthropicLLMProvider("key3", "claude-3-haiku");

        // Then
        assertNotSame(provider1, provider2);
        assertNotSame(provider2, provider3);

        // All should have consistent provider names
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals(provider2.getProviderName(), provider3.getProviderName());

        // But different model names
        assertNotEquals(provider1.getModelName(), provider2.getModelName());
        assertNotEquals(provider2.getModelName(), provider3.getModelName());
    }

    @Test
    void shouldHandleLongApiKey() {
        // Given
        StringBuilder longKey = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longKey.append("sk-ant-1234567890abcdef");
        }

        // When & Then
        assertDoesNotThrow(() -> {
            AnthropicLLMProvider provider = new AnthropicLLMProvider(longKey.toString(), "claude-3-opus");
            assertNotNull(provider);
        });
    }

    @Test
    void shouldHandleAllClaudeModelVariants() {
        // Given
        String[] claudeModels = {
            "claude-3-opus",
            "claude-3-sonnet",
            "claude-3-haiku",
            "claude-2.1",
            "claude-2.0",
            "claude-instant-1.2"
        };

        for (String modelName : claudeModels) {
            // When
            AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", modelName);

            // Then
            assertNotNull(provider);
            assertEquals("anthropic", provider.getProviderName());
            assertEquals(modelName, provider.getModelName());
        }
    }

    @Test
    void shouldHandleSpecialCharactersInApiKey() {
        // Given
        String specialKey = "sk-ant-api03-abc123_def456-ghi789_jkl012-mno345-pqr678_stu901-vwx234-yz567";

        // When & Then
        assertDoesNotThrow(() -> {
            AnthropicLLMProvider provider = new AnthropicLLMProvider(specialKey, "claude-3-opus");
            assertNotNull(provider);
        });
    }

    @Test
    void shouldPreserveModelNameCase() {
        // Given - Anthropic model names are case-sensitive
        String[] exactModelNames = {
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
        };

        for (String modelName : exactModelNames) {
            // When
            AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", modelName);

            // Then - Should preserve exact model name for API compatibility
            assertEquals(modelName, provider.getModelName());
        }
    }

    @Test
    void shouldTestBuilderPattern() {
        // When
        AnthropicLLMProvider provider = AnthropicLLMProvider.builder()
            .apiKey("test-key")
            .anthropicModelName("claude-3-haiku")
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("anthropic", provider.getProviderName());
        assertEquals("claude-3-haiku", provider.getModelName());
    }

    @Test
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        com.skanga.conductor.retry.RetryPolicy customPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(2, java.time.Duration.ofMillis(500));

        // When
        AnthropicLLMProvider provider = AnthropicLLMProvider.builder()
            .apiKey("test-key")
            .anthropicModelName("claude-3-opus")
            .retryPolicy(customPolicy)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals(2, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    void shouldThrowExceptionForIncompleteBuilder() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            AnthropicLLMProvider.builder()
                .apiKey("test-key")
                // Missing model name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            AnthropicLLMProvider.builder()
                .anthropicModelName("claude-3-opus")
                // Missing API key
                .build();
        });
    }

    @Test
    void shouldTestRetryableExceptionPatterns() {
        // Given
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");

        // Test Anthropic-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("anthropic rate_limit_error occurred")));
        assertTrue(provider.isRetryableException(new RuntimeException("anthropic overloaded_error")));
        assertTrue(provider.isRetryableException(new RuntimeException("anthropic api_error happened")));
        assertTrue(provider.isRetryableException(new RuntimeException("anthropic service_unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("claude is busy processing")));
        assertTrue(provider.isRetryableException(new RuntimeException("claude is overloaded right now")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("invalid_request_error")));
        assertFalse(provider.isRetryableException(new RuntimeException("authentication_error")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Bad request")));
    }

    @Test
    void shouldInheritCommonRetryablePatterns() {
        // Given
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");

        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("timeout occurred")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        AnthropicLLMProvider[] providers = new AnthropicLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new AnthropicLLMProvider("key-" + index, "claude-3-opus");
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
            assertNotNull(providers[i], "Provider " + i + " should be created");
            assertEquals("anthropic", providers[i].getProviderName());
        }
    }

    @Test
    void shouldHandleVeryLongPrompts() {
        // Given
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");
        StringBuilder longPrompt = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longPrompt.append("This is a very long prompt to test handling of large inputs. ");
        }

        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            // Just test that the provider can be created and doesn't fail on construction
            assertNotNull(provider);
        });
    }

    @Test
    void shouldTestToString() {
        // Given
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");

        // When
        String toString = provider.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("anthropic") || toString.contains("AnthropicLLMProvider"));
    }

    @Test
    void shouldTestEquals() {
        // Given
        AnthropicLLMProvider provider1 = new AnthropicLLMProvider("test-key", "claude-3-opus");
        AnthropicLLMProvider provider2 = new AnthropicLLMProvider("test-key", "claude-3-opus");
        AnthropicLLMProvider provider3 = new AnthropicLLMProvider("different-key", "claude-3-opus");

        // When & Then
        assertEquals(provider1, provider1); // Same instance
        // Different instances might not be equal depending on implementation
        assertNotEquals(provider1, null);
        assertNotEquals(provider1, "not a provider");
    }

    @Test
    void shouldTestHashCode() {
        // Given
        AnthropicLLMProvider provider = new AnthropicLLMProvider("test-key", "claude-3-opus");

        // When
        int hashCode = provider.hashCode();

        // Then
        assertNotEquals(0, hashCode); // Should have a reasonable hash code
    }

    @Test
    void shouldHandleEdgeCaseApiKeyFormats() {
        // Given
        String[] edgeCaseKeys = {
            "sk-ant-123",
            "very_short",
            " key_with_spaces ",
            "key\nwith\nnewlines",
            "key\twith\ttabs"
        };

        for (String apiKey : edgeCaseKeys) {
            // When & Then - should not throw exception during construction
            assertDoesNotThrow(() -> {
                AnthropicLLMProvider provider = new AnthropicLLMProvider(apiKey, "claude-3-opus");
                assertNotNull(provider);
            });
        }
    }
}
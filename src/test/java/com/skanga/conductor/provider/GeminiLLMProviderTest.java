package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiLLMProviderTest {

    @Test
    void shouldCreateProviderWithValidParameters() {
        // When
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro");

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullModelName() {
        // When
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", null);

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithEmptyModelName() {
        // When
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "");

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithWhitespaceModelName() {
        // When
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "   ");

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithCustomModelName() {
        // When
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro-vision");

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro-vision", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullApiKey() {
        // When & Then - should throw exception during construction for null API key
        assertThrows(RuntimeException.class, () -> {
            new GeminiLLMProvider(null, "gemini-pro");
        });
    }

    @Test
    void shouldCreateProviderWithEmptyApiKey() {
        // When & Then - should throw exception during construction for empty API key
        assertThrows(RuntimeException.class, () -> {
            new GeminiLLMProvider("", "gemini-pro");
        });
    }

    @Test
    void shouldCreateProviderWithCustomRetryPolicy() {
        // Given
        RetryPolicy customRetryPolicy = RetryPolicy.fixedDelay(3, java.time.Duration.ofSeconds(1));

        // When
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro", customRetryPolicy);

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro", provider.getModelName());
        assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    void shouldHaveConsistentProviderName() {
        // Given
        GeminiLLMProvider provider1 = new GeminiLLMProvider("key1", "gemini-pro");
        GeminiLLMProvider provider2 = new GeminiLLMProvider("key2", "gemini-pro-vision");

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("gemini", provider1.getProviderName());
    }

    @Test
    void shouldProvideProviderInformation() {
        // Given
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro");

        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("gemini", providerName);
        assertEquals("gemini-pro", modelName);
    }

    @Test
    void shouldHandleModelNameStandardization() {
        // Test various model name formats
        String[] modelNames = {
            "Gemini-Pro",
            "gemini_pro_vision",
            "GEMINI PRO",
            "Gemini-Pro-Vision-V1"
        };

        for (String modelName : modelNames) {
            // When
            GeminiLLMProvider provider = new GeminiLLMProvider("test-key", modelName);

            // Then
            assertNotNull(provider.getModelName());
            assertTrue(provider.getModelName().matches("[a-z0-9\\-]+"));
        }
    }

    @Test
    void shouldCreateMultipleProviderInstances() {
        // When
        GeminiLLMProvider provider1 = new GeminiLLMProvider("key1", "gemini-pro");
        GeminiLLMProvider provider2 = new GeminiLLMProvider("key2", "gemini-pro-vision");
        GeminiLLMProvider provider3 = new GeminiLLMProvider("key3", "gemini-ultra");

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
            longKey.append("AIzaSyC1234567890abcdef");
        }

        // When & Then
        assertDoesNotThrow(() -> {
            GeminiLLMProvider provider = new GeminiLLMProvider(longKey.toString(), "gemini-pro");
            assertNotNull(provider);
        });
    }

    @Test
    void shouldHandleAllGeminiModelVariants() {
        // Given
        String[] geminiModels = {
            "gemini-pro",
            "gemini-pro-vision",
            "gemini-ultra",
            "gemini-nano",
            "gemini-1.0-pro",
            "gemini-1.5-pro"
        };

        for (String modelName : geminiModels) {
            // When
            GeminiLLMProvider provider = new GeminiLLMProvider("test-key", modelName);

            // Then
            assertNotNull(provider);
            assertEquals("gemini", provider.getProviderName());
            assertEquals(modelName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", ""),
                        provider.getModelName());
        }
    }

    @Test
    void shouldHandleSpecialCharactersInApiKey() {
        // Given
        String specialKey = "AIzaSyC-api03_abc123-def456_ghi789-jkl012_mno345-pqr678_stu901-vwx234-yz567";

        // When & Then
        assertDoesNotThrow(() -> {
            GeminiLLMProvider provider = new GeminiLLMProvider(specialKey, "gemini-pro");
            assertNotNull(provider);
        });
    }

    @Test
    void shouldTestRetryableExceptionPatterns() {
        // Given
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro");

        // Test Google/Gemini specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("Google quota exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google resource exhausted")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google backend error")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google deadline exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Google service unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("Gemini is busy")));
        assertTrue(provider.isRetryableException(new RuntimeException("Gemini is overloaded")));
        assertTrue(provider.isRetryableException(new RuntimeException("AI platform error occurred")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("Authentication failed")));
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid API key")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Bad request")));
    }

    @Test
    void shouldInheritCommonRetryablePatterns() {
        // Given
        GeminiLLMProvider provider = new GeminiLLMProvider("test-key", "gemini-pro");

        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    void shouldTestBuilderPattern() {
        // When
        GeminiLLMProvider provider = GeminiLLMProvider.builder()
            .apiKey("test-key")
            .geminiModelName("gemini-pro-vision")
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-pro-vision", provider.getModelName());
    }

    @Test
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(2, java.time.Duration.ofMillis(500));

        // When
        GeminiLLMProvider provider = GeminiLLMProvider.builder()
            .apiKey("test-key")
            .geminiModelName("gemini-pro")
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
            GeminiLLMProvider.builder()
                .apiKey("test-key")
                // Missing model name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            GeminiLLMProvider.builder()
                .geminiModelName("gemini-pro")
                // Missing API key
                .build();
        });
    }

    @Test
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        GeminiLLMProvider[] providers = new GeminiLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new GeminiLLMProvider("key-" + index, "gemini-pro");
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
            assertEquals("gemini", providers[i].getProviderName());
        }
    }
}
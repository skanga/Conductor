package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalAiLLMProviderTest {

    private static final String VALID_BASE_URL = "http://localhost:8080";
    private static final String VALID_MODEL = "gpt-3.5-turbo";

    @Test
    void shouldCreateProviderWithValidParameters() {
        // When
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, VALID_MODEL);

        // Then
        assertNotNull(provider);
        assertEquals("localai", provider.getProviderName());
        assertEquals("gpt-3-5-turbo", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullModelName() {
        // When & Then - should throw exception for null model
        assertThrows(RuntimeException.class, () -> {
            new LocalAiLLMProvider(VALID_BASE_URL, null);
        });
    }

    @Test
    void shouldCreateProviderWithEmptyModelName() {
        // When & Then - should throw exception for empty model
        assertThrows(RuntimeException.class, () -> {
            new LocalAiLLMProvider(VALID_BASE_URL, "");
        });
    }

    @Test
    void shouldCreateProviderWithWhitespaceModelName() {
        // When & Then - should throw exception for whitespace model
        assertThrows(RuntimeException.class, () -> {
            new LocalAiLLMProvider(VALID_BASE_URL, "   ");
        });
    }

    @Test
    void shouldCreateProviderWithCustomModelName() {
        // When
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, "llama-7b");

        // Then
        assertNotNull(provider);
        assertEquals("localai", provider.getProviderName());
        assertEquals("llama-7b", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullBaseUrl() {
        // When & Then - should throw exception for null base URL
        assertThrows(RuntimeException.class, () -> {
            new LocalAiLLMProvider(null, VALID_MODEL);
        });
    }

    @Test
    void shouldCreateProviderWithEmptyBaseUrl() {
        // When & Then - should throw exception for empty base URL
        assertThrows(RuntimeException.class, () -> {
            new LocalAiLLMProvider("", VALID_MODEL);
        });
    }

    @Test
    void shouldCreateProviderWithCustomRetryPolicy() {
        // Given
        RetryPolicy customRetryPolicy = RetryPolicy.fixedDelay(3, java.time.Duration.ofSeconds(1));

        // When
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, VALID_MODEL, customRetryPolicy);

        // Then
        assertNotNull(provider);
        assertEquals("localai", provider.getProviderName());
        assertEquals("gpt-3-5-turbo", provider.getModelName());
        assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    void shouldHaveConsistentProviderName() {
        // Given
        LocalAiLLMProvider provider1 = new LocalAiLLMProvider("url1", "model1");
        LocalAiLLMProvider provider2 = new LocalAiLLMProvider("url2", "model2");

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("localai", provider1.getProviderName());
    }

    @Test
    void shouldProvideProviderInformation() {
        // Given
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, "custom-model");

        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("localai", providerName);
        assertEquals("custom-model", modelName);
    }

    @Test
    void shouldHandleModelNameStandardization() {
        // Test various model name formats
        String[] modelNames = {
            "Llama-7B",
            "gpt_3_5_turbo",
            "VICUNA 13B",
            "Code-Llama-7B-Instruct"
        };

        for (String modelName : modelNames) {
            // When
            LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, modelName);

            // Then
            assertNotNull(provider.getModelName());
            assertTrue(provider.getModelName().matches("[a-z0-9\\-]+"));
        }
    }

    @Test
    void shouldCreateMultipleProviderInstances() {
        // When
        LocalAiLLMProvider provider1 = new LocalAiLLMProvider("http://server1:8080", "model1");
        LocalAiLLMProvider provider2 = new LocalAiLLMProvider("http://server2:8080", "model2");
        LocalAiLLMProvider provider3 = new LocalAiLLMProvider("http://server3:8080", "model3");

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
    void shouldHandleVariousLocalAiUrls() {
        // Given
        String[] localAiUrls = {
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "http://localai.internal:8080",
            "https://secure.localai.com",
            "http://192.168.1.100:3000",
            "http://localai-server:80"
        };

        for (String url : localAiUrls) {
            // When
            LocalAiLLMProvider provider = new LocalAiLLMProvider(url, VALID_MODEL);

            // Then
            assertNotNull(provider);
            assertEquals("localai", provider.getProviderName());
            assertEquals("gpt-3-5-turbo", provider.getModelName());
        }
    }

    @Test
    void shouldHandleCommonLocalAiModels() {
        // Given
        String[] commonModels = {
            "gpt-3.5-turbo",
            "llama-7b",
            "llama-13b",
            "vicuna-7b",
            "alpaca-7b",
            "codellama-7b",
            "mistral-7b",
            "text-embedding-ada-002"
        };

        for (String model : commonModels) {
            // When
            LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, model);

            // Then
            assertNotNull(provider);
            assertEquals("localai", provider.getProviderName());
            assertEquals(model.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", ""),
                        provider.getModelName());
        }
    }

    @Test
    void shouldTestRetryableExceptionPatterns() {
        // Given
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, VALID_MODEL);

        // Test LocalAI-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("LocalAI server is busy")));
        assertTrue(provider.isRetryableException(new RuntimeException("LocalAI connection timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("LocalAI model loading")));
        assertTrue(provider.isRetryableException(new RuntimeException("LocalAI backend error")));
        assertTrue(provider.isRetryableException(new RuntimeException("model is busy processing")));
        assertTrue(provider.isRetryableException(new RuntimeException("server overloaded")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("model not found")));
        assertFalse(provider.isRetryableException(new RuntimeException("invalid request format")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Bad request")));
    }

    @Test
    void shouldInheritCommonRetryablePatterns() {
        // Given
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, VALID_MODEL);

        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    void shouldTestBuilderPattern() {
        // When
        LocalAiLLMProvider provider = LocalAiLLMProvider.builder()
            .baseUrl(VALID_BASE_URL)
            .localAiModelName("custom-llama")
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("localai", provider.getProviderName());
        assertEquals("custom-llama", provider.getModelName());
    }

    @Test
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(2, java.time.Duration.ofMillis(500));

        // When
        LocalAiLLMProvider provider = LocalAiLLMProvider.builder()
            .baseUrl(VALID_BASE_URL)
            .localAiModelName("test-model")
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
            LocalAiLLMProvider.builder()
                .baseUrl(VALID_BASE_URL)
                // Missing model name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            LocalAiLLMProvider.builder()
                .localAiModelName(VALID_MODEL)
                // Missing base URL
                .build();
        });
    }

    @Test
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        LocalAiLLMProvider[] providers = new LocalAiLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new LocalAiLLMProvider(
                        "http://server" + index + ":8080",
                        "model-" + index
                    );
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
            assertEquals("localai", providers[i].getProviderName());
        }
    }

    @Test
    void shouldHandleMalformedUrls() {
        // Given
        String[] malformedUrls = {
            "not-a-url",
            "ftp://wrong-protocol.com",
            "https://",
            "invalid url format",
            "localhost:8080", // Missing protocol
        };

        for (String url : malformedUrls) {
            // When & Then - should not throw exception during construction
            assertDoesNotThrow(() -> {
                LocalAiLLMProvider provider = new LocalAiLLMProvider(url, VALID_MODEL);
                assertNotNull(provider);
            });
        }
    }

    @Test
    void shouldHandlePortVariations() {
        // Given
        String[] urlsWithPorts = {
            "http://localhost:8080",
            "http://localhost:3000",
            "http://localhost:5000",
            "https://localhost:8443",
            "http://server.local:9090"
        };

        for (String url : urlsWithPorts) {
            // When
            LocalAiLLMProvider provider = new LocalAiLLMProvider(url, VALID_MODEL);

            // Then
            assertNotNull(provider);
            assertEquals("localai", provider.getProviderName());
        }
    }

    @Test
    void shouldTestDefaultBaseUrlHandling() {
        // When & Then - should throw exception for null base URL
        assertThrows(RuntimeException.class, () -> {
            new LocalAiLLMProvider(null, VALID_MODEL);
        });
    }

    @Test
    void shouldHandleSpecialModelNames() {
        // Given
        String[] specialModelNames = {
            "model-with-version-v1.2.3",
            "UPPERCASE-MODEL",
            "model_with_underscores",
            "model@special#chars!",
            "123-numeric-model"
        };

        for (String modelName : specialModelNames) {
            // When
            LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, modelName);

            // Then
            assertNotNull(provider);
            assertEquals("localai", provider.getProviderName());
            assertTrue(provider.getModelName().matches("[a-z0-9\\-]+"));
        }
    }

    @Test
    void shouldTestModelNamePreservation() {
        // Test that model name standardization works correctly
        LocalAiLLMProvider provider = new LocalAiLLMProvider(VALID_BASE_URL, "Custom_Model_Name_v2.1");

        assertEquals("custom-model-name-v2-1", provider.getModelName());
    }
}
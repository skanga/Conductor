package com.skanga.conductor.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for OllamaLLMProvider.
 *
 * Tests Ollama integration including:
 * - Constructor validation and parameter handling
 * - Provider and model name validation
 * - Builder pattern implementation
 * - URL configuration handling
 * - Retry policy configuration
 * - Error handling and exception patterns
 * - Threading safety
 * - Model format validation
 */
@DisplayName("Ollama LLM Provider Tests")
class OllamaLLMProviderTest {

    private OllamaLLMProvider provider;
    private String validBaseUrl;
    private String validModelName;

    @BeforeEach
    void setUp() {
        validBaseUrl = "http://localhost:11434";
        validModelName = "llama2";
        provider = new OllamaLLMProvider(validBaseUrl, validModelName);
    }

    @Test
    @DisplayName("Should create provider with valid Ollama configuration")
    void shouldCreateProviderWithValidParameters() {
        // When
        OllamaLLMProvider provider = new OllamaLLMProvider(validBaseUrl, validModelName);

        // Then
        assertNotNull(provider);
        assertEquals("ollama", provider.getProviderName());
        assertEquals(validModelName, provider.getModelName());
    }

    @Test
    @DisplayName("Should handle null base URL gracefully")
    void shouldHandleNullBaseUrl() {
        // When & Then - should throw exception during construction
        assertThrows(RuntimeException.class, () -> {
            new OllamaLLMProvider(null, "llama2");
        });
    }

    @Test
    @DisplayName("Should handle null model name gracefully")
    void shouldHandleNullModelName() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            OllamaLLMProvider provider = new OllamaLLMProvider(validBaseUrl, null);
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle empty string parameters")
    void shouldHandleEmptyStringParameters() {
        // When & Then - should throw exception during construction
        assertThrows(RuntimeException.class, () -> {
            new OllamaLLMProvider("", "");
        });

    }

    @Test
    @DisplayName("Should handle whitespace parameters")
    void shouldHandleWhitespaceParameters() {
        // When & Then - should throw exception during construction
        assertThrows(RuntimeException.class, () -> {
            new OllamaLLMProvider("   ", "   ");
        });

    }

    @Test
    @DisplayName("Should validate various Ollama base URLs")
    void shouldValidateOllamaBaseUrls() {
        // Given
        String[] validUrls = {
            "http://localhost:11434",
            "https://ollama.example.com",
            "http://192.168.1.100:11434",
            "https://ollama-server:8080",
            "http://127.0.0.1:11434"
        };

        for (String baseUrl : validUrls) {
            // When
            OllamaLLMProvider provider = new OllamaLLMProvider(baseUrl, validModelName);

            // Then
            assertNotNull(provider);
            assertEquals("ollama", provider.getProviderName());
        }
    }

    @Test
    @DisplayName("Should validate various Ollama model names")
    void shouldValidateOllamaModelNames() {
        // Given
        String[] validModelNames = {
            "llama2",
            "llama2-7b",
            "llama2-13b",
            "codellama",
            "codellama-7b-python",
            "mistral-7b",
            "neural-chat",
            "starcode",
            "vicuna-7b",
            "wizardcoder-python"
        };

        for (String modelName : validModelNames) {
            // When
            OllamaLLMProvider provider = new OllamaLLMProvider(validBaseUrl, modelName);

            // Then
            assertNotNull(provider);
            assertEquals("ollama", provider.getProviderName());
            assertEquals(modelName, provider.getModelName());
        }
    }

    @Test
    @DisplayName("Should test builder pattern with basic configuration")
    void shouldTestBuilderPattern() {
        // When
        OllamaLLMProvider provider = OllamaLLMProvider.builder()
            .baseUrl(validBaseUrl)
            .ollamaModelName(validModelName)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("ollama", provider.getProviderName());
        assertEquals(validModelName, provider.getModelName());
    }

    @Test
    @DisplayName("Should test builder pattern with retry policy")
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        com.skanga.conductor.retry.RetryPolicy customPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(3, Duration.ofSeconds(2));

        // When
        OllamaLLMProvider provider = OllamaLLMProvider.builder()
            .baseUrl(validBaseUrl)
            .ollamaModelName(validModelName)
            .retryPolicy(customPolicy)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    @DisplayName("Should test builder pattern with basic configuration")
    void shouldTestBuilderWithBasicConfiguration() {
        // When
        OllamaLLMProvider provider = OllamaLLMProvider.builder()
            .baseUrl(validBaseUrl)
            .ollamaModelName(validModelName)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("ollama", provider.getProviderName());
    }

    @Test
    @DisplayName("Should throw exception for incomplete builder")
    void shouldThrowExceptionForIncompleteBuilder() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            OllamaLLMProvider.builder()
                .baseUrl(validBaseUrl)
                // Missing model name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            OllamaLLMProvider.builder()
                .ollamaModelName(validModelName)
                // Missing base URL
                .build();
        });
    }

    @Test
    @DisplayName("Should test Ollama-specific retryable exception patterns")
    void shouldTestOllamaRetryableExceptionPatterns() {
        // Test Ollama-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("timeout occurred")));
        assertTrue(provider.isRetryableException(new RuntimeException("ollama service unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("model loading")));
        assertTrue(provider.isRetryableException(new RuntimeException("server overloaded")));
        assertTrue(provider.isRetryableException(new RuntimeException("too many requests")));
        assertTrue(provider.isRetryableException(new RuntimeException("model is busy")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("model not found")));
        assertFalse(provider.isRetryableException(new RuntimeException("invalid request format")));
        assertFalse(provider.isRetryableException(new RuntimeException("authentication failed")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Invalid input")));
    }

    @Test
    @DisplayName("Should inherit common retryable patterns")
    void shouldInheritCommonRetryablePatterns() {
        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("502 bad gateway")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection timeout")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    @DisplayName("Should handle concurrent provider creation")
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        OllamaLLMProvider[] providers = new OllamaLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new OllamaLLMProvider(
                        "http://localhost:" + (11434 + index), "llama2:7b"
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
            assertEquals("ollama", providers[i].getProviderName());
        }
    }

    @Test
    @DisplayName("Should validate provider consistency")
    void shouldValidateProviderConsistency() {
        // Given
        OllamaLLMProvider provider1 = new OllamaLLMProvider(
            "http://localhost:11434", "llama2:7b"
        );
        OllamaLLMProvider provider2 = new OllamaLLMProvider(
            "http://server:11434", "codellama:13b"
        );

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("ollama", provider1.getProviderName());

        // Different model names
        assertNotEquals(provider1.getModelName(), provider2.getModelName());
    }

    @Test
    @DisplayName("Should handle various URL formats")
    void shouldHandleVariousUrlFormats() {
        // Given
        String[] urlFormats = {
            "http://localhost:11434",
            "http://localhost:11434/",
            "https://ollama.example.com:8080",
            "https://ollama.example.com:8080/",
            "http://192.168.1.100:11434/api",
            "https://my-ollama-instance.cloud.com"
        };

        for (String baseUrl : urlFormats) {
            // When & Then
            assertDoesNotThrow(() -> {
                OllamaLLMProvider provider = new OllamaLLMProvider(baseUrl, validModelName);
                assertNotNull(provider);
                assertEquals("ollama", provider.getProviderName());
            });
        }
    }

    @Test
    @DisplayName("Should test provider information methods")
    void shouldTestProviderInformationMethods() {
        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("ollama", providerName);
        assertEquals(validModelName, modelName);
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToString() {
        // When
        String toString = provider.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("ollama") || toString.contains("OllamaLLMProvider"));
    }

    @Test
    @DisplayName("Should test hashCode method")
    void shouldTestHashCode() {
        // When
        int hashCode = provider.hashCode();

        // Then
        assertNotEquals(0, hashCode); // Should have a reasonable hash code
    }

    @Test
    @DisplayName("Should test equals method")
    void shouldTestEquals() {
        // Given
        OllamaLLMProvider provider1 = new OllamaLLMProvider(validBaseUrl, validModelName);
        OllamaLLMProvider provider2 = new OllamaLLMProvider(validBaseUrl, validModelName);
        OllamaLLMProvider provider3 = new OllamaLLMProvider("http://different:11434", validModelName);

        // When & Then
        assertEquals(provider1, provider1); // Same instance
        assertNotEquals(provider1, null);
        assertNotEquals(provider1, "not a provider");
    }

    @Test
    @DisplayName("Should handle model variants with tags")
    void shouldHandleModelVariantsWithTags() {
        // Given
        String[] modelVariants = {
            "llama2",
            "llama2-latest",
            "llama2-7b",
            "llama2-13b",
            "llama2-70b",
            "codellama-7b-instruct",
            "codellama-13b-python",
            "mistral-7b-instruct-v0-1",
            "neural-chat-7b-v3-1",
            "starcode-7b-alpha"
        };

        for (String modelName : modelVariants) {
            // When
            OllamaLLMProvider provider = new OllamaLLMProvider(validBaseUrl, modelName);

            // Then
            assertNotNull(provider);
            assertEquals("ollama", provider.getProviderName());
            assertEquals(modelName, provider.getModelName());
        }
    }

    @Test
    @DisplayName("Should handle custom configuration in builder")
    void shouldHandleCustomConfigurationInBuilder() {
        // When & Then
        assertDoesNotThrow(() -> {
            OllamaLLMProvider provider = OllamaLLMProvider.builder()
                .baseUrl(validBaseUrl)
                .ollamaModelName(validModelName)
                .build();
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle very long model names")
    void shouldHandleVeryLongModelNames() {
        // Given
        String longModelName = "very-long-custom-model-name-with-multiple-hyphens-and-descriptors-latest";

        // When & Then
        assertDoesNotThrow(() -> {
            OllamaLLMProvider provider = new OllamaLLMProvider(validBaseUrl, longModelName);
            assertNotNull(provider);
            assertEquals("ollama", provider.getProviderName());
            assertEquals(longModelName, provider.getModelName());
        });
    }

    @Test
    @DisplayName("Should handle special characters in URLs")
    void shouldHandleSpecialCharactersInUrls() {
        // Given
        String[] specialUrls = {
            "http://ollama-server_123.example.com:11434",
            "https://my-ollama-instance.example-domain.org:8080",
            "http://192.168.1.100:11434",
            "https://ollama.dev.company.internal:443"
        };

        for (String baseUrl : specialUrls) {
            // When & Then
            assertDoesNotThrow(() -> {
                OllamaLLMProvider provider = new OllamaLLMProvider(baseUrl, validModelName);
                assertNotNull(provider);
            });
        }
    }

    @Test
    @DisplayName("Should test builder method chaining")
    void shouldTestBuilderMethodChaining() {
        // Given
        com.skanga.conductor.retry.RetryPolicy retryPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(2, Duration.ofSeconds(1));

        // When & Then
        assertDoesNotThrow(() -> {
            OllamaLLMProvider provider = OllamaLLMProvider.builder()
                .baseUrl(validBaseUrl)
                .ollamaModelName(validModelName)
                .retryPolicy(retryPolicy)
                .build();
            assertNotNull(provider);
            assertEquals("ollama", provider.getProviderName());
        });
    }

    @Test
    @DisplayName("Should handle localhost variations")
    void shouldHandleLocalhostVariations() {
        // Given
        String[] localhostVariations = {
            "http://localhost:11434",
            "http://127.0.0.1:11434",
            "http://0.0.0.0:11434",
            "https://localhost:11434",
            "https://127.0.0.1:11434"
        };

        for (String baseUrl : localhostVariations) {
            // When
            OllamaLLMProvider provider = new OllamaLLMProvider(baseUrl, validModelName);

            // Then
            assertNotNull(provider);
            assertEquals("ollama", provider.getProviderName());
            assertEquals(validModelName, provider.getModelName());
        }
    }
}
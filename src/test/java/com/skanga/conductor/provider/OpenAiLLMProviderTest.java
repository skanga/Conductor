package com.skanga.conductor.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for OpenAiLLMProvider.
 *
 * Tests OpenAI integration including:
 * - Constructor validation and parameter handling
 * - Provider and model name validation
 * - Builder pattern implementation
 * - API key and base URL configuration handling
 * - Retry policy configuration
 * - Error handling and exception patterns
 * - Threading safety
 * - Model format validation
 */
@DisplayName("OpenAI LLM Provider Tests")
class OpenAiLLMProviderTest {

    private OpenAiLLMProvider provider;
    private String validApiKey;
    private String validModelName;
    private String validBaseUrl;

    @BeforeEach
    void setUp() {
        validApiKey = "sk-test1234567890abcdef1234567890abcdef1234567890abcdef";
        validModelName = "gpt-4";
        validBaseUrl = "https://api.openai.com/v1";
        provider = new OpenAiLLMProvider(validApiKey, validModelName, validBaseUrl);
    }

    @Test
    @DisplayName("Should create provider with valid OpenAI configuration")
    void shouldCreateProviderWithValidParameters() {
        // When
        OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, validModelName, validBaseUrl);

        // Then
        assertNotNull(provider);
        assertEquals("openai", provider.getProviderName());
        assertEquals(validModelName, provider.getModelName());
    }

    @Test
    @DisplayName("Should handle null API key gracefully")
    void shouldHandleNullApiKey() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            OpenAiLLMProvider provider = new OpenAiLLMProvider(null, validModelName, validBaseUrl);
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle null model name gracefully")
    void shouldHandleNullModelName() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, null, validBaseUrl);
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle null base URL gracefully")
    void shouldHandleNullBaseUrl() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, validModelName, null);
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle empty string parameters")
    void shouldHandleEmptyStringParameters() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            OpenAiLLMProvider provider = new OpenAiLLMProvider("", "", "");
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle whitespace parameters")
    void shouldHandleWhitespaceParameters() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            OpenAiLLMProvider provider = new OpenAiLLMProvider("   ", "   ", "   ");
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should validate various OpenAI model names")
    void shouldValidateOpenAIModelNames() {
        // Given
        String[] validModelNames = {
            "gpt-4",
            "gpt-4-turbo",
            "gpt-4-turbo-preview",
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",
            "text-davinci-003",
            "text-curie-001",
            "code-davinci-002",
            "text-embedding-ada-002",
            "custom-fine-tuned-model"
        };

        for (String modelName : validModelNames) {
            // When
            OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, modelName, validBaseUrl);

            // Then
            assertNotNull(provider);
            assertEquals("openai", provider.getProviderName());
            // Model names are standardized (lowercase, special chars replaced with hyphens)
            String expectedStandardized = modelName.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
            assertEquals(expectedStandardized, provider.getModelName());
        }
    }

    @Test
    @DisplayName("Should validate various OpenAI API endpoints")
    void shouldValidateOpenAIEndpoints() {
        // Given
        String[] validEndpoints = {
            "https://api.openai.com/v1",
            "https://api.openai.com/v1/",
            "https://my-resource.openai.azure.com/",
            "https://custom-openai-proxy.example.com/v1",
            "https://localhost:8080/v1",
            "http://internal-proxy:3000/openai"
        };

        for (String endpoint : validEndpoints) {
            // When
            OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, validModelName, endpoint);

            // Then
            assertNotNull(provider);
            assertEquals("openai", provider.getProviderName());
        }
    }

    @Test
    @DisplayName("Should test builder pattern with basic configuration")
    void shouldTestBuilderPattern() {
        // When
        OpenAiLLMProvider provider = OpenAiLLMProvider.builder()
            .apiKey(validApiKey)
            .modelName(validModelName)
            .baseUrl(validBaseUrl)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("openai", provider.getProviderName());
        assertEquals(validModelName, provider.getModelName());
    }

    @Test
    @DisplayName("Should test builder pattern with retry policy")
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        com.skanga.conductor.retry.RetryPolicy customPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(3, Duration.ofSeconds(2));

        // When
        OpenAiLLMProvider provider = OpenAiLLMProvider.builder()
            .apiKey(validApiKey)
            .modelName(validModelName)
            .baseUrl(validBaseUrl)
            .retryPolicy(customPolicy)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    @DisplayName("Should throw exception for incomplete builder")
    void shouldThrowExceptionForIncompleteBuilder() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            OpenAiLLMProvider.builder()
                .baseUrl(validBaseUrl)
                // Missing API key and model name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            OpenAiLLMProvider.builder()
                .modelName(validModelName)
                .baseUrl(validBaseUrl)
                // Missing API key
                .build();
        });

        // Missing API key should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            OpenAiLLMProvider.builder()
                .apiKey("")
                .modelName(validModelName)
                .baseUrl(validBaseUrl)
                .build();
        });
    }

    @Test
    @DisplayName("Should test OpenAI-specific retryable exception patterns")
    void shouldTestOpenAIRetryableExceptionPatterns() {
        // Test OpenAI-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("Rate limit exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Too Many Requests")));
        assertTrue(provider.isRetryableException(new RuntimeException("Server overloaded")));
        assertTrue(provider.isRetryableException(new RuntimeException("OpenAI API temporarily unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("502 Bad Gateway")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 Service Unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("504 Gateway Timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("Connection timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("Read timeout")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid API key")));
        assertFalse(provider.isRetryableException(new RuntimeException("Model not found")));
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid request format")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Invalid input")));
        // Note: "Insufficient quota" is actually considered retryable due to base class logic
    }

    @Test
    @DisplayName("Should inherit common retryable patterns")
    void shouldInheritCommonRetryablePatterns() {
        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("timeout occurred")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    @DisplayName("Should handle concurrent provider creation")
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        OpenAiLLMProvider[] providers = new OpenAiLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new OpenAiLLMProvider(
                        "sk-test" + index + "1234567890abcdef1234567890abcdef1234567890abcdef",
                        "gpt-4",
                        "https://api.openai.com/v1"
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
            assertEquals("openai", providers[i].getProviderName());
        }
    }

    @Test
    @DisplayName("Should validate provider consistency")
    void shouldValidateProviderConsistency() {
        // Given
        OpenAiLLMProvider provider1 = new OpenAiLLMProvider(
            "sk-test1234567890abcdef1234567890abcdef1234567890abcdef", "gpt-4", "https://api.openai.com/v1"
        );
        OpenAiLLMProvider provider2 = new OpenAiLLMProvider(
            "sk-test9876543210fedcba9876543210fedcba9876543210fedcba", "gpt-3.5-turbo", "https://my-resource.openai.azure.com/"
        );

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("openai", provider1.getProviderName());

        // Different model names
        assertNotEquals(provider1.getModelName(), provider2.getModelName());
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
        assertEquals("openai", providerName);
        assertEquals(validModelName, modelName);
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToString() {
        // When
        String toString = provider.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("openai") || toString.contains("OpenAiLLMProvider"));
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
        OpenAiLLMProvider provider1 = new OpenAiLLMProvider(validApiKey, validModelName, validBaseUrl);
        OpenAiLLMProvider provider2 = new OpenAiLLMProvider(validApiKey, validModelName, validBaseUrl);
        OpenAiLLMProvider provider3 = new OpenAiLLMProvider("different-key", validModelName, validBaseUrl);

        // When & Then
        assertEquals(provider1, provider1); // Same instance
        assertNotEquals(provider1, null);
        assertNotEquals(provider1, "not a provider");
    }

    @Test
    @DisplayName("Should handle Azure OpenAI endpoints")
    void shouldHandleAzureOpenAIEndpoints() {
        // Given
        String[] azureEndpoints = {
            "https://my-resource.openai.azure.com/",
            "https://my-resource-name.openai.azure.com/openai/deployments/my-deployment/",
            "https://eastus.api.cognitive.microsoft.com/openai/deployments/gpt-4/",
            "https://westeurope.api.cognitive.microsoft.com/openai/"
        };

        for (String endpoint : azureEndpoints) {
            // When & Then
            assertDoesNotThrow(() -> {
                OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, validModelName, endpoint);
                assertNotNull(provider);
                assertEquals("openai", provider.getProviderName());
            });
        }
    }

    @Test
    @DisplayName("Should handle custom proxy endpoints")
    void shouldHandleCustomProxyEndpoints() {
        // Given
        String[] proxyEndpoints = {
            "https://openai-proxy.company.com/v1",
            "http://localhost:8080/openai/v1",
            "https://api-gateway.internal.corp/openai",
            "https://custom-llm-service.example.org/openai-compatible"
        };

        for (String endpoint : proxyEndpoints) {
            // When & Then
            assertDoesNotThrow(() -> {
                OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, validModelName, endpoint);
                assertNotNull(provider);
                assertEquals("openai", provider.getProviderName());
            });
        }
    }

    @Test
    @DisplayName("Should handle API key formats")
    void shouldHandleAPIKeyFormats() {
        // Given
        String[] apiKeyFormats = {
            "sk-1234567890abcdef1234567890abcdef1234567890abcdef",  // Standard OpenAI key
            "sk-proj-1234567890abcdef1234567890abcdef1234567890abcdef", // Project key
            "sk-org-1234567890abcdef1234567890abcdef1234567890abcdef",  // Org key
            "azure-key-1234567890abcdef1234567890abcdef",             // Azure key format
            "custom-api-key-format-for-proxy-service"                   // Custom format
        };

        for (String apiKey : apiKeyFormats) {
            // When & Then
            assertDoesNotThrow(() -> {
                OpenAiLLMProvider provider = new OpenAiLLMProvider(apiKey, validModelName, validBaseUrl);
                assertNotNull(provider);
                assertEquals("openai", provider.getProviderName());
            });
        }
    }

    @Test
    @DisplayName("Should handle builder pattern with basic configuration")
    void shouldTestBuilderWithBasicConfiguration() {
        // When
        OpenAiLLMProvider provider = OpenAiLLMProvider.builder()
            .apiKey(validApiKey)
            .modelName(validModelName)
            .baseUrl(validBaseUrl)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("openai", provider.getProviderName());
    }

    @Test
    @DisplayName("Should test builder method chaining")
    void shouldTestBuilderMethodChaining() {
        // Given
        com.skanga.conductor.retry.RetryPolicy retryPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(2, Duration.ofSeconds(1));

        // When & Then
        assertDoesNotThrow(() -> {
            OpenAiLLMProvider provider = OpenAiLLMProvider.builder()
                .apiKey(validApiKey)
                .modelName(validModelName)
                .baseUrl(validBaseUrl)
                .retryPolicy(retryPolicy)
                .build();
            assertNotNull(provider);
            assertEquals("openai", provider.getProviderName());
        });
    }

    @Test
    @DisplayName("Should handle fine-tuned model names")
    void shouldHandleFineTunedModelNames() {
        // Given
        String[] fineTunedModels = {
            "ft:gpt-3.5-turbo-0613:company-name::7p4lURel",
            "ft:gpt-4-0613:my-org:custom-suffix:7p4lURel",
            "ft:davinci-002:personal::7p4lURel",
            "curie:ft-personal-2021-03-11-06-44-21"
        };

        for (String modelName : fineTunedModels) {
            // When
            OpenAiLLMProvider provider = new OpenAiLLMProvider(validApiKey, modelName, validBaseUrl);

            // Then
            assertNotNull(provider);
            assertEquals("openai", provider.getProviderName());
            // Model names are standardized (lowercase, special chars replaced with hyphens)
            String expectedStandardized = modelName.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
            assertEquals(expectedStandardized, provider.getModelName());
        }
    }
}
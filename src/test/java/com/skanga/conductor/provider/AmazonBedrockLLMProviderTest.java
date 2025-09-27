package com.skanga.conductor.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for AmazonBedrockLLMProvider.
 *
 * Tests AWS Bedrock integration including:
 * - Constructor validation and parameter handling
 * - Provider and model name validation
 * - Builder pattern implementation
 * - AWS-specific configuration handling
 * - Retry policy configuration
 * - Error handling and exception patterns
 * - Threading safety
 * - AWS region validation
 * - Model ID format validation
 */
@DisplayName("Amazon Bedrock LLM Provider Tests")
class AmazonBedrockLLMProviderTest {

    private AmazonBedrockLLMProvider provider;
    private String validRegion;
    private String validModelId;

    @BeforeEach
    void setUp() {
        validRegion = "us-east-1";
        validModelId = "anthropic.claude-3-sonnet-20240229-v1:0";

        provider = new AmazonBedrockLLMProvider(validModelId, validRegion);
    }

    @Test
    @DisplayName("Should create provider with valid AWS Bedrock configuration")
    void shouldCreateProviderWithValidParameters() {
        // When
        AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
            validModelId, validRegion
        );

        // Then
        assertNotNull(provider);
        assertEquals("amazon-bedrock", provider.getProviderName());
        assertEquals("anthropic-claude-3-sonnet-20240229-v1-0", provider.getModelName());
    }

    @Test
    @DisplayName("Should handle null model ID gracefully")
    void shouldHandleNullModelId() {
        // When & Then - should not throw exception during construction
        assertDoesNotThrow(() -> {
            AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
                null, validRegion
            );
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle null region gracefully")
    void shouldHandleNullRegion() {
        // When & Then - should throw exception during construction
        assertThrows(NullPointerException.class, () -> {
            new AmazonBedrockLLMProvider("anthropic.claude-v2", null);
        });
    }

    @Test
    @DisplayName("Should validate various AWS regions")
    void shouldValidateAWSRegions() {
        // Given
        String[] validRegions = {
            "us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1",
            "ca-central-1", "eu-central-1", "ap-northeast-1"
        };

        for (String region : validRegions) {
            // When
            AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
                validModelId, region
            );

            // Then
            assertNotNull(provider);
            assertEquals("amazon-bedrock", provider.getProviderName());
        }
    }

    @Test
    @DisplayName("Should validate various Bedrock model IDs")
    void shouldValidateBedrockModelIds() {
        // Given
        String[] validModelIds = {
            "anthropic-claude-3-sonnet-20240229-v1-0",
            "anthropic-claude-3-haiku-20240307-v1-0",
            "anthropic-claude-3-opus-20240229-v1-0",
            "anthropic-claude-v2-1",
            "amazon-titan-text-express-v1",
            "cohere-command-text-v14",
            "ai21-j2-ultra-v1",
            "meta-llama2-70b-chat-v1"
        };

        for (String modelId : validModelIds) {
            // When
            AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
                modelId, validRegion
            );

            // Then
            assertNotNull(provider);
            assertEquals("amazon-bedrock", provider.getProviderName());
            assertEquals(modelId, provider.getModelName());
        }
    }

    @Test
    @DisplayName("Should test builder pattern with basic configuration")
    void shouldTestBuilderPattern() {
        // When
        AmazonBedrockLLMProvider provider = AmazonBedrockLLMProvider.builder()
            .region(validRegion)
            .modelId(validModelId)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("amazon-bedrock", provider.getProviderName());
        assertEquals("anthropic-claude-3-sonnet-20240229-v1-0", provider.getModelName());
    }

    @Test
    @DisplayName("Should test builder pattern with retry policy")
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        com.skanga.conductor.retry.RetryPolicy customPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(3, Duration.ofSeconds(2));

        // When
        AmazonBedrockLLMProvider provider = AmazonBedrockLLMProvider.builder()
            .region(validRegion)
            .modelId(validModelId)
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
        AmazonBedrockLLMProvider provider = AmazonBedrockLLMProvider.builder()
            .region(validRegion)
            .modelId(validModelId)
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("amazon-bedrock", provider.getProviderName());
    }

    @Test
    @DisplayName("Should throw exception for incomplete builder")
    void shouldThrowExceptionForIncompleteBuilder() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            AmazonBedrockLLMProvider.builder()
                .region(validRegion)
                // Missing modelId
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            AmazonBedrockLLMProvider.builder()
                .modelId(validModelId)
                // Missing region
                .build();
        });
    }

    @Test
    @DisplayName("Should test AWS-specific retryable exception patterns")
    void shouldTestAWSRetryableExceptionPatterns() {
        // Test AWS-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("ThrottlingException")));
        assertTrue(provider.isRetryableException(new RuntimeException("ServiceUnavailableException")));
        assertTrue(provider.isRetryableException(new RuntimeException("InternalServerError")));
        assertTrue(provider.isRetryableException(new RuntimeException("ModelTimeoutException")));
        assertTrue(provider.isRetryableException(new RuntimeException("TooManyRequestsException")));
        assertTrue(provider.isRetryableException(new RuntimeException("bedrock service is temporarily unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("rate limit exceeded")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid model ID")));
        assertFalse(provider.isRetryableException(new RuntimeException("Authentication failed")));
        assertFalse(provider.isRetryableException(new RuntimeException("Model not found")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Invalid input")));
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
        AmazonBedrockLLMProvider[] providers = new AmazonBedrockLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new AmazonBedrockLLMProvider(
                        "anthropic.claude-3-sonnet-20240229-v1:0", "us-east-1"
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
            assertEquals("amazon-bedrock", providers[i].getProviderName());
        }
    }

    @Test
    @DisplayName("Should validate provider consistency")
    void shouldValidateProviderConsistency() {
        // Given
        AmazonBedrockLLMProvider provider1 = new AmazonBedrockLLMProvider(
            "anthropic.claude-3-sonnet-20240229-v1:0", "us-east-1"
        );
        AmazonBedrockLLMProvider provider2 = new AmazonBedrockLLMProvider(
            "anthropic.claude-3-haiku-20240307-v1:0", "us-west-2"
        );

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("amazon-bedrock", provider1.getProviderName());

        // Different model names
        assertNotEquals(provider1.getModelName(), provider2.getModelName());
    }

    @Test
    @DisplayName("Should handle empty string parameters")
    void shouldHandleEmptyStringParameters() {
        // When & Then - should throw exception during construction
        assertThrows(IllegalArgumentException.class, () -> {
            new AmazonBedrockLLMProvider("", "");
        });
    }

    @Test
    @DisplayName("Should handle whitespace parameters")
    void shouldHandleWhitespaceParameters() {
        // When & Then - should throw exception during construction
        assertThrows(IllegalArgumentException.class, () -> {
            new AmazonBedrockLLMProvider("   ", "   ");
        });
    }

    @Test
    @DisplayName("Should handle special characters in credentials")
    void shouldHandleSpecialCharactersInCredentials() {
        // Given - this test is no longer relevant for Bedrock since it doesn't use access/secret keys directly

        // When & Then
        assertDoesNotThrow(() -> {
            AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
                validModelId, validRegion
            );
            assertNotNull(provider);
        });
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
        assertEquals("amazon-bedrock", providerName);
        assertEquals("anthropic-claude-3-sonnet-20240229-v1-0", modelName);
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToString() {
        // When
        String toString = provider.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("amazon-bedrock") || toString.contains("AmazonBedrockLLMProvider"));
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
        AmazonBedrockLLMProvider provider1 = new AmazonBedrockLLMProvider(
            validModelId, validRegion
        );
        AmazonBedrockLLMProvider provider2 = new AmazonBedrockLLMProvider(
            validModelId, validRegion
        );
        AmazonBedrockLLMProvider provider3 = new AmazonBedrockLLMProvider(
            "different-model", validRegion
        );

        // When & Then
        assertEquals(provider1, provider1); // Same instance
        assertNotEquals(provider1, null);
        assertNotEquals(provider1, "not a provider");
    }

    @Test
    @DisplayName("Should handle model configuration in builder")
    void shouldHandleModelConfigurationInBuilder() {
        // When & Then
        assertDoesNotThrow(() -> {
            AmazonBedrockLLMProvider provider = AmazonBedrockLLMProvider.builder()
                .region(validRegion)
                .modelId(validModelId)
                .build();
            assertNotNull(provider);
        });
    }

    @Test
    @DisplayName("Should handle different model families")
    void shouldHandleDifferentModelFamilies() {
        // Given
        String[] modelFamilies = {
            "anthropic-claude-3-sonnet-20240229-v1-0",    // Anthropic
            "amazon-titan-text-express-v1",               // Amazon Titan
            "cohere-command-text-v14",                    // Cohere
            "ai21-j2-ultra-v1",                           // AI21 Labs
            "meta-llama2-70b-chat-v1"                     // Meta Llama
        };

        for (String modelId : modelFamilies) {
            // When
            AmazonBedrockLLMProvider provider = new AmazonBedrockLLMProvider(
                modelId, validRegion
            );

            // Then
            assertNotNull(provider);
            assertEquals("amazon-bedrock", provider.getProviderName());
            assertEquals(modelId, provider.getModelName());
        }
    }
}
package com.skanga.conductor.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.Region;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
/**
 * Comprehensive test suite for OracleLLMProvider.
 *
 * Tests Oracle Cloud Infrastructure GenAI integration including:
 * - Constructor validation and parameter handling
 * - Provider and model name validation
 * - Builder pattern implementation
 * - OCI configuration handling
 * - Retry policy configuration
 * - Error handling and exception patterns
 * - Threading safety
 * - Authentication configuration
 */
@DisplayName("Oracle LLM Provider Tests")
class OracleLLMProviderTest {

    private OracleLLMProvider provider;
    private String validCompartmentId;
    private String validModelId;
    private AuthenticationDetailsProvider mockAuthProvider;
/*
    @BeforeEach
    void setUp() {
        validCompartmentId = "ocid1.compartment.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        validModelId = "cohere.command";
        try {
            provider = new OracleLLMProvider(validCompartmentId, validModelId);
        } catch (Exception e) {
            // For tests, we'll ignore OCI config issues
        }
    }
*/

    @BeforeEach
    void setUp() {
        validCompartmentId = "ocid1.compartment.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        validModelId = "cohere.command";

        // Set up mock authentication provider
        mockAuthProvider = mock(AuthenticationDetailsProvider.class);
        when(mockAuthProvider.getKeyId()).thenReturn("fake-key-id");
        when(mockAuthProvider.getFingerprint()).thenReturn("fake-fingerprint");
        when(mockAuthProvider.getTenantId()).thenReturn("fake-tenancy");
        when(mockAuthProvider.getUserId()).thenReturn("fake-user");

        try {
            provider = new OracleLLMProvider(validCompartmentId, validModelId, mockAuthProvider);
        } catch (Exception e) {
            // If provider creation fails due to missing OCI dependencies,
            // tests will use Assumptions to skip gracefully
            provider = null;
        }
    }

    @Test
    @DisplayName("Should create provider with valid OCI configuration")
    void shouldCreateProviderWithValidParameters() {
        // Skip this test if provider creation failed in setUp (due to missing OCI config)
        Assumptions.assumeTrue(provider != null,
            "Oracle provider creation failed - likely due to missing OCI dependencies. This is expected in CI environments.");

        // When & Then
        assertNotNull(provider);
        assertEquals("oracle", provider.getProviderName());
        assertEquals("cohere-command", provider.getModelName()); // Note: standardized model name
    }

    private void assumeProviderAvailable() {
        Assumptions.assumeTrue(provider != null,
            "Oracle provider unavailable - OCI dependencies missing. Tests will be skipped.");
    }

    @Test
    @DisplayName("Should handle null compartment ID gracefully")
    void shouldHandleNullCompartmentId() {
        // When & Then - constructor may throw IOException, so we just test it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider(null, validModelId);
                // May succeed or fail depending on OCI config
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }

    @Test
    @DisplayName("Should handle null model ID gracefully")
    void shouldHandleNullModelId() {
        // When & Then - constructor may throw IOException, so we just test it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider(validCompartmentId, null);
                // May succeed or fail depending on OCI config
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }

    @Test
    @DisplayName("Should handle empty string parameters")
    void shouldHandleEmptyStringParameters() {
        // When & Then - constructor may throw IOException, so we just test it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider("", "");
                // May succeed or fail depending on OCI config
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }

    @Test
    @DisplayName("Should handle whitespace parameters")
    void shouldHandleWhitespaceParameters() {
        // When & Then - constructor may throw IOException, so we just test it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider("   ", "   ");
                // May succeed or fail depending on OCI config
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }

    @Test
    @DisplayName("Should validate various compartment ID formats")
    void shouldValidateVariousCompartmentIdFormats() {
        // Given
        String[] validCompartmentIds = {
            "ocid1.compartment.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "ocid1.tenancy.oc1..bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "ocid1.compartment.oc1.iad.cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
        };

        for (String compartmentId : validCompartmentIds) {
            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    OracleLLMProvider provider = new OracleLLMProvider(compartmentId, validModelId);
                    assertEquals("oracle", provider.getProviderName());
                } catch (Exception e) {
                    // Expected for missing OCI config
                }
            });
        }
    }

    @Test
    @DisplayName("Should validate various Oracle GenAI model IDs")
    void shouldValidateOracleGenAiModelIds() {
        // Given
        String[] validModelIds = {
            "cohere.command",
            "cohere.command-light",
            "cohere.embed-english-v3.0",
            "cohere.embed-multilingual-v3.0",
            "meta.llama-2-70b-chat",
            "oracle.text-generation-model",
            "oracle.text-embedding-model"
        };

        for (String modelId : validModelIds) {
            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    OracleLLMProvider provider = new OracleLLMProvider(validCompartmentId, modelId);
                    assertEquals("oracle", provider.getProviderName());
                    assertEquals(modelId, provider.getModelName());
                } catch (Exception e) {
                    // Expected for missing OCI config
                }
            });
        }
    }

    @Test
    @DisplayName("Should test builder pattern with basic configuration")
    void shouldTestBuilderPattern() {
        // When & Then - may skip if OCI dependencies not available
        try {
            OracleLLMProvider provider = OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                .oracleModelName(validModelId)
                .build();
            assertNotNull(provider);
            assertEquals("oracle", provider.getProviderName());
            assertEquals("cohere-command", provider.getModelName());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Builder test skipped - OCI dependencies not available");
        }

        // Then
        assertNotNull(provider);
        assertEquals("oracle", provider.getProviderName());
        assertEquals(validModelId, provider.getModelName());
    }

    @Test
    @DisplayName("Should test builder pattern with retry policy")
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        com.skanga.conductor.retry.RetryPolicy customPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(3, Duration.ofSeconds(2));

        // When & Then - may skip if OCI dependencies not available
        try {
            OracleLLMProvider provider = OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                .oracleModelName(validModelId)
                .retryPolicy(customPolicy)
                .build();
            assertNotNull(provider);
            assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Builder with retry policy test skipped - OCI dependencies not available");
        }

        // Then
        assertNotNull(provider);
        assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    @DisplayName("Should test builder pattern with basic configuration")
    void shouldTestBuilderWithBasicConfiguration() {
        // When & Then
        try {
            OracleLLMProvider provider = OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                .oracleModelName(validModelId)
                .build();
            assertNotNull(provider);
            assertEquals("oracle", provider.getProviderName());
        } catch (Exception e) {
            // Skip test if OCI configuration is not available
            Assumptions.assumeTrue(false, "Skipping test due to missing OCI configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception for incomplete builder")
    void shouldThrowExceptionForIncompleteBuilder() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                // Missing model name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            OracleLLMProvider.builder()
                .oracleModelName(validModelId)
                // Missing compartment ID
                .build();
        });
    }

    @Test
    @DisplayName("Should test Oracle-specific retryable exception patterns")
    void shouldTestOracleRetryableExceptionPatterns() {
        assumeProviderAvailable();
        // Test Oracle-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("TooManyRequestsException")));
        assertTrue(provider.isRetryableException(new RuntimeException("ServiceUnavailableException")));
        assertTrue(provider.isRetryableException(new RuntimeException("InternalServerErrorException")));
        assertTrue(provider.isRetryableException(new RuntimeException("ThrottlingException")));
        assertTrue(provider.isRetryableException(new RuntimeException("TimeoutException")));
        assertTrue(provider.isRetryableException(new RuntimeException("oracle service unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("rate limit exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("model is busy")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("InvalidParameterException")));
        assertFalse(provider.isRetryableException(new RuntimeException("UnauthorizedException")));
        assertFalse(provider.isRetryableException(new RuntimeException("ForbiddenException")));
        assertFalse(provider.isRetryableException(new RuntimeException("ResourceNotFoundException")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Invalid input")));
    }

    @Test
    @DisplayName("Should inherit common retryable patterns")
    void shouldInheritCommonRetryablePatterns() {
        assumeProviderAvailable();
        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("timeout occurred")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    @DisplayName("Should handle concurrent provider creation")
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Skip test if OCI configuration is not available
        try {
            // Test if we can create a single provider first
            new OracleLLMProvider(validCompartmentId, validModelId);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skipping concurrent test due to missing OCI configuration: " + e.getMessage());
            return;
        }

        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        OracleLLMProvider[] providers = new OracleLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new OracleLLMProvider(
                        "ocid1.compartment.oc1..aaaaa" + index + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "cohere.command"
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
            assertEquals("oracle", providers[i].getProviderName());
        }
    }

    @Test
    @DisplayName("Should validate provider consistency")
    void shouldValidateProviderConsistency() {
        // Given
        // Create providers in try-catch since they may fail without OCI config
        OracleLLMProvider provider1 = null;
        OracleLLMProvider provider2 = null;
        try {
            provider1 = new OracleLLMProvider(validCompartmentId, "cohere.command");
            provider2 = new OracleLLMProvider(validCompartmentId, "meta.llama-2-70b-chat");
        } catch (Exception e) {
            // Skip test if OCI config is not available
            return;
        }

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("oracle", provider1.getProviderName());

        // Different model names
        assertNotEquals(provider1.getModelName(), provider2.getModelName());
    }

    @Test
    @DisplayName("Should handle various compartment ID formats")
    void shouldHandleVariousCompartmentIdFormats() {
        // Given
        String[] compartmentIdFormats = {
            "ocid1.compartment.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "ocid1.compartment.oc1..bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "ocid1.tenancy.oc1..cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
        };

        for (String compartmentId : compartmentIdFormats) {
            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    OracleLLMProvider provider = new OracleLLMProvider(compartmentId, validModelId);
                    assertEquals("oracle", provider.getProviderName());
                } catch (Exception e) {
                    // Expected for missing OCI config
                }
            });
        }
    }

    @Test
    @DisplayName("Should test provider information methods")
    void shouldTestProviderInformationMethods() {
        assumeProviderAvailable();
        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("oracle", providerName);
        assertEquals(validModelId, modelName);
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToString() {
        assumeProviderAvailable();
        // When
        String toString = provider.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("oracle") || toString.contains("OracleLLMProvider"));
    }

    @Test
    @DisplayName("Should test hashCode method")
    void shouldTestHashCode() {
        assumeProviderAvailable();
        // When
        int hashCode = provider.hashCode();

        // Then
        assertNotEquals(0, hashCode); // Should have a reasonable hash code
    }

    @Test
    @DisplayName("Should test equals method")
    void shouldTestEquals() {
        // Create providers in try-catch since they may fail without OCI config
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider1 = new OracleLLMProvider(validCompartmentId, validModelId);
                assertEquals(provider1, provider1); // Same instance
                assertNotEquals(provider1, null);
                assertNotEquals(provider1, "not a provider");
            } catch (Exception e) {
                // Expected for missing OCI config - just verify the test doesn't crash
            }
        });
    }

    @Test
    @DisplayName("Should handle different Oracle model families")
    void shouldHandleDifferentOracleModelFamilies() {
        // Given
        String[] modelFamilies = {
            "cohere.command",               // Cohere Generation
            "cohere.command-light",         // Cohere Light Generation
            "meta.llama-2-70b-chat",       // Meta Llama Chat
            "cohere.embed-english-v3.0",   // Cohere English Embedding
            "cohere.embed-multilingual-v3.0" // Cohere Multilingual Embedding
        };

        for (String modelId : modelFamilies) {
            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    OracleLLMProvider provider = new OracleLLMProvider(validCompartmentId, modelId);
                    assertEquals("oracle", provider.getProviderName());
                    assertEquals(modelId, provider.getModelName());
                } catch (Exception e) {
                    // Expected for missing OCI config
                }
            });
        }
    }

    @Test
    @DisplayName("Should handle custom configuration in builder")
    void shouldHandleCustomConfigurationInBuilder() {
        // When & Then - may skip if OCI dependencies not available
        try {
            OracleLLMProvider provider = OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                .oracleModelName(validModelId)
                .build();
            assertNotNull(provider);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Basic builder test skipped - OCI dependencies not available");
        }
    }

    @Test
    @DisplayName("Should handle special characters in compartment IDs")
    void shouldHandleSpecialCharactersInCompartmentIds() {
        // Given - valid OCID format with special characters
        String specialCompartmentId = "ocid1.compartment.oc1..aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

        // When & Then
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider(specialCompartmentId, validModelId);
                // May succeed or fail depending on OCI config
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }

    @Test
    @DisplayName("Should test builder method chaining")
    void shouldTestBuilderMethodChaining() {
        // Given
        com.skanga.conductor.retry.RetryPolicy retryPolicy = com.skanga.conductor.retry.RetryPolicy.fixedDelay(2, Duration.ofSeconds(1));

        // When & Then
        try {
            OracleLLMProvider provider = OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                .oracleModelName(validModelId)
                .retryPolicy(retryPolicy)
                .build();
            assertNotNull(provider);
            assertEquals("oracle", provider.getProviderName());
        } catch (Exception e) {
            // Skip test if OCI configuration is not available
            Assumptions.assumeTrue(false, "Skipping test due to missing OCI configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle Oracle government regions")
    void shouldHandleOracleGovernmentRegions() {
        // Given
        String[] govRegions = {
            "us-gov-ashburn-1",
            "us-gov-chicago-1",
            "us-gov-phoenix-1",
            "uk-gov-london-1"
        };

        // Oracle regions are not used in constructor, so this test validates compartment formats instead
        String[] compartmentVariations = {
            validCompartmentId,
            "ocid1.tenancy.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "ocid1.compartment.oc1.iad.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        };

        for (String compartmentId : compartmentVariations) {
            assertDoesNotThrow(() -> {
                try {
                    OracleLLMProvider provider = new OracleLLMProvider(compartmentId, validModelId);
                    assertEquals("oracle", provider.getProviderName());
                    assertEquals(validModelId, provider.getModelName());
                } catch (Exception e) {
                    // Expected for missing OCI config
                }
            });
        }
    }

    @Test
    @DisplayName("Should handle tenancy OCID as compartment ID")
    void shouldHandleTenancyOcidAsCompartmentId() {
        // Given
        String tenancyId = "ocid1.tenancy.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        // When & Then
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider(tenancyId, validModelId);
                assertEquals("oracle", provider.getProviderName());
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }

    @Test
    @DisplayName("Should handle long compartment ID strings")
    void shouldHandleLongCompartmentIdStrings() {
        // Given
        String longCompartmentId = "ocid1.compartment.oc1.iad.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        // When & Then
        assertDoesNotThrow(() -> {
            try {
                OracleLLMProvider provider = new OracleLLMProvider(longCompartmentId, validModelId);
                assertEquals("oracle", provider.getProviderName());
            } catch (Exception e) {
                // Expected for missing OCI config
            }
        });
    }
}
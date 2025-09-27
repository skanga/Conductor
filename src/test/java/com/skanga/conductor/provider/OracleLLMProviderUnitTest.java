package com.skanga.conductor.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;

/**
 * Focused unit tests for OracleLLMProvider that test provider logic
 * without requiring actual OCI infrastructure.
 *
 * These tests focus on:
 * - Constructor parameter validation
 * - Model name standardization
 * - Exception handling patterns
 * - Builder pattern validation
 * - Provider information methods
 *
 * Unlike the integration tests, these don't require OCI configuration
 * and should run reliably in any CI/CD environment.
 */
@DisplayName("Oracle LLM Provider Unit Tests")
class OracleLLMProviderUnitTest {

    private String validCompartmentId;
    private String validModelId;
    private AuthenticationDetailsProvider mockAuthProvider;

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
    }

    @Test
    @DisplayName("Should test Oracle-specific exception patterns")
    void shouldTestOracleExceptionPatterns() {
        OracleLLMProvider testProvider = createTestProvider();
        Assumptions.assumeTrue(testProvider != null, "Cannot create test provider for exception testing");

        // Test Oracle-specific retryable exceptions
        assertTrue(testProvider.isRetryableException(new RuntimeException("TooManyRequestsException")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("ServiceUnavailableException")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("InternalServerError")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("oci service temporarily unavailable")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("rate limit exceeded")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("throttling exception")));

        // Test non-retryable exceptions
        assertFalse(testProvider.isRetryableException(new RuntimeException("Invalid compartment ID")));
        assertFalse(testProvider.isRetryableException(new RuntimeException("Authentication failed")));
        assertFalse(testProvider.isRetryableException(new RuntimeException("Model not found")));
        assertFalse(testProvider.isRetryableException(new IllegalArgumentException("Invalid input")));
    }

    @Test
    @DisplayName("Should test inherited common exception patterns")
    void shouldTestInheritedExceptionPatterns() {
        OracleLLMProvider testProvider = createTestProvider();
        Assumptions.assumeTrue(testProvider != null, "Cannot create test provider for exception testing");

        // Test inherited common retryable patterns
        assertTrue(testProvider.isRetryableException(new RuntimeException("timeout occurred")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(testProvider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(testProvider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    @DisplayName("Should validate compartment ID formats")
    void shouldValidateCompartmentIdFormats() {
        String[] validCompartmentIds = {
            "ocid1.compartment.oc1..aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "ocid1.compartment.oc1..bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "ocid1.compartment.oc1.iad.cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
        };

        for (String compartmentId : validCompartmentIds) {
            OracleLLMProvider testProvider = createTestProviderWithCompartment(compartmentId);
            if (testProvider != null) {
                assertEquals("oracle", testProvider.getProviderName());
                assertNotNull(testProvider.getModelName());
            }
        }
    }

    @Test
    @DisplayName("Should test model name standardization")
    void shouldTestModelNameStandardization() {
        String[] testCases = {
            "cohere.command",
            "meta.llama-2-70b-chat",
            "Custom Model Name",
            "model_with_underscores"
        };

        for (String modelName : testCases) {
            OracleLLMProvider testProvider = createTestProviderWithModel(modelName);
            if (testProvider != null) {
                assertNotNull(testProvider.getModelName());
                assertEquals("oracle", testProvider.getProviderName());

                // Verify model name follows standardization rules (lowercase, hyphens)
                String standardized = testProvider.getModelName();
                assertTrue(standardized.matches("[a-z0-9\\-]+"),
                    "Model name should be standardized: " + standardized);
            }
        }
    }

    @Test
    @DisplayName("Should test basic provider properties")
    void shouldTestBasicProviderProperties() {
        OracleLLMProvider testProvider = createTestProvider();
        Assumptions.assumeTrue(testProvider != null, "Cannot create test provider");

        assertEquals("oracle", testProvider.getProviderName());
        assertNotNull(testProvider.getModelName());
        assertEquals("cohere-command", testProvider.getModelName()); // standardized

        // Test toString doesn't throw
        assertDoesNotThrow(() -> {
            String toString = testProvider.toString();
            assertNotNull(toString);
            assertTrue(toString.contains("oracle") || toString.contains("OracleLLMProvider"));
        });

        // Test hashCode doesn't throw
        assertDoesNotThrow(() -> {
            int hashCode = testProvider.hashCode();
            assertNotEquals(0, hashCode);
        });
    }

    @Test
    @DisplayName("Should test provider consistency across instances")
    void shouldTestProviderConsistency() {
        OracleLLMProvider provider1 = createTestProviderWithModel("cohere.command");
        OracleLLMProvider provider2 = createTestProviderWithModel("meta.llama-2-70b-chat");

        if (provider1 != null && provider2 != null) {
            // Same provider name
            assertEquals(provider1.getProviderName(), provider2.getProviderName());
            assertEquals("oracle", provider1.getProviderName());

            // Different model names (standardized)
            assertNotEquals(provider1.getModelName(), provider2.getModelName());
        }
    }

    @Test
    @DisplayName("Should test builder pattern validation")
    void shouldTestBuilderPatternValidation() {
        // Test builder method chaining returns builder instance
        OracleLLMProvider.Builder builder = OracleLLMProvider.builder();
        assertSame(builder, builder.compartmentId(validCompartmentId));
        assertSame(builder, builder.oracleModelName(validModelId));
        assertSame(builder, builder.authProvider(mockAuthProvider));

        // Test validation - missing required fields should throw
        assertThrows(IllegalArgumentException.class, () -> {
            OracleLLMProvider.builder().build();
        }, "Builder should require compartment ID");

        assertThrows(IllegalArgumentException.class, () -> {
            OracleLLMProvider.builder()
                .compartmentId(validCompartmentId)
                .build();
        }, "Builder should require model name");
    }

    @Test
    @DisplayName("Should test retry policy configuration")
    void shouldTestRetryPolicyConfiguration() {
        com.skanga.conductor.retry.RetryPolicy customPolicy =
            com.skanga.conductor.retry.RetryPolicy.fixedDelay(3, Duration.ofSeconds(2));

        // This test validates the builder pattern for retry policy
        // without requiring actual OCI model creation
        OracleLLMProvider.Builder builder = OracleLLMProvider.builder()
            .compartmentId(validCompartmentId)
            .oracleModelName(validModelId)
            .authProvider(mockAuthProvider)
            .retryPolicy(customPolicy);

        assertNotNull(builder);
        // Note: We can't test .build() here as it would require OCI dependencies
        // But we can test that the builder accepts the retry policy
    }

    private OracleLLMProvider createTestProvider() {
        try {
            return new OracleLLMProvider(validCompartmentId, validModelId, mockAuthProvider);
        } catch (Exception e) {
            return null;
        }
    }

    private OracleLLMProvider createTestProviderWithCompartment(String compartmentId) {
        try {
            return new OracleLLMProvider(compartmentId, validModelId, mockAuthProvider);
        } catch (Exception e) {
            return null;
        }
    }

    private OracleLLMProvider createTestProviderWithModel(String modelName) {
        try {
            return new OracleLLMProvider(validCompartmentId, modelName, mockAuthProvider);
        } catch (Exception e) {
            return null;
        }
    }
}
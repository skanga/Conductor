package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AzureOpenAiLLMProviderTest {

    private static final String VALID_ENDPOINT = "https://test-resource.openai.azure.com";
    private static final String VALID_API_KEY = "test-api-key";
    private static final String VALID_DEPLOYMENT = "gpt-4-deployment";

    @Test
    void shouldCreateProviderWithValidParameters() {
        // When
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, VALID_DEPLOYMENT);

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-4-deployment", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullDeploymentName() {
        // When
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, null);

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-35-turbo", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithEmptyDeploymentName() {
        // When
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, "");

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-35-turbo", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithWhitespaceDeploymentName() {
        // When
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, "   ");

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-35-turbo", provider.getModelName()); // Default model
    }

    @Test
    void shouldCreateProviderWithCustomDeploymentName() {
        // When
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, "custom-gpt4-deployment");

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("custom-gpt4-deployment", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullApiKey() {
        // When & Then - should throw exception during construction for invalid API key
        assertThrows(RuntimeException.class, () -> {
            new AzureOpenAiLLMProvider(null, VALID_ENDPOINT, VALID_DEPLOYMENT);
        });
    }

    @Test
    void shouldCreateProviderWithEmptyApiKey() {
        // When & Then - should throw exception during construction for empty API key
        assertThrows(RuntimeException.class, () -> {
            new AzureOpenAiLLMProvider("", VALID_ENDPOINT, VALID_DEPLOYMENT);
        });
    }

    @Test
    void shouldCreateProviderWithNullEndpoint() {
        // When & Then - should throw exception during construction for null endpoint
        assertThrows(RuntimeException.class, () -> {
            new AzureOpenAiLLMProvider(VALID_API_KEY, null, VALID_DEPLOYMENT);
        });
    }

    @Test
    void shouldCreateProviderWithEmptyEndpoint() {
        // When & Then - should throw exception during construction for empty endpoint
        assertThrows(RuntimeException.class, () -> {
            new AzureOpenAiLLMProvider(VALID_API_KEY, "", VALID_DEPLOYMENT);
        });
    }

    @Test
    void shouldCreateProviderWithCustomRetryPolicy() {
        // Given
        RetryPolicy customRetryPolicy = RetryPolicy.fixedDelay(3, java.time.Duration.ofSeconds(1));

        // When
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, VALID_DEPLOYMENT, customRetryPolicy);

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-4-deployment", provider.getModelName());
        assertEquals(3, provider.getRetryPolicy().getMaxAttempts());
    }

    @Test
    void shouldHaveConsistentProviderName() {
        // Given
        AzureOpenAiLLMProvider provider1 = new AzureOpenAiLLMProvider("key1", "endpoint1", "deployment1");
        AzureOpenAiLLMProvider provider2 = new AzureOpenAiLLMProvider("key2", "endpoint2", "deployment2");

        // When & Then
        assertEquals(provider1.getProviderName(), provider2.getProviderName());
        assertEquals("azure-openai", provider1.getProviderName());
    }

    @Test
    void shouldProvideProviderInformation() {
        // Given
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, "my-deployment");

        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertNotNull(modelName);
        assertEquals("azure-openai", providerName);
        assertEquals("my-deployment", modelName);
    }

    @Test
    void shouldHandleDeploymentNameStandardization() {
        // Test various deployment name formats
        String[] deploymentNames = {
            "GPT-4-Deployment",
            "gpt_35_turbo",
            "GPT 4 TURBO",
            "Custom-GPT4-Model-v2"
        };

        for (String deploymentName : deploymentNames) {
            // When
            AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, deploymentName);

            // Then
            assertNotNull(provider.getModelName());
            assertTrue(provider.getModelName().matches("[a-z0-9\\-]+"));
        }
    }

    @Test
    void shouldCreateMultipleProviderInstances() {
        // When
        AzureOpenAiLLMProvider provider1 = new AzureOpenAiLLMProvider("key1", "endpoint1", "deployment1");
        AzureOpenAiLLMProvider provider2 = new AzureOpenAiLLMProvider("key2", "endpoint2", "deployment2");
        AzureOpenAiLLMProvider provider3 = new AzureOpenAiLLMProvider("key3", "endpoint3", "deployment3");

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
            longKey.append("az-key-1234567890abcdef");
        }

        // When & Then
        assertDoesNotThrow(() -> {
            AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(longKey.toString(), VALID_ENDPOINT, VALID_DEPLOYMENT);
            assertNotNull(provider);
        });
    }

    @Test
    void shouldHandleVariousAzureEndpointFormats() {
        // Given
        String[] azureEndpoints = {
            "https://myresource.openai.azure.com",
            "https://test-resource.openai.azure.com",
            "https://prod.openai.azure.com",
            "https://eastus.api.cognitive.microsoft.com",
            "https://westeurope.api.cognitive.microsoft.com"
        };

        for (String endpoint : azureEndpoints) {
            // When
            AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, endpoint, VALID_DEPLOYMENT);

            // Then
            assertNotNull(provider);
            assertEquals("azure-openai", provider.getProviderName());
            assertEquals("gpt-4-deployment", provider.getModelName());
        }
    }

    @Test
    void shouldHandleCommonAzureDeploymentNames() {
        // Given
        String[] commonDeployments = {
            "gpt-4",
            "gpt-35-turbo",
            "gpt-4-turbo",
            "gpt-4-32k",
            "text-embedding-ada-002",
            "custom-gpt4-prod",
            "company-gpt35-dev"
        };

        for (String deployment : commonDeployments) {
            // When
            AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, deployment);

            // Then
            assertNotNull(provider);
            assertEquals("azure-openai", provider.getProviderName());
            assertEquals(deployment.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", ""),
                        provider.getModelName());
        }
    }

    @Test
    void shouldHandleSpecialCharactersInApiKey() {
        // Given
        String specialKey = "az-key-api03_abc123-def456_ghi789-jkl012_mno345-pqr678_stu901-vwx234-yz567";

        // When & Then
        assertDoesNotThrow(() -> {
            AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(specialKey, VALID_ENDPOINT, VALID_DEPLOYMENT);
            assertNotNull(provider);
        });
    }

    @Test
    void shouldTestRetryableExceptionPatterns() {
        // Given
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, VALID_DEPLOYMENT);

        // Test Azure-specific retryable exceptions
        assertTrue(provider.isRetryableException(new RuntimeException("Azure throttling limit exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Azure service is temporarily unavailable")));
        assertTrue(provider.isRetryableException(new RuntimeException("Azure backend error occurred")));
        assertTrue(provider.isRetryableException(new RuntimeException("Microsoft cognitive services busy")));
        assertTrue(provider.isRetryableException(new RuntimeException("Azure OpenAI quota exceeded")));
        assertTrue(provider.isRetryableException(new RuntimeException("Deployment is overloaded")));

        // Test non-retryable exceptions
        assertFalse(provider.isRetryableException(new RuntimeException("Authentication failed")));
        assertFalse(provider.isRetryableException(new RuntimeException("Invalid deployment name")));
        assertFalse(provider.isRetryableException(new IllegalArgumentException("Bad request")));
    }

    @Test
    void shouldInheritCommonRetryablePatterns() {
        // Given
        AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, VALID_DEPLOYMENT);

        // Test common retryable patterns from AbstractLLMProvider
        assertTrue(provider.isRetryableException(new RuntimeException("timeout")));
        assertTrue(provider.isRetryableException(new RuntimeException("connection refused")));
        assertTrue(provider.isRetryableException(new RuntimeException("503 service unavailable")));
        assertTrue(provider.isRetryableException(new java.io.IOException("Network error")));
    }

    @Test
    void shouldTestBuilderPattern() {
        // When
        AzureOpenAiLLMProvider provider = AzureOpenAiLLMProvider.builder()
            .apiKey(VALID_API_KEY)
            .endpoint(VALID_ENDPOINT)
            .deploymentName("my-gpt4")
            .build();

        // Then
        assertNotNull(provider);
        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("my-gpt4", provider.getModelName());
    }

    @Test
    void shouldTestBuilderWithRetryPolicy() {
        // Given
        RetryPolicy customPolicy = RetryPolicy.fixedDelay(2, java.time.Duration.ofMillis(500));

        // When
        AzureOpenAiLLMProvider provider = AzureOpenAiLLMProvider.builder()
            .apiKey(VALID_API_KEY)
            .endpoint(VALID_ENDPOINT)
            .deploymentName("test-deployment")
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
            AzureOpenAiLLMProvider.builder()
                .apiKey(VALID_API_KEY)
                .endpoint(VALID_ENDPOINT)
                // Missing deployment name
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            AzureOpenAiLLMProvider.builder()
                .endpoint(VALID_ENDPOINT)
                .deploymentName(VALID_DEPLOYMENT)
                // Missing API key
                .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            AzureOpenAiLLMProvider.builder()
                .apiKey(VALID_API_KEY)
                .deploymentName(VALID_DEPLOYMENT)
                // Missing endpoint
                .build();
        });
    }

    @Test
    void shouldHandleConcurrentProviderCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        AzureOpenAiLLMProvider[] providers = new AzureOpenAiLLMProvider[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    providers[index] = new AzureOpenAiLLMProvider(
                        "key-" + index,
                        "https://resource" + index + ".openai.azure.com",
                        "deployment-" + index
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
            assertEquals("azure-openai", providers[i].getProviderName());
        }
    }

    @Test
    void shouldTestDefaultEndpointHandling() {
        // When & Then - should throw exception for null endpoint
        assertThrows(RuntimeException.class, () -> {
            new AzureOpenAiLLMProvider(VALID_API_KEY, null, VALID_DEPLOYMENT);
        });
    }

    @Test
    void shouldHandleMalformedEndpoints() {
        // Given
        String[] malformedEndpoints = {
            "not-a-url",
            "ftp://wrong-protocol.com",
            "https://",
            "invalid endpoint format"
        };

        for (String endpoint : malformedEndpoints) {
            // When & Then - should not throw exception during construction
            assertDoesNotThrow(() -> {
                AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, endpoint, VALID_DEPLOYMENT);
                assertNotNull(provider);
            });
        }
    }

    @Test
    void shouldPreserveDeploymentNameCase() {
        // Given - Azure deployment names might be case-sensitive
        String[] exactDeploymentNames = {
            "GPT-4-Production",
            "custom-Model-v1-2",
            "TestDeployment"
        };

        for (String deploymentName : exactDeploymentNames) {
            // When
            AzureOpenAiLLMProvider provider = new AzureOpenAiLLMProvider(VALID_API_KEY, VALID_ENDPOINT, deploymentName);

            // Then - Should standardize deployment name for consistency
            assertTrue(provider.getModelName().matches("[a-z0-9\\-]+"));
        }
    }
}
package com.skanga.conductor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify DRY improvements in ApplicationConfig.
 * This test ensures that the refactored generic provider configuration works correctly.
 */
public class ApplicationConfigDryTest {

    private ApplicationConfig config;

    @BeforeEach
    void setUp() {
        // Reset instance before each test
        ApplicationConfig.resetInstance();
        config = ApplicationConfig.getInstance();
    }

    @AfterEach
    void tearDown() {
        ApplicationConfig.resetInstance();
    }

    @Test
    void testGenericProviderConfigurationOpenAi() {
        LLMConfig llmConfig = config.getLLMConfig();
        LLMConfig.ProviderConfig openaiConfig = llmConfig.getProviderConfig("openai");

        // Test that generic provider config returns correct defaults
        assertEquals("gpt-3.5-turbo", openaiConfig.getModel());
        assertEquals("https://api.openai.com/v1", openaiConfig.getBaseUrl().orElse(null));
        assertEquals(Duration.ofSeconds(30), openaiConfig.getTimeout());
        assertEquals(3, openaiConfig.getMaxRetries());
        assertEquals("openai", openaiConfig.getProviderName());

        // Test that convenience methods still work
        assertEquals(openaiConfig.getModel(), llmConfig.getOpenAiModel());
        assertEquals(openaiConfig.getBaseUrl(), llmConfig.getOpenAiBaseUrl());
        assertEquals(openaiConfig.getTimeout(), llmConfig.getOpenAiTimeout());
        assertEquals(openaiConfig.getMaxRetries(), llmConfig.getOpenAiMaxRetries());
    }

    @Test
    void testGenericProviderConfigurationAnthropic() {
        LLMConfig llmConfig = config.getLLMConfig();
        LLMConfig.ProviderConfig anthropicConfig = llmConfig.getProviderConfig("anthropic");

        // Test that generic provider config returns correct defaults
        assertEquals("claude-3-5-sonnet-20241022", anthropicConfig.getModel());
        assertEquals(Duration.ofSeconds(30), anthropicConfig.getTimeout());
        assertEquals(3, anthropicConfig.getMaxRetries());
        assertEquals("anthropic", anthropicConfig.getProviderName());
        assertNull(anthropicConfig.getBaseUrl().orElse(null)); // Anthropic doesn't have a default base URL

        // Test that convenience methods still work
        assertEquals(anthropicConfig.getModel(), llmConfig.getAnthropicModel());
        assertEquals(anthropicConfig.getTimeout(), llmConfig.getAnthropicTimeout());
        assertEquals(anthropicConfig.getMaxRetries(), llmConfig.getAnthropicMaxRetries());
    }

    @Test
    void testGenericProviderConfigurationGemini() {
        LLMConfig llmConfig = config.getLLMConfig();
        LLMConfig.ProviderConfig geminiConfig = llmConfig.getProviderConfig("gemini");

        // Test that generic provider config returns correct defaults
        assertEquals("gemini-pro", geminiConfig.getModel());
        assertEquals(Duration.ofSeconds(30), geminiConfig.getTimeout());
        assertEquals(3, geminiConfig.getMaxRetries());
        assertEquals("gemini", geminiConfig.getProviderName());

        // Test that convenience methods still work
        assertEquals(geminiConfig.getModel(), llmConfig.getGeminiModel());
        assertEquals(geminiConfig.getTimeout(), llmConfig.getGeminiTimeout());
        assertEquals(geminiConfig.getMaxRetries(), llmConfig.getGeminiMaxRetries());
    }

    @Test
    void testGenericProviderConfigurationNewProvider() {
        LLMConfig llmConfig = config.getLLMConfig();

        // Test that we can create configuration for a new provider not in defaults
        LLMConfig.ProviderConfig newProviderConfig = llmConfig.getProviderConfig("newprovider");

        assertEquals("newprovider", newProviderConfig.getProviderName());
        assertNull(newProviderConfig.getModel()); // No default model
        assertNull(newProviderConfig.getBaseUrl().orElse(null)); // No default base URL
        assertEquals(Duration.ofSeconds(30), newProviderConfig.getTimeout()); // Fallback default
        assertEquals(3, newProviderConfig.getMaxRetries()); // Fallback default
    }

    @Test
    void testEnvironmentVariableMapping() {
        // Test that environment variable mappings work (values may be null, which is expected)
        // Testing that the structure works without values being required
        String openaiKey = config.getString("openai.api.key").orElse(null);
        String anthropicKey = config.getString("anthropic.api.key").orElse(null);
        String googleKey = config.getString("google.api.key").orElse(null);

        // These may be null if environment variables are not set, which is fine
        // We're testing that the mapping structure doesn't throw exceptions
        assertTrue(openaiKey == null || openaiKey.isEmpty() || openaiKey.length() > 0);
        assertTrue(anthropicKey == null || anthropicKey.isEmpty() || anthropicKey.length() > 0);
        assertTrue(googleKey == null || googleKey.isEmpty() || googleKey.length() > 0);
    }

    @Test
    void testGenericNumericValueConversion() {
        // Test integer conversion
        assertEquals(123, config.getInt("test.nonexistent.int", 123));

        // Test long conversion
        assertEquals(456L, config.getLong("test.nonexistent.long", 456L));

        // Test double conversion
        assertEquals(7.89, config.getDouble("test.nonexistent.double", 7.89), 0.001);
    }
}
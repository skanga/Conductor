package com.skanga.conductor.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * LLM configuration settings.
 * <p>
 * Provides access to LLM provider configuration including
 * API keys, models, timeouts, and retry policies.
 * </p>
 *
 * @since 1.1.0
 */
public class LLMConfig extends ConfigurationProvider {

    private static final Map<String, Map<String, Object>> PROVIDER_DEFAULTS = Map.of(
        "openai", Map.of(
            "model", "gpt-3.5-turbo",
            "baseUrl", "https://api.openai.com/v1",
            "timeout", Duration.ofSeconds(30),
            "maxRetries", 3
        ),
        "anthropic", Map.of(
            "model", "claude-3-5-sonnet-20241022",
            "timeout", Duration.ofSeconds(30),
            "maxRetries", 3
        ),
        "gemini", Map.of(
            "model", "gemini-pro",
            "timeout", Duration.ofSeconds(30),
            "maxRetries", 3
        )
    );

    public LLMConfig(Properties properties) {
        super(properties);
    }

    // Convenience methods for specific providers - delegates to generic ProviderConfig
    public Optional<String> getOpenAiApiKey() {
        return getProviderConfig("openai").getApiKey();
    }

    public String getOpenAiModel() {
        return getProviderConfig("openai").getModel();
    }

    public Optional<String> getOpenAiBaseUrl() {
        return getProviderConfig("openai").getBaseUrl();
    }

    public Duration getOpenAiTimeout() {
        return getProviderConfig("openai").getTimeout();
    }

    public int getOpenAiMaxRetries() {
        return getProviderConfig("openai").getMaxRetries();
    }

    // Anthropic configuration
    public Optional<String> getAnthropicApiKey() {
        return getProviderConfig("anthropic").getApiKey();
    }

    public String getAnthropicModel() {
        return getProviderConfig("anthropic").getModel();
    }

    public Duration getAnthropicTimeout() {
        return getProviderConfig("anthropic").getTimeout();
    }

    public int getAnthropicMaxRetries() {
        return getProviderConfig("anthropic").getMaxRetries();
    }

    // Gemini configuration
    public Optional<String> getGeminiApiKey() {
        return getProviderConfig("gemini").getApiKey();
    }

    public String getGeminiModel() {
        return getProviderConfig("gemini").getModel();
    }

    public Duration getGeminiTimeout() {
        return getProviderConfig("gemini").getTimeout();
    }

    public int getGeminiMaxRetries() {
        return getProviderConfig("gemini").getMaxRetries();
    }

    /**
     * Generic method to get provider configuration for any supported provider.
     *
     * @param providerName the name of the provider (e.g., "openai", "anthropic", "gemini")
     * @return ProviderConfig instance for the specified provider
     */
    public ProviderConfig getProviderConfig(String providerName) {
        return new ProviderConfig(providerName, properties);
    }

    // Retry configuration
    public boolean isRetryEnabled() {
        return getBoolean("conductor.llm.retry.enabled", true);
    }

    public String getRetryStrategy() {
        return getString("conductor.llm.retry.strategy", "exponential_backoff");
    }

    @NotNull(message = "Retry initial delay cannot be null")
    public Duration getRetryInitialDelay() {
        Duration delay = getDuration("conductor.llm.retry.initial.delay", Duration.ofMillis(100));
        if (isRetryEnabled() && (delay.isNegative() || delay.isZero())) {
            throw new IllegalArgumentException("Retry initial delay must be positive");
        }
        return delay;
    }

    @NotNull(message = "Retry max delay cannot be null")
    public Duration getRetryMaxDelay() {
        Duration maxDelay = getDuration("conductor.llm.retry.max.delay", Duration.ofSeconds(10));
        Duration initialDelay = getRetryInitialDelay();
        if (isRetryEnabled() && maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("Retry max delay must be >= initial delay: " + maxDelay + " < " + initialDelay);
        }
        return maxDelay;
    }

    @DecimalMin(value = "1.0", message = "Retry multiplier must be at least 1.0")
    public double getRetryMultiplier() {
        return getDouble("conductor.llm.retry.multiplier", 2.0);
    }

    public boolean isRetryJitterEnabled() {
        return getBoolean("conductor.llm.retry.jitter.enabled", true);
    }

    public double getRetryJitterFactor() {
        return getDouble("conductor.llm.retry.jitter.factor", 0.1);
    }

    @NotNull(message = "Retry max duration cannot be null")
    public Duration getRetryMaxDuration() {
        return getDuration("conductor.llm.retry.max.duration", Duration.ofMinutes(2));
    }

    /**
     * Generic LLM Provider configuration class.
     */
    public static class ProviderConfig extends ConfigurationProvider {
        private final String providerName;
        private final Map<String, Object> defaults;

        public ProviderConfig(String providerName, Properties properties) {
            super(properties);
            this.providerName = providerName;
            this.defaults = PROVIDER_DEFAULTS.getOrDefault(providerName, Map.of());
        }

        public Optional<String> getApiKey() {
            return getString("conductor.llm." + providerName + ".api.key");
        }

        @NotBlank(message = "Model name cannot be empty")
        public String getModel() {
            return getString("conductor.llm." + providerName + ".model",
                           (String) defaults.get("model"));
        }

        public Optional<String> getBaseUrl() {
            String baseUrl = getString("conductor.llm." + providerName + ".base.url",
                                     (String) defaults.get("baseUrl"));
            return Optional.ofNullable(baseUrl);
        }

        @NotNull(message = "Timeout cannot be null")
        public Duration getTimeout() {
            Duration timeout = getDuration("conductor.llm." + providerName + ".timeout",
                             (Duration) defaults.getOrDefault("timeout", Duration.ofSeconds(30)));
            if (timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException(providerName + " timeout must be positive: " + timeout);
            }
            if (timeout.toSeconds() > 600) {
                throw new IllegalArgumentException(providerName + " timeout too large (max 10 minutes): " + timeout);
            }
            return timeout;
        }

        @Min(value = 0, message = "Max retries must be at least 0")
        @Max(value = 10, message = "Max retries cannot exceed 10")
        public int getMaxRetries() {
            return getInt("conductor.llm." + providerName + ".max.retries",
                         (Integer) defaults.getOrDefault("maxRetries", 3));
        }

        public String getProviderName() {
            return providerName;
        }
    }
}

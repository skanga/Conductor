package com.skanga.conductor.config;

import com.skanga.conductor.utils.ValidationUtils;

import java.time.Duration;
import java.util.Set;

/**
 * Utility class that provides convenient access to configuration settings
 * without requiring direct ApplicationConfig.getInstance() calls.
 * <p>
 * This class eliminates DRY violations by centralizing configuration access
 * patterns and providing a cleaner API for accessing frequently used
 * configuration values. It acts as a facade over ApplicationConfig to
 * reduce coupling and improve code readability.
 * </p>
 * <p>
 * Key benefits:
 * </p>
 * <ul>
 * <li>Eliminates repetitive ApplicationConfig.getInstance() calls</li>
 * <li>Provides type-safe access to configuration values</li>
 * <li>Centralizes configuration access patterns</li>
 * <li>Reduces coupling to ApplicationConfig implementation</li>
 * <li>Enables easier testing through configuration abstraction</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * // Instead of: ApplicationConfig.getInstance().getDatabaseConfig().getJdbcUrl()
 * String jdbcUrl = ConfigurationAccessor.getDatabaseJdbcUrl();
 *
 * // Instead of: ApplicationConfig.getInstance().getToolConfig().getCodeRunnerTimeout()
 * Duration timeout = ConfigurationAccessor.getToolCodeRunnerTimeout();
 *
 * // Instead of: ApplicationConfig.getInstance().getLLMConfig().getOpenAiApiKey()
 * String apiKey = ConfigurationAccessor.getLLMOpenAiApiKey();
 * </pre>
 * <p>
 * Thread Safety: This class is thread-safe as it delegates to the thread-safe
 * ApplicationConfig singleton.
 * </p>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 */
public final class ConfigurationAccessor {

    private ConfigurationAccessor() {
        // Utility class - prevent instantiation
    }

    // === Core Configuration Access ===

    /**
     * Gets the application configuration instance.
     * <p>
     * This method is provided for cases where direct access to ApplicationConfig
     * is needed, while still benefiting from the centralized access pattern.
     * </p>
     *
     * @return the ApplicationConfig singleton instance
     */
    public static ApplicationConfig getConfig() {
        return ApplicationConfig.getInstance();
    }

    /**
     * Gets a string property with default value.
     *
     * @param key the property key
     * @param defaultValue the default value if property not found
     * @return the property value or default
     */
    public static String getString(String key, String defaultValue) {
        return getConfig().getString(key, defaultValue);
    }

    /**
     * Gets a boolean property with default value.
     *
     * @param key the property key
     * @param defaultValue the default value if property not found
     * @return the property value or default
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return getConfig().getBoolean(key, defaultValue);
    }

    /**
     * Gets an integer property with default value.
     *
     * @param key the property key
     * @param defaultValue the default value if property not found
     * @return the property value or default
     */
    public static int getInt(String key, int defaultValue) {
        return getConfig().getInt(key, defaultValue);
    }

    /**
     * Gets a duration property with default value.
     *
     * @param key the property key
     * @param defaultValue the default value if property not found
     * @return the property value or default
     */
    public static Duration getDuration(String key, Duration defaultValue) {
        return getConfig().getDuration(key, defaultValue);
    }

    // === Database Configuration ===

    /**
     * Gets the database JDBC URL.
     */
    public static String getDatabaseJdbcUrl() {
        return getConfig().getDatabaseConfig().getJdbcUrl();
    }

    /**
     * Gets the database username.
     */
    public static String getDatabaseUsername() {
        return getConfig().getDatabaseConfig().getUsername();
    }

    /**
     * Gets the database password.
     */
    public static String getDatabasePassword() {
        return getConfig().getDatabaseConfig().getPassword();
    }

    /**
     * Gets the database driver class name.
     */
    public static String getDatabaseDriver() {
        return getConfig().getDatabaseConfig().getDriver();
    }

    /**
     * Gets the maximum number of database connections.
     */
    public static int getDatabaseMaxConnections() {
        return getConfig().getDatabaseConfig().getMaxConnections();
    }


    // === Tool Configuration ===

    /**
     * Gets the code runner timeout.
     */
    public static Duration getToolCodeRunnerTimeout() {
        return getConfig().getToolConfig().getCodeRunnerTimeout();
    }


    /**
     * Gets the file read base directory.
     */
    public static String getToolFileReadBaseDir() {
        return getConfig().getToolConfig().getFileReadBaseDir();
    }

    /**
     * Gets the maximum file size for file read operations.
     */
    public static long getToolFileReadMaxSize() {
        return getConfig().getToolConfig().getFileReadMaxSize();
    }

    /**
     * Gets the maximum path length for file operations.
     */
    public static int getToolFileReadMaxPathLength() {
        return getConfig().getToolConfig().getFileReadMaxPathLength();
    }

    /**
     * Gets the audio output directory.
     */
    public static String getToolAudioOutputDir() {
        return getConfig().getToolConfig().getAudioOutputDir();
    }

    /**
     * Gets the allowed commands for code runner.
     */
    public static Set<String> getToolCodeRunnerAllowedCommands() {
        return getConfig().getToolConfig().getCodeRunnerAllowedCommands();
    }

    // === LLM Configuration ===

    /**
     * Gets the OpenAI API key.
     */
    public static String getLLMOpenAiApiKey() {
        return getConfig().getLLMConfig().getOpenAiApiKey().orElse(null);
    }

    /**
     * Gets the OpenAI model name.
     */
    public static String getLLMOpenAiModel() {
        return getConfig().getLLMConfig().getOpenAiModel();
    }

    /**
     * Gets the OpenAI base URL.
     */
    public static String getLLMOpenAiBaseUrl() {
        return getConfig().getLLMConfig().getOpenAiBaseUrl().orElse(null);
    }

    /**
     * Gets the OpenAI timeout.
     */
    public static Duration getLLMOpenAiTimeout() {
        return getConfig().getLLMConfig().getOpenAiTimeout();
    }

    /**
     * Gets the OpenAI max retries.
     */
    public static int getLLMOpenAiMaxRetries() {
        return getConfig().getLLMConfig().getOpenAiMaxRetries();
    }

    /**
     * Gets the Anthropic API key.
     */
    public static String getLLMAnthropicApiKey() {
        return getConfig().getLLMConfig().getAnthropicApiKey().orElse(null);
    }

    /**
     * Gets the Anthropic model name.
     */
    public static String getLLMAnthropicModel() {
        return getConfig().getLLMConfig().getAnthropicModel();
    }

    /**
     * Gets the Anthropic timeout.
     */
    public static Duration getLLMAnthropicTimeout() {
        return getConfig().getLLMConfig().getAnthropicTimeout();
    }

    /**
     * Gets the Anthropic max retries.
     */
    public static int getLLMAnthropicMaxRetries() {
        return getConfig().getLLMConfig().getAnthropicMaxRetries();
    }

    /**
     * Gets the Gemini API key.
     */
    public static String getLLMGeminiApiKey() {
        return getConfig().getLLMConfig().getGeminiApiKey().orElse(null);
    }

    /**
     * Gets the Gemini model name.
     */
    public static String getLLMGeminiModel() {
        return getConfig().getLLMConfig().getGeminiModel();
    }

    /**
     * Gets the Gemini timeout.
     */
    public static Duration getLLMGeminiTimeout() {
        return getConfig().getLLMConfig().getGeminiTimeout();
    }

    /**
     * Gets the Gemini max retries.
     */
    public static int getLLMGeminiMaxRetries() {
        return getConfig().getLLMConfig().getGeminiMaxRetries();
    }

    /**
     * Gets whether retry is enabled for LLM operations.
     */
    public static boolean isLLMRetryEnabled() {
        return getConfig().getLLMConfig().isRetryEnabled();
    }

    /**
     * Gets the LLM retry strategy.
     */
    public static String getLLMRetryStrategy() {
        return getConfig().getLLMConfig().getRetryStrategy();
    }

    /**
     * Gets the LLM retry initial delay.
     */
    public static Duration getLLMRetryInitialDelay() {
        return getConfig().getLLMConfig().getRetryInitialDelay();
    }

    /**
     * Gets the LLM retry max delay.
     */
    public static Duration getLLMRetryMaxDelay() {
        return getConfig().getLLMConfig().getRetryMaxDelay();
    }

    /**
     * Gets the LLM retry multiplier.
     */
    public static double getLLMRetryMultiplier() {
        return getConfig().getLLMConfig().getRetryMultiplier();
    }

    /**
     * Gets the LLM retry max duration.
     */
    public static Duration getLLMRetryMaxDuration() {
        return getConfig().getLLMConfig().getRetryMaxDuration();
    }

    // === Memory Configuration ===

    /**
     * Gets the default memory limit.
     */
    public static int getMemoryDefaultLimit() {
        return getConfig().getMemoryConfig().getDefaultMemoryLimit();
    }

    /**
     * Gets the maximum memory entries.
     */
    public static int getMemoryMaxEntries() {
        return getConfig().getMemoryConfig().getMaxMemoryEntries();
    }

    /**
     * Gets the memory retention period in days.
     */
    public static int getMemoryRetentionDays() {
        return getConfig().getMemoryConfig().getMemoryRetentionDays();
    }

    // === Metrics Configuration ===

    /**
     * Gets whether metrics collection is enabled.
     */
    public static boolean isMetricsEnabled() {
        return getConfig().getMetricsConfig().isEnabled();
    }

    /**
     * Gets the metrics retention period.
     */
    public static Duration getMetricsRetentionPeriod() {
        return getConfig().getMetricsConfig().getRetentionPeriod();
    }

    /**
     * Gets the maximum number of metrics to keep in memory.
     */
    public static int getMetricsMaxInMemory() {
        return getConfig().getMetricsConfig().getMaxMetricsInMemory();
    }

    /**
     * Gets whether console reporting is enabled for metrics.
     */
    public static boolean isMetricsConsoleReportingEnabled() {
        return getConfig().getMetricsConfig().isConsoleReportingEnabled();
    }

    /**
     * Gets the console reporting interval for metrics.
     */
    public static Duration getMetricsConsoleReportingInterval() {
        return getConfig().getMetricsConfig().getConsoleReportingInterval();
    }

    /**
     * Gets the metrics output directory.
     */
    public static String getMetricsOutputDirectory() {
        return getConfig().getMetricsConfig().getOutputDirectory();
    }

    /**
     * Gets whether file reporting is enabled for metrics.
     */
    public static boolean isMetricsFileReportingEnabled() {
        return getConfig().getMetricsConfig().isFileReportingEnabled();
    }

    /**
     * Gets the file reporting interval for metrics.
     */
    public static Duration getMetricsFileReportingInterval() {
        return getConfig().getMetricsConfig().getFileReportingInterval();
    }

    /**
     * Gets the enabled metrics patterns.
     */
    public static Set<String> getMetricsEnabledPatterns() {
        return getConfig().getMetricsConfig().getEnabledMetrics();
    }

    /**
     * Gets the disabled metrics patterns.
     */
    public static Set<String> getMetricsDisabledPatterns() {
        return getConfig().getMetricsConfig().getDisabledMetrics();
    }

    // === Convenience Methods ===

    /**
     * Gets a required secret property (throws exception if not found).
     *
     * @param key the property key
     * @return the property value
     * @throws ConfigurationException if property not found
     */
    public static String getRequiredSecret(String key) {
        ValidationUtils.requireNonBlank(key, "property key");
        return getConfig().getRequiredSecretProperty(key);
    }

    /**
     * Gets a secret property with default value.
     *
     * @param key the property key
     * @param defaultValue the default value if property not found
     * @return the property value or default
     */
    public static String getSecret(String key, String defaultValue) {
        ValidationUtils.requireNonBlank(key, "property key");
        return getConfig().getSecretProperty(key, defaultValue);
    }

    /**
     * Gets a provider configuration for the specified LLM provider.
     *
     * @param providerName the name of the provider (e.g., "openai", "anthropic", "gemini")
     * @return the provider configuration
     * @throws IllegalArgumentException if providerName is null or blank
     */
    public static LLMConfig.ProviderConfig getProviderConfig(String providerName) {
        ValidationUtils.requireNonBlank(providerName, "provider name");
        return getConfig().getLLMConfig().getProviderConfig(providerName);
    }

    // === Configuration Type Access ===

    /**
     * Gets the complete database configuration.
     *
     * @return the database configuration instance
     */
    public static DatabaseConfig getDatabaseConfig() {
        return getConfig().getDatabaseConfig();
    }

    /**
     * Gets the complete tool configuration.
     *
     * @return the tool configuration instance
     */
    public static ToolConfig getToolConfig() {
        return getConfig().getToolConfig();
    }

    /**
     * Gets the complete LLM configuration.
     *
     * @return the LLM configuration instance
     */
    public static LLMConfig getLLMConfig() {
        return getConfig().getLLMConfig();
    }

    /**
     * Gets the complete memory configuration.
     *
     * @return the memory configuration instance
     */
    public static MemoryConfig getMemoryConfig() {
        return getConfig().getMemoryConfig();
    }

    /**
     * Gets the complete metrics configuration.
     *
     * @return the metrics configuration instance
     */
    public static MetricsConfig getMetricsConfig() {
        return getConfig().getMetricsConfig();
    }
}
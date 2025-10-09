package com.skanga.conductor.config;

import com.skanga.conductor.exception.ConfigurationException;
import com.skanga.conductor.utils.SingletonHolder;
import com.skanga.conductor.utils.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

/**
 * Thread-safe singleton configuration manager for the Conductor framework.
 * <p>
 * This class provides centralized configuration management with support for:
 * </p>
 * <ul>
 * <li>Base configuration loading (application.properties)</li>
 * <li>Profile-specific configuration (application-{profile}.properties) when profile is specified</li>
 * <li>External configuration files via --config parameter</li>
 * <li>System property overrides (properties starting with "conductor.")</li>
 * <li>Environment variable support (variables starting with "CONDUCTOR_")</li>
 * <li>Type-safe property access with default values</li>
 * <li>Nested configuration classes for organized property grouping</li>
 * </ul>
 * <p>
 * Configuration sources are loaded in the following priority order:
 * </p>
 * <ol>
 * <li>Base application.properties from classpath</li>
 * <li>Profile-specific application-{profile}.properties (if profile specified via -Dconductor.profile or CONDUCTOR_PROFILE)</li>
 * <li>External configuration files (if specified via --config parameter)</li>
 * <li>System properties (highest priority)</li>
 * <li>Environment variables</li>
 * </ol>
 * <p>
 * The singleton instance is created using the generic SingletonHolder pattern for optimal
 * thread safety and performance. Once created, the configuration is immutable and can be
 * safely accessed from multiple threads concurrently.
 * </p>
 *
 * @since 1.0.0
 * @see DatabaseConfig
 * @see ToolConfig
 * @see LLMConfig
 * @see MemoryConfig
 */
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    private static final SingletonHolder<ApplicationConfig> HOLDER =
        SingletonHolder.of(ApplicationConfig::new);

    private final Properties properties;

    private ApplicationConfig() {
        this.properties = new Properties();
        loadProperties();
        loadExternalConfiguration();
        validateConfiguration();
    }

    /**
     * Returns the singleton instance of the application configuration.
     * <p>
     * This method uses the generic SingletonHolder pattern for optimal
     * thread safety and performance. The instance is created only once
     * and subsequent calls return the same instance.
     * </p>
     *
     * @return the singleton ApplicationConfig instance
     */
    public static ApplicationConfig getInstance() {
        return HOLDER.get();
    }

    /**
     * Resets the singleton instance for testing purposes.
     * <p>
     * This method should only be used in test scenarios where you need
     * to reset the configuration state between tests.
     * </p>
     */
    public static void resetInstance() {
        HOLDER.reset();
    }

    private void loadProperties() {
        // Always load base application.properties
        loadConfigFile("/application.properties");

        // Load environment-specific config only if profile is specified
        // Use synchronized block to prevent race conditions during profile loading
        synchronized (this) {
            String activeProfile = getActiveProfile();
            if (activeProfile != null && !activeProfile.trim().isEmpty()) {
                String profileConfigFile = "/application-" + activeProfile + ".properties";
                loadConfigFile(profileConfigFile);
            }
        }

        loadSystemProperties();
        loadEnvironmentVariables();
    }

    /**
     * Determines the active profile from system properties or environment variables.
     *
     * @return the active profile name, or null if none specified
     */
    private String getActiveProfile() {
        // Check system property first (highest priority)
        String profile = System.getProperty("conductor.profile");
        if (profile != null) {
            return profile.trim();
        }

        // Check environment variable
        profile = System.getenv("CONDUCTOR_PROFILE");
        if (profile != null) {
            return profile.trim();
        }

        // Legacy environment variable support
        profile = System.getenv("PROFILE");
        if (profile != null) {
            return profile.trim();
        }

        return null;
    }

    /**
     * Loads a single configuration file from the classpath.
     *
     * @param configFile the classpath resource path (e.g., "/application.properties")
     */
    private void loadConfigFile(String configFile) {
        try (InputStream inputStream = getClass().getResourceAsStream(configFile)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config file " + configFile + ": " + e.getMessage());
        }
    }

    /**
     * Loads external configuration files specified via command line or system properties.
     * <p>
     * Supports configuration source:
     * </p>
     * <ol>
     * <li>--config=path/to/file.properties command line argument</li>
     * <li>-Dconfig=path/to/file.properties system property</li>
     * </ol>
     * <p>
     * Note: demo.properties is only loaded when explicitly specified via --config parameter.
     * It is not automatically loaded from the current directory.
     * </p>
     */
    private void loadExternalConfiguration() {
        // Load configuration file specified via --config command line argument (stored as system property)
        String configPath = System.getProperty("config");
        if (configPath != null) {
            loadExternalFile(configPath, "command line --config");
        }
    }

    /**
     * Loads properties from an external file.
     *
     * @param filePath path to the properties file
     * @param source description of the configuration source for logging
     */
    private void loadExternalFile(String filePath, String source) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                if (!TestUtils.isRunningUnderTest())
                    System.err.println("Warning: Configuration file not found: " + filePath + " (from " + source + ")");
                return;
            }

            Properties externalProps = new Properties();
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                externalProps.load(fis);

                // Add all properties with logging
                int loadedCount = 0;
                for (String key : externalProps.stringPropertyNames()) {
                    String value = externalProps.getProperty(key);
                    properties.setProperty(key, value);
                    loadedCount++;
                }

                logger.info("Loaded {} properties from external configuration: {} (from {})",
                           loadedCount, filePath, source);
            }
        } catch (IOException e) {
            logger.warn("Failed to load external configuration from {} (from {}): {}",
                       filePath, source, e.getMessage());
        }
    }

    private void loadSystemProperties() {
        System.getProperties().forEach((key, value) -> {
            if (key.toString().startsWith("conductor.")) {
                properties.setProperty(key.toString(), value.toString());
            }
        });
    }

    /**
     * Environment variable to property key mapping configuration.
     * This eliminates hard-coded special cases and makes adding new mappings easier.
     */
    private static final Map<String, String> ENV_VAR_MAPPINGS = Map.of(
        "OPENAI_API_KEY", "openai.api.key",
        "ANTHROPIC_API_KEY", "anthropic.api.key",
        "GOOGLE_API_KEY", "google.api.key",
        "GEMINI_API_KEY", "gemini.api.key"
    );

    /**
     * LLM Provider default configuration values.
     * This centralizes default values and makes adding new providers easier.
     */
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

    private void loadEnvironmentVariables() {
        System.getenv().forEach((key, value) -> {
            String propertyKey = getPropertyKeyFromEnvironmentVariable(key);
            if (propertyKey != null) {
                properties.setProperty(propertyKey, value);
            }
        });
    }

    /**
     * Converts environment variable name to property key using consistent rules.
     *
     * @param envKey the environment variable name
     * @return the corresponding property key, or null if not applicable
     */
    private String getPropertyKeyFromEnvironmentVariable(String envKey) {
        // Check direct mappings first
        if (ENV_VAR_MAPPINGS.containsKey(envKey)) {
            return ENV_VAR_MAPPINGS.get(envKey);
        }

        // Handle prefixed environment variables with consistent transformation rules
        if (envKey.startsWith("CONDUCTOR_")) {
            return envKey.toLowerCase().replace("_", ".");
        } else if (envKey.startsWith("DEMO_")) {
            return envKey.toLowerCase().replace("_", ".");
        }

        return null; // Environment variable not applicable
    }

    /**
     * Retrieves a string property value with a default fallback.
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found
     * @return the property value or the default value if not found
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Retrieves a string property value.
     *
     * @param key the property key to lookup
     * @return Optional containing the property value, or empty if not found
     */
    public Optional<String> getString(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    /**
     * Generic method for retrieving numeric property values with type safety.
     * <p>
     * This method eliminates code duplication across getInt, getLong, and getDouble methods
     * by using a generic approach with function-based type conversion.
     * </p>
     *
     * @param <T> the numeric type to return
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found or invalid
     * @param parser the function to convert string to the target type
     * @param typeName the name of the type for error messages
     * @return the property value converted to the target type or the default value
     */
    private <T> T getNumericValue(String key, T defaultValue, Function<String, T> parser, String typeName) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return parser.apply(value.trim());
        } catch (NumberFormatException e) {
            logConfigurationWarning(key, value, typeName, String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Retrieves an integer property value with a default fallback.
     * <p>
     * If the property value cannot be parsed as an integer, a warning
     * is logged and the default value is returned.
     * </p>
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found or invalid
     * @return the property value as an integer or the default value
     */
    public int getInt(String key, int defaultValue) {
        return getNumericValue(key, defaultValue, Integer::parseInt, "integer");
    }

    /**
     * Gets a long property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a long, or the default value
     */
    public long getLong(String key, long defaultValue) {
        return getNumericValue(key, defaultValue, Long::parseLong, "long");
    }

    /**
     * Gets a double property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a double, or the default value
     */
    public double getDouble(String key, double defaultValue) {
        return getNumericValue(key, defaultValue, Double::parseDouble, "double");
    }

    /**
     * Retrieves a boolean property value with a default fallback.
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found
     * @return the property value as a boolean or the default value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Retrieves a duration property value with a default fallback.
     * <p>
     * Supports various duration formats:
     * </p>
     * <ul>
     * <li>"30s" - 30 seconds</li>
     * <li>"5000ms" - 5000 milliseconds</li>
     * <li>"10m" - 10 minutes</li>
     * <li>"30" - 30 seconds (default unit)</li>
     * </ul>
     * <p>
     * If the property value cannot be parsed as a duration, a warning
     * is logged and the default value is returned.
     * </p>
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found or invalid
     * @return the property value as a Duration or the default value
     */
    public Duration getDuration(String key, Duration defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            } else if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
            } else {
                return Duration.ofSeconds(Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            logConfigurationWarning(key, value, "duration", String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Retrieves a comma-separated string property as a Set with a default fallback.
     * <p>
     * Parses a comma-separated string value into a Set of trimmed strings.
     * Empty strings are filtered out. For example:
     * </p>
     * <pre>
     * "echo,ls,pwd" → {"echo", "ls", "pwd"}
     * "echo, ls , pwd " → {"echo", "ls", "pwd"}
     * "" → defaultValue
     * </pre>
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found or empty
     * @return the property value as a Set of strings or the default value
     */
    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Returns the database configuration settings.
     *
     * @return a DatabaseConfig instance with database-related properties
     */
    public DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig();
    }

    /**
     * Returns the tool configuration settings.
     *
     * @return a ToolConfig instance with tool-related properties
     */
    public ToolConfig getToolConfig() {
        return new ToolConfig();
    }

    /**
     * Returns the LLM configuration settings.
     *
     * @return an LLMConfig instance with LLM-related properties
     */
    public LLMConfig getLLMConfig() {
        return new LLMConfig();
    }

    /**
     * Returns the memory configuration settings.
     *
     * @return a MemoryConfig instance with memory-related properties
     */
    public MemoryConfig getMemoryConfig() {
        return new MemoryConfig();
    }

    public ParallelismConfig getParallelismConfig() {
        return new ParallelismConfig();
    }

    /**
     * Returns the metrics configuration settings.
     *
     * @return a MetricsConfig instance with metrics-related properties
     */
    public MetricsConfig getMetricsConfig() {
        return new MetricsConfig();
    }

    public class DatabaseConfig {
        public String getJdbcUrl() {
            return getString("conductor.database.url", "jdbc:h2:./data/subagentsdb;FILE_LOCK=FS");
        }

        public String getUsername() {
            return getString("conductor.database.username", "sa");
        }

        public String getPassword() {
            return getString("conductor.database.password", "");
        }

        public String getDriver() {
            return getString("conductor.database.driver", "org.h2.Driver");
        }

        public int getMaxConnections() {
            return getInt("conductor.database.max.connections", 10);
        }
    }

    public class ToolConfig {
        public Duration getCodeRunnerTimeout() {
            return getDuration("conductor.tools.coderunner.timeout", Duration.ofSeconds(5));
        }

        public Set<String> getCodeRunnerAllowedCommands() {
            return getStringSet("conductor.tools.coderunner.allowed.commands",
                Set.of("echo", "ls", "pwd", "date", "whoami"));
        }

        public String getFileReadBaseDir() {
            return getString("conductor.tools.fileread.basedir", "./sample_data");
        }

        public boolean getFileReadAllowSymlinks() {
            return getBoolean("conductor.tools.fileread.allow.symlinks", false);
        }

        public long getFileReadMaxSize() {
            return getLong("conductor.tools.fileread.max.size.bytes", 10 * 1024 * 1024); // 10MB
        }

        public int getFileReadMaxPathLength() {
            return getInt("conductor.tools.fileread.max.path.length", 260);
        }

        public String getAudioOutputDir() {
            return getString("conductor.tools.audio.output.dir", "./out_audio");
        }
    }

    /**
     * Generic LLM Provider configuration class that eliminates code duplication.
     * This class can handle any provider configuration using the same pattern.
     */
    public class ProviderConfig {
        private final String providerName;
        private final Map<String, Object> defaults;

        public ProviderConfig(String providerName) {
            this.providerName = providerName;
            this.defaults = PROVIDER_DEFAULTS.getOrDefault(providerName, Map.of());
        }

        public Optional<String> getApiKey() {
            return getString("conductor.llm." + providerName + ".api.key");
        }

        public String getModel() {
            return getString("conductor.llm." + providerName + ".model",
                           (String) defaults.get("model"));
        }

        public Optional<String> getBaseUrl() {
            String baseUrl = getString("conductor.llm." + providerName + ".base.url",
                                     (String) defaults.get("baseUrl"));
            return Optional.ofNullable(baseUrl);
        }

        public Duration getTimeout() {
            return getDuration("conductor.llm." + providerName + ".timeout",
                             (Duration) defaults.getOrDefault("timeout", Duration.ofSeconds(30)));
        }

        public int getMaxRetries() {
            return getInt("conductor.llm." + providerName + ".max.retries",
                         (Integer) defaults.getOrDefault("maxRetries", 3));
        }

        public String getProviderName() {
            return providerName;
        }
    }

    public class LLMConfig {
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

        // Anthropic configuration - now uses generic approach
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

        // Gemini configuration - now uses generic approach
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
         * This makes it easy to add new providers without changing the LLMConfig class.
         *
         * @param providerName the name of the provider (e.g., "openai", "anthropic", "gemini")
         * @return ProviderConfig instance for the specified provider
         */
        public ProviderConfig getProviderConfig(String providerName) {
            return new ProviderConfig(providerName);
        }

        // Retry configuration
        public boolean isRetryEnabled() {
            return getBoolean("conductor.llm.retry.enabled", true);
        }

        public String getRetryStrategy() {
            return getString("conductor.llm.retry.strategy", "exponential_backoff");
        }

        public Duration getRetryInitialDelay() {
            return getDuration("conductor.llm.retry.initial.delay", Duration.ofMillis(100));
        }

        public Duration getRetryMaxDelay() {
            return getDuration("conductor.llm.retry.max.delay", Duration.ofSeconds(10));
        }

        public double getRetryMultiplier() {
            return getDouble("conductor.llm.retry.multiplier", 2.0);
        }

        public boolean isRetryJitterEnabled() {
            return getBoolean("conductor.llm.retry.jitter.enabled", true);
        }

        public double getRetryJitterFactor() {
            return getDouble("conductor.llm.retry.jitter.factor", 0.1);
        }

        public Duration getRetryMaxDuration() {
            return getDuration("conductor.llm.retry.max.duration", Duration.ofMinutes(2));
        }
    }

    public class MemoryConfig {
        public int getDefaultMemoryLimit() {
            return getInt("conductor.memory.default.limit", 10);
        }

        public int getMaxMemoryEntries() {
            return getInt("conductor.memory.max.entries", 1000);
        }

        public int getMemoryRetentionDays() {
            return getInt("conductor.memory.retention.days", 30);
        }
    }

    public class MetricsConfig {
        public boolean isEnabled() {
            return getBoolean("conductor.metrics.enabled", true);
        }

        public Duration getRetentionPeriod() {
            return getDuration("conductor.metrics.retention.period", Duration.ofHours(24));
        }

        public int getMaxMetricsInMemory() {
            return getInt("conductor.metrics.max.in.memory", 100000);
        }

        public boolean isConsoleReportingEnabled() {
            return getBoolean("conductor.metrics.console.enabled", false);
        }

        public Duration getConsoleReportingInterval() {
            return getDuration("conductor.metrics.console.interval", Duration.ofMinutes(5));
        }

        public String getOutputDirectory() {
            return getString("conductor.metrics.output.dir", "./logs/metrics");
        }

        public boolean isFileReportingEnabled() {
            return getBoolean("conductor.metrics.file.enabled", false);
        }

        public Duration getFileReportingInterval() {
            return getDuration("conductor.metrics.file.interval", Duration.ofMinutes(15));
        }

        public Set<String> getEnabledMetrics() {
            return getStringSet("conductor.metrics.enabled.patterns",
                Set.of("agent.*", "tool.*", "orchestrator.*"));
        }

        public Set<String> getDisabledMetrics() {
            return getStringSet("conductor.metrics.disabled.patterns", Set.of());
        }
    }

    /**
     * Configuration settings for parallel task execution.
     */
    public class ParallelismConfig {

        public boolean isEnabled() {
            return getBoolean("conductor.parallelism.enabled", true);
        }

        public int getMaxThreads() {
            return getInt("conductor.parallelism.max.threads", Runtime.getRuntime().availableProcessors());
        }

        public int getMaxParallelTasksPerBatch() {
            return getInt("conductor.parallelism.max.tasks.per.batch", getMaxThreads());
        }

        public long getTaskTimeoutSeconds() {
            return getLong("conductor.parallelism.task.timeout.seconds", 300L); // 5 minutes
        }

        public long getBatchTimeoutSeconds() {
            return getLong("conductor.parallelism.batch.timeout.seconds", 1800L); // 30 minutes
        }

        public boolean isFallbackToSequentialEnabled() {
            return getBoolean("conductor.parallelism.fallback.sequential", true);
        }

        public int getMinTasksForParallelExecution() {
            return getInt("conductor.parallelism.min.tasks.threshold", 2);
        }

        public double getParallelismThreshold() {
            return getDouble("conductor.parallelism.threshold", 0.3); // 30% parallelizable tasks minimum
        }
    }

    /**
     * Validates all configuration settings for consistency and security.
     * <p>
     * This method is called during initialization to ensure all configuration
     * values are valid and safe to use. It checks for common configuration
     * errors that could cause runtime failures or security vulnerabilities.
     * </p>
     *
     * @throws ConfigurationException if any configuration value is invalid
     */
    private void validateConfiguration() {
        try {
            validateDatabaseConfig();
            validateToolConfig();
            validateLLMConfig();
            validateMemoryConfig();
            validateMetricsConfig();
            validateParallelismConfig();
        } catch (Exception e) {
            throw new ConfigurationException("Configuration validation failed", e);
        }
    }

    /**
     * Validates database configuration settings.
     */
    private void validateDatabaseConfig() {
        DatabaseConfig databaseConfig = getDatabaseConfig();

        // Validate JDBC URL format
        String jdbcUrl = databaseConfig.getJdbcUrl();
        if (!isValidJdbcUrl(jdbcUrl)) {
            throw new ConfigurationException("Invalid JDBC URL format: " + jdbcUrl);
        }

        // Validate connection pool settings
        int maxConnections = databaseConfig.getMaxConnections();
        if (maxConnections <= 0 || maxConnections > 1000) {
            throw new ConfigurationException("Invalid max connections value: " + maxConnections +
                " (must be between 1 and 1000)");
        }

        // Validate database driver
        String driver = databaseConfig.getDriver();
        if (driver == null || driver.trim().isEmpty()) {
            throw new ConfigurationException("Database driver cannot be empty");
        }
    }

    /**
     * Validates tool configuration settings.
     */
    private void validateToolConfig() {
        ToolConfig toolConfig = getToolConfig();

        // Validate code runner timeout
        Duration timeout = toolConfig.getCodeRunnerTimeout();
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException("Code runner timeout must be positive: " + timeout);
        }
        if (timeout.toSeconds() > 300) { // 5 minutes max
            throw new ConfigurationException("Code runner timeout too long: " + timeout +
                " (maximum 5 minutes)");
        }

        // Validate file read base directory
        String baseDir = toolConfig.getFileReadBaseDir();
        if (baseDir == null || baseDir.trim().isEmpty()) {
            throw new ConfigurationException("File read base directory cannot be empty");
        }
        if (baseDir.contains("..")) {
            throw new ConfigurationException("File read base directory cannot contain '..': " + baseDir);
        }

        // Validate file size limits
        long maxFileSize = toolConfig.getFileReadMaxSize();
        if (maxFileSize <= 0) {
            throw new ConfigurationException("File read max size must be positive: " + maxFileSize);
        }
        if (maxFileSize > 100L * 1024 * 1024 * 1024) { // 100GB max
            throw new ConfigurationException("File read max size too large: " + maxFileSize +
                " (maximum 100GB)");
        }

        // Validate path length
        int maxPathLength = toolConfig.getFileReadMaxPathLength();
        if (maxPathLength <= 0 || maxPathLength > 32767) {
            throw new ConfigurationException("Invalid path length limit: " + maxPathLength +
                " (must be between 1 and 32767)");
        }

        // Validate audio output directory
        String audioDir = toolConfig.getAudioOutputDir();
        if (audioDir == null || audioDir.trim().isEmpty()) {
            throw new ConfigurationException("Audio output directory cannot be empty");
        }
        if (audioDir.contains("..")) {
            throw new ConfigurationException("Audio output directory cannot contain '..': " + audioDir);
        }
    }

    /**
     * Generic provider timeout validation to eliminate duplicate validation logic.
     *
     * @param timeout the timeout duration to validate
     * @param providerName the name of the provider for error messages
     */
    private void validateProviderTimeout(Duration timeout, String providerName) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException(providerName + " timeout must be positive: " + timeout);
        }
        if (timeout.toMinutes() > 10) { // 10 minutes max
            throw new ConfigurationException(providerName + " timeout too long: " + timeout +
                " (maximum 10 minutes)");
        }
    }

    /**
     * Generic provider retry count validation to eliminate duplicate validation logic.
     *
     * @param maxRetries the maximum retry count to validate
     * @param providerName the name of the provider for error messages
     */
    private void validateProviderRetries(int maxRetries, String providerName) {
        if (maxRetries < 0 || maxRetries > 10) {
            throw new ConfigurationException("Invalid " + providerName + " max retries: " + maxRetries +
                " (must be between 0 and 10)");
        }
    }

    /**
     * Generic provider model name validation to eliminate duplicate validation logic.
     *
     * @param model the model name to validate
     * @param providerName the name of the provider for error messages
     */
    private void validateProviderModel(String model, String providerName) {
        if (model == null || model.trim().isEmpty()) {
            throw new ConfigurationException(providerName + " model name cannot be empty");
        }
    }

    /**
     * Generic provider base URL validation.
     *
     * @param baseUrl the base URL to validate
     * @param providerName the name of the provider for error messages
     */
    private void validateProviderBaseUrl(String baseUrl, String providerName) {
        if (baseUrl != null && !isValidHttpUrl(baseUrl)) {
            throw new ConfigurationException("Invalid " + providerName + " base URL: " + baseUrl);
        }
    }

    /**
     * Generic provider configuration validation that works for any provider.
     *
     * @param providerConfig the provider configuration to validate
     */
    private void validateProviderConfig(ProviderConfig providerConfig) {
        String providerName = providerConfig.getProviderName();

        validateProviderTimeout(providerConfig.getTimeout(), providerName);
        validateProviderRetries(providerConfig.getMaxRetries(), providerName);
        validateProviderModel(providerConfig.getModel(), providerName);
        validateProviderBaseUrl(providerConfig.getBaseUrl().orElse(null), providerName);
    }

    /**
     * Validates LLM configuration settings using generic validation methods.
     */
    private void validateLLMConfig() {
        LLMConfig llmConfig = getLLMConfig();

        // Validate all configured providers using generic validation
        validateProviderConfig(llmConfig.getProviderConfig("openai"));
        validateProviderConfig(llmConfig.getProviderConfig("anthropic"));
        validateProviderConfig(llmConfig.getProviderConfig("gemini"));

        // Validate retry configuration
        validateRetryConfig(llmConfig);
    }

    /**
     * Validates memory configuration settings.
     */
    private void validateMemoryConfig() {
        MemoryConfig memoryConfig = getMemoryConfig();

        // Validate default memory limit
        int defaultLimit = memoryConfig.getDefaultMemoryLimit();
        if (defaultLimit <= 0 || defaultLimit > 10000) {
            throw new ConfigurationException("Invalid default memory limit: " + defaultLimit +
                " (must be between 1 and 10000)");
        }

        // Validate max memory entries
        int maxEntries = memoryConfig.getMaxMemoryEntries();
        if (maxEntries <= 0 || maxEntries > 1000000) {
            throw new ConfigurationException("Invalid max memory entries: " + maxEntries +
                " (must be between 1 and 1,000,000)");
        }

        // Validate retention days
        int retentionDays = memoryConfig.getMemoryRetentionDays();
        if (retentionDays <= 0 || retentionDays > 36500) { // 100 years max
            throw new ConfigurationException("Invalid retention days: " + retentionDays +
                " (must be between 1 and 36500)");
        }
    }

    /**
     * Validates metrics configuration settings.
     */
    private void validateMetricsConfig() {
        MetricsConfig metricsConfig = getMetricsConfig();

        // Validate retention period
        Duration retention = metricsConfig.getRetentionPeriod();
        if (retention.isNegative() || retention.isZero()) {
            throw new ConfigurationException("Metrics retention period must be positive: " + retention);
        }
        if (retention.toDays() > 365) { // 1 year max
            throw new ConfigurationException("Metrics retention period too long: " + retention +
                " (maximum 365 days)");
        }

        // Validate max metrics in memory
        int maxMetrics = metricsConfig.getMaxMetricsInMemory();
        if (maxMetrics <= 0 || maxMetrics > 10_000_000) {
            throw new ConfigurationException("Invalid max metrics in memory: " + maxMetrics +
                " (must be between 1 and 10,000,000)");
        }

        // Validate console reporting interval
        Duration consoleInterval = metricsConfig.getConsoleReportingInterval();
        if (consoleInterval.isNegative() || consoleInterval.isZero()) {
            throw new ConfigurationException("Console reporting interval must be positive: " + consoleInterval);
        }
        if (consoleInterval.toHours() > 24) {
            throw new ConfigurationException("Console reporting interval too long: " + consoleInterval +
                " (maximum 24 hours)");
        }

        // Validate file reporting interval
        Duration fileInterval = metricsConfig.getFileReportingInterval();
        if (fileInterval.isNegative() || fileInterval.isZero()) {
            throw new ConfigurationException("File reporting interval must be positive: " + fileInterval);
        }
        if (fileInterval.toHours() > 24) {
            throw new ConfigurationException("File reporting interval too long: " + fileInterval +
                " (maximum 24 hours)");
        }

        // Validate output directory
        String outputDir = metricsConfig.getOutputDirectory();
        if (outputDir == null || outputDir.trim().isEmpty()) {
            throw new ConfigurationException("Metrics output directory cannot be empty");
        }
        if (outputDir.contains("..")) {
            throw new ConfigurationException("Metrics output directory cannot contain '..': " + outputDir);
        }
    }

    /**
     * Validates retry configuration settings.
     */
    private void validateRetryConfig(LLMConfig config) {
        // Validate retry strategy
        String strategy = config.getRetryStrategy();
        if (strategy != null && !strategy.equals("exponential_backoff") && !strategy.equals("fixed_delay") && !strategy.equals("none")) {
            throw new ConfigurationException("Invalid retry strategy: " + strategy +
                " (must be 'exponential_backoff', 'fixed_delay', or 'none')");
        }

        // Validate retry delays
        Duration initialDelay = config.getRetryInitialDelay();
        if (initialDelay.isNegative()) {
            throw new ConfigurationException("Retry initial delay cannot be negative: " + initialDelay);
        }
        if (initialDelay.toSeconds() > 60) {
            throw new ConfigurationException("Retry initial delay too long: " + initialDelay +
                " (maximum 60 seconds)");
        }

        Duration maxDelay = config.getRetryMaxDelay();
        if (maxDelay.isNegative()) {
            throw new ConfigurationException("Retry max delay cannot be negative: " + maxDelay);
        }
        if (maxDelay.toMinutes() > 5) {
            throw new ConfigurationException("Retry max delay too long: " + maxDelay +
                " (maximum 5 minutes)");
        }

        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new ConfigurationException("Retry max delay must be >= initial delay: " +
                maxDelay + " vs " + initialDelay);
        }

        // Validate retry multiplier
        double multiplier = config.getRetryMultiplier();
        if (multiplier <= 1.0 || multiplier > 10.0) {
            throw new ConfigurationException("Invalid retry multiplier: " + multiplier +
                " (must be between 1.0 and 10.0)");
        }

        // Validate jitter factor
        double jitterFactor = config.getRetryJitterFactor();
        if (jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new ConfigurationException("Invalid retry jitter factor: " + jitterFactor +
                " (must be between 0.0 and 1.0)");
        }

        // Validate max duration
        Duration maxDuration = config.getRetryMaxDuration();
        if (maxDuration.isNegative()) {
            throw new ConfigurationException("Retry max duration cannot be negative: " + maxDuration);
        }
        if (maxDuration.toMinutes() > 10) {
            throw new ConfigurationException("Retry max duration too long: " + maxDuration +
                " (maximum 10 minutes)");
        }
    }

    /**
     * Validates parallelism configuration settings.
     */
    private void validateParallelismConfig() {
        ParallelismConfig parallelismConfig = getParallelismConfig();

        // Validate max threads
        int maxThreads = parallelismConfig.getMaxThreads();
        if (maxThreads <= 0 || maxThreads > 100) {
            throw new ConfigurationException("Invalid max threads: " + maxThreads +
                " (must be between 1 and 100)");
        }

        // Validate max parallel tasks per batch
        int maxParallelTasks = parallelismConfig.getMaxParallelTasksPerBatch();
        if (maxParallelTasks <= 0 || maxParallelTasks > maxThreads) {
            throw new ConfigurationException("Invalid max parallel tasks per batch: " + maxParallelTasks +
                " (must be between 1 and " + maxThreads + ")");
        }

        // Validate task timeout
        long taskTimeout = parallelismConfig.getTaskTimeoutSeconds();
        if (taskTimeout <= 0 || taskTimeout > 3600) { // Max 1 hour
            throw new ConfigurationException("Invalid task timeout: " + taskTimeout +
                " seconds (must be between 1 and 3600)");
        }

        // Validate batch timeout
        long batchTimeout = parallelismConfig.getBatchTimeoutSeconds();
        if (batchTimeout <= 0 || batchTimeout > 7200) { // Max 2 hours
            throw new ConfigurationException("Invalid batch timeout: " + batchTimeout +
                " seconds (must be between 1 and 7200)");
        }

        if (batchTimeout < taskTimeout) {
            throw new ConfigurationException("Batch timeout (" + batchTimeout +
                ") must be >= task timeout (" + taskTimeout + ")");
        }

        // Validate minimum tasks threshold
        int minTasks = parallelismConfig.getMinTasksForParallelExecution();
        if (minTasks < 1 || minTasks > 10) {
            throw new ConfigurationException("Invalid min tasks threshold: " + minTasks +
                " (must be between 1 and 10)");
        }

        // Validate parallelism threshold
        double parallelismThreshold = parallelismConfig.getParallelismThreshold();
        if (parallelismThreshold < 0.0 || parallelismThreshold > 1.0) {
            throw new ConfigurationException("Invalid parallelism threshold: " + parallelismThreshold +
                " (must be between 0.0 and 1.0)");
        }
    }

    /**
     * Validates if a string is a valid JDBC URL.
     */
    private boolean isValidJdbcUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.startsWith("jdbc:") && url.contains(":");
    }

    /**
     * Validates if a string is a valid HTTP/HTTPS URL.
     */
    private boolean isValidHttpUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Set of property keys that contain sensitive information.
     */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "conductor.llm.openai.api.key",
            "conductor.llm.anthropic.api.key",
            "conductor.llm.google.api.key",
            "conductor.database.password"
    );

    /**
     * Checks if a property key contains sensitive information.
     */
    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.contains(key) ||
               key.toLowerCase().contains("password") ||
               key.toLowerCase().contains("secret") ||
               key.toLowerCase().contains("key");
    }

    /**
     * Retrieves a sensitive property value from environment variables or properties.
     * <p>
     * This method first checks for an environment variable using the key converted to
     * uppercase with dots replaced by underscores (e.g., "conductor.llm.openai.api.key"
     * becomes "CONDUCTOR_LLM_OPENAI_API_KEY"). If not found, it falls back to the
     * properties file value, then to the default value.
     * </p>
     * <p>
     * This approach allows sensitive values to be injected via environment variables
     * while maintaining backwards compatibility with properties files.
     * </p>
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found
     * @return the property value from environment variables, properties, or default
     */
    public String getSecretProperty(String key, String defaultValue) {
        // Convert key to environment variable format
        String envKey = key.toUpperCase().replace('.', '_');

        // Check environment variable first
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            logger.debug("Using environment variable {} for sensitive property {}", envKey, key);
            return envValue.trim();
        }

        // Fall back to properties file
        String propValue = getString(key, defaultValue);
        if (!defaultValue.equals(propValue)) {
            logger.debug("Using properties file value for sensitive property {}", key);
        } else {
            logger.warn("No value found for sensitive property {} (checked {} env var and properties file)", key, envKey);
        }

        return propValue;
    }

    /**
     * Retrieves a sensitive property value from environment variables or properties with no default.
     * <p>
     * This is a convenience method that calls getSecretProperty(key, null) and throws
     * a ConfigurationException if no value is found.
     * </p>
     *
     * @param key the property key to lookup
     * @return the property value from environment variables or properties
     * @throws ConfigurationException if no value is found
     */
    public String getRequiredSecretProperty(String key) {
        String value = getSecretProperty(key, null);
        if (value == null || value.trim().isEmpty()) {
            String envKey = key.toUpperCase().replace('.', '_');
            throw new ConfigurationException(
                String.format("Required sensitive property '%s' not found. Set environment variable '%s' or add to properties file.",
                             key, envKey));
        }
        return value;
    }

    /**
     * Logs configuration warnings with appropriate handling of sensitive data.
     */
    private void logConfigurationWarning(String key, String value, String expectedType, String defaultValue) {
        if (isSensitiveKey(key)) {
            logger.warn("Invalid {} value for sensitive property {}. Using default value.", expectedType, key);
        } else {
            logger.warn("Invalid {} value for {}: {}. Using default: {}", expectedType, key, value, defaultValue);
        }
    }
}
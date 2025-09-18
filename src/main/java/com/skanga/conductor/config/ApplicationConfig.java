package com.skanga.conductor.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Thread-safe singleton configuration manager for the Conductor framework.
 * <p>
 * This class provides centralized configuration management with support for:
 * </p>
 * <ul>
 * <li>Property file loading from multiple sources (application.properties, environment-specific files)</li>
 * <li>System property overrides (properties starting with "conductor.")</li>
 * <li>Environment variable support (variables starting with "CONDUCTOR_")</li>
 * <li>Type-safe property access with default values</li>
 * <li>Nested configuration classes for organized property grouping</li>
 * </ul>
 * <p>
 * Configuration sources are loaded in the following priority order:
 * </p>
 * <ol>
 * <li>Property files from classpath</li>
 * <li>System properties (highest priority)</li>
 * <li>Environment variables</li>
 * </ol>
 * <p>
 * The singleton instance is created using double-checked locking for thread safety.
 * Once created, the configuration is immutable and can be safely accessed from
 * multiple threads concurrently.
 * </p>
 *
 * @since 1.0.0
 * @see DatabaseConfig
 * @see ToolConfig
 * @see LLMConfig
 * @see MemoryConfig
 */
public class ApplicationConfig {

    private static volatile ApplicationConfig instance;
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
     * This method uses double-checked locking to ensure thread-safe
     * singleton initialization. The instance is created only once
     * and subsequent calls return the same instance.
     * </p>
     *
     * @return the singleton ApplicationConfig instance
     */
    public static ApplicationConfig getInstance() {
        if (instance == null) {
            synchronized (ApplicationConfig.class) {
                if (instance == null) {
                    instance = new ApplicationConfig();
                }
            }
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    private void loadProperties() {
        String[] configFiles = {
            "/application.properties",
            "/application-dev.properties",
            "/application-prod.properties",
            "/demo.properties"
        };

        for (String configFile : configFiles) {
            try (InputStream inputStream = getClass().getResourceAsStream(configFile)) {
                if (inputStream != null) {
                    properties.load(inputStream);
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load config file " + configFile + ": " + e.getMessage());
            }
        }

        loadSystemProperties();
        loadEnvironmentVariables();
    }

    /**
     * Loads external configuration files specified via command line or system properties.
     * <p>
     * Supports multiple configuration sources in priority order:
     * </p>
     * <ol>
     * <li>--config=path/to/file.properties command line argument</li>
     * <li>-Dconfig=path/to/file.properties system property</li>
     * <li>demo.properties in current working directory</li>
     * </ol>
     */
    private void loadExternalConfiguration() {
        // Priority 1: --config command line argument (stored as system property)
        String configPath = System.getProperty("config");
        if (configPath != null) {
            loadExternalFile(configPath, "command line --config");
            return;
        }

        // Priority 2: demo.properties in current directory
        Path localDemoProperties = Paths.get("demo.properties");
        if (Files.exists(localDemoProperties)) {
            loadExternalFile(localDemoProperties.toString(), "local demo.properties");
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

                System.out.println("Loaded " + loadedCount + " properties from external configuration: " +
                    filePath + " (from " + source + ")");
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to load external configuration from " + filePath +
                " (from " + source + "): " + e.getMessage());
        }
    }

    private void loadSystemProperties() {
        System.getProperties().forEach((key, value) -> {
            if (key.toString().startsWith("conductor.")) {
                properties.setProperty(key.toString(), value.toString());
            }
        });
    }

    private void loadEnvironmentVariables() {
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("CONDUCTOR_")) {
                String propertyKey = key.toLowerCase().replace("_", ".");
                properties.setProperty(propertyKey, value);
            } else if (key.startsWith("DEMO_")) {
                // Convert DEMO_PROVIDER_TYPE to demo.provider.type
                String propertyKey = key.toLowerCase().replace("_", ".").replace("demo.", "demo.");
                properties.setProperty(propertyKey, value);
            } else if (key.equals("OPENAI_API_KEY")) {
                properties.setProperty("openai.api.key", value);
            } else if (key.equals("ANTHROPIC_API_KEY")) {
                properties.setProperty("anthropic.api.key", value);
            } else if (key.equals("GOOGLE_API_KEY")) {
                properties.setProperty("google.api.key", value);
            }
        });
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
     * @return the property value or null if not found
     */
    public String getString(String key) {
        return properties.getProperty(key);
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
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logConfigurationWarning(key, value, "integer", String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Gets a long property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a long, or the default value
     */
    public long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            logConfigurationWarning(key, value, "long", String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Gets a double property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a double, or the default value
     */
    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logConfigurationWarning(key, value, "double", String.valueOf(defaultValue));
            return defaultValue;
        }
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

    public class LLMConfig {
        public String getOpenAiApiKey() {
            return getString("conductor.llm.openai.api.key");
        }

        public String getOpenAiModel() {
            return getString("conductor.llm.openai.model", "gpt-3.5-turbo");
        }

        public String getOpenAiBaseUrl() {
            return getString("conductor.llm.openai.base.url", "https://api.openai.com/v1");
        }

        public Duration getOpenAiTimeout() {
            return getDuration("conductor.llm.openai.timeout", Duration.ofSeconds(30));
        }

        public int getOpenAiMaxRetries() {
            return getInt("conductor.llm.openai.max.retries", 3);
        }

        // Anthropic configuration
        public String getAnthropicApiKey() {
            return getString("conductor.llm.anthropic.api.key");
        }

        public String getAnthropicModel() {
            return getString("conductor.llm.anthropic.model", "claude-3-5-sonnet-20241022");
        }

        public Duration getAnthropicTimeout() {
            return getDuration("conductor.llm.anthropic.timeout", Duration.ofSeconds(30));
        }

        public int getAnthropicMaxRetries() {
            return getInt("conductor.llm.anthropic.max.retries", 3);
        }

        // Gemini configuration
        public String getGeminiApiKey() {
            return getString("conductor.llm.gemini.api.key");
        }

        public String getGeminiModel() {
            return getString("conductor.llm.gemini.model", "gemini-pro");
        }

        public Duration getGeminiTimeout() {
            return getDuration("conductor.llm.gemini.timeout", Duration.ofSeconds(30));
        }

        public int getGeminiMaxRetries() {
            return getInt("conductor.llm.gemini.max.retries", 3);
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
        } catch (Exception e) {
            throw new ConfigurationException("Configuration validation failed", e);
        }
    }

    /**
     * Validates database configuration settings.
     */
    private void validateDatabaseConfig() {
        DatabaseConfig config = getDatabaseConfig();

        // Validate JDBC URL format
        String jdbcUrl = config.getJdbcUrl();
        if (!isValidJdbcUrl(jdbcUrl)) {
            throw new ConfigurationException("Invalid JDBC URL format: " + jdbcUrl);
        }

        // Validate connection pool settings
        int maxConnections = config.getMaxConnections();
        if (maxConnections <= 0 || maxConnections > 1000) {
            throw new ConfigurationException("Invalid max connections value: " + maxConnections +
                " (must be between 1 and 1000)");
        }

        // Validate database driver
        String driver = config.getDriver();
        if (driver == null || driver.trim().isEmpty()) {
            throw new ConfigurationException("Database driver cannot be empty");
        }
    }

    /**
     * Validates tool configuration settings.
     */
    private void validateToolConfig() {
        ToolConfig config = getToolConfig();

        // Validate code runner timeout
        Duration timeout = config.getCodeRunnerTimeout();
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException("Code runner timeout must be positive: " + timeout);
        }
        if (timeout.toSeconds() > 300) { // 5 minutes max
            throw new ConfigurationException("Code runner timeout too long: " + timeout +
                " (maximum 5 minutes)");
        }

        // Validate file read base directory
        String baseDir = config.getFileReadBaseDir();
        if (baseDir == null || baseDir.trim().isEmpty()) {
            throw new ConfigurationException("File read base directory cannot be empty");
        }
        if (baseDir.contains("..")) {
            throw new ConfigurationException("File read base directory cannot contain '..': " + baseDir);
        }

        // Validate file size limits
        long maxFileSize = config.getFileReadMaxSize();
        if (maxFileSize <= 0) {
            throw new ConfigurationException("File read max size must be positive: " + maxFileSize);
        }
        if (maxFileSize > 100L * 1024 * 1024 * 1024) { // 100GB max
            throw new ConfigurationException("File read max size too large: " + maxFileSize +
                " (maximum 100GB)");
        }

        // Validate path length
        int maxPathLength = config.getFileReadMaxPathLength();
        if (maxPathLength <= 0 || maxPathLength > 32767) {
            throw new ConfigurationException("Invalid path length limit: " + maxPathLength +
                " (must be between 1 and 32767)");
        }

        // Validate audio output directory
        String audioDir = config.getAudioOutputDir();
        if (audioDir == null || audioDir.trim().isEmpty()) {
            throw new ConfigurationException("Audio output directory cannot be empty");
        }
        if (audioDir.contains("..")) {
            throw new ConfigurationException("Audio output directory cannot contain '..': " + audioDir);
        }
    }

    /**
     * Validates LLM configuration settings.
     */
    private void validateLLMConfig() {
        LLMConfig config = getLLMConfig();

        // Validate base URL format
        String baseUrl = config.getOpenAiBaseUrl();
        if (!isValidHttpUrl(baseUrl)) {
            throw new ConfigurationException("Invalid OpenAI base URL: " + baseUrl);
        }

        // Validate timeout
        Duration timeout = config.getOpenAiTimeout();
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException("OpenAI timeout must be positive: " + timeout);
        }
        if (timeout.toMinutes() > 10) { // 10 minutes max
            throw new ConfigurationException("OpenAI timeout too long: " + timeout +
                " (maximum 10 minutes)");
        }

        // Validate retry count
        int maxRetries = config.getOpenAiMaxRetries();
        if (maxRetries < 0 || maxRetries > 10) {
            throw new ConfigurationException("Invalid max retries: " + maxRetries +
                " (must be between 0 and 10)");
        }

        // Validate model name
        String model = config.getOpenAiModel();
        if (model == null || model.trim().isEmpty()) {
            throw new ConfigurationException("OpenAI model name cannot be empty");
        }

        // Validate retry configuration
        validateRetryConfig(config);

        // Validate Anthropic configuration
        validateAnthropicConfig(config);

        // Validate Gemini configuration
        validateGeminiConfig(config);
    }

    /**
     * Validates Anthropic configuration settings.
     */
    private void validateAnthropicConfig(LLMConfig config) {
        // Validate timeout
        Duration timeout = config.getAnthropicTimeout();
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException("Anthropic timeout must be positive: " + timeout);
        }
        if (timeout.toMinutes() > 10) { // 10 minutes max
            throw new ConfigurationException("Anthropic timeout too long: " + timeout +
                " (maximum 10 minutes)");
        }

        // Validate retry count
        int maxRetries = config.getAnthropicMaxRetries();
        if (maxRetries < 0 || maxRetries > 10) {
            throw new ConfigurationException("Invalid Anthropic max retries: " + maxRetries +
                " (must be between 0 and 10)");
        }

        // Validate model name
        String model = config.getAnthropicModel();
        if (model == null || model.trim().isEmpty()) {
            throw new ConfigurationException("Anthropic model name cannot be empty");
        }
    }

    /**
     * Validates Gemini configuration settings.
     */
    private void validateGeminiConfig(LLMConfig config) {
        // Validate timeout
        Duration timeout = config.getGeminiTimeout();
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException("Gemini timeout must be positive: " + timeout);
        }
        if (timeout.toMinutes() > 10) { // 10 minutes max
            throw new ConfigurationException("Gemini timeout too long: " + timeout +
                " (maximum 10 minutes)");
        }

        // Validate retry count
        int maxRetries = config.getGeminiMaxRetries();
        if (maxRetries < 0 || maxRetries > 10) {
            throw new ConfigurationException("Invalid Gemini max retries: " + maxRetries +
                " (must be between 0 and 10)");
        }

        // Validate model name
        String model = config.getGeminiModel();
        if (model == null || model.trim().isEmpty()) {
            throw new ConfigurationException("Gemini model name cannot be empty");
        }
    }

    /**
     * Validates memory configuration settings.
     */
    private void validateMemoryConfig() {
        MemoryConfig config = getMemoryConfig();

        // Validate default memory limit
        int defaultLimit = config.getDefaultMemoryLimit();
        if (defaultLimit <= 0 || defaultLimit > 10000) {
            throw new ConfigurationException("Invalid default memory limit: " + defaultLimit +
                " (must be between 1 and 10000)");
        }

        // Validate max memory entries
        int maxEntries = config.getMaxMemoryEntries();
        if (maxEntries <= 0 || maxEntries > 1000000) {
            throw new ConfigurationException("Invalid max memory entries: " + maxEntries +
                " (must be between 1 and 1,000,000)");
        }

        // Validate retention days
        int retentionDays = config.getMemoryRetentionDays();
        if (retentionDays <= 0 || retentionDays > 36500) { // 100 years max
            throw new ConfigurationException("Invalid retention days: " + retentionDays +
                " (must be between 1 and 36500)");
        }
    }

    /**
     * Validates metrics configuration settings.
     */
    private void validateMetricsConfig() {
        MetricsConfig config = getMetricsConfig();

        // Validate retention period
        Duration retention = config.getRetentionPeriod();
        if (retention.isNegative() || retention.isZero()) {
            throw new ConfigurationException("Metrics retention period must be positive: " + retention);
        }
        if (retention.toDays() > 365) { // 1 year max
            throw new ConfigurationException("Metrics retention period too long: " + retention +
                " (maximum 365 days)");
        }

        // Validate max metrics in memory
        int maxMetrics = config.getMaxMetricsInMemory();
        if (maxMetrics <= 0 || maxMetrics > 10_000_000) {
            throw new ConfigurationException("Invalid max metrics in memory: " + maxMetrics +
                " (must be between 1 and 10,000,000)");
        }

        // Validate console reporting interval
        Duration consoleInterval = config.getConsoleReportingInterval();
        if (consoleInterval.isNegative() || consoleInterval.isZero()) {
            throw new ConfigurationException("Console reporting interval must be positive: " + consoleInterval);
        }
        if (consoleInterval.toHours() > 24) {
            throw new ConfigurationException("Console reporting interval too long: " + consoleInterval +
                " (maximum 24 hours)");
        }

        // Validate file reporting interval
        Duration fileInterval = config.getFileReportingInterval();
        if (fileInterval.isNegative() || fileInterval.isZero()) {
            throw new ConfigurationException("File reporting interval must be positive: " + fileInterval);
        }
        if (fileInterval.toHours() > 24) {
            throw new ConfigurationException("File reporting interval too long: " + fileInterval +
                " (maximum 24 hours)");
        }

        // Validate output directory
        String outputDir = config.getOutputDirectory();
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
            java.net.URI uri = java.net.URI.create(url);
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
     * Logs configuration warnings with appropriate handling of sensitive data.
     */
    private void logConfigurationWarning(String key, String value, String expectedType, String defaultValue) {
        if (isSensitiveKey(key)) {
            System.err.println("Warning: Invalid " + expectedType + " value for sensitive property " +
                key + ". Using default value.");
        } else {
            System.err.println("Warning: Invalid " + expectedType + " value for " + key +
                ": " + value + ". Using default: " + defaultValue);
        }
    }
}
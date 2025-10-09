package com.skanga.conductor.config;

import com.skanga.conductor.exception.ConfigurationException;
import com.skanga.conductor.utils.SingletonHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * Thread-safe singleton configuration manager for the Conductor framework.
 * <p>
 * This class provides centralized configuration management with support for:
 * </p>
 * <ul>
 * <li>Base configuration loading (application.properties)</li>
 * <li>Profile-specific configuration (application-{profile}.properties)</li>
 * <li>External configuration files via --config parameter</li>
 * <li>System property overrides</li>
 * <li>Environment variable support</li>
 * <li>Type-safe property access with default values</li>
 * <li>Organized configuration classes for different domains</li>
 * </ul>
 *
 * @since 1.0.0
 * @see DatabaseConfig
 * @see ToolConfig
 * @see LLMConfig
 * @see MemoryConfig
 * @see MetricsConfig
 * @see ParallelismConfig
 * @see ResilienceConfig
 * @see TemplateConfig
 * @see WorkflowConfig
 */
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    private static final SingletonHolder<ApplicationConfig> HOLDER =
        SingletonHolder.of(ApplicationConfig::new);

    private final Properties properties;
    private final DatabaseConfig databaseConfig;
    private final ToolConfig toolConfig;
    private final LLMConfig llmConfig;
    private final MemoryConfig memoryConfig;
    private final MetricsConfig metricsConfig;
    private final ParallelismConfig parallelismConfig;
    private final ResilienceConfig resilienceConfig;
    private final TemplateConfig templateConfig;
    private final WorkflowConfig workflowConfig;

    private ApplicationConfig() {
        // Load all properties
        PropertyLoader loader = new PropertyLoader();
        this.properties = loader.getProperties();

        // Initialize configuration objects
        this.databaseConfig = new DatabaseConfig(properties);
        this.toolConfig = new ToolConfig(properties);
        this.llmConfig = new LLMConfig(properties);
        this.memoryConfig = new MemoryConfig(properties);
        this.metricsConfig = new MetricsConfig(properties);
        this.parallelismConfig = new ParallelismConfig(properties);
        this.resilienceConfig = new ResilienceConfig(properties);
        this.templateConfig = new TemplateConfig(properties);
        this.workflowConfig = new WorkflowConfig(properties);

        // Validation is deferred to first access to avoid initialization order issues
        // Use validate() method explicitly if needed during initialization
    }

    /**
     * Returns the singleton instance of the application configuration.
     *
     * @return the singleton ApplicationConfig instance
     */
    public static ApplicationConfig getInstance() {
        return HOLDER.get();
    }

    /**
     * Resets the singleton instance for testing purposes.
     */
    public static void resetInstance() {
        HOLDER.reset();
    }

    /**
     * Returns the database configuration settings.
     *
     * @return a DatabaseConfig instance with database-related properties
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    /**
     * Returns the tool configuration settings.
     *
     * @return a ToolConfig instance with tool-related properties
     */
    public ToolConfig getToolConfig() {
        return toolConfig;
    }

    /**
     * Returns the LLM configuration settings.
     *
     * @return an LLMConfig instance with LLM-related properties
     */
    public LLMConfig getLLMConfig() {
        return llmConfig;
    }

    /**
     * Returns the memory configuration settings.
     *
     * @return a MemoryConfig instance with memory-related properties
     */
    public MemoryConfig getMemoryConfig() {
        return memoryConfig;
    }

    /**
     * Returns the parallelism configuration settings.
     *
     * @return a ParallelismConfig instance with parallelism-related properties
     */
    public ParallelismConfig getParallelismConfig() {
        return parallelismConfig;
    }

    /**
     * Returns the metrics configuration settings.
     *
     * @return a MetricsConfig instance with metrics-related properties
     */
    public MetricsConfig getMetricsConfig() {
        return metricsConfig;
    }

    /**
     * Returns the resilience configuration settings.
     *
     * @return a ResilienceConfig instance with resilience-related properties
     */
    public ResilienceConfig getResilienceConfig() {
        return resilienceConfig;
    }

    /**
     * Returns the template configuration settings.
     *
     * @return a TemplateConfig instance with template-related properties
     */
    public TemplateConfig getTemplateConfig() {
        return templateConfig;
    }

    /**
     * Returns the workflow configuration settings.
     *
     * @return a WorkflowConfig instance with workflow-related properties
     */
    public WorkflowConfig getWorkflowConfig() {
        return workflowConfig;
    }

    // ========== Backward Compatibility Methods ==========
    // These methods delegate to ConfigurationProvider for backward compatibility

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
     * Retrieves an integer property value with a default fallback.
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found or invalid
     * @return the property value as an integer or the default value
     */
    public int getInt(String key, int defaultValue) {
        return getProperty(key, Integer::parseInt, defaultValue);
    }

    /**
     * Gets a long property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a long, or the default value
     */
    public long getLong(String key, long defaultValue) {
        return getProperty(key, Long::parseLong, defaultValue);
    }

    /**
     * Gets a double property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a double, or the default value
     */
    public double getDouble(String key, double defaultValue) {
        return getProperty(key, Double::parseDouble, defaultValue);
    }

    /**
     * Gets a boolean property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found
     * @return the property value as a boolean, or the default value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Gets a Duration property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a Duration, or the default value
     */
    public Duration getDuration(String key, Duration defaultValue) {
        return getProperty(key, Duration::parse, defaultValue);
    }

    /**
     * Gets a Path property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found
     * @return the property value as a Path, or the default value
     */
    public Path getPath(String key, Path defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Paths.get(value) : defaultValue;
    }

    /**
     * Gets a URI property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the property value as a URI, or the default value
     */
    public URI getURI(String key, URI defaultValue) {
        return getProperty(key, URI::create, defaultValue);
    }

    /**
     * Gets a Set of string values from a comma-separated property.
     *
     * @param key the property key
     * @param defaultValues the default values if property is not found
     * @return Set of property values or defaults
     */
    public Set<String> getStringSet(String key, Set<String> defaultValues) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValues;
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    /**
     * Gets a required secret property value (for API keys, passwords, etc.).
     * Secrets are loaded from the same sources as regular properties.
     *
     * @param key the property key
     * @return the secret property value
     * @throws ConfigurationException if property not found
     */
    public String getRequiredSecretProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigurationException("Required secret property not found: " + key);
        }
        return value.trim();
    }

    /**
     * Gets a secret property value with a default fallback.
     *
     * @param key the property key
     * @param defaultValue the default value if property is not found
     * @return the secret property value or default
     */
    public String getSecretProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    /**
     * Generic method for getting and converting property values.
     *
     * @param key property key
     * @param converter function to convert string to desired type
     * @param defaultValue default value if property not found or conversion fails
     * @param <T> the type of the property value
     * @return converted property value or default
     */
    private <T> T getProperty(String key, Function<String, T> converter, T defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return converter.apply(value.trim());
        } catch (Exception e) {
            logger.warn("Failed to parse property '{}' with value '{}', using default: {}",
                       key, value, defaultValue);
            return defaultValue;
        }
    }

    // ========== Validation Methods ==========

    /**
     * Validates all configuration settings for consistency and security.
     *
     * @throws ConfigurationException if any configuration value is invalid
     */
    /**
     * Validates the complete configuration using centralized ConfigurationValidator.
     * <p>
     * This method delegates to {@link ConfigurationValidator#validate(ApplicationConfig)}
     * which validates all configuration sections by invoking their getter methods.
     * The getter methods contain validation logic through JSR-303 annotations and custom checks.
     * </p>
     *
     * @throws ConfigurationException if any validation fails
     */
    /**
     * Validates the configuration.
     * <p>
     * This method can be called explicitly after construction to validate all configuration.
     * Validation is deferred from constructor to avoid initialization order issues where
     * some components might not be fully initialized when validation runs.
     * </p>
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        ConfigurationValidator.validate(this);
    }


    /**
     * Returns a copy of all loaded properties.
     *
     * @return Properties object containing all configuration
     */
    public Properties getAllProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    /**
     * Checks if a property key exists.
     *
     * @param key the property key
     * @return true if the property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
}

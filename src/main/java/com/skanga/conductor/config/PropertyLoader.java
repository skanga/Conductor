package com.skanga.conductor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Handles loading of configuration properties from multiple sources.
 * <p>
 * Configuration sources are loaded in the following priority order:
 * </p>
 * <ol>
 * <li>Base application.properties from classpath</li>
 * <li>Profile-specific application-{profile}.properties (if profile specified)</li>
 * <li>External configuration files (if specified via --config parameter)</li>
 * <li>System properties (highest priority)</li>
 * <li>Environment variables</li>
 * </ol>
 *
 * @since 1.1.0
 */
public class PropertyLoader {

    private static final Logger logger = LoggerFactory.getLogger(PropertyLoader.class);

    private final Properties properties;

    public PropertyLoader() {
        this.properties = new Properties();
        loadAllProperties();
    }

    /**
     * Returns the loaded properties.
     *
     * @return Properties object containing all loaded configuration
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Loads properties from all configured sources.
     */
    private void loadAllProperties() {
        // Always load base application.properties
        loadConfigFile("/application.properties");

        // Load environment-specific config only if profile is specified
        String activeProfile = getActiveProfile();
        if (activeProfile != null && !activeProfile.trim().isEmpty()) {
            String profileConfigFile = "/application-" + activeProfile + ".properties";
            loadConfigFile(profileConfigFile);
        }

        loadSystemProperties();
        loadEnvironmentVariables();
        loadExternalConfiguration();
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
     */
    private void loadExternalConfiguration() {
        // Load configuration file specified via --config command line argument
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
                logger.warn("Configuration file not found: {} (from {})", filePath, source);
                return;
            }

            try (InputStream inputStream = new FileInputStream(path.toFile())) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load external config file " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Loads system properties that start with "conductor." prefix.
     * <p>
     * System properties have the highest priority and will override
     * any previously loaded properties with the same key.
     * </p>
     */
    private void loadSystemProperties() {
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> key.startsWith("conductor."))
            .forEach(key -> properties.setProperty(key, System.getProperty(key)));
    }

    /**
     * Loads environment variables that start with "CONDUCTOR_" prefix.
     * <p>
     * Environment variable names are converted to property keys using a safe transformation:
     * </p>
     * <ul>
     * <li>CONDUCTOR_DATABASE_URL → conductor.database.url</li>
     * <li>CONDUCTOR_LLM_OPENAI_API_KEY → conductor.llm.openai.api.key</li>
     * </ul>
     * <p>
     * The transformation converts to lowercase and replaces underscores with dots.
     * To include a literal underscore in a property key (rare), use double underscore (__).
     * </p>
     */
    private void loadEnvironmentVariables() {
        System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("CONDUCTOR_"))
            .forEach(entry -> {
                String propertyKey = transformEnvironmentVariableName(entry.getKey());
                properties.setProperty(propertyKey, entry.getValue());
                logger.trace("Loaded environment variable: {} -> {}", entry.getKey(), propertyKey);
            });
    }

    /**
     * Transforms an environment variable name to a property key.
     * <p>
     * Rules:
     * </p>
     * <ul>
     * <li>Convert to lowercase</li>
     * <li>Replace single underscore (_) with dot (.)</li>
     * <li>Replace double underscore (__) with single underscore (_)</li>
     * </ul>
     *
     * @param envVarName the environment variable name (e.g., "CONDUCTOR_DATABASE_URL")
     * @return the property key (e.g., "conductor.database.url")
     */
    private String transformEnvironmentVariableName(String envVarName) {
        // First replace double underscores with a placeholder to preserve them
        String transformed = envVarName.replace("__", "\u0000");
        // Convert to lowercase and replace single underscores with dots
        transformed = transformed.toLowerCase().replace("_", ".");
        // Restore preserved underscores
        transformed = transformed.replace("\u0000", "_");
        return transformed;
    }
}

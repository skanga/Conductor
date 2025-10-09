package com.skanga.conductor.config;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for configuration providers with type-safe property access.
 * <p>
 * Provides utility methods for retrieving configuration values with
 * type conversion and default values.
 * </p>
 *
 * @since 1.1.0
 */
public abstract class ConfigurationProvider {

    protected final Properties properties;

    protected ConfigurationProvider(Properties properties) {
        this.properties = properties;
    }

    /**
     * Gets a string property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found
     * @return property value or default
     */
    protected String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets an optional string property value.
     *
     * @param key property key
     * @return Optional containing the value, or empty if not found
     */
    protected Optional<String> getString(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    /**
     * Gets an integer property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found or invalid
     * @return property value or default
     */
    protected int getInt(String key, int defaultValue) {
        return getProperty(key, Integer::parseInt, defaultValue);
    }

    /**
     * Gets a long property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found or invalid
     * @return property value or default
     */
    protected long getLong(String key, long defaultValue) {
        return getProperty(key, Long::parseLong, defaultValue);
    }

    /**
     * Gets a double property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found or invalid
     * @return property value or default
     */
    protected double getDouble(String key, double defaultValue) {
        return getProperty(key, Double::parseDouble, defaultValue);
    }

    /**
     * Gets a boolean property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found
     * @return property value or default
     */
    protected boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Gets a Duration property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found or invalid
     * @return property value or default
     */
    protected Duration getDuration(String key, Duration defaultValue) {
        return getProperty(key, Duration::parse, defaultValue);
    }

    /**
     * Gets a Path property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found
     * @return property value or default
     */
    protected Path getPath(String key, Path defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Paths.get(value) : defaultValue;
    }

    /**
     * Gets a URI property value.
     *
     * @param key property key
     * @param defaultValue default value if property not found or invalid
     * @return property value or default
     */
    protected URI getURI(String key, URI defaultValue) {
        return getProperty(key, URI::create, defaultValue);
    }

    /**
     * Gets a Set of string values from a comma-separated property.
     *
     * @param key property key
     * @param defaultValues default values if property not found
     * @return Set of property values or defaults
     */
    protected Set<String> getStringSet(String key, Set<String> defaultValues) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValues;
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
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
    protected <T> T getProperty(String key, Function<String, T> converter, T defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return converter.apply(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

package com.skanga.conductor.config;

/**
 * Exception thrown when configuration validation fails.
 * <p>
 * This exception is thrown during application startup when configuration
 * values are invalid, missing required properties, or contain values that
 * would cause runtime failures. It helps identify configuration issues
 * early in the application lifecycle.
 * </p>
 * <p>
 * Common scenarios that trigger this exception:
 * </p>
 * <ul>
 * <li>Invalid JDBC URLs or database connection parameters</li>
 * <li>Invalid file paths or directories that don't exist</li>
 * <li>Invalid timeout values or negative numbers</li>
 * <li>Missing required API keys or credentials</li>
 * <li>Invalid URL formats for external services</li>
 * </ul>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Constructs a new ConfigurationException with the specified detail message.
     *
     * @param message the detail message explaining the configuration error
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConfigurationException with the specified detail message and cause.
     *
     * @param message the detail message explaining the configuration error
     * @param cause the underlying cause of the configuration error
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
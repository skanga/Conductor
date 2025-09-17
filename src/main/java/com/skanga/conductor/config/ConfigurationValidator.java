package com.skanga.conductor.config;

import java.time.Duration;

/**
 * Utility class for validating configuration values and providing validation feedback.
 * <p>
 * This class provides static methods for validating common configuration patterns
 * and can be used by external components to validate configuration before
 * applying changes or during runtime configuration updates.
 * </p>
 * <p>
 * The validator provides methods for:
 * </p>
 * <ul>
 * <li>URL validation (HTTP/HTTPS, JDBC)</li>
 * <li>Numeric range validation</li>
 * <li>Duration validation</li>
 * <li>Path validation for security</li>
 * <li>Sensitive data detection</li>
 * </ul>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 * @see ConfigurationException
 */
public final class ConfigurationValidator {

    private ConfigurationValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates that an integer value is within the specified range.
     *
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the value is outside the valid range
     */
    public static void validateIntRange(int value, int min, int max, String propertyName) {
        if (value < min || value > max) {
            throw new ConfigurationException(
                String.format("Invalid %s: %d (must be between %d and %d)",
                    propertyName, value, min, max));
        }
    }

    /**
     * Validates that a long value is within the specified range.
     *
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the value is outside the valid range
     */
    public static void validateLongRange(long value, long min, long max, String propertyName) {
        if (value < min || value > max) {
            throw new ConfigurationException(
                String.format("Invalid %s: %d (must be between %d and %d)",
                    propertyName, value, min, max));
        }
    }

    /**
     * Validates that a duration is positive and within reasonable bounds.
     *
     * @param duration the duration to validate
     * @param maxMinutes the maximum allowed duration in minutes
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the duration is invalid
     */
    public static void validateDuration(Duration duration, long maxMinutes, String propertyName) {
        if (duration == null) {
            throw new ConfigurationException(propertyName + " cannot be null");
        }
        if (duration.isNegative() || duration.isZero()) {
            throw new ConfigurationException(propertyName + " must be positive: " + duration);
        }
        if (duration.toMinutes() > maxMinutes) {
            throw new ConfigurationException(
                String.format("%s too long: %s (maximum %d minutes)",
                    propertyName, duration, maxMinutes));
        }
    }

    /**
     * Validates that a string is not null or empty.
     *
     * @param value the string to validate
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the string is null or empty
     */
    public static void validateNotEmpty(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigurationException(propertyName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a path is secure and doesn't contain dangerous patterns.
     *
     * @param path the path to validate
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the path contains dangerous patterns
     */
    public static void validateSecurePath(String path, String propertyName) {
        validateNotEmpty(path, propertyName);

        if (path.contains("..")) {
            throw new ConfigurationException(
                String.format("%s cannot contain '..': %s", propertyName, path));
        }

        // Check for other potentially dangerous patterns
        if (path.contains("${") || path.contains("#{")) {
            throw new ConfigurationException(
                String.format("%s contains potentially dangerous expressions: %s", propertyName, path));
        }
    }

    /**
     * Validates that a URL is properly formatted for HTTP/HTTPS.
     *
     * @param url the URL to validate
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the URL is invalid
     */
    public static void validateHttpUrl(String url, String propertyName) {
        validateNotEmpty(url, propertyName);

        try {
            java.net.URI uri = java.net.URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new ConfigurationException(
                    String.format("Invalid %s: %s (must be http or https)", propertyName, url));
            }
        } catch (Exception e) {
            throw new ConfigurationException(
                String.format("Invalid %s format: %s", propertyName, url), e);
        }
    }

    /**
     * Validates that a URL is properly formatted for JDBC.
     *
     * @param url the JDBC URL to validate
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the JDBC URL is invalid
     */
    public static void validateJdbcUrl(String url, String propertyName) {
        validateNotEmpty(url, propertyName);

        if (!url.startsWith("jdbc:")) {
            throw new ConfigurationException(
                String.format("Invalid %s: %s (must start with 'jdbc:')", propertyName, url));
        }

        if (url.split(":").length < 3) {
            throw new ConfigurationException(
                String.format("Invalid %s format: %s", propertyName, url));
        }
    }

    /**
     * Checks if a property key represents sensitive information.
     *
     * @param key the property key to check
     * @return true if the key represents sensitive information
     */
    public static boolean isSensitiveProperty(String key) {
        if (key == null) {
            return false;
        }

        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("key") ||
               lowerKey.contains("token") ||
               lowerKey.contains("credential");
    }

    /**
     * Validates configuration summary for reporting purposes.
     *
     * @return a configuration validation summary
     */
    public static String validateAndSummarize() {
        try {
            ApplicationConfig config = ApplicationConfig.getInstance();

            StringBuilder summary = new StringBuilder();
            summary.append("Configuration Validation Summary:\n");
            summary.append("================================\n");

            // Database validation
            summary.append("Database: ");
            try {
                config.getDatabaseConfig(); // This triggers validation
                summary.append("✓ Valid\n");
            } catch (ConfigurationException e) {
                summary.append("✗ Invalid - ").append(e.getMessage()).append("\n");
            }

            // Tool validation
            summary.append("Tools: ");
            try {
                config.getToolConfig(); // This triggers validation
                summary.append("✓ Valid\n");
            } catch (ConfigurationException e) {
                summary.append("✗ Invalid - ").append(e.getMessage()).append("\n");
            }

            // LLM validation
            summary.append("LLM: ");
            try {
                config.getLLMConfig(); // This triggers validation
                summary.append("✓ Valid\n");
            } catch (ConfigurationException e) {
                summary.append("✗ Invalid - ").append(e.getMessage()).append("\n");
            }

            // Memory validation
            summary.append("Memory: ");
            try {
                config.getMemoryConfig(); // This triggers validation
                summary.append("✓ Valid\n");
            } catch (ConfigurationException e) {
                summary.append("✗ Invalid - ").append(e.getMessage()).append("\n");
            }

            return summary.toString();

        } catch (Exception e) {
            return "Configuration validation failed: " + e.getMessage();
        }
    }
}
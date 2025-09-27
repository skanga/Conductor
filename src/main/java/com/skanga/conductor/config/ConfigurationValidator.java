package com.skanga.conductor.config;

import com.skanga.conductor.exception.ConfigurationException;
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
     * <p>
     * Performs comprehensive validation including path traversal, injection patterns,
     * encoding attacks, and platform-specific security checks.
     * </p>
     *
     * @param path the path to validate
     * @param propertyName the name of the property being validated (for error messages)
     * @throws ConfigurationException if the path contains dangerous patterns
     */
    public static void validateSecurePath(String path, String propertyName) {
        validateNotEmpty(path, propertyName);

        // Basic path traversal check
        if (path.contains("..")) {
            throw new ConfigurationException(
                String.format("%s cannot contain '..': %s", propertyName, path));
        }

        // Comprehensive injection pattern checks
        if (containsInjectionPatterns(path)) {
            throw new ConfigurationException(
                String.format("%s contains potentially dangerous expressions: %s", propertyName, path));
        }

        // Encoded traversal checks
        if (containsEncodedTraversalPatterns(path)) {
            throw new ConfigurationException(
                String.format("%s contains encoded path traversal: %s", propertyName, path));
        }

        // Platform-specific security checks
        if (containsPlatformSpecificThreats(path)) {
            throw new ConfigurationException(
                String.format("%s contains platform-specific security threats: %s", propertyName, path));
        }

        // Control character and invisible character checks
        if (containsDangerousCharacters(path)) {
            throw new ConfigurationException(
                String.format("%s contains dangerous characters: %s", propertyName, path));
        }

        // Length and complexity checks
        if (path.length() > 4096) { // Reasonable path length limit
            throw new ConfigurationException(
                String.format("%s exceeds maximum length of 4096 characters: %d", propertyName, path.length()));
        }

        // Check for excessive nesting (potential zip bomb indicators)
        int separatorCount = (int) path.chars().filter(c -> c == '/' || c == '\\').count();
        if (separatorCount > 50) { // Reasonable nesting limit for configuration paths
            throw new ConfigurationException(
                String.format("%s has excessive path nesting (%d levels): %s", propertyName, separatorCount, path));
        }
    }

    /**
     * Checks for various injection patterns in configuration paths.
     */
    private static boolean containsInjectionPatterns(String path) {
        String lowerPath = path.toLowerCase();
        return path.contains("${") ||      // Java EL injection
               path.contains("#{") ||      // EL injection
               path.contains("%{") ||      // Apache Struts injection
               path.contains("$(") ||      // Shell command substitution
               path.contains("{{") ||      // Template injection (Handlebars, etc.)
               path.contains("{%") ||      // Django/Jinja2 injection
               path.contains("<%") ||      // JSP/ASP injection
               path.contains("[%") ||      // Template Toolkit
               lowerPath.contains("javascript:") || // JavaScript protocol
               lowerPath.contains("vbscript:") ||   // VBScript protocol
               lowerPath.contains("data:") ||       // Data URI scheme
               path.contains("`") ||       // Shell execution
               path.contains(";") ||       // Command separator
               path.contains("&") ||       // Command chaining
               path.contains("|");         // Pipe operations
    }

    /**
     * Checks for encoded path traversal patterns.
     */
    private static boolean containsEncodedTraversalPatterns(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.contains("%2e%2e") ||          // URL encoded ..
               lowerPath.contains("%252e%252e") ||      // Double URL encoded ..
               lowerPath.contains("\\u002e\\u002e") ||  // Unicode escaped ..
               lowerPath.contains("\\x2e\\x2e") ||      // Hex escaped ..
               lowerPath.contains("%u002e%u002e") ||    // Unicode URL encoded ..
               lowerPath.contains("%c0%ae") ||          // Overlong UTF-8 encoded .
               lowerPath.contains("%e0%80%ae") ||       // Another overlong UTF-8 .
               lowerPath.contains("..%2f") ||           // Mixed encoded/unencoded
               lowerPath.contains("..%5c") ||           // Mixed with backslash
               lowerPath.contains("%2f..") ||           // Forward slash with ..
               lowerPath.contains("%5c..") ||           // Backslash with ..
               path.matches(".*\\.{3,}.*");            // Multiple dots (3 or more)
    }

    /**
     * Checks for platform-specific security threats.
     */
    private static boolean containsPlatformSpecificThreats(String path) {
        String upperPath = path.toUpperCase();

        // Windows device names
        String[] windowsDevices = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };

        for (String device : windowsDevices) {
            // Exact device name or device with extension
            if (upperPath.equals(device) || upperPath.startsWith(device + ".")) {
                return true;
            }

            // Device name as path component (must be preceded and followed by separators or end)
            String devicePattern = "(?:^|[/\\\\])" + device + "(?:[/\\\\]|\\.|$)";
            if (upperPath.matches(".*" + devicePattern + ".*")) {
                return true;
            }
        }

        // UNC path attempts
        if (path.startsWith("\\\\")) {
            return true;
        }

        // Drive letter access attempts (Windows)
        if (path.matches("^[A-Za-z]:.*") || path.contains(":[/\\\\]")) {
            return true;
        }

        // Check for system directory access attempts
        String lowerPath = path.toLowerCase();

        // Only block absolute paths to dangerous system directories
        return lowerPath.startsWith("/system32/") || lowerPath.startsWith("\\system32\\") ||
               lowerPath.startsWith("/windows/") || lowerPath.startsWith("\\windows\\") ||
               lowerPath.equals("/etc/passwd") || lowerPath.equals("/etc/shadow") ||
               lowerPath.startsWith("/etc/passwd/") || lowerPath.startsWith("/etc/shadow/") ||
               lowerPath.startsWith("/boot/") || lowerPath.startsWith("\\boot\\") ||
               lowerPath.startsWith("/proc/") || lowerPath.startsWith("/sys/");
    }

    /**
     * Checks for dangerous control characters and invisible characters.
     */
    private static boolean containsDangerousCharacters(String path) {
        for (char c : path.toCharArray()) {
            // Control characters (0x00-0x1F and 0x7F-0x9F)
            if ((c >= 0x00 && c <= 0x1F) || (c >= 0x7F && c <= 0x9F)) {
                return true;
            }

            // Zero-width and invisible characters
            if (c == '\u200B' || // Zero Width Space
                c == '\u200C' || // Zero Width Non-Joiner
                c == '\u200D' || // Zero Width Joiner
                c == '\uFEFF' || // Zero Width No-Break Space (BOM)
                c == '\u2060' || // Word Joiner
                c == '\u202D' || // Left-to-Right Override
                c == '\u202E' || // Right-to-Left Override
                c == '\u2066' || // Left-to-Right Isolate
                c == '\u2067' || // Right-to-Left Isolate
                c == '\u2068' || // First Strong Isolate
                c == '\u2069') { // Pop Directional Isolate
                return true;
            }

            // Forbidden filename characters (Windows and general)
            if (c == '<' || c == '>' || c == ':' || c == '"' ||
                c == '|' || c == '?' || c == '*') {
                return true;
            }
        }
        return false;
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
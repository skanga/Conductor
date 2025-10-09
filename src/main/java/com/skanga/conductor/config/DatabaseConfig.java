package com.skanga.conductor.config;

import jakarta.validation.constraints.*;
import java.util.Properties;

/**
 * Database configuration settings.
 * <p>
 * Provides access to database connection and pool configuration.
 * </p>
 *
 * @since 1.1.0
 */
public class DatabaseConfig extends ConfigurationProvider {

    public DatabaseConfig(Properties properties) {
        super(properties);
    }

    @NotNull(message = "Database JDBC URL cannot be null")
    @Pattern(regexp = "^jdbc:.*", message = "Database URL must start with 'jdbc:'")
    public String getJdbcUrl() {
        String url = getString("conductor.database.url", "jdbc:h2:./data/subagentsdb;FILE_LOCK=FS");
        if (url == null || !url.startsWith("jdbc:")) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database URL must start with 'jdbc:': " + url);
        }
        return url;
    }

    public String getUsername() {
        return getString("conductor.database.username", "sa");
    }

    /**
     * Gets the database password with strength validation.
     * <p>
     * Validates password strength when a non-empty password is provided.
     * For production use, passwords should meet minimum security requirements:
     * </p>
     * <ul>
     * <li>Minimum 8 characters length</li>
     * <li>Contains at least one uppercase letter</li>
     * <li>Contains at least one lowercase letter</li>
     * <li>Contains at least one digit</li>
     * <li>Contains at least one special character</li>
     * </ul>
     * <p>
     * Empty passwords are allowed for development/testing with embedded databases like H2,
     * but a warning is logged for production environments.
     * </p>
     *
     * @return the database password (may be empty for development)
     */
    public String getPassword() {
        String password = getString("conductor.database.password", "");

        // Allow empty passwords for development/testing (e.g., H2 embedded)
        if (password == null || password.isEmpty()) {
            // Check if this is a production-like database
            String jdbcUrl = getString("conductor.database.url", "");
            if (!jdbcUrl.contains("h2:mem") && !jdbcUrl.contains("h2:./data")) {
                org.slf4j.LoggerFactory.getLogger(DatabaseConfig.class)
                    .warn("Empty database password detected for non-embedded database. " +
                          "This is insecure for production use.");
            }
            return password;
        }

        // Validate password strength for non-empty passwords
        validatePasswordStrength(password);
        return password;
    }

    /**
     * Validates password strength according to security best practices.
     *
     * @param password the password to validate
     * @throws com.skanga.conductor.exception.ConfigurationException if password is weak
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database password must be at least 8 characters long");
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUppercase) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database password must contain at least one uppercase letter");
        }
        if (!hasLowercase) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database password must contain at least one lowercase letter");
        }
        if (!hasDigit) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database password must contain at least one digit");
        }
        if (!hasSpecial) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database password must contain at least one special character");
        }
    }

    @NotBlank(message = "Database driver cannot be empty")
    public String getDriver() {
        String driver = getString("conductor.database.driver", "org.h2.Driver");
        if (driver == null || driver.trim().isEmpty()) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Database driver cannot be empty");
        }
        return driver;
    }

    @Min(value = 1, message = "Max connections must be at least 1")
    @Max(value = 1000, message = "Max connections cannot exceed 1000")
    public int getMaxConnections() {
        int maxConnections = getInt("conductor.database.max.connections", 10);
        if (maxConnections < 1 || maxConnections > 1000) {
            throw new com.skanga.conductor.exception.ConfigurationException(
                "Max connections must be between 1 and 1000: " + maxConnections);
        }
        return maxConnections;
    }
}

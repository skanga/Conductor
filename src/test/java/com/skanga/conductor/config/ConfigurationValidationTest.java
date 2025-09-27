package com.skanga.conductor.config;

import com.skanga.conductor.utils.SingletonHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Configuration Validation Tests")
class ConfigurationValidationTest {

    @BeforeEach
    void setUp() {
        // Clear any existing system properties that might interfere
        System.clearProperty("conductor.database.url");
        System.clearProperty("conductor.database.max.connections");
        System.clearProperty("conductor.tools.coderunner.timeout");
        ApplicationConfig.resetInstance();
    }

    @Test
    @DisplayName("Valid configuration should pass validation")
    void testValidConfiguration() {
        // This should not throw any exceptions
        assertDoesNotThrow(() -> {
            ApplicationConfig config = ApplicationConfig.getInstance();
            assertNotNull(config);
        });
    }

    @Test
    @DisplayName("Invalid JDBC URL should throw ConfigurationException")
    void testInvalidJdbcUrl() {
        System.setProperty("conductor.database.url", "invalid-url");

        com.skanga.conductor.exception.SingletonException.InitializationException exception =
            assertThrows(com.skanga.conductor.exception.SingletonException.InitializationException.class, () -> {
                ApplicationConfig.getInstance();
            });

        // Verify the root cause is ConfigurationException
        Throwable cause = exception.getCause();
        assertInstanceOf(com.skanga.conductor.exception.ConfigurationException.class, cause);
    }

    @Test
    @DisplayName("Invalid max connections should throw ConfigurationException")
    void testInvalidMaxConnections() {
        System.setProperty("conductor.database.max.connections", "-1");

        com.skanga.conductor.exception.SingletonException.InitializationException exception =
            assertThrows(com.skanga.conductor.exception.SingletonException.InitializationException.class, () -> {
                ApplicationConfig.getInstance();
            });

        // Verify the root cause is ConfigurationException
        Throwable cause = exception.getCause();
        assertInstanceOf(com.skanga.conductor.exception.ConfigurationException.class, cause);
    }

    @Test
    @DisplayName("ConfigurationValidator should validate ranges correctly")
    void testConfigurationValidatorRanges() {
        // Valid range should not throw
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateIntRange(5, 1, 10, "test.property");
        });

        // Invalid range should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateIntRange(15, 1, 10, "test.property");
        });
    }

    @Test
    @DisplayName("ConfigurationValidator should validate durations correctly")
    void testConfigurationValidatorDurations() {
        // Valid duration should not throw
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateDuration(Duration.ofSeconds(30), 5, "test.timeout");
        });

        // Negative duration should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateDuration(Duration.ofSeconds(-1), 5, "test.timeout");
        });

        // Too long duration should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateDuration(Duration.ofMinutes(10), 5, "test.timeout");
        });
    }

    @Test
    @DisplayName("ConfigurationValidator should detect sensitive properties")
    void testSensitivePropertyDetection() {
        assertTrue(ConfigurationValidator.isSensitiveProperty("conductor.llm.openai.api.key"));
        assertTrue(ConfigurationValidator.isSensitiveProperty("database.password"));
        assertTrue(ConfigurationValidator.isSensitiveProperty("auth.secret"));
        assertFalse(ConfigurationValidator.isSensitiveProperty("database.url"));
        assertFalse(ConfigurationValidator.isSensitiveProperty("server.port"));
    }

    @Test
    @DisplayName("ConfigurationValidator should validate URLs correctly")
    void testUrlValidation() {
        // Valid HTTP URL should not throw
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateHttpUrl("https://api.openai.com/v1", "test.url");
        });

        // Invalid URL should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateHttpUrl("not-a-url", "test.url");
        });

        // Valid JDBC URL should not throw
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateJdbcUrl("jdbc:h2:mem:test", "test.jdbc");
        });

        // Invalid JDBC URL should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateJdbcUrl("not-jdbc-url", "test.jdbc");
        });
    }

    @Test
    @DisplayName("ConfigurationValidator should validate secure paths")
    void testSecurePathValidation() {
        // Valid path should not throw
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateSecurePath("./safe/path", "test.path");
        });

        // Path with .. should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath("../dangerous/path", "test.path");
        });

        // Path with expressions should throw
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath("./path/${evil.expression}", "test.path");
        });
    }

    @Test
    @DisplayName("Configuration validation summary should be generated")
    void testValidationSummary() {
        String summary = ConfigurationValidator.validateAndSummarize();
        assertNotNull(summary);
        assertTrue(summary.contains("Configuration Validation Summary"));
        assertTrue(summary.contains("Database:"));
        assertTrue(summary.contains("Tools:"));
        assertTrue(summary.contains("LLM:"));
        assertTrue(summary.contains("Memory:"));
    }
}
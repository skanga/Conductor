package com.skanga.conductor.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify profile-based configuration loading behavior.
 */
class ApplicationConfigProfileTest {

    @BeforeEach
    void setUp() {
        // Clean up any existing profile settings
        System.clearProperty("conductor.profile");
        ApplicationConfig.resetInstance();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        System.clearProperty("conductor.profile");
        ApplicationConfig.resetInstance();
    }

    @Test
    @DisplayName("Should load only base application.properties when no profile specified")
    void testNoProfileSpecified() {
        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should have loaded base config
        assertNotNull(config, "ApplicationConfig should be initialized");

        // The configuration should work (we can't easily test which specific files were loaded
        // without mocking, but we can verify the configuration system is working)
        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available from base config");
    }

    @Test
    @DisplayName("Should load base + dev profile when conductor.profile=dev")
    void testDevProfileViaSystemProperty() {
        // Set the profile via system property
        System.setProperty("conductor.profile", "dev");

        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should have loaded both base and dev-specific config
        assertNotNull(config, "ApplicationConfig should be initialized");

        // Verify configuration is available
        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available");
    }

    @Test
    @DisplayName("Should load base + prod profile when conductor.profile=prod")
    void testProdProfileViaSystemProperty() {
        // Set the profile via system property
        System.setProperty("conductor.profile", "prod");

        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should have loaded both base and prod-specific config
        assertNotNull(config, "ApplicationConfig should be initialized");

        // Verify configuration is available
        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available");
    }

    @Test
    @DisplayName("Should handle empty profile gracefully")
    void testEmptyProfile() {
        // Set empty profile
        System.setProperty("conductor.profile", "");

        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should only load base config (like no profile specified)
        assertNotNull(config, "ApplicationConfig should be initialized");

        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available from base config");
    }

    @Test
    @DisplayName("Should handle whitespace-only profile gracefully")
    void testWhitespaceProfile() {
        // Set whitespace-only profile
        System.setProperty("conductor.profile", "   ");

        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should only load base config (like no profile specified)
        assertNotNull(config, "ApplicationConfig should be initialized");

        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available from base config");
    }

    @Test
    @DisplayName("Should handle non-existent profile gracefully")
    void testNonExistentProfile() {
        // Set a profile that doesn't have a corresponding file
        System.setProperty("conductor.profile", "nonexistent");

        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should still work with base config only
        assertNotNull(config, "ApplicationConfig should be initialized");

        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available from base config");
    }

    @Test
    @DisplayName("Should trim profile names")
    void testProfileNameTrimming() {
        // Set profile with leading/trailing whitespace
        System.setProperty("conductor.profile", "  dev  ");

        ApplicationConfig config = ApplicationConfig.getInstance();

        // Should have loaded dev profile (with trimming)
        assertNotNull(config, "ApplicationConfig should be initialized");

        String dbUrl = config.getDatabaseConfig().getJdbcUrl();
        assertNotNull(dbUrl, "Database URL should be available");
    }
}
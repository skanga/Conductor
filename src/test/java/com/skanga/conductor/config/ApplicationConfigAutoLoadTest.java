package com.skanga.conductor.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that demo.properties is not automatically loaded from current directory.
 */
class ApplicationConfigAutoLoadTest {

    @BeforeEach
    void setUp() {
        // Reset any existing instance to ensure fresh loading
        ApplicationConfig.resetInstance();
    }

    @Test
    @DisplayName("demo.properties should not be automatically loaded from current directory")
    void testDemoPropertiesNotAutoLoaded() {
        ApplicationConfig config = ApplicationConfig.getInstance();

        // Check if demo.properties values are NOT loaded
        String testValue = config.getString("test.auto.loaded").orElse(null);
        String conductorTestValue = config.getString("conductor.test.value").orElse(null);

        assertNull(testValue, "test.auto.loaded should not be loaded from demo.properties");
        assertNull(conductorTestValue, "conductor.test.value should not be loaded from demo.properties");
    }

}
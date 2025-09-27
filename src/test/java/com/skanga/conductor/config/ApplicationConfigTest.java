package com.skanga.conductor.config;

import com.skanga.conductor.testbase.ConfigTestBase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationConfigTest extends ConfigTestBase {

    @Test
    void testSingletonAndDefaults() {
        ApplicationConfig config1 = createTestConfig();
        ApplicationConfig config2 = ApplicationConfig.getInstance();
        assertSame(config1, config2, "ApplicationConfig should be a singleton");

        // Verify base configuration values from application.properties
        // (profile-specific configs are only loaded when explicitly specified)
        assertEquals(10, config1.getDatabaseConfig().getMaxConnections());
        assertEquals(5, config1.getToolConfig().getCodeRunnerTimeout().getSeconds());
        assertEquals("gpt-3.5-turbo", config1.getLLMConfig().getOpenAiModel());
        assertEquals(10, config1.getMemoryConfig().getDefaultMemoryLimit());
        assertTrue(config1.getDatabaseConfig().getJdbcUrl().startsWith("jdbc:h2:"));
    }
}

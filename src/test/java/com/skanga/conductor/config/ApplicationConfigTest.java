package com.skanga.conductor.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationConfigTest {

    @Test
    void testSingletonAndDefaults() {
        ApplicationConfig config1 = ApplicationConfig.getInstance();
        ApplicationConfig config2 = ApplicationConfig.getInstance();
        assertSame(config1, config2, "ApplicationConfig should be a singleton");

        // Verify some default values
        // Expect values from production configuration (application-prod.properties) which overrides defaults
        assertEquals("jdbc:h2:./data/prod/subagentsdb;FILE_LOCK=FS;CACHE_SIZE=8192", config1.getDatabaseConfig().getJdbcUrl());
        assertEquals(20, config1.getDatabaseConfig().getMaxConnections());
        assertEquals(3, config1.getToolConfig().getCodeRunnerTimeout().getSeconds());
        assertEquals("gpt-3.5-turbo", config1.getLLMConfig().getOpenAiModel());
        assertEquals(5, config1.getMemoryConfig().getDefaultMemoryLimit());
    }
}

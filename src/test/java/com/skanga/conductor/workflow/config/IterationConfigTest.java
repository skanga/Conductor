package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IterationConfig Tests")
class IterationConfigTest {

    @Test
    @DisplayName("Should create IterationConfig with default values")
    void testDefaultValues() {
        // When
        IterationConfig config = new IterationConfig();

        // Then
        assertEquals(IterationConfig.IterationType.DATA_DRIVEN, config.getType());
        assertEquals("item", config.getVariable());
        assertEquals(Integer.valueOf(1), config.getStart());
        assertEquals(Integer.valueOf(100), config.getMaxIterations());
        assertEquals(Boolean.FALSE, config.getParallel());
        assertEquals(Integer.valueOf(4), config.getMaxConcurrent());
        assertEquals("fail_fast", config.getErrorStrategy());
        assertEquals(Integer.valueOf(3), config.getRetryCount());
        assertEquals(Long.valueOf(300000L), config.getIterationTimeout());
    }

    @Test
    @DisplayName("Should handle all iteration types")
    void testIterationTypes() {
        IterationConfig config = new IterationConfig();

        // Test DATA_DRIVEN
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        assertEquals(IterationConfig.IterationType.DATA_DRIVEN, config.getType());
        assertEquals("data_driven", config.getTypeAsString());

        // Test COUNT_BASED
        config.setType(IterationConfig.IterationType.COUNT_BASED);
        assertEquals(IterationConfig.IterationType.COUNT_BASED, config.getType());
        assertEquals("count_based", config.getTypeAsString());

        // Test CONDITIONAL
        config.setType(IterationConfig.IterationType.CONDITIONAL);
        assertEquals(IterationConfig.IterationType.CONDITIONAL, config.getType());
        assertEquals("conditional", config.getTypeAsString());
    }

    @Test
    @DisplayName("Should set type from string")
    void testSetTypeFromString() {
        IterationConfig config = new IterationConfig();

        config.setTypeFromString("data_driven");
        assertEquals(IterationConfig.IterationType.DATA_DRIVEN, config.getType());

        config.setTypeFromString("count_based");
        assertEquals(IterationConfig.IterationType.COUNT_BASED, config.getType());

        config.setTypeFromString("conditional");
        assertEquals(IterationConfig.IterationType.CONDITIONAL, config.getType());
    }

    @Test
    @DisplayName("Should throw exception for unknown iteration type")
    void testUnknownIterationType() {
        assertThrows(IllegalArgumentException.class, () -> {
            IterationConfig.IterationType.fromYamlValue("unknown_type");
        });
    }

    @Test
    @DisplayName("Should validate data-driven iteration configuration")
    void testDataDrivenValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("toc_generation.chapters");
        config.setVariable("chapter");

        // Should not throw exception
        assertDoesNotThrow(() -> config.validate());

        // Test missing source
        config.setSource(null);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setSource("");
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setSource("   ");
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should validate count-based iteration configuration")
    void testCountBasedValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.COUNT_BASED);
        config.setCount("5");
        config.setStart(1);
        config.setVariable("counter");

        // Should not throw exception
        assertDoesNotThrow(() -> config.validate());

        // Test missing count
        config.setCount(null);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setCount("");
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        // Test negative start
        config.setCount("5");
        config.setStart(-1);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should validate conditional iteration configuration")
    void testConditionalValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.CONDITIONAL);
        config.setCondition("${chapters_remaining} > 0");
        config.setMaxIterations(50);
        config.setVariable("iteration");

        // Should not throw exception
        assertDoesNotThrow(() -> config.validate());

        // Test missing condition
        config.setCondition(null);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setCondition("");
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        // Test invalid max iterations
        config.setCondition("${chapters_remaining} > 0");
        config.setMaxIterations(0);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setMaxIterations(-1);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should validate variable name")
    void testVariableValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("test.data");

        // Test null variable
        config.setVariable(null);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        // Test empty variable
        config.setVariable("");
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        // Test whitespace variable
        config.setVariable("   ");
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should validate parallel configuration")
    void testParallelValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("test.data");
        config.setVariable("item");
        config.setParallel(true);

        // Valid parallel configuration
        config.setMaxConcurrent(8);
        assertDoesNotThrow(() -> config.validate());

        // Invalid max concurrent
        config.setMaxConcurrent(0);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setMaxConcurrent(-1);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should validate retry configuration")
    void testRetryValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("test.data");
        config.setVariable("item");

        // Valid retry count
        config.setRetryCount(5);
        assertDoesNotThrow(() -> config.validate());

        // Invalid retry count
        config.setRetryCount(-1);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should validate timeout configuration")
    void testTimeoutValidation() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("test.data");
        config.setVariable("item");

        // Valid timeout
        config.setIterationTimeout(60000L);
        assertDoesNotThrow(() -> config.validate());

        // Invalid timeout
        config.setIterationTimeout(0L);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        config.setIterationTimeout(-1L);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    @DisplayName("Should handle parallel execution settings")
    void testParallelExecution() {
        IterationConfig config = new IterationConfig();

        // Default not parallel
        assertFalse(config.isParallelEnabled());
        assertEquals(1, config.getEffectiveMaxConcurrent());

        // Enable parallel
        config.setParallel(true);
        assertTrue(config.isParallelEnabled());
        assertEquals(4, config.getEffectiveMaxConcurrent()); // Default max concurrent

        // Custom max concurrent
        config.setMaxConcurrent(8);
        assertEquals(8, config.getEffectiveMaxConcurrent());

        // Disable parallel
        config.setParallel(false);
        assertFalse(config.isParallelEnabled());
        assertEquals(1, config.getEffectiveMaxConcurrent());
    }

    @Test
    @DisplayName("Should handle update variables for conditional iteration")
    void testUpdateVariables() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.CONDITIONAL);
        config.setCondition("${count} > 0");
        config.setVariable("item");

        Map<String, String> updateVars = Map.of(
            "count", "${count} - 1",
            "processed", "${processed} + 1"
        );

        config.setUpdateVariables(updateVars);
        assertEquals(updateVars, config.getUpdateVariables());

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    @DisplayName("Should handle error strategies")
    void testErrorStrategies() {
        IterationConfig config = new IterationConfig();

        // Test different error strategies
        String[] strategies = {"fail_fast", "continue", "retry"};

        for (String strategy : strategies) {
            config.setErrorStrategy(strategy);
            assertEquals(strategy, config.getErrorStrategy());
        }
    }

    @Test
    @DisplayName("Should create comprehensive data-driven configuration")
    void testDataDrivenConfiguration() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("book_outline.chapters");
        config.setVariable("chapter");
        config.setParallel(true);
        config.setMaxConcurrent(6);
        config.setErrorStrategy("continue");
        config.setRetryCount(2);
        config.setIterationTimeout(180000L);

        assertDoesNotThrow(() -> config.validate());
        assertTrue(config.isParallelEnabled());
        assertEquals(6, config.getEffectiveMaxConcurrent());
    }

    @Test
    @DisplayName("Should create comprehensive count-based configuration")
    void testCountBasedConfiguration() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.COUNT_BASED);
        config.setCount("${book.chapter_count}");
        config.setStart(0);
        config.setVariable("chapter_index");
        config.setParallel(false);
        config.setErrorStrategy("retry");
        config.setRetryCount(5);

        assertDoesNotThrow(() -> config.validate());
        assertFalse(config.isParallelEnabled());
        assertEquals("chapter_index", config.getVariable());
        assertEquals(Integer.valueOf(0), config.getStart());
    }

    @Test
    @DisplayName("Should create comprehensive conditional configuration")
    void testConditionalConfiguration() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.CONDITIONAL);
        config.setCondition("${remaining_chapters} > 0");
        config.setMaxIterations(20);
        config.setVariable("current_chapter");
        config.setUpdateVariables(Map.of(
            "remaining_chapters", "${remaining_chapters} - 1",
            "completed_chapters", "${completed_chapters} + 1"
        ));
        config.setParallel(false);
        config.setErrorStrategy("fail_fast");

        assertDoesNotThrow(() -> config.validate());
        assertEquals(Integer.valueOf(20), config.getMaxIterations());
        assertNotNull(config.getUpdateVariables());
        assertEquals(2, config.getUpdateVariables().size());
    }

    @Test
    @DisplayName("Should handle edge cases")
    void testEdgeCases() {
        IterationConfig config = new IterationConfig();

        // Test null type
        config.setType(null);
        assertThrows(IllegalArgumentException.class, () -> config.validate());

        // Test with null parallel (should default to false)
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("test.data");
        config.setVariable("item");
        config.setParallel(null);
        assertFalse(config.isParallelEnabled());
        assertEquals(1, config.getEffectiveMaxConcurrent());

        // Test with null max concurrent (should default to 4 when parallel)
        config.setParallel(true);
        config.setMaxConcurrent(null);
        assertEquals(4, config.getEffectiveMaxConcurrent());
    }

    @Test
    @DisplayName("Should provide meaningful toString")
    void testToString() {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("chapters");
        config.setVariable("chapter");
        config.setParallel(true);

        String toString = config.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("DATA_DRIVEN"));
        assertTrue(toString.contains("chapters"));
        assertTrue(toString.contains("chapter"));
        assertTrue(toString.contains("true"));
    }

    @Test
    @DisplayName("Should handle concurrent configuration access")
    void testConcurrentAccess() throws InterruptedException {
        IterationConfig config = new IterationConfig();
        config.setType(IterationConfig.IterationType.DATA_DRIVEN);
        config.setSource("test.data");
        config.setVariable("item");

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Test concurrent access to configuration methods
                    config.getType();
                    config.getSource();
                    config.getVariable();
                    config.isParallelEnabled();
                    config.getEffectiveMaxConcurrent();
                    config.validate();
                    config.toString();
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
        }
    }
}
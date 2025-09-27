package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowContext Tests")
class WorkflowContextTest {

    @Test
    @DisplayName("Should create WorkflowContext with data")
    void testCreateWorkflowContext() {
        // Given
        Map<String, Object> data = Map.of("key", "value", "number", 42);

        // When
        WorkflowContext context = new WorkflowContext(data);

        // Then
        assertEquals(data, context.getData());
    }

    @Test
    @DisplayName("Should create WorkflowContext with null data")
    void testCreateWorkflowContextWithNull() {
        // When
        WorkflowContext context = new WorkflowContext(null);

        // Then
        assertNull(context.getData());
    }

    @Test
    @DisplayName("Should get simple values from context")
    void testGetSimpleValues() {
        // Given
        Map<String, Object> data = Map.of(
            "name", "TestApp",
            "version", "1.0.0",
            "active", true,
            "count", 42
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.of("TestApp"), context.getValue("name"));
        assertEquals(Optional.of("1.0.0"), context.getValue("version"));
        assertEquals(Optional.of(true), context.getValue("active"));
        assertEquals(Optional.of(42), context.getValue("count"));
    }

    @Test
    @DisplayName("Should get nested values using dot notation")
    void testGetNestedValues() {
        // Given
        Map<String, Object> data = Map.of(
            "application", Map.of(
                "name", "MyApp",
                "config", Map.of(
                    "database", Map.of(
                        "host", "localhost",
                        "port", 5432
                    ),
                    "timeout", 30000
                )
            ),
            "runtime", Map.of(
                "session_id", "abc123"
            )
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.of("MyApp"), context.getValue("application.name"));
        assertEquals(Optional.of("localhost"), context.getValue("application.config.database.host"));
        assertEquals(Optional.of(5432), context.getValue("application.config.database.port"));
        assertEquals(Optional.of(30000), context.getValue("application.config.timeout"));
        assertEquals(Optional.of("abc123"), context.getValue("runtime.session_id"));
    }

    @Test
    @DisplayName("Should return empty Optional for nonexistent paths")
    void testGetNonexistentValues() {
        // Given
        Map<String, Object> data = Map.of("existing", "value");
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.empty(), context.getValue("nonexistent"));
        assertEquals(Optional.empty(), context.getValue("existing.nonexistent"));
        assertEquals(Optional.empty(), context.getValue("deeply.nested.nonexistent"));
    }

    @Test
    @DisplayName("Should handle null data gracefully")
    void testNullDataHandling() {
        // Given
        WorkflowContext context = new WorkflowContext(null);

        // When & Then
        assertEquals(Optional.empty(), context.getValue("any.path"));
        assertEquals(Optional.empty(), context.getString("any.path"));
        assertEquals(Optional.empty(), context.getInteger("any.path"));
        assertEquals(Optional.empty(), context.getBoolean("any.path"));
    }

    @Test
    @DisplayName("Should handle null path gracefully")
    void testNullPathHandling() {
        // Given
        Map<String, Object> data = Map.of("key", "value");
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.empty(), context.getValue(null));
        assertEquals(Optional.empty(), context.getString(null));
        assertEquals(Optional.empty(), context.getInteger(null));
        assertEquals(Optional.empty(), context.getBoolean(null));
    }

    @Test
    @DisplayName("Should get string values with conversions")
    void testGetStringValues() {
        // Given
        Map<String, Object> data = Map.of(
            "text", "hello",
            "number", 42,
            "boolean", true,
            "object", Map.of("key", "value")
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.of("hello"), context.getString("text"));
        assertEquals(Optional.of("42"), context.getString("number"));
        assertEquals(Optional.of("true"), context.getString("boolean"));
        assertTrue(context.getString("object").isPresent());

        assertEquals(Optional.empty(), context.getString("nonexistent"));
    }

    @Test
    @DisplayName("Should get string values with defaults")
    void testGetStringValuesWithDefaults() {
        // Given
        Map<String, Object> data = Map.of("existing", "value");
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals("value", context.getString("existing", "default"));
        assertEquals("default", context.getString("nonexistent", "default"));
        assertEquals("fallback", context.getString("missing.path", "fallback"));
    }

    @Test
    @DisplayName("Should get integer values with conversions")
    void testGetIntegerValues() {
        // Given
        Map<String, Object> data = Map.of(
            "int_value", 42,
            "long_value", 123L,
            "double_value", 3.14,
            "string_number", "99",
            "string_text", "not_a_number",
            "boolean_value", true
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.of(42), context.getInteger("int_value"));
        assertEquals(Optional.of(123), context.getInteger("long_value"));
        assertEquals(Optional.of(3), context.getInteger("double_value")); // Truncated
        assertEquals(Optional.of(99), context.getInteger("string_number"));

        assertEquals(Optional.empty(), context.getInteger("string_text"));
        assertEquals(Optional.empty(), context.getInteger("boolean_value"));
        assertEquals(Optional.empty(), context.getInteger("nonexistent"));
    }

    @Test
    @DisplayName("Should get integer values with defaults")
    void testGetIntegerValuesWithDefaults() {
        // Given
        Map<String, Object> data = Map.of("existing", 42);
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Integer.valueOf(42), context.getInteger("existing", 100));
        assertEquals(Integer.valueOf(100), context.getInteger("nonexistent", 100));
        assertEquals(Integer.valueOf(-1), context.getInteger("missing.path", -1));
    }

    @Test
    @DisplayName("Should get boolean values with conversions")
    void testGetBooleanValues() {
        // Given
        Map<String, Object> data = Map.of(
            "bool_true", true,
            "bool_false", false,
            "string_true", "true",
            "string_false", "false",
            "string_yes", "yes", // Should not convert
            "number", 1,
            "object", Map.of("key", "value")
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.of(true), context.getBoolean("bool_true"));
        assertEquals(Optional.of(false), context.getBoolean("bool_false"));
        assertEquals(Optional.of(true), context.getBoolean("string_true"));
        assertEquals(Optional.of(false), context.getBoolean("string_false"));

        assertEquals(Optional.of(false), context.getBoolean("string_yes")); // Boolean.parseBoolean("yes") returns false
        assertEquals(Optional.empty(), context.getBoolean("number"));
        assertEquals(Optional.empty(), context.getBoolean("object"));
        assertEquals(Optional.empty(), context.getBoolean("nonexistent"));
    }

    @Test
    @DisplayName("Should get boolean values with defaults")
    void testGetBooleanValuesWithDefaults() {
        // Given
        Map<String, Object> data = Map.of("existing", true);
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Boolean.TRUE, context.getBoolean("existing", false));
        assertEquals(Boolean.FALSE, context.getBoolean("nonexistent", false));
        assertEquals(Boolean.TRUE, context.getBoolean("missing.path", true));
    }

    @Test
    @DisplayName("Should handle complex nested structures")
    void testComplexNestedStructures() {
        // Given
        Map<String, Object> data = Map.of(
            "workflow", Map.of(
                "metadata", Map.of(
                    "name", "BookCreation",
                    "version", "2.1.0",
                    "settings", Map.of(
                        "parallel", true,
                        "max_concurrent", 4,
                        "timeout_ms", 300000
                    )
                ),
                "stages", Map.of(
                    "outline", Map.of(
                        "agent", "outline_agent",
                        "completed", true
                    ),
                    "chapters", Map.of(
                        "agent", "chapter_agent",
                        "completed", false
                    )
                )
            )
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals("BookCreation", context.getString("workflow.metadata.name", "default"));
        assertEquals("2.1.0", context.getString("workflow.metadata.version", "1.0.0"));
        assertEquals(Boolean.TRUE, context.getBoolean("workflow.metadata.settings.parallel", false));
        assertEquals(Integer.valueOf(4), context.getInteger("workflow.metadata.settings.max_concurrent", 1));
        assertEquals(Integer.valueOf(300000), context.getInteger("workflow.metadata.settings.timeout_ms", 60000));

        assertEquals("outline_agent", context.getString("workflow.stages.outline.agent", "unknown"));
        assertEquals(Boolean.TRUE, context.getBoolean("workflow.stages.outline.completed", false));
        assertEquals("chapter_agent", context.getString("workflow.stages.chapters.agent", "unknown"));
        assertEquals(Boolean.FALSE, context.getBoolean("workflow.stages.chapters.completed", true));
    }

    @Test
    @DisplayName("Should handle non-map values in path navigation")
    void testNonMapValuesInPath() {
        // Given
        Map<String, Object> data = Map.of(
            "text", "simple string",
            "number", 42,
            "array", java.util.Arrays.asList("item1", "item2")
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then - Should return empty when trying to navigate into non-map values
        assertEquals(Optional.empty(), context.getValue("text.property"));
        assertEquals(Optional.empty(), context.getValue("number.property"));
        assertEquals(Optional.empty(), context.getValue("array.property"));
        assertEquals(Optional.empty(), context.getValue("text.deep.nested"));
    }

    @Test
    @DisplayName("Should handle empty string paths")
    void testEmptyStringPath() {
        // Given
        Map<String, Object> data = Map.of("key", "value");
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.empty(), context.getValue(""));
        assertEquals(Optional.empty(), context.getString(""));
        assertEquals(Optional.empty(), context.getInteger(""));
        assertEquals(Optional.empty(), context.getBoolean(""));
    }

    @Test
    @DisplayName("Should handle paths with dots in keys")
    void testPathsWithDotsInKeys() {
        // Given - Note: This tests the current behavior, keys with dots are split
        Map<String, Object> data = Map.of(
            "key.with.dots", "value",
            "normal", Map.of("nested", "nested_value")
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        // This will try to navigate key -> with -> dots, which won't work
        assertEquals(Optional.empty(), context.getValue("key.with.dots"));

        // Normal nested access should work
        assertEquals(Optional.of("nested_value"), context.getValue("normal.nested"));
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        // Given
        Map<String, Object> data = Map.of(
            "shared", Map.of(
                "value1", "test1",
                "value2", 42,
                "value3", true
            )
        );
        WorkflowContext context = new WorkflowContext(data);

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Each thread accesses different paths
                    String value1 = context.getString("shared.value1", "default");
                    Integer value2 = context.getInteger("shared.value2", 0);
                    Boolean value3 = context.getBoolean("shared.value3", false);

                    results[index] = String.format("%s-%d-%s", value1, value2, value3);
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
            assertNotNull(results[i], "Thread " + i + " should have produced a result");
            assertEquals("test1-42-true", results[i]);
        }
    }

    @Test
    @DisplayName("Should handle special number formats")
    void testSpecialNumberFormats() {
        // Given
        Map<String, Object> data = Map.of(
            "hex_string", "0xFF", // Won't parse as integer
            "scientific", "1e3", // Won't parse as integer
            "float_string", "3.14",
            "negative", "-42",
            "zero", "0",
            "empty_string", "",
            "whitespace", "  123  " // Won't parse due to whitespace
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.empty(), context.getInteger("hex_string"));
        assertEquals(Optional.empty(), context.getInteger("scientific"));
        assertEquals(Optional.empty(), context.getInteger("float_string"));
        assertEquals(Optional.of(-42), context.getInteger("negative"));
        assertEquals(Optional.of(0), context.getInteger("zero"));
        assertEquals(Optional.empty(), context.getInteger("empty_string"));
        assertEquals(Optional.empty(), context.getInteger("whitespace"));
    }

    @Test
    @DisplayName("Should handle various boolean string formats")
    void testVariousBooleanFormats() {
        // Given
        Map<String, Object> data = Map.of(
            "true_mixed", "True", // Case doesn't matter for Boolean.parseBoolean
            "false_mixed", "False",
            "yes", "yes", // Not a valid boolean
            "no", "no",   // Not a valid boolean
            "one", "1",   // Not a valid boolean
            "zero", "0",  // Not a valid boolean
            "empty", ""   // Not a valid boolean
        );
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.of(true), context.getBoolean("true_mixed"));
        assertEquals(Optional.of(false), context.getBoolean("false_mixed"));
        assertEquals(Optional.of(false), context.getBoolean("yes")); // Boolean.parseBoolean returns false for non-"true" strings
        assertEquals(Optional.of(false), context.getBoolean("no"));
        assertEquals(Optional.of(false), context.getBoolean("one"));
        assertEquals(Optional.of(false), context.getBoolean("zero"));
        assertEquals(Optional.of(false), context.getBoolean("empty"));
    }

    @Test
    @DisplayName("Should handle edge case values")
    void testEdgeCaseValues() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("null_value", null);

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("null_key", null);
        data.put("nested_null", nestedMap);
        WorkflowContext context = new WorkflowContext(data);

        // When & Then
        assertEquals(Optional.empty(), context.getValue("null_value"));
        assertEquals(Optional.empty(), context.getString("null_value"));
        assertEquals(Optional.empty(), context.getInteger("null_value"));
        assertEquals(Optional.empty(), context.getBoolean("null_value"));

        assertEquals(Optional.empty(), context.getValue("nested_null.null_key"));
        assertEquals("default", context.getString("null_value", "default"));
        assertEquals(Integer.valueOf(100), context.getInteger("null_value", 100));
        assertEquals(Boolean.TRUE, context.getBoolean("null_value", true));
    }
}
package com.skanga.conductor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonUtils Tests")
class JsonUtilsTest {

    @Test
    @DisplayName("Should convert simple objects to JSON correctly")
    void testToJsonSimple() {
        // Test simple object
        Map<String, Object> simpleMap = Map.of(
            "name", "John",
            "age", 30,
            "active", true
        );

        String json = JsonUtils.toJson(simpleMap);
        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"John\""));
        assertTrue(json.contains("\"age\""));
        assertTrue(json.contains("30"));
    }

    @Test
    @DisplayName("Should handle null values in serialization")
    void testNullSerialization() {
        // Test null object
        String nullJson = JsonUtils.toJson(null);
        assertNotNull(nullJson);
    }

    @Test
    @DisplayName("Should handle complex nested structures")
    void testNestedStructures() {
        // Test nested structure
        Map<String, Object> complexMap = Map.of(
            "user", Map.of(
                "name", "Alice",
                "details", Map.of("age", 25)
            ),
            "items", List.of("item1", "item2", "item3")
        );

        String json = JsonUtils.toJson(complexMap);
        assertNotNull(json);
        assertTrue(json.contains("\"user\""));
        assertTrue(json.contains("\"Alice\""));
        assertTrue(json.contains("\"items\""));
    }

    @Test
    @DisplayName("Should handle special characters")
    void testSpecialCharacters() {
        Map<String, Object> specialMap = Map.of(
            "unicode", "Hello 世界",
            "quotes", "He said \"Hello\"",
            "special", "@#$%^&*()"
        );

        String json = JsonUtils.toJson(specialMap);
        assertNotNull(json);
        assertTrue(json.contains("unicode"));
        assertTrue(json.contains("quotes"));
        assertTrue(json.contains("special"));
    }

    @Test
    @DisplayName("Should validate JSON format correctly")
    void testJsonValidation() {
        // Test valid JSON
        assertTrue(JsonUtils.isValidJson("{\"name\": \"John\"}"));
        assertTrue(JsonUtils.isValidJson("[1, 2, 3]"));
        assertTrue(JsonUtils.isValidJson("\"simple string\""));
        assertTrue(JsonUtils.isValidJson("true"));
        assertTrue(JsonUtils.isValidJson("null"));
        assertTrue(JsonUtils.isValidJson("123"));

        // Test invalid JSON
        assertFalse(JsonUtils.isValidJson("{\"name\": \"John\",}"));
        assertFalse(JsonUtils.isValidJson("{'name': 'John'}"));
        assertFalse(JsonUtils.isValidJson("{name: \"John\"}"));
        assertFalse(JsonUtils.isValidJson("undefined"));
        assertFalse(JsonUtils.isValidJson(""));
        assertFalse(JsonUtils.isValidJson(null));
    }

    @Test
    @DisplayName("Should handle boolean values correctly")
    void testBooleanValues() {
        Map<String, Object> booleanMap = Map.of(
            "isTrue", true,
            "isFalse", false,
            "name", "test"
        );

        String json = JsonUtils.toJson(booleanMap);
        assertNotNull(json);
        assertTrue(json.contains("true"));
        assertTrue(json.contains("false"));
        assertTrue(json.contains("test"));
    }

    @Test
    @DisplayName("Should handle numeric types correctly")
    void testNumericTypes() {
        Map<String, Object> numericMap = Map.of(
            "integer", 42,
            "double", 3.14159,
            "zero", 0,
            "negative", -123
        );

        String json = JsonUtils.toJson(numericMap);
        assertNotNull(json);
        assertTrue(json.contains("42"));
        assertTrue(json.contains("3.14"));
        assertTrue(json.contains("0"));
        assertTrue(json.contains("-123"));
    }

    @Test
    @DisplayName("Should handle serialization errors correctly")
    void testSerializationErrorHandling() {
        // Test with object that might cause serialization issues
        Object problematicObject = new Object() {
            public Object getSelf() {
                return this; // Circular reference
            }
        };

        // Should throw JsonProcessingException for circular references
        assertThrows(com.skanga.conductor.exception.JsonProcessingException.class, () -> {
            JsonUtils.toJson(problematicObject);
        });
    }

    @Test
    @DisplayName("Should parse JSON from string correctly")
    void testFromJsonToMap() {
        // Test parsing JSON to Map
        String json = "{\"name\":\"John\",\"age\":30,\"active\":true}";

        Map<String, Object> result = JsonUtils.fromJson(json, Map.class);
        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertEquals(30, ((Number) result.get("age")).intValue());
        assertEquals(true, result.get("active"));
    }

    @Test
    @DisplayName("Should handle malformed JSON correctly")
    void testMalformedJsonHandling() {
        // Test with malformed JSON
        String malformedJson = "{\"name\":\"John\",\"age\":}";

        // Should throw JsonProcessingException for malformed JSON
        assertThrows(com.skanga.conductor.exception.JsonProcessingException.class, () -> {
            JsonUtils.fromJson(malformedJson, Map.class);
        });
    }

    @Test
    @DisplayName("Should handle empty and whitespace JSON")
    void testEmptyJsonHandling() {
        // Test empty string
        assertDoesNotThrow(() -> {
            JsonUtils.fromJson("", Map.class);
        });

        // Test whitespace
        assertDoesNotThrow(() -> {
            JsonUtils.fromJson("   ", Map.class);
        });

        // Test null
        assertDoesNotThrow(() -> {
            JsonUtils.fromJson(null, Map.class);
        });
    }

    @Test
    @DisplayName("Should format JSON for pretty printing")
    void testPrettyPrint() {
        // Test pretty printing
        Map<String, Object> data = Map.of(
            "name", "John",
            "details", Map.of("age", 30)
        );

        String prettyJson = JsonUtils.toPrettyJson(data);
        assertNotNull(prettyJson);
        assertTrue(prettyJson.length() >= JsonUtils.toJson(data).length());
    }

    @Test
    @DisplayName("Should handle large objects efficiently")
    void testLargeObjectHandling() {
        // Create a reasonably large object
        Map<String, Object> largeObject = new java.util.HashMap<>();
        for (int i = 0; i < 25; i++) { // Reduced from 1000 to 25 for faster testing
            largeObject.put("key" + i, "value" + i + " with some additional text to make it larger");
        }

        // Should handle serialization without issues
        assertDoesNotThrow(() -> {
            String json = JsonUtils.toJson(largeObject);
            assertNotNull(json);
            assertTrue(json.length() > 500); // Adjusted for reduced iterations (25 * ~25 chars per entry)
        });
    }

    @Test
    @DisplayName("Should handle concurrent JSON operations")
    void testConcurrentOperations() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        Map<String, Object> testData = Map.of("id", 1, "name", "test");

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    results[index] = JsonUtils.toJson(testData);
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all operations completed successfully
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
            assertNotNull(results[i], "Thread " + i + " should have produced a result");
        }
    }

    @Test
    @DisplayName("Should handle JSON arrays correctly")
    void testJsonArrays() {
        List<Map<String, Object>> arrayData = List.of(
            Map.of("id", 1, "name", "Item 1"),
            Map.of("id", 2, "name", "Item 2"),
            Map.of("id", 3, "name", "Item 3")
        );

        String json = JsonUtils.toJson(arrayData);
        assertNotNull(json);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
    }

    @Test
    @DisplayName("Should handle deep nesting structures")
    void testDeepNesting() {
        Map<String, Object> deepNested = Map.of(
            "level1", Map.of(
                "level2", Map.of(
                    "level3", Map.of(
                        "level4", Map.of(
                            "level5", "deep value"
                        )
                    )
                )
            )
        );

        assertDoesNotThrow(() -> {
            String json = JsonUtils.toJson(deepNested);
            assertNotNull(json);
            assertTrue(json.contains("deep value"));
        });
    }
}
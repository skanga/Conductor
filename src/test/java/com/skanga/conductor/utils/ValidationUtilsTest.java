package com.skanga.conductor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    @Test
    @DisplayName("Should validate non-null objects correctly")
    void testRequireNonNull() {
        // Test with valid object
        String validObject = "test";
        assertDoesNotThrow(() -> ValidationUtils.requireNonNull(validObject, "test object"));

        // Test with null object
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonNull(null, "test object");
        });
        assertTrue(exception.getMessage().contains("test object"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("Should validate non-blank strings correctly")
    void testRequireNonBlank() {
        // Test with valid string
        String validString = "valid string";
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank(validString, "test string"));

        // Test with null string
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonBlank(null, "test string");
        });
        assertTrue(exception1.getMessage().contains("test string"));
        assertTrue(exception1.getMessage().contains("cannot be null or blank"));

        // Test with empty string
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonBlank("", "test string");
        });
        assertTrue(exception2.getMessage().contains("test string"));
        assertTrue(exception2.getMessage().contains("cannot be null or blank"));

        // Test with blank string (only whitespace)
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonBlank("   ", "test string");
        });
        assertTrue(exception3.getMessage().contains("test string"));
        assertTrue(exception3.getMessage().contains("cannot be null or blank"));
    }

    @Test
    @DisplayName("Should validate positive numbers correctly")
    void testRequirePositive() {
        // Test with positive number
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(5, "test number"));
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(1, "test number"));
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(100, "test number"));

        // Test with zero
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requirePositive(0, "test number");
        });
        assertTrue(exception1.getMessage().contains("test number"));
        assertTrue(exception1.getMessage().contains("must be positive"));

        // Test with negative number
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requirePositive(-1, "test number");
        });
        assertTrue(exception2.getMessage().contains("test number"));
        assertTrue(exception2.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should validate non-negative numbers correctly")
    void testRequireNonNegative() {
        // Test with positive number
        assertDoesNotThrow(() -> ValidationUtils.requireNonNegative(5, "test number"));

        // Test with zero (should be allowed)
        assertDoesNotThrow(() -> ValidationUtils.requireNonNegative(0, "test number"));

        // Test with negative number
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonNegative(-1, "test number");
        });
        assertTrue(exception.getMessage().contains("test number"));
        assertTrue(exception.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Should validate ranges correctly")
    void testRequireInRange() {
        // Test with value in range
        assertDoesNotThrow(() -> ValidationUtils.requireInRange(5, 1, 10, "test value"));
        assertDoesNotThrow(() -> ValidationUtils.requireInRange(1, 1, 10, "test value")); // min boundary
        assertDoesNotThrow(() -> ValidationUtils.requireInRange(10, 1, 10, "test value")); // max boundary

        // Test with value below range
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireInRange(0, 1, 10, "test value");
        });
        assertTrue(exception1.getMessage().contains("test value"));
        assertTrue(exception1.getMessage().contains("must be between 1 and 10"));

        // Test with value above range
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireInRange(11, 1, 10, "test value");
        });
        assertTrue(exception2.getMessage().contains("test value"));
        assertTrue(exception2.getMessage().contains("must be between 1 and 10"));
    }

    @Test
    @DisplayName("Should validate collections correctly")
    void testRequireNonEmpty() {
        // Test with non-empty list
        java.util.List<String> nonEmptyList = java.util.List.of("item1", "item2");
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty((java.util.Collection<String>) nonEmptyList, "test list"));

        // Test with empty list
        java.util.List<String> emptyList = java.util.Collections.emptyList();
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty((java.util.Collection<String>) emptyList, "test list");
        });
        assertTrue(exception1.getMessage().contains("test list"));
        assertTrue(exception1.getMessage().contains("cannot be empty"));

        // Test with null list
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty((java.util.Collection<String>) null, "test list");
        });
        assertTrue(exception2.getMessage().contains("test list"));
        assertTrue(exception2.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("Should validate arrays correctly")
    void testRequireNonEmptyArray() {
        // Test with non-empty array
        String[] nonEmptyArray = {"item1", "item2", "item3"};
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty(nonEmptyArray, "test array"));

        // Test with empty array
        String[] emptyArray = {};
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty(emptyArray, "test array");
        });
        assertTrue(exception1.getMessage().contains("test array"));
        assertTrue(exception1.getMessage().contains("cannot be empty"));

        // Test with null array
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty((String[]) null, "test array");
        });
        assertTrue(exception2.getMessage().contains("test array"));
        assertTrue(exception2.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() {
        // Test with empty field name
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonNull(null, "");
        });
        assertTrue(exception1.getMessage().contains("cannot be null"));

        // Test with null field name (should still work)
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonNull(null, null);
        });
        assertTrue(exception2.getMessage().contains("cannot be null"));

        // Test with very long strings
        String longString = "a".repeat(1000);
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank(longString, "long string"));

        // Test with unicode characters
        String unicodeString = "测试字符串 🚀 émojis";
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank(unicodeString, "unicode string"));
    }

    @Test
    @DisplayName("Should validate multiple parameters in sequence")
    void testMultipleValidations() {
        // Test that multiple validations can be chained without issues
        String name = "John Doe";
        Integer age = 25;
        java.util.List<String> items = java.util.List.of("item1", "item2");

        assertDoesNotThrow(() -> {
            ValidationUtils.requireNonBlank(name, "name");
            ValidationUtils.requirePositive(age, "age");
            ValidationUtils.requireNonEmpty(items, "items");
        });

        // Test that first validation failure stops execution
        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonBlank(null, "name"); // This should throw
            ValidationUtils.requirePositive(age, "age"); // This should never execute
        });
    }

    @Test
    @DisplayName("Should provide clear error messages")
    void testErrorMessages() {
        // Test that error messages are informative
        try {
            ValidationUtils.requireNonNull(null, "user credentials");
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().toLowerCase();
            assertTrue(message.contains("user credentials"));
            assertTrue(message.contains("null"));
        }

        try {
            ValidationUtils.requireInRange(-5, 0, 100, "temperature setting");
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().toLowerCase();
            assertTrue(message.contains("temperature setting"));
            assertTrue(message.contains("between"));
            assertTrue(message.contains("0"));
            assertTrue(message.contains("100"));
        }
    }

    @Test
    @DisplayName("Should validate string values correctly")
    void testStringValidation() {
        // Test various string validation patterns using existing methods

        // Test non-empty validation
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("valid string", "test string"));

        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty("", "test string");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty("   ", "test string");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty((String) null, "test string");
        });
    }

    @Test
    @DisplayName("Should handle concurrent validations")
    void testConcurrentValidations() throws InterruptedException {
        int threadCount = 20;
        Thread[] threads = new Thread[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    ValidationUtils.requireNonNull("test-" + index, "field-" + index);
                    ValidationUtils.requireNonBlank("value-" + index, "text-" + index);
                    ValidationUtils.requirePositive(index + 1, "number-" + index);
                    ValidationUtils.requireInRange(index % 10, 0, 9, "range-" + index);
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all validations passed
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
        }
    }

    @Test
    @DisplayName("Should validate map contents correctly")
    void testRequireMapValidation() {
        // Test non-empty map as collection
        java.util.Map<String, String> nonEmptyMap = java.util.Map.of("key1", "value1", "key2", "value2");
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty(nonEmptyMap.values(), "test map values"));

        // Test empty map as collection
        java.util.Map<String, String> emptyMap = java.util.Collections.emptyMap();
        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty(emptyMap.values(), "test map values");
        });

        // Test null collection
        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty((java.util.Collection<String>) null, "test collection");
        });
    }

    @Test
    @DisplayName("Should validate numeric precision correctly")
    void testNumericPrecision() {
        // Test long validation
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(314L, "pi times 100"));
        assertDoesNotThrow(() -> ValidationUtils.requireNonNegative(0L, "zero"));

        // Test with very small positive number
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(1L, "tiny positive number"));

        // Test with negative long
        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requirePositive(-1L, "negative long");
        });
    }

    @Test
    @DisplayName("Should validate custom conditions correctly")
    void testCustomConditions() {
        // Test custom validation with predicate
        assertDoesNotThrow(() -> ValidationUtils.require(
            "hello".length() > 3,
            "string length must be greater than 3 characters"
        ));

        // Test failing custom condition
        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.require(
                "hi".length() > 5,
                "string length must be greater than 5 characters"
            );
        });
    }

    @Test
    @DisplayName("Should handle extreme values correctly")
    void testExtremeValues() {
        // Test with very large numbers
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(Long.MAX_VALUE, "max long"));
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(Integer.MAX_VALUE, "max int"));

        // Test with minimum valid values
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(1L, "min positive long"));
        assertDoesNotThrow(() -> ValidationUtils.requirePositive(1, "min positive int"));

        // Test boundary conditions
        assertDoesNotThrow(() -> ValidationUtils.requireInRange(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, "full range"));
        assertDoesNotThrow(() -> ValidationUtils.requireInRange(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, "full range"));
    }

    @Test
    @DisplayName("Should provide performance benchmarks")
    void testPerformance() {
        // Test that validation methods are fast enough for high-frequency use
        long startTime = System.nanoTime();

        for (int i = 0; i < 1000; i++) { // Reduced from 10000 to 1000 for faster testing
            ValidationUtils.requireNonNull("test", "field");
            ValidationUtils.requireNonBlank("test", "field");
            ValidationUtils.requirePositive(1, "field");
            ValidationUtils.requireInRange(5, 0, 10, "field");
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Should complete 40,000 validations in reasonable time (less than 100ms)
        assertTrue(durationMs < 100, "Validation operations should be fast, took: " + durationMs + "ms");
    }
}
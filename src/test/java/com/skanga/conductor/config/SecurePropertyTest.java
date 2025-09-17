package com.skanga.conductor.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecureProperty Tests")
class SecurePropertyTest {

    private SecureProperty secureProperty;

    @BeforeEach
    void setUp() {
        // Test with a known sensitive value
        secureProperty = SecureProperty.of("sensitive-api-key-12345");
    }

    @AfterEach
    void tearDown() {
        if (secureProperty != null && !secureProperty.isCleared()) {
            secureProperty.clear();
        }
    }

    @Test
    @DisplayName("Should create SecureProperty from string")
    void testCreateFromString() {
        try (SecureProperty prop = SecureProperty.of("test-value")) {
            assertEquals("test-value", prop.getValue());
            assertEquals(10, prop.length());
            assertFalse(prop.isEmpty());
            assertFalse(prop.isCleared());
        }
    }

    @Test
    @DisplayName("Should create SecureProperty from char array")
    void testCreateFromCharArray() {
        char[] value = "test-value".toCharArray();
        try (SecureProperty prop = SecureProperty.of(value)) {
            assertEquals("test-value", prop.getValue());
            assertArrayEquals("test-value".toCharArray(), prop.getValueAsChars());
        }
    }

    @Test
    @DisplayName("Should create empty SecureProperty")
    void testCreateEmpty() {
        try (SecureProperty prop = SecureProperty.empty()) {
            assertTrue(prop.isEmpty());
            assertEquals(0, prop.length());
            assertEquals("", prop.getValue());
        }
    }

    @Test
    @DisplayName("Should reject null values")
    void testRejectNullValues() {
        assertThrows(IllegalArgumentException.class, () -> SecureProperty.of((String) null));
        assertThrows(IllegalArgumentException.class, () -> SecureProperty.of((char[]) null));
    }

    @Test
    @DisplayName("Should clear sensitive data")
    void testClearSensitiveData() {
        assertFalse(secureProperty.isCleared());
        assertEquals("sensitive-api-key-12345", secureProperty.getValue());

        secureProperty.clear();

        assertTrue(secureProperty.isCleared());
        assertThrows(IllegalStateException.class, () -> secureProperty.getValue());
        assertThrows(IllegalStateException.class, () -> secureProperty.getValueAsChars());
        assertThrows(IllegalStateException.class, () -> secureProperty.length());
        assertThrows(IllegalStateException.class, () -> secureProperty.isEmpty());
    }

    @Test
    @DisplayName("Should auto-clear with try-with-resources")
    void testAutoCloseClearing() {
        SecureProperty prop;
        try (SecureProperty tempProp = SecureProperty.of("auto-clear-test")) {
            prop = tempProp;
            assertEquals("auto-clear-test", prop.getValue());
            assertFalse(prop.isCleared());
        }
        // Should be cleared after leaving try block
        assertTrue(prop.isCleared());
    }

    @Test
    @DisplayName("Should provide safe toString representation")
    void testSafeToString() {
        String toStringResult = secureProperty.toString();
        assertFalse(toStringResult.contains("sensitive-api-key-12345"));
        assertTrue(toStringResult.contains("SecureProperty"));
        assertTrue(toStringResult.contains("length="));
        assertTrue(toStringResult.contains("cleared=false"));

        secureProperty.clear();
        String clearedToString = secureProperty.toString();
        assertTrue(clearedToString.contains("cleared=true"));
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void testEqualsAndHashCode() {
        try (SecureProperty prop1 = SecureProperty.of("test-value");
             SecureProperty prop2 = SecureProperty.of("test-value");
             SecureProperty prop3 = SecureProperty.of("different-value")) {

            // Equal values should be equal
            assertEquals(prop1, prop2);
            assertEquals(prop1.hashCode(), prop2.hashCode());

            // Different values should not be equal
            assertNotEquals(prop1, prop3);
            assertNotEquals(prop1.hashCode(), prop3.hashCode());

            // Cleared properties
            prop1.clear();
            prop2.clear();
            assertEquals(prop1, prop2); // Both cleared should be equal
        }
    }

    @Test
    @DisplayName("Should handle idempotent clearing")
    void testIdempotentClearing() {
        assertFalse(secureProperty.isCleared());

        secureProperty.clear();
        assertTrue(secureProperty.isCleared());

        // Clearing again should not cause issues
        secureProperty.clear();
        assertTrue(secureProperty.isCleared());
    }

    @Test
    @DisplayName("Should return copy of char array to prevent external modification")
    void testCharArrayCopy() {
        char[] chars = secureProperty.getValueAsChars();

        // Modify the returned array
        chars[0] = 'X';

        // Original should be unchanged
        assertEquals("sensitive-api-key-12345", secureProperty.getValue());
        assertNotEquals('X', secureProperty.getValueAsChars()[0]);
    }

    @Test
    @DisplayName("Should handle empty strings correctly")
    void testEmptyString() {
        try (SecureProperty empty = SecureProperty.of("")) {
            assertTrue(empty.isEmpty());
            assertEquals(0, empty.length());
            assertEquals("", empty.getValue());
            assertArrayEquals(new char[0], empty.getValueAsChars());
        }
    }

    @Test
    @DisplayName("Should handle special characters")
    void testSpecialCharacters() {
        String specialValue = "key-with-!@#$%^&*()_+{}|:<>?[];',./~`";
        try (SecureProperty prop = SecureProperty.of(specialValue)) {
            assertEquals(specialValue, prop.getValue());
            assertArrayEquals(specialValue.toCharArray(), prop.getValueAsChars());
        }
    }

    @Test
    @DisplayName("Should handle Unicode characters")
    void testUnicodeCharacters() {
        String unicodeValue = "key-with-émojis-🔐🛡️-and-中文";
        try (SecureProperty prop = SecureProperty.of(unicodeValue)) {
            assertEquals(unicodeValue, prop.getValue());
            assertArrayEquals(unicodeValue.toCharArray(), prop.getValueAsChars());
        }
    }
}
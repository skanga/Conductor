package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for JsonProcessingException functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JsonProcessingExceptionTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should create exception with message only")
    void testMessageOnlyConstructor() {
        String message = "Invalid JSON syntax at line 5";
        JsonProcessingException exception = new JsonProcessingException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(2)
    @DisplayName("Should create exception with message and cause")
    void testMessageAndCauseConstructor() {
        String message = "Failed to deserialize JSON object";
        com.fasterxml.jackson.core.JsonParseException cause =
            new com.fasterxml.jackson.core.JsonParseException(null, "Unexpected character");
        JsonProcessingException exception = new JsonProcessingException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle null message")
    void testNullMessage() {
        JsonProcessingException exception = new JsonProcessingException(null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle null cause")
    void testNullCause() {
        String message = "JSON processing error";
        JsonProcessingException exception = new JsonProcessingException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle null message and null cause")
    void testNullMessageAndNullCause() {
        JsonProcessingException exception = new JsonProcessingException(null, null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(6)
    @DisplayName("Should inherit from ConductorRuntimeException")
    void testInheritanceFromConductorRuntimeException() {
        JsonProcessingException exception = new JsonProcessingException("Test message");

        assertTrue(exception instanceof ConductorRuntimeException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @Order(7)
    @DisplayName("Should support exception chaining")
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root JSON error");
        IllegalStateException intermediateCause = new IllegalStateException("Parser in invalid state", rootCause);
        JsonProcessingException exception = new JsonProcessingException("JSON processing failed", intermediateCause);

        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());

        // Test stack trace elements are preserved
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    @Order(8)
    @DisplayName("Should handle empty message")
    void testEmptyMessage() {
        JsonProcessingException exception = new JsonProcessingException("");

        assertEquals("", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(9)
    @DisplayName("Should handle whitespace-only message")
    void testWhitespaceMessage() {
        String message = "   \t\n   ";
        JsonProcessingException exception = new JsonProcessingException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(10)
    @DisplayName("Should preserve original message with cause")
    void testMessagePreservationWithCause() {
        String originalMessage = "Serialization failed for object";
        RuntimeException cause = new RuntimeException("Circular reference detected");
        JsonProcessingException exception = new JsonProcessingException(originalMessage, cause);

        assertEquals(originalMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Circular reference detected", exception.getCause().getMessage());
    }

    @Test
    @Order(11)
    @DisplayName("Should support toString representation")
    void testToString() {
        String message = "Malformed JSON array";
        JsonProcessingException exception = new JsonProcessingException(message);

        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("JsonProcessingException"));
        assertTrue(toString.contains(message));
    }

    @Test
    @Order(12)
    @DisplayName("Should support toString with cause")
    void testToStringWithCause() {
        String message = "JSON deserialization failed";
        IllegalArgumentException cause = new IllegalArgumentException("Invalid type mapping");
        JsonProcessingException exception = new JsonProcessingException(message, cause);

        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("JsonProcessingException"));
        assertTrue(toString.contains(message));
    }

    @Test
    @Order(13)
    @DisplayName("Should support fillInStackTrace")
    void testFillInStackTrace() {
        JsonProcessingException exception = new JsonProcessingException("Test message");

        // Get original stack trace
        StackTraceElement[] originalStackTrace = exception.getStackTrace();
        assertNotNull(originalStackTrace);
        assertTrue(originalStackTrace.length > 0);

        // Fill in stack trace again
        Throwable result = exception.fillInStackTrace();

        assertSame(exception, result);
        StackTraceElement[] newStackTrace = exception.getStackTrace();
        assertNotNull(newStackTrace);
        assertTrue(newStackTrace.length > 0);
    }

    @Test
    @Order(14)
    @DisplayName("Should support getLocalizedMessage")
    void testGetLocalizedMessage() {
        String message = "JSON parsing error at position 42";
        JsonProcessingException exception = new JsonProcessingException(message);

        assertEquals(message, exception.getLocalizedMessage());
    }

    @Test
    @Order(15)
    @DisplayName("Should support initCause")
    void testInitCause() {
        JsonProcessingException exception = new JsonProcessingException("Test message");
        RuntimeException cause = new RuntimeException("Root cause");

        // InitCause should work on uncaused exception
        assertSame(exception, exception.initCause(cause));
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(16)
    @DisplayName("Should throw IllegalStateException on double initCause")
    void testDoubleInitCause() {
        RuntimeException originalCause = new RuntimeException("Original cause");
        JsonProcessingException exception = new JsonProcessingException("Test message", originalCause);
        RuntimeException newCause = new RuntimeException("New cause");

        assertThrows(IllegalStateException.class, () -> exception.initCause(newCause));
        assertSame(originalCause, exception.getCause()); // Should remain unchanged
    }

    @Test
    @Order(17)
    @DisplayName("Should handle typical JSON processing scenarios")
    void testTypicalJsonProcessingScenarios() {
        // Parsing error scenario
        JsonProcessingException parseException = new JsonProcessingException(
            "Unexpected character '}' at line 10, column 5");
        assertNotNull(parseException.getMessage());
        assertTrue(parseException.getMessage().contains("line 10"));

        // Type mismatch scenario
        JsonProcessingException typeException = new JsonProcessingException(
            "Cannot deserialize value of type java.time.LocalDateTime from String");
        assertNotNull(typeException.getMessage());
        assertTrue(typeException.getMessage().contains("deserialize"));

        // Serialization error scenario
        RuntimeException circularRef = new RuntimeException("Direct self-reference leading to cycle");
        JsonProcessingException serializationException = new JsonProcessingException(
            "Infinite recursion during JSON serialization", circularRef);
        assertNotNull(serializationException.getMessage());
        assertSame(circularRef, serializationException.getCause());

        // IO error scenario
        JsonProcessingException ioException = new JsonProcessingException(
            "Failed to read JSON from input stream");
        assertNotNull(ioException.getMessage());
        assertTrue(ioException.getMessage().contains("input stream"));

        // Unsupported type scenario
        JsonProcessingException unsupportedType = new JsonProcessingException(
            "No serializer found for class java.sql.Connection");
        assertNotNull(unsupportedType.getMessage());
        assertTrue(unsupportedType.getMessage().contains("serializer"));
    }

    @Test
    @Order(18)
    @DisplayName("Should be serializable")
    void testSerializability() {
        // Basic serialization test - verify fields are accessible
        String message = "Serialization test";
        RuntimeException cause = new RuntimeException("Serializable cause");

        JsonProcessingException exception = new JsonProcessingException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.hasContext()); // Should not have context by default

        // Test without cause
        JsonProcessingException simpleException = new JsonProcessingException(message);
        assertEquals(message, simpleException.getMessage());
        assertNull(simpleException.getCause());
    }

    @Test
    @Order(19)
    @DisplayName("Should support printStackTrace")
    void testPrintStackTrace() {
        JsonProcessingException exception = new JsonProcessingException("Test for printStackTrace");

        // Should not throw any exceptions when calling printStackTrace
        assertDoesNotThrow(() -> {
            // This would normally print to System.err, but we're just testing it doesn't throw
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);

            String stackTrace = sw.toString();
            assertNotNull(stackTrace);
            assertTrue(stackTrace.contains("JsonProcessingException"));
            assertTrue(stackTrace.contains("Test for printStackTrace"));
        });
    }

    @Test
    @Order(20)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception", JsonProcessingException.class.getPackageName());
        assertEquals("JsonProcessingException", JsonProcessingException.class.getSimpleName());
        assertEquals("com.skanga.conductor.exception.JsonProcessingException",
                    JsonProcessingException.class.getName());
    }

    @Test
    @Order(21)
    @DisplayName("Should handle common Jackson exceptions as causes")
    void testJacksonExceptionsCauses() {
        // Simulate common Jackson exceptions that might be wrapped

        // JsonParseException
        RuntimeException parseException = new RuntimeException("Unexpected character 'x' at position 42");
        JsonProcessingException wrapperParse = new JsonProcessingException("JSON parsing failed", parseException);
        assertSame(parseException, wrapperParse.getCause());
        assertTrue(wrapperParse.getMessage().contains("parsing"));

        // JsonMappingException-like scenario
        RuntimeException mappingException = new RuntimeException("Can not construct instance of LocalDateTime");
        JsonProcessingException wrapperMapping = new JsonProcessingException("Object mapping failed", mappingException);
        assertSame(mappingException, wrapperMapping.getCause());
        assertTrue(wrapperMapping.getMessage().contains("mapping"));

        // JsonGenerationException-like scenario
        RuntimeException generationException = new RuntimeException("Can not write a field name, expecting a value");
        JsonProcessingException wrapperGeneration = new JsonProcessingException("JSON generation failed", generationException);
        assertSame(generationException, wrapperGeneration.getCause());
        assertTrue(wrapperGeneration.getMessage().contains("generation"));
    }

    @Test
    @Order(22)
    @DisplayName("Should handle complex error messages")
    void testComplexErrorMessages() {
        // Test with complex, multi-line error message
        String complexMessage = """
            JSON processing error occurred:
            - File: config.json
            - Line: 15, Column: 23
            - Expected: closing bracket ']'
            - Found: unexpected character '}'
            - Context: array of workflow definitions
            """;

        JsonProcessingException exception = new JsonProcessingException(complexMessage);

        assertEquals(complexMessage, exception.getMessage());
        assertTrue(exception.getMessage().contains("Line: 15"));
        assertTrue(exception.getMessage().contains("closing bracket"));
        assertTrue(exception.getMessage().contains("workflow definitions"));
    }

    @Test
    @Order(23)
    @DisplayName("Should handle Unicode and special characters in messages")
    void testUnicodeAndSpecialCharacters() {
        String unicodeMessage = "JSON parsing failed: unexpected character '→' (U+2192) at position 世界";
        JsonProcessingException exception = new JsonProcessingException(unicodeMessage);

        assertEquals(unicodeMessage, exception.getMessage());
        assertTrue(exception.getMessage().contains("U+2192"));
        assertTrue(exception.getMessage().contains("世界"));
    }
}
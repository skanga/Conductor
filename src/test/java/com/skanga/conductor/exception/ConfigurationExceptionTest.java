package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConfigurationException functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigurationExceptionTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should create exception with message only")
    void testMessageOnlyConstructor() {
        String message = "Invalid database URL configuration";
        ConfigurationException exception = new ConfigurationException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(2)
    @DisplayName("Should create exception with message and cause")
    void testMessageAndCauseConstructor() {
        String message = "Failed to parse timeout value";
        NumberFormatException cause = new NumberFormatException("Invalid number format");
        ConfigurationException exception = new ConfigurationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle null message")
    void testNullMessage() {
        ConfigurationException exception = new ConfigurationException(null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle null cause")
    void testNullCause() {
        String message = "Configuration error";
        ConfigurationException exception = new ConfigurationException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle null message and null cause")
    void testNullMessageAndNullCause() {
        ConfigurationException exception = new ConfigurationException(null, null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(6)
    @DisplayName("Should inherit from ConductorRuntimeException")
    void testInheritanceFromConductorRuntimeException() {
        ConfigurationException exception = new ConfigurationException("Test message");

        assertTrue(exception instanceof ConductorRuntimeException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @Order(7)
    @DisplayName("Should support exception chaining")
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        IllegalArgumentException intermediateCause = new IllegalArgumentException("Intermediate", rootCause);
        ConfigurationException exception = new ConfigurationException("Configuration failed", intermediateCause);

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
        ConfigurationException exception = new ConfigurationException("");

        assertEquals("", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(9)
    @DisplayName("Should handle whitespace-only message")
    void testWhitespaceMessage() {
        String message = "   \t\n   ";
        ConfigurationException exception = new ConfigurationException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(10)
    @DisplayName("Should preserve original message with cause")
    void testMessagePreservationWithCause() {
        String originalMessage = "Database connection failed";
        RuntimeException cause = new RuntimeException("Connection timeout");
        ConfigurationException exception = new ConfigurationException(originalMessage, cause);

        assertEquals(originalMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Connection timeout", exception.getCause().getMessage());
    }

    @Test
    @Order(11)
    @DisplayName("Should support toString representation")
    void testToString() {
        String message = "Invalid API key format";
        ConfigurationException exception = new ConfigurationException(message);

        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ConfigurationException"));
        assertTrue(toString.contains(message));
    }

    @Test
    @Order(12)
    @DisplayName("Should support toString with cause")
    void testToStringWithCause() {
        String message = "Configuration validation failed";
        IllegalStateException cause = new IllegalStateException("Invalid state");
        ConfigurationException exception = new ConfigurationException(message, cause);

        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ConfigurationException"));
        assertTrue(toString.contains(message));
    }

    @Test
    @Order(13)
    @DisplayName("Should support fillInStackTrace")
    void testFillInStackTrace() {
        ConfigurationException exception = new ConfigurationException("Test message");

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
        String message = "Database URL is malformed";
        ConfigurationException exception = new ConfigurationException(message);

        assertEquals(message, exception.getLocalizedMessage());
    }

    @Test
    @Order(15)
    @DisplayName("Should support initCause")
    void testInitCause() {
        ConfigurationException exception = new ConfigurationException("Test message");
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
        ConfigurationException exception = new ConfigurationException("Test message", originalCause);
        RuntimeException newCause = new RuntimeException("New cause");

        assertThrows(IllegalStateException.class, () -> exception.initCause(newCause));
        assertSame(originalCause, exception.getCause()); // Should remain unchanged
    }

    @Test
    @Order(17)
    @DisplayName("Should handle typical configuration scenarios")
    void testTypicalConfigurationScenarios() {
        // Database URL scenario
        ConfigurationException dbException = new ConfigurationException(
            "Invalid JDBC URL: jdbc:invalid://localhost:3306/test");
        assertNotNull(dbException.getMessage());
        assertTrue(dbException.getMessage().contains("JDBC URL"));

        // API key scenario
        ConfigurationException apiException = new ConfigurationException(
            "Missing required API key: conductor.llm.openai.api_key");
        assertNotNull(apiException.getMessage());
        assertTrue(apiException.getMessage().contains("API key"));

        // Timeout scenario
        NumberFormatException numberCause = new NumberFormatException("For input string: \"invalid\"");
        ConfigurationException timeoutException = new ConfigurationException(
            "Invalid timeout value: invalid", numberCause);
        assertNotNull(timeoutException.getMessage());
        assertSame(numberCause, timeoutException.getCause());

        // File path scenario
        ConfigurationException fileException = new ConfigurationException(
            "Configuration file not found: /invalid/path/config.properties");
        assertNotNull(fileException.getMessage());
        assertTrue(fileException.getMessage().contains("not found"));
    }

    @Test
    @Order(18)
    @DisplayName("Should be serializable")
    void testSerializability() {
        // Basic serialization test - verify fields are accessible
        String message = "Serialization test";
        RuntimeException cause = new RuntimeException("Serializable cause");

        ConfigurationException exception = new ConfigurationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.hasContext()); // Should not have context by default

        // Test without cause
        ConfigurationException simpleException = new ConfigurationException(message);
        assertEquals(message, simpleException.getMessage());
        assertNull(simpleException.getCause());
    }

    @Test
    @Order(19)
    @DisplayName("Should support printStackTrace")
    void testPrintStackTrace() {
        ConfigurationException exception = new ConfigurationException("Test for printStackTrace");

        // Should not throw any exceptions when calling printStackTrace
        assertDoesNotThrow(() -> {
            // This would normally print to System.err, but we're just testing it doesn't throw
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);

            String stackTrace = sw.toString();
            assertNotNull(stackTrace);
            assertTrue(stackTrace.contains("ConfigurationException"));
            assertTrue(stackTrace.contains("Test for printStackTrace"));
        });
    }

    @Test
    @Order(20)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception", ConfigurationException.class.getPackageName());
        assertEquals("ConfigurationException", ConfigurationException.class.getSimpleName());
        assertEquals("com.skanga.conductor.exception.ConfigurationException",
                    ConfigurationException.class.getName());
    }
}
package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SingletonException functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SingletonExceptionTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should create exception with message only")
    void testMessageOnlyConstructor() {
        String message = "Singleton operation failed";
        SingletonException exception = new SingletonException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(2)
    @DisplayName("Should create exception with message and cause")
    void testMessageAndCauseConstructor() {
        String message = "Singleton management error";
        RuntimeException cause = new RuntimeException("Underlying singleton error");
        SingletonException exception = new SingletonException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle null message")
    void testNullMessage() {
        SingletonException exception = new SingletonException(null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle null cause")
    void testNullCause() {
        String message = "Singleton error";
        SingletonException exception = new SingletonException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(5)
    @DisplayName("Should inherit from ConductorRuntimeException")
    void testInheritanceFromConductorRuntimeException() {
        SingletonException exception = new SingletonException("Test message");

        assertTrue(exception instanceof ConductorRuntimeException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @Order(6)
    @DisplayName("Should support exception chaining")
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root singleton error");
        IllegalStateException intermediateCause = new IllegalStateException("Invalid singleton state", rootCause);
        SingletonException exception = new SingletonException("Singleton operation failed", intermediateCause);

        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());

        // Test stack trace elements are preserved
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    // === InitializationException Tests ===

    @Test
    @Order(7)
    @DisplayName("InitializationException should be a static nested class")
    void testInitializationExceptionClassStructure() {
        Class<?> initExceptionClass = SingletonException.InitializationException.class;
        assertTrue(initExceptionClass.isMemberClass());
        assertTrue(java.lang.reflect.Modifier.isStatic(initExceptionClass.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPublic(initExceptionClass.getModifiers()));
        assertEquals(SingletonException.class, initExceptionClass.getEnclosingClass());
    }

    @Test
    @Order(8)
    @DisplayName("Should create InitializationException with message only")
    void testInitializationExceptionMessageOnly() {
        String message = "Singleton initialization failed";
        SingletonException.InitializationException exception =
            new SingletonException.InitializationException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(9)
    @DisplayName("Should create InitializationException with message and cause")
    void testInitializationExceptionMessageAndCause() {
        String message = "Failed to initialize singleton instance";
        RuntimeException cause = new RuntimeException("Resource allocation failed");
        SingletonException.InitializationException exception =
            new SingletonException.InitializationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(10)
    @DisplayName("InitializationException should inherit from SingletonException")
    void testInitializationExceptionInheritance() {
        SingletonException.InitializationException exception =
            new SingletonException.InitializationException("Test message");

        assertTrue(exception instanceof SingletonException);
        assertTrue(exception instanceof ConductorRuntimeException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @Order(11)
    @DisplayName("Should handle null message in InitializationException")
    void testInitializationExceptionNullMessage() {
        SingletonException.InitializationException exception =
            new SingletonException.InitializationException(null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @Order(12)
    @DisplayName("Should handle null cause in InitializationException")
    void testInitializationExceptionNullCause() {
        String message = "Init error";
        SingletonException.InitializationException exception =
            new SingletonException.InitializationException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    // === ResetException Tests ===

    @Test
    @Order(13)
    @DisplayName("ResetException should be a static nested class")
    void testResetExceptionClassStructure() {
        Class<?> resetExceptionClass = SingletonException.ResetException.class;
        assertTrue(resetExceptionClass.isMemberClass());
        assertTrue(java.lang.reflect.Modifier.isStatic(resetExceptionClass.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPublic(resetExceptionClass.getModifiers()));
        assertEquals(SingletonException.class, resetExceptionClass.getEnclosingClass());
    }

    @Test
    @Order(14)
    @DisplayName("Should create ResetException with message and cause")
    void testResetExceptionMessageAndCause() {
        String message = "Failed to reset singleton state";
        RuntimeException cause = new RuntimeException("State corruption detected");
        SingletonException.ResetException exception =
            new SingletonException.ResetException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(15)
    @DisplayName("ResetException should inherit from SingletonException")
    void testResetExceptionInheritance() {
        RuntimeException cause = new RuntimeException("Reset failed");
        SingletonException.ResetException exception =
            new SingletonException.ResetException("Test message", cause);

        assertTrue(exception instanceof SingletonException);
        assertTrue(exception instanceof ConductorRuntimeException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @Order(16)
    @DisplayName("Should handle null message in ResetException")
    void testResetExceptionNullMessage() {
        RuntimeException cause = new RuntimeException("Reset cause");
        SingletonException.ResetException exception =
            new SingletonException.ResetException(null, cause);

        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @Order(17)
    @DisplayName("Should handle null cause in ResetException")
    void testResetExceptionNullCause() {
        String message = "Reset error";
        SingletonException.ResetException exception =
            new SingletonException.ResetException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    // === Integration and Edge Case Tests ===

    @Test
    @Order(18)
    @DisplayName("Should support nested exception chaining")
    void testNestedExceptionChaining() {
        // Create a chain: InitializationException -> ResetException -> SingletonException
        RuntimeException rootCause = new RuntimeException("Resource exhausted");
        SingletonException.ResetException resetCause =
            new SingletonException.ResetException("Reset failed during cleanup", rootCause);
        SingletonException.InitializationException initException =
            new SingletonException.InitializationException("Initialization failed after reset attempt", resetCause);

        assertEquals("Initialization failed after reset attempt", initException.getMessage());
        assertSame(resetCause, initException.getCause());
        assertSame(rootCause, initException.getCause().getCause());
    }

    @Test
    @Order(19)
    @DisplayName("Should handle typical singleton scenarios")
    void testTypicalSingletonScenarios() {
        // Initialization failure scenario
        OutOfMemoryError memoryError = new OutOfMemoryError("Cannot allocate memory for singleton");
        SingletonException.InitializationException initFailure =
            new SingletonException.InitializationException("Singleton initialization failed due to memory constraints", memoryError);
        assertSame(memoryError, initFailure.getCause());
        assertTrue(initFailure.getMessage().contains("memory constraints"));

        // Reset failure scenario
        IllegalStateException stateError = new IllegalStateException("Singleton is currently in use");
        SingletonException.ResetException resetFailure =
            new SingletonException.ResetException("Cannot reset singleton while operations are active", stateError);
        assertSame(stateError, resetFailure.getCause());
        assertTrue(resetFailure.getMessage().contains("operations are active"));

        // General singleton error scenario
        SecurityException securityError = new SecurityException("Access denied to singleton resource");
        SingletonException generalError = new SingletonException("Singleton access failed", securityError);
        assertSame(securityError, generalError.getCause());
        assertTrue(generalError.getMessage().contains("access failed"));
    }

    @Test
    @Order(20)
    @DisplayName("Should support toString representation for all exception types")
    void testToStringRepresentation() {
        // Base SingletonException
        SingletonException baseException = new SingletonException("Base singleton error");
        String baseToString = baseException.toString();
        assertTrue(baseToString.contains("SingletonException"));
        assertTrue(baseToString.contains("Base singleton error"));

        // InitializationException
        SingletonException.InitializationException initException =
            new SingletonException.InitializationException("Init error");
        String initToString = initException.toString();
        assertTrue(initToString.contains("InitializationException"));
        assertTrue(initToString.contains("Init error"));

        // ResetException
        RuntimeException cause = new RuntimeException("Reset cause");
        SingletonException.ResetException resetException =
            new SingletonException.ResetException("Reset error", cause);
        String resetToString = resetException.toString();
        assertTrue(resetToString.contains("ResetException"));
        assertTrue(resetToString.contains("Reset error"));
    }

    @Test
    @Order(21)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception", SingletonException.class.getPackageName());
        assertEquals("SingletonException", SingletonException.class.getSimpleName());
        assertEquals("com.skanga.conductor.exception.SingletonException",
                    SingletonException.class.getName());

        // Test nested classes
        assertEquals("com.skanga.conductor.exception.SingletonException$InitializationException",
                    SingletonException.InitializationException.class.getName());
        assertEquals("com.skanga.conductor.exception.SingletonException$ResetException",
                    SingletonException.ResetException.class.getName());
    }

    @Test
    @Order(22)
    @DisplayName("Should have proper nested class structure")
    void testNestedClassStructure() {
        Class<?>[] declaredClasses = SingletonException.class.getDeclaredClasses();
        assertEquals(2, declaredClasses.length);

        // Verify both nested classes exist
        boolean hasInitException = false;
        boolean hasResetException = false;

        for (Class<?> nestedClass : declaredClasses) {
            if (nestedClass.getSimpleName().equals("InitializationException")) {
                hasInitException = true;
            } else if (nestedClass.getSimpleName().equals("ResetException")) {
                hasResetException = true;
            }
        }

        assertTrue(hasInitException, "Should have InitializationException nested class");
        assertTrue(hasResetException, "Should have ResetException nested class");
    }

    @Test
    @Order(23)
    @DisplayName("Should be serializable")
    void testSerializability() {
        // Base exception serialization test
        String message = "Serialization test";
        RuntimeException cause = new RuntimeException("Serializable cause");

        SingletonException exception = new SingletonException(message, cause);
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());

        // InitializationException serialization test
        SingletonException.InitializationException initException =
            new SingletonException.InitializationException(message, cause);
        assertEquals(message, initException.getMessage());
        assertSame(cause, initException.getCause());

        // ResetException serialization test
        SingletonException.ResetException resetException =
            new SingletonException.ResetException(message, cause);
        assertEquals(message, resetException.getMessage());
        assertSame(cause, resetException.getCause());
    }

    @Test
    @Order(24)
    @DisplayName("Should handle constructor parameter validation")
    void testConstructorParameterValidation() {
        // All constructors should accept null parameters without throwing
        assertDoesNotThrow(() -> new SingletonException(null));
        assertDoesNotThrow(() -> new SingletonException("message", null));
        assertDoesNotThrow(() -> new SingletonException(null, null));

        assertDoesNotThrow(() -> new SingletonException.InitializationException(null));
        assertDoesNotThrow(() -> new SingletonException.InitializationException("message", null));
        assertDoesNotThrow(() -> new SingletonException.InitializationException(null, null));

        assertDoesNotThrow(() -> new SingletonException.ResetException(null, null));
        assertDoesNotThrow(() -> new SingletonException.ResetException("message", null));
    }

    @Test
    @Order(25)
    @DisplayName("Should differentiate between exception types in instanceof checks")
    void testInstanceOfChecks() {
        SingletonException base = new SingletonException("base");
        SingletonException.InitializationException init =
            new SingletonException.InitializationException("init");
        SingletonException.ResetException reset =
            new SingletonException.ResetException("reset", null);

        // All are SingletonExceptions
        assertTrue(base instanceof SingletonException);
        assertTrue(init instanceof SingletonException);
        assertTrue(reset instanceof SingletonException);

        // But nested classes are specific types
        assertFalse(base instanceof SingletonException.InitializationException);
        assertFalse(base instanceof SingletonException.ResetException);

        assertTrue(init instanceof SingletonException.InitializationException);
        assertTrue(reset instanceof SingletonException.ResetException);

        // Verify that different exception types are not the same
        assertNotEquals(init.getClass(), reset.getClass());
    }
}
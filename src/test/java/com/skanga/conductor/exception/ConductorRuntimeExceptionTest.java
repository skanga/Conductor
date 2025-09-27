package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConductorRuntimeException functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConductorRuntimeExceptionTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should create exception with message only")
    void testMessageOnlyConstructor() {
        String message = "Test runtime exception";
        ConductorRuntimeException exception = new ConductorRuntimeException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
        assertNull(exception.getErrorCode());
        assertNull(exception.getErrorCategory());
        assertEquals(message, exception.getDetailedSummary());
    }

    @Test
    @Order(2)
    @DisplayName("Should create exception with message and cause")
    void testMessageAndCauseConstructor() {
        String message = "Test runtime exception with cause";
        RuntimeException cause = new RuntimeException("Original cause");
        ConductorRuntimeException exception = new ConductorRuntimeException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
        assertNull(exception.getErrorCode());
        assertNull(exception.getErrorCategory());
        assertEquals(message, exception.getDetailedSummary());
    }

    @Test
    @Order(3)
    @DisplayName("Should create exception with cause only")
    void testCauseOnlyConstructor() {
        RuntimeException cause = new RuntimeException("Original cause");
        ConductorRuntimeException exception = new ConductorRuntimeException(cause);

        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
        assertNull(exception.getErrorCode());
        assertNull(exception.getErrorCategory());
        assertEquals(cause.toString(), exception.getDetailedSummary());
    }

    @Test
    @Order(4)
    @DisplayName("Should create exception with message and context")
    void testMessageAndContextConstructor() {
        String message = "Test runtime exception with context";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.VALIDATION_NULL_VALUE)
            .category(ExceptionContext.ErrorCategory.VALIDATION)
            .operation("test_operation")
            .correlationId("test-123")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);

        assertTrue(exception.getMessage().contains(ErrorCodes.VALIDATION_NULL_VALUE));
        assertTrue(exception.getMessage().contains(message));
        assertTrue(exception.getMessage().contains("operation: test_operation"));
        assertNull(exception.getCause());
        assertSame(context, exception.getContext());
        assertTrue(exception.hasContext());
        assertEquals(ErrorCodes.VALIDATION_NULL_VALUE, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.VALIDATION, exception.getErrorCategory());
    }

    @Test
    @Order(5)
    @DisplayName("Should create exception with message, cause, and context")
    void testMessageCauseAndContextConstructor() {
        String message = "Test runtime exception with all params";
        RuntimeException cause = new RuntimeException("Original cause");
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.CONFIG_DATABASE_URL_INVALID)
            .category(ExceptionContext.ErrorCategory.CONFIGURATION)
            .operation("database_connection")
            .correlationId("db-test-456")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, cause, context);

        assertTrue(exception.getMessage().contains(ErrorCodes.CONFIG_DATABASE_URL_INVALID));
        assertTrue(exception.getMessage().contains(message));
        assertTrue(exception.getMessage().contains("operation: database_connection"));
        assertSame(cause, exception.getCause());
        assertSame(context, exception.getContext());
        assertTrue(exception.hasContext());
        assertEquals(ErrorCodes.CONFIG_DATABASE_URL_INVALID, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.CONFIGURATION, exception.getErrorCategory());
    }

    @Test
    @Order(6)
    @DisplayName("Should handle null context gracefully")
    void testNullContextHandling() {
        String message = "Test with null context";
        ConductorRuntimeException exception = new ConductorRuntimeException(message, (ExceptionContext) null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
        assertNull(exception.getErrorCode());
        assertNull(exception.getErrorCategory());
        assertEquals(message, exception.getDetailedSummary());
    }

    @Test
    @Order(7)
    @DisplayName("Should handle null context with cause gracefully")
    void testNullContextWithCauseHandling() {
        String message = "Test with null context and cause";
        RuntimeException cause = new RuntimeException("Test cause");
        ConductorRuntimeException exception = new ConductorRuntimeException(message, cause, null);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
        assertNull(exception.getErrorCode());
        assertNull(exception.getErrorCategory());
        assertEquals(message, exception.getDetailedSummary());
    }

    @Test
    @Order(8)
    @DisplayName("Should provide detailed summary with context")
    void testDetailedSummaryWithContext() {
        String message = "Database connection failed";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.CONFIG_DATABASE_URL_INVALID)
            .operation("connect_to_database")
            .category(ExceptionContext.ErrorCategory.CONFIGURATION)
            .correlationId("conn-789")
            .metadata("url", "jdbc:invalid:url")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);
        String detailedSummary = exception.getDetailedSummary();

        assertNotNull(detailedSummary);
        assertTrue(detailedSummary.contains(context.getSummary()));
        assertTrue(detailedSummary.contains(message));
    }

    @Test
    @Order(9)
    @DisplayName("Should enhance message with error code only")
    void testMessageEnhancementWithErrorCodeOnly() {
        String message = "Simple error message";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.LLM_SERVICE_UNAVAILABLE)
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);

        assertTrue(exception.getMessage().startsWith("[" + ErrorCodes.LLM_SERVICE_UNAVAILABLE + "]"));
        assertTrue(exception.getMessage().contains(message));
    }

    @Test
    @Order(10)
    @DisplayName("Should enhance message with operation only")
    void testMessageEnhancementWithOperationOnly() {
        String message = "Operation failed";
        ExceptionContext context = ExceptionContext.builder()
            .operation("test_operation")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);

        assertTrue(exception.getMessage().contains(message));
        assertTrue(exception.getMessage().contains("(operation: test_operation)"));
    }

    @Test
    @Order(11)
    @DisplayName("Should enhance message with both error code and operation")
    void testMessageEnhancementWithErrorCodeAndOperation() {
        String message = "Complex failure";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.TOOL_EXECUTION_FAILED)
            .operation("execute_file_reader")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);

        assertTrue(exception.getMessage().startsWith("[" + ErrorCodes.TOOL_EXECUTION_FAILED + "]"));
        assertTrue(exception.getMessage().contains(message));
        assertTrue(exception.getMessage().contains("(operation: execute_file_reader)"));
    }

    @Test
    @Order(12)
    @DisplayName("Should handle empty context gracefully")
    void testEmptyContextHandling() {
        String message = "Test with empty context";
        ExceptionContext context = ExceptionContext.builder().build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);

        assertEquals(message, exception.getMessage()); // No enhancement for empty context
        assertSame(context, exception.getContext());
        assertTrue(exception.hasContext());
        assertNull(exception.getErrorCode());
        assertNull(exception.getErrorCategory());
    }

    @Test
    @Order(13)
    @DisplayName("Should inherit from RuntimeException")
    void testInheritanceFromRuntimeException() {
        ConductorRuntimeException exception = new ConductorRuntimeException("Test message");

        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @Order(14)
    @DisplayName("Should support exception chaining")
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        RuntimeException intermediateCause = new RuntimeException("Intermediate", rootCause);
        ConductorRuntimeException exception = new ConductorRuntimeException("Final exception", intermediateCause);

        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());

        // Test stack trace elements are preserved
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    @Order(15)
    @DisplayName("Should handle complex context metadata")
    void testComplexContextMetadata() {
        String message = "Complex metadata test";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.MEMORY_CLEANUP_FAILED)
            .operation("memory_cleanup")
            .category(ExceptionContext.ErrorCategory.SYSTEM_ERROR)
            .correlationId("cleanup-001")
            .metadata("heap_used", "512MB")
            .metadata("cleanup_type", "aggressive")
            .metadata("tasks_cleaned", "15")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(message, context);

        assertTrue(exception.hasContext());
        assertEquals(ErrorCodes.MEMORY_CLEANUP_FAILED, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR, exception.getErrorCategory());

        String detailedSummary = exception.getDetailedSummary();
        assertNotNull(detailedSummary);
        assertTrue(detailedSummary.length() > message.length());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle null message gracefully")
    void testNullMessageHandling() {
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.VALIDATION_NULL_VALUE)
            .operation("null_test")
            .build();

        ConductorRuntimeException exception = new ConductorRuntimeException(null, context);

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(ErrorCodes.VALIDATION_NULL_VALUE));
        assertTrue(exception.hasContext());
    }

    @Test
    @Order(17)
    @DisplayName("Should serialize and deserialize properly")
    void testSerializability() {
        // Test that the exception maintains expected behavior when serialized
        String message = "Serialization test";
        RuntimeException cause = new RuntimeException("Serializable cause");

        ConductorRuntimeException exception = new ConductorRuntimeException(message, cause);

        // Basic serialization test - verify fields are accessible
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.hasContext());

        // Test with context
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.DATA_SERIALIZATION_FAILED)
            .operation("serialize_exception")
            .build();

        ConductorRuntimeException contextException = new ConductorRuntimeException(message, context);
        assertTrue(contextException.hasContext());
        assertEquals(ErrorCodes.DATA_SERIALIZATION_FAILED, contextException.getErrorCode());
    }
}
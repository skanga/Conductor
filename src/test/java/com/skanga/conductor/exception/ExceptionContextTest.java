package com.skanga.conductor.exception;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExceptionContext functionality.
 */
public class ExceptionContextTest {

    @Test
    void testBasicContextCreation() {
        ExceptionContext context = ExceptionContext.of(
                ErrorCodes.RATE_LIMIT_EXCEEDED,
                ExceptionContext.ErrorCategory.RATE_LIMIT
        );

        assertEquals(ErrorCodes.RATE_LIMIT_EXCEEDED, context.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT, context.getCategory());
        assertNotNull(context.getTimestamp());
    }

    @Test
    void testFullContextBuilder() {
        Instant timestamp = Instant.now();
        ExceptionContext context = ExceptionContext.builder()
                .errorCode(ErrorCodes.EXECUTION_FAILED)
                .category(ExceptionContext.ErrorCategory.BUSINESS_LOGIC)
                .operation("file_read")
                .timestamp(timestamp)
                .correlationId("test-123")
                .recoveryHint(ExceptionContext.RecoveryHint.USE_FALLBACK)
                .recoveryDetails("Try backup file reader")
                .duration(5000L)
                .attempt(2, 3)
                .metadata("tool.name", "file_reader")
                .metadata("tool.type", "io")
                .build();

        assertEquals(ErrorCodes.EXECUTION_FAILED, context.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC, context.getCategory());
        assertEquals("file_read", context.getOperation());
        assertEquals(timestamp, context.getTimestamp());
        assertEquals("test-123", context.getCorrelationId());
        assertEquals(ExceptionContext.RecoveryHint.USE_FALLBACK, context.getRecoveryHint());
        assertEquals("Try backup file reader", context.getRecoveryDetails());
        assertEquals(5000L, context.getDuration());
        assertEquals(2, context.getAttemptNumber());
        assertEquals(3, context.getMaxAttempts());
        assertEquals("file_reader", context.getMetadata("tool.name"));
        assertEquals("io", context.getMetadata("tool.type"));
    }

    @Test
    void testRetryableDetection() {
        // Retryable context
        ExceptionContext retryableContext = ExceptionContext.builder()
                .errorCode(ErrorCodes.TIMEOUT)
                .recoveryHint(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF)
                .build();

        assertTrue(retryableContext.isRetryable());

        // Non-retryable context
        ExceptionContext nonRetryableContext = ExceptionContext.builder()
                .errorCode(ErrorCodes.CONFIGURATION_ERROR)
                .recoveryHint(ExceptionContext.RecoveryHint.FIX_CONFIGURATION)
                .build();

        assertFalse(nonRetryableContext.isRetryable());
    }

    @Test
    void testFallbackSuggestion() {
        ExceptionContext context = ExceptionContext.builder()
                .errorCode(ErrorCodes.NOT_FOUND)
                .recoveryHint(ExceptionContext.RecoveryHint.USE_FALLBACK)
                .build();

        assertTrue(context.suggestsFallback());
    }

    @Test
    void testMetadataTypedAccess() {
        ExceptionContext context = ExceptionContext.builder()
                .metadata("string_value", "test")
                .metadata("int_value", 42)
                .metadata("boolean_value", true)
                .build();

        assertEquals("test", context.getMetadata("string_value", String.class));
        assertEquals(Integer.valueOf(42), context.getMetadata("int_value", Integer.class));
        assertEquals(Boolean.TRUE, context.getMetadata("boolean_value", Boolean.class));

        // Wrong type should return null
        assertNull(context.getMetadata("string_value", Integer.class));
        assertNull(context.getMetadata("nonexistent", String.class));
    }

    @Test
    void testSummaryGeneration() {
        ExceptionContext context = ExceptionContext.builder()
                .errorCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
                .category(ExceptionContext.ErrorCategory.RATE_LIMIT)
                .operation("generate_completion")
                .duration(2500L)
                .attempt(3, 5)
                .build();

        String summary = context.getSummary();

        assertTrue(summary.contains(ErrorCodes.RATE_LIMIT_EXCEEDED));
        assertTrue(summary.contains("RATE_LIMIT"));
        assertTrue(summary.contains("generate_completion"));
        assertTrue(summary.contains("2500ms"));
        assertTrue(summary.contains("3/5"));
    }

    @Test
    void testErrorCodeCategoryMapping() {
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT,
                ErrorCodes.toExceptionContextCategory(ErrorCodes.RATE_LIMIT_EXCEEDED));
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT,
                ErrorCodes.toExceptionContextCategory(ErrorCodes.TIMEOUT));
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION,
                ErrorCodes.toExceptionContextCategory(ErrorCodes.AUTH_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.CONFIGURATION,
                ErrorCodes.toExceptionContextCategory(ErrorCodes.CONFIGURATION_ERROR));
        // TIMEOUT maps to TIMEOUT category
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT,
                ErrorCodes.toExceptionContextCategory(ErrorCodes.TIMEOUT));
    }
}
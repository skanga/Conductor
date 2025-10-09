package com.skanga.conductor.exception;

import com.skanga.conductor.exception.ApprovalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExceptionBuilder functionality.
 */
public class ExceptionBuilderTest {

    @Test
    void testLLMProviderExceptionBuilder() throws Exception {
        ConductorException.LLMProviderException exception =
                (ConductorException.LLMProviderException) ExceptionBuilder.llmProvider("OpenAI request failed")
                        .errorCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
                        .operation("generate_completion")
                        .provider("openai", "gpt-4")
                        .httpStatus(429)
                        .rateLimitReset(3600)
                        .duration(5000L)
                        .attempt(2, 3)
                        .retryWithBackoff()
                        .build();

        assertTrue(exception.getMessage().contains("RATE_LIMIT_EXCEEDED"));
        assertTrue(exception.getMessage().contains("generate_completion"));
        assertTrue(exception.getMessage().contains("2/3"));

        assertEquals(ErrorCodes.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT, exception.getErrorCategory());
        assertTrue(exception.isRetryable());

        assertEquals("openai", exception.getProviderName());
        assertEquals("gpt-4", exception.getModelName());
        assertEquals(Integer.valueOf(429), exception.getHttpStatusCode());
        assertEquals(Long.valueOf(3600), exception.getRateLimitResetTime());
    }

    @Test
    void testToolExecutionExceptionBuilder() throws Exception {
        ConductorException.ToolExecutionException exception =
                (ConductorException.ToolExecutionException) ExceptionBuilder.toolExecution("File read failed")
                        .errorCode(ErrorCodes.EXECUTION_FAILED)
                        .operation("read_file")
                        .tool("file_reader", "io")
                        .fallbackTool("backup_reader")
                        .validationError("File not found: test.txt")
                        .useFallback()
                        .build();

        assertTrue(exception.getMessage().contains("EXECUTION_FAILED"));
        assertTrue(exception.getMessage().contains("read_file"));

        assertEquals(ErrorCodes.EXECUTION_FAILED, exception.getErrorCode());
        assertTrue(exception.suggestsFallback());

        assertEquals("file_reader", exception.getToolName());
        assertEquals("io", exception.getToolType());
        assertEquals("backup_reader", exception.getFallbackTool());
        assertEquals("File not found: test.txt", exception.getValidationError());
    }

    @Test
    void testApprovalExceptionBuilder() throws Exception {
        ApprovalException exception =
                (ApprovalException) ExceptionBuilder.approval("User approval timeout")
                        .errorCode(ErrorCodes.TIMEOUT)
                        .operation("workflow_approval")
                        .approvalRequest("req-123", "console")
                        .approvalTimeout(30000)
                        .userId("user-456")
                        .escalationContact("admin@company.com")
                        .userActionRequired()
                        .build();

        assertTrue(exception.getMessage().contains("TIMEOUT"));
        assertTrue(exception.getMessage().contains("workflow_approval"));

        assertEquals(ErrorCodes.TIMEOUT, exception.getErrorCode());
        // TIMEOUT maps to TIMEOUT category in simplified structure
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT, exception.getErrorCategory());

        assertEquals("req-123", exception.getApprovalRequestId());
        assertEquals("console", exception.getHandlerType());
        assertEquals(Long.valueOf(30000), exception.getTimeoutMs());
        assertEquals("user-456", exception.getUserId());
        assertEquals("admin@company.com", exception.getEscalationContact());
    }

    @Test
    void testConfigurationExceptionBuilder() throws Exception {
        com.skanga.conductor.exception.ConfigurationException exception =
                (com.skanga.conductor.exception.ConfigurationException) ExceptionBuilder.configuration("Invalid database URL")
                        .errorCode(ErrorCodes.CONFIGURATION_ERROR)
                        .operation("database_initialization")
                        .metadata("url", "invalid://url")
                        .metadata("expected_format", "jdbc:h2:mem:testdb")
                        .fixConfiguration()
                        .build();

        // ConfigurationException doesn't support enhanced context yet
        assertTrue(exception.getMessage().contains("Invalid database URL"));
    }

    @Test
    void testValidationExceptionBuilder() throws Exception {
        ConductorRuntimeException exception =
                (ConductorRuntimeException) ExceptionBuilder.validation("Null parameter")
                        .errorCode(ErrorCodes.INVALID_INPUT)
                        .operation("parameter_validation")
                        .metadata("parameter_name", "inputData")
                        .metadata("expected_type", "String")
                        .build();

        // INVALID_INPUT maps to VALIDATION category
        assertTrue(exception.getMessage().contains("INVALID_INPUT"));
        assertTrue(exception.getMessage().contains("parameter_validation"));

        // Error code returned is INVALID_INPUT
        assertEquals(ErrorCodes.INVALID_INPUT, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.VALIDATION, exception.getErrorCategory());
    }

    @Test
    void testExceptionWithCause() throws Exception {
        RuntimeException originalCause = new RuntimeException("Original error");

        ConductorException.LLMProviderException exception =
                (ConductorException.LLMProviderException) ExceptionBuilder.llmProvider("LLM service failed")
                        .errorCode(ErrorCodes.SERVICE_UNAVAILABLE)
                        .cause(originalCause)
                        .retryWithBackoff()
                        .build();

        assertEquals(originalCause, exception.getCause());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testContextAccessMethods() throws Exception {
        ConductorException.LLMProviderException exception =
                (ConductorException.LLMProviderException) ExceptionBuilder.llmProvider("Test message")
                        .errorCode(ErrorCodes.TIMEOUT)
                        .operation("test_operation")
                        .correlationId("corr-123")
                        .duration(2500L)
                        .build();

        assertTrue(exception.hasContext());
        assertNotNull(exception.getContext());

        String detailedSummary = exception.getDetailedSummary();
        // TIMEOUT error code
        assertTrue(detailedSummary.contains("TIMEOUT"));
        assertTrue(detailedSummary.contains("test_operation"));
        assertTrue(detailedSummary.contains("2500ms"));
        assertTrue(detailedSummary.contains("Test message"));
    }

    @Test
    void testAutoErrorCodeMapping() throws Exception {
        // Test that error code automatically sets category and recovery hint
        ConductorException.LLMProviderException exception =
                (ConductorException.LLMProviderException) ExceptionBuilder.llmProvider("Rate limit hit")
                        .errorCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
                        .build();

        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT, exception.getErrorCategory());
        // Recovery hint is not automatically set by errorCode() - it's set by explicit methods like retryWithBackoff()
        // Check that the recovery hint can be explicitly set
        ConductorException.LLMProviderException exceptionWithHint =
                (ConductorException.LLMProviderException) ExceptionBuilder.llmProvider("Rate limit hit")
                        .errorCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
                        .retryWithBackoff()
                        .build();
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF, exceptionWithHint.getContext().getRecoveryHint());
    }
}
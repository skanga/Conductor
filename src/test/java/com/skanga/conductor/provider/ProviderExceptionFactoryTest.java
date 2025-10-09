package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException.LLMProviderException;
import com.skanga.conductor.exception.ErrorCodes;
import com.skanga.conductor.exception.ExceptionContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ProviderExceptionFactory functionality.
 */
public class ProviderExceptionFactoryTest {

    private ProviderExceptionFactory.ProviderContext createTestContext() {
        return ProviderExceptionFactory.ProviderContext.builder("test-provider")
                .model("test-model")
                .operation("test-operation")
                .duration(1500L)
                .correlationId("test-correlation-123")
                .build();
    }

    @Test
    void testProviderContextBuilder() {
        ProviderExceptionFactory.ProviderContext context = ProviderExceptionFactory.ProviderContext
                .builder("openai")
                .model("gpt-4")
                .operation("generate_completion")
                .duration(2500L)
                .attempt(2, 3)
                .correlationId("req-456")
                .build();

        assertEquals("openai", context.getProviderName());
        assertEquals("gpt-4", context.getModelName());
        assertEquals("generate_completion", context.getOperation());
        assertEquals(Long.valueOf(2500), context.getDuration());
        assertEquals(Integer.valueOf(2), context.getAttemptNumber());
        assertEquals(Integer.valueOf(3), context.getMaxAttempts());
        assertEquals("req-456", context.getCorrelationId());
    }

    @Test
    void testInvalidApiKeyException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.invalidApiKey(context, "Invalid format");

        assertEquals(ErrorCodes.AUTH_FAILED, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION, exception.getErrorCategory());
        assertEquals("test-provider", exception.getProviderName());
        assertEquals("test-model", exception.getModelName());
        assertEquals("Invalid format", exception.getContext().getMetadata("key_info"));
        assertFalse(exception.isRetryable()); // Auth errors are not retryable
    }

    @Test
    void testExpiredApiKeyException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.expiredApiKey(context, "Expired on 2024-01-01");

        assertEquals(ErrorCodes.AUTH_FAILED, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION, exception.getErrorCategory());
        assertEquals("Expired on 2024-01-01", exception.getContext().getMetadata("expiration_info"));
    }

    @Test
    void testMissingApiKeyException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.missingApiKey(context);

        assertEquals(ErrorCodes.AUTH_FAILED, exception.getErrorCode());
        assertEquals("TEST-PROVIDER_API_KEY", exception.getContext().getMetadata("required_env_var"));
    }

    @Test
    void testRateLimitExceededException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.rateLimitExceeded(context, 120L);

        assertEquals(ErrorCodes.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT, exception.getErrorCategory());
        assertTrue(exception.isRetryable());
        assertEquals(Long.valueOf(120), exception.getRateLimitResetTime());
        assertEquals(Long.valueOf(120), exception.getContext().getMetadata("retry_after_seconds"));
        assertTrue(exception.getContext().getRecoveryDetails().contains("120 seconds"));
    }

    @Test
    void testQuotaExceededException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.quotaExceeded(context, "monthly_tokens");

        assertEquals(ErrorCodes.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        assertEquals("monthly_tokens", exception.getContext().getMetadata("quota_type"));
        assertFalse(exception.isRetryable()); // Quota exceeded requires user action
    }

    @Test
    void testRequestTimeoutException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.requestTimeout(context, 30000L);

        assertEquals(ErrorCodes.TIMEOUT, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT, exception.getErrorCategory());
        assertTrue(exception.isRetryable());
        assertEquals(Long.valueOf(30000), exception.getContext().getMetadata("timeout_ms"));
        assertTrue(exception.getContext().getRecoveryDetails().contains("timeout"));
    }

    @Test
    void testConnectionTimeoutException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.connectionTimeout(context, "https://api.openai.com");

        assertEquals(ErrorCodes.TIMEOUT, exception.getErrorCode());
        assertEquals("https://api.openai.com", exception.getContext().getMetadata("endpoint"));
        assertTrue(exception.isRetryable());
    }

    @Test
    void testNetworkConnectionFailedException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        RuntimeException cause = new RuntimeException("Connection refused");
        LLMProviderException exception = ProviderExceptionFactory.networkConnectionFailed(context, cause);

        assertEquals(ErrorCodes.NETWORK_ERROR, exception.getErrorCode());
        assertEquals(ExceptionContext.ErrorCategory.NETWORK, exception.getErrorCategory());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testServiceUnavailableException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.serviceUnavailable(context, 503);

        assertEquals(ErrorCodes.SERVICE_UNAVAILABLE, exception.getErrorCode());
        assertEquals(Integer.valueOf(503), exception.getHttpStatusCode());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testInvalidRequestFormatException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.invalidRequestFormat(context, "Missing required field 'model'");

        assertEquals(ErrorCodes.INVALID_FORMAT, exception.getErrorCode());
        assertEquals("Missing required field 'model'", exception.getContext().getMetadata("validation_error"));
        assertFalse(exception.isRetryable()); // Format errors are not retryable
    }

    @Test
    void testRequestTooLargeException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.requestTooLarge(context, 8192, 4096);

        assertEquals(ErrorCodes.SIZE_EXCEEDED, exception.getErrorCode());
        assertEquals(Integer.valueOf(8192), exception.getContext().getMetadata("actual_size"));
        assertEquals(Integer.valueOf(4096), exception.getContext().getMetadata("max_size"));
        assertTrue(exception.getContext().getRecoveryDetails().contains("Reduce prompt size"));
    }

    @Test
    void testInvalidModelException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.invalidModel(context, "gpt-3.5-turbo, gpt-4");

        assertEquals(ErrorCodes.NOT_FOUND, exception.getErrorCode());
        assertEquals("gpt-3.5-turbo, gpt-4", exception.getContext().getMetadata("available_models"));
        assertTrue(exception.getContext().getRecoveryDetails().contains("available models"));
    }

    @Test
    void testFromExceptionWithTimeoutException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        TimeoutException timeoutException = new TimeoutException("Request timed out");
        LLMProviderException exception = ProviderExceptionFactory.fromException(context, timeoutException);

        assertEquals(ErrorCodes.TIMEOUT, exception.getErrorCode());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testFromExceptionWithConnectionException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        RuntimeException connectionException = new RuntimeException("Connection refused");
        LLMProviderException exception = ProviderExceptionFactory.fromException(context, connectionException);

        assertEquals(ErrorCodes.NETWORK_ERROR, exception.getErrorCode());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testFromExceptionWithRateLimitException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        RuntimeException rateLimitException = new RuntimeException("Rate limit exceeded");
        LLMProviderException exception = ProviderExceptionFactory.fromException(context, rateLimitException);

        assertEquals(ErrorCodes.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testFromExceptionWithAuthenticationException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        RuntimeException authException = new RuntimeException("Authentication failed - 401 Unauthorized");
        LLMProviderException exception = ProviderExceptionFactory.fromException(context, authException);

        assertEquals(ErrorCodes.AUTH_FAILED, exception.getErrorCode());
        assertFalse(exception.isRetryable());
    }

    @Test
    void testFromExceptionWithGenericException() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        RuntimeException genericException = new RuntimeException("Unknown error");
        LLMProviderException exception = ProviderExceptionFactory.fromException(context, genericException);

        assertEquals(ErrorCodes.SERVICE_UNAVAILABLE, exception.getErrorCode());
        assertEquals(genericException, exception.getCause());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testExceptionMessageEnhancement() {
        ProviderExceptionFactory.ProviderContext context = ProviderExceptionFactory.ProviderContext
                .builder("openai")
                .model("gpt-4")
                .operation("generate_completion")
                .duration(2500L)
                .attempt(2, 3)
                .correlationId("req-456")
                .build();

        LLMProviderException exception = ProviderExceptionFactory.rateLimitExceeded(context, 60);

        String message = exception.getMessage();
        assertTrue(message.contains("RATE_LIMIT_EXCEEDED"));
        assertTrue(message.contains("generate_completion"));
        assertTrue(message.contains("2/3")); // attempt information
    }

    @Test
    void testContextPreservation() {
        ProviderExceptionFactory.ProviderContext context = createTestContext();
        LLMProviderException exception = ProviderExceptionFactory.invalidApiKey(context, "test");

        assertTrue(exception.hasContext());
        assertEquals("test-operation", exception.getContext().getOperation());
        assertEquals(Long.valueOf(1500), exception.getContext().getDuration());
        assertEquals("test-correlation-123", exception.getContext().getCorrelationId());
    }
}
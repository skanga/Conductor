package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException.LLMProviderException;
import com.skanga.conductor.exception.ErrorCodes;
import com.skanga.conductor.exception.ExceptionContext;

/**
 * Simple test for ProviderExceptionFactory functionality.
 * This is a standalone test to verify the basic functionality works.
 */
public class SimpleProviderExceptionTest {

    public static void main(String[] args) {
        try {
            testBasicFunctionality();
            testWithAttemptInfo();
            testFromException();
            System.out.println("All tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testBasicFunctionality() {
        // Test basic context creation
        ProviderExceptionFactory.ProviderContext context = ProviderExceptionFactory.ProviderContext
                .builder("test-provider")
                .model("test-model")
                .operation("test-operation")
                .duration(1500L)
                .correlationId("test-correlation")
                .build();

        // Test invalid API key exception
        LLMProviderException exception = ProviderExceptionFactory.invalidApiKey(context, "Invalid format");

        assert exception.getErrorCode().equals(ErrorCodes.AUTH_FAILED);
        assert exception.getProviderName().equals("test-provider");
        assert exception.getModelName().equals("test-model");
        assert exception.getContext().getOperation().equals("test-operation");
        assert exception.getContext().getDuration().equals(1500L);
        assert exception.getContext().getCorrelationId().equals("test-correlation");
        assert exception.getContext().getMetadata("key_info").equals("Invalid format");

        System.out.println("✓ Basic functionality test passed");
    }

    private static void testWithAttemptInfo() {
        // Test context with attempt information
        ProviderExceptionFactory.ProviderContext context = ProviderExceptionFactory.ProviderContext
                .builder("openai")
                .model("gpt-4")
                .operation("generate_completion")
                .duration(2500L)
                .attempt(2, 3)
                .correlationId("req-456")
                .build();

        // Test rate limit exception
        LLMProviderException exception = ProviderExceptionFactory.rateLimitExceeded(context, 120L);

        assert exception.getErrorCode().equals(ErrorCodes.RATE_LIMIT_EXCEEDED);
        assert exception.getErrorCategory().equals(ExceptionContext.ErrorCategory.RATE_LIMIT);
        assert exception.isRetryable();
        assert exception.getRateLimitResetTime().equals(120L);
        assert exception.getContext().getAttemptNumber().equals(2);
        assert exception.getContext().getMaxAttempts().equals(3);

        System.out.println("✓ Attempt info test passed");
    }

    private static void testFromException() {
        // Test automatic exception classification
        ProviderExceptionFactory.ProviderContext context = ProviderExceptionFactory.ProviderContext
                .builder("anthropic")
                .model("claude-3")
                .operation("generate_text")
                .duration(1000L)
                .build();

        // Test timeout exception classification
        Exception timeoutException = new RuntimeException("Request timeout after 30 seconds");
        LLMProviderException exception = ProviderExceptionFactory.fromException(context, timeoutException);

        assert exception.getErrorCode().equals(ErrorCodes.TIMEOUT);
        assert exception.isRetryable();
        assert exception.getCause() == timeoutException;

        System.out.println("✓ Automatic exception classification test passed");
    }
}
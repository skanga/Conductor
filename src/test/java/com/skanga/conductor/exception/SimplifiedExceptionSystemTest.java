package com.skanga.conductor.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the simplified exception system.
 */
class SimplifiedExceptionSystemTest {

    @Nested
    @DisplayName("ErrorCategory Tests")
    class ErrorCategoryTests {

        @Test
        @DisplayName("Should identify retryable categories correctly")
        void shouldIdentifyRetryableCategories() {
            assertTrue(ErrorCategory.RATE_LIMITED.isDefaultRetryable());
            assertTrue(ErrorCategory.TIMEOUT.isDefaultRetryable());
            assertTrue(ErrorCategory.SERVICE_ERROR.isDefaultRetryable());

            assertFalse(ErrorCategory.AUTH_ERROR.isDefaultRetryable());
            assertFalse(ErrorCategory.INVALID_INPUT.isDefaultRetryable());
            assertFalse(ErrorCategory.NOT_FOUND.isDefaultRetryable());
            assertFalse(ErrorCategory.CONFIGURATION_ERROR.isDefaultRetryable());
        }

        @Test
        @DisplayName("Should categorize from message - auth errors")
        void shouldCategorizeAuthErrors() {
            assertEquals(ErrorCategory.AUTH_ERROR,
                ErrorCategory.fromMessage("Invalid API key"));
            assertEquals(ErrorCategory.AUTH_ERROR,
                ErrorCategory.fromMessage("Authentication failed"));
            assertEquals(ErrorCategory.AUTH_ERROR,
                ErrorCategory.fromMessage("Unauthorized access"));
            assertEquals(ErrorCategory.AUTH_ERROR,
                ErrorCategory.fromMessage("Insufficient permissions"));
        }

        @Test
        @DisplayName("Should categorize from message - rate limiting")
        void shouldCategorizeRateLimiting() {
            assertEquals(ErrorCategory.RATE_LIMITED,
                ErrorCategory.fromMessage("Rate limit exceeded"));
            assertEquals(ErrorCategory.RATE_LIMITED,
                ErrorCategory.fromMessage("Too many requests"));
            assertEquals(ErrorCategory.RATE_LIMITED,
                ErrorCategory.fromMessage("Quota exhausted"));
            assertEquals(ErrorCategory.RATE_LIMITED,
                ErrorCategory.fromMessage("Request throttled"));
        }

        @Test
        @DisplayName("Should categorize from message - timeouts")
        void shouldCategorizeTimeouts() {
            assertEquals(ErrorCategory.TIMEOUT,
                ErrorCategory.fromMessage("Connection timeout"));
            assertEquals(ErrorCategory.TIMEOUT,
                ErrorCategory.fromMessage("Request timed out"));
            assertEquals(ErrorCategory.TIMEOUT,
                ErrorCategory.fromMessage("Read timeout after 30 seconds"));
        }

        @Test
        @DisplayName("Should categorize from message - service errors")
        void shouldCategorizeServiceErrors() {
            assertEquals(ErrorCategory.SERVICE_ERROR,
                ErrorCategory.fromMessage("Service unavailable"));
            assertEquals(ErrorCategory.SERVICE_ERROR,
                ErrorCategory.fromMessage("HTTP 503 Server Error"));
            assertEquals(ErrorCategory.SERVICE_ERROR,
                ErrorCategory.fromMessage("Server maintenance mode"));
            assertEquals(ErrorCategory.SERVICE_ERROR,
                ErrorCategory.fromMessage("System overloaded"));
        }

        @Test
        @DisplayName("Should categorize from exception type")
        void shouldCategorizeFromExceptionType() {
            Exception timeoutEx = new java.util.concurrent.TimeoutException("Timeout");
            assertEquals(ErrorCategory.TIMEOUT, ErrorCategory.fromException(timeoutEx));

            Exception authEx = new SecurityException("Auth failed");
            assertEquals(ErrorCategory.AUTH_ERROR, ErrorCategory.fromException(authEx));
        }
    }

    @Nested
    @DisplayName("SimpleConductorException Tests")
    class SimpleConductorExceptionTests {

        @Test
        @DisplayName("Should create exception with category and message")
        void shouldCreateWithCategoryAndMessage() {
            SimpleConductorException ex = new SimpleConductorException(
                ErrorCategory.TIMEOUT,
                "Request timed out"
            );

            assertEquals(ErrorCategory.TIMEOUT, ex.getCategory());
            assertTrue(ex.getMessage().contains("TIMEOUT"));
            assertTrue(ex.getMessage().contains("Request timed out"));
            assertTrue(ex.isRetryable());  // TIMEOUT is retryable
        }

        @Test
        @DisplayName("Should create exception with cause")
        void shouldCreateWithCause() {
            Exception cause = new RuntimeException("Original error");
            SimpleConductorException ex = new SimpleConductorException(
                ErrorCategory.SERVICE_ERROR,
                "Service failed",
                cause
            );

            assertEquals(ErrorCategory.SERVICE_ERROR, ex.getCategory());
            assertEquals(cause, ex.getCause());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("Should create exception with context")
        void shouldCreateWithContext() {
            SimpleConductorException ex = new SimpleConductorException(
                ErrorCategory.INVALID_INPUT,
                "Invalid parameter",
                null,
                "param=foo, value=bar"
            );

            assertEquals(ErrorCategory.INVALID_INPUT, ex.getCategory());
            assertEquals("param=foo, value=bar", ex.getContext());
            assertTrue(ex.getMessage().contains("param=foo"));
            assertFalse(ex.isRetryable());  // INVALID_INPUT not retryable
        }

        @Test
        @DisplayName("Should override default retryability")
        void shouldOverrideRetryability() {
            // Make normally non-retryable error retryable
            SimpleConductorException ex = new SimpleConductorException(
                ErrorCategory.AUTH_ERROR,
                "Temporary auth issue",
                null,
                null,
                true  // Override to retryable
            );

            assertEquals(ErrorCategory.AUTH_ERROR, ex.getCategory());
            assertFalse(ex.getCategory().isDefaultRetryable());
            assertTrue(ex.isRetryable());  // Overridden
        }

        @Test
        @DisplayName("Should create from exception with auto-categorization")
        void shouldCreateFromException() {
            Exception original = new RuntimeException("Rate limit exceeded");
            SimpleConductorException ex = SimpleConductorException.from(
                "Request failed",
                original
            );

            assertEquals(ErrorCategory.RATE_LIMITED, ex.getCategory());
            assertEquals(original, ex.getCause());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("Should create from exception preserving message")
        void shouldCreateFromExceptionPreservingMessage() {
            Exception original = new RuntimeException("Connection timeout");
            SimpleConductorException ex = SimpleConductorException.from(original);

            assertEquals(ErrorCategory.TIMEOUT, ex.getCategory());
            assertTrue(ex.getMessage().contains("Connection timeout"));
            assertEquals(original, ex.getCause());
        }
    }

    @Nested
    @DisplayName("ProviderException Tests")
    class ProviderExceptionTests {

        @Test
        @DisplayName("Should create provider exception with provider name")
        void shouldCreateWithProviderName() {
            ProviderException ex = new ProviderException(
                ErrorCategory.RATE_LIMITED,
                "Rate limit exceeded",
                "openai"
            );

            assertEquals(ErrorCategory.RATE_LIMITED, ex.getCategory());
            assertEquals("openai", ex.getProviderName());
            assertTrue(ex.getMessage().contains("provider=openai"));
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("Should create provider exception with model name")
        void shouldCreateWithModelName() {
            ProviderException ex = new ProviderException(
                ErrorCategory.AUTH_ERROR,
                "Invalid API key",
                null,
                "anthropic",
                "claude-3"
            );

            assertEquals("anthropic", ex.getProviderName());
            assertEquals("claude-3", ex.getModelName());
            assertTrue(ex.getMessage().contains("provider=anthropic"));
            assertTrue(ex.getMessage().contains("model=claude-3"));
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("Should create from exception")
        void shouldCreateFromException() {
            Exception cause = new RuntimeException("Service unavailable");
            ProviderException ex = ProviderException.from(
                "OpenAI request failed",
                cause,
                "openai"
            );

            assertEquals(ErrorCategory.SERVICE_ERROR, ex.getCategory());
            assertEquals("openai", ex.getProviderName());
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("ToolException Tests")
    class ToolExceptionTests {

        @Test
        @DisplayName("Should create tool exception with tool name")
        void shouldCreateWithToolName() {
            ToolException ex = new ToolException(
                ErrorCategory.NOT_FOUND,
                "Tool not found in registry",
                "web-search"
            );

            assertEquals(ErrorCategory.NOT_FOUND, ex.getCategory());
            assertEquals("web-search", ex.getToolName());
            assertTrue(ex.getMessage().contains("tool=web-search"));
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("Should create tool exception with cause")
        void shouldCreateWithCause() {
            Exception cause = new IllegalArgumentException("Invalid parameter");
            ToolException ex = new ToolException(
                ErrorCategory.INVALID_INPUT,
                "Tool execution failed",
                cause,
                "code-runner"
            );

            assertEquals(ErrorCategory.INVALID_INPUT, ex.getCategory());
            assertEquals("code-runner", ex.getToolName());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("Should create from exception")
        void shouldCreateFromException() {
            Exception cause = new RuntimeException("File not found: /path/to/file");
            ToolException ex = ToolException.from(
                "File read failed",
                cause,
                "file-reader"
            );

            assertEquals(ErrorCategory.NOT_FOUND, ex.getCategory());
            assertEquals("file-reader", ex.getToolName());
        }
    }

    @Nested
    @DisplayName("WorkflowException Tests")
    class WorkflowExceptionTests {

        @Test
        @DisplayName("Should create workflow exception with workflow name")
        void shouldCreateWithWorkflowName() {
            WorkflowException ex = new WorkflowException(
                ErrorCategory.CONFIGURATION_ERROR,
                "Invalid workflow configuration",
                "book-creation"
            );

            assertEquals(ErrorCategory.CONFIGURATION_ERROR, ex.getCategory());
            assertEquals("book-creation", ex.getWorkflowName());
            assertTrue(ex.getMessage().contains("workflow=book-creation"));
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("Should create workflow exception with stage name")
        void shouldCreateWithStageName() {
            WorkflowException ex = new WorkflowException(
                ErrorCategory.INTERNAL_ERROR,
                "Stage execution failed",
                null,
                "book-workflow",
                "title-generation"
            );

            assertEquals("book-workflow", ex.getWorkflowName());
            assertEquals("title-generation", ex.getStageName());
            assertTrue(ex.getMessage().contains("workflow=book-workflow"));
            assertTrue(ex.getMessage().contains("stage=title-generation"));
        }

        @Test
        @DisplayName("Should create from exception")
        void shouldCreateFromException() {
            Exception cause = new RuntimeException("Invalid workflow definition");
            WorkflowException ex = WorkflowException.from(
                "Workflow validation failed",
                cause,
                "test-workflow"
            );

            assertEquals(ErrorCategory.INVALID_INPUT, ex.getCategory());
            assertEquals("test-workflow", ex.getWorkflowName());
        }
    }

    @Nested
    @DisplayName("Comparison with Old System")
    class ComparisonTests {

        @Test
        @DisplayName("New system should be more concise than builder pattern")
        void shouldBeMorConcise() {
            // Old way (hypothetical - builder pattern with many methods):
            // throw new ExceptionBuilder()
            //     .errorCode("LLM_RATE_LIMIT_EXCEEDED")
            //     .message("Rate limit exceeded")
            //     .provider("openai")
            //     .model("gpt-4")
            //     .correlationId(UUID.randomUUID().toString())
            //     .retryable(true)
            //     .build();

            // New way - simple and clear:
            ProviderException ex = new ProviderException(
                ErrorCategory.RATE_LIMITED,
                "Rate limit exceeded",
                null,
                "openai",
                "gpt-4"
            );

            // Verify it works correctly
            assertTrue(ex.isRetryable());
            assertEquals("openai", ex.getProviderName());
            assertEquals("gpt-4", ex.getModelName());
        }

        @Test
        @DisplayName("Should automatically determine retryability from category")
        void shouldAutoDetermineRetryability() {
            // No need to explicitly set retryable=true/false
            // It's determined by the category
            ProviderException rateLimitEx = new ProviderException(
                ErrorCategory.RATE_LIMITED,
                "Too many requests",
                "openai"
            );
            assertTrue(rateLimitEx.isRetryable());

            ProviderException authEx = new ProviderException(
                ErrorCategory.AUTH_ERROR,
                "Invalid key",
                "openai"
            );
            assertFalse(authEx.isRetryable());
        }
    }
}

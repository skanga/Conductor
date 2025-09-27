package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ApprovalException functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApprovalExceptionTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should create exception with message only")
    void testMessageOnlyConstructor() {
        String message = "Approval request timed out";
        ApprovalException exception = new ApprovalException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
    }

    @Test
    @Order(2)
    @DisplayName("Should create exception with message and cause")
    void testMessageAndCauseConstructor() {
        String message = "Approval handler failed";
        RuntimeException cause = new RuntimeException("Handler communication error");
        ApprovalException exception = new ApprovalException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
    }

    @Test
    @Order(3)
    @DisplayName("Should create exception with message and context")
    void testMessageAndContextConstructor() {
        String message = "User rejected approval request";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_REJECTED)
            .operation("approval_request")
            .metadata("approval.request_id", "req-123")
            .metadata("approval.user_id", "user-456")
            .build();

        ApprovalException exception = new ApprovalException(message, context);

        assertTrue(exception.getMessage().contains(ErrorCodes.APPROVAL_REJECTED));
        assertTrue(exception.getMessage().contains(message));
        assertNull(exception.getCause());
        assertSame(context, exception.getContext());
        assertTrue(exception.hasContext());
    }

    @Test
    @Order(4)
    @DisplayName("Should create exception with message, cause, and context")
    void testMessageCauseAndContextConstructor() {
        String message = "Approval process failed completely";
        RuntimeException cause = new RuntimeException("Network connection lost");
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_HANDLER_FAILED)
            .operation("approval_communication")
            .metadata("approval.handler_type", "ConsoleApprovalHandler")
            .metadata("approval.timeout_ms", 30000L)
            .build();

        ApprovalException exception = new ApprovalException(message, cause, context);

        assertTrue(exception.getMessage().contains(ErrorCodes.APPROVAL_HANDLER_FAILED));
        assertTrue(exception.getMessage().contains(message));
        assertSame(cause, exception.getCause());
        assertSame(context, exception.getContext());
        assertTrue(exception.hasContext());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle null context gracefully")
    void testNullContextHandling() {
        String message = "Approval failed with null context";
        ApprovalException exception = new ApprovalException(message, (ExceptionContext) null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
    }

    @Test
    @Order(6)
    @DisplayName("Should handle null context with cause gracefully")
    void testNullContextWithCauseHandling() {
        String message = "Approval failed with cause but no context";
        RuntimeException cause = new RuntimeException("Handler error");
        ApprovalException exception = new ApprovalException(message, cause, null);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getContext());
        assertFalse(exception.hasContext());
    }

    @Test
    @Order(7)
    @DisplayName("Should inherit from ConductorException")
    void testInheritanceFromConductorException() {
        ApprovalException exception = new ApprovalException("Test message");

        assertTrue(exception instanceof ConductorException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
        // Note: ConductorException is checked, not a RuntimeException
        // ConductorException is checked, not a RuntimeException
    }

    @Test
    @Order(8)
    @DisplayName("Should get approval request ID from context")
    void testGetApprovalRequestId() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.request_id", "req-789")
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        assertEquals("req-789", exception.getApprovalRequestId());
    }

    @Test
    @Order(9)
    @DisplayName("Should return null approval request ID when no context")
    void testGetApprovalRequestIdNoContext() {
        ApprovalException exception = new ApprovalException("Test message");

        assertNull(exception.getApprovalRequestId());
    }

    @Test
    @Order(10)
    @DisplayName("Should return null approval request ID when not in metadata")
    void testGetApprovalRequestIdNotInMetadata() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("other.field", "value")
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        assertNull(exception.getApprovalRequestId());
    }

    @Test
    @Order(11)
    @DisplayName("Should get handler type from context")
    void testGetHandlerType() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.handler_type", "ConsoleApprovalHandler")
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        assertEquals("ConsoleApprovalHandler", exception.getHandlerType());
    }

    @Test
    @Order(12)
    @DisplayName("Should return null handler type when no context")
    void testGetHandlerTypeNoContext() {
        ApprovalException exception = new ApprovalException("Test message");

        assertNull(exception.getHandlerType());
    }

    @Test
    @Order(13)
    @DisplayName("Should get timeout from context")
    void testGetTimeoutMs() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.timeout_ms", 60000L)
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        assertEquals(60000L, exception.getTimeoutMs());
    }

    @Test
    @Order(14)
    @DisplayName("Should return null timeout when no context")
    void testGetTimeoutMsNoContext() {
        ApprovalException exception = new ApprovalException("Test message");

        assertNull(exception.getTimeoutMs());
    }

    @Test
    @Order(15)
    @DisplayName("Should get user ID from context")
    void testGetUserId() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.user_id", "admin-user")
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        assertEquals("admin-user", exception.getUserId());
    }

    @Test
    @Order(16)
    @DisplayName("Should return null user ID when no context")
    void testGetUserIdNoContext() {
        ApprovalException exception = new ApprovalException("Test message");

        assertNull(exception.getUserId());
    }

    @Test
    @Order(17)
    @DisplayName("Should get escalation contact from context")
    void testGetEscalationContact() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.escalation_contact", "admin@company.com")
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        assertEquals("admin@company.com", exception.getEscalationContact());
    }

    @Test
    @Order(18)
    @DisplayName("Should return null escalation contact when no context")
    void testGetEscalationContactNoContext() {
        ApprovalException exception = new ApprovalException("Test message");

        assertNull(exception.getEscalationContact());
    }

    @Test
    @Order(19)
    @DisplayName("Should handle all approval metadata together")
    void testAllApprovalMetadata() {
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_TIMEOUT)
            .operation("approval_timeout")
            .metadata("approval.request_id", "req-comprehensive")
            .metadata("approval.handler_type", "WebApprovalHandler")
            .metadata("approval.timeout_ms", 120000L)
            .metadata("approval.user_id", "reviewer-1")
            .metadata("approval.escalation_contact", "manager@company.com")
            .build();

        ApprovalException exception = new ApprovalException("Comprehensive approval test", context);

        assertEquals("req-comprehensive", exception.getApprovalRequestId());
        assertEquals("WebApprovalHandler", exception.getHandlerType());
        assertEquals(120000L, exception.getTimeoutMs());
        assertEquals("reviewer-1", exception.getUserId());
        assertEquals("manager@company.com", exception.getEscalationContact());
        assertTrue(exception.hasContext());
        assertEquals(ErrorCodes.APPROVAL_TIMEOUT, exception.getErrorCode());
    }

    @Test
    @Order(20)
    @DisplayName("Should support exception chaining")
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root approval error");
        IllegalStateException intermediateCause = new IllegalStateException("Handler in invalid state", rootCause);
        ApprovalException exception = new ApprovalException("Chained approval failure", intermediateCause);

        assertSame(intermediateCause, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());

        // Test stack trace elements are preserved
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    @Order(21)
    @DisplayName("Should handle wrong type in context metadata")
    void testWrongTypeInContextMetadata() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.timeout_ms", "not-a-number") // Wrong type
            .metadata("approval.request_id", 12345) // Wrong type
            .build();
        ApprovalException exception = new ApprovalException("Test message", context);

        // Should return null when type doesn't match
        assertNull(exception.getTimeoutMs());
        assertNull(exception.getApprovalRequestId());
    }

    @Test
    @Order(22)
    @DisplayName("Should handle typical approval scenarios")
    void testTypicalApprovalScenarios() {
        // Timeout scenario
        ExceptionContext timeoutContext = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_TIMEOUT)
            .metadata("approval.timeout_ms", 30000L)
            .metadata("approval.handler_type", "ConsoleApprovalHandler")
            .build();
        ApprovalException timeoutException = new ApprovalException("User approval timed out", timeoutContext);
        assertEquals(30000L, timeoutException.getTimeoutMs());
        assertEquals("ConsoleApprovalHandler", timeoutException.getHandlerType());

        // Rejection scenario
        ExceptionContext rejectionContext = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_REJECTED)
            .metadata("approval.user_id", "reviewer-2")
            .metadata("approval.request_id", "req-reject-001")
            .build();
        ApprovalException rejectionException = new ApprovalException("User denied approval", rejectionContext);
        assertEquals("reviewer-2", rejectionException.getUserId());
        assertEquals("req-reject-001", rejectionException.getApprovalRequestId());

        // Communication failure scenario
        RuntimeException commCause = new RuntimeException("Network error");
        ExceptionContext commContext = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_HANDLER_FAILED)
            .metadata("approval.escalation_contact", "admin@company.com")
            .build();
        ApprovalException commException = new ApprovalException("Communication with approval handler failed", commCause, commContext);
        assertEquals("admin@company.com", commException.getEscalationContact());
        assertSame(commCause, commException.getCause());
    }

    @Test
    @Order(23)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception", ApprovalException.class.getPackageName());
        assertEquals("ApprovalException", ApprovalException.class.getSimpleName());
        assertEquals("com.skanga.conductor.exception.ApprovalException",
                    ApprovalException.class.getName());
    }

    @Test
    @Order(24)
    @DisplayName("Should provide detailed summary with context")
    void testDetailedSummaryWithContext() {
        String message = "Approval workflow failed";
        ExceptionContext context = ExceptionContext.builder()
            .errorCode(ErrorCodes.APPROVAL_TIMEOUT)
            .operation("workflow_approval")
            .correlationId("workflow-123")
            .metadata("approval.request_id", "req-summary-test")
            .metadata("approval.timeout_ms", 45000L)
            .build();

        ApprovalException exception = new ApprovalException(message, context);
        String detailedSummary = exception.getDetailedSummary();

        assertNotNull(detailedSummary);
        assertTrue(detailedSummary.contains(context.getSummary()));
        assertTrue(detailedSummary.contains(message));
    }

    @Test
    @Order(25)
    @DisplayName("Should maintain metadata type safety")
    void testMetadataTypeSafety() {
        ExceptionContext context = ExceptionContext.builder()
            .metadata("approval.timeout_ms", 15000L) // Long
            .metadata("approval.request_id", "req-type-safety") // String
            .metadata("approval.handler_type", "TestHandler") // String
            .build();
        ApprovalException exception = new ApprovalException("Type safety test", context);

        // Correct types should work
        assertEquals(15000L, exception.getTimeoutMs());
        assertEquals("req-type-safety", exception.getApprovalRequestId());
        assertEquals("TestHandler", exception.getHandlerType());

        // Verify these are the expected types
        assertInstanceOf(Long.class, exception.getTimeoutMs());
        assertInstanceOf(String.class, exception.getApprovalRequestId());
        assertInstanceOf(String.class, exception.getHandlerType());
    }
}
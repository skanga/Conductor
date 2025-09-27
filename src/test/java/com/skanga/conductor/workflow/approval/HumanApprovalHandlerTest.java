package com.skanga.conductor.workflow.approval;

import com.skanga.conductor.exception.ApprovalException;
import com.skanga.conductor.exception.ApprovalTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HumanApprovalHandler Tests")
class HumanApprovalHandlerTest {

    @Test
    @DisplayName("Should implement approval handler interface correctly")
    void testApprovalHandlerInterface() {
        // Create a mock implementation for testing
        HumanApprovalHandler handler = new TestApprovalHandler();

        // Test basic interface methods
        assertTrue(handler.isInteractive());
        assertEquals("Test Approval Handler", handler.getDescription());
    }

    @Test
    @DisplayName("Should handle approval request with success")
    void testSuccessfulApproval() throws ApprovalException, ApprovalTimeoutException {
        // Given
        HumanApprovalHandler handler = new TestApprovalHandler();
        ApprovalRequest request = new ApprovalRequest(
            "test-workflow",
            "test-stage",
            "Test stage description",
            "Test content for approval",
            "Review this content for accuracy"
        );

        // When
        ApprovalResponse response = handler.requestApproval(request, 5000);

        // Then
        assertNotNull(response);
        assertTrue(response.isApproved());
        assertEquals("Approved by test handler", response.getFeedback());
    }

    @Test
    @DisplayName("Should handle approval timeout")
    void testApprovalTimeout() {
        // Given
        HumanApprovalHandler handler = new TimeoutApprovalHandler();
        ApprovalRequest request = new ApprovalRequest(
            "test-workflow",
            "timeout-stage",
            "Timeout test stage",
            "Content that will timeout",
            "This will timeout"
        );

        // When & Then
        assertThrows(ApprovalTimeoutException.class, () -> {
            handler.requestApproval(request, 100); // Very short timeout
        });
    }

    @Test
    @DisplayName("Should handle approval rejection")
    void testApprovalRejection() throws ApprovalException, ApprovalTimeoutException {
        // Given
        HumanApprovalHandler handler = new RejectingApprovalHandler();
        ApprovalRequest request = new ApprovalRequest(
            "test-workflow",
            "reject-stage",
            "Rejection test stage",
            "Content to be rejected",
            "This will be rejected"
        );

        // When
        ApprovalResponse response = handler.requestApproval(request, 5000);

        // Then
        assertNotNull(response);
        assertFalse(response.isApproved());
        assertEquals("Content rejected by test handler", response.getFeedback());
    }

    @Test
    @DisplayName("Should handle approval exception")
    void testApprovalException() {
        // Given
        HumanApprovalHandler handler = new ErrorApprovalHandler();
        ApprovalRequest request = new ApprovalRequest(
            "test-workflow",
            "error-stage",
            "Error test stage",
            "Content that causes error",
            "This will cause an error"
        );

        // When & Then
        assertThrows(ApprovalException.class, () -> {
            handler.requestApproval(request, 5000);
        });
    }

    @Test
    @DisplayName("Should handle non-interactive handler")
    void testNonInteractiveHandler() {
        // Given
        HumanApprovalHandler handler = new NonInteractiveApprovalHandler();

        // Then
        assertFalse(handler.isInteractive());
        assertEquals("Non-Interactive Auto-Approval Handler", handler.getDescription());
    }

    @Test
    @DisplayName("Should handle various timeout values")
    void testTimeoutValues() throws ApprovalException, ApprovalTimeoutException {
        // Given
        HumanApprovalHandler handler = new TestApprovalHandler();
        ApprovalRequest request = new ApprovalRequest(
            "test-workflow",
            "timeout-test",
            "Timeout test",
            "Timeout test content",
            "Testing timeout handling"
        );

        // Test different timeout values
        long[] timeouts = {1, 1000, 30000};

        for (long timeout : timeouts) {
            ApprovalResponse response = handler.requestApproval(request, timeout);
            assertNotNull(response);
        }
    }

    @Test
    @DisplayName("Should handle concurrent approval requests")
    void testConcurrentApprovals() throws InterruptedException {
        // Given
        HumanApprovalHandler handler = new TestApprovalHandler();
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        ApprovalResponse[] responses = new ApprovalResponse[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    ApprovalRequest request = new ApprovalRequest(
                        "test-workflow",
                        "concurrent-stage-" + index,
                        "Concurrent test stage " + index,
                        "Concurrent approval content " + index,
                        "Testing concurrent approvals"
                    );
                    responses[index] = handler.requestApproval(request, 5000);
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
            assertNotNull(responses[i], "Thread " + i + " should have received a response");
            assertTrue(responses[i].isApproved());
        }
    }

    // Test implementation classes

    private static class TestApprovalHandler implements HumanApprovalHandler {
        @Override
        public ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
                throws ApprovalTimeoutException, ApprovalException {
            return ApprovalResponse.approve("Approved by test handler");
        }

        @Override
        public boolean isInteractive() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Test Approval Handler";
        }
    }

    private static class TimeoutApprovalHandler implements HumanApprovalHandler {
        @Override
        public ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
                throws ApprovalTimeoutException, ApprovalException {
            throw new ApprovalTimeoutException(timeoutMs);
        }

        @Override
        public boolean isInteractive() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Timeout Test Handler";
        }
    }

    private static class RejectingApprovalHandler implements HumanApprovalHandler {
        @Override
        public ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
                throws ApprovalTimeoutException, ApprovalException {
            return ApprovalResponse.reject("Content rejected by test handler");
        }

        @Override
        public boolean isInteractive() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Rejecting Test Handler";
        }
    }

    private static class ErrorApprovalHandler implements HumanApprovalHandler {
        @Override
        public ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
                throws ApprovalTimeoutException, ApprovalException {
            throw new ApprovalException("Test approval error");
        }

        @Override
        public boolean isInteractive() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Error Test Handler";
        }
    }

    private static class NonInteractiveApprovalHandler implements HumanApprovalHandler {
        @Override
        public ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
                throws ApprovalTimeoutException, ApprovalException {
            return ApprovalResponse.approve("Auto-approved (non-interactive)");
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Non-Interactive Auto-Approval Handler";
        }
    }
}
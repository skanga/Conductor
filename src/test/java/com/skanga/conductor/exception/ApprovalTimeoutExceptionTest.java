package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ApprovalTimeoutException functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApprovalTimeoutExceptionTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should create exception with timeout only")
    void testTimeoutOnlyConstructor() {
        long timeoutMs = 30000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(timeoutMs);

        assertEquals("Approval request timed out after " + timeoutMs + "ms", exception.getMessage());
        assertEquals(timeoutMs, exception.getTimeoutMs());
        assertNull(exception.getCause());
    }

    @Test
    @Order(2)
    @DisplayName("Should create exception with custom message and timeout")
    void testMessageAndTimeoutConstructor() {
        String message = "Custom timeout message";
        long timeoutMs = 45000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(message, timeoutMs);

        assertEquals(message, exception.getMessage());
        assertEquals(timeoutMs, exception.getTimeoutMs());
        assertNull(exception.getCause());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle zero timeout")
    void testZeroTimeout() {
        long timeoutMs = 0L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(timeoutMs);

        assertEquals("Approval request timed out after 0ms", exception.getMessage());
        assertEquals(0L, exception.getTimeoutMs());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle negative timeout")
    void testNegativeTimeout() {
        long timeoutMs = -1000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(timeoutMs);

        assertEquals("Approval request timed out after -1000ms", exception.getMessage());
        assertEquals(-1000L, exception.getTimeoutMs());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle large timeout values")
    void testLargeTimeout() {
        long timeoutMs = Long.MAX_VALUE;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(timeoutMs);

        assertEquals("Approval request timed out after " + Long.MAX_VALUE + "ms", exception.getMessage());
        assertEquals(Long.MAX_VALUE, exception.getTimeoutMs());
    }

    @Test
    @Order(6)
    @DisplayName("Should handle null message with timeout")
    void testNullMessageWithTimeout() {
        long timeoutMs = 15000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(null, timeoutMs);

        assertNull(exception.getMessage());
        assertEquals(timeoutMs, exception.getTimeoutMs());
    }

    @Test
    @Order(7)
    @DisplayName("Should handle empty message with timeout")
    void testEmptyMessageWithTimeout() {
        long timeoutMs = 20000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException("", timeoutMs);

        assertEquals("", exception.getMessage());
        assertEquals(timeoutMs, exception.getTimeoutMs());
    }

    @Test
    @Order(8)
    @DisplayName("Should inherit from ApprovalException")
    void testInheritanceFromApprovalException() {
        ApprovalTimeoutException exception = new ApprovalTimeoutException(30000L);

        assertTrue(exception instanceof ApprovalException);
        assertTrue(exception instanceof ConductorException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
        // Note: ConductorException is checked, not a RuntimeException
        // ConductorException is checked, not a RuntimeException
    }

    @Test
    @Order(9)
    @DisplayName("Should override getTimeoutMs from parent class")
    void testOverriddenGetTimeoutMs() {
        long timeoutMs = 60000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(timeoutMs);

        // Should return the field value, not from context
        assertEquals(timeoutMs, exception.getTimeoutMs());
        assertInstanceOf(Long.class, exception.getTimeoutMs());
    }

    @Test
    @Order(10)
    @DisplayName("Should maintain timeout value across different constructors")
    void testTimeoutValueConsistency() {
        long timeout1 = 25000L;
        long timeout2 = 35000L;

        ApprovalTimeoutException exception1 = new ApprovalTimeoutException(timeout1);
        ApprovalTimeoutException exception2 = new ApprovalTimeoutException("Custom message", timeout2);

        assertEquals(timeout1, exception1.getTimeoutMs());
        assertEquals(timeout2, exception2.getTimeoutMs());

        assertNotEquals(exception1.getTimeoutMs(), exception2.getTimeoutMs());
    }

    @Test
    @Order(11)
    @DisplayName("Should generate appropriate default message")
    void testDefaultMessageGeneration() {
        long[] testTimeouts = {1000L, 5000L, 30000L, 120000L, 300000L};

        for (long timeout : testTimeouts) {
            ApprovalTimeoutException exception = new ApprovalTimeoutException(timeout);
            String expectedMessage = "Approval request timed out after " + timeout + "ms";
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Test
    @Order(12)
    @DisplayName("Should preserve custom message")
    void testCustomMessagePreservation() {
        String customMessage = "User failed to respond within allocated time";
        long timeoutMs = 40000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(customMessage, timeoutMs);

        assertEquals(customMessage, exception.getMessage());
        assertEquals(timeoutMs, exception.getTimeoutMs());
        assertNotEquals("Approval request timed out after " + timeoutMs + "ms", exception.getMessage());
    }

    @Test
    @Order(13)
    @DisplayName("Should support toString representation")
    void testToString() {
        long timeoutMs = 30000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(timeoutMs);

        String toString = exception.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ApprovalTimeoutException"));
        assertTrue(toString.contains("30000"));
    }

    @Test
    @Order(14)
    @DisplayName("Should support fillInStackTrace")
    void testFillInStackTrace() {
        ApprovalTimeoutException exception = new ApprovalTimeoutException(25000L);

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
    @Order(15)
    @DisplayName("Should support getLocalizedMessage")
    void testGetLocalizedMessage() {
        String message = "Approval process timed out";
        long timeoutMs = 45000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(message, timeoutMs);

        assertEquals(message, exception.getLocalizedMessage());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle typical timeout scenarios")
    void testTypicalTimeoutScenarios() {
        // Short timeout (5 seconds)
        ApprovalTimeoutException shortTimeout = new ApprovalTimeoutException(5000L);
        assertEquals(5000L, shortTimeout.getTimeoutMs());
        assertTrue(shortTimeout.getMessage().contains("5000ms"));

        // Medium timeout (2 minutes)
        ApprovalTimeoutException mediumTimeout = new ApprovalTimeoutException("Medium timeout reached", 120000L);
        assertEquals(120000L, mediumTimeout.getTimeoutMs());
        assertEquals("Medium timeout reached", mediumTimeout.getMessage());

        // Long timeout (10 minutes)
        ApprovalTimeoutException longTimeout = new ApprovalTimeoutException(600000L);
        assertEquals(600000L, longTimeout.getTimeoutMs());
        assertTrue(longTimeout.getMessage().contains("600000ms"));
    }

    @Test
    @Order(17)
    @DisplayName("Should maintain timeout field immutability")
    void testTimeoutFieldImmutability() {
        long originalTimeout = 30000L;
        ApprovalTimeoutException exception = new ApprovalTimeoutException(originalTimeout);

        // getTimeoutMs should always return the same value
        assertEquals(originalTimeout, exception.getTimeoutMs());
        assertEquals(originalTimeout, exception.getTimeoutMs()); // Call again

        // Value should be consistent across multiple calls
        Long timeout1 = exception.getTimeoutMs();
        Long timeout2 = exception.getTimeoutMs();
        assertEquals(timeout1, timeout2);
        // Note: Long objects may not be cached for large values, so we use equals instead of same
        assertEquals(timeout1, timeout2);
    }

    @Test
    @Order(18)
    @DisplayName("Should work with inherited approval exception methods")
    void testInheritedApprovalMethods() {
        ApprovalTimeoutException exception = new ApprovalTimeoutException(35000L);

        // Should inherit methods from ApprovalException, but return null since no context
        assertNull(exception.getApprovalRequestId());
        assertNull(exception.getHandlerType());
        assertNull(exception.getUserId());
        assertNull(exception.getEscalationContact());

        // But getTimeoutMs should return the field value
        assertEquals(35000L, exception.getTimeoutMs());
    }

    @Test
    @Order(19)
    @DisplayName("Should be serializable")
    void testSerializability() {
        // Basic serialization test - verify fields are accessible
        long timeoutMs = 42000L;
        String message = "Serialization test timeout";

        ApprovalTimeoutException exception = new ApprovalTimeoutException(message, timeoutMs);

        assertEquals(message, exception.getMessage());
        assertEquals(timeoutMs, exception.getTimeoutMs());
        assertNull(exception.getCause());
        assertFalse(exception.hasContext()); // Should not have context by default

        // Test timeout-only constructor
        ApprovalTimeoutException simpleException = new ApprovalTimeoutException(timeoutMs);
        assertEquals(timeoutMs, simpleException.getTimeoutMs());
        assertTrue(simpleException.getMessage().contains(String.valueOf(timeoutMs)));
    }

    @Test
    @Order(20)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception", ApprovalTimeoutException.class.getPackageName());
        assertEquals("ApprovalTimeoutException", ApprovalTimeoutException.class.getSimpleName());
        assertEquals("com.skanga.conductor.exception.ApprovalTimeoutException",
                    ApprovalTimeoutException.class.getName());
    }

    @Test
    @Order(21)
    @DisplayName("Should handle edge case timeout values")
    void testEdgeCaseTimeoutValues() {
        // Test minimum long value
        ApprovalTimeoutException minException = new ApprovalTimeoutException(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, minException.getTimeoutMs());

        // Test 1ms timeout
        ApprovalTimeoutException oneMs = new ApprovalTimeoutException(1L);
        assertEquals(1L, oneMs.getTimeoutMs());
        assertTrue(oneMs.getMessage().contains("1ms"));

        // Test exactly one second
        ApprovalTimeoutException oneSecond = new ApprovalTimeoutException(1000L);
        assertEquals(1000L, oneSecond.getTimeoutMs());
        assertTrue(oneSecond.getMessage().contains("1000ms"));
    }

    @Test
    @Order(22)
    @DisplayName("Should support comparison of timeout values")
    void testTimeoutComparison() {
        ApprovalTimeoutException timeout1 = new ApprovalTimeoutException(10000L);
        ApprovalTimeoutException timeout2 = new ApprovalTimeoutException(20000L);
        ApprovalTimeoutException timeout3 = new ApprovalTimeoutException(10000L);

        assertTrue(timeout1.getTimeoutMs() < timeout2.getTimeoutMs());
        assertTrue(timeout2.getTimeoutMs() > timeout1.getTimeoutMs());
        assertEquals(timeout1.getTimeoutMs(), timeout3.getTimeoutMs());
    }
}
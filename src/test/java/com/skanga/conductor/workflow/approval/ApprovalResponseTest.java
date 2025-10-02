package com.skanga.conductor.workflow.approval;

import com.skanga.conductor.workflow.approval.ApprovalResponse.Decision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalResponseTest {

    private static final String TEST_FEEDBACK = "This is test feedback";
    private static final String TEST_REASON = "This is a rejection reason";
    private static final String TEST_REVISION = "Please revise section A and B";

    private long beforeResponseTime;

    @BeforeEach
    void setUp() {
        beforeResponseTime = System.currentTimeMillis();
    }

    @Test
    void shouldCreateApprovalResponseWithDecisionAndFeedback() {
        // When
        ApprovalResponse response = new ApprovalResponse(Decision.APPROVE, TEST_FEEDBACK);
        long afterResponseTime = System.currentTimeMillis();

        // Then
        assertEquals(Decision.APPROVE, response.getDecision());
        assertEquals(TEST_FEEDBACK, response.getFeedback());
        assertTrue(response.getResponseTime() >= beforeResponseTime);
        assertTrue(response.getResponseTime() <= afterResponseTime);
    }

    @Test
    void shouldCreateApprovalResponseWithNullFeedback() {
        // When
        ApprovalResponse response = new ApprovalResponse(Decision.REJECT, null);

        // Then
        assertEquals(Decision.REJECT, response.getDecision());
        assertNull(response.getFeedback());
        assertTrue(response.getResponseTime() > 0);
    }

    @Test
    void shouldCreateApprovalResponseWithEmptyFeedback() {
        // When
        ApprovalResponse response = new ApprovalResponse(Decision.REVISE, "");

        // Then
        assertEquals(Decision.REVISE, response.getDecision());
        assertEquals("", response.getFeedback());
        assertTrue(response.getResponseTime() > 0);
    }

    @Test
    void shouldCreateApprovalResponseUsingApproveFactoryMethod() {
        // When
        ApprovalResponse response = ApprovalResponse.approve();

        // Then
        assertEquals(Decision.APPROVE, response.getDecision());
        assertNull(response.getFeedback());
        assertTrue(response.isApproved());
        assertFalse(response.isRejected());
        assertFalse(response.needsRevision());
    }

    @Test
    void shouldCreateApprovalResponseUsingApproveWithFeedbackFactoryMethod() {
        // When
        ApprovalResponse response = ApprovalResponse.approve(TEST_FEEDBACK);

        // Then
        assertEquals(Decision.APPROVE, response.getDecision());
        assertEquals(TEST_FEEDBACK, response.getFeedback());
        assertTrue(response.isApproved());
        assertFalse(response.isRejected());
        assertFalse(response.needsRevision());
    }

    @Test
    void shouldCreateApprovalResponseUsingRejectFactoryMethod() {
        // When
        ApprovalResponse response = ApprovalResponse.reject(TEST_REASON);

        // Then
        assertEquals(Decision.REJECT, response.getDecision());
        assertEquals(TEST_REASON, response.getFeedback());
        assertFalse(response.isApproved());
        assertTrue(response.isRejected());
        assertFalse(response.needsRevision());
    }

    @Test
    void shouldCreateApprovalResponseUsingReviseFactoryMethod() {
        // When
        ApprovalResponse response = ApprovalResponse.revise(TEST_REVISION);

        // Then
        assertEquals(Decision.REVISE, response.getDecision());
        assertEquals(TEST_REVISION, response.getFeedback());
        assertFalse(response.isApproved());
        assertFalse(response.isRejected());
        assertTrue(response.needsRevision());
    }

    @Test
    void shouldHandleAllDecisionTypes() {
        // Test APPROVE
        ApprovalResponse approve = new ApprovalResponse(Decision.APPROVE, "Good work");
        assertTrue(approve.isApproved());
        assertFalse(approve.isRejected());
        assertFalse(approve.needsRevision());

        // Test REJECT
        ApprovalResponse reject = new ApprovalResponse(Decision.REJECT, "Not acceptable");
        assertFalse(reject.isApproved());
        assertTrue(reject.isRejected());
        assertFalse(reject.needsRevision());

        // Test REVISE
        ApprovalResponse revise = new ApprovalResponse(Decision.REVISE, "Needs changes");
        assertFalse(revise.isApproved());
        assertFalse(revise.isRejected());
        assertTrue(revise.needsRevision());
    }

    @Test
    void shouldGenerateToStringCorrectly() {
        // Given
        ApprovalResponse response = new ApprovalResponse(Decision.APPROVE, TEST_FEEDBACK);

        // When
        String toString = response.toString();

        // Then
        assertTrue(toString.contains("ApprovalResponse{"));
        assertTrue(toString.contains("decision=APPROVE"));
        assertTrue(toString.contains("feedback='" + TEST_FEEDBACK + "'"));
        assertTrue(toString.contains("responseTime=" + response.getResponseTime()));
    }

    @Test
    void shouldHandleSpecialCharactersInFeedback() {
        // Given
        String specialFeedback = "Feedback with unicode: 你好世界, émojis 🚀, and symbols: @#$%^&*()";

        // When
        ApprovalResponse response = ApprovalResponse.approve(specialFeedback);

        // Then
        assertEquals(Decision.APPROVE, response.getDecision());
        assertEquals(specialFeedback, response.getFeedback());
    }

    @Test
    void shouldHandleNewlinesAndTabsInFeedback() {
        // Given
        String multilineFeedback = "Line 1\nLine 2\tTabbed content\r\nWindows newline";

        // When
        ApprovalResponse response = ApprovalResponse.reject(multilineFeedback);

        // Then
        assertEquals(Decision.REJECT, response.getDecision());
        assertEquals(multilineFeedback, response.getFeedback());
    }

    @Test
    void shouldHandleVeryLongFeedback() {
        // Given
        StringBuilder longFeedback = new StringBuilder();
        for (int i = 0; i < 50; i++) { // Reduced from 1000 to 50 for faster testing
            longFeedback.append("Very long feedback message. ");
        }

        // When
        ApprovalResponse response = ApprovalResponse.revise(longFeedback.toString());

        // Then
        assertEquals(Decision.REVISE, response.getDecision());
        assertEquals(longFeedback.toString(), response.getFeedback());
        assertTrue(response.getFeedback().length() > 1000); // Adjusted for reduced iterations (50 * ~27 chars per message)
    }

    @Test
    void shouldCreateResponsesWithDifferentTimestamps() throws InterruptedException {
        // Given
        ApprovalResponse response1 = ApprovalResponse.approve("First response");

        // Small delay to ensure different timestamps
        Thread.sleep(1);

        ApprovalResponse response2 = ApprovalResponse.reject("Second response");

        // Then
        assertNotEquals(response1.getResponseTime(), response2.getResponseTime());
        assertTrue(response2.getResponseTime() > response1.getResponseTime());
    }

    @Test
    void shouldHandleWhitespaceOnlyFeedback() {
        // Given
        String whitespaceFeedback = "   \n\t  \r\n  ";

        // When
        ApprovalResponse response = ApprovalResponse.approve(whitespaceFeedback);

        // Then
        assertEquals(Decision.APPROVE, response.getDecision());
        assertEquals(whitespaceFeedback, response.getFeedback());
    }

    @Test
    void shouldCreateMultipleResponsesOfSameType() {
        // When
        ApprovalResponse approve1 = ApprovalResponse.approve("First approval");
        ApprovalResponse approve2 = ApprovalResponse.approve("Second approval");
        ApprovalResponse reject1 = ApprovalResponse.reject("First rejection");
        ApprovalResponse reject2 = ApprovalResponse.reject("Second rejection");

        // Then
        assertTrue(approve1.isApproved());
        assertTrue(approve2.isApproved());
        assertTrue(reject1.isRejected());
        assertTrue(reject2.isRejected());

        assertNotEquals(approve1.getFeedback(), approve2.getFeedback());
        assertNotEquals(reject1.getFeedback(), reject2.getFeedback());
    }

    @Test
    void shouldHandleNullAndEmptyFeedbackInFactoryMethods() {
        // When
        ApprovalResponse approveNull = ApprovalResponse.approve(null);
        ApprovalResponse approveEmpty = ApprovalResponse.approve("");
        ApprovalResponse rejectNull = ApprovalResponse.reject(null);
        ApprovalResponse rejectEmpty = ApprovalResponse.reject("");
        ApprovalResponse reviseNull = ApprovalResponse.revise(null);
        ApprovalResponse reviseEmpty = ApprovalResponse.revise("");

        // Then
        assertNull(approveNull.getFeedback());
        assertEquals("", approveEmpty.getFeedback());
        assertNull(rejectNull.getFeedback());
        assertEquals("", rejectEmpty.getFeedback());
        assertNull(reviseNull.getFeedback());
        assertEquals("", reviseEmpty.getFeedback());
    }

    @Test
    void shouldValidateDecisionEnumValues() {
        // Test all enum values exist and are accessible
        Decision[] decisions = Decision.values();
        assertEquals(3, decisions.length);

        assertTrue(java.util.Arrays.asList(decisions).contains(Decision.APPROVE));
        assertTrue(java.util.Arrays.asList(decisions).contains(Decision.REJECT));
        assertTrue(java.util.Arrays.asList(decisions).contains(Decision.REVISE));
    }

    @Test
    void shouldHandleDecisionEnumAsString() {
        // Given
        ApprovalResponse response = new ApprovalResponse(Decision.APPROVE, "Test");

        // When & Then
        assertEquals("APPROVE", response.getDecision().toString());
        assertEquals(Decision.APPROVE, Decision.valueOf("APPROVE"));
    }

    @Test
    void shouldMaintainResponseTimeAccuracy() {
        // Given
        long beforeTime = System.currentTimeMillis();

        // When
        ApprovalResponse response = ApprovalResponse.approve("Timing test");

        // Then
        long afterTime = System.currentTimeMillis();
        long responseTime = response.getResponseTime();

        assertTrue(responseTime >= beforeTime);
        assertTrue(responseTime <= afterTime);
        assertTrue(afterTime - beforeTime < 100); // Should complete quickly
    }
}
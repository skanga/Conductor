package com.skanga.conductor.workflow.approval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalRequestTest {

    private static final String WORKFLOW_NAME = "TestWorkflow";
    private static final String STAGE_NAME = "TestStage";
    private static final String STAGE_DESCRIPTION = "Test stage description";
    private static final String GENERATED_CONTENT = "This is some generated content";
    private static final String REVIEW_CONTENT = "This is review content";

    private ApprovalRequest approvalRequest;
    private long beforeRequestTime;

    @BeforeEach
    void setUp() {
        beforeRequestTime = System.currentTimeMillis();
        approvalRequest = new ApprovalRequest(
            WORKFLOW_NAME,
            STAGE_NAME,
            STAGE_DESCRIPTION,
            GENERATED_CONTENT,
            REVIEW_CONTENT
        );
    }

    @Test
    void shouldCreateApprovalRequestWithAllFields() {
        // Then
        assertEquals(WORKFLOW_NAME, approvalRequest.getWorkflowName());
        assertEquals(STAGE_NAME, approvalRequest.getStageName());
        assertEquals(STAGE_DESCRIPTION, approvalRequest.getStageDescription());
        assertEquals(GENERATED_CONTENT, approvalRequest.getGeneratedContent());
        assertEquals(REVIEW_CONTENT, approvalRequest.getReviewContent());

        // Request time should be recent
        long afterRequestTime = System.currentTimeMillis();
        assertTrue(approvalRequest.getRequestTime() >= beforeRequestTime);
        assertTrue(approvalRequest.getRequestTime() <= afterRequestTime);
    }

    @Test
    void shouldCreateApprovalRequestWithNullValues() {
        // When
        ApprovalRequest requestWithNulls = new ApprovalRequest(null, null, null, null, null);

        // Then
        assertNull(requestWithNulls.getWorkflowName());
        assertNull(requestWithNulls.getStageName());
        assertNull(requestWithNulls.getStageDescription());
        assertNull(requestWithNulls.getGeneratedContent());
        assertNull(requestWithNulls.getReviewContent());
        assertTrue(requestWithNulls.getRequestTime() > 0);
    }

    @Test
    void shouldGetContentPreviewForShortContent() {
        // Given
        String shortContent = "Short content";
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, shortContent, REVIEW_CONTENT
        );

        // When
        String preview = request.getContentPreview();

        // Then
        assertEquals(shortContent, preview);
    }

    @Test
    void shouldGetContentPreviewForLongContent() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longContent.append("This is a long line of content that will exceed 200 characters. ");
        }

        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, longContent.toString(), REVIEW_CONTENT
        );

        // When
        String preview = request.getContentPreview();

        // Then
        assertEquals(203, preview.length()); // 200 characters + "..."
        assertTrue(preview.endsWith("..."));
        assertEquals(longContent.substring(0, 200) + "...", preview);
    }

    @Test
    void shouldGetContentPreviewForNullContent() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, null, REVIEW_CONTENT
        );

        // When
        String preview = request.getContentPreview();

        // Then
        assertEquals("No content generated", preview);
    }

    @Test
    void shouldGetContentPreviewForEmptyContent() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, "", REVIEW_CONTENT
        );

        // When
        String preview = request.getContentPreview();

        // Then
        assertEquals("", preview);
    }

    @Test
    void shouldGetReviewPreviewForShortReview() {
        // Given
        String shortReview = "Short review";
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, GENERATED_CONTENT, shortReview
        );

        // When
        String preview = request.getReviewPreview();

        // Then
        assertEquals(shortReview, preview);
    }

    @Test
    void shouldGetReviewPreviewForLongReview() {
        // Given
        StringBuilder longReview = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            longReview.append("This is a long review that will exceed 150 characters. ");
        }

        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, GENERATED_CONTENT, longReview.toString()
        );

        // When
        String preview = request.getReviewPreview();

        // Then
        assertEquals(153, preview.length()); // 150 characters + "..."
        assertTrue(preview.endsWith("..."));
        assertEquals(longReview.substring(0, 150) + "...", preview);
    }

    @Test
    void shouldGetReviewPreviewForNullReview() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, GENERATED_CONTENT, null
        );

        // When
        String preview = request.getReviewPreview();

        // Then
        assertEquals("No review available", preview);
    }

    @Test
    void shouldGetReviewPreviewForEmptyReview() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, GENERATED_CONTENT, ""
        );

        // When
        String preview = request.getReviewPreview();

        // Then
        assertEquals("", preview);
    }

    @Test
    void shouldHandleContentAtExactly200Characters() {
        // Given
        String exactContent = "A".repeat(200);
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, exactContent, REVIEW_CONTENT
        );

        // When
        String preview = request.getContentPreview();

        // Then
        assertEquals(exactContent, preview);
        assertFalse(preview.endsWith("..."));
    }

    @Test
    void shouldHandleReviewAtExactly150Characters() {
        // Given
        String exactReview = "B".repeat(150);
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, GENERATED_CONTENT, exactReview
        );

        // When
        String preview = request.getReviewPreview();

        // Then
        assertEquals(exactReview, preview);
        assertFalse(preview.endsWith("..."));
    }

    @Test
    void shouldGenerateToStringCorrectly() {
        // When
        String toString = approvalRequest.toString();

        // Then
        assertTrue(toString.contains("ApprovalRequest{"));
        assertTrue(toString.contains("workflow='" + WORKFLOW_NAME + "'"));
        assertTrue(toString.contains("stage='" + STAGE_NAME + "'"));
        assertTrue(toString.contains("requestTime=" + approvalRequest.getRequestTime()));
    }

    @Test
    void shouldHandleSpecialCharactersInFields() {
        // Given
        String specialWorkflow = "Wörkflöw with émojis 🚀 and spëcial chars";
        String specialStage = "Stage with newlines\nand\ttabs";
        String specialDescription = "Description with \"quotes\" and 'apostrophes'";
        String specialContent = "Content with unicode: 你好世界, ñáéíóú";
        String specialReview = "Review with symbols: @#$%^&*()_+-=[]{}|;:,.<>?/~`";

        // When
        ApprovalRequest request = new ApprovalRequest(
            specialWorkflow, specialStage, specialDescription, specialContent, specialReview
        );

        // Then
        assertEquals(specialWorkflow, request.getWorkflowName());
        assertEquals(specialStage, request.getStageName());
        assertEquals(specialDescription, request.getStageDescription());
        assertEquals(specialContent, request.getGeneratedContent());
        assertEquals(specialReview, request.getReviewContent());
    }

    @Test
    void shouldHandleWhitespaceOnlyContent() {
        // Given
        String whitespaceContent = "   \n\t  ";
        String whitespaceReview = "\t\n   \r\n";

        // When
        ApprovalRequest request = new ApprovalRequest(
            WORKFLOW_NAME, STAGE_NAME, STAGE_DESCRIPTION, whitespaceContent, whitespaceReview
        );

        // Then
        assertEquals(whitespaceContent, request.getContentPreview());
        assertEquals(whitespaceReview, request.getReviewPreview());
    }

    @Test
    void shouldCreateMultipleRequestsWithDifferentTimestamps() throws InterruptedException {
        // Given
        ApprovalRequest request1 = new ApprovalRequest(
            "Workflow1", "Stage1", "Description1", "Content1", "Review1"
        );

        // Small delay to ensure different timestamps
        Thread.sleep(1);

        ApprovalRequest request2 = new ApprovalRequest(
            "Workflow2", "Stage2", "Description2", "Content2", "Review2"
        );

        // Then
        assertNotEquals(request1.getRequestTime(), request2.getRequestTime());
        assertTrue(request2.getRequestTime() > request1.getRequestTime());
    }

    @Test
    void shouldHandleVeryLongFieldValues() {
        // Given
        String veryLongWorkflow = "W".repeat(1000);
        String veryLongStage = "S".repeat(1000);
        String veryLongDescription = "D".repeat(1000);
        String veryLongContent = "C".repeat(1000);
        String veryLongReview = "R".repeat(1000);

        // When
        ApprovalRequest request = new ApprovalRequest(
            veryLongWorkflow, veryLongStage, veryLongDescription, veryLongContent, veryLongReview
        );

        // Then
        assertEquals(veryLongWorkflow, request.getWorkflowName());
        assertEquals(veryLongStage, request.getStageName());
        assertEquals(veryLongDescription, request.getStageDescription());
        assertEquals(203, request.getContentPreview().length()); // 200 + "..."
        assertEquals(153, request.getReviewPreview().length()); // 150 + "..."
    }
}
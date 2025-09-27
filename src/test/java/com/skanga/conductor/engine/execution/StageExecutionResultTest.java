package com.skanga.conductor.engine.execution;

import com.skanga.conductor.workflow.config.IterativeWorkflowStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StageExecutionResultTest {

    private StageExecutionResult result;

    @BeforeEach
    void setUp() {
        result = new StageExecutionResult();
    }

    @Test
    void shouldCreateStageExecutionResultWithDefaults() {
        // Then
        assertEquals(0L, result.getStartTime());
        assertEquals(0L, result.getEndTime());
        assertTrue(result.isSuccess()); // Default is true
        assertNull(result.getErrorMessage());
        assertNull(result.getAgentResponse());
        assertNull(result.getReviewResponse());
        assertFalse(result.isApprovalRequested()); // Default is false
        assertFalse(result.isApproved()); // Default is false
        assertNull(result.getApprovalFeedback());
        assertNotNull(result.getGeneratedFiles());
        assertTrue(result.getGeneratedFiles().isEmpty());
        assertNotNull(result.getOutputErrors());
        assertTrue(result.getOutputErrors().isEmpty());
        assertEquals(0, result.getIterationCount()); // Default is 0
        assertNull(result.getIterationResults());
    }

    @Test
    void shouldSetAndGetBasicProperties() {
        // Given
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 5000;

        // When
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setSuccess(false);
        result.setErrorMessage("Test error");
        result.setAgentResponse("Test agent response");
        result.setReviewResponse("Test review response");

        // Then
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertFalse(result.isSuccess());
        assertEquals("Test error", result.getErrorMessage());
        assertEquals("Test agent response", result.getAgentResponse());
        assertEquals("Test review response", result.getReviewResponse());
    }

    @Test
    void shouldSetAndGetApprovalProperties() {
        // When
        result.setApprovalRequested(true);
        result.setApproved(true);
        result.setApprovalFeedback("Approved with feedback");

        // Then
        assertTrue(result.isApprovalRequested());
        assertTrue(result.isApproved());
        assertEquals("Approved with feedback", result.getApprovalFeedback());
    }

    @Test
    void shouldCalculateDuration() {
        // Given
        result.setStartTime(1000L);
        result.setEndTime(6000L);

        // When & Then
        assertEquals(5000L, result.getDurationMs());
        assertEquals(5.0, result.getDurationSeconds(), 0.001);
    }

    @Test
    void shouldReturnZeroDurationWhenTimesNotSet() {
        // Given - times are default (0)

        // When & Then
        assertEquals(0L, result.getDurationMs());
        assertEquals(0.0, result.getDurationSeconds(), 0.001);
    }

    @Test
    void shouldReturnNegativeDurationWhenEndTimeIsBeforeStartTime() {
        // Given
        result.setStartTime(5000L);
        result.setEndTime(3000L); // End time before start time

        // When & Then
        assertEquals(-2000L, result.getDurationMs());
        assertEquals(-2.0, result.getDurationSeconds(), 0.001);
    }

    @Test
    void shouldAddAndGetGeneratedFiles() {
        // Given
        Path file1 = Paths.get("file1.txt");
        Path file2 = Paths.get("file2.txt");

        // When
        result.addGeneratedFile(file1);
        result.addGeneratedFile(file2);

        // Then
        List<Path> generatedFiles = result.getGeneratedFiles();
        assertEquals(2, generatedFiles.size());
        assertTrue(generatedFiles.contains(file1));
        assertTrue(generatedFiles.contains(file2));
        assertTrue(result.hasGeneratedFiles());
    }

    @Test
    void shouldReturnDefensiveCopyOfGeneratedFiles() {
        // Given
        Path file1 = Paths.get("file1.txt");
        result.addGeneratedFile(file1);

        // When
        List<Path> files1 = result.getGeneratedFiles();
        List<Path> files2 = result.getGeneratedFiles();

        // Then
        assertNotSame(files1, files2); // Different instances
        assertEquals(files1, files2); // Same content

        // Modifying returned list shouldn't affect original
        files1.clear();
        assertEquals(1, result.getGeneratedFiles().size());
    }

    @Test
    void shouldAddAndGetOutputErrors() {
        // Given
        String error1 = "Error 1";
        String error2 = "Error 2";

        // When
        result.addOutputError(error1);
        result.addOutputError(error2);

        // Then
        List<String> outputErrors = result.getOutputErrors();
        assertEquals(2, outputErrors.size());
        assertTrue(outputErrors.contains(error1));
        assertTrue(outputErrors.contains(error2));
        assertTrue(result.hasOutputErrors());
    }

    @Test
    void shouldReturnDefensiveCopyOfOutputErrors() {
        // Given
        String error1 = "Error 1";
        result.addOutputError(error1);

        // When
        List<String> errors1 = result.getOutputErrors();
        List<String> errors2 = result.getOutputErrors();

        // Then
        assertNotSame(errors1, errors2); // Different instances
        assertEquals(errors1, errors2); // Same content

        // Modifying returned list shouldn't affect original
        errors1.clear();
        assertEquals(1, result.getOutputErrors().size());
    }

    @Test
    void shouldCheckHasGeneratedFiles() {
        // Given - no files
        // When & Then
        assertFalse(result.hasGeneratedFiles());

        // Given - add a file
        result.addGeneratedFile(Paths.get("test.txt"));

        // When & Then
        assertTrue(result.hasGeneratedFiles());
    }

    @Test
    void shouldCheckHasOutputErrors() {
        // Given - no errors
        // When & Then
        assertFalse(result.hasOutputErrors());

        // Given - add an error
        result.addOutputError("Test error");

        // When & Then
        assertTrue(result.hasOutputErrors());
    }

    @Test
    void shouldCheckHasReview() {
        // Given - no review
        // When & Then
        assertFalse(result.hasReview());

        // Given - empty review
        result.setReviewResponse("");
        // When & Then
        assertFalse(result.hasReview());

        // Given - whitespace-only review
        result.setReviewResponse("   ");
        // When & Then
        assertFalse(result.hasReview());

        // Given - actual review content
        result.setReviewResponse("This is a review");
        // When & Then
        assertTrue(result.hasReview());
    }

    @Test
    void shouldGetAgentResponsePreview() {
        // Given - null response
        result.setAgentResponse(null);
        // When & Then
        assertNull(result.getAgentResponsePreview());

        // Given - short response
        String shortResponse = "Short response";
        result.setAgentResponse(shortResponse);
        // When & Then
        assertEquals(shortResponse, result.getAgentResponsePreview());

        // Given - long response
        StringBuilder longResponse = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            longResponse.append("x");
        }
        result.setAgentResponse(longResponse.toString());

        // When
        String preview = result.getAgentResponsePreview();

        // Then
        assertNotNull(preview);
        assertEquals(103, preview.length()); // 100 chars + "..."
        assertTrue(preview.endsWith("..."));
        assertEquals(longResponse.substring(0, 100) + "...", preview);
    }

    @Test
    void shouldSetAndGetIterationProperties() {
        // Given
        List<IterativeWorkflowStage.IterationResult> iterationResults = new ArrayList<>();

        // Mock iteration results
        IterativeWorkflowStage.IterationResult result1 = new IterativeWorkflowStage.IterationResult(
            0, "item1", Map.of("result", "success"), true, null, 1000L);
        IterativeWorkflowStage.IterationResult result2 = new IterativeWorkflowStage.IterationResult(
            1, "item2", Map.of("result", "error"), false, "Test error", 2000L);

        iterationResults.add(result1);
        iterationResults.add(result2);

        // When
        result.setIterationResults(iterationResults);
        result.setIterationCount(5);

        // Then
        assertEquals(iterationResults, result.getIterationResults());
        assertEquals(5, result.getIterationCount());
    }

    @Test
    void shouldCheckIfIsIterative() {
        // Given - no iteration results
        result.setIterationResults(null);
        // When & Then
        assertFalse(result.isIterative());

        // Given - empty iteration results
        result.setIterationResults(new ArrayList<>());
        // When & Then
        assertFalse(result.isIterative());

        // Given - non-empty iteration results
        List<IterativeWorkflowStage.IterationResult> iterationResults = new ArrayList<>();
        IterativeWorkflowStage.IterationResult mockResult = new IterativeWorkflowStage.IterationResult(
            0, "item", Map.of("result", "success"), true, null, 1000L);
        iterationResults.add(mockResult);
        result.setIterationResults(iterationResults);

        // When & Then
        assertTrue(result.isIterative());
    }

    @Test
    void shouldGetSuccessfulIterationCount() {
        // Given - no iteration results
        result.setIterationResults(null);
        // When & Then
        assertEquals(0, result.getSuccessfulIterationCount());

        // Given - mixed success/failure results
        List<IterativeWorkflowStage.IterationResult> iterationResults = new ArrayList<>();

        // Add successful result
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            0, "item1", Map.of("result", "success"), true, null, 1000L));

        // Add failed result
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            1, "item2", Map.of("result", "error"), false, "Error message", 2000L));

        // Add another successful result
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            2, "item3", Map.of("result", "success"), true, null, 1500L));

        result.setIterationResults(iterationResults);

        // When & Then
        assertEquals(2, result.getSuccessfulIterationCount());
    }

    @Test
    void shouldGetFailedIterationCount() {
        // Given - no iteration results
        result.setIterationResults(null);
        // When & Then
        assertEquals(0, result.getFailedIterationCount());

        // Given - mixed success/failure results
        List<IterativeWorkflowStage.IterationResult> iterationResults = new ArrayList<>();

        // Add successful result
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            0, "item1", Map.of("result", "success"), true, null, 1000L));

        // Add failed result
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            1, "item2", Map.of("result", "error"), false, "Error 1", 2000L));

        // Add another failed result
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            2, "item3", Map.of("result", "error"), false, "Error 2", 1500L));

        result.setIterationResults(iterationResults);

        // When & Then
        assertEquals(2, result.getFailedIterationCount());
    }

    @Test
    void shouldGenerateToString() {
        // Given
        result.setStartTime(1000L);
        result.setEndTime(4000L);
        result.setSuccess(true);
        result.setReviewResponse("Review content");

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("duration=3000ms"));
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("hasReview=true"));
    }

    @Test
    void shouldGenerateToStringWithIterativeInfo() {
        // Given
        result.setStartTime(1000L);
        result.setEndTime(4000L);
        result.setSuccess(true);
        result.setIterationCount(5);

        List<IterativeWorkflowStage.IterationResult> iterationResults = new ArrayList<>();
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            0, "item1", Map.of("result", "success"), true, null, 1000L));
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            1, "item2", Map.of("result", "error"), false, "Error", 2000L));
        iterationResults.add(new IterativeWorkflowStage.IterationResult(
            2, "item3", Map.of("result", "success"), true, null, 1500L));

        result.setIterationResults(iterationResults);

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("duration=3000ms"));
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("iterative=true"));
        assertTrue(toString.contains("iterations=5"));
        assertTrue(toString.contains("successful=2"));
        assertTrue(toString.contains("failed=1"));
    }

    @Test
    void shouldHandleComplexStageExecutionResult() {
        // Given
        result.setStartTime(1000L);
        result.setEndTime(8000L);
        result.setSuccess(true);
        result.setAgentResponse("Complex agent response with lots of content that exceeds the preview limit and continues with more text to ensure it's over 100 characters long for proper truncation testing");
        result.setReviewResponse("Detailed review feedback");
        result.setApprovalRequested(true);
        result.setApproved(true);
        result.setApprovalFeedback("Approved with minor suggestions");

        result.addGeneratedFile(Paths.get("output1.txt"));
        result.addGeneratedFile(Paths.get("output2.md"));
        result.addOutputError("Warning: Minor formatting issue");

        // When & Then
        assertEquals(7000L, result.getDurationMs());
        assertEquals(7.0, result.getDurationSeconds(), 0.001);
        assertTrue(result.hasReview());
        assertTrue(result.hasGeneratedFiles());
        assertTrue(result.hasOutputErrors());
        assertTrue(result.isApprovalRequested());
        assertTrue(result.isApproved());

        String preview = result.getAgentResponsePreview();
        assertTrue(preview.endsWith("..."));
        assertEquals(103, preview.length());

        assertEquals(2, result.getGeneratedFiles().size());
        assertEquals(1, result.getOutputErrors().size());
    }
}
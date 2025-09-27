package com.skanga.conductor.engine.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowExecutionResultTest {

    private WorkflowExecutionResult result;
    private StageExecutionResult stageResult1;
    private StageExecutionResult stageResult2;

    @BeforeEach
    void setUp() {
        result = new WorkflowExecutionResult();

        stageResult1 = new StageExecutionResult();
        stageResult1.setSuccess(true);
        stageResult1.setAgentResponse("Stage 1 response");
        stageResult1.setStartTime(1000L);
        stageResult1.setEndTime(2000L);

        stageResult2 = new StageExecutionResult();
        stageResult2.setSuccess(true);
        stageResult2.setAgentResponse("Stage 2 response");
        stageResult2.setStartTime(2100L);
        stageResult2.setEndTime(3000L);
    }

    @Test
    void shouldCreateWorkflowExecutionResultWithDefaults() {
        // Then
        assertNull(result.getWorkflowName());
        assertEquals(0L, result.getStartTime());
        assertEquals(0L, result.getEndTime());
        assertTrue(result.isSuccess()); // Default is true
        assertNull(result.getErrorMessage());
        assertNotNull(result.getStageResults());
        assertTrue(result.getStageResults().isEmpty());
    }

    @Test
    void shouldSetAndGetBasicProperties() {
        // Given
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 10000;

        // When
        result.setWorkflowName("Test Workflow");
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setSuccess(false);
        result.setErrorMessage("Test error");

        // Then
        assertEquals("Test Workflow", result.getWorkflowName());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertFalse(result.isSuccess());
        assertEquals("Test error", result.getErrorMessage());
    }

    @Test
    void shouldAddAndGetStageResults() {
        // When
        result.addStageResult("stage1", stageResult1);
        result.addStageResult("stage2", stageResult2);

        // Then
        assertEquals(2, result.getStageResults().size());
        assertTrue(result.getStageResults().containsKey("stage1"));
        assertTrue(result.getStageResults().containsKey("stage2"));
        assertEquals(stageResult1, result.getStageResults().get("stage1"));
        assertEquals(stageResult2, result.getStageResults().get("stage2"));
    }

    @Test
    void shouldGetSpecificStageResult() {
        // Given
        result.addStageResult("stage1", stageResult1);
        result.addStageResult("stage2", stageResult2);

        // When & Then
        assertEquals(stageResult1, result.getStageResult("stage1"));
        assertEquals(stageResult2, result.getStageResult("stage2"));
        assertNull(result.getStageResult("non-existent"));
    }

    @Test
    void shouldCalculateExecutionDuration() {
        // Given
        result.setStartTime(1000L);
        result.setEndTime(5000L);

        // When & Then
        assertEquals(4000L, result.getDurationMs());
        assertEquals(4.0, result.getDurationSeconds(), 0.001);
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
    void shouldGetTotalStageCount() {
        // Given
        result.addStageResult("stage1", stageResult1);
        result.addStageResult("stage2", stageResult2);

        // When & Then
        assertEquals(2, result.getStageCount());
    }

    @Test
    void shouldGetSuccessfulStageCount() {
        // Given
        stageResult1.setSuccess(true);
        stageResult2.setSuccess(false);
        result.addStageResult("stage1", stageResult1);
        result.addStageResult("stage2", stageResult2);

        // When & Then
        assertEquals(1, result.getSuccessfulStageCount());
    }

    @Test
    void shouldGetFailedStageCount() {
        // Given
        stageResult1.setSuccess(true);
        stageResult2.setSuccess(false);
        result.addStageResult("stage1", stageResult1);
        result.addStageResult("stage2", stageResult2);

        // When & Then
        assertEquals(1, result.getFailedStageCount());
    }


    @Test
    void shouldGenerateExecutionSummary() {
        // Given
        result.setWorkflowName("Test Workflow");
        result.setStartTime(1000L);
        result.setEndTime(5000L);
        result.setSuccess(true);

        stageResult1.setSuccess(true);
        stageResult2.setSuccess(false);
        stageResult2.setErrorMessage("Stage 2 failed");

        result.addStageResult("stage1", stageResult1);
        result.addStageResult("stage2", stageResult2);

        // When
        String summary = result.getSummary();

        // Then
        assertNotNull(summary);
        assertTrue(summary.contains("Test Workflow"));
        assertTrue(summary.contains("Duration: 4.00s"));
        assertTrue(summary.contains("Stages: 1/2 successful"));
        assertTrue(summary.contains("Status: SUCCESS"));
    }

    @Test
    void shouldGenerateExecutionSummaryForFailedWorkflow() {
        // Given
        result.setWorkflowName("Failed Workflow");
        result.setStartTime(1000L);
        result.setEndTime(3000L);
        result.setSuccess(false);
        result.setErrorMessage("Workflow failed");

        // When
        String summary = result.getSummary();

        // Then
        assertNotNull(summary);
        assertTrue(summary.contains("Failed Workflow"));
        assertTrue(summary.contains("Duration: 2.00s"));
        assertTrue(summary.contains("Status: FAILED"));
        assertTrue(summary.contains("Error: Workflow failed"));
    }

    @Test
    void shouldGenerateToString() {
        // Given
        result.setWorkflowName("Test Workflow");
        result.setStartTime(1000L);
        result.setEndTime(5000L);
        result.setSuccess(true);
        result.addStageResult("stage1", stageResult1);

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Test Workflow"));
        assertTrue(toString.contains("duration=4000ms"));
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("stages=1"));
    }

    @Test
    void shouldMaintainStageExecutionOrder() {
        // Given
        StageExecutionResult stage1 = new StageExecutionResult();
        StageExecutionResult stage2 = new StageExecutionResult();
        StageExecutionResult stage3 = new StageExecutionResult();

        // When - add in specific order
        result.addStageResult("third", stage3);
        result.addStageResult("first", stage1);
        result.addStageResult("second", stage2);

        // Then - LinkedHashMap should maintain insertion order
        Map<String, StageExecutionResult> stageResults = result.getStageResults();
        List<String> keys = new ArrayList<>(stageResults.keySet());
        assertEquals("third", keys.get(0));
        assertEquals("first", keys.get(1));
        assertEquals("second", keys.get(2));
    }

    @Test
    void shouldHandleNullStageResult() {
        // When
        result.addStageResult("null-stage", null);

        // Then
        assertEquals(1, result.getStageResults().size());
        assertNull(result.getStageResult("null-stage"));
    }

    @Test
    void shouldOverwriteExistingStageResult() {
        // Given
        StageExecutionResult originalResult = new StageExecutionResult();
        originalResult.setAgentResponse("Original response");

        StageExecutionResult newResult = new StageExecutionResult();
        newResult.setAgentResponse("New response");

        // When
        result.addStageResult("stage1", originalResult);
        result.addStageResult("stage1", newResult); // Overwrite

        // Then
        assertEquals(1, result.getStageResults().size());
        assertEquals("New response", result.getStageResult("stage1").getAgentResponse());
    }
}
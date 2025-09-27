package com.skanga.conductor.workflow.output;

import com.skanga.conductor.workflow.config.WorkflowDefinition;
import com.skanga.conductor.workflow.config.WorkflowStage;
import com.skanga.conductor.engine.execution.StageExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutputGenerationRequestTest {

    private static final String WORKFLOW_NAME = "TestWorkflow";
    private static final String OUTPUT_DIRECTORY = "/tmp/output";
    private static final String AGENT_RESPONSE = "This is the agent response content";
    private static final String REVIEW_RESPONSE = "This is the review response content";

    @Mock
    private WorkflowStage mockStage;

    @Mock
    private WorkflowDefinition.WorkflowSettings mockSettings;

    private StageExecutionResult stageResult;
    private Map<String, Object> variables;
    private OutputGenerationRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        stageResult = new StageExecutionResult();
        stageResult.setAgentResponse(AGENT_RESPONSE);
        stageResult.setReviewResponse(REVIEW_RESPONSE);

        variables = new HashMap<>();
        variables.put("key1", "value1");
        variables.put("key2", 123);
        variables.put("key3", true);

        request = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, stageResult, OUTPUT_DIRECTORY, variables, mockSettings
        );
    }

    @Test
    void shouldCreateOutputGenerationRequestWithAllParameters() {
        // Then
        assertEquals(WORKFLOW_NAME, request.getWorkflowName());
        assertSame(mockStage, request.getStage());
        assertSame(stageResult, request.getStageResult());
        assertEquals(OUTPUT_DIRECTORY, request.getOutputDirectory());
        assertEquals(variables, request.getVariables());
        assertSame(mockSettings, request.getSettings());
    }

    @Test
    void shouldGetPrimaryContentFromAgentResponse() {
        // When
        String primaryContent = request.getPrimaryContent();

        // Then
        assertEquals(AGENT_RESPONSE, primaryContent);
    }

    @Test
    void shouldGetReviewContentFromStageResult() {
        // When
        String reviewContent = request.getReviewContent();

        // Then
        assertEquals(REVIEW_RESPONSE, reviewContent);
    }

    @Test
    void shouldCheckIfHasReview() {
        // Given
        stageResult.setReviewResponse("Some review content");

        // When
        boolean hasReview = request.hasReview();

        // Then
        assertTrue(hasReview);
    }

    @Test
    void shouldCheckIfHasNoReview() {
        // Given
        stageResult.setReviewResponse(null);

        // When
        boolean hasReview = request.hasReview();

        // Then
        assertFalse(hasReview);
    }

    @Test
    void shouldReturnTrueForIsApprovedWhenNoApprovalRequired() {
        // Given
        stageResult.setApprovalRequested(false);

        // When
        boolean approved = request.isApproved();

        // Then
        assertTrue(approved);
    }

    @Test
    void shouldReturnTrueForIsApprovedWhenApprovalRequiredAndApproved() {
        // Given
        stageResult.setApprovalRequested(true);
        stageResult.setApproved(true);

        // When
        boolean approved = request.isApproved();

        // Then
        assertTrue(approved);
    }

    @Test
    void shouldReturnFalseForIsApprovedWhenApprovalRequiredButNotApproved() {
        // Given
        stageResult.setApprovalRequested(true);
        stageResult.setApproved(false);

        // When
        boolean approved = request.isApproved();

        // Then
        assertFalse(approved);
    }

    @Test
    void shouldCreateRequestWithNullParameters() {
        // When
        OutputGenerationRequest nullRequest = new OutputGenerationRequest(
            null, null, null, null, null, null
        );

        // Then
        assertNull(nullRequest.getWorkflowName());
        assertNull(nullRequest.getStage());
        assertNull(nullRequest.getStageResult());
        assertNull(nullRequest.getOutputDirectory());
        assertNull(nullRequest.getVariables());
        assertNull(nullRequest.getSettings());
    }

    @Test
    void shouldHandleNullStageResult() {
        // Given
        OutputGenerationRequest requestWithNullResult = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, null, OUTPUT_DIRECTORY, variables, mockSettings
        );

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            requestWithNullResult.getPrimaryContent();
        });
    }

    @Test
    void shouldHandleEmptyVariables() {
        // Given
        Map<String, Object> emptyVariables = new HashMap<>();
        OutputGenerationRequest requestWithEmptyVars = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, stageResult, OUTPUT_DIRECTORY, emptyVariables, mockSettings
        );

        // When & Then
        assertEquals(emptyVariables, requestWithEmptyVars.getVariables());
        assertTrue(requestWithEmptyVars.getVariables().isEmpty());
    }

    @Test
    void shouldHandleComplexVariableTypes() {
        // Given
        Map<String, Object> complexVariables = new HashMap<>();
        complexVariables.put("string", "value");
        complexVariables.put("number", 42);
        complexVariables.put("double", 3.14);
        complexVariables.put("boolean", true);
        complexVariables.put("null", null);
        complexVariables.put("map", Map.of("nested", "value"));

        OutputGenerationRequest complexRequest = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, stageResult, OUTPUT_DIRECTORY, complexVariables, mockSettings
        );

        // When & Then
        assertEquals(complexVariables, complexRequest.getVariables());
        assertEquals("value", complexRequest.getVariables().get("string"));
        assertEquals(42, complexRequest.getVariables().get("number"));
        assertEquals(3.14, complexRequest.getVariables().get("double"));
        assertTrue((Boolean) complexRequest.getVariables().get("boolean"));
        assertNull(complexRequest.getVariables().get("null"));
    }

    @Test
    void shouldHandleSpecialCharactersInContent() {
        // Given
        String specialContent = "Content with unicode: 你好世界 🚀, symbols: @#$%^&*()";
        String specialReview = "Review with newlines\nand\ttabs";

        stageResult.setAgentResponse(specialContent);
        stageResult.setReviewResponse(specialReview);

        // When
        String primaryContent = request.getPrimaryContent();
        String reviewContent = request.getReviewContent();

        // Then
        assertEquals(specialContent, primaryContent);
        assertEquals(specialReview, reviewContent);
    }

    @Test
    void shouldHandleVeryLongContent() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("This is a very long piece of content. ");
        }

        stageResult.setAgentResponse(longContent.toString());

        // When
        String primaryContent = request.getPrimaryContent();

        // Then
        assertEquals(longContent.toString(), primaryContent);
        assertTrue(primaryContent.length() > 100000);
    }

    @Test
    void shouldHandleEmptyOutputDirectory() {
        // Given
        OutputGenerationRequest requestWithEmptyDir = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, stageResult, "", variables, mockSettings
        );

        // When & Then
        assertEquals("", requestWithEmptyDir.getOutputDirectory());
    }

    @Test
    void shouldHandleWhitespaceOutputDirectory() {
        // Given
        String whitespaceDir = "   \n\t  ";
        OutputGenerationRequest requestWithWhitespaceDir = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, stageResult, whitespaceDir, variables, mockSettings
        );

        // When & Then
        assertEquals(whitespaceDir, requestWithWhitespaceDir.getOutputDirectory());
    }

    @Test
    void shouldHandleDifferentPathFormats() {
        // Test different path formats
        String[] pathFormats = {
            "/tmp/output",
            "C:\\Users\\test\\output",
            "./relative/path",
            "../parent/output",
            "~/home/output",
            "output"
        };

        for (String path : pathFormats) {
            // When
            OutputGenerationRequest pathRequest = new OutputGenerationRequest(
                WORKFLOW_NAME, mockStage, stageResult, path, variables, mockSettings
            );

            // Then
            assertEquals(path, pathRequest.getOutputDirectory());
        }
    }

    @Test
    void shouldCreateMultipleIndependentRequests() {
        // Given
        StageExecutionResult result2 = new StageExecutionResult();
        result2.setAgentResponse("Different content");

        Map<String, Object> variables2 = Map.of("different", "variables");

        // When
        OutputGenerationRequest request2 = new OutputGenerationRequest(
            "DifferentWorkflow", mockStage, result2, "/different/path", variables2, mockSettings
        );

        // Then
        assertNotSame(request, request2);
        assertEquals(WORKFLOW_NAME, request.getWorkflowName());
        assertEquals("DifferentWorkflow", request2.getWorkflowName());
        assertEquals(AGENT_RESPONSE, request.getPrimaryContent());
        assertEquals("Different content", request2.getPrimaryContent());
    }

    @Test
    void shouldHandleStageResultWithNullContent() {
        // Given
        StageExecutionResult nullContentResult = new StageExecutionResult();
        nullContentResult.setAgentResponse(null);
        nullContentResult.setReviewResponse(null);

        OutputGenerationRequest nullContentRequest = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, nullContentResult, OUTPUT_DIRECTORY, variables, mockSettings
        );

        // When & Then
        assertNull(nullContentRequest.getPrimaryContent());
        assertNull(nullContentRequest.getReviewContent());
        assertFalse(nullContentRequest.hasReview());
    }

    @Test
    void shouldHandleStageResultWithEmptyContent() {
        // Given
        StageExecutionResult emptyContentResult = new StageExecutionResult();
        emptyContentResult.setAgentResponse("");
        emptyContentResult.setReviewResponse("");

        OutputGenerationRequest emptyContentRequest = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, emptyContentResult, OUTPUT_DIRECTORY, variables, mockSettings
        );

        // When & Then
        assertEquals("", emptyContentRequest.getPrimaryContent());
        assertEquals("", emptyContentRequest.getReviewContent());
        assertFalse(emptyContentRequest.hasReview()); // Empty string is considered "no review"
    }

    @Test
    void shouldHandleApprovalStatesCorrectly() {
        // Test case 1: No approval requested
        StageExecutionResult noApprovalResult = new StageExecutionResult();
        noApprovalResult.setApprovalRequested(false);
        OutputGenerationRequest noApprovalRequest = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, noApprovalResult, OUTPUT_DIRECTORY, variables, mockSettings
        );
        assertTrue(noApprovalRequest.isApproved());

        // Test case 2: Approval requested and granted
        StageExecutionResult approvedResult = new StageExecutionResult();
        approvedResult.setApprovalRequested(true);
        approvedResult.setApproved(true);
        OutputGenerationRequest approvedRequest = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, approvedResult, OUTPUT_DIRECTORY, variables, mockSettings
        );
        assertTrue(approvedRequest.isApproved());

        // Test case 3: Approval requested but denied
        StageExecutionResult deniedResult = new StageExecutionResult();
        deniedResult.setApprovalRequested(true);
        deniedResult.setApproved(false);
        OutputGenerationRequest deniedRequest = new OutputGenerationRequest(
            WORKFLOW_NAME, mockStage, deniedResult, OUTPUT_DIRECTORY, variables, mockSettings
        );
        assertFalse(deniedRequest.isApproved());
    }

    @Test
    void shouldMaintainVariableIntegrity() {
        // Given
        Map<String, Object> originalVariables = new HashMap<>(variables);

        // When - modify original variables after creating request
        variables.put("newKey", "newValue");

        // Then - request should maintain reference to the same map
        assertEquals(variables, request.getVariables());
        assertTrue(request.getVariables().containsKey("newKey"));
    }
}
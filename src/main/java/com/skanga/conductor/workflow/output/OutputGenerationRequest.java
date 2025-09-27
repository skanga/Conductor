package com.skanga.conductor.workflow.output;

import com.skanga.conductor.workflow.config.WorkflowDefinition;
import com.skanga.conductor.workflow.config.WorkflowStage;
import com.skanga.conductor.engine.execution.StageExecutionResult;

import java.util.Map;

/**
 * Request for generating output files from a workflow stage execution.
 * Contains all the information needed to generate appropriate output files.
 */
public class OutputGenerationRequest {

    private final String workflowName;
    private final WorkflowStage stage;
    private final StageExecutionResult stageResult;
    private final String outputDirectory;
    private final Map<String, Object> variables;
    private final WorkflowDefinition.WorkflowSettings settings;

    public OutputGenerationRequest(String workflowName, WorkflowStage stage, StageExecutionResult stageResult,
                                 String outputDirectory, Map<String, Object> variables,
                                 WorkflowDefinition.WorkflowSettings settings) {
        this.workflowName = workflowName;
        this.stage = stage;
        this.stageResult = stageResult;
        this.outputDirectory = outputDirectory;
        this.variables = variables;
        this.settings = settings;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public WorkflowStage getStage() {
        return stage;
    }

    public StageExecutionResult getStageResult() {
        return stageResult;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public WorkflowDefinition.WorkflowSettings getSettings() {
        return settings;
    }

    /**
     * Gets the primary content to write to files (usually the agent response).
     */
    public String getPrimaryContent() {
        return stageResult.getAgentResponse();
    }

    /**
     * Gets the review content if available.
     */
    public String getReviewContent() {
        return stageResult.getReviewResponse();
    }

    /**
     * Checks if this stage has review content.
     */
    public boolean hasReview() {
        return stageResult.hasReview();
    }

    /**
     * Checks if this stage was approved (if approval was required).
     */
    public boolean isApproved() {
        return !stageResult.isApprovalRequested() || stageResult.isApproved();
    }
}
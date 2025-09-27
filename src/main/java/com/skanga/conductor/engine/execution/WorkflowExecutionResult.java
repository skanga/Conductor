package com.skanga.conductor.engine.execution;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of a complete workflow execution.
 * Contains timing information, success status, and results from each stage.
 */
public class WorkflowExecutionResult {

    private String workflowName;
    private long startTime;
    private long endTime;
    private boolean success = true;
    private String errorMessage;
    private final Map<String, StageExecutionResult> stageResults = new LinkedHashMap<>();

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, StageExecutionResult> getStageResults() {
        return stageResults;
    }

    public void addStageResult(String stageName, StageExecutionResult result) {
        stageResults.put(stageName, result);
    }

    public StageExecutionResult getStageResult(String stageName) {
        return stageResults.get(stageName);
    }

    /**
     * Gets the total execution duration in milliseconds.
     */
    public long getDurationMs() {
        return endTime - startTime;
    }

    /**
     * Gets the total execution duration in seconds.
     */
    public double getDurationSeconds() {
        return getDurationMs() / 1000.0;
    }

    /**
     * Gets the number of stages that were executed.
     */
    public int getStageCount() {
        return stageResults.size();
    }

    /**
     * Gets the number of stages that succeeded.
     */
    public int getSuccessfulStageCount() {
        return (int) stageResults.values().stream()
            .mapToLong(result -> result.isSuccess() ? 1 : 0)
            .sum();
    }

    /**
     * Gets the number of stages that failed.
     */
    public int getFailedStageCount() {
        return getStageCount() - getSuccessfulStageCount();
    }

    /**
     * Returns a summary of the execution result.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Workflow: ").append(workflowName).append("\n");
        sb.append("Duration: ").append(String.format("%.2f", getDurationSeconds())).append("s\n");
        sb.append("Status: ").append(success ? "SUCCESS" : "FAILED").append("\n");
        sb.append("Stages: ").append(getSuccessfulStageCount()).append("/").append(getStageCount()).append(" successful\n");

        if (!success && errorMessage != null) {
            sb.append("Error: ").append(errorMessage).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "WorkflowExecutionResult{" +
               "workflowName='" + workflowName + '\'' +
               ", duration=" + getDurationMs() + "ms" +
               ", success=" + success +
               ", stages=" + getStageCount() +
               '}';
    }
}
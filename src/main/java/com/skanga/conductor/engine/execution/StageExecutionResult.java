package com.skanga.conductor.engine.execution;

import com.skanga.conductor.workflow.config.IterativeWorkflowStage;

/**
 * Result of executing a single workflow stage.
 * Contains timing information, agent responses, and success status.
 * Also supports iterative stage results with multiple iterations.
 */
public class StageExecutionResult {

    private long startTime;
    private long endTime;
    private boolean success = true;
    private String errorMessage;
    private String agentResponse;
    private String reviewResponse;
    private boolean approvalRequested = false;
    private boolean approved = false;
    private String approvalFeedback;
    private java.util.List<java.nio.file.Path> generatedFiles = new java.util.ArrayList<>();
    private java.util.List<String> outputErrors = new java.util.ArrayList<>();

    // Iteration support
    private java.util.List<IterativeWorkflowStage.IterationResult> iterationResults;
    private int iterationCount = 0;

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

    public String getAgentResponse() {
        return agentResponse;
    }

    public void setAgentResponse(String agentResponse) {
        this.agentResponse = agentResponse;
    }

    public String getReviewResponse() {
        return reviewResponse;
    }

    public void setReviewResponse(String reviewResponse) {
        this.reviewResponse = reviewResponse;
    }

    public boolean isApprovalRequested() {
        return approvalRequested;
    }

    public void setApprovalRequested(boolean approvalRequested) {
        this.approvalRequested = approvalRequested;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getApprovalFeedback() {
        return approvalFeedback;
    }

    public void setApprovalFeedback(String approvalFeedback) {
        this.approvalFeedback = approvalFeedback;
    }

    public java.util.List<java.nio.file.Path> getGeneratedFiles() {
        return new java.util.ArrayList<>(generatedFiles);
    }

    public void addGeneratedFile(java.nio.file.Path filePath) {
        this.generatedFiles.add(filePath);
    }

    public java.util.List<String> getOutputErrors() {
        return new java.util.ArrayList<>(outputErrors);
    }

    public void addOutputError(String error) {
        this.outputErrors.add(error);
    }

    public boolean hasGeneratedFiles() {
        return !generatedFiles.isEmpty();
    }

    public boolean hasOutputErrors() {
        return !outputErrors.isEmpty();
    }

    /**
     * Gets the execution duration in milliseconds.
     */
    public long getDurationMs() {
        return endTime - startTime;
    }

    /**
     * Gets the execution duration in seconds.
     */
    public double getDurationSeconds() {
        return getDurationMs() / 1000.0;
    }

    /**
     * Checks if this stage had a review step.
     */
    public boolean hasReview() {
        return reviewResponse != null && !reviewResponse.trim().isEmpty();
    }

    /**
     * Gets a preview of the agent response (first 100 characters).
     */
    public String getAgentResponsePreview() {
        if (agentResponse == null) {
            return null;
        }
        return agentResponse.length() > 100 ?
            agentResponse.substring(0, 100) + "..." :
            agentResponse;
    }

    /**
     * Gets the iteration results for iterative stages.
     */
    public java.util.List<IterativeWorkflowStage.IterationResult> getIterationResults() {
        return iterationResults;
    }

    /**
     * Sets the iteration results for iterative stages.
     */
    public void setIterationResults(java.util.List<IterativeWorkflowStage.IterationResult> iterationResults) {
        this.iterationResults = iterationResults;
    }

    /**
     * Gets the number of iterations executed.
     */
    public int getIterationCount() {
        return iterationCount;
    }

    /**
     * Sets the number of iterations executed.
     */
    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    /**
     * Checks if this stage was executed iteratively.
     */
    public boolean isIterative() {
        return iterationResults != null && !iterationResults.isEmpty();
    }

    /**
     * Gets the number of successful iterations.
     */
    public int getSuccessfulIterationCount() {
        if (iterationResults == null) {
            return 0;
        }
        return (int) iterationResults.stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum();
    }

    /**
     * Gets the number of failed iterations.
     */
    public int getFailedIterationCount() {
        if (iterationResults == null) {
            return 0;
        }
        return (int) iterationResults.stream().mapToLong(r -> !r.isSuccessful() ? 1 : 0).sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StageExecutionResult{");
        sb.append("duration=").append(getDurationMs()).append("ms");
        sb.append(", success=").append(success);
        sb.append(", hasReview=").append(hasReview());

        if (isIterative()) {
            sb.append(", iterative=true");
            sb.append(", iterations=").append(getIterationCount());
            sb.append(", successful=").append(getSuccessfulIterationCount());
            sb.append(", failed=").append(getFailedIterationCount());
        }

        sb.append('}');
        return sb.toString();
    }
}
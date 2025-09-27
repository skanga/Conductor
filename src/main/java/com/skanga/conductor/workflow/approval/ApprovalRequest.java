package com.skanga.conductor.workflow.approval;

/**
 * Represents a request for human approval during workflow execution.
 * Contains all the information needed for a human to make an approval decision.
 */
public class ApprovalRequest {

    private final String workflowName;
    private final String stageName;
    private final String stageDescription;
    private final String generatedContent;
    private final String reviewContent;
    private final long requestTime;

    public ApprovalRequest(String workflowName, String stageName, String stageDescription,
                          String generatedContent, String reviewContent) {
        this.workflowName = workflowName;
        this.stageName = stageName;
        this.stageDescription = stageDescription;
        this.generatedContent = generatedContent;
        this.reviewContent = reviewContent;
        this.requestTime = System.currentTimeMillis();
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageDescription() {
        return stageDescription;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public String getReviewContent() {
        return reviewContent;
    }

    public long getRequestTime() {
        return requestTime;
    }

    /**
     * Gets a preview of the generated content (first 200 characters).
     */
    public String getContentPreview() {
        if (generatedContent == null) {
            return "No content generated";
        }
        return generatedContent.length() > 200 ?
            generatedContent.substring(0, 200) + "..." :
            generatedContent;
    }

    /**
     * Gets a preview of the review content (first 150 characters).
     */
    public String getReviewPreview() {
        if (reviewContent == null) {
            return "No review available";
        }
        return reviewContent.length() > 150 ?
            reviewContent.substring(0, 150) + "..." :
            reviewContent;
    }

    @Override
    public String toString() {
        return "ApprovalRequest{" +
               "workflow='" + workflowName + '\'' +
               ", stage='" + stageName + '\'' +
               ", requestTime=" + requestTime +
               '}';
    }
}
package com.skanga.conductor.workflow.approval;

/**
 * Represents the response to an approval request.
 * Contains the decision and any feedback from the human reviewer.
 */
public class ApprovalResponse {

    public enum Decision {
        APPROVE,
        REJECT,
        REVISE
    }

    private final Decision decision;
    private final String feedback;
    private final long responseTime;

    public ApprovalResponse(Decision decision, String feedback) {
        this.decision = decision;
        this.feedback = feedback;
        this.responseTime = System.currentTimeMillis();
    }

    public static ApprovalResponse approve() {
        return new ApprovalResponse(Decision.APPROVE, null);
    }

    public static ApprovalResponse approve(String feedback) {
        return new ApprovalResponse(Decision.APPROVE, feedback);
    }

    public static ApprovalResponse reject(String reason) {
        return new ApprovalResponse(Decision.REJECT, reason);
    }

    public static ApprovalResponse revise(String revisionRequests) {
        return new ApprovalResponse(Decision.REVISE, revisionRequests);
    }

    public Decision getDecision() {
        return decision;
    }

    public String getFeedback() {
        return feedback;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public boolean isApproved() {
        return decision == Decision.APPROVE;
    }

    public boolean isRejected() {
        return decision == Decision.REJECT;
    }

    public boolean needsRevision() {
        return decision == Decision.REVISE;
    }

    @Override
    public String toString() {
        return "ApprovalResponse{" +
               "decision=" + decision +
               ", feedback='" + feedback + '\'' +
               ", responseTime=" + responseTime +
               '}';
    }
}
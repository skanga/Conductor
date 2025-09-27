package com.skanga.conductor.workflow.approval;

import com.skanga.conductor.exception.ApprovalException;
import com.skanga.conductor.exception.ApprovalTimeoutException;

/**
 * Interface for handling human approval requests during workflow execution.
 * Different implementations can provide console-based, web-based, or API-based approval mechanisms.
 */
public interface HumanApprovalHandler {

    /**
     * Requests human approval for a workflow stage.
     * This method should block until a human provides approval or the timeout is reached.
     *
     * @param request the approval request containing stage information and generated content
     * @param timeoutMs the maximum time to wait for approval in milliseconds
     * @return the approval response from the human reviewer
     * @throws ApprovalTimeoutException if no response is received within the timeout
     * @throws ApprovalException if there's an error during the approval process
     */
    ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
            throws ApprovalTimeoutException, ApprovalException;

    /**
     * Checks if this handler supports interactive approval.
     * Non-interactive handlers might auto-approve or always reject.
     *
     * @return true if this handler can interact with humans
     */
    boolean isInteractive();

    /**
     * Gets a description of this approval handler for logging/display purposes.
     *
     * @return a human-readable description of the handler
     */
    String getDescription();
}
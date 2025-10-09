package com.skanga.conductor.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.Duration;
import java.util.Properties;

/**
 * Workflow execution configuration settings.
 * <p>
 * Provides access to workflow-specific configuration including
 * approval timeouts, stage timeouts, and execution limits.
 * </p>
 *
 * @since 1.3.0
 */
public class WorkflowConfig extends ConfigurationProvider {

    public WorkflowConfig(Properties properties) {
        super(properties);
    }

    /**
     * Returns the default timeout for human approvals.
     * <p>
     * When an approval request has no explicit timeout,
     * this default value is used.
     * </p>
     *
     * @return default approval timeout
     */
    public Duration getApprovalDefaultTimeout() {
        return getDuration("conductor.workflow.approval.default.timeout", Duration.ofMinutes(5));
    }

    /**
     * Returns the maximum allowed timeout for human approvals.
     * <p>
     * This prevents approvals from blocking indefinitely.
     * </p>
     *
     * @return maximum approval timeout
     */
    public Duration getApprovalMaxTimeout() {
        Duration maxTimeout = getDuration("conductor.workflow.approval.max.timeout", Duration.ofHours(24));
        if (maxTimeout.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("Approval max timeout cannot exceed 7 days");
        }
        return maxTimeout;
    }

    /**
     * Returns the default timeout for workflow stages.
     * <p>
     * When a stage has no explicit timeout, this default value is used.
     * </p>
     *
     * @return default stage timeout
     */
    public Duration getStageDefaultTimeout() {
        return getDuration("conductor.workflow.stage.default.timeout", Duration.ofMinutes(10));
    }

    /**
     * Returns the maximum number of stages allowed in a workflow.
     * <p>
     * This prevents resource exhaustion from overly complex workflows.
     * </p>
     *
     * @return maximum stage count
     */
    @Min(value = 1, message = "Max stages must be at least 1")
    @Max(value = 1000, message = "Max stages cannot exceed 1000")
    public int getMaxStagesPerWorkflow() {
        return getInt("conductor.workflow.max.stages", 100);
    }

    /**
     * Returns the maximum depth for stage dependencies.
     * <p>
     * This prevents circular dependencies and overly deep dependency chains.
     * </p>
     *
     * @return maximum dependency depth
     */
    @Min(value = 1, message = "Max dependency depth must be at least 1")
    @Max(value = 100, message = "Max dependency depth cannot exceed 100")
    public int getMaxDependencyDepth() {
        return getInt("conductor.workflow.max.dependency.depth", 20);
    }
}

package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents a single stage within a workflow.
 * Each stage can have multiple agents, approval requirements, and outputs.
 */
public class WorkflowStage {

    private String name;
    private String description;

    @JsonProperty("depends_on")
    private List<String> dependsOn;

    private boolean parallel = false;

    private Map<String, String> agents;

    private StageApproval approval;

    private List<String> outputs;

    @JsonProperty("retry_limit")
    private Integer retryLimit;

    @JsonProperty("iteration")
    private IterationConfig iteration;

    // Default constructor for Jackson
    public WorkflowStage() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public Map<String, String> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, String> agents) {
        this.agents = agents;
    }

    public StageApproval getApproval() {
        return approval;
    }

    public void setApproval(StageApproval approval) {
        this.approval = approval;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public Integer getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(Integer retryLimit) {
        this.retryLimit = retryLimit;
    }

    public IterationConfig getIteration() {
        return iteration;
    }

    public void setIteration(IterationConfig iteration) {
        this.iteration = iteration;
    }

    /**
     * Configuration for human approval requirements in this stage.
     */
    public static class StageApproval {
        private boolean required = false;

        @JsonProperty("per_item")
        private boolean perItem = false;

        private String timeout = "5m";

        @JsonProperty("auto_approve")
        private boolean autoApprove = false;

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public boolean isPerItem() {
            return perItem;
        }

        public void setPerItem(boolean perItem) {
            this.perItem = perItem;
        }

        public String getTimeout() {
            return timeout;
        }

        public void setTimeout(String timeout) {
            this.timeout = timeout;
        }

        public boolean isAutoApprove() {
            return autoApprove;
        }

        public void setAutoApprove(boolean autoApprove) {
            this.autoApprove = autoApprove;
        }
    }

    /**
     * Validates that this stage definition is complete and valid.
     */
    public void validate() throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }

        if (agents == null || agents.isEmpty()) {
            throw new IllegalArgumentException("Stage '" + name + "' must have at least one agent");
        }

        // Validate agent references
        for (Map.Entry<String, String> agentEntry : agents.entrySet()) {
            String role = agentEntry.getKey();
            String agentId = agentEntry.getValue();

            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("Agent role cannot be empty in stage '" + name + "'");
            }

            if (agentId == null || agentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Agent ID cannot be empty for role '" + role + "' in stage '" + name + "'");
            }
        }

        // Validate iteration configuration if present
        if (iteration != null) {
            try {
                iteration.validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid iteration configuration in stage '" + name + "': " + e.getMessage());
            }
        }

        // Validate approval configuration for iterative stages
        if (isIterative() && approval != null && approval.isPerItem()) {
            if (iteration.isParallelEnabled()) {
                throw new IllegalArgumentException("Per-item approval is not supported with parallel iteration in stage '" + name + "'");
            }
        }
    }

    /**
     * Gets the primary agent for this stage (the first one defined).
     */
    public String getPrimaryAgentId() {
        if (agents == null || agents.isEmpty()) {
            return null;
        }
        return agents.values().iterator().next();
    }

    /**
     * Gets the agent ID for a specific role.
     */
    public String getAgentId(String role) {
        if (agents == null) {
            return null;
        }
        return agents.get(role);
    }

    /**
     * Checks if this stage requires human approval.
     */
    public boolean requiresApproval() {
        return approval != null && approval.isRequired();
    }

    /**
     * Checks if this stage is configured for iteration.
     */
    public boolean isIterative() {
        return iteration != null;
    }

    /**
     * Checks if this stage should execute iterations in parallel.
     */
    public boolean isParallelIteration() {
        return isIterative() && iteration.isParallelEnabled();
    }

    /**
     * Gets the effective parallelism for this stage.
     * Returns true if either stage-level parallel is enabled OR iteration parallel is enabled.
     */
    public boolean isEffectivelyParallel() {
        return parallel || isParallelIteration();
    }

    /**
     * Gets the maximum concurrent executions for iterative stages.
     */
    public int getMaxConcurrentIterations() {
        if (!isIterative()) {
            return 1;
        }
        return iteration.getEffectiveMaxConcurrent();
    }
}
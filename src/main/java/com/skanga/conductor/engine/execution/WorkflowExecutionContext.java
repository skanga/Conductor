package com.skanga.conductor.engine.execution;

import com.skanga.conductor.workflow.config.AgentConfigCollection;
import com.skanga.conductor.workflow.config.WorkflowContext;
import com.skanga.conductor.workflow.config.WorkflowDefinition;

/**
 * Context object that holds all information needed during workflow execution.
 * This includes the loaded configurations, runtime context, and input parameters.
 */
public class WorkflowExecutionContext {

    private final WorkflowDefinition workflowDefinition;
    private final AgentConfigCollection agentConfig;
    private final WorkflowContext context;
    private final String[] inputs;

    public WorkflowExecutionContext(WorkflowDefinition workflowDefinition,
                                  AgentConfigCollection agentConfig,
                                  WorkflowContext context,
                                  String... inputs) {
        this.workflowDefinition = workflowDefinition;
        this.agentConfig = agentConfig;
        this.context = context;
        this.inputs = inputs != null ? inputs : new String[0];
    }

    public WorkflowDefinition getWorkflowDefinition() {
        return workflowDefinition;
    }

    public AgentConfigCollection getAgentConfig() {
        return agentConfig;
    }

    public WorkflowContext getContext() {
        return context;
    }

    public String[] getInputs() {
        return inputs;
    }

    /**
     * Gets the primary input (usually the topic or main parameter).
     */
    public String getPrimaryInput() {
        return inputs.length > 0 ? inputs[0] : null;
    }

    /**
     * Gets a specific input by index.
     */
    public String getInput(int index) {
        return index < inputs.length ? inputs[index] : null;
    }

    /**
     * Gets the number of inputs provided.
     */
    public int getInputCount() {
        return inputs.length;
    }
}
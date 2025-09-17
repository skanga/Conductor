package com.skanga.conductor.orchestration;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.provider.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrator that uses an LLM-based planner to decompose the user request into tasks,
 * spins up implicit subagents for each task, executes them in order, and then executes them
 * sequentially with placeholder substitution.
 * <p>
 * For each TaskDefinition we create an LLMSubAgent with the given name + a UUID suffix (for
 * uniqueness),  wiring the provided workerProvider to the created subagents.
 * TODO: If we want to register explicit agents, adjust this to register them in the registry.
 * <p>
 * Example:
 * If the planner generates:
 * [
 * { "name": "draft_chapter", "promptTemplate": "Write a draft of the chapter: {{user_request}}" },
 * { "name": "edit_chapter", "promptTemplate": "Edit the following draft: {{prev_output}}" },
 * { "name": "critique_chapter", "promptTemplate": "Critique this chapter: {{draft_chapter}}" }
 * ]
 * <p>
 * Execution will flow like this:
 * <p>
 * draft_chapter → prompt contains {{user_request}} → filled in with user’s request.
 * edit_chapter → prompt contains {{prev_output}} → filled with draft_chapter output.
 * critique_chapter → prompt contains {{draft_chapter}} → filled directly from stored outputs.
 */
public class PlannerOrchestrator extends Orchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PlannerOrchestrator.class);

    public PlannerOrchestrator(SubAgentRegistry registry, MemoryStore memoryStore) {
        super(registry, memoryStore);
    }

    /**
     * Plan and execute a user request.
     * <p>
     * Decomposition drives subagents: LLMPlanner.plan() asks a planner LLM to return a JSON array of
     * TaskDefinition. PlannerOrchestrator creates an implicit LLMSubAgent for each task and runs them
     * sequentially. Each created subagent uses the MemoryStore, so their outputs are persisted and will
     * be rehydrated on later runs.
     *
     * @param userRequest     user prompt to decompose
     * @param plannerProvider LLMProvider used for planning/decomposition
     * @param workerProvider  LLMProvider used for actual worker subagents (writer/editor/etc.)
     * @return list of TaskResult produced by executing each planned subagent
     */

    public List<TaskResult> runWorkflow(
            String workflowId,
            String userRequest,
            LLMProvider plannerProvider,
            LLMProvider workerProvider,
            MemoryStore store) throws ConductorException {

        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId cannot be null or empty");
        }
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("userRequest cannot be null or empty");
        }
        if (plannerProvider == null) {
            throw new IllegalArgumentException("plannerProvider cannot be null");
        }
        if (workerProvider == null) {
            throw new IllegalArgumentException("workerProvider cannot be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }

        // Check if we already have a saved plan
        try {
            TaskDefinition[] existingPlan = store.loadPlan(workflowId);

            if (existingPlan == null) {
                // First run → plan + execute
                logger.info("No saved plan found. Running full planning for workflowId={}", workflowId);
                return planAndExecute(workflowId, userRequest, plannerProvider, workerProvider, store);
            } else {
                // Resume run → just execute remaining tasks
                logger.info("Resuming from saved plan for workflowId={}", workflowId);
                return resumeWorkflow(workflowId, userRequest, workerProvider, store, existingPlan);
            }
        } catch (SQLException e) {
            throw new ConductorException("Failed to load plan for workflowId=" + workflowId, e);
        }
    }

    public List<TaskResult> planAndExecute(
            String workflowId,
            String userRequest,
            LLMProvider plannerProvider,
            LLMProvider workerProvider,
            MemoryStore store) throws ConductorException {

        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId cannot be null or empty");
        }
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("userRequest cannot be null or empty");
        }
        if (plannerProvider == null) {
            throw new IllegalArgumentException("plannerProvider cannot be null");
        }
        if (workerProvider == null) {
            throw new IllegalArgumentException("workerProvider cannot be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }

        LLMPlanner planner = new LLMPlanner(plannerProvider);
        TaskDefinition[] plan = planner.plan(userRequest);

        // Save plan for resumability
        try {
            store.savePlan(workflowId, plan);
        } catch (SQLException e) {
            throw new ConductorException("Failed to save plan for workflowId=" + workflowId, e);
        }

        return executePlan(workflowId, userRequest, plan, workerProvider, store);
    }

    // Resume workflow without replanning by using saved state
    public List<TaskResult> resumeWorkflow(
            String workflowId,
            String userRequest,
            LLMProvider workerProvider,
            MemoryStore store,
            TaskDefinition[] plan) throws ConductorException {

        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId cannot be null or empty");
        }
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("userRequest cannot be null or empty");
        }
        if (workerProvider == null) {
            throw new IllegalArgumentException("workerProvider cannot be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }

        TaskDefinition[] finalPlan = plan;
        if (finalPlan == null) {
            try {
                finalPlan = store.loadPlan(workflowId);
            } catch (SQLException e) {
                throw new ConductorException("Failed to load plan for workflowId=" + workflowId, e);
            }
            if (finalPlan == null) {
                throw new IllegalStateException("No saved plan found for workflowId=" + workflowId);
            }
        }
        return executePlan(workflowId, userRequest, plan, workerProvider, store);
    }

    // Shared execution logic
    private List<TaskResult> executePlan(
            String workflowId,
            String userRequest,
            TaskDefinition[] plan,
            LLMProvider workerProvider,
            MemoryStore store) throws ConductorException {

        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId cannot be null or empty");
        }
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("userRequest cannot be null or empty");
        }
        if (plan == null) {
            throw new IllegalArgumentException("plan cannot be null");
        }
        if (workerProvider == null) {
            throw new IllegalArgumentException("workerProvider cannot be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }

        List<TaskResult> results = new ArrayList<>();

        // Load previously completed task outputs
        Map<String, String> taskOutputs = store.loadTaskOutputs(workflowId);

        // Carry forward last output for chaining
        String prevOutput = taskOutputs.isEmpty() ? "" :
                taskOutputs.values().stream().reduce((a, b) -> b).orElse("");

        for (TaskDefinition td : plan) {
            if (taskOutputs.containsKey(td.name)) {
                logger.info("Skipping completed task: {}", td.name);
                results.add(new TaskResult(taskOutputs.get(td.name), true, null));
                continue;
            }

            SubAgent agent = null;
            try {
                agent = createImplicitAgent(
                        td.name,
                        td.description,
                        workerProvider,
                        td.promptTemplate
                );
            } catch (SQLException e) {
                throw new ConductorException("Failed to create implicit agent", e);
            }

            String agentPrompt = td.promptTemplate
                    .replace("{{user_request}}", userRequest)
                    .replace("{{prev_output}}", prevOutput != null ? prevOutput : "");

            for (Map.Entry<String, String> entry : taskOutputs.entrySet()) {
                agentPrompt = agentPrompt.replace(
                        "{{" + entry.getKey() + "}}", entry.getValue()
                );
            }

            TaskResult res = agent.execute(new TaskInput(agentPrompt, null));
            results.add(res);

            String output = res.output() != null ? res.output() : "";
            prevOutput = output;
            taskOutputs.put(td.name, output);

            // Persist output for resumability
            store.saveTaskOutput(workflowId, td.name, output);
        }

        return results;
    }
}

package com.skanga.conductor.orchestration;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.workflow.templates.PromptTemplateEngine;
import com.skanga.conductor.utils.ValidationUtils;
import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * Orchestrator that uses an LLM-based planner to decompose the user request into tasks,
 * spins up implicit subagents for each task, executes them in order, and then executes them
 * sequentially with placeholder substitution.
 * <p>
 * For each TaskDefinition we create an LLMSubAgent with the given name + a UUID suffix (for
 * uniqueness), wiring the provided workerProvider to the created subagents.
 * TODO:Do we want to register explicit agents? If so we need to add code to register them in the registry.
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
    private final PromptTemplateEngine templateEngine;
    private final TaskDependencyAnalyzer dependencyAnalyzer;
    private final ParallelTaskExecutor parallelExecutor;
    private final ApplicationConfig.ParallelismConfig parallelismConfig;

    public PlannerOrchestrator(SubAgentRegistry registry, MemoryStore memoryStore) {
        super(registry, memoryStore);
        this.templateEngine = new PromptTemplateEngine();
        this.dependencyAnalyzer = new TaskDependencyAnalyzer();
        this.parallelismConfig = ApplicationConfig.getInstance().getParallelismConfig();

        // Create parallel executor with configuration
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelismConfig.getMaxThreads());
        this.parallelExecutor = new ParallelTaskExecutor(
            forkJoinPool,
            parallelismConfig.getMaxParallelTasksPerBatch(),
            parallelismConfig.getTaskTimeoutSeconds()
        );
    }

    /**
     * Common validation for workflow execution parameters.
     */
    private void validateWorkflowParams(String workflowId, String userRequest, MemoryStore memoryStore) {
        ValidationUtils.requireNonBlank(workflowId, "workflowId");
        ValidationUtils.requireNonBlank(userRequest, "userRequest");
        ValidationUtils.requireNonNull(memoryStore, "memory store");
    }

    /**
     * Common validation for workflow execution parameters with providers.
     */
    private void validateWorkflowParamsWithProviders(String workflowId, String userRequest,
                                                   LLMProvider workerProvider, MemoryStore memoryStore) {
        validateWorkflowParams(workflowId, userRequest, memoryStore);
        ValidationUtils.requireNonNull(workerProvider, "workerProvider");
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
     * @return list of ExecutionResult produced by executing each planned subagent
     */

    public List<ExecutionResult> runWorkflow(
            String workflowId,
            String userRequest,
            LLMProvider plannerProvider,
            LLMProvider workerProvider,
            MemoryStore memoryStore) throws ConductorException {

        validateWorkflowParamsWithProviders(workflowId, userRequest, workerProvider, memoryStore);
        ValidationUtils.requireNonNull(plannerProvider, "plannerProvider");

        // Check if we already have a saved plan
        try {
            var existingPlan = memoryStore.loadPlan(workflowId);

            if (existingPlan.isPresent()) {
                // Resume run → just execute remaining tasks
                logger.info("Resuming from saved plan for workflowId={}", workflowId);
                return resumeWorkflow(workflowId, userRequest, workerProvider, memoryStore, existingPlan.get());
            } else {
                // First run → plan + execute
                logger.info("No saved plan found. Running full planning for workflowId={}", workflowId);
                return planAndExecute(workflowId, userRequest, plannerProvider, workerProvider, memoryStore);
            }
        } catch (SQLException e) {
            throw new ConductorException("Failed to load plan for workflowId=" + workflowId, e);
        }
    }

    public List<ExecutionResult> planAndExecute(
            String workflowId,
            String userRequest,
            LLMProvider plannerProvider,
            LLMProvider workerProvider,
            MemoryStore memoryStore) throws ConductorException {

        validateWorkflowParamsWithProviders(workflowId, userRequest, workerProvider, memoryStore);
        ValidationUtils.requireNonNull(plannerProvider, "planner provider");

        LLMPlanMaker planner = new LLMPlanMaker(plannerProvider);
        TaskDefinition[] plan = planner.plan(userRequest);

        // Save plan for resumability
        try {
            memoryStore.savePlan(workflowId, plan);
        } catch (SQLException e) {
            throw new ConductorException("Failed to save plan for workflowId=" + workflowId, e);
        }

        return executePlan(workflowId, userRequest, plan, workerProvider, memoryStore);
    }

    // Resume workflow without replanning by using saved state
    public List<ExecutionResult> resumeWorkflow(
            String workflowId,
            String userRequest,
            LLMProvider workerProvider,
            MemoryStore memoryStore,
            TaskDefinition[] plan) throws ConductorException {

        validateWorkflowParamsWithProviders(workflowId, userRequest, workerProvider, memoryStore);

        TaskDefinition[] finalPlan = plan;
        if (finalPlan == null) {
            try {
                finalPlan = memoryStore.loadPlan(workflowId)
                    .orElseThrow(() -> new IllegalStateException("No saved plan found for workflowId=" + workflowId));
            } catch (SQLException e) {
                throw new ConductorException("Failed to load plan for workflowId=" + workflowId, e);
            }
        }
        return executePlan(workflowId, userRequest, plan, workerProvider, memoryStore);
    }

    // Shared execution logic with parallel support
    private List<ExecutionResult> executePlan(
            String workflowId,
            String userRequest,
            TaskDefinition[] plan,
            LLMProvider workerProvider,
            MemoryStore memoryStore) throws ConductorException {

        validateWorkflowParamsWithProviders(workflowId, userRequest, workerProvider, memoryStore);
        ValidationUtils.requireNonNull(plan, "plan");

        if (plan.length == 0) {
            return Collections.emptyList();
        }

        // Decide whether to use parallel or sequential execution
        if (shouldUseParallelExecution(plan)) {
            return executeInParallel(workflowId, userRequest, plan, workerProvider, memoryStore);
        } else {
            return executeSequentially(workflowId, userRequest, plan, workerProvider, memoryStore);
        }
    }

    /**
     * Determines if parallel execution should be used based on configuration and plan analysis.
     */
    private boolean shouldUseParallelExecution(TaskDefinition[] plan) {
        if (!parallelismConfig.isEnabled()) {
            logger.debug("Parallel execution disabled by configuration");
            return false;
        }

        if (plan.length < parallelismConfig.getMinTasksForParallelExecution()) {
            logger.debug("Too few tasks ({}) for parallel execution (minimum: {})",
                       plan.length, parallelismConfig.getMinTasksForParallelExecution());
            return false;
        }

        // Analyze potential for parallelism
        TaskDependencyAnalyzer.ParallelismAnalysis analysis = dependencyAnalyzer.analyzeParallelismBenefit(plan);
        if (analysis.getParallelismRatio() > parallelismConfig.getParallelismThreshold()) {
            logger.info("Using parallel execution: {}", analysis);
            return true;
        } else {
            logger.debug("Insufficient parallelism benefit ({}), using sequential execution", analysis);
            return false;
        }
    }

    /**
     * Executes tasks in parallel using dependency-aware batching.
     */
    private List<ExecutionResult> executeInParallel(
            String workflowId,
            String userRequest,
            TaskDefinition[] plan,
            LLMProvider workerProvider,
            MemoryStore memoryStore) throws ConductorException {

        try {
            // Analyze dependencies and create execution batches
            List<List<TaskDefinition>> taskBatches = dependencyAnalyzer.groupTasksIntoBatches(plan);

            logger.info("Executing {} tasks in {} parallel batches for workflow '{}'",
                      plan.length, taskBatches.size(), workflowId);

            // Create agent factory function
            Function<TaskDefinition, SubAgent> agentFactory = task -> {
                try {
                    return createImplicitAgent(task.taskName, task.taskDescription, workerProvider, task.promptTemplate);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create agent for task: " + task.taskName, e);
                }
            };

            // Execute batches in parallel
            return parallelExecutor.executeBatches(workflowId, userRequest, taskBatches, agentFactory, memoryStore);

        } catch (Exception e) {
            if (parallelismConfig.isFallbackToSequentialEnabled()) {
                logger.warn("Parallel execution failed, falling back to sequential execution: {}", e.getMessage());
                return executeSequentially(workflowId, userRequest, plan, workerProvider, memoryStore);
            } else {
                throw new ConductorException("Parallel task execution failed", e);
            }
        }
    }

    /**
     * Executes tasks sequentially (original implementation).
     */
    private List<ExecutionResult> executeSequentially(
            String workflowId,
            String userRequest,
            TaskDefinition[] plan,
            LLMProvider workerProvider,
            MemoryStore memoryStore) throws ConductorException {

        logger.info("Executing {} tasks sequentially for workflow '{}'", plan.length, workflowId);

        List<ExecutionResult> results = new ArrayList<>();

        // Load previously completed task outputs
        Map<String, String> taskOutputs = memoryStore.loadTaskOutputs(workflowId);

        // Carry forward last output for chaining
        String prevOutput = taskOutputs.isEmpty() ? "" :
                taskOutputs.values().stream().reduce((a, b) -> b).orElse("");

        for (TaskDefinition td : plan) {
            if (taskOutputs.containsKey(td.taskName)) {
                logger.info("Skipping completed task: {}", td.taskName);
                results.add(new ExecutionResult(true, taskOutputs.get(td.taskName), null));
                continue;
            }

            SubAgent agent = null;
            try {
                agent = createImplicitAgent(
                        td.taskName,
                        td.taskDescription,
                        workerProvider,
                        td.promptTemplate
                );
            } catch (SQLException e) {
                throw new ConductorException("Failed to create implicit agent", e);
            }

            // Create template variables map
            Map<String, Object> templateVars = new HashMap<>();
            templateVars.put("user_request", userRequest);
            templateVars.put("prev_output", prevOutput != null ? prevOutput : "");

            // Add all task outputs as template variables
            for (Map.Entry<String, String> entry : taskOutputs.entrySet()) {
                templateVars.put(entry.getKey(), entry.getValue());
            }

            // Use PromptTemplateEngine for consistent templating
            String agentPrompt = templateEngine.renderString(td.promptTemplate, templateVars);

            ExecutionResult res = agent.execute(new ExecutionInput(agentPrompt, null));
            results.add(res);

            String output = res.output() != null ? res.output() : "";
            prevOutput = output;
            taskOutputs.put(td.taskName, output);

            // Persist output for resumability
            memoryStore.saveTaskOutput(workflowId, td.taskName, output);
        }

        return results;
    }
}

package com.skanga.conductor.engine;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ApprovalException;
import com.skanga.conductor.exception.ApprovalTimeoutException;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.workflow.config.*;
import com.skanga.conductor.workflow.templates.AgentFactory;
import com.skanga.conductor.workflow.templates.PromptTemplateEngine;
import com.skanga.conductor.workflow.approval.HumanApprovalHandler;
import com.skanga.conductor.workflow.approval.ConsoleApprovalHandler;
import com.skanga.conductor.workflow.approval.ApprovalRequest;
import com.skanga.conductor.workflow.approval.ApprovalResponse;
import com.skanga.conductor.workflow.output.*;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.engine.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main execution engine for YAML-defined workflows.
 * <p>
 * Orchestrates the entire workflow from configuration loading to execution completion.
 * Implements the WorkflowEngine interface to provide standardized workflow execution
 * capabilities with YAML configuration support.
 * </p>
 */
public class YamlWorkflowEngine implements WorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(YamlWorkflowEngine.class);

    private final WorkflowConfigLoader configLoader;
    private final AgentFactory agentFactory;
    private final PromptTemplateEngine promptEngine;
    private final Map<String, SubAgent> agentCache;
    private final ParallelStageExecutor parallelExecutor;
    // private final IterativeStageExecutor iterativeExecutor;
    private final VariableSubstitution variableSubstitution;

    // Loaded configurations
    private WorkflowDefinition workflowDefinition;
    private AgentConfigCollection agentConfig;
    private WorkflowContext context;

    // Runtime components
    private Orchestrator orchestrator;
    private MemoryStore memoryStore;
    private HumanApprovalHandler approvalHandler;
    private FileOutputGenerator outputGenerator;

    // Execution state
    private Map<String, StageExecutionResult> completedStageResults = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public YamlWorkflowEngine() {
        this.configLoader = new WorkflowConfigLoader();
        this.agentFactory = new AgentFactory();
        this.promptEngine = new PromptTemplateEngine();
        this.agentCache = new ConcurrentHashMap<>();
        this.outputGenerator = new StandardFileOutputGenerator();
        this.variableSubstitution = new VariableSubstitution();
        this.parallelExecutor = new ParallelStageExecutor(
            Runtime.getRuntime().availableProcessors() * 2, // Max parallelism
            300_000L // 5 minute default timeout
        );
        // this.iterativeExecutor = new IterativeStageExecutor(
        //     agentFactory, promptEngine, variableSubstitution, null // Will be set when approval handler is configured
        // );
    }

    /**
     * Loads a workflow definition from a YAML file.
     */
    public YamlWorkflowEngine loadWorkflow(String workflowPath) throws IOException {
        logger.info("Loading workflow configuration from: {}", workflowPath);
        this.workflowDefinition = configLoader.loadWorkflow(workflowPath);
        return this;
    }

    /**
     * Loads agent configurations from a YAML file.
     */
    public YamlWorkflowEngine loadAgents(String agentsPath) throws IOException {
        logger.info("Loading agent configurations from: {}", agentsPath);
        this.agentConfig = configLoader.loadAgents(agentsPath);
        return this;
    }

    /**
     * Loads runtime context from a YAML file.
     */
    public YamlWorkflowEngine loadContext(String contextPath) throws IOException {
        logger.info("Loading workflow context from: {}", contextPath);
        this.context = configLoader.loadContext(contextPath);
        return this;
    }

    /**
     * Sets up the orchestrator with memory store.
     */
    public YamlWorkflowEngine withOrchestrator(Orchestrator orchestrator, MemoryStore memoryStore) {
        this.orchestrator = orchestrator;
        this.memoryStore = memoryStore;
        return this;
    }

    /**
     * Sets up a human approval handler for workflow stages that require approval.
     */
    public YamlWorkflowEngine withApprovalHandler(HumanApprovalHandler approvalHandler) {
        this.approvalHandler = approvalHandler;
        return this;
    }

    /**
     * Sets up a custom file output generator for workflow results.
     */
    public YamlWorkflowEngine withOutputGenerator(FileOutputGenerator outputGenerator) {
        this.outputGenerator = outputGenerator;
        return this;
    }

    /**
     * Executes the loaded workflow with the given input.
     * Internal method that returns the concrete WorkflowExecutionResult.
     */
    public WorkflowExecutionResult executeInternal(String... inputs) throws ConductorException {
        validateConfiguration();

        logger.info("Starting workflow execution: {}", workflowDefinition.getMetadata().getName());

        WorkflowExecutionContext executionContext = new WorkflowExecutionContext(
            workflowDefinition, agentConfig, context, inputs);

        WorkflowExecutionResult result = new WorkflowExecutionResult();
        result.setWorkflowName(workflowDefinition.getMetadata().getName());
        result.setStartTime(System.currentTimeMillis());

        try {
            // Create execution plan with dependency resolution
            StageExecutionPlan executionPlan = new StageExecutionPlan(workflowDefinition.getStages());

            if (executionPlan.hasParallelExecution()) {
                logger.info("Workflow supports parallel execution with {} waves and max parallelism of {}",
                    executionPlan.getWaveCount(), executionPlan.getMaxParallelism());
            } else {
                logger.info("Workflow will execute sequentially with {} waves", executionPlan.getWaveCount());
            }

            // Execute each wave in order
            for (StageExecutionPlan.ExecutionWave wave : executionPlan.getExecutionWaves()) {
                logger.info("Executing wave {}: {} stage(s)", wave.getWaveNumber(), wave.getStageCount());

                if (wave.hasParallelStages()) {
                    // Execute stages in parallel
                    Map<String, StageExecutionResult> waveResults = executeWaveInParallel(wave, executionContext);

                    // Add all wave results to the overall result
                    for (Map.Entry<String, StageExecutionResult> entry : waveResults.entrySet()) {
                        String stageName = entry.getKey();
                        StageExecutionResult stageResult = entry.getValue();
                        result.addStageResult(stageName, stageResult);

                        // Track completed stages for content aggregation
                        if (stageResult.isSuccess()) {
                            completedStageResults.put(stageName, stageResult);
                        }

                        // Check if any stage failed and should stop execution
                        if (!stageResult.isSuccess()) {
                            // Find the stage object to check failure behavior
                            WorkflowStage failedStage = wave.getStages().stream()
                                .filter(s -> s.getName().equals(stageName))
                                .findFirst()
                                .orElse(null);

                            if (!shouldContinueOnFailure(failedStage)) {
                                logger.error("Stage '{}' failed in parallel wave, stopping workflow execution", stageName);
                                result.setSuccess(false);
                                result.setErrorMessage("Stage '" + stageName + "' failed: " + stageResult.getErrorMessage());
                                return result;
                            }
                        }
                    }
                } else {
                    // Execute single stage
                    WorkflowStage stage = wave.getStages().get(0);
                    logger.info("Executing stage: {}", stage.getName());

                    StageExecutionResult stageResult = executeStage(stage, executionContext);
                    result.addStageResult(stage.getName(), stageResult);

                    // Track completed stages for content aggregation
                    if (stageResult.isSuccess()) {
                        completedStageResults.put(stage.getName(), stageResult);
                    }

                    // Check if stage failed and should stop execution
                    if (!stageResult.isSuccess() && !shouldContinueOnFailure(stage)) {
                        logger.error("Stage '{}' failed, stopping workflow execution", stage.getName());
                        result.setSuccess(false);
                        result.setErrorMessage("Stage '" + stage.getName() + "' failed: " + stageResult.getErrorMessage());
                        break;
                    }
                }
            }

            if (result.isSuccess()) {
                logger.info("Workflow execution completed successfully");
            }

        } catch (Exception e) {
            logger.error("Workflow execution failed with exception", e);
            result.setSuccess(false);
            result.setErrorMessage("Execution failed: " + e.getMessage());
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * Executes the loaded workflow without additional inputs.
     * Internal method that returns the concrete WorkflowExecutionResult.
     */
    public WorkflowExecutionResult executeInternal() throws ConductorException {
        return executeInternal(new String[0]);
    }

    /**
     * Executes a wave of stages in parallel by submitting individual stage tasks.
     */
    private Map<String, StageExecutionResult> executeWaveInParallel(StageExecutionPlan.ExecutionWave wave,
                                                                  WorkflowExecutionContext context) throws ConductorException {
        Map<String, StageExecutionResult> results = new ConcurrentHashMap<>();

        // Create individual stage execution tasks
        Map<String, java.util.concurrent.Future<StageExecutionResult>> futures = new ConcurrentHashMap<>();

        for (WorkflowStage stage : wave.getStages()) {
            java.util.concurrent.Future<StageExecutionResult> future =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return executeStage(stage, context);
                    } catch (ConductorException e) {
                        throw new RuntimeException(e);
                    }
                });
            futures.put(stage.getName(), future);
        }

        // Wait for all stages to complete
        for (Map.Entry<String, java.util.concurrent.Future<StageExecutionResult>> entry : futures.entrySet()) {
            try {
                StageExecutionResult result = entry.getValue().get(300, java.util.concurrent.TimeUnit.SECONDS);
                results.put(entry.getKey(), result);
            } catch (Exception e) {
                throw new ConductorException("Failed to execute stage '" + entry.getKey() + "' in parallel: " + e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Executes a single workflow stage, handling both regular and iterative stages.
     */
    private StageExecutionResult executeStage(WorkflowStage stage, WorkflowExecutionContext context)
            throws ConductorException {

        // Check if this is an iterative stage
        if (stage.isIterative()) {
            return executeIterativeStage(stage, context);
        } else {
            return executeRegularStage(stage, context);
        }
    }

    /**
     * Executes an iterative workflow stage.
     */
    private StageExecutionResult executeIterativeStage(WorkflowStage stage, WorkflowExecutionContext context)
            throws ConductorException {
        logger.info("Executing iterative stage: {}", stage.getName());

        // Convert to IterativeWorkflowStage
        IterativeWorkflowStage iterativeStage = convertToIterativeStage(stage);

        // Iterative functionality temporarily disabled
        throw new ConductorException("Iterative workflow functionality is temporarily disabled");
    }

    /**
     * Executes a regular (non-iterative) workflow stage.
     */
    private StageExecutionResult executeRegularStage(WorkflowStage stage, WorkflowExecutionContext context)
            throws ConductorException {

        StageExecutionResult result = new StageExecutionResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            // Get the primary agent for this stage
            String primaryAgentId = stage.getPrimaryAgentId();
            if (primaryAgentId == null) {
                throw new ConductorException("Stage '" + stage.getName() + "' has no agents defined");
            }

            SubAgent primaryAgent = getOrCreateAgent(primaryAgentId);

            // Prepare the prompt using context and template
            String prompt = prepareStagePrompt(stage, primaryAgentId, context);

            // Execute the agent
            ExecutionInput executionInput = new ExecutionInput(prompt, null);
            ExecutionResult executionResult = primaryAgent.execute(executionInput);

            result.setAgentResponse(executionResult.output());
            result.setSuccess(true);

            // If stage has a reviewer agent, execute review
            String reviewerAgentId = stage.getAgentId("reviewer");
            if (reviewerAgentId != null) {
                SubAgent reviewerAgent = getOrCreateAgent(reviewerAgentId);
                String reviewPrompt = prepareReviewPrompt(stage, reviewerAgentId, executionResult.output(), context);

                ExecutionInput reviewInput = new ExecutionInput(reviewPrompt, null);
                ExecutionResult reviewResult = reviewerAgent.execute(reviewInput);

                result.setReviewResponse(reviewResult.output());
            }

            // Check if human approval is required for this stage
            if (stage.requiresApproval()) {
                result.setApprovalRequested(true);
                boolean approved = requestHumanApproval(stage, result, context);
                result.setApproved(approved);

                if (!approved) {
                    result.setSuccess(false);
                    result.setErrorMessage("Stage rejected by human reviewer");
                    logger.warn("Stage '{}' was rejected by human reviewer", stage.getName());
                    return result;
                }
            }

            // Generate output files if the stage was successful and approved (if required)
            if (result.isSuccess() && (!result.isApprovalRequested() || result.isApproved())) {
                generateStageOutputFiles(stage, result, context);
            }

            logger.info("Stage '{}' executed successfully", stage.getName());

        } catch (Exception e) {
            logger.error("Stage '{}' execution failed", stage.getName(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * Converts a WorkflowStage to an IterativeWorkflowStage.
     */
    private IterativeWorkflowStage convertToIterativeStage(WorkflowStage stage) {
        IterativeWorkflowStage iterativeStage = new IterativeWorkflowStage();

        // Copy all properties from the original stage
        iterativeStage.setName(stage.getName());
        iterativeStage.setDescription(stage.getDescription());
        iterativeStage.setDependsOn(stage.getDependsOn());
        iterativeStage.setParallel(stage.isParallel());
        iterativeStage.setAgents(stage.getAgents());
        iterativeStage.setApproval(stage.getApproval());
        iterativeStage.setOutputs(stage.getOutputs());
        iterativeStage.setRetryLimit(stage.getRetryLimit());
        iterativeStage.setIteration(stage.getIteration());

        return iterativeStage;
    }

    /**
     * Converts an IterativeStageResult to a regular StageExecutionResult.
     */
    private StageExecutionResult convertIterativeResult(IterativeWorkflowStage.IterativeStageResult iterativeResult) {
        StageExecutionResult result = new StageExecutionResult();
        result.setStartTime(System.currentTimeMillis() - iterativeResult.getTotalExecutionTimeMs());
        result.setEndTime(System.currentTimeMillis());
        result.setSuccess(iterativeResult.isAllSuccessful());

        // Aggregate all iteration results into the response
        StringBuilder aggregatedResponse = new StringBuilder();
        Map<String, Object> aggregatedOutputs = iterativeResult.getAggregatedOutputs();

        for (IterativeWorkflowStage.IterationResult iteration : iterativeResult.getIterationResults()) {
            if (iteration.isSuccessful()) {
                aggregatedResponse.append("=== Iteration ").append(iteration.getIndex()).append(" ===\n");

                // Add each agent result from this iteration
                for (Map.Entry<String, Object> entry : iteration.getResult().entrySet()) {
                    aggregatedResponse.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                aggregatedResponse.append("\n");
            }
        }

        result.setAgentResponse(aggregatedResponse.toString());

        // Store iteration metadata
        result.setIterationResults(iterativeResult.getIterationResults());
        result.setIterationCount(iterativeResult.getIterationResults().size());

        if (!iterativeResult.isAllSuccessful()) {
            long failedCount = iterativeResult.getIterationResults().stream()
                .mapToLong(r -> r.isSuccessful() ? 0 : 1)
                .sum();
            result.setErrorMessage(failedCount + " of " +
                iterativeResult.getIterationResults().size() + " iterations failed");
        }

        return result;
    }

    /**
     * Prepares the prompt for a stage execution using templates and context.
     */
    private String prepareStagePrompt(WorkflowStage stage, String agentId, WorkflowExecutionContext context) {
        AgentDefinition agentDef = agentConfig.getAgent(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        String templateId = agentDef.getPromptTemplate();
        if (templateId == null) {
            throw new IllegalArgumentException("Agent '" + agentId + "' has no prompt template");
        }

        AgentConfigCollection.PromptTemplate template = agentConfig.getPromptTemplate(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt template not found: " + templateId));

        // Build template variables from context
        Map<String, Object> variables = buildTemplateVariables(context);

        // Special handling for final review stage - aggregate all previous content
        if (stage.getName().contains("final-review") || stage.getName().contains("book-review")) {
            String aggregatedContent = buildAggregatedBookContent(context);
            variables.put("content_to_review", aggregatedContent);
        }

        // Render the prompt using the template engine
        return promptEngine.renderPrompt(template, variables);
    }

    /**
     * Prepares a review prompt for an agent.
     */
    private String prepareReviewPrompt(WorkflowStage stage, String reviewerAgentId, String contentToReview,
                                     WorkflowExecutionContext context) {
        AgentDefinition reviewerDef = agentConfig.getAgent(reviewerAgentId)
            .orElseThrow(() -> new IllegalArgumentException("Reviewer agent not found: " + reviewerAgentId));
        String templateId = reviewerDef.getPromptTemplate();
        AgentConfigCollection.PromptTemplate template = agentConfig.getPromptTemplate(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt template not found: " + templateId));

        Map<String, Object> variables = buildTemplateVariables(context);
        variables.put("content_to_review", contentToReview);

        return promptEngine.renderPrompt(template, variables);
    }

    /**
     * Builds template variables from the execution context.
     */
    private Map<String, Object> buildTemplateVariables(WorkflowExecutionContext context) {
        Map<String, Object> variables = new HashMap<>();

        // Add workflow variables
        if (workflowDefinition.getVariables() != null) {
            variables.putAll(workflowDefinition.getVariables());
        }

        // Add input arguments
        String[] inputs = context.getInputs();
        if (inputs.length > 0) {
            variables.put("topic", inputs[0]);
        }
        if (inputs.length > 1) {
            variables.put("author", inputs[1]);
        }

        // Add workflow settings
        if (workflowDefinition.getSettings() != null) {
            WorkflowDefinition.WorkflowSettings settings = workflowDefinition.getSettings();
            variables.put("target_words_per_chapter", settings.getTargetWordsPerChapter());
            variables.put("max_words_per_chapter", settings.getMaxWordsPerChapter());
        }

        // Add context variables if available
        if (this.context != null) {
            variables.put("target_audience", this.context.getString("input.target_audience", "professionals"));
        }

        return variables;
    }

    /**
     * Builds aggregated content from all previous stages for final review.
     */
    private String buildAggregatedBookContent(WorkflowExecutionContext context) {
        StringBuilder content = new StringBuilder();

        // Get all completed stage results
        Map<String, StageExecutionResult> stageResults = this.completedStageResults;

        // Add title and subtitle
        if (stageResults.containsKey("title-generation")) {
            StageExecutionResult titleResult = stageResults.get("title-generation");
            content.append("# Book Title and Subtitle\n\n");
            content.append(titleResult.getAgentResponse()).append("\n\n");
        }

        // Add table of contents
        if (stageResults.containsKey("toc-generation")) {
            StageExecutionResult tocResult = stageResults.get("toc-generation");
            content.append("# Table of Contents\n\n");
            content.append(tocResult.getAgentResponse()).append("\n\n");
        }

        // Add all chapters
        for (Map.Entry<String, StageExecutionResult> entry : stageResults.entrySet()) {
            if (entry.getKey().startsWith("chapter-")) {
                content.append("---\n\n");
                content.append(entry.getValue().getAgentResponse()).append("\n\n");
            }
        }

        return content.toString();
    }

    /**
     * Gets or creates an agent based on its configuration.
     */
    private SubAgent getOrCreateAgent(String agentId) throws ConductorException {
        return agentCache.computeIfAbsent(agentId, id -> {
            try {
                AgentDefinition agentDef = agentConfig.getAgent(id)
                    .orElseThrow(() -> new RuntimeException("Agent definition not found: " + id));

                return agentFactory.createAgent(agentDef, orchestrator);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create agent: " + id, e);
            }
        });
    }

    /**
     * Validates that all required configurations are loaded.
     */
    private void validateConfiguration() {
        if (workflowDefinition == null) {
            throw new IllegalStateException("Workflow definition not loaded");
        }
        if (agentConfig == null) {
            throw new IllegalStateException("Agent configuration not loaded");
        }
        if (orchestrator == null) {
            throw new IllegalStateException("Orchestrator not configured");
        }
    }

    /**
     * Generates output files for a workflow stage.
     */
    private void generateStageOutputFiles(WorkflowStage stage, StageExecutionResult result, WorkflowExecutionContext context) {
        if (outputGenerator == null) {
            logger.warn("No output generator configured, skipping file generation for stage '{}'", stage.getName());
            return;
        }

        if (stage.getOutputs() == null || stage.getOutputs().isEmpty()) {
            logger.debug("No output files configured for stage '{}'", stage.getName());
            return;
        }

        try {
            // Get output directory from workflow settings
            String outputDir = getOutputDirectory();

            // Create output generation request
            Map<String, Object> variables = buildTemplateVariables(context);

            OutputGenerationRequest request = new OutputGenerationRequest(
                workflowDefinition.getMetadata().getName(),
                stage,
                result,
                outputDir,
                variables,
                workflowDefinition.getSettings()
            );

            // Generate output files
            logger.info("Generating output files for stage '{}' in directory: {}", stage.getName(), outputDir);
            OutputGenerationResult outputResult = outputGenerator.generateOutput(request);

            // Track generated files and errors in stage result
            for (java.nio.file.Path file : outputResult.getGeneratedFiles()) {
                result.addGeneratedFile(file);
            }

            for (String error : outputResult.getErrors()) {
                result.addOutputError(error);
            }

            if (outputResult.isSuccess()) {
                logger.info("Generated {} file(s) for stage '{}' in {}ms",
                    outputResult.getFileCount(), stage.getName(), outputResult.getGenerationTimeMs());
            } else {
                logger.warn("File generation partially failed for stage '{}': {} error(s)",
                    stage.getName(), outputResult.getErrors().size());
            }

        } catch (Exception e) {
            String error = "Failed to generate output files for stage '" + stage.getName() + "': " + e.getMessage();
            logger.error(error, e);
            result.addOutputError(error);
        }
    }

    /**
     * Gets the output directory for the workflow.
     */
    private String getOutputDirectory() {
        if (workflowDefinition.getSettings() != null && workflowDefinition.getSettings().getOutputDir() != null) {
            // Substitute variables in output directory path
            Map<String, Object> variables = new HashMap<>();
            variables.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            variables.put("workflow", workflowDefinition.getMetadata().getName());

            return substituteVariablesInString(workflowDefinition.getSettings().getOutputDir(), variables);
        }

        // Default output directory
        return "data/workflow-output/" + workflowDefinition.getMetadata().getName();
    }

    /**
     * Substitutes variables in a string using the provided variable map.
     */
    private String substituteVariablesInString(String input, Map<String, Object> variables) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Requests human approval for a workflow stage.
     */
    private boolean requestHumanApproval(WorkflowStage stage, StageExecutionResult result, WorkflowExecutionContext context) {
        if (approvalHandler == null) {
            logger.warn("Stage '{}' requires approval but no approval handler is configured. Auto-approving.", stage.getName());
            return true;
        }

        if (!approvalHandler.isInteractive()) {
            logger.info("Using non-interactive approval handler: {}", approvalHandler.getDescription());
        }

        try {
            ApprovalRequest request = new ApprovalRequest(
                workflowDefinition.getMetadata().getName(),
                stage.getName(),
                stage.getDescription(),
                result.getAgentResponse(),
                result.getReviewResponse()
            );

            // Parse timeout from stage configuration
            String timeoutString = stage.getApproval().getTimeout();
            long timeoutMs = ConsoleApprovalHandler.parseTimeout(timeoutString);

            logger.info("Requesting human approval for stage '{}' with timeout {}ms", stage.getName(), timeoutMs);

            ApprovalResponse response = approvalHandler.requestApproval(request, timeoutMs);

            // Store approval feedback
            if (response.getFeedback() != null) {
                result.setApprovalFeedback(response.getFeedback());
            }

            logger.info("Approval response for stage '{}': {} ({})",
                stage.getName(), response.getDecision(), response.getFeedback());

            return response.isApproved();

        } catch (ApprovalTimeoutException e) {
            logger.error("Approval request timed out for stage '{}': {}", stage.getName(), e.getMessage());
            result.setErrorMessage("Approval request timed out: " + e.getMessage());
            return false;
        } catch (ApprovalException e) {
            logger.error("Approval request failed for stage '{}': {}", stage.getName(), e.getMessage());
            result.setErrorMessage("Approval request failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Determines if workflow execution should continue after a stage failure.
     */
    private boolean shouldContinueOnFailure(WorkflowStage stage) {
        // For now, always stop on failure
        // Future enhancement: add continue_on_failure configuration
        return false;
    }

    // === WorkflowEngine Interface Implementation ===

    @Override
    public WorkflowEngine.WorkflowResult execute(String... inputs) throws ConductorException {
        if (closed) {
            throw new IllegalStateException("Workflow engine has been closed");
        }

        logger.info("Executing YAML workflow with {} inputs", inputs.length);

        try {
            WorkflowExecutionResult result = executeInternal(inputs);
            return new YamlWorkflowResult(result);
        } catch (Exception e) {
            logger.error("YAML workflow execution failed", e);
            throw new ConductorException("YAML workflow execution failed", e);
        }
    }

    @Override
    public WorkflowEngine.WorkflowResult execute(WorkflowDefinition definition, WorkflowContext context) throws ConductorException {
        if (closed) {
            throw new IllegalStateException("Workflow engine has been closed");
        }
        if (definition == null) {
            throw new IllegalArgumentException("workflow definition cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("workflow context cannot be null");
        }

        logger.info("Executing YAML workflow: {}", definition.getMetadata().getName());

        // Set the loaded configuration
        this.workflowDefinition = definition;
        this.context = context;

        try {
            WorkflowExecutionResult result = executeInternal();
            return new YamlWorkflowResult(result);
        } catch (Exception e) {
            logger.error("YAML workflow execution failed for definition: {}", definition.getMetadata().getName(), e);
            throw new ConductorException("YAML workflow execution failed", e);
        }
    }

    @Override
    public boolean isReady() {
        return !closed &&
               workflowDefinition != null &&
               agentConfig != null &&
               context != null &&
               orchestrator != null;
    }

    @Override
    public String getEngineName() {
        return "YamlWorkflowEngine";
    }

    @Override
    public EngineStatus getStatus() {
        return new YamlEngineStatus();
    }

    // === WorkflowEngine Interface Implementation Classes ===

    /**
     * Implementation of WorkflowResult interface that adapts the internal WorkflowExecutionResult.
     */
    private static class YamlWorkflowResult implements WorkflowEngine.WorkflowResult {
        private final WorkflowExecutionResult internalResult;

        public YamlWorkflowResult(WorkflowExecutionResult internalResult) {
            this.internalResult = internalResult;
        }

        @Override
        public boolean isSuccess() {
            return internalResult.isSuccess();
        }

        @Override
        public String getErrorMessage() {
            return internalResult.getErrorMessage();
        }

        @Override
        public long getStartTime() {
            return internalResult.getStartTime();
        }

        @Override
        public long getEndTime() {
            return internalResult.getEndTime();
        }

        @Override
        public long getTotalExecutionTimeMs() {
            return internalResult.getEndTime() - internalResult.getStartTime();
        }

        @Override
        public String getWorkflowName() {
            return internalResult.getWorkflowName();
        }

        @Override
        public Object getOutput() {
            return internalResult.getStageResults();
        }
    }

    /**
     * Implementation of EngineStatus interface for YAML workflow engine.
     */
    private class YamlEngineStatus implements EngineStatus {
        @Override
        public boolean isConfigured() {
            return workflowDefinition != null && agentConfig != null && context != null;
        }

        @Override
        public List<String> getValidationMessages() {
            List<String> messages = new ArrayList<>();
            if (workflowDefinition == null) {
                messages.add("Workflow definition not loaded");
            }
            if (agentConfig == null) {
                messages.add("Agent configuration not loaded");
            }
            if (context == null) {
                messages.add("Workflow context not loaded");
            }
            if (orchestrator == null) {
                messages.add("Orchestrator not configured");
            }
            if (closed) {
                messages.add("Engine has been closed");
            }
            return messages;
        }

        @Override
        public int getLoadedWorkflowCount() {
            return workflowDefinition != null ? 1 : 0;
        }

        @Override
        public int getActiveExecutionCount() {
            return closed ? 0 : (completedStageResults.isEmpty() ? 0 : 1);
        }

        @Override
        public Map<String, Object> getMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("engine_type", "yaml");
            metadata.put("workflow_loaded", workflowDefinition != null);
            metadata.put("agent_config_loaded", agentConfig != null);
            metadata.put("context_loaded", context != null);
            metadata.put("orchestrator_available", orchestrator != null);
            metadata.put("approval_handler_available", approvalHandler != null);
            metadata.put("parallel_executor_shutdown", parallelExecutor != null ? parallelExecutor.isShutdown() : true);
            metadata.put("completed_stages_count", completedStageResults.size());
            metadata.put("agent_cache_size", agentCache.size());
            metadata.put("closed", closed);
            if (workflowDefinition != null) {
                metadata.put("workflow_name", workflowDefinition.getMetadata().getName());
                metadata.put("workflow_version", workflowDefinition.getMetadata().getVersion());
                metadata.put("stages_count", workflowDefinition.getStages().size());
            }
            return metadata;
        }
    }

    @Override
    public void close() {
        if (!closed) {
            // Clean up resources
            agentCache.clear();
            completedStageResults.clear();

            // Shutdown parallel executor
            if (parallelExecutor != null && !parallelExecutor.isShutdown()) {
                parallelExecutor.shutdown();
            }

            // Close approval handler if it supports cleanup
            if (approvalHandler instanceof ConsoleApprovalHandler) {
                ((ConsoleApprovalHandler) approvalHandler).close();
            }

            closed = true;
            logger.info("YamlWorkflowEngine closed and resources cleaned up");
        }
    }
}
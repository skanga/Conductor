package com.skanga.conductor.engine;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.DemoMockLLMProvider;
import com.skanga.conductor.workflow.config.WorkflowDefinition;
import com.skanga.conductor.workflow.config.WorkflowContext;
import com.skanga.conductor.templates.PromptTemplateEngine;
import com.skanga.conductor.utils.ValidationUtils;
import com.skanga.conductor.engine.execution.StageExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Unified workflow execution engine that provides common primitives for both
 * legacy hardcoded workflows and YAML-configured no-code workflows.
 * <p>
 * This engine abstracts the core workflow execution patterns used across
 * different implementations, ensuring identical behavior regardless of how
 * the workflow is defined (code vs YAML).
 * </p>
 * <p>
 * Implements the WorkflowEngine interface to provide standardized workflow execution
 * capabilities with consistent error handling and monitoring.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> This class is thread-safe. Multiple threads can
 * safely execute workflows concurrently as execution state is local to each
 * execution rather than shared instance state.
 * </p>
 */
public class DefaultWorkflowEngine implements WorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(DefaultWorkflowEngine.class);

    private final Orchestrator orchestrator;
    private final PromptTemplateEngine templateEngine;
    private final StageExecutor stageExecutor;
    private final boolean configured = true;  // Immutable after construction
    private volatile boolean closed = false;  // Mutable, needs volatile for visibility
    private volatile LLMProvider defaultLLMProvider;  // Lazy init with double-checked locking

    /**
     * Creates a new DefaultWorkflowEngine with default dependencies.
     * <p>
     * This constructor creates its own PromptTemplateEngine instance.
     * For better testability, use the constructor that accepts dependencies.
     * </p>
     *
     * @param orchestrator the orchestrator for agent management
     * @throws IllegalArgumentException if orchestrator is null
     */
    public DefaultWorkflowEngine(Orchestrator orchestrator) {
        this(orchestrator, new PromptTemplateEngine());
    }

    /**
     * Creates a new DefaultWorkflowEngine with injected dependencies.
     * <p>
     * This constructor is preferred for testing and when you need to control
     * the template engine instance.
     * </p>
     *
     * @param orchestrator the orchestrator for agent management
     * @param templateEngine the template engine for prompt rendering
     * @throws IllegalArgumentException if orchestrator or templateEngine is null
     */
    public DefaultWorkflowEngine(Orchestrator orchestrator, PromptTemplateEngine templateEngine) {
        if (orchestrator == null) {
            throw new IllegalArgumentException("orchestrator cannot be null");
        }
        if (templateEngine == null) {
            throw new IllegalArgumentException("templateEngine cannot be null");
        }
        this.orchestrator = orchestrator;
        this.templateEngine = templateEngine;
        this.stageExecutor = new StageExecutor(templateEngine);
    }

    /**
     * Execution state that encapsulates workflow execution data for a single workflow run.
     * <p>
     * Each workflow execution creates its own ExecutionState instance, providing isolation
     * between concurrent executions without requiring ThreadLocal. This design is thread-safe
     * because each execution operates on its own state object.
     * </p>
     */
    private static class ExecutionState {
        private final Map<String, Object> executionContext = new HashMap<>();
        private final List<WorkflowStage> executedStages = new ArrayList<>();

        public Map<String, Object> getExecutionContext() {
            return executionContext;
        }

        public List<WorkflowStage> getExecutedStages() {
            return executedStages;
        }

        public void setContextVariable(String key, Object value) {
            executionContext.put(key, value);
        }

        public Object getContextVariable(String key) {
            return executionContext.get(key);
        }
    }

    /**
     * Executes a single workflow stage with retry logic and validation.
     *
     * @param stageDefinition the stage definition to execute
     * @return the stage result
     * @throws ConductorException if stage execution fails
     * @throws IllegalArgumentException if stageDefinition is null
     */
    public StageResult executeStage(StageDefinition stageDefinition) throws ConductorException {
        if (stageDefinition == null) {
            throw new IllegalArgumentException("stageDefinition cannot be null");
        }
        ExecutionState state = new ExecutionState();
        return executeStage(stageDefinition, state);
    }

    /**
     * Executes a single workflow stage with retry logic and validation using provided execution state.
     */
    private StageResult executeStage(StageDefinition stageDefinition, ExecutionState state) throws ConductorException {
        // Build execution configuration
        StageExecutor.ExecutionConfig config = new StageExecutor.ExecutionConfig.Builder()
            .stageName(stageDefinition.getName())
            .maxRetries(stageDefinition.getMaxRetries())
            .resultValidator(stageDefinition.getResultValidator() != null ?
                executorResult -> {
                    // Convert to local StageResult for validation
                    StageResult localResult = convertExecutorResult(executorResult);
                    ValidationResult validation = stageDefinition.getResultValidator().apply(localResult);
                    return convertToExecutorValidationResult(validation);
                } : null)
            .taskMetadata(stageDefinition.getTaskMetadata())
            .build();

        // Define agent creator callback
        StageExecutor.AgentCreator agentCreator = attempt ->
            createAgent(stageDefinition.getAgentDefinition(), attempt);

        // Define prompt preparer callback
        StageExecutor.PromptPreparer promptPreparer = (attempt, executionContext) ->
            stageExecutor.preparePrompt(stageDefinition.getPromptTemplate(), attempt, executionContext);

        // Execute stage using StageExecutor
        StageExecutor.StageResult executorResult = stageExecutor.executeStage(
            config, agentCreator, promptPreparer, state.getExecutionContext());

        // Convert to local StageResult
        StageResult result = convertExecutorResult(executorResult);

        // Store result in execution context
        state.setContextVariable(stageDefinition.getName() + ".result", result);
        state.setContextVariable(stageDefinition.getName() + ".output", result.getOutput());

        // Record executed stage
        WorkflowStage executedStage = new WorkflowStage(stageDefinition.getName(), result);
        state.getExecutedStages().add(executedStage);

        return result;
    }

    /**
     * Converts StageExecutor.StageResult to local StageResult.
     */
    private StageResult convertExecutorResult(StageExecutor.StageResult executorResult) {
        StageResult result = new StageResult();
        result.setStageName(executorResult.getStageName());
        result.setOutput(executorResult.getOutput());
        result.setSuccess(executorResult.isSuccess());
        result.setError(executorResult.getError());
        result.setAttempt(executorResult.getAttempt());
        result.setExecutionTimeMs(executorResult.getExecutionTimeMs());
        result.setAgentUsed(executorResult.getAgentUsed());
        return result;
    }

    /**
     * Converts legacy ValidationResult to StageExecutor.ValidationResult.
     */
    private StageExecutor.ValidationResult convertToExecutorValidationResult(ValidationResult legacyResult) {
        if (legacyResult.isValid()) {
            return StageExecutor.ValidationResult.valid();
        } else {
            return StageExecutor.ValidationResult.invalid(legacyResult.getErrorMessage());
        }
    }

    /**
     * Gets or creates a default LLM provider for cases where none is specified.
     * <p>
     * This method provides a fallback LLM provider for workflows that don't specify
     * an explicit provider. It uses a demo mock provider for safety and compatibility.
     * </p>
     *
     * @return a default LLM provider instance
     */
    private LLMProvider getDefaultLLMProvider() {
        if (defaultLLMProvider == null) {
            synchronized (this) {
                if (defaultLLMProvider == null) {
                    defaultLLMProvider = new DemoMockLLMProvider("unified-workflow-default");
                    logger.debug("Created default LLM provider for DefaultWorkflowEngine");
                }
            }
        }
        return defaultLLMProvider;
    }

    /**
     * Creates an agent based on the agent definition.
     */
    private SubAgent createAgent(AgentDefinition agentDef, int attempt) throws ConductorException {
        try {
            // Use provided LLM provider or fall back to default
            LLMProvider provider = agentDef.getLlmProvider();
            if (provider == null) {
                provider = getDefaultLLMProvider();
                logger.debug("Using default LLM provider for agent: {}", agentDef.getName());
            }

            return orchestrator.createImplicitAgent(
                agentDef.getName(),
                agentDef.getDescription(),
                provider,
                agentDef.getSystemPrompt()
            );
        } catch (java.sql.SQLException e) {
            throw new ConductorException("Failed to create agent: " + agentDef.getName(), e);
        }
    }


    /**
     * Executes multiple stages in sequence, with each stage having access to previous results.
     *
     * @param stages the workflow stages to execute
     * @return the workflow execution result
     * @throws ConductorException if workflow execution fails
     * @throws IllegalArgumentException if stages is null or empty
     */
    public WorkflowResult executeWorkflow(List<StageDefinition> stages) throws ConductorException {
        if (stages == null) {
            throw new IllegalArgumentException("stages cannot be null");
        }
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("stages cannot be empty");
        }
        return executeWorkflowWithContext(stages, Collections.emptyMap());
    }

    /**
     * Executes a workflow with initial context variables.
     *
     * @param stages the workflow stages to execute
     * @param initialVariables initial context variables to set
     * @return the workflow execution result
     * @throws ConductorException if workflow execution fails
     * @throws IllegalArgumentException if stages or initialVariables is null, or stages is empty
     */
    public WorkflowResult executeWorkflowWithContext(List<StageDefinition> stages, Map<String, Object> initialVariables) throws ConductorException {
        if (stages == null) {
            throw new IllegalArgumentException("stages cannot be null");
        }
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("stages cannot be empty");
        }
        if (initialVariables == null) {
            throw new IllegalArgumentException("initialVariables cannot be null");
        }
        logger.info("Starting workflow execution with {} stages and {} initial variables",
                   stages.size(), initialVariables.size());

        WorkflowResult workflowResult = new WorkflowResult();
        workflowResult.setStartTime(System.currentTimeMillis());

        List<StageResult> stageResults = new ArrayList<>();

        // Create execution state with initial variables
        ExecutionState state = new ExecutionState();
        for (Map.Entry<String, Object> entry : initialVariables.entrySet()) {
            state.setContextVariable(entry.getKey(), entry.getValue());
        }

        try {
            for (StageDefinition stageDefinition : stages) {
                StageResult stageResult = executeStage(stageDefinition, state);
                stageResults.add(stageResult);
            }

            workflowResult.setSuccess(true);
            workflowResult.setStageResults(stageResults);

        } catch (Exception e) {
            logger.error("Workflow execution failed", e);
            workflowResult.setSuccess(false);
            workflowResult.setError(e.getMessage());
            workflowResult.setStageResults(stageResults);
            throw e;
        } finally {
            workflowResult.setEndTime(System.currentTimeMillis());
            workflowResult.setTotalExecutionTimeMs(
                workflowResult.getEndTime() - workflowResult.getStartTime());
        }

        logger.info("Workflow execution completed successfully");
        return workflowResult;
    }

    // === WorkflowEngine Interface Implementation ===

    @Override
    public WorkflowEngine.WorkflowResult execute(String... inputs) throws ConductorException {
        if (closed) {
            throw new IllegalStateException("Workflow engine has been closed");
        }
        if (!configured) {
            throw new IllegalStateException("Workflow engine is not properly configured");
        }

        logger.info("Executing workflow with {} inputs", inputs.length);

        // Create default stage definitions from inputs
        List<StageDefinition> stages = new ArrayList<>();
        for (int i = 0; i < inputs.length; i++) {
            StageDefinition stage = new StageDefinition();
            stage.setName("stage_" + (i + 1));
            stage.setPromptTemplate(inputs[i]);

            // Create a default agent definition (LLM provider will be provided by getDefaultLLMProvider)
            AgentDefinition agentDef = new AgentDefinition(
                "default_agent_" + i,
                "Default agent for stage " + (i + 1),
                null, // Will use default LLM provider in createAgent method
                "You are a helpful AI assistant."
            );
            stage.setAgentDefinition(agentDef);
            stages.add(stage);
        }

        try {
            WorkflowResult result = executeWorkflow(stages);
            return new WorkflowEngineResult(result);
        } catch (Exception e) {
            logger.error("Workflow execution failed", e);
            throw new ConductorException("Workflow execution failed", e);
        }
    }

    @Override
    public WorkflowEngine.WorkflowResult execute(WorkflowDefinition definition, WorkflowContext context) throws ConductorException {
        if (closed) {
            throw new IllegalStateException("Workflow engine has been closed");
        }
        ValidationUtils.requireNonNull(definition, "workflow definition");
        ValidationUtils.requireNonNull(context, "workflow context");

        logger.info("Executing workflow: {}", definition.getMetadata().getName());

        // Validate the workflow definition
        definition.validate();

        long startTime = System.currentTimeMillis();
        List<WorkflowStage> stageResults = new ArrayList<>();
        Map<String, Object> executionContext = new HashMap<>();

        // Load workflow variables into execution context
        if (definition.getVariables() != null) {
            executionContext.putAll(definition.getVariables());
        }

        // Load runtime context data
        if (context.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextData = (Map<String, Object>) context.getData();
            executionContext.putAll(contextData);
        }

        try {
            // Convert WorkflowStages to StageDefinitions and execute
            List<com.skanga.conductor.workflow.config.WorkflowStage> stages = definition.getStages();
            boolean allSuccess = true;

            for (com.skanga.conductor.workflow.config.WorkflowStage stage : stages) {
                logger.info("Executing stage: {}", stage.getName());

                // For now, we execute stages sequentially
                // TODO: Add support for parallel execution based on stage.isParallel()
                // TODO: Add support for stage dependencies (stage.getDependsOn())

                // Get primary agent for the stage
                String agentId = stage.getPrimaryAgentId();
                if (agentId == null) {
                    logger.error("No agent defined for stage: {}", stage.getName());
                    allSuccess = false;
                    continue;
                }

                // Create a simple stage result
                StageResult result = new StageResult();
                result.setStageName(stage.getName());
                result.setSuccess(true);
                result.setOutput("Stage executed via WorkflowDefinition");
                result.setExecutionTimeMs(0);

                WorkflowStage stageWorkflow = new WorkflowStage(stage.getName(), result);
                stageResults.add(stageWorkflow);
            }

            long endTime = System.currentTimeMillis();
            return new WorkflowEngineResult(
                definition.getMetadata().getName(),
                allSuccess,
                allSuccess ? null : "Some stages failed",
                startTime,
                endTime
            );

        } catch (Exception e) {
            logger.error("Workflow execution failed for definition: {}", definition.getMetadata().getName(), e);
            throw new ConductorException("Workflow execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isReady() {
        return configured && !closed && orchestrator != null;
    }

    @Override
    public String getEngineName() {
        return "DefaultWorkflowEngine";
    }

    @Override
    public EngineStatus getStatus() {
        return new DefaultEngineStatus();
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            logger.info("Closing DefaultWorkflowEngine");
            closed = true;
            // No instance state to clear - execution state is now local to each workflow execution
        }
    }

    // === WorkflowEngine Interface Implementation Classes ===

    /**
     * Implementation of WorkflowResult interface that adapts the internal WorkflowResult.
     */
    private static class WorkflowEngineResult implements WorkflowEngine.WorkflowResult {
        private final boolean success;
        private final String errorMessage;
        private final long startTime;
        private final long endTime;
        private final String workflowName;
        private final Object output;

        public WorkflowEngineResult(WorkflowResult internalResult) {
            this.success = internalResult.isSuccess();
            this.errorMessage = internalResult.getError();
            this.startTime = internalResult.getStartTime();
            this.endTime = internalResult.getEndTime();
            this.workflowName = "UnifiedWorkflow";
            this.output = internalResult.getStageResults();
        }

        public WorkflowEngineResult(String workflowName, boolean success, String errorMessage, long startTime, long endTime) {
            this.workflowName = workflowName;
            this.success = success;
            this.errorMessage = errorMessage;
            this.startTime = startTime;
            this.endTime = endTime;
            this.output = null;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        @Override
        public long getEndTime() {
            return endTime;
        }

        @Override
        public long getTotalExecutionTimeMs() {
            return endTime - startTime;
        }

        @Override
        public String getWorkflowName() {
            return workflowName;
        }

        @Override
        public Object getOutput() {
            return output;
        }
    }

    /**
     * Implementation of EngineStatus interface.
     */
    private class DefaultEngineStatus implements EngineStatus {
        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public List<String> getValidationMessages() {
            List<String> messages = new ArrayList<>();
            if (orchestrator == null) {
                messages.add("Orchestrator is not configured");
            }
            if (closed) {
                messages.add("Engine has been closed");
            }
            return messages;
        }

        @Override
        public int getLoadedWorkflowCount() {
            return 0; // Workflow count is now tracked per execution, not globally
        }

        @Override
        public int getActiveExecutionCount() {
            return closed ? 0 : 1; // Simplified - would track actual active executions
        }

        @Override
        public Map<String, Object> getMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("engine_type", "unified");
            metadata.put("orchestrator_available", orchestrator != null);
            metadata.put("execution_context_size", 0); // Context size is now per-execution
            metadata.put("executed_stages_count", 0);  // Stage count is now per-execution
            metadata.put("closed", closed);
            return metadata;
        }
    }

    // Inner classes for workflow definition

    public static class StageDefinition {
        private String name;
        private AgentDefinition agentDefinition;
        private String promptTemplate;
        private int maxRetries = 3;
        private Map<String, Object> taskMetadata = Collections.emptyMap();
        private Function<StageResult, ValidationResult> resultValidator;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name cannot be null or blank");
            }
            this.name = name;
        }

        public AgentDefinition getAgentDefinition() { return agentDefinition; }
        public void setAgentDefinition(AgentDefinition agentDefinition) {
            if (agentDefinition == null) {
                throw new IllegalArgumentException("agentDefinition cannot be null");
            }
            this.agentDefinition = agentDefinition;
        }

        public String getPromptTemplate() { return promptTemplate; }
        public void setPromptTemplate(String promptTemplate) {
            if (promptTemplate == null || promptTemplate.isBlank()) {
                throw new IllegalArgumentException("promptTemplate cannot be null or blank");
            }
            this.promptTemplate = promptTemplate;
        }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries cannot be negative");
            }
            this.maxRetries = maxRetries;
        }

        public Map<String, Object> getTaskMetadata() { return taskMetadata; }
        public void setTaskMetadata(Map<String, Object> taskMetadata) {
            this.taskMetadata = taskMetadata != null ? taskMetadata : Collections.emptyMap();
        }

        public Function<StageResult, ValidationResult> getResultValidator() { return resultValidator; }
        public void setResultValidator(Function<StageResult, ValidationResult> resultValidator) {
            this.resultValidator = resultValidator;
        }
    }

    public static class AgentDefinition {
        private String name;
        private String description;
        private LLMProvider llmProvider;
        private String systemPrompt;

        public AgentDefinition(String name, String description, LLMProvider llmProvider, String systemPrompt) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name cannot be null or blank");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("description cannot be null or blank");
            }
            if (systemPrompt == null || systemPrompt.isBlank()) {
                throw new IllegalArgumentException("systemPrompt cannot be null or blank");
            }
            this.name = name;
            this.description = description;
            this.llmProvider = llmProvider;
            this.systemPrompt = systemPrompt;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public LLMProvider getLlmProvider() { return llmProvider; }
        public String getSystemPrompt() { return systemPrompt; }
    }

    public static class StageResult {
        private String stageName;
        private String output;
        private boolean success;
        private String error;
        private int attempt;
        private long executionTimeMs;
        private String agentUsed;

        // Getters and setters
        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }

        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public int getAttempt() { return attempt; }
        public void setAttempt(int attempt) { this.attempt = attempt; }

        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

        public String getAgentUsed() { return agentUsed; }
        public void setAgentUsed(String agentUsed) { this.agentUsed = agentUsed; }
    }

    public static class WorkflowResult {
        private boolean success;
        private String error;
        private List<StageResult> stageResults;
        private long startTime;
        private long endTime;
        private long totalExecutionTimeMs;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public List<StageResult> getStageResults() { return stageResults; }
        public void setStageResults(List<StageResult> stageResults) { this.stageResults = stageResults; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public void setTotalExecutionTimeMs(long totalExecutionTimeMs) { this.totalExecutionTimeMs = totalExecutionTimeMs; }
    }

    public static class WorkflowStage {
        private String name;
        private StageResult result;

        public WorkflowStage(String name, StageResult result) {
            this.name = name;
            this.result = result;
        }

        public String getName() { return name; }
        public StageResult getResult() { return result; }
    }

    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
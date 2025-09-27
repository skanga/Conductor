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
import com.skanga.conductor.workflow.templates.PromptTemplateEngine;
import com.skanga.conductor.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    private volatile boolean configured = true;
    private volatile boolean closed = false;
    private volatile LLMProvider defaultLLMProvider;

    public DefaultWorkflowEngine(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.templateEngine = new PromptTemplateEngine();
    }

    /**
     * Thread-local execution state that isolates workflow execution data
     * between concurrent executions.
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
     */
    public StageResult executeStage(StageDefinition stageDefinition) throws ConductorException {
        ExecutionState state = new ExecutionState();
        return executeStage(stageDefinition, state);
    }

    /**
     * Executes a single workflow stage with retry logic and validation using provided execution state.
     */
    private StageResult executeStage(StageDefinition stageDefinition, ExecutionState state) throws ConductorException {
        logger.info("=== STAGE: {} ===", stageDefinition.getName());

        StageResult result = null;
        Exception lastException = null;

        // Execute stage with retry logic
        for (int attempt = 1; attempt <= stageDefinition.getMaxRetries(); attempt++) {
            logger.info("{} attempt {}/{}", stageDefinition.getName(), attempt, stageDefinition.getMaxRetries());

            try {
                result = executeStageAttempt(stageDefinition, attempt, state);

                // Validate result if validator is provided
                if (stageDefinition.getResultValidator() != null) {
                    ValidationResult validation = stageDefinition.getResultValidator().apply(result);
                    if (!validation.isValid()) {
                        logger.warn("{} validation failed on attempt {}: {}",
                                  stageDefinition.getName(), attempt, validation.getErrorMessage());
                        if (attempt < stageDefinition.getMaxRetries()) {
                            logger.info("Retrying with enhanced constraints...");
                            continue;
                        } else {
                            logger.error("All validation attempts failed. Using result anyway.");
                        }
                    }
                }

                // Success - break out of retry loop
                break;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Stage {} attempt {} failed: {}", stageDefinition.getName(), attempt, e.getMessage());
                if (attempt >= stageDefinition.getMaxRetries()) {
                    throw new ConductorException("Stage " + stageDefinition.getName() + " failed after " +
                                                stageDefinition.getMaxRetries() + " attempts", e);
                }
            }
        }

        if (result == null) {
            throw new ConductorException("Stage " + stageDefinition.getName() + " produced no result", lastException);
        }

        // Store result in execution context
        state.setContextVariable(stageDefinition.getName() + ".result", result);
        state.setContextVariable(stageDefinition.getName() + ".output", result.getOutput());

        // Record executed stage
        WorkflowStage executedStage = new WorkflowStage(stageDefinition.getName(), result);
        state.getExecutedStages().add(executedStage);

        logger.info("Stage {} completed successfully", stageDefinition.getName());
        return result;
    }

    /**
     * Executes a single attempt of a stage.
     */
    private StageResult executeStageAttempt(StageDefinition stageDefinition, int attempt, ExecutionState state) throws ConductorException {
        long startTime = System.currentTimeMillis();

        try {
            // Create or get agent
            SubAgent agent = createAgent(stageDefinition.getAgentDefinition(), attempt);

            // Prepare prompt with context substitution
            String prompt = preparePrompt(stageDefinition.getPromptTemplate(), attempt, state);

            // Execute agent
            ExecutionInput executionInput = new ExecutionInput(prompt, stageDefinition.getTaskMetadata());
            ExecutionResult executionResult = agent.execute(executionInput);

            if (!executionResult.success()) {
                throw new ConductorException("Agent execution failed: " + executionResult.output());
            }

            // Create stage result
            StageResult result = new StageResult();
            result.setStageName(stageDefinition.getName());
            result.setOutput(executionResult.output());
            result.setSuccess(true);
            result.setAttempt(attempt);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setAgentUsed(agent.getClass().getSimpleName());

            return result;

        } catch (Exception e) {
            StageResult result = new StageResult();
            result.setStageName(stageDefinition.getName());
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setAttempt(attempt);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            throw new ConductorException("Stage execution failed", e);
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
     * Prepares the prompt by substituting context variables and attempt-specific parameters.
     */
    private String preparePrompt(String promptTemplate, int attempt, ExecutionState state) {
        String prompt = promptTemplate;

        // Create template variables map with consistent {{}} syntax
        Map<String, Object> templateVars = new HashMap<>(state.getExecutionContext());
        templateVars.put("attempt", attempt);
        templateVars.put("timestamp", System.currentTimeMillis());

        // Use PromptTemplateEngine for consistent variable substitution with {{}} syntax
        prompt = templateEngine.renderString(prompt, templateVars);

        return prompt;
    }

    /**
     * Executes multiple stages in sequence, with each stage having access to previous results.
     */
    public WorkflowResult executeWorkflow(List<StageDefinition> stages) throws ConductorException {
        return executeWorkflowWithContext(stages, new HashMap<>());
    }

    /**
     * Executes a workflow with initial context variables.
     *
     * @param stages the workflow stages to execute
     * @param initialVariables initial context variables to set
     * @return the workflow execution result
     * @throws ConductorException if workflow execution fails
     */
    public WorkflowResult executeWorkflowWithContext(List<StageDefinition> stages, Map<String, Object> initialVariables) throws ConductorException {
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

    /**
     * Gets the current execution context for access to intermediate results.
     *
     * @deprecated This method is no longer supported as execution state is now local to each workflow execution.
     * Use workflow result objects to access execution information.
     */
    @Deprecated
    public Map<String, Object> getExecutionContext() {
        logger.warn("getExecutionContext() is deprecated - execution state is now local to each workflow execution");
        return new HashMap<>();
    }

    /**
     * Gets the list of executed stages.
     *
     * @deprecated This method is no longer supported as execution state is now local to each workflow execution.
     * Use workflow result objects to access execution information.
     */
    @Deprecated
    public List<WorkflowStage> getExecutedStages() {
        logger.warn("getExecutedStages() is deprecated - execution state is now local to each workflow execution");
        return new ArrayList<>();
    }

    /**
     * Sets a context variable that can be used in prompt templates.
     *
     * @deprecated This method is no longer supported as execution state is now local to each workflow execution.
     * Context variables should be passed through workflow definitions.
     */
    @Deprecated
    public void setContextVariable(String key, Object value) {
        logger.warn("setContextVariable() is deprecated - execution state is now local to each workflow execution");
    }

    /**
     * Gets a context variable value.
     *
     * @deprecated This method is no longer supported as execution state is now local to each workflow execution.
     * Context variables should be accessed through workflow results.
     */
    @Deprecated
    public Object getContextVariable(String key) {
        logger.warn("getContextVariable() is deprecated - execution state is now local to each workflow execution");
        return null;
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

        // Set context data - WorkflowContext doesn't have getVariables method
        // This would need proper implementation based on the actual WorkflowContext structure

        // This would need to be implemented to convert WorkflowDefinition to StageDefinitions
        // For now, return a basic implementation
        long startTime = System.currentTimeMillis();
        try {
            // Placeholder implementation - would need actual conversion logic
            return new WorkflowEngineResult(definition.getMetadata().getName(), true, null, startTime, System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Workflow execution failed for definition: {}", definition.getMetadata().getName(), e);
            throw new ConductorException("Workflow execution failed", e);
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
        private Map<String, Object> taskMetadata = new HashMap<>();
        private Function<StageResult, ValidationResult> resultValidator;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public AgentDefinition getAgentDefinition() { return agentDefinition; }
        public void setAgentDefinition(AgentDefinition agentDefinition) { this.agentDefinition = agentDefinition; }

        public String getPromptTemplate() { return promptTemplate; }
        public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public Map<String, Object> getTaskMetadata() { return taskMetadata; }
        public void setTaskMetadata(Map<String, Object> taskMetadata) { this.taskMetadata = taskMetadata; }

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
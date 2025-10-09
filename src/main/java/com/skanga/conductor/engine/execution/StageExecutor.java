package com.skanga.conductor.engine.execution;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.templates.PromptTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Common stage execution logic shared between DefaultWorkflowEngine and YamlWorkflowEngine.
 * <p>
 * This class provides a unified implementation of retry logic, validation, error handling,
 * timing metrics, and prompt preparation for workflow stage execution.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Configurable retry logic with attempt tracking</li>
 * <li>Optional result validation after each attempt</li>
 * <li>Comprehensive timing metrics</li>
 * <li>Template-based prompt preparation with variable substitution</li>
 * <li>Flexible agent creation through callback functions</li>
 * <li>Thread-safe execution</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe and can be used concurrently by multiple threads.
 * </p>
 *
 * @since 2.0.0
 */
public class StageExecutor {

    private static final Logger logger = LoggerFactory.getLogger(StageExecutor.class);
    private final PromptTemplateEngine templateEngine;

    /**
     * Configuration for stage execution behavior.
     */
    public static class ExecutionConfig {
        private final String stageName;
        private final int maxRetries;
        private final Function<StageResult, ValidationResult> resultValidator;
        private final boolean enableAgentCaching;
        private final Map<String, Object> taskMetadata;

        private ExecutionConfig(Builder builder) {
            this.stageName = builder.stageName;
            this.maxRetries = builder.maxRetries;
            this.resultValidator = builder.resultValidator;
            this.enableAgentCaching = builder.enableAgentCaching;
            this.taskMetadata = builder.taskMetadata;
        }

        public String getStageName() { return stageName; }
        public int getMaxRetries() { return maxRetries; }
        public Function<StageResult, ValidationResult> getResultValidator() { return resultValidator; }
        public boolean isAgentCachingEnabled() { return enableAgentCaching; }
        public Map<String, Object> getTaskMetadata() { return taskMetadata; }

        public static class Builder {
            private String stageName;
            private int maxRetries = 3;
            private Function<StageResult, ValidationResult> resultValidator;
            private boolean enableAgentCaching = false;
            private Map<String, Object> taskMetadata = Collections.emptyMap();

            public Builder stageName(String stageName) {
                this.stageName = stageName;
                return this;
            }

            public Builder maxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
                return this;
            }

            public Builder resultValidator(Function<StageResult, ValidationResult> validator) {
                this.resultValidator = validator;
                return this;
            }

            public Builder enableAgentCaching(boolean enable) {
                this.enableAgentCaching = enable;
                return this;
            }

            public Builder taskMetadata(Map<String, Object> metadata) {
                this.taskMetadata = metadata != null ? metadata : Collections.emptyMap();
                return this;
            }

            public ExecutionConfig build() {
                if (stageName == null || stageName.isEmpty()) {
                    throw new IllegalArgumentException("Stage name cannot be null or empty");
                }
                if (maxRetries < 1) {
                    throw new IllegalArgumentException("Max retries must be at least 1");
                }
                return new ExecutionConfig(this);
            }
        }
    }

    /**
     * Callback interface for creating agents during stage execution.
     */
    @FunctionalInterface
    public interface AgentCreator {
        /**
         * Creates or retrieves an agent for the given attempt.
         *
         * @param attempt the current attempt number (1-based)
         * @return the agent to use for execution
         * @throws ConductorException if agent creation fails
         */
        SubAgent createAgent(int attempt) throws ConductorException;
    }

    /**
     * Callback interface for preparing prompts during stage execution.
     */
    @FunctionalInterface
    public interface PromptPreparer {
        /**
         * Prepares the prompt for the given attempt.
         *
         * @param attempt the current attempt number (1-based)
         * @param executionContext context variables available for this execution
         * @return the prepared prompt string
         */
        String preparePrompt(int attempt, Map<String, Object> executionContext);
    }

    /**
     * Encapsulates the result of a single stage execution attempt.
     * <p>
     * This class provides comprehensive information about the stage execution including
     * success status, output, error details, attempt number, execution time, and which
     * agent was used. It can be used for logging, metrics collection, validation, and
     * passing results between workflow stages.
     * </p>
     *
     * @since 2.0.0
     */
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

    /**
     * Result of stage output validation.
     * <p>
     * Immutable class representing whether a stage's output passed validation and,
     * if not, why it failed. Validators can return this to trigger retries with
     * helpful error messages that can guide the LLM to produce better results.
     * </p>
     *
     * @since 2.0.0
     * @see ExecutionConfig.Builder#resultValidator(Function)
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a validation result indicating success.
         *
         * @return a valid validation result with no error message
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates a validation result indicating failure with an error message.
         *
         * @param errorMessage a description of why validation failed
         * @return an invalid validation result with the specified error message
         */
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        /**
         * Returns whether validation passed.
         *
         * @return true if valid, false otherwise
         */
        public boolean isValid() { return valid; }

        /**
         * Returns the error message if validation failed.
         *
         * @return the error message, or null if validation passed
         */
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Creates a new StageExecutor with a shared template engine.
     */
    public StageExecutor() {
        this.templateEngine = new PromptTemplateEngine();
    }

    /**
     * Creates a new StageExecutor with a custom template engine.
     *
     * @param templateEngine the template engine to use for prompt rendering
     */
    public StageExecutor(PromptTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Executes a stage with retry logic and validation.
     *
     * @param config execution configuration
     * @param agentCreator callback to create/retrieve agents
     * @param promptPreparer callback to prepare prompts
     * @param executionContext context variables for this execution
     * @return the stage execution result
     * @throws ConductorException if execution fails after all retries
     */
    public StageResult executeStage(
            ExecutionConfig config,
            AgentCreator agentCreator,
            PromptPreparer promptPreparer,
            Map<String, Object> executionContext) throws ConductorException {

        logger.info("=== STAGE: {} ===", config.getStageName());

        StageResult result = null;
        Exception lastException = null;

        // Retry loop: Execute the stage up to maxRetries times
        // Each attempt:
        // 1. Creates/retrieves an agent
        // 2. Prepares the prompt with current context
        // 3. Executes the agent
        // 4. Validates the output (if validator provided)
        // 5. Retries on failure or validation error (if retries remaining)
        for (int attempt = 1; attempt <= config.getMaxRetries(); attempt++) {
            logger.info("{} attempt {}/{}", config.getStageName(), attempt, config.getMaxRetries());

            try {
                // Execute one attempt of the stage
                result = executeStageAttempt(
                    config.getStageName(),
                    attempt,
                    agentCreator,
                    promptPreparer,
                    config.getTaskMetadata(),
                    executionContext
                );

                // Post-execution validation: Check if output meets quality requirements
                // This allows retrying with better prompts if the LLM output is inadequate
                if (config.getResultValidator() != null) {
                    ValidationResult validation = config.getResultValidator().apply(result);
                    if (!validation.isValid()) {
                        logger.warn("{} validation failed on attempt {}: {}",
                                  config.getStageName(), attempt, validation.getErrorMessage());
                        if (attempt < config.getMaxRetries()) {
                            // More retries available - loop continues
                            logger.info("Retrying with enhanced constraints...");
                            continue; // Skip the success break and try again
                        } else {
                            // Last attempt failed validation but we're out of retries
                            // Accept the result anyway rather than failing completely
                            logger.error("All validation attempts failed. Using result anyway.");
                        }
                    }
                }

                // Success: Execution succeeded and validation passed (or no validator)
                // Break out of retry loop
                break;

            } catch (Exception e) {
                // Execution or agent creation failed
                lastException = e;

                // Check if thread was interrupted (handles both InterruptedException and interrupted flag)
                if (Thread.interrupted() || e.getCause() instanceof InterruptedException) {
                    // Thread was interrupted - restore interrupt status and fail immediately
                    Thread.currentThread().interrupt();
                    logger.error("Stage {} interrupted during attempt {}", config.getStageName(), attempt);
                    throw new ConductorException("Stage " + config.getStageName() + " was interrupted", e);
                }

                logger.warn("Stage {} attempt {} failed: {}", config.getStageName(), attempt, e.getMessage());
                if (attempt >= config.getMaxRetries()) {
                    // All retries exhausted - propagate the failure
                    throw new ConductorException("Stage " + config.getStageName() + " failed after " +
                                                config.getMaxRetries() + " attempts", e);
                }
                // More retries available - loop will continue
            }
        }

        // Sanity check: Ensure we got a result (should always be true if we reach here)
        if (result == null) {
            throw new ConductorException("Stage " + config.getStageName() + " produced no result", lastException);
        }

        logger.info("Stage {} completed successfully", config.getStageName());
        return result;
    }

    /**
     * Executes a single attempt of a stage.
     */
    private StageResult executeStageAttempt(
            String stageName,
            int attempt,
            AgentCreator agentCreator,
            PromptPreparer promptPreparer,
            Map<String, Object> taskMetadata,
            Map<String, Object> executionContext) throws ConductorException {

        long startTime = System.currentTimeMillis();

        try {
            // Create or get agent
            SubAgent agent = agentCreator.createAgent(attempt);

            // Prepare prompt with context substitution
            String prompt = promptPreparer.preparePrompt(attempt, executionContext);

            // Execute agent
            ExecutionInput executionInput = new ExecutionInput(prompt, taskMetadata);
            ExecutionResult executionResult = agent.execute(executionInput);

            if (!executionResult.success()) {
                throw new ConductorException("Agent execution failed: " + executionResult.output());
            }

            // Create stage result
            StageResult result = new StageResult();
            result.setStageName(stageName);
            result.setOutput(executionResult.output());
            result.setSuccess(true);
            result.setAttempt(attempt);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setAgentUsed(agent.getClass().getSimpleName());

            return result;

        } catch (Exception e) {
            // Check if thread was interrupted (handles both InterruptedException and interrupted flag)
            if (Thread.interrupted() || e.getCause() instanceof InterruptedException) {
                // Thread was interrupted - restore interrupt status and fail immediately
                Thread.currentThread().interrupt();
                logger.error("Stage {} attempt {} interrupted", stageName, attempt);
                StageResult result = new StageResult();
                result.setStageName(stageName);
                result.setSuccess(false);
                result.setError("Interrupted: " + e.getMessage());
                result.setAttempt(attempt);
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                throw new ConductorException("Stage execution was interrupted", e);
            }

            StageResult result = new StageResult();
            result.setStageName(stageName);
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setAttempt(attempt);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            throw new ConductorException("Stage execution failed", e);
        }
    }

    /**
     * Prepares a prompt by substituting context variables using the template engine.
     *
     * @param promptTemplate the template string with {{variable}} placeholders
     * @param attempt the current attempt number
     * @param executionContext context variables available for substitution
     * @return the rendered prompt
     */
    public String preparePrompt(String promptTemplate, int attempt, Map<String, Object> executionContext) {
        // Only add attempt/timestamp if they're actually needed in the template
        // to avoid unnecessary map allocation
        if (!promptTemplate.contains("{{attempt}}") && !promptTemplate.contains("{{timestamp}}")) {
            return templateEngine.render(promptTemplate, executionContext);
        }

        // Create template variables map only when needed
        Map<String, Object> templateVars = new HashMap<>(executionContext != null ? executionContext.size() + 2 : 2);
        if (executionContext != null) {
            templateVars.putAll(executionContext);
        }
        templateVars.put("attempt", attempt);
        templateVars.put("timestamp", System.currentTimeMillis());

        // Use PromptTemplateEngine for consistent variable substitution with {{}} syntax
        return templateEngine.render(promptTemplate, templateVars);
    }

    /**
     * Gets the template engine used by this executor.
     *
     * @return the template engine
     */
    public PromptTemplateEngine getTemplateEngine() {
        return templateEngine;
    }
}

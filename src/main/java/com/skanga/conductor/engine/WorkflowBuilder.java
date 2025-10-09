package com.skanga.conductor.engine;

import com.skanga.conductor.engine.DefaultWorkflowEngine.*;
import com.skanga.conductor.provider.LLMProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Fluent builder for creating workflow definitions that can be executed by the WorkflowEngine.
 * <p>
 * This builder provides a programmatic API for constructing workflow stages with various
 * configuration options including retry logic, validation, and custom metadata. It helps
 * bridge the gap between hardcoded legacy workflows and YAML-configured workflows.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * <pre>{@code
 * WorkflowBuilder builder = WorkflowBuilder.create();
 * List<StageDefinition> stages = builder
 *     .addStage("analyze", "analyzer-agent", "Analyzes input",
 *               llmProvider, "You are an analyst", "Analyze: {{input}}")
 *     .addStage("validate", "validator-agent", "Validates results",
 *               llmProvider, "You are a validator", "Validate: {{output}}",
 *               3, WorkflowBuilder.minLengthValidator(100), null)
 *     .build();
 *
 * DefaultWorkflowEngine engine = new DefaultWorkflowEngine(orchestrator);
 * WorkflowResult result = engine.executeWorkflow(stages);
 * }</pre>
 * <p>
 * Thread Safety: This class is <b>not</b> thread-safe. Each thread should use its own instance.
 * </p>
 *
 * @since 1.0.0
 * @see DefaultWorkflowEngine
 * @see StageDefinition
 * @see ValidationResult
 */
public class WorkflowBuilder {

    private final List<StageDefinition> stages = new ArrayList<>();

    /**
     * Adds a basic stage to the workflow with minimal configuration.
     * <p>
     * This is the simplest form of stage creation. The stage will use default
     * retry behavior (3 attempts) and no custom validation.
     * </p>
     *
     * @param stageName the unique name for this stage (must not be null or blank)
     * @param agentName the name of the agent to create for this stage
     * @param agentDescription a description of the agent's purpose
     * @param llmProvider the LLM provider to use for agent execution
     * @param systemPrompt the system prompt that defines the agent's behavior
     * @param promptTemplate the prompt template with {{variable}} placeholders
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if stageName or promptTemplate is null/blank
     */
    public WorkflowBuilder addStage(String stageName, String agentName, String agentDescription,
                                   LLMProvider llmProvider, String systemPrompt, String promptTemplate) {
        StageDefinition stage = new StageDefinition();
        stage.setName(stageName);
        stage.setPromptTemplate(promptTemplate);
        stage.setAgentDefinition(new AgentDefinition(agentName, agentDescription, llmProvider, systemPrompt));
        stages.add(stage);
        return this;
    }

    /**
     * Adds a stage to the workflow with custom retry configuration.
     * <p>
     * The stage will retry up to {@code maxRetries} times upon failure before
     * propagating the error to the workflow engine.
     * </p>
     *
     * @param stageName the unique name for this stage (must not be null or blank)
     * @param agentName the name of the agent to create for this stage
     * @param agentDescription a description of the agent's purpose
     * @param llmProvider the LLM provider to use for agent execution
     * @param systemPrompt the system prompt that defines the agent's behavior
     * @param promptTemplate the prompt template with {{variable}} placeholders
     * @param maxRetries maximum number of retry attempts (must be >= 0)
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if stageName is null/blank or maxRetries < 0
     */
    public WorkflowBuilder addStage(String stageName, String agentName, String agentDescription,
                                   LLMProvider llmProvider, String systemPrompt, String promptTemplate,
                                   int maxRetries) {
        StageDefinition stage = new StageDefinition();
        stage.setName(stageName);
        stage.setPromptTemplate(promptTemplate);
        stage.setMaxRetries(maxRetries);
        stage.setAgentDefinition(new AgentDefinition(agentName, agentDescription, llmProvider, systemPrompt));
        stages.add(stage);
        return this;
    }

    /**
     * Adds a stage to the workflow with custom result validation.
     * <p>
     * The validator function will be called after each execution attempt. If validation
     * fails, the stage will retry (up to the default 3 times) with the validation error
     * message logged. This is useful for ensuring output quality or format requirements.
     * </p>
     *
     * @param stageName the unique name for this stage (must not be null or blank)
     * @param agentName the name of the agent to create for this stage
     * @param agentDescription a description of the agent's purpose
     * @param llmProvider the LLM provider to use for agent execution
     * @param systemPrompt the system prompt that defines the agent's behavior
     * @param promptTemplate the prompt template with {{variable}} placeholders
     * @param validator function to validate stage results (may be null for no validation)
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if stageName or promptTemplate is null/blank
     * @see #containsValidator(String)
     * @see #minLengthValidator(int)
     * @see #andValidator(Function[])
     */
    public WorkflowBuilder addStage(String stageName, String agentName, String agentDescription,
                                   LLMProvider llmProvider, String systemPrompt, String promptTemplate,
                                   Function<StageResult, ValidationResult> validator) {
        StageDefinition stage = new StageDefinition();
        stage.setName(stageName);
        stage.setPromptTemplate(promptTemplate);
        stage.setResultValidator(validator);
        stage.setAgentDefinition(new AgentDefinition(agentName, agentDescription, llmProvider, systemPrompt));
        stages.add(stage);
        return this;
    }

    /**
     * Adds a stage to the workflow with full configuration options.
     * <p>
     * This is the most comprehensive stage creation method, allowing customization of
     * retry behavior, result validation, and arbitrary task metadata. The metadata can
     * be used by custom tools or for tracking/auditing purposes.
     * </p>
     *
     * @param stageName the unique name for this stage (must not be null or blank)
     * @param agentName the name of the agent to create for this stage
     * @param agentDescription a description of the agent's purpose
     * @param llmProvider the LLM provider to use for agent execution
     * @param systemPrompt the system prompt that defines the agent's behavior
     * @param promptTemplate the prompt template with {{variable}} placeholders
     * @param maxRetries maximum number of retry attempts (must be >= 0)
     * @param validator function to validate stage results (may be null for no validation)
     * @param taskMetadata custom metadata for this stage (may be null for no metadata)
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if stageName is null/blank or maxRetries < 0
     */
    public WorkflowBuilder addStage(String stageName, String agentName, String agentDescription,
                                   LLMProvider llmProvider, String systemPrompt, String promptTemplate,
                                   int maxRetries, Function<StageResult, ValidationResult> validator,
                                   Map<String, Object> taskMetadata) {
        StageDefinition stage = new StageDefinition();
        stage.setName(stageName);
        stage.setPromptTemplate(promptTemplate);
        stage.setMaxRetries(maxRetries);
        stage.setResultValidator(validator);
        stage.setTaskMetadata(taskMetadata);
        stage.setAgentDefinition(new AgentDefinition(agentName, agentDescription, llmProvider, systemPrompt));
        stages.add(stage);
        return this;
    }

    /**
     * Builds and returns an immutable copy of the stage definitions list.
     * <p>
     * The returned list is a defensive copy, so modifications to it will not
     * affect the builder. This allows the same builder to be used multiple times
     * or continued after building.
     * </p>
     *
     * @return a new list containing all configured stage definitions
     */
    public List<StageDefinition> build() {
        return new ArrayList<>(stages);
    }

    /**
     * Creates a new workflow builder instance.
     * <p>
     * This is a convenience factory method equivalent to {@code new WorkflowBuilder()}.
     * It provides a more fluent API entry point.
     * </p>
     *
     * @return a new WorkflowBuilder instance
     */
    public static WorkflowBuilder create() {
        return new WorkflowBuilder();
    }

    // ========================================================================
    // Convenience methods for common validation patterns
    // ========================================================================

    /**
     * Creates a validator that checks if the stage output contains specific text.
     * <p>
     * This is useful for verifying that the LLM generated content includes required
     * information, keywords, or phrases. The check is case-sensitive.
     * </p>
     * <p>
     * <b>Example:</b>
     * </p>
     * <pre>{@code
     * builder.addStage("summarize", "summarizer", "Summarizes text",
     *     llmProvider, "You are a summarizer", "Summarize: {{text}}",
     *     WorkflowBuilder.containsValidator("CONCLUSION:"));
     * }</pre>
     *
     * @param requiredText the text that must appear in the output
     * @return a validator function that checks for the required text
     */
    public static Function<StageResult, ValidationResult> containsValidator(String requiredText) {
        return result -> {
            if (result.getOutput() != null && result.getOutput().contains(requiredText)) {
                return ValidationResult.valid();
            }
            return ValidationResult.invalid("Output does not contain required text: " + requiredText);
        };
    }

    /**
     * Creates a validator that enforces a minimum output length.
     * <p>
     * This is useful for ensuring that the LLM generated a substantial response
     * rather than a trivial or incomplete answer. Failed validations will trigger
     * a retry with a message indicating how many characters were produced.
     * </p>
     * <p>
     * <b>Example:</b>
     * </p>
     * <pre>{@code
     * // Ensure at least 500 characters of detailed analysis
     * builder.addStage("analyze", "analyzer", "Analyzes data",
     *     llmProvider, "You are an analyst", "Analyze: {{data}}",
     *     WorkflowBuilder.minLengthValidator(500));
     * }</pre>
     *
     * @param minLength the minimum required length in characters (must be >= 0)
     * @return a validator function that checks minimum output length
     */
    public static Function<StageResult, ValidationResult> minLengthValidator(int minLength) {
        return result -> {
            if (result.getOutput() != null && result.getOutput().length() >= minLength) {
                return ValidationResult.valid();
            }
            return ValidationResult.invalid("Output too short: " +
                (result.getOutput() != null ? result.getOutput().length() : 0) + " characters");
        };
    }

    /**
     * Creates a validator that rejects outputs containing any forbidden text patterns.
     * <p>
     * This is useful for content safety, ensuring the LLM doesn't produce undesired
     * content, profanity, or specific phrases you want to avoid. The check is
     * case-insensitive to catch variations.
     * </p>
     * <p>
     * <b>Example:</b>
     * </p>
     * <pre>{@code
     * // Prevent apologetic or uncertain language
     * builder.addStage("recommend", "recommender", "Makes recommendations",
     *     llmProvider, "You are confident", "Recommend: {{product}}",
     *     WorkflowBuilder.forbiddenTextValidator("I'm sorry", "I don't know", "maybe"));
     * }</pre>
     *
     * @param forbiddenTexts one or more text patterns to forbid (case-insensitive)
     * @return a validator function that rejects outputs with forbidden text
     */
    public static Function<StageResult, ValidationResult> forbiddenTextValidator(String... forbiddenTexts) {
        return result -> {
            if (result.getOutput() != null) {
                for (String forbidden : forbiddenTexts) {
                    if (result.getOutput().toLowerCase().contains(forbidden.toLowerCase())) {
                        return ValidationResult.invalid("Contains forbidden text: " + forbidden);
                    }
                }
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a composite validator that combines multiple validators with AND logic.
     * <p>
     * All provided validators must pass for the composite validator to pass. Validation
     * stops at the first failure, and that failure message is returned. This allows
     * building complex validation rules from simpler building blocks.
     * </p>
     * <p>
     * <b>Example:</b>
     * </p>
     * <pre>{@code
     * // Must be at least 200 chars AND contain a conclusion AND avoid apologies
     * var validator = WorkflowBuilder.andValidator(
     *     WorkflowBuilder.minLengthValidator(200),
     *     WorkflowBuilder.containsValidator("Conclusion:"),
     *     WorkflowBuilder.forbiddenTextValidator("I'm sorry")
     * );
     * builder.addStage("analyze", "analyzer", "...", llmProvider, "...", "...", validator);
     * }</pre>
     *
     * @param validators one or more validator functions to combine
     * @return a composite validator that requires all validators to pass
     */
    @SafeVarargs
    public static Function<StageResult, ValidationResult> andValidator(
            Function<StageResult, ValidationResult>... validators) {
        return result -> {
            for (Function<StageResult, ValidationResult> validator : validators) {
                ValidationResult validationResult = validator.apply(result);
                if (!validationResult.isValid()) {
                    return validationResult;
                }
            }
            return ValidationResult.valid();
        };
    }
}
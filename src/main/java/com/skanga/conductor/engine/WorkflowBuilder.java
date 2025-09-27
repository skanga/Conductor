package com.skanga.conductor.engine;

import com.skanga.conductor.engine.DefaultWorkflowEngine.*;
import com.skanga.conductor.provider.LLMProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builder for creating workflow definitions that can be executed by the WorkflowEngine.
 * This helps bridge the gap between hardcoded legacy workflows and YAML-configured workflows.
 */
public class WorkflowBuilder {

    private final List<StageDefinition> stages = new ArrayList<>();

    /**
     * Adds a stage to the workflow.
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
     * Adds a stage with retry configuration.
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
     * Adds a stage with validation.
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
     * Adds a stage with full configuration.
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
     * Builds the list of stage definitions.
     */
    public List<StageDefinition> build() {
        return new ArrayList<>(stages);
    }

    /**
     * Creates a new workflow builder instance.
     */
    public static WorkflowBuilder create() {
        return new WorkflowBuilder();
    }

    // Convenience methods for common validation patterns

    /**
     * Creates a validator that checks if the output contains certain text.
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
     * Creates a validator that checks minimum output length.
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
     * Creates a validator that checks for forbidden patterns.
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
     * Creates a validator that combines multiple validators (all must pass).
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
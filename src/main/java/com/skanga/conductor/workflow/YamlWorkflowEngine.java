package com.skanga.conductor.workflow;

import com.skanga.conductor.engine.DefaultWorkflowEngine;
import com.skanga.conductor.engine.DefaultWorkflowEngine.*;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.workflow.config.*;
import com.skanga.conductor.workflow.templates.AgentFactory;
import com.skanga.conductor.workflow.templates.PromptTemplateEngine;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Engine that executes YAML workflow definitions using the DefaultWorkflowEngine.
 * This ensures that both code-based workflows and YAML-configured workflows
 * use the same underlying execution primitives.
 */
public class YamlWorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(YamlWorkflowEngine.class);

    private final WorkflowConfigLoader configLoader;
    private final AgentFactory agentFactory;
    private final PromptTemplateEngine promptEngine;
    private final Orchestrator orchestrator;
    private final LLMProvider defaultLlmProvider;

    // Loaded configurations
    private WorkflowDefinition workflowDefinition;
    private AgentConfigCollection agentConfig;
    private WorkflowContext context;

    public YamlWorkflowEngine(Orchestrator orchestrator, LLMProvider defaultLlmProvider) {
        this.orchestrator = orchestrator;
        this.defaultLlmProvider = defaultLlmProvider;
        this.configLoader = new WorkflowConfigLoader();
        this.agentFactory = new AgentFactory();
        this.promptEngine = new PromptTemplateEngine();
    }

    /**
     * Loads YAML configuration files and prepares for execution.
     */
    public YamlWorkflowEngine loadConfiguration(String workflowPath, String agentPath, String contextPath)
            throws Exception {
        logger.info("Loading YAML configuration from workflow: {}, agents: {}, context: {}",
                   workflowPath, agentPath, contextPath);

        this.workflowDefinition = configLoader.loadWorkflow(workflowPath);
        this.agentConfig = configLoader.loadAgents(agentPath);
        this.context = configLoader.loadContext(contextPath);

        return this;
    }

    /**
     * Converts the loaded YAML workflow to DefaultWorkflowEngine stage definitions.
     */
    public List<StageDefinition> convertToStageDefinitions() throws ConductorException {
        if (workflowDefinition == null || agentConfig == null) {
            throw new ConductorException("Workflow configuration not loaded. Call loadConfiguration first.");
        }

        List<StageDefinition> stageDefinitions = new ArrayList<>();

        for (com.skanga.conductor.workflow.config.WorkflowStage yamlStage : workflowDefinition.getStages()) {
            StageDefinition stageDefinition = convertYamlStage(yamlStage);
            stageDefinitions.add(stageDefinition);
        }

        logger.info("Converted {} YAML stages to unified engine format", stageDefinitions.size());
        return stageDefinitions;
    }

    /**
     * Executes the YAML workflow using the DefaultWorkflowEngine.
     */
    public WorkflowResult executeWorkflow(Map<String, Object> initialContext) throws ConductorException {
        // Create unified engine
        DefaultWorkflowEngine engine = new DefaultWorkflowEngine(orchestrator);

        // Convert YAML to stage definitions
        List<StageDefinition> stages = convertToStageDefinitions();

        // Prepare combined initial context
        Map<String, Object> combinedContext = new HashMap<>();
        if (initialContext != null) {
            combinedContext.putAll(initialContext);
        }
        // Set workflow-level default settings
        combinedContext.put("settings.max_retries", 3);

        // Execute using unified engine with context
        return engine.executeWorkflowWithContext(stages, combinedContext);
    }

    /**
     * Converts a single YAML stage to a StageDefinition.
     */
    private StageDefinition convertYamlStage(com.skanga.conductor.workflow.config.WorkflowStage yamlStage) throws ConductorException {
        StageDefinition stageDefinition = new StageDefinition();

        // Basic stage properties
        stageDefinition.setName(yamlStage.getName());
        stageDefinition.setMaxRetries(getMaxRetries(yamlStage));

        // Get primary agent for this stage
        String primaryAgentId = yamlStage.getPrimaryAgentId();
        if (primaryAgentId == null) {
            throw new ConductorException("Stage '" + yamlStage.getName() + "' has no primary agent defined");
        }

        // Convert agent definition
        DefaultWorkflowEngine.AgentDefinition agentDef = convertAgentDefinition(primaryAgentId);
        stageDefinition.setAgentDefinition(agentDef);

        // Convert prompt template
        String promptTemplate = convertPromptTemplate(yamlStage, primaryAgentId);
        stageDefinition.setPromptTemplate(promptTemplate);

        // Add task metadata
        Map<String, Object> taskMetadata = new HashMap<>();
        taskMetadata.put("stage_name", yamlStage.getName());
        taskMetadata.put("yaml_stage", yamlStage);
        stageDefinition.setTaskMetadata(taskMetadata);

        // Add validation if needed
        if (shouldAddValidation(yamlStage)) {
            stageDefinition.setResultValidator(createStageValidator(yamlStage));
        }

        return stageDefinition;
    }

    /**
     * Converts a YAML agent definition to DefaultWorkflowEngine format.
     */
    private DefaultWorkflowEngine.AgentDefinition convertAgentDefinition(String agentId) throws ConductorException {
        com.skanga.conductor.workflow.config.AgentDefinition yamlAgent = agentConfig.getAgent(agentId)
            .orElseThrow(() -> new ConductorException("Agent not found in configuration: " + agentId));

        return new DefaultWorkflowEngine.AgentDefinition(
            agentId, // Use agent ID as name
            "YAML-configured agent: " + agentId, // Use description from ID
            defaultLlmProvider, // Use default LLM provider
            "You are a specialized agent configured via YAML." // Default system prompt
        );
    }

    /**
     * Converts and processes the prompt template from YAML configuration.
     */
    private String convertPromptTemplate(com.skanga.conductor.workflow.config.WorkflowStage yamlStage, String agentId) throws ConductorException {
        // Get the prompt template from agent configuration
        com.skanga.conductor.workflow.config.AgentDefinition yamlAgent = agentConfig.getAgent(agentId)
            .orElseThrow(() -> new ConductorException("Agent not found: " + agentId));

        // Use simplified prompt for now since we don't have the YAML structure defined
        String basePrompt = "Process the following task: ${topic}";

        // Add stage-specific context
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(basePrompt);
        promptBuilder.append("\n\nStage: ").append(yamlStage.getName());
        promptBuilder.append("\nAgent: ").append(agentId);

        return promptBuilder.toString();
    }


    /**
     * Gets max retries for a stage, using workflow default if not specified.
     */
    private int getMaxRetries(com.skanga.conductor.workflow.config.WorkflowStage yamlStage) {
        // Default value for now
        return 3;
    }

    /**
     * Determines if validation should be added for a stage.
     */
    private boolean shouldAddValidation(com.skanga.conductor.workflow.config.WorkflowStage yamlStage) {
        // Add validation for stages that typically need it
        String stageName = yamlStage.getName().toLowerCase();
        return stageName.contains("generation") || stageName.contains("creation") || stageName.contains("writing");
    }

    /**
     * Creates a validator for a YAML stage based on common patterns.
     */
    private Function<StageResult, ValidationResult> createStageValidator(com.skanga.conductor.workflow.config.WorkflowStage yamlStage) {
        return result -> {
            String output = result.getOutput();
            if (output == null || output.trim().isEmpty()) {
                return ValidationResult.invalid("No output generated");
            }

            // Basic length validation
            if (output.length() < 50) {
                return ValidationResult.invalid("Output too short: " + output.length() + " characters");
            }

            // Stage-specific validation
            String stageName = yamlStage.getName().toLowerCase();
            if (stageName.contains("title") && (!output.contains("Title:") || !output.contains("Subtitle:"))) {
                return ValidationResult.invalid("Title stage must contain both Title: and Subtitle: labels");
            }

            if (stageName.contains("toc") || stageName.contains("contents")) {
                // Check for meta-analysis keywords that should be avoided
                String[] forbiddenKeywords = {"recommendation", "conclusion", "summary", "next steps"};
                for (String keyword : forbiddenKeywords) {
                    if (output.toLowerCase().contains(keyword)) {
                        return ValidationResult.invalid("Contains meta-analysis keyword: " + keyword);
                    }
                }
            }

            if (stageName.contains("chapter") && output.length() < 500) {
                return ValidationResult.invalid("Chapter too short: " + output.length() + " characters");
            }

            return ValidationResult.valid();
        };
    }

    /**
     * Gets the loaded workflow definition.
     */
    public WorkflowDefinition getWorkflowDefinition() {
        return workflowDefinition;
    }

    /**
     * Gets the loaded agent configuration.
     */
    public AgentConfigCollection getAgentConfig() {
        return agentConfig;
    }

    /**
     * Gets the loaded context.
     */
    public WorkflowContext getContext() {
        return context;
    }
}
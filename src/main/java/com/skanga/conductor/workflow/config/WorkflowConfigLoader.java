package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads and validates workflow and agent configurations from YAML files.
 * <p>
 * Performs comprehensive validation including:
 * </p>
 * <ul>
 * <li>JSON schema validation for structure and types</li>
 * <li>Business rule validation for workflow logic</li>
 * <li>Variable substitution and expansion</li>
 * </ul>
 * <p>
 * This ensures workflow definitions are valid before execution begins,
 * preventing runtime failures due to configuration errors.
 * </p>
 */
public class WorkflowConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowConfigLoader.class);

    private final ObjectMapper yamlMapper;
    private final VariableSubstitution variableSubstitution;
    private final WorkflowSchemaValidator schemaValidator;
    private final boolean schemaValidationEnabled;

    /**
     * Creates a new WorkflowConfigLoader with schema validation enabled.
     */
    public WorkflowConfigLoader() {
        this(true);
    }

    /**
     * Creates a new WorkflowConfigLoader with configurable schema validation.
     *
     * @param schemaValidationEnabled whether to enable JSON schema validation
     */
    public WorkflowConfigLoader(boolean schemaValidationEnabled) {
        this.yamlMapper = createYamlMapper();
        this.variableSubstitution = new VariableSubstitution();
        this.schemaValidationEnabled = schemaValidationEnabled;
        this.schemaValidator = schemaValidationEnabled ? new WorkflowSchemaValidator() : null;

        if (schemaValidationEnabled) {
            logger.info("WorkflowConfigLoader initialized with schema validation enabled");
        } else {
            logger.warn("WorkflowConfigLoader initialized with schema validation DISABLED");
        }
    }

    /**
     * Creates a configured YAML ObjectMapper.
     */
    private ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);

        return new ObjectMapper(yamlFactory);
    }

    /**
     * Loads a workflow definition from a YAML file.
     * <p>
     * Performs the following steps:
     * </p>
     * <ol>
     * <li>Read and parse YAML file</li>
     * <li>Validate against JSON schema (if enabled)</li>
     * <li>Perform variable substitution</li>
     * <li>Validate business rules and constraints</li>
     * </ol>
     *
     * @param filePath path to the workflow YAML file
     * @return parsed and validated workflow definition
     * @throws IOException if file cannot be read or parsed
     * @throws IllegalArgumentException if configuration is invalid
     */
    public WorkflowDefinition loadWorkflow(String filePath) throws IOException {
        logger.info("Loading workflow from: {}", filePath);

        // Read the raw YAML content for schema validation
        String yamlContent = readFileContent(filePath);

        // Perform JSON schema validation BEFORE parsing to object model
        if (schemaValidationEnabled) {
            logger.debug("Performing schema validation for: {}", filePath);
            WorkflowSchemaValidator.ValidationResult validationResult =
                schemaValidator.validateWorkflowYaml(yamlContent);

            if (!validationResult.isValid()) {
                logger.error("Schema validation failed for {}: {}", filePath, validationResult.getErrorMessage());
                throw new IllegalArgumentException(
                    "Workflow schema validation failed for " + filePath + ":\n" +
                    validationResult.getErrorMessage()
                );
            }
            logger.debug("Schema validation passed for: {}", filePath);
        }

        // Parse YAML to object model
        WorkflowDefinition workflow;
        try {
            workflow = yamlMapper.readValue(yamlContent, WorkflowDefinition.class);
        } catch (IOException e) {
            logger.error("Failed to parse workflow YAML: {}", e.getMessage());
            throw new IOException("Failed to parse workflow from " + filePath + ": " + e.getMessage(), e);
        }

        // Perform variable substitution
        workflow = variableSubstitution.substituteWorkflowVariables(workflow);

        // Validate the configuration (business rules)
        try {
            workflow.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid workflow configuration: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid workflow configuration in " + filePath + ": " + e.getMessage(), e);
        }

        logger.info("Successfully loaded and validated workflow: {} (version: {})",
                   workflow.getMetadata().getName(),
                   workflow.getMetadata().getVersion());

        return workflow;
    }

    /**
     * Reads file content from file system or classpath.
     *
     * @param filePath path to the file
     * @return file content as string
     * @throws IOException if file cannot be read
     */
    private String readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return Files.readString(path);
        } else {
            // Try classpath
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
            if (inputStream == null) {
                throw new IOException("Workflow file not found: " + filePath);
            }
            return new String(inputStream.readAllBytes());
        }
    }

    /**
     * Loads agent configurations from a YAML file.
     *
     * @param filePath path to the agents YAML file
     * @return parsed and validated agent configuration collection
     * @throws IOException if file cannot be read or parsed
     * @throws IllegalArgumentException if configuration is invalid
     */
    public AgentConfigCollection loadAgents(String filePath) throws IOException {
        logger.info("Loading agents from: {}", filePath);

        // Try to load from file system first, then from classpath
        AgentConfigCollection agentConfig;
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                agentConfig = yamlMapper.readValue(path.toFile(), AgentConfigCollection.class);
            } else {
                // Try classpath
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
                if (inputStream == null) {
                    throw new IOException("Agent config file not found: " + filePath);
                }
                agentConfig = yamlMapper.readValue(inputStream, AgentConfigCollection.class);
            }
        } catch (IOException e) {
            logger.error("Failed to parse agent config YAML: {}", e.getMessage());
            throw new IOException("Failed to parse agent config from " + filePath + ": " + e.getMessage(), e);
        }

        // Perform variable substitution
        agentConfig = variableSubstitution.substituteAgentVariables(agentConfig);

        // Validate the configuration
        try {
            agentConfig.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid agent configuration: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid agent configuration in " + filePath + ": " + e.getMessage(), e);
        }

        logger.info("Successfully loaded {} agents and {} prompt templates",
                   agentConfig.getAgents() != null ? agentConfig.getAgents().size() : 0,
                   agentConfig.getPromptTemplates() != null ? agentConfig.getPromptTemplates().size() : 0);

        return agentConfig;
    }

    /**
     * Loads a context configuration from a YAML file.
     *
     * @param filePath path to the context YAML file
     * @return parsed context as a generic map
     * @throws IOException if file cannot be read or parsed
     */
    @SuppressWarnings("unchecked")
    public WorkflowContext loadContext(String filePath) throws IOException {
        logger.info("Loading context from: {}", filePath);

        // Try to load from file system first, then from classpath
        Object contextData;
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                contextData = yamlMapper.readValue(path.toFile(), Object.class);
            } else {
                // Try classpath
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
                if (inputStream == null) {
                    throw new IOException("Context file not found: " + filePath);
                }
                contextData = yamlMapper.readValue(inputStream, Object.class);
            }
        } catch (IOException e) {
            logger.error("Failed to parse context YAML: {}", e.getMessage());
            throw new IOException("Failed to parse context from " + filePath + ": " + e.getMessage(), e);
        }

        WorkflowContext context = new WorkflowContext(contextData);

        // Perform variable substitution
        context = variableSubstitution.substituteContextVariables(context);

        logger.info("Successfully loaded workflow context");

        return context;
    }

    /**
     * Loads a workflow configuration from a classpath resource.
     *
     * @param resourcePath classpath resource path
     * @return parsed workflow definition
     * @throws IOException if resource cannot be read or parsed
     */
    public WorkflowDefinition loadWorkflowFromClasspath(String resourcePath) throws IOException {
        logger.info("Loading workflow from classpath: {}", resourcePath);

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Workflow resource not found: " + resourcePath);
        }

        try {
            WorkflowDefinition workflow = yamlMapper.readValue(inputStream, WorkflowDefinition.class);
            workflow = variableSubstitution.substituteWorkflowVariables(workflow);
            workflow.validate();

            logger.info("Successfully loaded workflow from classpath: {}",
                       workflow.getMetadata().getName());

            return workflow;
        } catch (IOException e) {
            throw new IOException("Failed to parse workflow from classpath " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Loads agent configurations from a classpath resource.
     *
     * @param resourcePath classpath resource path
     * @return parsed agent configuration collection
     * @throws IOException if resource cannot be read or parsed
     */
    public AgentConfigCollection loadAgentsFromClasspath(String resourcePath) throws IOException {
        logger.info("Loading agents from classpath: {}", resourcePath);

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Agent config resource not found: " + resourcePath);
        }

        try {
            AgentConfigCollection agentConfig = yamlMapper.readValue(inputStream, AgentConfigCollection.class);
            agentConfig = variableSubstitution.substituteAgentVariables(agentConfig);
            agentConfig.validate();

            logger.info("Successfully loaded agents from classpath: {} agents, {} templates",
                       agentConfig.getAgents() != null ? agentConfig.getAgents().size() : 0,
                       agentConfig.getPromptTemplates() != null ? agentConfig.getPromptTemplates().size() : 0);

            return agentConfig;
        } catch (IOException e) {
            throw new IOException("Failed to parse agent config from classpath " + resourcePath + ": " + e.getMessage(), e);
        }
    }
}
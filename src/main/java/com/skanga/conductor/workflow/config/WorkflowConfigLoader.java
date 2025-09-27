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
 * Handles file system access, YAML parsing, and configuration validation.
 */
public class WorkflowConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowConfigLoader.class);

    private final ObjectMapper yamlMapper;
    private final VariableSubstitution variableSubstitution;

    public WorkflowConfigLoader() {
        this.yamlMapper = createYamlMapper();
        this.variableSubstitution = new VariableSubstitution();
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
     *
     * @param filePath path to the workflow YAML file
     * @return parsed and validated workflow definition
     * @throws IOException if file cannot be read or parsed
     * @throws IllegalArgumentException if configuration is invalid
     */
    public WorkflowDefinition loadWorkflow(String filePath) throws IOException {
        logger.info("Loading workflow from: {}", filePath);

        // Try to load from file system first, then from classpath
        WorkflowDefinition workflow;
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                workflow = yamlMapper.readValue(path.toFile(), WorkflowDefinition.class);
            } else {
                // Try classpath
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
                if (inputStream == null) {
                    throw new IOException("Workflow file not found: " + filePath);
                }
                workflow = yamlMapper.readValue(inputStream, WorkflowDefinition.class);
            }
        } catch (IOException e) {
            logger.error("Failed to parse workflow YAML: {}", e.getMessage());
            throw new IOException("Failed to parse workflow from " + filePath + ": " + e.getMessage(), e);
        }

        // Perform variable substitution
        workflow = variableSubstitution.substituteWorkflowVariables(workflow);

        // Validate the configuration
        try {
            workflow.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid workflow configuration: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid workflow configuration in " + filePath + ": " + e.getMessage(), e);
        }

        logger.info("Successfully loaded workflow: {} (version: {})",
                   workflow.getMetadata().getName(),
                   workflow.getMetadata().getVersion());

        return workflow;
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
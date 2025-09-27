package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowConfigLoaderTest {

    private WorkflowConfigLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new WorkflowConfigLoader();
    }

    @Test
    void shouldLoadValidWorkflowFromFile() throws IOException {
        // Given
        String yamlContent = """
            workflow:
              name: "Test Workflow"
              description: "A test workflow"
              version: "1.0.0"
            settings:
              output_dir: "./output"
              max_retries: 3
              timeout: "10m"
            stages:
              - name: "test-stage"
                agents:
                  primary: "test-agent"
                depends_on: []
            variables:
              test_var: "test_value"
            """;

        Path workflowFile = tempDir.resolve("test-workflow.yaml");
        Files.writeString(workflowFile, yamlContent);

        // When
        WorkflowDefinition workflow = loader.loadWorkflow(workflowFile.toString());

        // Then
        assertNotNull(workflow);
        assertEquals("Test Workflow", workflow.getMetadata().getName());
        assertEquals("A test workflow", workflow.getMetadata().getDescription());
        assertEquals("1.0.0", workflow.getMetadata().getVersion());
        assertEquals("./output", workflow.getSettings().getOutputDir());
        assertEquals(3, workflow.getSettings().getMaxRetries());
        assertEquals(1, workflow.getStages().size());
        assertEquals("test-stage", workflow.getStages().get(0).getName());
        assertEquals("test_value", workflow.getVariables().get("test_var"));
    }

    @Test
    void shouldLoadValidAgentConfigFromFile() throws IOException {
        // Given
        String yamlContent = """
            agents:
              test-agent:
                type: "llm"
                role: "Test Agent"
                provider: "mock"
                model: "test-model"
                prompt_template: "test-template"
                parameters:
                  temperature: 0.7
                  max_tokens: 1000
            prompt_templates:
              test-template:
                system: "Template content"
            """;

        Path agentFile = tempDir.resolve("test-agents.yaml");
        Files.writeString(agentFile, yamlContent);

        // When
        AgentConfigCollection agentConfig = loader.loadAgents(agentFile.toString());

        // Then
        assertNotNull(agentConfig);
        assertEquals(1, agentConfig.getAgents().size());
        assertTrue(agentConfig.getAgents().containsKey("test-agent"));

        AgentDefinition agent = agentConfig.getAgents().get("test-agent");
        assertEquals("llm", agent.getType());
        assertEquals("Test Agent", agent.getRole());
        assertEquals("mock", agent.getProvider());
        assertEquals("test-model", agent.getModel());
        assertEquals("test-template", agent.getPromptTemplate());
        assertEquals(0.7, agent.getParameters().get("temperature"));

        assertEquals(1, agentConfig.getPromptTemplates().size());
        assertEquals("Template content", agentConfig.getPromptTemplates().get("test-template").getSystem());
    }

    @Test
    void shouldLoadValidContextFromFile() throws IOException {
        // Given
        String yamlContent = """
            variables:
              topic: "AI Technology"
              author: "Test Author"
              target_words: 800
            settings:
              debug: true
              verbose: false
            """;

        Path contextFile = tempDir.resolve("test-context.yaml");
        Files.writeString(contextFile, yamlContent);

        // When
        WorkflowContext context = loader.loadContext(contextFile.toString());

        // Then
        assertNotNull(context);
        assertNotNull(context.getData());
        assertNotNull(context.getData());
        // Context contains the parsed YAML structure
    }

    @Test
    void shouldThrowExceptionWhenWorkflowFileNotFound() {
        // When & Then
        IOException exception = assertThrows(IOException.class,
            () -> loader.loadWorkflow("non-existent-file.yaml"));
        assertTrue(exception.getMessage().contains("Workflow file not found"));
    }

    @Test
    void shouldThrowExceptionWhenAgentFileNotFound() {
        // When & Then
        IOException exception = assertThrows(IOException.class,
            () -> loader.loadAgents("non-existent-agents.yaml"));
        assertTrue(exception.getMessage().contains("Agent config file not found"));
    }

    @Test
    void shouldThrowExceptionWhenContextFileNotFound() {
        // When & Then
        IOException exception = assertThrows(IOException.class,
            () -> loader.loadContext("non-existent-context.yaml"));
        assertTrue(exception.getMessage().contains("Context file not found"));
    }

    @Test
    void shouldThrowExceptionForInvalidWorkflowYaml() throws IOException {
        // Given
        String invalidYamlContent = """
            workflow:
              name: "Test Workflow"
              # Missing required fields like stages
            """;

        Path workflowFile = tempDir.resolve("invalid-workflow.yaml");
        Files.writeString(workflowFile, invalidYamlContent);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> loader.loadWorkflow(workflowFile.toString()));
        assertTrue(exception.getMessage().contains("Invalid workflow configuration"));
    }

    @Test
    void shouldThrowExceptionForInvalidAgentConfig() throws IOException {
        // Given
        String invalidYamlContent = """
            agents:
              invalid-agent:
                type: "llm"
                # Missing required role
            """;

        Path agentFile = tempDir.resolve("invalid-agents.yaml");
        Files.writeString(agentFile, invalidYamlContent);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> loader.loadAgents(agentFile.toString()));
        assertTrue(exception.getMessage().contains("Invalid agent configuration"));
    }

    @Test
    void shouldLoadWorkflowFromClasspath() throws IOException {
        // When & Then - this should work if the test resource exists
        // For now we test the exception path
        IOException exception = assertThrows(IOException.class,
            () -> loader.loadWorkflowFromClasspath("non-existent-resource.yaml"));
        assertTrue(exception.getMessage().contains("Workflow resource not found"));
    }

    @Test
    void shouldLoadAgentsFromClasspath() throws IOException {
        // When & Then - this should work if the test resource exists
        // For now we test the exception path
        IOException exception = assertThrows(IOException.class,
            () -> loader.loadAgentsFromClasspath("non-existent-agents.yaml"));
        assertTrue(exception.getMessage().contains("Agent config resource not found"));
    }

    @Test
    void shouldHandleMalformedYaml() throws IOException {
        // Given
        String malformedYaml = """
            workflow:
              name: "Test"
              invalid: [unclosed bracket
            """;

        Path workflowFile = tempDir.resolve("malformed-workflow.yaml");
        Files.writeString(workflowFile, malformedYaml);

        // When & Then
        IOException exception = assertThrows(IOException.class,
            () -> loader.loadWorkflow(workflowFile.toString()));
        assertTrue(exception.getMessage().contains("Failed to parse workflow"));
    }

    @Test
    void shouldLoadWorkflowWithMinimalConfiguration() throws IOException {
        // Given
        String minimalYamlContent = """
            workflow:
              name: "Minimal Workflow"
            stages:
              - name: "minimal-stage"
                agents:
                  primary: "minimal-agent"
            """;

        Path workflowFile = tempDir.resolve("minimal-workflow.yaml");
        Files.writeString(workflowFile, minimalYamlContent);

        // When
        WorkflowDefinition workflow = loader.loadWorkflow(workflowFile.toString());

        // Then
        assertNotNull(workflow);
        assertEquals("Minimal Workflow", workflow.getMetadata().getName());
        assertNotNull(workflow.getSettings()); // Should use defaults
        assertEquals(3, workflow.getSettings().getMaxRetries()); // Default value
        assertEquals(1, workflow.getStages().size());
    }
}
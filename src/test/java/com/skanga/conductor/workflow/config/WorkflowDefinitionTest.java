package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDefinitionTest {

    private WorkflowDefinition workflowDefinition;
    private WorkflowDefinition.WorkflowMetadata metadata;
    private WorkflowDefinition.WorkflowSettings settings;
    private List<WorkflowStage> stages;

    @BeforeEach
    void setUp() {
        workflowDefinition = new WorkflowDefinition();

        metadata = new WorkflowDefinition.WorkflowMetadata();
        metadata.setName("Test Workflow");
        metadata.setDescription("A test workflow");
        metadata.setVersion("1.0.0");

        settings = new WorkflowDefinition.WorkflowSettings();
        settings.setOutputDir("./output");
        settings.setMaxRetries(3);
        settings.setTimeout("10m");
        settings.setTargetWordsPerChapter(800);
        settings.setMaxWordsPerChapter(1200);

        // Create a mock WorkflowStage that passes validation
        WorkflowStage stage = new WorkflowStage();
        stage.setName("test-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        stage.setAgents(agents);

        stages = new ArrayList<>();
        stages.add(stage);

        workflowDefinition.setMetadata(metadata);
        workflowDefinition.setSettings(settings);
        workflowDefinition.setStages(stages);
    }

    @Test
    void shouldCreateWorkflowDefinitionWithAllProperties() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("test_var", "test_value");
        workflowDefinition.setVariables(variables);

        // When & Then
        assertEquals(metadata, workflowDefinition.getMetadata());
        assertEquals(settings, workflowDefinition.getSettings());
        assertEquals(stages, workflowDefinition.getStages());
        assertEquals(variables, workflowDefinition.getVariables());
    }

    @Test
    void shouldValidateValidWorkflowDefinition() {
        // When & Then
        assertDoesNotThrow(() -> workflowDefinition.validate());
    }

    @Test
    void shouldThrowExceptionWhenMetadataIsNull() {
        // Given
        workflowDefinition.setMetadata(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowDefinition.validate());
        assertEquals("Workflow metadata with name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenMetadataNameIsNull() {
        // Given
        metadata.setName(null);
        workflowDefinition.setMetadata(metadata);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowDefinition.validate());
        assertEquals("Workflow metadata with name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenMetadataNameIsEmpty() {
        // Given
        metadata.setName("   "); // Empty/whitespace name
        workflowDefinition.setMetadata(metadata);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowDefinition.validate());
        assertEquals("Workflow metadata with name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStagesIsNull() {
        // Given
        workflowDefinition.setStages(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowDefinition.validate());
        assertEquals("Workflow must have at least one stage", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStagesIsEmpty() {
        // Given
        workflowDefinition.setStages(new ArrayList<>());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowDefinition.validate());
        assertEquals("Workflow must have at least one stage", exception.getMessage());
    }

    @Test
    void shouldCreateDefaultSettingsWhenSettingsIsNull() {
        // Given
        workflowDefinition.setSettings(null);

        // When
        workflowDefinition.validate();

        // Then
        assertNotNull(workflowDefinition.getSettings());
        assertEquals(3, workflowDefinition.getSettings().getMaxRetries()); // Default value
    }

    @Test
    void shouldValidateAllStages() {
        // Given - create an invalid stage
        WorkflowStage invalidStage = new WorkflowStage();
        invalidStage.setName("invalid-stage");
        // Missing required fields that would cause validation to fail

        stages.add(invalidStage);
        workflowDefinition.setStages(stages);

        // When & Then
        // This should throw an exception during stage validation
        assertThrows(IllegalArgumentException.class, () -> workflowDefinition.validate());
    }

    @Test
    void shouldTestWorkflowMetadata() {
        // Given
        WorkflowDefinition.WorkflowMetadata testMetadata = new WorkflowDefinition.WorkflowMetadata();

        // When
        testMetadata.setName("Test Name");
        testMetadata.setDescription("Test Description");
        testMetadata.setVersion("2.0.0");

        // Then
        assertEquals("Test Name", testMetadata.getName());
        assertEquals("Test Description", testMetadata.getDescription());
        assertEquals("2.0.0", testMetadata.getVersion());
    }

    @Test
    void shouldTestWorkflowSettings() {
        // Given
        WorkflowDefinition.WorkflowSettings testSettings = new WorkflowDefinition.WorkflowSettings();

        // When
        testSettings.setOutputDir("./custom-output");
        testSettings.setMaxRetries(5);
        testSettings.setTimeout("30m");
        testSettings.setTargetWordsPerChapter(1000);
        testSettings.setMaxWordsPerChapter(1500);

        // Then
        assertEquals("./custom-output", testSettings.getOutputDir());
        assertEquals(5, testSettings.getMaxRetries());
        assertEquals("30m", testSettings.getTimeout());
        assertEquals(1000, testSettings.getTargetWordsPerChapter());
        assertEquals(1500, testSettings.getMaxWordsPerChapter());
    }

    @Test
    void shouldHaveDefaultValuesInWorkflowSettings() {
        // Given
        WorkflowDefinition.WorkflowSettings defaultSettings = new WorkflowDefinition.WorkflowSettings();

        // Then
        assertEquals(3, defaultSettings.getMaxRetries());
        assertEquals("10m", defaultSettings.getTimeout());
        assertEquals(800, defaultSettings.getTargetWordsPerChapter());
        assertEquals(1200, defaultSettings.getMaxWordsPerChapter());
        // outputDir has no default and should be null
        assertNull(defaultSettings.getOutputDir());
    }

    @Test
    void shouldSetAndGetVariables() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("topic", "AI Technology");
        variables.put("author", "Test Author");
        variables.put("target_words", 1000);
        variables.put("config", Map.of("debug", true, "verbose", false));

        // When
        workflowDefinition.setVariables(variables);

        // Then
        assertEquals(variables, workflowDefinition.getVariables());
        assertEquals("AI Technology", workflowDefinition.getVariables().get("topic"));
        assertEquals("Test Author", workflowDefinition.getVariables().get("author"));
        assertEquals(1000, workflowDefinition.getVariables().get("target_words"));
        assertInstanceOf(Map.class, workflowDefinition.getVariables().get("config"));
    }

    @Test
    void shouldAllowNullVariables() {
        // Given & When
        workflowDefinition.setVariables(null);

        // Then
        assertNull(workflowDefinition.getVariables());
        // Should still validate successfully
        assertDoesNotThrow(() -> workflowDefinition.validate());
    }

    @Test
    void shouldCreateWorkflowDefinitionWithDefaultConstructor() {
        // Given
        WorkflowDefinition newWorkflow = new WorkflowDefinition();

        // Then
        assertNull(newWorkflow.getMetadata());
        assertNull(newWorkflow.getSettings());
        assertNull(newWorkflow.getStages());
        assertNull(newWorkflow.getVariables());
    }
}
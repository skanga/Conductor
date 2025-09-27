package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStageTest {

    private WorkflowStage workflowStage;

    @BeforeEach
    void setUp() {
        workflowStage = new WorkflowStage();
    }

    @Test
    void shouldCreateWorkflowStageWithDefaults() {
        // Then
        assertNull(workflowStage.getName());
        assertNull(workflowStage.getDescription());
        assertNull(workflowStage.getDependsOn());
        assertFalse(workflowStage.isParallel()); // Default is false
        assertNull(workflowStage.getAgents());
        assertNull(workflowStage.getApproval());
        assertNull(workflowStage.getOutputs());
        assertNull(workflowStage.getRetryLimit());
        assertNull(workflowStage.getIteration());
    }

    @Test
    void shouldSetAndGetBasicProperties() {
        // When
        workflowStage.setName("test-stage");
        workflowStage.setDescription("A test workflow stage");
        workflowStage.setParallel(true);

        // Then
        assertEquals("test-stage", workflowStage.getName());
        assertEquals("A test workflow stage", workflowStage.getDescription());
        assertTrue(workflowStage.isParallel());
    }

    @Test
    void shouldSetAndGetDependencies() {
        // Given
        List<String> dependencies = Arrays.asList("stage1", "stage2", "stage3");

        // When
        workflowStage.setDependsOn(dependencies);

        // Then
        assertEquals(dependencies, workflowStage.getDependsOn());
        assertEquals(3, workflowStage.getDependsOn().size());
        assertTrue(workflowStage.getDependsOn().contains("stage1"));
        assertTrue(workflowStage.getDependsOn().contains("stage2"));
        assertTrue(workflowStage.getDependsOn().contains("stage3"));
    }

    @Test
    void shouldSetAndGetAgents() {
        // Given
        Map<String, String> agents = new HashMap<>();
        agents.put("writer", "book-writer");
        agents.put("reviewer", "content-reviewer");
        agents.put("editor", "grammar-editor");

        // When
        workflowStage.setAgents(agents);

        // Then
        assertEquals(agents, workflowStage.getAgents());
        assertEquals("book-writer", workflowStage.getAgents().get("writer"));
        assertEquals("content-reviewer", workflowStage.getAgents().get("reviewer"));
        assertEquals("grammar-editor", workflowStage.getAgents().get("editor"));
    }

    @Test
    void shouldSetAndGetOutputs() {
        // Given
        List<String> outputs = Arrays.asList("chapter.md", "summary.txt", "metadata.json");

        // When
        workflowStage.setOutputs(outputs);

        // Then
        assertEquals(outputs, workflowStage.getOutputs());
        assertEquals(3, workflowStage.getOutputs().size());
        assertTrue(workflowStage.getOutputs().contains("chapter.md"));
        assertTrue(workflowStage.getOutputs().contains("summary.txt"));
        assertTrue(workflowStage.getOutputs().contains("metadata.json"));
    }

    @Test
    void shouldSetAndGetRetryLimit() {
        // When
        workflowStage.setRetryLimit(5);

        // Then
        assertEquals(5, workflowStage.getRetryLimit());
    }

    @Test
    void shouldSetAndGetIterationConfig() {
        // Given
        IterationConfig iterationConfig = new IterationConfig();

        // When
        workflowStage.setIteration(iterationConfig);

        // Then
        assertEquals(iterationConfig, workflowStage.getIteration());
    }

    @Test
    void shouldValidateValidStage() {
        // Given
        workflowStage.setName("valid-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);

        // When & Then
        assertDoesNotThrow(() -> workflowStage.validate());
    }

    @Test
    void shouldThrowExceptionWhenNameIsMissing() {
        // Given
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);
        // Name is not set

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertEquals("Stage name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Given
        workflowStage.setName("   "); // Empty/whitespace name
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertEquals("Stage name is required", exception.getMessage());
    }

    @Test
    void shouldValidateStageWithMinimalConfiguration() {
        // Given - only required fields
        workflowStage.setName("minimal-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);

        // When & Then
        assertDoesNotThrow(() -> workflowStage.validate());
    }

    @Test
    void shouldAllowEmptyDependencies() {
        // Given
        workflowStage.setName("no-deps-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);
        workflowStage.setDependsOn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> workflowStage.validate());
        assertNotNull(workflowStage.getDependsOn());
        assertTrue(workflowStage.getDependsOn().isEmpty());
    }

    @Test
    void shouldAllowNullDependencies() {
        // Given
        workflowStage.setName("null-deps-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);
        workflowStage.setDependsOn(null);

        // When & Then
        assertDoesNotThrow(() -> workflowStage.validate());
        assertNull(workflowStage.getDependsOn());
    }

    @Test
    void shouldThrowExceptionWhenAgentsIsNull() {
        // Given
        workflowStage.setName("null-agents-stage");
        workflowStage.setAgents(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertTrue(exception.getMessage().contains("must have at least one agent"));
    }

    @Test
    void shouldThrowExceptionWhenAgentsIsEmpty() {
        // Given
        workflowStage.setName("empty-agents-stage");
        workflowStage.setAgents(Collections.emptyMap());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertTrue(exception.getMessage().contains("must have at least one agent"));
    }

    @Test
    void shouldAllowNullRetryLimit() {
        // Given
        workflowStage.setName("null-retry-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);
        workflowStage.setRetryLimit(null);

        // When & Then
        assertDoesNotThrow(() -> workflowStage.validate());
        assertNull(workflowStage.getRetryLimit());
    }


    @Test
    void shouldCreateComplexWorkflowStage() {
        // Given
        List<String> dependencies = Arrays.asList("init-stage", "prep-stage");
        Map<String, String> agents = new HashMap<>();
        agents.put("primary", "main-agent");
        agents.put("secondary", "backup-agent");
        List<String> outputs = Arrays.asList("result.txt", "log.txt");

        // When
        workflowStage.setName("complex-stage");
        workflowStage.setDescription("A complex workflow stage");
        workflowStage.setDependsOn(dependencies);
        workflowStage.setParallel(true);
        workflowStage.setAgents(agents);
        workflowStage.setOutputs(outputs);
        workflowStage.setRetryLimit(3);
        // Then
        assertEquals("complex-stage", workflowStage.getName());
        assertEquals("A complex workflow stage", workflowStage.getDescription());
        assertEquals(dependencies, workflowStage.getDependsOn());
        assertTrue(workflowStage.isParallel());
        assertEquals(agents, workflowStage.getAgents());
        assertEquals(outputs, workflowStage.getOutputs());
        assertEquals(3, workflowStage.getRetryLimit());

        // Should validate successfully
        assertDoesNotThrow(() -> workflowStage.validate());
    }

    @Test
    void shouldGetPrimaryAgentId() {
        // Given
        Map<String, String> agents = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
        agents.put("writer", "book-writer");
        agents.put("reviewer", "content-reviewer");
        workflowStage.setAgents(agents);

        // When & Then
        assertEquals("book-writer", workflowStage.getPrimaryAgentId());
    }

    @Test
    void shouldReturnNullPrimaryAgentIdWhenAgentsIsNull() {
        // Given
        workflowStage.setAgents(null);

        // When & Then
        assertNull(workflowStage.getPrimaryAgentId());
    }

    @Test
    void shouldReturnNullPrimaryAgentIdWhenAgentsIsEmpty() {
        // Given
        workflowStage.setAgents(Collections.emptyMap());

        // When & Then
        assertNull(workflowStage.getPrimaryAgentId());
    }

    @Test
    void shouldGetAgentIdByRole() {
        // Given
        Map<String, String> agents = new HashMap<>();
        agents.put("writer", "book-writer");
        agents.put("reviewer", "content-reviewer");
        agents.put("editor", "grammar-editor");
        workflowStage.setAgents(agents);

        // When & Then
        assertEquals("book-writer", workflowStage.getAgentId("writer"));
        assertEquals("content-reviewer", workflowStage.getAgentId("reviewer"));
        assertEquals("grammar-editor", workflowStage.getAgentId("editor"));
        assertNull(workflowStage.getAgentId("non-existent"));
    }

    @Test
    void shouldReturnNullAgentIdWhenAgentsIsNull() {
        // Given
        workflowStage.setAgents(null);

        // When & Then
        assertNull(workflowStage.getAgentId("any-role"));
    }

    @Test
    void shouldCheckIfRequiresApproval() {
        // Given - no approval
        workflowStage.setApproval(null);

        // When & Then
        assertFalse(workflowStage.requiresApproval());

        // Given - approval not required
        WorkflowStage.StageApproval approval = new WorkflowStage.StageApproval();
        approval.setRequired(false);
        workflowStage.setApproval(approval);

        // When & Then
        assertFalse(workflowStage.requiresApproval());

        // Given - approval required
        approval.setRequired(true);
        workflowStage.setApproval(approval);

        // When & Then
        assertTrue(workflowStage.requiresApproval());
    }

    @Test
    void shouldCheckIfIsIterative() {
        // Given - no iteration config
        workflowStage.setIteration(null);

        // When & Then
        assertFalse(workflowStage.isIterative());

        // Given - with iteration config
        IterationConfig iterationConfig = new IterationConfig();
        workflowStage.setIteration(iterationConfig);

        // When & Then
        assertTrue(workflowStage.isIterative());
    }

    @Test
    void shouldCheckIfIsParallelIteration() {
        // Given - no iteration config
        workflowStage.setIteration(null);

        // When & Then
        assertFalse(workflowStage.isParallelIteration());

        // Given - iteration config with parallel disabled
        IterationConfig iterationConfig = new IterationConfig();
        iterationConfig.setParallel(false);
        workflowStage.setIteration(iterationConfig);

        // When & Then
        assertFalse(workflowStage.isParallelIteration());

        // Given - iteration config with parallel enabled
        iterationConfig.setParallel(true);
        workflowStage.setIteration(iterationConfig);

        // When & Then
        assertTrue(workflowStage.isParallelIteration());
    }

    @Test
    void shouldCheckIfIsEffectivelyParallel() {
        // Given - no parallel flags
        workflowStage.setParallel(false);
        workflowStage.setIteration(null);

        // When & Then
        assertFalse(workflowStage.isEffectivelyParallel());

        // Given - stage-level parallel enabled
        workflowStage.setParallel(true);

        // When & Then
        assertTrue(workflowStage.isEffectivelyParallel());

        // Given - stage-level parallel disabled, but iteration parallel enabled
        workflowStage.setParallel(false);
        IterationConfig iterationConfig = new IterationConfig();
        iterationConfig.setParallel(true);
        workflowStage.setIteration(iterationConfig);

        // When & Then
        assertTrue(workflowStage.isEffectivelyParallel());
    }

    @Test
    void shouldGetMaxConcurrentIterations() {
        // Given - non-iterative stage
        workflowStage.setIteration(null);

        // When & Then
        assertEquals(1, workflowStage.getMaxConcurrentIterations());

        // Given - iterative stage with parallel enabled
        IterationConfig iterationConfig = new IterationConfig();
        iterationConfig.setParallel(true); // Need to enable parallel for maxConcurrent to take effect
        iterationConfig.setMaxConcurrent(5);
        workflowStage.setIteration(iterationConfig);

        // When & Then
        assertEquals(5, workflowStage.getMaxConcurrentIterations());
    }

    @Test
    void shouldTestStageApprovalClass() {
        // Given
        WorkflowStage.StageApproval approval = new WorkflowStage.StageApproval();

        // Then - test defaults
        assertFalse(approval.isRequired());
        assertFalse(approval.isPerItem());
        assertEquals("5m", approval.getTimeout());
        assertFalse(approval.isAutoApprove());

        // When - set all properties
        approval.setRequired(true);
        approval.setPerItem(true);
        approval.setTimeout("10m");
        approval.setAutoApprove(true);

        // Then
        assertTrue(approval.isRequired());
        assertTrue(approval.isPerItem());
        assertEquals("10m", approval.getTimeout());
        assertTrue(approval.isAutoApprove());
    }

    @Test
    void shouldThrowExceptionWhenAgentRoleIsEmpty() {
        // Given
        workflowStage.setName("test-stage");
        Map<String, String> agents = new HashMap<>();
        agents.put("", "agent-id"); // Empty role
        agents.put("valid-role", "valid-agent-id");
        workflowStage.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertTrue(exception.getMessage().contains("Agent role cannot be empty"));
    }

    @Test
    void shouldThrowExceptionWhenAgentIdIsEmpty() {
        // Given
        workflowStage.setName("test-stage");
        Map<String, String> agents = new HashMap<>();
        agents.put("valid-role", ""); // Empty agent ID
        workflowStage.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertTrue(exception.getMessage().contains("Agent ID cannot be empty"));
    }

    @Test
    void shouldValidateIterationConfiguration() {
        // Given
        workflowStage.setName("iterative-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);

        IterationConfig iterationConfig = new IterationConfig();
        iterationConfig.setType(IterationConfig.IterationType.COUNT_BASED);
        iterationConfig.setCount("5"); // Set required count for count-based iteration
        workflowStage.setIteration(iterationConfig);

        // When & Then
        assertDoesNotThrow(() -> workflowStage.validate());
    }

    @Test
    void shouldThrowExceptionForInvalidIterationConfiguration() {
        // Given
        workflowStage.setName("invalid-iterative-stage");
        Map<String, String> agents = Map.of("primary", "test-agent");
        workflowStage.setAgents(agents);

        // Create an invalid iteration config (this would need to be implemented in IterationConfig)
        IterationConfig iterationConfig = new IterationConfig() {
            @Override
            public void validate() throws IllegalArgumentException {
                throw new IllegalArgumentException("Invalid iteration config");
            }
        };
        workflowStage.setIteration(iterationConfig);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> workflowStage.validate());
        assertTrue(exception.getMessage().contains("Invalid iteration configuration"));
    }
}
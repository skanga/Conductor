package com.skanga.conductor.engine;

import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for verifying the fix for NullPointerException in UnifiedWorkflowEngine.execute() method.
 */
class DefaultWorkflowEngineNullPointerFixTest {

    private DefaultWorkflowEngine engine;
    private MemoryStore memoryStore;

    @BeforeEach
    void setUp() throws Exception {
        memoryStore = new MemoryStore();
        SubAgentRegistry registry = new SubAgentRegistry();
        Orchestrator orchestrator = new Orchestrator(registry, memoryStore);
        engine = new DefaultWorkflowEngine(orchestrator);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (memoryStore != null) {
            memoryStore.close();
        }
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    @DisplayName("Execute method should not throw NullPointerException when LLM provider is null")
    void testExecuteWithNullLLMProviderDoesNotThrowNPE() {
        // This test verifies that the execute(String... inputs) method
        // no longer throws a NullPointerException when the default agent
        // definition has a null LLM provider

        assertDoesNotThrow(() -> {
            WorkflowEngine.WorkflowResult result = engine.execute("Test input prompt");

            // Verify the result is not null and execution completed
            assertNotNull(result, "Workflow result should not be null");
            assertNotNull(result.getWorkflowName(), "Workflow name should not be null");

            // The execution should complete without throwing NPE
            // The actual success/failure depends on the mock provider behavior
            // but the important thing is that we don't get NPE
        });
    }

    @Test
    @DisplayName("Execute method with multiple inputs should work correctly")
    void testExecuteWithMultipleInputs() {
        assertDoesNotThrow(() -> {
            WorkflowEngine.WorkflowResult result = engine.execute(
                "First stage prompt",
                "Second stage prompt",
                "Third stage prompt"
            );

            assertNotNull(result, "Workflow result should not be null");
            assertNotNull(result.getWorkflowName(), "Workflow name should not be null");

            // Verify timing information is populated
            assertTrue(result.getStartTime() > 0, "Start time should be positive");
            assertTrue(result.getEndTime() >= result.getStartTime(), "End time should be >= start time");
        });
    }

    @Test
    @DisplayName("Engine should be ready and properly configured")
    void testEngineReadiness() {
        assertTrue(engine.isReady(), "Engine should be ready");
        assertEquals("DefaultWorkflowEngine", engine.getEngineName());

        WorkflowEngine.EngineStatus status = engine.getStatus();
        assertNotNull(status, "Engine status should not be null");
        assertTrue(status.isConfigured(), "Engine should be configured");
    }
}
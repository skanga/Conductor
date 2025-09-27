package com.skanga.conductor.testbase;

import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.demo.DemoDatabaseManager;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.orchestration.PlannerOrchestrator;
import com.skanga.conductor.orchestration.MockPlannerProvider;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.tools.ToolRegistry;
import com.skanga.conductor.tools.FileReadTool;
import com.skanga.conductor.tools.WebSearchTool;
import com.skanga.conductor.tools.CodeRunnerTool;
import com.skanga.conductor.tools.TextToSpeechTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Base test class for integration tests that require full Conductor ecosystem setup.
 * <p>
 * This class extends ConductorTestBase and provides comprehensive integration test
 * infrastructure including:
 * </p>
 * <ul>
 * <li>Complete orchestrator setup with planning capabilities</li>
 * <li>Isolated database management for integration scenarios</li>
 * <li>Full tool registry with real and mock tools</li>
 * <li>Multiple LLM provider configurations</li>
 * <li>Agent registry management</li>
 * <li>End-to-end workflow execution utilities</li>
 * <li>Performance measurement and validation</li>
 * </ul>
 * <p>
 * Integration tests using this base class should be ordered using {@code @TestMethodOrder}
 * annotations to ensure proper execution sequence for complex scenarios.
 * </p>
 *
 * @since 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class IntegrationTestBase extends ConductorTestBase {

    protected static DemoDatabaseManager dbManager;
    protected static MemoryStore integrationMemoryStore;
    protected static String integrationWorkflowId;

    protected LLMProvider mockProvider;
    protected MockPlannerProvider plannerProvider;
    protected ToolRegistry toolRegistry;
    protected SubAgentRegistry agentRegistry;
    protected Orchestrator orchestrator;
    protected PlannerOrchestrator plannerOrchestrator;

    @BeforeAll
    static void setUpIntegration() throws Exception {
        // Create unique workflow ID for this integration test class
        integrationWorkflowId = "integration-test-" + System.currentTimeMillis();

        // Create isolated database for all integration tests in this class
        dbManager = new DemoDatabaseManager(integrationWorkflowId);
        integrationMemoryStore = dbManager.createIsolatedMemoryStore();
    }

    @AfterAll
    static void tearDownIntegration() throws Exception {
        // Clean up integration resources
        if (integrationMemoryStore != null) {
            try {
                integrationMemoryStore.close();
            } catch (Exception e) {
                System.err.println("Warning: Failed to close integration memory store: " + e.getMessage());
            }
        }

        if (dbManager != null) {
            try {
                dbManager.close();
            } catch (Exception e) {
                System.err.println("Warning: Failed to close integration database manager: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        // Initialize providers
        mockProvider = createIntegrationLLMProvider();
        plannerProvider = new MockPlannerProvider();

        // Create comprehensive tool registry
        toolRegistry = createIntegrationToolRegistry();

        // Create agent registry and orchestrators
        agentRegistry = new SubAgentRegistry();
        orchestrator = new Orchestrator(agentRegistry, integrationMemoryStore);
        plannerOrchestrator = new PlannerOrchestrator(agentRegistry, integrationMemoryStore);

        logger.info("Integration test setup complete for: {}", getClass().getSimpleName());
    }

    /**
     * Creates an LLM provider suitable for integration testing.
     * Override this method to customize the provider behavior.
     *
     * @return a configured LLM provider
     */
    protected LLMProvider createIntegrationLLMProvider() {
        return new MockLLMProvider("integration-test-" + getClass().getSimpleName());
    }

    /**
     * Creates a comprehensive tool registry for integration testing.
     * Override this method to customize the tool registry.
     *
     * @return a configured tool registry
     */
    protected ToolRegistry createIntegrationToolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        // Add mock tools for testing
        registry.register(new MockTool("test_tool", "Integration test output"));
        registry.register(new MockTool("echo_tool", "Echo: "));
        registry.register(new MockTool("success_tool", "Operation successful"));
        registry.register(new MockTool("data_tool", "Data processed successfully"));

        // Add real tools for comprehensive testing
        try {
            registry.register(new FileReadTool());
            registry.register(new WebSearchTool());
            registry.register(new CodeRunnerTool());
            registry.register(new TextToSpeechTool());
        } catch (Exception e) {
            logger.warn("Failed to register some real tools for integration testing: {}", e.getMessage());
        }

        return registry;
    }

    /**
     * Gets the integration memory store shared across all tests in this class.
     *
     * @return the integration memory store
     */
    protected MemoryStore getIntegrationMemoryStore() {
        return integrationMemoryStore;
    }

    /**
     * Gets the integration workflow ID for this test class.
     *
     * @return the integration workflow ID
     */
    protected String getIntegrationWorkflowId() {
        return integrationWorkflowId;
    }

    /**
     * Executes a timed operation and returns both the result and execution time.
     *
     * @param operation the operation to execute
     * @param <T> the return type
     * @return a timed result containing both the result and execution time
     * @throws Exception if the operation fails
     */
    protected <T> TimedResult<T> executeTimedOperation(TimedOperation<T> operation) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            T result = operation.execute();
            long duration = System.currentTimeMillis() - startTime;
            return new TimedResult<>(result, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Timed operation failed after {}ms: {}", duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Asserts that an operation completes within the specified time limit.
     *
     * @param operation the operation to execute
     * @param maxDurationMs the maximum allowed duration in milliseconds
     * @param <T> the return type
     * @return the operation result
     * @throws Exception if the operation fails
     */
    protected <T> T assertCompletesWithin(TimedOperation<T> operation, long maxDurationMs) throws Exception {
        TimedResult<T> result = executeTimedOperation(operation);
        if (result.getDurationMs() > maxDurationMs) {
            throw new AssertionError(String.format(
                "Operation took %dms but should have completed within %dms",
                result.getDurationMs(), maxDurationMs));
        }
        return result.getResult();
    }

    /**
     * Measures the performance of an operation and logs the results.
     *
     * @param operationName the name of the operation for logging
     * @param operation the operation to measure
     * @param <T> the return type
     * @return the operation result
     * @throws Exception if the operation fails
     */
    protected <T> T measurePerformance(String operationName, TimedOperation<T> operation) throws Exception {
        logger.info("Starting performance measurement for: {}", operationName);
        TimedResult<T> result = executeTimedOperation(operation);
        logger.info("Performance measurement for '{}': {}ms", operationName, result.getDurationMs());
        return result.getResult();
    }

    /**
     * Creates a comprehensive test scenario context with multiple agents and workflows.
     *
     * @return a test scenario context
     */
    protected TestScenarioContext createTestScenario() {
        return new TestScenarioContext(
            orchestrator,
            plannerOrchestrator,
            agentRegistry,
            toolRegistry,
            integrationMemoryStore,
            mockProvider
        );
    }

    /**
     * Functional interface for timed operations.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    protected interface TimedOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Result container for timed operations.
     *
     * @param <T> the result type
     */
    protected static class TimedResult<T> {
        private final T result;
        private final long durationMs;

        public TimedResult(T result, long durationMs) {
            this.result = result;
            this.durationMs = durationMs;
        }

        public T getResult() {
            return result;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }

    /**
     * Context object containing all the components needed for comprehensive testing scenarios.
     */
    protected static class TestScenarioContext {
        private final Orchestrator orchestrator;
        private final PlannerOrchestrator plannerOrchestrator;
        private final SubAgentRegistry agentRegistry;
        private final ToolRegistry toolRegistry;
        private final MemoryStore memoryStore;
        private final LLMProvider llmProvider;

        public TestScenarioContext(Orchestrator orchestrator,
                                 PlannerOrchestrator plannerOrchestrator,
                                 SubAgentRegistry agentRegistry,
                                 ToolRegistry toolRegistry,
                                 MemoryStore memoryStore,
                                 LLMProvider llmProvider) {
            this.orchestrator = orchestrator;
            this.plannerOrchestrator = plannerOrchestrator;
            this.agentRegistry = agentRegistry;
            this.toolRegistry = toolRegistry;
            this.memoryStore = memoryStore;
            this.llmProvider = llmProvider;
        }

        public Orchestrator getOrchestrator() { return orchestrator; }
        public PlannerOrchestrator getPlannerOrchestrator() { return plannerOrchestrator; }
        public SubAgentRegistry getAgentRegistry() { return agentRegistry; }
        public ToolRegistry getToolRegistry() { return toolRegistry; }
        public MemoryStore getMemoryStore() { return memoryStore; }
        public LLMProvider getLlmProvider() { return llmProvider; }
    }
}
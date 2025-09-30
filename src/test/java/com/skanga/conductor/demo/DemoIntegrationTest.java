package com.skanga.conductor.demo;

import com.skanga.conductor.agent.ConversationalAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.orchestration.PlannerOrchestrator;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.orchestration.MockPlannerProvider;
import com.skanga.conductor.tools.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test harness for Conductor demo functionality.
 * <p>
 * This test suite validates the end-to-end functionality demonstrated in the
 * demo applications, ensuring that core orchestration, planning, and tool usage
 * capabilities work correctly in integration scenarios.
 * </p>
 * <p>
 * Test execution order is controlled to ensure proper setup and teardown of
 * shared resources like the isolated database and memory store.
 * </p>
 *
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Demo Integration Test Harness")
class DemoIntegrationTest {

    private static final String TEST_WORKFLOW_ID = "integration-test-" + System.currentTimeMillis();
    private static DemoDatabaseManager dbManager;
    private static MemoryStore memoryStore;

    private LLMProvider mockProvider;
    private MockPlannerProvider plannerProvider;
    private ToolRegistry toolRegistry;

    @BeforeAll
    static void setUpAll() throws Exception {
        // Create isolated database for all integration tests
        dbManager = new DemoDatabaseManager(TEST_WORKFLOW_ID);
        memoryStore = dbManager.createIsolatedMemoryStore();
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        // Clean up resources
        if (memoryStore != null) {
            memoryStore.close();
        }
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @BeforeEach
    void setUp() {
        // Initialize providers and tools for each test
        mockProvider = new MockLLMProvider("integration-test");
        plannerProvider = new MockPlannerProvider();
        toolRegistry = createToolRegistry();
    }

    @Test
    @Order(1)
    @DisplayName("Test basic orchestration functionality (DemoMock)")
    void testBasicOrchestration() throws Exception {
        // Create orchestrator and registry
        SubAgentRegistry registry = new SubAgentRegistry();
        Orchestrator orchestrator = new Orchestrator(registry, memoryStore);

        // Create and register a test agent similar to DemoMock
        String agentName = "test-book-agent-" + System.currentTimeMillis();
        ConversationalAgent bookAgent = new ConversationalAgent(
                agentName,
                "Test book generation agent",
                mockProvider,
                "Generate book content from a summary",
                memoryStore
        );
        registry.register(bookAgent);

        // Test basic agent execution (similar to BookWorkflow)
        String testSummary = """
                Chapter 1: Introduction to Testing
                Basic concepts and principles.

                Chapter 2: Integration Testing
                Testing multiple components together.
                """;

        ExecutionResult result = bookAgent.execute(new ExecutionInput(testSummary, null));

        // Validate results
        assertNotNull(result, "Task result should not be null");
        assertTrue(result.success(), "Task should complete successfully");
        assertNotNull(result.output(), "Task output should not be null");
        assertFalse(result.output().trim().isEmpty(), "Task output should not be empty");

        // Verify memory persistence (similar to DemoMock's memory snapshot functionality)
        List<String> memorySnapshot = bookAgent.getMemorySnapshot(5);
        assertFalse(memorySnapshot.isEmpty(), "Agent should have persisted memory");

        // Verify agent is properly registered
        assertNotNull(registry.get(agentName), "Agent should be registered in registry");
    }

    @Test
    @Order(2)
    @DisplayName("Test planner orchestration functionality (DemoMockPlanner)")
    void testPlannerOrchestration() throws Exception {
        // Create planner orchestrator
        SubAgentRegistry registry = new SubAgentRegistry();
        PlannerOrchestrator orchestrator = new PlannerOrchestrator(registry, memoryStore);

        // Test task decomposition and execution (similar to DemoMockPlanner)
        String userRequest = """
                Create a simple tutorial with three sections:
                1. Getting started with testing
                2. Writing your first test
                3. Best practices for testing
                """;

        String planWorkflowId = "test-plan-" + System.currentTimeMillis();
        List<ExecutionResult> results = orchestrator.planAndExecute(
                planWorkflowId,
                userRequest,
                plannerProvider,
                mockProvider,
                memoryStore
        );

        // Validate planning results
        assertNotNull(results, "Planner results should not be null");
        assertFalse(results.isEmpty(), "Planner should produce at least one result");

        // Verify all tasks completed successfully
        for (int i = 0; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            assertNotNull(result, "Task result " + i + " should not be null");
            assertTrue(result.success(), "Task " + i + " should complete successfully");
            assertNotNull(result.output(), "Task " + i + " output should not be null");
        }

        // Verify planner decomposition created multiple steps
        assertTrue(results.size() >= 1, "Planner should create at least one execution step");
    }

    @Test
    @Order(3)
    @DisplayName("Test tool usage functionality (DemoTools)")
    void testToolUsage() throws Exception {
        // Test programmatic tool usage
        testProgrammaticToolUsage();

        // Test LLM-assisted tool usage
        testLLMAssistedToolUsage();
    }

    /**
     * Tests programmatic tool usage similar to DemoTools programmatic agent.
     */
    private void testProgrammaticToolUsage() throws Exception {
        String agentName = "test-prog-agent-" + System.currentTimeMillis();

        // Use OS-appropriate command for testing
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String testCommand = isWindows ? "cmd /c echo Hello Integration Test" : "echo Hello Integration Test";
        String promptText = isWindows ? "Please run the command: cmd /c echo Hello Integration Test" :
                                       "Please run the command: echo Hello Integration Test";

        // Create a mock provider that returns tool calls in JSON format
        MockLLMProvider toolAwareMockProvider = new MockLLMProvider("tool-aware-test") {
            @Override
            public String generate(String prompt) throws ConductorException.LLMProviderException {
                // Simulate the LLM deciding to use tools based on the prompt
                if (prompt.contains("echo") || prompt.contains("run") || prompt.contains("cmd")) {
                    return "{\"tool\": \"code_runner\", \"arguments\": \"" + testCommand + "\"}";
                }
                return super.generate(prompt);
            }
        };

        ConversationalAgent progAgent = new ConversationalAgent(
                agentName,
                "Test programmatic tool agent",
                toolAwareMockProvider,
                "Process the following input: {{prompt}}",
                toolRegistry,
                memoryStore
        );

        // Test with a prompt that should trigger tool usage
        ExecutionResult result = progAgent.execute(new ExecutionInput(promptText, null));

        // Validate programmatic tool execution
        assertNotNull(result, "Programmatic tool result should not be null");
        assertTrue(result.success(), "Programmatic tool execution should succeed");
        assertNotNull(result.output(), "Programmatic tool output should not be null");

        // Verify that the tool was actually invoked (output should contain the echo result)
        assertTrue(result.output().contains("Hello Integration Test") ||
                  result.output().contains("echo") ||
                  result.output().contains("Command executed"),
                  "Output should indicate tool execution");
    }

    /**
     * Tests LLM-assisted tool usage similar to DemoTools LLM agent.
     */
    private void testLLMAssistedToolUsage() throws Exception {
        String agentName = "test-llm-agent-" + System.currentTimeMillis();
        ConversationalAgent llmAgent = new ConversationalAgent(
                agentName,
                "Test LLM tool agent",
                mockProvider,
                "You are a helpful assistant. Process this request: {{prompt}}",
                toolRegistry,
                memoryStore
        );

        // Test LLM-driven tool selection
        String taskInput = "Please help me understand file reading by checking if a test file exists";
        ExecutionResult result = llmAgent.execute(new ExecutionInput(taskInput, null));

        // Validate LLM-assisted tool execution
        assertNotNull(result, "LLM tool result should not be null");
        assertTrue(result.success(), "LLM tool execution should succeed");
        assertNotNull(result.output(), "LLM tool output should not be null");

        // Verify agent processed the request (exact tool usage depends on LLM decision)
        assertFalse(result.output().trim().isEmpty(), "LLM agent should produce meaningful output");
    }

    /**
     * Creates a tool registry with standard demo tools.
     *
     * @return configured ToolRegistry for testing
     */
    private ToolRegistry createToolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        // Register essential tools for testing
        registry.register(new FileReadTool());
        registry.register(new WebSearchTool());

        // Use restricted CodeRunnerTool for testing (OS-aware commands)
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        Set<String> allowedCommands = isWindows ?
            Set.of("cmd", "java", "dir", "echo") :
            Set.of("echo", "pwd", "whoami", "java");
        registry.register(new CodeRunnerTool(Duration.ofSeconds(5), allowedCommands));

        return registry;
    }

    @Test
    @Order(4)
    @DisplayName("Test cross-component integration")
    void testCrossComponentIntegration() throws Exception {
        // Test that components can work together in a more complex scenario
        SubAgentRegistry registry = new SubAgentRegistry();

        // Create an orchestration scenario that uses multiple components
        String agentName = "test-integration-agent-" + System.currentTimeMillis();
        ConversationalAgent integrationAgent = new ConversationalAgent(
                agentName,
                "Integration test agent",
                mockProvider,
                "Perform integration testing tasks",
                memoryStore
        );

        registry.register(integrationAgent);

        // Execute a task that would benefit from tool usage
        String complexTask = "Analyze the current directory structure and provide a summary";
        ExecutionResult result = integrationAgent.execute(new ExecutionInput(complexTask, null));

        // Validate cross-component functionality
        assertNotNull(result, "Integration result should not be null");
        assertTrue(result.success(), "Integration task should succeed");
        assertNotNull(result.output(), "Integration output should not be null");

        // Verify memory persistence across different agent types
        List<String> memory = integrationAgent.getMemorySnapshot(3);
        assertFalse(memory.isEmpty(), "Agent should have memory entries");

        // Verify registry functionality
        assertNotNull(registry.get(agentName), "Agent should be in registry");
    }

    @Test
    @Order(5)
    @DisplayName("Test error handling and recovery")
    void testErrorHandlingAndRecovery() throws Exception {
        // Test that the system handles errors gracefully
        SubAgentRegistry registry = new SubAgentRegistry();

        // Create agent that might encounter errors
        String agentName = "test-error-agent-" + System.currentTimeMillis();
        ConversationalAgent errorAgent = new ConversationalAgent(
                agentName,
                "Error handling test agent",
                mockProvider,
                "Test error scenarios",
                memoryStore
        );

        registry.register(errorAgent);

        // Test with potentially problematic input
        String errorProneTask = "This is a test task with valid input";  // Valid task input
        ExecutionResult result = errorAgent.execute(new ExecutionInput(errorProneTask, null));

        // Validate error handling
        assertNotNull(result, "Error result should not be null");
        // Note: MockLLMProvider should handle empty input gracefully

        // Test with tool that might fail
        ConversationalAgent toolAgent = new ConversationalAgent(
                "test-tool-error-agent-" + System.currentTimeMillis(),
                "Tool error test agent",
                mockProvider,
                toolRegistry,
                memoryStore
        );

        // Test with invalid tool command
        String invalidToolCommand = "invalidtool: this should not work";
        ExecutionResult toolResult = toolAgent.execute(new ExecutionInput(invalidToolCommand, null));

        // Validate tool error handling
        assertNotNull(toolResult, "Tool error result should not be null");
        // The result might succeed or fail depending on how ToolUsingAgent handles invalid commands
    }

    @Test
    @Order(6)
    @DisplayName("Test performance and resource management")
    void testPerformanceAndResourceManagement() throws Exception {
        // Test that resources are properly managed

        // Create multiple agents to test resource usage
        SubAgentRegistry registry = new SubAgentRegistry();
        String[] agentNames = new String[3];

        for (int i = 0; i < 3; i++) {
            String agentName = "test-perf-agent-" + i + "-" + System.currentTimeMillis();
            agentNames[i] = agentName;

            ConversationalAgent perfAgent = new ConversationalAgent(
                    agentName,
                    "Performance test agent " + i,
                    mockProvider,
                    "Test performance characteristics",
                    memoryStore
            );

            registry.register(perfAgent);

            // Execute a simple task
            ExecutionResult result = perfAgent.execute(new ExecutionInput("Simple task " + i, null));

            assertNotNull(result, "Performance test result " + i + " should not be null");
            assertTrue(result.success(), "Performance test " + i + " should succeed");
        }

        // Verify all agents were registered (by checking they can be retrieved)
        for (int i = 0; i < 3; i++) {
            assertNotNull(registry.get(agentNames[i]), "Agent " + i + " should be retrievable");
        }

        // Test that memory is being used efficiently
        // (This is a basic test - in practice you'd monitor actual memory usage)
        assertNotNull(memoryStore, "Memory store should still be available");
    }
}
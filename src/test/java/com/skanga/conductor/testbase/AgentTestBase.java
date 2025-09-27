package com.skanga.conductor.testbase;

import com.skanga.conductor.agent.ConversationalAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.tools.ToolRegistry;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.exception.ConductorException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base test class for agent-related tests providing common agent setup and utilities.
 * <p>
 * This class extends ConductorTestBase and adds agent-specific functionality including:
 * </p>
 * <ul>
 * <li>MockLLMProvider creation and management</li>
 * <li>MemoryStore creation and automatic cleanup</li>
 * <li>ToolRegistry setup with common test tools</li>
 * <li>ConversationalAgent creation utilities</li>
 * <li>Orchestrator setup for integration testing</li>
 * <li>Common agent execution patterns</li>
 * </ul>
 *
 * @since 2.0.0
 */
public abstract class AgentTestBase extends ConductorTestBase {

    protected MockLLMProvider mockLLMProvider;
    protected MemoryStore memoryStore;
    protected ToolRegistry toolRegistry;
    protected SubAgentRegistry agentRegistry;
    protected Orchestrator orchestrator;

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        // Create and register mock LLM provider
        mockLLMProvider = createMockLLMProvider();

        // Create and register memory store for automatic cleanup
        memoryStore = registerForCleanup(createMemoryStore());

        // Create tool registry with common test tools
        toolRegistry = createToolRegistry();

        // Create agent registry and orchestrator
        agentRegistry = new SubAgentRegistry();
        orchestrator = new Orchestrator(agentRegistry, memoryStore);

        logger.debug("Agent test setup complete");
    }

    /**
     * Creates a mock LLM provider for testing.
     * Override this method to customize the mock provider behavior.
     *
     * @return a configured mock LLM provider
     */
    protected MockLLMProvider createMockLLMProvider() {
        return new MockLLMProvider(getTestId());
    }

    /**
     * Creates a memory store for testing.
     * Override this method to customize the memory store configuration.
     *
     * @return a configured memory store
     * @throws SQLException if memory store creation fails
     */
    protected MemoryStore createMemoryStore() throws SQLException {
        return new MemoryStore();
    }

    /**
     * Creates a tool registry with common test tools.
     * Override this method to customize the tool registry.
     *
     * @return a configured tool registry
     */
    protected ToolRegistry createToolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        // Add common test tools
        registry.register(new MockTool("test_tool", "Test tool output"));
        registry.register(new MockTool("echo_tool", "Echo: "));
        registry.register(new MockTool("success_tool", "Operation successful"));

        return registry;
    }

    /**
     * Creates a basic ConversationalAgent for testing.
     *
     * @param name the agent name
     * @param description the agent description
     * @param promptTemplate the prompt template
     * @return a configured ConversationalAgent
     * @throws SQLException if agent creation fails
     */
    protected ConversationalAgent createAgent(String name, String description, String promptTemplate) throws SQLException {
        return new ConversationalAgent(
            name,
            description,
            mockLLMProvider,
            promptTemplate,
            toolRegistry,
            memoryStore
        );
    }

    /**
     * Creates a basic ConversationalAgent with default settings for testing.
     *
     * @return a configured ConversationalAgent with default settings
     * @throws SQLException if agent creation fails
     */
    protected ConversationalAgent createDefaultAgent() throws SQLException {
        return createAgent(
            getTestId("agent"),
            "Test agent for " + getClass().getSimpleName(),
            "Test prompt template: {input}"
        );
    }

    /**
     * Creates a ConversationalAgent with a custom LLM provider.
     *
     * @param name the agent name
     * @param description the agent description
     * @param promptTemplate the prompt template
     * @param llmProvider the LLM provider to use
     * @return a configured ConversationalAgent
     * @throws SQLException if agent creation fails
     */
    protected ConversationalAgent createAgentWithProvider(String name, String description,
                                                          String promptTemplate, LLMProvider llmProvider) throws SQLException {
        return new ConversationalAgent(
            name,
            description,
            llmProvider,
            promptTemplate,
            toolRegistry,
            memoryStore
        );
    }

    /**
     * Executes an agent with the given input and returns the result.
     *
     * @param agent the agent to execute
     * @param input the input content
     * @return the execution result
     * @throws ConductorException if execution fails
     */
    protected ExecutionResult executeAgent(ConversationalAgent agent, String input) throws ConductorException {
        return agent.execute(new ExecutionInput(input, null));
    }

    /**
     * Executes an agent with the given input and metadata.
     *
     * @param agent the agent to execute
     * @param input the input content
     * @param metadata the metadata map
     * @return the execution result
     * @throws ConductorException if execution fails
     */
    protected ExecutionResult executeAgent(ConversationalAgent agent, String input, Map<String, Object> metadata)
            throws ConductorException {
        return agent.execute(new ExecutionInput(input, metadata));
    }

    /**
     * Creates a simple metadata map with a single key-value pair.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return a metadata map
     */
    protected Map<String, Object> metadata(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * Creates a metadata map with multiple key-value pairs.
     *
     * @param pairs alternating keys and values
     * @return a metadata map
     */
    protected Map<String, Object> metadata(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Pairs must have even number of elements");
        }

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i].toString(), pairs[i + 1]);
        }
        return map;
    }

    /**
     * Asserts that an agent execution succeeds.
     *
     * @param agent the agent to execute
     * @param input the input content
     * @return the execution result for further assertions
     * @throws ConductorException if execution fails
     */
    protected ExecutionResult assertAgentSucceeds(ConversationalAgent agent, String input) throws ConductorException {
        ExecutionResult result = executeAgent(agent, input);
        if (!result.success()) {
            throw new AssertionError("Expected agent execution to succeed, but it failed: " + result.output());
        }
        return result;
    }

    /**
     * Asserts that an agent execution fails.
     *
     * @param agent the agent to execute
     * @param input the input content
     * @return the execution result for further assertions
     * @throws ConductorException if execution setup fails
     */
    protected ExecutionResult assertAgentFails(ConversationalAgent agent, String input) throws ConductorException {
        ExecutionResult result = executeAgent(agent, input);
        if (result.success()) {
            throw new AssertionError("Expected agent execution to fail, but it succeeded: " + result.output());
        }
        return result;
    }

    /**
     * Adds memory entries to the memory store for the given agent.
     *
     * @param agentName the agent name
     * @param memories the memory entries to add
     * @throws SQLException if memory operation fails
     */
    protected void addMemories(String agentName, String... memories) throws SQLException {
        for (String memory : memories) {
            memoryStore.addMemory(agentName, memory);
        }
    }

    /**
     * Creates an implicit agent using the orchestrator.
     *
     * @param nameHint the agent name hint
     * @param description the agent description
     * @param promptTemplate the prompt template
     * @return the created agent
     * @throws SQLException if agent creation fails
     */
    protected ConversationalAgent createImplicitAgent(String nameHint, String description, String promptTemplate)
            throws SQLException {
        return (ConversationalAgent) orchestrator.createImplicitAgent(nameHint, description, mockLLMProvider, promptTemplate);
    }
}
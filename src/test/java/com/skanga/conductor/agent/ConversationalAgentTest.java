package com.skanga.conductor.agent;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.testbase.AgentTestBase;
import com.skanga.conductor.testbase.MockTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ConversationalAgentTest extends AgentTestBase {

    private ConversationalAgent agent;

    @BeforeEach
    void setUp() throws SQLException {
        // Add a test tool that returns exact output (not appending input)
        toolRegistry.register(new MockTool("test_tool", "Test tool output") {
            @Override
            public ExecutionResult runTool(ExecutionInput input) throws Exception {
                return new ExecutionResult(true, "Test tool output", null);
            }
        });

        // Create an agent using inherited components
        agent = createAgent(
            "test-unified-agent",
            "Test unified agent for unit testing",
            "Test prompt template: {input}"
        );
    }

    @Test
    void testAgentNameAndDescription() {
        assertEquals("test-unified-agent", agent.agentName());
        assertEquals("Test unified agent for unit testing", agent.agentDescription());
    }

    @Test
    void testTextOnlyMode() throws ConductorException {
        // Mock provider returns deterministic response - it will not be JSON format
        ExecutionInput input = new ExecutionInput("Tell me about something", null);
        ExecutionResult result = agent.execute(input);

        assertTrue(result.success());
        assertTrue(result.output().contains("MOCK OUTPUT"));
    }

    @Test
    void testToolExecutionMode() throws ConductorException, SQLException {
        // Create agent with custom mock that returns JSON
        MockLLMProvider jsonMockProvider = new MockLLMProvider("json-test") {
            @Override
            public String generate(String prompt) throws ConductorException.LLMProviderException {
                return "{\"tool\": \"test_tool\", \"arguments\": \"test arguments\"}";
            }
        };

        ConversationalAgent jsonAgent = createAgentWithProvider(
            "json-test-agent",
            "Test agent that returns JSON",
            "Test prompt template",
            jsonMockProvider
        );

        ExecutionInput input = new ExecutionInput("Use a tool to help", null);
        ExecutionResult result = jsonAgent.execute(input);

        assertTrue(result.success());
        assertEquals("Test tool output", result.output());
    }

    @Test
    void testInvalidToolCall() throws ConductorException, SQLException {
        // Create agent with custom mock that returns unknown tool JSON
        MockLLMProvider unknownToolMockProvider = new MockLLMProvider("unknown-tool-test") {
            @Override
            public String generate(String prompt) throws ConductorException.LLMProviderException {
                return "{\"tool\": \"unknown_tool\", \"arguments\": \"test arguments\"}";
            }
        };

        ConversationalAgent unknownToolAgent = createAgentWithProvider(
            "unknown-tool-test-agent",
            "Test agent with unknown tool",
            "Test prompt template",
            unknownToolMockProvider
        );

        ExecutionInput input = new ExecutionInput("Use unknown tool", null);
        ExecutionResult result = unknownToolAgent.execute(input);

        assertFalse(result.success());
        assertTrue(result.output().contains("ERROR: unknown tool"));
    }

    @Test
    void testTextOnlyAgentConstructor() throws SQLException {
        ConversationalAgent textAgent = new ConversationalAgent(
            "text-agent",
            "Text only agent",
            mockLLMProvider,
            "Text prompt template",
            memoryStore
        );

        assertEquals("text-agent", textAgent.agentName());
        assertEquals("Text only agent", textAgent.agentDescription());
    }

    @Test
    void testToolOnlyAgentConstructor() throws SQLException {
        ConversationalAgent toolAgent = new ConversationalAgent(
            "tool-agent",
            "Tool only agent",
            mockLLMProvider,
            toolRegistry,
            memoryStore
        );

        assertEquals("tool-agent", toolAgent.agentName());
        assertEquals("Tool only agent", toolAgent.agentDescription());
    }

    @Test
    void testNullAgentNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConversationalAgent(null, "description", mockLLMProvider, "template", memoryStore));
    }

    @Test
    void testEmptyAgentNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConversationalAgent("", "description", mockLLMProvider, "template", memoryStore));
    }

    @Test
    void testNullDescriptionThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConversationalAgent("name", null, mockLLMProvider, "template", memoryStore));
    }

    @Test
    void testNullLLMProviderThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConversationalAgent("name", "description", null, "template", memoryStore));
    }

    @Test
    void testNullMemoryStoreThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConversationalAgent("name", "description", mockLLMProvider, "template", null));
    }

    @Test
    void testMemorySnapshot() throws ConductorException {
        // Execute a task to generate memory
        agent.execute(new ExecutionInput("Test prompt", null));

        // Get memory snapshot
        var memory = agent.getMemorySnapshot(10);
        assertFalse(memory.isEmpty());
        assertTrue(memory.get(0).contains("LLM_OUTPUT"));
    }

    @Test
    void testInvalidTaskInputThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> agent.execute(null));
        assertThrows(IllegalArgumentException.class, () -> agent.execute(new ExecutionInput(null, null)));
        assertThrows(IllegalArgumentException.class, () -> agent.execute(new ExecutionInput("", null)));
    }
}
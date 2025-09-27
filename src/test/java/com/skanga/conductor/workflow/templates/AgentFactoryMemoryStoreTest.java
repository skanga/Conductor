package com.skanga.conductor.workflow.templates;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.testbase.ConductorTestBase;
import com.skanga.conductor.workflow.config.AgentDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentFactory MemoryStore Sharing Tests")
class AgentFactoryMemoryStoreTest extends ConductorTestBase {

    private MemoryStore sharedMemoryStore;
    private SubAgentRegistry registry;
    private Orchestrator orchestrator;
    private AgentFactory agentFactory;
    private MockLLMProvider mockProvider;

    @BeforeEach
    void setUp() throws Exception {
        sharedMemoryStore = registerForCleanup(new MemoryStore());
        registry = new SubAgentRegistry();
        orchestrator = new Orchestrator(registry, sharedMemoryStore);
        agentFactory = new AgentFactory();
        mockProvider = new MockLLMProvider("memory-test");
    }

    @Test
    @DisplayName("AgentFactory should use orchestrator's MemoryStore instance")
    void testAgentFactoryUsesSharedMemoryStore() throws Exception {
        // Create agent using orchestrator
        SubAgent orchestratorAgent = orchestrator.createImplicitAgent(
            "orchestrator-agent",
            "Test agent created by orchestrator",
            mockProvider,
            "You are a test agent."
        );

        // Create agent using AgentFactory
        AgentDefinition definition = new AgentDefinition();
        definition.setRole("factory-agent");
        definition.setType("llm");
        definition.setPromptTemplate("You are a test agent.");

        SubAgent factoryAgent = agentFactory.createAgent(definition, orchestrator);

        // Verify orchestrator exposes its memory store
        assertNotNull(orchestrator.getMemoryStore(), "Orchestrator should expose memory store");
        assertSame(sharedMemoryStore, orchestrator.getMemoryStore(),
                  "Orchestrator should return the same memory store instance");

        // Execute both agents to generate memory entries
        ExecutionInput input = new ExecutionInput("Hello, this is a test message.", null);

        ExecutionResult orchestratorResult = orchestratorAgent.execute(input);
        assertNotNull(orchestratorResult, "Orchestrator agent should execute");
        assertTrue(orchestratorResult.success(), "Orchestrator agent should succeed");

        ExecutionResult factoryResult = factoryAgent.execute(input);
        assertNotNull(factoryResult, "Factory agent should execute");
        assertTrue(factoryResult.success(), "Factory agent should succeed");

        // Verify both agents have memory entries in the shared store
        List<String> orchestratorMemory = sharedMemoryStore.loadMemory(orchestratorAgent.agentName());
        List<String> factoryMemory = sharedMemoryStore.loadMemory(factoryAgent.agentName());

        assertFalse(orchestratorMemory.isEmpty(),
                   "Orchestrator agent should have memory entries. Agent: " + orchestratorAgent.agentName());
        assertFalse(factoryMemory.isEmpty(),
                   "Factory agent should have memory entries. Agent: " + factoryAgent.agentName());

        // Log memory contents for debugging
        logger.info("Orchestrator agent memory ({}): {}", orchestratorAgent.agentName(), orchestratorMemory);
        logger.info("Factory agent memory ({}): {}", factoryAgent.agentName(), factoryMemory);
    }

    @Test
    @DisplayName("Multiple agents from AgentFactory should use same MemoryStore")
    void testMultipleFactoryAgentsShareMemoryStore() throws Exception {
        // Create two different types of agents using AgentFactory
        AgentDefinition llmDefinition = new AgentDefinition();
        llmDefinition.setRole("llm-agent");
        llmDefinition.setType("llm");
        llmDefinition.setPromptTemplate("You are an LLM agent.");

        AgentDefinition toolDefinition = new AgentDefinition();
        toolDefinition.setRole("tool-agent");
        toolDefinition.setType("tool");
        toolDefinition.setProvider("llm-tool");
        toolDefinition.setPromptTemplate("You are a tool agent.");

        SubAgent llmAgent = agentFactory.createAgent(llmDefinition, orchestrator);
        SubAgent toolAgent = agentFactory.createAgent(toolDefinition, orchestrator);

        // Execute both agents
        ExecutionInput input = new ExecutionInput("Test execution for memory verification.", null);

        ExecutionResult llmResult = llmAgent.execute(input);
        ExecutionResult toolResult = toolAgent.execute(input);

        assertTrue(llmResult.success(), "LLM agent should succeed");
        assertTrue(toolResult.success(), "Tool agent should succeed");

        // Verify both use the shared memory store
        List<String> llmMemory = sharedMemoryStore.loadMemory(llmAgent.agentName());
        List<String> toolMemory = sharedMemoryStore.loadMemory(toolAgent.agentName());

        assertFalse(llmMemory.isEmpty(), "LLM agent should have memory");
        assertFalse(toolMemory.isEmpty(), "Tool agent should have memory");

        // Verify they can both access the shared database
        assertNotNull(sharedMemoryStore.loadMemory(llmAgent.agentName()),
                     "Shared store should contain LLM agent memory");
        assertNotNull(sharedMemoryStore.loadMemory(toolAgent.agentName()),
                     "Shared store should contain tool agent memory");
    }

    @Test
    @DisplayName("Memory store consistency before and after fix")
    void testMemoryStoreInstanceConsistency() throws Exception {
        // This test verifies the fix works by checking that all agents use the same store

        // Create agents using different methods
        SubAgent orchestratorAgent = orchestrator.createImplicitAgent(
            "orch-test", "Orchestrator test agent", mockProvider, "Test prompt");

        AgentDefinition definition = new AgentDefinition();
        definition.setRole("factory-test");
        definition.setType("llm");
        definition.setPromptTemplate("Test prompt");

        SubAgent factoryAgent = agentFactory.createAgent(definition, orchestrator);

        // All agents should use the exact same MemoryStore instance
        MemoryStore orchestratorStore = orchestrator.getMemoryStore();

        // Before the fix: factoryAgent would have created new MemoryStore()
        // After the fix: factoryAgent should use orchestratorStore

        assertSame(sharedMemoryStore, orchestratorStore,
                  "Orchestrator should use the shared memory store");

        // The key test: both agents should be using the same physical store
        // We can verify this by adding memory through one agent and checking it exists in the store
        orchestratorAgent.execute(new ExecutionInput("Store this message", null));
        factoryAgent.execute(new ExecutionInput("Store this other message", null));

        // Both agents' memories should be in the same store
        assertNotNull(sharedMemoryStore.loadMemory(orchestratorAgent.agentName()),
                     "Shared store should contain orchestrator agent memory");
        assertNotNull(sharedMemoryStore.loadMemory(factoryAgent.agentName()),
                     "Shared store should contain factory agent memory");

        // Count total agents in the store
        List<String> orchestratorMemoryEntries = sharedMemoryStore.loadMemory(orchestratorAgent.agentName());
        List<String> factoryMemoryEntries = sharedMemoryStore.loadMemory(factoryAgent.agentName());

        assertTrue(orchestratorMemoryEntries.size() > 0, "Orchestrator agent should have memory entries");
        assertTrue(factoryMemoryEntries.size() > 0, "Factory agent should have memory entries");

        logger.info("Memory consistency test passed - all agents use shared MemoryStore");
    }

    @Test
    @DisplayName("Direct memory store access verification")
    void testDirectMemoryStoreAccess() throws Exception {
        // Create agent via factory
        AgentDefinition definition = new AgentDefinition();
        definition.setRole("direct-test");
        definition.setType("llm");
        definition.setPromptTemplate("Direct test agent");

        SubAgent agent = agentFactory.createAgent(definition, orchestrator);

        // Execute agent to create memory
        agent.execute(new ExecutionInput("Direct test message", null));

        // Directly verify memory exists in shared store
        List<String> memories = sharedMemoryStore.loadMemory(agent.agentName());
        assertFalse(memories.isEmpty(), "Agent should have memory entries in shared store");

        // Verify we can access the same memory through orchestrator's store
        List<String> memoriesViaOrchestrator = orchestrator.getMemoryStore().loadMemory(agent.agentName());
        assertEquals(memories.size(), memoriesViaOrchestrator.size(),
                    "Same memory should be accessible through orchestrator");

        // The memories should be identical (same store)
        assertEquals(memories, memoriesViaOrchestrator,
                    "Memory should be identical when accessed through different references");
    }

    @Test
    @DisplayName("Agent registry integration with shared memory")
    void testAgentRegistryMemoryIntegration() throws Exception {
        // Create and register agent
        AgentDefinition definition = new AgentDefinition();
        definition.setRole("registry-test");
        definition.setType("llm");
        definition.setPromptTemplate("Registry test agent");

        SubAgent agent = agentFactory.createAgent(definition, orchestrator);

        // Register agent in orchestrator's registry
        orchestrator.getRegistry().register(agent);

        // Execute via registry
        ExecutionInput input = new ExecutionInput("Registry test message", null);
        ExecutionResult result = orchestrator.callExplicit(agent.agentName(), input);

        assertTrue(result.success(), "Agent execution through registry should succeed");

        // Verify memory was stored in shared store
        List<String> memories = sharedMemoryStore.loadMemory(agent.agentName());
        assertFalse(memories.isEmpty(), "Agent should have memory after registry execution");

        logger.info("Registry integration test passed - agent memory properly stored");
    }
}
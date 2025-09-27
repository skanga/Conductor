package com.skanga.conductor.orchestration;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.testbase.DatabaseTestBase;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for Orchestrator functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrchestratorTest extends DatabaseTestBase {

    @Mock
    private SubAgentRegistry mockRegistry;

    @Mock
    private SubAgent mockAgent;

    @Mock
    private LLMProvider mockProvider;

    private Orchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Order(1)
    @DisplayName("Should construct with valid registry and memory store")
    void testValidConstruction() throws Exception {
        withDatabase(memoryStore -> {
            Orchestrator validOrchestrator = new Orchestrator(mockRegistry, memoryStore);
            assertNotNull(validOrchestrator);
            assertSame(mockRegistry, validOrchestrator.getRegistry());
            assertSame(memoryStore, validOrchestrator.getMemoryStore());
            return null;
        });
    }

    @Test
    @Order(2)
    @DisplayName("Should throw IllegalArgumentException for null registry")
    void testNullRegistryConstruction() throws Exception {
        withDatabase(memoryStore -> {
            assertThrows(IllegalArgumentException.class, () -> {
                new Orchestrator(null, memoryStore);
            });
            return null;
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should throw IllegalArgumentException for null memory store")
    void testNullMemoryStoreConstruction() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Orchestrator(mockRegistry, null);
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should execute agent successfully with callExplicit")
    void testSuccessfulExplicitCall() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            String agentName = "test-agent";
            ExecutionInput input = new ExecutionInput("test content", null);
            ExecutionResult expectedResult = new ExecutionResult(true, "Agent executed successfully", null);

            // Setup mocks
            when(mockRegistry.get(agentName)).thenReturn(mockAgent);
            when(mockAgent.execute(input)).thenReturn(expectedResult);

            // Execute
            ExecutionResult result = orchestrator.callExplicit(agentName, input);

            // Verify
            assertNotNull(result);
            assertEquals(expectedResult.output(), result.output());
            assertTrue(result.success());

            verify(mockRegistry).get(agentName);
            verify(mockAgent).execute(input);

            return null;
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should throw IllegalArgumentException for null agent name in callExplicit")
    void testCallExplicitWithNullAgentName() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);
            ExecutionInput input = new ExecutionInput("test content", null);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.callExplicit(null, input);
            });

            return null;
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should throw IllegalArgumentException for blank agent name in callExplicit")
    void testCallExplicitWithBlankAgentName() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);
            ExecutionInput input = new ExecutionInput("test content", null);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.callExplicit("", input);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.callExplicit("   ", input);
            });

            return null;
        });
    }

    @Test
    @Order(7)
    @DisplayName("Should throw IllegalArgumentException for null input in callExplicit")
    void testCallExplicitWithNullInput() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.callExplicit("test-agent", null);
            });

            return null;
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should throw IllegalArgumentException for non-existent agent in callExplicit")
    void testCallExplicitWithNonExistentAgent() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);
            String agentName = "non-existent-agent";
            ExecutionInput input = new ExecutionInput("test content", null);

            // Setup mock to return null (agent not found)
            when(mockRegistry.get(agentName)).thenReturn(null);

            // Execute and expect exception
            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.callExplicit(agentName, input);
            });

            verify(mockRegistry).get(agentName);

            return null;
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should propagate agent execution exceptions in callExplicit")
    void testCallExplicitAgentExecutionException() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);
            String agentName = "failing-agent";
            ExecutionInput input = new ExecutionInput("test content", null);

            // Setup mocks
            when(mockRegistry.get(agentName)).thenReturn(mockAgent);
            when(mockAgent.execute(input)).thenThrow(new ConductorException.LLMProviderException("Agent failed"));

            // Execute and expect exception
            ConductorException.LLMProviderException exception = assertThrows(
                ConductorException.LLMProviderException.class,
                () -> orchestrator.callExplicit(agentName, input)
            );

            assertEquals("Agent failed", exception.getMessage());

            return null;
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should create implicit agent successfully")
    void testCreateImplicitAgentSuccess() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            String nameHint = "test-agent";
            String description = "Test agent description";
            String promptTemplate = "Execute: {{user_request}}";

            // Mock LLM provider to return a response
            when(mockProvider.generate(anyString())).thenReturn("Agent response");

            // Create implicit agent
            SubAgent implicitAgent = orchestrator.createImplicitAgent(nameHint, description, mockProvider, promptTemplate);

            // Verify agent was created
            assertNotNull(implicitAgent);
            assertEquals(description, implicitAgent.agentDescription());
            assertTrue(implicitAgent.agentName().startsWith(nameHint + "-"));
            assertTrue(implicitAgent.agentName().contains("-")); // Should have UUID suffix

            return null;
        });
    }

    @Test
    @Order(11)
    @DisplayName("Should throw IllegalArgumentException for null nameHint in createImplicitAgent")
    void testCreateImplicitAgentNullNameHint() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent(null, "description", mockProvider, "template");
            });

            return null;
        });
    }

    @Test
    @Order(12)
    @DisplayName("Should throw IllegalArgumentException for blank nameHint in createImplicitAgent")
    void testCreateImplicitAgentBlankNameHint() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("", "description", mockProvider, "template");
            });

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("   ", "description", mockProvider, "template");
            });

            return null;
        });
    }

    @Test
    @Order(13)
    @DisplayName("Should throw IllegalArgumentException for null description in createImplicitAgent")
    void testCreateImplicitAgentNullDescription() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", null, mockProvider, "template");
            });

            return null;
        });
    }

    @Test
    @Order(14)
    @DisplayName("Should throw IllegalArgumentException for blank description in createImplicitAgent")
    void testCreateImplicitAgentBlankDescription() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", "", mockProvider, "template");
            });

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", "   ", mockProvider, "template");
            });

            return null;
        });
    }

    @Test
    @Order(15)
    @DisplayName("Should throw IllegalArgumentException for null provider in createImplicitAgent")
    void testCreateImplicitAgentNullProvider() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", "description", null, "template");
            });

            return null;
        });
    }

    @Test
    @Order(16)
    @DisplayName("Should throw IllegalArgumentException for null promptTemplate in createImplicitAgent")
    void testCreateImplicitAgentNullPromptTemplate() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", "description", mockProvider, null);
            });

            return null;
        });
    }

    @Test
    @Order(17)
    @DisplayName("Should throw IllegalArgumentException for blank promptTemplate in createImplicitAgent")
    void testCreateImplicitAgentBlankPromptTemplate() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", "description", mockProvider, "");
            });

            assertThrows(IllegalArgumentException.class, () -> {
                orchestrator.createImplicitAgent("agent", "description", mockProvider, "   ");
            });

            return null;
        });
    }

    @Test
    @Order(18)
    @DisplayName("Should create unique agent names for multiple implicit agents")
    void testMultipleImplicitAgentsHaveUniqueNames() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            String nameHint = "duplicate-agent";
            String description = "Duplicate agent test";
            String promptTemplate = "Template: {{input}}";

            // Mock provider
            when(mockProvider.generate(anyString())).thenReturn("Response");

            // Create multiple agents with same nameHint
            SubAgent agent1 = orchestrator.createImplicitAgent(nameHint, description, mockProvider, promptTemplate);
            SubAgent agent2 = orchestrator.createImplicitAgent(nameHint, description, mockProvider, promptTemplate);
            SubAgent agent3 = orchestrator.createImplicitAgent(nameHint, description, mockProvider, promptTemplate);

            // Verify all agents have different names
            assertNotEquals(agent1.agentName(), agent2.agentName());
            assertNotEquals(agent1.agentName(), agent3.agentName());
            assertNotEquals(agent2.agentName(), agent3.agentName());

            // Verify all names start with the hint
            assertTrue(agent1.agentName().startsWith(nameHint + "-"));
            assertTrue(agent2.agentName().startsWith(nameHint + "-"));
            assertTrue(agent3.agentName().startsWith(nameHint + "-"));

            return null;
        });
    }

    @Test
    @Order(19)
    @DisplayName("Should execute implicit agent successfully")
    void testExecuteImplicitAgent() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            String nameHint = "executable-agent";
            String description = "Executable agent test";
            String promptTemplate = "Process: {{content}}";

            // Mock provider response
            when(mockProvider.generate(anyString())).thenReturn("Processed successfully");

            // Create and execute implicit agent
            SubAgent implicitAgent = orchestrator.createImplicitAgent(nameHint, description, mockProvider, promptTemplate);
            ExecutionInput input = new ExecutionInput("test content", null);
            ExecutionResult result = implicitAgent.execute(input);

            // Verify execution
            assertNotNull(result);
            assertTrue(result.success());
            assertEquals("Processed successfully", result.output());

            return null;
        });
    }

    @Test
    @Order(20)
    @DisplayName("Should handle implicit agent memory persistence")
    void testImplicitAgentMemoryPersistence() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            String nameHint = "memory-agent";
            String description = "Memory persistence test";
            String promptTemplate = "Remember: {{content}}";

            // Mock provider responses
            when(mockProvider.generate(anyString()))
                .thenReturn("First response")
                .thenReturn("Second response with memory");

            // Create implicit agent
            SubAgent implicitAgent = orchestrator.createImplicitAgent(nameHint, description, mockProvider, promptTemplate);

            // Execute multiple times to test memory
            ExecutionInput input1 = new ExecutionInput("first execution", null);
            ExecutionResult result1 = implicitAgent.execute(input1);

            ExecutionInput input2 = new ExecutionInput("second execution", null);
            ExecutionResult result2 = implicitAgent.execute(input2);

            // Verify both executions succeeded
            assertTrue(result1.success());
            assertTrue(result2.success());
            assertEquals("First response", result1.output());
            assertEquals("Second response with memory", result2.output());

            return null;
        });
    }

    @Test
    @Order(21)
    @DisplayName("Should propagate SQLException from createImplicitAgent")
    void testCreateImplicitAgentSQLException() throws Exception {
        // Create a failing memory store
        MemoryStore failingMemoryStore = mock(MemoryStore.class);
        when(failingMemoryStore.loadMemory(anyString())).thenThrow(new SQLException("Database error"));

        orchestrator = new Orchestrator(mockRegistry, failingMemoryStore);

        // Attempt to create implicit agent should propagate SQLException
        SQLException exception = assertThrows(SQLException.class, () -> {
            orchestrator.createImplicitAgent("agent", "description", mockProvider, "template");
        });

        assertEquals("Database error", exception.getMessage());
    }

    @Test
    @Order(22)
    @DisplayName("Should provide access to memory store")
    void testGetMemoryStore() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            MemoryStore retrievedStore = orchestrator.getMemoryStore();

            assertNotNull(retrievedStore);
            assertSame(memoryStore, retrievedStore);

            return null;
        });
    }

    @Test
    @Order(23)
    @DisplayName("Should provide access to registry")
    void testGetRegistry() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            SubAgentRegistry retrievedRegistry = orchestrator.getRegistry();

            assertNotNull(retrievedRegistry);
            assertSame(mockRegistry, retrievedRegistry);

            return null;
        });
    }

    @Test
    @Order(24)
    @DisplayName("Should handle complex execution scenarios")
    void testComplexExecutionScenario() throws Exception {
        withDatabase(memoryStore -> {
            orchestrator = new Orchestrator(mockRegistry, memoryStore);

            // Setup multiple registered agents
            SubAgent registeredAgent1 = mock(SubAgent.class);
            SubAgent registeredAgent2 = mock(SubAgent.class);

            when(mockRegistry.get("agent1")).thenReturn(registeredAgent1);
            when(mockRegistry.get("agent2")).thenReturn(registeredAgent2);

            when(registeredAgent1.execute(any(ExecutionInput.class)))
                .thenReturn(new ExecutionResult(true, "Registered agent 1 result", null));
            when(registeredAgent2.execute(any(ExecutionInput.class)))
                .thenReturn(new ExecutionResult(true, "Registered agent 2 result", null));

            // Mock provider for implicit agent
            when(mockProvider.generate(anyString())).thenReturn("Implicit agent result");

            // Execute registered agents
            ExecutionInput input = new ExecutionInput("test input", null);
            ExecutionResult result1 = orchestrator.callExplicit("agent1", input);
            ExecutionResult result2 = orchestrator.callExplicit("agent2", input);

            // Create and execute implicit agent
            SubAgent implicitAgent = orchestrator.createImplicitAgent(
                "implicit", "Implicit agent", mockProvider, "Process: {{content}}");
            ExecutionResult result3 = implicitAgent.execute(input);

            // Verify all executions
            assertTrue(result1.success());
            assertTrue(result2.success());
            assertTrue(result3.success());

            assertEquals("Registered agent 1 result", result1.output());
            assertEquals("Registered agent 2 result", result2.output());
            assertEquals("Implicit agent result", result3.output());

            return null;
        });
    }
}
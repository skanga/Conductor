package com.skanga.conductor.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SubAgentRegistryTest {

    private SubAgentRegistry registry;
    private MockSubAgent mockAgent;

    @BeforeEach
    void setUp() {
        registry = new SubAgentRegistry();
        mockAgent = new MockSubAgent("test-agent");
    }

    @Test
    void shouldRegisterAgentSuccessfully() {
        // When
        registry.register(mockAgent);

        // Then
        assertEquals(1, registry.size());
        assertFalse(registry.isEmpty());
        assertSame(mockAgent, registry.get("test-agent"));
    }

    @Test
    void shouldThrowExceptionForNullAgent() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.register(null);
        });
        assertTrue(exception.getMessage().contains("agent"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void shouldThrowExceptionForAgentWithNullName() {
        // Given
        MockSubAgent agentWithNullName = new MockSubAgent(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.register(agentWithNullName);
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionForAgentWithEmptyName() {
        // Given
        MockSubAgent agentWithEmptyName = new MockSubAgent("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.register(agentWithEmptyName);
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionForAgentWithWhitespaceName() {
        // Given
        MockSubAgent agentWithWhitespaceName = new MockSubAgent("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.register(agentWithWhitespaceName);
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldReplaceExistingAgentWithSameName() {
        // Given
        MockSubAgent firstAgent = new MockSubAgent("duplicate-name");
        MockSubAgent secondAgent = new MockSubAgent("duplicate-name");

        // When
        registry.register(firstAgent);
        registry.register(secondAgent);

        // Then
        assertEquals(1, registry.size());
        assertSame(secondAgent, registry.get("duplicate-name"));
        assertNotSame(firstAgent, registry.get("duplicate-name"));
    }

    @Test
    void shouldRetrieveAgentByName() {
        // Given
        registry.register(mockAgent);

        // When
        SubAgent retrieved = registry.get("test-agent");

        // Then
        assertSame(mockAgent, retrieved);
    }

    @Test
    void shouldReturnNullForNonExistentAgent() {
        // When
        SubAgent retrieved = registry.get("non-existent");

        // Then
        assertNull(retrieved);
    }

    @Test
    void shouldThrowExceptionForNullAgentName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.get(null);
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionForEmptyAgentName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.get("");
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionForWhitespaceAgentName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.get("   ");
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldRemoveAgentSuccessfully() {
        // Given
        registry.register(mockAgent);
        assertEquals(1, registry.size());

        // When
        SubAgent removed = registry.remove("test-agent");

        // Then
        assertSame(mockAgent, removed);
        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
        assertNull(registry.get("test-agent"));
    }

    @Test
    void shouldReturnNullWhenRemovingNonExistentAgent() {
        // When
        SubAgent removed = registry.remove("non-existent");

        // Then
        assertNull(removed);
        assertEquals(0, registry.size());
    }

    @Test
    void shouldThrowExceptionWhenRemovingWithNullName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.remove(null);
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenRemovingWithEmptyName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.remove("");
        });
        assertTrue(exception.getMessage().contains("agent name"));
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void shouldClearAllAgents() {
        // Given
        registry.register(new MockSubAgent("agent1"));
        registry.register(new MockSubAgent("agent2"));
        registry.register(new MockSubAgent("agent3"));
        assertEquals(3, registry.size());

        // When
        registry.clear();

        // Then
        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
        assertNull(registry.get("agent1"));
        assertNull(registry.get("agent2"));
        assertNull(registry.get("agent3"));
    }

    @Test
    void shouldReportCorrectSize() {
        // Initially empty
        assertEquals(0, registry.size());

        // Add agents
        registry.register(new MockSubAgent("agent1"));
        assertEquals(1, registry.size());

        registry.register(new MockSubAgent("agent2"));
        assertEquals(2, registry.size());

        registry.register(new MockSubAgent("agent3"));
        assertEquals(3, registry.size());

        // Remove agent
        registry.remove("agent2");
        assertEquals(2, registry.size());

        // Clear all
        registry.clear();
        assertEquals(0, registry.size());
    }

    @Test
    void shouldReportEmptyStatusCorrectly() {
        // Initially empty
        assertTrue(registry.isEmpty());

        // Add agent
        registry.register(mockAgent);
        assertFalse(registry.isEmpty());

        // Remove agent
        registry.remove("test-agent");
        assertTrue(registry.isEmpty());

        // Add multiple agents
        registry.register(new MockSubAgent("agent1"));
        registry.register(new MockSubAgent("agent2"));
        assertFalse(registry.isEmpty());

        // Clear all
        registry.clear();
        assertTrue(registry.isEmpty());
    }

    @Test
    void shouldHandleMultipleAgentsWithDifferentNames() {
        // Given
        MockSubAgent agent1 = new MockSubAgent("agent1");
        MockSubAgent agent2 = new MockSubAgent("agent2");
        MockSubAgent agent3 = new MockSubAgent("agent3");

        // When
        registry.register(agent1);
        registry.register(agent2);
        registry.register(agent3);

        // Then
        assertEquals(3, registry.size());
        assertSame(agent1, registry.get("agent1"));
        assertSame(agent2, registry.get("agent2"));
        assertSame(agent3, registry.get("agent3"));
    }

    @Test
    void shouldBeThreadSafeForConcurrentRegistration() throws Exception {
        // Given
        int numThreads = 10;
        int agentsPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < agentsPerThread; j++) {
                        String agentName = "thread-" + threadIndex + "-agent-" + j;
                        registry.register(new MockSubAgent(agentName));
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();
        doneLatch.await();

        // Then
        assertEquals(0, errorCount.get());
        assertEquals(numThreads * agentsPerThread, registry.size());

        // Verify all agents are registered
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < agentsPerThread; j++) {
                String agentName = "thread-" + i + "-agent-" + j;
                assertNotNull(registry.get(agentName));
            }
        }
    }

    @Test
    void shouldBeThreadSafeForConcurrentAccess() throws Exception {
        // Given
        int numOperations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Register initial agents
        for (int i = 0; i < 10; i++) {
            registry.register(new MockSubAgent("initial-agent-" + i));
        }

        // When - perform concurrent operations
        for (int i = 0; i < numOperations; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    int operation = index % 4;
                    switch (operation) {
                        case 0: // Register
                            registry.register(new MockSubAgent("concurrent-agent-" + index));
                            break;
                        case 1: // Get
                            registry.get("initial-agent-" + (index % 10));
                            break;
                        case 2: // Remove (might not exist)
                            registry.remove("concurrent-agent-" + (index - 10));
                            break;
                        case 3: // Size check
                            registry.size();
                            registry.isEmpty();
                            break;
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Should not happen with thread-safe operations
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Then
        assertEquals(numOperations, successCount.get());
        assertTrue(registry.size() >= 10); // At least initial agents should remain
    }

    @Test
    void shouldHandleConcurrentRegistrationOfSameAgent() throws Exception {
        // Given
        int numThreads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicReference<SubAgent> finalAgent = new AtomicReference<>();

        // When - multiple threads try to register agents with same name
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    MockSubAgent agent = new MockSubAgent("same-name");
                    agent.setThreadId(threadIndex);
                    registry.register(agent);
                    finalAgent.set(agent);
                } catch (Exception e) {
                    // Should not happen
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        // Then
        assertEquals(1, registry.size());
        SubAgent registeredAgent = registry.get("same-name");
        assertNotNull(registeredAgent);
        assertTrue(registeredAgent instanceof MockSubAgent);
    }

    @Test
    void shouldHandleLargeNumberOfAgents() {
        // Given
        int numAgents = 1000;

        // When
        for (int i = 0; i < numAgents; i++) {
            registry.register(new MockSubAgent("agent-" + i));
        }

        // Then
        assertEquals(numAgents, registry.size());
        assertFalse(registry.isEmpty());

        // Verify all agents can be retrieved
        for (int i = 0; i < numAgents; i++) {
            assertNotNull(registry.get("agent-" + i));
        }

        // Remove half the agents
        for (int i = 0; i < numAgents / 2; i++) {
            SubAgent removed = registry.remove("agent-" + i);
            assertNotNull(removed);
        }

        assertEquals(numAgents / 2, registry.size());
    }

    @Test
    void shouldHandleSpecialCharactersInAgentNames() {
        // Given
        String[] specialNames = {
            "agent-with-dashes",
            "agent_with_underscores",
            "agent.with.dots",
            "agent@domain.com",
            "agent/with/slashes",
            "agent with spaces",
            "агент-кириллица",
            "エージェント",
            "agent-123-456"
        };

        // When
        for (String name : specialNames) {
            registry.register(new MockSubAgent(name));
        }

        // Then
        assertEquals(specialNames.length, registry.size());

        for (String name : specialNames) {
            SubAgent agent = registry.get(name);
            assertNotNull(agent, "Agent with name '" + name + "' should be retrievable");
            assertEquals(name, agent.agentName());
        }
    }

    @Test
    void shouldMaintainRegistryStateAfterExceptions() {
        // Given
        registry.register(new MockSubAgent("valid-agent"));

        // When - try operations that should throw exceptions
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
        assertThrows(IllegalArgumentException.class, () -> registry.get(""));
        assertThrows(IllegalArgumentException.class, () -> registry.remove(null));

        // Then - registry state should be unchanged
        assertEquals(1, registry.size());
        assertNotNull(registry.get("valid-agent"));
    }

    /**
     * Mock SubAgent implementation for testing
     */
    private static class MockSubAgent implements SubAgent {
        private final String name;
        private int threadId = -1;

        public MockSubAgent(String name) {
            this.name = name;
        }

        public void setThreadId(int threadId) {
            this.threadId = threadId;
        }

        public int getThreadId() {
            return threadId;
        }

        @Override
        public String agentName() {
            return name;
        }

        @Override
        public String agentDescription() {
            return "Mock agent for testing: " + name;
        }

        @Override
        public com.skanga.conductor.execution.ExecutionResult execute(com.skanga.conductor.execution.ExecutionInput input) throws com.skanga.conductor.exception.ConductorException {
            return new com.skanga.conductor.execution.ExecutionResult(true, "Mock execution result for " + name, null);
        }
    }
}
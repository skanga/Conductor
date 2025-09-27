package com.skanga.conductor;

import com.skanga.conductor.agent.ConversationalAgent;
import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.config.ApplicationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Thread Safety Tests")
class ThreadSafetyTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:threadsafetytest;DB_CLOSE_DELAY=-1";
    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 100;

    @BeforeEach
    void setUp() {
        // Reset any static state
        System.clearProperty("conductor.database.url");
        System.setProperty("conductor.database.url", TEST_DB_URL);
    }

    @Test
    @DisplayName("Concurrent ConversationalAgent memory operations")
    void testConcurrentMemoryOperations() throws Exception {
        MockLLMProvider mockProvider = new MockLLMProvider("test");

        try (MemoryStore store = new MemoryStore()) {
            ConversationalAgent agent = new ConversationalAgent(
                "test-agent",
                "Test agent for concurrency",
                mockProvider,
                "Test template",
                store
            );

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<List<String>>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);

            // Submit concurrent execution tasks
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                futures.add(executor.submit(() -> {
                    List<String> results = new ArrayList<>();
                    try {
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            // Execute agent to add memory
                            ExecutionInput input = new ExecutionInput("Test input " + threadId + "-" + j, null);
                            ExecutionResult result = agent.execute(input);
                            results.add(result.output());
                            successCount.incrementAndGet();

                            // Periodically check memory snapshot
                            if (j % 10 == 0) {
                                List<String> snapshot = agent.getMemorySnapshot(5);
                                assertNotNull(snapshot);
                                assertTrue(snapshot.size() <= 5);
                            }
                        }
                    } catch (Exception e) {
                        fail("Thread " + threadId + " failed: " + e.getMessage());
                    }
                    return results;
                }));
            }

            // Wait for all threads to complete
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS),
                      "Test threads should complete within 30 seconds");

            // Verify all operations succeeded
            assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, successCount.get());

            // Verify final memory state (ConversationalAgent adds to memory, plus it's persisted to DB)
            List<String> finalMemory = agent.getMemorySnapshot(Integer.MAX_VALUE);
            assertTrue(finalMemory.size() >= THREAD_COUNT * OPERATIONS_PER_THREAD,
                      "Memory should contain at least " + (THREAD_COUNT * OPERATIONS_PER_THREAD) + " entries, got " + finalMemory.size());

            // Verify no duplicate or corrupted entries
            for (String entry : finalMemory) {
                assertNotNull(entry);
                assertFalse(entry.isEmpty());
            }
        }
    }

    @Test
    @DisplayName("Concurrent MemoryStore database operations")
    void testConcurrentDatabaseOperations() throws Exception {
        try (MemoryStore store1 = new MemoryStore();
             MemoryStore store2 = new MemoryStore()) {

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<Void>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);

            // Submit concurrent database operations
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                futures.add(executor.submit(() -> {
                    try {
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            // Alternate between the two store instances
                            MemoryStore store = (j % 2 == 0) ? store1 : store2;

                            String agentName = "agent-" + threadId;
                            String content = "Content from thread " + threadId + " operation " + j;

                            // Add memory
                            store.addMemory(agentName, content);

                            // Load memory periodically
                            if (j % 10 == 0) {
                                List<String> memories = store.loadMemory(agentName, 50);
                                assertNotNull(memories);
                            }

                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        fail("Database thread " + threadId + " failed: " + e.getMessage());
                    }
                    return null;
                }));
            }

            // Wait for completion
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

            // Verify all operations succeeded
            assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, successCount.get());

            // Verify data integrity - each agent should have some memories
            for (int i = 0; i < THREAD_COUNT; i++) {
                String agentName = "agent-" + i;
                List<String> memories = store1.loadMemory(agentName);
                assertTrue(memories.size() > 0,
                          "Agent " + agentName + " should have some memory entries");
            }
        }
    }

    @Test
    @DisplayName("Concurrent ApplicationConfig access")
    void testConcurrentConfigurationAccess() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ApplicationConfig>> futures = new ArrayList<>();

        // Submit concurrent configuration access
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                ApplicationConfig config = ApplicationConfig.getInstance();

                // Access various configuration properties
                config.getDatabaseConfig().getJdbcUrl();
                config.getToolConfig().getCodeRunnerTimeout();
                config.getMemoryConfig().getDefaultMemoryLimit();
                config.getLLMConfig().getOpenAiModel();

                return config;
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Verify all threads got the same singleton instance
        ApplicationConfig firstInstance = futures.get(0).get();
        for (Future<ApplicationConfig> future : futures) {
            assertSame(firstInstance, future.get(),
                      "All threads should get the same ApplicationConfig instance");
        }
    }

    @Test
    @DisplayName("Concurrent SubAgentRegistry operations")
    void testConcurrentRegistryOperations() throws Exception {
        SubAgentRegistry registry = new SubAgentRegistry();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Void>> futures = new ArrayList<>();

        // Submit concurrent registry operations
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try (MemoryStore store = new MemoryStore()) {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String agentName = "agent-" + threadId + "-" + j;
                        MockLLMProvider provider = new MockLLMProvider("test");

                        ConversationalAgent agent = new ConversationalAgent(
                            agentName,
                            "Test agent",
                            provider,
                            "Template",
                            store
                        );

                        // Register agent
                        registry.register(agent);

                        // Retrieve agent
                        SubAgent retrieved = registry.get(agentName);
                        assertSame(agent, retrieved);
                    }
                } catch (Exception e) {
                    fail("Registry thread " + threadId + " failed: " + e.getMessage());
                }
                return null;
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify total registrations
        int totalRegistrations = THREAD_COUNT * OPERATIONS_PER_THREAD;
        for (int i = 0; i < THREAD_COUNT; i++) {
            for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                String agentName = "agent-" + i + "-" + j;
                assertNotNull(registry.get(agentName),
                             "Agent " + agentName + " should be registered");
            }
        }
    }

    @Test
    @DisplayName("Stress test with mixed concurrent operations")
    void testMixedConcurrentOperations() throws Exception {
        try (MemoryStore store = new MemoryStore()) {
            MockLLMProvider provider = new MockLLMProvider("stress");
            SubAgentRegistry registry = new SubAgentRegistry();

            // Create multiple agents
            List<ConversationalAgent> agents = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ConversationalAgent agent = new ConversationalAgent(
                    "stress-agent-" + i,
                    "Stress test agent " + i,
                    provider,
                    "Stress template",
                    store
                );
                agents.add(agent);
                registry.register(agent);
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<Integer>> futures = new ArrayList<>();

            // Submit mixed operations
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                futures.add(executor.submit(() -> {
                    int operationsCompleted = 0;
                    try {
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            // Randomly select an agent
                            ConversationalAgent agent = agents.get(j % agents.size());

                            // Execute agent
                            ExecutionInput input = new ExecutionInput("Stress test " + threadId + "-" + j, null);
                            agent.execute(input);

                            // Access configuration
                            ApplicationConfig.getInstance().getMemoryConfig().getDefaultMemoryLimit();

                            // Access registry
                            registry.get(agent.agentName());

                            // Database operations
                            store.addMemory("stress-" + threadId, "Stress data " + j);

                            operationsCompleted++;
                        }
                    } catch (Exception e) {
                        fail("Stress test thread " + threadId + " failed: " + e.getMessage());
                    }
                    return operationsCompleted;
                }));
            }

            // Wait for completion
            executor.shutdown();
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

            // Verify all operations completed
            int totalOperations = 0;
            for (Future<Integer> future : futures) {
                totalOperations += future.get();
            }
            assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, totalOperations);

            // Verify data integrity
            for (ConversationalAgent agent : agents) {
                List<String> memory = agent.getMemorySnapshot(Integer.MAX_VALUE);
                assertTrue(memory.size() > 0);
            }
        }
    }

    @Test
    @DisplayName("Race condition prevention in schema initialization")
    void testSchemaInitializationRaceCondition() throws Exception {
        // This test ensures schema is initialized only once even with concurrent MemoryStore creation
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<MemoryStore>> futures = new ArrayList<>();

        // Submit concurrent MemoryStore creation
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> new MemoryStore()));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all MemoryStore instances were created successfully
        List<MemoryStore> stores = new ArrayList<>();
        for (Future<MemoryStore> future : futures) {
            stores.add(future.get());
        }
        assertEquals(THREAD_COUNT, stores.size());

        // Test that they all work correctly
        for (int i = 0; i < stores.size(); i++) {
            MemoryStore store = stores.get(i);
            store.addMemory("test-agent-" + i, "Test content");
            List<String> memories = store.loadMemory("test-agent-" + i);
            assertTrue(memories.size() >= 1,
                      "Store " + i + " should have at least 1 memory entry, got " + memories.size());
        }

        // Cleanup
        for (MemoryStore store : stores) {
            store.close();
        }
    }
}
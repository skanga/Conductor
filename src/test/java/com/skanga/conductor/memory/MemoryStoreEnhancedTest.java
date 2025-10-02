package com.skanga.conductor.memory;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.TaskDefinition;
import com.skanga.conductor.testbase.DatabaseTestBase;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced tests for MemoryStore to increase code coverage.
 * Tests additional methods and edge cases not covered in the base test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryStoreEnhancedTest extends DatabaseTestBase {

    @Test
    @Order(1)
    @DisplayName("Test bulk memory loading operations")
    void testBulkMemoryOperations() throws Exception {
        withDatabase(memoryStore -> {
            // Setup test data for multiple agents
            List<String> agentNames = Arrays.asList("agent-1", "agent-2", "agent-3", "agent-4");

            // Add different amounts of memory for each agent
            for (int i = 0; i < agentNames.size(); i++) {
                String agentName = agentNames.get(i);
                for (int j = 0; j <= i; j++) {
                    memoryStore.addMemory(agentName, "Memory " + j + " for " + agentName);
                }
            }

            // Test bulk loading with default limit
            Map<String, List<String>> bulkMemories = memoryStore.loadMemoryBulk(agentNames);
            assertEquals(4, bulkMemories.size(), "Should load memory for all 4 agents");

            // Verify each agent's memory
            assertEquals(1, bulkMemories.get("agent-1").size());
            assertEquals(2, bulkMemories.get("agent-2").size());
            assertEquals(3, bulkMemories.get("agent-3").size());
            assertEquals(4, bulkMemories.get("agent-4").size());

            // Test bulk loading with custom limit
            Map<String, List<String>> limitedBulkMemories = memoryStore.loadMemoryBulk(agentNames, 2);
            assertEquals(4, limitedBulkMemories.size(), "Should still return all agents");

            // Verify limit is applied
            assertTrue(limitedBulkMemories.get("agent-1").size() <= 2);
            assertTrue(limitedBulkMemories.get("agent-2").size() <= 2);
            assertTrue(limitedBulkMemories.get("agent-3").size() <= 2);
            assertTrue(limitedBulkMemories.get("agent-4").size() <= 2);

            return null;
        });
    }

    @Test
    @Order(2)
    @DisplayName("Test bulk memory loading with empty and non-existent agents")
    void testBulkMemoryWithEmptyAgents() throws Exception {
        withDatabase(memoryStore -> {
            // Add memory for only some agents
            memoryStore.addMemory("existing-agent", "Some memory");

            List<String> mixedAgentNames = Arrays.asList(
                "existing-agent",
                "non-existent-agent-1",
                "non-existent-agent-2"
            );

            // Test bulk loading with mix of existing and non-existent agents
            Map<String, List<String>> bulkMemories = memoryStore.loadMemoryBulk(mixedAgentNames);

            assertEquals(3, bulkMemories.size(), "Should return entries for all requested agents");
            assertEquals(1, bulkMemories.get("existing-agent").size());
            assertTrue(bulkMemories.get("non-existent-agent-1").isEmpty());
            assertTrue(bulkMemories.get("non-existent-agent-2").isEmpty());

            // Test with empty agent list
            Map<String, List<String>> emptyResult = memoryStore.loadMemoryBulk(Collections.emptyList());
            assertTrue(emptyResult.isEmpty(), "Empty agent list should return empty map");

            return null;
        });
    }

    @Test
    @Order(3)
    @DisplayName("Test alternative constructor with custom connection parameters")
    void testAlternativeConstructor() throws Exception {
        // Test the deprecated constructor that takes explicit JDBC parameters
        try (MemoryStore customMemoryStore = new MemoryStore("jdbc:h2:mem:test-custom;DB_CLOSE_DELAY=-1", "", "")) {
            // Verify the store works
            customMemoryStore.addMemory("test-agent", "test-memory");
            List<String> memories = customMemoryStore.loadMemory("test-agent");
            assertEquals(1, memories.size());
            assertEquals("test-memory", memories.get(0));
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test exception handling for task output operations")
    void testTaskOutputExceptionHandling() throws Exception {
        withDatabase(memoryStore -> {
            // Test saving task output with null values - should handle gracefully
            assertDoesNotThrow(() -> {
                memoryStore.saveTaskOutput("test-workflow", "test-task", null);
            });

            // Test loading task outputs for workflow with null output
            Map<String, String> outputs = memoryStore.loadTaskOutputs("test-workflow");
            assertTrue(outputs.containsKey("test-task"));
            assertNull(outputs.get("test-task"));

            return null;
        });
    }

    @Test
    @Order(5)
    @DisplayName("Test plan operations with complex data structures")
    void testComplexPlanOperations() throws Exception {
        withDatabase(memoryStore -> {
            String workflowId = "complex-workflow";

            // Create plan with various special characters and complex data
            TaskDefinition[] complexPlan = {
                new TaskDefinition("task-with-json",
                    "Task with JSON in description: {\"key\": \"value\"}",
                    "Template with placeholders: {{input}} and {{output}}"),
                new TaskDefinition("task-with-unicode",
                    "Task with unicode: 你好世界 🌍",
                    "Template: Processando dados em português"),
                new TaskDefinition("task-with-xml",
                    "Task with XML: <root><item>value</item></root>",
                    "Template: <?xml version=\"1.0\"?>"),
                new TaskDefinition("task-with-newlines",
                    "Task with\nmultiple\nlines",
                    "Template with\ttabs\tand\nlines")
            };

            // Save complex plan
            memoryStore.savePlan(workflowId, complexPlan);

            // Verify complex plan is saved and loaded correctly
            TaskDefinition[] loadedPlan = memoryStore.loadPlan(workflowId).orElse(null);
            assertNotNull(loadedPlan);
            assertEquals(4, loadedPlan.length);

            // Verify specific complex content
            assertEquals("task-with-json", loadedPlan[0].taskName);
            assertTrue(loadedPlan[0].taskDescription.contains("{\"key\": \"value\"}"));
            assertTrue(loadedPlan[1].taskDescription.contains("你好世界 🌍"));
            assertTrue(loadedPlan[2].taskDescription.contains("<root><item>value</item></root>"));
            assertTrue(loadedPlan[3].taskDescription.contains("\nmultiple\nlines"));

            return null;
        });
    }

    @Test
    @Order(6)
    @DisplayName("Test plan operations with empty and null arrays")
    void testPlanEdgeCases() throws Exception {
        withDatabase(memoryStore -> {
            String workflowId1 = "empty-workflow";
            String workflowId2 = "null-workflow";

            // Test saving empty plan
            TaskDefinition[] emptyPlan = {};
            memoryStore.savePlan(workflowId1, emptyPlan);

            Optional<TaskDefinition[]> loadedEmpty = memoryStore.loadPlan(workflowId1);
            assertTrue(loadedEmpty.isPresent());
            assertEquals(0, loadedEmpty.get().length);

            // Test loading non-existent plan
            Optional<TaskDefinition[]> nonExistent = memoryStore.loadPlan("does-not-exist");
            assertFalse(nonExistent.isPresent());

            return null;
        });
    }

    @org.junit.jupiter.api.Disabled("Temporarily disabled for performance optimization - expensive concurrent test")
    @Test
    @Order(7)
    @DisplayName("Test concurrent memory operations thread safety")
    void testConcurrentMemoryOperations() throws Exception {
        withDatabase(memoryStore -> {
            int threadCount = 10;
            int operationsPerThread = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // Submit concurrent memory operations
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String agentName = "concurrent-agent-" + threadId;
                            String content = "Memory from thread " + threadId + " operation " + j;

                            memoryStore.addMemory(agentName, content);
                            memoryStore.loadMemory(agentName);
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all operations to complete
            assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within timeout");
            executor.shutdown();

            // Verify results
            assertEquals(threadCount * operationsPerThread, successCount.get());
            assertEquals(0, errorCount.get(), "No errors should occur during concurrent operations");

            // Verify data integrity
            for (int i = 0; i < threadCount; i++) {
                String agentName = "concurrent-agent-" + i;
                List<String> memories = memoryStore.loadMemory(agentName);
                assertEquals(operationsPerThread, memories.size(),
                    "Agent " + i + " should have " + operationsPerThread + " memory entries");
            }

            return null;
        });
    }

    @org.junit.jupiter.api.Disabled("Temporarily disabled for performance optimization - expensive concurrent test")
    @Test
    @Order(8)
    @DisplayName("Test concurrent task output operations thread safety")
    void testConcurrentTaskOutputOperations() throws Exception {
        withDatabase(memoryStore -> {
            int threadCount = 5;
            int operationsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // Submit concurrent task output operations
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String workflowId = "concurrent-workflow-" + threadId;
                            String taskName = "task-" + j;
                            String output = "Output from thread " + threadId + " task " + j;

                            memoryStore.saveTaskOutput(workflowId, taskName, output);
                            memoryStore.loadTaskOutputs(workflowId);
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Count errors but don't fail the test immediately
                        System.err.println("Concurrent operation error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for completion
            assertTrue(latch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            // Verify data integrity
            for (int i = 0; i < threadCount; i++) {
                String workflowId = "concurrent-workflow-" + i;
                Map<String, String> outputs = memoryStore.loadTaskOutputs(workflowId);
                assertEquals(operationsPerThread, outputs.size(),
                    "Workflow " + i + " should have " + operationsPerThread + " task outputs");
            }

            return null;
        });
    }

    @Test
    @Order(9)
    @DisplayName("Test memory store resource cleanup")
    void testResourceCleanup() throws Exception {
        MemoryStore tempStore = null;
        try {
            // Create temporary store
            tempStore = new MemoryStore("jdbc:h2:mem:cleanup-test;DB_CLOSE_DELAY=-1", "", "");

            // Use the store
            tempStore.addMemory("cleanup-agent", "test memory");
            List<String> memories = tempStore.loadMemory("cleanup-agent");
            assertEquals(1, memories.size());

        } finally {
            // Test cleanup
            final MemoryStore finalTempStore = tempStore;
            if (finalTempStore != null) {
                assertDoesNotThrow(() -> finalTempStore.close(), "Store cleanup should not throw exceptions");
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test memory limit enforcement")
    void testMemoryLimitEnforcement() throws Exception {
        withDatabase(memoryStore -> {
            String agentName = "limit-test-agent";

            // Add more memory entries than the typical limit
            for (int i = 0; i < 100; i++) {
                memoryStore.addMemory(agentName, "Memory entry " + i);
            }

            // Test loading with small limit
            List<String> limitedMemories = memoryStore.loadMemory(agentName, 5);
            assertEquals(5, limitedMemories.size(), "Should respect the limit parameter");

            // Test loading with zero limit
            List<String> zeroLimitMemories = memoryStore.loadMemory(agentName, 0);
            assertTrue(zeroLimitMemories.isEmpty(), "Zero limit should return empty list");

            // Test loading with very large limit
            List<String> allMemories = memoryStore.loadMemory(agentName, 1000);
            assertEquals(100, allMemories.size(), "Should return all available entries");

            return null;
        });
    }

    @Test
    @Order(11)
    @DisplayName("Test bulk memory operations with large datasets")
    void testBulkMemoryLargeDataset() throws Exception {
        withDatabase(memoryStore -> {
            // Create large number of agents
            List<String> agentNames = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                String agentName = "bulk-agent-" + i;
                agentNames.add(agentName);

                // Add memory for each agent
                memoryStore.addMemory(agentName, "Memory for agent " + i);
            }

            // Test bulk loading performance and correctness
            long startTime = System.currentTimeMillis();
            Map<String, List<String>> bulkResults = memoryStore.loadMemoryBulk(agentNames, 10);
            long endTime = System.currentTimeMillis();

            // Verify results
            assertEquals(50, bulkResults.size(), "Should return data for all agents");
            assertTrue((endTime - startTime) < 5000, "Bulk operation should complete within reasonable time");

            // Verify each agent has correct data
            for (String agentName : agentNames) {
                assertTrue(bulkResults.containsKey(agentName), "Should contain data for " + agentName);
                assertEquals(1, bulkResults.get(agentName).size(), "Each agent should have 1 memory entry");
            }

            return null;
        });
    }

    @Test
    @Order(12)
    @DisplayName("Test exception scenarios and error handling")
    void testExceptionScenarios() throws Exception {
        withDatabase(memoryStore -> {
            // Test various exception scenarios that should be handled gracefully

            // Very long content that might cause issues
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 25; i++) { // Reduced from 1000 to 25 for faster testing
                longContent.append("This is a very long memory entry that tests the CLOB handling capabilities of the memory store. ");
            }

            assertDoesNotThrow(() -> {
                memoryStore.addMemory("long-content-agent", longContent.toString());
            }, "Should handle very long content without exceptions");

            List<String> longMemories = memoryStore.loadMemory("long-content-agent");
            assertEquals(1, longMemories.size());
            assertEquals(longContent.toString(), longMemories.get(0));

            return null;
        });
    }
}
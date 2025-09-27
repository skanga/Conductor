package com.skanga.conductor.memory;

import com.skanga.conductor.orchestration.TaskDefinition;
import com.skanga.conductor.testbase.DatabaseTestBase;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryStoreTest extends DatabaseTestBase {

    @Test
    @Order(1)
    @DisplayName("Test memory operations - add and load memory entries")
    void testMemoryOperations() throws Exception {
        withDatabase(memoryStore -> {
            String agentName = "test-agent";
            String content1 = "First memory entry";
            String content2 = "Second memory entry with special chars: àáâãäåæçèéêë";
            String content3 = "Third entry with newlines\nand\ttabs";

            // Add memory entries
            memoryStore.addMemory(agentName, content1);
            memoryStore.addMemory(agentName, content2);
            memoryStore.addMemory(agentName, content3);

            // Load and verify
            List<String> memories = memoryStore.loadMemory(agentName);
            assertEquals(3, memories.size(), "Should have 3 memory entries");
            assertEquals(content1, memories.get(0), "First entry should match");
            assertEquals(content2, memories.get(1), "Second entry should match");
            assertEquals(content3, memories.get(2), "Third entry should match");

            // Test with limit
            List<String> limitedMemories = memoryStore.loadMemory(agentName, 2);
            assertEquals(2, limitedMemories.size(), "Should respect limit parameter");

            return null; // withDatabase expects a return value
        });
    }

    @Test
    @Order(2)
    @DisplayName("Test task output operations - save and load workflow task outputs")
    void testTaskOutputOperations() throws Exception {
        withDatabase(memoryStore -> {
            String workflowId1 = "workflow-1";
            String workflowId2 = "workflow-2";

            // Save task outputs for workflow 1
            memoryStore.saveTaskOutput(workflowId1, "task-1", "Output from task 1");
            memoryStore.saveTaskOutput(workflowId1, "task-2", "Output from task 2");
            memoryStore.saveTaskOutput(workflowId1, "task-3", "Output with special chars: @#$%^&*()");

            // Save task outputs for workflow 2
            memoryStore.saveTaskOutput(workflowId2, "task-1", "Different workflow output");
            memoryStore.saveTaskOutput(workflowId2, "task-2", "Another output");

            // Load and verify workflow 1
            Map<String, String> outputs1 = memoryStore.loadTaskOutputs(workflowId1);
            assertEquals(3, outputs1.size(), "Workflow 1 should have 3 task outputs");
            assertEquals("Output from task 1", outputs1.get("task-1"));
            assertEquals("Output from task 2", outputs1.get("task-2"));
            assertEquals("Output with special chars: @#$%^&*()", outputs1.get("task-3"));

            // Load and verify workflow 2
            Map<String, String> outputs2 = memoryStore.loadTaskOutputs(workflowId2);
            assertEquals(2, outputs2.size(), "Workflow 2 should have 2 task outputs");
            assertEquals("Different workflow output", outputs2.get("task-1"));
            assertEquals("Another output", outputs2.get("task-2"));

            // Test MERGE behavior - update existing task
            memoryStore.saveTaskOutput(workflowId1, "task-1", "Updated output from task 1");
            Map<String, String> updatedOutputs = memoryStore.loadTaskOutputs(workflowId1);
            assertEquals(3, updatedOutputs.size(), "Should still have 3 task outputs");
            assertEquals("Updated output from task 1", updatedOutputs.get("task-1"), "Task 1 output should be updated");

            return null;
        });
    }

    @Test
    @Order(3)
    @DisplayName("Test plan operations - save and load workflow plans with MERGE syntax")
    void testPlanOperations() throws Exception {
        withDatabase(memoryStore -> {
            String workflowId = "test-workflow";

            // Create initial plan
            TaskDefinition[] initialPlan = {
                new TaskDefinition("task1", "First task description", "Template for task 1"),
                new TaskDefinition("task2", "Second task description", "Template for task 2")
            };

            // Save initial plan
            memoryStore.savePlan(workflowId, initialPlan);

            // Load and verify initial plan
            TaskDefinition[] loadedPlan = memoryStore.loadPlan(workflowId).orElse(null);
            assertNotNull(loadedPlan, "Loaded plan should not be null");
            assertEquals(2, loadedPlan.length, "Should have 2 tasks");
            assertEquals("task1", loadedPlan[0].taskName);
            assertEquals("First task description", loadedPlan[0].taskDescription);
            assertEquals("Template for task 1", loadedPlan[0].promptTemplate);

            // Test MERGE behavior - update existing plan
            TaskDefinition[] updatedPlan = {
                new TaskDefinition("task1-updated", "Updated first task", "Updated template 1"),
                new TaskDefinition("task2-updated", "Updated second task", "Updated template 2"),
                new TaskDefinition("task3", "New third task", "Template for task 3")
            };

            memoryStore.savePlan(workflowId, updatedPlan);

            // Verify plan was updated (not duplicated)
            TaskDefinition[] finalPlan = memoryStore.loadPlan(workflowId).orElse(null);
            assertEquals(3, finalPlan.length, "Updated plan should have 3 tasks");
            assertEquals("task1-updated", finalPlan[0].taskName, "First task should be updated");
            assertEquals("task3", finalPlan[2].taskName, "Should have new third task");

            return null;
        });
    }

    @Test
    @Order(4)
    @DisplayName("Test handling of non-existent data")
    void testNonExistentDataHandling() throws Exception {
        withDatabase(memoryStore -> {
            // Test non-existent agent memory
            List<String> nonExistentMemory = memoryStore.loadMemory("non-existent-agent");
            assertTrue(nonExistentMemory.isEmpty(), "Non-existent agent should have empty memory");

            // Test non-existent workflow outputs
            Map<String, String> nonExistentOutputs = memoryStore.loadTaskOutputs("non-existent-workflow");
            assertTrue(nonExistentOutputs.isEmpty(), "Non-existent workflow should have empty outputs");

            // Test non-existent plan
            TaskDefinition[] nonExistentPlan = memoryStore.loadPlan("non-existent-workflow").orElse(null);
            assertNull(nonExistentPlan, "Non-existent plan should be null");

            return null;
        });
    }

    @Test
    @Order(5)
    @DisplayName("Test SQL injection prevention")
    void testSqlInjectionPrevention() throws Exception {
        withDatabase(memoryStore -> {
            // Test with malicious input that would cause SQL injection in vulnerable code
            String maliciousAgentName = "'; DROP TABLE subagent_memory; --";
            String maliciousContent = "Content with SQL: '; DELETE FROM subagent_memory WHERE 1=1; --";
            String maliciousWorkflowId = "'; DROP TABLE workflow_plans; --";
            String maliciousTaskName = "'; UPDATE TASK_OUTPUTS SET output='hacked' WHERE 1=1; --";

            // These operations should work safely without causing SQL injection
            assertDoesNotThrow(() -> {
                memoryStore.addMemory(maliciousAgentName, maliciousContent);
                memoryStore.saveTaskOutput(maliciousWorkflowId, maliciousTaskName, "Safe output");

                TaskDefinition[] plan = {new TaskDefinition("safe-task", "Safe description", "Safe template")};
                memoryStore.savePlan(maliciousWorkflowId, plan);
            }, "Operations with malicious input should not throw exceptions");

            // Verify the malicious input was stored as literal strings, not executed as SQL
            List<String> memories = memoryStore.loadMemory(maliciousAgentName);
            assertEquals(1, memories.size(), "Should have stored the malicious agent name safely");
            assertEquals(maliciousContent, memories.getFirst(), "Malicious content should be stored as literal string");

            Map<String, String> outputs = memoryStore.loadTaskOutputs(maliciousWorkflowId);
            assertEquals(1, outputs.size(), "Should have stored malicious workflow ID safely");
            assertTrue(outputs.containsKey(maliciousTaskName), "Malicious task name should be stored as key");

            return null;
        });
    }

    @Test
    @Order(6)
    @DisplayName("Test resource management - no connection leaks")
    void testResourceManagement() throws Exception {
        withDatabase(memoryStore -> {
            // Perform multiple operations to test that resources are properly closed
            for (int i = 0; i < 50; i++) {
                String agentName = "resource-test-agent-" + i;
                String workflowId = "resource-test-workflow-" + i;

                memoryStore.addMemory(agentName, "Memory entry " + i);
                memoryStore.saveTaskOutput(workflowId, "task-1", "Output " + i);

                TaskDefinition[] plan = {new TaskDefinition("task-" + i, "Description " + i, "Template " + i)};
                memoryStore.savePlan(workflowId, plan);

                // Load operations
                memoryStore.loadMemory(agentName);
                memoryStore.loadTaskOutputs(workflowId);
                memoryStore.loadPlan(workflowId);
            }

            // If we reach here without exceptions, resource management is working properly
            assertTrue(true, "Resource management test completed without issues");
            return null;
        });
    }
}
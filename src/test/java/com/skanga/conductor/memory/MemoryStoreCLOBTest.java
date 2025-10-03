package com.skanga.conductor.memory;

import com.skanga.conductor.orchestration.TaskDefinition;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryStoreCLOBTest {

    private static MemoryStore memoryStore;
    private static final String TEST_DB_URL = "jdbc:h2:mem:clobtest;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    @BeforeAll
    static void setUp() throws SQLException {
        memoryStore = new MemoryStore(TEST_DB_URL, USER, PASSWORD);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (memoryStore != null) {
            memoryStore.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test large text content handling")
    void testLargeTextContent() throws SQLException {
        String largeContent = "X".repeat(50000); // 50KB of text
        String agentName = "large-content-agent";

        memoryStore.addMemory(agentName, largeContent);

        List<String> memories = memoryStore.loadMemory(agentName);
        assertEquals(1, memories.size(), "Should store one large memory entry");
        assertEquals(largeContent, memories.getFirst(), "Large content should be stored and retrieved correctly");
        assertEquals(50000, memories.getFirst().length(), "Content length should be preserved");
    }

    @Test
    @Order(2)
    @DisplayName("Test Unicode and special character handling")
    void testUnicodeAndSpecialCharacters() throws SQLException {
        String unicodeContent = "Unicode test: 测试 العربية русский 日本語 हिन्दी 🌟🎉🚀💻🔥";
        String specialCharsContent = "Special chars: <>&\"'`~!@#$%^&*()_+-=[]{}|;:,.<>?/\\";
        String mixedContent = "Mixed: 测试<test>&\"quote\"🚀\n\tNewline and tab\r\n";
        String agentName = "unicode-agent";

        memoryStore.addMemory(agentName, unicodeContent);
        memoryStore.addMemory(agentName, specialCharsContent);
        memoryStore.addMemory(agentName, mixedContent);

        List<String> memories = memoryStore.loadMemory(agentName);
        assertEquals(3, memories.size(), "Should store all three entries");
        assertEquals(unicodeContent, memories.get(0), "Unicode content should be preserved exactly");
        assertEquals(specialCharsContent, memories.get(1), "Special characters should be preserved exactly");
        assertEquals(mixedContent, memories.get(2), "Mixed content should be preserved exactly");
    }

    @Test
    @Order(3)
    @DisplayName("Test JSON content in task outputs and plans")
    void testJSONContentHandling() throws SQLException {
        String complexJson = """
                {
                    "unicode": "测试 العربية русский",
                    "special": "<>&\\"'",
                    "nested": {
                        "array": [1, 2, 3],
                        "boolean": true,
                        "null": null
                    },
                    "multiline": "Line 1\\nLine 2\\tTabbed"
                }
                """;

        String workflowId = "json-workflow";

        // Test task output with complex JSON
        memoryStore.saveTaskOutput(workflowId, "json-task", complexJson);
        Map<String, String> outputs = memoryStore.loadTaskOutputs(workflowId);

        assertTrue(outputs.containsKey("json-task"), "Should contain the JSON task");
        assertEquals(complexJson, outputs.get("json-task"), "Complex JSON should be preserved exactly");

        // Test plan with complex descriptions
        TaskDefinition[] plan = {
            new TaskDefinition("task1", "Description with unicode: 测试", "Template with JSON: " + complexJson),
            new TaskDefinition("task2", "Description with quotes: \"hello\" and 'world'", "Template with special chars: <>&")
        };

        memoryStore.savePlan(workflowId, plan);
        TaskDefinition[] retrievedPlan = memoryStore.loadPlan(workflowId).orElse(null);

        assertNotNull(retrievedPlan, "Plan should be retrieved successfully");
        assertEquals(2, retrievedPlan.length, "Plan should have 2 tasks");
        assertEquals("Description with unicode: 测试", retrievedPlan[0].taskDescription, "Unicode in description should be preserved");
        assertTrue(retrievedPlan[0].promptTemplate.contains(complexJson), "Complex JSON in template should be preserved");
    }

    @Test
    @Order(4)
    @DisplayName("Test content with null bytes and control characters")
    void testControlCharacters() throws SQLException {
        // Note: Java strings automatically handle null bytes, but we test other control chars
        String controlCharsContent = "Control chars: \u0001\u0002\u0003\u0008\u000B\u000C\u000E\u000F";
        String tabsAndNewlines = "Tabs:\t\t\nNewlines:\n\n\rCarriage return:\r\r";
        String agentName = "control-chars-agent";

        memoryStore.addMemory(agentName, controlCharsContent);
        memoryStore.addMemory(agentName, tabsAndNewlines);

        List<String> memories = memoryStore.loadMemory(agentName);
        assertEquals(2, memories.size(), "Should store control character entries");
        assertEquals(controlCharsContent, memories.get(0), "Control characters should be preserved");
        assertEquals(tabsAndNewlines, memories.get(1), "Tabs and newlines should be preserved");
    }

    @Test
    @Order(5)
    @DisplayName("Test very long content boundary conditions")
    void testVeryLongContent() throws SQLException {
        // Test content at various size boundaries
        String[] testSizes = {
            "A".repeat(1000),      // 1KB
            "B".repeat(10000),     // 10KB
            "C".repeat(100000),    // 100KB
            "D".repeat(1000000)    // 1MB
        };

        String agentName = "boundary-test-agent";

        for (String testSize : testSizes) {
            memoryStore.addMemory(agentName, testSize);
        }

        List<String> memories = memoryStore.loadMemory(agentName);
        assertEquals(testSizes.length, memories.size(), "Should store all boundary test entries");

        for (int i = 0; i < testSizes.length; i++) {
            assertEquals(testSizes[i], memories.get(i),
                String.format("Content at size %d should be preserved exactly", testSizes[i].length()));
        }
    }

    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "test.comprehensive", matches = "true")
    @Test
    @Order(6)
    @DisplayName("Test CLOB handling efficiency")
    void testCLOBEfficiency() throws SQLException {
        // This test verifies that we're using efficient string operations
        // rather than creating unnecessary CLOB objects

        String agentName = "efficiency-test-agent";
        long startTime = System.currentTimeMillis();

        // Perform many operations
        for (int i = 0; i < 100; i++) {
            String content = "Efficiency test " + i + ": " + "X".repeat(1000);
            memoryStore.addMemory(agentName, content);

            // Also test task outputs and plans for efficiency
            String workflowId = "efficiency-workflow-" + i;
            memoryStore.saveTaskOutput(workflowId, "task", content);

            TaskDefinition[] plan = {new TaskDefinition("task", "desc", content)};
            memoryStore.savePlan(workflowId, plan);
        }

        // Load all memories
        List<String> allMemories = memoryStore.loadMemory(agentName);
        assertEquals(100, allMemories.size(), "Should have stored 100 memory entries");

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // If this takes more than 5 seconds, there might be efficiency issues
        assertTrue(duration < 5000,
            String.format("CLOB operations should be efficient (took %d ms)", duration));

        // Verify content integrity
        for (int i = 0; i < 100; i++) {
            String expectedContent = "Efficiency test " + i + ": " + "X".repeat(1000);
            assertEquals(expectedContent, allMemories.get(i),
                "Content should be preserved exactly during efficiency test");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test empty and whitespace content")
    void testEmptyAndWhitespaceContent() throws SQLException {
        String agentName = "whitespace-test-agent";

        String[] testContents = {
            "",                    // Empty string
            " ",                   // Single space
            "\t",                  // Single tab
            "\n",                  // Single newline
            "\r\n",               // Windows newline
            "   \t\n\r   ",       // Mixed whitespace
            "   content   ",       // Content with surrounding whitespace
        };

        for (String content : testContents) {
            memoryStore.addMemory(agentName, content);
        }

        List<String> memories = memoryStore.loadMemory(agentName);
        assertEquals(testContents.length, memories.size(), "Should store all whitespace test entries");

        for (int i = 0; i < testContents.length; i++) {
            assertEquals(testContents[i], memories.get(i),
                "Whitespace content should be preserved exactly: " +
                testContents[i].replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
        }
    }
}
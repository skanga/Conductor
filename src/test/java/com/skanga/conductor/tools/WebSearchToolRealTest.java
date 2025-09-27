package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the enhanced WebSearchTool with real web search capabilities.
 */
class WebSearchToolRealTest {

    @Test
    @Timeout(30) // 30 second timeout for web requests
    void testRealWebSearchWithDuckDuckGo() {
        WebSearchTool tool = new WebSearchTool();

        // Test a query that should return instant answer from DuckDuckGo
        ExecutionInput input = new ExecutionInput("what is java programming", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Web search should succeed");
        assertNotNull(result.output(), "Search should return output");
        assertFalse(result.output().trim().isEmpty(), "Output should not be empty");

        String output = result.output();
        System.out.println("=== Real Web Search Result ===");
        System.out.println(output);
        System.out.println("===============================");

        // Verify the output contains search-related content
        assertTrue(output.contains("Search results for: what is java programming") ||
                  output.contains("🔍"), "Output should contain search indicators");
    }

    @Test
    @Timeout(30)
    void testWebSearchFallbackToMock() {
        WebSearchTool tool = new WebSearchTool();

        // Test with a very specific query that might not return instant answers
        ExecutionInput input = new ExecutionInput("xyzabc123nonexistentquery", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Even failed real search should fall back to mock");
        assertNotNull(result.output(), "Should return fallback output");
        assertTrue(result.output().contains("xyzabc123nonexistentquery"),
                  "Output should contain the query");
    }

    @Test
    void testInputValidation() {
        WebSearchTool tool = new WebSearchTool();

        // Test null input
        ExecutionResult result = tool.runTool(null);
        assertFalse(result.success(), "Null input should fail");

        // Test empty query
        ExecutionInput emptyInput = new ExecutionInput("", null);
        result = tool.runTool(emptyInput);
        assertFalse(result.success(), "Empty query should fail");

        // Test null query
        ExecutionInput nullQuery = new ExecutionInput(null, null);
        result = tool.runTool(nullQuery);
        assertFalse(result.success(), "Null query should fail");
    }

    @Test
    void testToolMetadata() {
        WebSearchTool tool = new WebSearchTool();

        assertEquals("web_search", tool.toolName());
        assertNotNull(tool.toolDescription());
        assertTrue(tool.toolDescription().contains("real web search") ||
                  tool.toolDescription().contains("DuckDuckGo"));
    }
}
package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VariableSubstitution Simple Tests")
class VariableSubstitutionSimpleTest {

    private VariableSubstitution substitution;

    @BeforeEach
    void setUp() {
        substitution = new VariableSubstitution();
    }

    @Test
    @DisplayName("Should create VariableSubstitution with built-in variables")
    void testBuiltInVariables() {
        // When
        String timestamp = substitution.getVariable("timestamp");
        String uuid = substitution.getVariable("uuid");
        String userName = substitution.getVariable("user_name");

        // Then
        assertNotNull(timestamp);
        assertNotNull(uuid);
        assertNotNull(userName);
    }

    @Test
    @DisplayName("Should substitute simple variables")
    void testSimpleSubstitution() {
        // Given
        substitution.setRuntimeVariable("project_name", "test-project");

        // When
        String result = substitution.substitute("Project: ${project_name}");

        // Then
        assertEquals("Project: test-project", result);
    }

    @Test
    @DisplayName("Should substitute variables with defaults")
    void testDefaultValues() {
        // When
        String result = substitution.substitute("Value: ${nonexistent_var:-default_value}");

        // Then
        assertEquals("Value: default_value", result);
    }

    @Test
    @DisplayName("Should handle dot notation")
    void testDotNotation() {
        // Given
        Map<String, Object> context = Map.of(
            "stage", Map.of("result", Map.of("title", "Test Title"))
        );
        substitution.setRuntimeContext(context);

        // When
        String result = substitution.substitute("Title: ${stage.result.title}");

        // Then
        assertEquals("Title: Test Title", result);
    }

    @Test
    @DisplayName("Should handle multiple variables")
    void testMultipleVariables() {
        // Given
        substitution.setRuntimeVariable("name", "TestApp");
        substitution.setRuntimeVariable("version", "2.0");

        // When
        String result = substitution.substitute("App: ${name} v${version}");

        // Then
        assertEquals("App: TestApp v2.0", result);
    }

    @Test
    @DisplayName("Should leave unresolved variables unchanged")
    void testUnresolvedVariables() {
        // When
        String result = substitution.substitute("Unknown: ${unknown_variable}");

        // Then
        assertEquals("Unknown: ${unknown_variable}", result);
    }

    @Test
    @DisplayName("Should handle null and empty strings")
    void testNullAndEmpty() {
        // When & Then
        assertNull(substitution.substitute(null));
        assertEquals("", substitution.substitute(""));
        assertEquals("no variables", substitution.substitute("no variables"));
    }

    @Test
    @DisplayName("Should create copy with different context")
    void testWithContext() {
        // Given
        substitution.addVariable("global", "global-value");
        Map<String, Object> newContext = Map.of("local", "local-value");

        // When
        VariableSubstitution copy = substitution.withContext(newContext);

        // Then
        assertEquals("global-value", copy.getVariable("global"));
        assertEquals("local-value", copy.getVariable("local"));
    }

    @Test
    @DisplayName("Should substitute with temporary context")
    void testSubstituteWithContext() {
        // Given
        Map<String, Object> tempContext = Map.of("temp_var", "temp-value");

        // When
        String result = substitution.substituteWithContext("Temp: ${temp_var}", tempContext);

        // Then
        assertEquals("Temp: temp-value", result);
    }

    @Test
    @DisplayName("Should manage runtime context")
    void testRuntimeContextManagement() {
        // Test setting context
        Map<String, Object> context1 = Map.of("var1", "value1");
        substitution.setRuntimeContext(context1);
        assertEquals(context1, substitution.getRuntimeContext());

        // Test updating context
        Map<String, Object> context2 = Map.of("var2", "value2");
        substitution.updateRuntimeContext(context2);

        Map<String, Object> merged = substitution.getRuntimeContext();
        assertEquals("value1", merged.get("var1"));
        assertEquals("value2", merged.get("var2"));

        // Test clearing context
        substitution.clearRuntimeContext();
        assertTrue(substitution.getRuntimeContext().isEmpty());
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        // Given
        substitution.setRuntimeVariable("shared_var", "shared-value");

        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Map<String, Object> threadContext = new HashMap<>();
                threadContext.put("shared_var", "shared-value"); // Include shared variable in thread context
                threadContext.put("thread_id", "thread-" + index);
                results[index] = substitution.substituteWithContext(
                    "Shared: ${shared_var}, Thread: ${thread_id}",
                    threadContext
                );
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertEquals("Shared: shared-value, Thread: thread-" + i, results[i]);
        }
    }
}
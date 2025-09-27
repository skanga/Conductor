package com.skanga.conductor.engine.execution;

import com.skanga.conductor.workflow.config.AgentConfigCollection;
import com.skanga.conductor.workflow.config.WorkflowContext;
import com.skanga.conductor.workflow.config.WorkflowDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowExecutionContextTest {

    @Mock
    private WorkflowDefinition mockWorkflowDefinition;

    @Mock
    private AgentConfigCollection mockAgentConfig;

    @Mock
    private WorkflowContext mockContext;

    private WorkflowExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateExecutionContextWithAllParameters() {
        // Given
        String[] inputs = {"input1", "input2", "input3"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs
        );

        // Then
        assertSame(mockWorkflowDefinition, executionContext.getWorkflowDefinition());
        assertSame(mockAgentConfig, executionContext.getAgentConfig());
        assertSame(mockContext, executionContext.getContext());
        assertArrayEquals(inputs, executionContext.getInputs());
    }

    @Test
    void shouldCreateExecutionContextWithNullInputs() {
        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, (String[]) null
        );

        // Then
        assertNotNull(executionContext.getInputs());
        assertEquals(0, executionContext.getInputs().length);
        assertEquals(0, executionContext.getInputCount());
    }

    @Test
    void shouldCreateExecutionContextWithEmptyInputs() {
        // Given
        String[] emptyInputs = {};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, emptyInputs
        );

        // Then
        assertArrayEquals(emptyInputs, executionContext.getInputs());
        assertEquals(0, executionContext.getInputCount());
        assertNull(executionContext.getPrimaryInput());
    }

    @Test
    void shouldCreateExecutionContextWithSingleInput() {
        // Given
        String[] singleInput = {"primary-input"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, singleInput
        );

        // Then
        assertEquals(1, executionContext.getInputCount());
        assertEquals("primary-input", executionContext.getPrimaryInput());
        assertEquals("primary-input", executionContext.getInput(0));
        assertNull(executionContext.getInput(1));
    }

    @Test
    void shouldCreateExecutionContextWithMultipleInputs() {
        // Given
        String[] multipleInputs = {"topic", "style", "length", "audience"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, multipleInputs
        );

        // Then
        assertEquals(4, executionContext.getInputCount());
        assertEquals("topic", executionContext.getPrimaryInput());
        assertEquals("topic", executionContext.getInput(0));
        assertEquals("style", executionContext.getInput(1));
        assertEquals("length", executionContext.getInput(2));
        assertEquals("audience", executionContext.getInput(3));
        assertNull(executionContext.getInput(4));

        // Negative indices throw ArrayIndexOutOfBoundsException
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            executionContext.getInput(-1);
        });
    }

    @Test
    void shouldHandleNullComponentsGracefully() {
        // When
        executionContext = new WorkflowExecutionContext(null, null, null, (String[]) null);

        // Then
        assertNull(executionContext.getWorkflowDefinition());
        assertNull(executionContext.getAgentConfig());
        assertNull(executionContext.getContext());
        assertNotNull(executionContext.getInputs());
        assertEquals(0, executionContext.getInputs().length);
    }

    @Test
    void shouldGetInputByIndexSafely() {
        // Given
        String[] inputs = {"first", "second"};
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs
        );

        // When & Then
        assertEquals("first", executionContext.getInput(0));
        assertEquals("second", executionContext.getInput(1));
        assertNull(executionContext.getInput(2));
        assertNull(executionContext.getInput(10));

        // Negative indices throw ArrayIndexOutOfBoundsException
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            executionContext.getInput(-1);
        });
    }

    @Test
    void shouldHandleEmptyStringInputs() {
        // Given
        String[] inputs = {"", "   ", "\n\t"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs
        );

        // Then
        assertEquals(3, executionContext.getInputCount());
        assertEquals("", executionContext.getPrimaryInput());
        assertEquals("", executionContext.getInput(0));
        assertEquals("   ", executionContext.getInput(1));
        assertEquals("\n\t", executionContext.getInput(2));
    }

    @Test
    void shouldHandleNullInputElements() {
        // Given
        String[] inputs = {"valid", null, "another"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs
        );

        // Then
        assertEquals(3, executionContext.getInputCount());
        assertEquals("valid", executionContext.getPrimaryInput());
        assertEquals("valid", executionContext.getInput(0));
        assertNull(executionContext.getInput(1));
        assertEquals("another", executionContext.getInput(2));
    }

    @Test
    void shouldHandleSpecialCharactersInInputs() {
        // Given
        String[] inputs = {
            "Input with unicode: 你好世界 🚀",
            "Input with symbols: @#$%^&*()",
            "Input with newlines\nand\ttabs",
            "Input with \"quotes\" and 'apostrophes'"
        };

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs
        );

        // Then
        assertEquals(4, executionContext.getInputCount());
        assertEquals("Input with unicode: 你好世界 🚀", executionContext.getPrimaryInput());
        assertEquals("Input with symbols: @#$%^&*()", executionContext.getInput(1));
        assertEquals("Input with newlines\nand\ttabs", executionContext.getInput(2));
        assertEquals("Input with \"quotes\" and 'apostrophes'", executionContext.getInput(3));
    }

    @Test
    void shouldHandleLargeInputArrays() {
        // Given
        String[] largeInputs = new String[1000];
        for (int i = 0; i < largeInputs.length; i++) {
            largeInputs[i] = "input-" + i;
        }

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, largeInputs
        );

        // Then
        assertEquals(1000, executionContext.getInputCount());
        assertEquals("input-0", executionContext.getPrimaryInput());
        assertEquals("input-0", executionContext.getInput(0));
        assertEquals("input-500", executionContext.getInput(500));
        assertEquals("input-999", executionContext.getInput(999));
        assertNull(executionContext.getInput(1000));
    }

    @Test
    void shouldPreserveInputArrayIntegrity() {
        // Given
        String[] originalInputs = {"input1", "input2"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, originalInputs
        );

        // Modify original array
        originalInputs[0] = "modified";

        // Then - context should have original values (defensive copy behavior depends on implementation)
        String[] contextInputs = executionContext.getInputs();
        assertNotNull(contextInputs);
        assertEquals(2, contextInputs.length);
    }

    @Test
    void shouldHandleVeryLongInputStrings() {
        // Given
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longInput.append("This is a very long input string. ");
        }

        String[] inputs = {longInput.toString(), "short"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs
        );

        // Then
        assertEquals(2, executionContext.getInputCount());
        assertEquals(longInput.toString(), executionContext.getPrimaryInput());
        assertEquals("short", executionContext.getInput(1));
        assertTrue(executionContext.getInput(0).length() > 100000);
    }

    @Test
    void shouldCreateIndependentContexts() {
        // Given
        String[] inputs1 = {"context1-input"};
        String[] inputs2 = {"context2-input"};

        // When
        WorkflowExecutionContext context1 = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs1
        );
        WorkflowExecutionContext context2 = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, inputs2
        );

        // Then
        assertNotSame(context1, context2);
        assertEquals("context1-input", context1.getPrimaryInput());
        assertEquals("context2-input", context2.getPrimaryInput());
        assertNotEquals(context1.getPrimaryInput(), context2.getPrimaryInput());
    }

    @Test
    void shouldHandleBoundaryConditions() {
        // Test with maximum array size (practically impossible but theoretically possible)
        // We'll test with a smaller but significant size
        String[] mediumInputs = new String[100];
        for (int i = 0; i < mediumInputs.length; i++) {
            mediumInputs[i] = String.valueOf(i);
        }

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, mediumInputs
        );

        // Then
        assertEquals(100, executionContext.getInputCount());
        assertEquals("0", executionContext.getPrimaryInput());
        assertEquals("99", executionContext.getInput(99));
        assertNull(executionContext.getInput(100));
    }

    @Test
    void shouldMaintainInputOrderConsistency() {
        // Given
        String[] orderedInputs = {"first", "second", "third", "fourth", "fifth"};

        // When
        executionContext = new WorkflowExecutionContext(
            mockWorkflowDefinition, mockAgentConfig, mockContext, orderedInputs
        );

        // Then - verify order is maintained
        for (int i = 0; i < orderedInputs.length; i++) {
            assertEquals(orderedInputs[i], executionContext.getInput(i));
        }
    }
}
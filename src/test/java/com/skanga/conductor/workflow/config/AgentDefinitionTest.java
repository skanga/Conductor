package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentDefinitionTest {

    private AgentDefinition agentDefinition;

    @BeforeEach
    void setUp() {
        agentDefinition = new AgentDefinition();
    }

    @Test
    void shouldCreateAgentDefinitionWithDefaults() {
        // Then
        assertEquals("llm", agentDefinition.getType()); // Default type
        assertNull(agentDefinition.getRole());
        assertNull(agentDefinition.getProvider());
        assertNull(agentDefinition.getModel());
        assertNull(agentDefinition.getPromptTemplate());
        assertNull(agentDefinition.getContextWindow());
        assertNull(agentDefinition.getParameters());
    }

    @Test
    void shouldSetAndGetAllProperties() {
        // When
        agentDefinition.setType("custom");
        agentDefinition.setRole("Test Agent");
        agentDefinition.setProvider("openai");
        agentDefinition.setModel("gpt-4");
        agentDefinition.setPromptTemplate("Test prompt template");
        agentDefinition.setContextWindow(4096);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", 0.7);
        parameters.put("max_tokens", 1000);
        agentDefinition.setParameters(parameters);

        // Then
        assertEquals("custom", agentDefinition.getType());
        assertEquals("Test Agent", agentDefinition.getRole());
        assertEquals("openai", agentDefinition.getProvider());
        assertEquals("gpt-4", agentDefinition.getModel());
        assertEquals("Test prompt template", agentDefinition.getPromptTemplate());
        assertEquals(4096, agentDefinition.getContextWindow());
        assertEquals(parameters, agentDefinition.getParameters());
    }

    @Test
    void shouldGetParameterWithDefault() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", 0.7);
        parameters.put("max_tokens", 1000);
        agentDefinition.setParameters(parameters);

        // When & Then
        assertEquals(0.7, agentDefinition.getParameter("temperature", 0.5));
        assertEquals(1000, agentDefinition.getParameter("max_tokens", 500));
        assertEquals("default", agentDefinition.getParameter("non_existent", "default"));
    }

    @Test
    void shouldReturnDefaultWhenParametersIsNull() {
        // Given
        agentDefinition.setParameters(null);

        // When & Then
        assertEquals("default", agentDefinition.getParameter("any_key", "default"));
    }

    @Test
    void shouldGetStringParameter() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("string_param", "test_value");
        parameters.put("number_param", 123);
        parameters.put("null_param", null);
        agentDefinition.setParameters(parameters);

        // When & Then
        assertEquals("test_value", agentDefinition.getStringParameter("string_param", "default"));
        assertEquals("123", agentDefinition.getStringParameter("number_param", "default"));
        assertEquals("default", agentDefinition.getStringParameter("null_param", "default"));
        assertEquals("default", agentDefinition.getStringParameter("non_existent", "default"));
    }

    @Test
    void shouldGetIntegerParameter() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("int_param", 42);
        parameters.put("long_param", 123L);
        parameters.put("double_param", 45.6);
        parameters.put("string_number", "789");
        parameters.put("invalid_string", "not_a_number");
        parameters.put("null_param", null);
        agentDefinition.setParameters(parameters);

        // When & Then
        assertEquals(42, agentDefinition.getIntegerParameter("int_param", 0));
        assertEquals(123, agentDefinition.getIntegerParameter("long_param", 0));
        assertEquals(45, agentDefinition.getIntegerParameter("double_param", 0));
        assertEquals(789, agentDefinition.getIntegerParameter("string_number", 0));
        assertEquals(99, agentDefinition.getIntegerParameter("invalid_string", 99));
        assertEquals(99, agentDefinition.getIntegerParameter("null_param", 99));
        assertEquals(99, agentDefinition.getIntegerParameter("non_existent", 99));
    }

    @Test
    void shouldValidateValidLLMAgent() {
        // Given
        agentDefinition.setType("llm");
        agentDefinition.setRole("Test Agent");
        agentDefinition.setPromptTemplate("Test prompt template");

        // When & Then
        assertDoesNotThrow(() -> agentDefinition.validate());
    }

    @Test
    void shouldValidateValidNonLLMAgent() {
        // Given
        agentDefinition.setType("tool");
        agentDefinition.setRole("Tool Agent");
        // No prompt template needed for non-LLM agents

        // When & Then
        assertDoesNotThrow(() -> agentDefinition.validate());
    }

    @Test
    void shouldThrowExceptionWhenRoleIsMissing() {
        // Given
        agentDefinition.setType("llm");
        // Role is not set

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentDefinition.validate());
        assertEquals("Agent role is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRoleIsEmpty() {
        // Given
        agentDefinition.setType("llm");
        agentDefinition.setRole("   "); // Empty/whitespace role

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentDefinition.validate());
        assertEquals("Agent role is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        // Given
        agentDefinition.setRole("Test Agent");
        agentDefinition.setType(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentDefinition.validate());
        assertEquals("Agent type is required for agent: Test Agent", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTypeIsEmpty() {
        // Given
        agentDefinition.setRole("Test Agent");
        agentDefinition.setType("   "); // Empty/whitespace type

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentDefinition.validate());
        assertEquals("Agent type is required for agent: Test Agent", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenLLMAgentMissingPromptTemplate() {
        // Given
        agentDefinition.setType("llm");
        agentDefinition.setRole("LLM Agent");
        // Prompt template is not set

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentDefinition.validate());
        assertEquals("LLM agent 'LLM Agent' must have a prompt template", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenLLMAgentHasEmptyPromptTemplate() {
        // Given
        agentDefinition.setType("llm");
        agentDefinition.setRole("LLM Agent");
        agentDefinition.setPromptTemplate("   "); // Empty/whitespace prompt template

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentDefinition.validate());
        assertEquals("LLM agent 'LLM Agent' must have a prompt template", exception.getMessage());
    }

    @Test
    void shouldGenerateToStringRepresentation() {
        // Given
        agentDefinition.setType("llm");
        agentDefinition.setRole("Test Agent");
        agentDefinition.setProvider("openai");
        agentDefinition.setModel("gpt-4");
        agentDefinition.setPromptTemplate("Test prompt");

        // When
        String toString = agentDefinition.toString();

        // Then
        assertTrue(toString.contains("type='llm'"));
        assertTrue(toString.contains("role='Test Agent'"));
        assertTrue(toString.contains("provider='openai'"));
        assertTrue(toString.contains("model='gpt-4'"));
        assertTrue(toString.contains("promptTemplate='Test prompt'"));
    }

    @Test
    void shouldGetParameterWithNullParameters() {
        // Given
        agentDefinition.setParameters(null);

        // When & Then
        assertEquals("default", agentDefinition.getStringParameter("any_key", "default"));
        assertEquals(42, agentDefinition.getIntegerParameter("any_key", 42));
        assertEquals("fallback", agentDefinition.getParameter("any_key", "fallback"));
    }
}
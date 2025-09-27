package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentConfigCollectionTest {

    private AgentConfigCollection agentConfigCollection;

    @BeforeEach
    void setUp() {
        agentConfigCollection = new AgentConfigCollection();
    }

    @Test
    void shouldCreateAgentConfigCollectionWithNullValues() {
        // Then
        assertNull(agentConfigCollection.getAgents());
        assertNull(agentConfigCollection.getPromptTemplates());
    }

    @Test
    void shouldSetAndGetAgents() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent1 = new AgentDefinition();
        agent1.setType("llm");
        agent1.setRole("Test Agent 1");
        agent1.setPromptTemplate("Test prompt 1");
        agents.put("agent1", agent1);

        // When
        agentConfigCollection.setAgents(agents);

        // Then
        assertEquals(agents, agentConfigCollection.getAgents());
        assertEquals(1, agentConfigCollection.getAgents().size());
        assertTrue(agentConfigCollection.getAgents().containsKey("agent1"));
    }

    @Test
    void shouldSetAndGetPromptTemplates() {
        // Given
        Map<String, AgentConfigCollection.PromptTemplate> promptTemplates = new HashMap<>();
        AgentConfigCollection.PromptTemplate template1 = new AgentConfigCollection.PromptTemplate();
        template1.setSystem("System message");
        template1.setUser("User message");
        promptTemplates.put("template1", template1);

        // When
        agentConfigCollection.setPromptTemplates(promptTemplates);

        // Then
        assertEquals(promptTemplates, agentConfigCollection.getPromptTemplates());
        assertEquals(1, agentConfigCollection.getPromptTemplates().size());
        assertTrue(agentConfigCollection.getPromptTemplates().containsKey("template1"));
    }

    @Test
    void shouldGetAgentById() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent1 = new AgentDefinition();
        agent1.setRole("Test Agent 1");
        agents.put("agent1", agent1);

        AgentDefinition agent2 = new AgentDefinition();
        agent2.setRole("Test Agent 2");
        agents.put("agent2", agent2);

        agentConfigCollection.setAgents(agents);

        // When & Then
        Optional<AgentDefinition> result1 = agentConfigCollection.getAgent("agent1");
        assertTrue(result1.isPresent());
        assertEquals("Test Agent 1", result1.get().getRole());

        Optional<AgentDefinition> result2 = agentConfigCollection.getAgent("agent2");
        assertTrue(result2.isPresent());
        assertEquals("Test Agent 2", result2.get().getRole());

        Optional<AgentDefinition> result3 = agentConfigCollection.getAgent("non-existent");
        assertFalse(result3.isPresent());
    }

    @Test
    void shouldReturnEmptyOptionalWhenAgentsIsNull() {
        // Given
        agentConfigCollection.setAgents(null);

        // When & Then
        Optional<AgentDefinition> result = agentConfigCollection.getAgent("any-id");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldGetPromptTemplateById() {
        // Given
        Map<String, AgentConfigCollection.PromptTemplate> promptTemplates = new HashMap<>();
        AgentConfigCollection.PromptTemplate template1 = new AgentConfigCollection.PromptTemplate();
        template1.setSystem("System message 1");
        promptTemplates.put("template1", template1);

        AgentConfigCollection.PromptTemplate template2 = new AgentConfigCollection.PromptTemplate();
        template2.setUser("User message 2");
        promptTemplates.put("template2", template2);

        agentConfigCollection.setPromptTemplates(promptTemplates);

        // When & Then
        Optional<AgentConfigCollection.PromptTemplate> result1 = agentConfigCollection.getPromptTemplate("template1");
        assertTrue(result1.isPresent());
        assertEquals("System message 1", result1.get().getSystem());

        Optional<AgentConfigCollection.PromptTemplate> result2 = agentConfigCollection.getPromptTemplate("template2");
        assertTrue(result2.isPresent());
        assertEquals("User message 2", result2.get().getUser());

        Optional<AgentConfigCollection.PromptTemplate> result3 = agentConfigCollection.getPromptTemplate("non-existent");
        assertFalse(result3.isPresent());
    }

    @Test
    void shouldReturnEmptyOptionalWhenPromptTemplatesIsNull() {
        // Given
        agentConfigCollection.setPromptTemplates(null);

        // When & Then
        Optional<AgentConfigCollection.PromptTemplate> result = agentConfigCollection.getPromptTemplate("any-id");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldValidateValidConfiguration() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("tool"); // Use tool type which doesn't require prompt template
        agent.setRole("Test Agent");
        agents.put("test-agent", agent);
        agentConfigCollection.setAgents(agents);

        // When & Then
        assertDoesNotThrow(() -> agentConfigCollection.validate());
    }

    @Test
    void shouldThrowExceptionWhenAgentsIsNull() {
        // Given
        agentConfigCollection.setAgents(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertEquals("At least one agent definition is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAgentsIsEmpty() {
        // Given
        agentConfigCollection.setAgents(new HashMap<>());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertEquals("At least one agent definition is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAgentIdIsEmpty() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("llm");
        agent.setRole("Test Agent");
        agent.setPromptTemplate("Test prompt");
        agents.put("", agent); // Empty agent ID
        agentConfigCollection.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertEquals("Agent ID cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAgentIdIsNull() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("llm");
        agent.setRole("Test Agent");
        agent.setPromptTemplate("Test prompt");
        agents.put(null, agent); // Null agent ID
        agentConfigCollection.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertEquals("Agent ID cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAgentValidationFails() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition invalidAgent = new AgentDefinition();
        // Missing required fields - will fail validation
        agents.put("invalid-agent", invalidAgent);
        agentConfigCollection.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertTrue(exception.getMessage().contains("Invalid agent 'invalid-agent'"));
    }

    @Test
    void shouldThrowExceptionWhenPromptTemplateIdIsEmpty() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("tool"); // Use tool type to avoid template reference validation
        agent.setRole("Test Agent");
        agents.put("test-agent", agent);
        agentConfigCollection.setAgents(agents);

        Map<String, AgentConfigCollection.PromptTemplate> promptTemplates = new HashMap<>();
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        template.setSystem("System message");
        promptTemplates.put("", template); // Empty template ID
        agentConfigCollection.setPromptTemplates(promptTemplates);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertEquals("Prompt template ID cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPromptTemplateValidationFails() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("tool"); // Use tool type to avoid template reference validation
        agent.setRole("Test Agent");
        agents.put("test-agent", agent);
        agentConfigCollection.setAgents(agents);

        Map<String, AgentConfigCollection.PromptTemplate> promptTemplates = new HashMap<>();
        AgentConfigCollection.PromptTemplate invalidTemplate = new AgentConfigCollection.PromptTemplate();
        // Missing required content - will fail validation
        promptTemplates.put("invalid-template", invalidTemplate);
        agentConfigCollection.setPromptTemplates(promptTemplates);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertTrue(exception.getMessage().contains("Invalid prompt template 'invalid-template'"));
    }

    @Test
    void shouldThrowExceptionWhenAgentReferencesUnknownTemplate() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("llm");
        agent.setRole("Test Agent");
        agent.setPromptTemplate("unknown-template"); // References non-existent template
        agents.put("test-agent", agent);
        agentConfigCollection.setAgents(agents);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> agentConfigCollection.validate());
        assertTrue(exception.getMessage().contains("references unknown prompt template"));
    }

    @Test
    void shouldValidateWhenAgentReferencesExistingTemplate() {
        // Given
        Map<String, AgentConfigCollection.PromptTemplate> promptTemplates = new HashMap<>();
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        template.setSystem("System message");
        promptTemplates.put("existing-template", template);
        agentConfigCollection.setPromptTemplates(promptTemplates);

        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("llm");
        agent.setRole("Test Agent");
        agent.setPromptTemplate("existing-template"); // References existing template
        agents.put("test-agent", agent);
        agentConfigCollection.setAgents(agents);

        // When & Then
        assertDoesNotThrow(() -> agentConfigCollection.validate());
    }

    @Test
    void shouldValidateWhenPromptTemplatesIsNull() {
        // Given
        Map<String, AgentDefinition> agents = new HashMap<>();
        AgentDefinition agent = new AgentDefinition();
        agent.setType("tool"); // Tool type doesn't require prompt template
        agent.setRole("Test Agent");
        agents.put("test-agent", agent);
        agentConfigCollection.setAgents(agents);
        agentConfigCollection.setPromptTemplates(null);

        // When & Then
        assertDoesNotThrow(() -> agentConfigCollection.validate());
    }

    @Test
    void shouldTestPromptTemplateClass() {
        // Given
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();

        // Then - test defaults
        assertNull(template.getSystem());
        assertNull(template.getUser());
        assertNull(template.getAssistant());
        assertFalse(template.hasSystem());
        assertFalse(template.hasUser());
        assertFalse(template.hasAssistant());

        // When - set all properties
        template.setSystem("System message");
        template.setUser("User message");
        template.setAssistant("Assistant message");

        // Then
        assertEquals("System message", template.getSystem());
        assertEquals("User message", template.getUser());
        assertEquals("Assistant message", template.getAssistant());
        assertTrue(template.hasSystem());
        assertTrue(template.hasUser());
        assertTrue(template.hasAssistant());
    }

    @Test
    void shouldValidatePromptTemplateWithSystemMessage() {
        // Given
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        template.setSystem("System message");

        // When & Then
        assertDoesNotThrow(() -> template.validate());
    }

    @Test
    void shouldValidatePromptTemplateWithUserMessage() {
        // Given
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        template.setUser("User message");

        // When & Then
        assertDoesNotThrow(() -> template.validate());
    }

    @Test
    void shouldThrowExceptionWhenPromptTemplateHasNoContent() {
        // Given
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        // No content set

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> template.validate());
        assertEquals("Prompt template must have at least system or user content", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPromptTemplateHasOnlyWhitespace() {
        // Given
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        template.setSystem("   "); // Only whitespace
        template.setUser("   "); // Only whitespace

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> template.validate());
        assertEquals("Prompt template must have at least system or user content", exception.getMessage());
    }

    @Test
    void shouldTestPromptTemplateHasMethodsWithWhitespace() {
        // Given
        AgentConfigCollection.PromptTemplate template = new AgentConfigCollection.PromptTemplate();
        template.setSystem("   "); // Only whitespace
        template.setUser("   "); // Only whitespace
        template.setAssistant("   "); // Only whitespace

        // Then
        assertFalse(template.hasSystem());
        assertFalse(template.hasUser());
        assertFalse(template.hasAssistant());
    }

    @Test
    void shouldValidateComplexConfiguration() {
        // Given
        Map<String, AgentConfigCollection.PromptTemplate> promptTemplates = new HashMap<>();
        AgentConfigCollection.PromptTemplate template1 = new AgentConfigCollection.PromptTemplate();
        template1.setSystem("System message 1");
        template1.setUser("User message 1");
        promptTemplates.put("template1", template1);

        AgentConfigCollection.PromptTemplate template2 = new AgentConfigCollection.PromptTemplate();
        template2.setSystem("System message 2");
        promptTemplates.put("template2", template2);

        agentConfigCollection.setPromptTemplates(promptTemplates);

        Map<String, AgentDefinition> agents = new HashMap<>();

        AgentDefinition agent1 = new AgentDefinition();
        agent1.setType("llm");
        agent1.setRole("Agent 1");
        agent1.setPromptTemplate("template1");
        agents.put("agent1", agent1);

        AgentDefinition agent2 = new AgentDefinition();
        agent2.setType("llm");
        agent2.setRole("Agent 2");
        agent2.setPromptTemplate("template2");
        agents.put("agent2", agent2);

        AgentDefinition agent3 = new AgentDefinition();
        agent3.setType("tool");
        agent3.setRole("Tool Agent");
        // No template reference for tool agent
        agents.put("agent3", agent3);

        agentConfigCollection.setAgents(agents);

        // When & Then
        assertDoesNotThrow(() -> agentConfigCollection.validate());
    }
}
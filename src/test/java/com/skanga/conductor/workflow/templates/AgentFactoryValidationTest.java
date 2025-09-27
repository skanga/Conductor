package com.skanga.conductor.workflow.templates;

import com.skanga.conductor.workflow.config.AgentDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified tests for AgentFactory focusing on validation and creation logic
 * without complex mocking that causes issues.
 */
@DisplayName("AgentFactory Validation Tests")
class AgentFactoryValidationTest {

    private AgentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AgentFactory();
    }

    @Nested
    @DisplayName("Factory Creation Tests")
    class FactoryCreationTests {

        @Test
        @DisplayName("Should create factory with default tool registry")
        void shouldCreateFactoryWithDefaultToolRegistry() {
            AgentFactory defaultFactory = new AgentFactory();
            assertNotNull(defaultFactory);
        }

        @Test
        @DisplayName("Should create factory with null tool registry")
        void shouldCreateFactoryWithNullToolRegistry() {
            AgentFactory factory = new AgentFactory(null);
            assertNotNull(factory);
        }
    }

    @Nested
    @DisplayName("Agent Definition Validation Tests")
    class AgentDefinitionValidationTests {

        @Test
        @DisplayName("Should validate null definition")
        void shouldValidateNullDefinition() {
            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(null));
        }

        @Test
        @DisplayName("Should validate LLM agent definition")
        void shouldValidateLLMAgentDefinition() {
            AgentDefinition validDefinition = new AgentDefinition();
            validDefinition.setType("llm");
            validDefinition.setRole("test-role");
            validDefinition.setPromptTemplate("Valid prompt template");

            assertDoesNotThrow(() -> factory.validateDefinition(validDefinition));
        }

        @Test
        @DisplayName("Should reject LLM agent without prompt template")
        void shouldRejectLLMAgentWithoutPromptTemplate() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("llm");
            invalidDefinition.setRole("test-role");
            invalidDefinition.setPromptTemplate(null);

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }

        @Test
        @DisplayName("Should reject LLM agent with empty prompt template")
        void shouldRejectLLMAgentWithEmptyPromptTemplate() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("llm");
            invalidDefinition.setRole("test-role");
            invalidDefinition.setPromptTemplate("");

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }

        @Test
        @DisplayName("Should validate tool agent definition with valid strategy")
        void shouldValidateToolAgentDefinitionWithValidStrategy() {
            AgentDefinition validDefinition = new AgentDefinition();
            validDefinition.setType("tool");
            validDefinition.setRole("tool-role");
            validDefinition.setProvider("llm-tool");

            assertDoesNotThrow(() -> factory.validateDefinition(validDefinition));
        }

        @Test
        @DisplayName("Should validate tool agent definition with direct strategy")
        void shouldValidateToolAgentDefinitionWithDirectStrategy() {
            AgentDefinition validDefinition = new AgentDefinition();
            validDefinition.setType("tool");
            validDefinition.setRole("tool-role");
            validDefinition.setProvider("direct-tool");

            assertDoesNotThrow(() -> factory.validateDefinition(validDefinition));
        }

        @Test
        @DisplayName("Should reject tool agent with invalid strategy")
        void shouldRejectToolAgentWithInvalidStrategy() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("tool");
            invalidDefinition.setRole("tool-role");
            invalidDefinition.setProvider("invalid-strategy");

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }

        @Test
        @DisplayName("Should allow tool agent with null provider")
        void shouldAllowToolAgentWithNullProvider() {
            AgentDefinition validDefinition = new AgentDefinition();
            validDefinition.setType("tool");
            validDefinition.setRole("tool-role");
            validDefinition.setProvider(null);

            assertDoesNotThrow(() -> factory.validateDefinition(validDefinition));
        }

        @Test
        @DisplayName("Should reject unsupported agent types")
        void shouldRejectUnsupportedAgentTypes() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("unsupported");
            invalidDefinition.setRole("test-role");
            invalidDefinition.setPromptTemplate("template");

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }

        @Test
        @DisplayName("Should handle case insensitive validation")
        void shouldHandleCaseInsensitiveValidation() {
            AgentDefinition upperCaseDefinition = new AgentDefinition();
            upperCaseDefinition.setType("LLM");
            upperCaseDefinition.setRole("test-role");
            upperCaseDefinition.setPromptTemplate("template");

            assertDoesNotThrow(() -> factory.validateDefinition(upperCaseDefinition));

            AgentDefinition mixedCaseDefinition = new AgentDefinition();
            mixedCaseDefinition.setType("Tool");
            mixedCaseDefinition.setRole("tool-role");
            mixedCaseDefinition.setProvider("LLM-Tool");

            assertDoesNotThrow(() -> factory.validateDefinition(mixedCaseDefinition));
        }

        @Test
        @DisplayName("Should reject agent definition with null type")
        void shouldRejectAgentDefinitionWithNullType() {
            AgentDefinition definition = new AgentDefinition();
            definition.setType(null);
            definition.setRole("test-role");
            definition.setPromptTemplate("template");

            // Should throw because type is null
            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(definition));
        }

        @Test
        @DisplayName("Should reject agent definition with null role")
        void shouldRejectAgentDefinitionWithNullRole() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("llm");
            invalidDefinition.setRole(null);
            invalidDefinition.setPromptTemplate("template");

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }

        @Test
        @DisplayName("Should reject agent definition with empty role")
        void shouldRejectAgentDefinitionWithEmptyRole() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("llm");
            invalidDefinition.setRole("");
            invalidDefinition.setPromptTemplate("template");

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }

        @Test
        @DisplayName("Should reject agent definition with whitespace-only role")
        void shouldRejectAgentDefinitionWithWhitespaceOnlyRole() {
            AgentDefinition invalidDefinition = new AgentDefinition();
            invalidDefinition.setType("llm");
            invalidDefinition.setRole("   ");
            invalidDefinition.setPromptTemplate("template");

            assertThrows(IllegalArgumentException.class, () ->
                factory.validateDefinition(invalidDefinition));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle agent definitions with additional parameters")
        void shouldHandleAgentDefinitionsWithAdditionalParameters() {
            AgentDefinition definition = new AgentDefinition();
            definition.setType("llm");
            definition.setRole("parameterized-agent");
            definition.setPromptTemplate("Template with {{parameter}}");
            definition.setModel("gpt-4");
            definition.setContextWindow(4000);

            assertDoesNotThrow(() -> factory.validateDefinition(definition));
        }

        @Test
        @DisplayName("Should validate agent definition toString")
        void shouldValidateAgentDefinitionToString() {
            AgentDefinition definition = new AgentDefinition();
            definition.setType("llm");
            definition.setRole("test-agent");
            definition.setPromptTemplate("Test template");
            definition.setProvider("openai");
            definition.setModel("gpt-3.5-turbo");

            String toString = definition.toString();
            assertNotNull(toString);
            assertTrue(toString.contains("AgentDefinition"));
            assertTrue(toString.contains("llm"));
            assertTrue(toString.contains("test-agent"));
        }

        @Test
        @DisplayName("Should handle getParameter methods correctly")
        void shouldHandleGetParameterMethodsCorrectly() {
            AgentDefinition definition = new AgentDefinition();
            definition.setType("llm");
            definition.setRole("test-agent");
            definition.setPromptTemplate("Test template");

            // Test with null parameters
            assertEquals("default", definition.getStringParameter("missing", "default"));
            assertEquals(Integer.valueOf(42), definition.getIntegerParameter("missing", 42));
            assertEquals("fallback", definition.getParameter("missing", "fallback"));

            // Test with empty parameters map would require setting parameters map
            definition.setParameters(java.util.Map.of(
                "stringParam", "value",
                "intParam", 123,
                "numericStringParam", "456"
            ));

            assertEquals("value", definition.getStringParameter("stringParam", "default"));
            assertEquals(Integer.valueOf(123), definition.getIntegerParameter("intParam", 0));
            assertEquals(Integer.valueOf(456), definition.getIntegerParameter("numericStringParam", 0));
            assertEquals("value", definition.getParameter("stringParam", "default"));
        }
    }
}
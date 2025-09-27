package com.skanga.conductor.orchestration;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for LLMPlanMaker functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LLMPlanMakerTest extends ConductorTestBase {

    @Mock
    private LLMProvider mockLLMProvider;

    private LLMPlanMaker planMaker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        planMaker = new LLMPlanMaker(mockLLMProvider);
    }

    @Test
    @Order(1)
    @DisplayName("Should construct with valid LLMProvider")
    void testValidConstruction() {
        assertDoesNotThrow(() -> new LLMPlanMaker(mockLLMProvider));
    }

    @Test
    @Order(2)
    @DisplayName("Should throw IllegalArgumentException for null LLMProvider")
    void testNullLLMProviderConstruction() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LLMPlanMaker(null);
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should throw IllegalArgumentException for null user request")
    void testNullUserRequest() {
        assertThrows(IllegalArgumentException.class, () -> {
            planMaker.plan(null);
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should throw IllegalArgumentException for blank user request")
    void testBlankUserRequest() {
        assertThrows(IllegalArgumentException.class, () -> {
            planMaker.plan("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            planMaker.plan("   ");
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should successfully parse valid JSON array response")
    void testValidJsonResponse() throws Exception {
        String userRequest = "Create a blog post about AI";
        String validJsonResponse = """
            [
              {"name":"outline","description":"Create chapter outline","promptTemplate":"Given summary: {{user_request}} produce an outline..."},
              {"name":"writer","description":"Write chapter","promptTemplate":"Write chapter based on: {{outline}} ..."}
            ]
            """;

        when(mockLLMProvider.generate(any(String.class))).thenReturn(validJsonResponse);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("outline", result[0].taskName);
        assertEquals("Create chapter outline", result[0].taskDescription);
        assertEquals("Given summary: {{user_request}} produce an outline...", result[0].promptTemplate);
        assertEquals("writer", result[1].taskName);
        assertEquals("Write chapter", result[1].taskDescription);
        assertEquals("Write chapter based on: {{outline}} ...", result[1].promptTemplate);

        verify(mockLLMProvider).generate(any(String.class));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle JSON response with extra text around array")
    void testJsonWithExtraText() throws Exception {
        String userRequest = "Write a report";
        String responseWithExtraText = """
            Here's the plan for your request:
            [
              {"name":"research","description":"Research the topic","promptTemplate":"Research about {{user_request}}"},
              {"name":"draft","description":"Write first draft","promptTemplate":"Write draft using: {{research}}"}
            ]

            This plan should work well for your needs.
            """;

        when(mockLLMProvider.generate(any(String.class))).thenReturn(responseWithExtraText);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("research", result[0].taskName);
        assertEquals("draft", result[1].taskName);
    }

    @Test
    @Order(7)
    @DisplayName("Should handle empty JSON array")
    void testEmptyJsonArray() throws Exception {
        String userRequest = "Simple task";
        String emptyJsonResponse = "[]";

        when(mockLLMProvider.generate(any(String.class))).thenReturn(emptyJsonResponse);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    @Order(8)
    @DisplayName("Should throw PlannerException for invalid JSON")
    void testInvalidJsonResponse() throws Exception {
        String userRequest = "Create content";
        String invalidJsonResponse = "This is not valid JSON at all";

        when(mockLLMProvider.generate(any(String.class))).thenReturn(invalidJsonResponse);

        ConductorException.PlannerException exception = assertThrows(
            ConductorException.PlannerException.class,
            () -> planMaker.plan(userRequest)
        );

        assertTrue(exception.getMessage().contains("Failed to parse JSON from planner output"));
        assertTrue(exception.getMessage().contains(invalidJsonResponse));
    }

    @Test
    @Order(9)
    @DisplayName("Should throw PlannerException for malformed JSON array")
    void testMalformedJsonArray() throws Exception {
        String userRequest = "Create presentation";
        String malformedJsonResponse = """
            [
              {"name":"slide1","description":"First slide"},
              {"name":"slide2","description":"Second slide","promptTemplate":"Make slide with {{slide1}}"
            ]
            """; // Missing closing brace

        when(mockLLMProvider.generate(any(String.class))).thenReturn(malformedJsonResponse);

        ConductorException.PlannerException exception = assertThrows(
            ConductorException.PlannerException.class,
            () -> planMaker.plan(userRequest)
        );

        assertTrue(exception.getMessage().contains("Failed to parse JSON from planner output"));
    }

    @Test
    @Order(10)
    @DisplayName("Should throw PlannerException when LLM provider fails")
    void testLLMProviderFailure() throws Exception {
        String userRequest = "Generate plan";

        when(mockLLMProvider.generate(any(String.class)))
            .thenThrow(new ConductorException.LLMProviderException("LLM service unavailable"));

        ConductorException.PlannerException exception = assertThrows(
            ConductorException.PlannerException.class,
            () -> planMaker.plan(userRequest)
        );

        assertTrue(exception.getMessage().contains("LLM planner failed"));
        assertTrue(exception.getCause() instanceof ConductorException.LLMProviderException);
    }

    @Test
    @Order(11)
    @DisplayName("Should handle null LLM response by returning empty array")
    void testNullLLMResponse() throws Exception {
        String userRequest = "Create something";

        when(mockLLMProvider.generate(any(String.class))).thenReturn(null);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(0, result.length); // null input becomes empty array
    }

    @Test
    @Order(12)
    @DisplayName("Should handle single task in JSON array")
    void testSingleTaskArray() throws Exception {
        String userRequest = "Write a summary";
        String singleTaskResponse = """
            [
              {"name":"summarizer","description":"Summarize the content","promptTemplate":"Summarize: {{user_request}}"}
            ]
            """;

        when(mockLLMProvider.generate(any(String.class))).thenReturn(singleTaskResponse);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("summarizer", result[0].taskName);
        assertEquals("Summarize the content", result[0].taskDescription);
        assertEquals("Summarize: {{user_request}}", result[0].promptTemplate);
    }

    @Test
    @Order(13)
    @DisplayName("Should handle complex task definitions")
    void testComplexTaskDefinitions() throws Exception {
        String userRequest = "Create a comprehensive guide";
        String complexResponse = """
            [
              {
                "name":"research_phase",
                "description":"Research multiple sources and compile information",
                "promptTemplate":"Research {{user_request}} using these sources: academic papers, industry reports, expert interviews"
              },
              {
                "name":"structure_creation",
                "description":"Create a logical structure for the guide",
                "promptTemplate":"Based on research: {{research_phase}}, create a structured outline for {{user_request}}"
              },
              {
                "name":"content_writing",
                "description":"Write comprehensive content following the structure",
                "promptTemplate":"Write detailed content for {{user_request}} following this structure: {{structure_creation}}"
              },
              {
                "name":"review_and_polish",
                "description":"Review and polish the final content",
                "promptTemplate":"Review and improve this content: {{content_writing}} ensuring clarity and completeness"
              }
            ]
            """;

        when(mockLLMProvider.generate(any(String.class))).thenReturn(complexResponse);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("research_phase", result[0].taskName);
        assertEquals("structure_creation", result[1].taskName);
        assertEquals("content_writing", result[2].taskName);
        assertEquals("review_and_polish", result[3].taskName);

        // Verify complex template variables
        assertTrue(result[1].promptTemplate.contains("{{research_phase}}"));
        assertTrue(result[2].promptTemplate.contains("{{structure_creation}}"));
        assertTrue(result[3].promptTemplate.contains("{{content_writing}}"));
    }

    @Test
    @Order(14)
    @DisplayName("Should handle JSON with special characters")
    void testJsonWithSpecialCharacters() throws Exception {
        String userRequest = "Create content with special chars";
        String jsonWithSpecialChars = """
            [
              {
                "name":"special_task",
                "description":"Task with special chars: àáâãäåæçèéêë",
                "promptTemplate":"Process {{user_request}} with unicode: 你好世界 🌍"
              }
            ]
            """;

        when(mockLLMProvider.generate(any(String.class))).thenReturn(jsonWithSpecialChars);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("special_task", result[0].taskName);
        assertTrue(result[0].taskDescription.contains("àáâãäåæçèéêë"));
        assertTrue(result[0].promptTemplate.contains("你好世界 🌍"));
    }

    @Test
    @Order(15)
    @DisplayName("Should verify prompt template structure")
    void testPromptTemplateStructure() throws Exception {
        String userRequest = "Test request for template verification";

        // Capture the actual prompt sent to LLM
        when(mockLLMProvider.generate(any(String.class))).thenAnswer(invocation -> {
            String actualPrompt = invocation.getArgument(0);

            // Verify prompt contains key elements
            assertTrue(actualPrompt.contains("You are a planner"));
            assertTrue(actualPrompt.contains("Decompose the user's request"));
            assertTrue(actualPrompt.contains("JSON array"));
            assertTrue(actualPrompt.contains("name"));
            assertTrue(actualPrompt.contains("description"));
            assertTrue(actualPrompt.contains("promptTemplate"));
            assertTrue(actualPrompt.contains(userRequest));
            assertTrue(actualPrompt.contains("IMPORTANT: Output only the JSON array"));

            // Return valid response
            return """
                [
                  {"name":"test","description":"Test task","promptTemplate":"Test template"}
                ]
                """;
        });

        planMaker.plan(userRequest);

        verify(mockLLMProvider).generate(any(String.class));
    }

    @Test
    @Order(16)
    @DisplayName("Should handle edge case with no JSON brackets")
    void testNoJsonBrackets() throws Exception {
        String userRequest = "Simple request";
        String responseWithoutBrackets = "No JSON array here, just plain text response";

        when(mockLLMProvider.generate(any(String.class))).thenReturn(responseWithoutBrackets);

        ConductorException.PlannerException exception = assertThrows(
            ConductorException.PlannerException.class,
            () -> planMaker.plan(userRequest)
        );

        assertTrue(exception.getMessage().contains("Failed to parse JSON from planner output"));
    }

    @Test
    @Order(17)
    @DisplayName("Should handle JSON with nested arrays or objects")
    void testNestedJsonStructures() throws Exception {
        String userRequest = "Complex nested task";
        String nestedJsonResponse = """
            [
              {
                "name":"complex_task",
                "description":"Task with nested data",
                "promptTemplate":"Process {{user_request}} with config settings"
              }
            ]
            """;

        when(mockLLMProvider.generate(any(String.class))).thenReturn(nestedJsonResponse);

        TaskDefinition[] result = planMaker.plan(userRequest);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("complex_task", result[0].taskName);
        assertTrue(result[0].promptTemplate.contains("config settings"));
    }
}
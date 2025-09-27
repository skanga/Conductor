package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoMockLLMProviderTest {

    private DemoMockLLMProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DemoMockLLMProvider("demo-test");
    }

    @Test
    void shouldCreateProviderWithName() {
        // Then
        assertTrue(provider.getProviderName().contains("demo-test"));
        assertEquals("mock-model", provider.getModelName());
    }

    @Test
    void shouldCreateProviderWithNullName() {
        // When
        DemoMockLLMProvider nullProvider = new DemoMockLLMProvider(null);

        // Then
        assertNotNull(nullProvider.getProviderName());
        assertTrue(nullProvider.getProviderName().startsWith("llm-provider-"));
    }

    @Test
    void shouldHandleNullPrompt() throws ConductorException {
        // When
        String result = provider.generate(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Please provide a valid prompt"));
    }

    @Test
    void shouldHandleEmptyPrompt() throws ConductorException {
        // When
        String result = provider.generate("");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Please provide a valid prompt"));
    }

    @Test
    void shouldHandleWhitespaceOnlyPrompt() throws ConductorException {
        // When
        String result = provider.generate("   \n\t   ");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Please provide a valid prompt"));
    }

    @Test
    void shouldGenerateBookResponse() throws ConductorException {
        // Given
        String bookPrompt = "Write a book chapter about artificial intelligence";

        // When
        String result = provider.generate(bookPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Chapter") || result.contains("chapter"));
        assertTrue(result.contains("artificial intelligence") || result.contains("AI"));
    }

    @Test
    void shouldGenerateCodeResponse() throws ConductorException {
        // Given
        String codePrompt = "Write a Python function to sort an array";

        // When
        String result = provider.generate(codePrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("def ") || result.contains("function"));
        assertTrue(result.contains("Python") || result.contains("sort"));
    }

    @Test
    void shouldGenerateTechnicalResponse() throws ConductorException {
        // Given
        String techPrompt = "Explain how microservices architecture works";

        // When
        String result = provider.generate(techPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("microservices") || result.contains("architecture"));
        assertTrue(result.contains("service") || result.contains("distributed"));
    }

    @Test
    void shouldGenerateBusinessResponse() throws ConductorException {
        // Given
        String businessPrompt = "Create a business plan for a startup";

        // When
        String result = provider.generate(businessPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("business") || result.contains("plan") || result.contains("startup"));
        assertTrue(result.contains("Executive Summary") || result.contains("Market"));
    }

    @Test
    void shouldGenerateEducationResponse() throws ConductorException {
        // Given
        String educationPrompt = "Explain quantum physics to a student";

        // When
        String result = provider.generate(educationPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("quantum") || result.contains("physics"));
        assertTrue(result.contains("student") || result.contains("learn"));
    }

    @Test
    void shouldGenerateCreativeResponse() throws ConductorException {
        // Given
        String creativePrompt = "Write a creative story about time travel";

        // When
        String result = provider.generate(creativePrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("time") || result.contains("travel") || result.contains("story"));
        assertTrue(result.contains("Once upon") || result.contains("character") || result.contains("adventure"));
    }

    @Test
    void shouldGenerateAnalyticsResponse() throws ConductorException {
        // Given
        String analyticsPrompt = "Analyze the sales data and provide insights";

        // When
        String result = provider.generate(analyticsPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("analysis") || result.contains("data") || result.contains("sales"));
        assertTrue(result.contains("trend") || result.contains("insight") || result.contains("metric"));
    }

    @Test
    void shouldGenerateGenericResponse() throws ConductorException {
        // Given
        String genericPrompt = "Tell me something interesting";

        // When
        String result = provider.generate(genericPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("interesting") || result.contains("fact") || result.contains("Mock"));
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void shouldHandleCaseInsensitiveKeywords() throws ConductorException {
        // Given
        String upperCasePrompt = "WRITE CODE IN JAVA";

        // When
        String result = provider.generate(upperCasePrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Java") || result.contains("code") || result.contains("class"));
    }

    @Test
    void shouldHandleMultipleKeywords() throws ConductorException {
        // Given
        String multiKeywordPrompt = "Write code for a business application using Python";

        // When
        String result = provider.generate(multiKeywordPrompt);

        // Then
        assertNotNull(result);
        // Should respond to code prompt since it's first match
        assertTrue(result.contains("def ") || result.contains("function") || result.contains("Python"));
    }

    @Test
    void shouldHandleVeryLongPrompt() throws ConductorException {
        // Given
        StringBuilder longPrompt = new StringBuilder("Write a book about ");
        for (int i = 0; i < 100; i++) {
            longPrompt.append("artificial intelligence and machine learning ");
        }

        // When
        String result = provider.generate(longPrompt.toString());

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Chapter") || result.contains("book"));
    }

    @Test
    void shouldHandleSpecialCharacters() throws ConductorException {
        // Given
        String specialPrompt = "Write code with symbols: @#$%^&*() and unicode: 你好世界 🚀";

        // When
        String result = provider.generate(specialPrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("code") || result.contains("function"));
    }

    @Test
    void shouldHandleMultilinePrompt() throws ConductorException {
        // Given
        String multilinePrompt = "Write a book chapter about:\n- AI fundamentals\n- Machine learning\n- Deep learning";

        // When
        String result = provider.generate(multilinePrompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Chapter") || result.contains("book"));
    }

    @Test
    void shouldReturnConsistentResponseForSamePrompt() throws ConductorException {
        // Given
        String prompt = "Write a technical document about microservices";

        // When
        String result1 = provider.generate(prompt);
        String result2 = provider.generate(prompt);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2); // Mock provider should be deterministic
    }

    @Test
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    results[index] = provider.generate("Write code for thread " + index);
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception");
            assertNotNull(results[i]);
            assertTrue(results[i].contains("code") || results[i].contains("function"));
        }
    }

    @Test
    void shouldProvideProviderInformation() {
        // When
        String providerName = provider.getProviderName();
        String modelName = provider.getModelName();

        // Then
        assertNotNull(providerName);
        assertEquals("mock-model", modelName);
        assertTrue(providerName.contains("demo-test"));
    }
}
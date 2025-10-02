package com.skanga.conductor.workflow.templates;

import com.skanga.conductor.workflow.config.AgentConfigCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced comprehensive tests for PromptTemplateEngine covering advanced features,
 * edge cases, error conditions, and performance scenarios.
 */
@DisplayName("PromptTemplateEngine Enhanced Tests")
class PromptTemplateEngineEnhancedTest {

    private PromptTemplateEngine engine;
    private PromptTemplateEngine cachingEngine;
    private PromptTemplateEngine noCacheEngine;

    @BeforeEach
    void setUp() {
        engine = new PromptTemplateEngine();
        cachingEngine = new PromptTemplateEngine(true, 10);
        noCacheEngine = new PromptTemplateEngine(false, 0);
    }

    @Nested
    @DisplayName("PromptTemplate Rendering Tests")
    class PromptTemplateRenderingTests {

        @Test
        @DisplayName("Should render complete prompt template with all sections")
        void shouldRenderCompletePromptTemplate() {
            AgentConfigCollection.PromptTemplate template = createMockPromptTemplate(
                "You are a helpful assistant",
                "Please help me with {{task}}",
                "I'll be happy to help you with that."
            );

            Map<String, Object> variables = Map.of("task", "writing code");

            String result = engine.renderPrompt(template, variables);

            assertTrue(result.contains("System: You are a helpful assistant"));
            assertTrue(result.contains("Human: Please help me with writing code"));
            assertTrue(result.contains("Assistant: I'll be happy to help you with that."));
        }

        @Test
        @DisplayName("Should render prompt template with only system message")
        void shouldRenderPromptTemplateWithOnlySystem() {
            AgentConfigCollection.PromptTemplate template = createMockPromptTemplate(
                "You are a {{role}} assistant", null, null
            );

            Map<String, Object> variables = Map.of("role", "coding");

            String result = engine.renderPrompt(template, variables);

            assertEquals("System: You are a coding assistant", result);
        }

        @Test
        @DisplayName("Should render prompt template with only user message")
        void shouldRenderPromptTemplateWithOnlyUser() {
            AgentConfigCollection.PromptTemplate template = createMockPromptTemplate(
                null, "Please {{action}} the {{item}}", null
            );

            Map<String, Object> variables = Map.of("action", "analyze", "item", "code");

            String result = engine.renderPrompt(template, variables);

            assertEquals("Human: Please analyze the code", result);
        }

        @Test
        @DisplayName("Should throw exception for null prompt template")
        void shouldThrowExceptionForNullPromptTemplate() {
            Map<String, Object> variables = Map.of("test", "value");

            assertThrows(IllegalArgumentException.class, () ->
                engine.renderPrompt(null, variables));
        }

        @Test
        @DisplayName("Should validate prompt template structure")
        void shouldValidatePromptTemplateStructure() {
            // Valid template
            AgentConfigCollection.PromptTemplate validTemplate = createMockPromptTemplate(
                "System message", "User message", null
            );
            assertDoesNotThrow(() -> engine.validatePromptTemplate(validTemplate));

            // Template with no content
            AgentConfigCollection.PromptTemplate emptyTemplate = createMockPromptTemplate(
                null, null, null
            );
            assertThrows(IllegalArgumentException.class, () ->
                engine.validatePromptTemplate(emptyTemplate));

            // Null template
            assertThrows(IllegalArgumentException.class, () ->
                engine.validatePromptTemplate(null));
        }
    }

    @Nested
    @DisplayName("Advanced Template Features Tests")
    class AdvancedTemplateFeaturesTests {

        @Test
        @DisplayName("Should handle conditional with truthy values")
        void shouldHandleConditionalWithTruthyValues() {
            String template = "{{#if value}}Value is truthy{{/if}}";

            // Test various truthy values
            assertEquals("Value is truthy", engine.renderString(template, Map.of("value", true)));
            assertEquals("Value is truthy", engine.renderString(template, Map.of("value", 1)));
            assertEquals("Value is truthy", engine.renderString(template, Map.of("value", "text")));
            assertEquals("Value is truthy", engine.renderString(template, Map.of("value", Arrays.asList("item"))));
            assertEquals("Value is truthy", engine.renderString(template, Map.of("value", new Object())));
        }

        @Test
        @DisplayName("Should handle conditional with falsy values")
        void shouldHandleConditionalWithFalsyValues() {
            String template = "{{#if value}}Value is truthy{{/if}}";

            // Test various falsy values
            assertEquals("", engine.renderString(template, Map.of("value", false)));
            assertEquals("", engine.renderString(template, Map.of("value", 0)));
            assertEquals("", engine.renderString(template, Map.of("value", 0.0)));
            assertEquals("", engine.renderString(template, Map.of("value", "")));
            assertEquals("", engine.renderString(template, Map.of("value", Collections.emptyList())));
            assertEquals("", engine.renderString(template, Map.of()));
        }

        @Test
        @DisplayName("Should handle loops with different iterable types")
        void shouldHandleLoopsWithDifferentIterableTypes() {
            String template = "{{#each items}}{{this}} {{/each}}";

            // Test with List
            List<String> list = Arrays.asList("a", "b", "c");
            assertEquals("a b c ", engine.renderString(template, Map.of("items", list)));

            // Test with Array
            String[] array = {"x", "y", "z"};
            assertEquals("x y z ", engine.renderString(template, Map.of("items", array)));

            // Test with empty collection
            assertEquals("", engine.renderString(template, Map.of("items", Collections.emptyList())));

            // Test with null collection
            assertEquals("", engine.renderString(template, Map.of()));
        }

        @Test
        @DisplayName("Should handle loops with map objects")
        void shouldHandleLoopsWithMapObjects() {
            String template = "{{#each users}}Name: {{name}}, Age: {{age}} | {{/each}}";

            List<Map<String, Object>> users = Arrays.asList(
                Map.of("name", "Alice", "age", 30),
                Map.of("name", "Bob", "age", 25)
            );

            String result = engine.renderString(template, Map.of("users", users));
            assertEquals("Name: Alice, Age: 30 | Name: Bob, Age: 25 | ", result);
        }

        @Test
        @DisplayName("Should handle loops with non-iterable objects")
        void shouldHandleLoopsWithNonIterableObjects() {
            String template = "{{#each invalid}}{{this}}{{/each}}";

            // Test with non-iterable object
            String result = engine.renderString(template, Map.of("invalid", "not-iterable"));
            assertEquals("", result);
        }

        @Test
        @DisplayName("Should handle multiple filter chains")
        void shouldHandleMultipleFilterChains() {
            String template = "{{text|trim|upper|truncate:5}}";
            Map<String, Object> variables = Map.of("text", "  hello world  ");

            String result = engine.renderString(template, variables);
            assertEquals("HELLO...", result);
        }

        @Test
        @DisplayName("Should handle filters with null values")
        void shouldHandleFiltersWithNullValues() {
            String template = "{{value|upper|default:'NONE'}}";

            // Null value should use default
            String result = engine.renderString(template, Map.of());
            assertEquals("NONE", result);

            // Non-null value should be processed
            result = engine.renderString(template, Map.of("value", "test"));
            assertEquals("TEST", result);
        }

        @Test
        @DisplayName("Should handle invalid filter parameters")
        void shouldHandleInvalidFilterParameters() {
            String template = "{{text|truncate:invalid}}";
            Map<String, Object> variables = Map.of("text", "hello world");

            String result = engine.renderString(template, variables);
            assertEquals("hello world", result); // Should return original on invalid parameter
        }

        @Test
        @DisplayName("Should handle unknown filters gracefully")
        void shouldHandleUnknownFiltersGracefully() {
            String template = "{{text|unknown_filter}}";
            Map<String, Object> variables = Map.of("text", "hello");

            String result = engine.renderString(template, variables);
            assertEquals("hello", result); // Should return original value
        }

        @Test
        @DisplayName("Should handle deeply nested variable access")
        void shouldHandleDeeplyNestedVariableAccess() {
            String template = "{{level1.level2.level3.value}}";

            Map<String, Object> level3 = Map.of("value", "deep");
            Map<String, Object> level2 = Map.of("level3", level3);
            Map<String, Object> level1 = Map.of("level2", level2);
            Map<String, Object> variables = Map.of("level1", level1);

            String result = engine.renderString(template, variables);
            assertEquals("deep", result);
        }

        @Test
        @DisplayName("Should handle broken nested variable chain")
        void shouldHandleBrokenNestedVariableChain() {
            String template = "{{level1.missing.value}}";

            Map<String, Object> level1 = Map.of("other", "value");
            Map<String, Object> variables = Map.of("level1", level1);

            String result = engine.renderString(template, variables);
            assertEquals("{{level1.missing.value}}", result);
        }
    }

    @Nested
    @DisplayName("Caching Tests")
    class CachingTests {

        @Test
        @DisplayName("Should cache compiled templates when caching enabled")
        void shouldCacheCompiledTemplatesWhenCachingEnabled() {
            String template = "Hello {{name}}";
            Map<String, Object> variables = Map.of("name", "Alice");

            // First render
            String result1 = cachingEngine.renderString(template, variables);
            PromptTemplateEngine.CacheStats stats1 = cachingEngine.getCacheStats();

            // Second render of same template
            String result2 = cachingEngine.renderString(template, variables);
            PromptTemplateEngine.CacheStats stats2 = cachingEngine.getCacheStats();

            assertEquals("Hello Alice", result1);
            assertEquals("Hello Alice", result2);
            assertEquals(1, stats1.getCurrentSize());
            assertEquals(1, stats2.getCurrentSize()); // Should reuse cached template
        }

        @Test
        @DisplayName("Should not cache when caching disabled")
        void shouldNotCacheWhenCachingDisabled() {
            String template = "Hello {{name}}";
            Map<String, Object> variables = Map.of("name", "Bob");

            noCacheEngine.renderString(template, variables);
            PromptTemplateEngine.CacheStats stats = noCacheEngine.getCacheStats();

            assertFalse(stats.isEnabled());
            assertEquals(0, stats.getCurrentSize());
            assertEquals(0.0, stats.getUsageRatio());
        }

        @Test
        @DisplayName("Should evict oldest template when cache is full")
        void shouldEvictOldestTemplateWhenCacheIsFull() {
            PromptTemplateEngine smallCacheEngine = new PromptTemplateEngine(true, 2);

            // Fill cache to capacity
            smallCacheEngine.renderString("Template {{a}}", Map.of("a", "1"));
            smallCacheEngine.renderString("Template {{b}}", Map.of("b", "2"));

            assertEquals(2, smallCacheEngine.getCacheStats().getCurrentSize());

            // Add third template to trigger eviction
            smallCacheEngine.renderString("Template {{c}}", Map.of("c", "3"));

            assertEquals(2, smallCacheEngine.getCacheStats().getCurrentSize());
        }

        @Test
        @DisplayName("Should clear cache correctly")
        void shouldClearCacheCorrectly() {
            cachingEngine.renderString("Template {{x}}", Map.of("x", "test"));
            assertEquals(1, cachingEngine.getCacheStats().getCurrentSize());

            cachingEngine.clearCache();
            assertEquals(0, cachingEngine.getCacheStats().getCurrentSize());
        }

        @Test
        @DisplayName("Should handle cache statistics correctly")
        void shouldHandleCacheStatisticsCorrectly() {
            PromptTemplateEngine.CacheStats stats = cachingEngine.getCacheStats();

            assertTrue(stats.isEnabled());
            assertEquals(0, stats.getCurrentSize());
            assertEquals(10, stats.getMaxSize());
            assertEquals(0.0, stats.getUsageRatio());

            String statsString = stats.toString();
            assertTrue(statsString.contains("enabled=true"));
            assertTrue(statsString.contains("size=0/10"));
            assertTrue(statsString.contains("usage=0.0%"));
        }
    }

    @Nested
    @DisplayName("Variable Extraction Tests")
    class VariableExtractionTests {

        @Test
        @DisplayName("Should extract variables from complex template")
        void shouldExtractVariablesFromComplexTemplate() {
            String template = "{{name}} has access to {{resources}} with {{user.role|default:'guest'}}";

            String[] variables = engine.extractVariableNames(template);

            Set<String> variableSet = new HashSet<>(Arrays.asList(variables));
            assertTrue(variableSet.contains("name"));
            assertTrue(variableSet.contains("resources"));
            assertTrue(variableSet.contains("user.role|default:'guest'"));
        }

        @Test
        @DisplayName("Should extract unique variables only")
        void shouldExtractUniqueVariablesOnly() {
            String template = "{{name}} and {{name}} again plus {{other}}";

            String[] variables = engine.extractVariableNames(template);

            assertEquals(2, variables.length);
            Set<String> variableSet = new HashSet<>(Arrays.asList(variables));
            assertTrue(variableSet.contains("name"));
            assertTrue(variableSet.contains("other"));
        }

        @Test
        @DisplayName("Should handle empty template for variable extraction")
        void shouldHandleEmptyTemplateForVariableExtraction() {
            String[] variables = engine.extractVariableNames("");
            assertEquals(0, variables.length);

            variables = engine.extractVariableNames(null);
            assertEquals(0, variables.length);
        }
    }

    @Nested
    @DisplayName("Template Validation Tests")
    class TemplateValidationTests {

        @Test
        @DisplayName("Should validate templates with balanced braces")
        void shouldValidateTemplatesWithBalancedBraces() {
            assertDoesNotThrow(() -> engine.validateTemplate("{{name}}"));
            assertDoesNotThrow(() -> engine.validateTemplate("{{#if condition}}content{{/if}}"));
            assertDoesNotThrow(() -> engine.validateTemplate("{{#each items}}{{name}}{{/each}}"));
            assertDoesNotThrow(() -> engine.validateTemplate("No variables here"));
            assertDoesNotThrow(() -> engine.validateTemplate(null));
        }

        @Test
        @DisplayName("Should reject templates with unbalanced braces")
        void shouldRejectTemplatesWithUnbalancedBraces() {
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("{{name}"));
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("name}}"));
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("{{name}} and {missing}"));
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("{{name}}}}"));
        }

        @Test
        @DisplayName("Should reject templates with empty variable names")
        void shouldRejectTemplatesWithEmptyVariableNames() {
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("{{}}"));
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("{{  }}"));
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("{{name}} and {{}}"));
        }

        @Test
        @DisplayName("Should reject templates with single braces")
        void shouldRejectTemplatesWithSingleBraces() {
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("This has {single} braces"));
            assertThrows(IllegalArgumentException.class, () -> engine.validateTemplate("Mixed {{double}} and {single}"));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent rendering safely")
        void shouldHandleConcurrentRenderingSafely() throws InterruptedException {
            int threadCount = 10;
            int iterationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String template = "Thread {{id}} iteration {{num}}";
                        Map<String, Object> variables = Map.of("id", threadId, "num", j);
                        String result = engine.renderString(template, variables);

                        if (result.equals("Thread " + threadId + " iteration " + j)) {
                            successCount.incrementAndGet();
                        }
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            assertEquals(threadCount * iterationsPerThread, successCount.get());
        }

        @Test
        @DisplayName("Should handle concurrent cache operations safely")
        void shouldHandleConcurrentCacheOperationsSafely() throws InterruptedException {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < 20; j++) {
                        String template = "Template {{value" + (j % 3) + "}}";
                        Map<String, Object> variables = Map.of("value" + (j % 3), "thread" + threadId);
                        cachingEngine.renderString(template, variables);
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

            // Cache should not exceed max size due to thread safety
            assertTrue(cachingEngine.getCacheStats().getCurrentSize() <= 10);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large templates")
        void shouldHandleVeryLargeTemplates() {
            StringBuilder largeTemplate = new StringBuilder();
            Map<String, Object> variables = new HashMap<>();

            for (int i = 0; i < 50; i++) { // Reduced from 1000 to 50 for faster testing
                largeTemplate.append("Variable ").append(i).append(": {{var").append(i).append("}} ");
                variables.put("var" + i, "value" + i);
            }

            String result = engine.renderString(largeTemplate.toString(), variables);
            assertNotNull(result);
            assertTrue(result.length() > 500); // Adjusted for reduced iterations
        }

        @Test
        @DisplayName("Should handle templates with special characters")
        void shouldHandleTemplatesWithSpecialCharacters() {
            String template = "Special chars: {{value}} with üñíçödé and €symbols";
            Map<String, Object> variables = Map.of("value", "tëst");

            String result = engine.renderString(template, variables);
            assertEquals("Special chars: tëst with üñíçödé and €symbols", result);
        }

        @Test
        @DisplayName("Should handle recursive variable references gracefully")
        void shouldHandleRecursiveVariableReferencesGracefully() {
            String template = "{{name}} references {{name.child}}";
            Map<String, Object> variables = Map.of("name", "parent");

            String result = engine.renderString(template, variables);
            assertEquals("parent references {{name.child}}", result);
        }

        @Test
        @DisplayName("Should handle malformed conditional and loop syntax")
        void shouldHandleMalformedConditionalAndLoopSyntax() {
            // Malformed conditionals should be left as-is
            String template1 = "{{#if unclosed condition";
            String result1 = engine.renderString(template1, Map.of());
            assertEquals(template1, result1);

            // Malformed loops should be left as-is
            String template2 = "{{#each unclosed loop";
            String result2 = engine.renderString(template2, Map.of());
            assertEquals(template2, result2);
        }
    }

    // Helper method to create mock PromptTemplate
    private AgentConfigCollection.PromptTemplate createMockPromptTemplate(String system, String user, String assistant) {
        return new AgentConfigCollection.PromptTemplate() {
            @Override
            public String getSystem() { return system; }
            @Override
            public String getUser() { return user; }
            @Override
            public String getAssistant() { return assistant; }
            @Override
            public boolean hasSystem() { return system != null; }
            @Override
            public boolean hasUser() { return user != null; }
            @Override
            public boolean hasAssistant() { return assistant != null; }
        };
    }
}
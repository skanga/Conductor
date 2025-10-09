package com.skanga.conductor.workflow.templates;
import com.skanga.conductor.templates.PromptTemplateEngine;
import com.skanga.conductor.templates.TemplateException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the enhanced PromptTemplateEngine.
 */
class PromptTemplateEngineTest {

    private PromptTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PromptTemplateEngine();
    }

    @Test
    void testBasicVariableSubstitution() {
        String template = "Hello {{name}}, welcome to {{place}}!";
        Map<String, Object> variables = Map.of(
            "name", "Alice",
            "place", "Wonderland"
        );

        String result = engine.renderString(template, variables);
        assertEquals("Hello Alice, welcome to Wonderland!", result);
    }

    @Test
    void testMissingVariables() {
        String template = "Hello {{name}}, your score is {{score}}";
        Map<String, Object> variables = Map.of("name", "Bob");

        String result = engine.renderString(template, variables);
        assertEquals("Hello Bob, your score is {{score}}", result);
    }

    @Test
    void testConditionalLogic() {
        String template = "{{#if hasPermission}}You can access this resource{{/if}}";

        // Test true condition
        Map<String, Object> variables = Map.of("hasPermission", true);
        String result = engine.renderString(template, variables);
        assertEquals("You can access this resource", result);

        // Test false condition
        variables = Map.of("hasPermission", false);
        result = engine.renderString(template, variables);
        assertEquals("", result);

        // Test missing condition (falsy)
        variables = Map.of();
        result = engine.renderString(template, variables);
        assertEquals("", result);
    }

    @Test
    void testLoops() {
        String template = "Items: {{#each items}}{{name}} {{/each}}";
        List<Map<String, Object>> items = Arrays.asList(
            Map.of("name", "Apple"),
            Map.of("name", "Banana"),
            Map.of("name", "Cherry")
        );
        Map<String, Object> variables = Map.of("items", items);

        String result = engine.renderString(template, variables);
        assertEquals("Items: Apple Banana Cherry ", result);
    }

    @Test
    void testFilters() {
        String template = "{{message|upper}} and {{message|lower}}";
        Map<String, Object> variables = Map.of("message", "Hello World");

        String result = engine.renderString(template, variables);
        assertEquals("HELLO WORLD and hello world", result);
    }

    @Test
    void testDefaultFilter() {
        String template = "Name: {{name|default:'Anonymous'}}";

        // Test with value
        Map<String, Object> variables = Map.of("name", "Alice");
        String result = engine.renderString(template, variables);
        assertEquals("Name: Alice", result);

        // Test without value
        variables = Map.of();
        result = engine.renderString(template, variables);
        assertEquals("Name: Anonymous", result);
    }

    @Test
    void testTruncateFilter() {
        String template = "{{text|truncate:10}}";
        Map<String, Object> variables = Map.of("text", "This is a very long message");

        String result = engine.renderString(template, variables);
        assertEquals("This is a ...", result);
    }

    @Test
    void testNestedVariables() {
        String template = "Hello {{user.name}}, your email is {{user.email}}";
        Map<String, Object> user = Map.of(
            "name", "Alice",
            "email", "alice@example.com"
        );
        Map<String, Object> variables = Map.of("user", user);

        String result = engine.renderString(template, variables);
        assertEquals("Hello Alice, your email is alice@example.com", result);
    }

    @Test
    void testComplexTemplate() {
        String template = """
            Hello {{user.name}}!
            {{#if user.isAdmin}}You have admin privileges.{{/if}}
            Your recent items:
            {{#each items}}
            - {{name|upper}} ({{price|default:'Free'}})
            {{/each}}
            """;

        Map<String, Object> user = Map.of(
            "name", "Alice",
            "isAdmin", true
        );
        List<Map<String, Object>> items = Arrays.asList(
            Map.of("name", "book", "price", "$15"),
            Map.of("name", "pen")
        );
        Map<String, Object> variables = Map.of(
            "user", user,
            "items", items
        );

        String result = engine.renderString(template, variables);
        assertTrue(result.contains("Hello Alice!"));
        assertTrue(result.contains("You have admin privileges."));
        assertTrue(result.contains("BOOK ($15)"));
        assertTrue(result.contains("PEN (Free)"));
    }

    @Test
    void testTemplateValidation() {
        // Valid template
        assertDoesNotThrow(() -> engine.validateTemplate("{{name}}"));

        // Invalid templates - now throw TemplateException
        assertThrows(TemplateException.class, () ->
            engine.validateTemplate("{{name}"));
        assertThrows(TemplateException.class, () ->
            engine.validateTemplate("name}}"));
        assertThrows(TemplateException.class, () ->
            engine.validateTemplate("{{}}"));
    }

    @Test
    void testExtractVariableNames() {
        String template = "Hello {{name}}, your score is {{score}} and status is {{user.status}}";
        String[] variables = engine.extractVariableNames(template);

        assertEquals(3, variables.length);
        assertTrue(Arrays.asList(variables).contains("name"));
        assertTrue(Arrays.asList(variables).contains("score"));
        assertTrue(Arrays.asList(variables).contains("user.status"));
    }

    @Test
    void testCacheStatistics() {
        PromptTemplateEngine cachedEngine = new PromptTemplateEngine(true, 2);

        PromptTemplateEngine.CacheStats initialStats = cachedEngine.getCacheStats();
        assertTrue(initialStats.isEnabled());
        assertEquals(0, initialStats.getCurrentSize());
        assertEquals(2, initialStats.getMaxSize());

        // Render some templates to populate cache
        cachedEngine.renderString("{{name}}", Map.of("name", "test1"));
        cachedEngine.renderString("{{value}}", Map.of("value", "test2"));

        PromptTemplateEngine.CacheStats afterStats = cachedEngine.getCacheStats();
        assertEquals(2, afterStats.getCurrentSize());
        assertEquals(1.0, afterStats.getUsageRatio(), 0.01);
    }

    @Test
    void testCacheEviction() {
        PromptTemplateEngine cachedEngine = new PromptTemplateEngine(true, 2);

        // Fill cache to capacity
        cachedEngine.renderString("{{name1}}", Map.of("name1", "test1"));
        cachedEngine.renderString("{{name2}}", Map.of("name2", "test2"));

        assertEquals(2, cachedEngine.getCacheStats().getCurrentSize());

        // Add one more to trigger eviction
        cachedEngine.renderString("{{name3}}", Map.of("name3", "test3"));

        assertEquals(2, cachedEngine.getCacheStats().getCurrentSize());
    }

    @Test
    void testNullAndEmptyInputs() {
        assertNull(engine.renderString(null, Map.of()));
        assertEquals("", engine.renderString("", Map.of()));
        assertEquals("test", engine.renderString("test", null));
        assertEquals("test", engine.renderString("test", Collections.emptyMap()));
    }

    @Test
    void testVariousDataTypes() {
        String template = "Number: {{num}}, Boolean: {{bool}}, Object: {{obj}}";
        Map<String, Object> variables = Map.of(
            "num", 42,
            "bool", true,
            "obj", Arrays.asList("a", "b", "c")
        );

        String result = engine.renderString(template, variables);
        assertEquals("Number: 42, Boolean: true, Object: [a, b, c]", result);
    }
}
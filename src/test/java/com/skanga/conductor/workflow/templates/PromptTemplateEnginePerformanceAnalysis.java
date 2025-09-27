package com.skanga.conductor.workflow.templates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.*;

/**
 * Performance analysis for PromptTemplateEngine.
 * This test is disabled by default since it's for performance investigation.
 */
@Disabled("Performance analysis - run manually")
class PromptTemplateEnginePerformanceAnalysis {

    @Test
    void performanceAnalysis() {
        System.out.println("=== PromptTemplateEngine Performance Analysis ===\n");

        // Test configurations
        int warmupIterations = 1000;
        int benchmarkIterations = 10000;

        // Create engines with different cache configurations
        PromptTemplateEngine cachedEngine = new PromptTemplateEngine(true, 100);
        PromptTemplateEngine noCacheEngine = new PromptTemplateEngine(false, 0);

        // Test templates of varying complexity
        String simpleTemplate = "Hello {{name}}!";
        String complexTemplate = """
            Hello {{user.name}}!
            {{#if user.isAdmin}}You have admin privileges.{{/if}}
            Your recent items:
            {{#each items}}
            - {{name|upper}} ({{price|default:'Free'}})
            {{/each}}
            Total: {{total|default:'0'}}
            """;

        // Test data
        Map<String, Object> simpleVars = Map.of("name", "Alice");
        Map<String, Object> complexVars = createComplexVariables();

        System.out.println("Warmup phase...");
        // Warmup
        for (int i = 0; i < warmupIterations; i++) {
            cachedEngine.renderString(simpleTemplate, simpleVars);
            cachedEngine.renderString(complexTemplate, complexVars);
            noCacheEngine.renderString(simpleTemplate, simpleVars);
            noCacheEngine.renderString(complexTemplate, complexVars);
        }

        System.out.println("Running performance tests...\n");

        // Test 1: Simple template with cache
        long start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            cachedEngine.renderString(simpleTemplate, simpleVars);
        }
        long cachedSimpleTime = System.nanoTime() - start;

        // Test 2: Simple template without cache
        start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            noCacheEngine.renderString(simpleTemplate, simpleVars);
        }
        long noCacheSimpleTime = System.nanoTime() - start;

        // Test 3: Complex template with cache
        start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            cachedEngine.renderString(complexTemplate, complexVars);
        }
        long cachedComplexTime = System.nanoTime() - start;

        // Test 4: Complex template without cache
        start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            noCacheEngine.renderString(complexTemplate, complexVars);
        }
        long noCacheComplexTime = System.nanoTime() - start;

        // Test 5: Multiple different templates (cache effectiveness)
        List<String> templates = Arrays.asList(
            "Template 1: {{var1}}",
            "Template 2: {{var2|upper}}",
            "Template 3: {{#if condition}}{{message}}{{/if}}",
            "Template 4: {{#each items}}{{name}} {{/each}}",
            "Template 5: {{nested.value|default:'none'}}"
        );

        start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            String template = templates.get(i % templates.size());
            cachedEngine.renderString(template, complexVars);
        }
        long multiTemplateCachedTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            String template = templates.get(i % templates.size());
            noCacheEngine.renderString(template, complexVars);
        }
        long multiTemplateNoCacheTime = System.nanoTime() - start;

        // Results
        printResults("Simple Template (Cached)", cachedSimpleTime, benchmarkIterations);
        printResults("Simple Template (No Cache)", noCacheSimpleTime, benchmarkIterations);
        printResults("Complex Template (Cached)", cachedComplexTime, benchmarkIterations);
        printResults("Complex Template (No Cache)", noCacheComplexTime, benchmarkIterations);
        printResults("Multi-Template (Cached)", multiTemplateCachedTime, benchmarkIterations);
        printResults("Multi-Template (No Cache)", multiTemplateNoCacheTime, benchmarkIterations);

        System.out.println("\n=== Performance Gains ===");
        double simpleSpeedup = (double) noCacheSimpleTime / cachedSimpleTime;
        double complexSpeedup = (double) noCacheComplexTime / cachedComplexTime;
        double multiSpeedup = (double) multiTemplateNoCacheTime / multiTemplateCachedTime;

        System.out.printf("Simple Template Speedup: %.2fx\n", simpleSpeedup);
        System.out.printf("Complex Template Speedup: %.2fx\n", complexSpeedup);
        System.out.printf("Multi-Template Speedup: %.2fx\n", multiSpeedup);

        System.out.println("\n=== Cache Statistics ===");
        System.out.println("Cached Engine: " + cachedEngine.getCacheStats());
        System.out.println("No-Cache Engine: " + noCacheEngine.getCacheStats());

        // Memory usage test
        System.out.println("\n=== Memory Usage Test ===");
        testMemoryUsage();

        // Identify bottlenecks
        analyzeBottlenecks();
    }

    private Map<String, Object> createComplexVariables() {
        Map<String, Object> user = Map.of(
            "name", "Alice",
            "isAdmin", true
        );

        List<Map<String, Object>> items = Arrays.asList(
            Map.of("name", "book", "price", "$15"),
            Map.of("name", "pen", "price", "$2"),
            Map.of("name", "notebook")
        );

        Map<String, Object> nested = Map.of("value", "nested_data");

        return Map.of(
            "user", user,
            "items", items,
            "total", "$17",
            "condition", true,
            "var1", "value1",
            "var2", "value2",
            "message", "Hello World",
            "nested", nested
        );
    }

    private void printResults(String testName, long timeNanos, int iterations) {
        double timeMs = timeNanos / 1_000_000.0;
        double avgMicros = (timeNanos / 1000.0) / iterations;
        double opsPerSec = 1_000_000_000.0 / (timeNanos / (double) iterations);

        System.out.printf("%-25s: %8.2f ms total, %8.2f μs/op, %10.0f ops/sec\n",
                         testName, timeMs, avgMicros, opsPerSec);
    }

    private void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();

        // Force garbage collection to get accurate baseline
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Create engines and render many templates
        PromptTemplateEngine engine = new PromptTemplateEngine(true, 1000);
        Map<String, Object> vars = createComplexVariables();

        // Create many unique templates to fill cache
        for (int i = 0; i < 1000; i++) {
            String template = "Template " + i + ": {{user.name}} has items {{var" + (i % 10) + "}}";
            engine.renderString(template, vars);
        }

        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;

        System.out.printf("Memory used for 1000 cached templates: %d KB\n", memoryUsed / 1024);
        System.out.printf("Average memory per template: %d bytes\n", memoryUsed / 1000);
    }

    private void analyzeBottlenecks() {
        System.out.println("\n=== Bottleneck Analysis ===");

        PromptTemplateEngine engine = new PromptTemplateEngine(true, 100);
        Map<String, Object> vars = createComplexVariables();

        // Test regex compilation overhead
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String template = "Simple {{var" + (i % 100) + "}} template";
            engine.renderString(template, vars);
        }
        long regexTime = System.nanoTime() - start;

        // Test string operations overhead
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String template = "No variables here at all - just plain text";
            engine.renderString(template, vars);
        }
        long plainTextTime = System.nanoTime() - start;

        // Test complex processing
        String complexTemplate = "{{#each items}}{{name|upper|truncate:10}} {{#if price}}({{price}}){{/if}}{{/each}}";
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            engine.renderString(complexTemplate, vars);
        }
        long complexProcessingTime = System.nanoTime() - start;

        System.out.printf("Variable substitution overhead: %.2f μs/op\n", (regexTime - plainTextTime) / 10000.0 / 1000);
        System.out.printf("Plain text processing: %.2f μs/op\n", plainTextTime / 10000.0 / 1000);
        System.out.printf("Complex template processing: %.2f μs/op\n", complexProcessingTime / 1000.0 / 1000);

        // Test individual operations
        testRegexPerformance();
        testStringBuilderVsConcat();
    }

    private void testRegexPerformance() {
        System.out.println("\n=== Regex Performance ===");

        String template = "Hello {{name}}, your score is {{score}} and status is {{user.status}}";
        int iterations = 50000;

        // Pattern compilation cost
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            java.util.regex.Pattern.compile("\\{\\{([^}]*)\\}\\}");
        }
        long compilationTime = System.nanoTime() - start;

        // Using pre-compiled pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]*)\\}\\}");
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            java.util.regex.Matcher matcher = pattern.matcher(template);
            while (matcher.find()) {
                matcher.group(1);
            }
        }
        long matchingTime = System.nanoTime() - start;

        System.out.printf("Pattern compilation: %.2f μs/op\n", compilationTime / iterations / 1000.0);
        System.out.printf("Pattern matching: %.2f μs/op\n", matchingTime / iterations / 1000.0);
    }

    private void testStringBuilderVsConcat() {
        System.out.println("\n=== String Operations Performance ===");

        int iterations = 100000;
        String[] parts = {"Hello ", "{{name}}", ", your score is ", "{{score}}", "!"};

        // StringBuilder approach
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                sb.append(part);
            }
            sb.toString();
        }
        long stringBuilderTime = System.nanoTime() - start;

        // String concatenation
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String result = parts[0] + parts[1] + parts[2] + parts[3] + parts[4];
        }
        long concatTime = System.nanoTime() - start;

        System.out.printf("StringBuilder: %.2f μs/op\n", stringBuilderTime / iterations / 1000.0);
        System.out.printf("String concat: %.2f μs/op\n", concatTime / iterations / 1000.0);
    }
}
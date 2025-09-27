package com.skanga.conductor.workflow.templates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for PromptTemplateEngine to ensure acceptable performance
 * under various load conditions and template complexities.
 */
@DisplayName("PromptTemplateEngine Performance Tests")
@EnabledIfSystemProperty(named = "test.performance", matches = "true")
class PromptTemplateEnginePerformanceTest {

    private PromptTemplateEngine engine;
    private PromptTemplateEngine cachingEngine;
    private PromptTemplateEngine noCacheEngine;

    @BeforeEach
    void setUp() {
        engine = new PromptTemplateEngine();
        cachingEngine = new PromptTemplateEngine(true, 1000);
        noCacheEngine = new PromptTemplateEngine(false, 0);
    }

    @Test
    @DisplayName("Should render simple templates within performance threshold")
    void shouldRenderSimpleTemplatesWithinPerformanceThreshold() {
        String template = "Hello {{name}}, welcome to {{place}}!";
        Map<String, Object> variables = Map.of("name", "User", "place", "System");

        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            engine.renderString(template, variables);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Simple template rendering (10k iterations): " + durationMs + "ms");

        // Should complete 10k simple renderings in under 1 second
        assertTrue(durationMs < 1000, "Simple template rendering took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Should demonstrate caching performance benefits")
    void shouldDemonstrateCachingPerformanceBenefits() {
        String template = "Complex template with {{var1}} and {{var2|upper}} and {{#if condition}}conditional{{/if}}";
        Map<String, Object> variables = Map.of(
            "var1", "value1",
            "var2", "value2",
            "condition", true
        );

        int iterations = 5000;

        // Test with caching
        long startCached = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            cachingEngine.renderString(template, variables);
        }
        long endCached = System.nanoTime();
        long cachedDuration = (endCached - startCached) / 1_000_000;

        // Test without caching
        long startNoCache = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            noCacheEngine.renderString(template, variables);
        }
        long endNoCache = System.nanoTime();
        long noCacheDuration = (endNoCache - startNoCache) / 1_000_000;

        System.out.println("Cached rendering (" + iterations + " iterations): " + cachedDuration + "ms");
        System.out.println("Non-cached rendering (" + iterations + " iterations): " + noCacheDuration + "ms");

        // Caching should provide significant performance improvement for repeated templates
        assertTrue(cachedDuration < noCacheDuration,
            "Caching should be faster than non-cached for repeated templates");
    }

    @Test
    @DisplayName("Should handle large templates efficiently")
    void shouldHandleLargeTemplatesEfficiently() {
        StringBuilder largeTemplate = new StringBuilder();
        Map<String, Object> variables = new HashMap<>();

        // Create a template with 1000 variables
        for (int i = 0; i < 1000; i++) {
            largeTemplate.append("Variable ").append(i).append(": {{var").append(i).append("}} ");
            variables.put("var" + i, "value" + i);
        }

        long startTime = System.nanoTime();
        String result = engine.renderString(largeTemplate.toString(), variables);
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Large template rendering (1000 variables): " + durationMs + "ms");

        assertNotNull(result);
        assertTrue(result.length() > 10000);
        // Should complete large template rendering in under 100ms
        assertTrue(durationMs < 100, "Large template rendering took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Should handle complex nested templates efficiently")
    void shouldHandleComplexNestedTemplatesEfficiently() {
        String template = """
            {{#if user.isActive}}
            Hello {{user.name|upper}}!
            {{#each user.permissions}}
            - Permission: {{name}} ({{level|default:'basic'}})
            {{/each}}
            Recent activities:
            {{#each activities}}
            {{timestamp}}: {{action|trim|upper}} on {{target.name}}
            {{/each}}
            {{/if}}
            """;

        Map<String, Object> user = Map.of(
            "name", "John Doe",
            "isActive", true,
            "permissions", Arrays.asList(
                Map.of("name", "read", "level", "admin"),
                Map.of("name", "write"),
                Map.of("name", "delete", "level", "super")
            )
        );

        List<Map<String, Object>> activities = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            activities.add(Map.of(
                "timestamp", "2024-01-" + (i % 30 + 1),
                "action", "  action" + i + "  ",
                "target", Map.of("name", "resource" + i)
            ));
        }

        Map<String, Object> variables = Map.of(
            "user", user,
            "activities", activities
        );

        long startTime = System.nanoTime();
        String result = engine.renderString(template, variables);
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Complex nested template rendering: " + durationMs + "ms");

        assertNotNull(result);
        assertTrue(result.contains("JOHN DOE"));
        assertTrue(result.contains("ACTION0"));
        // Should complete complex nested template in under 50ms
        assertTrue(durationMs < 50, "Complex template rendering took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Should handle concurrent rendering performance")
    void shouldHandleConcurrentRenderingPerformance() throws InterruptedException {
        String template = "Hello {{name}}, your ID is {{id}} and status is {{status|upper}}";
        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicLong totalRenderTime = new AtomicLong(0);

        long overallStart = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                long threadStart = System.nanoTime();
                for (int i = 0; i < iterationsPerThread; i++) {
                    Map<String, Object> variables = Map.of(
                        "name", "User" + threadId + "_" + i,
                        "id", threadId * 1000 + i,
                        "status", "active"
                    );
                    engine.renderString(template, variables);
                }
                long threadEnd = System.nanoTime();
                totalRenderTime.addAndGet(threadEnd - threadStart);
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        long overallEnd = System.nanoTime();

        long overallDurationMs = (overallEnd - overallStart) / 1_000_000;
        long avgRenderTimeMs = totalRenderTime.get() / 1_000_000;

        System.out.println("Concurrent rendering (" + threadCount + " threads, " +
                          iterationsPerThread + " iterations each):");
        System.out.println("Overall time: " + overallDurationMs + "ms");
        System.out.println("Total render time: " + avgRenderTimeMs + "ms");

        // Should complete concurrent rendering in reasonable time
        assertTrue(overallDurationMs < 5000,
            "Concurrent rendering took too long: " + overallDurationMs + "ms");
    }

    @Test
    @DisplayName("Should validate template performance")
    void shouldValidateTemplatePerformance() {
        List<String> templates = Arrays.asList(
            "{{simple}}",
            "{{var1}} and {{var2}} with {{nested.value}}",
            "{{#if condition}}{{value|filter}}{{/if}}",
            "{{#each items}}{{name|upper|truncate:10}}{{/each}}",
            "Complex {{var1|upper}} with {{#if flag}}conditional{{/if}} and {{#each list}}{{item}}{{/each}}"
        );

        long startTime = System.nanoTime();
        for (String template : templates) {
            for (int i = 0; i < 1000; i++) {
                engine.validateTemplate(template);
            }
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Template validation (5000 validations): " + durationMs + "ms");

        // Should validate templates quickly
        assertTrue(durationMs < 200, "Template validation took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Should extract variables efficiently")
    void shouldExtractVariablesEfficiently() {
        String complexTemplate = "{{var1}} {{var2|filter}} {{nested.value}} {{#if condition}}{{conditional}}{{/if}} " +
                                "{{#each items}}{{name}} {{value|upper}}{{/each}} {{final.nested.deep.value}}";

        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            engine.extractVariableNames(complexTemplate);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Variable extraction (10k extractions): " + durationMs + "ms");

        // Should extract variables quickly
        assertTrue(durationMs < 500, "Variable extraction took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Should handle cache eviction performance")
    void shouldHandleCacheEvictionPerformance() {
        PromptTemplateEngine limitedCacheEngine = new PromptTemplateEngine(true, 100);
        Map<String, Object> variables = Map.of("value", "test");

        long startTime = System.nanoTime();

        // Generate more templates than cache can hold
        for (int i = 0; i < 1000; i++) {
            String template = "Template " + i + ": {{value}}";
            limitedCacheEngine.renderString(template, variables);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("Cache eviction test (1000 unique templates, 100 cache limit): " + durationMs + "ms");

        // Cache size should be at limit
        PromptTemplateEngine.CacheStats stats = limitedCacheEngine.getCacheStats();
        assertEquals(100, stats.getCurrentSize());

        // Should handle cache eviction efficiently
        assertTrue(durationMs < 100, "Cache eviction took too long: " + durationMs + "ms");
    }

    @Test
    @DisplayName("Should demonstrate memory efficiency")
    void shouldDemonstrateMemoryEfficiency() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Create many template instances
        for (int i = 0; i < 1000; i++) {
            String template = "Template {{var" + i + "}} with {{value|filter}} and {{nested.prop}}";
            Map<String, Object> variables = Map.of(
                "var" + i, "value" + i,
                "value", "test",
                "nested", Map.of("prop", "property")
            );
            engine.renderString(template, variables);
        }

        runtime.gc(); // Suggest garbage collection
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;
        System.out.println("Memory used for 1000 template renderings: " +
                          (memoryUsed / 1024) + " KB");

        // Should not use excessive memory (less than 10MB)
        assertTrue(memoryUsed < 10 * 1024 * 1024,
            "Template engine used too much memory: " + (memoryUsed / 1024) + " KB");
    }
}
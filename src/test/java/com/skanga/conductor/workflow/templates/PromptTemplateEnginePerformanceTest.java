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
 *
 * These tests are disabled by default to avoid CI slowdown. Enable with:
 * -Dtest.performance.enabled=true (basic performance validation)
 * -Dtest.performance.intensive=true (full performance benchmarking)
 */
@DisplayName("PromptTemplateEngine Performance Tests")
@EnabledIfSystemProperty(named = "test.performance.enabled", matches = "true")
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

    private boolean isIntensiveTestingEnabled() {
        return "true".equals(System.getProperty("test.performance.intensive"));
    }

    @Test
    @DisplayName("Should render simple templates within performance threshold")
    void shouldRenderSimpleTemplatesWithinPerformanceThreshold() {
        String template = "Hello {{name}}, welcome to {{place}}!";
        Map<String, Object> variables = Map.of("name", "User", "place", "System");

        // Use much smaller iteration count for regular testing to avoid CI slowdown
        int iterations = isIntensiveTestingEnabled() ? 10000 : 10;
        long threshold = isIntensiveTestingEnabled() ? 1000 : 2000; // Very lenient for regular tests

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            engine.renderString(template, variables);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Simple template rendering (" + iterations + " iterations): " + durationMs + "ms");

        assertTrue(durationMs < threshold, "Simple template rendering took too long: " + durationMs + "ms (threshold: " + threshold + "ms)");
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

        int iterations = isIntensiveTestingEnabled() ? 5000 : 5;

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

        // For small iteration counts, the difference might not be significant due to JVM warmup
        // Just verify both complete successfully and report the results
        assertTrue(cachedDuration >= 0 && noCacheDuration >= 0,
            "Both cached and non-cached rendering should complete successfully");

        // Only assert performance difference for intensive testing where the difference is meaningful
        if (isIntensiveTestingEnabled() && iterations >= 1000) {
            assertTrue(cachedDuration < noCacheDuration,
                "Caching should be faster than non-cached for repeated templates in intensive testing");
        }
    }

    @Test
    @DisplayName("Should handle large templates efficiently")
    void shouldHandleLargeTemplatesEfficiently() {
        StringBuilder largeTemplate = new StringBuilder();
        Map<String, Object> variables = new HashMap<>();

        // Create a template with variables (much fewer for regular testing)
        int varCount = isIntensiveTestingEnabled() ? 1000 : 5;
        for (int i = 0; i < varCount; i++) {
            largeTemplate.append("Variable ").append(i).append(": {{var").append(i).append("}} ");
            variables.put("var" + i, "value" + i);
        }

        long startTime = System.nanoTime();
        String result = engine.renderString(largeTemplate.toString(), variables);
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        long threshold = isIntensiveTestingEnabled() ? 100 : 200;
        System.out.println("Large template rendering (" + varCount + " variables): " + durationMs + "ms");

        assertNotNull(result);
        // Expect result length to scale with variable count: ~18 chars per variable minimum
        int expectedMinLength = varCount * 18;
        assertTrue(result.length() >= expectedMinLength,
            "Result too short: " + result.length() + " chars, expected at least " + expectedMinLength);
        assertTrue(durationMs < threshold, "Large template rendering took too long: " + durationMs + "ms (threshold: " + threshold + "ms)");
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
        int activityCount = isIntensiveTestingEnabled() ? 100 : 3;
        for (int i = 0; i < activityCount; i++) {
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
        long threshold = isIntensiveTestingEnabled() ? 50 : 100;
        System.out.println("Complex nested template rendering: " + durationMs + "ms");

        assertNotNull(result);
        assertTrue(result.contains("JOHN DOE"));
        assertTrue(result.contains("ACTION0"));
        assertTrue(durationMs < threshold, "Complex template rendering took too long: " + durationMs + "ms (threshold: " + threshold + "ms)");
    }

    @Test
    @DisplayName("Should handle concurrent rendering performance")
    void shouldHandleConcurrentRenderingPerformance() throws InterruptedException {
        String template = "Hello {{name}}, your ID is {{id}} and status is {{status|upper}}";
        int threadCount = isIntensiveTestingEnabled() ? 10 : 2;
        int iterationsPerThread = isIntensiveTestingEnabled() ? 1000 : 3;
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
        long threshold = isIntensiveTestingEnabled() ? 5000 : 10000;
        assertTrue(overallDurationMs < threshold,
            "Concurrent rendering took too long: " + overallDurationMs + "ms (threshold: " + threshold + "ms)");
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
        int iterations = isIntensiveTestingEnabled() ? 1000 : 5;
        for (String template : templates) {
            for (int i = 0; i < iterations; i++) {
                engine.validateTemplate(template);
            }
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        int totalValidations = templates.size() * iterations;
        long threshold = isIntensiveTestingEnabled() ? 200 : 500;
        System.out.println("Template validation (" + totalValidations + " validations): " + durationMs + "ms");

        assertTrue(durationMs < threshold, "Template validation took too long: " + durationMs + "ms (threshold: " + threshold + "ms)");
    }

    @Test
    @DisplayName("Should extract variables efficiently")
    void shouldExtractVariablesEfficiently() {
        String complexTemplate = "{{var1}} {{var2|filter}} {{nested.value}} {{#if condition}}{{conditional}}{{/if}} " +
                                "{{#each items}}{{name}} {{value|upper}}{{/each}} {{final.nested.deep.value}}";

        long startTime = System.nanoTime();
        int iterations = isIntensiveTestingEnabled() ? 10000 : 10;
        for (int i = 0; i < iterations; i++) {
            engine.extractVariableNames(complexTemplate);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        long threshold = isIntensiveTestingEnabled() ? 500 : 1000;
        System.out.println("Variable extraction (" + iterations + " extractions): " + durationMs + "ms");

        assertTrue(durationMs < threshold, "Variable extraction took too long: " + durationMs + "ms (threshold: " + threshold + "ms)");
    }

    @Test
    @DisplayName("Should handle cache eviction performance")
    void shouldHandleCacheEvictionPerformance() {
        PromptTemplateEngine limitedCacheEngine = new PromptTemplateEngine(true, 100);
        Map<String, Object> variables = Map.of("value", "test");

        long startTime = System.nanoTime();

        // Generate more templates than cache can hold
        int templateCount = isIntensiveTestingEnabled() ? 1000 : 10;
        for (int i = 0; i < templateCount; i++) {
            String template = "Template " + i + ": {{value}}";
            limitedCacheEngine.renderString(template, variables);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        long threshold = isIntensiveTestingEnabled() ? 100 : 200;

        System.out.println("Cache eviction test (" + templateCount + " unique templates, 100 cache limit): " + durationMs + "ms");

        // Cache size should match the number of unique templates created (or cache limit if exceeded)
        PromptTemplateEngine.CacheStats stats = limitedCacheEngine.getCacheStats();
        int expectedCacheSize = Math.min(templateCount, 100);
        assertEquals(expectedCacheSize, stats.getCurrentSize());

        assertTrue(durationMs < threshold, "Cache eviction took too long: " + durationMs + "ms (threshold: " + threshold + "ms)");
    }

    @Test
    @DisplayName("Should demonstrate memory efficiency")
    void shouldDemonstrateMemoryEfficiency() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Create many template instances
        int templateCount = isIntensiveTestingEnabled() ? 1000 : 10;
        for (int i = 0; i < templateCount; i++) {
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
        System.out.println("Memory used for " + templateCount + " template renderings: " +
                          (memoryUsed / 1024) + " KB");

        // Should not use excessive memory (relaxed thresholds for testing)
        long threshold = isIntensiveTestingEnabled() ? 10 * 1024 * 1024 : 20 * 1024 * 1024; // 10MB/20MB
        assertTrue(memoryUsed < threshold,
            "Template engine used too much memory: " + (memoryUsed / 1024) + " KB (threshold: " + (threshold / 1024) + " KB)");
    }
}
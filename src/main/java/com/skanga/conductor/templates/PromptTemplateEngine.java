package com.skanga.conductor.templates;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.TemplateConfig;
import com.skanga.conductor.workflow.config.AgentConfigCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced engine for rendering prompt templates with variable substitution.
 * Supports {{variable}} syntax for template variables, conditional logic,
 * loops, filters, and template caching for improved performance.
 *
 * <p><strong>Template Features:</strong></p>
 * <ul>
 * <li>Variables: {{variable}}</li>
 * <li>Conditionals: {{#if condition}}content{{/if}}</li>
 * <li>Loops: {{#each items}}{{name}}{{/each}}</li>
 * <li>Filters: {{variable|upper|truncate:50}}</li>
 * <li>Default values: {{variable|default:'fallback'}}</li>
 * <li>Nested access: {{context.user.name}}</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Create engine with default settings from config
 * PromptTemplateEngine engine = new PromptTemplateEngine();
 *
 * // Render a simple string template
 * String result = engine.render("Hello {{name}}!", Map.of("name", "Alice"));
 * // Output: "Hello Alice!"
 *
 * // Render a PromptTemplate object
 * AgentConfigCollection.PromptTemplate template = ...;
 * String prompt = engine.render(template, variables);
 *
 * // Validate template syntax
 * engine.validateTemplate("{{name}}");  // OK
 * engine.validateTemplate("{{name}");   // Throws TemplateException
 *
 * // Monitor cache performance
 * CacheStats stats = engine.getCacheStats();
 * System.out.println(stats.getHitRate());  // 0.95 (95% hit rate)
 * }</pre>
 *
 * <p><strong>Architecture:</strong></p>
 * <ul>
 * <li>{@link TemplateCompiler} - Compiles templates into executable form</li>
 * <li>{@link VariableResolver} - Resolves variable references and applies filters</li>
 * <li>{@link TemplateFilters} - Provides built-in template filters</li>
 * <li>{@link TemplateValidator} - Validates template syntax</li>
 * <li>LRU Cache - Templates are compiled once and cached with automatic eviction</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe for concurrent template rendering.
 * All caches use synchronized collections and atomic counters for metrics.</p>
 *
 * @since 1.0.0
 * @see TemplateException for error handling
 * @see CacheStats for cache monitoring
 */
public class PromptTemplateEngine implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateEngine.class);
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]*)\\}\\}");

    // Delegated components
    private final TemplateFilters filters;
    private final VariableResolver variableResolver;
    private final TemplateCompiler compiler;
    private final TemplateValidator validator;

    // Template cache for performance optimization with proper LRU eviction and TTL
    private final Map<String, CachedTemplate> templateCache;
    private final boolean cachingEnabled;
    private final int maxCacheSize;
    private final long cacheTtlMillis;
    private final boolean ttlEvictionEnabled;
    private final long cleanupIntervalMillis;
    private volatile long lastCleanupTime = System.currentTimeMillis();

    // Cache metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    private final AtomicLong cacheTtlEvictions = new AtomicLong(0);

    // Scheduled cleanup to prevent memory leaks in low-traffic systems
    private final ScheduledExecutorService cleanupScheduler;

    /**
     * Default constructor that reads configuration from ApplicationConfig.
     * Uses TemplateConfig for cache size, TTL, and eviction policies.
     */
    public PromptTemplateEngine() {
        TemplateConfig config = ApplicationConfig.getInstance().getTemplateConfig();
        this.cachingEnabled = config.isCacheEnabled();
        this.maxCacheSize = config.getCacheMaxSize();
        this.cacheTtlMillis = config.getCacheTtl().toMillis();
        this.ttlEvictionEnabled = config.isTtlEvictionEnabled();
        this.cleanupIntervalMillis = config.getCacheCleanupInterval().toMillis();
        this.filters = new TemplateFilters();
        this.variableResolver = new VariableResolver(filters);
        this.compiler = new TemplateCompiler(variableResolver);
        this.validator = new TemplateValidator();

        // Initialize cache with proper LRU eviction using LinkedHashMap
        if (cachingEnabled) {
            this.templateCache = Collections.synchronizedMap(
                new LinkedHashMap<String, CachedTemplate>(maxCacheSize, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, CachedTemplate> eldest) {
                        boolean shouldRemove = size() > maxCacheSize;
                        if (shouldRemove) {
                            cacheEvictions.incrementAndGet();
                        }
                        return shouldRemove;
                    }
                }
            );
        } else {
            this.templateCache = Collections.emptyMap();
        }

        // Start scheduled cleanup for TTL eviction to prevent memory leaks
        if (cachingEnabled && ttlEvictionEnabled) {
            this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TemplateCache-Cleanup");
                t.setDaemon(true);
                return t;
            });
            // Schedule periodic cleanup at configured interval
            cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                cleanupIntervalMillis,
                cleanupIntervalMillis,
                TimeUnit.MILLISECONDS
            );
            logger.debug("Started scheduled template cache cleanup with interval: {}ms", cleanupIntervalMillis);
        } else {
            this.cleanupScheduler = null;
        }
    }

    /**
     * Constructor with configurable caching.
     *
     * @param cachingEnabled whether to enable template caching
     * @param maxCacheSize maximum number of templates to cache
     * @throws IllegalArgumentException if maxCacheSize is less than 1
     */
    public PromptTemplateEngine(boolean cachingEnabled, int maxCacheSize) {
        this(cachingEnabled, maxCacheSize, 0, false);
    }

    /**
     * Constructor with full cache configuration.
     *
     * @param cachingEnabled whether to enable template caching
     * @param maxCacheSize maximum number of templates to cache
     * @param cacheTtlMillis time-to-live for cache entries in milliseconds (0 to disable)
     * @param ttlEvictionEnabled whether to enable TTL-based eviction
     * @throws IllegalArgumentException if maxCacheSize is less than 1
     */
    public PromptTemplateEngine(boolean cachingEnabled, int maxCacheSize, long cacheTtlMillis, boolean ttlEvictionEnabled) {
        if (cachingEnabled && maxCacheSize < 1) {
            throw new IllegalArgumentException("maxCacheSize must be at least 1 when caching is enabled");
        }
        this.cachingEnabled = cachingEnabled;
        this.maxCacheSize = cachingEnabled ? maxCacheSize : 0;
        this.cacheTtlMillis = cacheTtlMillis;
        this.ttlEvictionEnabled = ttlEvictionEnabled && cacheTtlMillis > 0;
        this.cleanupIntervalMillis = cacheTtlMillis > 0 ? cacheTtlMillis / 2 : 300000; // Default to TTL/2 or 5 minutes
        this.filters = new TemplateFilters();
        this.variableResolver = new VariableResolver(filters);
        this.compiler = new TemplateCompiler(variableResolver);
        this.validator = new TemplateValidator();

        // Initialize cache with proper LRU eviction using LinkedHashMap
        if (cachingEnabled) {
            this.templateCache = Collections.synchronizedMap(
                new LinkedHashMap<String, CachedTemplate>(maxCacheSize, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, CachedTemplate> eldest) {
                        boolean shouldRemove = size() > maxCacheSize;
                        if (shouldRemove) {
                            cacheEvictions.incrementAndGet();
                        }
                        return shouldRemove;
                    }
                }
            );
        } else {
            this.templateCache = Collections.emptyMap();
        }

        // Start scheduled cleanup for TTL eviction (only for this constructor path)
        if (cachingEnabled && this.ttlEvictionEnabled) {
            this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TemplateCache-Cleanup");
                t.setDaemon(true);
                return t;
            });
            cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                cleanupIntervalMillis,
                cleanupIntervalMillis,
                TimeUnit.MILLISECONDS
            );
            logger.debug("Started scheduled template cache cleanup with interval: {}ms", cleanupIntervalMillis);
        } else {
            this.cleanupScheduler = null;
        }
    }

    /**
     * Renders a string template with the given variables.
     * This is the main rendering method that supports all template features.
     *
     * @param template the template string to render
     * @param variables the variables to substitute in the template (can be null, will use empty map)
     * @return the rendered string, or null if template is null
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        // Variables can be null - will be converted to empty map in renderAdvancedTemplate
        return renderAdvancedTemplate(template, variables);
    }

    /**
     * Renders a prompt template with the given variables.
     * This method formats the template into a conversational structure.
     *
     * @param template the prompt template to render
     * @param variables the variables to substitute in the template (can be null, will use empty map)
     * @return the rendered prompt string
     * @throws IllegalArgumentException if template is null
     */
    public String render(AgentConfigCollection.PromptTemplate template, Map<String, Object> variables) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        // Variables can be null - will be converted to empty map in render()
        return formatPromptTemplate(template, variables);
    }

    /**
     * Formats a PromptTemplate object into a conversational structure.
     * This is a private helper that delegates rendering to the main render() method.
     */
    private String formatPromptTemplate(AgentConfigCollection.PromptTemplate template, Map<String, Object> variables) {
        StringBuilder result = new StringBuilder();

        // Add system message if present
        if (template.hasSystem()) {
            String systemPrompt = render(template.getSystem(), variables);
            result.append("System: ").append(systemPrompt);
        }

        // Add user message if present
        if (template.hasUser()) {
            if (result.length() > 0) {
                result.append("\n\n");
            }
            String userPrompt = render(template.getUser(), variables);
            result.append("Human: ").append(userPrompt);
        }

        // Add assistant message if present (for few-shot examples)
        if (template.hasAssistant()) {
            if (result.length() > 0) {
                result.append("\n\n");
            }
            String assistantPrompt = render(template.getAssistant(), variables);
            result.append("Assistant: ").append(assistantPrompt);
        }

        String renderedPrompt = result.toString();

        // Guard debug logging to avoid performance impact in hot path
        if (logger.isDebugEnabled()) {
            logger.debug("Rendered prompt template with {} variables: {} characters",
                        variables.size(), renderedPrompt.length());
        }

        return renderedPrompt;
    }

    /**
     * @deprecated Use {@link #render(AgentConfigCollection.PromptTemplate, Map)} instead.
     */
    @Deprecated
    public String renderPrompt(AgentConfigCollection.PromptTemplate template, Map<String, Object> variables) {
        return render(template, variables);
    }

    /**
     * @deprecated Use {@link #render(String, Map)} instead.
     */
    @Deprecated
    public String renderString(String template, Map<String, Object> variables) {
        return render(template, variables);
    }

    /**
     * Renders a template with advanced features including conditionals, loops, and filters.
     */
    private String renderAdvancedTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        // Use cached compiled template if available
        TemplateCompiler.CompiledTemplate compiled = getCompiledTemplate(template);
        return compiled.render(variables != null ? variables : Collections.emptyMap());
    }

    /**
     * Gets or creates a compiled template from cache.
     * LRU eviction is handled automatically by LinkedHashMap.
     * TTL eviction removes expired entries.
     * Tracks cache hits and misses for monitoring.
     * <p>
     * Thread-safe: uses synchronized block to ensure atomicity of check-then-act operations.
     * </p>
     */
    private TemplateCompiler.CompiledTemplate getCompiledTemplate(String template) {
        if (!cachingEnabled) {
            cacheMisses.incrementAndGet();
            return compiler.compile(template);
        }

        // Periodically clean up expired entries if TTL is enabled
        cleanupExpiredEntriesIfNeeded();

        // Synchronized to ensure atomic check-then-act for cache operations
        synchronized (templateCache) {
            // Check if template is in cache
            CachedTemplate cached = templateCache.get(template);
            if (cached != null) {
                // Check if entry has expired
                if (ttlEvictionEnabled && cached.isExpired(cacheTtlMillis)) {
                    templateCache.remove(template);
                    cacheTtlEvictions.incrementAndGet();
                    cacheMisses.incrementAndGet();
                } else {
                    cacheHits.incrementAndGet();
                    return cached.getCompiledTemplate();
                }
            } else {
                cacheMisses.incrementAndGet();
            }

            // Cache miss or expired - compile and store
            TemplateCompiler.CompiledTemplate compiled = compiler.compile(template);
            templateCache.put(template, new CachedTemplate(compiled));
            return compiled;
        }
    }

    /**
     * Cleans up expired cache entries periodically.
     * This prevents memory leaks from expired but unaccessed entries.
     * Cleanup interval is configurable via TemplateConfig.
     */
    private void cleanupExpiredEntriesIfNeeded() {
        if (!ttlEvictionEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        // Cleanup at configured interval
        if (now - lastCleanupTime > cleanupIntervalMillis) {
            cleanupExpiredEntries();
        }
    }

    /**
     * Cleans up expired cache entries immediately.
     * This is called by both manual cleanup and scheduled cleanup.
     */
    private void cleanupExpiredEntries() {
        if (!ttlEvictionEnabled) {
            return;
        }

        synchronized (templateCache) {
            Iterator<Map.Entry<String, CachedTemplate>> iterator = templateCache.entrySet().iterator();
            int removed = 0;
            while (iterator.hasNext()) {
                Map.Entry<String, CachedTemplate> entry = iterator.next();
                if (entry.getValue().isExpired(cacheTtlMillis)) {
                    iterator.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                cacheTtlEvictions.addAndGet(removed);
                logger.debug("Cleaned up {} expired template cache entries", removed);
            }
            lastCleanupTime = System.currentTimeMillis();
        }
    }

    /**
     * Shuts down the scheduled cleanup executor.
     * Should be called when the engine is no longer needed to prevent resource leaks.
     */
    public void shutdown() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.debug("Template cache cleanup scheduler shut down");
        }
    }

    /**
     * Closes the template engine and releases all resources.
     * This method implements AutoCloseable to support try-with-resources.
     * Delegates to {@link #shutdown()} for cleanup.
     */
    @Override
    public void close() {
        shutdown();
    }


    /**
     * Extracts all variable names from a template string.
     *
     * @param template the template to analyze
     * @return array of variable names found in the template, or empty array if template is null
     */
    public String[] extractVariableNames(String template) {
        if (template == null) {
            return new String[0];
        }
        return variableResolver.extractVariableNames(template);
    }

    /**
     * Validates that a template is syntactically correct.
     * Delegates to TemplateValidator for validation logic.
     *
     * @param template the template to validate
     * @throws TemplateException if the template is invalid
     */
    public void validateTemplate(String template) throws TemplateException {
        validator.validate(template);
    }

    /**
     * Validates a complete prompt template.
     * Delegates to TemplateValidator for validation logic.
     *
     * @param template the prompt template to validate
     * @throws IllegalArgumentException if the template is invalid
     */
    public void validatePromptTemplate(AgentConfigCollection.PromptTemplate template) throws IllegalArgumentException {
        validator.validatePromptTemplate(template);
    }

    /**
     * Clears the template cache and resets metrics.
     */
    public void clearCache() {
        templateCache.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        cacheEvictions.set(0);
        cacheTtlEvictions.set(0);
    }

    /**
     * Gets cache statistics including hit rate and eviction count.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            templateCache.size(),
            maxCacheSize,
            cachingEnabled,
            cacheHits.get(),
            cacheMisses.get(),
            cacheEvictions.get(),
            cacheTtlEvictions.get(),
            ttlEvictionEnabled
        );
    }


    /**
     * Cache statistics holder with hit rate and eviction tracking.
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final boolean enabled;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final long ttlEvictions;
        private final boolean ttlEnabled;

        public CacheStats(int currentSize, int maxSize, boolean enabled, long hits, long misses, long evictions, long ttlEvictions, boolean ttlEnabled) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.enabled = enabled;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.ttlEvictions = ttlEvictions;
            this.ttlEnabled = ttlEnabled;
        }

        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public boolean isEnabled() { return enabled; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public long getTtlEvictions() { return ttlEvictions; }
        public boolean isTtlEnabled() { return ttlEnabled; }
        public long getTotalRequests() { return hits + misses; }
        public double getUsageRatio() { return enabled ? (double) currentSize / maxSize : 0.0; }
        public double getHitRate() {
            long total = getTotalRequests();
            return total > 0 ? (double) hits / total : 0.0;
        }

        @Override
        public String toString() {
            if (ttlEnabled) {
                return String.format("CacheStats{enabled=%s, size=%d/%d, usage=%.1f%%, hits=%d, misses=%d, lruEvictions=%d, ttlEvictions=%d, hitRate=%.1f%%}",
                    enabled, currentSize, maxSize, getUsageRatio() * 100, hits, misses, evictions, ttlEvictions, getHitRate() * 100);
            } else {
                return String.format("CacheStats{enabled=%s, size=%d/%d, usage=%.1f%%, hits=%d, misses=%d, evictions=%d, hitRate=%.1f%%}",
                    enabled, currentSize, maxSize, getUsageRatio() * 100, hits, misses, evictions, getHitRate() * 100);
            }
        }
    }

    /**
     * Wrapper for cached templates with timestamp tracking for TTL eviction.
     */
    private static class CachedTemplate {
        private final TemplateCompiler.CompiledTemplate compiledTemplate;
        private final long creationTime;

        public CachedTemplate(TemplateCompiler.CompiledTemplate compiledTemplate) {
            this.compiledTemplate = compiledTemplate;
            this.creationTime = System.currentTimeMillis();
        }

        public TemplateCompiler.CompiledTemplate getCompiledTemplate() {
            return compiledTemplate;
        }

        public boolean isExpired(long ttlMillis) {
            return ttlMillis > 0 && (System.currentTimeMillis() - creationTime) > ttlMillis;
        }

        public long getAge() {
            return System.currentTimeMillis() - creationTime;
        }
    }
}
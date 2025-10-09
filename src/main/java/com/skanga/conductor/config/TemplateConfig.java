package com.skanga.conductor.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.Duration;
import java.util.Properties;

/**
 * Template engine configuration settings.
 * <p>
 * Provides access to template caching and rendering configuration
 * including cache size, TTL, and LRU eviction policies.
 * </p>
 *
 * @since 1.2.0
 */
public class TemplateConfig extends ConfigurationProvider {

    public TemplateConfig(Properties properties) {
        super(properties);
    }

    /**
     * Returns whether template caching is enabled.
     * <p>
     * When enabled, compiled templates are cached for reuse.
     * Disable caching for debugging or low-memory environments.
     * </p>
     *
     * @return true if template caching is enabled
     */
    public boolean isCacheEnabled() {
        return getBoolean("conductor.template.cache.enabled", true);
    }

    /**
     * Returns the maximum number of templates to cache.
     * <p>
     * Uses LRU (Least Recently Used) eviction when the cache is full.
     * Larger values improve hit rate but use more memory.
     * </p>
     *
     * @return maximum cache size (1-10000)
     */
    @Min(value = 1, message = "Template cache size must be at least 1")
    @Max(value = 10000, message = "Template cache size cannot exceed 10000")
    public int getCacheMaxSize() {
        return getInt("conductor.template.cache.max.size", 500);
    }

    /**
     * Returns the time-to-live for cached templates.
     * <p>
     * Templates older than this duration are automatically evicted
     * from the cache. Set to 0 to disable TTL-based eviction.
     * </p>
     *
     * @return TTL duration for cache entries
     */
    public Duration getCacheTtl() {
        return getDuration("conductor.template.cache.ttl", Duration.ofMinutes(30));
    }

    /**
     * Returns whether TTL-based eviction is enabled.
     * <p>
     * When enabled, cached templates expire after the configured TTL.
     * When disabled, only LRU eviction is used.
     * </p>
     *
     * @return true if TTL eviction is enabled
     */
    public boolean isTtlEvictionEnabled() {
        Duration ttl = getCacheTtl();
        return !ttl.isZero() && !ttl.isNegative();
    }

    /**
     * Returns the interval for checking expired cache entries.
     * <p>
     * The cleanup task runs at this interval to remove expired entries.
     * Should be less than or equal to the TTL duration.
     * </p>
     *
     * @return cleanup check interval
     */
    public Duration getCacheCleanupInterval() {
        Duration ttl = getCacheTtl();
        Duration defaultInterval = ttl.isZero() ? Duration.ofMinutes(5) : ttl.dividedBy(2);
        return getDuration("conductor.template.cache.cleanup.interval", defaultInterval);
    }
}

package com.skanga.conductor.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Properties;

/**
 * Memory configuration settings.
 * <p>
 * Provides access to memory store configuration including
 * limits and retention policies.
 * </p>
 *
 * @since 1.1.0
 */
public class MemoryConfig extends ConfigurationProvider {

    public MemoryConfig(Properties properties) {
        super(properties);
    }

    @Min(value = 1, message = "Default memory limit must be at least 1")
    @Max(value = 10000, message = "Default memory limit cannot exceed 10000")
    public int getDefaultMemoryLimit() {
        return getInt("conductor.memory.default.limit", 10);
    }

    @Min(value = 1, message = "Max memory entries must be at least 1")
    @Max(value = 1000000, message = "Max memory entries cannot exceed 1000000")
    public int getMaxMemoryEntries() {
        return getInt("conductor.memory.max.entries", 1000);
    }

    @Min(value = 1, message = "Memory retention days must be at least 1")
    @Max(value = 3650, message = "Memory retention days cannot exceed 3650")
    public int getMemoryRetentionDays() {
        return getInt("conductor.memory.retention.days", 30);
    }

    /**
     * Gets the memory warning threshold (0.0 to 1.0).
     * Default: 0.75 (75%)
     */
    @DecimalMin(value = "0.0", message = "Memory warning threshold must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Memory warning threshold cannot exceed 1.0")
    public double getMemoryWarningThreshold() {
        return getDouble("conductor.memory.threshold.warning", 0.75);
    }

    /**
     * Gets the memory critical threshold (0.0 to 1.0).
     * Default: 0.85 (85%)
     */
    @DecimalMin(value = "0.0", message = "Memory critical threshold must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Memory critical threshold cannot exceed 1.0")
    public double getMemoryCriticalThreshold() {
        return getDouble("conductor.memory.threshold.critical", 0.85);
    }

    /**
     * Gets the memory emergency threshold (0.0 to 1.0).
     * Default: 0.95 (95%)
     */
    @DecimalMin(value = "0.0", message = "Memory emergency threshold must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Memory emergency threshold cannot exceed 1.0")
    public double getMemoryEmergencyThreshold() {
        return getDouble("conductor.memory.threshold.emergency", 0.95);
    }

    /**
     * Gets the memory monitoring interval.
     * Default: 30 seconds
     */
    public Duration getMemoryMonitoringInterval() {
        return Duration.ofSeconds(getLong("conductor.memory.monitoring.interval.seconds", 30));
    }

    /**
     * Gets the memory cleanup interval.
     * Default: 5 minutes
     */
    public Duration getMemoryCleanupInterval() {
        return Duration.ofMinutes(getLong("conductor.memory.cleanup.interval.minutes", 5));
    }

    /**
     * Gets the resource expiry time.
     * Default: 1 hour
     */
    public Duration getResourceExpiryTime() {
        return Duration.ofHours(getLong("conductor.memory.resource.expiry.hours", 1));
    }

    /**
     * Gets the memory manager thread pool size.
     * <p>
     * This controls the number of background threads used for memory monitoring
     * and cleanup tasks.
     * </p>
     * Default: 2 (one for monitoring, one for cleanup)
     */
    @Min(value = 1, message = "Memory manager thread pool size must be at least 1")
    @Max(value = 10, message = "Memory manager thread pool size cannot exceed 10")
    public int getMemoryManagerThreadPoolSize() {
        return getInt("conductor.memory.threadpool.size", 2);
    }
}

package com.skanga.conductor.config;

import jakarta.validation.constraints.*;
import java.util.Properties;

/**
 * Configuration settings for parallel task execution.
 * <p>
 * Provides access to parallelism configuration including thread pool
 * settings, timeouts, and execution thresholds.
 * </p>
 *
 * @since 1.1.0
 */
public class ParallelismConfig extends ConfigurationProvider {

    public ParallelismConfig(Properties properties) {
        super(properties);
    }

    public boolean isEnabled() {
        return getBoolean("conductor.parallelism.enabled", true);
    }

    @Min(value = 1, message = "Max threads must be at least 1")
    @Max(value = 1000, message = "Max threads cannot exceed 1000")
    public int getMaxThreads() {
        return getInt("conductor.parallelism.max.threads", Runtime.getRuntime().availableProcessors());
    }

    @Min(value = 1, message = "Max tasks per batch must be at least 1")
    public int getMaxParallelTasksPerBatch() {
        int maxTasks = getInt("conductor.parallelism.max.tasks.per.batch", getMaxThreads());
        int maxThreads = getMaxThreads();
        if (maxTasks > maxThreads) {
            throw new IllegalArgumentException("Max tasks per batch cannot exceed max threads: " + maxTasks + " > " + maxThreads);
        }
        return maxTasks;
    }

    @Min(value = 1, message = "Task timeout must be at least 1 second")
    @Max(value = 86400, message = "Task timeout cannot exceed 1 day (86400 seconds)")
    public long getTaskTimeoutSeconds() {
        return getLong("conductor.parallelism.task.timeout.seconds", 300L); // 5 minutes
    }

    @Min(value = 1, message = "Batch timeout must be at least 1 second")
    public long getBatchTimeoutSeconds() {
        long batchTimeout = getLong("conductor.parallelism.batch.timeout.seconds", 1800L); // 30 minutes
        long taskTimeout = getTaskTimeoutSeconds();
        if (batchTimeout < taskTimeout) {
            throw new IllegalArgumentException("Batch timeout must be >= task timeout: " + batchTimeout + " < " + taskTimeout);
        }
        return batchTimeout;
    }

    public boolean isFallbackToSequentialEnabled() {
        return getBoolean("conductor.parallelism.fallback.sequential", true);
    }

    @Min(value = 1, message = "Min tasks for parallel execution must be at least 1")
    public int getMinTasksForParallelExecution() {
        return getInt("conductor.parallelism.min.tasks.threshold", 2);
    }

    @DecimalMin(value = "0.0", message = "Parallelism threshold must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Parallelism threshold cannot exceed 1.0")
    public double getParallelismThreshold() {
        return getDouble("conductor.parallelism.threshold", 0.3); // 30% parallelizable tasks minimum
    }
}

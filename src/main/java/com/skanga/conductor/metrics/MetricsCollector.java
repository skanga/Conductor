package com.skanga.conductor.metrics;

import java.util.Map;

/**
 * Interface for collecting metrics from agents and tools in the Conductor framework.
 * <p>
 * This interface defines the contract for recording various types of metrics
 * including execution times, success/failure rates, and operational statistics.
 * Implementations can store metrics in memory, write to files, send to external
 * monitoring systems, or provide real-time dashboards.
 * </p>
 * <p>
 * All methods should be thread-safe to support concurrent metric collection
 * from multiple agents and tools executing simultaneously.
 * </p>
 *
 * @since 1.0.0
 * @see Metric
 * @see MetricsRegistry
 */
public interface MetricsCollector {

    /**
     * Records a metric measurement.
     * <p>
     * This is the primary method for submitting metrics to the collection system.
     * Implementations should handle the metric asynchronously if possible to
     * minimize impact on agent and tool execution performance.
     * </p>
     *
     * @param metric the metric to record, must not be null
     * @throws IllegalArgumentException if metric is null or invalid
     */
    void record(Metric metric);

    /**
     * Records an execution timer metric for an agent or tool.
     * <p>
     * Convenience method for recording execution duration metrics with
     * standard naming and tagging conventions.
     * </p>
     *
     * @param component the component name (agent or tool name)
     * @param operation the operation type ("agent.execution" or "tool.execution")
     * @param durationMs the execution duration in milliseconds
     * @param success whether the execution was successful
     */
    default void recordExecution(String component, String operation, long durationMs, boolean success) {
        record(Metric.timer(
            operation + ".duration",
            durationMs,
            Map.of(
                "component", component,
                "success", String.valueOf(success)
            )
        ));

        record(Metric.counter(
            operation + ".count",
            Map.of(
                "component", component,
                "success", String.valueOf(success)
            )
        ));
    }

    /**
     * Records a success rate metric for an agent or tool.
     * <p>
     * Convenience method for tracking success/failure rates over time.
     * </p>
     *
     * @param component the component name
     * @param operation the operation type
     * @param successCount number of successful executions
     * @param totalCount total number of executions
     */
    default void recordSuccessRate(String component, String operation, long successCount, long totalCount) {
        double successRate = totalCount > 0 ? (double) successCount / totalCount : 0.0;
        record(Metric.gauge(
            operation + ".success_rate",
            successRate,
            Map.of("component", component)
        ));
    }

    /**
     * Records an error occurrence.
     * <p>
     * Convenience method for tracking error counts and types.
     * </p>
     *
     * @param component the component where the error occurred
     * @param errorType the type or category of error
     * @param errorMessage optional error message for context
     */
    default void recordError(String component, String errorType, String errorMessage) {
        var tags = new java.util.HashMap<String, String>();
        tags.put("component", component);
        tags.put("error_type", errorType);
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            // Truncate long error messages
            String truncated = errorMessage.length() > 100
                ? errorMessage.substring(0, 100) + "..."
                : errorMessage;
            tags.put("error_message", truncated);
        }

        record(Metric.counter("errors.count", tags));
    }

    /**
     * Starts a timer context for measuring execution duration.
     * <p>
     * This method provides a convenient way to measure operation duration
     * using try-with-resources pattern. The timer automatically records
     * the duration when closed.
     * </p>
     *
     * @param metricName the name of the timer metric
     * @param tags optional tags to include with the metric
     * @return a TimerContext that will record timing when closed
     */
    default TimerContext startTimer(String metricName, java.util.Map<String, String> tags) {
        return new TimerContext(metricName, tags, this::record);
    }

    /**
     * Checks if this collector is currently enabled and accepting metrics.
     * <p>
     * Implementations can use this to support dynamic enable/disable of
     * metrics collection for performance tuning or debugging purposes.
     * </p>
     *
     * @return true if metrics collection is enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Flushes any buffered metrics to the underlying storage system.
     * <p>
     * This method should be called when the application is shutting down
     * or when immediate persistence of metrics is required.
     * </p>
     */
    default void flush() {
        // Default implementation does nothing
    }

    /**
     * Closes the metrics collector and releases any resources.
     * <p>
     * This method should be called during application shutdown to ensure
     * proper cleanup of any underlying connections, files, or threads.
     * </p>
     */
    default void close() {
        flush();
    }
}
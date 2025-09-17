package com.skanga.conductor.metrics;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable data transfer object representing a metric measurement.
 * <p>
 * This record encapsulates all information about a metric, including its
 * name, type, value, timestamp, and optional metadata tags. Metrics are
 * used throughout the Conductor framework to track performance, errors,
 * and operational characteristics.
 * </p>
 * <p>
 * As a record, this class is immutable and provides automatic implementations
 * of equals(), hashCode(), toString(), and accessor methods.
 * </p>
 *
 * @param name the unique name of the metric (e.g., "agent.execution.time")
 * @param type the type of metric (counter, timer, gauge, histogram)
 * @param value the numeric value of the metric measurement
 * @param timestamp when this metric was recorded
 * @param tags optional key-value pairs for categorizing metrics (e.g., agent name, tool name)
 *
 * @since 1.0.0
 * @see MetricType
 * @see MetricsCollector
 */
public record Metric(
    String name,
    MetricType type,
    double value,
    Instant timestamp,
    Map<String, String> tags
) {

    /**
     * Creates a new Metric with validation.
     *
     * @param name the metric name, must not be null or blank
     * @param type the metric type, must not be null
     * @param value the metric value
     * @param timestamp the measurement timestamp, must not be null
     * @param tags optional tags map, can be null
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public Metric {
        Objects.requireNonNull(name, "Metric name cannot be null");
        Objects.requireNonNull(type, "Metric type cannot be null");
        Objects.requireNonNull(timestamp, "Metric timestamp cannot be null");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name cannot be blank");
        }

        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Metric value cannot be NaN");
        }

        // Make defensive copy of tags if provided
        if (tags != null) {
            tags = Map.copyOf(tags);
        }
    }

    /**
     * Creates a metric with the current timestamp.
     *
     * @param name the metric name
     * @param type the metric type
     * @param value the metric value
     * @param tags optional tags
     * @return a new Metric with current timestamp
     */
    public static Metric now(String name, MetricType type, double value, Map<String, String> tags) {
        return new Metric(name, type, value, Instant.now(), tags);
    }

    /**
     * Creates a metric with the current timestamp and no tags.
     *
     * @param name the metric name
     * @param type the metric type
     * @param value the metric value
     * @return a new Metric with current timestamp and no tags
     */
    public static Metric now(String name, MetricType type, double value) {
        return new Metric(name, type, value, Instant.now(), null);
    }

    /**
     * Creates a counter metric with the current timestamp.
     *
     * @param name the metric name
     * @param value the counter value (typically 1 for increments)
     * @param tags optional tags
     * @return a new counter Metric
     */
    public static Metric counter(String name, double value, Map<String, String> tags) {
        return now(name, MetricType.COUNTER, value, tags);
    }

    /**
     * Creates a counter metric with value 1 and current timestamp.
     *
     * @param name the metric name
     * @param tags optional tags
     * @return a new counter Metric with value 1
     */
    public static Metric counter(String name, Map<String, String> tags) {
        return counter(name, 1.0, tags);
    }

    /**
     * Creates a timer metric with the current timestamp.
     *
     * @param name the metric name
     * @param durationMs the duration in milliseconds
     * @param tags optional tags
     * @return a new timer Metric
     */
    public static Metric timer(String name, double durationMs, Map<String, String> tags) {
        return now(name, MetricType.TIMER, durationMs, tags);
    }

    /**
     * Creates a gauge metric with the current timestamp.
     *
     * @param name the metric name
     * @param value the gauge value
     * @param tags optional tags
     * @return a new gauge Metric
     */
    public static Metric gauge(String name, double value, Map<String, String> tags) {
        return now(name, MetricType.GAUGE, value, tags);
    }

    /**
     * Returns the tag value for the specified key.
     *
     * @param key the tag key
     * @return the tag value, or null if not found
     */
    public String getTag(String key) {
        return tags != null ? tags.get(key) : null;
    }

    /**
     * Checks if this metric has any tags.
     *
     * @return true if tags are present and non-empty
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
}
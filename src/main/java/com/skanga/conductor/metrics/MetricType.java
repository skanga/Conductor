package com.skanga.conductor.metrics;

/**
 * Enumeration of metric types supported by the Conductor metrics system.
 * <p>
 * Defines the various types of metrics that can be collected for monitoring
 * agent and tool performance, including execution counts, timing information,
 * and error tracking.
 * </p>
 *
 * @since 1.0.0
 * @see Metric
 * @see MetricsCollector
 */
public enum MetricType {
    /**
     * Counter metrics track cumulative totals that only increase.
     * Examples: total agent executions, total tool calls, error counts.
     */
    COUNTER,

    /**
     * Timer metrics measure the duration of operations.
     * Examples: agent execution time, tool execution time.
     */
    TIMER,

    /**
     * Gauge metrics represent instantaneous values that can go up or down.
     * Examples: active agent count, memory usage, queue size.
     */
    GAUGE,

    /**
     * Histogram metrics track the distribution of values over time.
     * Examples: execution time distributions, memory usage patterns.
     */
    HISTOGRAM
}
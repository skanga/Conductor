package com.skanga.conductor.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A timing context for measuring execution duration with automatic metric recording.
 * <p>
 * This class provides a convenient way to measure the duration of operations
 * and automatically record timer metrics when the measurement is complete.
 * It follows the try-with-resources pattern for automatic cleanup.
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * try (TimerContext timer = metricsCollector.startTimer("agent.execution", tags)) {
 *     // Execute agent operation
 * } // Timer automatically records duration when closed
 * </pre>
 * </p>
 * <p>
 * Thread Safety: This class is not thread-safe. Each timer context should be
 * used by a single thread for the duration of the measurement.
 * </p>
 *
 * @since 1.0.0
 * @see MetricsCollector
 * @see Metric
 */
public class TimerContext implements AutoCloseable {

    private final String metricName;
    private final Map<String, String> tags;
    private final Consumer<Metric> metricRecorder;
    private final Instant startTime;
    private boolean closed = false;


    /**
     * Creates a new timer context with a custom metric recorder.
     *
     * @param metricName the name of the timer metric to record
     * @param tags optional tags to include with the metric
     * @param metricRecorder function to record the metric
     */
    public TimerContext(String metricName, Map<String, String> tags, Consumer<Metric> metricRecorder) {
        this.metricName = metricName;
        this.tags = tags;
        this.metricRecorder = metricRecorder;
        this.startTime = Instant.now();
    }

    /**
     * Gets the elapsed time since this timer was started.
     *
     * @return the elapsed duration
     */
    public Duration getElapsed() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Gets the elapsed time in milliseconds.
     *
     * @return the elapsed time in milliseconds
     */
    public long getElapsedMs() {
        return getElapsed().toMillis();
    }

    /**
     * Stops the timer and records the duration metric.
     * <p>
     * This method is called automatically when using try-with-resources.
     * It can also be called manually to stop timing early.
     * </p>
     */
    @Override
    public void close() {
        if (!closed && metricRecorder != null) {
            closed = true;
            long durationMs = getElapsedMs();
            metricRecorder.accept(Metric.timer(metricName, durationMs, tags));
        }
    }

    /**
     * Records the timer metric with additional success/failure information.
     * <p>
     * This method allows recording both the timing and outcome of an operation.
     * The timer context remains open after calling this method and can be
     * used to record additional metrics if needed.
     * </p>
     *
     * @param success whether the timed operation was successful
     */
    public void recordWithSuccess(boolean success) {
        if (metricRecorder != null) {
            long durationMs = getElapsedMs();

            // Create tags that include success information
            Map<String, String> successTags = tags != null
                ? new java.util.HashMap<>(tags)
                : new java.util.HashMap<>();
            successTags.put("success", String.valueOf(success));

            metricRecorder.accept(Metric.timer(metricName, durationMs, successTags));
        }
    }

    /**
     * Checks if this timer context has been closed.
     *
     * @return true if the timer has been stopped and metric recorded
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Gets the start time of this timer.
     *
     * @return the instant when timing started
     */
    public Instant getStartTime() {
        return startTime;
    }
}
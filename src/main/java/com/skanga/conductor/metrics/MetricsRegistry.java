package com.skanga.conductor.metrics;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.utils.SingletonHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import com.skanga.conductor.config.MetricsConfig;

/**
 * Central registry for managing metrics collectors in the Conductor framework.
 * <p>
 * This singleton class provides centralized metrics collection and management,
 * allowing multiple collectors to be registered and metrics to be distributed
 * to all active collectors. It supports pattern-based metric filtering and
 * provides convenient methods for common metric operations.
 * </p>
 * <p>
 * The registry automatically initializes with a default in-memory collector
 * and can be extended with additional collectors for external monitoring
 * systems, file output, or real-time dashboards.
 * </p>
 * <p>
 * The singleton instance is created using the generic SingletonHolder pattern for optimal
 * thread safety and performance. This class is thread-safe for concurrent access.
 * </p>
 *
 * @since 1.0.0
 * @see MetricsCollector
 * @see InMemoryMetricsCollector
 */
public class MetricsRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MetricsRegistry.class);

    private static final SingletonHolder<MetricsRegistry> HOLDER =
        SingletonHolder.of(MetricsRegistry::new);
    private final List<MetricsCollector> collectors = new CopyOnWriteArrayList<>();
    private final List<Pattern> enabledPatterns = new CopyOnWriteArrayList<>();
    private final List<Pattern> disabledPatterns = new CopyOnWriteArrayList<>();
    private final boolean enabled;

    private MetricsRegistry() {
        MetricsConfig config = ApplicationConfig.getInstance().getMetricsConfig();
        this.enabled = config.isEnabled();

        if (enabled) {
            // Initialize enabled/disabled patterns
            initializePatterns(config);

            // Register default in-memory collector
            registerDefaultCollectors();

            logger.debug("MetricsRegistry initialized with {} collectors", collectors.size());
        } else {
            logger.debug("MetricsRegistry disabled by configuration");
        }
    }

    /**
     * Returns the singleton instance of the metrics registry.
     * <p>
     * This method uses the generic SingletonHolder pattern for optimal
     * thread safety and performance. The instance is created only once
     * and subsequent calls return the same instance.
     * </p>
     *
     * @return the singleton MetricsRegistry instance
     */
    public static MetricsRegistry getInstance() {
        return HOLDER.get();
    }

    /**
     * Resets the singleton instance for testing purposes.
     * <p>
     * This method should only be used in test scenarios where you need
     * to reset the metrics registry state between tests. It will properly
     * shutdown the current instance before resetting.
     * </p>
     */
    public static void resetInstance() {
        MetricsRegistry current = HOLDER.getCurrentInstance();
        if (current != null) {
            current.shutdown();
        }
        HOLDER.reset();
    }

    /**
     * Registers a metrics collector with the registry.
     *
     * @param collector the collector to register, must not be null
     * @throws IllegalArgumentException if collector is null
     */
    public void register(MetricsCollector collector) {
        if (collector == null) {
            throw new IllegalArgumentException("metrics collector cannot be null");
        }

        if (!collectors.contains(collector)) {
            collectors.add(collector);
            logger.debug("Registered metrics collector: {}", collector.getClass().getSimpleName());
        }
    }

    /**
     * Unregisters a metrics collector from the registry.
     *
     * @param collector the collector to unregister
     * @return true if the collector was removed
     */
    public boolean unregister(MetricsCollector collector) {
        boolean removed = collectors.remove(collector);
        if (removed) {
            logger.debug("Unregistered metrics collector: {}", collector.getClass().getSimpleName());
        }
        return removed;
    }

    /**
     * Records a metric to all registered collectors.
     * <p>
     * The metric will be filtered based on enabled/disabled patterns before
     * being sent to collectors. If metrics collection is disabled, this
     * method returns immediately without processing.
     * </p>
     *
     * @param metric the metric to record
     */
    public void record(Metric metric) {
        if (!enabled || metric == null || !isMetricEnabled(metric.name())) {
            return;
        }

        for (MetricsCollector collector : collectors) {
            try {
                if (collector.isEnabled()) {
                    collector.record(metric);
                }
            } catch (Exception e) {
                logger.warn("Error recording metric {} to collector {}: {}",
                    metric.name(), collector.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Records agent execution metrics.
     *
     * @param agentName the name of the agent
     * @param durationMs execution duration in milliseconds
     * @param success whether the execution was successful
     */
    public void recordAgentExecution(String agentName, long durationMs, boolean success) {
        if (!enabled) return;

        // Reuse tag map for both metrics to reduce allocation
        Map<String, String> tags = Map.of(
            "agent", agentName,
            "success", String.valueOf(success)
        );

        record(Metric.timer("agent.execution.duration", durationMs, tags));
        record(Metric.counter("agent.execution.count", tags));

        if (!success) {
            // Reuse single-entry map for error metric
            record(Metric.counter("agent.execution.errors", Map.of("agent", agentName)));
        }
    }

    /**
     * Records tool execution metrics.
     *
     * @param toolName the name of the tool
     * @param durationMs execution duration in milliseconds
     * @param success whether the execution was successful
     */
    public void recordToolExecution(String toolName, long durationMs, boolean success) {
        if (!enabled) return;

        // Reuse tag map for both metrics to reduce allocation
        Map<String, String> tags = Map.of(
            "tool", toolName,
            "success", String.valueOf(success)
        );

        record(Metric.timer("tool.execution.duration", durationMs, tags));
        record(Metric.counter("tool.execution.count", tags));

        if (!success) {
            // Reuse single-entry map for error metric
            record(Metric.counter("tool.execution.errors", Map.of("tool", toolName)));
        }
    }

    /**
     * Records an error metric.
     *
     * @param component the component where the error occurred
     * @param errorType the type of error
     * @param errorMessage optional error message
     */
    public void recordError(String component, String errorType, String errorMessage) {
        if (!enabled) return;

        for (MetricsCollector collector : collectors) {
            try {
                if (collector.isEnabled()) {
                    collector.recordError(component, errorType, errorMessage);
                }
            } catch (Exception e) {
                logger.warn("Error recording error metric to collector {}: {}",
                    collector.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Starts a timer for measuring execution duration.
     *
     * @param metricName the name of the timer metric
     * @param tags optional tags to include
     * @return a TimerContext for measuring duration
     */
    public TimerContext startTimer(String metricName, Map<String, String> tags) {
        if (!enabled || !isMetricEnabled(metricName)) {
            // Return a no-op timer if metrics are disabled
            return new NoOpTimerContext();
        }

        return new TimerContext(metricName, tags, this::record);
    }

    /**
     * Gets all registered collectors.
     *
     * @return list of registered collectors
     */
    public List<MetricsCollector> getCollectors() {
        return List.copyOf(collectors);
    }

    /**
     * Gets the default in-memory collector if available.
     *
     * @return the in-memory collector, or null if not found
     */
    public InMemoryMetricsCollector getInMemoryCollector() {
        return collectors.stream()
            .filter(c -> c instanceof InMemoryMetricsCollector)
            .map(c -> (InMemoryMetricsCollector) c)
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if metrics collection is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Flushes all registered collectors.
     */
    public void flush() {
        for (MetricsCollector collector : collectors) {
            try {
                collector.flush();
            } catch (Exception e) {
                logger.warn("Error flushing collector {}: {}",
                    collector.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Shuts down the metrics registry and all collectors.
     */
    public void shutdown() {
        for (MetricsCollector collector : collectors) {
            try {
                collector.close();
            } catch (Exception e) {
                logger.warn("Error closing collector {}: {}",
                    collector.getClass().getSimpleName(), e.getMessage());
            }
        }
        collectors.clear();
        logger.debug("MetricsRegistry shutdown complete");
    }

    private void initializePatterns(MetricsConfig config) {
        // Convert enabled patterns to compiled regex patterns
        for (String pattern : config.getEnabledMetrics()) {
            try {
                enabledPatterns.add(Pattern.compile(pattern.replace("*", ".*")));
            } catch (Exception e) {
                logger.warn("Invalid enabled metrics pattern: {}", pattern);
            }
        }

        // Convert disabled patterns to compiled regex patterns
        for (String pattern : config.getDisabledMetrics()) {
            try {
                disabledPatterns.add(Pattern.compile(pattern.replace("*", ".*")));
            } catch (Exception e) {
                logger.warn("Invalid disabled metrics pattern: {}", pattern);
            }
        }
    }

    private void registerDefaultCollectors() {
        // Always register in-memory collector for basic functionality
        register(new InMemoryMetricsCollector());

        // Additional collectors can be registered here based on configuration
        // For example: file-based collector, console reporter, etc.
    }

    private boolean isMetricEnabled(String metricName) {
        // Check disabled patterns first (they take precedence)
        for (Pattern pattern : disabledPatterns) {
            if (pattern.matcher(metricName).matches()) {
                return false;
            }
        }

        // If no enabled patterns are configured, allow all metrics
        if (enabledPatterns.isEmpty()) {
            return true;
        }

        // Check enabled patterns
        for (Pattern pattern : enabledPatterns) {
            if (pattern.matcher(metricName).matches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * No-op timer context for when metrics are disabled.
     */
    private static class NoOpTimerContext extends TimerContext {
        public NoOpTimerContext() {
            super("noop", null, null);
        }

        @Override
        public void close() {
            // Do nothing
        }

        @Override
        public void recordWithSuccess(boolean success) {
            // Do nothing
        }
    }

    /**
     * Timer context that records to the registry.
     */
    private static class RegistryTimerContext extends TimerContext {
        public RegistryTimerContext(String metricName, Map<String, String> tags, MetricsRegistry registry) {
            super(metricName, tags, registry::record);
        }
    }
}
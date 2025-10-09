package com.skanga.conductor.metrics;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.MetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * In-memory implementation of MetricsCollector for development and testing.
 * <p>
 * This collector stores all metrics in memory with thread-safe access patterns.
 * It provides basic aggregation capabilities and metric retention management.
 * For production environments, consider using external monitoring systems
 * like Prometheus, InfluxDB, or CloudWatch.
 * </p>
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Thread-safe metric storage using concurrent collections</li>
 * <li>Configurable metric retention period</li>
 * <li>Basic aggregation for counters and timers</li>
 * <li>Memory usage monitoring and cleanup</li>
 * <li>Query capabilities for dashboards and reports</li>
 * </ul>
 * <p>
 * Thread Safety: This class is fully thread-safe for concurrent access.
 * </p>
 *
 * @since 1.0.0
 * @see MetricsCollector
 * @see Metric
 */
public class InMemoryMetricsCollector implements MetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryMetricsCollector.class);

    private final List<Metric> allMetrics = new CopyOnWriteArrayList<>();
    private final Map<String, MetricSummary> summaries = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final long retentionPeriodMs;
    private final int maxMetrics;
    private final boolean enabled;

    /**
     * Creates a new in-memory metrics collector with configuration from ApplicationConfig.
     */
    public InMemoryMetricsCollector() {
        MetricsConfig config = ApplicationConfig.getInstance().getMetricsConfig();
        this.retentionPeriodMs = config.getRetentionPeriod().toMillis();
        this.maxMetrics = config.getMaxMetricsInMemory();
        this.enabled = config.isEnabled();
    }

    /**
     * Creates a new in-memory metrics collector with custom settings.
     *
     * @param retentionPeriodMs how long to keep metrics in memory (milliseconds)
     * @param maxMetrics maximum number of metrics to store
     * @param enabled whether metrics collection is enabled
     */
    public InMemoryMetricsCollector(long retentionPeriodMs, int maxMetrics, boolean enabled) {
        this.retentionPeriodMs = retentionPeriodMs;
        this.maxMetrics = maxMetrics;
        this.enabled = enabled;
    }

    @Override
    public void record(Metric metric) {
        if (!enabled || metric == null) {
            return;
        }

        try {
            // Add to raw metrics list
            allMetrics.add(metric);

            // Update aggregated summaries
            updateSummary(metric);

            // Clean up old metrics periodically
            if (allMetrics.size() % 1000 == 0) {
                cleanupOldMetrics();
            }

            // Enforce maximum metrics limit
            if (allMetrics.size() > maxMetrics) {
                trimOldestMetrics();
            }

        } catch (Exception e) {
            logger.warn("Failed to record metric {}: {}", metric.name(), e.getMessage());
        }
    }

    @Override
    public TimerContext startTimer(String metricName, Map<String, String> tags) {
        return new TimerContext(metricName, tags, this::record);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets all metrics recorded within the specified time range.
     *
     * @param since the earliest timestamp to include
     * @param until the latest timestamp to include
     * @return list of metrics in the time range
     */
    public List<Metric> getMetrics(Instant since, Instant until) {
        lock.readLock().lock();
        try {
            return allMetrics.stream()
                .filter(m -> !m.timestamp().isBefore(since) && !m.timestamp().isAfter(until))
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all metrics for a specific metric name.
     *
     * @param metricName the name of the metric to retrieve
     * @return list of metrics with the specified name
     */
    public List<Metric> getMetricsByName(String metricName) {
        lock.readLock().lock();
        try {
            return allMetrics.stream()
                .filter(m -> m.name().equals(metricName))
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets aggregated summary for a specific metric.
     *
     * @param metricName the name of the metric
     * @return metric summary, or null if not found
     */
    public MetricSummary getSummary(String metricName) {
        return summaries.get(metricName);
    }

    /**
     * Gets all metric summaries.
     * Returns an unmodifiable view to avoid defensive copying.
     *
     * @return map of metric names to their summaries
     */
    public Map<String, MetricSummary> getAllSummaries() {
        return Collections.unmodifiableMap(summaries);
    }

    /**
     * Gets the total number of metrics stored.
     *
     * @return total metric count
     */
    public int getMetricCount() {
        return allMetrics.size();
    }

    /**
     * Gets metrics with the highest values for a given metric name.
     *
     * @param metricName the metric name to analyze
     * @param limit maximum number of results to return
     * @return list of metrics sorted by value (highest first)
     */
    public List<Metric> getTopMetrics(String metricName, int limit) {
        lock.readLock().lock();
        try {
            return allMetrics.stream()
                .filter(m -> m.name().equals(metricName))
                .sorted((a, b) -> Double.compare(b.value(), a.value()))
                .limit(limit)
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all stored metrics and summaries.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            allMetrics.clear();
            summaries.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void updateSummary(Metric metric) {
        summaries.compute(metric.name(), (key, existing) -> {
            if (existing == null) {
                return new MetricSummary(metric);
            } else {
                return existing.update(metric);
            }
        });
    }

    private void cleanupOldMetrics() {
        if (retentionPeriodMs <= 0) {
            return;
        }

        Instant cutoff = Instant.now().minus(retentionPeriodMs, ChronoUnit.MILLIS);
        lock.writeLock().lock();
        try {
            int originalSize = allMetrics.size();
            allMetrics.removeIf(metric -> metric.timestamp().isBefore(cutoff));
            int removedCount = originalSize - allMetrics.size();

            if (removedCount > 0) {
                logger.debug("Cleaned up {} old metrics (retention cutoff: {})", removedCount, cutoff);
                // Rebuild summaries after cleanup
                rebuildSummaries();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void trimOldestMetrics() {
        lock.writeLock().lock();
        try {
            int excess = allMetrics.size() - maxMetrics;
            if (excess > 0) {
                // Remove oldest metrics first
                allMetrics.sort(Comparator.comparing(Metric::timestamp));
                for (int i = 0; i < excess; i++) {
                    allMetrics.remove(0);
                }
                logger.debug("Trimmed {} oldest metrics to stay within limit of {}", excess, maxMetrics);
                rebuildSummaries();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void rebuildSummaries() {
        summaries.clear();
        for (Metric metric : allMetrics) {
            updateSummary(metric);
        }
    }

    /**
     * Aggregated summary statistics for a metric.
     */
    public static class MetricSummary {
        private final String name;
        private final MetricType type;
        private long count;
        private double sum;
        private double min;
        private double max;
        private Instant firstSeen;
        private Instant lastSeen;

        public MetricSummary(Metric metric) {
            this.name = metric.name();
            this.type = metric.type();
            this.count = 1;
            this.sum = metric.value();
            this.min = metric.value();
            this.max = metric.value();
            this.firstSeen = metric.timestamp();
            this.lastSeen = metric.timestamp();
        }

        public MetricSummary update(Metric metric) {
            if (!metric.name().equals(this.name)) {
                throw new IllegalArgumentException("Metric name mismatch: " + metric.name() + " vs " + this.name);
            }

            this.count++;
            this.sum += metric.value();
            this.min = Math.min(this.min, metric.value());
            this.max = Math.max(this.max, metric.value());

            if (metric.timestamp().isBefore(this.firstSeen)) {
                this.firstSeen = metric.timestamp();
            }
            if (metric.timestamp().isAfter(this.lastSeen)) {
                this.lastSeen = metric.timestamp();
            }

            return this;
        }

        public String getName() { return name; }
        public MetricType getType() { return type; }
        public long getCount() { return count; }
        public double getSum() { return sum; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getAverage() { return count > 0 ? sum / count : 0.0; }
        public Instant getFirstSeen() { return firstSeen; }
        public Instant getLastSeen() { return lastSeen; }

        @Override
        public String toString() {
            return String.format("MetricSummary{name='%s', type=%s, count=%d, avg=%.2f, min=%.2f, max=%.2f}",
                name, type, count, getAverage(), min, max);
        }
    }
}
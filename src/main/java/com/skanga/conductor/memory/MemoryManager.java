package com.skanga.conductor.memory;

import com.skanga.conductor.utils.ValidationUtils;
import com.skanga.conductor.metrics.MetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized memory management and cleanup system for the Conductor framework.
 * <p>
 * This class provides comprehensive memory management capabilities including:
 * </p>
 * <ul>
 * <li>Automatic garbage collection triggering based on memory thresholds</li>
 * <li>Resource cleanup registration and execution</li>
 * <li>Memory usage monitoring and alerting</li>
 * <li>Periodic cleanup of expired resources</li>
 * <li>Emergency cleanup during memory pressure</li>
 * <li>Metrics collection for memory usage patterns</li>
 * </ul>
 * <p>
 * The memory manager runs background tasks to monitor memory usage and
 * automatically clean up resources when necessary. It integrates with the
 * metrics system to provide visibility into memory usage patterns.
 * </p>
 * <p>
 * Thread Safety: This class is fully thread-safe for concurrent access.
 * </p>
 *
 * @since 1.0.0
 * @see AutoCloseable
 * @see MemoryStore
 */
public class MemoryManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);

    // Memory thresholds
    private static final double WARNING_THRESHOLD = 0.70; // 70%
    private static final double CRITICAL_THRESHOLD = 0.85; // 85%
    private static final double EMERGENCY_THRESHOLD = 0.95; // 95%

    // Cleanup intervals
    private static final long MONITORING_INTERVAL_MS = 30_000; // 30 seconds
    private static final long CLEANUP_INTERVAL_MS = 300_000; // 5 minutes
    private static final long RESOURCE_EXPIRY_MS = 3600_000; // 1 hour

    private final MemoryMXBean memoryBean;
    private final MetricsRegistry metricsRegistry;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());

    // Tracked resources for cleanup
    private final Map<String, CleanupTask> cleanupTasks = new ConcurrentHashMap<>();
    private final Map<String, WeakReference<AutoCloseable>> weakReferences = new ConcurrentHashMap<>();
    private final Queue<TimestampedResource> expirableResources = new ConcurrentLinkedQueue<>();

    // Memory usage tracking
    private volatile double lastMemoryUsage = 0.0;
    private volatile MemoryState currentState = MemoryState.NORMAL;

    /**
     * Memory usage states.
     */
    public enum MemoryState {
        NORMAL("Normal operation"),
        WARNING("Memory usage above warning threshold"),
        CRITICAL("Memory usage above critical threshold"),
        EMERGENCY("Memory usage above emergency threshold");

        private final String description;

        MemoryState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Creates a new MemoryManager instance.
     */
    public MemoryManager() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.metricsRegistry = MetricsRegistry.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "MemoryManager");
            t.setDaemon(true);
            return t;
        });

        startBackgroundTasks();
        logger.info("MemoryManager initialized with monitoring enabled");
    }

    /**
     * Registers a cleanup task that will be executed during memory cleanup.
     * <p>
     * Cleanup tasks are executed in order of registration during memory pressure
     * or scheduled cleanup cycles.
     * </p>
     *
     * @param name unique name for the cleanup task
     * @param task the cleanup task to execute
     * @throws IllegalArgumentException if name is null/blank or task is null
     */
    public void registerCleanupTask(String name, CleanupTask task) {
        ValidationUtils.requireNonBlank(name, "task name");
        ValidationUtils.requireNonNull(task, "cleanup task");

        cleanupTasks.put(name, task);
        logger.debug("Registered cleanup task: {}", name);
    }

    /**
     * Unregisters a cleanup task.
     *
     * @param name the name of the task to unregister
     * @return true if a task was removed
     */
    public boolean unregisterCleanupTask(String name) {
        ValidationUtils.requireNonBlank(name, "task name");
        boolean removed = cleanupTasks.remove(name) != null;
        if (removed) {
            logger.debug("Unregistered cleanup task: {}", name);
        }
        return removed;
    }

    /**
     * Registers a resource for automatic cleanup using weak references.
     * <p>
     * The resource will be automatically closed when it becomes eligible
     * for garbage collection or during explicit cleanup cycles.
     * </p>
     *
     * @param name unique name for the resource
     * @param resource the resource to track
     * @throws IllegalArgumentException if name is null/blank or resource is null
     */
    public void registerResource(String name, AutoCloseable resource) {
        ValidationUtils.requireNonBlank(name, "resource name");
        ValidationUtils.requireNonNull(resource, "resource");

        weakReferences.put(name, new WeakReference<>(resource));
        logger.debug("Registered weak reference for resource: {}", name);
    }

    /**
     * Registers a resource with an expiration time.
     * <p>
     * The resource will be automatically cleaned up after the specified
     * expiration time, even if it's still referenced.
     * </p>
     *
     * @param resource the resource to track
     * @param expirationTime when the resource should expire
     * @throws IllegalArgumentException if resource is null or expiration is in the past
     */
    public void registerExpirableResource(AutoCloseable resource, Instant expirationTime) {
        ValidationUtils.requireNonNull(resource, "resource");
        ValidationUtils.requireNonNull(expirationTime, "expiration time");

        if (expirationTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("expiration time cannot be in the past");
        }

        expirableResources.offer(new TimestampedResource(resource, expirationTime));
        logger.debug("Registered expirable resource with expiration: {}", expirationTime);
    }

    /**
     * Manually triggers memory cleanup.
     * <p>
     * This method executes all registered cleanup tasks and performs
     * garbage collection. It can be called manually when memory pressure
     * is detected by the application.
     * </p>
     *
     * @param aggressive if true, performs more aggressive cleanup
     * @return the amount of memory freed (estimated in bytes)
     */
    public long performCleanup(boolean aggressive) {
        logger.info("Performing {} memory cleanup", aggressive ? "aggressive" : "standard");

        long startTime = System.currentTimeMillis();
        double memoryBefore = getCurrentMemoryUsage();

        try {
            // Clean up expired resources first
            cleanupExpiredResources();

            // Clean up weak references
            cleanupWeakReferences();

            // Execute registered cleanup tasks
            executeCleanupTasks(aggressive);

            // Suggest garbage collection
            if (aggressive) {
                System.gc();
                Thread.sleep(100); // Give GC time to run
            }

            double memoryAfter = getCurrentMemoryUsage();
            long duration = System.currentTimeMillis() - startTime;
            long memoryFreed = estimateMemoryFreed(memoryBefore, memoryAfter);

            logger.info("Memory cleanup completed in {}ms. Memory usage: {:.1f}% -> {:.1f}% (freed ~{}MB)",
                duration, memoryBefore * 100, memoryAfter * 100, memoryFreed / (1024 * 1024));

            // Record metrics
            recordCleanupMetrics(duration, memoryFreed, aggressive);

            lastCleanupTime.set(System.currentTimeMillis());
            return memoryFreed;

        } catch (Exception e) {
            logger.error("Error during memory cleanup", e);
            return 0;
        }
    }

    /**
     * Gets the current memory usage as a percentage (0.0 to 1.0).
     *
     * @return current memory usage percentage
     */
    public double getCurrentMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();

        if (max <= 0) {
            // Use committed memory if max is not available
            max = heapUsage.getCommitted();
        }

        return max > 0 ? (double) used / max : 0.0;
    }

    /**
     * Gets the current memory state based on usage thresholds.
     *
     * @return the current memory state
     */
    public MemoryState getCurrentMemoryState() {
        return currentState;
    }

    /**
     * Gets memory usage statistics.
     *
     * @return current memory statistics
     */
    public MemoryStats getMemoryStats() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        return new MemoryStats(
            heapUsage.getUsed(),
            heapUsage.getCommitted(),
            heapUsage.getMax(),
            nonHeapUsage.getUsed(),
            getCurrentMemoryUsage(),
            currentState,
            cleanupTasks.size(),
            weakReferences.size(),
            expirableResources.size(),
            lastCleanupTime.get()
        );
    }

    /**
     * Checks if memory usage is above the critical threshold.
     *
     * @return true if memory usage is critical
     */
    public boolean isMemoryPressureHigh() {
        return getCurrentMemoryUsage() >= CRITICAL_THRESHOLD;
    }

    /**
     * Forces an emergency cleanup if memory usage is critically high.
     * <p>
     * This method should be called when the application detects memory pressure
     * and needs immediate cleanup to prevent OutOfMemoryError.
     * </p>
     */
    public void emergencyCleanup() {
        double memoryUsage = getCurrentMemoryUsage();
        if (memoryUsage >= EMERGENCY_THRESHOLD) {
            logger.warn("Emergency memory cleanup triggered - memory usage: {:.1f}%", memoryUsage * 100);
            performCleanup(true);
        }
    }

    @Override
    public void close() {
        running.set(false);

        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            // Perform final cleanup
            performCleanup(false);

            logger.info("MemoryManager shutdown completed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("MemoryManager shutdown interrupted", e);
        }
    }

    /**
     * Starts background monitoring and cleanup tasks.
     */
    private void startBackgroundTasks() {
        // Memory monitoring task
        scheduler.scheduleAtFixedRate(
            this::monitorMemoryUsage,
            MONITORING_INTERVAL_MS,
            MONITORING_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        // Periodic cleanup task
        scheduler.scheduleAtFixedRate(
            () -> performCleanup(false),
            CLEANUP_INTERVAL_MS,
            CLEANUP_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Monitors memory usage and updates state.
     */
    private void monitorMemoryUsage() {
        if (!running.get()) {
            return;
        }

        try {
            double memoryUsage = getCurrentMemoryUsage();
            lastMemoryUsage = memoryUsage;

            MemoryState newState = determineMemoryState(memoryUsage);
            if (newState != currentState) {
                logger.info("Memory state changed: {} -> {} (usage: {:.1f}%)",
                    currentState, newState, memoryUsage * 100);
                currentState = newState;
            }

            // Record memory usage metrics
            metricsRegistry.record(
                com.skanga.conductor.metrics.Metric.gauge("memory.usage.heap.percentage", memoryUsage, Map.of())
            );

            // Trigger cleanup if memory usage is high
            if (memoryUsage >= CRITICAL_THRESHOLD) {
                long timeSinceLastCleanup = System.currentTimeMillis() - lastCleanupTime.get();
                if (timeSinceLastCleanup > 60_000) { // Don't cleanup too frequently
                    performCleanup(memoryUsage >= EMERGENCY_THRESHOLD);
                }
            }

        } catch (Exception e) {
            logger.debug("Error monitoring memory usage", e);
        }
    }

    /**
     * Determines memory state based on usage percentage.
     */
    private MemoryState determineMemoryState(double memoryUsage) {
        if (memoryUsage >= EMERGENCY_THRESHOLD) {
            return MemoryState.EMERGENCY;
        } else if (memoryUsage >= CRITICAL_THRESHOLD) {
            return MemoryState.CRITICAL;
        } else if (memoryUsage >= WARNING_THRESHOLD) {
            return MemoryState.WARNING;
        } else {
            return MemoryState.NORMAL;
        }
    }

    /**
     * Cleans up expired resources.
     */
    private void cleanupExpiredResources() {
        Instant now = Instant.now();
        int cleanedCount = 0;

        TimestampedResource resource;
        while ((resource = expirableResources.peek()) != null) {
            if (resource.expirationTime.isAfter(now)) {
                break; // Resources are ordered by expiration time
            }

            expirableResources.poll();
            try {
                resource.resource.close();
                cleanedCount++;
            } catch (Exception e) {
                logger.debug("Error closing expired resource", e);
            }
        }

        if (cleanedCount > 0) {
            logger.debug("Cleaned up {} expired resources", cleanedCount);
        }
    }

    /**
     * Cleans up weak references that have been garbage collected.
     */
    private void cleanupWeakReferences() {
        int cleanedCount = 0;
        Iterator<Map.Entry<String, WeakReference<AutoCloseable>>> iterator =
            weakReferences.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, WeakReference<AutoCloseable>> entry = iterator.next();
            AutoCloseable resource = entry.getValue().get();

            if (resource == null) {
                iterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            logger.debug("Cleaned up {} garbage collected weak references", cleanedCount);
        }
    }

    /**
     * Executes all registered cleanup tasks.
     */
    private void executeCleanupTasks(boolean aggressive) {
        for (Map.Entry<String, CleanupTask> entry : cleanupTasks.entrySet()) {
            try {
                entry.getValue().cleanup(aggressive);
            } catch (Exception e) {
                logger.warn("Error executing cleanup task '{}': {}", entry.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Estimates memory freed by comparing before and after usage.
     */
    private long estimateMemoryFreed(double memoryBefore, double memoryAfter) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long maxMemory = heapUsage.getMax();
        if (maxMemory <= 0) {
            maxMemory = heapUsage.getCommitted();
        }

        double memoryDiff = memoryBefore - memoryAfter;
        return (long) (memoryDiff * maxMemory);
    }

    /**
     * Records cleanup metrics.
     */
    private void recordCleanupMetrics(long duration, long memoryFreed, boolean aggressive) {
        Map<String, String> tags = Map.of("type", aggressive ? "aggressive" : "standard");

        metricsRegistry.record(
            com.skanga.conductor.metrics.Metric.timer("memory.cleanup.duration", duration, tags)
        );
        metricsRegistry.record(
            com.skanga.conductor.metrics.Metric.gauge("memory.cleanup.freed", memoryFreed, tags)
        );
        metricsRegistry.record(
            com.skanga.conductor.metrics.Metric.counter("memory.cleanup.count", tags)
        );
    }

    /**
     * Functional interface for cleanup tasks.
     */
    @FunctionalInterface
    public interface CleanupTask {
        /**
         * Executes the cleanup task.
         *
         * @param aggressive true if aggressive cleanup should be performed
         * @throws Exception if cleanup fails
         */
        void cleanup(boolean aggressive) throws Exception;
    }

    /**
     * Resource with expiration timestamp.
     */
    private static class TimestampedResource {
        final AutoCloseable resource;
        final Instant expirationTime;

        TimestampedResource(AutoCloseable resource, Instant expirationTime) {
            this.resource = resource;
            this.expirationTime = expirationTime;
        }
    }

    /**
     * Memory usage statistics.
     */
    public static class MemoryStats {
        private final long heapUsed;
        private final long heapCommitted;
        private final long heapMax;
        private final long nonHeapUsed;
        private final double usagePercentage;
        private final MemoryState state;
        private final int cleanupTaskCount;
        private final int weakReferenceCount;
        private final int expirableResourceCount;
        private final long lastCleanupTime;

        public MemoryStats(long heapUsed, long heapCommitted, long heapMax, long nonHeapUsed,
                          double usagePercentage, MemoryState state, int cleanupTaskCount,
                          int weakReferenceCount, int expirableResourceCount, long lastCleanupTime) {
            this.heapUsed = heapUsed;
            this.heapCommitted = heapCommitted;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.usagePercentage = usagePercentage;
            this.state = state;
            this.cleanupTaskCount = cleanupTaskCount;
            this.weakReferenceCount = weakReferenceCount;
            this.expirableResourceCount = expirableResourceCount;
            this.lastCleanupTime = lastCleanupTime;
        }

        // Getters
        public long getHeapUsed() { return heapUsed; }
        public long getHeapCommitted() { return heapCommitted; }
        public long getHeapMax() { return heapMax; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public double getUsagePercentage() { return usagePercentage; }
        public MemoryState getState() { return state; }
        public int getCleanupTaskCount() { return cleanupTaskCount; }
        public int getWeakReferenceCount() { return weakReferenceCount; }
        public int getExpirableResourceCount() { return expirableResourceCount; }
        public long getLastCleanupTime() { return lastCleanupTime; }

        @Override
        public String toString() {
            return String.format(
                "MemoryStats{heapUsed=%dMB, heapMax=%dMB, usage=%.1f%%, state=%s, tasks=%d, refs=%d, expirable=%d}",
                heapUsed / (1024 * 1024), heapMax / (1024 * 1024), usagePercentage * 100,
                state, cleanupTaskCount, weakReferenceCount, expirableResourceCount
            );
        }
    }
}
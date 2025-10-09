package com.skanga.conductor.memory;

import com.skanga.conductor.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks resources for automatic cleanup.
 * <p>
 * Manages three types of resource tracking:
 * </p>
 * <ul>
 * <li>Cleanup tasks - Named tasks executed during cleanup cycles</li>
 * <li>Weak references - Resources tracked via weak references</li>
 * <li>Expirable resources - Resources with expiration timestamps</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent access.
 * </p>
 *
 * @since 2.0.0
 */
public class ResourceTracker {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTracker.class);

    // Tracked resources for cleanup
    private final Map<String, MemoryManager.CleanupTask> cleanupTasks = new ConcurrentHashMap<>();
    private final Map<String, WeakReference<AutoCloseable>> weakReferences = new ConcurrentHashMap<>();
    private final Queue<TimestampedResource> expirableResources = new ConcurrentLinkedQueue<>();

    /**
     * Registers a cleanup task.
     *
     * @param name unique name for the cleanup task
     * @param task the cleanup task to execute
     */
    public void registerCleanupTask(String name, MemoryManager.CleanupTask task) {
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
     *
     * @param name unique name for the resource
     * @param resource the resource to track
     */
    public void registerResource(String name, AutoCloseable resource) {
        ValidationUtils.requireNonBlank(name, "resource name");
        ValidationUtils.requireNonNull(resource, "resource");

        weakReferences.put(name, new WeakReference<>(resource));
        logger.debug("Registered weak reference for resource: {}", name);
    }

    /**
     * Registers a resource with an expiration time.
     *
     * @param resource the resource to track
     * @param expirationTime when the resource should expire
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
     * Cleans up expired resources.
     *
     * @return number of resources cleaned up
     */
    public int cleanupExpiredResources() {
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
        return cleanedCount;
    }

    /**
     * Cleans up weak references that have been garbage collected.
     *
     * @return number of references cleaned up
     */
    public int cleanupWeakReferences() {
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
        return cleanedCount;
    }

    /**
     * Executes all registered cleanup tasks.
     *
     * @param aggressive whether to perform aggressive cleanup
     */
    public void executeCleanupTasks(boolean aggressive) {
        for (Map.Entry<String, MemoryManager.CleanupTask> entry : cleanupTasks.entrySet()) {
            try {
                entry.getValue().cleanup(aggressive);
            } catch (Exception e) {
                logger.warn("Error executing cleanup task '{}': {}", entry.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Gets the count of registered cleanup tasks.
     */
    public int getCleanupTaskCount() {
        return cleanupTasks.size();
    }

    /**
     * Gets the count of weak references.
     */
    public int getWeakReferenceCount() {
        return weakReferences.size();
    }

    /**
     * Gets the count of expirable resources.
     */
    public int getExpirableResourceCount() {
        return expirableResources.size();
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
}

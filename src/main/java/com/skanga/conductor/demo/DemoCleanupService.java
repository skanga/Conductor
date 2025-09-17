package com.skanga.conductor.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Service for cleaning up old demo databases and maintaining demo environment hygiene.
 * <p>
 * This service provides automatic cleanup of demo databases based on age and configuration.
 * It can run as a background service to prevent accumulation of demo database files and
 * maintain optimal disk usage.
 * </p>
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Automatic cleanup of databases older than configured threshold</li>
 * <li>Scheduled background cleanup operations</li>
 * <li>Manual cleanup triggers for immediate maintenance</li>
 * <li>Safe cleanup with validation and logging</li>
 * <li>Statistics reporting for cleanup operations</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * DemoCleanupService cleanupService = new DemoCleanupService();
 * cleanupService.startScheduledCleanup(); // Start background cleanup
 *
 * // Manual cleanup
 * CleanupResult result = cleanupService.performCleanup();
 * logger.info("Cleaned up {} databases, freed {} MB",
 *     result.getDeletedCount(), result.getFreedSpaceMB());
 *
 * cleanupService.shutdown(); // Stop background service
 * </pre>
 *
 * @since 1.0.0
 * @see DemoDatabaseManager
 * @see DemoConfig
 */
public class DemoCleanupService implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DemoCleanupService.class);
    private static final DemoConfig demoConfig = DemoConfig.getInstance();

    // Pattern to match demo database files
    private static final Pattern DEMO_DB_PATTERN = Pattern.compile("demo-.*\\.(mv\\.db|trace\\.db|lock\\.db)$");

    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    /**
     * Creates a new demo cleanup service.
     */
    public DemoCleanupService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "demo-cleanup-service");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts scheduled automatic cleanup based on configuration.
     * <p>
     * If automatic cleanup is enabled, this will schedule periodic cleanup
     * operations to maintain the demo environment.
     * </p>
     */
    public void startScheduledCleanup() {
        if (!demoConfig.isAutoCleanupEnabled()) {
            logger.info("Automatic demo cleanup is disabled in configuration");
            return;
        }

        if (running) {
            logger.warn("Demo cleanup service is already running");
            return;
        }

        running = true;

        // Schedule cleanup every hour
        scheduler.scheduleAtFixedRate(this::scheduledCleanupTask,
            1, 1, TimeUnit.HOURS);

        logger.info("Started demo cleanup service with {} hour database retention",
            demoConfig.getDatabaseMaxAgeHours());
    }

    /**
     * Performs immediate cleanup of old demo databases.
     * <p>
     * This method can be called manually to trigger immediate cleanup
     * without waiting for the scheduled cleanup cycle.
     * </p>
     *
     * @return cleanup result with statistics about the operation
     */
    public CleanupResult performCleanup() {
        logger.info("Starting manual demo database cleanup");

        CleanupResult result = new CleanupResult();

        try {
            // Clean up temporary databases
            cleanupDirectory(Paths.get(demoConfig.getDemoTempDatabaseDir()), result, true);

            // Clean up old persistent databases if enabled
            if (demoConfig.isAutoCleanupEnabled()) {
                cleanupDirectory(Paths.get(demoConfig.getDemoPersistentDatabaseDir()), result, false);
            }

            logger.info("Demo cleanup completed: {} databases deleted, {} MB freed, {} errors",
                result.getDeletedCount(), result.getFreedSpaceMB(), result.getErrorCount());

        } catch (Exception e) {
            logger.error("Error during demo cleanup: {}", e.getMessage(), e);
            result.recordError("General cleanup error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Gets statistics about current demo database usage.
     *
     * @return usage statistics for demo databases
     */
    public DatabaseUsageStats getUsageStats() {
        DatabaseUsageStats stats = new DatabaseUsageStats();

        try {
            // Collect stats from temporary directory
            collectDirectoryStats(Paths.get(demoConfig.getDemoTempDatabaseDir()), stats, true);

            // Collect stats from persistent directory
            collectDirectoryStats(Paths.get(demoConfig.getDemoPersistentDatabaseDir()), stats, false);

        } catch (Exception e) {
            logger.debug("Error collecting usage statistics: {}", e.getMessage());
        }

        return stats;
    }

    /**
     * Stops the scheduled cleanup service.
     */
    public void stopScheduledCleanup() {
        if (running) {
            running = false;
            logger.info("Stopped demo cleanup service");
        }
    }

    /**
     * Shuts down the cleanup service and releases resources.
     */
    @Override
    public void close() {
        stopScheduledCleanup();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Task executed by the scheduled cleanup service.
     */
    private void scheduledCleanupTask() {
        try {
            CleanupResult result = performCleanup();
            if (result.getDeletedCount() > 0) {
                logger.info("Scheduled cleanup: {} databases cleaned, {} MB freed",
                    result.getDeletedCount(), result.getFreedSpaceMB());
            }
        } catch (Exception e) {
            logger.error("Error in scheduled cleanup task: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleans up databases in the specified directory.
     */
    private void cleanupDirectory(Path directory, CleanupResult result, boolean isTemporary) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Instant cutoffTime = Instant.now().minus(demoConfig.getDatabaseMaxAgeHours(), ChronoUnit.HOURS);

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    if (shouldDeleteFile(file, attrs, cutoffTime, isTemporary)) {
                        long size = attrs.size();
                        Files.delete(file);
                        result.recordDeletion(size);

                        if (demoConfig.isVerboseLoggingEnabled()) {
                            logger.debug("Deleted old demo database file: {}", file);
                        }
                    }
                } catch (IOException e) {
                    result.recordError("Failed to delete " + file + ": " + e.getMessage());
                    logger.warn("Failed to delete demo database file {}: {}", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                result.recordError("Failed to access " + file + ": " + exc.getMessage());
                logger.debug("Failed to access file during cleanup: {}", file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Determines if a file should be deleted based on age and type.
     */
    private boolean shouldDeleteFile(Path file, BasicFileAttributes attrs, Instant cutoffTime, boolean isTemporary) {
        // Check if it's a demo database file
        if (!DEMO_DB_PATTERN.matcher(file.getFileName().toString()).matches()) {
            return false;
        }

        // Temporary files are always cleaned up if old enough
        if (isTemporary) {
            return attrs.lastModifiedTime().toInstant().isBefore(cutoffTime);
        }

        // Persistent files are only cleaned up if auto-cleanup is enabled and they're old enough
        return demoConfig.isAutoCleanupEnabled() &&
               attrs.lastModifiedTime().toInstant().isBefore(cutoffTime);
    }

    /**
     * Collects statistics about demo database files in a directory.
     */
    private void collectDirectoryStats(Path directory, DatabaseUsageStats stats, boolean isTemporary) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (DEMO_DB_PATTERN.matcher(file.getFileName().toString()).matches()) {
                    if (isTemporary) {
                        stats.addTemporaryFile(attrs.size());
                    } else {
                        stats.addPersistentFile(attrs.size());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Result of a cleanup operation.
     */
    public static class CleanupResult {
        private int deletedCount = 0;
        private long freedBytes = 0;
        private final List<String> errors = new ArrayList<>();

        /**
         * Records a successful file deletion.
         *
         * @param size the size of the deleted file in bytes
         */
        void recordDeletion(long size) {
            deletedCount++;
            freedBytes += size;
        }

        /**
         * Records an error that occurred during cleanup.
         *
         * @param error description of the error
         */
        void recordError(String error) {
            errors.add(error);
        }

        /**
         * Gets the number of files deleted during cleanup.
         *
         * @return count of deleted files
         */
        public int getDeletedCount() { return deletedCount; }

        /**
         * Gets the total bytes freed during cleanup.
         *
         * @return bytes freed
         */
        public long getFreedBytes() { return freedBytes; }

        /**
         * Gets the total space freed during cleanup in megabytes.
         *
         * @return megabytes freed
         */
        public long getFreedSpaceMB() { return freedBytes / (1024 * 1024); }

        /**
         * Gets the number of errors that occurred during cleanup.
         *
         * @return count of errors
         */
        public int getErrorCount() { return errors.size(); }

        /**
         * Gets a copy of all errors that occurred during cleanup.
         *
         * @return list of error descriptions
         */
        public List<String> getErrors() { return new ArrayList<>(errors); }

        @Override
        public String toString() {
            return String.format("CleanupResult{deleted=%d, freedMB=%d, errors=%d}",
                deletedCount, getFreedSpaceMB(), errors.size());
        }
    }

    /**
     * Statistics about demo database usage.
     */
    public static class DatabaseUsageStats {
        private int temporaryFileCount = 0;
        private long temporaryBytes = 0;
        private int persistentFileCount = 0;
        private long persistentBytes = 0;

        /**
         * Adds a temporary database file to the statistics.
         *
         * @param size the size of the temporary file in bytes
         */
        void addTemporaryFile(long size) {
            temporaryFileCount++;
            temporaryBytes += size;
        }

        /**
         * Adds a persistent database file to the statistics.
         *
         * @param size the size of the persistent file in bytes
         */
        void addPersistentFile(long size) {
            persistentFileCount++;
            persistentBytes += size;
        }

        /**
         * Gets the count of temporary database files.
         *
         * @return number of temporary files
         */
        public int getTemporaryFileCount() { return temporaryFileCount; }

        /**
         * Gets the total size of temporary database files in bytes.
         *
         * @return temporary files size in bytes
         */
        public long getTemporaryBytes() { return temporaryBytes; }

        /**
         * Gets the total size of temporary database files in megabytes.
         *
         * @return temporary files size in MB
         */
        public long getTemporaryMB() { return temporaryBytes / (1024 * 1024); }

        /**
         * Gets the count of persistent database files.
         *
         * @return number of persistent files
         */
        public int getPersistentFileCount() { return persistentFileCount; }

        /**
         * Gets the total size of persistent database files in bytes.
         *
         * @return persistent files size in bytes
         */
        public long getPersistentBytes() { return persistentBytes; }

        /**
         * Gets the total size of persistent database files in megabytes.
         *
         * @return persistent files size in MB
         */
        public long getPersistentMB() { return persistentBytes / (1024 * 1024); }

        /**
         * Gets the total count of all database files (temporary + persistent).
         *
         * @return total number of files
         */
        public int getTotalFileCount() { return temporaryFileCount + persistentFileCount; }

        /**
         * Gets the total size of all database files in bytes.
         *
         * @return total size in bytes
         */
        public long getTotalBytes() { return temporaryBytes + persistentBytes; }

        /**
         * Gets the total size of all database files in megabytes.
         *
         * @return total size in MB
         */
        public long getTotalMB() { return getTotalBytes() / (1024 * 1024); }

        @Override
        public String toString() {
            return String.format("DatabaseUsageStats{temp=%d files (%d MB), persistent=%d files (%d MB), total=%d MB}",
                temporaryFileCount, getTemporaryMB(), persistentFileCount, getPersistentMB(), getTotalMB());
        }
    }
}
package com.skanga.conductor.demo;

import com.skanga.conductor.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages isolated database instances for agentic runs.
 * <p>
 * This class provides database isolation for each agentic run to prevent interference
 * between concurrent or sequential executions. Each run gets its own isolated database
 * instance that is automatically cleaned up after completion, ensuring clean state
 * and preventing database file accumulation.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Automatic database isolation per agentic run</li>
 * <li>Unique database paths with workflow ID and timestamp</li>
 * <li>Automatic cleanup after run completion (configurable)</li>
 * <li>Database instance tracking and management</li>
 * <li>Optional preservation for debugging purposes</li>
 * <li>Prevention of database file accumulation</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * try (DemoDatabaseManager dbManager = new DemoDatabaseManager("book-creation-run-123")) {
 *     MemoryStore store = dbManager.createIsolatedMemoryStore();
 *     // Use the isolated memory store for agentic operations
 *     // Database is automatically cleaned up after completion (unless preserved for debug)
 * }
 * </pre>
 *
 * @since 1.0.0
 * @see MemoryStore
 * @see DemoConfig
 */
public class DemoDatabaseManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DemoDatabaseManager.class);
    private static final DemoConfig demoConfig = DemoConfig.getInstance();

    // Track active database instances for cleanup
    private static final ConcurrentMap<String, DemoDatabaseManager> activeInstances = new ConcurrentHashMap<>();

    private final String workflowId;
    private final String databaseUrl;
    private final String databasePath;
    private final boolean isTemporary;
    private final List<MemoryStore> activeStores;
    private volatile boolean closed = false;

    /**
     * Creates a database manager for the specified workflow.
     *
     * @param workflowId the unique identifier for the demo workflow
     * @throws IllegalArgumentException if workflowId is null or empty
     */
    public DemoDatabaseManager(String workflowId) {
        if (workflowId == null || workflowId.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID cannot be null or empty");
        }

        this.workflowId = workflowId.trim();
        this.isTemporary = true; // All databases are now temporary by design
        this.activeStores = new ArrayList<>();

        // Generate isolated database path - all use the same pattern now
        this.databasePath = generateIsolatedDatabasePath();

        this.databaseUrl = "jdbc:h2:" + databasePath;

        // Register this instance for tracking
        activeInstances.put(workflowId, this);

        if (demoConfig.isVerboseLoggingEnabled()) {
            logger.info("Created isolated database manager for workflow '{}' at: {}",
                workflowId, databasePath);
        }
    }

    /**
     * Creates an isolated MemoryStore instance for this demo workflow.
     * <p>
     * The created MemoryStore will use the isolated database and will be
     * tracked for automatic cleanup when the manager is closed.
     * </p>
     *
     * @return a new MemoryStore instance using the isolated database
     * @throws SQLException if database connection fails
     * @throws IllegalStateException if this manager has been closed
     */
    public MemoryStore createIsolatedMemoryStore() throws SQLException {
        if (closed) {
            throw new IllegalStateException("Database manager has been closed");
        }

        // Create MemoryStore with isolated database
        MemoryStore store = new MemoryStore(databaseUrl, "sa", "");

        // Track the store for cleanup
        synchronized (activeStores) {
            activeStores.add(store);
        }

        if (demoConfig.isVerboseLoggingEnabled()) {
            logger.debug("Created isolated MemoryStore for workflow '{}' using database: {}",
                workflowId, databaseUrl);
        }

        return store;
    }

    /**
     * Gets the isolated database URL for this workflow.
     *
     * @return the JDBC URL for the isolated database
     */
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * Gets the file path to the isolated database.
     *
     * @return the file system path to the database files
     */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * Gets the workflow ID for this database manager.
     *
     * @return the workflow identifier
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Checks if this database will be cleaned up after the agentic run completes.
     * <p>
     * All databases are now designed to be cleaned up by default unless
     * specifically preserved for debugging purposes.
     * </p>
     *
     * @return true if the database will be cleaned up automatically
     */
    public boolean isTemporary() {
        return shouldCleanupDatabase();
    }

    /**
     * Gets statistics about the isolated database.
     *
     * @return database statistics including size and table counts
     */
    public DatabaseStats getStatistics() {
        if (closed) {
            return new DatabaseStats(workflowId, 0, 0, false);
        }

        try (Connection conn = DriverManager.getConnection(databaseUrl, "sa", "")) {
            return collectStatistics(conn);
        } catch (SQLException e) {
            logger.warn("Failed to collect database statistics for workflow '{}': {}",
                workflowId, e.getMessage());
            return new DatabaseStats(workflowId, 0, 0, true);
        }
    }

    /**
     * Performs cleanup of the isolated database.
     * <p>
     * The cleanup behavior depends on the configuration:
     * </p>
     * <ul>
     * <li>If cleanup is enabled or database is temporary, removes all database files</li>
     * <li>If cleanup is disabled, closes connections but preserves data</li>
     * <li>Always closes and removes tracking for active MemoryStore instances</li>
     * </ul>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            // Close all active MemoryStore instances
            closeActiveStores();

            // Perform database cleanup based on configuration
            if (shouldCleanupDatabase()) {
                cleanupDatabaseFiles();
            } else {
                logger.info("Database preservation enabled - keeping database files for workflow '{}' at: {}",
                    workflowId, databasePath);
            }

        } catch (Exception e) {
            logger.error("Error during database cleanup for workflow '{}': {}", workflowId, e.getMessage(), e);
        } finally {
            // Remove from active tracking
            activeInstances.remove(workflowId);
        }
    }

    /**
     * Cleans up all active demo databases.
     * <p>
     * This method can be called to perform global cleanup of all demo databases,
     * useful for application shutdown or maintenance operations.
     * </p>
     */
    public static void cleanupAllDemoDatabases() {
        logger.info("Performing global cleanup of {} active demo databases", activeInstances.size());

        List<DemoDatabaseManager> managersToClose = new ArrayList<>(activeInstances.values());
        for (DemoDatabaseManager manager : managersToClose) {
            try {
                manager.close();
            } catch (Exception e) {
                logger.error("Error closing database manager for workflow '{}': {}",
                    manager.getWorkflowId(), e.getMessage(), e);
            }
        }

        logger.info("Global demo database cleanup completed");
    }

    /**
     * Gets information about all currently active demo databases.
     *
     * @return list of database statistics for all active instances
     */
    public static List<DatabaseStats> getActiveDatabaseStats() {
        List<DatabaseStats> stats = new ArrayList<>();
        for (DemoDatabaseManager manager : activeInstances.values()) {
            try {
                stats.add(manager.getStatistics());
            } catch (Exception e) {
                logger.debug("Failed to get statistics for workflow '{}': {}",
                    manager.getWorkflowId(), e.getMessage());
            }
        }
        return stats;
    }

    /**
     * Generates an isolated database path for this agentic run.
     * <p>
     * Each agentic run gets its own isolated database directory with a unique
     * path that includes the workflow ID and timestamp for easy identification.
     * </p>
     */
    private String generateIsolatedDatabasePath() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String basePath = demoConfig.getDemoDatabaseBaseDir();

        return basePath + "/" + workflowId + "-" + timestamp + "-" + uuid;
    }

    /**
     * Closes all active MemoryStore instances.
     */
    private void closeActiveStores() {
        synchronized (activeStores) {
            for (MemoryStore store : activeStores) {
                try {
                    store.close();
                } catch (Exception e) {
                    logger.debug("Error closing MemoryStore: {}", e.getMessage());
                }
            }
            activeStores.clear();
        }
    }

    /**
     * Determines if database cleanup should be performed.
     * <p>
     * Cleanup is performed unless explicitly disabled for debugging purposes.
     * This ensures that each agentic run starts with a clean state and
     * prevents database file accumulation.
     * </p>
     */
    private boolean shouldCleanupDatabase() {
        return demoConfig.isAutoCleanupEnabled() && !demoConfig.isPreserveForDebug();
    }

    /**
     * Removes database files from the file system.
     */
    private void cleanupDatabaseFiles() {
        try {
            // H2 creates multiple files with different extensions
            String[] extensions = {".mv.db", ".trace.db", ".lock.db"};

            for (String ext : extensions) {
                Path filePath = Paths.get(databasePath + ext);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.debug("Deleted database file: {}", filePath);
                }
            }

            logger.info("Cleaned up database files for workflow '{}' at: {}", workflowId, databasePath);

        } catch (IOException e) {
            logger.warn("Failed to cleanup database files for workflow '{}': {}", workflowId, e.getMessage());
        }
    }

    /**
     * Collects statistics about the database.
     */
    private DatabaseStats collectStatistics(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Get table count
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");
            int tableCount = rs.next() ? rs.getInt(1) : 0;

            // Get estimated database size (simplified)
            long estimatedSize = estimateDatabaseSize(conn);

            return new DatabaseStats(workflowId, tableCount, estimatedSize, true);

        } catch (SQLException e) {
            logger.debug("Error collecting detailed statistics: {}", e.getMessage());
            return new DatabaseStats(workflowId, 0, 0, false);
        }
    }

    /**
     * Estimates the database size by counting records in key tables.
     */
    private long estimateDatabaseSize(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Count memory entries as a proxy for database size
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM AGENT_MEMORY");
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            return 0; // Table might not exist yet
        }
    }

    /**
     * Statistics about a demo database instance.
     */
    public static class DatabaseStats {
        private final String workflowId;
        private final int tableCount;
        private final long recordCount;
        private final boolean accessible;

        public DatabaseStats(String workflowId, int tableCount, long recordCount, boolean accessible) {
            this.workflowId = workflowId;
            this.tableCount = tableCount;
            this.recordCount = recordCount;
            this.accessible = accessible;
        }

        public String getWorkflowId() { return workflowId; }
        public int getTableCount() { return tableCount; }
        public long getRecordCount() { return recordCount; }
        public boolean isAccessible() { return accessible; }

        @Override
        public String toString() {
            return String.format("DatabaseStats{workflowId='%s', tables=%d, records=%d, accessible=%s}",
                workflowId, tableCount, recordCount, accessible);
        }
    }
}
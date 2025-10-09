package com.skanga.conductor.memory;

import com.skanga.conductor.utils.JsonUtils;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.TaskDefinition;
import com.skanga.conductor.config.ApplicationConfig;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.skanga.conductor.config.MemoryConfig;
import com.skanga.conductor.config.DatabaseConfig;

/**
 * Thread-safe JDBC-backed memory store for subagents.
 *
 * This class provides safe concurrent access to a relational store that
 * persists sub‑agent memory, task outputs and workflow plans.  Thread safety
 * is achieved by:
 * <ul>
 *   <li>Using an {@link org.h2.jdbcx.JdbcConnectionPool} – the pool supplies
 *       a separate {@link java.sql.Connection} for each operation, and the
 *       pool implementation is itself thread‑safe.</li>
 *   <li>Guarding schema creation with a {@link java.util.concurrent.locks.ReadWriteLock}
 *       and a {@code volatile} {@code schemaInitialized} flag (double‑checked
 *       locking).  The schema is created exactly once even when multiple
 *       threads invoke the constructor concurrently.</li>
 *   <li>All mutating methods (`addMemory`, `saveTaskOutput`, `savePlan`) obtain
 *       a fresh connection from the pool and close it via try‑with‑resources,
 *       ensuring no shared mutable JDBC objects.</li>
 *   <li>Read methods (`loadMemory`, `loadTaskOutputs`, `loadPlan`) also use
 *       independent connections, allowing concurrent reads without blocking
 *       writes.</li>
 * </ul>
 *
 * The class is immutable except for the internal schema‑initialisation state,
 * which is safely published via the volatile flag.  Consequently, instances
 * can be shared freely across threads.
 */
public class MemoryStore implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    private final DataSource dataSource;
    private final MemoryConfig memoryConfig;
    private final ReadWriteLock schemaLock = new ReentrantReadWriteLock();
    private volatile boolean schemaInitialized = false;

    public MemoryStore() throws SQLException {
        ApplicationConfig config = ApplicationConfig.getInstance();
        DatabaseConfig dbConfig = config.getDatabaseConfig();
        this.memoryConfig = config.getMemoryConfig();

        // Create connection pool for thread safety
        this.dataSource = JdbcConnectionPool.create(
                dbConfig.getJdbcUrl(),
                dbConfig.getUsername(),
                dbConfig.getPassword()
        );
        ((JdbcConnectionPool) this.dataSource).setMaxConnections(dbConfig.getMaxConnections());

        ensureSchema();
    }

    public MemoryStore(String jdbcUrl, String user, String password) throws SQLException {
        this.memoryConfig = ApplicationConfig.getInstance().getMemoryConfig();

        // Create connection pool even for deprecated constructor
        this.dataSource = JdbcConnectionPool.create(jdbcUrl, user, password);
        ((JdbcConnectionPool) this.dataSource).setMaxConnections(10); // Default max connections

        ensureSchema();
    }

    private void ensureSchema() throws SQLException {
        if (schemaInitialized) {
            return;
        }

        schemaLock.writeLock().lock();
        try {
            if (schemaInitialized) {
                return; // Double-check locking pattern
            }

            // Table: subagent_memory
            String[] sqlCommands = new String[]{"""
                    CREATE TABLE IF NOT EXISTS subagent_memory (
                      id IDENTITY PRIMARY KEY,
                      agent_name VARCHAR(255) NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      content CLOB NOT NULL
                    );
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS TASK_OUTPUTS (
                        workflow_id VARCHAR,
                        task_name VARCHAR,
                        output CLOB,
                        PRIMARY KEY (workflow_id, task_name)
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS workflow_plans (
                        workflow_id VARCHAR(255) PRIMARY KEY,
                        plan_json CLOB
                    );
                    """,
                    "CREATE INDEX IF NOT EXISTS idx_agent_name ON subagent_memory(agent_name);"};

            try (Connection conn = dataSource.getConnection()) {
                for (String sql : sqlCommands) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(sql);
                    }
                }
            }
            schemaInitialized = true;
        } finally {
            schemaLock.writeLock().unlock();
        }
    }

    /**
     * Adds a memory entry for the specified agent.
     * <p>
     * Memory entries are timestamped and stored in chronological order. They can be
     * retrieved later using {@link #loadMemory(String)} or {@link #loadMemory(String, int)}.
     * This method is thread-safe and can be called concurrently from multiple threads.
     * </p>
     *
     * @param agentName the name of the agent to add memory for (must not be null)
     * @param content the memory content to store (must not be null)
     * @throws SQLException if database operation fails
     * @see #loadMemory(String)
     * @see #loadMemory(String, int)
     */
    public void addMemory(String agentName, String content) throws SQLException {
        String insert = "INSERT INTO subagent_memory (agent_name, created_at, content) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, agentName);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, content); // Use setString instead of creating SerialClob
            ps.executeUpdate();
        }
    }

    /**
     * Loads memory entries for the specified agent with a maximum entry limit.
     * <p>
     * Entries are returned in chronological order (oldest first). This method is
     * thread-safe and can be called concurrently. An independent database connection
     * is obtained for each call, allowing concurrent reads without blocking.
     * </p>
     *
     * @param agentName the name of the agent to load memory for (must not be null)
     * @param limit the maximum number of memory entries to retrieve (must be >= 0)
     * @return a list of memory content strings in chronological order (never null)
     * @throws SQLException if database operation fails
     * @see #loadMemory(String)
     * @see #addMemory(String, String)
     */
    public List<String> loadMemory(String agentName, int limit) throws SQLException {
        String q = "SELECT content FROM subagent_memory WHERE agent_name = ? ORDER BY id ASC LIMIT ?";
        List<String> out = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, agentName);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString(1); // Direct string retrieval instead of CLOB manipulation
                    out.add(content);
                }
            }
        }
        return out;
    }

    /**
     * Loads memory entries for the specified agent using the configured maximum limit.
     * <p>
     * This is a convenience method that delegates to {@link #loadMemory(String, int)}
     * using the limit from {@code conductor.memory.max-entries} configuration.
     * </p>
     *
     * @param agentName the name of the agent to load memory for (must not be null)
     * @return a list of memory content strings in chronological order (never null)
     * @throws SQLException if database operation fails
     * @see #loadMemory(String, int)
     * @see MemoryConfig#getMaxMemoryEntries()
     */
    public List<String> loadMemory(String agentName) throws SQLException {
        return loadMemory(agentName, memoryConfig.getMaxMemoryEntries());
    }

    /**
     * Loads memory for multiple agents in a single database query to avoid N+1 queries.
     * This method is optimized for bulk memory loading when processing multiple agents.
     *
     * @param agentNames the list of agent names to load memory for
     * @param limit the maximum number of memory entries per agent
     * @return a map where keys are agent names and values are lists of memory entries
     * @throws SQLException if database operation fails
     */
    public Map<String, List<String>> loadMemoryBulk(List<String> agentNames, int limit) throws SQLException {
        if (agentNames == null || agentNames.isEmpty()) {
            return new HashMap<>();
        }

        // Validate agent names as defense-in-depth (even though we use parameterized queries)
        for (String agentName : agentNames) {
            if (agentName == null || agentName.isBlank()) {
                throw new IllegalArgumentException("Agent name cannot be null or blank");
            }
            if (agentName.length() > 255) {
                throw new IllegalArgumentException("Agent name too long: " + agentName.length() + " characters");
            }
        }

        Map<String, List<String>> result = new HashMap<>();

        // Initialize empty lists for all requested agents
        for (String agentName : agentNames) {
            result.put(agentName, new ArrayList<>());
        }

        // Build IN clause for SQL query
        String placeholders = String.join(",", Collections.nCopies(agentNames.size(), "?"));
        String query = """
            SELECT agent_name, content FROM (
                SELECT agent_name, content, ROW_NUMBER() OVER (PARTITION BY agent_name ORDER BY id ASC) as rn
                FROM subagent_memory
                WHERE agent_name IN (%s)
            ) ranked
            WHERE rn <= ?
            ORDER BY agent_name, rn
            """.formatted(placeholders);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            // Set agent name parameters
            for (int i = 0; i < agentNames.size(); i++) {
                ps.setString(i + 1, agentNames.get(i));
            }
            // Set limit parameter
            ps.setInt(agentNames.size() + 1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String agentName = rs.getString("agent_name");
                    String content = rs.getString("content");

                    List<String> agentMemories = result.get(agentName);
                    if (agentMemories != null) {
                        agentMemories.add(content);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Loads memory for multiple agents using the default memory limit.
     *
     * @param agentNames the list of agent names to load memory for
     * @return a map where keys are agent names and values are lists of memory entries
     * @throws SQLException if database operation fails
     */
    public Map<String, List<String>> loadMemoryBulk(List<String> agentNames) throws SQLException {
        return loadMemoryBulk(agentNames, memoryConfig.getMaxMemoryEntries());
    }

    /**
     * Saves or updates the output of a workflow task.
     * <p>
     * Uses a MERGE (upsert) operation to either insert a new task output or update
     * an existing one if the workflow ID and task name combination already exists.
     * This allows tasks to be re-executed with their outputs updated in place.
     * </p>
     *
     * @param workflowId the unique identifier for the workflow (must not be null)
     * @param taskName the name of the task within the workflow (must not be null)
     * @param output the output produced by the task execution
     * @throws ConductorException.MemoryStoreException if database operation fails
     * @see #loadTaskOutputs(String)
     */
    public void saveTaskOutput(String workflowId, String taskName, String output) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "MERGE INTO TASK_OUTPUTS KEY(workflow_id, task_name) VALUES(?,?,?)")) {
            ps.setString(1, workflowId);
            ps.setString(2, taskName);
            ps.setString(3, output);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConductorException.MemoryStoreException("Failed to persist task output", e);
        }
    }

    /**
     * Loads all task outputs for a specific workflow.
     * <p>
     * Returns a map where keys are task names and values are their corresponding outputs.
     * If no outputs exist for the given workflow ID, an empty map is returned.
     * </p>
     *
     * @param workflowId the unique identifier for the workflow (must not be null)
     * @return a map of task names to their outputs (never null, may be empty)
     * @throws ConductorException.MemoryStoreException if database operation fails
     * @see #saveTaskOutput(String, String, String)
     */
    public Map<String, String> loadTaskOutputs(String workflowId) {
        Map<String, String> results = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT task_name, output FROM TASK_OUTPUTS WHERE workflow_id=?")) {
            ps.setString(1, workflowId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.put(rs.getString("task_name"), rs.getString("output"));
                }
            }
        } catch (SQLException e) {
            throw new ConductorException.MemoryStoreException("Failed to load task outputs", e);
        }
        return results;
    }

    /**
     * Saves or updates a workflow execution plan.
     * <p>
     * The plan is serialized to JSON and stored in the database. Uses a MERGE (upsert)
     * operation so subsequent calls with the same workflow ID will update the existing plan.
     * This is useful for LLM-based planning systems that decompose high-level goals into
     * executable task sequences.
     * </p>
     *
     * @param workflowId the unique identifier for the workflow (must not be null)
     * @param plan the array of task definitions representing the execution plan (must not be null)
     * @throws SQLException if database operation or JSON serialization fails
     * @see #loadPlan(String)
     * @see TaskDefinition
     */
    public void savePlan(String workflowId, TaskDefinition[] plan) throws SQLException {
        // Use H2's MERGE syntax instead of MySQL's ON DUPLICATE KEY UPDATE
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("MERGE INTO workflow_plans KEY(workflow_id) VALUES (?, ?)")) {
            String json = JsonUtils.toJson(plan);
            ps.setString(1, workflowId);
            ps.setString(2, json);
            ps.executeUpdate();
        }
    }

    /**
     * Loads a previously saved workflow execution plan.
     * <p>
     * Retrieves the plan from the database and deserializes it from JSON back into
     * task definitions. Returns an empty Optional if no plan exists for the given
     * workflow ID.
     * </p>
     *
     * @param workflowId the unique identifier for the workflow (must not be null)
     * @return an Optional containing the plan array if found, or empty if no plan exists
     * @throws SQLException if database operation or JSON deserialization fails
     * @see #savePlan(String, TaskDefinition[])
     * @see TaskDefinition
     */
    public Optional<TaskDefinition[]> loadPlan(String workflowId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT plan_json FROM workflow_plans WHERE workflow_id=?")) {
            ps.setString(1, workflowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("plan_json");
                    TaskDefinition[] plan = JsonUtils.fromJson(json, TaskDefinition[].class);
                    return Optional.ofNullable(plan);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        if (dataSource == null) {
            return;
        }

        if (dataSource instanceof JdbcConnectionPool pool) {
            pool.dispose();
            logger.debug("Disposed JdbcConnectionPool");
        } else if (dataSource instanceof AutoCloseable closeable) {
            // Handle other AutoCloseable DataSource implementations
            closeable.close();
            logger.debug("Closed AutoCloseable DataSource: {}", dataSource.getClass().getName());
        } else {
            // Last resort: try reflective cleanup for common DataSource implementations
            tryReflectiveCleanup(dataSource);
        }
    }

    /**
     * Attempts to close a DataSource using reflection when it doesn't implement AutoCloseable.
     * This handles common connection pool implementations that have close/shutdown methods.
     *
     * @param ds the DataSource to close
     */
    private void tryReflectiveCleanup(DataSource ds) {
        String className = ds.getClass().getName();
        logger.warn("DataSource {} does not implement AutoCloseable, attempting reflective cleanup", className);

        // Try common method names used by connection pools
        String[] methodNames = {"close", "shutdown", "dispose"};

        for (String methodName : methodNames) {
            try {
                var method = ds.getClass().getMethod(methodName);
                method.invoke(ds);
                logger.info("Successfully closed DataSource using reflective method: {}", methodName);
                return;
            } catch (NoSuchMethodException e) {
                // Method doesn't exist, try next one
                continue;
            } catch (Exception e) {
                logger.warn("Failed to invoke {} on DataSource: {}", methodName, e.getMessage());
            }
        }

        logger.error("Unable to close DataSource of type {}. Potential resource leak!", className);
    }
}

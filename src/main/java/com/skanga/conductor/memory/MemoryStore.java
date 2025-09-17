package com.skanga.conductor.memory;

import com.google.gson.Gson;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.TaskDefinition;
import com.skanga.conductor.config.ApplicationConfig;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;

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

    private final DataSource dataSource;
    private final ApplicationConfig.MemoryConfig memoryConfig;
    private final ReadWriteLock schemaLock = new ReentrantReadWriteLock();
    private volatile boolean schemaInitialized = false;

    public MemoryStore() throws SQLException {
        ApplicationConfig config = ApplicationConfig.getInstance();
        ApplicationConfig.DatabaseConfig dbConfig = config.getDatabaseConfig();
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

    public List<String> loadMemory(String agentName) throws SQLException {
        return loadMemory(agentName, memoryConfig.getMaxMemoryEntries());
    }

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

    public void savePlan(String workflowId, TaskDefinition[] plan) throws SQLException {
        // Use H2's MERGE syntax instead of MySQL's ON DUPLICATE KEY UPDATE
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "MERGE INTO workflow_plans KEY(workflow_id) VALUES (?, ?)")) {
            String json = new Gson().toJson(plan);
            ps.setString(1, workflowId);
            ps.setString(2, json);
            ps.executeUpdate();
        }
    }

    public TaskDefinition[] loadPlan(String workflowId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT plan_json FROM workflow_plans WHERE workflow_id=?")) {
            ps.setString(1, workflowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("plan_json");
                    return new Gson().fromJson(json, TaskDefinition[].class);
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (dataSource instanceof JdbcConnectionPool) {
            ((JdbcConnectionPool) dataSource).dispose();
        }
    }
}

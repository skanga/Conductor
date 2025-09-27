package com.skanga.conductor.database;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Template class for database operations that standardizes connection management
 * and exception handling across the Conductor framework.
 * <p>
 * This class implements the Template Method pattern to provide consistent database
 * interaction patterns including:
 * </p>
 * <ul>
 * <li>Automatic connection management with proper resource cleanup</li>
 * <li>Standardized exception handling and logging</li>
 * <li>Transaction support with automatic rollback on errors</li>
 * <li>Prepared statement management for SQL injection prevention</li>
 * <li>Type-safe result mapping with functional interfaces</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * DatabaseTemplate template = new DatabaseTemplate(dataSource);
 *
 * // Query single result
 * Optional&lt;String&gt; result = template.queryForObject(
 *     "SELECT name FROM users WHERE id = ?",
 *     rs -&gt; rs.getString("name"),
 *     userId
 * );
 *
 * // Query multiple results
 * List&lt;User&gt; users = template.queryForList(
 *     "SELECT id, name FROM users WHERE status = ?",
 *     rs -&gt; new User(rs.getLong("id"), rs.getString("name")),
 *     "ACTIVE"
 * );
 *
 * // Execute update
 * int rowsAffected = template.update(
 *     "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?",
 *     userId
 * );
 * </pre>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying DataSource
 * is thread-safe (which is the case for most connection pool implementations).
 * </p>
 *
 * @since 1.0.0
 * @see DataSource
 * @see MemoryStore
 */
public class DatabaseTemplate {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTemplate.class);

    private final DataSource dataSource;

    /**
     * Creates a new DatabaseTemplate with the specified DataSource.
     *
     * @param dataSource the DataSource to use for database connections
     * @throws IllegalArgumentException if dataSource is null
     */
    public DatabaseTemplate(DataSource dataSource) {
        ValidationUtils.requireNonNull(dataSource, "dataSource");
        this.dataSource = dataSource;
    }

    /**
     * Executes a query and returns a single optional result.
     * <p>
     * This method is useful for queries that should return at most one row.
     * If multiple rows are returned, only the first one is processed.
     * </p>
     *
     * @param <T> the type of the result object
     * @param sql the SQL query to execute
     * @param mapper function to map ResultSet to result object
     * @param parameters the query parameters
     * @return Optional containing the result, or empty if no rows found
     * @throws ConductorException if database operation fails
     */
    public <T> Optional<T> queryForObject(String sql, ResultSetMapper<T> mapper, Object... parameters)
            throws ConductorException {
        ValidationUtils.requireNonBlank(sql, "sql");
        ValidationUtils.requireNonNull(mapper, "mapper");

        return execute(sql, stmt -> {
            setParameters(stmt, parameters);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
            }
        });
    }

    /**
     * Executes a query and returns a list of results.
     * <p>
     * This method processes all rows returned by the query and maps each
     * row to an object using the provided mapper function.
     * </p>
     *
     * @param <T> the type of the result objects
     * @param sql the SQL query to execute
     * @param mapper function to map each ResultSet row to result object
     * @param parameters the query parameters
     * @return list of mapped results (never null, but may be empty)
     * @throws ConductorException if database operation fails
     */
    public <T> List<T> queryForList(String sql, ResultSetMapper<T> mapper, Object... parameters)
            throws ConductorException {
        ValidationUtils.requireNonBlank(sql, "sql");
        ValidationUtils.requireNonNull(mapper, "mapper");

        return execute(sql, stmt -> {
            setParameters(stmt, parameters);
            List<T> results = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
            return results;
        });
    }

    /**
     * Executes an update statement (INSERT, UPDATE, DELETE).
     * <p>
     * This method is used for SQL statements that modify data and return
     * the number of affected rows.
     * </p>
     *
     * @param sql the SQL statement to execute
     * @param parameters the statement parameters
     * @return the number of rows affected
     * @throws ConductorException if database operation fails
     */
    public int update(String sql, Object... parameters) throws ConductorException {
        ValidationUtils.requireNonBlank(sql, "sql");

        return execute(sql, stmt -> {
            setParameters(stmt, parameters);
            return stmt.executeUpdate();
        });
    }

    /**
     * Executes multiple statements in a transaction.
     * <p>
     * All operations are executed within a single transaction. If any operation
     * fails, all changes are rolled back automatically.
     * </p>
     *
     * @param operations the database operations to execute
     * @return the result of the transaction operation
     * @throws ConductorException if any operation fails
     */
    public <T> T executeInTransaction(TransactionCallback<T> operations) throws ConductorException {
        ValidationUtils.requireNonNull(operations, "operations");

        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                TransactionContext context = new TransactionContext(conn);
                T result = operations.execute(context);

                conn.commit();
                logger.debug("Transaction committed successfully");
                return result;

            } catch (Exception e) {
                try {
                    conn.rollback();
                    logger.debug("Transaction rolled back due to error: {}", e.getMessage());
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                    e.addSuppressed(rollbackEx);
                }
                throw new ConductorException("Transaction failed", e);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    logger.warn("Failed to restore auto-commit mode", e);
                }
            }
        } catch (SQLException e) {
            throw new ConductorException("Failed to obtain database connection", e);
        }
    }

    /**
     * Executes a batch update operation.
     * <p>
     * This method is useful for executing the same SQL statement multiple times
     * with different parameters, such as bulk inserts or updates.
     * </p>
     *
     * @param sql the SQL statement to execute
     * @param parametersList list of parameter arrays for each execution
     * @return array of update counts for each execution
     * @throws ConductorException if batch operation fails
     */
    public int[] batchUpdate(String sql, List<Object[]> parametersList) throws ConductorException {
        ValidationUtils.requireNonBlank(sql, "sql");
        ValidationUtils.requireNonNull(parametersList, "parametersList");

        if (parametersList.isEmpty()) {
            return new int[0];
        }

        return execute(sql, stmt -> {
            for (Object[] parameters : parametersList) {
                setParameters(stmt, parameters);
                stmt.addBatch();
            }
            return stmt.executeBatch();
        });
    }

    /**
     * Checks if the database connection is available and functioning.
     *
     * @return true if database is accessible
     */
    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.debug("Database health check failed", e);
            return false;
        }
    }

    /**
     * Core template method that handles connection management and exception handling.
     */
    private <T> T execute(String sql, StatementCallback<T> callback) throws ConductorException {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            T result = callback.execute(stmt);

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 1000) { // Log slow queries
                logger.warn("Slow query detected: {} ms for SQL: {}", duration, sql);
            }

            return result;

        } catch (SQLException e) {
            logger.error("Database operation failed for SQL: {} - Error: {}", sql, e.getMessage());
            throw new ConductorException("Database operation failed", e);
        }
    }

    /**
     * Sets parameters on a PreparedStatement.
     */
    private void setParameters(PreparedStatement stmt, Object... parameters) throws SQLException {
        if (parameters == null) {
            return;
        }

        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            if (param == null) {
                stmt.setNull(i + 1, java.sql.Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(i + 1, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) param);
            } else if (param instanceof java.util.Date) {
                stmt.setTimestamp(i + 1, new java.sql.Timestamp(((java.util.Date) param).getTime()));
            } else {
                // For other types, convert to string
                stmt.setString(i + 1, param.toString());
            }
        }
    }

    /**
     * Functional interface for mapping ResultSet rows to objects.
     */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        /**
         * Maps a ResultSet row to an object.
         *
         * @param rs the ResultSet positioned at the current row
         * @return the mapped object
         * @throws SQLException if database access error occurs
         */
        T map(ResultSet rs) throws SQLException;
    }

    /**
     * Functional interface for prepared statement operations.
     */
    @FunctionalInterface
    private interface StatementCallback<T> {
        T execute(PreparedStatement stmt) throws SQLException;
    }

    /**
     * Functional interface for transaction operations.
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        /**
         * Executes operations within a transaction context.
         *
         * @param context the transaction context
         * @return the result of the transaction
         * @throws Exception if operation fails
         */
        T execute(TransactionContext context) throws Exception;
    }

    /**
     * Provides context for transaction operations.
     */
    public static class TransactionContext {
        private final Connection connection;

        private TransactionContext(Connection connection) {
            this.connection = connection;
        }

        /**
         * Gets the connection for this transaction.
         * <p>
         * The connection is already configured for the transaction and should not
         * be closed by the caller.
         * </p>
         *
         * @return the transaction connection
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Creates a prepared statement within this transaction.
         *
         * @param sql the SQL statement
         * @return the prepared statement
         * @throws SQLException if statement creation fails
         */
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return connection.prepareStatement(sql);
        }
    }
}
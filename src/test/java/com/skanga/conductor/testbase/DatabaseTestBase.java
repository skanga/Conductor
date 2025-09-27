package com.skanga.conductor.testbase;

import com.skanga.conductor.demo.DemoDatabaseManager;
import com.skanga.conductor.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.sql.SQLException;

/**
 * Base test class for tests requiring database operations and isolated database management.
 * <p>
 * This class extends ConductorTestBase and adds database-specific functionality including:
 * </p>
 * <ul>
 * <li>Isolated test database creation and management</li>
 * <li>Automatic database cleanup after test completion</li>
 * <li>MemoryStore creation with isolated database</li>
 * <li>Database URL generation for test isolation</li>
 * <li>Support for both in-memory and file-based test databases</li>
 * </ul>
 * <p>
 * This class uses static setup/teardown to create a single isolated database per test class,
 * which improves test performance while maintaining isolation between test classes.
 * </p>
 *
 * @since 2.0.0
 */
public abstract class DatabaseTestBase extends ConductorTestBase {

    protected static DemoDatabaseManager dbManager;
    protected static MemoryStore isolatedMemoryStore;
    protected static String testWorkflowId;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        // Generate unique workflow ID for this test class
        testWorkflowId = "test-" + System.currentTimeMillis();

        // Create isolated database manager
        dbManager = new DemoDatabaseManager(testWorkflowId);

        // Create isolated memory store
        isolatedMemoryStore = dbManager.createIsolatedMemoryStore();
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        // Clean up database resources
        if (isolatedMemoryStore != null) {
            try {
                isolatedMemoryStore.close();
            } catch (Exception e) {
                // Log but don't fail - this is cleanup
                System.err.println("Warning: Failed to close isolated memory store: " + e.getMessage());
            }
        }

        if (dbManager != null) {
            try {
                dbManager.close();
            } catch (Exception e) {
                // Log but don't fail - this is cleanup
                System.err.println("Warning: Failed to close database manager: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the isolated memory store for this test class.
     *
     * @return the isolated memory store
     */
    protected MemoryStore getIsolatedMemoryStore() {
        return isolatedMemoryStore;
    }

    /**
     * Gets the test workflow ID for this test class.
     *
     * @return the test workflow ID
     */
    protected String getTestWorkflowId() {
        return testWorkflowId;
    }

    /**
     * Creates a new memory store with the isolated database.
     *
     * @return a new memory store instance
     * @throws SQLException if memory store creation fails
     */
    protected MemoryStore createIsolatedMemoryStore() throws SQLException {
        return dbManager.createIsolatedMemoryStore();
    }

    /**
     * Gets the database URL for the isolated test database.
     *
     * @return the database URL
     */
    protected String getDatabaseUrl() {
        return dbManager.getDatabaseUrl();
    }

    /**
     * Creates an in-memory database URL for temporary testing.
     *
     * @param dbName the database name
     * @return the in-memory database URL
     */
    protected String createInMemoryDatabaseUrl(String dbName) {
        return "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
    }

    /**
     * Creates a file-based database URL for persistent testing.
     *
     * @param dbName the database name
     * @return the file-based database URL
     */
    protected String createFileDatabaseUrl(String dbName) {
        return "jdbc:h2:./data/test/" + dbName + ";FILE_LOCK=FS";
    }

    /**
     * Creates a new MemoryStore with a custom database URL.
     *
     * @param dbUrl the database URL
     * @param user the database user
     * @param password the database password
     * @return a new MemoryStore instance
     * @throws SQLException if memory store creation fails
     */
    protected MemoryStore createMemoryStore(String dbUrl, String user, String password) throws SQLException {
        return registerForCleanup(new MemoryStore(dbUrl, user, password));
    }

    /**
     * Creates a new MemoryStore with an in-memory database.
     *
     * @param dbName the database name
     * @return a new MemoryStore instance
     * @throws SQLException if memory store creation fails
     */
    protected MemoryStore createInMemoryStore(String dbName) throws SQLException {
        return createMemoryStore(createInMemoryDatabaseUrl(dbName), "sa", "");
    }

    /**
     * Executes a database operation and ensures proper cleanup.
     *
     * @param operation the database operation to execute
     * @param <T> the return type
     * @return the operation result
     * @throws Exception if the operation fails
     */
    protected <T> T withDatabase(DatabaseOperation<T> operation) throws Exception {
        MemoryStore store = createIsolatedMemoryStore();
        try {
            return operation.execute(store);
        } finally {
            store.close();
        }
    }

    /**
     * Executes a database operation that doesn't return a value.
     *
     * @param operation the database operation to execute
     * @throws Exception if the operation fails
     */
    protected void withDatabase(VoidDatabaseOperation operation) throws Exception {
        withDatabase(store -> {
            operation.execute(store);
            return null;
        });
    }

    /**
     * Functional interface for database operations that return a value.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    protected interface DatabaseOperation<T> {
        T execute(MemoryStore store) throws Exception;
    }

    /**
     * Functional interface for database operations that don't return a value.
     */
    @FunctionalInterface
    protected interface VoidDatabaseOperation {
        void execute(MemoryStore store) throws Exception;
    }

    /**
     * Asserts that a database operation completes without throwing an exception.
     *
     * @param operation the operation to execute
     */
    protected void assertDatabaseOperationSucceeds(VoidDatabaseOperation operation) {
        try {
            withDatabase(operation);
        } catch (Exception e) {
            throw new AssertionError("Database operation should have succeeded but threw: " + e.getMessage(), e);
        }
    }

    /**
     * Asserts that a database operation throws an exception of the specified type.
     *
     * @param expectedType the expected exception type
     * @param operation the operation to execute
     * @param <T> the exception type
     * @return the thrown exception
     */
    protected <T extends Throwable> T assertDatabaseOperationThrows(Class<T> expectedType,
                                                                   VoidDatabaseOperation operation) {
        try {
            withDatabase(operation);
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " but no exception was thrown");
        } catch (Exception thrown) {
            if (expectedType.isInstance(thrown)) {
                return expectedType.cast(thrown);
            } else {
                throw new AssertionError("Expected " + expectedType.getSimpleName() +
                                       " but got " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage(), thrown);
            }
        }
    }
}
package com.skanga.conductor.database;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DatabaseTemplate Tests")
class DatabaseTemplateTest extends ConductorTestBase {

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    private DatabaseTemplate databaseTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        databaseTemplate = new DatabaseTemplate(mockDataSource);
    }

    @Test
    @DisplayName("Should throw exception with null DataSource")
    void testNullDataSource() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DatabaseTemplate(null);
        });
    }

    @Test
    @DisplayName("Should query for single object successfully")
    void testQueryForObjectSuccess() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("name")).thenReturn("testName");

        // Test
        Optional<String> result = databaseTemplate.queryForObject(
            "SELECT name FROM users WHERE id = ?",
            rs -> rs.getString("name"),
            1
        );

        // Verify
        assertTrue(result.isPresent());
        assertEquals("testName", result.get());
        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).executeQuery();
        verify(mockResultSet).close();
        verify(mockStatement).close();
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("Should return empty optional when no results found")
    void testQueryForObjectNoResults() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Test
        Optional<String> result = databaseTemplate.queryForObject(
            "SELECT name FROM users WHERE id = ?",
            rs -> rs.getString("name"),
            999
        );

        // Verify
        assertFalse(result.isPresent());
        verify(mockStatement).setInt(1, 999);
        verify(mockStatement).executeQuery();
    }

    @Test
    @DisplayName("Should throw exception for null SQL in queryForObject")
    void testQueryForObjectNullSql() {
        assertThrows(IllegalArgumentException.class, () -> {
            databaseTemplate.queryForObject(null, rs -> rs.getString("name"));
        });
    }

    @Test
    @DisplayName("Should throw exception for null mapper in queryForObject")
    void testQueryForObjectNullMapper() {
        assertThrows(IllegalArgumentException.class, () -> {
            databaseTemplate.queryForObject("SELECT * FROM test", null);
        });
    }

    @Test
    @DisplayName("Should query for list successfully")
    void testQueryForListSuccess() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next())
            .thenReturn(true)   // First row
            .thenReturn(true)   // Second row
            .thenReturn(false); // No more rows
        when(mockResultSet.getString("name"))
            .thenReturn("name1")
            .thenReturn("name2");

        // Test
        List<String> results = databaseTemplate.queryForList(
            "SELECT name FROM users WHERE status = ?",
            rs -> rs.getString("name"),
            "ACTIVE"
        );

        // Verify
        assertEquals(2, results.size());
        assertEquals("name1", results.get(0));
        assertEquals("name2", results.get(1));
        verify(mockStatement).setString(1, "ACTIVE");
        verify(mockStatement).executeQuery();
    }

    @Test
    @DisplayName("Should return empty list when no results found in queryForList")
    void testQueryForListNoResults() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Test
        List<String> results = databaseTemplate.queryForList(
            "SELECT name FROM users WHERE status = ?",
            rs -> rs.getString("name"),
            "INACTIVE"
        );

        // Verify
        assertTrue(results.isEmpty());
        verify(mockStatement).setString(1, "INACTIVE");
    }

    @Test
    @DisplayName("Should execute update successfully")
    void testUpdateSuccess() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        // Test
        int rowsAffected = databaseTemplate.update(
            "UPDATE users SET name = ? WHERE id = ?",
            "newName",
            123
        );

        // Verify
        assertEquals(1, rowsAffected);
        verify(mockStatement).setString(1, "newName");
        verify(mockStatement).setInt(2, 123);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should handle various parameter types correctly")
    void testParameterTypes() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        java.util.Date testDate = new java.util.Date();

        // Test
        databaseTemplate.update(
            "INSERT INTO test (str, num, bool, dt, nullval) VALUES (?, ?, ?, ?, ?)",
            "stringValue",
            42,
            true,
            testDate,
            null
        );

        // Verify parameter setting
        verify(mockStatement).setString(1, "stringValue");
        verify(mockStatement).setInt(2, 42);
        verify(mockStatement).setBoolean(3, true);
        verify(mockStatement).setTimestamp(eq(4), any(Timestamp.class));
        verify(mockStatement).setNull(5, Types.NULL);
    }

    @Test
    @DisplayName("Should execute batch update successfully")
    void testBatchUpdateSuccess() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeBatch()).thenReturn(new int[]{1, 1, 1});

        List<Object[]> parametersList = Arrays.asList(
            new Object[]{"name1", 1},
            new Object[]{"name2", 2},
            new Object[]{"name3", 3}
        );

        // Test
        int[] results = databaseTemplate.batchUpdate(
            "INSERT INTO users (name, id) VALUES (?, ?)",
            parametersList
        );

        // Verify
        assertEquals(3, results.length);
        assertEquals(1, results[0]);
        assertEquals(1, results[1]);
        assertEquals(1, results[2]);

        verify(mockStatement, times(3)).addBatch();
        verify(mockStatement).executeBatch();
    }

    @Test
    @DisplayName("Should return empty array for empty batch update")
    void testBatchUpdateEmpty() throws Exception {
        int[] results = databaseTemplate.batchUpdate(
            "INSERT INTO users (name) VALUES (?)",
            Arrays.asList()
        );

        assertEquals(0, results.length);
        verifyNoInteractions(mockDataSource);
    }

    @Test
    @DisplayName("Should execute transaction successfully")
    void testExecuteInTransactionSuccess() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getAutoCommit()).thenReturn(true);

        // Test
        String result = databaseTemplate.executeInTransaction(context -> {
            assertNotNull(context.getConnection());
            return "success";
        });

        // Verify
        assertEquals("success", result);
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).commit();
        verify(mockConnection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should rollback transaction on failure")
    void testExecuteInTransactionRollback() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getAutoCommit()).thenReturn(true);

        // Test
        ConductorException exception = assertThrows(ConductorException.class, () -> {
            databaseTemplate.executeInTransaction(context -> {
                throw new RuntimeException("Test exception");
            });
        });

        // Verify
        assertTrue(exception.getMessage().contains("Transaction failed"));
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).rollback();
        verify(mockConnection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should handle rollback failure gracefully")
    void testExecuteInTransactionRollbackFailure() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getAutoCommit()).thenReturn(true);
        doThrow(new SQLException("Rollback failed")).when(mockConnection).rollback();

        // Test
        ConductorException exception = assertThrows(ConductorException.class, () -> {
            databaseTemplate.executeInTransaction(context -> {
                throw new RuntimeException("Test exception");
            });
        });

        // Verify
        assertTrue(exception.getMessage().contains("Transaction failed"));
        assertEquals(1, exception.getCause().getSuppressed().length);
        verify(mockConnection).rollback();
    }

    @Test
    @DisplayName("Should check database health successfully")
    void testIsHealthySuccess() throws Exception {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        boolean healthy = databaseTemplate.isHealthy();

        assertTrue(healthy);
        verify(mockConnection).isValid(5);
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("Should return false when health check fails")
    void testIsHealthyFailure() throws Exception {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        boolean healthy = databaseTemplate.isHealthy();

        assertFalse(healthy);
    }

    @Test
    @DisplayName("Should throw ConductorException on SQL exception in query")
    void testQuerySQLException() throws Exception {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

        ConductorException exception = assertThrows(ConductorException.class, () -> {
            databaseTemplate.queryForObject(
                "SELECT name FROM users WHERE id = ?",
                rs -> rs.getString("name"),
                1
            );
        });

        assertTrue(exception.getMessage().contains("Database operation failed"));
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    @DisplayName("Should throw ConductorException on SQL exception in update")
    void testUpdateSQLException() throws Exception {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));

        ConductorException exception = assertThrows(ConductorException.class, () -> {
            databaseTemplate.update("UPDATE users SET name = ?", "newName");
        });

        assertTrue(exception.getMessage().contains("Database operation failed"));
    }

    @Test
    @DisplayName("Should throw ConductorException when connection fails")
    void testConnectionFailure() throws Exception {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        ConductorException exception = assertThrows(ConductorException.class, () -> {
            databaseTemplate.queryForObject(
                "SELECT name FROM users WHERE id = ?",
                rs -> rs.getString("name"),
                1
            );
        });

        assertTrue(exception.getMessage().contains("Database operation failed"));
    }

    @Test
    @DisplayName("Should validate transaction callback parameter")
    void testExecuteInTransactionNullCallback() {
        assertThrows(IllegalArgumentException.class, () -> {
            databaseTemplate.executeInTransaction(null);
        });
    }

    @Test
    @DisplayName("Should validate batch update parameters")
    void testBatchUpdateValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            databaseTemplate.batchUpdate(null, Arrays.<Object[]>asList(new Object[]{"test"}));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            databaseTemplate.batchUpdate("INSERT INTO test VALUES (?)", null);
        });
    }

    @Test
    @DisplayName("Should handle custom object parameter types")
    void testCustomObjectParameters() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        // Custom object that will be converted to string
        Object customObj = new Object() {
            @Override
            public String toString() {
                return "customValue";
            }
        };

        // Test
        databaseTemplate.update("INSERT INTO test (val) VALUES (?)", customObj);

        // Verify
        verify(mockStatement).setString(1, "customValue");
    }

    @Test
    @DisplayName("Should handle Long parameters correctly")
    void testLongParameters() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        // Test
        databaseTemplate.update("UPDATE test SET value = ? WHERE id = ?", 12345L, 678L);

        // Verify
        verify(mockStatement).setLong(1, 12345L);
        verify(mockStatement).setLong(2, 678L);
    }

    @Test
    @DisplayName("Should create prepared statement in transaction context")
    void testTransactionContextPrepareStatement() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getAutoCommit()).thenReturn(true);
        when(mockConnection.prepareStatement("SELECT 1")).thenReturn(mockStatement);

        // Test
        databaseTemplate.executeInTransaction(context -> {
            PreparedStatement stmt = context.prepareStatement("SELECT 1");
            assertNotNull(stmt);
            assertSame(mockStatement, stmt);
            return "success";
        });

        // Verify
        verify(mockConnection).prepareStatement("SELECT 1");
    }

    @Test
    @DisplayName("Should handle empty parameter arrays")
    void testEmptyParameters() throws Exception {
        // Setup mocks
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        // Test with no parameters
        databaseTemplate.update("DELETE FROM test");

        // Verify no parameter setting occurred
        verify(mockStatement, never()).setString(anyInt(), anyString());
        verify(mockStatement, never()).setInt(anyInt(), anyInt());
        verify(mockStatement).executeUpdate();
    }
}
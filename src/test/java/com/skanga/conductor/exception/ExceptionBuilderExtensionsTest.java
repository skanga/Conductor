package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ExceptionBuilderExtensions functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionBuilderExtensionsTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should be a final utility class")
    void testClassModifiers() {
        assertTrue(Modifier.isFinal(ExceptionBuilderExtensions.class.getModifiers()));
        assertTrue(Modifier.isPublic(ExceptionBuilderExtensions.class.getModifiers()));
    }

    @Test
    @Order(2)
    @DisplayName("Should have private constructor")
    void testPrivateConstructor() throws Exception {
        Constructor<ExceptionBuilderExtensions> constructor =
            ExceptionBuilderExtensions.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);
        assertDoesNotThrow(() -> constructor.newInstance());
    }

    @Test
    @Order(3)
    @DisplayName("Should not be publicly instantiable")
    void testNotPubliclyInstantiable() {
        Constructor<?>[] constructors = ExceptionBuilderExtensions.class.getConstructors();
        assertEquals(0, constructors.length, "Should have no public constructors");
    }

    // === Workflow Exception Builders Tests ===

    @Test
    @Order(4)
    @DisplayName("Should create workflow execution builder")
    void testWorkflowExecutionBuilder() {
        String message = "Workflow execution failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.workflowExecution(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    @Test
    @Order(5)
    @DisplayName("Should throw exception for null message in workflow execution")
    void testWorkflowExecutionNullMessage() {
        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.workflowExecution(null));
    }

    @Test
    @Order(6)
    @DisplayName("Should throw exception for blank message in workflow execution")
    void testWorkflowExecutionBlankMessage() {
        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.workflowExecution(""));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.workflowExecution("   "));
    }

    @Test
    @Order(7)
    @DisplayName("Should create workflow stage builder")
    void testWorkflowStageBuilder() {
        String message = "Stage execution failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.workflowStage(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    @Test
    @Order(8)
    @DisplayName("Should create workflow validation builder")
    void testWorkflowValidationBuilder() {
        String message = "Workflow validation failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.workflowValidation(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    // === Metrics Exception Builders Tests ===

    @Test
    @Order(9)
    @DisplayName("Should create metrics collection builder")
    void testMetricsCollectionBuilder() {
        String message = "Metrics collection failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.metricsCollection(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    @Test
    @Order(10)
    @DisplayName("Should create metrics export builder")
    void testMetricsExportBuilder() {
        String message = "Metrics export failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.metricsExport(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    // === Memory Management Exception Builders Tests ===

    @Test
    @Order(11)
    @DisplayName("Should create memory management builder")
    void testMemoryManagementBuilder() {
        String message = "Memory management failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.memoryManagement(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    @Test
    @Order(12)
    @DisplayName("Should create memory cleanup builder")
    void testMemoryCleanupBuilder() {
        String message = "Memory cleanup failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.memoryCleanup(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    // === Database Operation Exception Builders Tests ===

    @Test
    @Order(13)
    @DisplayName("Should create database operation builder")
    void testDatabaseOperationBuilder() {
        String message = "Database operation failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.databaseOperation(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    @Test
    @Order(14)
    @DisplayName("Should create database transaction builder")
    void testDatabaseTransactionBuilder() {
        String message = "Database transaction failed";
        ExceptionBuilder builder = ExceptionBuilderExtensions.databaseTransaction(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    @Test
    @Order(15)
    @DisplayName("Should create database connection pool builder")
    void testDatabaseConnectionPoolBuilder() {
        String message = "Connection pool exhausted";
        ExceptionBuilder builder = ExceptionBuilderExtensions.databaseConnectionPool(message);

        assertNotNull(builder);
        assertInstanceOf(ExceptionBuilder.class, builder);
    }

    // === ContextExtensions Tests ===

    @Test
    @Order(16)
    @DisplayName("ContextExtensions should be a final utility class")
    void testContextExtensionsClassModifiers() {
        Class<?> contextExtensionsClass = ExceptionBuilderExtensions.ContextExtensions.class;
        assertTrue(Modifier.isFinal(contextExtensionsClass.getModifiers()));
        assertTrue(Modifier.isPublic(contextExtensionsClass.getModifiers()));
        assertTrue(Modifier.isStatic(contextExtensionsClass.getModifiers()));
    }

    @Test
    @Order(17)
    @DisplayName("ContextExtensions should have private constructor")
    void testContextExtensionsPrivateConstructor() throws Exception {
        Constructor<?> constructor =
            ExceptionBuilderExtensions.ContextExtensions.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    @Order(18)
    @DisplayName("Should add workflow context")
    void testWithWorkflowContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String workflowName = "test-workflow";
        String stageName = "stage-1";

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withWorkflowContext(builder, workflowName, stageName);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(19)
    @DisplayName("Should handle null stage name in workflow context")
    void testWithWorkflowContextNullStage() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String workflowName = "test-workflow";

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.ContextExtensions
                .withWorkflowContext(builder, workflowName, null));
    }

    @Test
    @Order(20)
    @DisplayName("Should throw exception for null builder in workflow context")
    void testWithWorkflowContextNullBuilder() {
        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withWorkflowContext(null, "workflow", "stage"));
    }

    @Test
    @Order(21)
    @DisplayName("Should throw exception for null workflow name")
    void testWithWorkflowContextNullWorkflowName() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withWorkflowContext(builder, null, "stage"));
    }

    @Test
    @Order(22)
    @DisplayName("Should add timing context")
    void testWithTimingContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withTimingContext(builder, start, end);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(23)
    @DisplayName("Should throw exception for null times in timing context")
    void testWithTimingContextNullTimes() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Instant now = Instant.now();

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withTimingContext(builder, null, now));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withTimingContext(builder, now, null));
    }

    @Test
    @Order(24)
    @DisplayName("Should add performance context")
    void testWithPerformanceContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Duration executionTime = Duration.ofMillis(5000);
        Long memoryUsed = 1024L * 1024L; // 1MB
        Long cpuTime = 3000L;

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withPerformanceContext(builder, executionTime, memoryUsed, cpuTime);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(25)
    @DisplayName("Should handle null optional values in performance context")
    void testWithPerformanceContextNullOptionals() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Duration executionTime = Duration.ofMillis(5000);

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.ContextExtensions
                .withPerformanceContext(builder, executionTime, null, null));
    }

    @Test
    @Order(26)
    @DisplayName("Should add database context")
    void testWithDatabaseContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String operation = "SELECT";
        String tableName = "agent_memory";
        Integer rowsAffected = 42;

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withDatabaseContext(builder, operation, tableName, rowsAffected);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(27)
    @DisplayName("Should handle null rows affected in database context")
    void testWithDatabaseContextNullRowsAffected() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.ContextExtensions
                .withDatabaseContext(builder, "SELECT", "test_table", null));
    }

    @Test
    @Order(28)
    @DisplayName("Should throw exception for invalid database context parameters")
    void testWithDatabaseContextInvalidParameters() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withDatabaseContext(null, "SELECT", "table", 1));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withDatabaseContext(builder, null, "table", 1));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withDatabaseContext(builder, "SELECT", null, 1));
    }

    @Test
    @Order(29)
    @DisplayName("Should add memory context")
    void testWithMemoryContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        double currentUsage = 0.85;
        Double thresholdExceeded = 0.80;
        String cleanupTaskName = "cleanup-expired";

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withMemoryContext(builder, currentUsage, thresholdExceeded, cleanupTaskName);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(30)
    @DisplayName("Should handle null optional values in memory context")
    void testWithMemoryContextNullOptionals() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.ContextExtensions
                .withMemoryContext(builder, 0.75, null, null));
    }

    @Test
    @Order(31)
    @DisplayName("Should add metrics context")
    void testWithMetricsContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String metricName = "execution_time";
        String metricType = "timer";
        String collectorType = "in-memory";

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withMetricsContext(builder, metricName, metricType, collectorType);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(32)
    @DisplayName("Should handle null optional values in metrics context")
    void testWithMetricsContextNullOptionals() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.ContextExtensions
                .withMetricsContext(builder, "test_metric", null, null));
    }

    @Test
    @Order(33)
    @DisplayName("Should add batch context")
    void testWithBatchContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        int batchSize = 100;
        int processedItems = 75;
        int failedItems = 5;

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withBatchContext(builder, batchSize, processedItems, failedItems);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(34)
    @DisplayName("Should throw exception for negative values in batch context")
    void testWithBatchContextNegativeValues() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withBatchContext(builder, -1, 0, 0));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withBatchContext(builder, 100, -1, 0));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withBatchContext(builder, 100, 75, -1));
    }

    @Test
    @Order(35)
    @DisplayName("Should add resource context")
    void testWithResourceContext() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String resourceType = "database_connections";
        int currentUsage = 18;
        int maxCapacity = 20;

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.ContextExtensions
            .withResourceContext(builder, resourceType, currentUsage, maxCapacity);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(36)
    @DisplayName("Should throw exception for invalid resource context parameters")
    void testWithResourceContextInvalidParameters() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withResourceContext(builder, "", 10, 20));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withResourceContext(builder, "connections", -1, 20));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.ContextExtensions
                .withResourceContext(builder, "connections", 10, 0));
    }

    // === RecoveryExtensions Tests ===

    @Test
    @Order(37)
    @DisplayName("RecoveryExtensions should be a final utility class")
    void testRecoveryExtensionsClassModifiers() {
        Class<?> recoveryExtensionsClass = ExceptionBuilderExtensions.RecoveryExtensions.class;
        assertTrue(Modifier.isFinal(recoveryExtensionsClass.getModifiers()));
        assertTrue(Modifier.isPublic(recoveryExtensionsClass.getModifiers()));
        assertTrue(Modifier.isStatic(recoveryExtensionsClass.getModifiers()));
    }

    @Test
    @Order(38)
    @DisplayName("Should add increase timeout recovery")
    void testIncreaseTimeoutRecovery() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Duration currentTimeout = Duration.ofSeconds(30);
        Duration suggestedTimeout = Duration.ofMinutes(2);

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.RecoveryExtensions
            .increaseTimeout(builder, currentTimeout, suggestedTimeout);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(39)
    @DisplayName("Should throw exception for null timeouts in increase timeout recovery")
    void testIncreaseTimeoutRecoveryNullTimeouts() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Duration timeout = Duration.ofSeconds(30);

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .increaseTimeout(builder, null, timeout));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .increaseTimeout(builder, timeout, null));
    }

    @Test
    @Order(40)
    @DisplayName("Should add retry with specific backoff recovery")
    void testRetryWithSpecificBackoffRecovery() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Duration initialDelay = Duration.ofMillis(500);
        Duration maxDelay = Duration.ofSeconds(30);
        double multiplier = 2.0;

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.RecoveryExtensions
            .retryWithSpecificBackoff(builder, initialDelay, maxDelay, multiplier);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(41)
    @DisplayName("Should throw exception for invalid backoff parameters")
    void testRetryWithSpecificBackoffInvalidParameters() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Duration delay = Duration.ofMillis(500);

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .retryWithSpecificBackoff(builder, null, delay, 2.0));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .retryWithSpecificBackoff(builder, delay, null, 2.0));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .retryWithSpecificBackoff(builder, delay, delay, 0.0));
    }

    @Test
    @Order(42)
    @DisplayName("Should add scale resources recovery")
    void testScaleResourcesRecovery() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String resourceType = "worker_threads";
        int currentCapacity = 10;
        int suggestedCapacity = 20;

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.RecoveryExtensions
            .scaleResources(builder, resourceType, currentCapacity, suggestedCapacity);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(43)
    @DisplayName("Should throw exception for invalid scale resources parameters")
    void testScaleResourcesInvalidParameters() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .scaleResources(builder, "", 10, 20));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .scaleResources(builder, "threads", 0, 20));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .scaleResources(builder, "threads", 10, 0));
    }

    @Test
    @Order(44)
    @DisplayName("Should add optimize configuration recovery")
    void testOptimizeConfigurationRecovery() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        String configSection = "database";
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("max_connections", "50");
        suggestions.put("connection_timeout", "60s");

        ExceptionBuilder enhancedBuilder = ExceptionBuilderExtensions.RecoveryExtensions
            .optimizeConfiguration(builder, configSection, suggestions);

        assertSame(builder, enhancedBuilder);
    }

    @Test
    @Order(45)
    @DisplayName("Should handle null suggestions in optimize configuration")
    void testOptimizeConfigurationNullSuggestions() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .optimizeConfiguration(builder, "database", null));
    }

    @Test
    @Order(46)
    @DisplayName("Should throw exception for null config section")
    void testOptimizeConfigurationNullConfigSection() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .optimizeConfiguration(builder, null, new HashMap<>()));
    }

    @Test
    @Order(47)
    @DisplayName("Should handle empty suggestions map in optimize configuration")
    void testOptimizeConfigurationEmptySuggestions() {
        ExceptionBuilder builder = ExceptionBuilder.validation("Test message");
        Map<String, String> emptySuggestions = new HashMap<>();

        assertDoesNotThrow(() ->
            ExceptionBuilderExtensions.RecoveryExtensions
                .optimizeConfiguration(builder, "database", emptySuggestions));
    }

    @Test
    @Order(48)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception",
                    ExceptionBuilderExtensions.class.getPackageName());
        assertEquals("ExceptionBuilderExtensions",
                    ExceptionBuilderExtensions.class.getSimpleName());
    }

    @Test
    @Order(49)
    @DisplayName("Should have proper inner class structure")
    void testInnerClassStructure() {
        Class<?>[] declaredClasses = ExceptionBuilderExtensions.class.getDeclaredClasses();
        assertEquals(2, declaredClasses.length);

        // Verify both inner classes exist
        boolean hasContextExtensions = false;
        boolean hasRecoveryExtensions = false;

        for (Class<?> innerClass : declaredClasses) {
            if (innerClass.getSimpleName().equals("ContextExtensions")) {
                hasContextExtensions = true;
            } else if (innerClass.getSimpleName().equals("RecoveryExtensions")) {
                hasRecoveryExtensions = true;
            }
        }

        assertTrue(hasContextExtensions, "Should have ContextExtensions inner class");
        assertTrue(hasRecoveryExtensions, "Should have RecoveryExtensions inner class");
    }
}
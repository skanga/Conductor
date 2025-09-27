package com.skanga.conductor.exception;

import com.skanga.conductor.utils.ValidationUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Extensions to the ExceptionBuilder pattern for enhanced error handling capabilities.
 * <p>
 * This class provides additional fluent methods and factory patterns to extend
 * the core ExceptionBuilder functionality with domain-specific error handling
 * patterns and enhanced context building capabilities.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Workflow-specific exception builders</li>
 * <li>Metrics collection error handling</li>
 * <li>Memory management exception patterns</li>
 * <li>Database operation error builders</li>
 * <li>Enhanced timing and performance context</li>
 * <li>Batch operation error handling</li>
 * </ul>
 * <p>
 * Usage examples:
 * </p>
 * <pre>
 * // Workflow execution failure
 * throw ExceptionBuilderExtensions.workflowExecution("Stage failed")
 *     .errorCode(ErrorCodes.WORKFLOW_STAGE_FAILED)
 *     .workflow("book-creation", "stage-1")
 *     .executionTime(Duration.ofMinutes(5))
 *     .retryWithBackoff()
 *     .build();
 *
 * // Memory management failure
 * throw ExceptionBuilderExtensions.memoryManagement("Cleanup failed")
 *     .errorCode(ErrorCodes.MEMORY_CLEANUP_FAILED)
 *     .memoryUsage(0.95)
 *     .cleanupTask("expired-resources")
 *     .contactAdmin()
 *     .build();
 *
 * // Database operation failure
 * throw ExceptionBuilderExtensions.databaseOperation("Query timeout")
 *     .errorCode(ErrorCodes.DATABASE_QUERY_TIMEOUT)
 *     .sqlOperation("SELECT", "agent_memory")
 *     .queryTimeout(Duration.ofSeconds(30))
 *     .increaseTimeout()
 *     .build();
 * </pre>
 * <p>
 * Thread Safety: This class is thread-safe as all methods are static and stateless.
 * </p>
 *
 * @since 1.0.0
 * @see ExceptionBuilder
 * @see ErrorCodes
 */
public final class ExceptionBuilderExtensions {

    private ExceptionBuilderExtensions() {
        // Utility class - prevent instantiation
    }

    // === Workflow Exception Builders ===

    /**
     * Creates a builder for workflow execution exceptions.
     *
     * @param message the error message
     * @return new exception builder with workflow context
     */
    public static ExceptionBuilder workflowExecution(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.planner(message);
    }

    /**
     * Creates a builder for workflow stage exceptions.
     *
     * @param message the error message
     * @return new exception builder with stage context
     */
    public static ExceptionBuilder workflowStage(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.planner(message);
    }

    /**
     * Creates a builder for workflow validation exceptions.
     *
     * @param message the error message
     * @return new exception builder with validation context
     */
    public static ExceptionBuilder workflowValidation(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.validation(message);
    }

    // === Metrics Exception Builders ===

    /**
     * Creates a builder for metrics collection exceptions.
     *
     * @param message the error message
     * @return new exception builder with metrics context
     */
    public static ExceptionBuilder metricsCollection(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.memoryStore(message);
    }

    /**
     * Creates a builder for metrics export exceptions.
     *
     * @param message the error message
     * @return new exception builder with export context
     */
    public static ExceptionBuilder metricsExport(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.toolExecution(message);
    }

    // === Memory Management Exception Builders ===

    /**
     * Creates a builder for memory management exceptions.
     *
     * @param message the error message
     * @return new exception builder with memory context
     */
    public static ExceptionBuilder memoryManagement(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.memoryStore(message);
    }

    /**
     * Creates a builder for memory cleanup exceptions.
     *
     * @param message the error message
     * @return new exception builder with cleanup context
     */
    public static ExceptionBuilder memoryCleanup(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.memoryStore(message);
    }

    // === Database Operation Exception Builders ===

    /**
     * Creates a builder for database operation exceptions.
     *
     * @param message the error message
     * @return new exception builder with database context
     */
    public static ExceptionBuilder databaseOperation(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.memoryStore(message);
    }

    /**
     * Creates a builder for database transaction exceptions.
     *
     * @param message the error message
     * @return new exception builder with transaction context
     */
    public static ExceptionBuilder databaseTransaction(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.memoryStore(message);
    }

    /**
     * Creates a builder for database connection pool exceptions.
     *
     * @param message the error message
     * @return new exception builder with connection pool context
     */
    public static ExceptionBuilder databaseConnectionPool(String message) {
        ValidationUtils.requireNonBlank(message, "message");
        return ExceptionBuilder.memoryStore(message);
    }

    // === Enhanced Context Builder Extensions ===

    /**
     * Extension class for enhanced ExceptionBuilder context methods.
     * <p>
     * This class provides additional fluent methods that can be used to enhance
     * any ExceptionBuilder instance with domain-specific context information.
     * </p>
     */
    public static final class ContextExtensions {

        private ContextExtensions() {
            // Utility class - prevent instantiation
        }

        /**
         * Adds workflow execution context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param workflowName the name of the workflow
         * @param stageName the name of the stage (optional)
         * @return the enhanced builder
         */
        public static ExceptionBuilder withWorkflowContext(ExceptionBuilder builder,
                                                          String workflowName,
                                                          String stageName) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonBlank(workflowName, "workflow name");

            builder.metadata("workflow.name", workflowName);
            if (stageName != null && !stageName.isBlank()) {
                builder.metadata("workflow.stage", stageName);
            }
            return builder;
        }

        /**
         * Adds execution timing context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param startTime when the operation started
         * @param endTime when the operation ended
         * @return the enhanced builder
         */
        public static ExceptionBuilder withTimingContext(ExceptionBuilder builder,
                                                        Instant startTime,
                                                        Instant endTime) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonNull(startTime, "start time");
            ValidationUtils.requireNonNull(endTime, "end time");

            long duration = Duration.between(startTime, endTime).toMillis();
            return builder
                    .metadata("timing.start", startTime.toString())
                    .metadata("timing.end", endTime.toString())
                    .duration(duration);
        }

        /**
         * Adds performance context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param executionTime the total execution time
         * @param memoryUsed the memory used during operation (bytes)
         * @param cpuTime the CPU time used (milliseconds)
         * @return the enhanced builder
         */
        public static ExceptionBuilder withPerformanceContext(ExceptionBuilder builder,
                                                             Duration executionTime,
                                                             Long memoryUsed,
                                                             Long cpuTime) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonNull(executionTime, "execution time");

            builder.duration(executionTime.toMillis());
            if (memoryUsed != null) {
                builder.metadata("performance.memory_used", memoryUsed);
            }
            if (cpuTime != null) {
                builder.metadata("performance.cpu_time", cpuTime);
            }
            return builder;
        }

        /**
         * Adds database operation context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param operation the SQL operation type (SELECT, INSERT, UPDATE, DELETE)
         * @param tableName the table name involved
         * @param rowsAffected the number of rows affected (optional)
         * @return the enhanced builder
         */
        public static ExceptionBuilder withDatabaseContext(ExceptionBuilder builder,
                                                          String operation,
                                                          String tableName,
                                                          Integer rowsAffected) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonBlank(operation, "operation");
            ValidationUtils.requireNonBlank(tableName, "table name");

            builder
                    .metadata("database.operation", operation)
                    .metadata("database.table", tableName);

            if (rowsAffected != null) {
                builder.metadata("database.rows_affected", rowsAffected);
            }
            return builder;
        }

        /**
         * Adds memory management context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param currentUsage current memory usage percentage (0.0 to 1.0)
         * @param thresholdExceeded the threshold that was exceeded (optional)
         * @param cleanupTaskName the name of the cleanup task (optional)
         * @return the enhanced builder
         */
        public static ExceptionBuilder withMemoryContext(ExceptionBuilder builder,
                                                        double currentUsage,
                                                        Double thresholdExceeded,
                                                        String cleanupTaskName) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireInRange((int)(currentUsage * 100), 0, 100, "current usage");

            builder.metadata("memory.current_usage", currentUsage);

            if (thresholdExceeded != null) {
                builder.metadata("memory.threshold_exceeded", thresholdExceeded);
            }
            if (cleanupTaskName != null && !cleanupTaskName.isBlank()) {
                builder.metadata("memory.cleanup_task", cleanupTaskName);
            }
            return builder;
        }

        /**
         * Adds metrics collection context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param metricName the name of the metric
         * @param metricType the type of metric (counter, gauge, timer, etc.)
         * @param collectorType the type of collector (in-memory, file, etc.)
         * @return the enhanced builder
         */
        public static ExceptionBuilder withMetricsContext(ExceptionBuilder builder,
                                                         String metricName,
                                                         String metricType,
                                                         String collectorType) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonBlank(metricName, "metric name");

            builder.metadata("metrics.name", metricName);

            if (metricType != null && !metricType.isBlank()) {
                builder.metadata("metrics.type", metricType);
            }
            if (collectorType != null && !collectorType.isBlank()) {
                builder.metadata("metrics.collector", collectorType);
            }
            return builder;
        }

        /**
         * Adds batch operation context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param batchSize the size of the batch
         * @param processedItems the number of items processed
         * @param failedItems the number of items that failed
         * @return the enhanced builder
         */
        public static ExceptionBuilder withBatchContext(ExceptionBuilder builder,
                                                       int batchSize,
                                                       int processedItems,
                                                       int failedItems) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonNegative(batchSize, "batch size");
            ValidationUtils.requireNonNegative(processedItems, "processed items");
            ValidationUtils.requireNonNegative(failedItems, "failed items");

            return builder
                    .metadata("batch.size", batchSize)
                    .metadata("batch.processed", processedItems)
                    .metadata("batch.failed", failedItems)
                    .metadata("batch.success_rate",
                             batchSize > 0 ? (double)(processedItems - failedItems) / batchSize : 0.0);
        }

        /**
         * Adds resource usage context to an exception builder.
         *
         * @param builder the exception builder to enhance
         * @param resourceType the type of resource (file, connection, memory, etc.)
         * @param currentUsage the current usage count
         * @param maxCapacity the maximum capacity
         * @return the enhanced builder
         */
        public static ExceptionBuilder withResourceContext(ExceptionBuilder builder,
                                                          String resourceType,
                                                          int currentUsage,
                                                          int maxCapacity) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonBlank(resourceType, "resource type");
            ValidationUtils.requireNonNegative(currentUsage, "current usage");
            ValidationUtils.requirePositive(maxCapacity, "max capacity");

            return builder
                    .metadata("resource.type", resourceType)
                    .metadata("resource.current_usage", currentUsage)
                    .metadata("resource.max_capacity", maxCapacity)
                    .metadata("resource.usage_percentage", (double)currentUsage / maxCapacity);
        }
    }

    // === Enhanced Recovery Hint Methods ===

    /**
     * Extension class for enhanced recovery hint methods.
     */
    public static final class RecoveryExtensions {

        private RecoveryExtensions() {
            // Utility class - prevent instantiation
        }

        /**
         * Sets increase timeout as the recovery hint with specific timeout value.
         *
         * @param builder the exception builder to enhance
         * @param currentTimeout the current timeout value
         * @param suggestedTimeout the suggested timeout value
         * @return the enhanced builder
         */
        public static ExceptionBuilder increaseTimeout(ExceptionBuilder builder,
                                                      Duration currentTimeout,
                                                      Duration suggestedTimeout) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonNull(currentTimeout, "current timeout");
            ValidationUtils.requireNonNull(suggestedTimeout, "suggested timeout");

            return builder
                    .metadata("recovery.current_timeout", currentTimeout.toString())
                    .metadata("recovery.suggested_timeout", suggestedTimeout.toString())
                    .recoveryDetails("Increase timeout from " + currentTimeout + " to " + suggestedTimeout);
        }

        /**
         * Sets retry with specific backoff strategy as the recovery hint.
         *
         * @param builder the exception builder to enhance
         * @param initialDelay the initial retry delay
         * @param maxDelay the maximum retry delay
         * @param multiplier the backoff multiplier
         * @return the enhanced builder
         */
        public static ExceptionBuilder retryWithSpecificBackoff(ExceptionBuilder builder,
                                                               Duration initialDelay,
                                                               Duration maxDelay,
                                                               double multiplier) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonNull(initialDelay, "initial delay");
            ValidationUtils.requireNonNull(maxDelay, "max delay");
            ValidationUtils.requirePositive((int)(multiplier * 100), "multiplier");

            return builder
                    .retryWithBackoff()
                    .metadata("recovery.initial_delay", initialDelay.toString())
                    .metadata("recovery.max_delay", maxDelay.toString())
                    .metadata("recovery.multiplier", multiplier)
                    .recoveryDetails(
                        String.format("Retry with exponential backoff: initial=%s, max=%s, multiplier=%.2f",
                                    initialDelay, maxDelay, multiplier));
        }

        /**
         * Sets scale resources as the recovery hint.
         *
         * @param builder the exception builder to enhance
         * @param resourceType the type of resource to scale
         * @param currentCapacity the current capacity
         * @param suggestedCapacity the suggested capacity
         * @return the enhanced builder
         */
        public static ExceptionBuilder scaleResources(ExceptionBuilder builder,
                                                     String resourceType,
                                                     int currentCapacity,
                                                     int suggestedCapacity) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonBlank(resourceType, "resource type");
            ValidationUtils.requirePositive(currentCapacity, "current capacity");
            ValidationUtils.requirePositive(suggestedCapacity, "suggested capacity");

            return builder
                    .metadata("recovery.resource_type", resourceType)
                    .metadata("recovery.current_capacity", currentCapacity)
                    .metadata("recovery.suggested_capacity", suggestedCapacity)
                    .recoveryDetails(
                        String.format("Scale %s from %d to %d",
                                    resourceType, currentCapacity, suggestedCapacity))
                    .userActionRequired();
        }

        /**
         * Sets optimize configuration as the recovery hint.
         *
         * @param builder the exception builder to enhance
         * @param configSection the configuration section to optimize
         * @param suggestions specific configuration suggestions
         * @return the enhanced builder
         */
        public static ExceptionBuilder optimizeConfiguration(ExceptionBuilder builder,
                                                            String configSection,
                                                            Map<String, String> suggestions) {
            ValidationUtils.requireNonNull(builder, "builder");
            ValidationUtils.requireNonBlank(configSection, "config section");

            builder
                    .fixConfiguration()
                    .metadata("recovery.config_section", configSection)
                    .recoveryDetails("Optimize configuration in section: " + configSection);

            if (suggestions != null && !suggestions.isEmpty()) {
                suggestions.forEach((key, value) ->
                    builder.metadata("recovery.suggestion." + key, value));
            }

            return builder;
        }
    }
}
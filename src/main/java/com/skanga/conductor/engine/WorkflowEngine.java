package com.skanga.conductor.engine;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.workflow.config.WorkflowDefinition;
import com.skanga.conductor.workflow.config.WorkflowContext;

/**
 * Standard interface for workflow execution engines.
 * <p>
 * This interface defines the contract for executing workflows regardless of their
 * definition format (YAML, code, etc.) or execution strategy (sequential, parallel).
 * Implementations should provide thread-safe execution and proper resource management.
 * </p>
 * <p>
 * The interface supports both simple execution with inputs and complex execution
 * with full workflow context for advanced scenarios.
 * </p>
 * <p>
 * Thread Safety: Implementations must be thread-safe for concurrent workflow execution.
 * </p>
 *
 * @since 1.0.0
 * @see DefaultWorkflowEngine
 * @see com.skanga.conductor.engine.YamlWorkflowEngine
 */
public interface WorkflowEngine extends AutoCloseable {

    /**
     * Executes a workflow with the provided inputs.
     * <p>
     * This is the primary execution method for simple workflow execution.
     * The workflow definition and context should be pre-loaded or configured
     * before calling this method.
     * </p>
     *
     * @param inputs variable arguments representing workflow inputs
     * @return the workflow execution result containing success status and outputs
     * @throws ConductorException if workflow execution fails
     * @throws IllegalStateException if the engine is not properly configured
     */
    WorkflowResult execute(String... inputs) throws ConductorException;

    /**
     * Executes a workflow with explicit definition and context.
     * <p>
     * This method provides more control over workflow execution by allowing
     * the caller to specify the exact workflow definition and execution context.
     * Useful for dynamic workflow execution scenarios.
     * </p>
     *
     * @param definition the workflow definition to execute
     * @param context the execution context containing variables and settings
     * @return the workflow execution result containing success status and outputs
     * @throws ConductorException if workflow execution fails
     * @throws IllegalArgumentException if definition or context is null
     */
    WorkflowResult execute(WorkflowDefinition definition, WorkflowContext context) throws ConductorException;

    /**
     * Checks if the workflow engine is properly configured and ready for execution.
     * <p>
     * This method performs validation of the engine's configuration including
     * required dependencies, workflow definitions, and runtime components.
     * </p>
     *
     * @return true if the engine is ready for execution
     */
    boolean isReady();

    /**
     * Gets the name of this workflow engine implementation.
     * <p>
     * This is useful for logging, monitoring, and debugging purposes to identify
     * which specific engine implementation is being used.
     * </p>
     *
     * @return a descriptive name for this engine implementation
     */
    String getEngineName();

    /**
     * Gets the current engine configuration status.
     * <p>
     * Provides information about the engine's configuration state including
     * loaded definitions, available resources, and any configuration issues.
     * </p>
     *
     * @return configuration status information
     */
    EngineStatus getStatus();

    /**
     * Closes the workflow engine and releases all associated resources.
     * <p>
     * This method should be called when the engine is no longer needed to
     * ensure proper cleanup of thread pools, database connections, and other
     * system resources.
     * </p>
     * <p>
     * After calling this method, the engine should not be used for further
     * workflow execution.
     * </p>
     *
     * @throws Exception if an error occurs during resource cleanup
     */
    @Override
    void close() throws Exception;

    /**
     * Represents the result of a workflow execution.
     */
    interface WorkflowResult {
        /**
         * Returns true if the workflow executed successfully.
         */
        boolean isSuccess();

        /**
         * Gets the error message if the workflow failed.
         */
        String getErrorMessage();

        /**
         * Gets the workflow execution start time.
         */
        long getStartTime();

        /**
         * Gets the workflow execution end time.
         */
        long getEndTime();

        /**
         * Gets the total execution time in milliseconds.
         */
        long getTotalExecutionTimeMs();

        /**
         * Gets the workflow name that was executed.
         */
        String getWorkflowName();

        /**
         * Gets any output data produced by the workflow.
         */
        Object getOutput();
    }

    /**
     * Represents the configuration and runtime status of a workflow engine.
     */
    interface EngineStatus {
        /**
         * Returns true if the engine is properly configured.
         */
        boolean isConfigured();

        /**
         * Gets the configuration validation messages.
         */
        java.util.List<String> getValidationMessages();

        /**
         * Gets the number of loaded workflow definitions.
         */
        int getLoadedWorkflowCount();

        /**
         * Gets the number of active executions.
         */
        int getActiveExecutionCount();

        /**
         * Gets engine-specific metadata.
         */
        java.util.Map<String, Object> getMetadata();
    }
}
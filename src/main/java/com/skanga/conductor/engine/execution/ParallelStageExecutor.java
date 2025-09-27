package com.skanga.conductor.engine.execution;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.workflow.config.WorkflowStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Handles parallel execution of workflow stages within an execution wave.
 * Manages thread pools, timeouts, and error handling for concurrent stage execution.
 */
public class ParallelStageExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ParallelStageExecutor.class);

    private final ExecutorService executorService;
    private final int maxParallelism;
    private final long defaultTimeoutMs;

    /**
     * Creates a parallel stage executor with the specified configuration.
     */
    public ParallelStageExecutor(int maxParallelism, long defaultTimeoutMs) {
        this.maxParallelism = maxParallelism;
        this.defaultTimeoutMs = defaultTimeoutMs;

        // Create a thread pool with a reasonable number of threads
        this.executorService = Executors.newFixedThreadPool(
            Math.min(maxParallelism, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "WorkflowStageExecutor-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );

        logger.info("Created ParallelStageExecutor with maxParallelism={}, defaultTimeoutMs={}ms",
            maxParallelism, defaultTimeoutMs);
    }

    /**
     * Executes a wave of stages in parallel.
     *
     * @param wave The execution wave containing stages to execute
     * @param stageExecutor Function that executes a single stage
     * @param context The workflow execution context
     * @return Map of stage names to their execution results
     * @throws ConductorException if any stage fails and should stop the workflow
     */
    public <T, R> Map<String, R> executeWave(
            StageExecutionPlan.ExecutionWave wave,
            Function<T, R> stageExecutor,
            T context) throws ConductorException {

        List<WorkflowStage> stages = wave.getStages();

        if (stages.size() == 1) {
            // Single stage - execute directly
            WorkflowStage stage = stages.get(0);
            logger.info("Executing single stage: {}", stage.getName());
            R result = stageExecutor.apply(context);
            return Map.of(stage.getName(), result);
        }

        // Multiple stages - execute in parallel
        logger.info("Executing wave {} with {} stages in parallel: [{}]",
            wave.getWaveNumber(), stages.size(),
            stages.stream().map(WorkflowStage::getName)
                .reduce((a, b) -> a + ", " + b).orElse(""));

        Map<String, Future<R>> futures = new ConcurrentHashMap<>();
        Map<String, R> results = new ConcurrentHashMap<>();

        try {
            // Submit all stages for execution
            for (WorkflowStage stage : stages) {
                String stageName = stage.getName();

                Future<R> future = executorService.submit(() -> {
                    try {
                        logger.debug("Starting parallel execution of stage: {}", stageName);
                        long startTime = System.currentTimeMillis();

                        R result = stageExecutor.apply(context);

                        long duration = System.currentTimeMillis() - startTime;
                        logger.debug("Completed parallel execution of stage: {} in {}ms", stageName, duration);

                        return result;
                    } catch (Exception e) {
                        logger.error("Stage '{}' failed during parallel execution", stageName, e);
                        throw new RuntimeException("Stage '" + stageName + "' failed: " + e.getMessage(), e);
                    }
                });

                futures.put(stageName, future);
            }

            // Wait for all stages to complete
            for (Map.Entry<String, Future<R>> entry : futures.entrySet()) {
                String stageName = entry.getKey();
                Future<R> future = entry.getValue();

                try {
                    // Use default timeout, could be made configurable per stage
                    R result = future.get(defaultTimeoutMs, TimeUnit.MILLISECONDS);
                    results.put(stageName, result);

                } catch (TimeoutException e) {
                    String error = "Stage '" + stageName + "' timed out after " + defaultTimeoutMs + "ms";
                    logger.error(error);

                    // Cancel the future and any remaining ones
                    future.cancel(true);
                    cancelRemainingFutures(futures);

                    throw new ConductorException(error, e);

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String error = "Stage '" + stageName + "' failed during execution: " + cause.getMessage();
                    logger.error(error, cause);

                    // Cancel any remaining futures
                    cancelRemainingFutures(futures);

                    if (cause instanceof ConductorException) {
                        throw (ConductorException) cause;
                    } else {
                        throw new ConductorException(error, cause);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String error = "Stage execution was interrupted";
                    logger.error(error);

                    cancelRemainingFutures(futures);
                    throw new ConductorException(error, e);
                }
            }

            logger.info("Successfully completed wave {} with {} stages",
                wave.getWaveNumber(), results.size());

            return results;

        } catch (Exception e) {
            // Ensure all futures are cancelled on any error
            cancelRemainingFutures(futures);
            throw e;
        }
    }

    /**
     * Cancels all remaining futures in the map.
     */
    private <R> void cancelRemainingFutures(Map<String, Future<R>> futures) {
        for (Map.Entry<String, Future<R>> entry : futures.entrySet()) {
            Future<R> future = entry.getValue();
            if (!future.isDone()) {
                logger.debug("Cancelling stage: {}", entry.getKey());
                future.cancel(true);
            }
        }
    }

    /**
     * Gets the maximum parallelism supported by this executor.
     */
    public int getMaxParallelism() {
        return maxParallelism;
    }

    /**
     * Gets the default timeout in milliseconds.
     */
    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    /**
     * Checks if the executor is shut down.
     */
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        logger.info("Shutting down ParallelStageExecutor");
        executorService.shutdown();

        try {
            // Wait for existing tasks to complete
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();

                // Wait for forced shutdown
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor shutdown");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ParallelStageExecutor shutdown complete");
    }
}
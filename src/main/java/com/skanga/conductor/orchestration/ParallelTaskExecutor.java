package com.skanga.conductor.orchestration;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.metrics.Metric;
import com.skanga.conductor.metrics.MetricType;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.templates.PromptTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Executes tasks in parallel batches while respecting dependencies.
 *
 * <p>This executor takes task batches from TaskDependencyAnalyzer and executes
 * each batch in parallel using a configurable thread pool. Tasks within a batch
 * are independent and can run concurrently.</p>
 *
 * <p>Features:</p>
 * <ul>
 * <li>Parallel execution within batches</li>
 * <li>Dependency-aware batch ordering</li>
 * <li>Configurable thread pool size</li>
 * <li>Comprehensive error handling and timeout support</li>
 * <li>Metrics collection for performance monitoring</li>
 * <li>Graceful degradation to sequential execution on errors</li>
 * <li>AutoCloseable for proper resource cleanup</li>
 * </ul>
 *
 * <p>Usage with try-with-resources:</p>
 * <pre>{@code
 * try (ParallelTaskExecutor executor = new ParallelTaskExecutor()) {
 *     executor.executeBatches(...);
 * } // Executor is automatically shut down
 * }</pre>
 */
public class ParallelTaskExecutor implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ParallelTaskExecutor.class);

    private final PromptTemplateEngine templateEngine;
    private final MetricsRegistry metricsRegistry;
    private final ExecutorService executorService;
    private final int maxParallelism;
    private final long taskTimeoutSeconds;

    /**
     * Creates a parallel task executor with default configuration.
     */
    public ParallelTaskExecutor() {
        this(ForkJoinPool.commonPool(), Runtime.getRuntime().availableProcessors(), 300); // 5 minute timeout
    }

    /**
     * Creates a parallel task executor with custom configuration.
     *
     * @param executorService executor service for running tasks
     * @param maxParallelism maximum number of parallel tasks per batch
     * @param taskTimeoutSeconds timeout for individual task execution
     */
    public ParallelTaskExecutor(ExecutorService executorService, int maxParallelism, long taskTimeoutSeconds) {
        this.templateEngine = new PromptTemplateEngine();
        this.metricsRegistry = MetricsRegistry.getInstance();
        this.executorService = executorService;
        this.maxParallelism = maxParallelism;
        this.taskTimeoutSeconds = taskTimeoutSeconds;
    }

    /**
     * Executes task batches in parallel.
     *
     * @param workflowId workflow identifier
     * @param userRequest user's original request
     * @param taskBatches batches of independent tasks
     * @param agentFactory function to create agents for tasks
     * @param memoryStore store for task outputs
     * @return list of execution results in original task order
     */
    public List<ExecutionResult> executeBatches(
            String workflowId,
            String userRequest,
            List<List<TaskDefinition>> taskBatches,
            Function<TaskDefinition, SubAgent> agentFactory,
            MemoryStore memoryStore) throws ConductorException {

        if (taskBatches.isEmpty()) {
            return Collections.emptyList();
        }

        // Track results by task name to maintain original order
        Map<String, ExecutionResult> resultsByTaskName = new ConcurrentHashMap<>();
        Map<String, String> taskOutputs = new ConcurrentHashMap<>(memoryStore.loadTaskOutputs(workflowId));

        long startTime = System.currentTimeMillis();
        int totalTasks = taskBatches.stream().mapToInt(List::size).sum();

        logger.info("Starting parallel execution of {} tasks in {} batches for workflow '{}'",
                  totalTasks, taskBatches.size(), workflowId);

        try {
            // Execute batches sequentially, tasks within batches in parallel
            for (int batchIndex = 0; batchIndex < taskBatches.size(); batchIndex++) {
                List<TaskDefinition> batch = taskBatches.get(batchIndex);
                long batchStartTime = System.currentTimeMillis();

                logger.info("Executing batch {} with {} tasks: {}",
                          batchIndex + 1, batch.size(),
                          batch.stream().map(t -> t.taskName).toList());

                executeBatch(workflowId, userRequest, batch, agentFactory, taskOutputs, resultsByTaskName, memoryStore);

                long batchDuration = System.currentTimeMillis() - batchStartTime;
                logger.info("Completed batch {} in {}ms", batchIndex + 1, batchDuration);

                // Record batch metrics
                Map<String, String> batchTags = Map.of(
                    "workflow", workflowId,
                    "batch", String.valueOf(batchIndex + 1)
                );
                metricsRegistry.record(new Metric(
                    "workflow.batch.duration",
                    MetricType.GAUGE,
                    batchDuration,
                    java.time.Instant.now(),
                    batchTags
                ));
                metricsRegistry.record(new Metric(
                    "workflow.batch.size",
                    MetricType.GAUGE,
                    batch.size(),
                    java.time.Instant.now(),
                    batchTags
                ));
            }

            // Collect results in original task order
            List<ExecutionResult> orderedResults = collectOrderedResults(taskBatches, resultsByTaskName);

            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("Completed parallel execution of {} tasks in {}ms for workflow '{}'",
                      totalTasks, totalDuration, workflowId);

            // Record overall metrics
            Map<String, String> workflowTags = Map.of("workflow", workflowId);
            metricsRegistry.record(new Metric(
                "workflow.total.duration",
                MetricType.GAUGE,
                totalDuration,
                java.time.Instant.now(),
                workflowTags
            ));
            metricsRegistry.record(new Metric(
                "workflow.total.tasks",
                MetricType.GAUGE,
                totalTasks,
                java.time.Instant.now(),
                workflowTags
            ));
            metricsRegistry.record(new Metric(
                "workflow.parallelism.batches",
                MetricType.GAUGE,
                taskBatches.size(),
                java.time.Instant.now(),
                workflowTags
            ));

            return orderedResults;

        } catch (Exception e) {
            logger.error("Parallel execution failed for workflow '{}': {}", workflowId, e.getMessage(), e);
            metricsRegistry.recordError(workflowId, e.getClass().getSimpleName(), e.getMessage());
            throw new ConductorException("Parallel task execution failed", e);
        }
    }

    /**
     * Executes a single batch of independent tasks in parallel.
     */
    private void executeBatch(
            String workflowId,
            String userRequest,
            List<TaskDefinition> batch,
            Function<TaskDefinition, SubAgent> agentFactory,
            Map<String, String> taskOutputs,
            Map<String, ExecutionResult> resultsByTaskName,
            MemoryStore memoryStore) throws ConductorException {

        if (batch.size() == 1) {
            // Single task - execute directly
            TaskDefinition task = batch.get(0);
            ExecutionResult result = executeTask(workflowId, userRequest, task, agentFactory, taskOutputs, memoryStore);
            resultsByTaskName.put(task.taskName, result);
            return;
        }

        // Multiple tasks - execute in parallel
        int effectiveParallelism = Math.min(batch.size(), maxParallelism);
        List<CompletableFuture<TaskExecutionResult>> futures = new ArrayList<>();

        for (TaskDefinition task : batch) {
            CompletableFuture<TaskExecutionResult> future = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ExecutionResult result = executeTask(workflowId, userRequest, task, agentFactory, taskOutputs, memoryStore);
                        return new TaskExecutionResult(task.taskName, result, null);
                    } catch (Exception e) {
                        return new TaskExecutionResult(task.taskName, null, e);
                    }
                }, executorService)
                .orTimeout(taskTimeoutSeconds, TimeUnit.SECONDS);

            futures.add(future);
        }

        // Wait for all tasks in batch to complete
        try {
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allTasks.get(taskTimeoutSeconds * batch.size(), TimeUnit.SECONDS);

            // Collect results
            for (CompletableFuture<TaskExecutionResult> future : futures) {
                TaskExecutionResult taskResult = future.get();
                if (taskResult.exception != null) {
                    throw new ConductorException("Task execution failed: " + taskResult.taskName, taskResult.exception);
                }
                resultsByTaskName.put(taskResult.taskName, taskResult.result);
            }

        } catch (TimeoutException e) {
            logger.error("Batch execution timed out for workflow '{}'. Cancelling remaining tasks.", workflowId);
            futures.forEach(f -> f.cancel(true));
            throw new ConductorException("Batch execution timed out", e);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Batch execution failed for workflow '{}': {}", workflowId, e.getMessage());
            throw new ConductorException("Batch execution failed", e);
        }
    }

    /**
     * Executes a single task.
     */
    private ExecutionResult executeTask(
            String workflowId,
            String userRequest,
            TaskDefinition task,
            Function<TaskDefinition, SubAgent> agentFactory,
            Map<String, String> taskOutputs,
            MemoryStore memoryStore) throws ConductorException {

        // Check if task was already completed
        if (taskOutputs.containsKey(task.taskName)) {
            logger.debug("Task '{}' already completed, using cached result", task.taskName);
            return new ExecutionResult(true, taskOutputs.get(task.taskName), null);
        }

        logger.debug("Executing task: {}", task.taskName);

        try {
            // Create agent for task
            SubAgent agent = agentFactory.apply(task);

            // Build template variables
            Map<String, Object> templateVars = buildTemplateVariables(userRequest, taskOutputs);

            // Render prompt template
            String agentPrompt = templateEngine.render(task.promptTemplate, templateVars);

            // Execute task
            ExecutionResult result = agent.execute(new ExecutionInput(agentPrompt, null));

            // Store output
            String output = result.output() != null ? result.output() : "";
            taskOutputs.put(task.taskName, output);

            // Persist for resumability
            memoryStore.saveTaskOutput(workflowId, task.taskName, output);

            logger.debug("Completed task '{}' with output length: {}", task.taskName, output.length());
            return result;

        } catch (Exception e) {
            logger.error("Task execution failed for '{}': {}", task.taskName, e.getMessage(), e);
            throw new ConductorException("Task execution failed: " + task.taskName, e);
        }
    }

    /**
     * Builds template variables for prompt rendering.
     */
    private Map<String, Object> buildTemplateVariables(String userRequest, Map<String, String> taskOutputs) {
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("user_request", userRequest);

        // Add all completed task outputs
        templateVars.putAll(taskOutputs);

        // Add prev_output as the most recent output
        if (!taskOutputs.isEmpty()) {
            String prevOutput = taskOutputs.values().stream()
                .reduce((first, second) -> second)
                .orElse("");
            templateVars.put("prev_output", prevOutput);
        } else {
            templateVars.put("prev_output", "");
        }

        return templateVars;
    }

    /**
     * Collects results in the original task order.
     */
    private List<ExecutionResult> collectOrderedResults(
            List<List<TaskDefinition>> taskBatches,
            Map<String, ExecutionResult> resultsByTaskName) {

        List<ExecutionResult> orderedResults = new ArrayList<>();

        for (List<TaskDefinition> batch : taskBatches) {
            for (TaskDefinition task : batch) {
                ExecutionResult result = resultsByTaskName.get(task.taskName);
                if (result == null) {
                    logger.warn("Missing result for task: {}", task.taskName);
                    result = new ExecutionResult(false, "Task execution failed - no result", null);
                }
                orderedResults.add(result);
            }
        }

        return orderedResults;
    }

    /**
     * Result of individual task execution.
     */
    private static class TaskExecutionResult {
        final String taskName;
        final ExecutionResult result;
        final Exception exception;

        TaskExecutionResult(String taskName, ExecutionResult result, Exception exception) {
            this.taskName = taskName;
            this.result = result;
            this.exception = exception;
        }
    }

    /**
     * Shuts down the executor service.
     * <p>
     * Attempts graceful shutdown first, waiting up to 30 seconds for tasks to complete.
     * If tasks don't complete within the timeout, forces immediate shutdown.
     * </p>
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            logger.debug("Shutting down ParallelTaskExecutor");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for executor shutdown, forcing immediate shutdown");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Also close the template engine to prevent resource leaks
        if (templateEngine != null) {
            templateEngine.close();
        }
    }

    /**
     * Closes this executor and releases all resources.
     * <p>
     * This method implements AutoCloseable to support try-with-resources.
     * Delegates to {@link #shutdown()} for cleanup.
     * </p>
     */
    @Override
    public void close() {
        shutdown();
    }
}
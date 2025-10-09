package com.skanga.conductor.orchestration;

import com.skanga.conductor.templates.PromptTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Analyzes task dependencies to enable parallel execution of independent tasks.
 *
 * <p>This class identifies dependencies between tasks by analyzing template variables
 * in prompt templates. Tasks that don't depend on each other can be executed in parallel.</p>
 *
 * <p>Dependency Types:</p>
 * <ul>
 * <li><strong>Direct Task Dependency:</strong> Task B depends on Task A if B's template contains {{task_a}}</li>
 * <li><strong>Sequential Dependency:</strong> Task B depends on previous task if B's template contains {{prev_output}}</li>
 * <li><strong>No Dependency:</strong> Task only uses {{user_request}} or constants</li>
 * </ul>
 */
public class TaskDependencyAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(TaskDependencyAnalyzer.class);
    private final PromptTemplateEngine templateEngine;

    // Special dependency markers
    private static final String PREV_OUTPUT_VAR = "prev_output";
    private static final String USER_REQUEST_VAR = "user_request";

    public TaskDependencyAnalyzer() {
        this.templateEngine = new PromptTemplateEngine();
    }

    /**
     * Analyzes dependencies and groups tasks into execution batches.
     * Tasks in the same batch can be executed in parallel.
     *
     * @param tasks array of tasks to analyze
     * @return list of task batches, where each batch contains independent tasks
     */
    public List<List<TaskDefinition>> groupTasksIntoBatches(TaskDefinition[] tasks) {
        if (tasks == null || tasks.length == 0) {
            return Collections.emptyList();
        }

        // Build dependency graph
        Map<String, Set<String>> dependencies = buildDependencyGraph(tasks);

        // Group tasks into batches using topological ordering
        return createExecutionBatches(tasks, dependencies);
    }

    /**
     * Builds a dependency graph showing which tasks depend on which other tasks.
     *
     * @param tasks array of tasks to analyze
     * @return map where key is task name and value is set of task names it depends on
     */
    private Map<String, Set<String>> buildDependencyGraph(TaskDefinition[] tasks) {
        Map<String, Set<String>> dependencies = new HashMap<>();
        Set<String> taskNames = extractTaskNames(tasks);

        for (TaskDefinition task : tasks) {
            Set<String> taskDependencies = new HashSet<>();

            // Extract all variables from the template
            String[] variables = templateEngine.extractVariableNames(task.promptTemplate);

            for (String variable : variables) {
                if (taskNames.contains(variable)) {
                    // Direct task dependency
                    taskDependencies.add(variable);
                    logger.debug("Task '{}' depends on task '{}'", task.taskName, variable);
                } else if (PREV_OUTPUT_VAR.equals(variable)) {
                    // Sequential dependency - depends on previous task in array
                    String previousTask = findPreviousTask(task, tasks);
                    if (previousTask != null) {
                        taskDependencies.add(previousTask);
                        logger.debug("Task '{}' depends on previous task '{}'", task.taskName, previousTask);
                    }
                }
                // user_request and other variables don't create dependencies
            }

            dependencies.put(task.taskName, taskDependencies);
        }

        return dependencies;
    }

    /**
     * Creates execution batches using topological sorting to respect dependencies.
     */
    private List<List<TaskDefinition>> createExecutionBatches(TaskDefinition[] tasks,
                                                            Map<String, Set<String>> dependencies) {
        List<List<TaskDefinition>> batches = new ArrayList<>();
        Map<String, TaskDefinition> taskMap = createTaskMap(tasks);
        Set<String> completed = new HashSet<>();
        Set<String> remaining = new HashSet<>(Arrays.stream(tasks).map(t -> t.taskName).toList());

        while (!remaining.isEmpty()) {
            List<TaskDefinition> currentBatch = new ArrayList<>();

            // Find tasks that can be executed (all dependencies completed)
            Iterator<String> iterator = remaining.iterator();
            while (iterator.hasNext()) {
                String taskName = iterator.next();
                Set<String> taskDependencies = dependencies.get(taskName);

                if (completed.containsAll(taskDependencies)) {
                    currentBatch.add(taskMap.get(taskName));
                    iterator.remove();
                }
            }

            if (currentBatch.isEmpty()) {
                // Circular dependency detected
                logger.error("Circular dependency detected. Remaining tasks: {}", remaining);
                throw new IllegalStateException("Circular dependency detected in task plan");
            }

            batches.add(currentBatch);

            // Mark batch tasks as completed
            for (TaskDefinition task : currentBatch) {
                completed.add(task.taskName);
            }

            logger.info("Created execution batch {} with {} tasks: {}",
                      batches.size(), currentBatch.size(),
                      currentBatch.stream().map(t -> t.taskName).toList());
        }

        return batches;
    }

    /**
     * Extracts all task names from the task array.
     */
    private Set<String> extractTaskNames(TaskDefinition[] tasks) {
        return Arrays.stream(tasks)
            .map(t -> t.taskName)
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    /**
     * Finds the task that appears immediately before the given task in the array.
     */
    private String findPreviousTask(TaskDefinition targetTask, TaskDefinition[] tasks) {
        for (int i = 1; i < tasks.length; i++) {
            if (tasks[i].taskName.equals(targetTask.taskName)) {
                return tasks[i - 1].taskName;
            }
        }
        return null; // First task has no previous task
    }

    /**
     * Creates a map for quick task lookup by name.
     */
    private Map<String, TaskDefinition> createTaskMap(TaskDefinition[] tasks) {
        Map<String, TaskDefinition> map = new HashMap<>();
        for (TaskDefinition task : tasks) {
            map.put(task.taskName, task);
        }
        return map;
    }

    /**
     * Calculates the maximum parallelism benefit of the given task plan.
     *
     * @param tasks array of tasks to analyze
     * @return analysis result with parallelism metrics
     */
    public ParallelismAnalysis analyzeParallelismBenefit(TaskDefinition[] tasks) {
        if (tasks == null || tasks.length == 0) {
            return new ParallelismAnalysis(0, 0, 0, 1.0);
        }

        List<List<TaskDefinition>> batches = groupTasksIntoBatches(tasks);

        int totalTasks = tasks.length;
        int batchCount = batches.size();
        int maxBatchSize = batches.stream().mapToInt(List::size).max().orElse(0);
        double parallelismRatio = (double) batchCount / totalTasks;

        return new ParallelismAnalysis(totalTasks, batchCount, maxBatchSize, parallelismRatio);
    }

    /**
     * Result of parallelism analysis.
     */
    public static class ParallelismAnalysis {
        private final int totalTasks;
        private final int batchCount;
        private final int maxBatchSize;
        private final double parallelismRatio;

        public ParallelismAnalysis(int totalTasks, int batchCount, int maxBatchSize, double parallelismRatio) {
            this.totalTasks = totalTasks;
            this.batchCount = batchCount;
            this.maxBatchSize = maxBatchSize;
            this.parallelismRatio = parallelismRatio;
        }

        public int getTotalTasks() { return totalTasks; }
        public int getBatchCount() { return batchCount; }
        public int getMaxBatchSize() { return maxBatchSize; }
        public double getParallelismRatio() { return parallelismRatio; }
        public double getSpeedupPotential() { return totalTasks > 0 ? (double) totalTasks / batchCount : 1.0; }

        @Override
        public String toString() {
            return String.format("ParallelismAnalysis{tasks=%d, batches=%d, maxBatchSize=%d, ratio=%.2f, speedup=%.2fx}",
                totalTasks, batchCount, maxBatchSize, parallelismRatio, getSpeedupPotential());
        }
    }
}
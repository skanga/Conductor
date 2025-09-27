package com.skanga.conductor.engine.execution;

import com.skanga.conductor.workflow.config.WorkflowStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the execution plan for workflow stages, handling dependencies and parallel execution.
 * Uses topological sorting to determine the correct execution order and identifies
 * stages that can be executed in parallel.
 */
public class StageExecutionPlan {

    private static final Logger logger = LoggerFactory.getLogger(StageExecutionPlan.class);

    private final List<WorkflowStage> stages;
    private final Map<String, WorkflowStage> stageMap;
    private final List<ExecutionWave> executionWaves;
    private final Map<String, Set<String>> dependencies;
    private final Map<String, Set<String>> dependents;

    /**
     * Represents a wave of stages that can be executed in parallel.
     */
    public static class ExecutionWave {
        private final int waveNumber;
        private final List<WorkflowStage> stages;
        private final boolean hasParallelStages;

        public ExecutionWave(int waveNumber, List<WorkflowStage> stages) {
            this.waveNumber = waveNumber;
            this.stages = stages;
            this.hasParallelStages = stages.size() > 1 || stages.stream().anyMatch(WorkflowStage::isParallel);
        }

        public int getWaveNumber() { return waveNumber; }
        public List<WorkflowStage> getStages() { return stages; }
        public boolean hasParallelStages() { return hasParallelStages; }
        public int getStageCount() { return stages.size(); }

        @Override
        public String toString() {
            return String.format("Wave %d: %d stage(s) [%s]",
                waveNumber, stages.size(),
                stages.stream().map(WorkflowStage::getName).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Creates an execution plan for the given stages.
     */
    public StageExecutionPlan(List<WorkflowStage> stages) {
        this.stages = new ArrayList<>(stages);
        this.stageMap = new HashMap<>();
        this.dependencies = new HashMap<>();
        this.dependents = new HashMap<>();
        this.executionWaves = new ArrayList<>();

        // Build stage map for quick lookup
        for (WorkflowStage stage : stages) {
            stageMap.put(stage.getName(), stage);
        }

        // Build dependency and dependent maps
        buildDependencyGraph();

        // Validate dependencies
        validateDependencies();

        // Create execution waves using topological sort
        createExecutionWaves();

        logger.info("Created execution plan with {} waves for {} stages",
            executionWaves.size(), stages.size());
        logExecutionPlan();
    }

    /**
     * Builds the dependency graph from the stage definitions.
     */
    private void buildDependencyGraph() {
        for (WorkflowStage stage : stages) {
            String stageName = stage.getName();
            dependencies.put(stageName, new HashSet<>());
            dependents.put(stageName, new HashSet<>());
        }

        for (WorkflowStage stage : stages) {
            String stageName = stage.getName();
            List<String> stageDependsOn = stage.getDependsOn();

            if (stageDependsOn != null) {
                for (String dependency : stageDependsOn) {
                    if (stageMap.containsKey(dependency)) {
                        dependencies.get(stageName).add(dependency);
                        dependents.get(dependency).add(stageName);
                    } else {
                        throw new IllegalArgumentException(
                            "Stage '" + stageName + "' depends on unknown stage: " + dependency);
                    }
                }
            }
        }
    }

    /**
     * Validates the dependency graph for cycles and other issues.
     */
    private void validateDependencies() {
        // Check for circular dependencies using DFS
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String stageName : stageMap.keySet()) {
            if (!visited.contains(stageName)) {
                if (hasCycle(stageName, visited, visiting)) {
                    throw new IllegalArgumentException(
                        "Circular dependency detected involving stage: " + stageName);
                }
            }
        }
    }

    /**
     * Detects cycles in the dependency graph using DFS.
     */
    private boolean hasCycle(String stageName, Set<String> visited, Set<String> visiting) {
        visiting.add(stageName);

        for (String dependency : dependencies.get(stageName)) {
            if (visiting.contains(dependency)) {
                return true; // Back edge found - cycle detected
            }
            if (!visited.contains(dependency) && hasCycle(dependency, visited, visiting)) {
                return true;
            }
        }

        visiting.remove(stageName);
        visited.add(stageName);
        return false;
    }

    /**
     * Creates execution waves using topological sorting.
     */
    private void createExecutionWaves() {
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<String> readyQueue = new LinkedList<>();

        // Initialize in-degree count
        for (String stageName : stageMap.keySet()) {
            inDegree.put(stageName, dependencies.get(stageName).size());
            if (inDegree.get(stageName) == 0) {
                readyQueue.offer(stageName);
            }
        }

        int waveNumber = 0;
        while (!readyQueue.isEmpty()) {
            // All stages in the current queue can be executed in parallel
            List<WorkflowStage> currentWave = new ArrayList<>();
            List<String> currentBatch = new ArrayList<>();

            // Collect all stages ready for this wave
            while (!readyQueue.isEmpty()) {
                currentBatch.add(readyQueue.poll());
            }

            // Convert stage names to stage objects
            for (String stageName : currentBatch) {
                currentWave.add(stageMap.get(stageName));
            }

            if (!currentWave.isEmpty()) {
                executionWaves.add(new ExecutionWave(waveNumber++, currentWave));

                // Update in-degrees for dependent stages
                for (String stageName : currentBatch) {
                    for (String dependent : dependents.get(stageName)) {
                        int newInDegree = inDegree.get(dependent) - 1;
                        inDegree.put(dependent, newInDegree);
                        if (newInDegree == 0) {
                            readyQueue.offer(dependent);
                        }
                    }
                }
            }
        }

        // Verify all stages were processed
        int totalProcessed = executionWaves.stream().mapToInt(ExecutionWave::getStageCount).sum();
        if (totalProcessed != stages.size()) {
            throw new IllegalStateException(
                "Failed to process all stages. Expected: " + stages.size() + ", Processed: " + totalProcessed);
        }
    }

    /**
     * Logs the execution plan for debugging.
     */
    private void logExecutionPlan() {
        logger.debug("=== Execution Plan ===");
        for (ExecutionWave wave : executionWaves) {
            logger.debug("  {}", wave);
            if (wave.hasParallelStages()) {
                logger.debug("    -> Can execute {} stages in parallel", wave.getStageCount());
            }
        }
        logger.debug("=== End Execution Plan ===");
    }

    /**
     * Gets the execution waves in order.
     */
    public List<ExecutionWave> getExecutionWaves() {
        return new ArrayList<>(executionWaves);
    }

    /**
     * Gets the total number of execution waves.
     */
    public int getWaveCount() {
        return executionWaves.size();
    }

    /**
     * Gets the maximum parallelism (largest wave size).
     */
    public int getMaxParallelism() {
        return executionWaves.stream().mapToInt(ExecutionWave::getStageCount).max().orElse(1);
    }

    /**
     * Checks if any wave has parallel execution.
     */
    public boolean hasParallelExecution() {
        return executionWaves.stream().anyMatch(ExecutionWave::hasParallelStages);
    }

    /**
     * Gets stages that have no dependencies.
     */
    public List<WorkflowStage> getRootStages() {
        return executionWaves.isEmpty() ?
            Collections.emptyList() :
            executionWaves.get(0).getStages();
    }

    /**
     * Gets stages that have no dependents.
     */
    public List<WorkflowStage> getLeafStages() {
        return executionWaves.isEmpty() ?
            Collections.emptyList() :
            executionWaves.get(executionWaves.size() - 1).getStages();
    }
}
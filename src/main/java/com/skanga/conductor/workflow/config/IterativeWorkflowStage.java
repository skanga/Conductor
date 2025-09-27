package com.skanga.conductor.workflow.config;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A specialized workflow stage that handles iterative execution.
 * This class extends WorkflowStage with iteration-specific logic and state management.
 */
public class IterativeWorkflowStage extends WorkflowStage {

    /**
     * Current iteration state for this stage execution.
     */
    public static class IterationState {
        private final IterationConfig.IterationType type;
        private final String iterationVariable;
        private final Map<String, Object> context;
        private final AtomicInteger currentIndex = new AtomicInteger(0);
        private final List<Object> iterationData;
        private final Map<String, Object> updateVariables;

        public IterationState(IterationConfig config, Map<String, Object> workflowContext) {
            this.type = config.getType();
            this.iterationVariable = config.getVariable();
            this.context = new HashMap<>(workflowContext);
            this.updateVariables = config.getUpdateVariables() != null ?
                new HashMap<>(config.getUpdateVariables()) : new HashMap<>();
            this.iterationData = resolveIterationData(config, workflowContext);
        }

        private List<Object> resolveIterationData(IterationConfig config, Map<String, Object> workflowContext) {
            switch (config.getType()) {
                case DATA_DRIVEN:
                    return extractDataFromSource(config.getSource(), workflowContext);

                case COUNT_BASED:
                    return generateCountBasedData(config, workflowContext);

                case CONDITIONAL:
                    return new ArrayList<>(); // Will be populated dynamically

                default:
                    throw new IllegalArgumentException("Unsupported iteration type: " + config.getType());
            }
        }

        private List<Object> extractDataFromSource(String source, Map<String, Object> workflowContext) {
            try {
                // First try as JSON path
                if (source.startsWith("$.")) {
                    Object result = JsonPath.read(workflowContext, source);
                    if (result instanceof List<?>) {
                        List<?> list = (List<?>) result;
                        return new ArrayList<>(list);
                    } else {
                        return Arrays.asList(result);
                    }
                }

                // Try as variable reference
                String[] parts = source.split("\\.");
                Object current = workflowContext;

                for (String part : parts) {
                    if (current instanceof Map<?, ?>) {
                        current = ((Map<?, ?>) current).get(part);
                    } else {
                        throw new IllegalArgumentException("Cannot navigate path: " + source);
                    }
                }

                if (current instanceof List<?>) {
                    List<?> list = (List<?>) current;
                    return new ArrayList<>(list);
                } else if (current != null) {
                    return Arrays.asList(current);
                } else {
                    return new ArrayList<>();
                }

            } catch (PathNotFoundException | ClassCastException e) {
                throw new IllegalArgumentException("Cannot resolve iteration source: " + source, e);
            }
        }

        private List<Object> generateCountBasedData(IterationConfig config, Map<String, Object> workflowContext) {
            String countExpression = config.getCount();
            int count;

            try {
                // Try to parse as integer first
                count = Integer.parseInt(countExpression);
            } catch (NumberFormatException e) {
                // Try as variable reference
                Object countValue = resolveVariableExpression(countExpression, workflowContext);
                if (countValue instanceof Number) {
                    count = ((Number) countValue).intValue();
                } else {
                    throw new IllegalArgumentException("Count expression must resolve to a number: " + countExpression);
                }
            }

            int start = config.getStart() != null ? config.getStart() : 1;
            List<Object> result = new ArrayList<>();

            for (int i = start; i < start + count; i++) {
                result.add(i);
            }

            return result;
        }

        private Object resolveVariableExpression(String expression, Map<String, Object> context) {
            if (expression.startsWith("${") && expression.endsWith("}")) {
                String varPath = expression.substring(2, expression.length() - 1);
                return resolveVariablePath(varPath, context);
            }
            return expression;
        }

        private Object resolveVariablePath(String path, Map<String, Object> context) {
            String[] parts = path.split("\\.");
            Object current = context;

            for (String part : parts) {
                if (current instanceof Map<?, ?>) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }

            return current;
        }

        public boolean hasNext() {
            switch (type) {
                case DATA_DRIVEN:
                case COUNT_BASED:
                    return currentIndex.get() < iterationData.size();

                case CONDITIONAL:
                    // For conditional, we need to evaluate the condition
                    // This would be handled by the calling code
                    return true;

                default:
                    return false;
            }
        }

        public Object getNext() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more iterations available");
            }

            Object item = iterationData.get(currentIndex.getAndIncrement());

            // Update context with current iteration item
            context.put(iterationVariable, item);

            return item;
        }

        public Map<String, Object> getCurrentContext() {
            return new HashMap<>(context);
        }

        public int getCurrentIndex() {
            return currentIndex.get();
        }

        public String getIterationVariable() {
            return iterationVariable;
        }

        public int getTotalIterations() {
            return iterationData.size();
        }

        public void updateVariables(Map<String, Object> newValues) {
            if (newValues != null) {
                context.putAll(newValues);
            }
        }

        public void reset() {
            currentIndex.set(0);
        }
    }

    /**
     * Result of a single iteration execution.
     */
    public static class IterationResult {
        private final int index;
        private final Object iterationItem;
        private final Map<String, Object> result;
        private final boolean successful;
        private final String errorMessage;
        private final long executionTimeMs;

        public IterationResult(int index, Object iterationItem, Map<String, Object> result,
                              boolean successful, String errorMessage, long executionTimeMs) {
            this.index = index;
            this.iterationItem = iterationItem;
            this.result = result != null ? new HashMap<>(result) : new HashMap<>();
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.executionTimeMs = executionTimeMs;
        }

        public static IterationResult success(int index, Object item, Map<String, Object> result, long executionTimeMs) {
            return new IterationResult(index, item, result, true, null, executionTimeMs);
        }

        public static IterationResult failure(int index, Object item, String error, long executionTimeMs) {
            return new IterationResult(index, item, null, false, error, executionTimeMs);
        }

        // Getters
        public int getIndex() { return index; }
        public Object getIterationItem() { return iterationItem; }
        public Map<String, Object> getResult() { return result; }
        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
        public long getExecutionTimeMs() { return executionTimeMs; }
    }

    /**
     * Aggregated results from all iterations of this stage.
     */
    public static class IterativeStageResult {
        private final String stageName;
        private final List<IterationResult> iterationResults;
        private final Map<String, Object> aggregatedOutputs;
        private final boolean allSuccessful;
        private final long totalExecutionTimeMs;

        public IterativeStageResult(String stageName, List<IterationResult> iterationResults) {
            this.stageName = stageName;
            this.iterationResults = new ArrayList<>(iterationResults);
            this.aggregatedOutputs = aggregateOutputs(iterationResults);
            this.allSuccessful = iterationResults.stream().allMatch(IterationResult::isSuccessful);
            this.totalExecutionTimeMs = iterationResults.stream()
                .mapToLong(IterationResult::getExecutionTimeMs)
                .sum();
        }

        private Map<String, Object> aggregateOutputs(List<IterationResult> results) {
            Map<String, Object> aggregated = new HashMap<>();
            List<Map<String, Object>> allResults = new ArrayList<>();

            for (IterationResult result : results) {
                if (result.isSuccessful()) {
                    allResults.add(result.getResult());
                }
            }

            aggregated.put("iterations", allResults);
            aggregated.put("count", allResults.size());
            aggregated.put("successful_count", results.stream().mapToInt(r -> r.isSuccessful() ? 1 : 0).sum());
            aggregated.put("failed_count", results.stream().mapToInt(r -> !r.isSuccessful() ? 1 : 0).sum());

            return aggregated;
        }

        // Getters
        public String getStageName() { return stageName; }
        public List<IterationResult> getIterationResults() { return iterationResults; }
        public Map<String, Object> getAggregatedOutputs() { return aggregatedOutputs; }
        public boolean isAllSuccessful() { return allSuccessful; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
    }

    // Default constructor for Jackson
    public IterativeWorkflowStage() {
        super();
    }

    /**
     * Creates an iteration state for this stage.
     */
    public IterationState createIterationState(Map<String, Object> workflowContext) {
        if (!isIterative()) {
            throw new IllegalStateException("Cannot create iteration state for non-iterative stage: " + getName());
        }

        return new IterationState(getIteration(), workflowContext);
    }

    /**
     * Validates the iterative configuration and ensures it's properly set up.
     */
    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();

        if (isIterative()) {
            IterationConfig config = getIteration();

            // Additional validation for iterative stages
            if (config.getType() == IterationConfig.IterationType.CONDITIONAL &&
                config.getMaxIterations() == null) {
                throw new IllegalArgumentException("Conditional iteration must specify max_iterations in stage '" + getName() + "'");
            }

            // Validate that iteration timeout is reasonable
            if (config.getIterationTimeout() != null && config.getIterationTimeout() < 1000) {
                throw new IllegalArgumentException("Iteration timeout must be at least 1 second in stage '" + getName() + "'");
            }
        }
    }

    /**
     * Determines if this stage can execute iterations in parallel based on configuration and constraints.
     */
    public boolean canExecuteInParallel() {
        if (!isIterative()) {
            return false;
        }

        IterationConfig config = getIteration();

        // Cannot parallelize if approval is required per item
        if (requiresApproval() && getApproval().isPerItem()) {
            return false;
        }

        // Cannot parallelize conditional iterations (they may depend on previous results)
        if (config.getType() == IterationConfig.IterationType.CONDITIONAL) {
            return false;
        }

        return config.isParallelEnabled();
    }

    /**
     * Gets the effective timeout for each iteration.
     */
    public long getIterationTimeoutMs() {
        if (!isIterative()) {
            return 300000L; // 5 minutes default
        }

        return getIteration().getIterationTimeout();
    }

    /**
     * Creates a string representation suitable for logging.
     */
    @Override
    public String toString() {
        if (isIterative()) {
            IterationConfig config = getIteration();
            return String.format("IterativeWorkflowStage{name='%s', type=%s, parallel=%s, maxConcurrent=%d}",
                getName(), config.getType(), config.isParallelEnabled(), config.getEffectiveMaxConcurrent());
        } else {
            return String.format("WorkflowStage{name='%s'}", getName());
        }
    }
}
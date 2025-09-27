package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Configuration for iterative stage execution.
 * Defines how a stage should iterate over data or repeat execution.
 */
public class IterationConfig {

    /**
     * Types of iteration supported by the workflow engine.
     */
    public enum IterationType {
        /**
         * Iterate based on data from previous stage results.
         * Uses JSON path or variable reference to extract iteration data.
         */
        DATA_DRIVEN("data_driven"),

        /**
         * Iterate a specific number of times with a counter variable.
         */
        COUNT_BASED("count_based"),

        /**
         * Iterate while a condition is true.
         */
        CONDITIONAL("conditional");

        private final String yamlValue;

        IterationType(String yamlValue) {
            this.yamlValue = yamlValue;
        }

        public String getYamlValue() {
            return yamlValue;
        }

        public static IterationType fromYamlValue(String yamlValue) {
            for (IterationType type : values()) {
                if (type.yamlValue.equals(yamlValue)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown iteration type: " + yamlValue);
        }
    }

    private IterationType type = IterationType.DATA_DRIVEN;

    /**
     * For DATA_DRIVEN: JSON path or variable reference to iteration data.
     * Examples: "toc_generation.chapters", "config.languages", "previous_stage.items"
     */
    private String source;

    /**
     * Name of the variable that will hold the current iteration item.
     * This variable is available in templates and agent prompts during iteration.
     */
    private String variable = "item";

    /**
     * For COUNT_BASED: Number of iterations to perform.
     * Can be a literal number or variable reference like "${toc.chapter_count}"
     */
    private String count;

    /**
     * For COUNT_BASED: Starting value for the counter (default: 1).
     */
    private Integer start = 1;

    /**
     * For CONDITIONAL: Condition expression to evaluate each iteration.
     * Example: "${chapters_remaining} > 0"
     */
    private String condition;

    /**
     * For CONDITIONAL: Maximum number of iterations to prevent infinite loops.
     */
    @JsonProperty("max_iterations")
    private Integer maxIterations = 100;

    /**
     * For CONDITIONAL: Variables to update after each iteration.
     * Map of variable names to expressions.
     */
    @JsonProperty("update_variables")
    private Map<String, String> updateVariables;

    /**
     * Whether to execute iterations in parallel (default: false).
     */
    private Boolean parallel = false;

    /**
     * Maximum number of concurrent iterations when parallel is true.
     */
    @JsonProperty("max_concurrent")
    private Integer maxConcurrent = 4;

    /**
     * Strategy for handling failed iterations.
     * Options: "fail_fast", "continue", "retry"
     */
    @JsonProperty("error_strategy")
    private String errorStrategy = "fail_fast";

    /**
     * Number of retries for failed iterations (when error_strategy is "retry").
     */
    @JsonProperty("retry_count")
    private Integer retryCount = 3;

    /**
     * Timeout for each individual iteration (in milliseconds).
     */
    @JsonProperty("iteration_timeout")
    private Long iterationTimeout = 300000L; // 5 minutes

    // Default constructor for Jackson
    public IterationConfig() {}

    // Getters and setters

    public IterationType getType() {
        return type;
    }

    public void setType(IterationType type) {
        this.type = type;
    }

    @JsonProperty("type")
    public void setTypeFromString(String typeString) {
        this.type = IterationType.fromYamlValue(typeString);
    }

    @JsonProperty("type")
    public String getTypeAsString() {
        return type.getYamlValue();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    public Map<String, String> getUpdateVariables() {
        return updateVariables;
    }

    public void setUpdateVariables(Map<String, String> updateVariables) {
        this.updateVariables = updateVariables;
    }

    public Boolean getParallel() {
        return parallel;
    }

    public void setParallel(Boolean parallel) {
        this.parallel = parallel;
    }

    public Integer getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(Integer maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public String getErrorStrategy() {
        return errorStrategy;
    }

    public void setErrorStrategy(String errorStrategy) {
        this.errorStrategy = errorStrategy;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Long getIterationTimeout() {
        return iterationTimeout;
    }

    public void setIterationTimeout(Long iterationTimeout) {
        this.iterationTimeout = iterationTimeout;
    }

    /**
     * Validates the iteration configuration.
     */
    public void validate() throws IllegalArgumentException {
        if (type == null) {
            throw new IllegalArgumentException("Iteration type is required");
        }

        if (variable == null || variable.trim().isEmpty()) {
            throw new IllegalArgumentException("Iteration variable name is required");
        }

        switch (type) {
            case DATA_DRIVEN:
                if (source == null || source.trim().isEmpty()) {
                    throw new IllegalArgumentException("Data-driven iteration requires a source");
                }
                break;

            case COUNT_BASED:
                if (count == null || count.trim().isEmpty()) {
                    throw new IllegalArgumentException("Count-based iteration requires a count");
                }
                if (start != null && start < 0) {
                    throw new IllegalArgumentException("Count-based iteration start must be non-negative");
                }
                break;

            case CONDITIONAL:
                if (condition == null || condition.trim().isEmpty()) {
                    throw new IllegalArgumentException("Conditional iteration requires a condition");
                }
                if (maxIterations != null && maxIterations <= 0) {
                    throw new IllegalArgumentException("Max iterations must be positive");
                }
                break;
        }

        if (parallel != null && parallel && maxConcurrent != null && maxConcurrent <= 0) {
            throw new IllegalArgumentException("Max concurrent must be positive when parallel is enabled");
        }

        if (retryCount != null && retryCount < 0) {
            throw new IllegalArgumentException("Retry count must be non-negative");
        }

        if (iterationTimeout != null && iterationTimeout <= 0) {
            throw new IllegalArgumentException("Iteration timeout must be positive");
        }
    }

    /**
     * Checks if this iteration configuration enables parallel execution.
     */
    public boolean isParallelEnabled() {
        return parallel != null && parallel;
    }

    /**
     * Gets the effective max concurrent iterations (considering parallel setting).
     */
    public int getEffectiveMaxConcurrent() {
        if (!isParallelEnabled()) {
            return 1;
        }
        return maxConcurrent != null ? maxConcurrent : 4;
    }

    @Override
    public String toString() {
        return String.format("IterationConfig{type=%s, source='%s', variable='%s', parallel=%s}",
                type, source, variable, parallel);
    }
}
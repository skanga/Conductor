package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete workflow definition loaded from YAML configuration.
 * This is the root configuration object that defines how a workflow should execute.
 */
public class WorkflowDefinition {

    @JsonProperty("workflow")
    private WorkflowMetadata metadata;

    @JsonProperty("settings")
    private WorkflowSettings settings;

    @JsonProperty("stages")
    private List<WorkflowStage> stages;

    @JsonProperty("variables")
    private Map<String, Object> variables;

    // Default constructor for Jackson
    public WorkflowDefinition() {}

    public WorkflowMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(WorkflowMetadata metadata) {
        this.metadata = metadata;
    }

    public WorkflowSettings getSettings() {
        return settings;
    }

    public void setSettings(WorkflowSettings settings) {
        this.settings = settings;
    }

    public List<WorkflowStage> getStages() {
        return stages;
    }

    public void setStages(List<WorkflowStage> stages) {
        this.stages = stages;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    /**
     * Metadata about the workflow itself.
     */
    public static class WorkflowMetadata {
        private String name;
        private String description;
        private String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    /**
     * Global settings that apply to the entire workflow.
     */
    public static class WorkflowSettings {
        @JsonProperty("output_dir")
        private String outputDir;

        @JsonProperty("max_retries")
        private Integer maxRetries = 3;

        private String timeout = "10m";

        @JsonProperty("target_words_per_chapter")
        private Integer targetWordsPerChapter = 800;

        @JsonProperty("max_words_per_chapter")
        private Integer maxWordsPerChapter = 1200;

        public String getOutputDir() {
            return outputDir;
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public String getTimeout() {
            return timeout;
        }

        public void setTimeout(String timeout) {
            this.timeout = timeout;
        }

        public Integer getTargetWordsPerChapter() {
            return targetWordsPerChapter;
        }

        public void setTargetWordsPerChapter(Integer targetWordsPerChapter) {
            this.targetWordsPerChapter = targetWordsPerChapter;
        }

        public Integer getMaxWordsPerChapter() {
            return maxWordsPerChapter;
        }

        public void setMaxWordsPerChapter(Integer maxWordsPerChapter) {
            this.maxWordsPerChapter = maxWordsPerChapter;
        }
    }

    /**
     * Validates that this workflow definition is complete and valid.
     */
    public void validate() throws IllegalArgumentException {
        if (metadata == null || metadata.getName() == null || metadata.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow metadata with name is required");
        }

        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one stage");
        }

        if (settings == null) {
            settings = new WorkflowSettings(); // Use defaults
        }

        // Validate each stage
        for (WorkflowStage stage : stages) {
            stage.validate();
        }
    }
}
package com.skanga.conductor.workflow.output;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of generating output files for a workflow stage.
 * Contains information about the files that were created and any errors.
 */
public class OutputGenerationResult {

    private final List<Path> generatedFiles;
    private final List<String> errors;
    private final long generationTimeMs;
    private boolean success;

    public OutputGenerationResult() {
        this.generatedFiles = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.generationTimeMs = System.currentTimeMillis();
        this.success = true;
    }

    public void addGeneratedFile(Path filePath) {
        generatedFiles.add(filePath);
    }

    public void addError(String error) {
        errors.add(error);
        success = false;
    }

    public List<Path> getGeneratedFiles() {
        return new ArrayList<>(generatedFiles);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean isSuccess() {
        return success && errors.isEmpty();
    }

    public long getGenerationTimeMs() {
        return System.currentTimeMillis() - generationTimeMs;
    }

    public int getFileCount() {
        return generatedFiles.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets a summary of the output generation.
     */
    public String getSummary() {
        if (isSuccess()) {
            return String.format("Generated %d file(s) in %dms", getFileCount(), getGenerationTimeMs());
        } else {
            return String.format("Failed to generate files: %d error(s)", errors.size());
        }
    }

    @Override
    public String toString() {
        return "OutputGenerationResult{" +
               "files=" + generatedFiles.size() +
               ", errors=" + errors.size() +
               ", success=" + success +
               ", duration=" + getGenerationTimeMs() + "ms" +
               '}';
    }
}
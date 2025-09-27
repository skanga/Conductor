package com.skanga.conductor.workflow.output;

import java.io.IOException;
import java.util.List;

/**
 * Interface for generating output files from workflow execution results.
 * Supports various output formats and file organization strategies.
 */
public interface FileOutputGenerator {

    /**
     * Generates output files for a workflow stage.
     *
     * @param request the output generation request containing stage results and file specifications
     * @return the result of the output generation operation
     * @throws IOException if file writing fails
     */
    OutputGenerationResult generateOutput(OutputGenerationRequest request) throws IOException;

    /**
     * Creates the output directory structure if it doesn't exist.
     *
     * @param outputDir the base output directory path
     * @throws IOException if directory creation fails
     */
    void createOutputDirectory(String outputDir) throws IOException;

    /**
     * Gets the supported file extensions for this generator.
     *
     * @return list of supported file extensions (e.g., "md", "txt", "json")
     */
    List<String> getSupportedExtensions();

    /**
     * Checks if this generator can handle the specified output file.
     *
     * @param outputPath the output file path
     * @return true if this generator can handle the file type
     */
    boolean canHandle(String outputPath);
}
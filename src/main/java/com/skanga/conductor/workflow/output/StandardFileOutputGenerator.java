package com.skanga.conductor.workflow.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard implementation of FileOutputGenerator that creates text and markdown files.
 * Supports variable substitution in file paths and content formatting.
 */
public class StandardFileOutputGenerator implements FileOutputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(StandardFileOutputGenerator.class);

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("md", "txt", "json", "yaml", "yml");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public OutputGenerationResult generateOutput(OutputGenerationRequest request) throws IOException {
        OutputGenerationResult result = new OutputGenerationResult();

        logger.info("Generating output files for stage: {}", request.getStage().getName());

        // Create output directory
        createOutputDirectory(request.getOutputDirectory());

        // Process each output file specification
        if (request.getStage().getOutputs() != null) {
            for (String outputSpec : request.getStage().getOutputs()) {
                try {
                    Path outputFile = generateSingleFile(request, outputSpec);
                    result.addGeneratedFile(outputFile);
                    logger.info("Generated output file: {}", outputFile);
                } catch (IOException e) {
                    String error = "Failed to generate file '" + outputSpec + "': " + e.getMessage();
                    result.addError(error);
                    logger.error(error, e);
                }
            }
        }

        return result;
    }

    private Path generateSingleFile(OutputGenerationRequest request, String outputSpec) throws IOException {
        // Substitute variables in the output file path
        String resolvedPath = substituteVariables(outputSpec, request);

        // Resolve the full file path
        Path outputDir = Paths.get(request.getOutputDirectory());
        Path outputFile = outputDir.resolve(resolvedPath);

        // Ensure parent directories exist
        Files.createDirectories(outputFile.getParent());

        // Generate file content
        String content = generateFileContent(request, outputFile);

        // Write the file
        Files.write(outputFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return outputFile;
    }

    private String substituteVariables(String template, OutputGenerationRequest request) {
        Map<String, Object> allVariables = new HashMap<>();

        // Add workflow variables
        if (request.getVariables() != null) {
            allVariables.putAll(request.getVariables());
        }

        // Add built-in variables
        allVariables.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        allVariables.put("stage_name", request.getStage().getName());
        allVariables.put("workflow_name", request.getWorkflowName());

        // Add stage-specific variables
        if (request.getStage().getName().contains("chapter")) {
            // Extract chapter number from TOC or default to 1
            String chapterNumber = extractChapterNumber(request);
            allVariables.put("chapter_number", chapterNumber);
        }

        return substituteVariablesInString(template, allVariables);
    }

    private String generateFileContent(OutputGenerationRequest request, Path outputFile) {
        StringBuilder content = new StringBuilder();

        // Add file header
        content.append(generateFileHeader(request, outputFile));
        content.append("\n\n");

        // Add main content
        if (request.getPrimaryContent() != null) {
            content.append("## Generated Content\n\n");
            content.append(request.getPrimaryContent());
            content.append("\n\n");
        }

        // Add review content if available
        if (request.hasReview()) {
            content.append("## Review Feedback\n\n");
            content.append(request.getReviewContent());
            content.append("\n\n");
        }

        // Add approval information if applicable
        if (request.getStageResult().isApprovalRequested()) {
            content.append("## Approval Status\n\n");
            String approvalStatus = request.getStageResult().isApproved() ? " **APPROVED**" : " **REJECTED**";
            content.append("**Status:** ").append(approvalStatus).append("\n\n");

            if (request.getStageResult().getApprovalFeedback() != null) {
                content.append("**Feedback:** ").append(request.getStageResult().getApprovalFeedback()).append("\n\n");
            }
        }

        // Add generation metadata
        content.append(generateFileFooter(request));

        return content.toString();
    }

    private String generateFileHeader(OutputGenerationRequest request, Path outputFile) {
        StringBuilder header = new StringBuilder();

        header.append("# ").append(request.getStage().getName().replace("-", " ").toUpperCase()).append("\n");
        header.append("\n");
        header.append("**Workflow:** ").append(request.getWorkflowName()).append("\n");
        header.append("**Stage:** ").append(request.getStage().getName()).append("\n");
        header.append("**Generated:** ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        header.append("**File:** ").append(outputFile.getFileName()).append("\n");

        if (request.getStage().getDescription() != null) {
            header.append("**Description:** ").append(request.getStage().getDescription()).append("\n");
        }

        return header.toString();
    }

    private String generateFileFooter(OutputGenerationRequest request) {
        StringBuilder footer = new StringBuilder();

        footer.append("---\n\n");
        footer.append("*Generated by Conductor No-Code Workflow System*\n");
        footer.append("- Execution Duration: ").append(String.format("%.2fs", request.getStageResult().getDurationSeconds())).append("\n");
        footer.append("- Has Review: ").append(request.hasReview() ? "Yes" : "No").append("\n");

        if (request.getStageResult().isApprovalRequested()) {
            footer.append("- Human Approval: ").append(request.getStageResult().isApproved() ? "Approved" : "Rejected").append("\n");
        }

        footer.append("- Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");

        return footer.toString();
    }

    @Override
    public void createOutputDirectory(String outputDir) throws IOException {
        if (outputDir != null && !outputDir.trim().isEmpty()) {
            Path dir = Paths.get(outputDir);
            Files.createDirectories(dir);
            logger.info("Created output directory: {}", dir.toAbsolutePath());
        }
    }

    @Override
    public List<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public boolean canHandle(String outputPath) {
        if (outputPath == null) {
            return false;
        }

        String lowerPath = outputPath.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(ext -> lowerPath.endsWith("." + ext));
    }

    /**
     * Substitutes variables in a string using the provided variable map.
     */
    private String substituteVariablesInString(String input, Map<String, Object> variables) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Extracts chapter number from the workflow context or defaults to 1.
     */
    private String extractChapterNumber(OutputGenerationRequest request) {
        // Try to get chapter number from request variables
        Object chapterNum = request.getVariables().get("chapter_number");
        if (chapterNum != null) {
            return chapterNum.toString();
        }

        // Try to extract from stage results (TOC parsing)
        // For now, default to 1 - this should be enhanced to parse TOC results
        return "1";
    }
}
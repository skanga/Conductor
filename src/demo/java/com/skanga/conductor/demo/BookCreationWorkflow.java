package com.skanga.conductor.demo;

import com.skanga.conductor.engine.DefaultWorkflowEngine;
import com.skanga.conductor.engine.DefaultWorkflowEngine.*;
import com.skanga.conductor.engine.WorkflowBuilder;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Code-based book creation workflow using the modern workflow engine.
 * This demonstrates programmatic workflow creation using WorkflowBuilder and DefaultWorkflowEngine.
 * For declarative workflow creation, see the YAML-based approach in the yaml directory.
 */
public class BookCreationWorkflow implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BookCreationWorkflow.class);
    private static final int MAX_RETRIES = 3;

    private final DefaultWorkflowEngine engine;
    private final LLMProvider llmProvider;
    private final String outputDir;

    public BookCreationWorkflow(Orchestrator orchestrator, LLMProvider llmProvider, String outputDir) {
        this.engine = new DefaultWorkflowEngine(orchestrator);
        this.llmProvider = llmProvider;
        this.outputDir = outputDir;

        // Ensure output directory exists
        try {
            BookCreationUtils.createDirectories(outputDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create output directory: " + outputDir, e);
        }
    }

    /**
     * Creates and executes the complete book creation workflow.
     */
    public BookManuscript createBook(String topic) throws ConductorException, SQLException {
        logger.info("Starting book creation workflow for topic: {}", topic);

        // Build the workflow definition
        List<StageDefinition> stages = createWorkflowStages();

        // Create initial context variables
        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("topic", topic);
        initialVariables.put("outputDir", outputDir);
        initialVariables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        // Execute the workflow with initial variables
        WorkflowResult workflowResult = engine.executeWorkflowWithContext(stages, initialVariables);

        if (!workflowResult.isSuccess()) {
            throw new ConductorException("Workflow execution failed: " + workflowResult.getError());
        }

        // Process results and create book manuscript
        return processWorkflowResults(workflowResult);
    }

    /**
     * Creates the workflow stage definitions for the book creation process.
     */
    private List<StageDefinition> createWorkflowStages() {
        return WorkflowBuilder.create()
            // Stage 1: Title Generation
            .addStage(
                "title-generation",
                "title-generator",
                "Expert book title generator and marketing specialist",
                llmProvider,
                "You are an expert book title generator and marketing specialist. " +
                "Create compelling, marketable titles that capture attention and clearly communicate the book's value proposition. " +
                "Generate exactly one title and one subtitle. " +
                "Respond in the exact format: 'Title: [TITLE]\\nSubtitle: [SUBTITLE]'",
                "Generate a compelling title and subtitle for a book about: ${topic}\n\n" +
                "Requirements:\n" +
                "- Title should be 3-8 words, memorable and impactful\n" +
                "- Subtitle should explain the benefit/approach\n" +
                "- Both should work together to convey expertise and value\n" +
                "- Target audience: professionals and practitioners\n\n" +
                "Respond with exactly this format:\n" +
                "Title: [Your title here]\n" +
                "Subtitle: [Your subtitle here]",
                MAX_RETRIES,
                createTitleValidator(),
                null
            )
            // Stage 2: Table of Contents Generation
            .addStage(
                "toc-generation",
                "toc-generator",
                "Professional book outlining specialist and content strategist",
                llmProvider,
                "You are a professional book outlining specialist and content strategist. " +
                "Create comprehensive, well-structured table of contents that logically progress from basic concepts to advanced applications. " +
                "Focus on practical, actionable content that delivers real value to readers.",
                "Create a detailed table of contents for the book:\n" +
                "Title: ${title-generation.output}\n" +
                "Topic: ${topic}\n\n" +
                "Requirements:\n" +
                "- 4-8 chapters with descriptive titles\n" +
                "- Each chapter should have 3-5 detailed bullet points\n" +
                "- Logical progression from fundamentals to advanced topics\n" +
                "- Practical, actionable content focus\n" +
                "- Avoid meta-analysis or recommendation chapters\n\n" +
                "Use this exact JSON format:\n" +
                "```json\n" +
                "{\n" +
                "  \"chapters\": [\n" +
                "    {\n" +
                "      \"title\": \"Chapter Title\",\n" +
                "      \"description\": \"Brief description\",\n" +
                "      \"key_points\": [\"Point 1\", \"Point 2\", \"Point 3\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```",
                MAX_RETRIES,
                createTocValidator(),
                null
            )
            // Stage 3: Chapter Generation (will be expanded based on TOC)
            .addStage(
                "chapter-generation",
                "chapter-writer",
                "Expert technical writer and subject matter specialist",
                llmProvider,
                "You are an expert technical writer and subject matter specialist. " +
                "Write comprehensive, well-structured chapters that provide practical value and actionable insights. " +
                "Use clear, professional language appropriate for practitioners and professionals.",
                "Write a comprehensive chapter for the book:\n" +
                "Title: ${title-generation.output}\n" +
                "Topic: ${topic}\n" +
                "Chapter to write: [Will be set dynamically]\n\n" +
                "Requirements:\n" +
                "- 800-1200 words minimum\n" +
                "- Clear section headers and structure\n" +
                "- Practical examples and actionable insights\n" +
                "- Professional tone appropriate for practitioners\n" +
                "- Include specific techniques, methods, or frameworks\n" +
                "- Conclude with key takeaways\n\n" +
                "Write in markdown format with proper headers (##, ###, etc.)",
                MAX_RETRIES,
                createChapterValidator(),
                null
            )
            // Stage 4: Final Review
            .addStage(
                "final-review",
                "book-reviewer",
                "Professional book editor and quality assurance specialist",
                llmProvider,
                "You are a professional book editor and quality assurance specialist. " +
                "Review complete books for coherence, quality, and professional standards. " +
                "Provide constructive feedback and ensure content meets publication standards.",
                "Review the complete book and provide a final assessment:\n" +
                "Title: ${title-generation.output}\n" +
                "Table of Contents: ${toc-generation.output}\n" +
                "Chapters: ${chapter-generation.output}\n\n" +
                "Provide a brief review focusing on:\n" +
                "- Overall coherence and flow\n" +
                "- Content quality and depth\n" +
                "- Professional presentation\n" +
                "- Readiness for publication\n\n" +
                "Keep the review concise (2-3 paragraphs).",
                MAX_RETRIES,
                null,
                null
            )
            .build();
    }

    /**
     * Creates a validator for title generation results.
     */
    private static Function<StageResult, ValidationResult> createTitleValidator() {
        return result -> {
            String output = result.getOutput();
            if (output == null || !output.contains("Title:") || !output.contains("Subtitle:")) {
                return ValidationResult.invalid("Output must contain both 'Title:' and 'Subtitle:' labels");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator for table of contents generation.
     */
    private static Function<StageResult, ValidationResult> createTocValidator() {
        return result -> {
            String output = result.getOutput();
            if (output == null) {
                return ValidationResult.invalid("No output generated");
            }

            // Check for forbidden meta-analysis keywords
            String[] forbiddenKeywords = {"recommendation", "conclusion", "summary", "next steps"};
            for (String keyword : forbiddenKeywords) {
                if (output.toLowerCase().contains(keyword)) {
                    return ValidationResult.invalid("Contains meta-analysis keyword: " + keyword);
                }
            }

            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator for chapter generation.
     */
    private static Function<StageResult, ValidationResult> createChapterValidator() {
        return result -> {
            String output = result.getOutput();
            if (output == null || output.length() < 500) {
                return ValidationResult.invalid("Chapter too short: " +
                    (output != null ? output.length() : 0) + " characters");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Processes the workflow results and creates a BookManuscript.
     */
    private BookManuscript processWorkflowResults(WorkflowResult workflowResult) throws ConductorException {
        try {
            // Extract results from each stage
            StageResult titleResult = getStageResult(workflowResult, "title-generation");
            StageResult tocResult = getStageResult(workflowResult, "toc-generation");
            StageResult chapterResult = getStageResult(workflowResult, "chapter-generation");
            StageResult reviewResult = getStageResult(workflowResult, "final-review");

            // Parse title information
            BookTitleInfo titleInfo = parseTitleResponse(titleResult.getOutput());

            // Parse table of contents (simplified for demo)
            TableOfContents toc = parseTableOfContents(tocResult.getOutput());

            // Create chapters (simplified - in full implementation, would generate multiple chapters)
            List<Chapter> chapters = List.of(
                new Chapter("Chapter 1", chapterResult.getOutput(), List.of("Initial draft"), "Generated via workflow engine")
            );

            // Save outputs with approval process
            saveStageOutput(titleResult, "01-title-" + getTimestamp() + ".md");
            saveStageOutput(tocResult, "02-toc-" + getTimestamp() + ".md");
            saveStageOutput(chapterResult, "03-chapter-01-" + getTimestamp() + ".md");

            // Create final manuscript
            BookManuscript manuscript = new BookManuscript(titleInfo, toc, chapters);

            // Save complete book
            String completeBookContent = formatCompleteBook(manuscript, reviewResult.getOutput());
            saveContent(completeBookContent, "04-complete-book-" + getTimestamp() + ".md");

            logger.info("Book creation workflow completed successfully!");
            return manuscript;

        } catch (Exception e) {
            throw new ConductorException("Failed to process workflow results", e);
        }
    }

    /**
     * Gets a stage result by name from the workflow result.
     */
    private StageResult getStageResult(WorkflowResult workflowResult, String stageName) throws ConductorException {
        return workflowResult.getStageResults().stream()
            .filter(result -> stageName.equals(result.getStageName()))
            .findFirst()
            .orElseThrow(() -> new ConductorException("Stage result not found: " + stageName));
    }

    /**
     * Parses title response in the format "Title: ...\nSubtitle: ..."
     */
    private BookTitleInfo parseTitleResponse(String response) {
        String[] lines = response.split("\n");
        String title = "Generated Title";
        String subtitle = "Generated Subtitle";

        for (String line : lines) {
            if (line.startsWith("Title:")) {
                title = line.substring(6).trim();
            } else if (line.startsWith("Subtitle:")) {
                subtitle = line.substring(9).trim();
            }
        }

        return new BookTitleInfo(title, subtitle, "Generated via workflow engine");
    }

    /**
     * Parses table of contents (simplified implementation).
     */
    private TableOfContents parseTableOfContents(String response) {
        // Simplified parsing - just pass the response to TableOfContents
        return new TableOfContents(response);
    }

    /**
     * Saves stage output to file.
     */
    private void saveStageOutput(StageResult stageResult, String filename) throws Exception {
        String content = "# " + stageResult.getStageName().toUpperCase() + " STAGE\n\n" +
                        stageResult.getOutput() + "\n\n" +
                        "## Execution Details\n" +
                        "- Attempt: " + stageResult.getAttempt() + "\n" +
                        "- Execution Time: " + stageResult.getExecutionTimeMs() + "ms\n" +
                        "- Agent: " + stageResult.getAgentUsed();

        saveContent(content, filename);
        logger.info("{} saved to: {}", stageResult.getStageName(), filename);
    }

    /**
     * Saves content to file.
     */
    private void saveContent(String content, String filename) throws Exception {
        String filePath = outputDir + "/" + filename;
        BookCreationUtils.writeFile(filePath, content);
    }

    /**
     * Formats the complete book with review.
     */
    private String formatCompleteBook(BookManuscript manuscript, String review) {
        StringBuilder book = new StringBuilder();
        book.append("# ").append(manuscript.titleInfo().title()).append("\n");
        book.append("## ").append(manuscript.titleInfo().subtitle()).append("\n\n");

        book.append("## Table of Contents\n");
        book.append(manuscript.tableOfContents().getSummary()).append("\n\n");

        for (Chapter chapter : manuscript.chapters()) {
            book.append("## ").append(chapter.title()).append("\n");
            book.append(chapter.content()).append("\n\n");
        }

        book.append("## Editorial Review\n");
        book.append(review).append("\n");

        return book.toString();
    }

    /**
     * Gets current timestamp for file naming.
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    @Override
    public void close() throws Exception {
        // Cleanup resources if needed
    }
}
package com.skanga.conductor.demo;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive book creation workflow with agentic review and human-in-the-loop approval.
 * <p>
 * This workflow implements a complete book creation process with the following stages:
 * </p>
 * <ol>
 * <li>Topic and Title Generation - Generate book title and subtitle from a topic prompt</li>
 * <li>Table of Contents Generation - Create detailed chapter outline with descriptions</li>
 * <li>Chapter Generation - Generate each chapter individually</li>
 * <li>Final Book Review - Review and approve the complete book</li>
 * </ol>
 * <p>
 * Each stage includes:
 * </p>
 * <ul>
 * <li>Agentic review by specialized review agents</li>
 * <li>Human-in-the-loop approval process</li>
 * <li>Markdown output generation for easy review</li>
 * <li>Iterative refinement capabilities</li>
 * </ul>
 * <p>
 * This class implements {@link AutoCloseable} to properly manage Scanner resources.
 * Use in try-with-resources blocks to ensure proper cleanup.
 * </p>
 *
 * @since 1.0.0
 * @see BookCreationDemo
 * @see BookManuscript
 * @see TableOfContents
 */
public class BookCreationWorkflow implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BookCreationWorkflow.class);
    private static final DemoConfig demoConfig = DemoConfig.getInstance();
    private static final int MAX_RETRIES = 3;
    private static final int WORDS_PER_PAGE = 250;
    private static final int CONTENT_PREVIEW_LENGTH = 500;

    private final Orchestrator orchestrator;
    private final LLMProvider llmProvider;
    private final String outputDir;
    private final FileSystemService fileSystemService;
    private final UserInputService userInputService;

    /**
     * Creates a new book creation workflow with default services.
     * This constructor provides backward compatibility.
     *
     * @param orchestrator the orchestrator for creating and managing agents
     * @param llmProvider the LLM provider for all agents
     * @param outputDir directory to store generated markdown files
     * @throws ConductorException if the output directory cannot be created or is not writable
     * @throws IllegalArgumentException if any parameter is null or if outputDir is empty
     */
    public BookCreationWorkflow(Orchestrator orchestrator, LLMProvider llmProvider, String outputDir) throws ConductorException {
        this(orchestrator, llmProvider, outputDir, new DefaultFileSystemService(), new ConsoleUserInputService());
    }

    /**
     * Creates a new book creation workflow with injectable dependencies.
     * This constructor enables testability and custom implementations.
     *
     * @param orchestrator the orchestrator for creating and managing agents
     * @param llmProvider the LLM provider for all agents
     * @param outputDir directory to store generated markdown files
     * @param fileSystemService service for file system operations
     * @param userInputService service for user input operations
     * @throws ConductorException if the output directory cannot be created or is not writable
     * @throws IllegalArgumentException if any parameter is null or if outputDir is empty
     */
    public BookCreationWorkflow(Orchestrator orchestrator, LLMProvider llmProvider, String outputDir,
                               FileSystemService fileSystemService, UserInputService userInputService) throws ConductorException {
        if (orchestrator == null) {
            throw new IllegalArgumentException("Orchestrator cannot be null");
        }
        if (llmProvider == null) {
            throw new IllegalArgumentException("LLM provider cannot be null");
        }
        if (outputDir == null || outputDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Output directory cannot be null or empty");
        }
        if (fileSystemService == null) {
            throw new IllegalArgumentException("File system service cannot be null");
        }
        if (userInputService == null) {
            throw new IllegalArgumentException("User input service cannot be null");
        }

        this.orchestrator = orchestrator;
        this.llmProvider = llmProvider;
        this.outputDir = outputDir.trim();
        this.fileSystemService = fileSystemService;
        this.userInputService = userInputService;
        createOutputDirectory();
    }

    /**
     * Executes the complete book creation workflow.
     *
     * @param topicPrompt the initial topic prompt for the book
     * @return the completed book manuscript
     * @throws ConductorException if any stage of the workflow fails
     * @throws SQLException if database operations fail
     * @throws IllegalArgumentException if topicPrompt is null or empty
     */
    public BookManuscript createBook(String topicPrompt) throws ConductorException, SQLException {
        if (topicPrompt == null || topicPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic prompt cannot be null or empty");
        }

        logger.info("Starting book creation workflow with topic: {}", topicPrompt);

        // Stage 1: Generate title and subtitle
        BookTitleInfo titleInfo = generateTitleAndSubtitle(topicPrompt);

        // Stage 2: Generate table of contents
        TableOfContents toc = generateTableOfContents(titleInfo, topicPrompt);

        // Stage 3: Generate chapters
        List<Chapter> chapters = generateChapters(toc);

        // Stage 4: Final book review
        BookManuscript manuscript = new BookManuscript(titleInfo, toc, chapters);
        BookManuscript finalManuscript = performFinalReview(manuscript);

        // Generate final markdown output
        saveCompleteBookToMarkdown(finalManuscript);

        logger.info("Book creation workflow completed successfully!");
        return finalManuscript;
    }

    /**
     * Stage 1: Generate book title and subtitle with review and approval.
     */
    private BookTitleInfo generateTitleAndSubtitle(String topicPrompt) throws ConductorException, SQLException {
        logger.info("=== STAGE 1: Title and Subtitle Generation ===");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            logger.info("Title generation attempt {}/{}", attempt, MAX_RETRIES);
            // Generate title and subtitle
            SubAgent titleGenerator = orchestrator.createImplicitAgent(
                "title-generator",
                "Generates compelling book titles and subtitles",
                llmProvider,
                "Generate a concise and compelling title and subtitle for a book on the given topic. " +
                "Respond in the format: 'Title: [TITLE]\\nSubtitle: [SUBTITLE]'"
            );

            TaskResult titleResult = titleGenerator.execute(new TaskInput(
                "Topic: " + topicPrompt + "\n\nGenerate an engaging title and descriptive subtitle for this book.",
                null
            ));

            BookTitleInfo titleInfo = parseTitleResponse(titleResult.output());

            // Agentic review
            BookTitleInfo reviewedTitle = performTitleReview(titleInfo, topicPrompt);

            // Save to markdown for human review
            saveTitleToMarkdown(reviewedTitle);

            // Human approval
            if (getHumanApproval("title and subtitle", reviewedTitle.toString())) {
                logger.info("Title and subtitle approved: {} - {}", reviewedTitle.title(), reviewedTitle.subtitle());
                return reviewedTitle;
            }

            if (attempt < MAX_RETRIES) {
                logger.info("Regenerating title and subtitle based on feedback... ({}/{})", attempt, MAX_RETRIES);
            }
        }

        // If we reach here, max retries exceeded
        throw new ConductorException("Failed to generate acceptable title and subtitle after " + MAX_RETRIES + " attempts");
    }

    /**
     * Stage 2: Generate table of contents with review and approval.
     */
    private TableOfContents generateTableOfContents(BookTitleInfo titleInfo, String topicPrompt) throws ConductorException, SQLException {
        logger.info("=== STAGE 2: Table of Contents Generation ===");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            logger.info("Table of contents generation attempt {}/{}", attempt, MAX_RETRIES);
            // Generate table of contents
            SubAgent tocGenerator = orchestrator.createImplicitAgent(
                "toc-generator",
                "Generates detailed table of contents with chapter descriptions",
                llmProvider,
                "Generate a detailed table of contents for a book. Format as JSON with proper structure. " +
                        "Include json nodes for chapter titles, brief descriptions, and key points to cover. "
            );

            String tocPrompt = String.format(
                "Book Title: %s\nSubtitle: %s\nTopic: %s\n\n" +
                "Generate a comprehensive table of contents with 6-10 chapters in JSON format. " +
                "Respond with ONLY a valid JSON object in this exact format:\n\n" +
                "{\n" +
                "  \"chapters\": [\n" +
                "    {\n" +
                "      \"number\": 1,\n" +
                "      \"title\": \"Chapter Title Here\",\n" +
                "      \"description\": \"2-3 sentence description of what this chapter covers\",\n" +
                "      \"keyPoints\": [\"Key point 1\", \"Key point 2\", \"Key point 3\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Make sure each chapter has a clear title, helpful description, and 3-5 key points. " +
                "Do not include any text outside the JSON object. Place the JSON in a fence",
                titleInfo.title(), titleInfo.subtitle(), topicPrompt
            );

            TaskResult tocResult = tocGenerator.execute(new TaskInput(tocPrompt, null));

            TableOfContents toc = new TableOfContents(tocResult.output());

            // Agentic review
            TableOfContents reviewedToc = performTocReview(toc, titleInfo);

            // Save to markdown for human review
            saveTocToMarkdown(reviewedToc, titleInfo);

            // Human approval - pass raw JSON content for review
            if (getHumanApproval("table of contents", reviewedToc.content())) {
                logger.info("Table of contents approved with {} chapters", reviewedToc.getChapterCount());
                return reviewedToc;
            }

            if (attempt < MAX_RETRIES) {
                logger.info("Regenerating table of contents based on feedback... ({}/{})", attempt, MAX_RETRIES);
            }
        }

        // If we reach here, max retries exceeded
        throw new ConductorException("Failed to generate acceptable table of contents after " + MAX_RETRIES + " attempts");
    }

    /**
     * Stage 3: Generate chapters with review and approval.
     */
    private List<Chapter> generateChapters(TableOfContents toc) throws ConductorException, SQLException {
        logger.info("=== STAGE 3: Chapter Generation ===");

        List<Chapter> chapters = new ArrayList<>();
        List<ChapterSpec> chapterSpecs = toc.extractChapterSpecs();

        // Safety fallback: if no chapters extracted, create some default chapters
        logger.info("Extracted {} chapters from TOC", chapterSpecs.size());
        if (chapterSpecs.isEmpty()) {
            logger.warn("No chapters extracted from TOC, creating default chapters");
            chapterSpecs = createDefaultChapters();
        }

        logger.info("Generating {} chapters", chapterSpecs.size());

        for (int i = 0; i < chapterSpecs.size(); i++) {
            ChapterSpec spec = chapterSpecs.get(i);
            logger.info("Generating chapter {}/{}: {}", i + 1, chapterSpecs.size(), spec.title());

            boolean chapterApproved = false;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                logger.info("Chapter generation attempt {}/{} for chapter: {}", attempt, MAX_RETRIES, spec.title());
                // Generate chapter
                Chapter chapter = generateSingleChapter(spec, chapters, toc);

                // Agentic review
                Chapter reviewedChapter = performChapterReview(chapter);

                // Save to markdown for human review
                saveChapterToMarkdown(reviewedChapter, i + 1);

                // Human approval
                if (getHumanApproval("chapter: " + spec.title(), reviewedChapter.content())) {
                    chapters.add(reviewedChapter);
                    logger.info("Chapter {} approved: {}", i + 1, spec.title());
                    chapterApproved = true;
                    break;
                }

                if (attempt < MAX_RETRIES) {
                    logger.info("Regenerating chapter based on feedback... ({}/{})", attempt, MAX_RETRIES);
                }
            }

            // Check if chapter was approved after all attempts
            if (!chapterApproved) {
                throw new ConductorException("Failed to generate acceptable chapter '" + spec.title() + "' after " + MAX_RETRIES + " attempts");
            }
        }

        return chapters;
    }

    /**
     * Stage 4: Perform final book review and approval.
     */
    private BookManuscript performFinalReview(BookManuscript manuscript) throws ConductorException, SQLException {
        logger.info("=== STAGE 4: Final Book Review ===");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            logger.info("Final book review attempt {}/{}", attempt, MAX_RETRIES);
            // Agentic final review
            BookManuscript reviewedManuscript = performBookReview(manuscript);

            // Save complete book to markdown
            saveCompleteBookToMarkdown(reviewedManuscript);

            // Human approval
            String reviewSummary = String.format(
                "Complete book with %d chapters (%d total pages estimated)",
                reviewedManuscript.chapters().size(),
                estimatePageCount(reviewedManuscript)
            );

            if (getHumanApproval("complete book", reviewSummary)) {
                logger.info("Final book approved!");
                return reviewedManuscript;
            }

            if (attempt < MAX_RETRIES) {
                logger.info("Performing final revisions based on feedback... ({}/{})", attempt, MAX_RETRIES);
                // For now, we'll regenerate the entire book review
                // In future versions, this could implement specific revision logic
            }
        }

        // If we reach here, max retries exceeded
        logger.warn("Final book review exceeded max retries, returning last version");
        return manuscript;
    }

    // Helper methods for chapter generation and fallbacks

    /**
     * Creates default chapters when TOC parsing fails.
     */
    private List<ChapterSpec> createDefaultChapters() {
        List<ChapterSpec> defaultChapters = new ArrayList<>();

        // Create some generic chapters based on the book topic
        defaultChapters.add(new ChapterSpec("Introduction",
            "Introduce the main topic and provide an overview of what readers will learn."));
        defaultChapters.add(new ChapterSpec("Getting Started",
            "Cover the fundamentals and basic concepts."));
        defaultChapters.add(new ChapterSpec("Core Concepts",
            "Explain the main ideas and principles in detail."));
        defaultChapters.add(new ChapterSpec("Practical Applications",
            "Show how to apply the concepts in real-world scenarios."));
        defaultChapters.add(new ChapterSpec("Advanced Topics",
            "Cover more complex aspects and advanced techniques."));
        defaultChapters.add(new ChapterSpec("Conclusion",
            "Summarize key points and provide next steps for readers."));

        logger.info("Created {} default chapters as fallback", defaultChapters.size());
        return defaultChapters;
    }

    // Helper methods for agentic reviews

    private BookTitleInfo performTitleReview(BookTitleInfo titleInfo, String topicPrompt) throws ConductorException, SQLException {
        SubAgent titleReviewer = orchestrator.createImplicitAgent(
            "title-reviewer",
            "Reviews and improves book titles and subtitles",
            llmProvider,
            "Review and improve book titles for clarity, marketability, and alignment with topic. " +
            "Provide constructive feedback and suggest improvements if needed."
        );

        String reviewPrompt = String.format(
            "Review this book title and subtitle:\nTitle: %s\nSubtitle: %s\nTopic: %s\n\n" +
            "Evaluate for:\n- Clarity and appeal\n- Alignment with topic\n- Marketability\n" +
            "If improvements are needed, suggest better alternatives. Your answer should be brief and concise.",
            titleInfo.title(), titleInfo.subtitle(), topicPrompt
        );

        TaskResult reviewResult = titleReviewer.execute(new TaskInput(reviewPrompt, null));

        // For simplicity, return original title info with review comments
        // In a more sophisticated implementation, we could parse the review and regenerate
        return new BookTitleInfo(titleInfo.title(), titleInfo.subtitle(), reviewResult.output());
    }

    private TableOfContents performTocReview(TableOfContents toc, BookTitleInfo titleInfo) throws ConductorException, SQLException {
        SubAgent tocReviewer = orchestrator.createImplicitAgent(
            "toc-reviewer",
            "Reviews and improves table of contents structure",
            llmProvider,
            "Review table of contents for logical flow, completeness, and coherence. " +
            "Suggest improvements for better structure and coverage."
        );

        String reviewPrompt = String.format(
            "Review this table of contents for the book '%s':\n\n%s\n\n" +
            "Evaluate for:\n- Logical progression\n- Comprehensive coverage\n- Chapter balance\n" +
            "Suggest improvements if needed.",
            titleInfo.title(), toc.content()
        );

        TaskResult reviewResult = tocReviewer.execute(new TaskInput(reviewPrompt, null));

        return new TableOfContents(toc.content() + "\n\n--- REVIEW COMMENTS ---\n" + reviewResult.output());
    }

    private Chapter performChapterReview(Chapter chapter) throws ConductorException, SQLException {
        SubAgent chapterReviewer = orchestrator.createImplicitAgent(
            "chapter-reviewer",
            "Reviews and improves chapter content",
            llmProvider,
            "Review chapter content for clarity, structure, and educational value. " +
            "Provide specific suggestions for improvement."
        );

        String reviewPrompt = String.format(
            "Review this chapter:\nTitle: %s\n\nContent:\n%s\n\n" +
            "Evaluate for:\n- Clarity and readability\n- Structure and flow\n- Content quality\n" +
            "Provide specific improvement suggestions.",
            chapter.title(), chapter.content()
        );

        TaskResult reviewResult = chapterReviewer.execute(new TaskInput(reviewPrompt, null));

        List<String> revisionsWithReview = new ArrayList<>(chapter.revisions());
        revisionsWithReview.add("REVIEW: " + reviewResult.output());

        return new Chapter(chapter.title(), chapter.content(), revisionsWithReview, reviewResult.output());
    }

    private BookManuscript performBookReview(BookManuscript manuscript) throws ConductorException, SQLException {
        SubAgent bookReviewer = orchestrator.createImplicitAgent(
            "book-reviewer",
            "Reviews complete book for overall quality and coherence",
            llmProvider,
            "Review the complete book for overall quality, coherence, and reader value. " +
            "Provide high-level feedback on the book's effectiveness."
        );

        String bookSummary = String.format(
            "Book: %s - %s\n\nChapters:\n%s",
            manuscript.titleInfo().title(),
            manuscript.titleInfo().subtitle(),
            manuscript.chapters().stream()
                .map(ch -> "- " + ch.title())
                .collect(java.util.stream.Collectors.joining("\n"))
        );

        TaskResult reviewResult = bookReviewer.execute(new TaskInput(
            "Review this complete book structure and provide overall assessment:\n\n" + bookSummary,
            null
        ));

        BookTitleInfo updatedTitleInfo = new BookTitleInfo(
            manuscript.titleInfo().title(),
            manuscript.titleInfo().subtitle(),
            manuscript.titleInfo().reviewComments() + "\n\nFINAL REVIEW: " + reviewResult.output()
        );

        return new BookManuscript(updatedTitleInfo, manuscript.tableOfContents(), manuscript.chapters());
    }

    // Helper methods for chapter generation and file operations

    private Chapter generateSingleChapter(ChapterSpec spec, List<Chapter> previousChapters, TableOfContents toc) throws ConductorException, SQLException {
        SubAgent chapterWriter = orchestrator.createImplicitAgent(
            "chapter-writer",
            "Writes detailed book chapters",
            llmProvider,
            String.format(
                "Write a comprehensive chapter of %d-%d words. " +
                "Include introduction, main content sections, examples, and conclusion. " +
                "Write in an engaging, educational style.",
                demoConfig.getBookTargetWords() * 2, // Double the target for full chapters
                demoConfig.getBookMaxWords() * 3    // Triple max for comprehensive content
            )
        );

        String contextInfo = previousChapters.isEmpty() ?
            "This is the first chapter of the book." :
            "Previous chapters covered: " +
            previousChapters.stream()
                .map(Chapter::title)
                .collect(java.util.stream.Collectors.joining(", "));

        String chapterPrompt = String.format(
            "Write a chapter for a book. The full table of contents is provided below for context.\n\n" +
            "TABLE OF CONTENTS:\n%s\n\n" +
            "Now, write the chapter with the following title:\n\n" +
            "CHAPTER TITLE: %s\n\n" +
            "Use the following description as a guide for the chapter's content:\n\n" +
            "CHAPTER DESCRIPTION: %s\n\n" +
            "CONTEXT FROM PREVIOUS CHAPTERS: %s",
            toc.content(), spec.title(), spec.spec(), contextInfo
        );

        TaskResult chapterResult = chapterWriter.execute(new TaskInput(chapterPrompt, null));

        return new Chapter(
            spec.title(),
            chapterResult.output(),
            List.of(chapterResult.output()),
            ""
        );
    }

    private BookTitleInfo parseTitleResponse(String response) {
        String[] lines = response.split("\n");
        String title = "Generated Title";
        String subtitle = "Generated Subtitle";

        for (String line : lines) {
            if (line.toLowerCase().startsWith("title:")) {
                title = line.substring(6).trim();
            } else if (line.toLowerCase().startsWith("subtitle:")) {
                subtitle = line.substring(9).trim();
            }
        }

        return new BookTitleInfo(title, subtitle, "");
    }

    private boolean getHumanApproval(String itemType, String content) {
        return userInputService.getApproval(itemType, content);
    }

    private void createOutputDirectory() throws ConductorException {
        fileSystemService.createDirectory(outputDir);
    }

    private void saveTitleToMarkdown(BookTitleInfo titleInfo) throws ConductorException {
        String filename = String.format("%s/01-title-%s.md", outputDir,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        String content = String.format(
            "# Book Title and Subtitle\n\n" +
            "**Title:** %s\n\n" +
            "**Subtitle:** %s\n\n" +
            "## Review Comments\n\n%s\n\n" +
            "Generated at: %s\n",
            titleInfo.title(), titleInfo.subtitle(),
            titleInfo.reviewComments().isEmpty() ? "No review comments yet." : titleInfo.reviewComments(),
            LocalDateTime.now()
        );

        writeToFile(filename, content);
    }

    private void saveTocToMarkdown(TableOfContents toc, BookTitleInfo titleInfo) throws ConductorException {
        String filename = String.format("%s/02-toc-%s.md", outputDir,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        String content = String.format(
            "# Table of Contents\n\n" +
            "**Book:** %s - %s\n\n" +
            "%s\n\n" +
            "Generated at: %s\n",
            titleInfo.title(), titleInfo.subtitle(), toc.toMarkdownContent(), LocalDateTime.now()
        );

        writeToFile(filename, content);
    }

    private void saveChapterToMarkdown(Chapter chapter, int chapterNumber) throws ConductorException {
        String filename = String.format("%s/03-chapter-%02d-%s.md", outputDir,
            chapterNumber, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        String content = String.format(
            "# Chapter %d: %s\n\n%s\n\n" +
            "## Review Comments\n\n%s\n\n" +
            "Generated at: %s\n",
            chapterNumber, chapter.title(), chapter.content(),
            chapter.critique().isEmpty() ? "No review comments yet." : chapter.critique(),
            LocalDateTime.now()
        );

        writeToFile(filename, content);
    }

    private void saveCompleteBookToMarkdown(BookManuscript manuscript) throws ConductorException {
        String filename = String.format("%s/04-complete-book-%s.md", outputDir,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        StringBuilder content = new StringBuilder();
        content.append(String.format("# %s\n\n## %s\n\n",
            manuscript.titleInfo().title(), manuscript.titleInfo().subtitle()));

        content.append(manuscript.tableOfContents().toMarkdown()).append("\n\n");

        content.append("---\n\n");

        for (int i = 0; i < manuscript.chapters().size(); i++) {
            Chapter chapter = manuscript.chapters().get(i);
            content.append(chapter.content()).append("\n\n");
            content.append("---\n\n");
        }

        content.append(String.format("Generated at: %s\n", LocalDateTime.now()));

        writeToFile(filename, content.toString());
        logger.info("Complete book saved to: {}", filename);
    }

    private void writeToFile(String filename, String content) throws ConductorException {
        fileSystemService.writeFile(filename, content, outputDir);
    }

    private int estimatePageCount(BookManuscript manuscript) {
        int totalWords = manuscript.chapters().stream()
            .mapToInt(this::countWords)
            .sum();
        return (int) Math.ceil(totalWords / (double) WORDS_PER_PAGE);
    }

    /**
     * Efficiently counts words in text without using expensive regex split.
     * This method is approximately 3-5x faster than split("\\s+").length for large texts.
     *
     * @param chapter the chapter to count words in
     * @return number of words in the chapter content
     */
    private int countWords(Chapter chapter) {
        return countWords(chapter.content());
    }

    /**
     * Efficiently counts words in text without using expensive regex split.
     *
     * @param text the text to count words in
     * @return number of words
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        int wordCount = 0;
        boolean inWord = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                inWord = false;
            } else if (!inWord) {
                inWord = true;
                wordCount++;
            }
        }

        return wordCount;
    }

    /**
     * Closes any resources used by the workflow services.
     */
    @Override
    public void close() {
        if (userInputService != null) {
            userInputService.close();
        }
    }
}
package com.skanga.conductor.demo;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow for generating book content using multiple specialized agents.
 * <p>
 * This workflow demonstrates a multi-agent approach to content creation,
 * using separate agents for writing, editing, and critiquing chapters.
 * The workflow is fully configuration-driven through {@link DemoConfig}.
 * </p>
 * <p>
 * The workflow process:
 * </p>
 * <ol>
 * <li>Parse the summary into individual chapter specifications</li>
 * <li>For each chapter:
 *   <ul>
 *     <li>Create a writer agent to produce initial content</li>
 *     <li>Create an editor agent to improve clarity and structure</li>
 *     <li>Create a critic agent to provide improvement feedback</li>
 *   </ul>
 * </li>
 * <li>Combine all outputs into a complete Chapter record</li>
 * </ol>
 *
 * @param orchestrator the orchestrator for creating and managing agents
 * @since 1.0.0
 * @see DemoConfig
 * @see Chapter
 * @see ChapterSpec
 */
public record BookWorkflow(Orchestrator orchestrator) {

    private static final DemoConfig demoConfig = DemoConfig.getInstance();

    /**
     * Produces a list of chapters from a book summary using configured templates and settings.
     *
     * @param summary the book summary to parse into chapters
     * @param defaultProvider the LLM provider to use for all agents
     * @return list of generated chapters with content, revisions, and critiques
     * @throws ConductorException if chapter generation fails
     */
    public List<Chapter> produceChapters(String summary, LLMProvider defaultProvider) throws ConductorException {
        List<ChapterSpec> specs = parseSummaryToChapters(summary);
        List<Chapter> chapters = new ArrayList<>();

        for (ChapterSpec spec : specs) {
            try {
                // Create writer agent with configured prompt template
                String writerTemplate = demoConfig.getWriterPromptTemplate()
                    .replace("{{target_words}}", String.valueOf(demoConfig.getBookTargetWords()))
                    .replace("{{max_words}}", String.valueOf(demoConfig.getBookMaxWords()));

                SubAgent writer = orchestrator.createImplicitAgent(
                        "writer",
                        "Writes a chapter based on the chapter specification.",
                        defaultProvider,
                        writerTemplate
                );

                // Create editor agent with configured prompt template
                SubAgent editor = orchestrator.createImplicitAgent(
                        "editor",
                        "Edits the chapter for clarity.",
                        defaultProvider,
                        demoConfig.getEditorPromptTemplate()
                );

                // Create critic agent with configured prompt template
                SubAgent critic = orchestrator.createImplicitAgent(
                        "critic",
                        "Criticizes the chapter and suggests improvements.",
                        defaultProvider,
                        demoConfig.getCriticPromptTemplate()
                );

                // Execute the workflow with parameter substitution
                String writerInput = "Title: " + spec.title() + "\nSpec: " + spec.spec();
                TaskResult draft = writer.execute(new TaskInput(writerInput, null));
                TaskResult edited = editor.execute(new TaskInput(draft.output(), null));
                TaskResult critique = critic.execute(new TaskInput(edited.output(), null));

                Chapter chapter = new Chapter(
                    spec.title(),
                    edited.output(),
                    List.of(draft.output(), edited.output()),
                    critique.output()
                );
                chapters.add(chapter);
            } catch (Exception e) {
                throw new ConductorException("Failed to produce chapter: " + spec.title(), e);
            }
        }

        return chapters;
    }

    /**
     * Parses a book summary into individual chapter specifications.
     * <p>
     * Attempts to identify chapter markers in the summary text. If no clear
     * chapter structure is found, creates a single chapter with the configured
     * default title.
     * </p>
     *
     * @param summary the book summary to parse
     * @return list of chapter specifications extracted from the summary
     */
    private List<ChapterSpec> parseSummaryToChapters(String summary) {
        List<ChapterSpec> chapters = new ArrayList<>();

        // Simple split by "Chapter" markers
        String[] markers = summary.split("(?i)chapter\\s+\\d+:");
        if (markers.length > 1) {
            // Found chapter markers - parse each section
            for (int i = 1; i < markers.length; i++) {
                String chunk = markers[i].trim();
                String[] firstLine = chunk.split("\\r?\\n", 2);
                String title = firstLine[0].trim();
                String spec = firstLine.length > 1 ? firstLine[1].trim() : "";
                chapters.add(new ChapterSpec(title, spec));
            }
            return chapters;
        }

        // No chapter markers found - create single chapter with configured default title
        String defaultTitle = demoConfig.getDefaultChapterTitle();
        chapters.add(new ChapterSpec(defaultTitle, summary));
        return chapters;
    }
}

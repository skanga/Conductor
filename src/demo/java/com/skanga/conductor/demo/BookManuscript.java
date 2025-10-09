package com.skanga.conductor.demo;

import java.util.List;

/**
 * Represents a complete book manuscript with all components.
 * <p>
 * This record encapsulates the complete book creation output, including
 * the title information, table of contents, and all generated chapters.
 * It represents the final deliverable from the book creation workflow.
 * </p>
 *
 * @param titleInfo the book title and subtitle information with review comments
 * @param tableOfContents the book's table of contents in markdown format
 * @param chapters the list of generated chapters with their content and reviews
 * @since 1.0.0
 * @see BookCreationWorkflow
 * @see BookTitleInfo
 * @see TableOfContents
 * @see Chapter
 */
public record BookManuscript(
    BookTitleInfo titleInfo,
    TableOfContents tableOfContents,
    List<Chapter> chapters
) {

    /**
     * Returns the estimated word count for the entire manuscript.
     *
     * @return total estimated word count across all chapters
     */
    public int getEstimatedWordCount() {
        return chapters.stream()
            .mapToInt(chapter -> chapter.content().split("\\s+").length)
            .sum();
    }

    /**
     * Returns the number of chapters in the manuscript.
     *
     * @return chapter count
     */
    public int getChapterCount() {
        return chapters.size();
    }

    /**
     * Returns a brief summary of the manuscript.
     *
     * @return formatted summary with title, chapter count, and word count
     */
    public String getSummary() {
        return String.format(
            "Book: %s - %s\nChapters: %d\nEstimated Words: %,d\nEstimated Pages: %d",
            titleInfo.title(),
            titleInfo.subtitle(),
            getChapterCount(),
            getEstimatedWordCount(),
            getEstimatedPageCount()
        );
    }

    /**
     * Returns the estimated page count for the manuscript.
     * <p>
     * Uses a standard estimate of 250 words per page.
     * </p>
     *
     * @return estimated page count
     */
    public int getEstimatedPageCount() {
        return (int) Math.ceil(getEstimatedWordCount() / 250.0);
    }

    /**
     * Checks if the manuscript is complete and ready for publication.
     * <p>
     * A manuscript is considered complete if it has:
     * - Valid title and subtitle
     * - At least one chapter
     * - Reasonable word count (at least 1000 words total)
     * </p>
     *
     * @return true if the manuscript appears complete
     */
    public boolean isComplete() {
        return titleInfo != null &&
               !titleInfo.title().trim().isEmpty() &&
               !titleInfo.subtitle().trim().isEmpty() &&
               chapters != null &&
               !chapters.isEmpty() &&
               getEstimatedWordCount() >= 1000;
    }

    /**
     * Returns a list of chapter titles in order.
     *
     * @return ordered list of chapter titles
     */
    public List<String> getChapterTitles() {
        return chapters.stream()
            .map(Chapter::title)
            .toList();
    }

    /**
     * Creates a new manuscript with updated title information.
     *
     * @param newTitleInfo the updated title information
     * @return new BookManuscript instance with updated title info
     */
    public BookManuscript withUpdatedTitleInfo(BookTitleInfo newTitleInfo) {
        return new BookManuscript(newTitleInfo, tableOfContents, chapters);
    }

    /**
     * Creates a new manuscript with an additional chapter.
     *
     * @param chapter the chapter to add
     * @return new BookManuscript instance with the additional chapter
     */
    public BookManuscript withAdditionalChapter(Chapter chapter) {
        List<Chapter> updatedChapters = new java.util.ArrayList<>(chapters);
        updatedChapters.add(chapter);
        return new BookManuscript(titleInfo, tableOfContents, updatedChapters);
    }
}
package com.skanga.conductor.demo;

/**
 * Represents a chapter specification used as input for the book generation workflow.
 * <p>
 * This record contains the basic requirements for generating a chapter,
 * parsed from the overall book summary. It serves as input to the writer
 * agent in the multi-agent workflow.
 * </p>
 * <p>
 * The specification is typically extracted by parsing chapter markers
 * from a book summary or outline.
 * </p>
 *
 * @param title the chapter title
 * @param spec the detailed specification or description for the chapter content
 * @since 1.0.0
 * @see BookCreationWorkflow
 * @see Chapter
 */
public record ChapterSpec(String title, String spec) {
}
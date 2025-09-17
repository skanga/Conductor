package com.skanga.conductor.demo;

import java.util.List;

/**
 * Represents a book chapter with its content, revision history, and critique.
 * <p>
 * This record captures the complete lifecycle of a chapter through the
 * multi-agent workflow, including the original draft, edited versions,
 * and critical feedback for improvement.
 * </p>
 * <p>
 * The workflow typically produces:
 * </p>
 * <ol>
 * <li>Initial draft from the writer agent</li>
 * <li>Edited content from the editor agent</li>
 * <li>Critical analysis from the critic agent</li>
 * </ol>
 *
 * @param title the chapter title
 * @param content the final edited chapter content
 * @param revisions the list of revision steps (draft, edited versions, etc.)
 * @param critique the critical feedback and suggestions for improvement
 * @since 1.0.0
 * @see BookWorkflow
 * @see ChapterSpec
 */
public record Chapter(String title, String content, List<String> revisions, String critique) {
}
package com.skanga.conductor.demo;

import java.util.List;

/**
 * Represents a complete book with manuscript and chapters.
 * <p>
 * This record encapsulates the final output of the book generation workflow,
 * containing both the overall manuscript summary and the individual chapters
 * that were generated through the multi-agent process.
 * </p>
 *
 * @param manuscript the overall book manuscript or summary
 * @param chapters the list of generated chapters with their content and revisions
 * @since 1.0.0
 * @see Chapter
 * @see BookWorkflow
 */
public record Book(String manuscript, List<Chapter> chapters) {
}
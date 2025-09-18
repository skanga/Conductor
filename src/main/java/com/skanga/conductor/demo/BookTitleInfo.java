package com.skanga.conductor.demo;

/**
 * Represents book title information including title, subtitle, and review comments.
 * <p>
 * This record holds the title and subtitle generated for a book, along with
 * any review comments from agentic reviewers during the approval process.
 * </p>
 *
 * @param title the main book title
 * @param subtitle the book subtitle providing additional context
 * @param reviewComments comments and feedback from agentic reviewers
 * @since 1.0.0
 * @see BookCreationWorkflow
 */
public record BookTitleInfo(String title, String subtitle, String reviewComments) {

    /**
     * Returns a formatted string representation of the title and subtitle.
     *
     * @return formatted title information
     */
    @Override
    public String toString() {
        return String.format("%s - %s", title, subtitle);
    }

    /**
     * Creates a new BookTitleInfo with updated review comments.
     *
     * @param newComments additional review comments to append
     * @return new BookTitleInfo instance with updated comments
     */
    public BookTitleInfo withAdditionalComments(String newComments) {
        String updatedComments = reviewComments.isEmpty() ?
            newComments :
            reviewComments + "\n\n" + newComments;
        return new BookTitleInfo(title, subtitle, updatedComments);
    }
}
package com.skanga.conductor.demo;

/**
 * Interface for user input operations to enable testability and dependency injection.
 * <p>
 * This interface abstracts user input operations used by the book creation workflow,
 * allowing for easy mocking in tests and alternative implementations like
 * batch processing, web interfaces, or automated testing.
 * </p>
 *
 * @since 1.0.0
 * @see BookCreationWorkflow
 */
public interface UserInputService extends AutoCloseable {

    /**
     * Gets user approval for a generated item.
     *
     * @param itemType the type of item being reviewed (e.g., "title", "chapter")
     * @param content the content to review
     * @return true if the user approves, false if they want to regenerate
     */
    boolean getApproval(String itemType, String content);

    /**
     * Checks if the service is running in non-interactive mode.
     * In non-interactive mode, all approvals are automatically granted.
     *
     * @return true if non-interactive, false if interactive
     */
    boolean isNonInteractive();

    /**
     * Closes any resources used by the user input service.
     * Default implementation does nothing.
     */
    @Override
    default void close() {
        // Default implementation - no cleanup needed
    }
}
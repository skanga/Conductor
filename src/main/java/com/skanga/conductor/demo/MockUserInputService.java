package com.skanga.conductor.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Mock implementation of UserInputService for testing purposes.
 * <p>
 * This implementation allows pre-programming responses for automated testing
 * without requiring actual user interaction.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create mock with pre-programmed responses
 * MockUserInputService mockInput = new MockUserInputService();
 * mockInput.addResponse(true);  // Approve title
 * mockInput.addResponse(false); // Reject TOC (will regenerate)
 * mockInput.addResponse(true);  // Approve TOC on second attempt
 * mockInput.addResponse(true);  // Approve chapter 1
 * mockInput.addResponse(true);  // Approve final book
 *
 * // Use in workflow
 * BookCreationWorkflow workflow = new BookCreationWorkflow(
 *     orchestrator, llmProvider, outputDir,
 *     new DefaultFileSystemService(), mockInput);
 * }</pre>
 *
 * @since 1.0.0
 * @see UserInputService
 * @see BookCreationWorkflow
 */
public class MockUserInputService implements UserInputService {

    private static final Logger logger = LoggerFactory.getLogger(MockUserInputService.class);

    private final Queue<Boolean> presetResponses = new LinkedList<>();
    private final boolean defaultResponse;
    private final boolean nonInteractive;

    /**
     * Creates a mock service that defaults to approving everything.
     */
    public MockUserInputService() {
        this(true, true);
    }

    /**
     * Creates a mock service with specified default behavior.
     *
     * @param defaultResponse the response to give when no preset responses are available
     * @param nonInteractive whether to report as non-interactive mode
     */
    public MockUserInputService(boolean defaultResponse, boolean nonInteractive) {
        this.defaultResponse = defaultResponse;
        this.nonInteractive = nonInteractive;
    }

    /**
     * Adds a preset response to the queue.
     * Responses are used in FIFO order.
     *
     * @param approve whether to approve when this response is used
     * @return this instance for method chaining
     */
    public MockUserInputService addResponse(boolean approve) {
        presetResponses.offer(approve);
        return this;
    }

    /**
     * Adds multiple preset responses.
     *
     * @param responses array of responses to add
     * @return this instance for method chaining
     */
    public MockUserInputService addResponses(boolean... responses) {
        for (boolean response : responses) {
            addResponse(response);
        }
        return this;
    }

    @Override
    public boolean getApproval(String itemType, String content) {
        boolean response;

        if (!presetResponses.isEmpty()) {
            response = presetResponses.poll();
            logger.debug("Mock approval for '{}': {} (from preset queue)", itemType, response);
        } else {
            response = defaultResponse;
            logger.debug("Mock approval for '{}': {} (default response)", itemType, response);
        }

        // Log the interaction for test verification
        logger.info("Mock user input - Item: '{}', Content length: {}, Response: {}",
            itemType, content.length(), response ? "APPROVED" : "REJECTED");

        return response;
    }

    @Override
    public boolean isNonInteractive() {
        return nonInteractive;
    }

    /**
     * Returns the number of preset responses remaining.
     *
     * @return number of responses in the queue
     */
    public int getRemainingResponses() {
        return presetResponses.size();
    }

    /**
     * Clears all preset responses.
     */
    public void clearResponses() {
        presetResponses.clear();
    }

    @Override
    public void close() {
        // No resources to clean up
        logger.debug("Mock user input service closed");
    }
}
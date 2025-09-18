package com.skanga.conductor.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Console-based implementation of UserInputService using System.in.
 * <p>
 * This implementation provides interactive user input through the console,
 * with automatic fallback to non-interactive mode when no console is available.
 * </p>
 *
 * @since 1.0.0
 * @see UserInputService
 * @see BookCreationWorkflow
 */
public class ConsoleUserInputService implements UserInputService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleUserInputService.class);
    private static final int CONTENT_PREVIEW_LENGTH = 500;

    private final Scanner scanner;
    private final boolean nonInteractive;

    /**
     * Creates a new console user input service.
     * Automatically detects if running in non-interactive mode.
     */
    public ConsoleUserInputService() {
        this.scanner = new Scanner(System.in);
        this.nonInteractive = detectNonInteractiveMode();

        if (nonInteractive) {
            logger.info("Running in non-interactive mode - all approvals will be automatic");
        }
    }

    @Override
    public boolean getApproval(String itemType, String content) {
        if (nonInteractive) {
            logger.info("Auto-approving {} in non-interactive mode", itemType);
            return true;
        }

        displayReviewPrompt(itemType, content);

        while (true) {
            System.out.print("Do you approve this " + itemType + "? ([y]/n/view): ");

            String response = scanner.nextLine().trim().toLowerCase();

            switch (response) {
                case "view":
                case "v":
                    displayFullContent(content);
                    continue; // Ask again after showing full content

                case "n":
                case "no":
                    return false;

                case "y":
                case "yes":
                case "": // Default to yes
                    return true;

                default:
                    System.out.println("Please enter 'y' (yes), 'n' (no), or 'view' to see full content.");
                    continue;
            }
        }
    }

    @Override
    public boolean isNonInteractive() {
        return nonInteractive;
    }

    @Override
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }

    /**
     * Displays the review prompt with content preview.
     */
    private void displayReviewPrompt(String itemType, String content) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HUMAN REVIEW REQUIRED: " + itemType.toUpperCase());
        System.out.println("=".repeat(80));
        System.out.println("Please review the generated " + itemType + ":");

        String preview = content.length() <= CONTENT_PREVIEW_LENGTH ?
            content :
            content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";

        System.out.println("\n" + preview);
        System.out.println("\n" + "=".repeat(80));
    }

    /**
     * Displays the full content when user requests to view it.
     */
    private void displayFullContent(String content) {
        System.out.println("\nFull content:");
        System.out.println(content);
        System.out.println();
    }

    /**
     * Detects if running in non-interactive mode.
     */
    private boolean detectNonInteractiveMode() {
        try {
            // Check if console is available
            if (System.console() == null) {
                return true;
            }

            // Additional checks could be added here for CI environments
            // For example: checking environment variables like CI=true

            return false;
        } catch (Exception e) {
            logger.debug("Error detecting interactive mode: {}", e.getMessage());
            return true; // Default to non-interactive on error
        }
    }
}
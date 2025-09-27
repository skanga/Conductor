package com.skanga.conductor.workflow.approval;

import com.skanga.conductor.exception.ApprovalException;
import com.skanga.conductor.exception.ApprovalTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * Console-based human approval handler that prompts for approval via standard input.
 * Provides an interactive command-line interface for reviewing and approving workflow stages.
 */
public class ConsoleApprovalHandler implements HumanApprovalHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleApprovalHandler.class);

    private final ExecutorService executorService;
    private final BufferedReader reader;

    public ConsoleApprovalHandler() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public ApprovalResponse requestApproval(ApprovalRequest request, long timeoutMs)
            throws ApprovalTimeoutException, ApprovalException {

        logger.info("Requesting human approval for stage: {}", request.getStageName());

        displayApprovalRequest(request);

        Future<ApprovalResponse> approvalFuture = executorService.submit(() -> {
            try {
                return promptForApproval(request);
            } catch (IOException e) {
                throw new RuntimeException("Error reading user input", e);
            }
        });

        try {
            return approvalFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            approvalFuture.cancel(true);
            throw new ApprovalTimeoutException(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApprovalException("Approval request was interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw new ApprovalException("Error during approval: " + cause.getMessage(), cause);
            }
            throw new ApprovalException("Unexpected error during approval", e);
        }
    }

    private void displayApprovalRequest(ApprovalRequest request) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HUMAN APPROVAL REQUIRED");
        System.out.println("=".repeat(80));
        System.out.println("Workflow: " + request.getWorkflowName());
        System.out.println("Stage: " + request.getStageName());
        if (request.getStageDescription() != null) {
            System.out.println("Description: " + request.getStageDescription());
        }
        System.out.println();

        // Display generated content
        if (request.getGeneratedContent() != null) {
            System.out.println("GENERATED CONTENT:");
            System.out.println("-".repeat(60));
            displayContent(request.getGeneratedContent());
            System.out.println();
        }

        // Display review content if available
        if (request.getReviewContent() != null) {
            System.out.println("REVIEW FEEDBACK:");
            System.out.println("-".repeat(60));
            displayContent(request.getReviewContent());
            System.out.println();
        }

        System.out.println("=".repeat(80));
    }

    private void displayContent(String content) {
        // Display content with word wrapping at 75 characters
        String[] words = content.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() + 1 > 75) {
                System.out.println(line.toString());
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) {
                    line.append(" ");
                }
                line.append(word);
            }
        }

        if (line.length() > 0) {
            System.out.println(line.toString());
        }
    }

    private ApprovalResponse promptForApproval(ApprovalRequest request) throws IOException {
        while (true) {
            System.out.println("Please review the content and choose an action:");
            System.out.println("  [A]pprove - Accept and continue to next stage");
            System.out.println("  [R]eject - Stop workflow execution");
            System.out.println("  [V]iew - Display content again");
            System.out.println("  [H]elp - Show detailed options");
            System.out.print("\nYour decision (A/R/V/H): ");

            String input = reader.readLine();
            if (input == null) {
                throw new IOException("End of input stream reached");
            }

            input = input.trim().toLowerCase();

            switch (input) {
                case "a":
                case "approve":
                    System.out.print("Optional approval comment: ");
                    String comment = reader.readLine();
                    System.out.println("Stage approved!");
                    return ApprovalResponse.approve(comment != null && !comment.trim().isEmpty() ? comment : null);

                case "r":
                case "reject":
                    System.out.print("Reason for rejection: ");
                    String reason = reader.readLine();
                    System.out.println("ERROR: Stage rejected!");
                    return ApprovalResponse.reject(reason != null && !reason.trim().isEmpty() ? reason : "Rejected by user");

                case "v":
                case "view":
                    displayApprovalRequest(request);
                    continue;

                case "h":
                case "help":
                    displayHelp();
                    continue;

                default:
                    System.out.println("Invalid option. Please enter A (approve), R (reject), V (view), or H (help).");
                    continue;
            }
        }
    }

    private void displayHelp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROVAL OPTIONS HELP");
        System.out.println("=".repeat(60));
        System.out.println("A, Approve:");
        System.out.println("  Accept the generated content and continue to the next workflow stage.");
        System.out.println("  You can optionally provide a comment with your approval.");
        System.out.println();
        System.out.println("R, Reject:");
        System.out.println("  Reject the generated content and stop workflow execution.");
        System.out.println("  Please provide a reason for rejection to help improve future runs.");
        System.out.println();
        System.out.println("V, View:");
        System.out.println("  Display the generated content and review again.");
        System.out.println();
        System.out.println("H, Help:");
        System.out.println("  Show this help message.");
        System.out.println("=".repeat(60) + "\n");
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Console-based interactive approval handler";
    }

    /**
     * Parses a timeout string like "5m", "30s", "2h" into milliseconds.
     */
    public static long parseTimeout(String timeoutString) {
        if (timeoutString == null || timeoutString.trim().isEmpty()) {
            return 300000; // Default 5 minutes
        }

        String clean = timeoutString.trim().toLowerCase();

        try {
            if (clean.endsWith("ms")) {
                return Long.parseLong(clean.substring(0, clean.length() - 2));
            } else if (clean.endsWith("s")) {
                return Long.parseLong(clean.substring(0, clean.length() - 1)) * 1000;
            } else if (clean.endsWith("m")) {
                return Long.parseLong(clean.substring(0, clean.length() - 1)) * 60 * 1000;
            } else if (clean.endsWith("h")) {
                return Long.parseLong(clean.substring(0, clean.length() - 1)) * 60 * 60 * 1000;
            } else {
                // Assume seconds if no unit
                return Long.parseLong(clean) * 1000;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid timeout format '{}', using default 5 minutes", timeoutString);
            return 300000; // Default 5 minutes
        }
    }

    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
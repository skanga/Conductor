package com.skanga.conductor.tools;

/**
 * Mock web search tool for testing and demonstration purposes.
 * <p>
 * This tool provides a simple mock implementation of web search functionality
 * for development, testing, and demonstration scenarios. It returns deterministic
 * synthetic search results based on the input query without making actual
 * web requests.
 * </p>
 * <p>
 * In production environments, this should be replaced with a real web search
 * integration using services such as:
 * </p>
 * <ul>
 * <li>Google Custom Search API</li>
 * <li>Bing Search API</li>
 * <li>DuckDuckGo API</li>
 * <li>Other search service providers</li>
 * </ul>
 * <p>
 * The mock results are designed to be predictable and consistent, making them
 * suitable for testing agent behavior and workflow development.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe as it maintains no mutable state.
 * </p>
 *
 * @since 1.0.0
 * @see Tool
 * @see ToolInput
 * @see ToolResult
 */
public class MockWebSearchTool implements Tool {

    /**
     * Returns the name identifier for this tool.
     *
     * @return "web_search" as the tool identifier
     */
    @Override
    public String name() {
        return "web_search";
    }

    /**
     * Returns a description of this tool's functionality.
     *
     * @return description explaining this is a mock search tool
     */
    @Override
    public String description() {
        return "Mock web search that returns synthetic results for a query";
    }

    /**
     * Executes the mock web search with the provided query.
     * <p>
     * Generates deterministic synthetic search results based on the input query.
     * The results are formatted as a simple text list with numbered entries.
     * </p>
     *
     * @param input the tool input containing the search query
     * @return a ToolResult containing synthetic search results
     */
    @Override
    public ToolResult run(ToolInput input) {
        // Validate input parameters
        ValidationResult inputValidation = validateInput(input);
        if (!inputValidation.isValid()) {
            return new ToolResult(false, inputValidation.getErrorMessage(), null);
        }

        String query = input.text().trim();
        // deterministic synthetic results (demo)
        String out = "Search results for: " + query + "\n" +
                "1) Example result title A — summary for '" + query + "'\n" +
                "2) Example result title B — more details about '" + query + "'\n";
        return new ToolResult(true, out, null);
    }

    /**
     * Validates the tool input for completeness and security.
     *
     * @param input the tool input to validate
     * @return validation result indicating success or specific error
     */
    private ValidationResult validateInput(ToolInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }

        if (input.text() == null) {
            return ValidationResult.invalid("Search query cannot be null");
        }

        String query = input.text().trim();
        if (query.isEmpty()) {
            return ValidationResult.invalid("Search query cannot be empty");
        }

        // Check for extremely long queries that could cause issues
        if (query.length() > 1000) {
            return ValidationResult.invalid("Search query is too long (max 1000 characters)");
        }

        // Check for control characters that might cause formatting issues
        for (char c : query.toCharArray()) {
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return ValidationResult.invalid("Search query contains invalid control characters");
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Result class for validation operations.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

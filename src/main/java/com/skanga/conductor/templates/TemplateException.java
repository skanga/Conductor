package com.skanga.conductor.templates;

/**
 * Exception thrown when template parsing or rendering fails.
 * <p>
 * Provides rich error context including line number, column, and a snippet
 * of the template showing where the error occurred.
 * </p>
 *
 * @since 2.0.0
 */
public class TemplateException extends RuntimeException {

    private final String template;
    private final int position;
    private final int line;
    private final int column;
    private final String snippet;

    /**
     * Creates a template exception with error location context.
     *
     * @param message the error message
     * @param template the template that caused the error
     * @param position the character position in the template (0-based)
     */
    public TemplateException(String message, String template, int position) {
        super(formatMessage(message, template, position));
        this.template = template;
        this.position = position;
        this.line = calculateLine(template, position);
        this.column = calculateColumn(template, position);
        this.snippet = extractSnippet(template, position);
    }

    /**
     * Creates a template exception without position information.
     *
     * @param message the error message
     */
    public TemplateException(String message) {
        super(message);
        this.template = null;
        this.position = -1;
        this.line = -1;
        this.column = -1;
        this.snippet = null;
    }

    /**
     * Creates a template exception with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public TemplateException(String message, Throwable cause) {
        super(message, cause);
        this.template = null;
        this.position = -1;
        this.line = -1;
        this.column = -1;
        this.snippet = null;
    }

    /**
     * Formats the error message with location information.
     */
    private static String formatMessage(String message, String template, int position) {
        if (template == null || position < 0 || position >= template.length()) {
            return message;
        }

        int line = calculateLine(template, position);
        int column = calculateColumn(template, position);
        String snippet = extractSnippet(template, position);

        return String.format("%s at line %d, column %d:\n%s\n%s^",
            message, line, column, snippet, " ".repeat(Math.max(0, column - 1)));
    }

    /**
     * Calculates the line number (1-based) for a given position.
     */
    private static int calculateLine(String template, int position) {
        if (template == null || position < 0) {
            return 1;
        }

        int line = 1;
        for (int i = 0; i < Math.min(position, template.length()); i++) {
            if (template.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * Calculates the column number (1-based) for a given position.
     */
    private static int calculateColumn(String template, int position) {
        if (template == null || position < 0) {
            return 1;
        }

        int column = 1;
        for (int i = Math.min(position, template.length()) - 1; i >= 0; i--) {
            if (template.charAt(i) == '\n') {
                break;
            }
            column = position - i;
        }
        return column;
    }

    /**
     * Extracts a snippet of the template around the error position.
     */
    private static String extractSnippet(String template, int position) {
        if (template == null || position < 0 || position >= template.length()) {
            return "";
        }

        // Find the start of the current line
        int lineStart = position;
        while (lineStart > 0 && template.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        // Find the end of the current line
        int lineEnd = position;
        while (lineEnd < template.length() && template.charAt(lineEnd) != '\n') {
            lineEnd++;
        }

        return template.substring(lineStart, lineEnd);
    }

    // Getters

    public String getTemplate() {
        return template;
    }

    public int getPosition() {
        return position;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getSnippet() {
        return snippet;
    }

    /**
     * Returns whether this exception has location information.
     */
    public boolean hasLocationInfo() {
        return template != null && position >= 0;
    }
}

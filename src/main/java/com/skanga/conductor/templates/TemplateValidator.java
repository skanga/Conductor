package com.skanga.conductor.templates;

import com.skanga.conductor.workflow.config.AgentConfigCollection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for template syntax and structure.
 * <p>
 * This class provides validation for template strings and PromptTemplate objects,
 * ensuring they are syntactically correct before compilation and rendering.
 * </p>
 * <p>
 * Validates:
 * </p>
 * <ul>
 * <li>Balanced braces - all {{}} pairs are properly closed</li>
 * <li>Variable names - no empty variable names like {{}}</li>
 * <li>Template structure - at least one message component present</li>
 * </ul>
 *
 * @since 2.0.0
 */
public class TemplateValidator {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]*)\\}\\}");

    /**
     * Validates that a template string is syntactically correct.
     *
     * @param template the template to validate
     * @throws TemplateException if the template is invalid
     */
    public void validate(String template) throws TemplateException {
        if (template == null) {
            return; // null templates are allowed
        }

        validateBraces(template);
        validateVariableNames(template);
    }

    /**
     * Validates that all braces in the template are balanced.
     *
     * @param template the template to validate
     * @throws TemplateException if braces are unbalanced
     */
    private void validateBraces(String template) throws TemplateException {
        // Check for unbalanced braces by removing all valid {{...}} patterns
        String withoutValid = template.replaceAll("\\{\\{[^}]*\\}\\}", "");

        // If any single braces remain, they're invalid
        int invalidBracePos = withoutValid.indexOf('{');
        if (invalidBracePos == -1) {
            invalidBracePos = withoutValid.indexOf('}');
        }

        if (invalidBracePos >= 0) {
            // Find the actual position in the original template
            int actualPos = findActualPosition(template, invalidBracePos);
            char invalidChar = template.charAt(actualPos);

            if (invalidChar == '{') {
                throw new TemplateException("Unbalanced opening brace (use {{}} for variables)", template, actualPos);
            } else {
                throw new TemplateException("Unbalanced closing brace", template, actualPos);
            }
        }
    }

    /**
     * Validates that all variable names in the template are non-empty.
     *
     * @param template the template to validate
     * @throws TemplateException if any variable name is empty
     */
    private void validateVariableNames(String template) throws TemplateException {
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            if (variableName.isEmpty()) {
                throw new TemplateException("Empty variable name", template, matcher.start());
            }
        }
    }

    /**
     * Helper to find the actual position in the original template after regex replacement.
     */
    private int findActualPosition(String original, int positionInModified) {
        int originalPos = 0;
        int modifiedPos = 0;
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(original);

        while (matcher.find()) {
            // Count characters before this match
            int matchStart = matcher.start();
            while (originalPos < matchStart && modifiedPos <= positionInModified) {
                originalPos++;
                modifiedPos++;
            }

            if (modifiedPos >= positionInModified) {
                return originalPos;
            }

            // Skip the matched pattern in original, but it's removed in modified
            originalPos = matcher.end();
            // modifiedPos stays the same (pattern was removed)
        }

        // Handle remaining characters after last match
        while (modifiedPos < positionInModified && originalPos < original.length()) {
            originalPos++;
            modifiedPos++;
        }

        return Math.min(originalPos, original.length() - 1);
    }

    /**
     * Validates a complete PromptTemplate object.
     *
     * @param template the prompt template to validate
     * @throws IllegalArgumentException if the template is invalid
     */
    public void validatePromptTemplate(AgentConfigCollection.PromptTemplate template) throws IllegalArgumentException {
        if (template == null) {
            throw new IllegalArgumentException("Prompt template cannot be null");
        }

        validate(template.getSystem());
        validate(template.getUser());
        validate(template.getAssistant());

        if (!template.hasSystem() && !template.hasUser()) {
            throw new IllegalArgumentException("Prompt template must have at least system or user content");
        }
    }
}

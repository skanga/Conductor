package com.skanga.conductor.tools.security;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Detects various path traversal and injection attack patterns.
 * <p>
 * Detects:
 * </p>
 * <ul>
 * <li>Encoded traversal patterns</li>
 * <li>Windows device names</li>
 * <li>Invisible characters</li>
 * <li>Template injection</li>
 * <li>Mixed separators</li>
 * <li>Case variation attacks</li>
 * </ul>
 * <p>
 * Thread Safety: This class is stateless and thread-safe.
 * </p>
 *
 * @since 2.0.0
 */
public class PathAttackDetector {

    // Comprehensive patterns to detect various path traversal and injection attacks
    private static final Pattern SUSPICIOUS_PATTERNS = Pattern.compile(
            ".*(" +
            // Standard path traversal patterns
            "\\.{2}[/\\\\]|[/\\\\]\\.{2}|^\\.{2}$|^[/\\\\]|" +
            // Windows drive access patterns
            "^[A-Za-z]:|.*[A-Za-z]:[/\\\\]|" +
            // UNC path patterns (\\server\share)
            "^\\\\\\\\|.*\\\\\\\\.*\\\\|" +
            // URL/URI schemes that could lead to remote access
            "^[a-zA-Z][a-zA-Z0-9+.-]*:|.*://|" +
            // Special file/device names (Windows)
            "(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\.|$)|" +
            // Forbidden characters in file paths
            ".*[<>:\"|?*\\x00-\\x1f]|" +
            // Expression injection patterns
            "\\$\\{|#\\{|%\\{|\\$\\(|`|" +
            // Double encoding attacks
            "%2[eE]%2[eE]|%5[cC]|%2[fF]|" +
            // Alternative path separators and encoding
            "\\x2e\\x2e|\\u002e\\u002e|\\\\x2e|\\\\u002e|" +
            // Control characters and Unicode attacks
            "[\\x00-\\x1f\\x7f-\\x9f]|\\\\[nrtbfav0]|" +
            // Shell metacharacters
            "[;&|`$(){}\\[\\]]|" +
            // Potential command injection
            "\\|[^|]|&&|;[^;]" +
            ").*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Checks if path contains suspicious patterns.
     */
    public boolean containsSuspiciousPatterns(String path) {
        return SUSPICIOUS_PATTERNS.matcher(path).matches();
    }

    /**
     * Validates against specific attack patterns with detailed categorization.
     */
    public PathSecurityValidator.ValidationResult validateSpecificAttacks(String path) {
        // Unicode normalization attacks
        String normalizedPath = Normalizer.normalize(path, Normalizer.Form.NFC);
        if (!normalizedPath.equals(path)) {
            if (SUSPICIOUS_PATTERNS.matcher(normalizedPath).matches()) {
                return PathSecurityValidator.ValidationResult.invalid("Path contains Unicode normalization attack: " + path);
            }
        }

        // Double-dot encoding variations
        if (containsEncodedTraversal(path)) {
            return PathSecurityValidator.ValidationResult.invalid("Path contains encoded traversal patterns: " + path);
        }

        // Windows device name attacks
        if (containsWindowsDeviceName(path)) {
            return PathSecurityValidator.ValidationResult.invalid("Path references Windows device name: " + path);
        }

        // Long path attacks (Windows MAX_PATH limit bypass attempts)
        if (path.length() > 32767) { // Windows extended path limit
            return PathSecurityValidator.ValidationResult.invalid("Path exceeds maximum length limit: " + path.length());
        }

        // Deep nesting attacks (zip bombs, directory bombs)
        int separatorCount = (int) path.chars().filter(c -> c == '/' || c == '\\').count();
        if (separatorCount > 100) { // Reasonable depth limit
            return PathSecurityValidator.ValidationResult.invalid("Path nesting too deep: " + separatorCount + " levels");
        }

        // Zero-width characters and invisible characters
        if (containsInvisibleCharacters(path)) {
            return PathSecurityValidator.ValidationResult.invalid("Path contains invisible or zero-width characters: " + path);
        }

        // Mixed separator attacks
        if (containsMixedSeparators(path)) {
            return PathSecurityValidator.ValidationResult.invalid("Path contains mixed directory separators: " + path);
        }

        // Template injection patterns
        if (containsTemplateInjection(path)) {
            return PathSecurityValidator.ValidationResult.invalid("Path contains template injection patterns: " + path);
        }

        // Case variation attacks (for case-insensitive filesystems)
        if (containsCaseVariationAttack(path)) {
            return PathSecurityValidator.ValidationResult.invalid("Path contains potential case variation attack: " + path);
        }

        return PathSecurityValidator.ValidationResult.valid();
    }

    /**
     * Checks for various encoded traversal patterns.
     */
    public boolean containsEncodedTraversal(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.contains("%2e%2e") ||          // URL encoded ..
               lowerPath.contains("%252e%252e") ||      // Double URL encoded ..
               lowerPath.contains("\\u002e\\u002e") ||  // Unicode escaped ..
               lowerPath.contains("\\x2e\\x2e") ||      // Hex escaped ..
               lowerPath.contains("%u002e%u002e") ||    // Unicode URL encoded ..
               lowerPath.contains("%c0%ae%c0%ae") ||    // Overlong UTF-8 encoded ..
               lowerPath.contains("%e0%80%ae%e0%80%ae") || // Another overlong UTF-8 ..
               lowerPath.contains("..%2f") ||           // Mixed encoded/unencoded
               lowerPath.contains("..%5c") ||           // Mixed with backslash
               lowerPath.matches(".*\\.{2,}.*");       // Multiple dots (3 or more)
    }

    /**
     * Checks for Windows reserved device names.
     */
    public boolean containsWindowsDeviceName(String path) {
        String upperPath = path.toUpperCase();
        String[] deviceNames = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };

        for (String device : deviceNames) {
            // Check exact match or with extension
            if (upperPath.equals(device) ||
                upperPath.startsWith(device + ".") ||
                upperPath.contains("/" + device) ||
                upperPath.contains("\\" + device) ||
                upperPath.contains("/" + device + ".") ||
                upperPath.contains("\\" + device + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for invisible and zero-width characters.
     */
    public boolean containsInvisibleCharacters(String path) {
        for (char c : path.toCharArray()) {
            // Zero-width characters
            if (c == '\u200B' || // Zero Width Space
                c == '\u200C' || // Zero Width Non-Joiner
                c == '\u200D' || // Zero Width Joiner
                c == '\uFEFF' || // Zero Width No-Break Space (BOM)
                c == '\u2060' || // Word Joiner
                // Right-to-left override attacks
                c == '\u202D' || // Left-to-Right Override
                c == '\u202E' || // Right-to-Left Override
                c == '\u2066' || // Left-to-Right Isolate
                c == '\u2067' || // Right-to-Left Isolate
                c == '\u2068' || // First Strong Isolate
                c == '\u2069' || // Pop Directional Isolate
                // Other invisible characters
                Character.getType(c) == Character.FORMAT ||
                Character.getType(c) == Character.CONTROL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for mixed directory separators that could bypass validation.
     */
    public boolean containsMixedSeparators(String path) {
        boolean hasForwardSlash = path.contains("/");
        boolean hasBackwardSlash = path.contains("\\");
        return hasForwardSlash && hasBackwardSlash;
    }

    /**
     * Checks for template injection patterns.
     */
    public boolean containsTemplateInjection(String path) {
        return path.contains("${") ||      // EL injection
               path.contains("#{") ||      // EL injection
               path.contains("%{") ||      // Apache Struts injection
               path.contains("$(") ||      // Shell command substitution
               path.contains("{{") ||      // Handlebars/Mustache
               path.contains("{%") ||      // Django/Jinja2
               path.contains("<%") ||      // JSP/ASP
               path.contains("[%") ||      // Template Toolkit
               path.contains("[[") ||      // Lua template
               path.contains("]]") ||      // Lua template end
               path.contains("}}");        // Template end
    }

    /**
     * Checks for case variation attacks on case-insensitive filesystems.
     */
    public boolean containsCaseVariationAttack(String path) {
        String lowerPath = path.toLowerCase();
        // Check for variations of system directories
        return lowerPath.contains("/system32/") ||
               lowerPath.contains("\\system32\\") ||
               lowerPath.contains("/windows/") ||
               lowerPath.contains("\\windows\\") ||
               lowerPath.contains("/etc/") ||
               lowerPath.contains("/usr/") ||
               lowerPath.contains("/var/") ||
               lowerPath.contains("/bin/") ||
               lowerPath.contains("/sbin/") ||
               // Check for variations of .. with different cases
               lowerPath.matches(".*\\.[A-Z]\\.[a-z].*") ||
               lowerPath.matches(".*\\.[a-z]\\.[A-Z].*");
    }
}

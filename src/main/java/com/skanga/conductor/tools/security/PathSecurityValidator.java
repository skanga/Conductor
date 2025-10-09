package com.skanga.conductor.tools.security;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.ToolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;

/**
 * Validates file paths for security before and after resolution.
 * <p>
 * Provides multi-layered security validation:
 * </p>
 * <ul>
 * <li>Pre-resolution validation - checks before path resolution</li>
 * <li>Post-resolution validation - checks after path resolution</li>
 * <li>Input validation - basic input sanitization</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent validation.
 * </p>
 *
 * @since 2.0.0
 */
public class PathSecurityValidator {

    private static final Logger logger = LoggerFactory.getLogger(PathSecurityValidator.class);

    private final Path baseDir;
    private final boolean allowSymlinks;
    private final PathAttackDetector attackDetector;

    /**
     * Creates a new PathSecurityValidator.
     *
     * @param baseDir the base directory for path validation
     * @param allowSymlinks whether to allow symbolic links
     */
    public PathSecurityValidator(Path baseDir, boolean allowSymlinks) {
        this.baseDir = baseDir;
        this.allowSymlinks = allowSymlinks;
        this.attackDetector = new PathAttackDetector();
    }

    /**
     * Validates input before any processing.
     *
     * @param input the input string to validate
     * @return validation result
     */
    public ValidationResult validateInput(String input) {
        if (input == null) {
            return ValidationResult.invalid("File path cannot be null");
        }

        String path = input.trim();
        if (path.isEmpty()) {
            return ValidationResult.invalid("File path cannot be empty");
        }

        // Check for extremely long inputs that could cause DoS
        if (path.length() > 4096) {
            return ValidationResult.invalid("File path is too long (max 4096 characters)");
        }

        // Check for control characters (except normal whitespace)
        for (char c : path.toCharArray()) {
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return ValidationResult.invalid("File path contains invalid control characters");
            }
        }

        // Check for Unicode normalization attacks
        String normalized = Normalizer.normalize(path, Normalizer.Form.NFC);
        if (!path.equals(normalized)) {
            return ValidationResult.invalid("File path contains potentially dangerous Unicode sequences");
        }

        return ValidationResult.valid();
    }

    /**
     * Validates the path before resolution.
     *
     * @param path the path to validate
     * @return validation result
     */
    public ValidationResult validatePath(String path) {
        // Check for null bytes (common in path traversal attacks)
        if (path.contains("\0")) {
            return ValidationResult.invalid("Path contains null byte");
        }

        // Check for suspicious patterns using attack detector
        if (attackDetector.containsSuspiciousPatterns(path)) {
            return ValidationResult.invalid("Path contains suspicious patterns: " + path);
        }

        // Additional specific validations
        ValidationResult specificValidation = attackDetector.validateSpecificAttacks(path);
        if (!specificValidation.isValid()) {
            return specificValidation;
        }

        // Check for absolute paths
        if (Paths.get(path).isAbsolute()) {
            return ValidationResult.invalid("Absolute paths not allowed: " + path);
        }

        // Check for excessively long paths
        ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        if (path.length() > config.getFileReadMaxPathLength()) {
            return ValidationResult.invalid("Path too long");
        }

        // Check for too many path components (potential zip bomb via deep nesting)
        String[] components = path.split("[/\\\\]");
        if (components.length > 10) {
            return ValidationResult.invalid("Path has too many components");
        }

        return ValidationResult.valid();
    }

    /**
     * Validates the resolved path for additional security checks.
     *
     * @param resolvedPath the resolved path to validate
     * @return validation result
     * @throws IOException if path operations fail
     */
    public ValidationResult validateResolvedPath(Path resolvedPath) throws IOException {
        // First check if the path exists
        if (!Files.exists(resolvedPath)) {
            return ValidationResult.valid(); // Let the caller handle file not found
        }

        // Normalize and get real path to resolve all symbolic links
        Path realPath;
        try {
            realPath = resolvedPath.toRealPath();
        } catch (IOException e) {
            // Path exists but can't be resolved
            return ValidationResult.invalid("Cannot resolve path: " + e.getMessage());
        }

        // Check that the real path is still within the base directory
        if (!realPath.startsWith(baseDir)) {
            return ValidationResult.invalid("Path escapes base directory: " + resolvedPath);
        }

        // Check for symbolic links if not allowed
        if (!allowSymlinks && Files.isSymbolicLink(resolvedPath)) {
            return ValidationResult.invalid("Symbolic links not allowed: " + resolvedPath);
        }

        // Additional check: verify the resolved path components don't contain suspicious elements
        for (Path component : realPath) {
            String componentStr = component.toString();
            if (componentStr.startsWith(".") && !componentStr.equals(".") && !componentStr.equals("..")) {
                // Allow hidden files but log suspicious patterns
                if (componentStr.contains("..") || componentStr.contains("/") || componentStr.contains("\\")) {
                    return ValidationResult.invalid("Suspicious path component: " + componentStr);
                }
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Result class for validation operations.
     */
    public static class ValidationResult {
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

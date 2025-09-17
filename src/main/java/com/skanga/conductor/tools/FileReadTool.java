package com.skanga.conductor.tools;

import com.skanga.conductor.config.ApplicationConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Secure file reading tool with comprehensive path traversal prevention.
 * <p>
 * This tool provides safe file reading capabilities within a configured base directory,
 * with extensive security measures to prevent path traversal attacks and other
 * file system security vulnerabilities. Key security features include:
 * </p>
 * <ul>
 * <li>Path traversal prevention (../, ../../, etc.)</li>
 * <li>Absolute path blocking</li>
 * <li>Symbolic link validation and optional blocking</li>
 * <li>File size limits to prevent resource exhaustion</li>
 * <li>Path length and component count validation</li>
 * <li>Null byte injection prevention</li>
 * <li>Suspicious pattern detection</li>
 * </ul>
 * <p>
 * The tool operates within a configurable base directory and will reject any
 * attempts to access files outside this sandbox. All paths are resolved to
 * their real paths to ensure symbolic links cannot be used to escape the sandbox.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent file reading operations.
 * </p>
 *
 * @since 1.0.0
 * @see Tool
 * @see ToolInput
 * @see ToolResult
 */
public class FileReadTool implements Tool {

    private final Path baseDir;
    private final boolean allowSymlinks;
    private final long maxFileSize;

    // Pattern to detect suspicious path components
    private static final Pattern SUSPICIOUS_PATTERNS = Pattern.compile(
            ".*(\\.{2}[/\\\\]|[/\\\\]\\.{2}|^\\.{2}$|^[/\\\\]|.*[<>:\"|?*].*)"
    );

    /**
     * Creates a new FileReadTool with configuration from ApplicationConfig.
     * <p>
     * Initializes the tool with the base directory, symlink policy, and file size
     * limits from the application configuration. The base directory is resolved
     * to its real path during construction to establish the security boundary.
     * </p>
     *
     * @throws IllegalArgumentException if the configured base directory is invalid
     */
    public FileReadTool() {
        ApplicationConfig.ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        try {
            this.baseDir = Paths.get(config.getFileReadBaseDir()).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + config.getFileReadBaseDir(), e);
        }
        this.allowSymlinks = config.getFileReadAllowSymlinks();
        this.maxFileSize = config.getFileReadMaxSize();
    }

    /**
     * Creates a new FileReadTool with a custom base directory.
     * <p>
     * Uses the specified base directory while taking symlink and file size
     * configuration from ApplicationConfig.
     * </p>
     *
     * @param baseDir the base directory path for file operations
     * @throws IllegalArgumentException if the base directory is invalid or inaccessible
     */
    public FileReadTool(String baseDir) {
        ApplicationConfig.ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        try {
            this.baseDir = Paths.get(baseDir).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + baseDir, e);
        }
        this.allowSymlinks = config.getFileReadAllowSymlinks();
        this.maxFileSize = config.getFileReadMaxSize();
    }

    /**
     * Creates a new FileReadTool with custom base directory and symlink policy.
     * <p>
     * Uses the specified base directory and symlink setting while taking
     * file size limits from ApplicationConfig.
     * </p>
     *
     * @param baseDir the base directory path for file operations
     * @param allowSymlinks whether to allow symbolic link traversal
     * @throws IllegalArgumentException if the base directory is invalid or inaccessible
     */
    public FileReadTool(String baseDir, boolean allowSymlinks) {
        ApplicationConfig.ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        try {
            this.baseDir = Paths.get(baseDir).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + baseDir, e);
        }
        this.allowSymlinks = allowSymlinks;
        this.maxFileSize = config.getFileReadMaxSize();
    }

    /**
     * Creates a new FileReadTool with full custom configuration.
     * <p>
     * Uses all specified parameters without referencing ApplicationConfig.
     * This constructor provides complete control over tool behavior.
     * </p>
     *
     * @param baseDir the base directory path for file operations
     * @param allowSymlinks whether to allow symbolic link traversal
     * @param maxFileSize maximum file size in bytes that can be read
     * @throws IllegalArgumentException if the base directory is invalid or inaccessible
     */
    public FileReadTool(String baseDir, boolean allowSymlinks, long maxFileSize) {
        try {
            this.baseDir = Paths.get(baseDir).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + baseDir, e);
        }
        this.allowSymlinks = allowSymlinks;
        this.maxFileSize = maxFileSize;
    }

    @Override
    public String name() {
        return "file_read";
    }

    @Override
    public String description() {
        return "Securely read a text file under the configured base directory. Input: relative path";
    }

    @Override
    public ToolResult run(ToolInput input) {
        // Comprehensive input validation
        ValidationResult inputValidation = validateInput(input);
        if (!inputValidation.isValid()) {
            return new ToolResult(false, inputValidation.getErrorMessage(), null);
        }

        try {
            String relativePath = input.text().trim();

            // Security validation
            SecurityValidationResult validation = validatePath(relativePath);
            if (!validation.isValid()) {
                return new ToolResult(false, validation.getErrorMessage(), null);
            }

            Path candidate = baseDir.resolve(relativePath);

            // Additional security checks after resolution
            SecurityValidationResult postResolutionValidation = validateResolvedPath(candidate);
            if (!postResolutionValidation.isValid()) {
                return new ToolResult(false, postResolutionValidation.getErrorMessage(), null);
            }

            // File existence and type checks
            if (!Files.exists(candidate)) {
                return new ToolResult(false, "File not found: " + relativePath, null);
            }

            if (Files.isDirectory(candidate)) {
                return new ToolResult(false, "Path is a directory, not a file: " + relativePath, null);
            }

            // Check file size
            long fileSize = Files.size(candidate);
            if (fileSize > maxFileSize) {
                return new ToolResult(false,
                        String.format("File too large: %d bytes (max: %d bytes)", fileSize, maxFileSize),
                        null);
            }

            // Read and return file content
            String content = Files.readString(candidate);
            return new ToolResult(true, content, null);

        } catch (IOException e) {
            return new ToolResult(false, "Error reading file: " + e.getMessage(), null);
        } catch (SecurityException e) {
            return new ToolResult(false, "Security error: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ToolResult(false, "Unexpected error: " + e.getMessage(), null);
        }
    }

    /**
     * Validates the tool input for completeness and basic security.
     *
     * @param input the tool input to validate
     * @return validation result indicating success or specific error
     */
    private ValidationResult validateInput(ToolInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }

        if (input.text() == null) {
            return ValidationResult.invalid("File path cannot be null");
        }

        String path = input.text().trim();
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
        String normalized = java.text.Normalizer.normalize(path, java.text.Normalizer.Form.NFC);
        if (!path.equals(normalized)) {
            return ValidationResult.invalid("File path contains potentially dangerous Unicode sequences");
        }

        return ValidationResult.valid();
    }

    /**
     * Validates the input path before resolution for obvious attack patterns.
     */
    private SecurityValidationResult validatePath(String path) {
        // Check for null bytes (common in path traversal attacks)
        if (path.contains("\0")) {
            return SecurityValidationResult.invalid("Path contains null byte");
        }

        // Check for suspicious patterns
        if (SUSPICIOUS_PATTERNS.matcher(path).matches()) {
            return SecurityValidationResult.invalid("Path contains suspicious patterns: " + path);
        }

        // Check for absolute paths
        if (Paths.get(path).isAbsolute()) {
            return SecurityValidationResult.invalid("Absolute paths not allowed: " + path);
        }

        // Check for excessively long paths
        ApplicationConfig.ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        if (path.length() > config.getFileReadMaxPathLength()) {
            return SecurityValidationResult.invalid("Path too long");
        }

        // Check for too many path components (potential zip bomb via deep nesting)
        String[] components = path.split("[/\\\\]");
        if (components.length > 10) {
            return SecurityValidationResult.invalid("Path has too many components");
        }

        return SecurityValidationResult.valid();
    }

    /**
     * Validates the resolved path for additional security checks.
     */
    private SecurityValidationResult validateResolvedPath(Path resolvedPath) throws IOException {
        // First check if the path exists
        if (!Files.exists(resolvedPath)) {
            return SecurityValidationResult.valid(); // Let the caller handle file not found
        }

        // Normalize and get real path to resolve all symbolic links
        Path realPath;
        try {
            realPath = resolvedPath.toRealPath();
        } catch (IOException e) {
            // Path exists but can't be resolved
            return SecurityValidationResult.invalid("Cannot resolve path: " + e.getMessage());
        }

        // Check that the real path is still within the base directory
        if (!realPath.startsWith(baseDir)) {
            return SecurityValidationResult.invalid("Path escapes base directory: " + resolvedPath);
        }

        // Check for symbolic links if not allowed
        if (!allowSymlinks && Files.isSymbolicLink(resolvedPath)) {
            return SecurityValidationResult.invalid("Symbolic links not allowed: " + resolvedPath);
        }

        // Additional check: verify the resolved path components don't contain suspicious elements
        for (Path component : realPath) {
            String componentStr = component.toString();
            if (componentStr.startsWith(".") && !componentStr.equals(".") && !componentStr.equals("..")) {
                // Allow hidden files but log suspicious patterns
                if (componentStr.contains("..") || componentStr.contains("/") || componentStr.contains("\\")) {
                    return SecurityValidationResult.invalid("Suspicious path component: " + componentStr);
                }
            }
        }

        // Case sensitivity check on Windows-like systems
        // Verify that the case-sensitive path matches what was requested
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                Path relativePath = baseDir.relativize(realPath);
                // This is a simplified check - more sophisticated validation could be added
                // For now, we'll skip this check as it's complex to implement correctly
            } catch (Exception e) {
                // Skip case sensitivity check if path operations fail
            }
        }

        return SecurityValidationResult.valid();
    }

    /**
     * Result class for security validation.
     */
    private static class SecurityValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private SecurityValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static SecurityValidationResult valid() {
            return new SecurityValidationResult(true, null);
        }

        public static SecurityValidationResult invalid(String errorMessage) {
            return new SecurityValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Result class for input validation.
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

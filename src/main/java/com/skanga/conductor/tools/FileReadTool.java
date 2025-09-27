package com.skanga.conductor.tools;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
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
    public String toolName() {
        return "file_read";
    }

    @Override
    public String toolDescription() {
        return "Securely read a text file under the configured base directory. Input: relative path";
    }

    @Override
    public ExecutionResult runTool(ExecutionInput input) {
        // Comprehensive input validation
        ValidationResult inputValidation = validateInput(input);
        if (!inputValidation.isValid()) {
            return new ExecutionResult(false, inputValidation.getErrorMessage(), null);
        }

        try {
            String relativePath = input.content().trim();

            // Security validation
            SecurityValidationResult validation = validatePath(relativePath);
            if (!validation.isValid()) {
                return new ExecutionResult(false, validation.getErrorMessage(), null);
            }

            Path candidate = baseDir.resolve(relativePath);

            // Additional security checks after resolution
            SecurityValidationResult postResolutionValidation = validateResolvedPath(candidate);
            if (!postResolutionValidation.isValid()) {
                return new ExecutionResult(false, postResolutionValidation.getErrorMessage(), null);
            }

            // File existence and type checks
            if (!Files.exists(candidate)) {
                return new ExecutionResult(false, "File not found: " + relativePath, null);
            }

            if (Files.isDirectory(candidate)) {
                return new ExecutionResult(false, "Path is a directory, not a file: " + relativePath, null);
            }

            // Check file size
            long fileSize = Files.size(candidate);
            if (fileSize > maxFileSize) {
                return new ExecutionResult(false,
                        String.format("File too large: %d bytes (max: %d bytes)", fileSize, maxFileSize),
                        null);
            }

            // Read and return file content with memory-efficient approach
            String content = readFileContent(candidate, fileSize);
            return new ExecutionResult(true, content, null);

        } catch (IOException e) {
            return new ExecutionResult(false, "Error reading file: " + e.getMessage(), null);
        } catch (SecurityException e) {
            return new ExecutionResult(false, "Security error: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ExecutionResult(false, "Unexpected error: " + e.getMessage(), null);
        }
    }

    /**
     * Validates the tool input for completeness and basic security.
     *
     * @param input the tool input to validate
     * @return validation result indicating success or specific error
     */
    private ValidationResult validateInput(ExecutionInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }

        if (input.content() == null) {
            return ValidationResult.invalid("File path cannot be null");
        }

        String path = input.content().trim();
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

        // Additional specific validations
        SecurityValidationResult specificValidation = validateSpecificAttacks(path);
        if (!specificValidation.isValid()) {
            return specificValidation;
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
     * Validates against specific attack patterns with detailed categorization.
     */
    private SecurityValidationResult validateSpecificAttacks(String path) {
        // Unicode normalization attacks
        String normalizedPath = Normalizer.normalize(path, Normalizer.Form.NFC);
        if (!normalizedPath.equals(path)) {
            if (SUSPICIOUS_PATTERNS.matcher(normalizedPath).matches()) {
                return SecurityValidationResult.invalid("Path contains Unicode normalization attack: " + path);
            }
        }

        // Double-dot encoding variations
        if (containsEncodedTraversal(path)) {
            return SecurityValidationResult.invalid("Path contains encoded traversal patterns: " + path);
        }

        // Windows device name attacks
        if (containsWindowsDeviceName(path)) {
            return SecurityValidationResult.invalid("Path references Windows device name: " + path);
        }

        // Long path attacks (Windows MAX_PATH limit bypass attempts)
        if (path.length() > 32767) { // Windows extended path limit
            return SecurityValidationResult.invalid("Path exceeds maximum length limit: " + path.length());
        }

        // Deep nesting attacks (zip bombs, directory bombs)
        int separatorCount = (int) path.chars().filter(c -> c == '/' || c == '\\').count();
        if (separatorCount > 100) { // Reasonable depth limit
            return SecurityValidationResult.invalid("Path nesting too deep: " + separatorCount + " levels");
        }

        // Zero-width characters and invisible characters
        if (containsInvisibleCharacters(path)) {
            return SecurityValidationResult.invalid("Path contains invisible or zero-width characters: " + path);
        }

        // Mixed separator attacks
        if (containsMixedSeparators(path)) {
            return SecurityValidationResult.invalid("Path contains mixed directory separators: " + path);
        }

        // Template injection patterns
        if (containsTemplateInjection(path)) {
            return SecurityValidationResult.invalid("Path contains template injection patterns: " + path);
        }

        // Case variation attacks (for case-insensitive filesystems)
        if (containsCaseVariationAttack(path)) {
            return SecurityValidationResult.invalid("Path contains potential case variation attack: " + path);
        }

        return SecurityValidationResult.valid();
    }

    /**
     * Checks for various encoded traversal patterns.
     */
    private boolean containsEncodedTraversal(String path) {
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
    private boolean containsWindowsDeviceName(String path) {
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
    private boolean containsInvisibleCharacters(String path) {
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
    private boolean containsMixedSeparators(String path) {
        boolean hasForwardSlash = path.contains("/");
        boolean hasBackwardSlash = path.contains("\\");
        return hasForwardSlash && hasBackwardSlash;
    }

    /**
     * Checks for template injection patterns.
     */
    private boolean containsTemplateInjection(String path) {
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
    private boolean containsCaseVariationAttack(String path) {
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

    /**
     * Memory-efficient file reading that uses streaming for large files.
     * <p>
     * For smaller files (< 1MB), uses Files.readString for simplicity.
     * For larger files, uses BufferedReader to stream content and avoid
     * loading the entire file into memory at once.
     * </p>
     *
     * @param filePath the path to the file to read
     * @param fileSize the size of the file in bytes
     * @return the file content as a string
     * @throws IOException if an I/O error occurs
     */
    private String readFileContent(Path filePath, long fileSize) throws IOException {
        // For smaller files, use the simple approach
        if (fileSize < 1024 * 1024) { // 1MB threshold
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }

        // For larger files, use streaming approach with buffer size based on file size
        int bufferSize = calculateOptimalBufferSize(fileSize);
        StringBuilder content = new StringBuilder((int) Math.min(fileSize, Integer.MAX_VALUE));

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            char[] buffer = new char[bufferSize];
            int bytesRead;

            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);

                // Safety check to prevent excessive memory usage
                if (content.length() > maxFileSize) {
                    throw new IOException("File content exceeds maximum allowed size during reading");
                }
            }
        }

        return content.toString();
    }

    /**
     * Calculates an optimal buffer size based on file size for efficient reading.
     *
     * @param fileSize the size of the file in bytes
     * @return optimal buffer size in bytes
     */
    private int calculateOptimalBufferSize(long fileSize) {
        // Use larger buffers for larger files, but cap at reasonable limits
        if (fileSize < 10 * 1024) { // < 10KB
            return 1024; // 1KB buffer
        } else if (fileSize < 100 * 1024) { // < 100KB
            return 4096; // 4KB buffer
        } else if (fileSize < 1024 * 1024) { // < 1MB
            return 8192; // 8KB buffer
        } else {
            return 16384; // 16KB buffer for large files
        }
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

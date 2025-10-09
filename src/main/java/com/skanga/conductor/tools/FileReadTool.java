package com.skanga.conductor.tools;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.ToolConfig;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.tools.security.PathSecurityValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private final long maxFileSize;
    private final PathSecurityValidator securityValidator;

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
        ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        try {
            this.baseDir = Paths.get(config.getFileReadBaseDir()).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + config.getFileReadBaseDir(), e);
        }
        this.maxFileSize = config.getFileReadMaxSize();
        this.securityValidator = new PathSecurityValidator(baseDir, config.getFileReadAllowSymlinks());
    }

    /**
     * Creates a new FileReadTool with a custom base directory.
     * <p>
     * Uses the specified base directory while taking symlink and file size
     * configuration from ApplicationConfig.
     * </p>
     *
     * @param baseDir the base directory path for file operations
     * @throws IllegalArgumentException if the base directory is null, blank, invalid or inaccessible
     */
    public FileReadTool(String baseDir) {
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("baseDir cannot be null or blank");
        }
        ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        try {
            this.baseDir = Paths.get(baseDir).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + baseDir, e);
        }
        this.maxFileSize = config.getFileReadMaxSize();
        this.securityValidator = new PathSecurityValidator(this.baseDir, config.getFileReadAllowSymlinks());
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
     * @throws IllegalArgumentException if the base directory is null, blank, invalid or inaccessible
     */
    public FileReadTool(String baseDir, boolean allowSymlinks) {
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("baseDir cannot be null or blank");
        }
        ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        try {
            this.baseDir = Paths.get(baseDir).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + baseDir, e);
        }
        this.maxFileSize = config.getFileReadMaxSize();
        this.securityValidator = new PathSecurityValidator(this.baseDir, allowSymlinks);
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
     * @throws IllegalArgumentException if the base directory is null, blank, invalid or inaccessible, or maxFileSize is non-positive
     */
    public FileReadTool(String baseDir, boolean allowSymlinks, long maxFileSize) {
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("baseDir cannot be null or blank");
        }
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("maxFileSize must be positive");
        }
        try {
            this.baseDir = Paths.get(baseDir).toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base directory: " + baseDir, e);
        }
        this.maxFileSize = maxFileSize;
        this.securityValidator = new PathSecurityValidator(this.baseDir, allowSymlinks);
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
        PathSecurityValidator.ValidationResult inputValidation = securityValidator.validateInput(
            input != null ? input.content() : null);
        if (!inputValidation.isValid()) {
            return new ExecutionResult(false, inputValidation.getErrorMessage(), null);
        }

        try {
            String relativePath = input.content().trim();

            // Security validation
            PathSecurityValidator.ValidationResult validation = securityValidator.validatePath(relativePath);
            if (!validation.isValid()) {
                return new ExecutionResult(false, validation.getErrorMessage(), null);
            }

            Path candidate = baseDir.resolve(relativePath);

            // Additional security checks after resolution
            PathSecurityValidator.ValidationResult postResolutionValidation = securityValidator.validateResolvedPath(candidate);
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

}

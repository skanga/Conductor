package com.skanga.conductor.demo;

import com.skanga.conductor.exception.ConductorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of FileSystemService for testing purposes.
 * <p>
 * This implementation stores files in memory instead of writing to disk,
 * allowing for fast, isolated testing without file system side effects.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create mock file system
 * MockFileSystemService mockFs = new MockFileSystemService();
 *
 * // Use in workflow
 * BookCreationWorkflow workflow = new BookCreationWorkflow(
 *     orchestrator, llmProvider, "/test/output",
 *     mockFs, new MockUserInputService());
 *
 * // After workflow execution, verify files were created
 * Map<String, String> writtenFiles = mockFs.getWrittenFiles();
 * assertThat(writtenFiles).containsKey("/test/output/01-title-20241225-123456.md");
 * assertThat(writtenFiles.get("/test/output/01-title-20241225-123456.md"))
 *     .contains("Book Title");
 * }</pre>
 *
 * @since 1.0.0
 * @see FileSystemService
 * @see BookCreationWorkflow
 */
public class MockFileSystemService implements FileSystemService {

    private static final Logger logger = LoggerFactory.getLogger(MockFileSystemService.class);

    private final Map<String, String> writtenFiles = new HashMap<>();
    private final Set<String> createdDirectories = new java.util.HashSet<>();
    private final Map<String, Boolean> directoryWritableStatus = new HashMap<>();

    // Configuration for testing different scenarios
    private boolean simulateDirectoryCreationFailure = false;
    private boolean simulateFileWriteFailure = false;
    private String failureMessage = "Simulated failure for testing";

    /**
     * Creates a mock file system service with default successful behavior.
     */
    public MockFileSystemService() {
        // Default constructor
    }

    @Override
    public void createDirectory(String directoryPath) throws ConductorException {
        if (simulateDirectoryCreationFailure) {
            throw new ConductorException(failureMessage);
        }

        createdDirectories.add(directoryPath);
        directoryWritableStatus.put(directoryPath, true);
        logger.debug("Mock created directory: {}", directoryPath);
    }

    @Override
    public void writeFile(String filePath, String content, String allowedBasePath) throws ConductorException {
        if (simulateFileWriteFailure) {
            throw new ConductorException(failureMessage);
        }

        // Simulate path safety check
        if (!isPathSafe(filePath, allowedBasePath)) {
            throw new ConductorException("File path outside of allowed directory: " + filePath);
        }

        writtenFiles.put(filePath, content);
        logger.debug("Mock wrote file: {} ({} chars)", filePath, content.length());
    }

    @Override
    public boolean directoryExists(String directoryPath) {
        boolean exists = createdDirectories.contains(directoryPath);
        logger.debug("Mock directory exists check for '{}': {}", directoryPath, exists);
        return exists;
    }

    @Override
    public boolean isDirectoryWritable(String directoryPath) {
        boolean writable = directoryWritableStatus.getOrDefault(directoryPath, false);
        logger.debug("Mock directory writable check for '{}': {}", directoryPath, writable);
        return writable;
    }

    @Override
    public boolean isPathSafe(String filePath, String allowedBasePath) {
        // Simple mock implementation - just check if file path starts with allowed base
        boolean safe = filePath.startsWith(allowedBasePath);
        logger.debug("Mock path safety check for '{}' against base '{}': {}", filePath, allowedBasePath, safe);
        return safe;
    }

    // Test utility methods

    /**
     * Gets all files written during the test.
     *
     * @return map of file path to content
     */
    public Map<String, String> getWrittenFiles() {
        return new HashMap<>(writtenFiles);
    }

    /**
     * Gets all directories created during the test.
     *
     * @return set of directory paths
     */
    public Set<String> getCreatedDirectories() {
        return new java.util.HashSet<>(createdDirectories);
    }

    /**
     * Gets the content of a specific file.
     *
     * @param filePath the file path
     * @return file content or null if file doesn't exist
     */
    public String getFileContent(String filePath) {
        return writtenFiles.get(filePath);
    }

    /**
     * Checks if a specific file was written.
     *
     * @param filePath the file path to check
     * @return true if the file was written
     */
    public boolean wasFileWritten(String filePath) {
        return writtenFiles.containsKey(filePath);
    }

    /**
     * Configures the mock to simulate directory creation failure.
     *
     * @param shouldFail whether directory creation should fail
     * @param message the error message to use
     */
    public void simulateDirectoryCreationFailure(boolean shouldFail, String message) {
        this.simulateDirectoryCreationFailure = shouldFail;
        this.failureMessage = message;
    }

    /**
     * Configures the mock to simulate file write failure.
     *
     * @param shouldFail whether file writing should fail
     * @param message the error message to use
     */
    public void simulateFileWriteFailure(boolean shouldFail, String message) {
        this.simulateFileWriteFailure = shouldFail;
        this.failureMessage = message;
    }

    /**
     * Resets the mock to initial state.
     */
    public void reset() {
        writtenFiles.clear();
        createdDirectories.clear();
        directoryWritableStatus.clear();
        simulateDirectoryCreationFailure = false;
        simulateFileWriteFailure = false;
        failureMessage = "Simulated failure for testing";
    }

    /**
     * Gets statistics about the mock's usage.
     *
     * @return string with usage statistics
     */
    public String getUsageStats() {
        return String.format("MockFileSystemService - Directories created: %d, Files written: %d",
            createdDirectories.size(), writtenFiles.size());
    }
}
package com.skanga.conductor.demo;

import com.skanga.conductor.exception.ConductorException;

import java.io.IOException;

/**
 * Interface for file system operations to enable testability and dependency injection.
 * <p>
 * This interface abstracts file system operations used by the book creation workflow,
 * allowing for easy mocking in tests and alternative implementations.
 * </p>
 *
 * @since 1.0.0
 * @see BookCreationWorkflow
 */
public interface FileSystemService {

    /**
     * Creates a directory if it doesn't exist and validates it's writable.
     *
     * @param directoryPath the path to the directory to create
     * @throws ConductorException if the directory cannot be created or is not writable
     */
    void createDirectory(String directoryPath) throws ConductorException;

    /**
     * Writes content to a file, creating parent directories if necessary.
     * Performs security validation to prevent path traversal attacks.
     *
     * @param filePath the path to the file to write
     * @param content the content to write
     * @param allowedBasePath the base path that the file must be within (for security)
     * @throws ConductorException if the file cannot be written or path is invalid
     */
    void writeFile(String filePath, String content, String allowedBasePath) throws ConductorException;

    /**
     * Checks if a directory exists.
     *
     * @param directoryPath the path to check
     * @return true if the directory exists, false otherwise
     */
    boolean directoryExists(String directoryPath);

    /**
     * Checks if a directory is writable.
     *
     * @param directoryPath the path to check
     * @return true if the directory is writable, false otherwise
     */
    boolean isDirectoryWritable(String directoryPath);

    /**
     * Validates that a file path is within an allowed base directory.
     * This prevents path traversal attacks.
     *
     * @param filePath the file path to validate
     * @param allowedBasePath the base path the file must be within
     * @return true if the path is safe, false otherwise
     */
    boolean isPathSafe(String filePath, String allowedBasePath);
}
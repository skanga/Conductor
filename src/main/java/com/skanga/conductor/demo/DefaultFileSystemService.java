package com.skanga.conductor.demo;

import com.skanga.conductor.exception.ConductorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default implementation of FileSystemService using standard Java NIO operations.
 * <p>
 * This implementation provides production-ready file system operations with
 * proper error handling, security validation, and logging.
 * </p>
 *
 * @since 1.0.0
 * @see FileSystemService
 * @see BookCreationWorkflow
 */
public class DefaultFileSystemService implements FileSystemService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFileSystemService.class);

    @Override
    public void createDirectory(String directoryPath) throws ConductorException {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory path cannot be null or empty");
        }

        try {
            Path dir = Paths.get(directoryPath).normalize();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                logger.info("Created directory: {}", directoryPath);
            }

            // Validate that the directory is writable
            if (!Files.isWritable(dir)) {
                throw new ConductorException("Directory is not writable: " + directoryPath);
            }
        } catch (IOException e) {
            throw new ConductorException("Failed to create directory: " + directoryPath, e);
        }
    }

    @Override
    public void writeFile(String filePath, String content, String allowedBasePath) throws ConductorException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (allowedBasePath == null || allowedBasePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Allowed base path cannot be null or empty");
        }

        try {
            Path normalizedFilePath = Paths.get(filePath).normalize();
            Path normalizedBasePath = Paths.get(allowedBasePath).normalize();

            // Security check: ensure file is within allowed base directory
            if (!normalizedFilePath.startsWith(normalizedBasePath)) {
                throw new ConductorException("File path outside of allowed directory: " + filePath);
            }

            // Ensure parent directory exists
            Path parentDir = normalizedFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.writeString(normalizedFilePath, content);
            logger.info("Successfully wrote file: {}", filePath);
        } catch (IOException e) {
            throw new ConductorException("Failed to write file: " + filePath, e);
        }
    }

    @Override
    public boolean directoryExists(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return false;
        }

        try {
            return Files.exists(Paths.get(directoryPath));
        } catch (Exception e) {
            logger.debug("Error checking directory existence for {}: {}", directoryPath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isDirectoryWritable(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return false;
        }

        try {
            Path dir = Paths.get(directoryPath);
            return Files.exists(dir) && Files.isDirectory(dir) && Files.isWritable(dir);
        } catch (Exception e) {
            logger.debug("Error checking directory writability for {}: {}", directoryPath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isPathSafe(String filePath, String allowedBasePath) {
        if (filePath == null || allowedBasePath == null) {
            return false;
        }

        try {
            Path normalizedFilePath = Paths.get(filePath).normalize();
            Path normalizedBasePath = Paths.get(allowedBasePath).normalize();
            return normalizedFilePath.startsWith(normalizedBasePath);
        } catch (Exception e) {
            logger.debug("Error validating path safety for {} against base {}: {}",
                filePath, allowedBasePath, e.getMessage());
            return false;
        }
    }
}
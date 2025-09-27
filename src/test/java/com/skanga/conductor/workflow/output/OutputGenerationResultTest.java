package com.skanga.conductor.workflow.output;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutputGenerationResultTest {

    private OutputGenerationResult result;
    private long beforeCreationTime;

    @BeforeEach
    void setUp() {
        beforeCreationTime = System.currentTimeMillis();
        result = new OutputGenerationResult();
    }

    @Test
    void shouldCreateOutputGenerationResultWithDefaultValues() {
        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getFileCount());
        assertTrue(result.getGeneratedFiles().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldAddGeneratedFileCorrectly() {
        // Given
        Path testFile = Paths.get("/tmp/output/test.txt");

        // When
        result.addGeneratedFile(testFile);

        // Then
        assertEquals(1, result.getFileCount());
        assertTrue(result.getGeneratedFiles().contains(testFile));
        assertTrue(result.isSuccess());
        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAddMultipleGeneratedFiles() {
        // Given
        Path file1 = Paths.get("/tmp/output/file1.txt");
        Path file2 = Paths.get("/tmp/output/file2.md");
        Path file3 = Paths.get("/tmp/output/file3.json");

        // When
        result.addGeneratedFile(file1);
        result.addGeneratedFile(file2);
        result.addGeneratedFile(file3);

        // Then
        assertEquals(3, result.getFileCount());
        List<Path> files = result.getGeneratedFiles();
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
        assertTrue(files.contains(file3));
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldAddErrorAndUpdateSuccessStatus() {
        // Given
        String errorMessage = "Failed to write file";

        // When
        result.addError(errorMessage);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().contains(errorMessage));
    }

    @Test
    void shouldAddMultipleErrors() {
        // Given
        String error1 = "File not found";
        String error2 = "Permission denied";
        String error3 = "Disk full";

        // When
        result.addError(error1);
        result.addError(error2);
        result.addError(error3);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
        assertEquals(3, result.getErrors().size());
        List<String> errors = result.getErrors();
        assertTrue(errors.contains(error1));
        assertTrue(errors.contains(error2));
        assertTrue(errors.contains(error3));
    }

    @Test
    void shouldReturnDefensiveCopyOfGeneratedFiles() {
        // Given
        Path testFile = Paths.get("/tmp/test.txt");
        result.addGeneratedFile(testFile);

        // When
        List<Path> files1 = result.getGeneratedFiles();
        List<Path> files2 = result.getGeneratedFiles();

        // Then
        assertNotSame(files1, files2); // Different list instances
        assertEquals(files1, files2);  // Same content

        // Modifying returned list should not affect internal state
        files1.clear();
        assertEquals(1, result.getFileCount()); // Internal list unchanged
    }

    @Test
    void shouldReturnDefensiveCopyOfErrors() {
        // Given
        String error = "Test error";
        result.addError(error);

        // When
        List<String> errors1 = result.getErrors();
        List<String> errors2 = result.getErrors();

        // Then
        assertNotSame(errors1, errors2); // Different list instances
        assertEquals(errors1, errors2);   // Same content

        // Modifying returned list should not affect internal state
        errors1.clear();
        assertEquals(1, result.getErrors().size()); // Internal list unchanged
    }

    @Test
    void shouldCalculateGenerationTimeCorrectly() throws InterruptedException {
        // Given
        long beforeTime = System.currentTimeMillis();

        // Small delay to ensure measurable time difference
        Thread.sleep(10);

        // When
        long generationTime = result.getGenerationTimeMs();
        long afterTime = System.currentTimeMillis();

        // Then
        assertTrue(generationTime >= 0);
        assertTrue(generationTime <= (afterTime - beforeCreationTime + 100)); // Allow some tolerance
    }

    @Test
    void shouldReturnCorrectSuccessStatusWithFilesAndNoErrors() {
        // Given
        result.addGeneratedFile(Paths.get("/tmp/test.txt"));

        // When & Then
        assertTrue(result.isSuccess());
        assertFalse(result.hasErrors());
    }

    @Test
    void shouldReturnCorrectSuccessStatusWithNoFilesAndNoErrors() {
        // When & Then
        assertTrue(result.isSuccess());
        assertFalse(result.hasErrors());
    }

    @Test
    void shouldReturnCorrectSuccessStatusWithFilesAndErrors() {
        // Given
        result.addGeneratedFile(Paths.get("/tmp/test.txt"));
        result.addError("Some error occurred");

        // When & Then
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
    }

    @Test
    void shouldGenerateCorrectSummaryForSuccess() {
        // Given
        result.addGeneratedFile(Paths.get("/tmp/file1.txt"));
        result.addGeneratedFile(Paths.get("/tmp/file2.txt"));

        // When
        String summary = result.getSummary();

        // Then
        assertTrue(summary.contains("Generated 2 file(s)"));
        assertTrue(summary.contains("ms"));
        assertFalse(summary.contains("Failed"));
    }

    @Test
    void shouldGenerateCorrectSummaryForFailure() {
        // Given
        result.addError("Error 1");
        result.addError("Error 2");

        // When
        String summary = result.getSummary();

        // Then
        assertTrue(summary.contains("Failed to generate files"));
        assertTrue(summary.contains("2 error(s)"));
        assertFalse(summary.contains("Generated"));
    }

    @Test
    void shouldGenerateCorrectToString() {
        // Given
        result.addGeneratedFile(Paths.get("/tmp/test.txt"));
        result.addError("Test error");

        // When
        String toString = result.toString();

        // Then
        assertTrue(toString.contains("OutputGenerationResult{"));
        assertTrue(toString.contains("files=1"));
        assertTrue(toString.contains("errors=1"));
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains("duration="));
        assertTrue(toString.contains("ms"));
    }

    @Test
    void shouldHandleNullFilePathGracefully() {
        // When
        result.addGeneratedFile(null);

        // Then
        assertEquals(1, result.getFileCount());
        assertTrue(result.getGeneratedFiles().contains(null));
        assertTrue(result.isSuccess()); // Null file doesn't make it unsuccessful
    }

    @Test
    void shouldHandleNullErrorGracefully() {
        // When
        result.addError(null);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().contains(null));
    }

    @Test
    void shouldHandleEmptyErrorMessage() {
        // Given
        String emptyError = "";

        // When
        result.addError(emptyError);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().contains(emptyError));
    }

    @Test
    void shouldHandleSpecialCharactersInErrors() {
        // Given
        String specialError = "Error with unicode: 你好世界, émojis 🚀, and symbols: @#$%^&*()";

        // When
        result.addError(specialError);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains(specialError));
    }

    @Test
    void shouldHandleMultilineErrors() {
        // Given
        String multilineError = "Error on line 1\nError on line 2\tTabbed error\r\nWindows newline";

        // When
        result.addError(multilineError);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains(multilineError));
    }

    @Test
    void shouldHandleVeryLongErrors() {
        // Given
        StringBuilder longError = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longError.append("Very long error message. ");
        }

        // When
        result.addError(longError.toString());

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains(longError.toString()));
        assertTrue(result.getErrors().get(0).length() > 10000);
    }

    @Test
    void shouldHandleVaryingPathFormats() {
        // Given
        Path[] pathFormats = {
            Paths.get("/tmp/output.txt"),
            Paths.get("C:\\Users\\test\\output.txt"),
            Paths.get("./relative/path.txt"),
            Paths.get("../parent/output.txt"),
            Paths.get("~/home/output.txt"),
            Paths.get("output.txt")
        };

        // When
        for (Path path : pathFormats) {
            result.addGeneratedFile(path);
        }

        // Then
        assertEquals(pathFormats.length, result.getFileCount());
        List<Path> files = result.getGeneratedFiles();
        for (Path path : pathFormats) {
            assertTrue(files.contains(path));
        }
    }

    @Test
    void shouldMaintainOrderOfAddedFiles() {
        // Given
        Path file1 = Paths.get("file1.txt");
        Path file2 = Paths.get("file2.txt");
        Path file3 = Paths.get("file3.txt");

        // When
        result.addGeneratedFile(file1);
        result.addGeneratedFile(file2);
        result.addGeneratedFile(file3);

        // Then
        List<Path> files = result.getGeneratedFiles();
        assertEquals(file1, files.get(0));
        assertEquals(file2, files.get(1));
        assertEquals(file3, files.get(2));
    }

    @Test
    void shouldMaintainOrderOfAddedErrors() {
        // Given
        String error1 = "First error";
        String error2 = "Second error";
        String error3 = "Third error";

        // When
        result.addError(error1);
        result.addError(error2);
        result.addError(error3);

        // Then
        List<String> errors = result.getErrors();
        assertEquals(error1, errors.get(0));
        assertEquals(error2, errors.get(1));
        assertEquals(error3, errors.get(2));
    }

    @Test
    void shouldHandleLargeNumberOfFiles() {
        // Given
        int fileCount = 1000;

        // When
        for (int i = 0; i < fileCount; i++) {
            result.addGeneratedFile(Paths.get("file" + i + ".txt"));
        }

        // Then
        assertEquals(fileCount, result.getFileCount());
        assertTrue(result.isSuccess());
        assertEquals(fileCount, result.getGeneratedFiles().size());
    }

    @Test
    void shouldHandleLargeNumberOfErrors() {
        // Given
        int errorCount = 500;

        // When
        for (int i = 0; i < errorCount; i++) {
            result.addError("Error " + i);
        }

        // Then
        assertFalse(result.isSuccess());
        assertEquals(errorCount, result.getErrors().size());
        assertTrue(result.hasErrors());
    }

    @Test
    void shouldCreateIndependentResults() {
        // Given
        OutputGenerationResult result1 = new OutputGenerationResult();
        OutputGenerationResult result2 = new OutputGenerationResult();

        // When
        result1.addGeneratedFile(Paths.get("file1.txt"));
        result1.addError("Error 1");

        result2.addGeneratedFile(Paths.get("file2.txt"));

        // Then
        assertNotSame(result1, result2);
        assertEquals(1, result1.getFileCount());
        assertEquals(1, result2.getFileCount());
        assertFalse(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertNotEquals(result1.getGeneratedFiles(), result2.getGeneratedFiles());
    }

    @Test
    void shouldHandleMixedOperations() {
        // When - add files and errors in mixed order
        result.addGeneratedFile(Paths.get("file1.txt"));
        result.addError("Error 1");
        result.addGeneratedFile(Paths.get("file2.txt"));
        result.addError("Error 2");
        result.addGeneratedFile(Paths.get("file3.txt"));

        // Then
        assertEquals(3, result.getFileCount());
        assertEquals(2, result.getErrors().size());
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());

        String summary = result.getSummary();
        assertTrue(summary.contains("Failed to generate files"));
        assertTrue(summary.contains("2 error(s)"));
    }
}
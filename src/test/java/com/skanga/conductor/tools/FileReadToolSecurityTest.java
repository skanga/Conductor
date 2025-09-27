package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileReadToolSecurityTest {

    @TempDir
    static Path tempDir;

    private static FileReadTool tool;

    @BeforeAll
    static void setUp() throws IOException {
        // Create test directory structure
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);

        // Create test files
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");

        Path subdirFile = subDir.resolve("nested.txt");
        Files.writeString(subdirFile, "Nested content");

        // Create FileReadTool with temp directory as base
        tool = new FileReadTool(tempDir.toString());
    }

    @Test
    @Order(1)
    @DisplayName("Test legitimate file access")
    void testLegitimateAccess() {
        ExecutionResult result = tool.runTool(new ExecutionInput("test.txt", null));
        assertTrue(result.success(), "Should read legitimate file successfully");
        assertEquals("Test content", result.output(), "Should return correct content");

        ExecutionResult nestedResult = tool.runTool(new ExecutionInput("subdir/nested.txt", null));
        assertTrue(nestedResult.success(), "Should read nested file successfully");
        assertEquals("Nested content", nestedResult.output(), "Should return correct nested content");
    }

    @Test
    @Order(2)
    @DisplayName("Test basic path traversal prevention")
    void testBasicPathTraversalPrevention() {
        String[] traversalAttempts = {
            "../etc/passwd",
            "..\\windows\\system32\\config\\sam",
            "../../../etc/shadow",
            "subdir/../../../etc/passwd",
            "subdir\\..\\..\\..\\windows\\system32",
            "./../etc/passwd",
            ".\\..\\windows\\system32"
        };

        for (String maliciousPath : traversalAttempts) {
            ExecutionResult result = tool.runTool(new ExecutionInput(maliciousPath, null));
            assertFalse(result.success(), "Should reject path traversal attempt: " + maliciousPath);
            assertTrue(result.output().contains("suspicious patterns") ||
                      result.output().contains("escapes base directory") ||
                      result.output().contains("Cannot resolve path") ||
                      result.output().contains("File not found"),
                      "Should indicate security issue for: " + maliciousPath);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test absolute path prevention")
    void testAbsolutePathPrevention() {
        String[] absolutePaths;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            absolutePaths = new String[]{
                "C:\\windows\\system32\\config\\sam",
                "C:/windows/system32/drivers/etc/hosts",
                "\\\\server\\share\\file.txt"
            };
        } else {
            absolutePaths = new String[]{
                "/etc/passwd",
                "/usr/bin/sudo",
                "/home/user/.ssh/id_rsa"
            };
        }

        for (String absolutePath : absolutePaths) {
            ExecutionResult result = tool.runTool(new ExecutionInput(absolutePath, null));
            assertFalse(result.success(), "Should reject absolute path: " + absolutePath);
            assertTrue(result.output().contains("Absolute paths not allowed") ||
                      result.output().contains("suspicious patterns"),
                      "Should indicate absolute path rejection for: " + absolutePath);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test null byte injection prevention")
    void testNullByteInjection() {
        // Note: Java automatically strips null bytes from strings, so this test
        // demonstrates that the file system handles these gracefully
        String[] nullByteAttempts = {
            "test.txt\0",
            "test.txt\0.jpg",
            "subdir/nested.txt\0/../../etc/passwd",
            "\0etc/passwd"
        };

        for (String nullBytePath : nullByteAttempts) {
            ExecutionResult result = tool.runTool(new ExecutionInput(nullBytePath, null));
            if (nullBytePath.equals("test.txt\0")) {
                // This becomes just "test.txt" after null byte removal, so it succeeds
                assertTrue(result.success(), "Null byte should be stripped: " + nullBytePath);
            } else {
                // All other null byte attempts should be rejected
                assertFalse(result.success(), "Should reject null byte injection: " + nullBytePath);
                assertTrue(result.output().contains("null byte") ||
                            result.output().contains("invalid control characters") ||
                            result.output().contains("File not found") ||
                          result.output().contains("suspicious patterns"),
                          "Should indicate security issue for: " + nullBytePath);
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test special character filtering")
    void testSpecialCharacterFiltering() {
        String[] specialCharPaths = {
            "test<script>.txt",
            "test>output.txt",
            "test:stream.txt",
            "test\"quote.txt",
            "test|pipe.txt",
            "test?query.txt",
            "test*wildcard.txt"
        };

        for (String specialPath : specialCharPaths) {
            ExecutionResult result = tool.runTool(new ExecutionInput(specialPath, null));
            assertFalse(result.success(), "Should reject special characters: " + specialPath);
            assertTrue(result.output().contains("suspicious patterns"),
                      "Should indicate suspicious pattern for: " + specialPath);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test long path prevention")
    void testLongPathPrevention() {
        // Create a path longer than 260 characters
        String longPath = "verylongdirectoryname/".repeat(30) +
                "file.txt";

        ExecutionResult result = tool.runTool(new ExecutionInput(longPath, null));
        assertFalse(result.success(), "Should reject excessively long path");
        assertTrue(result.output().contains("Path too long"),
                  "Should indicate path too long");
    }

    @Test
    @Order(7)
    @DisplayName("Test deep nesting prevention")
    void testDeepNestingPrevention() {
        // Create a path with too many components
        StringBuilder deepPath = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            deepPath.append("dir").append(i).append("/");
        }
        deepPath.append("file.txt");

        ExecutionResult result = tool.runTool(new ExecutionInput(deepPath.toString(), null));
        assertFalse(result.success(), "Should reject deeply nested path");
        assertTrue(result.output().contains("too many components"),
                  "Should indicate too many path components");
    }

    @Test
    @Order(8)
    @DisplayName("Test symbolic link handling")
    void testSymbolicLinkHandling() {
        // Skip on Windows as symbolic links require admin privileges
        Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"),
            "Skipping symbolic link tests on Windows");

        // Create a symbolic link that points outside the base directory
        Path linkTarget = Paths.get("/etc/passwd");
        Path symlink = tempDir.resolve("malicious_link");

        try {
            Files.createSymbolicLink(symlink, linkTarget);

            // Test with symlinks not allowed (default)
            ExecutionResult result = tool.runTool(new ExecutionInput("malicious_link", null));
            assertFalse(result.success(), "Should reject symbolic link when not allowed");
            assertTrue(result.output().contains("Symbolic links not allowed") ||
                      result.output().contains("Cannot resolve path"),
                      "Should indicate symbolic link rejection");

            // Test with symlinks allowed
            FileReadTool symlinkTool = new FileReadTool(tempDir.toString(), true);
            ExecutionResult symlinkResult = symlinkTool.runTool(new ExecutionInput("malicious_link", null));
            assertFalse(symlinkResult.success(), "Should still reject symlink that escapes base directory");

        } catch (UnsupportedOperationException | IOException e) {
            // Symbolic links not supported on this system - skip test
            Assumptions.assumeFalse(true, "Symbolic links not supported: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test file size limit enforcement")
    void testFileSizeLimit() throws IOException {
        // Create a tool with very small file size limit
        FileReadTool smallLimitTool = new FileReadTool(tempDir.toString(), false, 10);

        // Create a file larger than the limit
        Path largeFile = tempDir.resolve("large.txt");
        Files.writeString(largeFile, "This content is definitely longer than 10 bytes");

        ExecutionResult result = smallLimitTool.runTool(new ExecutionInput("large.txt", null));
        assertFalse(result.success(), "Should reject file larger than size limit");
        assertTrue(result.output().contains("File too large"),
                  "Should indicate file size limit exceeded");
    }

    @Test
    @Order(10)
    @DisplayName("Test directory access prevention")
    void testDirectoryAccessPrevention() {
        ExecutionResult result = tool.runTool(new ExecutionInput("subdir", null));
        assertFalse(result.success(), "Should reject directory access");
        assertTrue(result.output().contains("directory, not a file"),
                  "Should indicate directory rejection");
    }

    @Test
    @Order(11)
    @DisplayName("Test non-existent file handling")
    void testNonExistentFileHandling() {
        ExecutionResult result = tool.runTool(new ExecutionInput("nonexistent.txt", null));
        assertFalse(result.success(), "Should handle non-existent file gracefully");
        assertTrue(result.output().contains("File not found") ||
                  result.output().contains("Cannot resolve path"),
                  "Should indicate file not found");
    }

    @Test
    @Order(12)
    @DisplayName("Test empty and null input handling")
    void testEmptyInputHandling() {
        ExecutionResult emptyResult = tool.runTool(new ExecutionInput("", null));
        assertFalse(emptyResult.success(), "Should reject empty path");
        assertTrue(emptyResult.output().contains("File path cannot be empty"),
                  "Should indicate no path provided");

        ExecutionResult nullResult = tool.runTool(new ExecutionInput(null, null));
        assertFalse(nullResult.success(), "Should reject null path");
        assertTrue(nullResult.output().contains("File path cannot be null"),
                  "Should indicate no path provided");

        ExecutionResult whitespaceResult = tool.runTool(new ExecutionInput("   ", null));
        assertFalse(whitespaceResult.success(), "Should reject whitespace-only path");
        assertTrue(whitespaceResult.output().contains("File path cannot be empty"),
                  "Should indicate no path provided");
    }

    @Test
    @Order(13)
    @DisplayName("Test hidden file access patterns")
    void testHiddenFilePatterns() throws IOException {
        // Create hidden files (files starting with .)
        Path hiddenFile = tempDir.resolve(".hidden");
        Files.writeString(hiddenFile, "Hidden content");

        // Legitimate hidden file access should work
        ExecutionResult result = tool.runTool(new ExecutionInput(".hidden", null));
        assertTrue(result.success(), "Should allow legitimate hidden file access");

        // But suspicious hidden file patterns should be rejected
        String[] suspiciousHidden = {
            "./../etc/passwd",
            ".\\..\\windows\\system32"
        };

        for (String suspicious : suspiciousHidden) {
            ExecutionResult suspiciousResult = tool.runTool(new ExecutionInput(suspicious, null));
            assertFalse(suspiciousResult.success(), "Should reject suspicious hidden pattern: " + suspicious);
        }
    }

    @Test
    @Order(14)
    @DisplayName("Test Unicode and encoding attacks")
    void testUnicodeAttacks() {
        String[] unicodeAttacks = {
            "test\u002e\u002e/etc/passwd",  // Unicode dots
            "test\uFF0E\uFF0E/etc/passwd",  // Fullwidth dots
            "test\u2024\u2024/etc/passwd",  // One dot leader
            "test%2e%2e/etc/passwd",        // URL encoded dots
            "test\u0000.txt"                // Unicode null
        };

        for (String unicodeAttack : unicodeAttacks) {
            ExecutionResult result = tool.runTool(new ExecutionInput(unicodeAttack, null));
            assertFalse(result.success(), "Should reject Unicode attack: " + unicodeAttack);
            // The specific error message may vary depending on which validation catches it
            assertFalse(result.output().isEmpty(), "Should provide error message for: " + unicodeAttack);
        }
    }

    @Test
    @Order(15)
    @DisplayName("Test constructor validation")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new FileReadTool("/nonexistent/directory/that/does/not/exist"), "Should reject non-existent base directory");
    }
}
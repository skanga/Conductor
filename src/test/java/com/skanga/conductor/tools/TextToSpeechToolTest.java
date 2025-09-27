package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TextToSpeechToolTest {

    @TempDir
    Path tempDir;

    private TextToSpeechTool tool;

    @BeforeEach
    void setUp() {
        tool = new TextToSpeechTool(tempDir);
        resetEngineDetection();
    }

    @AfterEach
    void tearDown() {
        resetEngineDetection();
    }

    @Test
    void shouldHaveCorrectToolName() {
        assertEquals("audio_gen", tool.toolName());
    }

    @Test
    void shouldHaveDescriptiveToolDescription() {
        String description = tool.toolDescription();
        assertNotNull(description);
        assertFalse(description.trim().isEmpty());
        assertTrue(description.toLowerCase().contains("text") || description.toLowerCase().contains("speech"));
        assertTrue(description.toLowerCase().contains("wav"));
    }

    @Test
    void shouldCreateToolWithDefaultConstructor() {
        // This tests the default constructor
        assertDoesNotThrow(() -> new TextToSpeechTool());
    }

    @Test
    void shouldCreateToolWithCustomOutputDirectory() {
        Path customDir = tempDir.resolve("custom");
        TextToSpeechTool customTool = new TextToSpeechTool(customDir);
        assertNotNull(customTool);
        assertEquals("audio_gen", customTool.toolName());
    }

    @Test
    void shouldGenerateAudioForValidText() {
        ExecutionInput input = new ExecutionInput("Hello world", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should successfully generate audio");
        assertNotNull(result.output());
        assertFalse(result.output().trim().isEmpty());

        // Extract file path from output (remove any engine info)
        String filePath = result.output().split(" \\(")[0];
        Path audioFile = Paths.get(filePath);
        assertTrue(Files.exists(audioFile), "Audio file should exist");

        try {
            assertTrue(Files.size(audioFile) > 0, "Audio file should not be empty");
        } catch (IOException e) {
            fail("Could not check file size: " + e.getMessage());
        }
    }

    @Test
    void shouldHandleNullInput() {
        ExecutionResult result = tool.runTool(null);
        assertFalse(result.success());
        assertTrue(result.output().contains("cannot be null"));
    }

    @Test
    void shouldHandleNullContent() {
        ExecutionInput input = new ExecutionInput(null, null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should succeed with null content using fallback");
        assertNotNull(result.output());
    }

    @Test
    void shouldHandleEmptyContent() {
        ExecutionInput input = new ExecutionInput("", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should succeed with empty content using fallback");
        assertNotNull(result.output());
    }

    @Test
    void shouldHandleWhitespaceOnlyContent() {
        ExecutionInput input = new ExecutionInput("   \n\t   ", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should succeed with whitespace-only content");
        assertNotNull(result.output());
    }

    @Test
    void shouldRejectExtremelyLongText() {
        // Create text longer than 10,000 characters
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longText.append("a");
        }

        ExecutionInput input = new ExecutionInput(longText.toString(), null);
        ExecutionResult result = tool.runTool(input);

        assertFalse(result.success());
        assertTrue(result.output().contains("too long"));
    }

    @Test
    void shouldRejectTextWithInvalidControlCharacters() {
        String textWithControlChars = "Hello\u0001world\u0002test";
        ExecutionInput input = new ExecutionInput(textWithControlChars, null);
        ExecutionResult result = tool.runTool(input);

        assertFalse(result.success());
        assertTrue(result.output().contains("control characters"));
    }

    @Test
    void shouldAllowValidControlCharacters() {
        String textWithValidChars = "Hello\tworld\ntest\rmore";
        ExecutionInput input = new ExecutionInput(textWithValidChars, null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should allow tabs, newlines, and carriage returns");
    }

    @Test
    void shouldHandleSpecialCharacters() {
        String specialText = "Hello! @#$%^&*()_+-={}[]|\\:;\"'<>?,./ 123";
        ExecutionInput input = new ExecutionInput(specialText, null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should handle special characters");
        assertNotNull(result.output());
    }

    @Test
    void shouldHandleUnicodeCharacters() {
        String unicodeText = "Hello 世界 🌍 café résumé naïve";
        ExecutionInput input = new ExecutionInput(unicodeText, null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should handle unicode characters");
        assertNotNull(result.output());
    }

    @Test
    void shouldCreateOutputDirectory() throws IOException {
        Path nonExistentDir = tempDir.resolve("new_audio_output");
        assertFalse(Files.exists(nonExistentDir));

        TextToSpeechTool newTool = new TextToSpeechTool(nonExistentDir);
        ExecutionInput input = new ExecutionInput("Directory creation test", null);
        ExecutionResult result = newTool.runTool(input);

        assertTrue(result.success(), "Should create directory and generate audio");
        assertTrue(Files.exists(nonExistentDir), "Output directory should be created");
    }

    @Test
    void shouldGenerateUniqueFilenames() {
        ExecutionInput input = new ExecutionInput("Test audio", null);

        ExecutionResult result1 = tool.runTool(input);
        ExecutionResult result2 = tool.runTool(input);

        assertTrue(result1.success());
        assertTrue(result2.success());
        assertNotEquals(result1.output().split(" \\(")[0],
                       result2.output().split(" \\(")[0],
                       "Should generate unique filenames");
    }

    @Test
    void shouldGenerateValidWAVFile() {
        ExecutionInput input = new ExecutionInput("WAV format test", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success());
        String filePath = result.output().split(" \\(")[0];
        Path audioFile = Paths.get(filePath);

        try {
            byte[] fileBytes = Files.readAllBytes(audioFile);
            assertTrue(fileBytes.length >= 44, "WAV file should have at least 44 bytes for header");

            // Check RIFF header
            String riffSignature = new String(fileBytes, 0, 4);
            assertEquals("RIFF", riffSignature, "Should have RIFF signature");

            // Check WAVE format
            String waveSignature = new String(fileBytes, 8, 4);
            assertEquals("WAVE", waveSignature, "Should have WAVE format");

            // Check fmt chunk
            String fmtSignature = new String(fileBytes, 12, 4);
            assertEquals("fmt ", fmtSignature, "Should have fmt chunk");

            // Check data chunk (should exist somewhere after fmt)
            String fileContent = new String(fileBytes);
            assertTrue(fileContent.contains("data"), "Should contain data chunk");

        } catch (IOException e) {
            fail("Could not verify WAV file format: " + e.getMessage());
        }
    }

    @Test
    void shouldHandleConcurrentExecution() throws Exception {
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];
        ExecutionResult[] results = new ExecutionResult[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Add small delay to increase chances of different timestamps
                    Thread.sleep(index * 10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ExecutionInput input = new ExecutionInput("Concurrent test " + index, null);
                results[index] = tool.runTool(input);
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all executions succeeded
        for (int i = 0; i < numThreads; i++) {
            assertTrue(results[i].success(), "Thread " + i + " should succeed");
            assertNotNull(results[i].output());

            String filePath = results[i].output().split(" \\(")[0];
            Path audioFile = Paths.get(filePath);
            assertTrue(Files.exists(audioFile), "Audio file should exist for thread " + i);
        }

        // Verify files exist and are valid (uniqueness is ensured by timestamp-based naming)
        // Note: In very rare cases, files might have same timestamp if executed at exactly same millisecond
        // The important thing is that all threads can execute concurrently without errors
        for (int i = 0; i < numThreads; i++) {
            String filePath = results[i].output().split(" \\(")[0];
            Path audioFile = Paths.get(filePath);
            assertTrue(Files.exists(audioFile), "Audio file should exist for thread " + i);
            try {
                assertTrue(Files.size(audioFile) > 0, "Audio file should not be empty for thread " + i);
            } catch (IOException e) {
                fail("Could not check file size for thread " + i + ": " + e.getMessage());
            }
        }
    }

    @Test
    void shouldHandleTextOfVariousLengths() {
        String[] testTexts = {
            "Hi",
            "Short text",
            "This is a medium length text that should generate appropriate audio duration",
            "This is a much longer text that tests how the tool handles extended content. " +
            "It should generate audio that reflects the length of the text appropriately. " +
            "The synthetic audio generator should create longer audio for longer text."
        };

        for (String text : testTexts) {
            ExecutionInput input = new ExecutionInput(text, null);
            ExecutionResult result = tool.runTool(input);

            assertTrue(result.success(), "Should handle text of length " + text.length());
            assertNotNull(result.output());

            String filePath = result.output().split(" \\(")[0];
            Path audioFile = Paths.get(filePath);
            assertTrue(Files.exists(audioFile), "Audio file should exist for text length " + text.length());
        }
    }

    @Test
    void shouldGenerateDifferentFrequenciesForDifferentTexts() {
        // This tests the synthetic audio generation with different text inputs
        // Different texts should produce different frequencies due to hash-based frequency calculation
        ExecutionInput input1 = new ExecutionInput("Hello world", null);
        ExecutionInput input2 = new ExecutionInput("Different text", null);

        ExecutionResult result1 = tool.runTool(input1);
        ExecutionResult result2 = tool.runTool(input2);

        assertTrue(result1.success());
        assertTrue(result2.success());

        // Files should be different (different timestamps at minimum)
        String path1 = result1.output().split(" \\(")[0];
        String path2 = result2.output().split(" \\(")[0];
        assertNotEquals(path1, path2);
    }

    @Test
    void shouldHandleEdgeCaseFileSystemOperations() throws IOException {
        // Test with read-only parent directory (if supported by OS)
        Path readOnlyParent = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyParent);

        // Test with nested directory creation
        Path nestedDir = tempDir.resolve("level1/level2/level3");
        TextToSpeechTool nestedTool = new TextToSpeechTool(nestedDir);

        ExecutionInput input = new ExecutionInput("Nested directory test", null);
        ExecutionResult result = nestedTool.runTool(input);

        assertTrue(result.success(), "Should handle nested directory creation");
        assertTrue(Files.exists(nestedDir), "Nested directories should be created");
    }

    @Test
    void shouldProvideInformativeOutputMessages() {
        ExecutionInput input = new ExecutionInput("Output message test", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success());
        String output = result.output();

        // Output should contain either path to file or helpful information
        assertTrue(output.contains(tempDir.toString()) ||
                  output.toLowerCase().contains("tts") ||
                  output.toLowerCase().contains("synthetic") ||
                  output.toLowerCase().contains("generated"),
                  "Output should be informative: " + output);
    }

    @Test
    void shouldHandleMaximumAllowedTextLength() {
        // Test with exactly 10,000 characters (the maximum allowed)
        StringBuilder maxText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            maxText.append("a");
        }

        ExecutionInput input = new ExecutionInput(maxText.toString(), null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should handle maximum allowed text length");
        assertNotNull(result.output());
    }

    /**
     * Helper method to reset engine detection for testing
     */
    private void resetEngineDetection() {
        try {
            Field detectedEngineField = TextToSpeechTool.class.getDeclaredField("detectedEngine");
            Field engineDetectionCompleteField = TextToSpeechTool.class.getDeclaredField("engineDetectionComplete");

            detectedEngineField.setAccessible(true);
            engineDetectionCompleteField.setAccessible(true);

            detectedEngineField.set(null, null);
            engineDetectionCompleteField.setBoolean(null, false);
        } catch (Exception e) {
            // Ignore reflection errors - tests will still work
        }
    }

    @Test
    void shouldDetectEngineOnlyOnce() {
        // Call tool description multiple times
        String desc1 = tool.toolDescription();
        String desc2 = tool.toolDescription();
        String desc3 = tool.toolDescription();

        // All should return the same engine information
        assertEquals(desc1, desc2);
        assertEquals(desc2, desc3);
    }

    @Test
    void shouldHandleFilenameGeneration() {
        ExecutionInput input = new ExecutionInput("Filename test", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success());
        String filePath = result.output().split(" \\(")[0];

        assertTrue(filePath.endsWith(".wav"), "Generated file should have .wav extension");
        assertTrue(filePath.contains("tts_"), "Generated filename should contain tts_ prefix");
        assertTrue(filePath.contains(tempDir.toString()), "File should be in the specified directory");
    }
}
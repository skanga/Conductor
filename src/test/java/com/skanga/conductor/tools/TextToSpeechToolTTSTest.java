package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the enhanced SimpleAudioTool with real TTS capabilities.
 */
class TextToSpeechToolTTSTest {

    @TempDir
    Path tempDir;

    @Test
    @Timeout(60) // 60 second timeout for TTS generation
    void testTTSGeneration() {
        TextToSpeechTool tool = new TextToSpeechTool(tempDir);

        // Test TTS generation with sample text
        ExecutionInput input = new ExecutionInput("Hello world, this is a test of text to speech functionality.", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "TTS generation should succeed");
        assertNotNull(result.output(), "Should return output path");
        assertFalse(result.output().trim().isEmpty(), "Output path should not be empty");

        String output = result.output();
        System.out.println("=== TTS Audio Generation Result ===");
        System.out.println(output);
        System.out.println("===================================");

        // Extract the file path (remove engine info if present)
        String filePath = output.split(" \\(")[0];
        Path audioFile = Paths.get(filePath);

        // Verify the audio file was created
        assertTrue(Files.exists(audioFile), "Audio file should exist: " + audioFile);

        try {
            long fileSize = Files.size(audioFile);
            assertTrue(fileSize > 0, "Audio file should not be empty");
            System.out.println("Generated audio file: " + audioFile);
            System.out.println("File size: " + fileSize + " bytes");

            // Basic WAV header verification
            byte[] header = Files.readAllBytes(audioFile);
            assertTrue(header.length >= 44, "WAV file should have at least 44 bytes for header");

            // Check RIFF header
            String riffSignature = new String(header, 0, 4);
            assertEquals("RIFF", riffSignature, "Should have RIFF signature");

            // Check WAVE format
            String waveSignature = new String(header, 8, 4);
            assertEquals("WAVE", waveSignature, "Should have WAVE format");

        } catch (Exception e) {
            fail("Failed to verify audio file: " + e.getMessage());
        }
    }

    @Test
    void testEmptyTextHandling() {
        TextToSpeechTool tool = new TextToSpeechTool(tempDir);

        // Test with empty text
        ExecutionInput input = new ExecutionInput("", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Empty text should still generate audio");
        assertTrue(result.output().contains("Silent audio") ||
                  Files.exists(Paths.get(result.output().split(" \\(")[0])),
                  "Should generate audio for empty text");
    }

    @Test
    void testLongTextHandling() {
        TextToSpeechTool tool = new TextToSpeechTool(tempDir);

        // Test with longer text
        String longText = "This is a longer piece of text to test how the text-to-speech engine handles " +
                         "multiple sentences and longer content. It should generate appropriate audio " +
                         "that speaks all of this text clearly and at a reasonable pace.";

        ExecutionInput input = new ExecutionInput(longText, null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Long text TTS should succeed");
        assertNotNull(result.output(), "Should return output path");

        String filePath = result.output().split(" \\(")[0];
        Path audioFile = Paths.get(filePath);
        assertTrue(Files.exists(audioFile), "Audio file should exist for long text");
    }

    @Test
    void testSpecialCharactersHandling() {
        TextToSpeechTool tool = new TextToSpeechTool(tempDir);

        // Test with special characters and punctuation
        String specialText = "Hello! How are you? I'm fine, thanks. Let's test: numbers 1, 2, 3; " +
                           "symbols @#$%; and quotes \"like this\".";

        ExecutionInput input = new ExecutionInput(specialText, null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Special characters should be handled properly");
        assertNotNull(result.output(), "Should return output path");
    }

    @Test
    void testInputValidation() {
        TextToSpeechTool tool = new TextToSpeechTool(tempDir);

        // Test null input
        ExecutionResult result = tool.runTool(null);
        assertFalse(result.success(), "Null input should fail");

        // Test null content (should succeed with fallback)
        ExecutionInput nullContent = new ExecutionInput(null, null);
        result = tool.runTool(nullContent);
        assertTrue(result.success(), "Null content should succeed with fallback text");
    }

    @Test
    void testToolMetadata() {
        TextToSpeechTool tool = new TextToSpeechTool(tempDir);

        assertEquals("audio_gen", tool.toolName());
        assertNotNull(tool.toolDescription());
        assertTrue(tool.toolDescription().toLowerCase().contains("tts") ||
                  tool.toolDescription().toLowerCase().contains("speech") ||
                  tool.toolDescription().toLowerCase().contains("audio"));
    }

    @Test
    void testOutputDirectoryCreation() {
        // Create tool with non-existent directory
        Path nonExistentDir = tempDir.resolve("new_audio_dir");
        assertFalse(Files.exists(nonExistentDir), "Directory should not exist initially");

        TextToSpeechTool tool = new TextToSpeechTool(nonExistentDir);
        ExecutionInput input = new ExecutionInput("Directory creation test", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success(), "Should create directory and generate audio");
        assertTrue(Files.exists(nonExistentDir), "Directory should be created");
    }
}
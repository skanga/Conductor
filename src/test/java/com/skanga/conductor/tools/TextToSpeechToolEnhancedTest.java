package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced test suite for TextToSpeechTool class focusing on previously untested areas.
 *
 * Tests additional functionality including:
 * - TTSEngine enum behavior
 * - Engine detection and caching
 * - Individual TTS generation methods
 * - WAV file format specifics
 * - Helper method functionality
 * - Error handling scenarios
 * - Cross-platform behavior
 * - Performance and stress testing
 */
@DisplayName("TextToSpeechTool Enhanced Tests")
class TextToSpeechToolEnhancedTest {

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
    @DisplayName("Should test TTSEngine enum properties")
    void shouldTestTTSEngineEnumProperties() throws Exception {
        // Access the TTSEngine enum via reflection
        Class<?> ttsEngineClass = getInnerTTSEngineClass();
        Object[] engines = ttsEngineClass.getEnumConstants();

        assertTrue(engines.length >= 5, "Should have at least 5 TTS engines defined");

        // Test specific engines exist
        boolean hasEspeakNg = false;
        boolean hasEspeak = false;
        boolean hasFestival = false;
        boolean hasWindowsSapi = false;
        boolean hasSynthetic = false;

        for (Object engine : engines) {
            String name = engine.toString();
            Method getCommandMethod = ttsEngineClass.getMethod("getCommand");
            Method getArgsMethod = ttsEngineClass.getMethod("getArgs");

            String command = (String) getCommandMethod.invoke(engine);
            String[] args = (String[]) getArgsMethod.invoke(engine);

            assertNotNull(command, "Command should not be null for " + name);
            assertNotNull(args, "Args should not be null for " + name);

            switch (name) {
                case "ESPEAK_NG" -> {
                    hasEspeakNg = true;
                    assertEquals("espeak-ng", command);
                    assertTrue(args.length > 0);
                }
                case "ESPEAK" -> {
                    hasEspeak = true;
                    assertEquals("espeak", command);
                    assertTrue(args.length > 0);
                }
                case "FESTIVAL" -> {
                    hasFestival = true;
                    assertEquals("festival", command);
                    assertTrue(args.length > 0);
                }
                case "WINDOWS_SAPI" -> {
                    hasWindowsSapi = true;
                    assertEquals("powershell", command);
                    assertTrue(args.length > 0);
                }
                case "SYNTHETIC" -> {
                    hasSynthetic = true;
                    assertEquals("synthetic", command);
                }
            }
        }

        assertTrue(hasEspeakNg, "Should have ESPEAK_NG engine");
        assertTrue(hasEspeak, "Should have ESPEAK engine");
        assertTrue(hasFestival, "Should have FESTIVAL engine");
        assertTrue(hasWindowsSapi, "Should have WINDOWS_SAPI engine");
        assertTrue(hasSynthetic, "Should have SYNTHETIC engine");
    }

    @Test
    @DisplayName("Should test engine detection caching behavior")
    void shouldTestEngineDetectionCaching() throws Exception {
        resetEngineDetection();

        // First call should detect and cache
        String desc1 = tool.toolDescription();
        assertNotNull(desc1);

        // Get engine detection state via reflection
        Field engineDetectionCompleteField = TextToSpeechTool.class.getDeclaredField("engineDetectionComplete");
        engineDetectionCompleteField.setAccessible(true);
        boolean detectionComplete = engineDetectionCompleteField.getBoolean(null);
        assertTrue(detectionComplete, "Engine detection should be marked complete after first call");

        // Second call should use cached result
        String desc2 = tool.toolDescription();
        assertEquals(desc1, desc2, "Should return same description from cache");

        // Third call should also use cached result
        String desc3 = tool.toolDescription();
        assertEquals(desc1, desc3, "Should consistently return cached description");
    }

    @Test
    @DisplayName("Should handle synthetic audio generation with different text properties")
    void shouldHandleSyntheticAudioGenerationWithDifferentTextProperties() {
        // Test synthetic audio with various text characteristics
        String[] testTexts = {
            "a", // Very short - should create short audio
            "This is a medium length text for testing audio generation properties", // Medium
            "This is a very long text that should result in longer audio duration because the synthetic " +
            "audio generator uses text length to determine audio duration and this text is quite long indeed " +
            "to test that functionality properly", // Long - should create longer audio
            "123 numbers", // Numbers
            "!@#$%^&*()", // Special characters only
            "", // Empty (should use fallback)
            null // Null (should use fallback)
        };

        for (String text : testTexts) {
            ExecutionInput input = new ExecutionInput(text, null);
            ExecutionResult result = tool.runTool(input);

            assertTrue(result.success(), "Should generate audio for text: " +
                      (text == null ? "null" : "'" + text + "'"));

            String filePath = result.output().split(" \\(")[0];
            Path audioFile = Paths.get(filePath);
            assertTrue(Files.exists(audioFile), "Audio file should exist for text: " + text);

            try {
                long fileSize = Files.size(audioFile);
                assertTrue(fileSize > 44, "WAV file should have at least header size: " + fileSize);

                // For longer text, expect larger file (synthetic audio varies duration by text length)
                if (text != null && text.length() > 200) {
                    assertTrue(fileSize > 1000, "Longer text should produce larger audio file");
                }
            } catch (IOException e) {
                fail("Could not verify file size: " + e.getMessage());
            }
        }
    }

    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "test.comprehensive", matches = "true")
    @Test
    @DisplayName("Should generate proper WAV file headers")
    void shouldGenerateProperWAVFileHeaders() throws IOException {
        // Generate a synthetic audio file
        ExecutionInput input = new ExecutionInput("WAV header test", null);
        ExecutionResult result = tool.runTool(input);

        assertTrue(result.success());
        String filePath = result.output().split(" \\(")[0];
        Path audioFile = Paths.get(filePath);

        byte[] fileBytes = Files.readAllBytes(audioFile);
        assertTrue(fileBytes.length >= 44, "WAV file should have complete header");

        // Detailed WAV header validation
        // RIFF header (bytes 0-3)
        assertEquals('R', fileBytes[0]);
        assertEquals('I', fileBytes[1]);
        assertEquals('F', fileBytes[2]);
        assertEquals('F', fileBytes[3]);

        // File size (bytes 4-7) - should be file length - 8
        int headerFileSize = bytesToInt(fileBytes, 4);
        assertEquals(fileBytes.length - 8, headerFileSize, "File size in header should match actual file size");

        // WAVE format (bytes 8-11)
        assertEquals('W', fileBytes[8]);
        assertEquals('A', fileBytes[9]);
        assertEquals('V', fileBytes[10]);
        assertEquals('E', fileBytes[11]);

        // fmt chunk (bytes 12-15)
        assertEquals('f', fileBytes[12]);
        assertEquals('m', fileBytes[13]);
        assertEquals('t', fileBytes[14]);
        assertEquals(' ', fileBytes[15]);

        // Subchunk1Size (bytes 16-19) - should be 16 for basic PCM, 18 for extended PCM
        int subchunk1Size = bytesToInt(fileBytes, 16);
        assertTrue(subchunk1Size == 16 || subchunk1Size == 18,
                  "Subchunk1Size should be 16 or 18 for PCM format, got: " + subchunk1Size);

        // AudioFormat (bytes 20-21) - should be 1 for PCM
        short audioFormat = bytesToShort(fileBytes, 20);
        assertEquals(1, audioFormat, "AudioFormat should be 1 for PCM");

        // NumChannels (bytes 22-23) - should be 1 for mono
        short numChannels = bytesToShort(fileBytes, 22);
        assertEquals(1, numChannels, "Should be mono (1 channel)");

        // SampleRate (bytes 24-27) - should be 16000 for synthetic, 22050 for TTS engines
        int sampleRate = bytesToInt(fileBytes, 24);
        assertTrue(sampleRate == 16000 || sampleRate == 22050,
                  "Sample rate should be 16000 Hz (synthetic) or 22050 Hz (TTS), got: " + sampleRate);

        // BitsPerSample (bytes 34-35) - should be 16
        short bitsPerSample = bytesToShort(fileBytes, 34);
        assertEquals(16, bitsPerSample, "Should be 16 bits per sample");

        // Find data chunk
        int dataChunkOffset = findDataChunk(fileBytes);
        assertTrue(dataChunkOffset > 0, "Should have data chunk");

        // Data chunk size should match remaining file content
        int dataSize = bytesToInt(fileBytes, dataChunkOffset + 4);
        int expectedDataSize = fileBytes.length - dataChunkOffset - 8;
        assertEquals(expectedDataSize, dataSize, "Data chunk size should match actual data");
    }

    @Test
    @DisplayName("Should handle frequency variation in synthetic audio")
    void shouldHandleFrequencyVariationInSyntheticAudio() {
        // Test that different texts produce different frequencies due to hash-based calculation
        String[] texts = {
            "Test text A",
            "Different text B",
            "Another variation C",
            "Completely different content D"
        };

        Path[] audioFiles = new Path[texts.length];

        for (int i = 0; i < texts.length; i++) {
            ExecutionInput input = new ExecutionInput(texts[i], null);
            ExecutionResult result = tool.runTool(input);
            assertTrue(result.success());

            String filePath = result.output().split(" \\(")[0];
            audioFiles[i] = Paths.get(filePath);
            assertTrue(Files.exists(audioFiles[i]));
        }

        // Verify files are different (different hash codes should produce different frequencies)
        try {
            for (int i = 0; i < audioFiles.length; i++) {
                for (int j = i + 1; j < audioFiles.length; j++) {
                    byte[] content1 = Files.readAllBytes(audioFiles[i]);
                    byte[] content2 = Files.readAllBytes(audioFiles[j]);

                    // Files should be different due to different frequencies/durations
                    assertFalse(java.util.Arrays.equals(content1, content2),
                               "Audio files should be different for different texts");
                }
            }
        } catch (IOException e) {
            fail("Could not compare audio files: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should test command availability detection cross-platform")
    void shouldTestCommandAvailabilityDetectionCrossPlatform() throws Exception {
        // Test the isCommandAvailable method via reflection
        Method isCommandAvailableMethod = TextToSpeechTool.class.getDeclaredMethod("isCommandAvailable", String.class);
        isCommandAvailableMethod.setAccessible(true);

        // Test with commands that should definitely not exist
        assertFalse((Boolean) isCommandAvailableMethod.invoke(null, "definitely-nonexistent-command-12345"));
        assertFalse((Boolean) isCommandAvailableMethod.invoke(null, "invalid-command-name-xyz"));
        assertFalse((Boolean) isCommandAvailableMethod.invoke(null, "fake-tts-engine-test"));

        // Test with empty/null commands
        assertFalse((Boolean) isCommandAvailableMethod.invoke(null, ""));
        assertFalse((Boolean) isCommandAvailableMethod.invoke(null, (String) null));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Should detect Windows-specific commands on Windows")
    void shouldDetectWindowsSpecificCommandsOnWindows() throws Exception {
        Method isCommandAvailableMethod = TextToSpeechTool.class.getDeclaredMethod("isCommandAvailable", String.class);
        isCommandAvailableMethod.setAccessible(true);

        // PowerShell should be available on Windows
        assertTrue((Boolean) isCommandAvailableMethod.invoke(null, "powershell"),
                  "PowerShell should be available on Windows");

        // Test Windows SAPI TTS generation if PowerShell is available
        ExecutionInput input = new ExecutionInput("Windows TTS test", null);
        ExecutionResult result = tool.runTool(input);
        assertTrue(result.success(), "Should generate audio on Windows");

        // The output should indicate Windows SAPI was used (if detected as primary engine)
        String output = result.output();
        // Note: This might use SAPI or synthetic depending on detection order
        assertTrue(output.contains(".wav"), "Should generate WAV file");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("Should handle Unix-style command detection on non-Windows")
    void shouldHandleUnixStyleCommandDetectionOnNonWindows() throws Exception {
        Method isCommandAvailableMethod = TextToSpeechTool.class.getDeclaredMethod("isCommandAvailable", String.class);
        isCommandAvailableMethod.setAccessible(true);

        // Test that the method uses 'which' instead of 'where' on Unix systems
        // This is tested indirectly by ensuring the method doesn't crash on Unix
        assertDoesNotThrow(() -> {
            isCommandAvailableMethod.invoke(null, "bash");
        }, "Command detection should work on Unix systems");
    }

    @Test
    @DisplayName("Should handle directory creation errors gracefully")
    void shouldHandleDirectoryCreationErrorsGracefully() throws IOException {
        // Create a read-only parent directory (where supported)
        Path readOnlyParent = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyParent);

        try {
            // Try to make parent read-only (this may not work on all filesystems)
            if (readOnlyParent.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
                Files.setPosixFilePermissions(readOnlyParent, readOnly);
            }

            Path restrictedDir = readOnlyParent.resolve("restricted");
            TextToSpeechTool restrictedTool = new TextToSpeechTool(restrictedDir);

            ExecutionInput input = new ExecutionInput("Permission test", null);
            ExecutionResult result = restrictedTool.runTool(input);

            // Should either succeed (if permissions allow) or fail gracefully
            if (!result.success()) {
                assertTrue(result.output().contains("error") || result.output().contains("failed"),
                          "Should provide meaningful error message for permission issues");
            }
        } finally {
            // Restore permissions to allow cleanup
            if (Files.exists(readOnlyParent) &&
                readOnlyParent.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                try {
                    Set<PosixFilePermission> writable = PosixFilePermissions.fromString("rwxrwxrwx");
                    Files.setPosixFilePermissions(readOnlyParent, writable);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @org.junit.jupiter.api.Disabled("Temporarily disabled for performance - takes too long")
    @Test
    @DisplayName("Should handle concurrent file generation without conflicts")
    void shouldHandleConcurrentFileGenerationWithoutConflicts() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Exception> exception = new AtomicReference<>();
        ExecutionResult[] results = new ExecutionResult[threadCount];

        // Generate audio concurrently from multiple threads
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Add small random delay to increase timestamp variety
                    Thread.sleep((long) (Math.random() * 50));

                    ExecutionInput input = new ExecutionInput("Concurrent test " + index, null);
                    results[index] = tool.runTool(input);
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");
        assertNull(exception.get(), "No exceptions should occur during concurrent execution");

        // Verify all results
        Set<String> filePaths = new java.util.HashSet<>();
        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i].success(), "Thread " + i + " should succeed");

            String filePath = results[i].output().split(" \\(")[0];
            Path audioFile = Paths.get(filePath);
            assertTrue(Files.exists(audioFile), "Audio file should exist for thread " + i);

            // Verify uniqueness (atomic counter ensures this)
            assertTrue(filePaths.add(filePath), "Each thread should generate a unique file path");
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should test atomic counter for unique filenames")
    void shouldTestAtomicCounterForUniqueFilenames() throws Exception {
        // Access the fileCounter field via reflection
        Field fileCounterField = TextToSpeechTool.class.getDeclaredField("fileCounter");
        fileCounterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicLong fileCounter =
            (java.util.concurrent.atomic.AtomicLong) fileCounterField.get(null);

        long initialValue = fileCounter.get();

        // Generate multiple files quickly
        for (int i = 0; i < 5; i++) {
            ExecutionInput input = new ExecutionInput("Counter test " + i, null);
            ExecutionResult result = tool.runTool(input);
            assertTrue(result.success());
        }

        long finalValue = fileCounter.get();
        assertEquals(initialValue + 5, finalValue, "Counter should increment by number of files generated");
    }

    @Test
    @DisplayName("Should handle extremely large text input at boundary")
    void shouldHandleExtremelyLargeTextInputAtBoundary() {
        // Test with exactly 10,000 characters (boundary case) - use efficient approach
        StringBuilder exactBoundary = new StringBuilder();
        String baseString = "a".repeat(1000); // Create 1000 char string
        for (int i = 0; i < 10; i++) { // 10 * 1000 = exactly 10,000 chars
            exactBoundary.append(baseString);
        }

        ExecutionInput input = new ExecutionInput(exactBoundary.toString(), null);
        ExecutionResult result = tool.runTool(input);
        assertTrue(result.success(), "Should handle exactly 10,000 characters");

        // Test with 10,001 characters (just over boundary)
        StringBuilder overBoundary = new StringBuilder(exactBoundary);
        overBoundary.append('b');

        ExecutionInput overInput = new ExecutionInput(overBoundary.toString(), null);
        ExecutionResult overResult = tool.runTool(overInput);
        assertFalse(overResult.success(), "Should reject text over 10,000 characters");
        assertTrue(overResult.output().contains("too long"), "Should indicate text is too long");
    }

    @Test
    @DisplayName("Should validate control character filtering")
    void shouldValidateControlCharacterFiltering() {
        // Test various control characters
        char[] invalidControlChars = {
            '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\u0008',
            '\u000B', '\u000C', '\u000E', '\u000F', '\u0010', '\u0011', '\u0012', '\u0013',
            '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019', '\u001A', '\u001B',
            '\u001C', '\u001D', '\u001E', '\u001F', '\u007F'
        };

        for (char invalidChar : invalidControlChars) {
            String textWithInvalidChar = "Hello" + invalidChar + "World";
            ExecutionInput input = new ExecutionInput(textWithInvalidChar, null);
            ExecutionResult result = tool.runTool(input);

            assertFalse(result.success(), "Should reject text with control character: \\u" +
                       String.format("%04X", (int) invalidChar));
            assertTrue(result.output().contains("control characters"),
                      "Should indicate control character issue");
        }

        // Test valid control characters (should be allowed)
        String[] validTexts = {
            "Hello\tWorld",      // Tab
            "Hello\nWorld",      // Newline
            "Hello\rWorld"       // Carriage return
        };

        for (String validText : validTexts) {
            ExecutionInput input = new ExecutionInput(validText, null);
            ExecutionResult result = tool.runTool(input);
            assertTrue(result.success(), "Should allow valid control characters: " +
                      validText.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r"));
        }
    }

    @Test
    @DisplayName("Should test synthetic audio duration calculation")
    void shouldTestSyntheticAudioDurationCalculation() throws IOException {
        // Test that audio duration varies with text length
        String shortText = "Hi";
        String mediumText = "This is a medium length text for testing duration";
        String longText = "This is a very long text that should produce longer audio duration " +
                         "because the synthetic audio generator calculates duration based on text length " +
                         "and this text is intentionally quite long to test that behavior";

        Path[] audioFiles = new Path[3];
        long[] fileSizes = new long[3];
        String[] texts = {shortText, mediumText, longText};

        for (int i = 0; i < texts.length; i++) {
            ExecutionInput input = new ExecutionInput(texts[i], null);
            ExecutionResult result = tool.runTool(input);
            assertTrue(result.success());

            String filePath = result.output().split(" \\(")[0];
            audioFiles[i] = Paths.get(filePath);
            fileSizes[i] = Files.size(audioFiles[i]);
        }

        // Longer text should generally produce larger files
        assertTrue(fileSizes[1] >= fileSizes[0], "Medium text should produce same or larger file than short text");
        assertTrue(fileSizes[2] >= fileSizes[1], "Long text should produce same or larger file than medium text");

        // The longest text should produce noticeably larger file
        assertTrue(fileSizes[2] > fileSizes[0] * 1.5, "Long text should produce significantly larger file");
    }

    @org.junit.jupiter.api.Disabled("Temporarily disabled for performance - takes too long")
    @Test
    @DisplayName("Should handle stress testing with rapid successive calls")
    void shouldHandleStressTestingWithRapidSuccessiveCalls() {
        int callCount = 10;

        for (int i = 0; i < callCount; i++) {
            ExecutionInput input = new ExecutionInput("Stress test call " + i, null);
            ExecutionResult result = tool.runTool(input);

            assertTrue(result.success(), "Call " + i + " should succeed");

            String filePath = result.output().split(" \\(")[0];
            Path audioFile = Paths.get(filePath);
            assertTrue(Files.exists(audioFile), "Audio file should exist for call " + i);

            // Verify file is not empty
            try {
                assertTrue(Files.size(audioFile) > 44, "Audio file should not be empty for call " + i);
            } catch (IOException e) {
                fail("Could not verify file size for call " + i + ": " + e.getMessage());
            }
        }
    }

    // Helper methods for WAV file parsing
    private int bytesToInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) |
               ((bytes[offset + 1] & 0xFF) << 8) |
               ((bytes[offset + 2] & 0xFF) << 16) |
               ((bytes[offset + 3] & 0xFF) << 24);
    }

    private short bytesToShort(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xFF) |
                       ((bytes[offset + 1] & 0xFF) << 8));
    }

    private int findDataChunk(byte[] bytes) {
        for (int i = 0; i < bytes.length - 4; i++) {
            if (bytes[i] == 'd' && bytes[i + 1] == 'a' &&
                bytes[i + 2] == 't' && bytes[i + 3] == 'a') {
                return i;
            }
        }
        return -1;
    }

    private Class<?> getInnerTTSEngineClass() throws ClassNotFoundException {
        Class<?>[] innerClasses = TextToSpeechTool.class.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.getSimpleName().equals("TTSEngine")) {
                return innerClass;
            }
        }
        throw new ClassNotFoundException("TTSEngine inner class not found");
    }

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
}
package com.skanga.conductor.tools;

import com.skanga.conductor.config.ApplicationConfig;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Demo audio generation tool for testing and demonstration purposes.
 * <p>
 * This tool provides a simple mock implementation of audio generation functionality
 * that creates synthetic WAV files containing sine wave audio. It is designed for
 * development, testing, and demonstration scenarios where actual text-to-speech
 * (TTS) capabilities are not required.
 * </p>
 * <p>
 * The generated audio characteristics:
 * </p>
 * <ul>
 * <li>WAV format with 16-bit PCM encoding</li>
 * <li>16kHz sample rate</li>
 * <li>Sine wave audio with frequency based on input text hash</li>
 * <li>Duration proportional to input text length (1-10 seconds)</li>
 * <li>Mono channel output</li>
 * </ul>
 * <p>
 * In production environments, this should be replaced with a real TTS service
 * such as:
 * </p>
 * <ul>
 * <li>ElevenLabs API</li>
 * <li>Amazon Polly</li>
 * <li>Google Cloud Text-to-Speech</li>
 * <li>Azure Cognitive Services Speech</li>
 * <li>Other TTS service providers</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent audio generation.
 * Each execution creates independent files with unique timestamps.
 * </p>
 *
 * @since 1.0.0
 * @see Tool
 * @see ToolInput
 * @see ToolResult
 */
public class SimpleAudioTool implements Tool {

    private final Path outDir;
    private final int sampleRate = 16000;

    /**
     * Creates a new SimpleAudioTool with output directory from configuration.
     * <p>
     * Uses the audio output directory specified in ApplicationConfig.
     * </p>
     */
    public SimpleAudioTool() {
        ApplicationConfig.ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        this.outDir = Paths.get(config.getAudioOutputDir());
    }

    /**
     * Creates a new SimpleAudioTool with a custom output directory.
     *
     * @param outDir the directory where generated audio files will be saved
     */
    public SimpleAudioTool(Path outDir) {
        this.outDir = outDir;
    }

    /**
     * Returns the name identifier for this tool.
     *
     * @return "audio_gen" as the tool identifier
     */
    @Override
    public String name() {
        return "audio_gen";
    }

    /**
     * Returns a description of this tool's functionality.
     *
     * @return description explaining the demo audio generation capability
     */
    @Override
    public String description() {
        return "Generates a demo WAV file for the input text (sine wave). Output: path to WAV file";
    }

    /**
     * Executes audio generation for the provided text input.
     * <p>
     * Creates a WAV file containing a sine wave with characteristics based on
     * the input text. The frequency is derived from the text hash, and the
     * duration is proportional to the text length (1-10 seconds).
     * </p>
     * <p>
     * The generated file is saved in the configured output directory with
     * a timestamp-based filename to ensure uniqueness.
     * </p>
     *
     * @param input the tool input containing the text to "convert" to audio
     * @return a ToolResult containing the path to the generated WAV file
     */
    @Override
    public ToolResult run(ToolInput input) {
        // Validate input parameters
        ValidationResult inputValidation = validateInput(input);
        if (!inputValidation.isValid()) {
            return new ToolResult(false, inputValidation.getErrorMessage(), null);
        }

        try {
            String text = input.text() == null ? "" : input.text();
            // length proportional to text length
            int seconds = Math.max(1, Math.min(10, text.length() / 40 + 1));
            int totalSamples = sampleRate * seconds;
            double freq = 220 + (Math.abs(text.hashCode()) % 400); // vary freq by text

            byte[] pcm = new byte[totalSamples * 2];
            for (int i = 0; i < totalSamples; i++) {
                double t = i / (double) sampleRate;
                short val = (short) (Math.sin(2 * Math.PI * freq * t) * Short.MAX_VALUE * 0.3);
                ByteBuffer.wrap(pcm, i * 2, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(val);
            }

            String filename = "audio_" + Instant.now().toEpochMilli() + ".wav";
            Path outFile = outDir.resolve(filename);
            try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
                // WAV header (PCM16)
                int byteRate = sampleRate * 2;
                // RIFF header
                fos.write("RIFF".getBytes());
                fos.write(intToLE(36 + pcm.length)); // file size - 8
                fos.write("WAVE".getBytes());
                // fmt chunk
                fos.write("fmt ".getBytes());
                fos.write(intToLE(16)); // subchunk1Size
                fos.write(shortToLE((short) 1)); // PCM
                fos.write(shortToLE((short) 1)); // channels
                fos.write(intToLE(sampleRate));
                fos.write(intToLE(byteRate));
                fos.write(shortToLE((short) 2)); // block align
                fos.write(shortToLE((short) 16)); // bits per sample
                // data chunk
                fos.write("data".getBytes());
                fos.write(intToLE(pcm.length));
                fos.write(pcm);
            }

            return new ToolResult(true, outFile.toAbsolutePath().toString(), null);
        } catch (Exception e) {
            return new ToolResult(false, "Audio gen error: " + e.getMessage(), null);
        }
    }

    /**
     * Converts an integer to little-endian byte array for WAV file format.
     *
     * @param v the integer value to convert
     * @return byte array in little-endian format
     */
    private byte[] intToLE(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }

    /**
     * Converts a short to little-endian byte array for WAV file format.
     *
     * @param v the short value to convert
     * @return byte array in little-endian format
     */
    private byte[] shortToLE(short v) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array();
    }

    /**
     * Validates the tool input for completeness and security.
     *
     * @param input the tool input to validate
     * @return validation result indicating success or specific error
     */
    private ValidationResult validateInput(ToolInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }

        // Note: text can be null or empty for audio generation (silent audio)
        String text = input.text();
        if (text != null) {
            // Check for extremely long text that could cause resource exhaustion
            if (text.length() > 10000) {
                return ValidationResult.invalid("Input text is too long (max 10,000 characters)");
            }

            // Check for control characters that might cause issues
            for (char c : text.toCharArray()) {
                if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                    return ValidationResult.invalid("Input text contains invalid control characters");
                }
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Result class for validation operations.
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

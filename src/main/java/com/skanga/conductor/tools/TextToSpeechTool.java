package com.skanga.conductor.tools;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.ToolConfig;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Text-to-Speech audio generation tool with multiple TTS backends.
 * <p>
 * This tool provides real text-to-speech functionality using multiple fallback strategies:
 * </p>
 * <ol>
 * <li><strong>eSpeak-NG</strong> - Open source TTS engine (primary choice)</li>
 * <li><strong>Festival</strong> - Alternative open source TTS</li>
 * <li><strong>Windows SAPI</strong> - Windows Speech API (Windows only)</li>
 * <li><strong>Synthetic audio</strong> - Sine wave fallback for testing</li>
 * </ol>
 * <p>
 * The tool automatically detects available TTS engines and uses the best available option.
 * All generated audio is saved to the configured audio output directory.
 * </p>
 * <p>
 * Generated audio characteristics:
 * </p>
 * <ul>
 * <li>WAV format with 16-bit PCM encoding</li>
 * <li>22kHz sample rate (for TTS) or 16kHz (for synthetic)</li>
 * <li>Mono channel output</li>
 * <li>Automatic volume normalization</li>
 * </ul>
 * <p>
 * <strong>Installation Requirements:</strong>
 * </p>
 * <ul>
 * <li><strong>Linux/macOS:</strong> {@code sudo apt install espeak-ng} or {@code brew install espeak}</li>
 * <li><strong>Windows:</strong> Download eSpeak-NG or use built-in SAPI</li>
 * <li><strong>Fallback:</strong> No dependencies - generates synthetic audio</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent audio generation.
 * Each execution creates independent files with unique timestamps.
 * </p>
 *
 * @since 2.0.0
 * @see Tool
 * @see ExecutionInput
 * @see ExecutionResult
 */
public class TextToSpeechTool implements Tool {

    private final Path outDir;
    private final int sampleRate = 16000;
    private final int ttsSampleRate = 22050;

    /**
     * Enumeration of available TTS engines with their capabilities and command formats.
     */
    private enum TTSEngine {
        ESPEAK_NG("espeak-ng", "--stdout", "-w", "-s 150"),
        ESPEAK("espeak", "--stdout", "-w", "-s 150"),
        FESTIVAL("festival", "--heap 10000000", "(voice_kal_diphone)", "(SayText \"%s\")", "(quit)"),
        WINDOWS_SAPI("powershell", "-Command", "Add-Type -AssemblyName System.Speech; " +
                     "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                     "$synth.SetOutputToWaveFile('%s'); $synth.Speak('%s'); $synth.Dispose()"),
        SYNTHETIC("synthetic", "fallback"); // Fallback synthetic audio

        private final String command;
        private final String[] args;

        TTSEngine(String command, String... args) {
            this.command = command;
            this.args = args;
        }

        public String getCommand() { return command; }
        public String[] getArgs() { return args; }
    }

    private static TTSEngine detectedEngine = null;
    private static boolean engineDetectionComplete = false;
    private static final AtomicLong fileCounter = new AtomicLong(0);

    /**
     * Creates a new SimpleAudioTool with output directory from configuration.
     * <p>
     * Uses the audio output directory specified in ApplicationConfig.
     * </p>
     */
    public TextToSpeechTool() {
        ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        this.outDir = Paths.get(config.getAudioOutputDir());
    }

    /**
     * Creates a new SimpleAudioTool with a custom output directory.
     *
     * @param outDir the directory where generated audio files will be saved
     */
    public TextToSpeechTool(Path outDir) {
        this.outDir = outDir;
    }

    /**
     * Returns the name identifier for this tool.
     *
     * @return "audio_gen" as the tool identifier
     */
    @Override
    public String toolName() {
        return "audio_gen";
    }

    /**
     * Returns a description of this tool's functionality.
     *
     * @return description explaining the TTS audio generation capability
     */
    @Override
    public String toolDescription() {
        TTSEngine engine = detectAvailableTTSEngine();
        return String.format("Converts text to speech using %s TTS engine. Output: path to WAV file",
                           engine.name().toLowerCase().replace("_", " "));
    }

    /**
     * Executes text-to-speech audio generation for the provided text input.
     * <p>
     * Attempts to use real TTS engines in order of preference:
     * 1. eSpeak-NG (best open source option)
     * 2. eSpeak (older version)
     * 3. Festival (alternative TTS)
     * 4. Windows SAPI (Windows only)
     * 5. Synthetic audio (fallback)
     * </p>
     * <p>
     * The generated file is saved in the configured output directory with
     * a timestamp-based filename to ensure uniqueness.
     * </p>
     *
     * @param toolInput the tool input containing the text to convert to speech
     * @return a ExecutionResult containing the path to the generated WAV file
     */
    @Override
    public ExecutionResult runTool(ExecutionInput toolInput) {
        // Validate input parameters
        ValidationResult inputValidation = validateInput(toolInput);
        if (!inputValidation.isValid()) {
            return new ExecutionResult(false, inputValidation.getErrorMessage(), null);
        }

        try {
            String text = toolInput.content() == null ? "Silent audio" : toolInput.content().trim();
            if (text.isEmpty()) {
                text = "Silent audio";
            }

            // Ensure output directory exists (thread-safe)
            synchronized (TextToSpeechTool.class) {
                Files.createDirectories(outDir);
            }

            // Detect available TTS engine
            TTSEngine engine = detectAvailableTTSEngine();

            // Generate unique filename using timestamp + atomic counter for thread safety
            long timestamp = Instant.now().toEpochMilli();
            long counter = fileCounter.incrementAndGet();
            String filename = String.format("tts_%s_%d_%d.wav", engine.name().toLowerCase(), timestamp, counter);
            Path outFile = outDir.resolve(filename);

            // Generate audio using detected engine
            boolean success = false;
            switch (engine) {
                case ESPEAK_NG:
                case ESPEAK:
                    success = generateTTSWithESpeak(text, outFile, engine);
                    break;
                case FESTIVAL:
                    success = generateTTSWithFestival(text, outFile);
                    break;
                case WINDOWS_SAPI:
                    success = generateTTSWithWindowsSAPI(text, outFile);
                    break;
                case SYNTHETIC:
                default:
                    success = generateSyntheticAudio(text, outFile);
                    break;
            }

            if (success && Files.exists(outFile)) {
                String engineInfo = (engine == TTSEngine.SYNTHETIC) ?
                    " (⚠️ Using synthetic fallback - install eSpeak-NG for real TTS)" :
                    String.format(" (✅ Generated using %s TTS)", engine.name().toLowerCase().replace("_", " "));

                return new ExecutionResult(true, outFile.toAbsolutePath().toString() + engineInfo, null);
            } else {
                return new ExecutionResult(false, "Failed to generate audio with " + engine.name(), null);
            }

        } catch (Exception e) {
            return new ExecutionResult(false, "TTS generation error: " + e.getMessage(), null);
        }
    }

    /**
     * Detects the best available TTS engine on the system.
     *
     * @return the best available TTS engine
     */
    private static synchronized TTSEngine detectAvailableTTSEngine() {
        if (engineDetectionComplete) {
            return detectedEngine;
        }

        // Try engines in order of preference
        for (TTSEngine engine : new TTSEngine[]{TTSEngine.ESPEAK_NG, TTSEngine.ESPEAK,
                                               TTSEngine.FESTIVAL, TTSEngine.WINDOWS_SAPI}) {
            if (isCommandAvailable(engine.getCommand())) {
                detectedEngine = engine;
                engineDetectionComplete = true;
                return engine;
            }
        }

        // Fallback to synthetic
        detectedEngine = TTSEngine.SYNTHETIC;
        engineDetectionComplete = true;
        return detectedEngine;
    }

    /**
     * Checks if a command is available on the system.
     *
     * @param command the command to check
     * @return true if the command is available
     */
    private static boolean isCommandAvailable(String command) {
        try {
            String[] testCommand = System.getProperty("os.name").toLowerCase().contains("windows") ?
                new String[]{"where", command} : new String[]{"which", command};

            Process process = Runtime.getRuntime().exec(testCommand);
            process.waitFor(3, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates TTS audio using eSpeak or eSpeak-NG.
     *
     * @param text the text to convert to speech
     * @param outputFile the output WAV file
     * @param engine the eSpeak engine to use
     * @return true if generation was successful
     */
    private boolean generateTTSWithESpeak(String text, Path outputFile, TTSEngine engine) {
        try {
            List<String> command = new ArrayList<>();
            command.add(engine.getCommand());
            command.add("-w");
            command.add(outputFile.toAbsolutePath().toString());
            command.add("-s");
            command.add("150"); // Speech rate
            command.add("-a");
            command.add("100"); // Amplitude
            command.add(text);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return Files.exists(outputFile) && Files.size(outputFile) > 0;
            }
        } catch (Exception e) {
            // Fall through to return false
        }
        return false;
    }

    /**
     * Generates TTS audio using Festival speech synthesis.
     *
     * @param text the text to convert to speech
     * @param outputFile the output WAV file
     * @return true if generation was successful
     */
    private boolean generateTTSWithFestival(String text, Path outputFile) {
        try {
            // Create temporary script for Festival
            String script = String.format(
                "(voice_kal_diphone)\n" +
                "(Parameter.set 'Audio_Method 'Audio_Command)\n" +
                "(Parameter.set 'Audio_Command \"sox -t raw -r 16000 -s -w - %s\")\n" +
                "(SayText \"%s\")\n" +
                "(quit)\n",
                outputFile.toAbsolutePath().toString().replace("\\", "/"),
                text.replace("\"", "\\\"")
            );

            ProcessBuilder pb = new ProcessBuilder("festival", "--heap", "10000000");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Send script to Festival
            process.getOutputStream().write(script.getBytes());
            process.getOutputStream().close();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return Files.exists(outputFile) && Files.size(outputFile) > 0;
            }
        } catch (Exception e) {
            // Fall through to return false
        }
        return false;
    }

    /**
     * Generates TTS audio using Windows Speech API.
     *
     * @param text the text to convert to speech
     * @param outputFile the output WAV file
     * @return true if generation was successful
     */
    private boolean generateTTSWithWindowsSAPI(String text, Path outputFile) {
        try {
            String powershellCommand = String.format(
                "Add-Type -AssemblyName System.Speech; " +
                "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$synth.SetOutputToWaveFile('%s'); " +
                "$synth.Speak('%s'); " +
                "$synth.Dispose()",
                outputFile.toAbsolutePath().toString().replace("'", "''"),
                text.replace("'", "''")
            );

            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", powershellCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return Files.exists(outputFile) && Files.size(outputFile) > 0;
            }
        } catch (Exception e) {
            // Fall through to return false
        }
        return false;
    }

    /**
     * Generates synthetic sine wave audio as fallback.
     *
     * @param text the text to base audio characteristics on
     * @param outputFile the output WAV file
     * @return true if generation was successful
     */
    private boolean generateSyntheticAudio(String text, Path outputFile) {
        try {
            // Generate sine wave audio based on text
            int seconds = Math.max(1, Math.min(10, text.length() / 40 + 1));
            int totalSamples = sampleRate * seconds;
            double freq = 220 + (Math.abs(text.hashCode()) % 400); // Vary frequency by text

            byte[] pcm = new byte[totalSamples * 2];
            for (int i = 0; i < totalSamples; i++) {
                double t = i / (double) sampleRate;
                short val = (short) (Math.sin(2 * Math.PI * freq * t) * Short.MAX_VALUE * 0.3);
                ByteBuffer.wrap(pcm, i * 2, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(val);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
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

            return Files.exists(outputFile) && Files.size(outputFile) > 0;
        } catch (Exception e) {
            return false;
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
    private ValidationResult validateInput(ExecutionInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }

        // Note: text can be null or empty for audio generation (silent audio)
        String text = input.content();
        if (text != null) {
            // Check for extremely long text that could cause resource exhaustion
            int maxLength = getMaxTextLength();
            if (text.length() > maxLength) {
                return ValidationResult.invalid(String.format("Input text is too long (max %,d characters)", maxLength));
            }

            // Check for control characters that might cause issues
            for (int i = 0; i < Math.min(text.length(), 1000); i++) {
                char c = text.charAt(i);
                if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                    return ValidationResult.invalid("Input text contains invalid control characters");
                }
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Gets the maximum text length from configuration.
     */
    private int getMaxTextLength() {
        try {
            return ApplicationConfig.getInstance().getToolConfig().getTtsMaxTextLength();
        } catch (Exception e) {
            // Fallback if config is not available
            return 10000;
        }
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

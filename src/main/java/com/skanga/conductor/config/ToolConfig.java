package com.skanga.conductor.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;

/**
 * Tool configuration settings.
 * <p>
 * Provides access to tool-specific configuration like code runner,
 * file read, and audio generation settings.
 * </p>
 *
 * @since 1.1.0
 */
public class ToolConfig extends ConfigurationProvider {

    public ToolConfig(Properties properties) {
        super(properties);
    }

    @NotNull(message = "Code runner timeout cannot be null")
    public Duration getCodeRunnerTimeout() {
        Duration timeout = getDuration("conductor.tools.coderunner.timeout", Duration.ofSeconds(5));
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Code runner timeout must be positive");
        }
        if (timeout.toSeconds() > 3600) {
            throw new IllegalArgumentException("Code runner timeout cannot exceed 1 hour");
        }
        return timeout;
    }

    public Set<String> getCodeRunnerAllowedCommands() {
        return getStringSet("conductor.tools.coderunner.allowed.commands",
            Set.of("echo", "ls", "pwd", "date", "whoami"));
    }

    public String getFileReadBaseDir() {
        return getString("conductor.tools.fileread.basedir", "./sample_data");
    }

    public boolean getFileReadAllowSymlinks() {
        return getBoolean("conductor.tools.fileread.allow.symlinks", false);
    }

    @Min(value = 1024, message = "File read max size must be at least 1KB")
    @Max(value = 104857600, message = "File read max size cannot exceed 100MB")
    public long getFileReadMaxSize() {
        return getLong("conductor.tools.fileread.max.size.bytes", 10 * 1024 * 1024); // 10MB
    }

    @Min(value = 1, message = "Max path length must be at least 1")
    @Max(value = 32767, message = "Max path length cannot exceed 32767")
    public int getFileReadMaxPathLength() {
        return getInt("conductor.tools.fileread.max.path.length", 260);
    }

    public String getAudioOutputDir() {
        return getString("conductor.tools.audio.output.dir", "./out_audio");
    }

    /**
     * Returns the maximum text length for text-to-speech conversion.
     * <p>
     * This prevents resource exhaustion from extremely long text inputs.
     * </p>
     *
     * @return maximum text length in characters
     */
    @Min(value = 100, message = "TTS max text length must be at least 100")
    @Max(value = 100000, message = "TTS max text length cannot exceed 100,000")
    public int getTtsMaxTextLength() {
        return getInt("conductor.tools.tts.max.text.length", 10000);
    }

    /**
     * Returns the GC sleep delay for memory manager cleanup.
     * <p>
     * Time to wait after requesting garbage collection.
     * </p>
     *
     * @return GC sleep delay in milliseconds
     */
    @Min(value = 0, message = "GC sleep delay cannot be negative")
    @Max(value = 10000, message = "GC sleep delay cannot exceed 10 seconds")
    public long getMemoryGcSleepDelay() {
        return getLong("conductor.memory.gc.sleep.delay", 100);
    }
}

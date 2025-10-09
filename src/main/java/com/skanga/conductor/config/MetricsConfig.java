package com.skanga.conductor.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;

/**
 * Metrics configuration settings.
 * <p>
 * Provides access to metrics collection and reporting configuration.
 * </p>
 *
 * @since 1.1.0
 */
public class MetricsConfig extends ConfigurationProvider {

    public MetricsConfig(Properties properties) {
        super(properties);
    }

    public boolean isEnabled() {
        return getBoolean("conductor.metrics.enabled", true);
    }

    @NotNull(message = "Metrics retention period cannot be null")
    public Duration getRetentionPeriod() {
        Duration period = getDuration("conductor.metrics.retention.period", Duration.ofHours(24));
        if (period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("Metrics retention period must be positive");
        }
        return period;
    }

    @Min(value = 100, message = "Max metrics in memory must be at least 100")
    @Max(value = 10000000, message = "Max metrics in memory cannot exceed 10000000")
    public int getMaxMetricsInMemory() {
        return getInt("conductor.metrics.max.in.memory", 100000);
    }

    public boolean isConsoleReportingEnabled() {
        return getBoolean("conductor.metrics.console.enabled", false);
    }

    @NotNull(message = "Console reporting interval cannot be null")
    public Duration getConsoleReportingInterval() {
        Duration interval = getDuration("conductor.metrics.console.interval", Duration.ofMinutes(5));
        if (isConsoleReportingEnabled() && (interval.isNegative() || interval.isZero())) {
            throw new IllegalArgumentException("Console reporting interval must be positive when enabled");
        }
        return interval;
    }

    @NotBlank(message = "Metrics output directory cannot be empty")
    public String getOutputDirectory() {
        return getString("conductor.metrics.output.dir", "./logs/metrics");
    }

    public boolean isFileReportingEnabled() {
        return getBoolean("conductor.metrics.file.enabled", false);
    }

    @NotNull(message = "File reporting interval cannot be null")
    public Duration getFileReportingInterval() {
        Duration interval = getDuration("conductor.metrics.file.interval", Duration.ofMinutes(15));
        if (isFileReportingEnabled() && (interval.isNegative() || interval.isZero())) {
            throw new IllegalArgumentException("File reporting interval must be positive when enabled");
        }
        return interval;
    }

    public Set<String> getEnabledMetrics() {
        return getStringSet("conductor.metrics.enabled.patterns",
            Set.of("agent.*", "tool.*", "orchestrator.*"));
    }

    public Set<String> getDisabledMetrics() {
        return getStringSet("conductor.metrics.disabled.patterns", Set.of());
    }
}

package com.skanga.conductor.config;

import com.skanga.conductor.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line tool for managing and validating configuration properties.
 * <p>
 * This utility provides commands for validating configuration files and checking
 * system status. Sensitive properties should be provided via environment variables
 * rather than stored in configuration files.
 * </p>
 * <p>
 * Available commands:
 * </p>
 * <ul>
 * <li>validate: Validate configuration file</li>
 * <li>status: Show configuration status</li>
 * <li>help: Show help message</li>
 * </ul>
 * <p>
 * Usage examples:
 * </p>
 * <pre>
 * java -cp conductor.jar com.skanga.conductor.config.ConfigurationTool validate
 * java -cp conductor.jar com.skanga.conductor.config.ConfigurationTool status
 * </pre>
 * <p>
 * <strong>Security Note:</strong> For sensitive properties like API keys, use environment
 * variables rather than storing them in configuration files. The tool will automatically
 * check for environment variables following the convention PROPERTY_KEY → PROPERTY_KEY.
 * </p>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 */
public class ConfigurationTool {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTool.class);

    private static final String USAGE = """
        Conductor Configuration Tool
        ============================

        Commands:
          validate        Validate configuration
          status          Show configuration status
          help            Show this help message

        Sensitive Properties:
          Use environment variables for sensitive values like API keys.
          Example: CONDUCTOR_LLM_OPENAI_API_KEY for conductor.llm.openai.api.key

        Examples:
          java ConfigurationTool validate
          java ConfigurationTool status
        """;

    /**
     * Main entry point for the configuration tool.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(1);
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "validate" -> validateConfiguration();
                case "status" -> showStatus();
                case "help", "-h", "--help" -> System.out.println(USAGE);
                default -> {
                    System.err.println("Unknown command: " + command);
                    System.out.println(USAGE);
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            logger.error("Configuration tool command failed: {}", command, e);
            System.err.println("Error: " + e.getMessage());
            if (isVerbose()) {
                System.err.println("Full error details:");
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Validates the current configuration.
     */
    private static void validateConfiguration() {
        System.out.println("Configuration Validation");
        System.out.println("========================");

        try {
            // Test loading configuration
            ApplicationConfig config = ApplicationConfig.getInstance();
            System.out.println("✓ Configuration loaded successfully");

            // Check for sensitive properties and their environment variables
            checkSensitiveProperties(config);

            // Validate using the validator if available
            try {
                String validationSummary = ConfigurationValidator.validateAndSummarize();
                System.out.println(validationSummary);
            } catch (Exception e) {
                logger.warn("Configuration validator not available: {}", e.getMessage());
                System.out.println("⚠ Advanced validation not available");
            }

            System.out.println("\n✓ Configuration validation completed");

        } catch (ConfigurationException e) {
            System.err.println("❌ Configuration validation failed:");
            System.err.println("  " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error during configuration validation", e);
            System.err.println("❌ Unexpected error during validation:");
            System.err.println("  " + e.getMessage());
            if (isVerbose()) {
                System.err.println("Full error details:");
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Shows the current configuration status.
     */
    private static void showStatus() {
        System.out.println("Configuration Status");
        System.out.println("===================");

        try {
            ApplicationConfig config = ApplicationConfig.getInstance();
            System.out.println("✓ Configuration: Loaded successfully");

            // Check sensitive properties
            checkSensitiveProperties(config);

        } catch (Exception e) {
            System.out.println("❌ Configuration: Failed to load (" + e.getMessage() + ")");
        }

        // Environment info
        System.out.println("\nEnvironment Information:");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
    }

    /**
     * Checks sensitive properties and their environment variable availability.
     */
    private static void checkSensitiveProperties(ApplicationConfig config) {
        System.out.println("\nSensitive Property Status:");

        String[] sensitiveKeys = {
            "conductor.llm.openai.api.key",
            "conductor.llm.anthropic.api.key",
            "conductor.llm.google.api.key",
            "conductor.database.password"
        };

        for (String key : sensitiveKeys) {
            String envKey = key.toUpperCase().replace('.', '_');
            String envValue = System.getenv(envKey);
            String propValue = config.getString(key, null);

            if (envValue != null && !envValue.trim().isEmpty()) {
                System.out.println("✓ " + key + " → Available via " + envKey + " (environment variable)");
            } else if (propValue != null && !propValue.trim().isEmpty()) {
                System.out.println("⚠ " + key + " → Available in properties file (consider using " + envKey + " environment variable)");
            } else {
                System.out.println("- " + key + " → Not configured (set " + envKey + " environment variable if needed)");
            }
        }
    }

    /**
     * Checks if verbose output is requested.
     */
    private static boolean isVerbose() {
        return "true".equalsIgnoreCase(System.getProperty("verbose")) ||
               "true".equalsIgnoreCase(System.getenv("VERBOSE"));
    }
}

package com.skanga.conductor.config;

import java.io.Console;
import java.util.Scanner;

/**
 * Command-line tool for managing encrypted configuration properties.
 * <p>
 * This utility provides commands for encrypting sensitive configuration values,
 * generating encryption keys, and validating configuration files. It's designed
 * to be used during deployment and configuration management processes.
 * </p>
 * <p>
 * Available commands:
 * </p>
 * <ul>
 * <li>generate-key: Generate a new encryption key</li>
 * <li>encrypt: Encrypt a property value</li>
 * <li>decrypt: Decrypt a property value (for testing)</li>
 * <li>validate: Validate configuration file</li>
 * <li>status: Show encryption status</li>
 * </ul>
 * <p>
 * Usage examples:
 * </p>
 * <pre>
 * java -cp conductor.jar config.conductor.com.skanga.conductor.ConfigurationTool generate-key
 * java -cp conductor.jar config.conductor.com.skanga.conductor.ConfigurationTool encrypt
 * java -cp conductor.jar config.conductor.com.skanga.conductor.ConfigurationTool validate
 * </pre>
 *
 * @since 1.0.0
 * @see PropertyEncryptor
 * @see SecureApplicationConfig
 */
public class ConfigurationTool {

    private static final String USAGE = """
        Conductor Configuration Tool
        ============================

        Commands:
          generate-key    Generate a new encryption key
          encrypt         Encrypt a property value
          decrypt         Decrypt a property value (for testing)
          validate        Validate configuration
          status          Show encryption status
          help            Show this help message

        Environment Variables:
          CONDUCTOR_ENCRYPTION_KEY    Base64-encoded encryption key (required for encrypt/decrypt)

        Examples:
          java ConfigurationTool generate-key
          java ConfigurationTool encrypt
          java ConfigurationTool validate
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
                case "generate-key" -> generateKey();
                case "encrypt" -> encryptValue();
                case "decrypt" -> decryptValue();
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
            System.err.println("Error: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Generates a new encryption key and displays it.
     */
    private static void generateKey() {
        System.out.println("Generating new encryption key...");
        String key = PropertyEncryptor.generateKeyBase64();

        System.out.println("\nGenerated encryption key:");
        System.out.println("========================");
        System.out.println(key);
        System.out.println("\nTo use this key, set the environment variable:");
        System.out.println("export CONDUCTOR_ENCRYPTION_KEY=\"" + key + "\"");
        System.out.println("\nOr on Windows:");
        System.out.println("set CONDUCTOR_ENCRYPTION_KEY=" + key);
        System.out.println("\nIMPORTANT: Store this key securely and do not lose it!");
        System.out.println("Without this key, encrypted properties cannot be decrypted.");
    }

    /**
     * Encrypts a property value entered by the user.
     */
    private static void encryptValue() {
        // Check if encryption key is available
        String keyEnv = System.getenv("CONDUCTOR_ENCRYPTION_KEY");
        if (keyEnv == null || keyEnv.trim().isEmpty()) {
            System.err.println("Error: CONDUCTOR_ENCRYPTION_KEY environment variable not set");
            System.err.println("Generate a key first with: java ConfigurationTool generate-key");
            System.exit(1);
        }

        PropertyEncryptor encryptor = PropertyEncryptor.fromEnvironment();

        System.out.println("Property Encryption");
        System.out.println("==================");

        String value = readSensitiveInput("Enter the value to encrypt: ");
        if (value == null || value.trim().isEmpty()) {
            System.err.println("Error: Value cannot be empty");
            System.exit(1);
        }

        String encrypted = encryptor.encrypt(value.trim());
        String configValue = "ENC(" + encrypted + ")";

        System.out.println("\nEncrypted value:");
        System.out.println("===============");
        System.out.println(configValue);
        System.out.println("\nYou can now use this in your configuration file:");
        System.out.println("conductor.some.property=" + configValue);
    }

    /**
     * Decrypts a property value for testing purposes.
     */
    private static void decryptValue() {
        // Check if encryption key is available
        String keyEnv = System.getenv("CONDUCTOR_ENCRYPTION_KEY");
        if (keyEnv == null || keyEnv.trim().isEmpty()) {
            System.err.println("Error: CONDUCTOR_ENCRYPTION_KEY environment variable not set");
            System.exit(1);
        }

        PropertyEncryptor encryptor = PropertyEncryptor.fromEnvironment();

        System.out.println("Property Decryption (Testing)");
        System.out.println("=============================");
        System.out.print("Enter the encrypted value (with or without ENC(...)): ");

        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            System.err.println("Error: Encrypted value cannot be empty");
            System.exit(1);
        }

        // Handle both ENC(...) format and raw encrypted value
        String encryptedValue = input;
        if (input.startsWith("ENC(") && input.endsWith(")")) {
            encryptedValue = input.substring(4, input.length() - 1);
        }

        try {
            String decrypted = encryptor.decrypt(encryptedValue);
            System.out.println("\nDecrypted value:");
            System.out.println("===============");
            System.out.println(decrypted);
        } catch (ConfigurationException e) {
            System.err.println("Error: Failed to decrypt value - " + e.getMessage());
            System.err.println("Make sure you're using the correct encryption key");
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
            SecureApplicationConfig config = SecureApplicationConfig.getInstance();

            System.out.println("✓ Configuration loaded successfully");

            // Show encryption status
            System.out.println("\n" + config.getEncryptionStatus());

            // Validate using the validator
            String validationSummary = ConfigurationValidator.validateAndSummarize();
            System.out.println(validationSummary);

        } catch (ConfigurationException e) {
            System.err.println("✗ Configuration validation failed:");
            System.err.println("  " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("✗ Unexpected error during validation:");
            System.err.println("  " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Shows the current encryption and configuration status.
     */
    private static void showStatus() {
        System.out.println("Configuration Status");
        System.out.println("===================");

        // Check encryption key
        String keyEnv = System.getenv("CONDUCTOR_ENCRYPTION_KEY");
        if (keyEnv != null && !keyEnv.trim().isEmpty()) {
            System.out.println("✓ Encryption key: Available");
            try {
                PropertyEncryptor encryptor = PropertyEncryptor.fromEnvironment();
                System.out.println("✓ Encryption: " + encryptor.getAlgorithm());
            } catch (Exception e) {
                System.out.println("✗ Encryption key: Invalid (" + e.getMessage() + ")");
            }
        } else {
            System.out.println("⚠ Encryption key: Not set (CONDUCTOR_ENCRYPTION_KEY)");
        }

        // Check configuration loading
        try {
            SecureApplicationConfig config = SecureApplicationConfig.getInstance();
            System.out.println("✓ Configuration: Loaded successfully");

            // Show detailed status
            System.out.println("\n" + config.getEncryptionStatus());

        } catch (Exception e) {
            System.out.println("✗ Configuration: Failed to load (" + e.getMessage() + ")");
        }

        // Environment info
        System.out.println("\nEnvironment Information:");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
    }

    /**
     * Reads sensitive input from the user, using Console if available for hidden input.
     */
    private static String readSensitiveInput(String prompt) {
        Console console = System.console();
        if (console != null) {
            // Use Console for hidden input
            char[] password = console.readPassword(prompt);
            return password != null ? new String(password) : null;
        } else {
            // Fall back to regular input (not hidden)
            System.out.print(prompt + "(WARNING: Input will be visible) ");
            Scanner scanner = new Scanner(System.in);
            return scanner.nextLine();
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
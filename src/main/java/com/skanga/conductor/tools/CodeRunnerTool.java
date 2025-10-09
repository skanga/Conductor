package com.skanga.conductor.tools;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.ToolConfig;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Secure command execution tool with injection attack prevention.
 * <p>
 * This tool provides safe command execution capabilities with comprehensive
 * security measures to prevent command injection attacks and resource abuse.
 * Key security features include:
 * </p>
 * <ul>
 * <li>Command whitelist support for restricted execution environments</li>
 * <li>Proper argument parsing to prevent shell injection</li>
 * <li>Execution timeout limits to prevent resource exhaustion</li>
 * <li>Direct process execution (bypasses shell interpretation)</li>
 * <li>Quoted string support for arguments with spaces</li>
 * </ul>
 * <p>
 * The tool parses command strings safely by splitting them into individual
 * arguments without shell interpretation, preventing common injection vectors.
 * Commands are executed directly via ProcessBuilder rather than through a shell.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent command execution.
 * Each execution creates its own process and doesn't share mutable state.
 * </p>
 *
 * @since 1.0.0
 * @see Tool
 * @see ExecutionInput
 * @see ExecutionResult
 */
public class CodeRunnerTool implements Tool {

    private final Duration timeout;
    private final Set<String> allowedCommands;

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "\"([^\"]*)\"|'([^']*)'|(\\S+)"
    );

    /**
     * Creates a new CodeRunnerTool with configuration from ApplicationConfig.
     */
    public CodeRunnerTool() {
        ToolConfig config = ApplicationConfig.getInstance().getToolConfig();
        this.timeout = config.getCodeRunnerTimeout();
        this.allowedCommands = config.getCodeRunnerAllowedCommands();
    }

    public CodeRunnerTool(Duration timeout) {
        this(timeout, Set.of());
    }

    public CodeRunnerTool(Duration timeout, Set<String> allowedCommands) {
        this.timeout = timeout;
        this.allowedCommands = allowedCommands != null ? allowedCommands : Set.of();
    }

    @Override
    public String toolName() {
        return "code_runner";
    }

    @Override
    public String toolDescription() {
        return "Run a shell command safely. Input: command with arguments (supports quoted strings)";
    }

    @Override
    public ExecutionResult runTool(ExecutionInput toolInput) {
        ValidationResult inputValidation = validateInput(toolInput);
        if (!inputValidation.isValid()) {
            return new ExecutionResult(false, inputValidation.getErrorMessage(), null);
        }
        try {
            String command = toolInput.content().trim();
            List<String> commandArgs = parseCommand(command);
            if (commandArgs.isEmpty()) {
                return new ExecutionResult(false, "Invalid command format", null);
            }
            ValidationResult commandValidation = validateCommand(commandArgs);
            if (!commandValidation.isValid()) {
                return new ExecutionResult(false, commandValidation.getErrorMessage(), null);
            }
            if (!allowedCommands.isEmpty() && !allowedCommands.contains(commandArgs.get(0))) {
                return new ExecutionResult(false, "Command not allowed: " + commandArgs.get(0), null);
            }
            ProcessBuilder pb = new ProcessBuilder(commandArgs);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                return new ExecutionResult(false, "Command timed out after " + timeout.toSeconds() + " seconds", null);
            }
            String output;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                output = r.lines().collect(Collectors.joining("\n"));
            }
            int code = p.exitValue();
            boolean success = code == 0;
            String result = "ExitCode=" + code + "\n" + output;
            return new ExecutionResult(success, result, Map.of("exitCode", code, "command", commandArgs.get(0)));
        } catch (Exception e) {
            return new ExecutionResult(false, "Execution error: " + e.getMessage(), null);
        }
    }

    private List<String> parseCommand(String command) {
        List<String> args = new ArrayList<>();
        Matcher matcher = COMMAND_PATTERN.matcher(command);
        while (matcher.find()) {
            String arg = null;
            if (matcher.group(1) != null) {
                arg = matcher.group(1);
            } else if (matcher.group(2) != null) {
                arg = matcher.group(2);
            } else if (matcher.group(3) != null) {
                arg = matcher.group(3);
            }
            if (arg != null && !arg.isEmpty()) {
                args.add(arg);
            }
        }
        return args;
    }

    private ValidationResult validateInput(ExecutionInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }
        if (input.content() == null) {
            return ValidationResult.invalid("Command cannot be null");
        }
        String command = input.content().trim();
        if (command.isEmpty()) {
            return ValidationResult.invalid("Command cannot be empty");
        }
        if (command.length() > 8192) {
            return ValidationResult.invalid("Command is too long (max 8192 characters)");
        }
        for (char c : command.toCharArray()) {
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r' && c != ' ') {
                return ValidationResult.invalid("Command contains invalid control characters");
            }
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateCommand(List<String> commandArgs) {
        if (commandArgs.isEmpty()) {
            return ValidationResult.invalid("Command arguments cannot be empty");
        }

        String executable = commandArgs.get(0);

        // Block dangerous commands
        Set<String> blockedCommands = Set.of(
                "rm", "del", "format", "fdisk", "mkfs", "dd",
                "shutdown", "reboot", "halt", "poweroff",
                "su", "sudo", "runas", "net", "sc", "service",
                "kill", "killall", "taskkill", "wmic"
        );
        if (blockedCommands.contains(executable.toLowerCase())) {
            return ValidationResult.invalid("Dangerous command blocked: " + executable);
        }

        // Validate executable path - only block path traversal
        if (executable.contains("..")) {
            return ValidationResult.invalid("Executable path contains path traversal (..)");
        }

        // Limit number of arguments
        if (commandArgs.size() > 100) {
            return ValidationResult.invalid("Too many command arguments (max 100)");
        }

        // Validate each argument for length and dangerous patterns
        for (int i = 0; i < commandArgs.size(); i++) {
            String arg = commandArgs.get(i);

            if (arg.length() > 2048) {
                return ValidationResult.invalid("Command argument too long (max 2048 characters)");
            }

            // Check for null bytes which are always dangerous
            if (arg.indexOf('\0') >= 0) {
                return ValidationResult.invalid("Command argument contains null byte");
            }

            // Check for newlines which could be used for command chaining in some shells
            if (arg.contains("\n") || arg.contains("\r")) {
                return ValidationResult.invalid("Command argument contains newline characters");
            }
        }

        return ValidationResult.valid();
    }

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
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
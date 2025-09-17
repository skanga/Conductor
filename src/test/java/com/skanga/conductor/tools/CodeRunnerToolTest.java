package com.skanga.conductor.tools;

import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeRunnerToolTest {

    private CodeRunnerTool unrestricted;
    private CodeRunnerTool restricted;

    @BeforeEach
    void setUp() {
        unrestricted = new CodeRunnerTool(Duration.ofSeconds(5));
        Set<String> allowedCommands = Set.of("echo", "pwd", "whoami");
        restricted = new CodeRunnerTool(Duration.ofSeconds(5), allowedCommands);
    }

    @Test
    @Order(1)
    @DisplayName("Test basic tool properties")
    void testBasicProperties() {
        assertEquals("code_runner", unrestricted.name());
        assertTrue(unrestricted.description().contains("shell command"));
        assertTrue(unrestricted.description().contains("safely"));
    }

    @Test
    @Order(2)
    @DisplayName("Test simple command execution")
    void testSimpleCommandExecution() {
        ToolInput input = new ToolInput("echo hello", null);
        ToolResult result = unrestricted.run(input);

        assertTrue(result.success(), "Simple echo command should succeed");
        assertTrue(result.output().contains("hello"), "Output should contain expected text");
        assertTrue(result.output().contains("ExitCode=0"), "Should show successful exit code");
    }

    @Test
    @Order(3)
    @DisplayName("Test command with quoted arguments")
    void testQuotedArguments() {
        ToolInput input = new ToolInput("echo \"hello world with spaces\"", null);
        ToolResult result = unrestricted.run(input);

        assertTrue(result.success(), "Command with quoted args should succeed");
        assertTrue(result.output().contains("hello world with spaces"), "Quoted argument should be preserved");
    }

    @Test
    @Order(4)
    @DisplayName("Test command with single quotes")
    void testSingleQuotes() {
        ToolInput input = new ToolInput("echo 'single quoted text'", null);
        ToolResult result = unrestricted.run(input);

        assertTrue(result.success(), "Command with single quotes should succeed");
        assertTrue(result.output().contains("single quoted text"), "Single quoted argument should be preserved");
    }

    @Test
    @Order(5)
    @DisplayName("Test command injection prevention")
    void testCommandInjectionPrevention() {
        // Test that command injection attempts are treated as literal arguments
        ToolInput maliciousInput = new ToolInput("echo hello; rm dangerous.txt", null);
        ToolResult result = unrestricted.run(maliciousInput);

        assertTrue(result.success(), "Command should execute successfully");
        assertTrue(result.output().contains("hello; rm dangerous.txt"),
                  "Injection attempt should be treated as literal argument to echo");
        assertFalse(result.output().contains("dangerous.txt: not found"),
                   "Second command should not be executed");
    }

    @Test
    @Order(6)
    @DisplayName("Test command whitelist - allowed commands")
    void testAllowedCommands() {
        ToolInput input = new ToolInput("echo allowed command", null);
        ToolResult result = restricted.run(input);

        assertTrue(result.success(), "Allowed command should succeed");
        assertTrue(result.output().contains("allowed command"), "Output should contain expected text");
    }

    @Test
    @Order(7)
    @DisplayName("Test command whitelist - blocked commands")
    void testBlockedCommands() {
        ToolInput input = new ToolInput("rm dangerous.txt", null);
        ToolResult result = restricted.run(input);

        assertFalse(result.success(), "Blocked command should fail");
        assertTrue(result.output().contains("Dangerous command blocked: rm"),
                  "Should indicate command is not allowed");
    }

    @Test
    @Order(8)
    @DisplayName("Test empty and null input handling")
    void testEmptyInput() {
        // Test empty command
        ToolResult emptyResult = unrestricted.run(new ToolInput("", null));
        assertFalse(emptyResult.success(), "Empty command should fail");
        assertTrue(emptyResult.output().contains("Command cannot be empty"),
                  "Should indicate no command provided");

        // Test null command
        ToolResult nullResult = unrestricted.run(new ToolInput(null, null));
        assertFalse(nullResult.success(), "Null command should fail");
        assertTrue(nullResult.output().contains("Command cannot be null"),
                  "Should indicate no command provided");

        // Test whitespace-only command
        ToolResult whitespaceResult = unrestricted.run(new ToolInput("   ", null));
        assertFalse(whitespaceResult.success(), "Command cannot be empty");
    }

    @Test
    @Order(9)
    @DisplayName("Test command parsing edge cases")
    void testCommandParsingEdgeCases() {
        // Test mixed quotes
        ToolInput mixedQuotes = new ToolInput("echo \"double\" 'single' unquoted", null);
        ToolResult result = unrestricted.run(mixedQuotes);
        assertTrue(result.success(), "Mixed quotes should be parsed correctly");

        // Test escaped quotes within quotes
        ToolInput nestedQuotes = new ToolInput("echo \"text with 'nested' quotes\"", null);
        ToolResult nestedResult = unrestricted.run(nestedQuotes);
        assertTrue(nestedResult.success(), "Nested quotes should be handled correctly");

        // Test multiple spaces
        ToolInput multipleSpaces = new ToolInput("echo    multiple     spaces", null);
        ToolResult spacesResult = unrestricted.run(multipleSpaces);
        assertTrue(spacesResult.success(), "Multiple spaces should be handled correctly");
    }

    @Test
    @Order(10)
    @DisplayName("Test command that returns non-zero exit code")
    void testNonZeroExitCode() {
        // Use a command that will fail (trying to access non-existent file)
        ToolInput input = new ToolInput("ls /non/existent/path", null);
        ToolResult result = unrestricted.run(input);

        assertFalse(result.success(), "Command with non-zero exit code should report failure");
        assertTrue(result.output().contains("ExitCode="), "Should include exit code in output");

        // Check metadata contains exit code
        if (result.metadata() != null && result.metadata() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) result.metadata();
            Object exitCode = metadata.get("exitCode");
            assertNotNull(exitCode, "Metadata should contain exit code");
            assertNotEquals(0, exitCode, "Exit code should be non-zero");
        }
    }

    @Test
    @Order(11)
    @DisplayName("Test timeout handling")
    void testTimeoutHandling() {
        // Create tool with very short timeout and use a simple loop that should time out
        Set<String> allowedCommands = Set.of("java", "echo");
        CodeRunnerTool shortTimeout = new CodeRunnerTool(Duration.ofMillis(50), allowedCommands);

        // Create a simple java command that will take longer than 50ms
        String longRunningCommand = "java -version"; // This typically takes more than 50ms

        ToolInput input = new ToolInput(longRunningCommand, null);
        ToolResult result = shortTimeout.run(input);

        // The command should either time out or complete but should generally be rejected by timing constraint
        // Since java -version is fast, let's test with an even shorter timeout
        CodeRunnerTool veryShortTimeout = new CodeRunnerTool(Duration.ofMillis(1), allowedCommands);
        ToolResult result2 = veryShortTimeout.run(input);

        // At least one should show timeout behavior
        boolean anyTimeout = result.output().contains("timed out") || result2.output().contains("timed out");
        assertTrue(anyTimeout || (!result.success() || !result2.success()),
                  "At least one execution should timeout or fail. Result1: " + result.output() + ", Result2: " + result2.output());
    }

    @Test
    @Order(12)
    @DisplayName("Test security - path traversal attempts")
    void testPathTraversalPrevention() {
        // These commands with path traversal attempts should be treated as literal arguments
        String[] traversalAttempts = {
            "echo ../../../etc/passwd",
            "echo ..\\..\\..\\windows\\system32",
            "echo /etc/shadow"
        };

        for (String command : traversalAttempts) {
            ToolInput input = new ToolInput(command, null);
            ToolResult result = unrestricted.run(input);

            assertTrue(result.success(), "Path traversal attempt should be treated as echo argument");
            // The path should appear in output as literal text, not be interpreted
            String expectedPath = command.substring("echo ".length());
            assertTrue(result.output().contains(expectedPath),
                      "Path should appear as literal output: " + expectedPath);
        }
    }

    @Test
    @Order(13)
    @DisplayName("Test special characters in arguments")
    void testSpecialCharacters() {
        // Test various special characters that might cause issues
        String[] specialInputs = {
            "echo @#$%^&*()",
            "echo |<>",
            "echo `backticks`",
            "echo $HOME",
            "echo ~user"
        };

        for (String command : specialInputs) {
            ToolInput input = new ToolInput(command, null);
            ToolResult result = unrestricted.run(input);

            assertTrue(result.success(), "Command with special characters should succeed: " + command);

            // Special characters should be treated literally in echo output
            String expectedOutput = command.substring("echo ".length());
            assertTrue(result.output().contains(expectedOutput),
                      "Special characters should appear literally in output: " + expectedOutput);
        }
    }

    @Test
    @Order(14)
    @DisplayName("Test metadata content")
    void testMetadata() {
        ToolInput input = new ToolInput("echo test metadata", null);
        ToolResult result = unrestricted.run(input);

        assertTrue(result.success(), "Command should succeed");
        assertNotNull(result.metadata(), "Metadata should not be null");

        // Check that metadata contains expected information
        if (result.metadata() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) result.metadata();
            assertEquals(0, metadata.get("exitCode"), "Exit code should be 0");
            assertEquals("echo", metadata.get("command"), "Command name should be stored");
        }
    }
}
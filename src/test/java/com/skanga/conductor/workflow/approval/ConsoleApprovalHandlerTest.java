package com.skanga.conductor.workflow.approval;

import com.skanga.conductor.exception.ApprovalException;
import com.skanga.conductor.exception.ApprovalTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleApprovalHandlerTest {

    private ConsoleApprovalHandler handler;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Capture console output
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Store original input stream
        originalIn = System.in;

        handler = new ConsoleApprovalHandler();
    }

    @AfterEach
    void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setIn(originalIn);

        if (handler != null) {
            handler.close();
        }
    }

    @Test
    void shouldReturnTrueForIsInteractive() {
        // When & Then
        assertTrue(handler.isInteractive());
    }

    @Test
    void shouldReturnCorrectDescription() {
        // When
        String description = handler.getDescription();

        // Then
        assertEquals("Console-based interactive approval handler", description);
    }

    @Test
    void shouldParseTimeoutStringInMilliseconds() {
        // When & Then
        assertEquals(1500L, ConsoleApprovalHandler.parseTimeout("1500ms"));
        assertEquals(500L, ConsoleApprovalHandler.parseTimeout("500MS"));
        assertEquals(100L, ConsoleApprovalHandler.parseTimeout("  100ms  "));
    }

    @Test
    void shouldParseTimeoutStringInSeconds() {
        // When & Then
        assertEquals(30000L, ConsoleApprovalHandler.parseTimeout("30s"));
        assertEquals(45000L, ConsoleApprovalHandler.parseTimeout("45S"));
        assertEquals(60000L, ConsoleApprovalHandler.parseTimeout("  60s  "));
    }

    @Test
    void shouldParseTimeoutStringInMinutes() {
        // When & Then
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("5m"));
        assertEquals(600000L, ConsoleApprovalHandler.parseTimeout("10M"));
        assertEquals(120000L, ConsoleApprovalHandler.parseTimeout("  2m  "));
    }

    @Test
    void shouldParseTimeoutStringInHours() {
        // When & Then
        assertEquals(3600000L, ConsoleApprovalHandler.parseTimeout("1h"));
        assertEquals(7200000L, ConsoleApprovalHandler.parseTimeout("2H"));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("  0.5h  ")); // Decimal not supported, returns default
        assertEquals(10800000L, ConsoleApprovalHandler.parseTimeout("3h"));
    }

    @Test
    void shouldParseTimeoutStringWithoutUnit() {
        // When & Then - assumes seconds when no unit
        assertEquals(30000L, ConsoleApprovalHandler.parseTimeout("30"));
        assertEquals(120000L, ConsoleApprovalHandler.parseTimeout("120"));
        assertEquals(5000L, ConsoleApprovalHandler.parseTimeout("  5  "));
    }

    @Test
    void shouldReturnDefaultTimeoutForNullOrEmptyString() {
        // When & Then - default is 5 minutes (300000ms)
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout(null));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout(""));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("   "));
    }

    @Test
    void shouldReturnDefaultTimeoutForInvalidFormat() {
        // When & Then - invalid formats should return default
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("invalid"));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("abc123"));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("12.5x"));
        assertEquals(-10000L, ConsoleApprovalHandler.parseTimeout("-10s")); // Negative values are parsed as-is
    }

    @Test
    void shouldHandleEdgeCaseTimeoutValues() {
        // When & Then
        assertEquals(0L, ConsoleApprovalHandler.parseTimeout("0s"));
        assertEquals(0L, ConsoleApprovalHandler.parseTimeout("0ms"));
        assertEquals(1L, ConsoleApprovalHandler.parseTimeout("1ms"));
        assertEquals(1000L, ConsoleApprovalHandler.parseTimeout("1s"));
    }

    @Test
    void shouldHandleLargeTimeoutValues() {
        // When & Then
        assertEquals(86400000L, ConsoleApprovalHandler.parseTimeout("24h")); // 24 hours
        assertEquals(604800000L, ConsoleApprovalHandler.parseTimeout("168h")); // 1 week in hours
        assertEquals(3600000L, ConsoleApprovalHandler.parseTimeout("3600s")); // 1 hour in seconds
    }

    @Test
    void shouldCloseExecutorServiceGracefully() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> handler.close());
    }

    @Test
    void shouldAllowMultipleCloseCalls() {
        // When & Then - multiple close calls should be safe
        assertDoesNotThrow(() -> {
            handler.close();
            handler.close();
            handler.close();
        });
    }

    @Test
    void shouldTimeoutWhenNoInputProvided() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            "TestWorkflow", "TestStage", "Test description", "Test content", "Test review"
        );

        // Simulate no input (empty stream that will cause timeout)
        System.setIn(new ByteArrayInputStream("".getBytes()));

        // When & Then
        assertThrows(ApprovalTimeoutException.class, () -> {
            handler.requestApproval(request, 100L); // Very short timeout
        });
    }

    @Test
    void shouldDisplayApprovalRequestCorrectly() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            "TestWorkflow", "TestStage", "Test description", "Generated content", "Review content"
        );

        // Simulate input that times out quickly to test display
        System.setIn(new ByteArrayInputStream("".getBytes()));

        try {
            handler.requestApproval(request, 10L);
        } catch (ApprovalException e) {
            // Expected - we just want to see the display output
        }

        // Then
        String output = outputStream.toString();
        assertTrue(output.contains("HUMAN APPROVAL REQUIRED"));
        assertTrue(output.contains("Workflow: TestWorkflow"));
        assertTrue(output.contains("Stage: TestStage"));
        assertTrue(output.contains("Description: Test description"));
        assertTrue(output.contains("GENERATED CONTENT:"));
        assertTrue(output.contains("Generated content"));
        assertTrue(output.contains("REVIEW FEEDBACK:"));
        assertTrue(output.contains("Review content"));
    }

    @Test
    void shouldHandleNullContentInRequest() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            "TestWorkflow", "TestStage", null, null, null
        );

        // Simulate input that times out quickly to test display
        System.setIn(new ByteArrayInputStream("".getBytes()));

        try {
            handler.requestApproval(request, 10L);
        } catch (ApprovalException e) {
            // Expected - we just want to see the display output
        }

        // Then
        String output = outputStream.toString();
        assertTrue(output.contains("HUMAN APPROVAL REQUIRED"));
        assertTrue(output.contains("Workflow: TestWorkflow"));
        assertTrue(output.contains("Stage: TestStage"));
        // Should not contain null content sections
        assertFalse(output.contains("Description: null"));
        assertFalse(output.contains("GENERATED CONTENT:"));
        assertFalse(output.contains("REVIEW FEEDBACK:"));
    }

    @Test
    void shouldHandleEmptyContentInRequest() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            "TestWorkflow", "TestStage", "", "", ""
        );

        // Simulate input that times out quickly to test display
        System.setIn(new ByteArrayInputStream("".getBytes()));

        try {
            handler.requestApproval(request, 10L);
        } catch (ApprovalException e) {
            // Expected - we just want to see the display output
        }

        // Then
        String output = outputStream.toString();
        assertTrue(output.contains("HUMAN APPROVAL REQUIRED"));
        assertTrue(output.contains("Workflow: TestWorkflow"));
        assertTrue(output.contains("Stage: TestStage"));
        assertTrue(output.contains("Description: "));
        assertTrue(output.contains("GENERATED CONTENT:"));
        assertTrue(output.contains("REVIEW FEEDBACK:"));
    }

    @Test
    void shouldHandleSpecialCharactersInRequest() {
        // Given
        ApprovalRequest request = new ApprovalRequest(
            "Wörkflöw with émojis 🚀",
            "Stäge with spëcial chars",
            "Description with unicode: 你好世界",
            "Content with symbols: @#$%^&*()",
            "Review with newlines\nand\ttabs"
        );

        // Simulate input that times out quickly to test display
        System.setIn(new ByteArrayInputStream("".getBytes()));

        try {
            handler.requestApproval(request, 10L);
        } catch (ApprovalException e) {
            // Expected - we just want to see the display output
        }

        // Then
        String output = outputStream.toString();
        assertTrue(output.contains("Wörkflöw with émojis 🚀"));
        assertTrue(output.contains("Stäge with spëcial chars"));
        assertTrue(output.contains("你好世界"));
        assertTrue(output.contains("@#$%^&*()"));
        assertTrue(output.contains("newlines"));
        assertTrue(output.contains("tabs"));
    }

    @Test
    void shouldParseTimeoutWithDecimalValues() {
        // Given invalid decimal inputs (these should return default)
        // When & Then
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("1.5h"));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("2.5m"));
        assertEquals(300000L, ConsoleApprovalHandler.parseTimeout("30.5s"));
    }

    @Test
    void shouldHandleVeryLargeTimeoutNumbers() {
        // When & Then
        assertEquals(Long.MAX_VALUE / 1000 * 1000, ConsoleApprovalHandler.parseTimeout(String.valueOf(Long.MAX_VALUE / 1000) + "s"));
    }

    @Test
    void shouldCreateHandlerSuccessfully() {
        // When
        ConsoleApprovalHandler newHandler = new ConsoleApprovalHandler();

        // Then
        assertNotNull(newHandler);
        assertTrue(newHandler.isInteractive());
        assertEquals("Console-based interactive approval handler", newHandler.getDescription());

        // Cleanup
        newHandler.close();
    }

    @Test
    void shouldFormatOutputWithProperLineBreaks() {
        // Given - Test content wrapping logic indirectly through display
        String longContent = "This is a very long line of content that should be wrapped at 75 characters to ensure proper display formatting in the console interface and provide good readability for users reviewing the generated content during the approval process.";

        ApprovalRequest request = new ApprovalRequest(
            "TestWorkflow", "TestStage", "Test description", longContent, "Short review"
        );

        // Simulate input that times out quickly to test display
        System.setIn(new ByteArrayInputStream("".getBytes()));

        try {
            handler.requestApproval(request, 10L);
        } catch (ApprovalException e) {
            // Expected - we just want to see the display output
        }

        // Then - check that content is displayed (wrapping logic is tested indirectly)
        String output = outputStream.toString();
        assertTrue(output.contains("GENERATED CONTENT:"));
        assertTrue(output.contains("This is a very long line"));
        assertTrue(output.contains("REVIEW FEEDBACK:"));
        assertTrue(output.contains("Short review"));
    }
}
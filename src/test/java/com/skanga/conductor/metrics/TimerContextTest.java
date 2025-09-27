package com.skanga.conductor.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TimerContext class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TimerContext Unit Tests")
class TimerContextTest {

    @Mock
    private Consumer<Metric> mockMetricRecorder;

    @Captor
    private ArgumentCaptor<Metric> metricCaptor;

    private Map<String, String> testTags;

    @BeforeEach
    void setUp() {
        testTags = new HashMap<>();
        testTags.put("component", "test");
        testTags.put("operation", "test-operation");
    }

    @Test
    @DisplayName("Should record timer metric when closed")
    void shouldRecordTimerMetricWhenClosed() throws InterruptedException {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);

        // Wait a small amount to ensure non-zero duration
        Thread.sleep(10);

        // Act
        timer.close();

        // Assert
        verify(mockMetricRecorder).accept(metricCaptor.capture());
        Metric capturedMetric = metricCaptor.getValue();

        assertEquals("test.timer", capturedMetric.name());
        assertEquals(MetricType.TIMER, capturedMetric.type());
        assertTrue(capturedMetric.value() >= 10); // At least 10ms
        assertEquals(testTags, capturedMetric.tags());
        assertTrue(timer.isClosed());
    }

    @Test
    @DisplayName("Should handle null tags")
    void shouldHandleNullTags() {
        // Arrange & Act
        TimerContext timer = new TimerContext("test.timer", null, mockMetricRecorder);
        timer.close();

        // Assert
        verify(mockMetricRecorder).accept(metricCaptor.capture());
        Metric capturedMetric = metricCaptor.getValue();

        assertNull(capturedMetric.tags());
    }

    @Test
    @DisplayName("Should handle null metric recorder")
    void shouldHandleNullMetricRecorder() {
        // Arrange & Act
        TimerContext timer = new TimerContext("test.timer", testTags, null);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            timer.close();
            timer.recordWithSuccess(true);
        });

        assertFalse(timer.isClosed()); // Should not be closed when metricRecorder is null
    }

    @Test
    @DisplayName("Should track elapsed time accurately")
    void shouldTrackElapsedTimeAccurately() throws InterruptedException {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);
        Instant startTime = timer.getStartTime();

        // Act
        Thread.sleep(50);
        Duration elapsed = timer.getElapsed();
        long elapsedMs = timer.getElapsedMs();

        // Assert
        assertNotNull(startTime);
        assertTrue(elapsed.toMillis() >= 50);
        assertTrue(elapsedMs >= 50);
        assertEquals(elapsed.toMillis(), elapsedMs);
    }

    @Test
    @DisplayName("Should record metric with success information")
    void shouldRecordMetricWithSuccessInformation() throws InterruptedException {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);
        Thread.sleep(10);

        // Act
        timer.recordWithSuccess(true);

        // Assert
        verify(mockMetricRecorder).accept(metricCaptor.capture());
        Metric capturedMetric = metricCaptor.getValue();

        assertEquals("test.timer", capturedMetric.name());
        assertEquals(MetricType.TIMER, capturedMetric.type());
        assertTrue(capturedMetric.value() >= 10);

        Map<String, String> expectedTags = new HashMap<>(testTags);
        expectedTags.put("success", "true");
        assertEquals(expectedTags, capturedMetric.tags());

        assertFalse(timer.isClosed()); // Should not be closed after recordWithSuccess
    }

    @Test
    @DisplayName("Should record metric with failure information")
    void shouldRecordMetricWithFailureInformation() {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);

        // Act
        timer.recordWithSuccess(false);

        // Assert
        verify(mockMetricRecorder).accept(metricCaptor.capture());
        Metric capturedMetric = metricCaptor.getValue();

        Map<String, String> expectedTags = new HashMap<>(testTags);
        expectedTags.put("success", "false");
        assertEquals(expectedTags, capturedMetric.tags());
    }

    @Test
    @DisplayName("Should handle recordWithSuccess with null tags")
    void shouldHandleRecordWithSuccessWithNullTags() {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", null, mockMetricRecorder);

        // Act
        timer.recordWithSuccess(true);

        // Assert
        verify(mockMetricRecorder).accept(metricCaptor.capture());
        Metric capturedMetric = metricCaptor.getValue();

        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put("success", "true");
        assertEquals(expectedTags, capturedMetric.tags());
    }

    @Test
    @DisplayName("Should not record multiple times when closed multiple times")
    void shouldNotRecordMultipleTimesWhenClosedMultipleTimes() {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);

        // Act
        timer.close();
        timer.close(); // Second close should be ignored

        // Assert
        verify(mockMetricRecorder, times(1)).accept(any(Metric.class));
        assertTrue(timer.isClosed());
    }

    @Test
    @DisplayName("Should work with try-with-resources pattern")
    void shouldWorkWithTryWithResourcesPattern() {
        // Act
        try (TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder)) {
            // Simulate some work
            assertFalse(timer.isClosed());
        }

        // Assert
        verify(mockMetricRecorder).accept(any(Metric.class));
    }

    @Test
    @DisplayName("Should allow recording additional metrics after recordWithSuccess")
    void shouldAllowRecordingAdditionalMetricsAfterRecordWithSuccess() {
        // Arrange
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);

        // Act
        timer.recordWithSuccess(true);
        timer.recordWithSuccess(false);
        timer.close();

        // Assert
        verify(mockMetricRecorder, times(3)).accept(any(Metric.class));
        assertTrue(timer.isClosed());
    }

    @Test
    @DisplayName("Should preserve original tags when recording with success")
    void shouldPreserveOriginalTagsWhenRecordingWithSuccess() {
        // Arrange
        Map<String, String> originalTags = new HashMap<>();
        originalTags.put("key1", "value1");
        originalTags.put("key2", "value2");

        TimerContext timer = new TimerContext("test.timer", originalTags, mockMetricRecorder);

        // Act
        timer.recordWithSuccess(true);

        // Assert
        // Verify original tags are not modified
        assertEquals(2, originalTags.size());
        assertEquals("value1", originalTags.get("key1"));
        assertEquals("value2", originalTags.get("key2"));
        assertFalse(originalTags.containsKey("success"));

        // Verify captured metric has success tag added
        verify(mockMetricRecorder).accept(metricCaptor.capture());
        Metric capturedMetric = metricCaptor.getValue();

        Map<String, String> capturedTags = capturedMetric.tags();
        assertEquals(3, capturedTags.size());
        assertEquals("value1", capturedTags.get("key1"));
        assertEquals("value2", capturedTags.get("key2"));
        assertEquals("true", capturedTags.get("success"));
    }

    @Test
    @DisplayName("Should have consistent start time")
    void shouldHaveConsistentStartTime() {
        // Arrange
        Instant beforeCreation = Instant.now();

        // Act
        TimerContext timer = new TimerContext("test.timer", testTags, mockMetricRecorder);
        Instant startTime1 = timer.getStartTime();
        Instant startTime2 = timer.getStartTime();

        Instant afterCreation = Instant.now();

        // Assert
        assertEquals(startTime1, startTime2); // Should be the same instance
        assertTrue(startTime1.isAfter(beforeCreation) || startTime1.equals(beforeCreation));
        assertTrue(startTime1.isBefore(afterCreation) || startTime1.equals(afterCreation));
    }
}
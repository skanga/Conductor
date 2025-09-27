package com.skanga.conductor.metrics;

import com.skanga.conductor.config.ApplicationConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryMetricsCollector class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryMetricsCollector Unit Tests")
class InMemoryMetricsCollectorTest {

    private InMemoryMetricsCollector collector;

    @BeforeEach
    void setUp() {
        ApplicationConfig.resetInstance();
        collector = new InMemoryMetricsCollector(3600000, 1000, true); // 1 hour, 1000 metrics, enabled
    }

    @AfterEach
    void tearDown() {
        ApplicationConfig.resetInstance();
    }

    @Test
    @DisplayName("Should record metrics when enabled")
    void shouldRecordMetricsWhenEnabled() {
        // Arrange
        Metric metric = Metric.counter("test.counter", Map.of("key", "value"));

        // Act
        collector.record(metric);

        // Assert
        assertEquals(1, collector.getMetricCount());
        List<Metric> metrics = collector.getMetricsByName("test.counter");
        assertEquals(1, metrics.size());
        assertEquals(metric, metrics.get(0));
    }

    @Test
    @DisplayName("Should not record metrics when disabled")
    void shouldNotRecordMetricsWhenDisabled() {
        // Arrange
        InMemoryMetricsCollector disabledCollector = new InMemoryMetricsCollector(3600000, 1000, false);
        Metric metric = Metric.counter("test.counter", Map.of("key", "value"));

        // Act
        disabledCollector.record(metric);

        // Assert
        assertEquals(0, disabledCollector.getMetricCount());
        assertFalse(disabledCollector.isEnabled());
    }

    @Test
    @DisplayName("Should handle null metrics gracefully")
    void shouldHandleNullMetricsGracefully() {
        // Act & Assert
        assertDoesNotThrow(() -> collector.record(null));
        assertEquals(0, collector.getMetricCount());
    }

    @Test
    @DisplayName("Should create and update metric summaries")
    void shouldCreateAndUpdateMetricSummaries() {
        // Arrange
        Metric metric1 = Metric.timer("test.timer", 100, Map.of("key", "value1"));
        Metric metric2 = Metric.timer("test.timer", 200, Map.of("key", "value2"));
        Metric metric3 = Metric.timer("test.timer", 150, Map.of("key", "value3"));

        // Act
        collector.record(metric1);
        collector.record(metric2);
        collector.record(metric3);

        // Assert
        InMemoryMetricsCollector.MetricSummary summary = collector.getSummary("test.timer");
        assertNotNull(summary);
        assertEquals("test.timer", summary.getName());
        assertEquals(MetricType.TIMER, summary.getType());
        assertEquals(3, summary.getCount());
        assertEquals(450.0, summary.getSum());
        assertEquals(100.0, summary.getMin());
        assertEquals(200.0, summary.getMax());
        assertEquals(150.0, summary.getAverage());
    }

    @Test
    @DisplayName("Should retrieve metrics by time range")
    void shouldRetrieveMetricsByTimeRange() throws InterruptedException {
        // Arrange
        Instant start = Instant.now();
        Metric metric1 = Metric.counter("test.counter", Map.of("key", "value1"));
        collector.record(metric1);

        Thread.sleep(10);
        Instant middle = Instant.now();

        Metric metric2 = Metric.counter("test.counter", Map.of("key", "value2"));
        collector.record(metric2);

        Instant end = Instant.now();

        // Act
        List<Metric> allMetrics = collector.getMetrics(start, end);
        List<Metric> firstHalf = collector.getMetrics(start, middle.minus(1, java.time.temporal.ChronoUnit.MILLIS));

        // Assert
        assertEquals(2, allMetrics.size());
        assertEquals(1, firstHalf.size());
        assertEquals(metric1, firstHalf.get(0));
    }

    @Test
    @DisplayName("Should retrieve top metrics by value")
    void shouldRetrieveTopMetricsByValue() {
        // Arrange
        collector.record(Metric.gauge("test.gauge", 10.0, Map.of("id", "1")));
        collector.record(Metric.gauge("test.gauge", 50.0, Map.of("id", "2")));
        collector.record(Metric.gauge("test.gauge", 30.0, Map.of("id", "3")));
        collector.record(Metric.gauge("test.gauge", 40.0, Map.of("id", "4")));

        // Act
        List<Metric> top2 = collector.getTopMetrics("test.gauge", 2);

        // Assert
        assertEquals(2, top2.size());
        assertEquals(50.0, top2.get(0).value());
        assertEquals(40.0, top2.get(1).value());
    }

    @Test
    @DisplayName("Should clear all metrics and summaries")
    void shouldClearAllMetricsAndSummaries() {
        // Arrange
        collector.record(Metric.counter("test.counter", Map.of("key", "value")));
        collector.record(Metric.timer("test.timer", 100, Map.of("key", "value")));

        assertEquals(2, collector.getMetricCount());
        assertNotNull(collector.getSummary("test.counter"));
        assertNotNull(collector.getSummary("test.timer"));

        // Act
        collector.clear();

        // Assert
        assertEquals(0, collector.getMetricCount());
        assertNull(collector.getSummary("test.counter"));
        assertNull(collector.getSummary("test.timer"));
        assertTrue(collector.getAllSummaries().isEmpty());
    }

    @Test
    @DisplayName("Should start timer context")
    void shouldStartTimerContext() {
        // Arrange
        Map<String, String> tags = Map.of("component", "test");

        // Act
        TimerContext timer = collector.startTimer("test.timer", tags);

        // Assert
        assertNotNull(timer);
        assertFalse(timer.isClosed());

        // Clean up
        timer.close();
        assertEquals(1, collector.getMetricCount());
    }

    @Test
    @DisplayName("Should trim oldest metrics when exceeding limit")
    void shouldTrimOldestMetricsWhenExceedingLimit() {
        // Arrange
        InMemoryMetricsCollector limitedCollector = new InMemoryMetricsCollector(3600000, 3, true);

        // Act - Add more metrics than the limit
        limitedCollector.record(Metric.counter("test1", Map.of("order", "1")));
        limitedCollector.record(Metric.counter("test2", Map.of("order", "2")));
        limitedCollector.record(Metric.counter("test3", Map.of("order", "3")));
        limitedCollector.record(Metric.counter("test4", Map.of("order", "4"))); // Should trigger trimming

        // Assert
        assertEquals(3, limitedCollector.getMetricCount());

        // The oldest metric should be removed, so test1 should not be found
        List<Metric> test1Metrics = limitedCollector.getMetricsByName("test1");
        assertTrue(test1Metrics.isEmpty());

        // test4 should still be there
        List<Metric> test4Metrics = limitedCollector.getMetricsByName("test4");
        assertEquals(1, test4Metrics.size());
    }

    @Test
    @DisplayName("Should handle retention period cleanup")
    void shouldHandleRetentionPeriodCleanup() throws InterruptedException {
        // Arrange
        InMemoryMetricsCollector shortRetentionCollector = new InMemoryMetricsCollector(50, 1000, true); // 50ms retention

        // Add initial metrics
        shortRetentionCollector.record(Metric.counter("old.metric", Map.of("key", "value")));

        // Wait for retention period to pass
        Thread.sleep(60);

        // Add enough metrics to trigger cleanup (every 1000 metrics, but we'll simulate by adding directly)
        for (int i = 0; i < 1000; i++) {
            shortRetentionCollector.record(Metric.counter("new.metric." + i, Map.of("key", "value")));
        }

        // Assert - old metrics should be cleaned up
        List<Metric> oldMetrics = shortRetentionCollector.getMetricsByName("old.metric");
        assertTrue(oldMetrics.isEmpty());
    }

    @Test
    @DisplayName("Should get all summaries")
    void shouldGetAllSummaries() {
        // Arrange
        collector.record(Metric.counter("test.counter", Map.of("key", "value")));
        collector.record(Metric.timer("test.timer", 100, Map.of("key", "value")));
        collector.record(Metric.gauge("test.gauge", 50.0, Map.of("key", "value")));

        // Act
        Map<String, InMemoryMetricsCollector.MetricSummary> summaries = collector.getAllSummaries();

        // Assert
        assertEquals(3, summaries.size());
        assertTrue(summaries.containsKey("test.counter"));
        assertTrue(summaries.containsKey("test.timer"));
        assertTrue(summaries.containsKey("test.gauge"));
    }

    @Nested
    @DisplayName("MetricSummary Tests")
    class MetricSummaryTest {

        @Test
        @DisplayName("Should create metric summary from single metric")
        void shouldCreateMetricSummaryFromSingleMetric() {
            // Arrange
            Metric metric = Metric.timer("test.timer", 100, Map.of("key", "value"));

            // Act
            InMemoryMetricsCollector.MetricSummary summary = new InMemoryMetricsCollector.MetricSummary(metric);

            // Assert
            assertEquals("test.timer", summary.getName());
            assertEquals(MetricType.TIMER, summary.getType());
            assertEquals(1, summary.getCount());
            assertEquals(100.0, summary.getSum());
            assertEquals(100.0, summary.getMin());
            assertEquals(100.0, summary.getMax());
            assertEquals(100.0, summary.getAverage());
            assertEquals(metric.timestamp(), summary.getFirstSeen());
            assertEquals(metric.timestamp(), summary.getLastSeen());
        }

        @Test
        @DisplayName("Should update metric summary correctly")
        void shouldUpdateMetricSummaryCorrectly() throws InterruptedException {
            // Arrange
            Instant time1 = Instant.now();
            Metric metric1 = new Metric("test.timer", MetricType.TIMER, 100.0, time1, Map.of("key", "value1"));

            Thread.sleep(10);

            Instant time2 = Instant.now();
            Metric metric2 = new Metric("test.timer", MetricType.TIMER, 200.0, time2, Map.of("key", "value2"));

            InMemoryMetricsCollector.MetricSummary summary = new InMemoryMetricsCollector.MetricSummary(metric1);

            // Act
            summary.update(metric2);

            // Assert
            assertEquals(2, summary.getCount());
            assertEquals(300.0, summary.getSum());
            assertEquals(100.0, summary.getMin());
            assertEquals(200.0, summary.getMax());
            assertEquals(150.0, summary.getAverage());
            assertEquals(time1, summary.getFirstSeen());
            assertEquals(time2, summary.getLastSeen());
        }

        @Test
        @DisplayName("Should throw exception for metric name mismatch")
        void shouldThrowExceptionForMetricNameMismatch() {
            // Arrange
            Metric metric1 = Metric.timer("test.timer1", 100, Map.of("key", "value"));
            Metric metric2 = Metric.timer("test.timer2", 200, Map.of("key", "value"));

            InMemoryMetricsCollector.MetricSummary summary = new InMemoryMetricsCollector.MetricSummary(metric1);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> summary.update(metric2));

            assertTrue(exception.getMessage().contains("Metric name mismatch"));
        }

        @Test
        @DisplayName("Should handle zero average for empty summary")
        void shouldHandleZeroAverageForEmptySummary() {
            // Arrange
            Metric metric = Metric.timer("test.timer", 100, Map.of("key", "value"));
            InMemoryMetricsCollector.MetricSummary summary = new InMemoryMetricsCollector.MetricSummary(metric);

            // Manually set count to 0 to test edge case
            java.lang.reflect.Field countField;
            try {
                countField = InMemoryMetricsCollector.MetricSummary.class.getDeclaredField("count");
                countField.setAccessible(true);
                countField.set(summary, 0L);
            } catch (Exception e) {
                fail("Could not access count field for testing");
            }

            // Act & Assert
            assertEquals(0.0, summary.getAverage());
        }

        @Test
        @DisplayName("Should have meaningful toString representation")
        void shouldHaveMeaningfulToString() {
            // Arrange
            Metric metric = Metric.timer("test.timer", 100, Map.of("key", "value"));
            InMemoryMetricsCollector.MetricSummary summary = new InMemoryMetricsCollector.MetricSummary(metric);

            // Act
            String result = summary.toString();

            // Assert
            assertTrue(result.contains("MetricSummary"));
            assertTrue(result.contains("test.timer"));
            assertTrue(result.contains("TIMER"));
            assertTrue(result.contains("count=1"));
        }
    }

    @Test
    @DisplayName("Should use default constructor with ApplicationConfig")
    void shouldUseDefaultConstructorWithApplicationConfig() {
        // This test verifies the default constructor works with ApplicationConfig
        // The setup in @BeforeEach already resets ApplicationConfig, so this should work

        // Act & Assert
        assertDoesNotThrow(() -> new InMemoryMetricsCollector());
    }
}
package com.skanga.conductor.retry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for RetryContext class.
 *
 * Tests the retry context functionality including:
 * - Context creation and initialization
 * - Recording success and failure attempts
 * - Thread safety for concurrent operations
 * - Timing and elapsed time calculations
 * - Attempt history tracking
 * - AttemptRecord functionality
 * - Edge cases and boundary conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryContext Tests")
class RetryContextTest {

    @Mock
    private RetryPolicy mockRetryPolicy;

    private RetryContext retryContext;

    @BeforeEach
    void setUp() {
        retryContext = new RetryContext(mockRetryPolicy);
    }

    @Test
    @DisplayName("Should initialize context with correct default values")
    void shouldInitializeContextWithDefaultValues() {
        // When
        RetryContext context = new RetryContext(mockRetryPolicy);

        // Then
        assertEquals(mockRetryPolicy, context.getRetryPolicy());
        assertEquals(0, context.getAttemptCount());
        assertTrue(context.isFirstAttempt());
        assertFalse(context.hasSucceeded());
        assertNull(context.getLastException());
        assertEquals(0, context.getFailureCount());
        assertTrue(context.getAttempts().isEmpty());
        assertNotNull(context.getStartTime());
        assertTrue(context.getElapsedTime().toNanos() >= 0);
    }

    @Test
    @DisplayName("Should record single failure attempt correctly")
    void shouldRecordSingleFailureAttempt() {
        // Given
        RuntimeException testException = new RuntimeException("Test failure");

        // When
        retryContext.recordFailure(testException);

        // Then
        assertEquals(1, retryContext.getAttemptCount());
        assertEquals(1, retryContext.getFailureCount());
        assertFalse(retryContext.isFirstAttempt());
        assertFalse(retryContext.hasSucceeded());
        assertEquals(testException, retryContext.getLastException());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(1, attempts.size());

        RetryContext.AttemptRecord attempt = attempts.get(0);
        assertEquals(1, attempt.getAttemptNumber());
        assertFalse(attempt.isSuccess());
        assertEquals(testException, attempt.getException());
        assertNotNull(attempt.getTimestamp());
    }

    @Test
    @DisplayName("Should record single success attempt correctly")
    void shouldRecordSingleSuccessAttempt() {
        // When
        retryContext.recordSuccess();

        // Then
        assertEquals(1, retryContext.getAttemptCount());
        assertEquals(0, retryContext.getFailureCount());
        assertFalse(retryContext.isFirstAttempt());
        assertTrue(retryContext.hasSucceeded());
        assertNull(retryContext.getLastException());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(1, attempts.size());

        RetryContext.AttemptRecord attempt = attempts.get(0);
        assertEquals(1, attempt.getAttemptNumber());
        assertTrue(attempt.isSuccess());
        assertNull(attempt.getException());
        assertNotNull(attempt.getTimestamp());
    }

    @Test
    @DisplayName("Should record multiple failure attempts correctly")
    void shouldRecordMultipleFailureAttempts() {
        // Given
        RuntimeException exception1 = new RuntimeException("First failure");
        RuntimeException exception2 = new RuntimeException("Second failure");
        RuntimeException exception3 = new RuntimeException("Third failure");

        // When
        retryContext.recordFailure(exception1);
        retryContext.recordFailure(exception2);
        retryContext.recordFailure(exception3);

        // Then
        assertEquals(3, retryContext.getAttemptCount());
        assertEquals(3, retryContext.getFailureCount());
        assertFalse(retryContext.hasSucceeded());
        assertEquals(exception3, retryContext.getLastException());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(3, attempts.size());

        assertEquals(1, attempts.get(0).getAttemptNumber());
        assertEquals(2, attempts.get(1).getAttemptNumber());
        assertEquals(3, attempts.get(2).getAttemptNumber());

        assertEquals(exception1, attempts.get(0).getException());
        assertEquals(exception2, attempts.get(1).getException());
        assertEquals(exception3, attempts.get(2).getException());
    }

    @Test
    @DisplayName("Should record mixed success and failure attempts correctly")
    void shouldRecordMixedAttempts() {
        // Given
        RuntimeException exception1 = new RuntimeException("First failure");
        RuntimeException exception2 = new RuntimeException("Second failure");

        // When
        retryContext.recordFailure(exception1);
        retryContext.recordFailure(exception2);
        retryContext.recordSuccess();

        // Then
        assertEquals(3, retryContext.getAttemptCount());
        assertEquals(2, retryContext.getFailureCount());
        assertTrue(retryContext.hasSucceeded());
        assertEquals(exception2, retryContext.getLastException()); // Last failure, not last attempt

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(3, attempts.size());

        assertFalse(attempts.get(0).isSuccess());
        assertFalse(attempts.get(1).isSuccess());
        assertTrue(attempts.get(2).isSuccess());
    }

    @Test
    @DisplayName("Should track elapsed time accurately")
    void shouldTrackElapsedTimeAccurately() throws InterruptedException {
        // Given
        Instant startTime = retryContext.getStartTime();

        // When
        Thread.sleep(10); // Small delay to ensure time progression
        Duration elapsedTime = retryContext.getElapsedTime();

        // Then
        assertTrue(elapsedTime.toMillis() >= 0);
        assertTrue(elapsedTime.toMillis() >= 5); // Should be at least 5ms
        assertTrue(startTime.isBefore(Instant.now()));
    }

    @Test
    @DisplayName("Should handle null exceptions gracefully")
    void shouldHandleNullExceptionsGracefully() {
        // When
        retryContext.recordFailure(null);

        // Then
        assertEquals(1, retryContext.getAttemptCount());
        assertEquals(1, retryContext.getFailureCount());
        assertNull(retryContext.getLastException());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(1, attempts.size());
        assertNull(attempts.get(0).getException());
        assertFalse(attempts.get(0).isSuccess());
    }

    @Test
    @DisplayName("Should return immutable attempts list")
    void shouldReturnImmutableAttemptsList() {
        // Given
        retryContext.recordFailure(new RuntimeException("Test"));

        // When
        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();

        // Then
        assertThrows(UnsupportedOperationException.class, () -> {
            attempts.add(new RetryContext.AttemptRecord(999, Instant.now(), true, null));
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            attempts.remove(0);
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            attempts.clear();
        });
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent operations")
    void shouldBeThreadSafeForConcurrentOperations() throws InterruptedException {
        // Given
        int threadCount = 10;
        int attemptsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Exception> exception = new AtomicReference<>();

        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < attemptsPerThread; j++) {
                        if (j % 2 == 0) {
                            retryContext.recordFailure(new RuntimeException("Thread " + threadId + " attempt " + j));
                        } else {
                            retryContext.recordSuccess();
                        }
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(exception.get());

        int expectedTotalAttempts = threadCount * attemptsPerThread;
        assertEquals(expectedTotalAttempts, retryContext.getAttemptCount());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(expectedTotalAttempts, attempts.size());

        // Verify attempt numbers are sequential
        for (int i = 0; i < attempts.size(); i++) {
            assertEquals(i + 1, attempts.get(i).getAttemptNumber());
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should generate meaningful toString representation")
    void shouldGenerateMeaningfulToString() {
        // Given
        retryContext.recordFailure(new IllegalArgumentException("Test exception"));
        retryContext.recordSuccess();

        // When
        String toString = retryContext.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("RetryContext"));
        assertTrue(toString.contains("attempts=2"));
        assertTrue(toString.contains("elapsed="));
        assertTrue(toString.contains("lastException=IllegalArgumentException"));
    }

    @Test
    @DisplayName("Should handle toString with no exceptions")
    void shouldHandleToStringWithNoExceptions() {
        // Given
        retryContext.recordSuccess();

        // When
        String toString = retryContext.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("RetryContext"));
        assertTrue(toString.contains("attempts=1"));
        assertTrue(toString.contains("lastException=none"));
    }

    @Test
    @DisplayName("Should test AttemptRecord creation and properties")
    void shouldTestAttemptRecordCreationAndProperties() {
        // Given
        int attemptNumber = 3;
        Instant timestamp = Instant.now();
        RuntimeException exception = new RuntimeException("Test exception");

        // When
        RetryContext.AttemptRecord failureRecord = new RetryContext.AttemptRecord(
            attemptNumber, timestamp, false, exception
        );
        RetryContext.AttemptRecord successRecord = new RetryContext.AttemptRecord(
            attemptNumber + 1, timestamp.plusMillis(100), true, null
        );

        // Then
        assertEquals(attemptNumber, failureRecord.getAttemptNumber());
        assertEquals(timestamp, failureRecord.getTimestamp());
        assertFalse(failureRecord.isSuccess());
        assertEquals(exception, failureRecord.getException());

        assertEquals(attemptNumber + 1, successRecord.getAttemptNumber());
        assertEquals(timestamp.plusMillis(100), successRecord.getTimestamp());
        assertTrue(successRecord.isSuccess());
        assertNull(successRecord.getException());
    }

    @Test
    @DisplayName("Should test AttemptRecord toString method")
    void shouldTestAttemptRecordToString() {
        // Given
        RuntimeException exception = new RuntimeException("Test");
        RetryContext.AttemptRecord failureRecord = new RetryContext.AttemptRecord(
            1, Instant.now(), false, exception
        );
        RetryContext.AttemptRecord successRecord = new RetryContext.AttemptRecord(
            2, Instant.now(), true, null
        );

        // When
        String failureString = failureRecord.toString();
        String successString = successRecord.toString();

        // Then
        assertTrue(failureString.contains("Attempt{#1"));
        assertTrue(failureString.contains("FAILURE"));
        assertTrue(failureString.contains("RuntimeException"));

        assertTrue(successString.contains("Attempt{#2"));
        assertTrue(successString.contains("SUCCESS"));
    }

    @Test
    @DisplayName("Should correctly identify last exception with mixed attempts")
    void shouldCorrectlyIdentifyLastExceptionWithMixedAttempts() {
        // Given
        RuntimeException exception1 = new RuntimeException("First");
        RuntimeException exception2 = new RuntimeException("Second");

        // When
        retryContext.recordFailure(exception1);
        retryContext.recordSuccess();
        retryContext.recordFailure(exception2);
        retryContext.recordSuccess();

        // Then
        assertEquals(exception2, retryContext.getLastException());
        assertEquals(4, retryContext.getAttemptCount());
        assertEquals(2, retryContext.getFailureCount());
        assertTrue(retryContext.hasSucceeded());
    }

    @Test
    @DisplayName("Should handle edge case with only success attempts")
    void shouldHandleEdgeCaseWithOnlySuccessAttempts() {
        // When
        retryContext.recordSuccess();
        retryContext.recordSuccess();
        retryContext.recordSuccess();

        // Then
        assertNull(retryContext.getLastException());
        assertEquals(3, retryContext.getAttemptCount());
        assertEquals(0, retryContext.getFailureCount());
        assertTrue(retryContext.hasSucceeded());
    }

    @Test
    @DisplayName("Should validate context state consistency")
    void shouldValidateContextStateConsistency() {
        // Given
        RuntimeException exception1 = new RuntimeException("Exception 1");
        RuntimeException exception2 = new RuntimeException("Exception 2");

        // When
        retryContext.recordFailure(exception1);
        retryContext.recordFailure(exception2);
        retryContext.recordSuccess();

        // Then
        assertEquals(3, retryContext.getAttemptCount());
        assertEquals(2, retryContext.getFailureCount());
        assertTrue(retryContext.hasSucceeded());
        assertEquals(exception2, retryContext.getLastException());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(3, attempts.size());

        // Verify consistency between attempts list and context state
        long actualFailureCount = attempts.stream().filter(a -> !a.isSuccess()).count();
        assertEquals(retryContext.getFailureCount(), actualFailureCount);

        boolean actualHasSucceeded = attempts.stream().anyMatch(RetryContext.AttemptRecord::isSuccess);
        assertEquals(retryContext.hasSucceeded(), actualHasSucceeded);
    }

    @Test
    @DisplayName("Should handle very high attempt counts")
    void shouldHandleVeryHighAttemptCounts() {
        // Given
        int attemptCount = 1000;

        // When
        for (int i = 0; i < attemptCount; i++) {
            if (i % 2 == 0) {
                retryContext.recordFailure(new RuntimeException("Attempt " + i));
            } else {
                retryContext.recordSuccess();
            }
        }

        // Then
        assertEquals(attemptCount, retryContext.getAttemptCount());
        assertEquals(attemptCount / 2, retryContext.getFailureCount());
        assertTrue(retryContext.hasSucceeded());

        List<RetryContext.AttemptRecord> attempts = retryContext.getAttempts();
        assertEquals(attemptCount, attempts.size());

        // Verify last attempt number
        assertEquals(attemptCount, attempts.get(attempts.size() - 1).getAttemptNumber());
    }
}
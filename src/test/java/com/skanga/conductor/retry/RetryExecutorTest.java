package com.skanga.conductor.retry;

import com.skanga.conductor.config.ApplicationConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RetryExecutor class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryExecutor Unit Tests")
class RetryExecutorTest {

    @Mock
    private RetryPolicy mockPolicy;

    @Mock
    private RetryContext mockContext;

    private RetryExecutor retryExecutor;

    @BeforeEach
    void setUp() {
        ApplicationConfig.resetInstance();
        retryExecutor = new RetryExecutor(mockPolicy, "test-operation");
    }

    @AfterEach
    void tearDown() {
        ApplicationConfig.resetInstance();
    }

    @Test
    @DisplayName("Should successfully execute callable on first attempt")
    void shouldExecuteCallableSuccessfully() throws Exception {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockContext.getAttemptCount()).thenReturn(1);

        Callable<String> operation = () -> "success";

        // Act
        String result = retryExecutor.execute(operation);

        // Assert
        assertEquals("success", result);
        verify(mockContext).recordSuccess();
    }

    @Test
    @DisplayName("Should successfully execute supplier on first attempt")
    void shouldExecuteSupplierSuccessfully() {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockContext.getAttemptCount()).thenReturn(1);

        Supplier<String> operation = () -> "success";

        // Act
        String result = retryExecutor.execute(operation);

        // Assert
        assertEquals("success", result);
        verify(mockContext).recordSuccess();
    }

    @Test
    @DisplayName("Should retry on transient failure and eventually succeed")
    void shouldRetryOnTransientFailureAndSucceed() throws Exception {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockContext.getAttemptCount()).thenReturn(1, 2);
        when(mockPolicy.shouldRetry(mockContext)).thenReturn(true);
        when(mockPolicy.getRetryDelay(mockContext)).thenReturn(Duration.ZERO);

        AtomicInteger attemptCount = new AtomicInteger(0);
        Callable<String> operation = () -> {
            if (attemptCount.incrementAndGet() == 1) {
                throw new RuntimeException("Transient failure");
            }
            return "success";
        };

        // Act
        String result = retryExecutor.execute(operation);

        // Assert
        assertEquals("success", result);
        verify(mockContext).recordFailure(any(RuntimeException.class));
        verify(mockContext).recordSuccess();
        verify(mockPolicy).shouldRetry(mockContext);
    }

    @Test
    @DisplayName("Should fail after exhausting retries")
    void shouldFailAfterExhaustingRetries() {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockContext.getAttemptCount()).thenReturn(3);
        when(mockPolicy.shouldRetry(mockContext)).thenReturn(false);

        Supplier<String> operation = () -> {
            throw new RuntimeException("Persistent failure");
        };

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> retryExecutor.execute(operation));

        assertEquals("Persistent failure", exception.getMessage());
        verify(mockContext).recordFailure(any(RuntimeException.class));
        verify(mockContext, never()).recordSuccess();
    }

    @Test
    @DisplayName("Should handle checked exceptions in callable")
    void shouldHandleCheckedExceptionsInCallable() {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockPolicy.shouldRetry(mockContext)).thenReturn(false);

        Callable<String> operation = () -> {
            throw new Exception("Checked exception");
        };

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
            () -> retryExecutor.execute(operation));

        assertEquals("Checked exception", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle interruption during retry delay")
    void shouldHandleInterruptionDuringRetryDelay() {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockPolicy.shouldRetry(mockContext)).thenReturn(true);
        when(mockPolicy.getRetryDelay(mockContext)).thenReturn(Duration.ofSeconds(10));

        Supplier<String> operation = () -> {
            throw new RuntimeException("Failure");
        };

        // Interrupt the current thread before execution
        Thread.currentThread().interrupt();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> retryExecutor.execute(operation));

        assertEquals("Retry operation was interrupted", exception.getMessage());
        assertTrue(Thread.interrupted()); // Clear interrupt status
    }

    @Test
    @DisplayName("Should use NoRetryPolicy when configured")
    void shouldUseNoRetryPolicy() throws Exception {
        // Arrange
        NoRetryPolicy noRetryPolicy = NoRetryPolicy.INSTANCE;
        RetryExecutor executor = new RetryExecutor(noRetryPolicy, "no-retry-test");

        AtomicInteger attemptCount = new AtomicInteger(0);
        Callable<String> operation = () -> {
            if (attemptCount.incrementAndGet() == 1) {
                throw new RuntimeException("First attempt fails");
            }
            return "success";
        };

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> executor.execute(operation));

        assertTrue(exception.getMessage().contains("First attempt fails"));
        assertEquals(1, attemptCount.get());
    }

    @Test
    @DisplayName("Should use FixedDelayRetryPolicy")
    void shouldUseFixedDelayRetryPolicy() throws Exception {
        // Arrange
        FixedDelayRetryPolicy fixedDelayPolicy = new FixedDelayRetryPolicy(3, Duration.ZERO);
        RetryExecutor executor = new RetryExecutor(fixedDelayPolicy, "fixed-delay-test");

        AtomicInteger attemptCount = new AtomicInteger(0);
        Callable<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                throw new IOException("Attempt " + attempt + " fails");
            }
            return "success";
        };

        // Act
        String result = executor.execute(operation);

        // Assert
        assertEquals("success", result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    @DisplayName("Should use ExponentialBackoffRetryPolicy")
    void shouldUseExponentialBackoffRetryPolicy() throws Exception {
        // Arrange
        ExponentialBackoffRetryPolicy backoffPolicy = new ExponentialBackoffRetryPolicy(
            3, Duration.ZERO, Duration.ofMillis(10), 1.5);
        RetryExecutor executor = new RetryExecutor(backoffPolicy, "backoff-test");

        AtomicInteger attemptCount = new AtomicInteger(0);
        Callable<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 1) {
                throw new IOException("Attempt " + attempt + " fails");
            }
            return "success";
        };

        // Act
        String result = executor.execute(operation);

        // Assert
        assertEquals("success", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    @DisplayName("Should return correct policy and operation name")
    void shouldReturnCorrectPolicyAndOperationName() {
        // Act & Assert
        assertEquals(mockPolicy, retryExecutor.getRetryPolicy());
        assertEquals("test-operation", retryExecutor.getOperationName());
    }

    @Test
    @DisplayName("Should create executor using factory method")
    void shouldCreateExecutorUsingFactoryMethod() {
        // Act
        RetryExecutor executor = RetryExecutor.create(mockPolicy, "factory-test");

        // Assert
        assertNotNull(executor);
        assertEquals(mockPolicy, executor.getRetryPolicy());
        assertEquals("factory-test", executor.getOperationName());
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToString() {
        // Act
        String result = retryExecutor.toString();

        // Assert
        assertTrue(result.contains("RetryExecutor"));
        assertTrue(result.contains("test-operation"));
        assertTrue(result.contains(mockPolicy.toString()));
    }

    @Test
    @DisplayName("Should unwrap RuntimeException containing checked exception")
    void shouldUnwrapRuntimeExceptionContainingCheckedException() {
        // Arrange
        when(mockPolicy.createContext()).thenReturn(mockContext);
        when(mockPolicy.shouldRetry(mockContext)).thenReturn(false);

        Exception checkedException = new Exception("Original checked exception");
        Supplier<String> operation = () -> {
            throw new RuntimeException(checkedException);
        };

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
            () -> retryExecutor.execute(operation));

        assertEquals("Unexpected checked exception", exception.getMessage());
        assertSame(checkedException, exception.getCause());
    }

    @Test
    @DisplayName("Should handle null operation gracefully")
    void shouldHandleNullOperationGracefully() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> retryExecutor.execute((Callable<String>) null));

        assertThrows(NullPointerException.class,
            () -> retryExecutor.execute((Supplier<String>) null));
    }
}
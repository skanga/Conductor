package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExceptionContext.Builder Concurrency Tests")
class ExceptionContextBuilderConcurrencyTest extends ConductorTestBase {

    private static final int THREAD_COUNT = 20;
    private static final int OPERATIONS_PER_THREAD = 100;

    @Test
    @DisplayName("Concurrent builder usage should be thread-safe")
    void testConcurrentBuilderUsage() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ExceptionContext>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit concurrent builder tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    ExceptionContext.Builder builder = ExceptionContext.builder();

                    // Build context with multiple concurrent operations
                    ExceptionContext context = builder
                            .errorCode("TEST_ERROR_" + threadId)
                            .category(ExceptionContext.ErrorCategory.SYSTEM_ERROR)
                            .operation("test-operation-" + threadId)
                            .timestamp(Instant.now())
                            .correlationId("corr-" + threadId)
                            .recoveryHint(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF)
                            .recoveryDetails("Test recovery details for thread " + threadId)
                            .duration((long) (threadId * 100))
                            .attempt(1, 3)
                            .metadata("threadId", threadId)
                            .metadata("testData", "data-" + threadId)
                            .build();

                    // Verify the context was built correctly
                    assertEquals("TEST_ERROR_" + threadId, context.getErrorCode());
                    assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR, context.getCategory());
                    assertEquals("test-operation-" + threadId, context.getOperation());
                    assertEquals("corr-" + threadId, context.getCorrelationId());
                    assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF, context.getRecoveryHint());
                    assertEquals(Long.valueOf(threadId * 100), context.getDuration());
                    assertEquals(Integer.valueOf(threadId), context.getMetadata("threadId"));

                    successCount.incrementAndGet();
                    return context;
                } catch (Exception e) {
                    fail("Builder thread " + threadId + " failed: " + e.getMessage());
                    return null;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all operations succeeded
        assertEquals(THREAD_COUNT, successCount.get());

        // Verify all contexts were built correctly
        for (int i = 0; i < futures.size(); i++) {
            ExceptionContext context = futures.get(i).get();
            assertNotNull(context, "Context " + i + " should not be null");
            assertTrue(context.getErrorCode().startsWith("TEST_ERROR_"), "Error code should be correct");
        }
    }

    @Test
    @DisplayName("Shared builder instance should handle concurrent access safely")
    void testSharedBuilderConcurrentAccess() throws Exception {
        // Create a single builder instance to be shared across threads
        ExceptionContext.Builder sharedBuilder = ExceptionContext.builder();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ExceptionContext>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);

        // Submit concurrent operations on the same builder
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Multiple threads modifying the same builder
                    ExceptionContext context = sharedBuilder
                            .errorCode("SHARED_ERROR_" + threadId)
                            .category(ExceptionContext.ErrorCategory.NETWORK)
                            .operation("shared-operation-" + threadId)
                            .correlationId("shared-corr-" + threadId)
                            .metadata("sharedThreadId", threadId)
                            .build();

                    // The context should be valid (though we can't predict exact values due to race conditions)
                    assertNotNull(context, "Context should not be null");
                    assertNotNull(context.getErrorCode(), "Error code should not be null");
                    assertNotNull(context.getCategory(), "Category should not be null");

                    return context;
                } catch (Exception e) {
                    fail("Shared builder thread " + threadId + " failed: " + e.getMessage());
                    return null;
                }
            }));
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all contexts were created successfully
        for (Future<ExceptionContext> future : futures) {
            ExceptionContext context = future.get();
            assertNotNull(context, "All contexts should be non-null");
        }
    }

    @Test
    @DisplayName("Concurrent metadata operations should be thread-safe")
    void testConcurrentMetadataOperations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Integer>> futures = new ArrayList<>();
        AtomicInteger totalOperations = new AtomicInteger(0);

        // Submit concurrent metadata manipulation tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    int operationCount = 0;
                    ExceptionContext.Builder builder = ExceptionContext.builder();

                    // Perform many metadata operations
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        builder.metadata("key" + j, "value" + threadId + "_" + j);

                        // Occasionally add bulk metadata
                        if (j % 10 == 0) {
                            Map<String, Object> bulkMetadata = new HashMap<>();
                            bulkMetadata.put("bulk_key_" + j, "bulk_value_" + threadId);
                            bulkMetadata.put("bulk_timestamp", Instant.now().toString());
                            builder.metadata(bulkMetadata);
                        }

                        operationCount++;
                        totalOperations.incrementAndGet();
                    }

                    // Build the final context
                    ExceptionContext context = builder
                            .errorCode("METADATA_TEST_" + threadId)
                            .category(ExceptionContext.ErrorCategory.VALIDATION)
                            .build();

                    // Verify the context was built successfully
                    assertNotNull(context, "Context should not be null");
                    assertFalse(context.getMetadata().isEmpty(), "Metadata should not be empty");

                    return operationCount;
                } catch (Exception e) {
                    fail("Metadata thread " + threadId + " failed: " + e.getMessage());
                    return 0;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all operations completed
        int expectedOperations = THREAD_COUNT * OPERATIONS_PER_THREAD;
        assertEquals(expectedOperations, totalOperations.get());

        // Verify all threads completed successfully
        for (Future<Integer> future : futures) {
            Integer operationCount = future.get();
            assertEquals(Integer.valueOf(OPERATIONS_PER_THREAD), operationCount);
        }
    }

    @Test
    @DisplayName("Builder state should remain consistent under concurrent access")
    void testBuilderStateConsistency() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Test different patterns of builder usage
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    // Pattern 1: Build immediately after each setter
                    ExceptionContext.Builder builder1 = ExceptionContext.builder();
                    ExceptionContext context1 = builder1
                            .errorCode("IMMEDIATE_" + threadId)
                            .build();
                    assertNotNull(context1);
                    assertEquals("IMMEDIATE_" + threadId, context1.getErrorCode());

                    // Pattern 2: Multiple setters then build
                    ExceptionContext.Builder builder2 = ExceptionContext.builder();
                    builder2.errorCode("BATCH_" + threadId);
                    builder2.category(ExceptionContext.ErrorCategory.TIMEOUT);
                    builder2.operation("batch-operation");
                    builder2.metadata("batchId", threadId);
                    ExceptionContext context2 = builder2.build();

                    assertNotNull(context2);
                    assertEquals("BATCH_" + threadId, context2.getErrorCode());
                    assertEquals(ExceptionContext.ErrorCategory.TIMEOUT, context2.getCategory());

                    // Pattern 3: Reuse builder (this should work since each build creates a new context)
                    ExceptionContext.Builder builder3 = ExceptionContext.builder();
                    ExceptionContext context3a = builder3
                            .errorCode("REUSE_A_" + threadId)
                            .category(ExceptionContext.ErrorCategory.NETWORK)
                            .build();

                    ExceptionContext context3b = builder3
                            .errorCode("REUSE_B_" + threadId)
                            .category(ExceptionContext.ErrorCategory.AUTHENTICATION)
                            .build();

                    // Both contexts should be valid but potentially have different values
                    assertNotNull(context3a);
                    assertNotNull(context3b);

                    return true;
                } catch (Exception e) {
                    fail("State consistency thread " + threadId + " failed: " + e.getMessage());
                    return false;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all patterns succeeded
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "All builder patterns should succeed");
        }
    }

    @Test
    @DisplayName("Concurrent exception context creation should handle all field types")
    void testConcurrentAllFieldTypes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ExceptionContext>> futures = new ArrayList<>();

        // Test all possible field combinations concurrently
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    Instant now = Instant.now();

                    ExceptionContext context = ExceptionContext.builder()
                            .errorCode("FULL_TEST_" + threadId)
                            .category(ExceptionContext.ErrorCategory.values()[threadId % ExceptionContext.ErrorCategory.values().length])
                            .operation("full-operation-" + threadId)
                            .timestamp(now)
                            .correlationId("full-corr-" + threadId)
                            .recoveryHint(ExceptionContext.RecoveryHint.values()[threadId % ExceptionContext.RecoveryHint.values().length])
                            .recoveryDetails("Full recovery details for thread " + threadId)
                            .duration((long) threadId * 50)
                            .attempt(threadId % 5 + 1, 5)
                            .metadata("fullTestId", threadId)
                            .metadata("timestamp", now.toString())
                            .metadata("category", "full-test")
                            .build();

                    // Verify all fields are set correctly
                    assertEquals("FULL_TEST_" + threadId, context.getErrorCode());
                    assertNotNull(context.getCategory());
                    assertEquals("full-operation-" + threadId, context.getOperation());
                    assertEquals(now, context.getTimestamp());
                    assertEquals("full-corr-" + threadId, context.getCorrelationId());
                    assertNotNull(context.getRecoveryHint());
                    assertEquals("Full recovery details for thread " + threadId, context.getRecoveryDetails());
                    assertEquals(Long.valueOf(threadId * 50), context.getDuration());
                    assertEquals(Integer.valueOf(threadId % 5 + 1), context.getAttemptNumber());
                    assertEquals(Integer.valueOf(5), context.getMaxAttempts());
                    assertEquals(Integer.valueOf(threadId), context.getMetadata("fullTestId"));

                    return context;
                } catch (Exception e) {
                    fail("Full field test thread " + threadId + " failed: " + e.getMessage());
                    return null;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all contexts were created correctly
        for (Future<ExceptionContext> future : futures) {
            ExceptionContext context = future.get();
            assertNotNull(context, "All contexts should be created successfully");
            assertTrue(context.getErrorCode().startsWith("FULL_TEST_"), "Error code should be correct");
        }
    }
}
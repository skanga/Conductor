package com.skanga.conductor.config;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApplicationConfig Concurrency Tests")
class ApplicationConfigConcurrencyTest extends ConductorTestBase {

    private static final int THREAD_COUNT = 20;
    private static final int OPERATIONS_PER_THREAD = 50;

    @BeforeEach
    void setUp() {
        // Reset any existing system properties that might affect configuration loading
        System.clearProperty("conductor.profile");
        System.clearProperty("CONDUCTOR_PROFILE");
        ApplicationConfig.resetInstance();
    }

    @Test
    @DisplayName("Concurrent ApplicationConfig singleton access should be thread-safe")
    void testConcurrentSingletonAccess() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ApplicationConfig>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(THREAD_COUNT);

        // Submit concurrent singleton access tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Get instance multiple times
                    ApplicationConfig firstCall = ApplicationConfig.getInstance();
                    ApplicationConfig secondCall = ApplicationConfig.getInstance();

                    // Verify same instance returned
                    assertSame(firstCall, secondCall, "Multiple calls should return same instance");

                    return firstCall;
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                    return null;
                } finally {
                    finishLatch.countDown();
                }
            }));
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        assertTrue(finishLatch.await(30, TimeUnit.SECONDS));

        // Verify all threads got the same instance
        ApplicationConfig firstInstance = futures.get(0).get();
        assertNotNull(firstInstance, "First instance should not be null");

        for (Future<ApplicationConfig> future : futures) {
            ApplicationConfig instance = future.get();
            assertNotNull(instance, "Instance should not be null");
            assertSame(firstInstance, instance, "All threads should get the same singleton instance");
        }
    }

    @Test
    @DisplayName("Concurrent profile loading should be thread-safe")
    void testConcurrentProfileLoading() throws Exception {
        // Set profile before any instance is created
        System.setProperty("conductor.profile", "test");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<String>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit concurrent configuration access tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                try {
                    // Wait for synchronized start
                    startLatch.await();

                    // Access configuration that triggers profile loading
                    ApplicationConfig config = ApplicationConfig.getInstance();

                    // Access various configuration properties to stress test
                    String jdbcUrl = config.getDatabaseConfig().getJdbcUrl();
                    int memoryLimit = config.getMemoryConfig().getDefaultMemoryLimit();
                    String openAiModel = config.getLLMConfig().getOpenAiModel();
                    boolean metricsEnabled = config.getMetricsConfig().isEnabled();

                    // Verify non-null responses
                    assertNotNull(jdbcUrl, "JDBC URL should not be null");
                    assertTrue(memoryLimit > 0, "Memory limit should be positive");
                    assertNotNull(openAiModel, "OpenAI model should not be null");
                    assertNotNull(metricsEnabled, "Metrics enabled should not be null");

                    successCount.incrementAndGet();
                    return "success-" + Thread.currentThread().getName();
                } catch (Exception e) {
                    fail("Profile loading thread failed: " + e.getMessage());
                    return "failed";
                }
            }));
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all operations succeeded
        assertEquals(THREAD_COUNT, successCount.get(), "All profile loading operations should succeed");

        // Verify all futures completed successfully
        for (Future<String> future : futures) {
            String result = future.get();
            assertTrue(result.startsWith("success-"), "Each thread should succeed: " + result);
        }
    }

    @Test
    @DisplayName("Concurrent configuration property access should be thread-safe")
    void testConcurrentPropertyAccess() throws Exception {
        // Ensure instance is created
        ApplicationConfig config = ApplicationConfig.getInstance();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Void>> futures = new ArrayList<>();
        AtomicInteger operationCount = new AtomicInteger(0);

        // Submit concurrent property access tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // Access different types of properties concurrently
                        switch (j % 8) {
                            case 0:
                                config.getDatabaseConfig().getJdbcUrl();
                                break;
                            case 1:
                                config.getMemoryConfig().getDefaultMemoryLimit();
                                break;
                            case 2:
                                config.getLLMConfig().getOpenAiModel();
                                break;
                            case 3:
                                config.getToolConfig().getCodeRunnerTimeout();
                                break;
                            case 4:
                                config.getMetricsConfig().isEnabled();
                                break;
                            case 5:
                                config.getParallelismConfig().getMaxThreads();
                                break;
                            case 6:
                                config.getString("conductor.test.property", "default");
                                break;
                            case 7:
                                config.getBoolean("conductor.test.boolean", true);
                                break;
                        }
                        operationCount.incrementAndGet();
                    }
                    return null;
                } catch (Exception e) {
                    fail("Property access thread " + threadId + " failed: " + e.getMessage());
                    return null;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all operations completed
        assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, operationCount.get(),
                "All property access operations should complete");

        // Verify all futures completed without exceptions
        for (Future<Void> future : futures) {
            assertDoesNotThrow(() -> future.get(), "No thread should throw exceptions");
        }
    }

    @Test
    @DisplayName("Concurrent configuration access should be thread-safe")
    void testConcurrentConfigurationAccess() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger accessCount = new AtomicInteger(0);

        // Submit multiple configuration access operations without problematic resets
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        // Access configuration from threads - test thread safety of access
                        ApplicationConfig config = ApplicationConfig.getInstance();
                        assertNotNull(config, "Config should never be null");
                        config.getDatabaseConfig().getJdbcUrl();
                        config.getLLMConfig().getOpenAiModel();
                        config.getMemoryConfig().getDefaultMemoryLimit();
                        accessCount.incrementAndGet();

                        // Small yield to encourage thread switching
                        Thread.yield();
                    }
                    return true;
                } catch (Exception e) {
                    fail("Access thread " + threadId + " failed: " + e.getMessage());
                    return false;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify operations completed without errors
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "All access operations should succeed");
        }

        // Verify we had concurrent accesses
        assertTrue(accessCount.get() > 0, "Access operations should have occurred");
        assertEquals(THREAD_COUNT * 10, accessCount.get(), "All access operations should complete");

        logger.info("Concurrent access test completed: {} accesses", accessCount.get());
    }

    @Test
    @DisplayName("Concurrent external configuration loading should be thread-safe")
    void testConcurrentExternalConfigLoading() throws Exception {
        // Set different config paths for different threads to test concurrent loading
        System.setProperty("config", "nonexistent-config.properties");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ApplicationConfig>> futures = new ArrayList<>();

        // Submit concurrent configuration loading tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                try {
                    // This will trigger external config loading attempt
                    ApplicationConfig config = ApplicationConfig.getInstance();

                    // Verify config is usable despite external config failure
                    assertNotNull(config.getDatabaseConfig().getJdbcUrl());
                    assertNotNull(config.getLLMConfig().getOpenAiModel());

                    return config;
                } catch (Exception e) {
                    fail("External config loading thread failed: " + e.getMessage());
                    return null;
                }
            }));
        }

        // Wait for completion
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify all threads got valid configurations
        ApplicationConfig firstInstance = futures.get(0).get();
        assertNotNull(firstInstance);

        for (Future<ApplicationConfig> future : futures) {
            ApplicationConfig config = future.get();
            assertNotNull(config, "All configs should be valid");
            assertSame(firstInstance, config, "All configs should be the same singleton instance");
        }
    }
}
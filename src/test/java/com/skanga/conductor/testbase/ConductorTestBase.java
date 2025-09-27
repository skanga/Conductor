package com.skanga.conductor.testbase;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.metrics.MetricsRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base test class providing common setup and cleanup functionality for all Conductor tests.
 * <p>
 * This class handles the fundamental test lifecycle operations that are needed across
 * all test categories, including configuration reset, resource cleanup, and basic
 * test utilities.
 * </p>
 * <p>
 * Features provided:
 * </p>
 * <ul>
 * <li>ApplicationConfig singleton reset before/after each test</li>
 * <li>MetricsRegistry reset for clean test isolation</li>
 * <li>Automatic resource cleanup tracking and execution</li>
 * <li>Common assertion utilities</li>
 * <li>Logging setup for test debugging</li>
 * </ul>
 *
 * @since 2.0.0
 */
public abstract class ConductorTestBase {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * List of resources that need cleanup after test execution.
     * Resources are cleaned up in reverse order to handle dependencies correctly.
     */
    private final List<AutoCloseable> testResources = new ArrayList<>();

    @BeforeEach
    void baseSetUp() throws Exception {
        logger.debug("Setting up test: {}", getClass().getSimpleName());

        // Reset singleton instances for clean test isolation
        ApplicationConfig.resetInstance();
        MetricsRegistry.resetInstance();

        // Perform test-specific setup
        doSetUp();
    }

    @AfterEach
    void baseTearDown() throws Exception {
        logger.debug("Tearing down test: {}", getClass().getSimpleName());

        try {
            // Perform test-specific cleanup
            doTearDown();
        } finally {
            // Clean up registered resources in reverse order
            cleanupTestResources();

            // Reset singletons again for extra safety
            ApplicationConfig.resetInstance();
            MetricsRegistry.resetInstance();
        }
    }

    /**
     * Hook for test-specific setup. Override this method to perform custom setup.
     * This is called after base setup is complete.
     *
     * @throws Exception if setup fails
     */
    protected void doSetUp() throws Exception {
        // Default: no additional setup
    }

    /**
     * Hook for test-specific cleanup. Override this method to perform custom cleanup.
     * This is called before base cleanup.
     *
     * @throws Exception if cleanup fails
     */
    protected void doTearDown() throws Exception {
        // Default: no additional cleanup
    }

    /**
     * Registers a resource for automatic cleanup after the test.
     * Resources are closed in reverse order of registration.
     *
     * @param resource the resource to register for cleanup
     * @param <T> the type of the resource
     * @return the same resource for convenient chaining
     */
    protected <T extends AutoCloseable> T registerForCleanup(T resource) {
        if (resource != null) {
            testResources.add(resource);
        }
        return resource;
    }

    /**
     * Creates a test-specific unique identifier.
     *
     * @return a unique identifier for this test
     */
    protected String getTestId() {
        return getClass().getSimpleName() + "-" + System.currentTimeMillis();
    }

    /**
     * Creates a test-specific unique identifier with a custom suffix.
     *
     * @param suffix custom suffix to append
     * @return a unique identifier for this test
     */
    protected String getTestId(String suffix) {
        return getTestId() + "-" + suffix;
    }

    /**
     * Asserts that a runnable throws an exception of the specified type.
     *
     * @param expectedType the expected exception type
     * @param runnable the code that should throw the exception
     * @param <T> the exception type
     * @return the thrown exception for further assertions
     */
    protected <T extends Throwable> T assertThrowsType(Class<T> expectedType, Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " but no exception was thrown");
        } catch (Throwable thrown) {
            if (expectedType.isInstance(thrown)) {
                return expectedType.cast(thrown);
            } else {
                throw new AssertionError("Expected " + expectedType.getSimpleName() +
                                       " but got " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage(), thrown);
            }
        }
    }

    /**
     * Waits for a condition to become true within a timeout period.
     *
     * @param condition the condition to wait for
     * @param timeoutMs timeout in milliseconds
     * @param checkIntervalMs interval between checks in milliseconds
     * @return true if condition became true, false if timed out
     */
    protected boolean waitForCondition(BooleanSupplier condition, long timeoutMs, long checkIntervalMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Waits for a condition to become true within a default timeout of 5 seconds.
     *
     * @param condition the condition to wait for
     * @return true if condition became true, false if timed out
     */
    protected boolean waitForCondition(BooleanSupplier condition) {
        return waitForCondition(condition, 5000, 100);
    }

    /**
     * Functional interface for boolean suppliers.
     */
    @FunctionalInterface
    protected interface BooleanSupplier {
        boolean getAsBoolean();
    }

    /**
     * Cleanup all registered test resources.
     */
    private void cleanupTestResources() {
        // Clean up in reverse order to handle dependencies
        for (int i = testResources.size() - 1; i >= 0; i--) {
            AutoCloseable resource = testResources.get(i);
            try {
                resource.close();
            } catch (Exception e) {
                logger.warn("Failed to cleanup test resource: {}", e.getMessage(), e);
            }
        }
        testResources.clear();
    }
}
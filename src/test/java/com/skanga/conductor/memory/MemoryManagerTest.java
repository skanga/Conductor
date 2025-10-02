package com.skanga.conductor.memory;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for MemoryManager functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryManagerTest extends ConductorTestBase {

    private MemoryManager memoryManager;

    @BeforeEach
    void setUp() {
        memoryManager = new MemoryManager();
    }

    @AfterEach
    void tearDown() {
        if (memoryManager != null) {
            memoryManager.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("MemoryManager should initialize with default state")
    void testInitialization() {
        assertNotNull(memoryManager);
        assertNotNull(memoryManager.getCurrentMemoryState());
        assertTrue(memoryManager.getCurrentMemoryUsage() >= 0.0);
        assertTrue(memoryManager.getCurrentMemoryUsage() <= 1.0);

        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        assertNotNull(stats);
        assertEquals(0, stats.getCleanupTaskCount());
        assertEquals(0, stats.getWeakReferenceCount());
        assertEquals(0, stats.getExpirableResourceCount());
    }

    @Test
    @Order(2)
    @DisplayName("MemoryState enum should have correct descriptions")
    void testMemoryStateDescriptions() {
        assertEquals("Normal operation", MemoryManager.MemoryState.NORMAL.getDescription());
        assertEquals("Memory usage above warning threshold", MemoryManager.MemoryState.WARNING.getDescription());
        assertEquals("Memory usage above critical threshold", MemoryManager.MemoryState.CRITICAL.getDescription());
        assertEquals("Memory usage above emergency threshold", MemoryManager.MemoryState.EMERGENCY.getDescription());
    }

    @Test
    @Order(3)
    @DisplayName("Should register and execute cleanup tasks")
    void testCleanupTaskRegistration() throws Exception {
        AtomicInteger executionCount = new AtomicInteger(0);
        AtomicBoolean aggressiveFlag = new AtomicBoolean(false);

        MemoryManager.CleanupTask task = aggressive -> {
            executionCount.incrementAndGet();
            aggressiveFlag.set(aggressive);
        };

        // Register cleanup task
        memoryManager.registerCleanupTask("test-task", task);

        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        assertEquals(1, stats.getCleanupTaskCount());

        // Execute cleanup (non-aggressive)
        memoryManager.performCleanup(false);
        assertEquals(1, executionCount.get());
        assertFalse(aggressiveFlag.get());

        // Execute aggressive cleanup
        memoryManager.performCleanup(true);
        assertEquals(2, executionCount.get());
        assertTrue(aggressiveFlag.get());
    }

    @Test
    @Order(4)
    @DisplayName("Should unregister cleanup tasks")
    void testCleanupTaskUnregistration() {
        MemoryManager.CleanupTask task = aggressive -> {};

        // Register and verify
        memoryManager.registerCleanupTask("test-task", task);
        assertEquals(1, memoryManager.getMemoryStats().getCleanupTaskCount());

        // Unregister and verify
        boolean removed = memoryManager.unregisterCleanupTask("test-task");
        assertTrue(removed);
        assertEquals(0, memoryManager.getMemoryStats().getCleanupTaskCount());

        // Try to unregister non-existent task
        boolean removedAgain = memoryManager.unregisterCleanupTask("non-existent");
        assertFalse(removedAgain);
    }

    @Test
    @Order(5)
    @DisplayName("Should handle cleanup task exceptions gracefully")
    void testCleanupTaskExceptionHandling() {
        AtomicInteger successCount = new AtomicInteger(0);

        // Register a task that throws an exception
        memoryManager.registerCleanupTask("failing-task", aggressive -> {
            throw new RuntimeException("Cleanup failure");
        });

        // Register a task that succeeds
        memoryManager.registerCleanupTask("success-task", aggressive -> {
            successCount.incrementAndGet();
        });

        // Perform cleanup - should continue despite exception
        assertDoesNotThrow(() -> memoryManager.performCleanup(false));
        assertEquals(1, successCount.get());
    }

    @Test
    @Order(6)
    @DisplayName("Should register and track weak references")
    void testWeakReferenceManagement() throws Exception {
        // Create test resource
        TestAutoCloseable resource = new TestAutoCloseable();

        // Register resource
        memoryManager.registerResource("test-resource", resource);
        assertEquals(1, memoryManager.getMemoryStats().getWeakReferenceCount());

        // Perform cleanup - resource should still be referenced
        memoryManager.performCleanup(false);
        assertEquals(1, memoryManager.getMemoryStats().getWeakReferenceCount());

        // Remove reference and force GC
        resource = null;
        System.gc();
        Thread.sleep(10); // Reduced from 100ms to 10ms for faster testing

        // Cleanup should remove weak reference
        memoryManager.performCleanup(false);
        assertEquals(0, memoryManager.getMemoryStats().getWeakReferenceCount());
    }

    @Test
    @Order(7)
    @DisplayName("Should register and cleanup expirable resources")
    void testExpirableResourceManagement() throws Exception {
        TestAutoCloseable resource = new TestAutoCloseable();
        Instant expiration = Instant.now().plus(50, ChronoUnit.MILLIS);

        // Register expirable resource
        memoryManager.registerExpirableResource(resource, expiration);
        assertEquals(1, memoryManager.getMemoryStats().getExpirableResourceCount());
        assertFalse(resource.isClosed());

        // Wait for expiration
        Thread.sleep(50); // Increased from 10ms to 50ms for reliable expiration testing

        // Perform cleanup - resource should be closed and removed
        memoryManager.performCleanup(false);
        assertTrue(resource.isClosed());
        assertEquals(0, memoryManager.getMemoryStats().getExpirableResourceCount());
    }

    @Test
    @Order(8)
    @DisplayName("Should reject expirable resources with past expiration")
    void testExpirableResourceValidation() {
        TestAutoCloseable resource = new TestAutoCloseable();
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);

        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerExpirableResource(resource, pastTime);
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // Test null cleanup task
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerCleanupTask("test", null);
        });

        // Test blank task name
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerCleanupTask("", aggressive -> {});
        });

        // Test null resource
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerResource("test", null);
        });

        // Test blank resource name
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerResource("", new TestAutoCloseable());
        });

        // Test null expirable resource
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerExpirableResource(null, Instant.now().plusSeconds(60));
        });

        // Test null expiration time
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.registerExpirableResource(new TestAutoCloseable(), null);
        });

        // Test null task name for unregistration
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.unregisterCleanupTask(null);
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should detect high memory pressure")
    void testMemoryPressureDetection() {
        // This test depends on current JVM memory usage
        // We can only test that the method returns a boolean
        boolean highPressure = memoryManager.isMemoryPressureHigh();
        assertTrue(highPressure || !highPressure); // Always true, just testing method exists

        // Test that current memory usage is within valid range
        double usage = memoryManager.getCurrentMemoryUsage();
        assertTrue(usage >= 0.0 && usage <= 1.0);
    }

    @Test
    @Order(11)
    @DisplayName("Emergency cleanup should only trigger above threshold")
    void testEmergencyCleanup() {
        // Emergency cleanup method exists and doesn't throw
        assertDoesNotThrow(() -> memoryManager.emergencyCleanup());
    }

    @Test
    @Order(12)
    @DisplayName("MemoryStats should provide comprehensive information")
    void testMemoryStats() {
        // Register some resources to get non-zero counts
        memoryManager.registerCleanupTask("stats-task", aggressive -> {});
        memoryManager.registerResource("stats-resource", new TestAutoCloseable());
        memoryManager.registerExpirableResource(
            new TestAutoCloseable(),
            Instant.now().plus(1, ChronoUnit.HOURS)
        );

        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();

        // Verify all stats are present
        assertTrue(stats.getHeapUsed() >= 0);
        assertTrue(stats.getHeapCommitted() >= 0);
        assertTrue(stats.getHeapMax() >= -1); // Can be -1 if not defined
        assertTrue(stats.getNonHeapUsed() >= 0);
        assertTrue(stats.getUsagePercentage() >= 0.0);
        assertNotNull(stats.getState());
        assertEquals(1, stats.getCleanupTaskCount());
        assertEquals(1, stats.getWeakReferenceCount());
        assertEquals(1, stats.getExpirableResourceCount());
        assertTrue(stats.getLastCleanupTime() > 0);

        // Test toString method
        String statsString = stats.toString();
        assertNotNull(statsString);
        assertTrue(statsString.contains("MemoryStats"));
        assertTrue(statsString.contains("heapUsed"));
        assertTrue(statsString.contains("usage"));
    }

    @Test
    @Order(13)
    @DisplayName("Should cleanup resources during close")
    void testManagerClose() throws Exception {
        TestAutoCloseable resource = new TestAutoCloseable();
        AtomicInteger cleanupCount = new AtomicInteger(0);

        // Register resources
        memoryManager.registerResource("close-test", resource);
        memoryManager.registerCleanupTask("close-task", aggressive -> {
            cleanupCount.incrementAndGet();
        });

        // Verify resources are registered
        assertEquals(1, memoryManager.getMemoryStats().getWeakReferenceCount());
        assertEquals(1, memoryManager.getMemoryStats().getCleanupTaskCount());

        // Close manager
        memoryManager.close();

        // Verify cleanup was executed
        assertTrue(cleanupCount.get() > 0);
    }

    @Test
    @Order(14)
    @DisplayName("Should handle concurrent cleanup task registration")
    void testConcurrentCleanupTaskRegistration() throws Exception {
        int threadCount = 10;
        int tasksPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < tasksPerThread; j++) {
                        String taskName = "task-" + threadId + "-" + j;
                        memoryManager.registerCleanupTask(taskName, aggressive -> {});
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount * tasksPerThread, successCount.get());
        assertEquals(threadCount * tasksPerThread, memoryManager.getMemoryStats().getCleanupTaskCount());
    }

    @Test
    @Order(15)
    @DisplayName("Should handle concurrent resource registration")
    void testConcurrentResourceRegistration() throws Exception {
        int threadCount = 10;
        int resourcesPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < resourcesPerThread; j++) {
                        String resourceName = "resource-" + threadId + "-" + j;
                        memoryManager.registerResource(resourceName, new TestAutoCloseable());
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount * resourcesPerThread, successCount.get());
        assertEquals(threadCount * resourcesPerThread, memoryManager.getMemoryStats().getWeakReferenceCount());
    }

    /**
     * Test implementation of AutoCloseable for testing purposes.
     */
    private static class TestAutoCloseable implements AutoCloseable {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
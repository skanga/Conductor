package com.skanga.conductor.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SingletonHolder functionality.
 */
class SingletonHolderTest {

    private static class TestClass {
        private static final AtomicInteger instanceCount = new AtomicInteger(0);
        private final int instanceId;

        public TestClass() {
            instanceId = instanceCount.incrementAndGet();
        }

        public int getInstanceId() {
            return instanceId;
        }

        public static int getInstanceCount() {
            return instanceCount.get();
        }

        public static void resetCount() {
            instanceCount.set(0);
        }
    }

    private static class TestClassWithAutoCloseable implements AutoCloseable {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    @BeforeEach
    void setUp() {
        TestClass.resetCount();
    }

    @AfterEach
    void tearDown() {
        TestClass.resetCount();
    }

    @Test
    void testBasicSingletonBehavior() {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);

        // First call should create instance
        TestClass instance1 = holder.get();
        assertNotNull(instance1);
        assertEquals(1, instance1.getInstanceId());
        assertEquals(1, TestClass.getInstanceCount());

        // Second call should return same instance
        TestClass instance2 = holder.get();
        assertSame(instance1, instance2);
        assertEquals(1, TestClass.getInstanceCount());
    }

    @Test
    void testThreadSafety() throws Exception {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        List<AtomicReference<TestClass>> instances = new ArrayList<>(numThreads);

        // Initialize list
        for (int i = 0; i < numThreads; i++) {
            instances.add(new AtomicReference<>());
        }

        // Create threads that will all try to get instance simultaneously
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    TestClass instance = holder.get();
                    instances.get(threadIndex).set(instance);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();
        doneLatch.await();

        // Verify all threads got the same instance
        TestClass firstInstance = instances.get(0).get();
        assertNotNull(firstInstance);
        for (int i = 1; i < numThreads; i++) {
            assertSame(firstInstance, instances.get(i).get());
        }

        // Verify only one instance was created
        assertEquals(1, TestClass.getInstanceCount());
    }

    @Test
    void testResetFunctionality() {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);

        // Create first instance
        TestClass instance1 = holder.get();
        assertEquals(1, instance1.getInstanceId());
        assertEquals(1, TestClass.getInstanceCount());

        // Reset the holder
        holder.reset();

        // Should create new instance
        TestClass instance2 = holder.get();
        assertEquals(2, instance2.getInstanceId());
        assertEquals(2, TestClass.getInstanceCount());

        // Instances should be different
        assertNotSame(instance1, instance2);
    }

    @Test
    void testIsInitialized() {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);

        // Initially not initialized
        assertFalse(holder.isInitialized());

        // After getting instance, should be initialized
        holder.get();
        assertTrue(holder.isInitialized());

        // After reset, should not be initialized
        holder.reset();
        assertFalse(holder.isInitialized());
    }

    @Test
    void testGetCurrentInstance() {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);

        // Initially no current instance
        assertNull(holder.getCurrentInstance());

        // After getting instance, current instance should be available
        TestClass instance = holder.get();
        assertSame(instance, holder.getCurrentInstance());

        // After reset, no current instance
        holder.reset();
        assertNull(holder.getCurrentInstance());
    }

    @Test
    void testAutoCloseableSupport() {
        SingletonHolder<TestClassWithAutoCloseable> holder =
            SingletonHolder.of(TestClassWithAutoCloseable::new);

        // Create instance
        TestClassWithAutoCloseable instance = holder.get();
        assertFalse(instance.isClosed());

        // Reset should close the instance
        holder.reset();
        assertTrue(instance.isClosed());
    }

    @Test
    void testFactoryException() {
        // Factory that always throws exception
        SingletonHolder<TestClass> holder = SingletonHolder.of(() -> {
            throw new RuntimeException("Factory failure");
        });

        // Should throw SingletonInitializationException
        assertThrows(com.skanga.conductor.exception.SingletonException.InitializationException.class,
                     holder::get);
    }

    @Test
    void testFactoryReturnsNull() {
        // Factory that returns null
        SingletonHolder<TestClass> holder = SingletonHolder.of(() -> null);

        // Should throw SingletonInitializationException
        com.skanga.conductor.exception.SingletonException.InitializationException exception =
            assertThrows(com.skanga.conductor.exception.SingletonException.InitializationException.class,
                         holder::get);
        // Just check that we got the expected exception type
        assertNotNull(exception.getMessage());
    }

    @Test
    void testNullFactory() {
        // Should throw IllegalArgumentException for null factory
        assertThrows(IllegalArgumentException.class,
                     () -> SingletonHolder.of(null));
    }

    @Test
    void testToString() {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);

        // Before initialization
        String toStringBefore = holder.toString();
        assertTrue(toStringBefore.contains("SingletonHolder"));
        assertTrue(toStringBefore.contains("initialized=false"));

        // After initialization
        holder.get();
        String toStringAfter = holder.toString();
        assertTrue(toStringAfter.contains("SingletonHolder"));
        assertTrue(toStringAfter.contains("initialized=true"));
        assertTrue(toStringAfter.contains("TestClass"));
    }

    @Test
    void testConcurrentReset() throws Exception {
        SingletonHolder<TestClass> holder = SingletonHolder.of(TestClass::new);
        int numOperations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Create initial instance
        holder.get();

        // Submit mix of get and reset operations
        List<CompletableFuture<Void>> futures = new ArrayList<>(numOperations);
        for (int i = 0; i < numOperations; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (index % 3 == 0) {
                    holder.reset();
                } else {
                    holder.get();
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Should still be able to get a valid instance
        TestClass finalInstance = holder.get();
        assertNotNull(finalInstance);
    }

    @Test
    void testResetWithCloseException() {
        // Create holder with instance that throws on close
        SingletonHolder<AutoCloseable> holder = SingletonHolder.of(() -> () -> {
            throw new RuntimeException("Close failure");
        });

        // Create instance
        holder.get();

        // Reset should throw SingletonResetException
        assertThrows(com.skanga.conductor.exception.SingletonException.ResetException.class,
                     holder::reset);
    }

    @Test
    void testMultipleHolders() {
        // Test that different holders are independent
        SingletonHolder<TestClass> holder1 = SingletonHolder.of(TestClass::new);
        SingletonHolder<TestClass> holder2 = SingletonHolder.of(TestClass::new);

        TestClass instance1 = holder1.get();
        TestClass instance2 = holder2.get();

        // Should be different instances
        assertNotSame(instance1, instance2);
        assertEquals(2, TestClass.getInstanceCount());

        // Reset one shouldn't affect the other
        holder1.reset();
        assertFalse(holder1.isInitialized());
        assertTrue(holder2.isInitialized());
        assertSame(instance2, holder2.getCurrentInstance());
    }
}
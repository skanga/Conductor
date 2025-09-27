package com.skanga.conductor.utils;

import com.skanga.conductor.exception.SingletonException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Generic singleton holder that provides thread-safe lazy initialization
 * and proper lifecycle management for singleton instances.
 *
 * <p>This class uses the initialization-on-demand holder pattern combined
 * with additional safety features:</p>
 * <ul>
 * <li><strong>Thread Safety:</strong> Uses holder class pattern for performance</li>
 * <li><strong>Lazy Initialization:</strong> Instance created only when first accessed</li>
 * <li><strong>Reset Support:</strong> Allows singleton reset for testing scenarios</li>
 * <li><strong>Exception Safety:</strong> Handles initialization failures gracefully</li>
 * <li><strong>Memory Efficiency:</strong> No unnecessary volatile fields or synchronization in common path</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class MyService {
 *     private static final SingletonHolder<MyService> HOLDER =
 *         SingletonHolder.of(MyService::new);
 *
 *     public static MyService getInstance() {
 *         return HOLDER.get();
 *     }
 *
 *     public static void resetInstance() {
 *         HOLDER.reset();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of the singleton instance
 * @since 2.0.0
 */
public final class SingletonHolder<T> {

    private final Supplier<T> instanceFactory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean initialized = false;
    private volatile T instance;

    /**
     * Creates a new singleton holder with the given factory.
     *
     * @param instanceFactory function to create the singleton instance
     * @throws IllegalArgumentException if factory is null
     */
    private SingletonHolder(Supplier<T> instanceFactory) {
        if (instanceFactory == null) {
            throw new IllegalArgumentException("Instance factory cannot be null");
        }
        this.instanceFactory = instanceFactory;
    }

    /**
     * Creates a singleton holder with the given factory function.
     *
     * @param <T> the type of singleton
     * @param instanceFactory function to create the singleton instance
     * @return a new singleton holder
     * @throws IllegalArgumentException if factory is null
     */
    public static <T> SingletonHolder<T> of(Supplier<T> instanceFactory) {
        return new SingletonHolder<>(instanceFactory);
    }

    /**
     * Gets the singleton instance, creating it if necessary.
     * <p>
     * This method uses double-checked locking for optimal performance
     * while maintaining thread safety. The common case (instance already
     * initialized) requires no synchronization.
     * </p>
     *
     * @return the singleton instance
     * @throws SingletonException.InitializationException if instance creation fails
     */
    public T get() {
        if (!initialized) {
            lock.writeLock().lock();
            try {
                if (!initialized) {
                    instance = createInstance();
                    initialized = true;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return instance;
    }

    /**
     * Checks if the singleton instance has been initialized.
     *
     * @return true if the instance has been created, false otherwise
     */
    public boolean isInitialized() {
        lock.readLock().lock();
        try {
            return initialized;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Resets the singleton instance, allowing a new instance to be created
     * on the next call to {@link #get()}.
     * <p>
     * This method is primarily intended for testing scenarios where you need
     * to reset the singleton state. It should not be used in production code
     * under normal circumstances.
     * </p>
     * <p>
     * If the instance implements {@link AutoCloseable}, this method will
     * attempt to close it before resetting.
     * </p>
     *
     * @throws SingletonException.ResetException if the reset operation fails
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            if (initialized && instance != null) {
                // Try to cleanup the instance if it's closeable
                tryCleanupInstance(instance);
                instance = null;
                initialized = false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the current instance without creating it if it doesn't exist.
     *
     * @return the current instance, or null if not yet initialized
     */
    public T getCurrentInstance() {
        lock.readLock().lock();
        try {
            return initialized ? instance : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Creates the singleton instance using the provided factory.
     */
    private T createInstance() {
        try {
            T newInstance = instanceFactory.get();
            if (newInstance == null) {
                throw new SingletonException.InitializationException(
                    "Instance factory returned null");
            }
            return newInstance;
        } catch (Exception e) {
            throw new SingletonException.InitializationException(
                "Failed to create singleton instance", e);
        }
    }

    /**
     * Attempts to cleanup the instance if it implements AutoCloseable.
     */
    private void tryCleanupInstance(T instance) {
        if (instance instanceof AutoCloseable) {
            try {
                ((AutoCloseable) instance).close();
            } catch (Exception e) {
                throw new SingletonException.ResetException(
                    "Failed to cleanup singleton instance during reset", e);
            }
        }
    }


    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("SingletonHolder{initialized=%s, instance=%s}",
                initialized, initialized ? instance.getClass().getSimpleName() : "null");
        } finally {
            lock.readLock().unlock();
        }
    }
}
package com.skanga.conductor.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token bucket rate limiter for client-side API rate limiting.
 * <p>
 * This class implements a token bucket algorithm to limit the rate of API calls,
 * helping prevent exceeding provider quotas and controlling costs.
 * </p>
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Thread-safe token bucket implementation using atomic operations</li>
 * <li>Configurable bucket capacity and refill rate</li>
 * <li>Non-blocking tryAcquire for checking availability without waiting</li>
 * <li>Blocking acquire with timeout support</li>
 * <li>Automatic token refill based on elapsed time</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe and can be used concurrently.
 * </p>
 *
 * @since 2.0.0
 */
public class TokenBucketRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final long capacity;
    private final long refillRatePerSecond;
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTimestamp;

    /**
     * Creates a new token bucket rate limiter.
     *
     * @param capacity the maximum number of tokens the bucket can hold
     * @param refillRatePerSecond the number of tokens to add per second
     */
    public TokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("Refill rate must be positive");
        }

        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = new AtomicLong(capacity); // Start with full bucket
        this.lastRefillTimestamp = new AtomicLong(System.nanoTime());

        logger.debug("Initialized TokenBucketRateLimiter with capacity={}, refillRate={}/sec",
                    capacity, refillRatePerSecond);
    }

    /**
     * Attempts to acquire a token without blocking.
     *
     * @return true if a token was acquired, false if no tokens available
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Attempts to acquire the specified number of tokens without blocking.
     *
     * @param tokens the number of tokens to acquire
     * @return true if tokens were acquired, false if insufficient tokens available
     */
    public boolean tryAcquire(long tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Token count must be positive");
        }

        refill();

        // Try to atomically decrement tokens
        long current;
        long newValue;
        do {
            current = this.tokens.get();
            if (current < tokens) {
                logger.debug("Rate limit: insufficient tokens (requested={}, available={})", tokens, current);
                return false;
            }
            newValue = current - tokens;
        } while (!this.tokens.compareAndSet(current, newValue));

        logger.trace("Acquired {} token(s), remaining={}", tokens, newValue);
        return true;
    }

    /**
     * Acquires a token, blocking if necessary until one becomes available or timeout elapses.
     *
     * @param timeout maximum time to wait for a token
     * @return true if token acquired, false if timeout elapsed
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean acquire(Duration timeout) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadlineNanos) {
            if (tryAcquire()) {
                return true;
            }

            // Calculate time until next token refill
            long now = System.nanoTime();
            long elapsed = now - lastRefillTimestamp.get();
            long nanosPerToken = 1_000_000_000L / refillRatePerSecond;
            long nanosUntilNextToken = nanosPerToken - (elapsed % nanosPerToken);

            // Wait for next token, but no more than remaining timeout
            long waitNanos = Math.min(nanosUntilNextToken, deadlineNanos - now);
            if (waitNanos > 0) {
                Thread.sleep(waitNanos / 1_000_000, (int) (waitNanos % 1_000_000));
            }
        }

        logger.warn("Rate limit: timeout waiting for token");
        return false;
    }

    /**
     * Refills tokens based on elapsed time since last refill.
     */
    private void refill() {
        long now = System.nanoTime();
        long lastRefill = lastRefillTimestamp.get();
        long elapsed = now - lastRefill;

        // Calculate tokens to add based on elapsed time
        long tokensToAdd = (elapsed * refillRatePerSecond) / 1_000_000_000L;

        if (tokensToAdd > 0) {
            // Try to update the refill timestamp
            if (lastRefillTimestamp.compareAndSet(lastRefill, now)) {
                // Successfully updated timestamp, now add tokens
                long current;
                long newValue;
                do {
                    current = tokens.get();
                    newValue = Math.min(capacity, current + tokensToAdd);
                } while (!tokens.compareAndSet(current, newValue));

                if (tokensToAdd > 0) {
                    logger.trace("Refilled {} token(s), total={}", tokensToAdd, newValue);
                }
            }
        }
    }

    /**
     * Gets the current number of available tokens.
     * <p>
     * Note: This value may change immediately after being read due to concurrent access.
     * </p>
     *
     * @return the current token count
     */
    public long getAvailableTokens() {
        refill();
        return tokens.get();
    }

    /**
     * Gets the bucket capacity.
     *
     * @return the maximum number of tokens
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Gets the refill rate.
     *
     * @return the number of tokens added per second
     */
    public long getRefillRate() {
        return refillRatePerSecond;
    }
}

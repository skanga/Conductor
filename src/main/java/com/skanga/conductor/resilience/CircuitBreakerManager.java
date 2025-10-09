package com.skanga.conductor.resilience;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.ResilienceConfig;
import com.skanga.conductor.utils.SingletonHolder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages circuit breakers and resilience patterns for external service calls.
 * <p>
 * This singleton class provides centralized management of Resilience4j circuit breakers,
 * retry policies, and other resilience patterns for LLM providers, tools, and external services.
 * </p>
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Circuit breaker pattern to prevent cascading failures</li>
 * <li>Automatic retry with exponential backoff</li>
 * <li>Per-service circuit breaker configuration</li>
 * <li>Metrics and event monitoring</li>
 * </ul>
 *
 * @since 1.1.0
 */
public class CircuitBreakerManager {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerManager.class);

    private static final SingletonHolder<CircuitBreakerManager> HOLDER =
        SingletonHolder.of(CircuitBreakerManager::new);

    private final ResilienceConfig config;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, Retry> retries = new ConcurrentHashMap<>();

    private CircuitBreakerManager() {
        this.config = ApplicationConfig.getInstance().getResilienceConfig();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(createDefaultCircuitBreakerConfig());
        this.retryRegistry = RetryRegistry.of(createDefaultRetryConfig());

        logger.info("CircuitBreakerManager initialized - CB enabled: {}, Retry enabled: {}",
            config.isCircuitBreakerEnabled(), config.isRetryEnabled());
    }

    /**
     * Returns the singleton instance of the CircuitBreakerManager.
     *
     * @return the singleton instance
     */
    public static CircuitBreakerManager getInstance() {
        return HOLDER.get();
    }

    /**
     * Resets the singleton instance for testing purposes.
     */
    public static void resetInstance() {
        HOLDER.reset();
    }

    /**
     * Executes a supplier with circuit breaker and retry protection.
     *
     * @param serviceName the name of the service/operation
     * @param supplier the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws Exception if the operation fails after retries
     */
    public <T> T executeWithProtection(String serviceName, Supplier<T> supplier) throws Exception {
        if (!config.isCircuitBreakerEnabled() && !config.isRetryEnabled()) {
            // No protection enabled, execute directly
            return supplier.get();
        }

        Supplier<T> decoratedSupplier = supplier;

        // Apply circuit breaker if enabled
        if (config.isCircuitBreakerEnabled()) {
            CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
            decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, decoratedSupplier);
        }

        // Apply retry if enabled
        if (config.isRetryEnabled()) {
            Retry retry = getOrCreateRetry(serviceName);
            decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        }

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            logger.error("Operation failed for service '{}' after circuit breaker and retry: {}",
                serviceName, e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a runnable with circuit breaker and retry protection.
     *
     * @param serviceName the name of the service/operation
     * @param runnable the operation to execute
     * @throws Exception if the operation fails after retries
     */
    public void executeWithProtection(String serviceName, Runnable runnable) throws Exception {
        executeWithProtection(serviceName, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Gets or creates a circuit breaker for a specific service.
     *
     * @param serviceName the service name
     * @return the circuit breaker
     */
    public CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
        return circuitBreakers.computeIfAbsent(serviceName, name -> {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            registerCircuitBreakerEventListeners(cb);
            logger.info("Created circuit breaker for service '{}'", name);
            return cb;
        });
    }

    /**
     * Gets or creates a retry policy for a specific service.
     *
     * @param serviceName the service name
     * @return the retry policy
     */
    public Retry getOrCreateRetry(String serviceName) {
        return retries.computeIfAbsent(serviceName, name -> {
            Retry retry = retryRegistry.retry(name);
            registerRetryEventListeners(retry);
            logger.info("Created retry policy for service '{}'", name);
            return retry;
        });
    }

    /**
     * Gets the state of a circuit breaker.
     *
     * @param serviceName the service name
     * @return the circuit breaker state, or null if not exists
     */
    public CircuitBreaker.State getCircuitBreakerState(String serviceName) {
        CircuitBreaker cb = circuitBreakers.get(serviceName);
        return cb != null ? cb.getState() : null;
    }

    /**
     * Manually transitions a circuit breaker to a specific state.
     *
     * @param serviceName the service name
     * @param state the target state
     */
    public void transitionCircuitBreaker(String serviceName, CircuitBreaker.State state) {
        CircuitBreaker cb = circuitBreakers.get(serviceName);
        if (cb != null) {
            switch (state) {
                case CLOSED:
                    cb.transitionToClosedState();
                    logger.info("Manually closed circuit breaker for '{}'", serviceName);
                    break;
                case OPEN:
                    cb.transitionToOpenState();
                    logger.info("Manually opened circuit breaker for '{}'", serviceName);
                    break;
                case FORCED_OPEN:
                    cb.transitionToForcedOpenState();
                    logger.info("Manually forced open circuit breaker for '{}'", serviceName);
                    break;
                case DISABLED:
                    cb.transitionToDisabledState();
                    logger.info("Manually disabled circuit breaker for '{}'", serviceName);
                    break;
                default:
                    logger.warn("Cannot manually transition to state: {}", state);
            }
        }
    }

    /**
     * Resets a circuit breaker to its initial state.
     *
     * @param serviceName the service name
     */
    public void resetCircuitBreaker(String serviceName) {
        CircuitBreaker cb = circuitBreakers.get(serviceName);
        if (cb != null) {
            cb.reset();
            logger.info("Reset circuit breaker for '{}'", serviceName);
        }
    }

    /**
     * Creates the default circuit breaker configuration.
     *
     * @return the circuit breaker config
     */
    private CircuitBreakerConfig createDefaultCircuitBreakerConfig() {
        CircuitBreakerConfig.SlidingWindowType windowType =
            config.getCircuitBreakerSlidingWindowType().equals("COUNT_BASED")
                ? CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                : CircuitBreakerConfig.SlidingWindowType.TIME_BASED;

        return CircuitBreakerConfig.custom()
            .failureRateThreshold(config.getCircuitBreakerFailureRateThreshold())
            .slowCallRateThreshold(config.getCircuitBreakerSlowCallRateThreshold())
            .slowCallDurationThreshold(Duration.ofMillis(config.getCircuitBreakerSlowCallDurationThreshold()))
            .waitDurationInOpenState(Duration.ofMillis(config.getCircuitBreakerWaitDurationInOpenState()))
            .permittedNumberOfCallsInHalfOpenState(config.getCircuitBreakerPermittedCallsInHalfOpenState())
            .minimumNumberOfCalls(config.getCircuitBreakerMinimumNumberOfCalls())
            .slidingWindowType(windowType)
            .slidingWindowSize(config.getCircuitBreakerSlidingWindowSize())
            .build();
    }

    /**
     * Creates the default retry configuration.
     *
     * @return the retry config
     */
    private RetryConfig createDefaultRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(config.getRetryMaxAttempts())
            .waitDuration(Duration.ofMillis(config.getRetryWaitDuration()))
            .retryExceptions(
                Exception.class,
                RuntimeException.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class
            )
            .build();
    }

    /**
     * Registers event listeners for circuit breaker state changes.
     *
     * @param circuitBreaker the circuit breaker
     */
    private void registerCircuitBreakerEventListeners(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                logger.warn("Circuit breaker '{}' transitioned from {} to {}",
                    circuitBreaker.getName(),
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                logger.debug("Circuit breaker '{}' recorded error: {}",
                    circuitBreaker.getName(),
                    event.getThrowable().getMessage()))
            .onSuccess(event ->
                logger.trace("Circuit breaker '{}' recorded success (duration: {}ms)",
                    circuitBreaker.getName(),
                    event.getElapsedDuration().toMillis()))
            .onCallNotPermitted(event ->
                logger.warn("Circuit breaker '{}' rejected call - circuit is OPEN",
                    circuitBreaker.getName()));
    }

    /**
     * Registers event listeners for retry events.
     *
     * @param retry the retry policy
     */
    private void registerRetryEventListeners(Retry retry) {
        retry.getEventPublisher()
            .onRetry(event ->
                logger.warn("Retry '{}' attempt #{} after failure: {}",
                    retry.getName(),
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage()))
            .onSuccess(event ->
                logger.debug("Retry '{}' succeeded after {} attempts",
                    retry.getName(),
                    event.getNumberOfRetryAttempts()))
            .onError(event ->
                logger.error("Retry '{}' exhausted all {} attempts: {}",
                    retry.getName(),
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage()));
    }

    /**
     * Gets metrics for all circuit breakers.
     *
     * @return map of service names to their circuit breaker metrics
     */
    public Map<String, CircuitBreakerMetrics> getAllMetrics() {
        Map<String, CircuitBreakerMetrics> metrics = new ConcurrentHashMap<>();
        circuitBreakers.forEach((name, cb) -> {
            CircuitBreaker.Metrics cbMetrics = cb.getMetrics();
            metrics.put(name, new CircuitBreakerMetrics(
                cb.getState(),
                cbMetrics.getFailureRate(),
                cbMetrics.getSlowCallRate(),
                cbMetrics.getNumberOfSuccessfulCalls(),
                cbMetrics.getNumberOfFailedCalls(),
                cbMetrics.getNumberOfSlowCalls(),
                cbMetrics.getNumberOfNotPermittedCalls()
            ));
        });
        return metrics;
    }

    /**
     * Circuit breaker metrics snapshot.
     */
    public record CircuitBreakerMetrics(
        CircuitBreaker.State state,
        float failureRate,
        float slowCallRate,
        int numberOfSuccessfulCalls,
        int numberOfFailedCalls,
        int numberOfSlowCalls,
        long numberOfNotPermittedCalls
    ) {}
}

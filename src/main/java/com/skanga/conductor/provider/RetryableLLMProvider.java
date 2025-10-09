package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryExecutor;
import com.skanga.conductor.retry.RetryPolicy;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Decorator that adds retry logic to any LLMProvider implementation.
 * <p>
 * This decorator implements the Decorator pattern to add retry behavior
 * to LLM providers without forcing all providers to inherit retry logic
 * through an abstract base class. Providers can opt-in to retry behavior
 * by wrapping themselves with this decorator.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Configurable retry policies (no retry, fixed delay, exponential backoff)</li>
 * <li>Exception classification (retryable vs permanent failures)</li>
 * <li>Correlation ID tracking for request tracing</li>
 * <li>Duration tracking and metrics integration</li>
 * <li>Provider-specific exception handling</li>
 * <li>Thread-safe execution</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * // Create a provider without retry
 * LLMProvider baseProvider = new OpenAiLLMProvider(apiKey, model, endpoint);
 *
 * // Wrap with retry decorator using default retry policy
 * LLMProvider retryableProvider = new RetryableLLMProvider(baseProvider);
 *
 * // Or with custom retry policy
 * RetryPolicy customPolicy = RetryPolicy.exponentialBackoff(5, 1000, 2.0);
 * LLMProvider customRetryProvider = new RetryableLLMProvider(baseProvider, customPolicy);
 * }</pre>
 * <p>
 * Thread Safety: This class is thread-safe, assuming the wrapped provider
 * and retry executor are thread-safe.
 * </p>
 *
 * @since 1.1.0
 * @see LLMProvider
 * @see RetryPolicy
 * @see RetryExecutor
 */
public class RetryableLLMProvider implements LLMProvider {

    private final LLMProvider delegate;
    private final RetryExecutor retryExecutor;
    private final String providerName;
    private final ExceptionClassifier exceptionClassifier;

    /**
     * Creates a retryable provider with default retry policy.
     * <p>
     * Uses exponential backoff with 3 retries, 1 second initial delay,
     * 30 second max delay, and 2.0 multiplier.
     * </p>
     *
     * @param delegate the underlying LLM provider to add retry behavior to
     */
    public RetryableLLMProvider(LLMProvider delegate) {
        this(delegate, RetryPolicy.exponentialBackoff(
            3,
            java.time.Duration.ofSeconds(1),
            java.time.Duration.ofSeconds(30),
            2.0
        ));
    }

    /**
     * Creates a retryable provider with custom retry policy.
     *
     * @param delegate the underlying LLM provider to add retry behavior to
     * @param retryPolicy the retry policy to use
     */
    public RetryableLLMProvider(LLMProvider delegate, RetryPolicy retryPolicy) {
        this(delegate, retryPolicy, new DefaultExceptionClassifier());
    }

    /**
     * Creates a retryable provider with custom retry policy and exception classifier.
     *
     * @param delegate the underlying LLM provider to add retry behavior to
     * @param retryPolicy the retry policy to use
     * @param exceptionClassifier classifier to determine if exceptions are retryable
     */
    public RetryableLLMProvider(LLMProvider delegate, RetryPolicy retryPolicy,
                                ExceptionClassifier exceptionClassifier) {
        this.delegate = delegate;
        this.providerName = extractProviderName(delegate);
        this.retryExecutor = new RetryExecutor(retryPolicy, this.providerName + "-llm-call");
        this.exceptionClassifier = exceptionClassifier;
    }

    @Override
    public String generate(String prompt) throws ConductorException.LLMProviderException {
        final String correlationId = UUID.randomUUID().toString();
        final Instant startTime = Instant.now();
        final String operation = "generate_completion";

        try {
            return retryExecutor.execute((Supplier<String>) () -> {
                final long duration = System.currentTimeMillis() - startTime.toEpochMilli();

                try {
                    // Delegate to the wrapped provider
                    return delegate.generate(prompt);
                } catch (ConductorException.LLMProviderException e) {
                    // LLM provider exceptions are already standardized
                    if (exceptionClassifier.isRetryable(e)) {
                        throw new TransientLLMException(e.getMessage(), e);
                    } else {
                        throw new PermanentLLMException(e.getMessage(), e);
                    }
                } catch (Exception e) {
                    // Classify FIRST using original exception
                    boolean isRetryable = exceptionClassifier.isRetryable(e);

                    // Then create provider context for standardized exception creation
                    ProviderExceptionFactory.ProviderContext context =
                        ProviderExceptionFactory.ProviderContext.builder(providerName)
                            .model("unknown")  // Model information not available in decorator
                            .operation(operation)
                            .duration(duration)
                            .correlationId(correlationId)
                            .build();

                    // Use standardized exception factory
                    ConductorException.LLMProviderException standardizedException =
                        ProviderExceptionFactory.fromException(context, e);

                    // Wrap appropriately based on classification
                    if (isRetryable) {
                        throw new TransientLLMException(standardizedException.getMessage(), e);
                    } else {
                        throw new PermanentLLMException(standardizedException.getMessage(), e);
                    }
                }
            });
        } catch (TransientLLMException e) {
            // Recreate the standardized exception from the original cause
            Throwable cause = e.getCause();
            if (cause instanceof ConductorException.LLMProviderException) {
                throw (ConductorException.LLMProviderException) cause;
            }

            final long duration = System.currentTimeMillis() - startTime.toEpochMilli();
            ProviderExceptionFactory.ProviderContext context =
                ProviderExceptionFactory.ProviderContext.builder(providerName)
                    .model("unknown")
                    .operation(operation)
                    .duration(duration)
                    .correlationId(correlationId)
                    .build();
            throw ProviderExceptionFactory.fromException(context, (Exception) cause);
        } catch (PermanentLLMException e) {
            // Recreate the standardized exception from the original cause
            Throwable cause = e.getCause();
            if (cause instanceof ConductorException.LLMProviderException) {
                throw (ConductorException.LLMProviderException) cause;
            }

            final long duration = System.currentTimeMillis() - startTime.toEpochMilli();
            ProviderExceptionFactory.ProviderContext context =
                ProviderExceptionFactory.ProviderContext.builder(providerName)
                    .model("unknown")
                    .operation(operation)
                    .duration(duration)
                    .correlationId(correlationId)
                    .build();
            throw ProviderExceptionFactory.fromException(context, (Exception) cause);
        } catch (Exception e) {
            // Handle unexpected exceptions
            final long duration = System.currentTimeMillis() - startTime.toEpochMilli();
            ProviderExceptionFactory.ProviderContext context =
                ProviderExceptionFactory.ProviderContext.builder(providerName)
                    .model("unknown")
                    .operation(operation)
                    .duration(duration)
                    .correlationId(correlationId)
                    .build();

            throw ProviderExceptionFactory.fromException(context, e);
        }
    }

    /**
     * Extracts a meaningful provider name from the delegate.
     */
    private String extractProviderName(LLMProvider provider) {
        String className = provider.getClass().getSimpleName();
        // Remove "LLMProvider" suffix if present
        if (className.endsWith("LLMProvider")) {
            className = className.substring(0, className.length() - "LLMProvider".length());
        }
        return className.toLowerCase();
    }

    /**
     * Interface for classifying exceptions as retryable or permanent.
     */
    public interface ExceptionClassifier {
        /**
         * Determines if an exception should be retried.
         *
         * @param exception the exception to classify
         * @return true if the exception is transient and should be retried
         */
        boolean isRetryable(Exception exception);
    }

    /**
     * Default exception classifier using common patterns.
     */
    public static class DefaultExceptionClassifier implements ExceptionClassifier {
        @Override
        public boolean isRetryable(Exception exception) {
            // Non-retryable by default
            if (exception instanceof IllegalArgumentException) {
                return false;
            }

            String message = exception.getMessage();
            if (message == null) {
                return exception instanceof RuntimeException;
            }

            String lowerMessage = message.toLowerCase();

            // Explicit non-retryable patterns
            if (lowerMessage.contains("invalid") ||
                lowerMessage.contains("authentication") ||
                lowerMessage.contains("authorization") ||
                lowerMessage.contains("not found") ||
                lowerMessage.contains("malformed") ||
                lowerMessage.contains("permanent")) {
                return false;
            }

            // Common retryable patterns across LLM providers
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network") ||
                   lowerMessage.contains("rate limit") ||
                   lowerMessage.contains("too many requests") ||
                   lowerMessage.contains("server error") ||
                   lowerMessage.contains("service unavailable") ||
                   lowerMessage.contains("temporary") ||
                   lowerMessage.contains("502") ||
                   lowerMessage.contains("503") ||
                   lowerMessage.contains("504") ||
                   lowerMessage.contains("throttled") ||
                   lowerMessage.contains("overloaded") ||
                   lowerMessage.contains("capacity") ||
                   lowerMessage.contains("read timeout") ||
                   lowerMessage.contains("connect timeout") ||
                   lowerMessage.contains("socket timeout") ||
                   lowerMessage.contains("internal error") ||
                   lowerMessage.contains("gateway timeout") ||
                   lowerMessage.contains("simulated") ||
                   lowerMessage.contains("transient failure") ||
                   (exception instanceof RuntimeException);
        }
    }

    /**
     * Transient exception marker for retry logic.
     */
    private static class TransientLLMException extends RuntimeException {
        public TransientLLMException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Permanent exception marker for retry logic.
     */
    private static class PermanentLLMException extends RuntimeException {
        public PermanentLLMException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

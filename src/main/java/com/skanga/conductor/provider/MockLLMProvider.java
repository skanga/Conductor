package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.retry.RetryPolicy;

import java.util.Random;

/**
 * Deterministic mock provider for demo / testing.
 * <p>
 * This provider can optionally simulate failures and retry scenarios
 * for testing the retry logic in LLM operations.
 * </p>
 */
public class MockLLMProvider implements LLMProvider {

    private final String providerName;
    private final FailureSimulator failureSimulator;

    /**
     * Creates a mock LLM provider with the specified name.
     * <p>
     * The provider name is used to tag mock responses, making it easy to
     * identify which provider generated specific outputs during testing
     * and debugging scenarios.
     * </p>
     *
     * @param providerName the name to identify this provider instance
     */
    public MockLLMProvider(String providerName) {
        this.providerName = providerName;
        this.failureSimulator = null;
    }

    /**
     * Creates a mock LLM provider with failure simulation capabilities.
     * <p>
     * This constructor allows testing of retry logic by simulating
     * various failure scenarios.
     * </p>
     *
     * @param providerName the name to identify this provider instance
     * @param failureSimulator the failure simulator for testing retry logic
     */
    public MockLLMProvider(String providerName, FailureSimulator failureSimulator) {
        this.providerName = providerName;
        this.failureSimulator = failureSimulator;
    }

    @Override
    public String generate(String prompt) throws ConductorException.LLMProviderException {
        // Simulate failures if configured
        if (failureSimulator != null) {
            failureSimulator.maybeThrowException();
        }

        // simple deterministic response — in real use, swap in an OpenAI/Vertex provider.
        return String.format("[%s MOCK OUTPUT] Prompt length=%d. First 120 chars: %s",
                providerName, prompt == null ? 0 : prompt.length(),
                prompt == null ? "" : (prompt.length() > 120 ? prompt.substring(0, 120) + "..." : prompt));
    }

    /**
     * Interface for simulating failures in mock LLM providers.
     */
    public interface FailureSimulator {
        /**
         * Potentially throws an exception to simulate a failure.
         * This method is called before each LLM generation attempt.
         */
        void maybeThrowException() throws ConductorException.LLMProviderException;
    }

    /**
     * Failure simulator that fails a specified number of times before succeeding.
     */
    public static class FailFirstNAttemptsSimulator implements FailureSimulator {
        private final int failureCount;
        private int attemptCount = 0;
        private final String errorMessage;

        public FailFirstNAttemptsSimulator(int failureCount, String errorMessage) {
            this.failureCount = failureCount;
            this.errorMessage = errorMessage;
        }

        @Override
        public synchronized void maybeThrowException() throws ConductorException.LLMProviderException {
            attemptCount++;
            if (attemptCount <= failureCount) {
                throw new ConductorException.LLMProviderException(errorMessage + " (attempt " + attemptCount + ")");
            }
        }
    }

    /**
     * Failure simulator that fails with a specified probability.
     */
    public static class RandomFailureSimulator implements FailureSimulator {
        private final double failureProbability;
        private final String errorMessage;
        private final Random random = new Random();

        public RandomFailureSimulator(double failureProbability, String errorMessage) {
            this.failureProbability = failureProbability;
            this.errorMessage = errorMessage;
        }

        @Override
        public void maybeThrowException() throws ConductorException.LLMProviderException {
            if (random.nextDouble() < failureProbability) {
                throw new ConductorException.LLMProviderException(errorMessage);
            }
        }
    }
}

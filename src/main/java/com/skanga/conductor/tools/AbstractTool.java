package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.resilience.CircuitBreakerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for tools that provides circuit breaker protection.
 * <p>
 * This class wraps tool execution with Resilience4j circuit breaker and retry patterns,
 * providing automatic fault tolerance for external service calls and potentially failing operations.
 * </p>
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Circuit breaker protection to prevent cascading failures</li>
 * <li>Automatic retry with exponential backoff</li>
 * <li>Per-tool circuit breaker configuration</li>
 * <li>Comprehensive logging</li>
 * </ul>
 *
 * @since 1.1.0
 */
public abstract class AbstractTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTool.class);

    /**
     * Executes the tool with circuit breaker and retry protection.
     * <p>
     * This method wraps the actual tool execution ({@link #executeInternal(ExecutionInput)})
     * with circuit breaker protection. If circuit breaker is disabled in configuration,
     * it delegates directly to the internal method.
     * </p>
     *
     * @param input the execution input
     * @return the execution result
     * @throws Exception if tool execution fails after all retries
     */
    @Override
    public final ExecutionResult runTool(ExecutionInput input) throws Exception {
        final String serviceName = "tool-" + toolName();

        try {
            return CircuitBreakerManager.getInstance().executeWithProtection(
                serviceName,
                () -> {
                    try {
                        return executeInternal(input);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        } catch (RuntimeException e) {
            // Unwrap RuntimeException if it was thrown from the supplier
            if (e.getCause() instanceof Exception) {
                logger.error("Tool '{}' execution failed after circuit breaker and retry: {}",
                    toolName(), e.getCause().getMessage());
                throw (Exception) e.getCause();
            }
            logger.error("Tool '{}' execution failed after circuit breaker and retry: {}",
                toolName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Tool '{}' execution failed after circuit breaker and retry: {}",
                toolName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Performs the actual tool execution logic.
     * <p>
     * Concrete implementations must override this method to provide tool-specific functionality.
     * This method should not handle circuit breaker or retry logic, as that is managed by the base class.
     * </p>
     *
     * @param input the execution input
     * @return the execution result
     * @throws Exception if the tool execution fails
     */
    protected abstract ExecutionResult executeInternal(ExecutionInput input) throws Exception;
}

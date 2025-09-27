package com.skanga.conductor.exception;

/**
 * Exception thrown when singleton operations fail.
 * <p>
 * This exception encompasses various singleton-related failures including
 * initialization errors and reset operations that encounter problems.
 * </p>
 *
 * @since 1.0.0
 */
public class SingletonException extends ConductorRuntimeException {

    public SingletonException(String message) {
        super(message);
    }

    public SingletonException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when singleton initialization fails.
     */
    public static class InitializationException extends SingletonException {
        public InitializationException(String message) {
            super(message);
        }

        public InitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when singleton reset fails.
     */
    public static class ResetException extends SingletonException {
        public ResetException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
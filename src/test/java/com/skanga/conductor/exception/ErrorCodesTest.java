package com.skanga.conductor.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCodes Tests")
class ErrorCodesTest {

    @Test
    @DisplayName("Should provide correct category mapping for LLM auth errors")
    void testLLMAuthErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_AUTH_INVALID_KEY));
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_AUTH_EXPIRED_KEY));
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_AUTH_MISSING_KEY));
        assertEquals(ExceptionContext.ErrorCategory.AUTHENTICATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_AUTH_INSUFFICIENT_PERMISSIONS));
    }

    @Test
    @DisplayName("Should provide correct category mapping for LLM rate limit errors")
    void testLLMRateLimitErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_RATE_LIMIT_EXCEEDED));
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_RATE_QUOTA_EXCEEDED));
        assertEquals(ExceptionContext.ErrorCategory.RATE_LIMIT,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_RATE_CONCURRENT_LIMIT));
    }

    @Test
    @DisplayName("Should provide correct category mapping for timeout errors")
    void testTimeoutErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_TIMEOUT_REQUEST));
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_TIMEOUT_CONNECTION));
        assertEquals(ExceptionContext.ErrorCategory.TIMEOUT,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_TIMEOUT_READ));
    }

    @Test
    @DisplayName("Should provide correct category mapping for network errors")
    void testNetworkErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.NETWORK,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_NETWORK_CONNECTION_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.NETWORK,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_NETWORK_DNS_RESOLUTION));
        assertEquals(ExceptionContext.ErrorCategory.NETWORK,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_NETWORK_SSL_ERROR));
    }

    @Test
    @DisplayName("Should provide correct category mapping for validation errors")
    void testValidationErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.VALIDATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.TOOL_INPUT_INVALID));
        assertEquals(ExceptionContext.ErrorCategory.VALIDATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.TOOL_OUTPUT_INVALID));
        assertEquals(ExceptionContext.ErrorCategory.VALIDATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.VALIDATION_NULL_VALUE));
        assertEquals(ExceptionContext.ErrorCategory.VALIDATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.VALIDATION_INVALID_TYPE));
    }

    @Test
    @DisplayName("Should provide correct category mapping for configuration errors")
    void testConfigurationErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.CONFIGURATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.CONFIG_FILE_NOT_FOUND));
        assertEquals(ExceptionContext.ErrorCategory.CONFIGURATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.CONFIG_PROPERTY_MISSING));
        assertEquals(ExceptionContext.ErrorCategory.CONFIGURATION,
            ErrorCodes.getCategoryForCode(ErrorCodes.CONFIG_DATABASE_URL_INVALID));
    }

    @Test
    @DisplayName("Should provide correct category mapping for system errors")
    void testSystemErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.MEMORY_CONNECTION_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.MEMORY_STORAGE_CORRUPT));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.METRICS_COLLECTION_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.DATABASE_QUERY_FAILED));
    }

    @Test
    @DisplayName("Should provide correct category mapping for business logic errors")
    void testBusinessLogicErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.LLM_SERVICE_UNAVAILABLE));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.TOOL_EXECUTION_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.PLANNER_TASK_GENERATION_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.WORKFLOW_STAGE_FAILED));
    }

    @Test
    @DisplayName("Should provide correct category mapping for provider-specific errors")
    void testProviderSpecificErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.OPENAI_MODEL_OVERLOADED));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.ANTHROPIC_MODEL_UNAVAILABLE));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.AZURE_DEPLOYMENT_NOT_FOUND));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.BEDROCK_MODEL_NOT_AVAILABLE));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.GEMINI_API_KEY_INVALID));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.OLLAMA_CONNECTION_REFUSED));
    }

    @Test
    @DisplayName("Should provide correct category mapping for data format errors")
    void testDataFormatErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.DATA_FORMAT,
            ErrorCodes.getCategoryForCode(ErrorCodes.JSON_PARSE_ERROR));
        assertEquals(ExceptionContext.ErrorCategory.DATA_FORMAT,
            ErrorCodes.getCategoryForCode(ErrorCodes.YAML_PARSE_ERROR));
        assertEquals(ExceptionContext.ErrorCategory.DATA_FORMAT,
            ErrorCodes.getCategoryForCode(ErrorCodes.DATA_SERIALIZATION_FAILED));
    }

    @Test
    @DisplayName("Should provide correct category mapping for user interaction errors")
    void testUserInteractionErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.USER_INTERACTION,
            ErrorCodes.getCategoryForCode(ErrorCodes.APPROVAL_TIMEOUT));
        assertEquals(ExceptionContext.ErrorCategory.USER_INTERACTION,
            ErrorCodes.getCategoryForCode(ErrorCodes.APPROVAL_REJECTED));
        assertEquals(ExceptionContext.ErrorCategory.USER_INTERACTION,
            ErrorCodes.getCategoryForCode(ErrorCodes.APPROVAL_USER_NOT_FOUND));
    }

    @Test
    @DisplayName("Should provide correct category mapping for resource errors")
    void testResourceErrorCategories() {
        assertEquals(ExceptionContext.ErrorCategory.RESOURCE_UNAVAILABLE,
            ErrorCodes.getCategoryForCode(ErrorCodes.TOOL_RESOURCE_UNAVAILABLE));
        assertEquals(ExceptionContext.ErrorCategory.RESOURCE_UNAVAILABLE,
            ErrorCodes.getCategoryForCode(ErrorCodes.TOOL_RESOURCE_EXHAUSTED));
        assertEquals(ExceptionContext.ErrorCategory.RESOURCE_UNAVAILABLE,
            ErrorCodes.getCategoryForCode(ErrorCodes.TOOL_RESOURCE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("Should handle null and unknown error codes")
    void testNullAndUnknownErrorCodes() {
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(null));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode("UNKNOWN_ERROR_CODE"));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(""));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for authentication errors")
    void testAuthenticationRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.CHECK_CREDENTIALS,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_AUTH_INVALID_KEY));
        assertEquals(ExceptionContext.RecoveryHint.CHECK_CREDENTIALS,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_AUTH_EXPIRED_KEY));
        assertEquals(ExceptionContext.RecoveryHint.CHECK_CREDENTIALS,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.CONFIG_SECURITY_INVALID_CREDENTIALS));
        assertEquals(ExceptionContext.RecoveryHint.CHECK_CREDENTIALS,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.GEMINI_API_KEY_INVALID));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for rate limit errors")
    void testRateLimitRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.WAIT_RATE_LIMIT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_RATE_LIMIT_EXCEEDED));
        assertEquals(ExceptionContext.RecoveryHint.WAIT_RATE_LIMIT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_RATE_QUOTA_EXCEEDED));
        assertEquals(ExceptionContext.RecoveryHint.WAIT_RATE_LIMIT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.BEDROCK_THROTTLING_EXCEPTION));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for timeout errors")
    void testTimeoutRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_TIMEOUT_REQUEST));
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.TOOL_EXECUTION_TIMEOUT));
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.MEMORY_QUERY_TIMEOUT));
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.WORKFLOW_STAGE_TIMEOUT));
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.DATABASE_QUERY_TIMEOUT));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for network errors")
    void testNetworkRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_NETWORK_CONNECTION_FAILED));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_SERVICE_UNAVAILABLE));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OPENAI_MODEL_OVERLOADED));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.ANTHROPIC_MODEL_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for configuration errors")
    void testConfigurationRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.FIX_CONFIGURATION,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.CONFIG_FILE_NOT_FOUND));
        assertEquals(ExceptionContext.RecoveryHint.FIX_CONFIGURATION,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.CONFIG_PROPERTY_MISSING));
        assertEquals(ExceptionContext.RecoveryHint.FIX_CONFIGURATION,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.AZURE_DEPLOYMENT_NOT_FOUND));
        assertEquals(ExceptionContext.RecoveryHint.FIX_CONFIGURATION,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.WORKFLOW_DEFINITION_INVALID));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for validation errors")
    void testValidationRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.TOOL_INPUT_INVALID));
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.JSON_PARSE_ERROR));
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.VALIDATION_NULL_VALUE));
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OPENAI_CONTENT_POLICY_VIOLATION));
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OPENAI_CONTEXT_LENGTH_EXCEEDED));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for user action required")
    void testUserActionRequiredRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.APPROVAL_TIMEOUT));
        assertEquals(ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.APPROVAL_REJECTED));
        assertEquals(ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OPENAI_INSUFFICIENT_QUOTA));
        assertEquals(ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OCI_SERVICE_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for fallback usage")
    void testFallbackRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.USE_FALLBACK,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.TOOL_NOT_FOUND));
        assertEquals(ExceptionContext.RecoveryHint.USE_FALLBACK,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.TOOL_NOT_AVAILABLE));
        assertEquals(ExceptionContext.RecoveryHint.USE_FALLBACK,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.TOOL_EXECUTION_FAILED));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for admin contact")
    void testContactAdminRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.MEMORY_STORAGE_CORRUPT));
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.CONFIG_SECURITY_MISSING_CERTIFICATE));
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OLLAMA_CONNECTION_REFUSED));
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.OLLAMA_INSUFFICIENT_MEMORY));
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.MEMORY_THRESHOLD_EXCEEDED));
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.METRICS_REGISTRY_FULL));
    }

    @Test
    @DisplayName("Should provide correct recovery hints for immediate retry")
    void testImmediateRetryRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.RETRY_IMMEDIATE,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.MEMORY_TRANSACTION_DEADLOCK));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_IMMEDIATE,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.LLM_SERVICE_MAINTENANCE));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_IMMEDIATE,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.WORKFLOW_EXECUTION_INTERRUPTED));
    }

    @Test
    @DisplayName("Should handle null and unknown error codes for recovery hints")
    void testNullAndUnknownErrorCodesRecoveryHints() {
        assertEquals(ExceptionContext.RecoveryHint.NO_RECOVERY,
            ErrorCodes.getRecoveryHintForCode(null));
        assertEquals(ExceptionContext.RecoveryHint.NO_RECOVERY,
            ErrorCodes.getRecoveryHintForCode("UNKNOWN_ERROR_CODE"));
        assertEquals(ExceptionContext.RecoveryHint.NO_RECOVERY,
            ErrorCodes.getRecoveryHintForCode(""));
    }

    @Test
    @DisplayName("All error code constants should be unique")
    void testErrorCodeUniqueness() {
        Set<String> errorCodes = new HashSet<>();
        Field[] fields = ErrorCodes.class.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                field.getType() == String.class &&
                !field.getName().equals("class")) {

                try {
                    String errorCode = (String) field.get(null);
                    assertNotNull(errorCode, "Error code should not be null: " + field.getName());
                    assertFalse(errorCode.isEmpty(), "Error code should not be empty: " + field.getName());
                    assertFalse(errorCodes.contains(errorCode),
                        "Duplicate error code found: " + errorCode + " in field " + field.getName());
                    errorCodes.add(errorCode);
                } catch (IllegalAccessException e) {
                    fail("Could not access field: " + field.getName());
                }
            }
        }

        assertTrue(errorCodes.size() > 0, "Should have found error code constants");
    }

    @Test
    @DisplayName("All error codes should follow naming convention")
    void testErrorCodeNamingConvention() {
        Field[] fields = ErrorCodes.class.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                field.getType() == String.class &&
                !field.getName().equals("class")) {

                try {
                    String errorCode = (String) field.get(null);
                    assertNotNull(errorCode, "Error code should not be null: " + field.getName());

                    // Should be uppercase with underscores
                    assertTrue(errorCode.matches("^[A-Z][A-Z0-9_]*$"),
                        "Error code should be uppercase with underscores: " + errorCode);

                    // Should have at least one underscore (category_subcategory format)
                    assertTrue(errorCode.contains("_"),
                        "Error code should contain underscores for hierarchy: " + errorCode);

                    // Field name should match the error code value
                    assertEquals(field.getName(), errorCode,
                        "Field name should match error code value");

                } catch (IllegalAccessException e) {
                    fail("Could not access field: " + field.getName());
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle workflow-specific error codes correctly")
    void testWorkflowErrorCodes() {
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.WORKFLOW_STAGE_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.BUSINESS_LOGIC,
            ErrorCodes.getCategoryForCode(ErrorCodes.WORKFLOW_PARALLEL_STAGE_FAILED));

        assertEquals(ExceptionContext.RecoveryHint.FIX_CONFIGURATION,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.WORKFLOW_DEFINITION_INVALID));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.WORKFLOW_STAGE_FAILED));
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.WORKFLOW_STAGE_TIMEOUT));
    }

    @Test
    @DisplayName("Should handle memory management error codes correctly")
    void testMemoryManagementErrorCodes() {
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.MEMORY_CLEANUP_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.MEMORY_THRESHOLD_EXCEEDED));

        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.MEMORY_THRESHOLD_EXCEEDED));
        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.MEMORY_CLEANUP_FAILED));
    }

    @Test
    @DisplayName("Should handle metrics error codes correctly")
    void testMetricsErrorCodes() {
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.METRICS_COLLECTION_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.METRICS_EXPORT_FAILED));

        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.METRICS_COLLECTION_FAILED));
        assertEquals(ExceptionContext.RecoveryHint.CONTACT_ADMIN,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.METRICS_EXPORT_FAILED));
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.METRICS_INVALID_FORMAT));
    }

    @Test
    @DisplayName("Should handle database template error codes correctly")
    void testDatabaseTemplateErrorCodes() {
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.DATABASE_QUERY_FAILED));
        assertEquals(ExceptionContext.ErrorCategory.SYSTEM_ERROR,
            ErrorCodes.getCategoryForCode(ErrorCodes.DATABASE_BATCH_OPERATION_FAILED));

        assertEquals(ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.DATABASE_QUERY_FAILED));
        assertEquals(ExceptionContext.RecoveryHint.INCREASE_TIMEOUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.DATABASE_QUERY_TIMEOUT));
        assertEquals(ExceptionContext.RecoveryHint.VALIDATE_INPUT,
            ErrorCodes.getRecoveryHintForCode(ErrorCodes.DATABASE_PARAMETER_BINDING_FAILED));
    }
}
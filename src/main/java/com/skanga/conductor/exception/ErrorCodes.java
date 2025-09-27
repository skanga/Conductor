package com.skanga.conductor.exception;

/**
 * Standardized error codes for the Conductor framework.
 * <p>
 * This class provides a centralized registry of error codes used throughout
 * the framework for consistent error classification and handling. Error codes
 * follow a hierarchical naming convention for easy categorization.
 * </p>
 * <p>
 * Error Code Format: CATEGORY_SUBCATEGORY_SPECIFIC_ERROR
 * </p>
 * <ul>
 * <li>CATEGORY: High-level error category (LLM, TOOL, CONFIG, etc.)</li>
 * <li>SUBCATEGORY: Specific area within category (AUTH, TIMEOUT, etc.)</li>
 * <li>SPECIFIC_ERROR: Detailed error type</li>
 * </ul>
 *
 * @since 1.0.0
 * @see ExceptionContext
 * @see ExceptionContext.ErrorCategory
 */
public final class ErrorCodes {

    // LLM Provider Error Codes
    public static final String LLM_AUTH_INVALID_KEY = "LLM_AUTH_INVALID_KEY";
    public static final String LLM_AUTH_EXPIRED_KEY = "LLM_AUTH_EXPIRED_KEY";
    public static final String LLM_AUTH_MISSING_KEY = "LLM_AUTH_MISSING_KEY";
    public static final String LLM_AUTH_INSUFFICIENT_PERMISSIONS = "LLM_AUTH_INSUFFICIENT_PERMISSIONS";

    public static final String LLM_RATE_LIMIT_EXCEEDED = "LLM_RATE_LIMIT_EXCEEDED";
    public static final String LLM_RATE_QUOTA_EXCEEDED = "LLM_RATE_QUOTA_EXCEEDED";
    public static final String LLM_RATE_CONCURRENT_LIMIT = "LLM_RATE_CONCURRENT_LIMIT";

    public static final String LLM_TIMEOUT_REQUEST = "LLM_TIMEOUT_REQUEST";
    public static final String LLM_TIMEOUT_CONNECTION = "LLM_TIMEOUT_CONNECTION";
    public static final String LLM_TIMEOUT_READ = "LLM_TIMEOUT_READ";

    public static final String LLM_NETWORK_CONNECTION_FAILED = "LLM_NETWORK_CONNECTION_FAILED";
    public static final String LLM_NETWORK_DNS_RESOLUTION = "LLM_NETWORK_DNS_RESOLUTION";
    public static final String LLM_NETWORK_SSL_ERROR = "LLM_NETWORK_SSL_ERROR";

    public static final String LLM_SERVICE_UNAVAILABLE = "LLM_SERVICE_UNAVAILABLE";
    public static final String LLM_SERVICE_MAINTENANCE = "LLM_SERVICE_MAINTENANCE";
    public static final String LLM_SERVICE_OVERLOADED = "LLM_SERVICE_OVERLOADED";

    public static final String LLM_REQUEST_INVALID_FORMAT = "LLM_REQUEST_INVALID_FORMAT";
    public static final String LLM_REQUEST_TOO_LARGE = "LLM_REQUEST_TOO_LARGE";
    public static final String LLM_REQUEST_INVALID_MODEL = "LLM_REQUEST_INVALID_MODEL";

    public static final String LLM_RESPONSE_INVALID_FORMAT = "LLM_RESPONSE_INVALID_FORMAT";
    public static final String LLM_RESPONSE_TRUNCATED = "LLM_RESPONSE_TRUNCATED";
    public static final String LLM_RESPONSE_EMPTY = "LLM_RESPONSE_EMPTY";

    // Provider-Specific Error Codes

    // OpenAI Provider
    public static final String OPENAI_MODEL_OVERLOADED = "OPENAI_MODEL_OVERLOADED";
    public static final String OPENAI_CONTENT_POLICY_VIOLATION = "OPENAI_CONTENT_POLICY_VIOLATION";
    public static final String OPENAI_CONTEXT_LENGTH_EXCEEDED = "OPENAI_CONTEXT_LENGTH_EXCEEDED";
    public static final String OPENAI_INSUFFICIENT_QUOTA = "OPENAI_INSUFFICIENT_QUOTA";

    // Anthropic Provider
    public static final String ANTHROPIC_MODEL_UNAVAILABLE = "ANTHROPIC_MODEL_UNAVAILABLE";
    public static final String ANTHROPIC_TOKEN_LIMIT_EXCEEDED = "ANTHROPIC_TOKEN_LIMIT_EXCEEDED";
    public static final String ANTHROPIC_SAFETY_FILTER_TRIGGERED = "ANTHROPIC_SAFETY_FILTER_TRIGGERED";

    // Azure OpenAI Provider
    public static final String AZURE_DEPLOYMENT_NOT_FOUND = "AZURE_DEPLOYMENT_NOT_FOUND";
    public static final String AZURE_RESOURCE_NOT_FOUND = "AZURE_RESOURCE_NOT_FOUND";
    public static final String AZURE_SUBSCRIPTION_DISABLED = "AZURE_SUBSCRIPTION_DISABLED";

    // AWS Bedrock Provider
    public static final String BEDROCK_MODEL_NOT_AVAILABLE = "BEDROCK_MODEL_NOT_AVAILABLE";
    public static final String BEDROCK_ACCESS_DENIED = "BEDROCK_ACCESS_DENIED";
    public static final String BEDROCK_THROTTLING_EXCEPTION = "BEDROCK_THROTTLING_EXCEPTION";
    public static final String BEDROCK_VALIDATION_EXCEPTION = "BEDROCK_VALIDATION_EXCEPTION";

    // Google Gemini Provider
    public static final String GEMINI_API_KEY_INVALID = "GEMINI_API_KEY_INVALID";
    public static final String GEMINI_BLOCKED_PROMPT = "GEMINI_BLOCKED_PROMPT";
    public static final String GEMINI_RECITATION_BLOCKED = "GEMINI_RECITATION_BLOCKED";

    // Oracle Cloud Provider
    public static final String OCI_COMPARTMENT_NOT_FOUND = "OCI_COMPARTMENT_NOT_FOUND";
    public static final String OCI_IDENTITY_ERROR = "OCI_IDENTITY_ERROR";
    public static final String OCI_SERVICE_LIMIT_EXCEEDED = "OCI_SERVICE_LIMIT_EXCEEDED";

    // Local/Ollama Provider
    public static final String OLLAMA_CONNECTION_REFUSED = "OLLAMA_CONNECTION_REFUSED";
    public static final String OLLAMA_MODEL_NOT_FOUND = "OLLAMA_MODEL_NOT_FOUND";
    public static final String OLLAMA_INSUFFICIENT_MEMORY = "OLLAMA_INSUFFICIENT_MEMORY";

    // Tool Execution Error Codes
    public static final String TOOL_NOT_FOUND = "TOOL_NOT_FOUND";
    public static final String TOOL_NOT_AVAILABLE = "TOOL_NOT_AVAILABLE";
    public static final String TOOL_INITIALIZATION_FAILED = "TOOL_INITIALIZATION_FAILED";

    public static final String TOOL_INPUT_INVALID = "TOOL_INPUT_INVALID";
    public static final String TOOL_INPUT_MISSING_REQUIRED = "TOOL_INPUT_MISSING_REQUIRED";
    public static final String TOOL_INPUT_TYPE_MISMATCH = "TOOL_INPUT_TYPE_MISMATCH";
    public static final String TOOL_INPUT_OUT_OF_RANGE = "TOOL_INPUT_OUT_OF_RANGE";

    public static final String TOOL_EXECUTION_FAILED = "TOOL_EXECUTION_FAILED";
    public static final String TOOL_EXECUTION_TIMEOUT = "TOOL_EXECUTION_TIMEOUT";
    public static final String TOOL_EXECUTION_INTERRUPTED = "TOOL_EXECUTION_INTERRUPTED";

    public static final String TOOL_RESOURCE_UNAVAILABLE = "TOOL_RESOURCE_UNAVAILABLE";
    public static final String TOOL_RESOURCE_EXHAUSTED = "TOOL_RESOURCE_EXHAUSTED";
    public static final String TOOL_RESOURCE_ACCESS_DENIED = "TOOL_RESOURCE_ACCESS_DENIED";

    public static final String TOOL_OUTPUT_INVALID = "TOOL_OUTPUT_INVALID";
    public static final String TOOL_OUTPUT_TOO_LARGE = "TOOL_OUTPUT_TOO_LARGE";
    public static final String TOOL_OUTPUT_CORRUPT = "TOOL_OUTPUT_CORRUPT";

    // Configuration Error Codes
    public static final String CONFIG_FILE_NOT_FOUND = "CONFIG_FILE_NOT_FOUND";
    public static final String CONFIG_FILE_UNREADABLE = "CONFIG_FILE_UNREADABLE";
    public static final String CONFIG_FILE_INVALID_FORMAT = "CONFIG_FILE_INVALID_FORMAT";

    public static final String CONFIG_PROPERTY_MISSING = "CONFIG_PROPERTY_MISSING";
    public static final String CONFIG_PROPERTY_INVALID = "CONFIG_PROPERTY_INVALID";
    public static final String CONFIG_PROPERTY_OUT_OF_RANGE = "CONFIG_PROPERTY_OUT_OF_RANGE";

    public static final String CONFIG_DATABASE_URL_INVALID = "CONFIG_DATABASE_URL_INVALID";
    public static final String CONFIG_DATABASE_DRIVER_MISSING = "CONFIG_DATABASE_DRIVER_MISSING";
    public static final String CONFIG_DATABASE_CONNECTION_FAILED = "CONFIG_DATABASE_CONNECTION_FAILED";

    public static final String CONFIG_SECURITY_INVALID_CREDENTIALS = "CONFIG_SECURITY_INVALID_CREDENTIALS";
    public static final String CONFIG_SECURITY_MISSING_CERTIFICATE = "CONFIG_SECURITY_MISSING_CERTIFICATE";
    public static final String CONFIG_SECURITY_WEAK_ENCRYPTION = "CONFIG_SECURITY_WEAK_ENCRYPTION";

    // Memory Store Error Codes
    public static final String MEMORY_CONNECTION_FAILED = "MEMORY_CONNECTION_FAILED";
    public static final String MEMORY_CONNECTION_TIMEOUT = "MEMORY_CONNECTION_TIMEOUT";
    public static final String MEMORY_CONNECTION_POOL_EXHAUSTED = "MEMORY_CONNECTION_POOL_EXHAUSTED";

    public static final String MEMORY_QUERY_FAILED = "MEMORY_QUERY_FAILED";
    public static final String MEMORY_QUERY_TIMEOUT = "MEMORY_QUERY_TIMEOUT";
    public static final String MEMORY_QUERY_SYNTAX_ERROR = "MEMORY_QUERY_SYNTAX_ERROR";

    public static final String MEMORY_TRANSACTION_FAILED = "MEMORY_TRANSACTION_FAILED";
    public static final String MEMORY_TRANSACTION_DEADLOCK = "MEMORY_TRANSACTION_DEADLOCK";
    public static final String MEMORY_TRANSACTION_ROLLBACK = "MEMORY_TRANSACTION_ROLLBACK";

    public static final String MEMORY_STORAGE_FULL = "MEMORY_STORAGE_FULL";
    public static final String MEMORY_STORAGE_CORRUPT = "MEMORY_STORAGE_CORRUPT";
    public static final String MEMORY_STORAGE_ACCESS_DENIED = "MEMORY_STORAGE_ACCESS_DENIED";

    // Approval Process Error Codes
    public static final String APPROVAL_TIMEOUT = "APPROVAL_TIMEOUT";
    public static final String APPROVAL_REJECTED = "APPROVAL_REJECTED";
    public static final String APPROVAL_CANCELLED = "APPROVAL_CANCELLED";

    public static final String APPROVAL_HANDLER_NOT_AVAILABLE = "APPROVAL_HANDLER_NOT_AVAILABLE";
    public static final String APPROVAL_HANDLER_FAILED = "APPROVAL_HANDLER_FAILED";
    public static final String APPROVAL_HANDLER_MISCONFIGURED = "APPROVAL_HANDLER_MISCONFIGURED";

    public static final String APPROVAL_USER_NOT_FOUND = "APPROVAL_USER_NOT_FOUND";
    public static final String APPROVAL_USER_OFFLINE = "APPROVAL_USER_OFFLINE";
    public static final String APPROVAL_USER_INSUFFICIENT_PERMISSIONS = "APPROVAL_USER_INSUFFICIENT_PERMISSIONS";

    public static final String APPROVAL_REQUEST_INVALID = "APPROVAL_REQUEST_INVALID";
    public static final String APPROVAL_REQUEST_EXPIRED = "APPROVAL_REQUEST_EXPIRED";
    public static final String APPROVAL_REQUEST_DUPLICATE = "APPROVAL_REQUEST_DUPLICATE";

    // Planning and Orchestration Error Codes
    public static final String PLANNER_TASK_GENERATION_FAILED = "PLANNER_TASK_GENERATION_FAILED";
    public static final String PLANNER_TASK_INVALID = "PLANNER_TASK_INVALID";
    public static final String PLANNER_TASK_DEPENDENCY_CYCLE = "PLANNER_TASK_DEPENDENCY_CYCLE";

    public static final String PLANNER_EXECUTION_FAILED = "PLANNER_EXECUTION_FAILED";
    public static final String PLANNER_EXECUTION_TIMEOUT = "PLANNER_EXECUTION_TIMEOUT";
    public static final String PLANNER_EXECUTION_INTERRUPTED = "PLANNER_EXECUTION_INTERRUPTED";

    public static final String PLANNER_RESOURCE_ALLOCATION_FAILED = "PLANNER_RESOURCE_ALLOCATION_FAILED";
    public static final String PLANNER_RESOURCE_EXHAUSTED = "PLANNER_RESOURCE_EXHAUSTED";
    public static final String PLANNER_RESOURCE_CONFLICT = "PLANNER_RESOURCE_CONFLICT";

    // Data Format Error Codes
    public static final String JSON_PARSE_ERROR = "JSON_PARSE_ERROR";
    public static final String JSON_INVALID_STRUCTURE = "JSON_INVALID_STRUCTURE";
    public static final String JSON_MISSING_REQUIRED_FIELD = "JSON_MISSING_REQUIRED_FIELD";
    public static final String JSON_TYPE_MISMATCH = "JSON_TYPE_MISMATCH";

    public static final String YAML_PARSE_ERROR = "YAML_PARSE_ERROR";
    public static final String YAML_INVALID_STRUCTURE = "YAML_INVALID_STRUCTURE";
    public static final String YAML_INDENTATION_ERROR = "YAML_INDENTATION_ERROR";

    public static final String DATA_SERIALIZATION_FAILED = "DATA_SERIALIZATION_FAILED";
    public static final String DATA_DESERIALIZATION_FAILED = "DATA_DESERIALIZATION_FAILED";
    public static final String DATA_VALIDATION_FAILED = "DATA_VALIDATION_FAILED";

    // Validation Error Codes
    public static final String VALIDATION_NULL_VALUE = "VALIDATION_NULL_VALUE";
    public static final String VALIDATION_EMPTY_VALUE = "VALIDATION_EMPTY_VALUE";
    public static final String VALIDATION_INVALID_TYPE = "VALIDATION_INVALID_TYPE";
    public static final String VALIDATION_OUT_OF_RANGE = "VALIDATION_OUT_OF_RANGE";
    public static final String VALIDATION_PATTERN_MISMATCH = "VALIDATION_PATTERN_MISMATCH";
    public static final String VALIDATION_LENGTH_EXCEEDED = "VALIDATION_LENGTH_EXCEEDED";
    public static final String VALIDATION_CONSTRAINT_VIOLATION = "VALIDATION_CONSTRAINT_VIOLATION";

    // Workflow Error Codes
    public static final String WORKFLOW_DEFINITION_INVALID = "WORKFLOW_DEFINITION_INVALID";
    public static final String WORKFLOW_STAGE_FAILED = "WORKFLOW_STAGE_FAILED";
    public static final String WORKFLOW_STAGE_TIMEOUT = "WORKFLOW_STAGE_TIMEOUT";
    public static final String WORKFLOW_DEPENDENCY_UNRESOLVED = "WORKFLOW_DEPENDENCY_UNRESOLVED";
    public static final String WORKFLOW_EXECUTION_INTERRUPTED = "WORKFLOW_EXECUTION_INTERRUPTED";
    public static final String WORKFLOW_PARALLEL_STAGE_FAILED = "WORKFLOW_PARALLEL_STAGE_FAILED";
    public static final String WORKFLOW_CONTEXT_INVALID = "WORKFLOW_CONTEXT_INVALID";
    public static final String WORKFLOW_VALIDATION_FAILED = "WORKFLOW_VALIDATION_FAILED";

    // Memory Management Error Codes
    public static final String MEMORY_CLEANUP_FAILED = "MEMORY_CLEANUP_FAILED";
    public static final String MEMORY_THRESHOLD_EXCEEDED = "MEMORY_THRESHOLD_EXCEEDED";
    public static final String MEMORY_RESOURCE_LEAK_DETECTED = "MEMORY_RESOURCE_LEAK_DETECTED";
    public static final String MEMORY_WEAK_REFERENCE_CLEANUP_FAILED = "MEMORY_WEAK_REFERENCE_CLEANUP_FAILED";
    public static final String MEMORY_EXPIRABLE_RESOURCE_CLEANUP_FAILED = "MEMORY_EXPIRABLE_RESOURCE_CLEANUP_FAILED";
    public static final String MEMORY_EMERGENCY_CLEANUP_TRIGGERED = "MEMORY_EMERGENCY_CLEANUP_TRIGGERED";
    public static final String MEMORY_MONITORING_FAILED = "MEMORY_MONITORING_FAILED";

    // Metrics Error Codes
    public static final String METRICS_COLLECTION_FAILED = "METRICS_COLLECTION_FAILED";
    public static final String METRICS_EXPORT_FAILED = "METRICS_EXPORT_FAILED";
    public static final String METRICS_RETENTION_CLEANUP_FAILED = "METRICS_RETENTION_CLEANUP_FAILED";
    public static final String METRICS_AGGREGATION_FAILED = "METRICS_AGGREGATION_FAILED";
    public static final String METRICS_COLLECTOR_UNAVAILABLE = "METRICS_COLLECTOR_UNAVAILABLE";
    public static final String METRICS_REGISTRY_FULL = "METRICS_REGISTRY_FULL";
    public static final String METRICS_INVALID_FORMAT = "METRICS_INVALID_FORMAT";

    // Database Template Error Codes
    public static final String DATABASE_QUERY_FAILED = "DATABASE_QUERY_FAILED";
    public static final String DATABASE_QUERY_TIMEOUT = "DATABASE_QUERY_TIMEOUT";
    public static final String DATABASE_BATCH_OPERATION_FAILED = "DATABASE_BATCH_OPERATION_FAILED";
    public static final String DATABASE_RESULT_MAPPING_FAILED = "DATABASE_RESULT_MAPPING_FAILED";
    public static final String DATABASE_PARAMETER_BINDING_FAILED = "DATABASE_PARAMETER_BINDING_FAILED";
    public static final String DATABASE_HEALTH_CHECK_FAILED = "DATABASE_HEALTH_CHECK_FAILED";

    // Configuration Accessor Error Codes
    public static final String CONFIG_ACCESSOR_UNAVAILABLE = "CONFIG_ACCESSOR_UNAVAILABLE";
    public static final String CONFIG_ACCESSOR_INITIALIZATION_FAILED = "CONFIG_ACCESSOR_INITIALIZATION_FAILED";
    public static final String CONFIG_PROVIDER_CONFIG_INVALID = "CONFIG_PROVIDER_CONFIG_INVALID";

    private ErrorCodes() {
        // Utility class - no instantiation
    }

    /**
     * Gets the error category for a given error code.
     *
     * @param errorCode the error code
     * @return the corresponding error category
     */
    public static ExceptionContext.ErrorCategory getCategoryForCode(String errorCode) {
        if (errorCode == null) {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        }

        if (errorCode.startsWith("LLM_AUTH")) {
            return ExceptionContext.ErrorCategory.AUTHENTICATION;
        } else if (errorCode.startsWith("LLM_RATE")) {
            return ExceptionContext.ErrorCategory.RATE_LIMIT;
        } else if (errorCode.startsWith("LLM_TIMEOUT")) {
            return ExceptionContext.ErrorCategory.TIMEOUT;
        } else if (errorCode.startsWith("LLM_NETWORK")) {
            return ExceptionContext.ErrorCategory.NETWORK;
        } else if (errorCode.startsWith("LLM_") ||
                   errorCode.startsWith("OPENAI_") ||
                   errorCode.startsWith("ANTHROPIC_") ||
                   errorCode.startsWith("AZURE_") ||
                   errorCode.startsWith("BEDROCK_") ||
                   errorCode.startsWith("GEMINI_") ||
                   errorCode.startsWith("OCI_") ||
                   errorCode.startsWith("OLLAMA_")) {
            return ExceptionContext.ErrorCategory.BUSINESS_LOGIC;
        } else if (errorCode.startsWith("TOOL_INPUT") || errorCode.startsWith("TOOL_OUTPUT")) {
            return ExceptionContext.ErrorCategory.VALIDATION;
        } else if (errorCode.startsWith("TOOL_RESOURCE")) {
            return ExceptionContext.ErrorCategory.RESOURCE_UNAVAILABLE;
        } else if (errorCode.startsWith("TOOL_")) {
            return ExceptionContext.ErrorCategory.BUSINESS_LOGIC;
        } else if (errorCode.startsWith("CONFIG_")) {
            return ExceptionContext.ErrorCategory.CONFIGURATION;
        } else if (errorCode.startsWith("MEMORY_")) {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        } else if (errorCode.startsWith("APPROVAL_")) {
            return ExceptionContext.ErrorCategory.USER_INTERACTION;
        } else if (errorCode.startsWith("PLANNER_")) {
            return ExceptionContext.ErrorCategory.BUSINESS_LOGIC;
        } else if (errorCode.startsWith("JSON_") || errorCode.startsWith("YAML_") || errorCode.startsWith("DATA_")) {
            return ExceptionContext.ErrorCategory.DATA_FORMAT;
        } else if (errorCode.startsWith("VALIDATION_")) {
            return ExceptionContext.ErrorCategory.VALIDATION;
        } else if (errorCode.startsWith("WORKFLOW_")) {
            return ExceptionContext.ErrorCategory.BUSINESS_LOGIC;
        } else if (errorCode.startsWith("MEMORY_")) {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        } else if (errorCode.startsWith("METRICS_")) {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        } else if (errorCode.startsWith("DATABASE_")) {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        } else {
            return ExceptionContext.ErrorCategory.SYSTEM_ERROR;
        }
    }

    /**
     * Gets the recommended recovery hint for a given error code.
     *
     * @param errorCode the error code
     * @return the recommended recovery hint
     */
    public static ExceptionContext.RecoveryHint getRecoveryHintForCode(String errorCode) {
        if (errorCode == null) {
            return ExceptionContext.RecoveryHint.NO_RECOVERY;
        }

        switch (errorCode) {
            // Authentication errors - check credentials
            case LLM_AUTH_INVALID_KEY:
            case LLM_AUTH_EXPIRED_KEY:
            case LLM_AUTH_MISSING_KEY:
            case CONFIG_SECURITY_INVALID_CREDENTIALS:
                return ExceptionContext.RecoveryHint.CHECK_CREDENTIALS;

            // Rate limit errors - wait for reset
            case LLM_RATE_LIMIT_EXCEEDED:
            case LLM_RATE_QUOTA_EXCEEDED:
            case LLM_RATE_CONCURRENT_LIMIT:
                return ExceptionContext.RecoveryHint.WAIT_RATE_LIMIT;

            // Timeout errors - retry or increase timeout
            case LLM_TIMEOUT_REQUEST:
            case LLM_TIMEOUT_CONNECTION:
            case LLM_TIMEOUT_READ:
            case TOOL_EXECUTION_TIMEOUT:
            case MEMORY_CONNECTION_TIMEOUT:
            case MEMORY_QUERY_TIMEOUT:
                return ExceptionContext.RecoveryHint.INCREASE_TIMEOUT;

            // Network and service errors - retry with backoff
            case LLM_NETWORK_CONNECTION_FAILED:
            case LLM_SERVICE_UNAVAILABLE:
            case LLM_SERVICE_OVERLOADED:
            case MEMORY_CONNECTION_FAILED:
            case PLANNER_EXECUTION_FAILED:
                return ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;

            // Tool errors - use fallback
            case TOOL_NOT_FOUND:
            case TOOL_NOT_AVAILABLE:
            case TOOL_EXECUTION_FAILED:
                return ExceptionContext.RecoveryHint.USE_FALLBACK;

            // Configuration errors - fix configuration
            case CONFIG_FILE_NOT_FOUND:
            case CONFIG_PROPERTY_MISSING:
            case CONFIG_PROPERTY_INVALID:
            case CONFIG_DATABASE_URL_INVALID:
                return ExceptionContext.RecoveryHint.FIX_CONFIGURATION;

            // Validation errors - validate input
            case TOOL_INPUT_INVALID:
            case TOOL_INPUT_MISSING_REQUIRED:
            case JSON_PARSE_ERROR:
            case VALIDATION_NULL_VALUE:
            case VALIDATION_INVALID_TYPE:
                return ExceptionContext.RecoveryHint.VALIDATE_INPUT;

            // Approval errors - user action required
            case APPROVAL_TIMEOUT:
            case APPROVAL_REJECTED:
            case APPROVAL_USER_NOT_FOUND:
                return ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED;

            // Transient errors - retry immediately
            case MEMORY_TRANSACTION_DEADLOCK:
            case LLM_SERVICE_MAINTENANCE:
                return ExceptionContext.RecoveryHint.RETRY_IMMEDIATE;

            // Provider-specific errors
            case OPENAI_MODEL_OVERLOADED:
            case ANTHROPIC_MODEL_UNAVAILABLE:
            case BEDROCK_MODEL_NOT_AVAILABLE:
                return ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;

            case OPENAI_CONTENT_POLICY_VIOLATION:
            case ANTHROPIC_SAFETY_FILTER_TRIGGERED:
            case GEMINI_BLOCKED_PROMPT:
                return ExceptionContext.RecoveryHint.VALIDATE_INPUT;

            case OPENAI_CONTEXT_LENGTH_EXCEEDED:
            case ANTHROPIC_TOKEN_LIMIT_EXCEEDED:
                return ExceptionContext.RecoveryHint.VALIDATE_INPUT;

            case OPENAI_INSUFFICIENT_QUOTA:
            case OCI_SERVICE_LIMIT_EXCEEDED:
                return ExceptionContext.RecoveryHint.USER_ACTION_REQUIRED;

            case AZURE_DEPLOYMENT_NOT_FOUND:
            case AZURE_RESOURCE_NOT_FOUND:
            case BEDROCK_ACCESS_DENIED:
            case OCI_COMPARTMENT_NOT_FOUND:
            case OLLAMA_MODEL_NOT_FOUND:
                return ExceptionContext.RecoveryHint.FIX_CONFIGURATION;

            case GEMINI_API_KEY_INVALID:
            case OCI_IDENTITY_ERROR:
                return ExceptionContext.RecoveryHint.CHECK_CREDENTIALS;

            case OLLAMA_CONNECTION_REFUSED:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            case BEDROCK_THROTTLING_EXCEPTION:
                return ExceptionContext.RecoveryHint.WAIT_RATE_LIMIT;

            // System errors - contact admin
            case MEMORY_STORAGE_CORRUPT:
            case CONFIG_SECURITY_MISSING_CERTIFICATE:
            case TOOL_RESOURCE_ACCESS_DENIED:
            case OLLAMA_INSUFFICIENT_MEMORY:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            // Workflow errors
            case WORKFLOW_DEFINITION_INVALID:
            case WORKFLOW_CONTEXT_INVALID:
            case WORKFLOW_VALIDATION_FAILED:
                return ExceptionContext.RecoveryHint.FIX_CONFIGURATION;

            case WORKFLOW_STAGE_FAILED:
            case WORKFLOW_PARALLEL_STAGE_FAILED:
                return ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;

            case WORKFLOW_STAGE_TIMEOUT:
                return ExceptionContext.RecoveryHint.INCREASE_TIMEOUT;

            case WORKFLOW_DEPENDENCY_UNRESOLVED:
                return ExceptionContext.RecoveryHint.FIX_CONFIGURATION;

            case WORKFLOW_EXECUTION_INTERRUPTED:
                return ExceptionContext.RecoveryHint.RETRY_IMMEDIATE;

            // Memory management errors
            case MEMORY_THRESHOLD_EXCEEDED:
            case MEMORY_EMERGENCY_CLEANUP_TRIGGERED:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            case MEMORY_CLEANUP_FAILED:
            case MEMORY_WEAK_REFERENCE_CLEANUP_FAILED:
            case MEMORY_EXPIRABLE_RESOURCE_CLEANUP_FAILED:
                return ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;

            case MEMORY_RESOURCE_LEAK_DETECTED:
            case MEMORY_MONITORING_FAILED:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            // Metrics errors
            case METRICS_COLLECTION_FAILED:
            case METRICS_AGGREGATION_FAILED:
                return ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;

            case METRICS_EXPORT_FAILED:
            case METRICS_RETENTION_CLEANUP_FAILED:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            case METRICS_COLLECTOR_UNAVAILABLE:
                return ExceptionContext.RecoveryHint.FIX_CONFIGURATION;

            case METRICS_REGISTRY_FULL:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            case METRICS_INVALID_FORMAT:
                return ExceptionContext.RecoveryHint.VALIDATE_INPUT;

            // Database template errors
            case DATABASE_QUERY_FAILED:
            case DATABASE_BATCH_OPERATION_FAILED:
                return ExceptionContext.RecoveryHint.RETRY_WITH_BACKOFF;

            case DATABASE_QUERY_TIMEOUT:
                return ExceptionContext.RecoveryHint.INCREASE_TIMEOUT;

            case DATABASE_RESULT_MAPPING_FAILED:
            case DATABASE_PARAMETER_BINDING_FAILED:
                return ExceptionContext.RecoveryHint.VALIDATE_INPUT;

            case DATABASE_HEALTH_CHECK_FAILED:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            // Configuration accessor errors
            case CONFIG_ACCESSOR_UNAVAILABLE:
            case CONFIG_ACCESSOR_INITIALIZATION_FAILED:
                return ExceptionContext.RecoveryHint.CONTACT_ADMIN;

            case CONFIG_PROVIDER_CONFIG_INVALID:
                return ExceptionContext.RecoveryHint.FIX_CONFIGURATION;

            default:
                return ExceptionContext.RecoveryHint.NO_RECOVERY;
        }
    }
}
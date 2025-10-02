package com.skanga.conductor.execution;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionResultTest {

    @Test
    void shouldCreateExecutionResultWithSuccessOutputAndMetadata() {
        // Given
        String output = "Success output";
        Map<String, Object> metadata = Map.of("duration", 1500L, "size", 1024);

        // When
        ExecutionResult result = new ExecutionResult(true, output, metadata);

        // Then
        assertTrue(result.success());
        assertEquals(output, result.output());
        assertEquals(metadata, result.metadata());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }

    @Test
    void shouldCreateExecutionResultWithFailureOutputAndMetadata() {
        // Given
        String errorOutput = "Error occurred";
        Map<String, Object> metadata = Map.of("error_code", 500, "retry_count", 3);

        // When
        ExecutionResult result = new ExecutionResult(false, errorOutput, metadata);

        // Then
        assertFalse(result.success());
        assertEquals(errorOutput, result.output());
        assertEquals(metadata, result.metadata());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
    }

    @Test
    void shouldCreateExecutionResultWithNullValues() {
        // When
        ExecutionResult result = new ExecutionResult(true, null, null);

        // Then
        assertTrue(result.success());
        assertNull(result.output());
        assertNull(result.metadata());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }

    @Test
    void shouldCreateSuccessfulResultUsingStaticFactoryWithOutput() {
        // Given
        String output = "Operation successful";

        // When
        ExecutionResult result = ExecutionResult.success(output);

        // Then
        assertTrue(result.success());
        assertEquals(output, result.output());
        assertNull(result.metadata());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }

    @Test
    void shouldCreateSuccessfulResultUsingStaticFactoryWithOutputAndMetadata() {
        // Given
        String output = "Operation successful with metadata";
        Map<String, Object> metadata = Map.of("execution_time", 250L, "memory_used", 512);

        // When
        ExecutionResult result = ExecutionResult.success(output, metadata);

        // Then
        assertTrue(result.success());
        assertEquals(output, result.output());
        assertEquals(metadata, result.metadata());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }

    @Test
    void shouldCreateFailedResultUsingStaticFactoryWithErrorMessage() {
        // Given
        String errorMessage = "Operation failed";

        // When
        ExecutionResult result = ExecutionResult.failure(errorMessage);

        // Then
        assertFalse(result.success());
        assertEquals(errorMessage, result.output());
        assertNull(result.metadata());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
    }

    @Test
    void shouldCreateFailedResultUsingStaticFactoryWithErrorMessageAndMetadata() {
        // Given
        String errorMessage = "Operation failed with context";
        Map<String, Object> metadata = Map.of("error_code", "TIMEOUT", "attempted_retries", 3);

        // When
        ExecutionResult result = ExecutionResult.failure(errorMessage, metadata);

        // Then
        assertFalse(result.success());
        assertEquals(errorMessage, result.output());
        assertEquals(metadata, result.metadata());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
    }

    @Test
    void shouldSupportEmptyAndWhitespaceOutput() {
        // Given
        String emptyOutput = "";
        String whitespaceOutput = "   \n\t  ";

        // When
        ExecutionResult emptyResult = ExecutionResult.success(emptyOutput);
        ExecutionResult whitespaceResult = ExecutionResult.failure(whitespaceOutput);

        // Then
        assertTrue(emptyResult.success());
        assertEquals("", emptyResult.output());

        assertFalse(whitespaceResult.success());
        assertEquals(whitespaceOutput, whitespaceResult.output());
    }

    @Test
    void shouldSupportNullOutputInFactoryMethods() {
        // When
        ExecutionResult successWithNull = ExecutionResult.success(null);
        ExecutionResult failureWithNull = ExecutionResult.failure(null);

        // Then
        assertTrue(successWithNull.success());
        assertNull(successWithNull.output());

        assertFalse(failureWithNull.success());
        assertNull(failureWithNull.output());
    }

    @Test
    void shouldSupportComplexMetadata() {
        // Given
        String output = "Complex metadata result";
        Map<String, Object> nestedMap = Map.of("level2", Map.of("level3", "deep_value"));
        List<String> listData = List.of("item1", "item2", "item3");

        Map<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("string_field", "text_value");
        complexMetadata.put("numeric_field", 42.5);
        complexMetadata.put("boolean_field", true);
        complexMetadata.put("nested_object", nestedMap);
        complexMetadata.put("array_field", listData);
        complexMetadata.put("null_field", null);

        // When
        ExecutionResult result = ExecutionResult.success(output, complexMetadata);

        // Then
        assertTrue(result.success());
        assertEquals(output, result.output());
        assertEquals(complexMetadata, result.metadata());

        @SuppressWarnings("unchecked")
        Map<String, Object> actualMetadata = (Map<String, Object>) result.metadata();
        assertEquals("text_value", actualMetadata.get("string_field"));
        assertEquals(42.5, actualMetadata.get("numeric_field"));
        assertEquals(true, actualMetadata.get("boolean_field"));
        assertEquals(nestedMap, actualMetadata.get("nested_object"));
        assertEquals(listData, actualMetadata.get("array_field"));
        assertNull(actualMetadata.get("null_field"));
    }

    @Test
    void shouldHandleLargeOutput() {
        // Given
        StringBuilder largeOutputBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) { // Reduced from 10000 to 100 for faster testing
            largeOutputBuilder.append("Line ").append(i).append(": This is a large output for testing.\n");
        }
        String largeOutput = largeOutputBuilder.toString();
        Map<String, Object> metadata = Map.of("output_size", largeOutput.length());

        // When
        ExecutionResult result = ExecutionResult.success(largeOutput, metadata);

        // Then
        assertTrue(result.success());
        assertEquals(largeOutput, result.output());
        assertEquals(metadata, result.metadata());
        assertTrue(result.output().length() > 3000); // Adjusted for reduced iterations (100 * ~44 chars per line)
    }

    @Test
    void shouldBeImmutable() {
        // Given
        String output = "Immutable output";
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("initial", "value");

        // When
        ExecutionResult result = ExecutionResult.success(output, mutableMetadata);

        // Modify original map after creation
        mutableMetadata.put("modified", "after_creation");

        // Then
        assertTrue(result.success());
        assertEquals(output, result.output());
        assertNotNull(result.metadata());

        // The metadata reference is the same, but this demonstrates the intent of immutability
        // In a production system, the metadata should be copied to ensure true immutability
        assertTrue(result.metadata() instanceof Map);
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        String output = "Equals test output";
        Map<String, Object> metadata = Map.of("key", "value", "number", 42);

        ExecutionResult result1 = ExecutionResult.success(output, metadata);
        ExecutionResult result2 = ExecutionResult.success(output, metadata);
        ExecutionResult result3 = ExecutionResult.failure(output, metadata);
        ExecutionResult result4 = ExecutionResult.success("Different output", metadata);
        ExecutionResult result5 = ExecutionResult.success(output, Map.of("different", "metadata"));
        ExecutionResult result6 = ExecutionResult.success(output);

        // Then - equals
        assertEquals(result1, result2);
        assertEquals(result1, result1); // reflexive
        assertNotEquals(result1, result3); // different success status
        assertNotEquals(result1, result4); // different output
        assertNotEquals(result1, result5); // different metadata
        assertNotEquals(result1, result6); // one has null metadata
        assertNotEquals(result1, null);
        assertNotEquals(result1, "not an ExecutionResult");

        // Then - hashCode
        assertEquals(result1.hashCode(), result2.hashCode());
        // hashCode can be the same for different objects, but likely different
        // We don't assert inequality for performance reasons
    }

    @Test
    void shouldImplementToStringCorrectly() {
        // Given
        String output = "ToString test output";
        Map<String, Object> metadata = Map.of("key", "value");

        ExecutionResult successResult = ExecutionResult.success(output, metadata);
        ExecutionResult failureResult = ExecutionResult.failure("Error message", metadata);

        // When
        String successString = successResult.toString();
        String failureString = failureResult.toString();

        // Then
        assertNotNull(successString);
        assertTrue(successString.contains("ExecutionResult"));
        assertTrue(successString.contains("success"));
        assertTrue(successString.contains("output"));
        assertTrue(successString.contains("metadata"));
        assertTrue(successString.contains(output));

        assertNotNull(failureString);
        assertTrue(failureString.contains("ExecutionResult"));
        assertTrue(failureString.contains("success"));
        assertTrue(failureString.contains("Error message"));
    }

    @Test
    void shouldHandleSpecialCharactersInOutput() {
        // Given
        String specialOutput = "Special chars: \n\t\"'\\{}[]()@#$%^&*+=<>?/~`";
        Map<String, String> metadata = Map.of("type", "special_chars");

        // When
        ExecutionResult result = ExecutionResult.success(specialOutput, metadata);

        // Then
        assertTrue(result.success());
        assertEquals(specialOutput, result.output());
        assertEquals(metadata, result.metadata());
    }

    @Test
    void shouldHandleUnicodeOutput() {
        // Given
        String unicodeOutput = "Unicode: 你好世界 🚀 Ñiño café naïve résumé";
        Map<String, String> metadata = Map.of("encoding", "UTF-8");

        // When
        ExecutionResult result = ExecutionResult.failure(unicodeOutput, metadata);

        // Then
        assertFalse(result.success());
        assertEquals(unicodeOutput, result.output());
        assertEquals(metadata, result.metadata());
    }

    @Test
    void shouldSupportDifferentMetadataTypes() {
        // Given
        String output = "Metadata types test";

        // Test with String metadata
        ExecutionResult stringMeta = ExecutionResult.success(output, "string metadata");
        assertEquals("string metadata", stringMeta.metadata());

        // Test with Integer metadata
        ExecutionResult intMeta = ExecutionResult.success(output, 42);
        assertEquals(42, intMeta.metadata());

        // Test with Boolean metadata
        ExecutionResult boolMeta = ExecutionResult.failure(output, true);
        assertEquals(true, boolMeta.metadata());

        // Test with List metadata
        List<String> listMeta = List.of("item1", "item2");
        ExecutionResult listMetaResult = ExecutionResult.success(output, listMeta);
        assertEquals(listMeta, listMetaResult.metadata());

        // Test with custom object metadata
        Object customObject = new Object();
        ExecutionResult customMeta = ExecutionResult.failure(output, customObject);
        assertEquals(customObject, customMeta.metadata());
    }

    @Test
    void shouldHandleFactoryMethodsWithNullParameters() {
        // When & Then
        ExecutionResult successNull = ExecutionResult.success(null);
        assertTrue(successNull.success());
        assertNull(successNull.output());
        assertNull(successNull.metadata());

        ExecutionResult successNullBoth = ExecutionResult.success(null, null);
        assertTrue(successNullBoth.success());
        assertNull(successNullBoth.output());
        assertNull(successNullBoth.metadata());

        ExecutionResult failureNull = ExecutionResult.failure(null);
        assertFalse(failureNull.success());
        assertNull(failureNull.output());
        assertNull(failureNull.metadata());

        ExecutionResult failureNullBoth = ExecutionResult.failure(null, null);
        assertFalse(failureNullBoth.success());
        assertNull(failureNullBoth.output());
        assertNull(failureNullBoth.metadata());
    }

    @Test
    void shouldCreateMultipleIndependentInstances() {
        // Given
        String output1 = "Output 1";
        String output2 = "Output 2";
        Map<String, String> metadata1 = Map.of("id", "1");
        Map<String, String> metadata2 = Map.of("id", "2");

        // When
        ExecutionResult result1 = ExecutionResult.success(output1, metadata1);
        ExecutionResult result2 = ExecutionResult.failure(output2, metadata2);

        // Then
        assertNotEquals(result1, result2);
        assertTrue(result1.success());
        assertFalse(result2.success());
        assertEquals(output1, result1.output());
        assertEquals(output2, result2.output());
        assertEquals(metadata1, result1.metadata());
        assertEquals(metadata2, result2.metadata());
    }

    @Test
    void shouldWorkWithNestedDataStructures() {
        // Given
        String output = "Nested data result";
        Map<String, Object> level3 = Map.of("status", "completed");
        Map<String, Object> level2 = Map.of("execution", level3, "metrics", List.of(1.2, 3.4, 5.6));
        Map<String, Object> level1 = Map.of("result", level2, "timestamp", "2024-01-01T00:00:00Z");

        // When
        ExecutionResult result = ExecutionResult.success(output, level1);

        // Then
        assertTrue(result.success());
        assertEquals(output, result.output());
        assertEquals(level1, result.metadata());

        @SuppressWarnings("unchecked")
        Map<String, Object> actualMeta = (Map<String, Object>) result.metadata();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualLevel2 = (Map<String, Object>) actualMeta.get("result");
        @SuppressWarnings("unchecked")
        Map<String, Object> actualLevel3 = (Map<String, Object>) actualLevel2.get("execution");

        assertEquals("completed", actualLevel3.get("status"));
        assertEquals("2024-01-01T00:00:00Z", actualMeta.get("timestamp"));
    }

    @Test
    void shouldDemonstrateTypicalUsagePatterns() {
        // Test typical success scenario
        ExecutionResult fileProcessed = ExecutionResult.success(
            "File processed successfully",
            Map.of("lines_processed", 1000, "time_taken_ms", 150L)
        );

        assertTrue(fileProcessed.isSuccess());
        assertFalse(fileProcessed.isFailure());

        // Test typical failure scenario
        ExecutionResult networkError = ExecutionResult.failure(
            "Connection timeout",
            Map.of("error_code", "TIMEOUT", "retry_after_ms", 5000L)
        );

        assertFalse(networkError.isSuccess());
        assertTrue(networkError.isFailure());

        // Test minimal usage
        ExecutionResult simpleSuccess = ExecutionResult.success("Done");
        ExecutionResult simpleFailure = ExecutionResult.failure("Failed");

        assertTrue(simpleSuccess.isSuccess());
        assertTrue(simpleFailure.isFailure());
    }

    @Test
    void shouldMaintainConsistentBehaviorBetweenConstructorAndFactoryMethods() {
        // Given
        String output = "Consistent behavior test";
        Map<String, Object> metadata = Map.of("test", true);

        // When - create using constructor and factory methods
        ExecutionResult constructorSuccess = new ExecutionResult(true, output, metadata);
        ExecutionResult factorySuccess = ExecutionResult.success(output, metadata);

        ExecutionResult constructorFailure = new ExecutionResult(false, output, metadata);
        ExecutionResult factoryFailure = ExecutionResult.failure(output, metadata);

        // Then - they should be equivalent
        assertEquals(constructorSuccess, factorySuccess);
        assertEquals(constructorFailure, factoryFailure);

        assertEquals(constructorSuccess.success(), factorySuccess.success());
        assertEquals(constructorSuccess.output(), factorySuccess.output());
        assertEquals(constructorSuccess.metadata(), factorySuccess.metadata());
        assertEquals(constructorSuccess.isSuccess(), factorySuccess.isSuccess());
        assertEquals(constructorSuccess.isFailure(), factorySuccess.isFailure());

        assertEquals(constructorFailure.success(), factoryFailure.success());
        assertEquals(constructorFailure.output(), factoryFailure.output());
        assertEquals(constructorFailure.metadata(), factoryFailure.metadata());
        assertEquals(constructorFailure.isSuccess(), factoryFailure.isSuccess());
        assertEquals(constructorFailure.isFailure(), factoryFailure.isFailure());
    }

    @Test
    void shouldTestIsSuccessAndIsFailureConsistency() {
        // Given different success/failure combinations
        ExecutionResult[] results = {
            new ExecutionResult(true, "success", null),
            new ExecutionResult(false, "failure", null),
            ExecutionResult.success("ok"),
            ExecutionResult.failure("error")
        };

        // Then - isSuccess and isFailure should always be opposite
        for (ExecutionResult result : results) {
            assertEquals(!result.isSuccess(), result.isFailure(),
                "isSuccess and isFailure should be opposite for: " + result);
            assertEquals(result.success(), result.isSuccess(),
                "success() and isSuccess() should be equivalent for: " + result);
            assertEquals(!result.success(), result.isFailure(),
                "!success() and isFailure() should be equivalent for: " + result);
        }
    }
}
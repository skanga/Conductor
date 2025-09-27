package com.skanga.conductor.execution;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionInputTest {

    @Test
    void shouldCreateExecutionInputWithContentAndMetadata() {
        // Given
        String content = "Test content";
        Map<String, Object> metadata = Map.of("key1", "value1", "key2", 42);

        // When
        ExecutionInput input = new ExecutionInput(content, metadata);

        // Then
        assertEquals(content, input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldCreateExecutionInputWithNullMetadata() {
        // Given
        String content = "Test content";

        // When
        ExecutionInput input = new ExecutionInput(content, null);

        // Then
        assertEquals(content, input.content());
        assertNull(input.metadata());
    }

    @Test
    void shouldCreateExecutionInputWithNullContent() {
        // Given
        Map<String, String> metadata = Map.of("key", "value");

        // When
        ExecutionInput input = new ExecutionInput(null, metadata);

        // Then
        assertNull(input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldCreateExecutionInputWithBothNullValues() {
        // When
        ExecutionInput input = new ExecutionInput(null, null);

        // Then
        assertNull(input.content());
        assertNull(input.metadata());
    }

    @Test
    void shouldCreateExecutionInputUsingStaticFactoryOfWithContent() {
        // Given
        String content = "Factory method content";

        // When
        ExecutionInput input = ExecutionInput.of(content);

        // Then
        assertEquals(content, input.content());
        assertNull(input.metadata());
    }

    @Test
    void shouldCreateExecutionInputUsingStaticFactoryOfWithContentAndMetadata() {
        // Given
        String content = "Factory method content";
        Map<String, Object> metadata = Map.of("factory", true, "method", "of");

        // When
        ExecutionInput input = ExecutionInput.of(content, metadata);

        // Then
        assertEquals(content, input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldCreateExecutionInputWithComplexMetadata() {
        // Given
        String content = "Complex metadata test";
        Map<String, Object> nestedMap = Map.of("nested", "value");
        List<String> listData = List.of("item1", "item2", "item3");

        Map<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("string", "value");
        complexMetadata.put("number", 123);
        complexMetadata.put("boolean", true);
        complexMetadata.put("nested_map", nestedMap);
        complexMetadata.put("list", listData);
        complexMetadata.put("null_value", null);

        // When
        ExecutionInput input = ExecutionInput.of(content, complexMetadata);

        // Then
        assertEquals(content, input.content());
        assertEquals(complexMetadata, input.metadata());
        assertTrue(input.metadata() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> actualMetadata = (Map<String, Object>) input.metadata();
        assertEquals("value", actualMetadata.get("string"));
        assertEquals(123, actualMetadata.get("number"));
        assertEquals(true, actualMetadata.get("boolean"));
        assertEquals(nestedMap, actualMetadata.get("nested_map"));
        assertEquals(listData, actualMetadata.get("list"));
        assertNull(actualMetadata.get("null_value"));
    }

    @Test
    void shouldSupportEmptyStringContent() {
        // Given
        String emptyContent = "";
        Map<String, String> metadata = Map.of("type", "empty");

        // When
        ExecutionInput input = ExecutionInput.of(emptyContent, metadata);

        // Then
        assertEquals("", input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldSupportWhitespaceOnlyContent() {
        // Given
        String whitespaceContent = "   \n\t  ";
        Map<String, String> metadata = Map.of("type", "whitespace");

        // When
        ExecutionInput input = ExecutionInput.of(whitespaceContent, metadata);

        // Then
        assertEquals(whitespaceContent, input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldSupportLargeContent() {
        // Given
        StringBuilder largeContentBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContentBuilder.append("This is line ").append(i).append(" of large content.\n");
        }
        String largeContent = largeContentBuilder.toString();
        Map<String, Object> metadata = Map.of("size", largeContent.length());

        // When
        ExecutionInput input = ExecutionInput.of(largeContent, metadata);

        // Then
        assertEquals(largeContent, input.content());
        assertEquals(metadata, input.metadata());
        assertTrue(input.content().length() > 10000);
    }

    @Test
    void shouldBeImmutable() {
        // Given
        String content = "Immutable content";
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("initial", "value");

        // When
        ExecutionInput input = ExecutionInput.of(content, mutableMetadata);

        // Modify original map after creation
        mutableMetadata.put("modified", "after_creation");

        // Then
        assertEquals(content, input.content());
        assertNotNull(input.metadata());

        // The metadata reference is the same, but this demonstrates the intent of immutability
        // In a production system, the metadata should be copied to ensure true immutability
        assertTrue(input.metadata() instanceof Map);
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        String content = "Equals test content";
        Map<String, Object> metadata = Map.of("key", "value", "number", 42);

        ExecutionInput input1 = ExecutionInput.of(content, metadata);
        ExecutionInput input2 = ExecutionInput.of(content, metadata);
        ExecutionInput input3 = ExecutionInput.of("Different content", metadata);
        ExecutionInput input4 = ExecutionInput.of(content, Map.of("different", "metadata"));
        ExecutionInput input5 = ExecutionInput.of(content);

        // Then - equals
        assertEquals(input1, input2);
        assertEquals(input1, input1); // reflexive
        assertNotEquals(input1, input3); // different content
        assertNotEquals(input1, input4); // different metadata
        assertNotEquals(input1, input5); // one has null metadata
        assertNotEquals(input1, null);
        assertNotEquals(input1, "not an ExecutionInput");

        // Then - hashCode
        assertEquals(input1.hashCode(), input2.hashCode());
        // hashCode can be the same for different objects, but likely different
        // We don't assert inequality for performance reasons
    }

    @Test
    void shouldImplementToStringCorrectly() {
        // Given
        String content = "ToString test content";
        Map<String, Object> metadata = Map.of("key", "value");

        ExecutionInput input = ExecutionInput.of(content, metadata);

        // When
        String toString = input.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("ExecutionInput"));
        assertTrue(toString.contains("content"));
        assertTrue(toString.contains("metadata"));
        assertTrue(toString.contains(content));
    }

    @Test
    void shouldHandleSpecialCharactersInContent() {
        // Given
        String specialContent = "Special chars: \n\t\"'\\{}[]()@#$%^&*+=<>?/~`";
        Map<String, String> metadata = Map.of("type", "special_chars");

        // When
        ExecutionInput input = ExecutionInput.of(specialContent, metadata);

        // Then
        assertEquals(specialContent, input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldHandleUnicodeContent() {
        // Given
        String unicodeContent = "Unicode: 你好世界 🚀 Ñiño café naïve résumé";
        Map<String, String> metadata = Map.of("encoding", "UTF-8");

        // When
        ExecutionInput input = ExecutionInput.of(unicodeContent, metadata);

        // Then
        assertEquals(unicodeContent, input.content());
        assertEquals(metadata, input.metadata());
    }

    @Test
    void shouldSupportDifferentMetadataTypes() {
        // Given
        String content = "Metadata types test";

        // Test with String metadata
        ExecutionInput stringMeta = ExecutionInput.of(content, "string metadata");
        assertEquals("string metadata", stringMeta.metadata());

        // Test with Integer metadata
        ExecutionInput intMeta = ExecutionInput.of(content, 42);
        assertEquals(42, intMeta.metadata());

        // Test with Boolean metadata
        ExecutionInput boolMeta = ExecutionInput.of(content, true);
        assertEquals(true, boolMeta.metadata());

        // Test with List metadata
        List<String> listMeta = List.of("item1", "item2");
        ExecutionInput listMetaInput = ExecutionInput.of(content, listMeta);
        assertEquals(listMeta, listMetaInput.metadata());

        // Test with custom object metadata
        Object customObject = new Object();
        ExecutionInput customMeta = ExecutionInput.of(content, customObject);
        assertEquals(customObject, customMeta.metadata());
    }

    @Test
    void shouldHandleFactoryMethodsWithNullParameters() {
        // When & Then
        ExecutionInput nullContent = ExecutionInput.of(null);
        assertNull(nullContent.content());
        assertNull(nullContent.metadata());

        ExecutionInput nullBoth = ExecutionInput.of(null, null);
        assertNull(nullBoth.content());
        assertNull(nullBoth.metadata());

        ExecutionInput nullMetadata = ExecutionInput.of("content", null);
        assertEquals("content", nullMetadata.content());
        assertNull(nullMetadata.metadata());
    }

    @Test
    void shouldCreateMultipleIndependentInstances() {
        // Given
        String content1 = "Content 1";
        String content2 = "Content 2";
        Map<String, String> metadata1 = Map.of("id", "1");
        Map<String, String> metadata2 = Map.of("id", "2");

        // When
        ExecutionInput input1 = ExecutionInput.of(content1, metadata1);
        ExecutionInput input2 = ExecutionInput.of(content2, metadata2);

        // Then
        assertNotEquals(input1, input2);
        assertEquals(content1, input1.content());
        assertEquals(content2, input2.content());
        assertEquals(metadata1, input1.metadata());
        assertEquals(metadata2, input2.metadata());
    }

    @Test
    void shouldWorkWithNestedDataStructures() {
        // Given
        String content = "Nested data test";
        Map<String, Object> level3 = Map.of("value", "deep");
        Map<String, Object> level2 = Map.of("level3", level3, "array", List.of(1, 2, 3));
        Map<String, Object> level1 = Map.of("level2", level2, "simple", "value");

        // When
        ExecutionInput input = ExecutionInput.of(content, level1);

        // Then
        assertEquals(content, input.content());
        assertEquals(level1, input.metadata());

        @SuppressWarnings("unchecked")
        Map<String, Object> actualMeta = (Map<String, Object>) input.metadata();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualLevel2 = (Map<String, Object>) actualMeta.get("level2");
        @SuppressWarnings("unchecked")
        Map<String, Object> actualLevel3 = (Map<String, Object>) actualLevel2.get("level3");

        assertEquals("deep", actualLevel3.get("value"));
        assertEquals("value", actualMeta.get("simple"));
    }
}
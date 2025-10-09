package com.skanga.conductor.engine;

import com.skanga.conductor.provider.LLMProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for WorkflowBuilder covering builder pattern functionality,
 * stage configuration, validation, and workflow construction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowBuilder Tests")
class WorkflowBuilderTest {

    @Mock
    private LLMProvider mockLLMProvider;

    @Mock
    private Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> mockValidator;

    @Nested
    @DisplayName("Builder Creation Tests")
    class BuilderCreationTests {

        @Test
        @DisplayName("Should create builder instance")
        void shouldCreateBuilderInstance() {
            WorkflowBuilder builder = new WorkflowBuilder();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Should create builder using static factory method")
        void shouldCreateBuilderUsingStaticFactoryMethod() {
            WorkflowBuilder builder = WorkflowBuilder.create();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Should return empty list for new builder")
        void shouldReturnEmptyListForNewBuilder() {
            WorkflowBuilder builder = new WorkflowBuilder();
            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();

            assertNotNull(stages);
            assertTrue(stages.isEmpty());
        }
    }

    @Nested
    @DisplayName("Stage Addition Tests")
    class StageAdditionTests {

        @Test
        @DisplayName("Should add simple stage")
        void shouldAddSimpleStage() {
            WorkflowBuilder builder = new WorkflowBuilder();

            WorkflowBuilder result = builder.addStage(
                "test-stage",
                "test-agent",
                "Test agent description",
                mockLLMProvider,
                "System prompt",
                "Prompt template"
            );

            assertSame(builder, result); // Should return same instance for chaining
            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();
            assertEquals(1, stages.size());

            DefaultWorkflowEngine.StageDefinition stage = stages.get(0);
            assertEquals("test-stage", stage.getName());
            assertEquals("Prompt template", stage.getPromptTemplate());
            assertEquals(3, stage.getMaxRetries()); // Default value
            assertNotNull(stage.getAgentDefinition());
            assertEquals("test-agent", stage.getAgentDefinition().getName());
        }

        @Test
        @DisplayName("Should add stage with validator")
        void shouldAddStageWithValidator() {
            WorkflowBuilder builder = new WorkflowBuilder();

            builder.addStage(
                "validated-stage",
                "test-agent",
                "Test agent description",
                mockLLMProvider,
                "System prompt",
                "Prompt template",
                mockValidator
            );

            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();
            assertEquals(1, stages.size());

            DefaultWorkflowEngine.StageDefinition stage = stages.get(0);
            assertEquals("validated-stage", stage.getName());
            assertSame(mockValidator, stage.getResultValidator());
        }

        @Test
        @DisplayName("Should add stage with full configuration")
        void shouldAddStageWithFullConfiguration() {
            Map<String, Object> taskMetadata = Map.of("priority", "high", "timeout", 300);

            WorkflowBuilder builder = new WorkflowBuilder();

            builder.addStage(
                "full-stage",
                "test-agent",
                "Test agent description",
                mockLLMProvider,
                "System prompt",
                "Prompt template",
                5,
                mockValidator,
                taskMetadata
            );

            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();
            assertEquals(1, stages.size());

            DefaultWorkflowEngine.StageDefinition stage = stages.get(0);
            assertEquals("full-stage", stage.getName());
            assertEquals(5, stage.getMaxRetries());
            assertSame(mockValidator, stage.getResultValidator());
            assertEquals(taskMetadata, stage.getTaskMetadata());
        }

        @Test
        @DisplayName("Should add multiple stages")
        void shouldAddMultipleStages() {
            WorkflowBuilder builder = new WorkflowBuilder();

            builder.addStage("stage1", "agent1", "desc1", mockLLMProvider, "prompt1", "template1")
                   .addStage("stage2", "agent2", "desc2", mockLLMProvider, "prompt2", "template2")
                   .addStage("stage3", "agent3", "desc3", mockLLMProvider, "prompt3", "template3");

            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();
            assertEquals(3, stages.size());

            assertEquals("stage1", stages.get(0).getName());
            assertEquals("stage2", stages.get(1).getName());
            assertEquals("stage3", stages.get(2).getName());
        }

        @Test
        @DisplayName("Should preserve stage order")
        void shouldPreserveStageOrder() {
            WorkflowBuilder builder = new WorkflowBuilder();

            for (int i = 0; i < 10; i++) {
                builder.addStage(
                    "stage" + i,
                    "agent" + i,
                    "description" + i,
                    mockLLMProvider,
                    "system" + i,
                    "template" + i
                );
            }

            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();
            assertEquals(10, stages.size());

            for (int i = 0; i < 10; i++) {
                assertEquals("stage" + i, stages.get(i).getName());
            }
        }
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should support method chaining")
        void shouldSupportMethodChaining() {
            WorkflowBuilder builder = WorkflowBuilder.create()
                .addStage("s1", "a1", "d1", mockLLMProvider, "sys1", "tmpl1")
                .addStage("s2", "a2", "d2", mockLLMProvider, "sys2", "tmpl2")
                .addStage("s3", "a3", "d3", mockLLMProvider, "sys3", "tmpl3");

            assertNotNull(builder);
            assertEquals(3, builder.build().size());
        }

        @Test
        @DisplayName("Should allow reusing builder instance")
        void shouldAllowReusingBuilderInstance() {
            WorkflowBuilder builder = new WorkflowBuilder();

            // First workflow
            builder.addStage("stage1", "agent1", "desc1", mockLLMProvider, "sys1", "tmpl1");
            List<DefaultWorkflowEngine.StageDefinition> workflow1 = builder.build();
            assertEquals(1, workflow1.size());

            // Add more stages
            builder.addStage("stage2", "agent2", "desc2", mockLLMProvider, "sys2", "tmpl2");
            List<DefaultWorkflowEngine.StageDefinition> workflow2 = builder.build();
            assertEquals(2, workflow2.size());

            // Verify first workflow is unchanged (immutable copy)
            assertEquals(1, workflow1.size());
            assertEquals(2, workflow2.size());
        }

        @Test
        @DisplayName("Should return immutable copy of stages")
        void shouldReturnImmutableCopyOfStages() {
            WorkflowBuilder builder = new WorkflowBuilder();
            builder.addStage("stage1", "agent1", "desc1", mockLLMProvider, "sys1", "tmpl1");

            List<DefaultWorkflowEngine.StageDefinition> stages1 = builder.build();
            List<DefaultWorkflowEngine.StageDefinition> stages2 = builder.build();

            assertNotSame(stages1, stages2); // Should be different instances
            assertEquals(stages1.size(), stages2.size()); // But same content

            // Adding to builder should not affect previously returned lists
            builder.addStage("stage2", "agent2", "desc2", mockLLMProvider, "sys2", "tmpl2");
            assertEquals(1, stages1.size());
            assertEquals(1, stages2.size());
        }
    }

    @Nested
    @DisplayName("Validation Helper Tests")
    class ValidationHelperTests {

        @Test
        @DisplayName("Should create contains validator")
        void shouldCreateContainsValidator() {
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> validator =
                WorkflowBuilder.containsValidator("required text");

            DefaultWorkflowEngine.StageResult resultWithText = new DefaultWorkflowEngine.StageResult();
            resultWithText.setOutput("This contains required text in the middle");

            DefaultWorkflowEngine.ValidationResult validResult = validator.apply(resultWithText);
            assertTrue(validResult.isValid());

            DefaultWorkflowEngine.StageResult resultWithoutText = new DefaultWorkflowEngine.StageResult();
            resultWithoutText.setOutput("This does not contain the expected content");

            DefaultWorkflowEngine.ValidationResult invalidResult = validator.apply(resultWithoutText);
            assertFalse(invalidResult.isValid());
            assertTrue(invalidResult.getErrorMessage().contains("required text"));
        }

        @Test
        @DisplayName("Should create min length validator")
        void shouldCreateMinLengthValidator() {
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> validator =
                WorkflowBuilder.minLengthValidator(10);

            DefaultWorkflowEngine.StageResult longResult = new DefaultWorkflowEngine.StageResult();
            longResult.setOutput("This is definitely longer than 10 characters");

            DefaultWorkflowEngine.ValidationResult validResult = validator.apply(longResult);
            assertTrue(validResult.isValid());

            DefaultWorkflowEngine.StageResult shortResult = new DefaultWorkflowEngine.StageResult();
            shortResult.setOutput("Short");

            DefaultWorkflowEngine.ValidationResult invalidResult = validator.apply(shortResult);
            assertFalse(invalidResult.isValid());
            assertTrue(invalidResult.getErrorMessage().contains("too short"));
        }

        @Test
        @DisplayName("Should create forbidden text validator")
        void shouldCreateForbiddenTextValidator() {
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> validator =
                WorkflowBuilder.forbiddenTextValidator("bad", "evil", "forbidden");

            DefaultWorkflowEngine.StageResult cleanResult = new DefaultWorkflowEngine.StageResult();
            cleanResult.setOutput("This is a clean and good output");

            DefaultWorkflowEngine.ValidationResult validResult = validator.apply(cleanResult);
            assertTrue(validResult.isValid());

            DefaultWorkflowEngine.StageResult badResult = new DefaultWorkflowEngine.StageResult();
            badResult.setOutput("This contains BAD content");

            DefaultWorkflowEngine.ValidationResult invalidResult = validator.apply(badResult);
            assertFalse(invalidResult.isValid());
            assertTrue(invalidResult.getErrorMessage().contains("bad"));
        }

        @Test
        @DisplayName("Should create and validator")
        void shouldCreateAndValidator() {
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> lengthValidator =
                WorkflowBuilder.minLengthValidator(5);
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> containsValidator =
                WorkflowBuilder.containsValidator("test");

            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> andValidator =
                WorkflowBuilder.andValidator(lengthValidator, containsValidator);

            // Result that passes both validations
            DefaultWorkflowEngine.StageResult validResult = new DefaultWorkflowEngine.StageResult();
            validResult.setOutput("This is a test output");

            DefaultWorkflowEngine.ValidationResult result1 = andValidator.apply(validResult);
            assertTrue(result1.isValid());

            // Result that fails length validation
            DefaultWorkflowEngine.StageResult shortResult = new DefaultWorkflowEngine.StageResult();
            shortResult.setOutput("test");

            DefaultWorkflowEngine.ValidationResult result2 = andValidator.apply(shortResult);
            assertFalse(result2.isValid());

            // Result that fails contains validation
            DefaultWorkflowEngine.StageResult noTestResult = new DefaultWorkflowEngine.StageResult();
            noTestResult.setOutput("This is a long output without the required word");

            DefaultWorkflowEngine.ValidationResult result3 = andValidator.apply(noTestResult);
            assertFalse(result3.isValid());
        }

        @Test
        @DisplayName("Should handle null output in validators")
        void shouldHandleNullOutputInValidators() {
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> containsValidator =
                WorkflowBuilder.containsValidator("test");
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> lengthValidator =
                WorkflowBuilder.minLengthValidator(5);
            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> forbiddenValidator =
                WorkflowBuilder.forbiddenTextValidator("bad");

            DefaultWorkflowEngine.StageResult nullResult = new DefaultWorkflowEngine.StageResult();
            nullResult.setOutput(null);

            // Contains validator should fail with null
            assertFalse(containsValidator.apply(nullResult).isValid());

            // Length validator should fail with null
            assertFalse(lengthValidator.apply(nullResult).isValid());

            // Forbidden validator should pass with null (no forbidden text found)
            assertTrue(forbiddenValidator.apply(nullResult).isValid());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should reject null stage name parameter")
        void shouldHandleNullParametersInStageAddition() {
            WorkflowBuilder builder = new WorkflowBuilder();

            assertThrows(IllegalArgumentException.class, () -> {
                builder.addStage(null, null, null, null, null, null);
            });
        }

        @Test
        @DisplayName("Should reject empty stage name parameter")
        void shouldHandleEmptyStringParameters() {
            WorkflowBuilder builder = new WorkflowBuilder();

            assertThrows(IllegalArgumentException.class, () -> {
                builder.addStage("", "", "", mockLLMProvider, "", "");
            });
        }

        @Test
        @DisplayName("Should accept zero retries but reject negative retries")
        void shouldHandleZeroOrNegativeRetries() {
            WorkflowBuilder builder1 = new WorkflowBuilder();
            WorkflowBuilder builder2 = new WorkflowBuilder();

            // Zero retries should be accepted
            builder1.addStage("stage1", "agent1", "desc1", mockLLMProvider, "sys1", "tmpl1", 0, null, null);
            List<DefaultWorkflowEngine.StageDefinition> stages = builder1.build();
            assertEquals(1, stages.size());
            assertEquals(0, stages.get(0).getMaxRetries());

            // Negative retries should be rejected
            assertThrows(IllegalArgumentException.class, () -> {
                builder2.addStage("stage2", "agent2", "desc2", mockLLMProvider, "sys2", "tmpl2", -5, null, null);
            });
        }

        @Test
        @DisplayName("Should handle empty task metadata")
        void shouldHandleEmptyTaskMetadata() {
            WorkflowBuilder builder = new WorkflowBuilder();

            Map<String, Object> emptyMetadata = new HashMap<>();
            builder.addStage("stage1", "agent1", "desc1", mockLLMProvider, "sys1", "tmpl1", 3, null, emptyMetadata);

            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();
            assertEquals(1, stages.size());

            DefaultWorkflowEngine.StageDefinition stage = stages.get(0);
            assertNotNull(stage.getTaskMetadata());
            assertTrue(stage.getTaskMetadata().isEmpty());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should build complex workflow with all features")
        void shouldBuildComplexWorkflowWithAllFeatures() {
            Map<String, Object> metadata1 = Map.of("priority", "high", "timeout", 300);
            Map<String, Object> metadata2 = Map.of("priority", "low", "cache", true);

            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> validator1 =
                WorkflowBuilder.andValidator(
                    WorkflowBuilder.minLengthValidator(50),
                    WorkflowBuilder.containsValidator("summary")
                );

            Function<DefaultWorkflowEngine.StageResult, DefaultWorkflowEngine.ValidationResult> validator2 =
                WorkflowBuilder.forbiddenTextValidator("error", "failed", "invalid");

            WorkflowBuilder builder = WorkflowBuilder.create()
                .addStage("analyze", "analyzer", "Analysis agent", mockLLMProvider,
                         "You are an analyzer", "Analyze: {{input}}", validator1)
                .addStage("validate", "validator", "Validation agent", mockLLMProvider,
                         "You are a validator", "Validate: {{analysis}}", 5, validator2, metadata1)
                .addStage("summarize", "summarizer", "Summary agent", mockLLMProvider,
                         "You are a summarizer", "Summarize: {{validation}}", 2, null, metadata2);

            List<DefaultWorkflowEngine.StageDefinition> stages = builder.build();

            assertEquals(3, stages.size());

            // Verify analyze stage
            DefaultWorkflowEngine.StageDefinition analyzeStage = stages.get(0);
            assertEquals("analyze", analyzeStage.getName());
            assertEquals("Analyze: {{input}}", analyzeStage.getPromptTemplate());
            assertEquals(3, analyzeStage.getMaxRetries()); // Default
            assertNotNull(analyzeStage.getResultValidator());
            assertNotNull(analyzeStage.getAgentDefinition());
            assertEquals("analyzer", analyzeStage.getAgentDefinition().getName());

            // Verify validate stage
            DefaultWorkflowEngine.StageDefinition validateStage = stages.get(1);
            assertEquals("validate", validateStage.getName());
            assertEquals(5, validateStage.getMaxRetries());
            assertEquals(metadata1, validateStage.getTaskMetadata());

            // Verify summarize stage
            DefaultWorkflowEngine.StageDefinition summarizeStage = stages.get(2);
            assertEquals("summarize", summarizeStage.getName());
            assertEquals(2, summarizeStage.getMaxRetries());
            assertEquals(metadata2, summarizeStage.getTaskMetadata());
        }

        @Test
        @DisplayName("Should support workflow variations")
        void shouldSupportWorkflowVariations() {
            WorkflowBuilder baseBuilder = WorkflowBuilder.create()
                .addStage("common1", "agent1", "desc1", mockLLMProvider, "sys1", "tmpl1")
                .addStage("common2", "agent2", "desc2", mockLLMProvider, "sys2", "tmpl2");

            // Variation 1: Add validation stage
            List<DefaultWorkflowEngine.StageDefinition> workflow1 = baseBuilder
                .addStage("validate", "validator", "Validation", mockLLMProvider, "sys", "tmpl")
                .build();

            // Variation 2: Add different final stage
            List<DefaultWorkflowEngine.StageDefinition> workflow2 = baseBuilder
                .addStage("finalize", "finalizer", "Finalization", mockLLMProvider, "sys", "tmpl")
                .build();

            assertEquals(3, workflow1.size());
            assertEquals(4, workflow2.size()); // baseBuilder now has 3 stages + 1 new = 4

            assertEquals("validate", workflow1.get(2).getName());
            assertEquals("finalize", workflow2.get(3).getName());
        }
    }
}
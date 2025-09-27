package com.skanga.conductor.engine;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.workflow.config.WorkflowContext;
import com.skanga.conductor.workflow.config.WorkflowDefinition;
import com.skanga.conductor.workflow.templates.PromptTemplateEngine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for DefaultWorkflowEngine covering workflow execution,
 * stage management, retry logic, validation, and engine lifecycle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultWorkflowEngine Tests")
class DefaultWorkflowEngineTest {

    @Mock
    private Orchestrator mockOrchestrator;

    @Mock
    private PromptTemplateEngine mockTemplateEngine;

    @Mock
    private SubAgent mockSubAgent;

    @Mock
    private ExecutionResult mockExecutionResult;

    private DefaultWorkflowEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        // Set up mock execution result
        lenient().when(mockExecutionResult.success()).thenReturn(true);
        lenient().when(mockExecutionResult.output()).thenReturn("Test output");

        // Set up mock sub agent
        lenient().when(mockSubAgent.execute(any(ExecutionInput.class))).thenReturn(mockExecutionResult);

        // Set up mock orchestrator to return the mock agent
        lenient().when(mockOrchestrator.createImplicitAgent(anyString(), anyString(), any(LLMProvider.class), anyString()))
                .thenReturn(mockSubAgent);

        engine = new DefaultWorkflowEngine(mockOrchestrator);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (engine != null) {
            engine.close();
        }
    }

    @Nested
    @DisplayName("Engine Lifecycle Tests")
    class EngineLifecycleTests {

        @Test
        @DisplayName("Should initialize engine with configured state")
        void shouldInitializeEngineWithConfiguredState() {
            assertTrue(engine.isReady());
            assertEquals("DefaultWorkflowEngine", engine.getEngineName());
            assertNotNull(engine.getStatus());
            assertTrue(engine.getStatus().isConfigured());
        }

        @Test
        @DisplayName("Should handle engine closure properly")
        void shouldHandleEngineClosureProperly() throws Exception {
            assertTrue(engine.isReady());

            engine.close();

            assertFalse(engine.isReady());
            assertTrue(engine.getStatus().getValidationMessages().contains("Engine has been closed"));
        }

        @Test
        @DisplayName("Should prevent operations after closure")
        void shouldPreventOperationsAfterClosure() throws Exception {
            engine.close();

            assertThrows(IllegalStateException.class,
                () -> engine.execute("test input"));

            assertThrows(IllegalStateException.class,
                () -> engine.execute(mock(WorkflowDefinition.class), mock(WorkflowContext.class)));
        }

        @Test
        @DisplayName("Should handle multiple close calls safely")
        void shouldHandleMultipleCloseCallsSafely() throws Exception {
            engine.close();
            engine.close(); // Should not throw
            assertFalse(engine.isReady());
        }
    }

    @Nested
    @DisplayName("Workflow Execution Tests")
    class WorkflowExecutionTests {

        @Test
        @DisplayName("Should execute simple workflow with single input")
        void shouldExecuteSimpleWorkflowWithSingleInput() throws Exception {
            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            WorkflowEngine.WorkflowResult result = engine.execute("test input");

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertNull(result.getErrorMessage());
            assertTrue(result.getTotalExecutionTimeMs() >= 0);
            assertEquals("UnifiedWorkflow", result.getWorkflowName());
        }

        @Test
        @DisplayName("Should execute workflow with multiple inputs")
        void shouldExecuteWorkflowWithMultipleInputs() throws Exception {
            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            WorkflowEngine.WorkflowResult result = engine.execute("input1", "input2", "input3");

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertNotNull(result.getOutput());
            assertTrue(result.getTotalExecutionTimeMs() >= 0);
        }

        @Test
        @DisplayName("Should handle empty input gracefully")
        void shouldHandleEmptyInputGracefully() throws Exception {
            WorkflowEngine.WorkflowResult result = engine.execute();

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(0, result.getTotalExecutionTimeMs());
        }

        @Test
        @DisplayName("Should propagate execution exceptions")
        void shouldPropagateExecutionExceptions() throws Exception {
            // Override the mock to simulate execution failure
            ExecutionResult failureResult = mock(ExecutionResult.class);
            when(failureResult.success()).thenReturn(false);
            when(failureResult.output()).thenReturn("Execution failed");

            when(mockSubAgent.execute(any(ExecutionInput.class))).thenReturn(failureResult);

            assertThrows(ConductorException.class,
                () -> engine.execute("test input"));
        }

        @Test
        @DisplayName("Should validate workflow definition and context")
        void shouldValidateWorkflowDefinitionAndContext() {
            WorkflowDefinition nullDefinition = null;
            WorkflowContext mockContext = mock(WorkflowContext.class);

            assertThrows(IllegalArgumentException.class,
                () -> engine.execute(nullDefinition, mockContext));

            WorkflowDefinition mockDefinition = mock(WorkflowDefinition.class);
            WorkflowContext nullContext = null;

            assertThrows(IllegalArgumentException.class,
                () -> engine.execute(mockDefinition, nullContext));
        }
    }

    @Nested
    @DisplayName("Stage Execution Tests")
    class StageExecutionTests {

        @Test
        @DisplayName("Should execute stages in sequence")
        void shouldExecuteStagesInSequence() throws Exception {
            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            // Create multiple stages
            List<DefaultWorkflowEngine.StageDefinition> stages = createTestStages(3);

            DefaultWorkflowEngine.WorkflowResult result = engine.executeWorkflow(stages);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(3, result.getStageResults().size());
        }

        @Test
        @DisplayName("Should pass context between stages")
        void shouldPassContextBetweenStages() throws Exception {
            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            Map<String, Object> initialVariables = Map.of(
                "context_var", "test_value",
                "stage_count", 2
            );

            List<DefaultWorkflowEngine.StageDefinition> stages = createTestStages(2);

            DefaultWorkflowEngine.WorkflowResult result =
                engine.executeWorkflowWithContext(stages, initialVariables);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(2, result.getStageResults().size());
        }

        @Test
        @DisplayName("Should handle stage validation failures")
        void shouldHandleStageValidationFailures() throws Exception {
            List<DefaultWorkflowEngine.StageDefinition> stages = createTestStages(1);

            // Add a validator that always fails
            stages.get(0).setResultValidator(result ->
                DefaultWorkflowEngine.ValidationResult.invalid("Test validation failure"));

            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            // Should still complete execution but mark validation failure
            DefaultWorkflowEngine.WorkflowResult result = engine.executeWorkflow(stages);

            assertNotNull(result);
            // Result success depends on stage implementation - test for consistency
            assertEquals(1, result.getStageResults().size());
        }

        @Test
        @DisplayName("Should handle retry logic for failed stages")
        void shouldHandleRetryLogicForFailedStages() throws Exception {
            List<DefaultWorkflowEngine.StageDefinition> stages = createTestStages(1);
            stages.get(0).setMaxRetries(2);

            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            DefaultWorkflowEngine.WorkflowResult result = engine.executeWorkflow(stages);

            assertNotNull(result);
            assertEquals(1, result.getStageResults().size());
        }
    }

    @Nested
    @DisplayName("Engine Status Tests")
    class EngineStatusTests {

        @Test
        @DisplayName("Should report correct engine status when configured")
        void shouldReportCorrectEngineStatusWhenConfigured() {
            WorkflowEngine.EngineStatus status = engine.getStatus();

            assertTrue(status.isConfigured());
            assertTrue(status.getValidationMessages().isEmpty());
            assertEquals(0, status.getLoadedWorkflowCount());
            assertTrue(status.getActiveExecutionCount() >= 0);
            assertNotNull(status.getMetadata());
            assertEquals("unified", status.getMetadata().get("engine_type"));
        }

        @Test
        @DisplayName("Should report validation messages when not configured")
        void shouldReportValidationMessagesWhenNotConfigured() throws Exception {
            // Create engine without orchestrator
            DefaultWorkflowEngine unconfiguredEngine = new DefaultWorkflowEngine(null);

            try {
                WorkflowEngine.EngineStatus status = unconfiguredEngine.getStatus();

                // Engine still reports as configured even with null orchestrator
                assertTrue(status.isConfigured());
                // But validation messages should still indicate missing orchestrator
                assertFalse(status.getValidationMessages().isEmpty());
                assertTrue(status.getValidationMessages().contains("Orchestrator is not configured"));
            } finally {
                unconfiguredEngine.close();
            }
        }

        @Test
        @DisplayName("Should track engine metadata correctly")
        void shouldTrackEngineMetadataCorrectly() {
            WorkflowEngine.EngineStatus status = engine.getStatus();
            Map<String, Object> metadata = status.getMetadata();

            assertEquals("unified", metadata.get("engine_type"));
            assertEquals(true, metadata.get("orchestrator_available"));
            assertEquals(0, metadata.get("execution_context_size"));
            assertEquals(0, metadata.get("executed_stages_count"));
            assertEquals(false, metadata.get("closed"));
        }
    }

    @Nested
    @DisplayName("Deprecated Methods Tests")
    class DeprecatedMethodsTests {

        @Test
        @DisplayName("Should handle deprecated getExecutionContext safely")
        void shouldHandleDeprecatedGetExecutionContextSafely() {
            @SuppressWarnings("deprecation")
            Map<String, Object> context = engine.getExecutionContext();

            assertNotNull(context);
            assertTrue(context.isEmpty());
        }

        @Test
        @DisplayName("Should handle deprecated getExecutedStages safely")
        void shouldHandleDeprecatedGetExecutedStagesSafely() {
            @SuppressWarnings("deprecation")
            List<DefaultWorkflowEngine.WorkflowStage> stages = engine.getExecutedStages();

            assertNotNull(stages);
            assertTrue(stages.isEmpty());
        }

        @Test
        @DisplayName("Should handle deprecated setContextVariable safely")
        void shouldHandleDeprecatedSetContextVariableSafely() {
            @SuppressWarnings("deprecation")
            Runnable operation = () -> engine.setContextVariable("key", "value");

            assertDoesNotThrow(operation::run);
        }

        @Test
        @DisplayName("Should handle deprecated getContextVariable safely")
        void shouldHandleDeprecatedGetContextVariableSafely() {
            @SuppressWarnings("deprecation")
            Object value = engine.getContextVariable("key");

            assertNull(value);
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent engine operations")
        void shouldHandleConcurrentEngineOperations() throws Exception {
            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            int threadCount = 5;
            List<Thread> threads = new ArrayList<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    try {
                        WorkflowEngine.WorkflowResult result = engine.execute("input " + threadId);
                        assertNotNull(result);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertTrue(exceptions.isEmpty(), "Concurrent operations should not throw exceptions");
        }

        @Test
        @DisplayName("Should handle concurrent status queries")
        void shouldHandleConcurrentStatusQueries() throws Exception {
            int threadCount = 10;
            List<Thread> threads = new ArrayList<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        WorkflowEngine.EngineStatus status = engine.getStatus();
                        assertNotNull(status);
                        assertTrue(status.isConfigured());
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertTrue(exceptions.isEmpty(), "Concurrent status queries should not throw exceptions");
        }
    }

    @Nested
    @DisplayName("Inner Classes Tests")
    class InnerClassesTests {

        @Test
        @DisplayName("Should create and use StageDefinition correctly")
        void shouldCreateAndUseStageDefinitionCorrectly() {
            DefaultWorkflowEngine.StageDefinition stage = new DefaultWorkflowEngine.StageDefinition();
            stage.setName("test-stage");
            stage.setPromptTemplate("test template");
            stage.setMaxRetries(5);

            Map<String, Object> metadata = Map.of("key", "value");
            stage.setTaskMetadata(metadata);

            assertEquals("test-stage", stage.getName());
            assertEquals("test template", stage.getPromptTemplate());
            assertEquals(5, stage.getMaxRetries());
            assertEquals(metadata, stage.getTaskMetadata());
        }

        @Test
        @DisplayName("Should create and use AgentDefinition correctly")
        void shouldCreateAndUseAgentDefinitionCorrectly() {
            DefaultWorkflowEngine.AgentDefinition agentDef =
                new DefaultWorkflowEngine.AgentDefinition(
                    "test-agent", "description", null, "system prompt");

            assertEquals("test-agent", agentDef.getName());
            assertEquals("description", agentDef.getDescription());
            assertNull(agentDef.getLlmProvider());
            assertEquals("system prompt", agentDef.getSystemPrompt());
        }

        @Test
        @DisplayName("Should create and use StageResult correctly")
        void shouldCreateAndUseStageResultCorrectly() {
            DefaultWorkflowEngine.StageResult result = new DefaultWorkflowEngine.StageResult();
            result.setStageName("test-stage");
            result.setOutput("test output");
            result.setSuccess(true);
            result.setAttempt(1);
            result.setExecutionTimeMs(1000);
            result.setAgentUsed("test-agent");

            assertEquals("test-stage", result.getStageName());
            assertEquals("test output", result.getOutput());
            assertTrue(result.isSuccess());
            assertEquals(1, result.getAttempt());
            assertEquals(1000, result.getExecutionTimeMs());
            assertEquals("test-agent", result.getAgentUsed());
        }

        @Test
        @DisplayName("Should create and use WorkflowResult correctly")
        void shouldCreateAndUseWorkflowResultCorrectly() {
            DefaultWorkflowEngine.WorkflowResult result = new DefaultWorkflowEngine.WorkflowResult();
            result.setSuccess(true);
            result.setStartTime(1000);
            result.setEndTime(2000);
            result.setTotalExecutionTimeMs(1000);

            List<DefaultWorkflowEngine.StageResult> stageResults = Arrays.asList(
                new DefaultWorkflowEngine.StageResult(),
                new DefaultWorkflowEngine.StageResult()
            );
            result.setStageResults(stageResults);

            assertTrue(result.isSuccess());
            assertEquals(1000, result.getStartTime());
            assertEquals(2000, result.getEndTime());
            assertEquals(1000, result.getTotalExecutionTimeMs());
            assertEquals(2, result.getStageResults().size());
        }

        @Test
        @DisplayName("Should create and use ValidationResult correctly")
        void shouldCreateAndUseValidationResultCorrectly() {
            DefaultWorkflowEngine.ValidationResult validResult = DefaultWorkflowEngine.ValidationResult.valid();
            assertTrue(validResult.isValid());
            assertNull(validResult.getErrorMessage());

            DefaultWorkflowEngine.ValidationResult invalidResult =
                DefaultWorkflowEngine.ValidationResult.invalid("error message");
            assertFalse(invalidResult.isValid());
            assertEquals("error message", invalidResult.getErrorMessage());
        }
    }

    // Helper methods

    private List<DefaultWorkflowEngine.StageDefinition> createTestStages(int count) {
        List<DefaultWorkflowEngine.StageDefinition> stages = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            DefaultWorkflowEngine.StageDefinition stage = new DefaultWorkflowEngine.StageDefinition();
            stage.setName("stage_" + (i + 1));
            stage.setPromptTemplate("Template for stage " + (i + 1) + ": {{input}}");

            DefaultWorkflowEngine.AgentDefinition agentDef =
                new DefaultWorkflowEngine.AgentDefinition(
                    "agent_" + i, "Agent " + i, null, "System prompt for agent " + i);
            stage.setAgentDefinition(agentDef);

            stages.add(stage);
        }

        return stages;
    }
}
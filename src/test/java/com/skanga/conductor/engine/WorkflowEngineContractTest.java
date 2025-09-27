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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Contract tests for WorkflowEngine interface to ensure all implementations
 * conform to the expected behavior and interface contracts.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowEngine Contract Tests")
class WorkflowEngineContractTest {

    @Mock
    private Orchestrator mockOrchestrator;

    @Mock
    private PromptTemplateEngine mockTemplateEngine;

    @Mock
    private WorkflowDefinition mockWorkflowDefinition;

    @Mock
    private WorkflowContext mockWorkflowContext;

    @Mock
    private SubAgent mockSubAgent;

    @Mock
    private ExecutionResult mockExecutionResult;

    @Nested
    @DisplayName("DefaultWorkflowEngine Contract Tests")
    class DefaultWorkflowEngineContractTests {

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

        @Test
        @DisplayName("Should implement WorkflowEngine interface")
        void shouldImplementWorkflowEngineInterface() {
            assertTrue(engine instanceof WorkflowEngine);
            assertTrue(engine instanceof AutoCloseable);
        }

        @Test
        @DisplayName("Should return consistent engine name")
        void shouldReturnConsistentEngineName() {
            String name1 = engine.getEngineName();
            String name2 = engine.getEngineName();

            assertNotNull(name1);
            assertNotNull(name2);
            assertEquals(name1, name2);
            assertEquals("DefaultWorkflowEngine", name1);
        }

        @Test
        @DisplayName("Should provide valid engine status")
        void shouldProvideValidEngineStatus() {
            WorkflowEngine.EngineStatus status = engine.getStatus();

            assertNotNull(status);
            assertNotNull(status.getValidationMessages());
            assertNotNull(status.getMetadata());
            assertTrue(status.getLoadedWorkflowCount() >= 0);
            assertTrue(status.getActiveExecutionCount() >= 0);
        }

        @Test
        @DisplayName("Should handle readiness checks correctly")
        void shouldHandleReadinessChecksCorrectly() {
            // Initially ready
            assertTrue(engine.isReady());

            // After close should not be ready
            assertDoesNotThrow(() -> engine.close());
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should execute with string inputs")
        void shouldExecuteWithStringInputs() throws Exception {
            lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                .thenReturn("processed prompt");

            WorkflowEngine.WorkflowResult result = engine.execute("test input");

            assertNotNull(result);
            assertNotNull(result.getWorkflowName());
            assertTrue(result.getStartTime() > 0);
            assertTrue(result.getEndTime() >= result.getStartTime());
            assertTrue(result.getTotalExecutionTimeMs() >= 0);
        }

        @Test
        @DisplayName("Should execute with workflow definition and context")
        void shouldExecuteWithWorkflowDefinitionAndContext() throws Exception {
            WorkflowDefinition.WorkflowMetadata metadata = mock(WorkflowDefinition.WorkflowMetadata.class);
            when(metadata.getName()).thenReturn("test-workflow");
            when(mockWorkflowDefinition.getMetadata()).thenReturn(metadata);

            WorkflowEngine.WorkflowResult result = engine.execute(mockWorkflowDefinition, mockWorkflowContext);

            assertNotNull(result);
            assertNotNull(result.getWorkflowName());
        }

        @Test
        @DisplayName("Should handle closure properly")
        void shouldHandleClosureProperly() throws Exception {
            assertTrue(engine.isReady());

            engine.close();

            assertFalse(engine.isReady());
            assertThrows(IllegalStateException.class, () -> engine.execute("test"));
        }
    }

    @Nested
    @DisplayName("YamlWorkflowEngine Contract Tests")
    class YamlWorkflowEngineContractTests {

        private YamlWorkflowEngine engine;

        @BeforeEach
        void setUp() {
            engine = new YamlWorkflowEngine();
        }

        @AfterEach
        void tearDown() {
            if (engine != null) {
                engine.close();
            }
        }

        @Test
        @DisplayName("Should implement WorkflowEngine interface")
        void shouldImplementWorkflowEngineInterface() {
            assertTrue(engine instanceof WorkflowEngine);
            assertTrue(engine instanceof AutoCloseable);
        }

        @Test
        @DisplayName("Should return consistent engine name")
        void shouldReturnConsistentEngineName() {
            String name1 = engine.getEngineName();
            String name2 = engine.getEngineName();

            assertNotNull(name1);
            assertNotNull(name2);
            assertEquals(name1, name2);
            assertEquals("YamlWorkflowEngine", name1);
        }

        @Test
        @DisplayName("Should provide valid engine status")
        void shouldProvideValidEngineStatus() {
            WorkflowEngine.EngineStatus status = engine.getStatus();

            assertNotNull(status);
            assertNotNull(status.getValidationMessages());
            assertNotNull(status.getMetadata());
            assertTrue(status.getLoadedWorkflowCount() >= 0);
            assertTrue(status.getActiveExecutionCount() >= 0);
        }

        @Test
        @DisplayName("Should handle readiness checks correctly")
        void shouldHandleReadinessChecksCorrectly() {
            // Initially not ready (needs configuration)
            assertFalse(engine.isReady());

            // After close should not be ready
            engine.close();
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should validate configuration before execution")
        void shouldValidateConfigurationBeforeExecution() {
            assertThrows(ConductorException.class, () -> engine.execute("test"));
            assertThrows(IllegalArgumentException.class, () ->
                engine.execute(null, mockWorkflowContext));
            assertThrows(IllegalArgumentException.class, () ->
                engine.execute(mockWorkflowDefinition, null));
        }

        @Test
        @DisplayName("Should handle closure properly")
        void shouldHandleClosureProperly() {
            engine.close();
            assertFalse(engine.isReady());
            assertThrows(IllegalStateException.class, () -> engine.execute("test"));
        }
    }

    @Nested
    @DisplayName("WorkflowResult Contract Tests")
    class WorkflowResultContractTests {

        @Test
        @DisplayName("Should provide complete result information from DefaultWorkflowEngine")
        void shouldProvideCompleteResultInformationFromDefaultWorkflowEngine() throws Exception {
            // Create fresh mocks for this test to avoid interference
            SubAgent testAgent = mock(SubAgent.class);
            ExecutionResult testResult = mock(ExecutionResult.class);
            Orchestrator testOrchestrator = mock(Orchestrator.class);

            // Set up fresh mocks
            when(testResult.success()).thenReturn(true);
            when(testResult.output()).thenReturn("Test output from agent");
            when(testAgent.execute(any(ExecutionInput.class))).thenReturn(testResult);
            when(testOrchestrator.createImplicitAgent(anyString(), anyString(), any(LLMProvider.class), anyString()))
                    .thenReturn(testAgent);

            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(testOrchestrator);
            try {
                WorkflowEngine.WorkflowResult result = engine.execute("test");

                assertNotNull(result);
                assertNotNull(result.getWorkflowName());
                assertTrue(result.getStartTime() > 0);
                assertTrue(result.getEndTime() >= result.getStartTime());
                assertTrue(result.getTotalExecutionTimeMs() >= 0);
                assertEquals(result.getEndTime() - result.getStartTime(), result.getTotalExecutionTimeMs());

                // Should have either success or error
                if (!result.isSuccess()) {
                    assertNotNull(result.getErrorMessage());
                }
            } finally {
                engine.close();
            }
        }

        @Test
        @DisplayName("Should maintain result consistency")
        void shouldMaintainResultConsistency() throws Exception {
            // Create fresh mocks for this test to avoid interference
            SubAgent testAgent = mock(SubAgent.class);
            ExecutionResult testResult = mock(ExecutionResult.class);
            Orchestrator testOrchestrator = mock(Orchestrator.class);

            // Set up fresh mocks
            when(testResult.success()).thenReturn(true);
            when(testResult.output()).thenReturn("Consistent test output");
            when(testAgent.execute(any(ExecutionInput.class))).thenReturn(testResult);
            when(testOrchestrator.createImplicitAgent(anyString(), anyString(), any(LLMProvider.class), anyString()))
                    .thenReturn(testAgent);

            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(testOrchestrator);
            try {
                WorkflowEngine.WorkflowResult result = engine.execute("test");

                // Multiple calls to same methods should return same values
                assertEquals(result.isSuccess(), result.isSuccess());
                assertEquals(result.getWorkflowName(), result.getWorkflowName());
                assertEquals(result.getStartTime(), result.getStartTime());
                assertEquals(result.getEndTime(), result.getEndTime());
                assertEquals(result.getTotalExecutionTimeMs(), result.getTotalExecutionTimeMs());
                assertEquals(result.getErrorMessage(), result.getErrorMessage());
                assertEquals(result.getOutput(), result.getOutput());
            } finally {
                engine.close();
            }
        }
    }

    @Nested
    @DisplayName("EngineStatus Contract Tests")
    class EngineStatusContractTests {

        @Test
        @DisplayName("Should provide complete status information from DefaultWorkflowEngine")
        void shouldProvideCompleteStatusInformationFromDefaultWorkflowEngine() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);
            try {
                WorkflowEngine.EngineStatus status = engine.getStatus();

                assertNotNull(status);
                assertNotNull(status.getValidationMessages());
                assertNotNull(status.getMetadata());
                assertTrue(status.getLoadedWorkflowCount() >= 0);
                assertTrue(status.getActiveExecutionCount() >= 0);

                // Validation messages should be empty if configured
                if (status.isConfigured()) {
                    assertTrue(status.getValidationMessages().isEmpty());
                }

                // Metadata should contain engine type
                Map<String, Object> metadata = status.getMetadata();
                assertTrue(metadata.containsKey("engine_type"));
            } finally {
                engine.close();
            }
        }

        @Test
        @DisplayName("Should provide complete status information from YamlWorkflowEngine")
        void shouldProvideCompleteStatusInformationFromYamlWorkflowEngine() {
            YamlWorkflowEngine engine = new YamlWorkflowEngine();
            try {
                WorkflowEngine.EngineStatus status = engine.getStatus();

                assertNotNull(status);
                assertNotNull(status.getValidationMessages());
                assertNotNull(status.getMetadata());
                assertTrue(status.getLoadedWorkflowCount() >= 0);
                assertTrue(status.getActiveExecutionCount() >= 0);

                // Should not be configured initially
                assertFalse(status.isConfigured());
                assertFalse(status.getValidationMessages().isEmpty());

                // Metadata should contain engine type
                Map<String, Object> metadata = status.getMetadata();
                assertTrue(metadata.containsKey("engine_type"));
                assertEquals("yaml", metadata.get("engine_type"));
            } finally {
                engine.close();
            }
        }

        @Test
        @DisplayName("Should maintain status consistency")
        void shouldMaintainStatusConsistency() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);
            try {
                WorkflowEngine.EngineStatus status = engine.getStatus();

                // Multiple calls should return consistent values
                assertEquals(status.isConfigured(), status.isConfigured());
                assertEquals(status.getLoadedWorkflowCount(), status.getLoadedWorkflowCount());
                assertEquals(status.getActiveExecutionCount(), status.getActiveExecutionCount());

                List<String> messages1 = status.getValidationMessages();
                List<String> messages2 = status.getValidationMessages();
                assertEquals(messages1.size(), messages2.size());

                Map<String, Object> metadata1 = status.getMetadata();
                Map<String, Object> metadata2 = status.getMetadata();
                assertEquals(metadata1.size(), metadata2.size());
            } finally {
                engine.close();
            }
        }
    }

    @Nested
    @DisplayName("AutoCloseable Contract Tests")
    class AutoCloseableContractTests {

        @Test
        @DisplayName("Should handle multiple close calls safely for DefaultWorkflowEngine")
        void shouldHandleMultipleCloseCallsSafelyForDefaultWorkflowEngine() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);

            assertTrue(engine.isReady());

            // First close
            engine.close();
            assertFalse(engine.isReady());

            // Second close should not throw
            assertDoesNotThrow(() -> engine.close());
            assertFalse(engine.isReady());

            // Third close should not throw
            assertDoesNotThrow(() -> engine.close());
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should handle multiple close calls safely for YamlWorkflowEngine")
        void shouldHandleMultipleCloseCallsSafelyForYamlWorkflowEngine() {
            YamlWorkflowEngine engine = new YamlWorkflowEngine();

            // First close
            engine.close();
            assertFalse(engine.isReady());

            // Second close should not throw
            assertDoesNotThrow(() -> engine.close());
            assertFalse(engine.isReady());

            // Third close should not throw
            assertDoesNotThrow(() -> engine.close());
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should prevent operations after close")
        void shouldPreventOperationsAfterClose() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);

            engine.close();

            assertThrows(IllegalStateException.class, () -> engine.execute("test"));
            assertThrows(IllegalStateException.class, () ->
                engine.execute(mockWorkflowDefinition, mockWorkflowContext));
        }
    }

    @Nested
    @DisplayName("Exception Handling Contract Tests")
    class ExceptionHandlingContractTests {

        @Test
        @DisplayName("Should throw ConductorException for execution failures")
        void shouldThrowConductorExceptionForExecutionFailures() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);
            try {
                lenient().when(mockTemplateEngine.renderString(anyString(), any(Map.class)))
                    .thenThrow(new RuntimeException("Template processing failed"));

                assertThrows(ConductorException.class, () -> engine.execute("test"));
            } finally {
                engine.close();
            }
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null parameters")
        void shouldThrowIllegalArgumentExceptionForNullParameters() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);
            try {
                assertThrows(IllegalArgumentException.class, () ->
                    engine.execute(null, mockWorkflowContext));
                assertThrows(IllegalArgumentException.class, () ->
                    engine.execute(mockWorkflowDefinition, null));
            } finally {
                engine.close();
            }
        }

        @Test
        @DisplayName("Should throw IllegalStateException for closed engine operations")
        void shouldThrowIllegalStateExceptionForClosedEngineOperations() throws Exception {
            DefaultWorkflowEngine engine = new DefaultWorkflowEngine(mockOrchestrator);
            engine.close();

            assertThrows(IllegalStateException.class, () -> engine.execute("test"));
            assertThrows(IllegalStateException.class, () ->
                engine.execute(mockWorkflowDefinition, mockWorkflowContext));
        }
    }
}
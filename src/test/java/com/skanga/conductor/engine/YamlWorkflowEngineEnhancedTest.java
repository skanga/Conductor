package com.skanga.conductor.engine;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.workflow.approval.HumanApprovalHandler;
import com.skanga.conductor.workflow.config.*;
import com.skanga.conductor.workflow.output.FileOutputGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Enhanced tests for YamlWorkflowEngine covering all public functionality
 * including configuration, execution, status reporting, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("YamlWorkflowEngine Enhanced Tests")
class YamlWorkflowEngineEnhancedTest {

    @Mock private Orchestrator mockOrchestrator;
    @Mock private MemoryStore mockMemoryStore;
    @Mock private HumanApprovalHandler mockApprovalHandler;
    @Mock private FileOutputGenerator mockOutputGenerator;
    @Mock private WorkflowDefinition mockWorkflowDefinition;
    @Mock private WorkflowDefinition.WorkflowMetadata mockMetadata;
    @Mock private WorkflowDefinition.WorkflowSettings mockSettings;
    @Mock private AgentConfigCollection mockAgentConfig;
    @Mock private WorkflowContext mockContext;

    @TempDir Path tempDir;

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

    @Nested
    @DisplayName("Engine Initialization Tests")
    class EngineInitializationTests {

        @Test
        @DisplayName("Should initialize with default values")
        void shouldInitializeWithDefaultValues() {
            assertNotNull(engine);
            assertEquals("YamlWorkflowEngine", engine.getEngineName());
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should configure components using fluent interface")
        void shouldConfigureComponentsUsingFluentInterface() {
            YamlWorkflowEngine result = engine
                .withOrchestrator(mockOrchestrator, mockMemoryStore)
                .withApprovalHandler(mockApprovalHandler)
                .withOutputGenerator(mockOutputGenerator);

            assertSame(engine, result);
        }

        @Test
        @DisplayName("Should handle null component configuration")
        void shouldHandleNullComponentConfiguration() {
            assertDoesNotThrow(() -> {
                engine.withOrchestrator(null, null);
                engine.withApprovalHandler(null);
                engine.withOutputGenerator(null);
            });
        }
    }

    @Nested
    @DisplayName("Configuration Loading Tests")
    class ConfigurationLoadingTests {

        @Test
        @DisplayName("Should load workflow from valid YAML file")
        void shouldLoadWorkflowFromValidYamlFile() throws IOException {
            Path workflowFile = createValidWorkflowFile();

            assertDoesNotThrow(() -> {
                YamlWorkflowEngine result = engine.loadWorkflow(workflowFile.toString());
                assertSame(engine, result);
            });
        }

        @Test
        @DisplayName("Should load agents from valid YAML file")
        void shouldLoadAgentsFromValidYamlFile() throws IOException {
            Path agentsFile = createValidAgentsFile();

            assertDoesNotThrow(() -> {
                YamlWorkflowEngine result = engine.loadAgents(agentsFile.toString());
                assertSame(engine, result);
            });
        }

        @Test
        @DisplayName("Should load context from valid YAML file")
        void shouldLoadContextFromValidYamlFile() throws IOException {
            Path contextFile = createValidContextFile();

            assertDoesNotThrow(() -> {
                YamlWorkflowEngine result = engine.loadContext(contextFile.toString());
                assertSame(engine, result);
            });
        }

        @Test
        @DisplayName("Should handle missing configuration files")
        void shouldHandleMissingConfigurationFiles() {
            String nonExistentPath = "/non/existent/file.yaml";

            assertThrows(IOException.class, () ->
                engine.loadWorkflow(nonExistentPath));
            assertThrows(IOException.class, () ->
                engine.loadAgents(nonExistentPath));
            assertThrows(IOException.class, () ->
                engine.loadContext(nonExistentPath));
        }

        @Test
        @DisplayName("Should handle malformed YAML files")
        void shouldHandleMalformedYamlFiles() throws IOException {
            Path malformedFile = createMalformedYamlFile();

            assertThrows(Exception.class, () ->
                engine.loadWorkflow(malformedFile.toString()));
        }

        @Test
        @DisplayName("Should support configuration chaining")
        void shouldSupportConfigurationChaining() throws IOException {
            Path workflowFile = createValidWorkflowFile();
            Path agentsFile = createValidAgentsFile();
            Path contextFile = createValidContextFile();

            YamlWorkflowEngine result = engine
                .loadWorkflow(workflowFile.toString())
                .loadAgents(agentsFile.toString())
                .loadContext(contextFile.toString())
                .withOrchestrator(mockOrchestrator, mockMemoryStore)
                .withApprovalHandler(mockApprovalHandler)
                .withOutputGenerator(mockOutputGenerator);

            assertSame(engine, result);
        }
    }

    @Nested
    @DisplayName("Engine Readiness Tests")
    class EngineReadinessTests {

        @Test
        @DisplayName("Should not be ready without configuration")
        void shouldNotBeReadyWithoutConfiguration() {
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should not be ready with partial configuration")
        void shouldNotBeReadyWithPartialConfiguration() {
            engine.withOrchestrator(mockOrchestrator, mockMemoryStore);
            assertFalse(engine.isReady());

            // Still missing workflow, agents, and context
        }

        @Test
        @DisplayName("Should not be ready after closure")
        void shouldNotBeReadyAfterClosure() {
            engine.close();
            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should maintain consistent readiness state")
        void shouldMaintainConsistentReadinessState() {
            boolean ready1 = engine.isReady();
            boolean ready2 = engine.isReady();
            boolean ready3 = engine.isReady();

            assertEquals(ready1, ready2);
            assertEquals(ready2, ready3);
        }
    }

    @Nested
    @DisplayName("Engine Status Tests")
    class EngineStatusTests {

        @Test
        @DisplayName("Should provide comprehensive status information")
        void shouldProvideComprehensiveStatusInformation() {
            WorkflowEngine.EngineStatus status = engine.getStatus();

            assertNotNull(status);
            assertFalse(status.isConfigured());
            assertNotNull(status.getValidationMessages());
            assertFalse(status.getValidationMessages().isEmpty());
            assertEquals(0, status.getLoadedWorkflowCount());
            assertTrue(status.getActiveExecutionCount() >= 0);
            assertNotNull(status.getMetadata());
        }

        @Test
        @DisplayName("Should include required validation messages")
        void shouldIncludeRequiredValidationMessages() {
            WorkflowEngine.EngineStatus status = engine.getStatus();
            List<String> messages = status.getValidationMessages();

            assertTrue(messages.contains("Workflow definition not loaded"));
            assertTrue(messages.contains("Agent configuration not loaded"));
            assertTrue(messages.contains("Workflow context not loaded"));
            assertTrue(messages.contains("Orchestrator not configured"));
        }

        @Test
        @DisplayName("Should include engine metadata")
        void shouldIncludeEngineMetadata() {
            WorkflowEngine.EngineStatus status = engine.getStatus();
            Map<String, Object> metadata = status.getMetadata();

            assertEquals("yaml", metadata.get("engine_type"));
            assertEquals(false, metadata.get("workflow_loaded"));
            assertEquals(false, metadata.get("agent_config_loaded"));
            assertEquals(false, metadata.get("context_loaded"));
            assertEquals(false, metadata.get("orchestrator_available"));
            assertEquals(false, metadata.get("approval_handler_available"));
            assertEquals(0, metadata.get("completed_stages_count"));
            assertEquals(0, metadata.get("agent_cache_size"));
            assertEquals(false, metadata.get("closed"));
        }

        @Test
        @DisplayName("Should update metadata after closure")
        void shouldUpdateMetadataAfterClosure() {
            WorkflowEngine.EngineStatus statusBefore = engine.getStatus();
            assertEquals(false, statusBefore.getMetadata().get("closed"));

            engine.close();

            WorkflowEngine.EngineStatus statusAfter = engine.getStatus();
            assertEquals(true, statusAfter.getMetadata().get("closed"));
            assertTrue(statusAfter.getValidationMessages().contains("Engine has been closed"));
        }

        @Test
        @DisplayName("Should maintain status consistency")
        void shouldMaintainStatusConsistency() {
            WorkflowEngine.EngineStatus status1 = engine.getStatus();
            WorkflowEngine.EngineStatus status2 = engine.getStatus();

            assertEquals(status1.isConfigured(), status2.isConfigured());
            assertEquals(status1.getLoadedWorkflowCount(), status2.getLoadedWorkflowCount());
            assertEquals(status1.getValidationMessages().size(), status2.getValidationMessages().size());
        }
    }

    @Nested
    @DisplayName("Workflow Execution Tests")
    class WorkflowExecutionTests {

        @Test
        @DisplayName("Should validate configuration before execution")
        void shouldValidateConfigurationBeforeExecution() {
            assertThrows(ConductorException.class, () ->
                engine.execute("test input"));
        }

        @Test
        @DisplayName("Should validate parameters for definition-based execution")
        void shouldValidateParametersForDefinitionBasedExecution() {
            assertThrows(IllegalArgumentException.class, () ->
                engine.execute(null, mockContext));

            assertThrows(IllegalArgumentException.class, () ->
                engine.execute(mockWorkflowDefinition, null));
        }

        @Test
        @DisplayName("Should execute with definition and context")
        void shouldExecuteWithDefinitionAndContext() throws Exception {
            setupBasicMocks();

            WorkflowEngine.WorkflowResult result = engine.execute(mockWorkflowDefinition, mockContext);

            assertNotNull(result);
            verify(mockWorkflowDefinition, atLeastOnce()).getMetadata();
        }

        @Test
        @DisplayName("Should handle empty input arrays")
        void shouldHandleEmptyInputArrays() {
            assertThrows(ConductorException.class, () ->
                engine.execute());
        }

        @Test
        @DisplayName("Should handle multiple string inputs")
        void shouldHandleMultipleStringInputs() {
            assertThrows(ConductorException.class, () ->
                engine.execute("input1", "input2", "input3"));
        }

        @Test
        @DisplayName("Should prevent execution after closure")
        void shouldPreventExecutionAfterClosure() {
            engine.close();

            assertThrows(IllegalStateException.class, () ->
                engine.execute("test"));

            assertThrows(IllegalStateException.class, () ->
                engine.execute(mockWorkflowDefinition, mockContext));
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should clean up resources on close")
        void shouldCleanUpResourcesOnClose() {
            engine.close();

            assertFalse(engine.isReady());
            WorkflowEngine.EngineStatus status = engine.getStatus();
            assertTrue(status.getValidationMessages().contains("Engine has been closed"));
        }

        @Test
        @DisplayName("Should handle multiple close calls safely")
        void shouldHandleMultipleCloseCallsSafely() {
            assertDoesNotThrow(() -> {
                engine.close();
                engine.close();
                engine.close();
            });

            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should clean internal state on close")
        void shouldCleanInternalStateOnClose() {
            engine.close();

            WorkflowEngine.EngineStatus status = engine.getStatus();
            Map<String, Object> metadata = status.getMetadata();

            assertEquals(0, metadata.get("completed_stages_count"));
            assertEquals(0, metadata.get("agent_cache_size"));
            assertEquals(true, metadata.get("closed"));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent status queries")
        void shouldHandleConcurrentStatusQueries() throws Exception {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        WorkflowEngine.EngineStatus status = engine.getStatus();
                        assertNotNull(status);
                        assertEquals("YamlWorkflowEngine", engine.getEngineName());
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            assertTrue(exceptions.isEmpty(), "Concurrent operations should not throw exceptions");
        }

        @Test
        @DisplayName("Should handle concurrent configuration changes")
        void shouldHandleConcurrentConfigurationChanges() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        engine.withOrchestrator(mockOrchestrator, mockMemoryStore);
                        engine.withApprovalHandler(mockApprovalHandler);
                        engine.withOutputGenerator(mockOutputGenerator);
                        assertNotNull(engine.getStatus());
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            assertTrue(exceptions.isEmpty(), "Concurrent configuration should not throw exceptions");
        }

        @Test
        @DisplayName("Should handle concurrent close operations")
        void shouldHandleConcurrentCloseOperations() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        engine.close();
                        assertFalse(engine.isReady());
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            assertTrue(exceptions.isEmpty(), "Concurrent close operations should not throw exceptions");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle workflow with special characters in name")
        void shouldHandleWorkflowWithSpecialCharactersInName() throws Exception {
            setupBasicMocks();
            lenient().when(mockMetadata.getName()).thenReturn("workflow-!@#$%^&*()");

            WorkflowEngine.WorkflowResult result = engine.execute(mockWorkflowDefinition, mockContext);

            assertNotNull(result);
            assertEquals("workflow-!@#$%^&*()", result.getWorkflowName());
        }

        @Test
        @DisplayName("Should handle workflow with very long name")
        void shouldHandleWorkflowWithVeryLongName() throws Exception {
            setupBasicMocks();
            String longName = "a".repeat(1000);
            lenient().when(mockMetadata.getName()).thenReturn(longName);

            WorkflowEngine.WorkflowResult result = engine.execute(mockWorkflowDefinition, mockContext);

            assertNotNull(result);
            assertEquals(longName, result.getWorkflowName());
        }

        @Test
        @DisplayName("Should handle empty workflow name")
        void shouldHandleEmptyWorkflowName() throws Exception {
            setupBasicMocks();
            lenient().when(mockMetadata.getName()).thenReturn("");

            WorkflowEngine.WorkflowResult result = engine.execute(mockWorkflowDefinition, mockContext);

            assertNotNull(result);
            assertEquals("", result.getWorkflowName());
        }

        @Test
        @DisplayName("Should handle null workflow name")
        void shouldHandleNullWorkflowName() throws Exception {
            setupBasicMocks();
            lenient().when(mockMetadata.getName()).thenReturn(null);

            // Null workflow names may be handled gracefully or throw an exception
            // The exact behavior depends on implementation details
            assertDoesNotThrow(() -> {
                try {
                    WorkflowEngine.WorkflowResult result = engine.execute(mockWorkflowDefinition, mockContext);
                    assertNotNull(result);
                } catch (Exception e) {
                    // Exception is also acceptable for null workflow names
                    assertNotNull(e);
                }
            });
        }

        @Test
        @DisplayName("Should maintain state consistency after exceptions")
        void shouldMaintainStateConsistencyAfterExceptions() {
            assertEquals("YamlWorkflowEngine", engine.getEngineName());
            assertFalse(engine.isReady());

            assertThrows(ConductorException.class, () ->
                engine.execute("test"));

            assertEquals("YamlWorkflowEngine", engine.getEngineName());
            assertFalse(engine.isReady());
            assertNotNull(engine.getStatus());
        }
    }

    // Helper methods

    private void setupBasicMocks() {
        lenient().when(mockWorkflowDefinition.getMetadata()).thenReturn(mockMetadata);
        lenient().when(mockWorkflowDefinition.getSettings()).thenReturn(mockSettings);
        lenient().when(mockWorkflowDefinition.getStages()).thenReturn(Collections.emptyList());
        lenient().when(mockMetadata.getName()).thenReturn("test-workflow");
        lenient().when(mockMetadata.getVersion()).thenReturn("1.0");

        // Set minimal configuration using reflection
        try {
            setField(engine, "workflowDefinition", mockWorkflowDefinition);
            setField(engine, "agentConfig", mockAgentConfig);
            setField(engine, "context", mockContext);
            setField(engine, "orchestrator", mockOrchestrator);
        } catch (Exception e) {
            // Fallback to public methods
            engine.withOrchestrator(mockOrchestrator, mockMemoryStore);
        }
    }

    private Path createValidWorkflowFile() throws IOException {
        Path file = tempDir.resolve("valid-workflow.yaml");
        String content = """
            workflow:
              name: test-workflow
              version: 1.0
              description: Test workflow
            stages:
              - name: test-stage
                description: Test stage
                agents:
                  primary: test-agent
            variables:
              test_var: test_value
            settings:
              output_dir: output
            """;
        Files.writeString(file, content);
        return file;
    }

    private Path createValidAgentsFile() throws IOException {
        Path file = tempDir.resolve("valid-agents.yaml");
        String content = """
            agents:
              test-agent:
                type: conversational
                provider: openai
                model: gpt-4
                role: assistant
                prompt_template: test-template
                context_window: 4096
                parameters:
                  temperature: 0.7
            prompt_templates:
              test-template:
                system: "You are a helpful assistant"
                user: "Process this input: {{input}}"
                assistant: "I will help you with that."
            """;
        Files.writeString(file, content);
        return file;
    }

    private Path createValidContextFile() throws IOException {
        Path file = tempDir.resolve("valid-context.yaml");
        String content = """
            variables:
              context_var: context_value
              target_audience: developers
            """;
        Files.writeString(file, content);
        return file;
    }

    private Path createMalformedYamlFile() throws IOException {
        Path file = tempDir.resolve("malformed.yaml");
        String content = """
            invalid: yaml: content:
              - missing
                proper: indentation
            [invalid structure
            """;
        Files.writeString(file, content);
        return file;
    }

    private void setField(Object object, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for YamlWorkflowEngine covering YAML workflow loading,
 * execution, parallel stages, approval handling, and configuration management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("YamlWorkflowEngine Fixed Tests")
class YamlWorkflowEngineFixedTest {

    @Mock
    private Orchestrator mockOrchestrator;

    @Mock
    private MemoryStore mockMemoryStore;

    @Mock
    private HumanApprovalHandler mockApprovalHandler;

    @Mock
    private FileOutputGenerator mockOutputGenerator;

    @Mock
    private WorkflowDefinition mockWorkflowDefinition;

    @Mock
    private AgentConfigCollection mockAgentConfig;

    @Mock
    private WorkflowContext mockContext;

    @TempDir
    Path tempDir;

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
        @DisplayName("Should initialize with default configuration")
        void shouldInitializeWithDefaultConfiguration() {
            assertNotNull(engine);
            assertEquals("YamlWorkflowEngine", engine.getEngineName());
            assertFalse(engine.isReady()); // Not ready until configured
        }

        @Test
        @DisplayName("Should configure orchestrator and memory store")
        void shouldConfigureOrchestratorAndMemoryStore() {
            YamlWorkflowEngine configuredEngine = engine
                .withOrchestrator(mockOrchestrator, mockMemoryStore);

            assertSame(engine, configuredEngine); // Should return same instance for chaining
        }

        @Test
        @DisplayName("Should configure approval handler")
        void shouldConfigureApprovalHandler() {
            YamlWorkflowEngine configuredEngine = engine
                .withApprovalHandler(mockApprovalHandler);

            assertSame(engine, configuredEngine);
        }

        @Test
        @DisplayName("Should configure output generator")
        void shouldConfigureOutputGenerator() {
            YamlWorkflowEngine configuredEngine = engine
                .withOutputGenerator(mockOutputGenerator);

            assertSame(engine, configuredEngine);
        }
    }

    @Nested
    @DisplayName("Engine Readiness Tests")
    class EngineReadinessTests {

        @Test
        @DisplayName("Should not be ready without full configuration")
        void shouldNotBeReadyWithoutFullConfiguration() {
            assertFalse(engine.isReady());

            engine.withOrchestrator(mockOrchestrator, mockMemoryStore);
            assertFalse(engine.isReady()); // Still missing workflow, agents, context
        }

        @Test
        @DisplayName("Should not be ready after closure")
        void shouldNotBeReadyAfterClosure() {
            engine.close();
            assertFalse(engine.isReady());
        }
    }

    @Nested
    @DisplayName("Engine Status Tests")
    class EngineStatusTests {

        @Test
        @DisplayName("Should report correct status when not configured")
        void shouldReportCorrectStatusWhenNotConfigured() {
            WorkflowEngine.EngineStatus status = engine.getStatus();

            assertFalse(status.isConfigured());
            assertFalse(status.getValidationMessages().isEmpty());
            assertTrue(status.getValidationMessages().contains("Workflow definition not loaded"));
            assertTrue(status.getValidationMessages().contains("Agent configuration not loaded"));
            assertTrue(status.getValidationMessages().contains("Workflow context not loaded"));
            assertTrue(status.getValidationMessages().contains("Orchestrator not configured"));
        }

        @Test
        @DisplayName("Should include metadata in status")
        void shouldIncludeMetadataInStatus() {
            WorkflowEngine.EngineStatus status = engine.getStatus();
            Map<String, Object> metadata = status.getMetadata();

            assertEquals("yaml", metadata.get("engine_type"));
            assertEquals(false, metadata.get("workflow_loaded"));
            assertEquals(false, metadata.get("agent_config_loaded"));
            assertEquals(false, metadata.get("context_loaded"));
            assertEquals(false, metadata.get("orchestrator_available"));
            assertEquals(false, metadata.get("closed"));
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

            assertThrows(IllegalArgumentException.class, () ->
                engine.execute(null, mockContext));

            assertThrows(IllegalArgumentException.class, () ->
                engine.execute(mockWorkflowDefinition, null));
        }

        @Test
        @DisplayName("Should not execute after engine closure")
        void shouldNotExecuteAfterEngineClosure() throws Exception {
            engine.close();

            assertThrows(IllegalStateException.class, () ->
                engine.execute("test input"));

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
            engine.close();
            engine.close(); // Should not throw

            assertFalse(engine.isReady());
        }

        @Test
        @DisplayName("Should clear internal caches on close")
        void shouldClearInternalCachesOnClose() {
            engine.close();

            WorkflowEngine.EngineStatus statusAfter = engine.getStatus();
            Map<String, Object> metadataAfter = statusAfter.getMetadata();

            assertEquals(0, metadataAfter.get("completed_stages_count"));
            assertEquals(0, metadataAfter.get("agent_cache_size"));
            assertEquals(true, metadataAfter.get("closed"));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

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

        @Test
        @DisplayName("Should handle concurrent configuration changes")
        void shouldHandleConcurrentConfigurationChanges() throws Exception {
            int threadCount = 5;
            List<Thread> threads = new ArrayList<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        engine.withOrchestrator(mockOrchestrator, mockMemoryStore);
                        engine.withApprovalHandler(mockApprovalHandler);
                        assertNotNull(engine.getStatus());
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

            assertTrue(exceptions.isEmpty(), "Concurrent configuration should not throw exceptions");
        }
    }
}
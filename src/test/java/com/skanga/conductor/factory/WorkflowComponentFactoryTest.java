package com.skanga.conductor.factory;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.engine.DefaultWorkflowEngine;
import com.skanga.conductor.engine.YamlWorkflowEngine;
import com.skanga.conductor.memory.MemoryManager;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.memory.ResourceTracker;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.templates.PromptTemplateEngine;
import com.skanga.conductor.templates.TemplateFilters;
import com.skanga.conductor.templates.VariableResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WorkflowComponentFactory demonstrating dependency injection.
 */
public class WorkflowComponentFactoryTest {

    private WorkflowComponentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new WorkflowComponentFactory();
    }

    @AfterEach
    void tearDown() {
        factory.reset();
    }

    @Test
    void testCreatePromptTemplateEngine() {
        PromptTemplateEngine engine = factory.createPromptTemplateEngine();
        assertNotNull(engine);

        // Should be able to render templates
        String result = engine.renderString("Hello {{name}}", java.util.Map.of("name", "World"));
        assertEquals("Hello World", result);
    }

    @Test
    void testCreateTemplateFilters() {
        TemplateFilters filters = factory.createTemplateFilters();
        assertNotNull(filters);

        // Test filter application
        Object result = filters.applyFilter("hello", "upper");
        assertEquals("HELLO", result);
    }

    @Test
    void testCreateVariableResolver() {
        TemplateFilters filters = factory.createTemplateFilters();
        VariableResolver resolver = factory.createVariableResolver(filters);
        assertNotNull(resolver);

        // Test variable resolution
        java.util.Map<String, Object> vars = java.util.Map.of("name", "Test");
        Object value = resolver.getVariableValue("name", vars);
        assertEquals("Test", value);
    }

    @Test
    void testCreateDefaultWorkflowEngine() {
        Orchestrator orchestrator = mock(Orchestrator.class);
        DefaultWorkflowEngine engine = factory.createDefaultWorkflowEngine(orchestrator);
        assertNotNull(engine);
    }

    @Test
    void testCreateYamlWorkflowEngine() {
        YamlWorkflowEngine engine = factory.createYamlWorkflowEngine();
        assertNotNull(engine);
    }

    @Test
    void testGetMemoryManager_Singleton() {
        MemoryManager manager1 = factory.getMemoryManager();
        MemoryManager manager2 = factory.getMemoryManager();

        // Should return same instance (singleton)
        assertSame(manager1, manager2);
    }

    // Note: MemoryStore creation requires valid JDBC URL setup, so it's not easily testable here
    // The factory method is provided for consistency, actual MemoryStore creation
    // should be tested in integration tests with proper database setup

    @Test
    void testCustomSupplier() {
        // Create a mock PromptTemplateEngine
        PromptTemplateEngine mockEngine = mock(PromptTemplateEngine.class);

        // Register custom supplier
        factory.registerCustomSupplier(PromptTemplateEngine.class, () -> mockEngine);

        // Factory should now return the mock
        PromptTemplateEngine engine = factory.createPromptTemplateEngine();
        assertSame(mockEngine, engine);
    }

    @Test
    void testReset_ClearsCustomSuppliers() {
        PromptTemplateEngine mockEngine = mock(PromptTemplateEngine.class);
        factory.registerCustomSupplier(PromptTemplateEngine.class, () -> mockEngine);

        // Reset should clear custom suppliers
        factory.reset();

        // Should now create a real instance
        PromptTemplateEngine engine = factory.createPromptTemplateEngine();
        assertNotSame(mockEngine, engine);
        assertNotNull(engine);
    }

    @Test
    void testReset_ClearsSingletons() {
        MemoryManager manager1 = factory.getMemoryManager();
        factory.reset();
        MemoryManager manager2 = factory.getMemoryManager();

        // After reset, should get a different instance
        assertNotSame(manager1, manager2);
    }

    @Test
    void testGetMetricsRegistry_Singleton() {
        MetricsRegistry registry1 = factory.getMetricsRegistry();
        MetricsRegistry registry2 = factory.getMetricsRegistry();

        // Should return same instance (singleton)
        assertSame(registry1, registry2);
    }

    @Test
    void testBuilder() {
        WorkflowComponentFactory customFactory = WorkflowComponentFactory.builder()
            .withConfig(ApplicationConfig.getInstance())
            .build();

        assertNotNull(customFactory);
        assertNotNull(customFactory.getConfig());
    }

    @Test
    void testCreateResourceTracker() {
        ResourceTracker tracker = factory.createResourceTracker();
        assertNotNull(tracker);
    }

    /**
     * Example test showing how to inject mock dependencies for testing.
     */
    @Test
    void testDependencyInjectionForTesting() {
        // Create mock dependencies
        PromptTemplateEngine mockEngine = mock(PromptTemplateEngine.class);
        when(mockEngine.renderString(anyString(), anyMap())).thenReturn("Mocked output");

        // Register the mock in factory
        factory.registerCustomSupplier(PromptTemplateEngine.class, () -> mockEngine);

        // Create workflow engine - it will use the mocked template engine
        Orchestrator orchestrator = mock(Orchestrator.class);
        DefaultWorkflowEngine engine = factory.createDefaultWorkflowEngine(orchestrator);

        assertNotNull(engine);
        // The engine internally uses the mocked template engine
        verify(mockEngine, never()).renderString(anyString(), anyMap());
    }

    /**
     * Example test showing dependency injection with MemoryManager.
     */
    @Test
    void testMemoryManagerDependencyInjection() {
        // Create mock dependencies
        ResourceTracker mockTracker = mock(ResourceTracker.class);
        MetricsRegistry mockRegistry = mock(MetricsRegistry.class);

        // Create MemoryManager with injected dependencies
        MemoryManager manager = new MemoryManager(mockTracker, mockRegistry);

        assertNotNull(manager);

        // Register a cleanup task
        MemoryManager.CleanupTask mockTask = mock(MemoryManager.CleanupTask.class);
        manager.registerCleanupTask("test-task", mockTask);

        // Verify the mock tracker was called
        verify(mockTracker).registerCleanupTask("test-task", mockTask);
    }
}

package com.skanga.conductor.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskDependencyAnalyzer functionality.
 */
class TaskDependencyAnalyzerTest {

    private TaskDependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new TaskDependencyAnalyzer();
    }

    @Test
    void testIndependentTasks() {
        TaskDefinition[] tasks = {
            new TaskDefinition("task1", "First task", "Process {{user_request}}"),
            new TaskDefinition("task2", "Second task", "Analyze {{user_request}}"),
            new TaskDefinition("task3", "Third task", "Review {{user_request}}")
        };

        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(tasks);

        // All tasks are independent, should be in one batch
        assertEquals(1, batches.size());
        assertEquals(3, batches.get(0).size());
    }

    @Test
    void testSequentialDependencies() {
        TaskDefinition[] tasks = {
            new TaskDefinition("task1", "First task", "Process {{user_request}}"),
            new TaskDefinition("task2", "Second task", "Edit {{prev_output}}"),
            new TaskDefinition("task3", "Third task", "Review {{prev_output}}")
        };

        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(tasks);

        // Sequential dependencies, should be in separate batches
        assertEquals(3, batches.size());
        assertEquals("task1", batches.get(0).get(0).taskName);
        assertEquals("task2", batches.get(1).get(0).taskName);
        assertEquals("task3", batches.get(2).get(0).taskName);
    }

    @Test
    void testDirectTaskDependencies() {
        TaskDefinition[] tasks = {
            new TaskDefinition("draft", "Draft content", "Write {{user_request}}"),
            new TaskDefinition("edit", "Edit content", "Edit {{draft}}"),
            new TaskDefinition("review", "Review content", "Review {{draft}}")
        };

        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(tasks);

        // draft should be first, edit and review can be parallel
        assertEquals(2, batches.size());
        assertEquals("draft", batches.get(0).get(0).taskName);
        assertEquals(2, batches.get(1).size());
        assertTrue(batches.get(1).stream().anyMatch(t -> t.taskName.equals("edit")));
        assertTrue(batches.get(1).stream().anyMatch(t -> t.taskName.equals("review")));
    }

    @Test
    void testComplexDependencies() {
        TaskDefinition[] tasks = {
            new TaskDefinition("research", "Research topic", "Research {{user_request}}"),
            new TaskDefinition("outline", "Create outline", "Create outline for {{user_request}}"),
            new TaskDefinition("intro", "Write intro", "Write intro using {{research}} and {{outline}}"),
            new TaskDefinition("body", "Write body", "Write body using {{research}} and {{outline}}"),
            new TaskDefinition("conclusion", "Write conclusion", "Write conclusion using {{intro}} and {{body}}"),
            new TaskDefinition("review", "Review all", "Review {{intro}}, {{body}}, and {{conclusion}}")
        };

        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(tasks);

        // Expected batches:
        // Batch 1: research, outline (independent)
        // Batch 2: intro, body (depend on research + outline)
        // Batch 3: conclusion (depends on intro + body)
        // Batch 4: review (depends on conclusion)
        assertEquals(4, batches.size());

        // Verify batch 1 has research and outline
        assertEquals(2, batches.get(0).size());
        assertTrue(batches.get(0).stream().anyMatch(t -> t.taskName.equals("research")));
        assertTrue(batches.get(0).stream().anyMatch(t -> t.taskName.equals("outline")));

        // Verify batch 2 has intro and body
        assertEquals(2, batches.get(1).size());
        assertTrue(batches.get(1).stream().anyMatch(t -> t.taskName.equals("intro")));
        assertTrue(batches.get(1).stream().anyMatch(t -> t.taskName.equals("body")));

        // Verify batch 3 has conclusion
        assertEquals(1, batches.get(2).size());
        assertEquals("conclusion", batches.get(2).get(0).taskName);

        // Verify batch 4 has review
        assertEquals(1, batches.get(3).size());
        assertEquals("review", batches.get(3).get(0).taskName);
    }

    @Test
    void testCircularDependencyDetection() {
        TaskDefinition[] tasks = {
            new TaskDefinition("task1", "First task", "Process {{task2}}"),
            new TaskDefinition("task2", "Second task", "Process {{task1}}")
        };

        assertThrows(IllegalStateException.class, () -> analyzer.groupTasksIntoBatches(tasks));
    }

    @Test
    void testParallelismAnalysis() {
        TaskDefinition[] independentTasks = {
            new TaskDefinition("task1", "First task", "Process {{user_request}}"),
            new TaskDefinition("task2", "Second task", "Analyze {{user_request}}"),
            new TaskDefinition("task3", "Third task", "Review {{user_request}}")
        };

        TaskDependencyAnalyzer.ParallelismAnalysis analysis = analyzer.analyzeParallelismBenefit(independentTasks);

        assertEquals(3, analysis.getTotalTasks());
        assertEquals(1, analysis.getBatchCount());
        assertEquals(3, analysis.getMaxBatchSize());
        assertEquals(3.0, analysis.getSpeedupPotential(), 0.01);
    }

    @Test
    void testSequentialAnalysis() {
        TaskDefinition[] sequentialTasks = {
            new TaskDefinition("task1", "First task", "Process {{user_request}}"),
            new TaskDefinition("task2", "Second task", "Edit {{prev_output}}"),
            new TaskDefinition("task3", "Third task", "Review {{prev_output}}")
        };

        TaskDependencyAnalyzer.ParallelismAnalysis analysis = analyzer.analyzeParallelismBenefit(sequentialTasks);

        assertEquals(3, analysis.getTotalTasks());
        assertEquals(3, analysis.getBatchCount());
        assertEquals(1, analysis.getMaxBatchSize());
        assertEquals(1.0, analysis.getSpeedupPotential(), 0.01);
    }

    @Test
    void testEmptyTaskArray() {
        TaskDefinition[] emptyTasks = {};
        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(emptyTasks);

        assertTrue(batches.isEmpty());

        TaskDependencyAnalyzer.ParallelismAnalysis analysis = analyzer.analyzeParallelismBenefit(emptyTasks);
        assertEquals(0, analysis.getTotalTasks());
    }

    @Test
    void testSingleTask() {
        TaskDefinition[] singleTask = {
            new TaskDefinition("task1", "Only task", "Process {{user_request}}")
        };

        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(singleTask);

        assertEquals(1, batches.size());
        assertEquals(1, batches.get(0).size());
        assertEquals("task1", batches.get(0).get(0).taskName);
    }

    @Test
    void testMixedDependencyPattern() {
        // Pattern: some parallel, some sequential
        TaskDefinition[] tasks = {
            new TaskDefinition("research_a", "Research A", "Research A for {{user_request}}"),
            new TaskDefinition("research_b", "Research B", "Research B for {{user_request}}"),
            new TaskDefinition("synthesis", "Synthesize", "Combine {{research_a}} and {{research_b}}"),
            new TaskDefinition("parallel_task", "Parallel task", "Independent work on {{user_request}}")
        };

        List<List<TaskDefinition>> batches = analyzer.groupTasksIntoBatches(tasks);

        // Expected:
        // Batch 1: research_a, research_b, parallel_task (all independent)
        // Batch 2: synthesis (depends on research results)
        assertEquals(2, batches.size());

        // First batch should have 3 independent tasks
        assertEquals(3, batches.get(0).size());
        assertTrue(batches.get(0).stream().anyMatch(t -> t.taskName.equals("research_a")));
        assertTrue(batches.get(0).stream().anyMatch(t -> t.taskName.equals("research_b")));
        assertTrue(batches.get(0).stream().anyMatch(t -> t.taskName.equals("parallel_task")));

        // Second batch should have synthesis
        assertEquals(1, batches.get(1).size());
        assertEquals("synthesis", batches.get(1).get(0).taskName);
    }
}
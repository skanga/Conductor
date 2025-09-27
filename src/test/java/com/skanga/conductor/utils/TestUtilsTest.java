package com.skanga.conductor.utils;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for TestUtils functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestUtilsTest extends ConductorTestBase {

    private String originalPropertyValue;

    @BeforeEach
    void setUp() {
        // Save original property value to restore after each test
        originalPropertyValue = System.getProperty("runningUnderTest");
    }

    @AfterEach
    void tearDown() {
        // Restore original property value
        if (originalPropertyValue != null) {
            System.setProperty("runningUnderTest", originalPropertyValue);
        } else {
            System.clearProperty("runningUnderTest");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should be a public class")
    void testClassModifiers() {
        assertTrue(Modifier.isPublic(TestUtils.class.getModifiers()));
    }

    @Test
    @Order(2)
    @DisplayName("Should have public constructor")
    void testPublicConstructor() throws Exception {
        Constructor<TestUtils> constructor = TestUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPublic(constructor.getModifiers()));

        // Should be able to instantiate
        TestUtils testUtils = constructor.newInstance();
        assertNotNull(testUtils);
    }

    @Test
    @Order(3)
    @DisplayName("Should have TEST_PROPERTY constant")
    void testTestPropertyConstant() throws Exception {
        Field testPropertyField = TestUtils.class.getDeclaredField("TEST_PROPERTY");
        assertTrue(Modifier.isStatic(testPropertyField.getModifiers()));
        assertTrue(Modifier.isFinal(testPropertyField.getModifiers()));

        testPropertyField.setAccessible(true);
        String value = (String) testPropertyField.get(null);
        assertEquals("runningUnderTest", value);
    }

    @Test
    @Order(4)
    @DisplayName("Should return false by default when property not set")
    void testIsRunningUnderTestDefault() {
        System.clearProperty("runningUnderTest");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(5)
    @DisplayName("Should return true when property set to 'true'")
    void testIsRunningUnderTestTrue() {
        System.setProperty("runningUnderTest", "true");
        assertTrue(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(6)
    @DisplayName("Should return false when property set to 'false'")
    void testIsRunningUnderTestFalse() {
        System.setProperty("runningUnderTest", "false");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(7)
    @DisplayName("Should be case insensitive for true values")
    void testIsRunningUnderTestCaseInsensitiveTrue() {
        System.setProperty("runningUnderTest", "TRUE");
        assertTrue(TestUtils.isRunningUnderTest());

        System.setProperty("runningUnderTest", "True");
        assertTrue(TestUtils.isRunningUnderTest());

        System.setProperty("runningUnderTest", "tRuE");
        assertTrue(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(8)
    @DisplayName("Should be case insensitive for false values")
    void testIsRunningUnderTestCaseInsensitiveFalse() {
        System.setProperty("runningUnderTest", "FALSE");
        assertFalse(TestUtils.isRunningUnderTest());

        System.setProperty("runningUnderTest", "False");
        assertFalse(TestUtils.isRunningUnderTest());

        System.setProperty("runningUnderTest", "fAlSe");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(9)
    @DisplayName("Should return false for invalid boolean values")
    void testIsRunningUnderTestInvalidValues() {
        String[] invalidValues = {"", "yes", "no", "1", "0", "on", "off", "invalid", "null"};

        for (String invalidValue : invalidValues) {
            System.setProperty("runningUnderTest", invalidValue);
            assertFalse(TestUtils.isRunningUnderTest(),
                       "Should return false for invalid value: " + invalidValue);
        }
    }

    @Test
    @Order(10)
    @DisplayName("Should handle whitespace in property values")
    void testIsRunningUnderTestWithWhitespace() {
        // Boolean.parseBoolean() handles whitespace by trimming
        System.setProperty("runningUnderTest", " true ");
        assertFalse(TestUtils.isRunningUnderTest()); // Boolean.parseBoolean(" true ") returns false

        System.setProperty("runningUnderTest", "true");
        assertTrue(TestUtils.isRunningUnderTest());

        System.setProperty("runningUnderTest", "\ttrue\n");
        assertFalse(TestUtils.isRunningUnderTest()); // Boolean.parseBoolean doesn't trim

        System.setProperty("runningUnderTest", " false ");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(11)
    @DisplayName("Should handle empty string property value")
    void testIsRunningUnderTestEmptyString() {
        System.setProperty("runningUnderTest", "");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(12)
    @DisplayName("Should be consistent across multiple calls")
    void testIsRunningUnderTestConsistency() {
        System.setProperty("runningUnderTest", "true");

        boolean result1 = TestUtils.isRunningUnderTest();
        boolean result2 = TestUtils.isRunningUnderTest();
        boolean result3 = TestUtils.isRunningUnderTest();

        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    @Order(13)
    @DisplayName("Should reflect changes to system property")
    void testIsRunningUnderTestReflectsChanges() {
        // Initially false
        System.setProperty("runningUnderTest", "false");
        assertFalse(TestUtils.isRunningUnderTest());

        // Change to true
        System.setProperty("runningUnderTest", "true");
        assertTrue(TestUtils.isRunningUnderTest());

        // Change back to false
        System.setProperty("runningUnderTest", "false");
        assertFalse(TestUtils.isRunningUnderTest());

        // Clear property
        System.clearProperty("runningUnderTest");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(14)
    @DisplayName("Should handle concurrent access safely")
    void testIsRunningUnderTestConcurrency() throws Exception {
        System.setProperty("runningUnderTest", "true");

        int numberOfThreads = 10;
        int callsPerThread = 100;
        Thread[] threads = new Thread[numberOfThreads];
        boolean[][] results = new boolean[numberOfThreads][callsPerThread];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    results[threadIndex][j] = TestUtils.isRunningUnderTest();
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(1000); // 1 second timeout per thread
        }

        // Verify all results are consistent
        for (int i = 0; i < numberOfThreads; i++) {
            for (int j = 0; j < callsPerThread; j++) {
                assertTrue(results[i][j],
                          "Result should be true for thread " + i + " call " + j);
            }
        }
    }

    @Test
    @Order(15)
    @DisplayName("Should use Boolean.parseBoolean semantics exactly")
    void testBooleanParseBooleanSemantics() {
        // Test that TestUtils.isRunningUnderTest() behaves exactly like Boolean.parseBoolean()
        String[] testValues = {
            "true", "TRUE", "True", "tRuE",
            "false", "FALSE", "False", "fAlSe",
            "", "yes", "no", "1", "0", "on", "off",
            " true", "true ", " true ",
            "\ttrue", "true\n", "\ttrue\n",
            "null", "undefined", "invalid"
        };

        for (String testValue : testValues) {
            if (testValue.equals("null")) {
                // Special case for null string
                System.clearProperty("runningUnderTest");
                boolean expected = Boolean.parseBoolean(System.getProperty("runningUnderTest", "false"));
                boolean actual = TestUtils.isRunningUnderTest();
                assertEquals(expected, actual, "Mismatch for null property");
            } else {
                System.setProperty("runningUnderTest", testValue);
                boolean expected = Boolean.parseBoolean(testValue);
                boolean actual = TestUtils.isRunningUnderTest();
                assertEquals(expected, actual, "Mismatch for value: '" + testValue + "'");
            }
        }
    }

    @Test
    @Order(16)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.utils", TestUtils.class.getPackageName());
        assertEquals("TestUtils", TestUtils.class.getSimpleName());
        assertEquals("com.skanga.conductor.utils.TestUtils", TestUtils.class.getName());
    }

    @Test
    @Order(17)
    @DisplayName("Should have only static methods")
    void testOnlyStaticMethods() throws Exception {
        // Check that isRunningUnderTest is static
        assertTrue(Modifier.isStatic(
            TestUtils.class.getMethod("isRunningUnderTest").getModifiers()));
    }

    @Test
    @Order(18)
    @DisplayName("Should work in typical testing scenarios")
    void testTypicalTestingScenarios() {
        // Scenario 1: Test framework sets property to enable test mode
        System.setProperty("runningUnderTest", "true");
        assertTrue(TestUtils.isRunningUnderTest());

        // Code under test could use this to enable test-specific behavior
        if (TestUtils.isRunningUnderTest()) {
            // Test-specific logic would go here
            assertTrue(true, "Test mode is enabled");
        }

        // Scenario 2: Production environment (property not set or false)
        System.clearProperty("runningUnderTest");
        assertFalse(TestUtils.isRunningUnderTest());

        // Production code path
        if (!TestUtils.isRunningUnderTest()) {
            // Production-specific logic would go here
            assertTrue(true, "Production mode is active");
        }

        // Scenario 3: Explicitly disabled in test
        System.setProperty("runningUnderTest", "false");
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(19)
    @DisplayName("Should handle system property not found gracefully")
    void testSystemPropertyNotFound() {
        // Ensure property is not set
        System.clearProperty("runningUnderTest");
        assertNull(System.getProperty("runningUnderTest"));

        // Should default to false
        assertFalse(TestUtils.isRunningUnderTest());
    }

    @Test
    @Order(20)
    @DisplayName("Should be usable as utility class")
    void testUtilityClassUsage() {
        // Should be able to use without instantiation
        assertDoesNotThrow(() -> {
            boolean result = TestUtils.isRunningUnderTest();
            // Result can be either true or false, both are valid
        });

        // Should also be able to instantiate if needed (though not typical for utility classes)
        assertDoesNotThrow(() -> {
            TestUtils instance = new TestUtils();
            assertNotNull(instance);
        });
    }
}
package com.skanga.conductor.exception;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ExceptionStrategy functionality and documentation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionStrategyTest extends ConductorTestBase {

    @Test
    @Order(1)
    @DisplayName("Should be a final utility class")
    void testClassModifiers() {
        assertTrue(Modifier.isFinal(ExceptionStrategy.class.getModifiers()));
        assertTrue(Modifier.isPublic(ExceptionStrategy.class.getModifiers()));
    }

    @Test
    @Order(2)
    @DisplayName("Should have private constructor")
    void testPrivateConstructor() throws Exception {
        Constructor<ExceptionStrategy> constructor = ExceptionStrategy.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        // Test that constructor is accessible via reflection but throws no exceptions
        constructor.setAccessible(true);
        assertDoesNotThrow(() -> constructor.newInstance());
    }

    @Test
    @Order(3)
    @DisplayName("Should not be instantiable through public means")
    void testNotPubliclyInstantiable() {
        Constructor<?>[] constructors = ExceptionStrategy.class.getConstructors();
        assertEquals(0, constructors.length, "Should have no public constructors");
    }

    @Test
    @Order(4)
    @DisplayName("Should have Category enum with all expected values")
    void testCategoryEnumValues() {
        ExceptionStrategy.Category[] categories = ExceptionStrategy.Category.values();
        assertEquals(4, categories.length);

        // Verify all expected categories exist
        assertNotNull(ExceptionStrategy.Category.PROGRAMMING_ERROR);
        assertNotNull(ExceptionStrategy.Category.BUSINESS_FAILURE);
        assertNotNull(ExceptionStrategy.Category.DATA_FORMAT_ERROR);
        assertNotNull(ExceptionStrategy.Category.INFRASTRUCTURE_ERROR);
    }

    @Test
    @Order(5)
    @DisplayName("Should have correct Category enum ordering")
    void testCategoryEnumOrdering() {
        ExceptionStrategy.Category[] categories = ExceptionStrategy.Category.values();

        assertEquals(ExceptionStrategy.Category.PROGRAMMING_ERROR, categories[0]);
        assertEquals(ExceptionStrategy.Category.BUSINESS_FAILURE, categories[1]);
        assertEquals(ExceptionStrategy.Category.DATA_FORMAT_ERROR, categories[2]);
        assertEquals(ExceptionStrategy.Category.INFRASTRUCTURE_ERROR, categories[3]);
    }

    @Test
    @Order(6)
    @DisplayName("Should support Category enum name() method")
    void testCategoryEnumNames() {
        assertEquals("PROGRAMMING_ERROR", ExceptionStrategy.Category.PROGRAMMING_ERROR.name());
        assertEquals("BUSINESS_FAILURE", ExceptionStrategy.Category.BUSINESS_FAILURE.name());
        assertEquals("DATA_FORMAT_ERROR", ExceptionStrategy.Category.DATA_FORMAT_ERROR.name());
        assertEquals("INFRASTRUCTURE_ERROR", ExceptionStrategy.Category.INFRASTRUCTURE_ERROR.name());
    }

    @Test
    @Order(7)
    @DisplayName("Should support Category enum valueOf() method")
    void testCategoryEnumValueOf() {
        assertEquals(ExceptionStrategy.Category.PROGRAMMING_ERROR,
                    ExceptionStrategy.Category.valueOf("PROGRAMMING_ERROR"));
        assertEquals(ExceptionStrategy.Category.BUSINESS_FAILURE,
                    ExceptionStrategy.Category.valueOf("BUSINESS_FAILURE"));
        assertEquals(ExceptionStrategy.Category.DATA_FORMAT_ERROR,
                    ExceptionStrategy.Category.valueOf("DATA_FORMAT_ERROR"));
        assertEquals(ExceptionStrategy.Category.INFRASTRUCTURE_ERROR,
                    ExceptionStrategy.Category.valueOf("INFRASTRUCTURE_ERROR"));
    }

    @Test
    @Order(8)
    @DisplayName("Should throw IllegalArgumentException for invalid Category valueOf")
    void testCategoryEnumValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
            ExceptionStrategy.Category.valueOf("INVALID_CATEGORY"));

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionStrategy.Category.valueOf("programming_error")); // case sensitive

        assertThrows(IllegalArgumentException.class, () ->
            ExceptionStrategy.Category.valueOf(""));
    }

    @Test
    @Order(9)
    @DisplayName("Should handle null valueOf gracefully")
    void testCategoryEnumValueOfNull() {
        assertThrows(NullPointerException.class, () ->
            ExceptionStrategy.Category.valueOf(null));
    }

    @Test
    @Order(10)
    @DisplayName("Should support Category enum ordinal() method")
    void testCategoryEnumOrdinals() {
        assertEquals(0, ExceptionStrategy.Category.PROGRAMMING_ERROR.ordinal());
        assertEquals(1, ExceptionStrategy.Category.BUSINESS_FAILURE.ordinal());
        assertEquals(2, ExceptionStrategy.Category.DATA_FORMAT_ERROR.ordinal());
        assertEquals(3, ExceptionStrategy.Category.INFRASTRUCTURE_ERROR.ordinal());
    }

    @Test
    @Order(11)
    @DisplayName("Should support Category enum toString() method")
    void testCategoryEnumToString() {
        assertEquals("PROGRAMMING_ERROR", ExceptionStrategy.Category.PROGRAMMING_ERROR.toString());
        assertEquals("BUSINESS_FAILURE", ExceptionStrategy.Category.BUSINESS_FAILURE.toString());
        assertEquals("DATA_FORMAT_ERROR", ExceptionStrategy.Category.DATA_FORMAT_ERROR.toString());
        assertEquals("INFRASTRUCTURE_ERROR", ExceptionStrategy.Category.INFRASTRUCTURE_ERROR.toString());
    }

    @Test
    @Order(12)
    @DisplayName("Should support Category enum equality")
    void testCategoryEnumEquality() {
        ExceptionStrategy.Category cat1 = ExceptionStrategy.Category.PROGRAMMING_ERROR;
        ExceptionStrategy.Category cat2 = ExceptionStrategy.Category.valueOf("PROGRAMMING_ERROR");

        assertEquals(cat1, cat2);
        assertSame(cat1, cat2); // Enums are singletons

        assertNotEquals(ExceptionStrategy.Category.PROGRAMMING_ERROR,
                       ExceptionStrategy.Category.BUSINESS_FAILURE);
    }

    @Test
    @Order(13)
    @DisplayName("Should support Category enum hashCode consistency")
    void testCategoryEnumHashCode() {
        ExceptionStrategy.Category cat1 = ExceptionStrategy.Category.PROGRAMMING_ERROR;
        ExceptionStrategy.Category cat2 = ExceptionStrategy.Category.valueOf("PROGRAMMING_ERROR");

        assertEquals(cat1.hashCode(), cat2.hashCode());

        // Different enum values should likely have different hash codes
        assertNotEquals(ExceptionStrategy.Category.PROGRAMMING_ERROR.hashCode(),
                       ExceptionStrategy.Category.BUSINESS_FAILURE.hashCode());
    }

    @Test
    @Order(14)
    @DisplayName("Should support Category enum in switch statements")
    void testCategoryEnumInSwitch() {
        for (ExceptionStrategy.Category category : ExceptionStrategy.Category.values()) {
            String description = switch (category) {
                case PROGRAMMING_ERROR -> "Programming errors and system failures";
                case BUSINESS_FAILURE -> "Recoverable business logic failures";
                case DATA_FORMAT_ERROR -> "Data format and parsing errors";
                case INFRASTRUCTURE_ERROR -> "Infrastructure and resource errors";
            };

            assertNotNull(description);
            assertFalse(description.isEmpty());
        }
    }

    @Test
    @Order(15)
    @DisplayName("Should support Category enum compareTo")
    void testCategoryEnumCompareTo() {
        assertTrue(ExceptionStrategy.Category.PROGRAMMING_ERROR.compareTo(
                  ExceptionStrategy.Category.BUSINESS_FAILURE) < 0);

        assertTrue(ExceptionStrategy.Category.BUSINESS_FAILURE.compareTo(
                  ExceptionStrategy.Category.DATA_FORMAT_ERROR) < 0);

        assertTrue(ExceptionStrategy.Category.DATA_FORMAT_ERROR.compareTo(
                  ExceptionStrategy.Category.INFRASTRUCTURE_ERROR) < 0);

        assertEquals(0, ExceptionStrategy.Category.PROGRAMMING_ERROR.compareTo(
                       ExceptionStrategy.Category.PROGRAMMING_ERROR));
    }

    @Test
    @Order(16)
    @DisplayName("Should have Category enum as public static inner class")
    void testCategoryEnumAccessibility() {
        Class<?> categoryClass = ExceptionStrategy.Category.class;

        assertTrue(Modifier.isPublic(categoryClass.getModifiers()));
        assertTrue(Modifier.isStatic(categoryClass.getModifiers()));
        assertTrue(Modifier.isFinal(categoryClass.getModifiers())); // Enums are implicitly final
        assertTrue(categoryClass.isEnum());
        assertEquals(ExceptionStrategy.class, categoryClass.getEnclosingClass());
    }

    @Test
    @Order(17)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.exception", ExceptionStrategy.class.getPackageName());
        assertEquals("ExceptionStrategy", ExceptionStrategy.class.getSimpleName());
        assertEquals("com.skanga.conductor.exception.ExceptionStrategy",
                    ExceptionStrategy.class.getName());
    }

    @Test
    @Order(18)
    @DisplayName("Should have immutable Category enum")
    void testCategoryEnumImmutability() {
        // Enums are inherently immutable - test that values() returns a copy
        ExceptionStrategy.Category[] values1 = ExceptionStrategy.Category.values();
        ExceptionStrategy.Category[] values2 = ExceptionStrategy.Category.values();

        assertNotSame(values1, values2); // Should return different array instances
        assertArrayEquals(values1, values2); // But with same content

        // Modifying returned array shouldn't affect future calls
        values1[0] = null;
        ExceptionStrategy.Category[] values3 = ExceptionStrategy.Category.values();
        assertNotNull(values3[0]);
        assertEquals(ExceptionStrategy.Category.PROGRAMMING_ERROR, values3[0]);
    }

    @Test
    @Order(19)
    @DisplayName("Should support Category enum reflection")
    void testCategoryEnumReflection() throws Exception {
        Class<ExceptionStrategy.Category> categoryClass = ExceptionStrategy.Category.class;

        assertTrue(categoryClass.isEnum());
        assertEquals(Enum.class, categoryClass.getSuperclass());

        // Test enum constants via reflection
        Object[] enumConstants = categoryClass.getEnumConstants();
        assertEquals(4, enumConstants.length);
        assertTrue(enumConstants[0] instanceof ExceptionStrategy.Category);
    }
}
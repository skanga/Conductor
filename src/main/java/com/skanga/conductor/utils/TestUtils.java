package com.skanga.conductor.utils;

public class TestUtils {
    private static final String TEST_PROPERTY = "runningUnderTest";

    public static boolean isRunningUnderTest() {
        return Boolean.parseBoolean(System.getProperty(TEST_PROPERTY, "false"));
    }
}

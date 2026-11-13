package com.prx.directory;

import com.prx.directory.support.ClasspathScan;
import com.prx.directory.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every transfer object (records of `api.v1.to` and of the client packages) can be built and printed, and `equals`/`hashCode` are consistent. Built from the classpath so a new record is covered without touching this test.
@DisplayName("Transfer objects")
class TransferObjectsSmokeTest {

    private static List<Class<?>> records() {
        List<Class<?>> found = new ArrayList<>();
        for (String pkg : List.of("com.prx.directory.api.v1.to", "com.prx.directory.client")) {
            for (Class<?> c : ClasspathScan.classesIn(pkg)) {
                if (c.isRecord() && !Modifier.isAbstract(c.getModifiers())) {
                    found.add(c);
                }
            }
        }
        return found;
    }

    @Test
    @DisplayName("they are found on the classpath")
    void recordsAreFound() {
        assertTrue(records().size() > 20);
    }

    @Test
    @DisplayName("each builds, prints and keeps equals/hashCode consistent")
    void buildPrintAndCompare() {
        for (Class<?> type : records()) {
            Object first = Fixtures.of(type);
            assertNotNull(first, type.getName());
            assertFalse(first.toString().isBlank(), type.getName());
            assertEquals(first, first, type.getName());
            assertEquals(first.hashCode(), first.hashCode(), type.getName());
        }
    }
}

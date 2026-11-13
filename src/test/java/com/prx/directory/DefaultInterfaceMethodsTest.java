package com.prx.directory;

import com.prx.directory.support.ClasspathScan;
import com.prx.directory.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The service and API interfaces ship default methods that answer "not implemented" until an implementation overrides them. This pins
/// that contract: each default either throws `UnsupportedOperationException` or answers `501 Not Implemented`, never something else
/// (an implementation that forgets to override a method is then visible, not silently wrong).
@DisplayName("Default methods of the service and API interfaces")
class DefaultInterfaceMethodsTest {

    private static List<Class<?>> interfaces() {
        List<Class<?>> found = new ArrayList<>();
        for (String pkg : List.of("com.prx.directory.api.v1.service", "com.prx.directory.api.v1.controller", "com.prx.directory.kafka.producer")) {
            for (Class<?> c : ClasspathScan.classesIn(pkg)) {
                if (c.isInterface()) {
                    found.add(c);
                }
            }
        }
        return found;
    }

    @Test
    @DisplayName("every default answers 501 or throws UnsupportedOperationException")
    void defaultsAreNotImplemented() throws IllegalAccessException {
        int checked = 0;
        for (Class<?> type : interfaces()) {
            Object target = Mockito.mock(type, Mockito.CALLS_REAL_METHODS);
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isDefault() || method.isSynthetic()) {
                    continue;
                }
                Object[] args = new Object[method.getParameterCount()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = Fixtures.of(method.getParameterTypes()[i], method.getGenericParameterTypes()[i], 0);
                }
                method.setAccessible(true);
                try {
                    Object result = method.invoke(target, args);
                    checked++;
                    if (result instanceof ResponseEntity<?> response) {
                        assertEquals(501, response.getStatusCode().value(), type.getSimpleName() + "." + method.getName());
                    }
                } catch (InvocationTargetException e) {
                    if (!(e.getCause() instanceof UnsupportedOperationException)) {
                        fail(type.getSimpleName() + "." + method.getName() + " threw " + e.getCause(), e.getCause());
                    }
                    checked++;
                }
            }
        }
        assertTrue(checked > 40, "checked " + checked);
    }
}

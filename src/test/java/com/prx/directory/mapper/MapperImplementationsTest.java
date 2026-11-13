package com.prx.directory.mapper;

import com.prx.directory.support.ClasspathScan;
import com.prx.directory.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Runs every method of every MapStruct implementation (and the default methods of its interface) with fully populated arguments, so
/// each mapping body, null-check branch and helper executes and any mapping that throws on real data fails here, not in production.
@DisplayName("MapStruct mappers")
class MapperImplementationsTest {

    private static List<Class<?>> implementations() {
        List<Class<?>> found = new ArrayList<>();
        for (Class<?> c : ClasspathScan.classesIn("com.prx.directory.mapper")) {
            if (c.getSimpleName().endsWith("Impl") && !c.isInterface()) {
                found.add(c);
            }
        }
        return found;
    }

    /// One instance of each implementation, with the other mappers it uses injected.
    private static Map<Class<?>, Object> instances() throws ReflectiveOperationException {
        Map<Class<?>, Object> created = new HashMap<>();
        for (Class<?> c : implementations()) {
            var constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            created.put(c, constructor.newInstance());
        }
        for (Object instance : created.values()) {
            for (Field field : instance.getClass().getDeclaredFields()) {
                for (Object candidate : created.values()) {
                    if (!Modifier.isStatic(field.getModifiers()) && field.getType().isInstance(candidate)) {
                        field.setAccessible(true);
                        field.set(instance, candidate);
                    }
                }
            }
        }
        return created;
    }

    @Test
    @DisplayName("every mapper method accepts populated arguments")
    void everyMethodRuns() throws ReflectiveOperationException {
        var created = instances();
        assertTrue(created.size() >= 8);
        int calls = 0;
        for (Object mapper : created.values()) {
            for (Method method : mapper.getClass().getMethods()) {
                if (method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Object[] args = new Object[method.getParameterCount()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = Fixtures.of(method.getParameterTypes()[i], method.getGenericParameterTypes()[i], 0);
                }
                try {
                    method.invoke(mapper, args);
                    calls++;
                } catch (java.lang.reflect.InvocationTargetException e) {
                    fail(mapper.getClass().getSimpleName() + "." + method.getName() + " threw " + e.getCause(), e.getCause());
                }
            }
        }
        assertTrue(calls > 30);
    }
}

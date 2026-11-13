package com.prx.directory.support;

import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/// Builds populated instances of transfer objects, entities and their parameters for the reflection-driven tests: a non-null value for
/// every field, one element in every collection, so the mappers walk their whole body instead of stopping at the first null.
public final class Fixtures {

    private Fixtures() {
        // static helpers
    }

    /// A populated instance of the type.
    ///
    /// @param type the type
    /// @return a non-null value (a mock for an interface that is not a collection)
    public static Object of(Class<?> type) {
        return of(type, type, 0);
    }

    /// A populated instance of a generic type.
    ///
    /// @param type    the raw type
    /// @param generic the generic type, to size the collections
    /// @param depth   recursion guard
    /// @return a non-null value
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object of(Class<?> type, Type generic, int depth) {
        if (type == String.class || type == CharSequence.class) return "value";
        if (type == UUID.class) return UUID.fromString("11111111-1111-1111-1111-111111111111");
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == int.class || type == Integer.class) return 1;
        if (type == long.class || type == Long.class) return 1L;
        if (type == double.class || type == Double.class) return 1.0d;
        if (type == float.class || type == Float.class) return 1.0f;
        if (type == short.class || type == Short.class) return (short) 1;
        if (type == byte.class || type == Byte.class) return (byte) 1;
        if (type == char.class || type == Character.class) return 'a';
        if (type == BigDecimal.class) return BigDecimal.TEN;
        if (type == BigInteger.class) return BigInteger.TEN;
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        if (type == LocalDate.class) return LocalDate.of(2026, 1, 2);
        if (type == LocalTime.class) return LocalTime.of(3, 4);
        if (type == Instant.class) return Instant.parse("2026-01-02T03:04:05Z");
        if (type == OffsetDateTime.class) return OffsetDateTime.parse("2026-01-02T03:04:05Z");
        if (type == ZonedDateTime.class) return ZonedDateTime.parse("2026-01-02T03:04:05Z");
        if (type == Date.class) return new Date(0L);
        if (type == java.time.Duration.class) return java.time.Duration.ofSeconds(1);
        if (type == java.time.ZoneId.class) return java.time.ZoneId.of("UTC");
        if (type == java.net.URI.class) return java.net.URI.create("https://example.test");
        if (type == byte[].class) return new byte[]{1, 2};
        if (type == Object.class) return "value";
        if (type.isEnum()) return type.getEnumConstants()[0];
        if (type == Optional.class) return Optional.of(element(generic, 0, depth));
        if (type.isArray()) {
            Object array = java.lang.reflect.Array.newInstance(type.getComponentType(), 1);
            java.lang.reflect.Array.set(array, 0, of(type.getComponentType(), type.getComponentType(), depth + 1));
            return array;
        }
        if (Map.class.isAssignableFrom(type)) {
            Map map = new HashMap();
            map.put(element(generic, 0, depth), element(generic, 1, depth));
            return map;
        }
        if (Set.class.isAssignableFrom(type)) {
            Set set = new HashSet();
            set.add(element(generic, 0, depth));
            return set;
        }
        if (Collection.class.isAssignableFrom(type)) {
            List list = new ArrayList();
            list.add(element(generic, 0, depth));
            return list;
        }
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return Mockito.mock(type);
        }
        if (type.getName().startsWith("java.")) {
            throw new IllegalArgumentException("no sample for " + type);
        }
        return construct(type, depth);
    }

    private static Object element(Type generic, int index, int depth) {
        if (generic instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length > index) {
            Type argument = parameterized.getActualTypeArguments()[index];
            if (argument instanceof Class<?> clazz) {
                return of(clazz, clazz, depth + 1);
            }
            if (argument instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> raw) {
                return of(raw, nested, depth + 1);
            }
        }
        return "value";
    }

    private static Object construct(Class<?> type, int depth) {
        if (depth > 6) {
            return null;
        }
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        Arrays.sort(constructors, Comparator.comparingInt((Constructor<?> c) -> c.getParameterCount()).reversed());
        RuntimeException last = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isSynthetic()) {
                continue;
            }
            try {
                constructor.setAccessible(true);
                Class<?>[] types = constructor.getParameterTypes();
                Type[] generics = constructor.getGenericParameterTypes();
                Object[] args = new Object[types.length];
                for (int i = 0; i < types.length; i++) {
                    args[i] = of(types[i], generics[i], depth + 1);
                }
                Object instance = constructor.newInstance(args);
                populate(instance, depth);
                return instance;
            } catch (ReflectiveOperationException | RuntimeException e) {
                last = new IllegalStateException(e);
            }
        }
        throw last == null ? new IllegalStateException("no constructor for " + type) : last;
    }

    /// Calls every public one-argument setter of a bean with a populated value (entities are built with their no-argument constructor).
    private static void populate(Object instance, int depth) {
        for (var method : instance.getClass().getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1 && !Modifier.isStatic(method.getModifiers())) {
                try {
                    method.invoke(instance, of(method.getParameterTypes()[0], method.getGenericParameterTypes()[0], depth + 1));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // a setter that refuses the sample value is simply skipped
                }
            }
        }
    }
}

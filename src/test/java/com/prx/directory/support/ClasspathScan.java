package com.prx.directory.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/// Finds the classes of a package of the application, so the reflection tests need no hand-kept list.
public final class ClasspathScan {

    private ClasspathScan() {
        // static helpers
    }

    /// The classes of the package and its sub-packages (nested and anonymous classes excluded).
    ///
    /// @param basePackage the package
    /// @return the classes
    public static List<Class<?>> classesIn(String basePackage) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
            try {
                classes.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return classes;
    }
}

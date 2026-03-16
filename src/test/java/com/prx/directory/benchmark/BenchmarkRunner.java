package com.prx.directory.benchmark;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;
import java.util.Comparator;

/**
 * Entry point for running all JMH benchmarks via the Maven {@code benchmark} profile.
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java -P benchmark -DskipTests
 * </pre>
 *
 * <p>Profilers enabled:
 * <ul>
 *   <li>{@code gc}   – GC pressure, allocation rate and norm (bytes/op)</li>
 *   <li>{@code stack} – sampling stack profiler to identify hot methods</li>
 * </ul>
 */
public class BenchmarkRunner {

    private BenchmarkRunner() {}

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                // Run both benchmark classes
                .include(FavoriteStreamBenchmark.class.getSimpleName())
                .include(CampaignCountBenchmark.class.getSimpleName())
                // GC profiler measures allocation rate and GC pressure inline.
                // Stack profiler is omitted here because it requires forking.
                .addProfiler("gc")
                // forks(0): run inline in the current JVM so the exec-maven-plugin classpath
                // is inherited. Using forks(1) would launch a subprocess that lacks the
                // test-scope JARs on its classpath, causing ClassNotFoundException.
                .forks(0)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        System.out.println("=============================================================");
        System.out.println(" Directory Backend – JMH Benchmark Suite");
        System.out.println("=============================================================");

        Collection<RunResult> results = new Runner(opts).run();

        System.out.println("\n=============================================================");
        System.out.println(" Summary (sorted by score, worst first)");
        System.out.println("=============================================================");

        results.stream()
                .sorted(Comparator.comparingDouble(r -> -r.getPrimaryResult().getScore()))
                .forEach(r -> System.out.printf(
                        "  %-70s  %10.3f ± %6.3f %s%n",
                        r.getPrimaryResult().getLabel(),
                        r.getPrimaryResult().getScore(),
                        r.getPrimaryResult().getScoreError(),
                        r.getPrimaryResult().getScoreUnit()));
    }
}

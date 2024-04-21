package com.acabra;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.IntStream;

class KeyGenerationServiceTest {

    private static boolean DEBUG = false;
    private static int MOD = 20;

    @Test
    public void howManyUniqueKeys_beforeOneDuplicates() {
        // Given
        final int keySize = 8;
        final int repetitions = 1;
        System.out.printf("max keys -> %.2f\n", Math.pow(36, keySize));
        final int runs = 100;

        final ResultsAggregator results = ResultsAggregator.empty();

        // When
        IntStream.range(0, runs).forEach((i) -> {
            results.accept(
                    this.getExecutionResult(
                            Runtime.getRuntime().availableProcessors() - 1,
                            keySize,
                            repetitions, i+1)
            );
        });

        // Then

        // The duplicated generated keys is less thank 5%
        Assertions.assertThat(results.repeatedKeys().size()).isCloseTo(runs, Percentage.withPercentage(5));
        // The unique total keys generated before a duplicate is found is about 2 Million keys
        Assertions.assertThat(results.uniqueKeys().getAverage()).isCloseTo(2_000_000, Percentage.withPercentage(10));
        // Running on 3 processors it takes more than 1 second (on average) to generate a duplicate
        Assertions.assertThat(results.time().getAverage()).isGreaterThan(1);
        // The probability of a first duplicate (on average) is less than 0.00008%
        Assertions.assertThat(results.prob().getAverage()).isLessThan(0.00008);
    }
    @Test
    public void howManyUniqueKeys_beforeFiveDuplicates() {
        // Given
        final int keySize = 8;
        final int repetitions = 5;
        System.out.printf("max keys -> %.2f\n", Math.pow(36, keySize));
        final int runs = 100;

        final ResultsAggregator results = ResultsAggregator.empty();

        // When
        IntStream.range(0, runs).forEach((i) -> {
            results.accept(
                    this.getExecutionResult(
                            Runtime.getRuntime().availableProcessors() - 1,
                            keySize,
                            repetitions, i+1)
            );
        });

        // Then

        // The duplicated generated keys is less thank 5%
        Assertions.assertThat(results.repeatedKeys().size()).isCloseTo(runs, Percentage.withPercentage(5));
        // The unique total keys generated before a duplicate is found is about 2 Million keys
        Assertions.assertThat(results.uniqueKeys().getAverage()).isCloseTo(5_000_000, Percentage.withPercentage(10));
        // Running on 3 processors it takes more than 1 second (on average) to generate a duplicate
        Assertions.assertThat(results.time().getAverage()).isGreaterThan(3);
        // The probability of a first duplicate (on average) is less than 0.000188%
        Assertions.assertThat(results.prob().getAverage()).isLessThan(0.000188);
    }

    private ExecutionResult getExecutionResult(int nThreads, int keySize, int target, int index) {
        final ConcurrentHashMap<String, Integer> uniqueKeys = new ConcurrentHashMap<>();
        final AtomicLong repetitions = new AtomicLong(0L);
        final LongAdder hits = new LongAdder();
        final AtomicBoolean ab = new AtomicBoolean(true);
        KeyGenerationService kgs = new KeyGenerationService(keySize);
        final long globalTime = System.nanoTime();

        // when
        try (ExecutorService ex = Executors.newFixedThreadPool(nThreads)) {
            CompletableFuture<KeyAndTime>[] tasks = new CompletableFuture[nThreads];
            final List<CompletableFuture<KeyAndTime>> list = IntStream.range(0, nThreads)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        final long initTime = System.nanoTime();
                        try {
                            while (ab.get()) {
                                final String nextKey = kgs.getNextKey();
                                hits.increment();
                                final int value = uniqueKeys.merge(nextKey, 1, Integer::sum);
                                if (value > 1 && repetitions.incrementAndGet() == target) {
                                    ab.set(false);
                                    return new KeyAndTime(initTime - System.nanoTime(), nextKey, value - 1);
                                }
                            }
                            return new KeyAndTime(initTime - System.nanoTime(), null, 0);
                        } catch (Exception e) {
                            ab.set(false);
                            e.printStackTrace();
                            return new KeyAndTime(initTime - System.nanoTime(), null, 0);
                        }
                    }, ex)).toList();
            IntStream.range(0, nThreads).forEach(i -> tasks[i] = list.get(i));
            CompletableFuture.allOf(tasks).join();
            final double time = (System.nanoTime() - globalTime) / 1_000_000_000.0;
            final double probability = (repetitions.doubleValue() / hits.doubleValue()) * 100;
            if (DEBUG && index % MOD == 1) {

                System.out.printf(
                        "%d. Hits: [%d] Reps: [%d] Time %.4f secs\n", index, hits.sum(), repetitions.get(), time);
            }
            final List<String> repeatedKeys = Arrays.stream(tasks)
                    .map(s -> {
                        try {
                            if (s.isDone() && s.get().times() > 0) {
                                return s.get().key();
                            }
                            return "";
                        } catch (Exception e) {
                            return "";
                        }
                    }).filter(s -> !s.isEmpty()).toList();
            return new ExecutionResult(probability, time, hits.sum() - target, repeatedKeys);
        }
    }
}

record KeyAndTime(double time, String key, int times) {}

record ResultsAggregator(
        DoubleSummaryStatistics prob,
        DoubleSummaryStatistics time,
        LongSummaryStatistics uniqueKeys,
        Set<String> repeatedKeys
    ) implements Consumer<ExecutionResult>{
        public static ResultsAggregator empty() {
            return new ResultsAggregator(
                    new DoubleSummaryStatistics(),
                    new DoubleSummaryStatistics(),
                    new LongSummaryStatistics(),
                    new HashSet<>());
        }

        @Override
        public void accept(ExecutionResult executionResult) {
            prob.accept(executionResult.probability());
            time.accept(executionResult.timeSecs());
            uniqueKeys.accept(executionResult.uniqueKeys());
            repeatedKeys.addAll(executionResult.repeatedKeys());
        }
}
record ExecutionResult(double probability, double timeSecs, long uniqueKeys, List<String> repeatedKeys){}
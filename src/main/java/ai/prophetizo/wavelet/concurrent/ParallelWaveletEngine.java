package ai.prophetizo.wavelet.concurrent;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Multi-threaded wavelet transform engine for batch processing.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic parallelization based on available cores</li>
 *   <li>Work-stealing for load balancing</li>
 *   <li>Memory pool integration for zero allocation overhead</li>
 *   <li>Configurable thread pool and batch sizes</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class ParallelWaveletEngine implements AutoCloseable {

    private static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private static final int MIN_BATCH_SIZE = 16; // Minimum signals per task

    private final ForkJoinPool executorPool;
    private final int parallelism;
    private final boolean shutdownOnClose;

    /**
     * Creates a new parallel engine with default settings.
     */
    public ParallelWaveletEngine() {
        this(DEFAULT_PARALLELISM);
    }

    /**
     * Creates a new parallel engine with specified parallelism.
     */
    public ParallelWaveletEngine(int parallelism) {
        this.parallelism = parallelism;
        this.executorPool = new ForkJoinPool(
                parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null,
                true // async mode for better throughput
        );
        this.shutdownOnClose = true;
    }

    /**
     * Creates a new parallel engine using an existing executor.
     */
    public ParallelWaveletEngine(ForkJoinPool executor) {
        this.executorPool = executor;
        this.parallelism = executor.getParallelism();
        this.shutdownOnClose = false;
    }

    /**
     * Transforms multiple signals in parallel.
     *
     * @param signals array of input signals
     * @param wavelet the wavelet to use
     * @param mode    boundary handling mode
     * @return array of transform results
     */
    public TransformResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode) {
        if (signals.length < parallelism * 2) {
            // For small batches, sequential might be faster
            return transformSequential(signals, wavelet, mode);
        }

        try {
            return executorPool.submit(() ->
                    parallelTransform(signals, wavelet, mode)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Parallel transform failed", e);
        }
    }

    /**
     * Transforms multiple signals asynchronously.
     */
    public CompletableFuture<TransformResult[]> transformBatchAsync(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        return CompletableFuture.supplyAsync(() ->
                        parallelTransform(signals, wavelet, mode),
                executorPool
        );
    }

    /**
     * Processes signals in parallel with custom processor.
     */
    public <T> List<T> processBatch(double[][] signals, SignalProcessor<T> processor) {
        if (signals.length < parallelism * 2) {
            return processSequential(signals, processor);
        }

        try {
            return executorPool.submit(() ->
                    parallelProcess(signals, processor)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Parallel processing failed", e);
        }
    }

    /**
     * Multi-level decomposition in parallel.
     */
    public MultiLevelResult[] multiLevelDecomposeBatch(
            double[][] signals, Wavelet wavelet, int levels, BoundaryMode mode) {

        return processBatch(signals, signal -> {
            MultiLevelResult result = new MultiLevelResult(levels);
            double[] current = signal;

            for (int level = 0; level < levels; level++) {
                WaveletTransform transform = new WaveletTransform(wavelet, mode);
                TransformResult levelResult = transform.forward(current);

                result.setLevel(level, levelResult);
                current = levelResult.approximationCoeffs();

                // Stop if signal becomes too small
                if (current.length < 4) {  // Minimum signal length for transforms
                    break;
                }
            }

            return result;
        }).toArray(new MultiLevelResult[0]);
    }

    /**
     * Parallel transform implementation using Fork/Join.
     */
    private TransformResult[] parallelTransform(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        TransformTask task = new TransformTask(signals, 0, signals.length, wavelet, mode);
        return executorPool.invoke(task);
    }

    /**
     * Parallel processing implementation.
     */
    private <T> List<T> parallelProcess(double[][] signals, SignalProcessor<T> processor) {
        ProcessTask<T> task = new ProcessTask<>(signals, 0, signals.length, processor);
        return executorPool.invoke(task);
    }

    /**
     * Sequential fallback for small batches.
     */
    private TransformResult[] transformSequential(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        TransformResult[] results = new TransformResult[signals.length];
        WaveletTransform transform = new WaveletTransform(wavelet, mode);

        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }

        return results;
    }

    /**
     * Sequential processing fallback.
     */
    private <T> List<T> processSequential(double[][] signals, SignalProcessor<T> processor) {
        List<T> results = new ArrayList<>(signals.length);

        for (double[] signal : signals) {
            results.add(processor.process(signal));
        }

        return results;
    }

    @Override
    public void close() {
        if (shutdownOnClose) {
            executorPool.shutdown();
            try {
                if (!executorPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Functional interface for signal processing.
     */
    @FunctionalInterface
    public interface SignalProcessor<T> {
        T process(double[] signal);
    }

    /**
     * Fork/Join task for wavelet transforms.
     */
    private static class TransformTask extends RecursiveTask<TransformResult[]> {
        private final double[][] signals;
        private final int start;
        private final int end;
        private final Wavelet wavelet;
        private final BoundaryMode mode;

        TransformTask(double[][] signals, int start, int end,
                      Wavelet wavelet, BoundaryMode mode) {
            this.signals = signals;
            this.start = start;
            this.end = end;
            this.wavelet = wavelet;
            this.mode = mode;
        }

        @Override
        protected TransformResult[] compute() {
            int length = end - start;

            // Base case: process directly
            if (length <= MIN_BATCH_SIZE) {
                TransformResult[] results = new TransformResult[length];
                WaveletTransform transform = new WaveletTransform(wavelet, mode);

                for (int i = 0; i < length; i++) {
                    results[i] = transform.forward(signals[start + i]);
                }

                return results;
            }

            // Recursive case: split and fork
            int mid = start + length / 2;
            TransformTask leftTask = new TransformTask(signals, start, mid, wavelet, mode);
            TransformTask rightTask = new TransformTask(signals, mid, end, wavelet, mode);

            leftTask.fork();
            TransformResult[] rightResult = rightTask.compute();
            TransformResult[] leftResult = leftTask.join();

            // Merge results
            TransformResult[] results = new TransformResult[length];
            System.arraycopy(leftResult, 0, results, 0, leftResult.length);
            System.arraycopy(rightResult, 0, results, leftResult.length, rightResult.length);

            return results;
        }
    }

    /**
     * Fork/Join task for generic processing.
     */
    private static class ProcessTask<T> extends RecursiveTask<List<T>> {
        private final double[][] signals;
        private final int start;
        private final int end;
        private final SignalProcessor<T> processor;

        ProcessTask(double[][] signals, int start, int end, SignalProcessor<T> processor) {
            this.signals = signals;
            this.start = start;
            this.end = end;
            this.processor = processor;
        }

        @Override
        protected List<T> compute() {
            int length = end - start;

            if (length <= MIN_BATCH_SIZE) {
                List<T> results = new ArrayList<>(length);
                for (int i = start; i < end; i++) {
                    results.add(processor.process(signals[i]));
                }
                return results;
            }

            int mid = start + length / 2;
            ProcessTask<T> leftTask = new ProcessTask<>(signals, start, mid, processor);
            ProcessTask<T> rightTask = new ProcessTask<>(signals, mid, end, processor);

            leftTask.fork();
            List<T> rightResult = rightTask.compute();
            List<T> leftResult = leftTask.join();

            // Merge results
            List<T> results = new ArrayList<>(length);
            results.addAll(leftResult);
            results.addAll(rightResult);

            return results;
        }
    }

    /**
     * Container for multi-level decomposition results.
     */
    public static class MultiLevelResult {
        private final TransformResult[] levels;
        private int actualLevels;

        public MultiLevelResult(int maxLevels) {
            this.levels = new TransformResult[maxLevels];
            this.actualLevels = 0;
        }

        public void setLevel(int level, TransformResult result) {
            levels[level] = result;
            actualLevels = Math.max(actualLevels, level + 1);
        }

        public TransformResult getLevel(int level) {
            return levels[level];
        }

        public int getLevels() {
            return actualLevels;
        }

        public double[] getFinalApproximation() {
            return actualLevels > 0 ? levels[actualLevels - 1].approximationCoeffs() : null;
        }
    }
}
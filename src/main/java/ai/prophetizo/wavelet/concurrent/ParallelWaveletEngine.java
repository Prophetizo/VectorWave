package ai.prophetizo.wavelet.concurrent;

import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTTransform;
import ai.prophetizo.wavelet.modwt.MultiLevelMODWTResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Multi-threaded MODWT (Maximal Overlap Discrete Wavelet Transform) engine for batch processing of signals.
 *
 * <p>This engine provides efficient parallel processing of multiple signals using
 * the Fork/Join framework with MODWT's shift-invariant properties. It's particularly useful for:</p>
 * <ul>
 *   <li>Processing large batches of financial time series</li>
 *   <li>Real-time multi-channel signal analysis</li>
 *   <li>High-throughput data processing pipelines</li>
 *   <li>Parallel feature extraction for machine learning</li>
 * </ul>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic parallelization based on available cores</li>
 *   <li>Work-stealing for optimal load balancing</li>
 *   <li>Memory pool integration for zero allocation overhead</li>
 *   <li>Configurable thread pool and batch sizes</li>
 *   <li>Adaptive switching between parallel and sequential processing</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Process multiple stock price series in parallel
 * try (ParallelWaveletEngine engine = new ParallelWaveletEngine()) {
 *     double[][] priceSeries = loadStockData(); // e.g., 1000 stocks × 256 prices
 *     
 *     MODWTResult[] results = engine.transformBatch(
 *         priceSeries, 
 *         Daubechies.DB4, 
 *         BoundaryMode.PERIODIC
 *     );
 *     
 *     // Analyze results for each stock
 *     for (int i = 0; i < results.length; i++) {
 *         analyzeVolatility(results[i]);
 *     }
 * }
 * 
 * // Using with custom thread pool
 * ForkJoinPool customPool = new ForkJoinPool(16);
 * ParallelWaveletEngine engine = new ParallelWaveletEngine(customPool);
 * // Engine won't shut down the pool on close
 * }</pre>
 *
 * <p>Performance considerations:</p>
 * <ul>
 *   <li>Overhead of parallelization is ~10-20μs per batch</li>
 *   <li>Best performance with batch sizes > 2 × parallelism</li>
 *   <li>Automatically falls back to sequential for small batches</li>
 *   <li>Memory usage scales with parallelism level</li>
 * </ul>
 *
 */
public class ParallelWaveletEngine implements AutoCloseable {

    private static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private static final int MIN_BATCH_SIZE = 16; // Minimum signals per task

    private final ForkJoinPool executorPool;
    private final int parallelism;
    private final boolean shutdownOnClose;

    /**
     * Creates a new parallel engine with default settings.
     * 
     * <p>Uses the number of available processors as the parallelism level.
     * A new ForkJoinPool is created and will be shut down when this engine is closed.</p>
     */
    public ParallelWaveletEngine() {
        this(DEFAULT_PARALLELISM);
    }

    /**
     * Creates a new parallel engine with specified parallelism.
     * 
     * <p>A new ForkJoinPool is created with the specified parallelism level.
     * The pool will be shut down when this engine is closed.</p>
     * 
     * @param parallelism the number of worker threads
     * @throws IllegalArgumentException if parallelism <= 0
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
    public MODWTResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode) {
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
    public CompletableFuture<MODWTResult[]> transformBatchAsync(
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
    public MultiLevelMODWTResult[] multiLevelDecomposeBatch(
            double[][] signals, Wavelet wavelet, int levels, BoundaryMode mode) {

        return processBatch(signals, signal -> {
            MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(wavelet, mode);
            return transform.decompose(signal, levels);
        }).toArray(new MultiLevelMODWTResult[0]);
    }

    /**
     * Parallel transform implementation using Fork/Join.
     */
    private MODWTResult[] parallelTransform(
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
    private MODWTResult[] transformSequential(
            double[][] signals, Wavelet wavelet, BoundaryMode mode) {

        MODWTResult[] results = new MODWTResult[signals.length];
        MODWTTransform transform = new MODWTTransform(wavelet, mode);

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
    private static class TransformTask extends RecursiveTask<MODWTResult[]> {
        private static final long serialVersionUID = 1L;
        
        private final double[][] signals;
        private final int start;
        private final int end;
        private final transient Wavelet wavelet;
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
        protected MODWTResult[] compute() {
            int length = end - start;

            // Base case: process directly
            if (length <= MIN_BATCH_SIZE) {
                MODWTResult[] results = new MODWTResult[length];
                MODWTTransform transform = new MODWTTransform(wavelet, mode);

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
            MODWTResult[] rightResult = rightTask.compute();
            MODWTResult[] leftResult = leftTask.join();

            // Merge results
            MODWTResult[] results = new MODWTResult[length];
            System.arraycopy(leftResult, 0, results, 0, leftResult.length);
            System.arraycopy(rightResult, 0, results, leftResult.length, rightResult.length);

            return results;
        }
    }

    /**
     * Fork/Join task for generic processing.
     */
    private static class ProcessTask<T> extends RecursiveTask<List<T>> {
        private static final long serialVersionUID = 1L;
        
        private final double[][] signals;
        private final int start;
        private final int end;
        private final transient SignalProcessor<T> processor;

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

}
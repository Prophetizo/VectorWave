package ai.prophetizo.wavelet.concurrent;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Virtual thread-based wavelet transform engine for batch processing of signals.
 *
 * <p>This engine leverages Java 23's virtual threads for efficient parallel processing
 * of multiple signals. Virtual threads provide better scalability than platform threads
 * for I/O-bound and fine-grained parallel tasks.</p>
 * 
 * <p>Key advantages over traditional thread pools:</p>
 * <ul>
 *   <li>Lightweight threads with minimal memory overhead</li>
 *   <li>Better scalability for thousands of concurrent operations</li>
 *   <li>Simplified programming model without thread pool sizing concerns</li>
 *   <li>Automatic optimal scheduling by the JVM</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (VirtualThreadWaveletEngine engine = new VirtualThreadWaveletEngine()) {
 *     double[][] signals = loadSignals();
 *     
 *     TransformResult[] results = engine.transformBatch(
 *         signals, 
 *         Daubechies.DB4, 
 *         BoundaryMode.PERIODIC
 *     );
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class VirtualThreadWaveletEngine implements AutoCloseable {
    
    private final ExecutorService executor;
    private final boolean shutdownOnClose;
    
    /**
     * Creates a new virtual thread wavelet engine.
     * 
     * <p>Uses virtual threads for optimal performance and scalability.</p>
     */
    public VirtualThreadWaveletEngine() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.shutdownOnClose = true;
    }
    
    /**
     * Creates a new engine using an existing virtual thread executor.
     * 
     * @param executor existing virtual thread executor
     */
    public VirtualThreadWaveletEngine(ExecutorService executor) {
        this.executor = executor;
        this.shutdownOnClose = false;
    }
    
    /**
     * Transforms multiple signals in parallel using virtual threads.
     * 
     * @param signals array of signals to transform
     * @param wavelet the wavelet to use
     * @param mode boundary handling mode
     * @return array of transform results
     * @throws InterruptedException if interrupted while waiting
     */
    public TransformResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode) 
            throws InterruptedException {
        
        if (signals == null || signals.length == 0) {
            return new TransformResult[0];
        }
        
        List<Future<TransformResult>> futures = new ArrayList<>(signals.length);
        
        // Submit each transform as a separate virtual thread task
        for (double[] signal : signals) {
            Future<TransformResult> future = executor.submit(() -> {
                WaveletTransform transform = new WaveletTransform(wavelet, mode);
                return transform.forward(signal);
            });
            futures.add(future);
        }
        
        // Collect results
        TransformResult[] results = new TransformResult[signals.length];
        for (int i = 0; i < futures.size(); i++) {
            try {
                results[i] = futures.get(i).get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Transform failed for signal " + i, e.getCause());
            }
        }
        
        return results;
    }
    
    /**
     * Transforms multiple signals with a timeout.
     * 
     * @param signals array of signals to transform
     * @param wavelet the wavelet to use
     * @param mode boundary handling mode
     * @param timeout maximum time to wait
     * @param unit time unit for timeout
     * @return array of transform results
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException if timeout expires
     * @throws ExecutionException if a transform fails
     */
    public TransformResult[] transformBatch(double[][] signals, Wavelet wavelet, BoundaryMode mode,
                                          long timeout, TimeUnit unit) 
            throws InterruptedException, TimeoutException, ExecutionException {
        
        if (signals == null || signals.length == 0) {
            return new TransformResult[0];
        }
        
        List<Future<TransformResult>> futures = new ArrayList<>(signals.length);
        
        // Submit all tasks
        for (double[] signal : signals) {
            Future<TransformResult> future = executor.submit(() -> {
                WaveletTransform transform = new WaveletTransform(wavelet, mode);
                return transform.forward(signal);
            });
            futures.add(future);
        }
        
        // Collect results with timeout
        TransformResult[] results = new TransformResult[signals.length];
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        
        for (int i = 0; i < futures.size(); i++) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                // Cancel remaining futures
                for (int j = i; j < futures.size(); j++) {
                    futures.get(j).cancel(true);
                }
                throw new TimeoutException("Timeout after processing " + i + " signals");
            }
            
            results[i] = futures.get(i).get(remaining, TimeUnit.NANOSECONDS);
        }
        
        return results;
    }
    
    /**
     * Processes signals with a custom function using virtual threads.
     * 
     * @param signals input signals
     * @param processor function to process each signal
     * @return processed results
     * @throws InterruptedException if interrupted
     * @throws ExecutionException if processing fails
     */
    public <T> List<T> processBatch(double[][] signals, Function<double[], T> processor) 
            throws InterruptedException, ExecutionException {
        
        if (signals == null || signals.length == 0) {
            return List.of();
        }
        
        List<Future<T>> futures = new ArrayList<>(signals.length);
        
        // Submit all tasks
        for (double[] signal : signals) {
            futures.add(executor.submit(() -> processor.apply(signal)));
        }
        
        // Collect results
        List<T> results = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        
        return results;
    }
    
    /**
     * Gets the executor service used by this engine.
     * 
     * @return the executor service
     */
    public ExecutorService getExecutor() {
        return executor;
    }
    
    /**
     * Checks if this engine is using virtual threads.
     * 
     * @return true (this implementation always uses virtual threads)
     */
    public boolean isUsingVirtualThreads() {
        return true;
    }
    
    @Override
    public void close() {
        if (shutdownOnClose && executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
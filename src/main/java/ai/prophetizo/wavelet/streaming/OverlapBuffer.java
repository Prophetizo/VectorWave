package ai.prophetizo.wavelet.streaming;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages overlapping buffers for smooth block transitions in streaming processing.
 *
 * <p>This class handles the overlap-add method for processing streaming signals
 * in blocks while maintaining continuity across block boundaries.</p>
 *
 * <p>The overlap-add method works as follows:
 * <ol>
 *   <li>Each input block is windowed before processing</li>
 *   <li>Overlapping portions of consecutive blocks are summed</li>
 *   <li>The hop size (non-overlapping portion) determines the output size</li>
 * </ol>
 * </p>
 *
 * @since 1.0.0
 */
public class OverlapBuffer {

    // Default cache size - can be overridden via system property
    private static final int DEFAULT_CACHE_SIZE = 100;
    private static final int MAX_CACHE_SIZE = Integer.getInteger(
            "ai.prophetizo.wavelet.windowCacheSize", DEFAULT_CACHE_SIZE);
    
    // LinkedHashMap configuration constants
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    
    // Thread-safe LRU cache for pre-computed windows
    // Design choice: We use LinkedHashMap with explicit synchronization rather than:
    // 1. Collections.synchronizedMap - doesn't make put-and-evict atomic, same issue we're solving
    // 2. ConcurrentHashMap - doesn't support LRU eviction, would require complex custom implementation
    //
    // This approach gives us:
    // - True LRU eviction with the accessOrder=true parameter
    // - Atomic put-and-evict operations via synchronized blocks
    // - Simple, maintainable code that's easy to reason about
    // - Consistent performance characteristics
    private static final Map<WindowKey, double[]> WINDOW_CACHE = new LinkedHashMap<WindowKey, double[]>(
            INITIAL_CAPACITY, LOAD_FACTOR, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<WindowKey, double[]> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    // Lock for cache operations to ensure thread safety
    private static final Object CACHE_LOCK = new Object();

    private final int blockSize;
    private final int overlapSize;
    private final int hopSize;
    private final WindowFunction windowFunction;
    private final double[] window;
    private final double[] overlapAddBuffer;

    private boolean hasHistory;

    /**
     * Creates an overlap buffer with specified parameters.
     *
     * <p>The overlap buffer itself can work with any positive block size and does not
     * require power-of-2 sizes. Non-power-of-2 block sizes work correctly for general
     * signal processing applications. However, when using this buffer with wavelet
     * transforms (e.g., in StreamingDenoiser), the block size must be a power of 2
     * to satisfy wavelet transform requirements.</p>
     *
     * <p>The power-of-2 validation is enforced by the wavelet transform components,
     * not by this buffer, allowing the OverlapBuffer to be used in non-wavelet
     * applications without restrictions.</p>
     *
     * @param blockSize      size of each processing block (must be positive, any value accepted)
     * @param overlapFactor  overlap factor (0.0 to 0.95)
     * @param windowFunction window function for overlap regions
     */
    public OverlapBuffer(int blockSize, double overlapFactor, WindowFunction windowFunction) {
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be positive");
        }
        if (overlapFactor < 0.0 || overlapFactor >= 1.0) {
            throw new IllegalArgumentException("Overlap factor must be in [0, 1)");
        }

        this.blockSize = blockSize;
        this.overlapSize = (int) (blockSize * overlapFactor);
        this.hopSize = blockSize - overlapSize;
        this.windowFunction = windowFunction;

        // Get full-sized window for the block
        this.window = getCachedWindow(blockSize, windowFunction);

        // Buffer for storing overlapping portion from previous block
        this.overlapAddBuffer = new double[overlapSize];
        this.hasHistory = false;
    }

    /**
     * Creates a window function of specified type.
     */
    private static double[] createWindow(int length, WindowFunction type) {
        double[] window = new double[length];

        switch (type) {
            case RECTANGULAR:
                java.util.Arrays.fill(window, 1.0);
                break;

            case HANN:
                // Use periodic Hann window for perfect COLA at 50% overlap
                for (int i = 0; i < length; i++) {
                    window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / length));
                }
                break;

            case TUKEY:
                double alpha = 0.5; // Tukey parameter
                int taperLength = (int) (alpha * length / 2);

                // Left taper
                for (int i = 0; i < taperLength; i++) {
                    window[i] = 0.5 * (1 + Math.cos(Math.PI * (2.0 * i / (alpha * length) - 1)));
                }

                // Middle (flat)
                for (int i = taperLength; i < length - taperLength; i++) {
                    window[i] = 1.0;
                }

                // Right taper
                for (int i = length - taperLength; i < length; i++) {
                    window[i] = 0.5 * (1 + Math.cos(Math.PI * (2.0 * i / (alpha * length) - 2.0 / alpha + 1)));
                }
                break;

            case HAMMING:
                // Use periodic Hamming window
                for (int i = 0; i < length; i++) {
                    window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / length);
                }
                break;

            default:
                java.util.Arrays.fill(window, 1.0);
        }

        return window;
    }

    /**
     * Gets a cached window or creates and caches a new one.
     * This reduces allocation overhead when creating many buffers with the same parameters.
     * Uses LRU eviction when cache size exceeds the limit.
     */
    private static double[] getCachedWindow(int length, WindowFunction type) {
        WindowKey key = new WindowKey(length, type);

        // Check cache first
        synchronized (CACHE_LOCK) {
            double[] cachedWindow = WINDOW_CACHE.get(key);
            if (cachedWindow != null) {
                // Return a copy to prevent external modification
                return cachedWindow.clone();
            }
        }

        // Create new window outside of synchronized block to minimize lock time
        double[] newWindow = createWindow(length, type);

        // Cache it - LRU eviction happens automatically if needed
        synchronized (CACHE_LOCK) {
            // Double-check in case another thread cached it while we were creating
            double[] existing = WINDOW_CACHE.get(key);
            if (existing != null) {
                return existing.clone();
            }
            WINDOW_CACHE.put(key, newWindow.clone());
        }

        return newWindow;
    }

    /**
     * Clears the window cache. Useful for memory-constrained environments.
     */
    public static void clearWindowCache() {
        synchronized (CACHE_LOCK) {
            WINDOW_CACHE.clear();
        }
    }

    /**
     * Gets the current window cache size.
     *
     * @return number of cached windows
     */
    public static int getWindowCacheSize() {
        synchronized (CACHE_LOCK) {
            return WINDOW_CACHE.size();
        }
    }

    /**
     * Gets the maximum window cache size.
     *
     * @return maximum cache size (configurable via system property)
     */
    public static int getMaxWindowCacheSize() {
        return MAX_CACHE_SIZE;
    }

    /**
     * Processes a block with overlap-add handling.
     *
     * <p>For streaming applications, this method returns only the non-overlapping
     * portion (hop size) of the processed block to avoid data duplication.</p>
     *
     * @param input input block (must be blockSize length)
     * @return processed block of hopSize length (except for first block which returns full size)
     */
    public double[] process(double[] input) {
        if (input.length != blockSize) {
            throw new IllegalArgumentException("Input must be exactly blockSize");
        }

        // Special handling for rectangular windows - no overlap-add needed
        if (windowFunction == WindowFunction.RECTANGULAR) {
            if (!hasHistory) {
                hasHistory = true;
                return input.clone(); // Return full first block
            }
            // For subsequent blocks, just return the hop portion
            double[] output = new double[hopSize];
            System.arraycopy(input, 0, output, 0, hopSize);
            return output;
        }

        // Normal overlap-add processing for other window types
        if (!hasHistory) {
            // First block - return the full block and save overlap portion
            hasHistory = true;

            // Apply window to the full block
            double[] processed = new double[blockSize];
            for (int i = 0; i < blockSize; i++) {
                processed[i] = input[i] * window[i];
            }

            // Save the overlap portion (already windowed) for next iteration
            if (overlapSize > 0) {
                System.arraycopy(processed, hopSize, overlapAddBuffer, 0, overlapSize);
            }

            return processed;
        }

        // For subsequent blocks, perform overlap-add
        double[] output = new double[hopSize];

        // Apply window to current block
        double[] windowed = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            windowed[i] = input[i] * window[i];
        }

        // Overlap-add: combine the saved overlap portion with the beginning of current block
        // For properly designed windows (Hann, Hamming), the overlapping windows should sum to ~1.0
        for (int i = 0; i < overlapSize; i++) {
            windowed[i] += overlapAddBuffer[i];
        }

        // Extract the hop-size portion as output
        System.arraycopy(windowed, 0, output, 0, hopSize);

        // Save the new overlap portion (already windowed) for next iteration
        if (overlapSize > 0) {
            System.arraycopy(windowed, hopSize, overlapAddBuffer, 0, overlapSize);
        }

        return output;
    }

    /**
     * Processes denoised coefficients with overlap-add reconstruction.
     *
     * @param denoisedCoeffs denoised wavelet coefficients
     * @return reconstructed signal block
     */
    public double[] processCoefficients(double[] denoisedCoeffs) {
        return process(denoisedCoeffs);
    }

    /**
     * Gets the overlap buffer contents (for testing/debugging).
     *
     * @return copy of overlap buffer or null if no history
     */
    public double[] getOverlapBuffer() {
        if (!hasHistory || overlapSize == 0) {
            return null;
        }
        return overlapAddBuffer.clone();
    }

    /**
     * Resets the buffer to initial state.
     */
    public void reset() {
        hasHistory = false;
        if (overlapSize > 0) {
            java.util.Arrays.fill(overlapAddBuffer, 0.0);
        }
    }

    /**
     * Gets the hop size (non-overlapping portion).
     */
    public int getHopSize() {
        return hopSize;
    }

    /**
     * Gets the overlap size.
     */
    public int getOverlapSize() {
        return overlapSize;
    }

    /**
     * Window function types for overlap processing.
     */
    public enum WindowFunction {
        RECTANGULAR,
        HANN,
        TUKEY,
        HAMMING
    }

    /**
     * Key for window cache lookup.
     */
    private static class WindowKey {
        private final int length;
        private final WindowFunction type;

        WindowKey(int length, WindowFunction type) {
            this.length = length;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WindowKey windowKey = (WindowKey) o;
            return length == windowKey.length && type == windowKey.type;
        }

        @Override
        public int hashCode() {
            return 31 * length + type.hashCode();
        }
    }
}
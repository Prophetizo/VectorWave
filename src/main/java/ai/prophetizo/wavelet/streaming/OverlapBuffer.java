package ai.prophetizo.wavelet.streaming;

/**
 * Manages overlapping buffers for smooth block transitions in streaming processing.
 *
 * <p>This class handles the overlap-add method for processing streaming signals
 * in blocks while maintaining continuity across block boundaries.</p>
 *
 * @since 1.6.0
 */
public class OverlapBuffer {

    private final int blockSize;
    private final int overlapSize;
    private final int hopSize;
    private final WindowFunction windowFunction;

    // Buffers for overlap processing
    private final double[] previousBlock;
    private final double[] currentBlock;
    private final double[] window;
    private final double[] overlapRegion;

    private boolean hasHistory;

    /**
     * Creates an overlap buffer with specified parameters.
     *
     * <p>The overlap buffer can work with any positive block size. However,
     * when using this buffer with wavelet transforms (e.g., in StreamingDenoiser),
     * the block size must be a power of 2 to satisfy wavelet transform requirements.
     * The power-of-2 validation is enforced by the wavelet transform components,
     * not by this buffer.</p>
     *
     * @param blockSize      size of each processing block (must be positive)
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

        this.previousBlock = new double[blockSize];
        this.currentBlock = new double[blockSize];
        // Only create window for overlap region, not entire block
        this.window = createWindow(overlapSize, windowFunction);
        this.overlapRegion = new double[overlapSize];
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
                for (int i = 0; i < length; i++) {
                    window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (length - 1)));
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
                for (int i = 0; i < length; i++) {
                    window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (length - 1));
                }
                break;

            default:
                java.util.Arrays.fill(window, 1.0);
        }

        return window;
    }

    /**
     * Processes a block with overlap handling.
     *
     * @param input input block (must be blockSize length)
     * @return processed block with smooth transitions
     */
    public double[] process(double[] input) {
        if (input.length != blockSize) {
            throw new IllegalArgumentException("Input must be exactly blockSize");
        }

        // Copy input to current block
        System.arraycopy(input, 0, currentBlock, 0, blockSize);

        if (!hasHistory) {
            // First block - no overlap to handle
            hasHistory = true;
            System.arraycopy(currentBlock, 0, previousBlock, 0, blockSize);
            return currentBlock.clone();
        }

        // Apply window to overlapping regions
        double[] output = new double[blockSize];

        // Handle overlap region with cross-fade
        for (int i = 0; i < overlapSize; i++) {
            // Window is now sized for overlap region only
            double fadeOut = window[i];
            double fadeIn = 1.0 - fadeOut;
            output[i] = previousBlock[blockSize - overlapSize + i] * fadeOut +
                    currentBlock[i] * fadeIn;
        }

        // Copy non-overlapping portion
        System.arraycopy(currentBlock, overlapSize, output, overlapSize, hopSize);

        // Save current block for next iteration
        System.arraycopy(currentBlock, 0, previousBlock, 0, blockSize);

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
     * Gets the overlap region from the previous block.
     *
     * @return overlap region or null if no history
     */
    public double[] getOverlapRegion() {
        if (!hasHistory) {
            return null;
        }
        System.arraycopy(previousBlock, blockSize - overlapSize, overlapRegion, 0, overlapSize);
        return overlapRegion.clone();
    }

    /**
     * Resets the buffer to initial state.
     */
    public void reset() {
        hasHistory = false;
        java.util.Arrays.fill(previousBlock, 0.0);
        java.util.Arrays.fill(currentBlock, 0.0);
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
}
package ai.prophetizo.wavelet.util;

/**
 * Utility class for common signal processing operations.
 * 
 * <p>This class provides static methods for signal manipulation operations
 * that are used across different wavelet transform implementations.</p>
 * 
 * @since 1.0
 */
public final class SignalUtils {
    
    private SignalUtils() {
        // Prevent instantiation
    }
    
    /**
     * Applies a circular shift to a signal.
     * 
     * <p><strong>Direction Convention:</strong></p>
     * <ul>
     *   <li><strong>Positive shift (right):</strong> Elements move to higher indices. 
     *       The element at index i moves to index (i + shift) % length.
     *       Elements that would go past the end wrap around to the beginning.</li>
     *   <li><strong>Negative shift (left):</strong> Elements move to lower indices.
     *       The element at index i moves to index (i + shift + length) % length.
     *       Elements that would go before the beginning wrap around to the end.</li>
     * </ul>
     * 
     * <p>This operation is commonly used in wavelet transforms to compensate
     * for phase shifts introduced by certain wavelet filters, particularly
     * biorthogonal wavelets.</p>
     * 
     * <p><strong>Examples:</strong></p>
     * <pre>
     * Original: [1,2,3,4,5] (indices 0,1,2,3,4)
     * 
     * shift=2 (right):  [4,5,1,2,3]
     *   - Element 1 at index 0 → index 2
     *   - Element 2 at index 1 → index 3
     *   - Element 3 at index 2 → index 4
     *   - Element 4 at index 3 → index 0 (wrapped)
     *   - Element 5 at index 4 → index 1 (wrapped)
     * 
     * shift=-2 (left):  [3,4,5,1,2]
     *   - Element 1 at index 0 → index 3 (wrapped)
     *   - Element 2 at index 1 → index 4 (wrapped)
     *   - Element 3 at index 2 → index 0
     *   - Element 4 at index 3 → index 1
     *   - Element 5 at index 4 → index 2
     * 
     * shift=0: [1,2,3,4,5] (no change)
     * shift=5: [1,2,3,4,5] (full rotation, no change)
     * </pre>
     *
     * @param signal the input signal
     * @param shift the number of positions to shift (positive = right, negative = left)
     * @return the shifted signal (new array)
     * @throws NullPointerException if signal is null
     * @throws IllegalArgumentException if signal is empty
     */
    public static double[] circularShift(double[] signal, int shift) {
        if (signal == null) {
            throw new NullPointerException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
        
        int n = signal.length;
        double[] shifted = new double[n];
        
        // Normalize the shift to be in the valid range [0, n)
        shift = normalizeShift(shift, n);
        
        // Perform the circular shift
        for (int i = 0; i < n; i++) {
            shifted[(i + shift) % n] = signal[i];
        }
        
        return shifted;
    }
    
    /**
     * Normalizes a shift value to be in the range [0, n).
     * 
     * <p>This method handles both positive and negative shifts, as well as
     * shifts larger than the array length. The normalization ensures that:</p>
     * <ul>
     *   <li>Negative shifts are converted to equivalent positive shifts</li>
     *   <li>Shifts larger than n are reduced to their equivalent within one rotation</li>
     *   <li>The result is always in the range [0, n)</li>
     * </ul>
     * 
     * <p>Mathematical explanation:</p>
     * <ol>
     *   <li>{@code shift % n} gives a value in range (-n, n)</li>
     *   <li>Adding n ensures the value is positive: range (0, 2n)</li>
     *   <li>Final {@code % n} brings it back to range [0, n)</li>
     * </ol>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>shift=2, n=8 → 2 (no change needed)</li>
     *   <li>shift=-2, n=8 → 6 (left by 2 = right by 6)</li>
     *   <li>shift=10, n=8 → 2 (10 mod 8)</li>
     *   <li>shift=-10, n=8 → 6 (-10 mod 8 = -2, then normalized to 6)</li>
     * </ul>
     * 
     * @param shift the shift amount (can be any integer)
     * @param n the array length (must be positive)
     * @return normalized shift in range [0, n)
     */
    private static int normalizeShift(int shift, int n) {
        // The formula ((shift % n) + n) % n handles all cases:
        // - Positive shifts: (shift % n) is already in [0, n), adding n gives [n, 2n), 
        //   final % n brings back to [0, n)
        // - Negative shifts: (shift % n) is in (-n, 0], adding n gives (0, n], 
        //   final % n gives [0, n)
        // - Large shifts: first % n reduces to (-n, n), then normalized as above
        return ((shift % n) + n) % n;
    }
}
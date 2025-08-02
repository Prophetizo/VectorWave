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
     * Positive shift values shift right, negative values shift left.
     * 
     * <p>This operation is commonly used in wavelet transforms to compensate
     * for phase shifts introduced by certain wavelet filters, particularly
     * biorthogonal wavelets.</p>
     *
     * @param signal the input signal
     * @param shift the number of positions to shift
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
        
        // Normalize shift to be in range [0, n) regardless of sign
        // This handles both positive and negative shifts correctly:
        // - (shift % n) gives a value in range (-n, n)
        // - Adding n ensures the value is positive: range (0, 2n)
        // - Final % n brings it back to range [0, n)
        // Example: shift=-2, n=8 → (-2%8)=-2 → (-2+8)=6 → 6%8=6 (shift left by 2 = shift right by 6)
        shift = ((shift % n) + n) % n;
        
        for (int i = 0; i < n; i++) {
            shifted[(i + shift) % n] = signal[i];
        }
        
        return shifted;
    }
}
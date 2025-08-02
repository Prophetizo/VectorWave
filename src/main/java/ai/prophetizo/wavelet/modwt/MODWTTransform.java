package ai.prophetizo.wavelet.modwt;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.internal.ScalarOps;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Objects;

/**
 * Implementation of the MODWT (Maximal Overlap Discrete Wavelet Transform).
 * 
 * <p>The MODWT is a non-decimated form of the discrete wavelet transform that offers
 * several advantages over the standard DWT:</p>
 * <ul>
 *   <li><strong>Shift-invariant:</strong> Translation of input results in corresponding translation of coefficients</li>
 *   <li><strong>Arbitrary length signals:</strong> Can handle signals of any length, not just powers of two</li>
 *   <li><strong>Same-length output:</strong> Both approximation and detail coefficients have the same length as input</li>
 *   <li><strong>Redundant representation:</strong> Provides more information but at computational cost</li>
 * </ul>
 * 
 * <p>The MODWT uses circular convolution without downsampling and employs scaled filters
 * at each level: h_j,l = h_l / 2^(j/2) for level j.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create MODWT transform with Haar wavelet
 * MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
 * 
 * // Transform arbitrary length signal
 * double[] signal = {1, 2, 3, 4, 5, 6, 7};  // Not power of 2!
 * MODWTResult result = modwt.forward(signal);
 * 
 * // Reconstruct signal
 * double[] reconstructed = modwt.inverse(result);
 * }</pre>
 * 
 * @see ai.prophetizo.wavelet.WaveletTransform
 */
public class MODWTTransform {
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    
    /**
     * Constructs a MODWT transformer with the specified wavelet and boundary mode.
     * 
     * @param wavelet      The wavelet to use for the transformations
     * @param boundaryMode The boundary handling mode (currently only PERIODIC is supported)
     * @throws NullPointerException     if any parameter is null
     * @throws UnsupportedOperationException if boundary mode is not supported
     */
    public MODWTTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null");
        
        // MODWT typically uses periodic boundary conditions
        if (boundaryMode != BoundaryMode.PERIODIC) {
            throw new UnsupportedOperationException(
                "MODWT currently only supports PERIODIC boundary mode. Got: " + boundaryMode);
        }
    }
    
    /**
     * Performs a single-level forward MODWT.
     * 
     * <p>Unlike the standard DWT, this produces approximation and detail coefficients
     * that are the same length as the input signal, making the transform shift-invariant
     * and applicable to arbitrary length signals.</p>
     * 
     * @param signal The input signal of any length ≥ 1
     * @return A MODWTResult containing same-length approximation and detail coefficients
     * @throws InvalidSignalException if signal is invalid
     */
    public MODWTResult forward(double[] signal) {
        // Validate input signal (MODWT supports arbitrary lengths, not just power-of-2)
        Objects.requireNonNull(signal, "signal cannot be null");
        if (signal.length == 0) {
            throw new InvalidSignalException("Signal cannot be empty");
        }
        
        // Validate signal values (finite values check)
        ValidationUtils.validateFiniteValues(signal, "signal");
        
        // Get filter coefficients and scale them for MODWT
        double[] lowPassFilter = wavelet.lowPassDecomposition();
        double[] highPassFilter = wavelet.highPassDecomposition();
        
        // Scale filters for MODWT (level 1 uses scale factor 1/sqrt(2))
        double[] scaledLowPass = ScalarOps.scaleFilterForMODWT(lowPassFilter, 1);
        double[] scaledHighPass = ScalarOps.scaleFilterForMODWT(highPassFilter, 1);
        
        // Prepare output arrays (same length as input)
        int signalLength = signal.length;
        double[] approximationCoeffs = new double[signalLength];
        double[] detailCoeffs = new double[signalLength];
        
        // Perform circular convolution without downsampling
        ScalarOps.circularConvolveMODWT(signal, scaledLowPass, approximationCoeffs);
        ScalarOps.circularConvolveMODWT(signal, scaledHighPass, detailCoeffs);
        
        return new MODWTResultImpl(approximationCoeffs, detailCoeffs);
    }
    
    /**
     * Performs a single-level inverse MODWT to reconstruct the signal.
     * 
     * <p>The reconstruction follows the same pattern as DWT but without upsampling.
     * Uses scaled reconstruction filters and circular convolution.</p>
     * 
     * @param modwtResult The MODWT result containing approximation and detail coefficients
     * @return The reconstructed signal
     * @throws NullPointerException   if modwtResult is null
     * @throws InvalidSignalException if the result contains invalid coefficients
     */
    public double[] inverse(MODWTResult modwtResult) {
        Objects.requireNonNull(modwtResult, "modwtResult cannot be null");
        
        if (!modwtResult.isValid()) {
            throw new InvalidSignalException("MODWTResult contains invalid coefficients");
        }
        
        // Get coefficients (defensive copies)
        double[] approxCoeffs = modwtResult.approximationCoeffs();
        double[] detailCoeffs = modwtResult.detailCoeffs();
        int signalLength = modwtResult.getSignalLength();
        
        // Get reconstruction filter coefficients (use original, no scaling for inverse)
        double[] lowPassRecon = wavelet.lowPassReconstruction();
        double[] highPassRecon = wavelet.highPassReconstruction();
        
        // Prepare intermediate arrays
        double[] approxRecon = new double[signalLength];
        double[] detailRecon = new double[signalLength];
        
        // Perform circular convolution for reconstruction (use original filters, not scaled)
        ScalarOps.circularConvolveMODWT(approxCoeffs, lowPassRecon, approxRecon);
        ScalarOps.circularConvolveMODWT(detailCoeffs, highPassRecon, detailRecon);
        
        // Combine the reconstructed components
        double[] reconstructed = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            reconstructed[i] = approxRecon[i] + detailRecon[i];
        }
        
        return reconstructed;
    }
    
    /**
     * Gets the wavelet used by this transform.
     * 
     * @return the wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this transform.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
}
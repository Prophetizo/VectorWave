package ai.prophetizo.wavelet.denoising;

import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResultImpl;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.internal.VectorOps;

/**
 * Wavelet-based signal denoising using various thresholding strategies.
 *
 * <p>This class provides comprehensive denoising capabilities for signals
 * corrupted by noise, particularly effective for financial time series data
 * where preserving important features while removing noise is critical.</p>
 *
 * <p>Now uses MODWT (Maximal Overlap Discrete Wavelet Transform) which provides:</p>
 * <ul>
 *   <li>Shift-invariant denoising (better for time series)</li>
 *   <li>Works with any signal length (not just power-of-2)</li>
 *   <li>Same-length coefficients preserve temporal alignment</li>
 *   <li>Multiple threshold selection methods (Universal, SURE, Minimax)</li>
 *   <li>Soft and hard thresholding</li>
 *   <li>Level-dependent thresholding</li>
 *   <li>Multi-level decomposition support</li>
 *   <li>SIMD-optimized thresholding operations</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
 * double[] noisySignal = ...;
 * double[] denoised = denoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);
 * }</pre>
 *
 * @since 1.0.0
 */
public class WaveletDenoiser {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final boolean useVectorOps;

    /**
     * Creates a wavelet denoiser with the specified wavelet and boundary mode.
     *
     * @param wavelet      the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @throws InvalidArgumentException if wavelet or boundaryMode is null
     */
    public WaveletDenoiser(Wavelet wavelet, BoundaryMode boundaryMode) {
        if (wavelet == null) {
            throw InvalidArgumentException.nullArgument("wavelet");
        }
        if (boundaryMode == null) {
            throw InvalidArgumentException.nullArgument("boundaryMode");
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.useVectorOps = VectorOps.isVectorizedOperationBeneficial(256); // Check if SIMD is beneficial
    }

    /**
     * Creates a denoiser optimized for financial time series.
     * Uses DB4 wavelet with periodic boundaries and SURE thresholding.
     */
    public static WaveletDenoiser forFinancialData() {
        return new WaveletDenoiser(ai.prophetizo.wavelet.api.Daubechies.DB4, BoundaryMode.PERIODIC);
    }

    /**
     * Denoises a signal using single-level wavelet transform with automatic threshold selection.
     *
     * @param signal the noisy signal to denoise
     * @param method the threshold selection method
     * @return the denoised signal
     * @throws InvalidSignalException if signal is invalid
     */
    public double[] denoise(double[] signal, ThresholdMethod method) {
        return denoise(signal, method, ThresholdType.SOFT);
    }

    /**
     * Denoises a signal using single-level wavelet transform.
     *
     * @param signal the noisy signal to denoise
     * @param method the threshold selection method
     * @param type   the thresholding type (soft or hard)
     * @return the denoised signal
     * @throws InvalidSignalException if signal is invalid
     */
    public double[] denoise(double[] signal, ThresholdMethod method, ThresholdType type) {
        MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
        MODWTResult result = transform.forward(signal);

        // Estimate noise level from detail coefficients
        double sigma = estimateNoiseSigma(result.detailCoeffs());

        // Calculate threshold
        double threshold = calculateThreshold(result.detailCoeffs(), sigma, method);

        // Apply thresholding to detail coefficients
        double[] denoisedDetails = applyThreshold(result.detailCoeffs(), threshold, type);

        // Reconstruct with denoised coefficients
        MODWTResult denoisedResult = new MODWTResultImpl(
                result.approximationCoeffs(), denoisedDetails);

        return transform.inverse(denoisedResult);
    }

    /**
     * Denoises a signal using multi-level wavelet transform with level-dependent thresholding.
     *
     * @param signal the noisy signal to denoise
     * @param levels the number of decomposition levels
     * @param method the threshold selection method
     * @param type   the thresholding type
     * @return the denoised signal
     * @throws InvalidSignalException   if signal is invalid
     * @throws InvalidArgumentException if levels is invalid
     */
    public double[] denoiseMultiLevel(double[] signal, int levels,
                                      ThresholdMethod method, ThresholdType type) {
        // With MODWT, multi-level decomposition preserves signal length at each level
        // This approach uses iterative single-level transforms
        double[] result = signal.clone();

        // Apply wavelet transform and threshold at each level
        for (int level = 0; level < levels; level++) {
            // MODWT preserves signal length, so we work with full-length signal
            // Apply single-level transform
            MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
            MODWTResult levelResult = transform.forward(result);

            // Estimate noise from detail coefficients at first level and reuse for other levels
            double sigma;
            if (level == 0) {
                sigma = estimateNoiseSigma(levelResult.detailCoeffs());
            } else {
                // Reuse sigma from first level, but scale based on level
                sigma = estimateNoiseSigma(levelResult.detailCoeffs());
            }

            // Calculate threshold with level-dependent scaling
            double levelScale = Math.sqrt(1.0 / (level + 1));
            double threshold = calculateThreshold(levelResult.detailCoeffs(), sigma, method) * levelScale;

            // Apply thresholding to detail coefficients
            double[] denoisedDetails = applyThreshold(levelResult.detailCoeffs(), threshold, type);

            // Reconstruct with denoised coefficients
            MODWTResult denoisedResult = new MODWTResultImpl(
                    levelResult.approximationCoeffs(), denoisedDetails);

            result = transform.inverse(denoisedResult);
        }

        return result;
    }

    /**
     * Denoises a signal using a fixed threshold value.
     *
     * @param signal    the noisy signal
     * @param threshold the threshold value
     * @param type      the thresholding type
     * @return the denoised signal
     */
    public double[] denoiseFixed(double[] signal, double threshold, ThresholdType type) {
        MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
        MODWTResult result = transform.forward(signal);

        double[] denoisedDetails = applyThreshold(result.detailCoeffs(), threshold, type);

        MODWTResult denoisedResult = new MODWTResultImpl(
                result.approximationCoeffs(), denoisedDetails);

        return transform.inverse(denoisedResult);
    }

    /**
     * Estimates the noise standard deviation using the Median Absolute Deviation (MAD)
     * of the detail coefficients at the finest scale.
     *
     * @param detailCoeffs the detail coefficients
     * @return estimated noise standard deviation
     */
    private double estimateNoiseSigma(double[] detailCoeffs) {
        // Calculate median absolute deviation
        double[] absCoeffs = new double[detailCoeffs.length];
        for (int i = 0; i < detailCoeffs.length; i++) {
            absCoeffs[i] = Math.abs(detailCoeffs[i]);
        }

        double median = calculateMedian(absCoeffs);

        // Scale factor for Gaussian noise
        return median / 0.6745;
    }

    /**
     * Calculates the threshold value based on the selected method.
     */
    private double calculateThreshold(double[] coeffs, double sigma, ThresholdMethod method) {
        int n = coeffs.length;

        switch (method) {
            case UNIVERSAL:
                // Universal threshold (VisuShrink)
                return sigma * Math.sqrt(2.0 * Math.log(n));

            case SURE:
                // SURE threshold
                return calculateSUREThreshold(coeffs, sigma);

            case MINIMAX:
                // Minimax threshold
                return calculateMinimaxThreshold(n, sigma);

            case FIXED:
                // Should not reach here for automatic threshold selection
                throw new InvalidArgumentException("Use denoiseFixed() for fixed threshold");

            default:
                throw new InvalidArgumentException("Unknown threshold method: " + method);
        }
    }

    /**
     * Calculates SURE (Stein's Unbiased Risk Estimate) threshold.
     */
    private double calculateSUREThreshold(double[] coeffs, double sigma) {
        int n = coeffs.length;

        // Sort coefficients by absolute value
        double[] sortedAbs = new double[n];
        for (int i = 0; i < n; i++) {
            sortedAbs[i] = Math.abs(coeffs[i]);
        }
        java.util.Arrays.sort(sortedAbs);

        // Calculate SURE for each possible threshold
        double minRisk = Double.POSITIVE_INFINITY;
        double bestThreshold = 0;

        for (int k = 0; k < n; k++) {
            double t = sortedAbs[k];
            double risk = calculateSURERisk(coeffs, t, sigma);

            if (risk < minRisk) {
                minRisk = risk;
                bestThreshold = t;
            }
        }

        // Compare with universal threshold
        double universalThreshold = sigma * Math.sqrt(2.0 * Math.log(n));
        if (bestThreshold > universalThreshold) {
            bestThreshold = universalThreshold;
        }

        return bestThreshold;
    }

    /**
     * Calculates SURE risk for a given threshold.
     */
    private double calculateSURERisk(double[] coeffs, double threshold, double sigma) {
        int n = coeffs.length;
        double sigma2 = sigma * sigma;
        double risk = -n * sigma2;

        for (double c : coeffs) {
            double absC = Math.abs(c);
            if (absC <= threshold) {
                risk += c * c;
            } else {
                risk += sigma2 + (absC - threshold) * (absC - threshold);
            }
        }

        return risk / n;
    }

    /**
     * Calculates Minimax threshold.
     */
    private double calculateMinimaxThreshold(int n, double sigma) {
        // Minimax threshold approximation
        double logN = Math.log(n);

        if (n <= 32) {
            return 0;
        } else if (n <= 64) {
            return sigma * 0.3936 + 0.1829 * sigma * logN;
        } else {
            return sigma * (0.4745 + 0.1148 * logN);
        }
    }

    /**
     * Applies thresholding to coefficients.
     */
    private double[] applyThreshold(double[] coeffs, double threshold, ThresholdType type) {
        if (useVectorOps) {
            // Use SIMD-optimized thresholding
            return type == ThresholdType.SOFT
                    ? VectorOps.Denoising.softThreshold(coeffs, threshold)
                    : VectorOps.Denoising.hardThreshold(coeffs, threshold);
        } else {
            // Scalar implementation
            double[] result = new double[coeffs.length];

            if (type == ThresholdType.SOFT) {
                for (int i = 0; i < coeffs.length; i++) {
                    double absCoeff = Math.abs(coeffs[i]);
                    result[i] = absCoeff <= threshold ? 0.0
                            : Math.signum(coeffs[i]) * (absCoeff - threshold);
                }
            } else { // HARD
                for (int i = 0; i < coeffs.length; i++) {
                    result[i] = Math.abs(coeffs[i]) <= threshold ? 0.0 : coeffs[i];
                }
            }

            return result;
        }
    }

    /**
     * Calculates the median of an array.
     */
    private double calculateMedian(double[] values) {
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);

        int n = sorted.length;
        if (n % 2 == 0) {
            return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
        } else {
            return sorted[n / 2];
        }
    }

    /**
     * Threshold selection methods.
     */
    public enum ThresholdMethod {
        /**
         * Universal threshold (VisuShrink): sqrt(2 * log(N)) * sigma
         * Conservative, tends to oversmooth but ensures noise removal.
         */
        UNIVERSAL,

        /**
         * SURE (Stein's Unbiased Risk Estimate) threshold.
         * Adapts to signal characteristics, good for smooth signals.
         */
        SURE,

        /**
         * Minimax threshold: optimal for worst-case MSE.
         * Good compromise between smoothing and feature preservation.
         */
        MINIMAX,

        /**
         * Fixed threshold: user-specified value.
         */
        FIXED
    }

    /**
     * Thresholding function types.
     */
    public enum ThresholdType {
        /**
         * Soft thresholding: shrinks coefficients towards zero.
         * Produces smoother results with less artifacts.
         */
        SOFT,

        /**
         * Hard thresholding: keeps or kills coefficients.
         * Better preserves signal features but may introduce artifacts.
         */
        HARD
    }
}
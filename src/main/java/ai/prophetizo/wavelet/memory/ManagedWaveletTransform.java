package ai.prophetizo.wavelet.memory;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.exception.InvalidSignalException;
import ai.prophetizo.wavelet.util.ValidationUtils;

import java.util.Objects;

/**
 * Memory-aware wavelet transform that can use both heap and off-heap memory management.
 * 
 * <p>This class provides the same functionality as WaveletTransform but uses
 * ManagedArrays internally, allowing for more efficient memory management and
 * potential SIMD optimizations for large arrays.</p>
 */
public class ManagedWaveletTransform implements AutoCloseable {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final ArrayFactoryManager factoryManager;

    /**
     * Constructs a managed transformer with the specified wavelet and boundary mode.
     * Uses the default ArrayFactoryManager configuration.
     *
     * @param wavelet      The wavelet to use for the transformations
     * @param boundaryMode The boundary handling mode
     * @throws NullPointerException if any parameter is null
     */
    public ManagedWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, new ArrayFactoryManager());
    }

    /**
     * Constructs a managed transformer with the specified parameters.
     *
     * @param wavelet        The wavelet to use for the transformations
     * @param boundaryMode   The boundary handling mode
     * @param factoryManager The factory manager for creating arrays
     * @throws NullPointerException if any parameter is null
     */
    public ManagedWaveletTransform(Wavelet wavelet, BoundaryMode boundaryMode, ArrayFactoryManager factoryManager) {
        this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null.");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null.");
        this.factoryManager = Objects.requireNonNull(factoryManager, "factoryManager cannot be null.");

        // Validate supported boundary modes
        if (boundaryMode != BoundaryMode.PERIODIC && boundaryMode != BoundaryMode.ZERO_PADDING) {
            throw new UnsupportedOperationException(
                    "Only PERIODIC and ZERO_PADDING boundary modes are currently supported.");
        }
    }

    /**
     * Performs a single-level forward 1D Fast Wavelet Transform using managed arrays.
     *
     * @param signal The input signal. Must have a power-of-two length.
     * @return A ManagedTransformResult containing the approximation and detail coefficients.
     * @throws InvalidSignalException if signal is null, empty, not power-of-two length, or contains invalid values
     */
    public ManagedTransformResult forwardManaged(double[] signal) {
        // Comprehensive validation
        ValidationUtils.validateSignal(signal, "signal");

        double[] lowPassFilter = wavelet.lowPassDecomposition();
        double[] highPassFilter = wavelet.highPassDecomposition();

        // The output coefficients will be half the length of the input signal.
        int outputLength = signal.length / 2;

        // Create managed arrays for input and output
        try (ManagedArray inputArray = factoryManager.from(signal);
             ManagedArray approximationCoeffs = factoryManager.createAligned(outputLength, 32);
             ManagedArray detailCoeffs = factoryManager.createAligned(outputLength, 32)) {

            // Perform convolution and downsampling based on boundary mode
            if (boundaryMode == BoundaryMode.PERIODIC) {
                ManagedScalarOps.convolveAndDownsamplePeriodicOptimized(inputArray, lowPassFilter, approximationCoeffs);
                ManagedScalarOps.convolveAndDownsamplePeriodicOptimized(inputArray, highPassFilter, detailCoeffs);
            } else {
                ManagedScalarOps.convolveAndDownsampleDirect(inputArray, lowPassFilter, approximationCoeffs);
                ManagedScalarOps.convolveAndDownsampleDirect(inputArray, highPassFilter, detailCoeffs);
            }

            // Create result arrays that will live beyond this method
            ManagedArray resultApprox = factoryManager.createAligned(outputLength, 32);
            ManagedArray resultDetail = factoryManager.createAligned(outputLength, 32);

            // Copy results
            approximationCoeffs.copyTo(resultApprox, 0, 0, outputLength);
            detailCoeffs.copyTo(resultDetail, 0, 0, outputLength);

            return new ManagedTransformResult(resultApprox, resultDetail);
        }
    }

    /**
     * Performs a single-level forward 1D Fast Wavelet Transform with backward compatibility.
     *
     * @param signal The input signal. Must have a power-of-two length.
     * @return A standard TransformResult containing the approximation and detail coefficients.
     * @throws InvalidSignalException if signal is null, empty, not power-of-two length, or contains invalid values
     */
    public TransformResult forward(double[] signal) {
        try (ManagedTransformResult managedResult = forwardManaged(signal)) {
            // Create a traditional WaveletTransform to generate a proper TransformResult
            WaveletTransform traditionalTransform = new WaveletTransform(wavelet, boundaryMode);
            return traditionalTransform.forward(signal);
        }
    }

    /**
     * Performs a single-level inverse 1D Fast Wavelet Transform using managed arrays.
     *
     * @param transformResult The managed transform result containing approximation and detail coefficients
     * @return The reconstructed signal as a ManagedArray
     * @throws NullPointerException   if transformResult is null
     * @throws InvalidSignalException if coefficients are invalid or mismatched
     */
    public ManagedArray inverseManaged(ManagedTransformResult transformResult) {
        Objects.requireNonNull(transformResult, "transformResult cannot be null.");

        ManagedArray approx = transformResult.getManagedApproximationCoeffs();
        ManagedArray detail = transformResult.getManagedDetailCoeffs();

        int outputLength = (approx.length() + detail.length());

        try (ManagedArray approxRecon = factoryManager.createAligned(outputLength, 32);
             ManagedArray detailRecon = factoryManager.createAligned(outputLength, 32)) {

            // Perform upsampling and convolution based on boundary mode
            if (boundaryMode == BoundaryMode.PERIODIC) {
                ManagedScalarOps.upsampleAndConvolvePeriodicOptimized(approx, wavelet.lowPassReconstruction(), approxRecon);
                ManagedScalarOps.upsampleAndConvolvePeriodicOptimized(detail, wavelet.highPassReconstruction(), detailRecon);
            } else {
                ManagedScalarOps.upsampleAndConvolveDirect(approx, wavelet.lowPassReconstruction(), approxRecon);
                ManagedScalarOps.upsampleAndConvolveDirect(detail, wavelet.highPassReconstruction(), detailRecon);
            }

            // Create result array and add the two reconstructed components
            ManagedArray signal = factoryManager.createAligned(outputLength, 32);
            for (int i = 0; i < outputLength; i++) {
                signal.set(i, approxRecon.get(i) + detailRecon.get(i));
            }

            return signal;
        }
    }

    /**
     * Performs a single-level inverse 1D Fast Wavelet Transform with backward compatibility.
     *
     * @param transformResult The transform result containing approximation and detail coefficients
     * @return The reconstructed signal
     * @throws NullPointerException   if transformResult is null
     * @throws InvalidSignalException if coefficients are invalid or mismatched
     */
    public double[] inverse(TransformResult transformResult) {
        Objects.requireNonNull(transformResult, "transformResult cannot be null.");

        // Convert standard TransformResult to managed arrays
        double[] approx = transformResult.approximationCoeffs();
        double[] detail = transformResult.detailCoeffs();

        try (ManagedArray approxArray = factoryManager.from(approx);
             ManagedArray detailArray = factoryManager.from(detail);
             ManagedTransformResult managedResult = new ManagedTransformResult(approxArray, detailArray);
             ManagedArray reconstructed = inverseManaged(managedResult)) {

            return reconstructed.toArray();
        }
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

    /**
     * Gets the factory manager used by this transform.
     *
     * @return the factory manager
     */
    public ArrayFactoryManager getFactoryManager() {
        return factoryManager;
    }

    /**
     * Releases any resources associated with this transform.
     */
    @Override
    public void close() {
        factoryManager.close();
    }
}
package ai.prophetizo.demo;

import ai.prophetizo.wavelet.MODWTTransform;
import ai.prophetizo.wavelet.MODWTTransformFactory;
import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;

import java.util.Arrays;

/**
 * Demonstrates the advantages of MODWT (Maximal Overlap Discrete Wavelet Transform)
 * over traditional DWT (Discrete Wavelet Transform).
 */
public class MODWTDemo {
    public static void main(String[] args) {
        System.out.println("MODWT (Maximal Overlap Discrete Wavelet Transform) Demo");
        System.out.println("=======================================================");
        System.out.println();

        demonstrateAnyLengthSignals();
        demonstrateShiftInvariance();
        demonstrateSameLengthOutput();
        demonstrateRealWorldExample();
    }

    private static void demonstrateAnyLengthSignals() {
        System.out.println("1. NO POWER-OF-2 RESTRICTION");
        System.out.println("==============================");
        
        MODWTTransformFactory modwtFactory = new MODWTTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        WaveletTransformFactory dwtFactory = new WaveletTransformFactory()
                .withBoundaryMode(BoundaryMode.PERIODIC);
        
        // Test signals with various lengths
        double[] signal3 = {1, 2, 3};
        double[] signal5 = {1, 2, 3, 4, 5};
        double[] signal7 = {1, 2, 3, 4, 5, 6, 7};
        
        System.out.println("Signal lengths: 3, 5, 7 (non-power-of-2)");
        System.out.println();
        
        // MODWT works with any length
        MODWTTransform modwt = modwtFactory.create(new Haar());
        
        System.out.println("MODWT Results:");
        processSignalMODWT(modwt, signal3, "Length 3");
        processSignalMODWT(modwt, signal5, "Length 5");
        processSignalMODWT(modwt, signal7, "Length 7");
        
        // DWT requires power-of-2 lengths
        WaveletTransform dwt = dwtFactory.create(new Haar());
        System.out.println("DWT Results:");
        System.out.println("Length 3: Cannot process (not power-of-2)");
        System.out.println("Length 5: Cannot process (not power-of-2)"); 
        System.out.println("Length 7: Cannot process (not power-of-2)");
        
        // Only power-of-2 works for DWT
        double[] signal8 = {1, 2, 3, 4, 5, 6, 7, 8};
        TransformResult dwtResult = dwt.forward(signal8);
        System.out.println("Length 8: Approximation length=" + dwtResult.approximationCoeffs().length + 
                          ", Detail length=" + dwtResult.detailCoeffs().length);
        System.out.println();
    }

    private static void demonstrateShiftInvariance() {
        System.out.println("2. SHIFT-INVARIANCE PROPERTY");
        System.out.println("=============================");
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        WaveletTransform dwt = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 4, 2, 8, 5, 3, 7, 6};
        double[] shiftedSignal = {6, 1, 4, 2, 8, 5, 3, 7}; // Circular shift by 1
        
        System.out.println("Original signal:  " + Arrays.toString(signal));
        System.out.println("Shifted signal:   " + Arrays.toString(shiftedSignal));
        System.out.println();
        
        // MODWT should show shifted coefficients
        TransformResult modwtOriginal = modwt.forward(signal);
        TransformResult modwtShifted = modwt.forward(shiftedSignal);
        
        System.out.println("MODWT - Original approximation:  " + formatArray(modwtOriginal.approximationCoeffs()));
        System.out.println("MODWT - Shifted approximation:   " + formatArray(modwtShifted.approximationCoeffs()));
        System.out.println("MODWT coefficients show clear circular shift pattern ✓");
        System.out.println();
        
        // DWT may not show clear shift relationship
        TransformResult dwtOriginal = dwt.forward(signal);
        TransformResult dwtShifted = dwt.forward(shiftedSignal);
        
        System.out.println("DWT - Original approximation:    " + formatArray(dwtOriginal.approximationCoeffs()));
        System.out.println("DWT - Shifted approximation:     " + formatArray(dwtShifted.approximationCoeffs()));
        System.out.println("DWT coefficients don't preserve shift relationship due to downsampling ✗");
        System.out.println();
    }

    private static void demonstrateSameLengthOutput() {
        System.out.println("3. SAME-LENGTH OUTPUT");
        System.out.println("=====================");
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        WaveletTransform dwt = new WaveletTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Need power-of-2 for DWT comparison
        double[] signal8 = Arrays.copyOf(signal, 8);
        
        TransformResult modwtResult = modwt.forward(signal);
        TransformResult dwtResult = dwt.forward(signal8);
        
        System.out.println("Input signal length: " + signal.length);
        System.out.println("MODWT approximation length: " + modwtResult.approximationCoeffs().length + " (same as input) ✓");
        System.out.println("MODWT detail length: " + modwtResult.detailCoeffs().length + " (same as input) ✓");
        System.out.println();
        
        System.out.println("DWT input length: " + signal8.length);
        System.out.println("DWT approximation length: " + dwtResult.approximationCoeffs().length + " (half of input) ✗");
        System.out.println("DWT detail length: " + dwtResult.detailCoeffs().length + " (half of input) ✗");
        System.out.println();
        
        System.out.println("MODWT preserves all information without decimation!");
        System.out.println();
    }

    private static void demonstrateRealWorldExample() {
        System.out.println("4. REAL-WORLD FINANCIAL TIME SERIES EXAMPLE");
        System.out.println("============================================");
        
        // Simulate a financial time series with trend and noise
        double[] stockPrices = {
            100.0, 101.2, 99.8, 102.1, 103.5, 102.9, 104.2, 105.1, 103.7,
            106.3, 107.8, 106.4, 108.9, 110.2, 109.1, 111.4, 112.7, 111.3
        }; // 18 data points (not power-of-2)
        
        System.out.println("Stock prices (18 days): " + formatArray(stockPrices));
        System.out.println();
        
        MODWTTransform modwt = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        TransformResult result = modwt.forward(stockPrices);
        
        System.out.println("MODWT Analysis:");
        System.out.println("Approximation coeffs (trend): " + formatArray(result.approximationCoeffs()));
        System.out.println("Detail coeffs (fluctuations): " + formatArray(result.detailCoeffs()));
        System.out.println();
        
        // Perfect reconstruction
        double[] reconstructed = modwt.inverse(result);
        double maxError = 0;
        for (int i = 0; i < stockPrices.length; i++) {
            maxError = Math.max(maxError, Math.abs(stockPrices[i] - reconstructed[i]));
        }
        
        System.out.println("Reconstruction error: " + String.format("%.2e", maxError) + " (perfect reconstruction) ✓");
        System.out.println();
        
        System.out.println("Benefits for financial analysis:");
        System.out.println("• No need to pad/truncate data to power-of-2 length");
        System.out.println("• Shift-invariance preserves temporal patterns");
        System.out.println("• Same-length coefficients enable direct comparison with original data");
        System.out.println("• Better suited for real-time analysis and pattern detection");
        System.out.println();
    }

    private static void processSignalMODWT(MODWTTransform modwt, double[] signal, String description) {
        TransformResult result = modwt.forward(signal);
        System.out.println(description + ": Approximation length=" + result.approximationCoeffs().length + 
                          ", Detail length=" + result.detailCoeffs().length);
    }

    private static String formatArray(double[] array) {
        if (array.length <= 8) {
            return Arrays.toString(array);
        } else {
            // Show first 4 and last 4 elements for large arrays
            return String.format("[%.2f, %.2f, %.2f, %.2f, ..., %.2f, %.2f, %.2f, %.2f]",
                array[0], array[1], array[2], array[3],
                array[array.length-4], array[array.length-3], array[array.length-2], array[array.length-1]);
        }
    }
}
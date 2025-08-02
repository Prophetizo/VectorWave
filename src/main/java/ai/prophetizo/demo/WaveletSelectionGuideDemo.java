package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.api.*;

import java.util.*;

/**
 * Interactive guide for selecting the appropriate wavelet for different applications.
 *
 * <p>This demo helps users understand:
 * <ul>
 *   <li>Characteristics of different wavelet families</li>
 *   <li>How to match wavelets to signal properties</li>
 *   <li>Trade-offs between different wavelets</li>
 *   <li>Visual comparison of wavelet behaviors</li>
 * </ul>
 */
public class WaveletSelectionGuideDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Wavelet Selection Guide ===\n");

        // Demo 1: Wavelet characteristics overview
        demonstrateWaveletCharacteristics();

        // Demo 2: Signal type matching
        demonstrateSignalTypeMatching();

        // Demo 3: Visual wavelet comparison
        demonstrateVisualComparison();

        // Demo 4: Application-specific recommendations
        demonstrateApplicationRecommendations();

        // Demo 5: Performance vs accuracy trade-offs
        demonstrateTradeoffs();
    }

    private static void demonstrateWaveletCharacteristics() {
        System.out.println("1. Wavelet Family Characteristics");
        System.out.println("---------------------------------\n");

        System.out.println("HAAR WAVELET");
        System.out.println("  Properties: Discontinuous, compact support");
        System.out.println("  Best for: Sharp transitions, edge detection");
        System.out.println("  Advantages: Fastest computation, simple");
        System.out.println("  Disadvantages: Poor frequency localization");
        System.out.println("  Vanishing moments: 1");
        printWaveletCoefficients(new Haar());

        System.out.println("\nDAUBECHIES WAVELETS (DB2, DB4, ...)");
        System.out.println("  Properties: Smooth, compact support");
        System.out.println("  Best for: General purpose, smooth signals");
        System.out.println("  Advantages: Good time-frequency localization");
        System.out.println("  Disadvantages: Slightly asymmetric");
        System.out.println("  Vanishing moments: N (for DBN)");
        printWaveletCoefficients(Daubechies.DB4);

        System.out.println("\nSYMLET WAVELETS (SYM2, SYM3, ...)");
        System.out.println("  Properties: Nearly symmetric, smooth");
        System.out.println("  Best for: Signal/image processing");
        System.out.println("  Advantages: Better symmetry than Daubechies");
        System.out.println("  Disadvantages: Slightly longer filters");
        System.out.println("  Vanishing moments: N (for SYMN)");
        printWaveletCoefficients(Symlet.SYM3);

        System.out.println("\nCOIFLET WAVELETS (COIF1, COIF2, ...)");
        System.out.println("  Properties: Near-symmetric, smooth");
        System.out.println("  Best for: High-precision applications");
        System.out.println("  Advantages: Vanishing moments for scaling function too");
        System.out.println("  Disadvantages: Longer computation time");
        System.out.println("  Vanishing moments: 2N (for COIFN)");
        printWaveletCoefficients(Coiflet.COIF1);

        System.out.println("\nBIORTHOGONAL WAVELETS");
        System.out.println("  Properties: Symmetric, linear phase");
        System.out.println("  Best for: Image compression (JPEG2000)");
        System.out.println("  Advantages: Perfect reconstruction, symmetric");
        System.out.println("  Disadvantages: Different analysis/synthesis filters");
        printBiorthogonalCoefficients(BiorthogonalSpline.BIOR1_3);

        System.out.println("\nMORLET WAVELET (Continuous)");
        System.out.println("  Properties: Complex-valued, Gaussian envelope");
        System.out.println("  Best for: Time-frequency analysis");
        System.out.println("  Advantages: Excellent frequency resolution");
        System.out.println("  Disadvantages: No compact support");
        System.out.println("  Note: Discretized for DWT use\n");
    }

    private static void demonstrateSignalTypeMatching() {
        System.out.println("2. Matching Wavelets to Signal Types");
        System.out.println("------------------------------------\n");

        // Test different signal types
        Map<String, double[]> testSignals = createTestSignals();

        for (Map.Entry<String, double[]> entry : testSignals.entrySet()) {
            String signalType = entry.getKey();
            double[] signal = entry.getValue();

            System.out.println("Signal Type: " + signalType);
            System.out.println("Recommended wavelets:");

            Map<Wavelet, Double> scores = evaluateWavelets(signal);
            List<Map.Entry<Wavelet, Double>> sorted = new ArrayList<>(scores.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (int i = 0; i < 3 && i < sorted.size(); i++) {
                Map.Entry<Wavelet, Double> e = sorted.get(i);
                System.out.printf("  %d. %-6s (score: %.3f)\n",
                        i + 1, e.getKey().name(), e.getValue());
            }
            System.out.println();
        }
    }

    private static void demonstrateVisualComparison() {
        System.out.println("3. Visual Wavelet Comparison");
        System.out.println("----------------------------\n");

        // Create test signal with multiple features
        double[] testSignal = createComplexTestSignal(128);

        System.out.println("Analyzing complex test signal with different wavelets:");
        System.out.println("(Signal contains: trend + oscillation + spikes + noise)\n");

        Wavelet[] wavelets = {
                new Haar(),
                Daubechies.DB2,
                Daubechies.DB4,
                Symlet.SYM4,
                Coiflet.COIF1
        };

        for (Wavelet wavelet : wavelets) {
            analyzeSignalWithWavelet(testSignal, wavelet);
        }
    }

    private static void demonstrateApplicationRecommendations() {
        System.out.println("4. Application-Specific Recommendations");
        System.out.println("---------------------------------------\n");

        System.out.println("FINANCIAL TIME SERIES ANALYSIS");
        System.out.println("  Primary: Daubechies DB4");
        System.out.println("  Alternative: Symlet SYM4");
        System.out.println("  Reason: Good balance of smoothness and localization");
        System.out.println("  Use case: Volatility estimation, trend extraction\n");

        System.out.println("AUDIO SIGNAL PROCESSING");
        System.out.println("  Primary: Daubechies DB6-DB10");
        System.out.println("  Alternative: Symlet SYM8");
        System.out.println("  Reason: Better frequency resolution for complex signals");
        System.out.println("  Use case: Compression, denoising, feature extraction\n");

        System.out.println("IMAGE COMPRESSION");
        System.out.println("  Primary: Biorthogonal BIOR1.3, BIOR2.2");
        System.out.println("  Alternative: CDF 9/7 (if available)");
        System.out.println("  Reason: Symmetric filters, linear phase");
        System.out.println("  Use case: JPEG2000, medical imaging\n");

        System.out.println("EDGE DETECTION");
        System.out.println("  Primary: Haar");
        System.out.println("  Alternative: Daubechies DB2");
        System.out.println("  Reason: Sharp transitions, simple structure");
        System.out.println("  Use case: Feature detection, segmentation\n");

        System.out.println("ECG/BIOMEDICAL SIGNALS");
        System.out.println("  Primary: Symlet SYM4-SYM8");
        System.out.println("  Alternative: Coiflet COIF1-COIF2");
        System.out.println("  Reason: Near-symmetry, smooth reconstruction");
        System.out.println("  Use case: QRS detection, artifact removal\n");

        System.out.println("REAL-TIME PROCESSING");
        System.out.println("  Primary: Haar");
        System.out.println("  Alternative: Daubechies DB2");
        System.out.println("  Reason: Minimal computational complexity");
        System.out.println("  Use case: Streaming analytics, IoT sensors\n");
    }

    private static void demonstrateTradeoffs() {
        System.out.println("5. Performance vs Accuracy Trade-offs");
        System.out.println("-------------------------------------\n");

        // Create signals for testing
        double[] smoothSignal = createSmoothSignal(256);
        double[] sharpSignal = createSharpSignal(256);
        double[] noisySignal = createNoisySignal(256);

        Wavelet[] wavelets = {
                new Haar(),
                Daubechies.DB2,
                Daubechies.DB4,
                Symlet.SYM4,
                Coiflet.COIF1
        };

        System.out.println("Wavelet | Filter | Compute | Smooth | Sharp | Noisy");
        System.out.println("        | Length | Time(µs)| Error  | Error | SNR");
        System.out.println("--------|--------|---------|--------|-------|------");

        for (Wavelet wavelet : wavelets) {
            int filterLength = wavelet.lowPassDecomposition().length;

            // Measure computation time
            long computeTime = measureComputeTime(wavelet, smoothSignal);

            // Measure reconstruction error
            double smoothError = measureReconstructionError(wavelet, smoothSignal);
            double sharpError = measureReconstructionError(wavelet, sharpSignal);
            double noisySNR = measureDenoisingSNR(wavelet, noisySignal);

            System.out.printf("%-7s | %6d | %7d | %.2e | %.2e | %.1f\n",
                    wavelet.name(), filterLength, computeTime,
                    smoothError, sharpError, noisySNR);
        }

        System.out.println("\nKey insights:");
        System.out.println("- Shorter filters = faster computation");
        System.out.println("- Longer filters = better frequency localization");
        System.out.println("- Haar excels at sharp edges but poor for smooth signals");
        System.out.println("- Higher-order wavelets better for denoising\n");

        // Decision flowchart
        printDecisionFlowchart();
    }

    // Helper methods

    private static void printWaveletCoefficients(OrthogonalWavelet wavelet) {
        double[] lowPass = wavelet.lowPassDecomposition();
        System.out.printf("  Filter length: %d\n", lowPass.length);
        System.out.print("  Low-pass filter: [");
        for (int i = 0; i < Math.min(4, lowPass.length); i++) {
            System.out.printf("%.4f", lowPass[i]);
            if (i < Math.min(4, lowPass.length) - 1) System.out.print(", ");
        }
        if (lowPass.length > 4) System.out.print(", ...");
        System.out.println("]");
    }

    private static void printBiorthogonalCoefficients(BiorthogonalWavelet wavelet) {
        double[] decLow = wavelet.lowPassDecomposition();
        double[] recLow = wavelet.lowPassReconstruction();
        System.out.printf("  Decomposition filter length: %d\n", decLow.length);
        System.out.printf("  Reconstruction filter length: %d\n", recLow.length);
    }

    private static Map<String, double[]> createTestSignals() {
        Map<String, double[]> signals = new LinkedHashMap<>();

        // Smooth signal
        double[] smooth = new double[64];
        for (int i = 0; i < smooth.length; i++) {
            smooth[i] = Math.sin(2 * Math.PI * i / smooth.length);
        }
        signals.put("Smooth Sinusoid", smooth);

        // Step function
        double[] step = new double[64];
        Arrays.fill(step, 0, 32, 0.0);
        Arrays.fill(step, 32, 64, 1.0);
        signals.put("Step Function", step);

        // Spiky signal
        double[] spiky = new double[64];
        spiky[16] = 1.0;
        spiky[32] = -1.0;
        spiky[48] = 0.5;
        signals.put("Sparse Spikes", spiky);

        // Polynomial trend
        double[] poly = new double[64];
        for (int i = 0; i < poly.length; i++) {
            double x = (double) i / poly.length;
            poly[i] = x * x - 0.5 * x + 0.1;
        }
        signals.put("Polynomial Trend", poly);

        return signals;
    }

    private static Map<Wavelet, Double> evaluateWavelets(double[] signal) {
        Map<Wavelet, Double> scores = new HashMap<>();

        Wavelet[] candidates = {
                new Haar(),
                Daubechies.DB2,
                Daubechies.DB4,
                Symlet.SYM3,
                Coiflet.COIF1
        };

        for (Wavelet wavelet : candidates) {
            double score = calculateWaveletScore(wavelet, signal);
            scores.put(wavelet, score);
        }

        return scores;
    }

    private static double calculateWaveletScore(Wavelet wavelet, double[] signal) {
        try {
            WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
            TransformResult result = transform.forward(signal);

            // Score based on energy compaction
            double[] approx = result.approximationCoeffs();
            double[] detail = result.detailCoeffs();

            double approxEnergy = 0, detailEnergy = 0;
            for (double a : approx) approxEnergy += a * a;
            for (double d : detail) detailEnergy += d * d;

            // Also consider sparsity of detail coefficients
            int sparseCount = 0;
            for (double d : detail) {
                if (Math.abs(d) < 0.01) sparseCount++;
            }
            double sparsity = (double) sparseCount / detail.length;

            // Combined score (higher is better)
            double energyRatio = approxEnergy / (approxEnergy + detailEnergy + 1e-10);
            return 0.7 * energyRatio + 0.3 * sparsity;

        } catch (Exception e) {
            return 0.0;
        }
    }

    private static double[] createComplexTestSignal(int length) {
        double[] signal = new double[length];
        Random rng = new Random(42);

        for (int i = 0; i < length; i++) {
            // Trend
            signal[i] = 0.01 * i;

            // Oscillation
            signal[i] += 0.5 * Math.sin(2 * Math.PI * i / 16);

            // Spikes
            if (i % 32 == 16) signal[i] += 2.0;

            // Noise
            signal[i] += 0.1 * rng.nextGaussian();
        }

        return signal;
    }

    private static void analyzeSignalWithWavelet(double[] signal, Wavelet wavelet) {
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(signal);

        // Calculate metrics
        double approxEnergy = calculateEnergy(result.approximationCoeffs());
        double detailEnergy = calculateEnergy(result.detailCoeffs());
        double totalEnergy = approxEnergy + detailEnergy;

        int sparseDetails = countSparse(result.detailCoeffs(), 0.01);

        System.out.printf("%-6s: Energy distribution: %.1f%% approx, %.1f%% detail | " +
                        "Sparse details: %d/%d\n",
                wavelet.name(),
                100 * approxEnergy / totalEnergy,
                100 * detailEnergy / totalEnergy,
                sparseDetails,
                result.detailCoeffs().length);
    }

    private static double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) energy += c * c;
        return energy;
    }

    private static int countSparse(double[] coeffs, double threshold) {
        int count = 0;
        for (double c : coeffs) {
            if (Math.abs(c) < threshold) count++;
        }
        return count;
    }

    private static double[] createSmoothSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) +
                    0.3 * Math.sin(2 * Math.PI * i / 16);
        }
        return signal;
    }

    private static double[] createSharpSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            if ((i / 32) % 2 == 0) {
                signal[i] = 1.0;
            } else {
                signal[i] = -1.0;
            }
        }
        return signal;
    }

    private static double[] createNoisySignal(int length) {
        Random rng = new Random(42);
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) +
                    0.3 * rng.nextGaussian();
        }
        return signal;
    }

    private static long measureComputeTime(Wavelet wavelet, double[] signal) {
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);

        // Warmup
        for (int i = 0; i < 100; i++) {
            transform.forward(signal);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            transform.forward(signal);
        }
        return (System.nanoTime() - start) / 1000000; // Convert to microseconds
    }

    private static double measureReconstructionError(Wavelet wavelet, double[] signal) {
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);

        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
        }
        return maxError;
    }

    private static double measureDenoisingSNR(Wavelet wavelet, double[] noisySignal) {
        // Simple soft thresholding
        WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
        TransformResult result = transform.forward(noisySignal);

        double[] detail = result.detailCoeffs();
        double threshold = 0.2;
        for (int i = 0; i < detail.length; i++) {
            if (Math.abs(detail[i]) < threshold) {
                detail[i] = 0;
            }
        }

        double[] denoised = transform.inverse(TransformResult.create(
                result.approximationCoeffs(), detail));

        // Calculate SNR improvement
        double noiseEnergy = 0;
        double signalEnergy = 0;
        for (int i = 0; i < noisySignal.length; i++) {
            double clean = Math.sin(2 * Math.PI * i / 32); // Known clean signal
            noiseEnergy += Math.pow(denoised[i] - clean, 2);
            signalEnergy += clean * clean;
        }

        return 10 * Math.log10(signalEnergy / noiseEnergy);
    }

    private static void printDecisionFlowchart() {
        System.out.println("\nWAVELET SELECTION DECISION FLOWCHART");
        System.out.println("====================================");
        System.out.println();
        System.out.println("Is real-time processing critical?");
        System.out.println("  YES → Use Haar or DB2");
        System.out.println("  NO  → Continue...");
        System.out.println();
        System.out.println("Does signal have sharp discontinuities?");
        System.out.println("  YES → Use Haar or low-order Daubechies");
        System.out.println("  NO  → Continue...");
        System.out.println();
        System.out.println("Is symmetry important (e.g., image processing)?");
        System.out.println("  YES → Use Biorthogonal or Symlets");
        System.out.println("  NO  → Continue...");
        System.out.println();
        System.out.println("Need high vanishing moments (smooth signals)?");
        System.out.println("  YES → Use Coiflets or high-order Daubechies");
        System.out.println("  NO  → Use DB4 or SYM4 (good general purpose)");
        System.out.println();
    }
}
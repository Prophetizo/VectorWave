package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;

import java.util.Arrays;

/**
 * Demonstrates the effects of different boundary handling modes with MODWT.
 *
 * <p>This demo shows how boundary modes affect:
 * <ul>
 *   <li>Transform coefficients near signal edges</li>
 *   <li>Reconstruction accuracy</li>
 *   <li>Artifacts and edge effects</li>
 *   <li>Choosing the right mode for your application</li>
 *   <li>MODWT supports PERIODIC, ZERO_PADDING, and SYMMETRIC modes</li>
 * </ul>
 */
public class BoundaryModesDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Boundary Modes Demo (MODWT) ===\n");

        // Demo 1: Visual comparison of boundary modes
        demonstrateBoundaryEffects();

        // Demo 2: Edge artifact analysis
        demonstrateEdgeArtifacts();

        // Demo 3: Impact on reconstruction
        demonstrateReconstructionAccuracy();

        // Demo 4: Boundary mode selection guide
        demonstrateModeSelection();

        // Demo 5: Real-world examples
        demonstrateRealWorldScenarios();
    }

    private static void demonstrateBoundaryEffects() {
        System.out.println("1. Boundary Mode Effects");
        System.out.println("------------------------");

        // Create a signal with a clear edge pattern
        double[] signal = createEdgeSignal(16);
        System.out.println("Original signal: " + Arrays.toString(signal));
        System.out.println("  (Notice the jump from 1.0 to -1.0 at the midpoint)\n");

        // Test each boundary mode
        BoundaryMode[] modes = {
                BoundaryMode.PERIODIC,
                BoundaryMode.ZERO_PADDING,
                BoundaryMode.SYMMETRIC
        };

        for (BoundaryMode mode : modes) {
            System.out.println(mode + " mode:");

            MODWTTransform transform = new MODWTTransform(Daubechies.DB2, mode);
            MODWTResult result = transform.forward(signal);

            // Show first and last few coefficients (where boundary effects appear)
            double[] approx = result.approximationCoeffs();
            double[] detail = result.detailCoeffs();

            System.out.printf("  First 3 approx: [%.3f, %.3f, %.3f]\n",
                    approx[0], approx[1], approx[2]);
            System.out.printf("  Last 3 approx:  [%.3f, %.3f, %.3f]\n",
                    approx[approx.length - 3], approx[approx.length - 2], approx[approx.length - 1]);
            System.out.printf("  First 3 detail: [%.3f, %.3f, %.3f]\n",
                    detail[0], detail[1], detail[2]);
            System.out.printf("  Last 3 detail:  [%.3f, %.3f, %.3f]\n\n",
                    detail[detail.length - 3], detail[detail.length - 2], detail[detail.length - 1]);
        }
    }

    private static void demonstrateEdgeArtifacts() {
        System.out.println("2. Edge Artifact Analysis");
        System.out.println("-------------------------");

        // Create a smooth signal
        int length = 64;
        double[] smoothSignal = new double[length];
        for (int i = 0; i < length; i++) {
            smoothSignal[i] = Math.sin(2 * Math.PI * i / length);
        }

        System.out.println("Testing with smooth sine wave...\n");

        BoundaryMode[] modes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING, BoundaryMode.SYMMETRIC};

        for (BoundaryMode mode : modes) {
            MODWTTransform transform = new MODWTTransform(Daubechies.DB4, mode);

            // Forward and inverse transform
            MODWTResult result = transform.forward(smoothSignal);
            double[] reconstructed = transform.inverse(result);

            // Measure edge artifacts
            double edgeError = 0;
            double middleError = 0;
            int edgeSize = 8;

            for (int i = 0; i < edgeSize; i++) {
                // Beginning edge
                edgeError += Math.abs(smoothSignal[i] - reconstructed[i]);
                // Ending edge
                edgeError += Math.abs(smoothSignal[length - 1 - i] - reconstructed[length - 1 - i]);
            }

            for (int i = edgeSize; i < length - edgeSize; i++) {
                middleError += Math.abs(smoothSignal[i] - reconstructed[i]);
            }

            edgeError /= (2 * edgeSize);
            middleError /= (length - 2 * edgeSize);

            System.out.printf("%s mode:\n", mode);
            System.out.printf("  Average edge error:   %.2e\n", edgeError);
            System.out.printf("  Average middle error: %.2e\n", middleError);
            System.out.printf("  Edge/Middle ratio:    %.2f\n\n",
                    edgeError / (middleError + 1e-15));
        }

        System.out.println("Note: PERIODIC mode shows no edge artifacts for periodic signals\n");
    }

    private static void demonstrateReconstructionAccuracy() {
        System.out.println("3. Reconstruction Accuracy");
        System.out.println("--------------------------");

        // Test different signal types
        int length = 32;
        double[][] testSignals = {
                createConstantSignal(length, 5.0),
                createLinearSignal(length),
                createStepSignal(length),
                createNoiseSignal(length, 42)
        };
        String[] signalNames = {"Constant", "Linear", "Step", "Random"};

        System.out.println("Maximum reconstruction error for different signals:\n");
        System.out.println("Signal Type  | PERIODIC    | ZERO_PADDING | SYMMETRIC");
        System.out.println("-------------|-------------|--------------|-----------");

        for (int s = 0; s < testSignals.length; s++) {
            System.out.printf("%-12s |", signalNames[s]);

            for (BoundaryMode mode : new BoundaryMode[]{BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING, BoundaryMode.SYMMETRIC}) {
                MODWTTransform transform = new MODWTTransform(new Haar(), mode);

                MODWTResult result = transform.forward(testSignals[s]);
                double[] reconstructed = transform.inverse(result);

                double maxError = 0;
                for (int i = 0; i < length; i++) {
                    maxError = Math.max(maxError, Math.abs(testSignals[s][i] - reconstructed[i]));
                }

                System.out.printf(" %.2e |", maxError);
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void demonstrateModeSelection() {
        System.out.println("4. Boundary Mode Selection Guide");
        System.out.println("--------------------------------");

        System.out.println("When to use each boundary mode:\n");

        System.out.println("PERIODIC (Circular) Boundary Mode:");
        System.out.println("  ✓ Best for: Periodic signals (audio, vibrations)");
        System.out.println("  ✓ Preserves signal energy");
        System.out.println("  ✓ No edge artifacts for truly periodic signals");
        System.out.println("  ✗ Can create artifacts if signal is not periodic");
        System.out.println("  Example: Analyzing steady-state oscillations\n");

        System.out.println("ZERO_PADDING Boundary Mode:");
        System.out.println("  ✓ Best for: Finite-duration signals");
        System.out.println("  ✓ No assumptions about signal beyond boundaries");
        System.out.println("  ✓ Suitable for transient analysis");
        System.out.println("  ✗ Can reduce energy near boundaries");
        System.out.println("  Example: Analyzing financial time series\n");

        System.out.println("SYMMETRIC Boundary Mode:");
        System.out.println("  ✓ Best for: Smooth signals requiring minimal edge artifacts");
        System.out.println("  ✓ Mirrors signal at boundaries");
        System.out.println("  ✗ Slightly higher computational cost than PERIODIC");
        System.out.println("  Example: Image processing and smooth time series\n");

        // Demonstrate with examples
        demonstrateModeExamples();
    }

    private static void demonstrateModeExamples() {
        System.out.println("Mode Selection Examples:");
        System.out.println("-----------------------");

        // Example 1: Periodic signal
        System.out.println("\nExample 1: Analyzing a periodic heartbeat signal");
        double[] heartbeat = createHeartbeatSignal(64);
        compareModesForSignal(heartbeat, "Heartbeat");

        // Example 2: Financial returns
        System.out.println("\nExample 2: Analyzing financial returns (non-periodic)");
        double[] returns = createFinancialReturns(64);
        compareModesForSignal(returns, "Returns");
    }

    private static void demonstrateRealWorldScenarios() {
        System.out.println("\n5. Real-World Scenarios");
        System.out.println("-----------------------");

        // Scenario 1: Audio processing
        System.out.println("\nScenario 1: Audio Signal Processing");
        System.out.println("Signal: 1 second of audio at 256 Hz sample rate");

        int audioLength = 256;
        double[] audio = new double[audioLength];
        for (int i = 0; i < audioLength; i++) {
            // Mix of frequencies typical in audio
            audio[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / audioLength) + // A4 note
                    0.3 * Math.sin(2 * Math.PI * 880 * i / audioLength) + // A5 note
                    0.1 * Math.sin(2 * Math.PI * 220 * i / audioLength);  // A3 note
        }

        System.out.println("Recommendation: Use PERIODIC mode");
        System.out.println("Reason: Audio is often analyzed in blocks with overlap\n");

        // Scenario 2: Image processing (1D slice)
        System.out.println("Scenario 2: Image Row Processing");
        System.out.println("Signal: One row of image pixels");

        double[] imageRow = new double[64];
        // Simulate image with edges
        for (int i = 0; i < 20; i++) imageRow[i] = 50;    // Dark region
        for (int i = 20; i < 44; i++) imageRow[i] = 200;  // Bright region
        for (int i = 44; i < 64; i++) imageRow[i] = 100;  // Medium region

        System.out.println("Recommendation: Use SYMMETRIC mode");
        System.out.println("Reason: Mirroring preserves edge continuity\n");

        // Scenario 3: Sensor data
        System.out.println("Scenario 3: IoT Sensor Data");
        System.out.println("Signal: Temperature readings over time");

        double[] temperature = new double[128];
        double baseTemp = 20.0;
        for (int i = 0; i < temperature.length; i++) {
            // Daily temperature cycle with noise
            temperature[i] = baseTemp +
                    5 * Math.sin(2 * Math.PI * i / temperature.length) +
                    0.5 * (Math.random() - 0.5);
        }

        System.out.println("Recommendation: Depends on analysis goal");
        System.out.println("- For daily pattern analysis: PERIODIC");
        System.out.println("- For anomaly detection: ZERO_PADDING");
        System.out.println("- For smoothing and edge preservation: SYMMETRIC");
    }

    // Helper methods

    private static double[] createEdgeSignal(int length) {
        double[] signal = new double[length];
        Arrays.fill(signal, 0, length / 2, 1.0);
        Arrays.fill(signal, length / 2, length, -1.0);
        return signal;
    }

    private static double[] createConstantSignal(int length, double value) {
        double[] signal = new double[length];
        Arrays.fill(signal, value);
        return signal;
    }

    private static double[] createLinearSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = i;
        }
        return signal;
    }

    private static double[] createStepSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = (i < length / 2) ? 0.0 : 1.0;
        }
        return signal;
    }

    private static double[] createNoiseSignal(int length, long seed) {
        double[] signal = new double[length];
        java.util.Random rng = new java.util.Random(seed);
        for (int i = 0; i < length; i++) {
            signal[i] = rng.nextGaussian();
        }
        return signal;
    }

    private static double[] createHeartbeatSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            // Simulate QRS complex
            signal[i] = Math.exp(-50 * (t - 0.3) * (t - 0.3)) +
                    0.3 * Math.exp(-100 * (t - 0.7) * (t - 0.7));
        }
        return signal;
    }

    private static double[] createFinancialReturns(int length) {
        double[] returns = new double[length];
        java.util.Random rng = new java.util.Random(123);

        double volatility = 0.02;
        for (int i = 0; i < length; i++) {
            returns[i] = volatility * rng.nextGaussian();
            // Volatility clustering
            volatility = 0.98 * volatility + 0.02 * 0.02 +
                    0.1 * Math.abs(returns[i]);
        }
        return returns;
    }

    private static void compareModesForSignal(double[] signal, String name) {
        BoundaryMode[] modes = {BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING, BoundaryMode.SYMMETRIC};

        System.out.printf("\n%s signal analysis:\n", name);
        for (BoundaryMode mode : modes) {
            MODWTTransform transform = new MODWTTransform(Daubechies.DB2, mode);
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);

            // Calculate reconstruction metrics
            double mse = 0;
            double maxError = 0;
            for (int i = 0; i < signal.length; i++) {
                double error = Math.abs(signal[i] - reconstructed[i]);
                mse += error * error;
                maxError = Math.max(maxError, error);
            }
            mse /= signal.length;

            System.out.printf("  %s: MSE=%.2e, MaxErr=%.2e\n",
                    mode, mse, maxError);
        }
    }
}
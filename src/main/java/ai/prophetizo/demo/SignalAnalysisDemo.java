package ai.prophetizo.demo;

import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.api.*;

import java.util.*;

/**
 * Demonstrates signal analysis capabilities using MODWT wavelet transforms.
 *
 * <p>This demo covers:
 * <ul>
 *   <li>Time-frequency analysis with shift-invariance</li>
 *   <li>Feature extraction from arbitrary-length signals</li>
 *   <li>Anomaly detection using MODWT coefficients</li>
 *   <li>Signal classification with wavelet features</li>
 *   <li>Pattern recognition across scales</li>
 * </ul>
 * 
 * @since 3.0.0
 */
public class SignalAnalysisDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave MODWT Signal Analysis Demo ===\n");

        // Demo 1: Time-frequency analysis
        demonstrateTimeFrequencyAnalysis();

        // Demo 2: Feature extraction
        demonstrateFeatureExtraction();

        // Demo 3: Anomaly detection
        demonstrateAnomalyDetection();

        // Demo 4: Signal classification
        demonstrateSignalClassification();

        // Demo 5: Pattern recognition
        demonstratePatternRecognition();
    }

    private static void demonstrateTimeFrequencyAnalysis() {
        System.out.println("1. Time-Frequency Analysis with MODWT");
        System.out.println("-------------------------------------");

        // Create chirp signal (frequency increases with time)
        int length = 777; // Non-power-of-2 to showcase MODWT!
        double[] chirp = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double frequency = 5 + 20 * t; // 5 Hz to 25 Hz
            chirp[i] = Math.sin(2 * Math.PI * frequency * t * length / 100);
        }

        System.out.println("Analyzing chirp signal (frequency increases over time)...");
        System.out.println("Signal length: " + length + " (non-power-of-2!)\n");

        // Multi-level MODWT decomposition for time-frequency analysis
        MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        int levels = 5;
        MultiLevelMODWTResult result = mwt.forward(chirp, levels);

        System.out.println("Time-Frequency content by scale (MODWT preserves time alignment):");
        System.out.println("Level | Scale | Frequency Band | Energy | Peak Location | Shift-Invariant");
        System.out.println("------|-------|----------------|--------|---------------|----------------");

        double samplingRate = 100; // Hz
        for (int level = 1; level <= levels; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            double energy = calculateEnergy(details);
            int scale = 1 << level;

            // Approximate frequency band
            double highFreq = samplingRate / (2 * scale);
            double lowFreq = samplingRate / (2 * scale * 2);

            // Find temporal location of maximum energy
            int maxIndex = findMaxEnergyWindow(details, 10);
            double maxTime = (double) maxIndex / details.length;

            System.out.printf("%5d | %5d | %4.1f-%4.1f Hz | %6.2f | t=%.2f (%.0f%%) | YES\n",
                    level, scale, lowFreq, highFreq, energy, maxTime, maxTime * 100);
        }

        System.out.println("\nMODWT Advantage: Time alignment preserved across all scales!");
        System.out.println("DWT would decimate coefficients, losing precise time information.\n");
    }

    private static void demonstrateFeatureExtraction() {
        System.out.println("2. Feature Extraction with MODWT");
        System.out.println("---------------------------------");

        // Create different test signals of varying lengths
        Map<String, double[]> signals = createFeatureTestSignals();

        System.out.println("Extracting wavelet features from different signal types:");
        System.out.println("(Note: MODWT handles arbitrary signal lengths)\n");

        MODWTTransform transform = new MODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        System.out.println("Signal Type    | Length | Mean | Std  | Energy | Entropy | Zero-X | Dominant");
        System.out.println("---------------|--------|------|------|--------|---------|--------|----------");

        for (Map.Entry<String, double[]> entry : signals.entrySet()) {
            String name = entry.getKey();
            double[] signal = entry.getValue();

            // Extract features using MODWT
            WaveletFeatures features = extractMODWTFeatures(transform, signal);

            System.out.printf("%-14s | %6d | %4.2f | %4.2f | %6.2f | %7.3f | %6d | %s\n",
                    name,
                    signal.length,
                    features.mean,
                    features.stdDev,
                    features.energy,
                    features.entropy,
                    features.zeroCrossings,
                    features.dominantScale);
        }

        System.out.println("\nMODWT advantages for feature extraction:");
        System.out.println("- Works with any signal length (no padding needed)");
        System.out.println("- Shift-invariant features (robust to signal shifts)");
        System.out.println("- Better time-frequency localization\n");
    }

    private static void demonstrateAnomalyDetection() {
        System.out.println("3. Anomaly Detection with MODWT");
        System.out.println("--------------------------------");

        // Create signal with anomalies
        int length = 333; // Non-power-of-2
        double[] normalSignal = new double[length];
        Random rng = new Random(42);

        // Normal behavior with injected anomalies
        for (int i = 0; i < length; i++) {
            normalSignal[i] = Math.sin(2 * Math.PI * i / 32) +
                    0.1 * rng.nextGaussian();

            // Inject anomalies
            if (i == 100) normalSignal[i] += 3.0;  // Spike anomaly
            if (i >= 200 && i <= 210) {           // Pattern anomaly
                normalSignal[i] = 0.5 * Math.sin(2 * Math.PI * i / 4);
            }
        }

        System.out.println("Detecting anomalies in signal of length " + length + "...\n");

        // Use MODWT for anomaly detection
        MODWTTransform transform = new MODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        // Sliding window analysis with MODWT
        int windowSize = 32;
        int stride = 8;
        List<AnomalyScore> anomalies = new ArrayList<>();

        for (int start = 0; start <= length - windowSize; start += stride) {
            double[] window = Arrays.copyOfRange(normalSignal, start, start + windowSize);

            MODWTResult result = transform.forward(window);
            double score = calculateMODWTAnomalyScore(result);

            anomalies.add(new AnomalyScore(start, start + windowSize, score));
        }

        // Find significant anomalies (top 10%)
        anomalies.sort((a, b) -> Double.compare(b.score, a.score));
        double threshold = anomalies.get(anomalies.size() / 10).score;

        System.out.println("Detected anomalies (score > " + String.format("%.2f", threshold) + "):");
        System.out.println("Position | Score | Type             | MODWT Advantage");
        System.out.println("---------|-------|------------------|------------------");

        for (AnomalyScore anomaly : anomalies) {
            if (anomaly.score > threshold) {
                String type = classifyAnomaly(normalSignal, anomaly.start, anomaly.end);
                System.out.printf("%3d-%3d  | %5.2f | %-16s | Shift-invariant detection\n",
                        anomaly.start, anomaly.end, anomaly.score, type);
            }
        }

        System.out.println("\nMODWT benefits for anomaly detection:");
        System.out.println("- Consistent detection regardless of anomaly position");
        System.out.println("- No edge effects from signal padding");
        System.out.println("- Better localization of transient anomalies\n");
    }

    private static void demonstrateSignalClassification() {
        System.out.println("4. Signal Classification with MODWT");
        System.out.println("------------------------------------");

        // Create different signal classes with varying lengths
        Map<String, List<double[]>> signalClasses = createSignalClasses();

        System.out.println("Training MODWT-based classifier on variable-length signals...\n");

        // Extract features for each class
        Map<String, List<WaveletFeatures>> classFeatures = new HashMap<>();
        MODWTTransform transform = new MODWTTransform(
                Symlet.SYM4, BoundaryMode.PERIODIC);

        for (Map.Entry<String, List<double[]>> entry : signalClasses.entrySet()) {
            String className = entry.getKey();
            List<WaveletFeatures> features = new ArrayList<>();

            for (double[] signal : entry.getValue()) {
                features.add(extractMODWTFeatures(transform, signal));
            }

            classFeatures.put(className, features);
        }

        // Compute class statistics
        System.out.println("Class Statistics (from variable-length training signals):");
        System.out.println("Class       | Avg Energy | Avg Entropy | MODWT Characteristic");
        System.out.println("------------|------------|-------------|---------------------");

        for (Map.Entry<String, List<WaveletFeatures>> entry : classFeatures.entrySet()) {
            String className = entry.getKey();
            List<WaveletFeatures> features = entry.getValue();

            double avgEnergy = features.stream()
                    .mapToDouble(f -> f.energy)
                    .average().orElse(0);
            double avgEntropy = features.stream()
                    .mapToDouble(f -> f.entropy)
                    .average().orElse(0);

            System.out.printf("%-11s | %10.2f | %11.3f | %s\n",
                    className, avgEnergy, avgEntropy,
                    getMODWTCharacteristic(className));
        }

        // Test classification on new signals
        System.out.println("\nClassification test on new signals (various lengths):");
        testMODWTSignalClassification(transform, classFeatures);

        System.out.println();
    }

    private static void demonstratePatternRecognition() {
        System.out.println("5. Pattern Recognition with MODWT");
        System.out.println("----------------------------------");

        // Create signal with repeating patterns
        int length = 555; // Non-power-of-2
        double[] signal = createPatternSignal(length);

        System.out.println("Detecting repeating patterns in signal of length " + length + "...\n");

        // Multi-resolution pattern analysis with MODWT
        MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC);

        MultiLevelMODWTResult result = mwt.forward(signal, 4);

        // Analyze each scale for patterns
        System.out.println("Pattern Detection Results (MODWT preserves pattern alignment):");
        System.out.println("Level | Scale | Pattern Period | Confidence | Time Alignment | Pattern Type");
        System.out.println("------|-------|----------------|------------|----------------|-------------");

        for (int level = 1; level <= 4; level++) {
            double[] coeffs = result.getDetailCoeffsAtLevel(level);
            PatternInfo pattern = detectMODWTPattern(coeffs);

            if (pattern.confidence > 0.5) {
                int scale = 1 << level;
                int actualPeriod = pattern.period; // MODWT preserves length!

                System.out.printf("%5d | %5d | %14d | %10.2f | Preserved      | %s\n",
                        level, scale, actualPeriod,
                        pattern.confidence, pattern.type);
            }
        }

        // Cross-scale correlation analysis
        System.out.println("\nCross-scale correlation (MODWT maintains alignment):");
        demonstrateMODWTCrossScaleCorrelation(result);

        System.out.println("\nMODWT advantages for pattern recognition:");
        System.out.println("- Patterns remain aligned across scales");
        System.out.println("- No phase distortion from downsampling");
        System.out.println("- Works with patterns of any period");
        System.out.println("- Better detection of non-stationary patterns\n");
    }

    // Helper methods updated for MODWT

    private static double calculateEnergy(double[] coeffs) {
        double energy = 0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }

    private static int findMaxEnergyWindow(double[] coeffs, int windowSize) {
        double maxEnergy = 0;
        int maxIndex = 0;

        for (int i = 0; i <= coeffs.length - windowSize; i++) {
            double energy = 0;
            for (int j = 0; j < windowSize; j++) {
                energy += coeffs[i + j] * coeffs[i + j];
            }
            if (energy > maxEnergy) {
                maxEnergy = energy;
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    private static Map<String, double[]> createFeatureTestSignals() {
        Map<String, double[]> signals = new LinkedHashMap<>();
        Random rng = new Random(42);

        // Different lengths to showcase MODWT flexibility
        
        // Sinusoidal (length 137 - prime number!)
        double[] sine = new double[137];
        for (int i = 0; i < sine.length; i++) {
            sine[i] = Math.sin(2 * Math.PI * i / 16);
        }
        signals.put("Sinusoidal", sine);

        // Square wave (length 200)
        double[] square = new double[200];
        for (int i = 0; i < square.length; i++) {
            square[i] = (i / 16) % 2 == 0 ? 1.0 : -1.0;
        }
        signals.put("Square", square);

        // White noise (length 99)
        double[] noise = new double[99];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = rng.nextGaussian();
        }
        signals.put("WhiteNoise", noise);

        // Impulse train (length 150)
        double[] impulse = new double[150];
        for (int i = 0; i < impulse.length; i += 16) {
            impulse[i] = 1.0;
        }
        signals.put("Impulse", impulse);

        // Chirp (length 256)
        double[] chirp = new double[256];
        for (int i = 0; i < chirp.length; i++) {
            double f = 1 + 10.0 * i / chirp.length;
            chirp[i] = Math.sin(2 * Math.PI * f * i / chirp.length);
        }
        signals.put("Chirp", chirp);

        return signals;
    }

    private static WaveletFeatures extractMODWTFeatures(MODWTTransform transform,
                                                        double[] signal) {
        MODWTResult result = transform.forward(signal);

        // Basic statistics
        double mean = 0;
        for (double s : signal) mean += s;
        mean /= signal.length;

        double variance = 0;
        for (double s : signal) {
            variance += (s - mean) * (s - mean);
        }
        double stdDev = Math.sqrt(variance / signal.length);

        // Energy
        double energy = calculateEnergy(signal);

        // Entropy of MODWT coefficients
        double entropy = calculateMODWTEntropy(result);

        // Zero crossings in detail coefficients
        int zeroCrossings = countZeroCrossings(result.detailCoeffs());

        // Dominant scale
        String dominantScale = findDominantScale(result);

        return new WaveletFeatures(mean, stdDev, energy, entropy,
                zeroCrossings, dominantScale);
    }

    private static double calculateMODWTEntropy(MODWTResult result) {
        List<Double> allCoeffs = new ArrayList<>();
        for (double c : result.approximationCoeffs()) allCoeffs.add(Math.abs(c));
        for (double c : result.detailCoeffs()) allCoeffs.add(Math.abs(c));

        double total = allCoeffs.stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return 0;

        double entropy = 0;
        for (double c : allCoeffs) {
            if (c > 0) {
                double p = c / total;
                entropy -= p * Math.log(p) / Math.log(2);
            }
        }

        return entropy;
    }

    private static int countZeroCrossings(double[] coeffs) {
        int count = 0;
        for (int i = 1; i < coeffs.length; i++) {
            if (coeffs[i - 1] * coeffs[i] < 0) {
                count++;
            }
        }
        return count;
    }

    private static String findDominantScale(MODWTResult result) {
        double approxEnergy = calculateEnergy(result.approximationCoeffs());
        double detailEnergy = calculateEnergy(result.detailCoeffs());

        return approxEnergy > detailEnergy ? "Low" : "High";
    }

    private static double calculateMODWTAnomalyScore(MODWTResult result) {
        // High-frequency energy indicates anomaly
        double detailEnergy = calculateEnergy(result.detailCoeffs());
        double approxEnergy = calculateEnergy(result.approximationCoeffs());

        // Ratio of detail to total energy
        double ratio = detailEnergy / (detailEnergy + approxEnergy + 1e-10);

        // Also consider coefficient magnitude
        double maxDetail = 0;
        for (double d : result.detailCoeffs()) {
            maxDetail = Math.max(maxDetail, Math.abs(d));
        }

        // MODWT coefficients are not decimated, so we get better localization
        return ratio * 0.7 + maxDetail * 0.3;
    }

    private static String classifyAnomaly(double[] signal, int start, int end) {
        // Simple classification based on signal characteristics
        double localMean = 0;
        double localVar = 0;

        for (int i = start; i < end && i < signal.length; i++) {
            localMean += signal[i];
        }
        localMean /= (end - start);

        for (int i = start; i < end && i < signal.length; i++) {
            localVar += (signal[i] - localMean) * (signal[i] - localMean);
        }
        localVar /= (end - start);

        if (localVar > 1.0) return "High variance";
        if (Math.abs(localMean) > 2.0) return "Level shift";
        return "Pattern change";
    }

    private static Map<String, List<double[]>> createSignalClasses() {
        Map<String, List<double[]>> classes = new HashMap<>();
        Random rng = new Random(42);

        // Create multiple examples per class with varying lengths
        int samplesPerClass = 5;

        // Periodic signals (varying lengths)
        List<double[]> periodic = new ArrayList<>();
        for (int i = 0; i < samplesPerClass; i++) {
            int length = 50 + rng.nextInt(100); // 50-150 samples
            double[] signal = new double[length];
            double freq = 2 + rng.nextDouble() * 3;
            double phase = rng.nextDouble() * 2 * Math.PI;
            for (int j = 0; j < length; j++) {
                signal[j] = Math.sin(freq * 2 * Math.PI * j / length + phase);
            }
            periodic.add(signal);
        }
        classes.put("Periodic", periodic);

        // Transient signals (varying lengths)
        List<double[]> transientSignals = new ArrayList<>();
        for (int i = 0; i < samplesPerClass; i++) {
            int length = 60 + rng.nextInt(80); // 60-140 samples
            double[] signal = new double[length];
            int position = length/4 + rng.nextInt(length/2);
            double amplitude = 1 + rng.nextDouble() * 2;
            for (int j = 0; j < length; j++) {
                signal[j] = amplitude * Math.exp(-0.5 * (j - position) * (j - position) / 4);
            }
            transientSignals.add(signal);
        }
        classes.put("Transient", transientSignals);

        // Noisy signals (varying lengths)
        List<double[]> noisy = new ArrayList<>();
        for (int i = 0; i < samplesPerClass; i++) {
            int length = 70 + rng.nextInt(60); // 70-130 samples
            double[] signal = new double[length];
            for (int j = 0; j < length; j++) {
                signal[j] = rng.nextGaussian() * (0.5 + 0.5 * rng.nextDouble());
            }
            noisy.add(signal);
        }
        classes.put("Noisy", noisy);

        return classes;
    }

    private static String getMODWTCharacteristic(String className) {
        switch (className) {
            case "Periodic":
                return "Shift-invariant oscillations";
            case "Transient":
                return "Localized events preserved";
            case "Noisy":
                return "Scale-invariant randomness";
            default:
                return "Unknown";
        }
    }

    private static void testMODWTSignalClassification(MODWTTransform transform,
                                                      Map<String, List<WaveletFeatures>> classFeatures) {
        Random rng = new Random(123);

        // Create test signals with different lengths
        String[] testClasses = {"Periodic", "Transient", "Noisy"};
        int[] testLengths = {77, 93, 111}; // All different, non-power-of-2

        for (int idx = 0; idx < testClasses.length; idx++) {
            String actualClass = testClasses[idx];
            int length = testLengths[idx];
            
            // Generate test signal
            double[] testSignal = new double[length];
            switch (actualClass) {
                case "Periodic":
                    for (int i = 0; i < length; i++) {
                        testSignal[i] = Math.sin(2.5 * 2 * Math.PI * i / length);
                    }
                    break;
                case "Transient":
                    for (int i = 0; i < length; i++) {
                        testSignal[i] = Math.exp(-0.5 * (i - length/2) * (i - length/2) / 9);
                    }
                    break;
                case "Noisy":
                    for (int i = 0; i < length; i++) {
                        testSignal[i] = rng.nextGaussian() * 0.7;
                    }
                    break;
            }

            // Extract features
            WaveletFeatures features = extractMODWTFeatures(transform, testSignal);

            // Simple nearest neighbor classification
            String predictedClass = classifySignal(features, classFeatures);

            System.out.printf("Test signal (length=%d, actual: %s) → predicted: %s %s\n",
                    length, actualClass, predictedClass,
                    actualClass.equals(predictedClass) ? "✓" : "✗");
        }
    }

    private static String classifySignal(WaveletFeatures features,
                                         Map<String, List<WaveletFeatures>> classFeatures) {
        String bestClass = "";
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, List<WaveletFeatures>> entry : classFeatures.entrySet()) {
            String className = entry.getKey();

            // Compute average distance to class
            double avgDistance = 0;
            for (WaveletFeatures classFeature : entry.getValue()) {
                double distance = computeFeatureDistance(features, classFeature);
                avgDistance += distance;
            }
            avgDistance /= entry.getValue().size();

            if (avgDistance < minDistance) {
                minDistance = avgDistance;
                bestClass = className;
            }
        }

        return bestClass;
    }

    private static double computeFeatureDistance(WaveletFeatures f1, WaveletFeatures f2) {
        // Simple Euclidean distance on normalized features
        double d1 = (f1.energy - f2.energy) / 100;
        double d2 = (f1.entropy - f2.entropy) / 10;
        double d3 = (f1.zeroCrossings - f2.zeroCrossings) / 50.0;

        return Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
    }

    private static double[] createPatternSignal(int length) {
        double[] signal = new double[length];
        Random rng = new Random(42);

        // Base pattern
        int patternLength = 32;
        double[] pattern = new double[patternLength];
        for (int i = 0; i < patternLength; i++) {
            pattern[i] = Math.sin(2 * Math.PI * i / 8) +
                    0.5 * Math.sin(2 * Math.PI * i / 16);
        }

        // Repeat pattern with variations
        for (int i = 0; i < length; i++) {
            signal[i] = pattern[i % patternLength] * (0.8 + 0.4 * rng.nextDouble());

            // Add some noise
            signal[i] += 0.1 * rng.nextGaussian();
        }

        return signal;
    }

    private static PatternInfo detectMODWTPattern(double[] coeffs) {
        // Autocorrelation for pattern detection
        // MODWT preserves time structure, making pattern detection more reliable
        int maxLag = coeffs.length / 2;
        double maxCorr = 0;
        int bestPeriod = 0;

        for (int lag = 1; lag < maxLag; lag++) {
            double corr = 0;
            int count = 0;

            for (int i = 0; i < coeffs.length - lag; i++) {
                corr += coeffs[i] * coeffs[i + lag];
                count++;
            }
            corr /= count;

            if (corr > maxCorr) {
                maxCorr = corr;
                bestPeriod = lag;
            }
        }

        // Normalize correlation
        double variance = 0;
        for (double c : coeffs) variance += c * c;
        variance /= coeffs.length;

        double confidence = maxCorr / (variance + 1e-10);
        confidence = Math.min(1.0, Math.max(0.0, confidence));

        String type = confidence > 0.8 ? "Strong periodic" :
                confidence > 0.5 ? "Weak periodic" : "Non-periodic";

        return new PatternInfo(bestPeriod, confidence, type);
    }

    private static void demonstrateMODWTCrossScaleCorrelation(MultiLevelMODWTResult result) {
        System.out.println("\nScale | Correlation with next | MODWT Benefit");
        System.out.println("------|----------------------|---------------");

        for (int level = 1; level < result.getLevels(); level++) {
            double[] current = result.getDetailCoeffsAtLevel(level);
            double[] next = result.getDetailCoeffsAtLevel(level + 1);

            // MODWT coefficients have same length - direct correlation!
            double corr = computeMODWTScaleCorrelation(current, next);

            System.out.printf("  %d   | %20.3f | Same length alignment\n", level, corr);
        }
    }

    private static double computeMODWTScaleCorrelation(double[] scale1, double[] scale2) {
        // MODWT advantage: scales have same length!
        assert scale1.length == scale2.length : "MODWT scales should have equal length";

        double sum1 = 0, sum2 = 0, sum12 = 0, sum11 = 0, sum22 = 0;
        int n = scale1.length;

        for (int i = 0; i < n; i++) {
            sum1 += scale1[i];
            sum2 += scale2[i];
            sum12 += scale1[i] * scale2[i];
            sum11 += scale1[i] * scale1[i];
            sum22 += scale2[i] * scale2[i];
        }

        double corr = (n * sum12 - sum1 * sum2) /
                Math.sqrt((n * sum11 - sum1 * sum1) *
                        (n * sum22 - sum2 * sum2) + 1e-10);

        return corr;
    }

    // Helper classes

    static class WaveletFeatures {
        double mean;
        double stdDev;
        double energy;
        double entropy;
        int zeroCrossings;
        String dominantScale;

        WaveletFeatures(double mean, double stdDev, double energy,
                        double entropy, int zeroCrossings, String dominantScale) {
            this.mean = mean;
            this.stdDev = stdDev;
            this.energy = energy;
            this.entropy = entropy;
            this.zeroCrossings = zeroCrossings;
            this.dominantScale = dominantScale;
        }
    }

    static class AnomalyScore {
        int start;
        int end;
        double score;

        AnomalyScore(int start, int end, double score) {
            this.start = start;
            this.end = end;
            this.score = score;
        }
    }

    static class PatternInfo {
        int period;
        double confidence;
        String type;

        PatternInfo(int period, double confidence, String type) {
            this.period = period;
            this.confidence = confidence;
            this.type = type;
        }
    }
}
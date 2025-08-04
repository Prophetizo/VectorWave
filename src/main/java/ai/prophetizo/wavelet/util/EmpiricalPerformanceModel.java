package ai.prophetizo.wavelet.util;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.modwt.MODWTTransform;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Empirical performance modeling system for wavelet transforms.
 * 
 * <p>This system replaces hardcoded performance estimates with adaptive models
 * based on actual runtime measurements. The models are platform-specific and
 * automatically calibrate based on observed performance.</p>
 * 
 * <p><strong>Key features:</strong></p>
 * <ul>
 *   <li>Platform-specific calibration (ARM, x86, etc.)</li>
 *   <li>Wavelet-specific models (Haar vs. Daubechies, etc.)</li>
 *   <li>Confidence intervals and reliability tracking</li>
 *   <li>Automatic recalibration when performance characteristics change</li>
 * </ul>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // Calibrate the model with actual measurements
 * PerformanceModel model = EmpiricalPerformanceModel.getModel("MODWT", "Haar");
 * 
 * // Use for estimation
 * double estimated = model.estimateProcessingTime(4096);
 * 
 * // Update with actual measurement
 * model.recordMeasurement(4096, actualTimeMs);
 * }</pre>
 * 
 * @since 3.1.0
 */
public final class EmpiricalPerformanceModel {
    
    private static final Logger logger = Logger.getLogger(EmpiricalPerformanceModel.class.getName());
    
    // Cache of performance models by transform type and wavelet
    private static final ConcurrentMap<String, PerformanceModel> MODELS = new ConcurrentHashMap<>();
    
    // Platform characteristics detected at startup
    private static final PlatformCharacteristics PLATFORM = detectPlatform();
    
    static {
        logger.info("Initialized empirical performance modeling for platform: " + PLATFORM);
    }
    
    private EmpiricalPerformanceModel() {
        // Utility class
    }
    
    /**
     * Gets or creates a performance model for a specific transform and wavelet combination.
     * 
     * @param transformType the type of transform (e.g., "MODWT", "DWT", "CWT")
     * @param waveletName the name of the wavelet (e.g., "Haar", "DB4", "BIOR2_2")
     * @return a performance model for the specified combination
     */
    public static PerformanceModel getModel(String transformType, String waveletName) {
        String key = transformType + ":" + waveletName + ":" + PLATFORM.signature();
        
        return MODELS.computeIfAbsent(key, k -> {
            logger.fine("Creating new performance model for: " + k);
            return new PerformanceModel(transformType, waveletName, PLATFORM);
        });
    }
    
    /**
     * Performs calibration measurements for a specific transform configuration.
     * 
     * <p>This method runs a series of benchmark measurements to establish
     * baseline performance characteristics for the current platform.</p>
     * 
     * @param transform the transform to calibrate
     * @param wavelet the wavelet to use for calibration
     * @param signalSizes array of signal sizes to test
     * @return the calibrated performance model
     */
    public static PerformanceModel calibrateModel(MODWTTransform transform, Wavelet wavelet, int[] signalSizes) {
        String transformType = "MODWT";
        String waveletName = wavelet.name();
        PerformanceModel model = getModel(transformType, waveletName);
        
        logger.info("Starting performance calibration for " + transformType + " with " + waveletName + " wavelet");
        
        for (int size : signalSizes) {
            // Warmup
            double[] signal = generateTestSignal(size);
            for (int i = 0; i < 3; i++) {
                transform.forward(signal);
            }
            
            // Measure multiple times for statistical accuracy
            double totalTime = 0.0;
            int measurements = Math.max(3, 100_000 / size); // More measurements for smaller signals
            
            for (int i = 0; i < measurements; i++) {
                long startTime = System.nanoTime();
                transform.forward(signal);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
            }
            
            double averageTime = totalTime / measurements;
            model.recordMeasurement(size, averageTime);
            
            logger.fine(String.format("Calibrated size %d: %.3f ms (avg of %d measurements)", 
                                    size, averageTime, measurements));
        }
        
        logger.info("Completed performance calibration for " + transformType + " with " + waveletName + " wavelet");
        return model;
    }
    
    /**
     * Detects platform characteristics that affect performance.
     */
    private static PlatformCharacteristics detectPlatform() {
        String osArch = System.getProperty("os.arch", "unknown");
        String osName = System.getProperty("os.name", "unknown");
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        boolean isARM = osArch.toLowerCase().contains("arm") || osArch.toLowerCase().contains("aarch64");
        boolean isAppleSilicon = isARM && osName.toLowerCase().contains("mac");
        boolean isIntel = osArch.toLowerCase().contains("x86") || osArch.toLowerCase().contains("amd64");
        
        // Estimate vector width based on platform
        int estimatedVectorWidth;
        if (isAppleSilicon) {
            estimatedVectorWidth = 2; // 128-bit NEON
        } else if (isIntel) {
            estimatedVectorWidth = 4; // Conservative estimate (AVX2)
        } else {
            estimatedVectorWidth = 1; // Scalar fallback
        }
        
        return new PlatformCharacteristics(osArch, osName, availableProcessors, 
                                         isARM, isAppleSilicon, isIntel, estimatedVectorWidth);
    }
    
    /**
     * Generates a test signal for calibration purposes.
     */
    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / size) + 0.1 * Math.random();
        }
        return signal;
    }
    
    /**
     * Platform characteristics record.
     */
    public record PlatformCharacteristics(
        String architecture,
        String operatingSystem,
        int processors,
        boolean isARM,
        boolean isAppleSilicon,
        boolean isIntel,
        int estimatedVectorWidth
    ) {
        public String signature() {
            return architecture + "-" + processors + "core-vec" + estimatedVectorWidth;
        }
        
        @Override
        public String toString() {
            return String.format("%s on %s (%d cores, %d-wide vectors)", 
                               architecture, operatingSystem, processors, estimatedVectorWidth);
        }
    }
    
    /**
     * Adaptive performance model that learns from measurements.
     */
    public static class PerformanceModel {
        private final String transformType;
        private final String waveletName;
        private final PlatformCharacteristics platform;
        
        // Performance measurements: signal size -> MeasurementStatistics
        private final ConcurrentMap<Integer, MeasurementStatistics> measurements = new ConcurrentHashMap<>();
        
        // Model coefficients (updated as measurements come in)
        private volatile double baseCoefficient = 0.1; // Base time in ms
        private volatile double linearCoefficient = 0.00001; // Linear scaling factor
        private volatile double logCoefficient = 0.0; // Logarithmic scaling factor
        private volatile double confidenceLevel = 0.0; // 0.0 = no confidence, 1.0 = high confidence
        
        PerformanceModel(String transformType, String waveletName, PlatformCharacteristics platform) {
            this.transformType = transformType;
            this.waveletName = waveletName;
            this.platform = platform;
            
            // Initialize with platform-specific defaults
            initializeDefaults();
        }
        
        /**
         * Records an actual measurement to improve the model.
         */
        public void recordMeasurement(int signalSize, double actualTimeMs) {
            MeasurementStatistics stats = measurements.computeIfAbsent(signalSize, 
                k -> new MeasurementStatistics());
            
            stats.addMeasurement(actualTimeMs);
            
            // Recalibrate model if we have enough measurements
            if (getTotalMeasurements() >= 10) {
                recalibrateModel();
            }
        }
        
        /**
         * Estimates processing time for a given signal size.
         */
        public double estimateProcessingTime(int signalSize) {
            // If we have a direct measurement, use it with interpolation
            MeasurementStatistics exact = measurements.get(signalSize);
            if (exact != null && exact.getCount() >= 3) {
                return exact.getMean();
            }
            
            // Use model-based estimation
            double estimatedTime = baseCoefficient + 
                                 linearCoefficient * signalSize + 
                                 logCoefficient * Math.log(signalSize);
            
            // Apply platform-specific adjustments
            estimatedTime /= platform.estimatedVectorWidth();
            
            return Math.max(0.001, estimatedTime); // Minimum 1 microsecond
        }
        
        /**
         * Gets the confidence level of the model (0.0 to 1.0).
         */
        public double getConfidenceLevel() {
            return confidenceLevel;
        }
        
        /**
         * Gets model information for debugging.
         */
        public String getModelInfo() {
            return String.format("Model[%s,%s]: base=%.6f, linear=%.9f, log=%.6f, confidence=%.2f, measurements=%d",
                transformType, waveletName, baseCoefficient, linearCoefficient, logCoefficient, 
                confidenceLevel, getTotalMeasurements());
        }
        
        private void initializeDefaults() {
            // Platform-specific initialization
            if (platform.isAppleSilicon()) {
                baseCoefficient = 0.05; // Apple Silicon is generally faster
                linearCoefficient = 0.000005;
            } else if (platform.isIntel()) {
                baseCoefficient = 0.1;
                linearCoefficient = 0.00001;
            } else {
                baseCoefficient = 0.2; // Conservative for unknown platforms
                linearCoefficient = 0.00002;
            }
            
            // Wavelet-specific adjustments
            if ("Haar".equals(waveletName)) {
                linearCoefficient *= 0.8; // Haar is simpler/faster
            } else if (waveletName.startsWith("DB")) {
                linearCoefficient *= 1.2; // Daubechies are more complex
            }
        }
        
        private void recalibrateModel() {
            // Simple linear regression to update coefficients
            if (measurements.size() < 2) return;
            
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            int count = 0;
            
            for (var entry : measurements.entrySet()) {
                int x = entry.getKey();
                double y = entry.getValue().getMean();
                
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
                count++;
            }
            
            if (count >= 2) {
                double denominator = count * sumX2 - sumX * sumX;
                if (Math.abs(denominator) > 1e-10) {
                    linearCoefficient = (count * sumXY - sumX * sumY) / denominator;
                    baseCoefficient = (sumY - linearCoefficient * sumX) / count;
                    
                    // Update confidence based on number of measurements
                    confidenceLevel = Math.min(1.0, count / 20.0);
                }
            }
        }
        
        private int getTotalMeasurements() {
            return measurements.values().stream()
                             .mapToInt(MeasurementStatistics::getCount)
                             .sum();
        }
    }
    
    /**
     * Statistics for repeated measurements at a specific signal size.
     */
    private static class MeasurementStatistics {
        private double sum = 0.0;
        private double sumSquares = 0.0;
        private int count = 0;
        
        synchronized void addMeasurement(double value) {
            sum += value;
            sumSquares += value * value;
            count++;
        }
        
        synchronized double getMean() {
            return count > 0 ? sum / count : 0.0;
        }
        
        synchronized double getStandardDeviation() {
            if (count < 2) return 0.0;
            double mean = getMean();
            return Math.sqrt((sumSquares - count * mean * mean) / (count - 1));
        }
        
        synchronized int getCount() {
            return count;
        }
    }
}
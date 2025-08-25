package ai.prophetizo.wavelet.performance;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.modwt.MODWTTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.WaveletOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Calibrates performance models by running benchmarks on the target platform.
 * 
 * <p>This class performs systematic benchmarking across different input sizes
 * and configurations to build accurate performance models.</p>
 * 
 */
public class PerformanceCalibrator {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 50;
    private static final int MIN_RUNTIME_MS = 10; // Minimum time per measurement
    
    // Test sizes for calibration
    private static final int[] CALIBRATION_SIZES = {
        64, 128, 256, 512, 777, 1024, 1337, 2048, 3000, 4096, 
        6000, 8192, 12000, 16384, 24000, 32768, 50000, 65536
    };
    
    private final PlatformFactors platformFactors;
    private final List<CalibrationResult> results = new ArrayList<>();
    
    /**
     * Creates a new calibrator for the current platform.
     */
    public PerformanceCalibrator() {
        this.platformFactors = PlatformFactors.detectPlatform();
    }
    
    /**
     * Creates a calibrator with specified platform factors.
     * 
     * @param platformFactors Platform-specific factors
     */
    public PerformanceCalibrator(PlatformFactors platformFactors) {
        this.platformFactors = platformFactors;
    }
    
    /**
     * Runs full calibration suite and returns performance models.
     * 
     * @return Calibrated performance models
     */
    public CalibratedModels calibrate() {
        System.out.println("Starting performance calibration...");
        System.out.println("Platform: " + platformFactors);
        
        // Create models for different operations
        PerformanceModel modwtModel = calibrateMODWT();
        PerformanceModel convolutionModel = calibrateConvolution();
        PerformanceModel batchModel = calibrateBatchOperations();
        
        // Save calibration results
        CalibratedModels models = new CalibratedModels(
            platformFactors,
            modwtModel,
            convolutionModel,
            batchModel,
            System.currentTimeMillis()
        );
        
        System.out.println("\nCalibration complete!");
        System.out.println("MODWT model accuracy: " + modwtModel.getAccuracy());
        System.out.println("Convolution model accuracy: " + convolutionModel.getAccuracy());
        System.out.println("Batch model accuracy: " + batchModel.getAccuracy());
        
        return models;
    }
    
    /**
     * Calibrates MODWT transform performance.
     */
    private PerformanceModel calibrateMODWT() {
        System.out.println("\nCalibrating MODWT performance...");
        
        PerformanceModel model = new PerformanceModel(platformFactors);
        List<PerformanceModel.Measurement> measurements = new ArrayList<>();
        
        // Test with different wavelets
        Wavelet[] wavelets = { new Haar(), Daubechies.DB4 };
        
        for (Wavelet wavelet : wavelets) {
            System.out.println("  Testing " + wavelet.name() + " wavelet");
            
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            for (int size : CALIBRATION_SIZES) {
                // Generate test signal
                double[] signal = generateTestSignal(size);
                
                // Warm up
                for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                    transform.forward(signal);
                }
                
                // Measure
                long totalTime = 0;
                int iterations = MEASUREMENT_ITERATIONS;
                
                // Adjust iterations to ensure minimum runtime
                long startCheck = System.nanoTime();
                for (int i = 0; i < 10; i++) {
                    transform.forward(signal);
                }
                long checkTime = System.nanoTime() - startCheck;
                if (TimeUnit.NANOSECONDS.toMillis(checkTime) < MIN_RUNTIME_MS) {
                    iterations = (int) (MIN_RUNTIME_MS * 10 / TimeUnit.NANOSECONDS.toMillis(checkTime));
                }
                
                // Actual measurement
                for (int i = 0; i < iterations; i++) {
                    long start = System.nanoTime();
                    MODWTResult result = transform.forward(signal);
                    long end = System.nanoTime();
                    totalTime += (end - start);
                    
                    // Prevent optimization
                    if (result.approximationCoeffs()[0] == Double.MAX_VALUE) {
                        System.out.println("Prevent optimization");
                    }
                }
                
                double avgTimeMs = TimeUnit.NANOSECONDS.toMillis(totalTime) / (double) iterations;
                boolean hasVectorization = WaveletOperations.getPerformanceInfo().vectorizationEnabled();
                
                measurements.add(new PerformanceModel.Measurement(size, avgTimeMs, hasVectorization));
                
                if (size <= 1024 || size % 4096 == 0) {
                    System.out.printf("    Size %6d: %.3f ms\n", size, avgTimeMs);
                }
            }
        }
        
        // Calibrate model with all measurements
        model.calibrate(measurements.toArray(new PerformanceModel.Measurement[0]));
        
        return model;
    }
    
    /**
     * Calibrates convolution operation performance.
     */
    private PerformanceModel calibrateConvolution() {
        System.out.println("\nCalibrating convolution performance...");
        
        PerformanceModel model = new PerformanceModel(platformFactors);
        List<PerformanceModel.Measurement> measurements = new ArrayList<>();
        
        // Test with different filter sizes
        int[] filterSizes = {2, 4, 8, 16};
        
        for (int filterSize : filterSizes) {
            System.out.println("  Testing filter size " + filterSize);
            
            double[] filter = new double[filterSize];
            for (int i = 0; i < filterSize; i++) {
                filter[i] = 1.0 / Math.sqrt(filterSize);
            }
            
            for (int size : CALIBRATION_SIZES) {
                double[] signal = generateTestSignal(size);
                double[] output = new double[size];
                
                // Warm up
                for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                    WaveletOperations.circularConvolveMODWT(signal, filter, output);
                }
                
                // Measure
                long totalTime = measureOperation(
                    () -> WaveletOperations.circularConvolveMODWT(signal, filter, output),
                    MEASUREMENT_ITERATIONS
                );
                
                double avgTimeMs = TimeUnit.NANOSECONDS.toMillis(totalTime) / 
                    (double) MEASUREMENT_ITERATIONS;
                boolean hasVectorization = WaveletOperations.getPerformanceInfo().vectorizationEnabled();
                
                measurements.add(new PerformanceModel.Measurement(size, avgTimeMs, hasVectorization));
            }
        }
        
        model.calibrate(measurements.toArray(new PerformanceModel.Measurement[0]));
        return model;
    }
    
    /**
     * Calibrates batch operation performance.
     */
    private PerformanceModel calibrateBatchOperations() {
        System.out.println("\nCalibrating batch operations...");
        
        PerformanceModel model = new PerformanceModel(platformFactors);
        List<PerformanceModel.Measurement> measurements = new ArrayList<>();
        
        // Test different batch sizes
        int[] batchSizes = {4, 8, 16, 32, 64};
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        for (int batchSize : batchSizes) {
            System.out.println("  Testing batch size " + batchSize);
            
            for (int size : new int[]{256, 512, 1024, 2048, 4096}) {
                // Generate batch of signals
                double[][] signals = new double[batchSize][size];
                for (int i = 0; i < batchSize; i++) {
                    signals[i] = generateTestSignal(size);
                }
                
                // Warm up
                for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
                    for (double[] sig : signals) {
                        transform.forward(sig);
                    }
                }
                
                // Measure batch as sequential operations for now
                long totalTime = 0;
                for (int iter = 0; iter < MEASUREMENT_ITERATIONS / 10; iter++) {
                    long start = System.nanoTime();
                    for (double[] sig : signals) {
                        transform.forward(sig);
                    }
                    long end = System.nanoTime();
                    totalTime += (end - start);
                }
                
                double avgTimeMs = TimeUnit.NANOSECONDS.toMillis(totalTime) / 
                    (double) (MEASUREMENT_ITERATIONS / 10);
                double avgTimePerSignal = avgTimeMs / batchSize;
                boolean hasVectorization = WaveletOperations.getPerformanceInfo().vectorizationEnabled();
                
                // Store as effective single-signal time
                measurements.add(new PerformanceModel.Measurement(
                    size, avgTimePerSignal, hasVectorization));
                
                System.out.printf("    Batch %2d × Size %4d: %.3f ms total, %.3f ms/signal\n", 
                    batchSize, size, avgTimeMs, avgTimePerSignal);
            }
        }
        
        model.calibrate(measurements.toArray(new PerformanceModel.Measurement[0]));
        return model;
    }
    
    /**
     * Measures the execution time of an operation.
     */
    private long measureOperation(Runnable operation, int iterations) {
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            operation.run();
            long end = System.nanoTime();
            totalTime += (end - start);
        }
        
        return totalTime;
    }
    
    /**
     * Generates a test signal with realistic characteristics.
     */
    private double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        Random rand = new Random(42); // Fixed seed for reproducibility
        
        // Mix of smooth trend and noise
        for (int i = 0; i < size; i++) {
            double trend = Math.sin(2 * Math.PI * i / size) * 10;
            double noise = rand.nextGaussian() * 0.5;
            signal[i] = trend + noise;
        }
        
        return signal;
    }
    
    /**
     * Result of a calibration run.
     */
    public record CalibrationResult(
        String operation,
        int inputSize,
        double executionTime,
        boolean vectorized
    ) {}
    
    /**
     * Collection of calibrated performance models.
     */
    public record CalibratedModels(
        PlatformFactors platformFactors,
        PerformanceModel modwtModel,
        PerformanceModel convolutionModel,
        PerformanceModel batchModel,
        long calibrationTimestamp
    ) {
        
        /**
         * Saves models to a file for persistence.
         * 
         * @param filename Path to save the models
         */
        public void save(String filename) {
            try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                    new java.io.FileOutputStream(filename))) {
                out.writeObject(this);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to save performance models", e);
            }
        }
        
        /**
         * Loads models from a file.
         * 
         * @param filename Path to load the models from
         * @return Loaded models
         */
        public static CalibratedModels load(String filename) {
            try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                    new java.io.FileInputStream(filename))) {
                return (CalibratedModels) in.readObject();
            } catch (java.io.IOException | ClassNotFoundException e) {
                throw new RuntimeException("Failed to load performance models", e);
            }
        }
    }
}
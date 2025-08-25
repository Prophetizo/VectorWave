package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.padding.*;import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.modwt.MODWTBatchSIMD;
import ai.prophetizo.wavelet.memory.BatchMemoryLayout;
import jdk.incubator.vector.DoubleVector;

/**
 * Comprehensive demonstration of MODWT batch processing capabilities.
 * Shows various ways to process multiple signals efficiently using SIMD with MODWT.
 * 
 * <p>Key advantages over DWT batch processing:</p>
 * <ul>
 *   <li>Handles arbitrary signal lengths</li>
 *   <li>Shift-invariant processing</li>
 *   <li>Better continuity across batches</li>
 *   <li>No padding requirements</li>
 * </ul>
 * 
 */
public class MODWTBatchProcessingDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave MODWT Batch Processing Demo");
        System.out.println("======================================\n");
        
        // Check SIMD capabilities
        System.out.println("Platform Information:");
        System.out.println("SIMD vector length: " + DoubleVector.SPECIES_PREFERRED.length());
        System.out.println(MODWTBatchSIMD.getBatchSIMDInfo());
        System.out.println();
        
        // Demo 1: Basic MODWT batch transform
        basicMODWTBatchTransform();
        
        // Demo 2: Optimized MODWT engine
        optimizedMODWTEngineDemo();
        
        // Demo 3: Multi-level MODWT batch processing
        multiLevelMODWTBatchDemo();
        
        // Demo 4: Non-power-of-2 signal processing
        nonPowerOfTwoDemo();
        
        // Demo 5: Financial time series with MODWT
        financialTimeSeriesMODWTDemo();
        
        // Demo 6: Performance comparison
        performanceComparisonDemo();
    }
    
    private static void basicMODWTBatchTransform() {
        System.out.println("1. Basic MODWT Batch Transform");
        System.out.println("------------------------------");
        
        // Create MODWT transform
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Prepare multiple signals with varying lengths
        int[] signalLengths = {100, 150, 200, 250};  // Non-power-of-2 lengths
        double[][] signals = new double[signalLengths.length][];
        
        // Fill with test data
        for (int i = 0; i < signalLengths.length; i++) {
            signals[i] = new double[signalLengths[i]];
            for (int j = 0; j < signalLengths[i]; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * (i + 1) * j / signalLengths[i]);
            }
        }
        
        // Process each signal (MODWT doesn't have batch method yet, but we can add one)
        MODWTResult[] results = new MODWTResult[signals.length];
        long startTime = System.nanoTime();
        
        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }
        
        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("Processed " + results.length + " signals in " + 
                          (elapsedTime / 1_000_000.0) + " ms");
        
        // Show results
        for (int i = 0; i < results.length; i++) {
            System.out.println("Signal " + i + " (length=" + signalLengths[i] + "):");
            System.out.println("  Approximation coeffs: " + results[i].approximationCoeffs().length);
            System.out.println("  Detail coeffs: " + results[i].detailCoeffs().length);
        }
        
        // Verify perfect reconstruction
        double maxError = 0;
        for (int i = 0; i < signals.length; i++) {
            double[] reconstructed = transform.inverse(results[i]);
            for (int j = 0; j < signals[i].length; j++) {
                maxError = Math.max(maxError, Math.abs(signals[i][j] - reconstructed[j]));
            }
        }
        System.out.println("Max reconstruction error: " + maxError);
        System.out.println();
    }
    
    private static void optimizedMODWTEngineDemo() {
        System.out.println("2. Optimized MODWT Transform Engine");
        System.out.println("-----------------------------------");
        
        // Create MODWT transform - optimizations are automatic
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Prepare batch of signals with same length for optimal processing
        int batchSize = 32;
        int signalLength = 1000;  // Non-power-of-2
        double[][] signals = new double[batchSize][signalLength];
        
        // Generate test signals
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = Math.cos(2 * Math.PI * i * j / signalLength) + 
                               0.5 * Math.sin(4 * Math.PI * i * j / signalLength);
            }
        }
        
        // Process batch with optimized engine
        long startTime = System.nanoTime();
        MODWTResult[] results = transform.forwardBatch(signals);
        long elapsedTime = System.nanoTime() - startTime;
        
        System.out.println("Processed " + batchSize + " signals of length " + signalLength);
        System.out.println("Total time: " + (elapsedTime / 1_000_000.0) + " ms");
        System.out.println("Average time per signal: " + (elapsedTime / 1_000_000.0 / batchSize) + " ms");
        System.out.println("Throughput: " + (batchSize * signalLength / (elapsedTime / 1e9)) + " samples/second");
        System.out.println();
    }
    
    private static void multiLevelMODWTBatchDemo() {
        System.out.println("3. Multi-Level MODWT Batch Processing");
        System.out.println("-------------------------------------");
        
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Create signals
        int batchSize = 8;
        int signalLength = 500;
        int levels = 4;
        
        double[][] signals = new double[batchSize][signalLength];
        for (int i = 0; i < batchSize; i++) {
            // Generate composite signal with multiple frequencies
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * j / 100.0) +      // Low freq
                               0.5 * Math.sin(2 * Math.PI * j / 25.0) +  // Mid freq
                               0.25 * Math.sin(2 * Math.PI * j / 10.0);  // High freq
            }
        }
        
        // Process each signal with multi-level MODWT
        long startTime = System.nanoTime();
        MultiLevelMODWTResult[] results = new MultiLevelMODWTResult[batchSize];
        
        for (int i = 0; i < batchSize; i++) {
            results[i] = engine.transformMultiLevel(signals[i], new Haar(), 
                                                  BoundaryMode.PERIODIC, levels);
        }
        
        long elapsedTime = System.nanoTime() - startTime;
        
        System.out.println("Processed " + batchSize + " signals with " + levels + " levels");
        System.out.println("Time: " + (elapsedTime / 1_000_000.0) + " ms");
        
        // Analyze energy distribution
        System.out.println("\nEnergy distribution for first signal:");
        double[] energyDist = results[0].getRelativeEnergyDistribution();
        for (int level = 0; level < energyDist.length; level++) {
            if (level < levels) {
                System.out.printf("  Level %d details: %.2f%%\n", level + 1, energyDist[level] * 100);
            } else {
                System.out.printf("  Approximation: %.2f%%\n", energyDist[level] * 100);
            }
        }
        System.out.println();
    }
    
    private static void nonPowerOfTwoDemo() {
        System.out.println("4. Non-Power-of-2 Signal Processing");
        System.out.println("-----------------------------------");
        
        // Various non-power-of-2 lengths that would fail with DWT
        int[] lengths = {100, 250, 500, 1000, 1500, 2000, 3000, 5000};
        
        MODWTTransform transform = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
        
        System.out.println("Processing signals of various lengths:");
        for (int length : lengths) {
            double[] signal = new double[length];
            // Generate chirp signal
            for (int i = 0; i < length; i++) {
                double t = (double) i / length;
                signal[i] = Math.sin(2 * Math.PI * (10 + 40 * t) * t);
            }
            
            long startTime = System.nanoTime();
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            long elapsedTime = System.nanoTime() - startTime;
            
            // Calculate reconstruction error
            double rmse = 0;
            for (int i = 0; i < length; i++) {
                double diff = signal[i] - reconstructed[i];
                rmse += diff * diff;
            }
            rmse = Math.sqrt(rmse / length);
            
            System.out.printf("  Length %5d: Time=%.2f ms, RMSE=%.2e\n", 
                            length, elapsedTime / 1_000_000.0, rmse);
        }
        System.out.println();
    }
    
    private static void financialTimeSeriesMODWTDemo() {
        System.out.println("5. Financial Time Series with MODWT");
        System.out.println("-----------------------------------");
        
        // Simulate multiple asset price series
        int numAssets = 10;
        int numDays = 252;  // One trading year
        double[][] returns = new double[numAssets][numDays];
        
        // Generate synthetic returns with different volatility regimes
        java.util.Random rand = new java.util.Random(42);
        for (int asset = 0; asset < numAssets; asset++) {
            double baseVol = 0.01 + asset * 0.005;  // Different volatility per asset
            for (int day = 0; day < numDays; day++) {
                // Add volatility clustering
                double volMultiplier = (day > 100 && day < 150) ? 3.0 : 1.0;
                returns[asset][day] = rand.nextGaussian() * baseVol * volMultiplier;
            }
        }
        
        // Process with MODWT for multi-scale analysis
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        MODWTResult[] results = transform.forwardBatch(returns);
        
        System.out.println("Analyzed " + numAssets + " asset return series");
        
        // Analyze volatility at different scales
        System.out.println("\nVolatility analysis (detail coefficient energy):");
        for (int asset = 0; asset < Math.min(3, numAssets); asset++) {
            double[] details = results[asset].detailCoeffs();
            double energy = 0;
            for (double d : details) {
                energy += d * d;
            }
            System.out.printf("  Asset %d: Energy=%.4f (volatility proxy)\n", asset, Math.sqrt(energy));
        }
        System.out.println();
    }
    
    private static void performanceComparisonDemo() {
        System.out.println("6. Performance Comparison");
        System.out.println("-------------------------");
        
        int[] batchSizes = {1, 4, 8, 16, 32, 64};
        int signalLength = 1024;
        
        System.out.println("Signal length: " + signalLength);
        System.out.println("\nBatch Size | Sequential (ms) | Optimized (ms) | Speedup");
        System.out.println("-----------|-----------------|----------------|--------");
        
        for (int batchSize : batchSizes) {
            // Generate test signals
            double[][] signals = new double[batchSize][signalLength];
            for (int i = 0; i < batchSize; i++) {
                for (int j = 0; j < signalLength; j++) {
                    signals[i][j] = Math.random();
                }
            }
            
            // Sequential processing
            MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            long seqStart = System.nanoTime();
            for (int i = 0; i < batchSize; i++) {
                transform.forward(signals[i]);
            }
            long seqTime = System.nanoTime() - seqStart;
            
            // Optimized batch processing
            MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            long optStart = System.nanoTime();
            transform.forwardBatch(signals);
            long optTime = System.nanoTime() - optStart;
            
            double speedup = (double) seqTime / optTime;
            System.out.printf("%10d | %15.2f | %14.2f | %6.2fx\n",
                            batchSize, 
                            seqTime / 1_000_000.0,
                            optTime / 1_000_000.0,
                            speedup);
        }
        
        System.out.println("\nNote: MODWT can process arbitrary length signals unlike DWT!");
    }
}
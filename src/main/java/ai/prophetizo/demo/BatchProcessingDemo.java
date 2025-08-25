package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.padding.*;import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.modwt.MODWTBatchSIMD;
import ai.prophetizo.wavelet.memory.BatchMemoryLayout;
import jdk.incubator.vector.DoubleVector;

/**
 * Comprehensive demonstration of batch processing capabilities in VectorWave.
 * Shows various ways to process multiple signals efficiently using SIMD.
 */
public class BatchProcessingDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave Batch Processing Demo");
        System.out.println("================================\n");
        
        // Check SIMD capabilities
        System.out.println("Platform Information:");
        System.out.println("SIMD vector length: " + DoubleVector.SPECIES_PREFERRED.length());
        System.out.println(MODWTBatchSIMD.getBatchSIMDInfo());
        System.out.println();
        
        // Demo 1: Basic batch transform
        basicBatchTransform();
        
        // Demo 2: Optimized transform engine
        optimizedEngineDemo();
        
        // Demo 3: Memory-aligned batch processing
        memoryAlignedBatchDemo();
        
        // Demo 4: Multi-channel audio processing
        multiChannelAudioDemo();
        
        // Demo 5: Financial time series analysis
        financialTimeSeriesDemo();
        
        // Demo 6: Performance comparison
        performanceComparisonDemo();
    }
    
    private static void basicBatchTransform() {
        System.out.println("1. Basic Batch Transform");
        System.out.println("------------------------");
        
        // Create MODWT transform
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        // Prepare multiple signals
        int batchSize = 8;
        int signalLength = 256;
        double[][] signals = new double[batchSize][signalLength];
        
        // Fill with test data
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                signals[i][j] = Math.sin(2 * Math.PI * (i + 1) * j / signalLength);
            }
        }
        
        // Process batch
        MODWTResult[] results = new MODWTResult[batchSize];
        for (int i = 0; i < batchSize; i++) {
            results[i] = transform.forward(signals[i]);
        }
        System.out.println("Processed " + results.length + " signals in batch");
        
        // Show first result
        System.out.println("First signal results:");
        System.out.println("  Approximation coeffs: " + results[0].approximationCoeffs().length);
        System.out.println("  Detail coeffs: " + results[0].detailCoeffs().length);
        
        // Inverse transform
        double[][] reconstructed = new double[batchSize][signalLength];
        for (int i = 0; i < batchSize; i++) {
            reconstructed[i] = transform.inverse(results[i]);
        }
        System.out.println("Reconstructed " + reconstructed.length + " signals");
        
        // Verify reconstruction
        double maxError = 0;
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                maxError = Math.max(maxError, Math.abs(signals[i][j] - reconstructed[i][j]));
            }
        }
        System.out.println("Max reconstruction error: " + maxError);
        System.out.println();
    }
    
    private static void optimizedEngineDemo() {
        System.out.println("2. Optimized Transform Engine");
        System.out.println("-----------------------------");
        
        // MODWT automatically applies optimizations based on signal characteristics
        
        // Prepare larger batch
        int batchSize = 64;
        int signalLength = 1024;
        double[][] signals = generateTestSignals(batchSize, signalLength);
        
        // Process with different wavelets
        Wavelet[] wavelets = {new Haar(), Daubechies.DB4, Symlet.SYM4};
        
        for (Wavelet wavelet : wavelets) {
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            long start = System.nanoTime();
            MODWTResult[] results = transform.forwardBatch(signals);
            long elapsed = System.nanoTime() - start;
            
            System.out.printf("  %s: %.2f ms for %d signals\n", 
                wavelet.name(), elapsed / 1_000_000.0, batchSize);
        }
        
        System.out.println("\nNote: MODWT automatically applies SIMD and other optimizations when beneficial");
        System.out.println();
    }
    
    private static void memoryAlignedBatchDemo() {
        System.out.println("3. Memory-Aligned Batch Processing");
        System.out.println("----------------------------------");
        
        int batchSize = 16;
        int signalLength = 512;
        
        try (BatchMemoryLayout layout = new BatchMemoryLayout(batchSize, signalLength)) {
            System.out.println("Memory layout created:");
            System.out.println("  Batch size: " + batchSize);
            System.out.println("  Signal length: " + signalLength);
            
            // Generate test signals
            double[][] signals = generateTestSignals(batchSize, signalLength);
            
            // Load signals with interleaving for better SIMD access
            layout.loadSignalsInterleaved(signals, true);
            
            // Perform Haar transform directly on interleaved data
            long start = System.nanoTime();
            layout.haarTransformInterleaved();
            long elapsed = System.nanoTime() - start;
            
            System.out.printf("Interleaved transform time: %.3f ms\n", elapsed / 1_000_000.0);
            
            // Extract results
            double[][] approxResults = new double[batchSize][signalLength / 2];
            double[][] detailResults = new double[batchSize][signalLength / 2];
            layout.extractResultsInterleaved(approxResults, detailResults);
            
            System.out.println("Results extracted successfully");
        }
        System.out.println();
    }
    
    private static void multiChannelAudioDemo() {
        System.out.println("4. Multi-Channel Audio Processing");
        System.out.println("---------------------------------");
        
        // Simulate stereo audio processing
        int channels = 2;
        int sampleRate = 44100;
        double duration = 0.1; // 100ms
        int samples = (int)(sampleRate * duration);
        
        // Round up to next power of 2
        int signalLength = 1;
        while (signalLength < samples) signalLength *= 2;
        
        System.out.println("Processing " + channels + " channel audio:");
        System.out.println("  Sample rate: " + sampleRate + " Hz");
        System.out.println("  Duration: " + duration + " seconds");
        System.out.println("  Samples: " + samples + " (padded to " + signalLength + ")");
        
        // Generate test audio (different frequencies per channel)
        double[][] audioData = new double[channels][signalLength];
        for (int ch = 0; ch < channels; ch++) {
            double frequency = 440 * (ch + 1); // A4 and A5
            for (int i = 0; i < samples; i++) {
                audioData[ch][i] = Math.sin(2 * Math.PI * frequency * i / sampleRate);
            }
        }
        
        // Process with suitable wavelet for audio
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        long start = System.nanoTime();
        MODWTResult[] channelResults = new MODWTResult[channels];
        for (int i = 0; i < channels; i++) {
            channelResults[i] = transform.forward(audioData[i]);
        }
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("Transform time: %.3f ms\n", elapsed / 1_000_000.0);
        
        // Apply simple denoising (threshold small coefficients)
        double threshold = 0.01;
        for (MODWTResult result : channelResults) {
            double[] detail = result.detailCoeffs();
            for (int i = 0; i < detail.length; i++) {
                if (Math.abs(detail[i]) < threshold) {
                    detail[i] = 0;
                }
            }
        }
        
        // Reconstruct
        start = System.nanoTime();
        double[][] processedAudio = new double[channels][signalLength];
        for (int i = 0; i < channels; i++) {
            processedAudio[i] = transform.inverse(channelResults[i]);
        }
        elapsed = System.nanoTime() - start;
        
        System.out.printf("Inverse transform time: %.3f ms\n", elapsed / 1_000_000.0);
        System.out.println("Audio processing complete");
        System.out.println();
    }
    
    private static void financialTimeSeriesDemo() {
        System.out.println("5. Financial Time Series Analysis");
        System.out.println("---------------------------------");
        
        // Simulate multiple stock price series
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "META", "TSLA", "NVDA", "JPM"};
        int tradingDays = 256; // ~1 year
        double[][] priceData = new double[symbols.length][tradingDays];
        
        // Generate synthetic price data with different volatilities
        for (int i = 0; i < symbols.length; i++) {
            double price = 100 + Math.random() * 200;
            double volatility = 0.01 + Math.random() * 0.03;
            
            priceData[i][0] = price;
            for (int day = 1; day < tradingDays; day++) {
                double dailyReturn = (Math.random() - 0.5) * 2 * volatility;
                priceData[i][day] = priceData[i][day-1] * (1 + dailyReturn);
            }
        }
        
        // MODWT provides optimal processing for financial analysis
        
        // Analyze with Daubechies wavelets (good for financial data)
        long start = System.nanoTime();
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        MODWTResult[] results = transform.forwardBatch(priceData);
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("Analyzed %d stocks in %.2f ms\n", 
            symbols.length, elapsed / 1_000_000.0);
        
        // Analyze volatility from detail coefficients
        System.out.println("\nVolatility analysis (from wavelet coefficients):");
        for (int i = 0; i < symbols.length; i++) {
            double[] detail = results[i].detailCoeffs();
            double volatility = calculateStandardDeviation(detail);
            System.out.printf("  %s: %.4f\n", symbols[i], volatility);
        }
        System.out.println();
    }
    
    private static void performanceComparisonDemo() {
        System.out.println("6. Performance Comparison");
        System.out.println("-------------------------");
        
        int[] batchSizes = {1, 2, 4, 8, 16, 32, 64};
        int signalLength = 1024;
        int iterations = 100;
        
        System.out.println("Comparing sequential vs batch processing:");
        System.out.println("Signal length: " + signalLength);
        System.out.println("Iterations: " + iterations);
        System.out.println();
        
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        System.out.println("Batch Size | Sequential (ms) | Batch (ms) | Speedup");
        System.out.println("-----------|-----------------|------------|--------");
        
        for (int batchSize : batchSizes) {
            double[][] signals = generateTestSignals(batchSize, signalLength);
            
            // Warmup
            MODWTTransform warmupTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            for (int i = 0; i < 10; i++) {
                warmupTransform.forwardBatch(signals);
            }
            
            // Sequential timing
            long seqStart = System.nanoTime();
            for (int iter = 0; iter < iterations; iter++) {
                for (double[] signal : signals) {
                    transform.forward(signal);
                }
            }
            long seqTime = System.nanoTime() - seqStart;
            
            // Batch timing (automatic optimization)
            MODWTTransform batchTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
            long batchStart = System.nanoTime();
            for (int iter = 0; iter < iterations; iter++) {
                batchTransform.forwardBatch(signals);
            }
            long batchTime = System.nanoTime() - batchStart;
            
            double seqMs = seqTime / 1_000_000.0;
            double batchMs = batchTime / 1_000_000.0;
            double speedup = seqMs / batchMs;
            
            System.out.printf("    %3d    |     %7.2f     |   %7.2f  |  %.2fx\n",
                batchSize, seqMs, batchMs, speedup);
        }
        
        System.out.println("\nNote: Speedup increases with batch size due to SIMD utilization");
        
        // No cleanup needed for MODWT operations
    }
    
    // Helper methods
    
    private static double[][] generateTestSignals(int batchSize, int signalLength) {
        double[][] signals = new double[batchSize][signalLength];
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                // Mix of frequencies
                signals[i][j] = Math.sin(2 * Math.PI * j / signalLength) * 0.5 +
                               Math.sin(4 * Math.PI * j / signalLength) * 0.3 +
                               Math.sin(8 * Math.PI * j / signalLength) * 0.2;
            }
        }
        return signals;
    }
    
    private static double calculateStandardDeviation(double[] data) {
        double mean = 0;
        for (double value : data) {
            mean += value;
        }
        mean /= data.length;
        
        double variance = 0;
        for (double value : data) {
            double diff = value - mean;
            variance += diff * diff;
        }
        variance /= data.length;
        
        return Math.sqrt(variance);
    }
}
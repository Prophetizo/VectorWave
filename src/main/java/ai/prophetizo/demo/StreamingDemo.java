package ai.prophetizo.demo;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.streaming.StreamingWaveletTransformImpl;

/**
 * Demonstration of the new streaming wavelet transform capabilities.
 * 
 * This demo showcases:
 * 1. Ring buffer-based streaming with zero-copy optimization
 * 2. Overlapping window processing for temporal continuity
 * 3. Real-time wavelet analysis of continuous data streams
 * 4. Performance benefits over traditional array-copying approaches
 */
public class StreamingDemo {
    
    public static void main(String[] args) {
        System.out.println("=== VectorWave Streaming Wavelet Transform Demo ===\n");
        
        demonstrateBasicStreaming();
        System.out.println();
        
        demonstrateOverlapConfigurations();
        System.out.println();
        
        demonstratePerformanceCharacteristics();
        System.out.println();
        
        demonstrateRealTimeProcessing();
    }
    
    /**
     * Demonstrates basic streaming functionality.
     */
    private static void demonstrateBasicStreaming() {
        System.out.println("1. Basic Streaming Functionality");
        System.out.println("================================");
        
        // Create streaming transform: 8-sample windows with 50% overlap
        StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
            new Haar(), BoundaryMode.PERIODIC, 8, 4);
        
        System.out.printf("Configuration: Window=%d, Overlap=%d, Hop=%d%n", 
            streaming.getWindowSize(), streaming.getOverlapSize(), streaming.getHopSize());
        
        // Simulate streaming data arrival
        double[] batch1 = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] batch2 = {9.0, 10.0, 11.0, 12.0};
        double[] batch3 = {13.0, 14.0, 15.0, 16.0};
        
        // Process first batch
        streaming.addSamples(batch1);
        if (streaming.isResultReady()) {
            TransformResult result1 = streaming.getNextResult();
            System.out.printf("Result 1: %d approx coeffs, %d detail coeffs%n", 
                result1.approximationCoeffs().length, result1.detailCoeffs().length);
        }
        
        // Process second batch
        streaming.addSamples(batch2);
        if (streaming.isResultReady()) {
            TransformResult result2 = streaming.getNextResult();
            System.out.printf("Result 2: %d approx coeffs, %d detail coeffs%n", 
                result2.approximationCoeffs().length, result2.detailCoeffs().length);
        }
        
        // Show buffered samples
        System.out.printf("Buffered samples: %d%n", streaming.getBufferedSampleCount());
    }
    
    /**
     * Demonstrates different overlap configurations.
     */
    private static void demonstrateOverlapConfigurations() {
        System.out.println("2. Overlap Configuration Comparison");
        System.out.println("==================================");
        
        int windowSize = 16;
        double[] testSignal = generateTestSignal(64);
        
        // Test different overlap ratios
        double[] overlapRatios = {0.0, 0.25, 0.5, 0.75};
        
        for (double ratio : overlapRatios) {
            int overlapSize = (int) (windowSize * ratio);
            StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
                Daubechies.DB2, BoundaryMode.PERIODIC, windowSize, overlapSize);
            
            int resultCount = 0;
            int sampleIndex = 0;
            int hopSize = windowSize - overlapSize;
            
            // Process signal in batches
            while (sampleIndex < testSignal.length) {
                int batchSize = Math.min(hopSize, testSignal.length - sampleIndex);
                double[] batch = new double[batchSize];
                System.arraycopy(testSignal, sampleIndex, batch, 0, batchSize);
                
                streaming.addSamples(batch);
                if (streaming.isResultReady()) {
                    streaming.getNextResult();
                    resultCount++;
                }
                
                sampleIndex += batchSize;
            }
            
            StreamingWaveletTransformImpl.StreamingPerformanceStats stats = 
                streaming.getPerformanceStats();
            
            System.out.printf("Overlap %.0f%%: %d results, efficiency=%.1f%%%n", 
                ratio * 100, resultCount, stats.getMemoryEfficiency() * 100);
        }
    }
    
    /**
     * Demonstrates performance characteristics and memory usage.
     */
    private static void demonstratePerformanceCharacteristics() {
        System.out.println("3. Performance Characteristics");
        System.out.println("=============================");
        
        // Test with different window sizes
        int[] windowSizes = {64, 128, 256, 512, 1024};
        
        for (int windowSize : windowSizes) {
            StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
                new Haar(), BoundaryMode.PERIODIC, windowSize, windowSize / 2);
            
            // Measure processing time for multiple batches
            double[] batch = generateTestSignal(windowSize / 2);
            
            long startTime = System.nanoTime();
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                streaming.addSamples(batch);
                if (streaming.isResultReady()) {
                    streaming.getNextResult();
                }
            }
            
            long endTime = System.nanoTime();
            double avgTimeUs = (endTime - startTime) / (double) iterations / 1000.0;
            
            System.out.printf("Window %4d: %.2f μs/batch, %.1f MB/s throughput%n", 
                windowSize, avgTimeUs, 
                (batch.length * 8.0) / (avgTimeUs / 1000.0) / (1024 * 1024));
        }
    }
    
    /**
     * Demonstrates real-time processing simulation.
     */
    private static void demonstrateRealTimeProcessing() {
        System.out.println("4. Real-Time Processing Simulation");
        System.out.println("==================================");
        
        StreamingWaveletTransformImpl streaming = new StreamingWaveletTransformImpl(
            Daubechies.DB4, BoundaryMode.PERIODIC, 256, 128);
        
        System.out.println("Simulating real-time signal processing...");
        
        // Simulate real-time data with sample-by-sample processing
        int totalSamples = 2000;
        int processedResults = 0;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalSamples; i++) {
            // Generate sample (simulating ADC input)
            double sample = Math.sin(2 * Math.PI * i / 50.0) + 
                           0.5 * Math.sin(2 * Math.PI * i / 23.0) +
                           0.1 * Math.random(); // Add some noise
            
            // Add sample to streaming buffer
            streaming.addSample(sample);
            
            // Process result if available
            if (streaming.isResultReady()) {
                TransformResult result = streaming.getNextResult();
                processedResults++;
                
                // In a real application, you would analyze the coefficients here
                if (processedResults % 5 == 0) {
                    System.out.printf("Processed %d results (latest: %.2f avg coeff)%n", 
                        processedResults, 
                        average(result.approximationCoeffs()));
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.printf("Processed %d samples in %d ms%n", totalSamples, endTime - startTime);
        System.out.printf("Generated %d transform results%n", processedResults);
        System.out.printf("Throughput: %.1f samples/second%n", 
            totalSamples * 1000.0 / (endTime - startTime));
        
        // Show final statistics
        StreamingWaveletTransformImpl.StreamingPerformanceStats stats = 
            streaming.getPerformanceStats();
        System.out.println("Final stats: " + stats);
    }
    
    /**
     * Generates a test signal with multiple frequency components.
     */
    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8.0) +
                       0.25 * Math.cos(2 * Math.PI * i / 16.0);
        }
        return signal;
    }
    
    /**
     * Calculates the average of an array.
     */
    private static double average(double[] array) {
        double sum = 0.0;
        for (double value : array) {
            sum += value;
        }
        return sum / array.length;
    }
}
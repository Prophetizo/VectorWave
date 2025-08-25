package ai.prophetizo.demo;

import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.parallel.*;
import ai.prophetizo.wavelet.api.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the massive performance improvements of parallel CWT implementation.
 * 
 * <p>This demo showcases:</p>
 * <ul>
 *   <li>Scale-space parallelization for multi-scale analysis</li>
 *   <li>Signal chunking for very long signals</li>
 *   <li>Hybrid parallelization strategies</li>
 *   <li>Performance comparison with sequential implementation</li>
 *   <li>Virtual thread utilization for I/O-bound operations</li>
 * </ul>
 */
public class ParallelCWTDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Parallel CWT Performance Demo ===\n");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB\n");
        
        // Demo 1: Scale parallelization
        demoScaleParallelization();
        
        // Demo 2: Signal chunking
        demoSignalChunking();
        
        // Demo 3: Hybrid parallelization
        demoHybridParallelization();
        
        // Demo 4: FFT acceleration with parallelization
        demoFFTAcceleration();
        
        // Demo 5: Real-world financial signal analysis
        demoFinancialAnalysis();
    }
    
    private static void demoScaleParallelization() {
        System.out.println("1. Scale Parallelization Demo");
        System.out.println("-----------------------------");
        System.out.println("Analyzing moderate signal (4096 samples) with many scales (32)...\n");
        
        // Create test signal - chirp with noise
        int signalLength = 4096;
        double[] signal = createChirpSignal(signalLength);
        
        // Create scale space
        double[] scales = ScaleSpace.exponential(1, 128, 32);
        
        // Create wavelets
        MorletWavelet morlet = new MorletWavelet();
        
        // Sequential CWT
        CWTTransform sequentialCWT = new CWTTransform(morlet);
        long startSeq = System.nanoTime();
        CWTResult seqResult = sequentialCWT.analyze(signal, scales);
        long timeSeq = System.nanoTime() - startSeq;
        
        // Parallel CWT with auto configuration
        ParallelCWTTransform parallelCWT = new ParallelCWTTransform(morlet);
        long startPar = System.nanoTime();
        CWTResult parResult = parallelCWT.analyze(signal, scales);
        long timePar = System.nanoTime() - startPar;
        
        // Custom parallel configuration for maximum performance
        ParallelConfig customConfig = ParallelConfig.builder()
            .parallelismLevel(Runtime.getRuntime().availableProcessors())
            .parallelThreshold(256)
            .useVirtualThreads(true)
            .mode(ParallelConfig.ExecutionMode.PARALLEL_ALWAYS)
            .enableMetrics(true)
            .build();
        
        ParallelCWTTransform customParallelCWT = new ParallelCWTTransform(
            morlet, CWTConfig.defaultConfig(), customConfig
        );
        long startCustom = System.nanoTime();
        CWTResult customResult = customParallelCWT.analyze(signal, scales);
        long timeCustom = System.nanoTime() - startCustom;
        
        // Display results
        System.out.printf("Sequential time:     %8.2f ms\n", timeSeq / 1e6);
        System.out.printf("Parallel (auto):     %8.2f ms (%.2fx speedup)\n", 
            timePar / 1e6, (double) timeSeq / timePar);
        System.out.printf("Parallel (custom):   %8.2f ms (%.2fx speedup)\n", 
            timeCustom / 1e6, (double) timeSeq / timeCustom);
        
        // Show statistics
        ParallelConfig.ExecutionStats stats = parallelCWT.getStats();
        System.out.printf("\nParallel execution ratio: %.1f%%\n", stats.parallelRatio() * 100);
        System.out.printf("Estimated speedup: %.2fx\n", stats.estimatedSpeedup());
        
        // Verify results are identical (within numerical precision)
        double maxDiff = compareResults(seqResult, parResult);
        System.out.printf("Maximum difference: %.2e (should be < 1e-10)\n", maxDiff);
        
        System.out.println();
    }
    
    private static void demoSignalChunking() {
        System.out.println("2. Signal Chunking Demo");
        System.out.println("-----------------------");
        System.out.println("Analyzing very long signal (65536 samples) with few scales (8)...\n");
        
        // Create very long signal
        int signalLength = 65536;
        double[] signal = createComplexSignal(signalLength);
        
        // Few scales
        double[] scales = ScaleSpace.linear(2, 16, 8);
        
        // Create wavelet
        DOGWavelet dog = new DOGWavelet(2);
        
        // Time sequential vs parallel
        CWTTransform sequentialCWT = new CWTTransform(dog);
        long startSeq = System.nanoTime();
        CWTResult seqResult = sequentialCWT.analyze(signal, scales);
        long timeSeq = System.nanoTime() - startSeq;
        
        // Parallel with chunking optimization
        ParallelConfig chunkConfig = ParallelConfig.builder()
            .parallelismLevel(Runtime.getRuntime().availableProcessors())
            .parallelThreshold(1024)
            .chunkSize(4096)
            .mode(ParallelConfig.ExecutionMode.ADAPTIVE)
            .build();
        
        ParallelCWTTransform parallelCWT = new ParallelCWTTransform(
            dog, CWTConfig.defaultConfig(), chunkConfig
        );
        long startPar = System.nanoTime();
        CWTResult parResult = parallelCWT.analyze(signal, scales);
        long timePar = System.nanoTime() - startPar;
        
        System.out.printf("Sequential time:  %8.2f ms\n", timeSeq / 1e6);
        System.out.printf("Parallel time:    %8.2f ms (%.2fx speedup)\n", 
            timePar / 1e6, (double) timeSeq / timePar);
        
        System.out.println();
    }
    
    private static void demoHybridParallelization() {
        System.out.println("3. Hybrid Parallelization Demo");
        System.out.println("-------------------------------");
        System.out.println("Large problem: 16384 samples × 64 scales...\n");
        
        // Large signal and many scales
        int signalLength = 16384;
        double[] signal = createChirpSignal(signalLength);
        double[] scales = ScaleSpace.exponential(1, 256, 64);
        
        PaulWavelet paul = new PaulWavelet(4);
        
        // Sequential baseline
        System.out.println("Running sequential (this may take a while)...");
        CWTTransform sequentialCWT = new CWTTransform(paul);
        long startSeq = System.nanoTime();
        CWTResult seqResult = sequentialCWT.analyze(signal, scales);
        long timeSeq = System.nanoTime() - startSeq;
        
        // Parallel with hybrid strategy
        System.out.println("Running parallel with hybrid strategy...");
        ParallelCWTTransform parallelCWT = new ParallelCWTTransform(paul);
        long startPar = System.nanoTime();
        CWTResult parResult = parallelCWT.analyze(signal, scales);
        long timePar = System.nanoTime() - startPar;
        
        System.out.printf("\nSequential time:  %8.2f ms\n", timeSeq / 1e6);
        System.out.printf("Parallel time:    %8.2f ms (%.2fx speedup)\n", 
            timePar / 1e6, (double) timeSeq / timePar);
        
        // Memory usage estimate
        long coefficientMemory = (long) signalLength * scales.length * 8; // bytes
        System.out.printf("\nCoefficient matrix size: %.2f MB\n", coefficientMemory / (1024.0 * 1024.0));
        
        System.out.println();
    }
    
    private static void demoFFTAcceleration() {
        System.out.println("4. FFT Acceleration with Parallelization");
        System.out.println("-----------------------------------------");
        System.out.println("Comparing direct convolution vs FFT-accelerated parallel CWT...\n");
        
        int signalLength = 8192;
        double[] signal = createComplexSignal(signalLength);
        double[] scales = ScaleSpace.exponential(2, 64, 16);
        
        MorletWavelet morlet = new MorletWavelet();
        
        // Direct convolution (no FFT)
        CWTConfig directConfig = CWTConfig.builder()
            .useFFT(false)
            .normalizeAcrossScales(true)
            .build();
        
        ParallelCWTTransform directCWT = new ParallelCWTTransform(
            morlet, directConfig, ParallelConfig.auto()
        );
        
        long startDirect = System.nanoTime();
        CWTResult directResult = directCWT.analyze(signal, scales);
        long timeDirect = System.nanoTime() - startDirect;
        
        // FFT-accelerated
        CWTConfig fftConfig = CWTConfig.builder()
            .useFFT(true)
            .fftThreshold(512)
            .normalizeAcrossScales(true)
            .build();
        
        ParallelCWTTransform fftCWT = new ParallelCWTTransform(
            morlet, fftConfig, ParallelConfig.auto()
        );
        
        long startFFT = System.nanoTime();
        CWTResult fftResult = fftCWT.analyze(signal, scales);
        long timeFFT = System.nanoTime() - startFFT;
        
        System.out.printf("Direct convolution:  %8.2f ms\n", timeDirect / 1e6);
        System.out.printf("FFT-accelerated:     %8.2f ms (%.2fx speedup)\n", 
            timeFFT / 1e6, (double) timeDirect / timeFFT);
        
        System.out.println();
    }
    
    private static void demoFinancialAnalysis() {
        System.out.println("5. Real-World Financial Signal Analysis");
        System.out.println("----------------------------------------");
        System.out.println("Analyzing simulated stock returns with multiple time scales...\n");
        
        // Simulate daily returns for 5 years
        int days = 252 * 5; // 5 years of trading days
        double[] returns = createFinancialReturns(days);
        
        // Time scales from intraday to yearly
        double[] scales = new double[] {
            1,    // Daily
            5,    // Weekly
            21,   // Monthly
            63,   // Quarterly
            126,  // Semi-annual
            252   // Annual
        };
        
        // Use Paul wavelet for financial analysis
        PaulWavelet paul = new PaulWavelet(4);
        
        // Configure for financial analysis
        ParallelConfig financeConfig = ParallelConfig.builder()
            .parallelismLevel(scales.length) // One thread per scale
            .useVirtualThreads(true)
            .enableMetrics(true)
            .build();
        
        ParallelCWTTransform cwt = new ParallelCWTTransform(
            paul, CWTConfig.defaultConfig(), financeConfig
        );
        
        // Analyze
        long start = System.nanoTime();
        CWTResult result = cwt.analyze(returns, scales);
        long time = System.nanoTime() - start;
        
        System.out.printf("Analysis time: %.2f ms\n", time / 1e6);
        System.out.println("\nScale Analysis:");
        System.out.println("Scale  | Period      | Energy   | Volatility");
        System.out.println("-------|-------------|----------|------------");
        
        for (int i = 0; i < scales.length; i++) {
            double[] coeffs = result.getCoefficientsAtScale(i);
            double energy = computeEnergy(coeffs);
            double volatility = Math.sqrt(energy / coeffs.length);
            
            String period = switch(i) {
                case 0 -> "Daily      ";
                case 1 -> "Weekly     ";
                case 2 -> "Monthly    ";
                case 3 -> "Quarterly  ";
                case 4 -> "Semi-Annual";
                case 5 -> "Annual     ";
                default -> "Unknown    ";
            };
            
            System.out.printf("%6.0f | %s | %8.4f | %10.6f\n", 
                scales[i], period, energy, volatility);
        }
        
        // Show parallelization statistics
        ParallelConfig.ExecutionStats stats = cwt.getStats();
        System.out.printf("\nParallelization efficiency: %.1f%%\n", 
            stats.parallelRatio() * 100);
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    // Helper methods for signal generation
    
    private static double[] createChirpSignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 5 + 50 * t; // Frequency increases from 5 to 55
            signal[i] = Math.sin(2 * Math.PI * freq * t) + 0.1 * rand.nextGaussian();
        }
        
        return signal;
    }
    
    private static double[] createComplexSignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) +      // Low frequency
                       0.5 * Math.sin(2 * Math.PI * 25 * t) + // Medium frequency
                       0.3 * Math.sin(2 * Math.PI * 100 * t) + // High frequency
                       0.2 * rand.nextGaussian();              // Noise
        }
        
        return signal;
    }
    
    private static double[] createFinancialReturns(int length) {
        double[] returns = new double[length];
        Random rand = new Random(42);
        
        // GARCH-like model with volatility clustering
        double volatility = 0.01;
        double persistence = 0.9;
        double meanReversion = 0.1;
        
        for (int i = 0; i < length; i++) {
            // Update volatility (volatility clustering)
            if (i > 0) {
                double shock = Math.abs(returns[i-1]);
                volatility = persistence * volatility + meanReversion * shock;
            }
            
            // Generate return
            returns[i] = volatility * rand.nextGaussian();
            
            // Add occasional jumps (rare events)
            if (rand.nextDouble() < 0.01) { // 1% chance of jump
                returns[i] += (rand.nextBoolean() ? 1 : -1) * 0.05; // ±5% jump
            }
        }
        
        return returns;
    }
    
    private static double compareResults(CWTResult result1, CWTResult result2) {
        double maxDiff = 0.0;
        double[][] coeffs1 = result1.getCoefficients();
        double[][] coeffs2 = result2.getCoefficients();
        
        for (int i = 0; i < coeffs1.length; i++) {
            for (int j = 0; j < coeffs1[i].length; j++) {
                double diff = Math.abs(coeffs1[i][j] - coeffs2[i][j]);
                maxDiff = Math.max(maxDiff, diff);
            }
        }
        
        return maxDiff;
    }
    
    private static double computeEnergy(double[] coeffs) {
        double energy = 0.0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
}
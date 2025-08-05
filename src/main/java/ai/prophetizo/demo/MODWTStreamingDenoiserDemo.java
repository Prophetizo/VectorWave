package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdType;
import ai.prophetizo.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import ai.prophetizo.wavelet.modwt.streaming.MODWTStreamingTransform;
import ai.prophetizo.wavelet.modwt.MODWTResult;
import ai.prophetizo.wavelet.api.BoundaryMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates MODWT-based streaming wavelet denoising for real-time applications.
 *
 * <p>This demo showcases:</p>
 * <ul>
 *   <li>Real-time audio signal denoising with MODWT</li>
 *   <li>Financial time series filtering without block artifacts</li>
 *   <li>Shift-invariant denoising</li>
 *   <li>Non-power-of-2 buffer sizes</li>
 *   <li>Performance metrics and latency analysis</li>
 * </ul>
 * 
 * <p>Advantages of MODWT streaming denoiser:</p>
 * <ul>
 *   <li>No blocking artifacts due to shift-invariance</li>
 *   <li>Flexible buffer sizes (not limited to powers of 2)</li>
 *   <li>Better preservation of signal features</li>
 *   <li>Smoother transitions across buffers</li>
 * </ul>
 * 
 */
public class MODWTStreamingDenoiserDemo {

    // Performance simulation constants
    private static final double REALTIME_SPEED_MULTIPLIER = 0.25; // Process 4x faster than real-time
    private static final Random random = new Random(42);

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("     VectorWave MODWT Streaming Denoiser Demo     ");
        System.out.println("==================================================\n");

        demonstrateAudioDenoising();
        System.out.println();

        demonstrateFinancialDataCleaning();
        System.out.println();

        demonstrateShiftInvariantDenoising();
        System.out.println();

        compareBufferSizes();
        System.out.println();

        demonstrateMultiChannelProcessing();
    }

    private static void demonstrateAudioDenoising() throws Exception {
        System.out.println("1. Real-Time Audio Denoising with MODWT");
        System.out.println("---------------------------------------");

        // Simulate 48kHz audio with 10ms blocks (480 samples - not power of 2!)
        int sampleRate = 48000;
        int blockSize = 480;  // Exactly 10ms at 48kHz
        double blockDuration = blockSize * 1000.0 / sampleRate;

        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .bufferSize(blockSize)
                .thresholdType(ThresholdType.SOFT)
                .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
                .thresholdMultiplier(1.0)
                .noiseWindowSize(2048)
                .build();

        System.out.print("Configuration:\n");
        System.out.printf("  Sample rate: %d Hz\n", sampleRate);
        System.out.printf("  Block size: %d samples (%.1f ms)\n", blockSize, blockDuration);
        System.out.print("  Wavelet: Daubechies DB4\n");
        System.out.print("  Threshold: Soft with MAD noise estimation\n\n");

        // Simulate noisy audio stream
        int numBlocks = 100;
        double totalLatency = 0;
        int processedSamples = 0;

        System.out.println("Processing " + numBlocks + " blocks of audio...");

        for (int block = 0; block < numBlocks; block++) {
            // Generate noisy audio block
            double[] cleanAudio = new double[blockSize];
            double[] noisyAudio = new double[blockSize];
            
            for (int i = 0; i < blockSize; i++) {
                // Sine wave at 1kHz
                cleanAudio[i] = 0.5 * Math.sin(2 * Math.PI * 1000 * (block * blockSize + i) / sampleRate);
                // Add white noise
                noisyAudio[i] = cleanAudio[i] + 0.1 * random.nextGaussian();
            }

            // Process block
            long startTime = System.nanoTime();
            double[] denoisedAudio = denoiser.denoise(noisyAudio);
            long processingTime = System.nanoTime() - startTime;
            
            totalLatency += processingTime / 1_000_000.0; // Convert to ms
            processedSamples += blockSize;

            // Show progress every 10 blocks
            if ((block + 1) % 10 == 0) {
                System.out.printf("  Processed %d blocks, avg latency: %.2f ms\n", 
                                block + 1, totalLatency / (block + 1));
            }
        }

        double avgLatency = totalLatency / numBlocks;
        System.out.printf("\nResults:\n");
        System.out.printf("  Total samples: %d\n", processedSamples);
        System.out.printf("  Average latency: %.2f ms (%.1fx real-time)\n", 
                        avgLatency, blockDuration / avgLatency);
        System.out.printf("  Estimated noise level: %.4f\n", denoiser.getEstimatedNoiseLevel());
        
        denoiser.close();
    }

    private static void demonstrateFinancialDataCleaning() throws Exception {
        System.out.println("2. Financial Data Cleaning with MODWT");
        System.out.println("-------------------------------------");

        // Simulate tick data with microstructure noise
        int ticksPerMinute = 300;  // High-frequency data
        int bufferSize = ticksPerMinute;  // Process 1 minute at a time

        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(new Haar())
                .bufferSize(bufferSize)
                .thresholdType(ThresholdType.SOFT)
                .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
                .thresholdMultiplier(0.8)  // Less aggressive for financial data
                .build();

        System.out.println("Configuration:");
        System.out.println("  Buffer size: " + bufferSize + " ticks (1 minute)");
        System.out.println("  Wavelet: Haar (good for jumps)");
        System.out.println("  Threshold: Soft with 0.8x multiplier\n");

        // Simulate 10 minutes of data
        double basePrice = 100.0;
        double[] prices = new double[ticksPerMinute];
        
        for (int minute = 0; minute < 10; minute++) {
            // Generate price data with microstructure noise
            for (int tick = 0; tick < ticksPerMinute; tick++) {
                double trend = 0.001 * tick / ticksPerMinute;  // Slight upward trend
                double microNoise = 0.005 * random.nextGaussian();  // Microstructure noise
                double jump = (random.nextDouble() < 0.01) ? 0.1 * random.nextGaussian() : 0;  // Rare jumps
                
                prices[tick] = basePrice + trend + microNoise + jump;
            }
            
            // Denoise the price data
            double[] cleanedPrices = denoiser.denoise(prices);
            
            // Calculate noise reduction
            double noiseReduction = calculateNoiseReduction(prices, cleanedPrices);
            System.out.printf("Minute %d: Noise reduced by %.1f%%\n", minute + 1, noiseReduction * 100);
            
            // Update base price for next minute
            basePrice = cleanedPrices[cleanedPrices.length - 1];
        }
        
        denoiser.close();
    }

    private static void demonstrateShiftInvariantDenoising() throws Exception {
        System.out.println("3. Shift-Invariant Denoising Demonstration");
        System.out.println("------------------------------------------");

        int signalLength = 500;
        int shifts = 5;
        
        // Create a test signal with a spike
        double[] originalSignal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            originalSignal[i] = Math.sin(2 * Math.PI * i / 50.0);
            if (i == 250) originalSignal[i] += 2.0;  // Add spike
        }
        
        // Add noise
        double[] noisySignal = originalSignal.clone();
        for (int i = 0; i < signalLength; i++) {
            noisySignal[i] += 0.2 * random.nextGaussian();
        }
        
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .bufferSize(signalLength)
                .thresholdType(ThresholdType.SOFT)
                .build();
        
        System.out.println("Testing shift-invariance with " + shifts + " different shifts:");
        
        double[] denoisedVariances = new double[shifts];
        
        for (int shift = 0; shift < shifts; shift++) {
            // Create shifted version
            double[] shiftedSignal = new double[signalLength];
            for (int i = 0; i < signalLength; i++) {
                shiftedSignal[i] = noisySignal[(i - shift + signalLength) % signalLength];
            }
            
            // Denoise
            double[] denoised = denoiser.denoise(shiftedSignal);
            
            // Shift back
            double[] denoisedShiftedBack = new double[signalLength];
            for (int i = 0; i < signalLength; i++) {
                denoisedShiftedBack[i] = denoised[(i + shift) % signalLength];
            }
            
            // Calculate variance of denoised signal
            double variance = calculateVariance(denoisedShiftedBack);
            denoisedVariances[shift] = variance;
            
            System.out.printf("  Shift %d: Denoised signal variance = %.6f\n", shift, variance);
        }
        
        // Check consistency
        double maxVarianceDiff = 0;
        for (int i = 1; i < shifts; i++) {
            maxVarianceDiff = Math.max(maxVarianceDiff, 
                Math.abs(denoisedVariances[i] - denoisedVariances[0]));
        }
        
        System.out.printf("\nMax variance difference: %.6f (should be small for shift-invariance)\n", 
                        maxVarianceDiff);
        
        denoiser.close();
    }

    private static void compareBufferSizes() throws Exception {
        System.out.println("4. Buffer Size Comparison");
        System.out.println("-------------------------");

        int[] bufferSizes = {100, 200, 300, 400, 500, 1000};
        int signalLength = 10000;
        
        System.out.println("Processing " + signalLength + " samples with different buffer sizes:");
        System.out.println("\nBuffer Size | Processing Time | Throughput");
        System.out.println("------------|-----------------|------------");
        
        for (int bufferSize : bufferSizes) {
            MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                    .wavelet(new Haar())
                    .bufferSize(bufferSize)
                    .build();
            
            // Generate test signal
            double[] signal = new double[signalLength];
            for (int i = 0; i < signalLength; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 100.0) + 0.3 * random.nextGaussian();
            }
            
            // Process and time
            long startTime = System.nanoTime();
            
            for (int i = 0; i < signalLength; i += bufferSize) {
                int chunkSize = Math.min(bufferSize, signalLength - i);
                double[] chunk = new double[chunkSize];
                System.arraycopy(signal, i, chunk, 0, chunkSize);
                denoiser.denoise(chunk);
            }
            
            long elapsedTime = System.nanoTime() - startTime;
            double elapsedMs = elapsedTime / 1_000_000.0;
            double throughput = signalLength / (elapsedTime / 1e9);
            
            System.out.printf("%11d | %14.2f ms | %10.0f samples/s\n", 
                            bufferSize, elapsedMs, throughput);
            
            denoiser.close();
        }
    }

    private static void demonstrateMultiChannelProcessing() throws Exception {
        System.out.println("5. Multi-Channel Processing");
        System.out.println("---------------------------");

        int numChannels = 8;
        int bufferSize = 256;
        int numBuffers = 50;
        
        System.out.println("Processing " + numChannels + " channels simultaneously");
        System.out.println("Buffer size: " + bufferSize + " samples per channel");
        
        // Create denoisers for each channel
        MODWTStreamingDenoiser[] denoisers = new MODWTStreamingDenoiser[numChannels];
        for (int ch = 0; ch < numChannels; ch++) {
            denoisers[ch] = new MODWTStreamingDenoiser.Builder()
                    .wavelet(Daubechies.DB2)
                    .bufferSize(bufferSize)
                    .build();
        }
        
        long totalTime = 0;
        
        for (int buffer = 0; buffer < numBuffers; buffer++) {
            // Generate multi-channel data
            double[][] multiChannelData = new double[numChannels][bufferSize];
            for (int ch = 0; ch < numChannels; ch++) {
                for (int i = 0; i < bufferSize; i++) {
                    // Different frequency per channel
                    double freq = 100 * (ch + 1);
                    multiChannelData[ch][i] = Math.sin(2 * Math.PI * freq * i / 8000.0) + 
                                            0.2 * random.nextGaussian();
                }
            }
            
            // Process all channels
            long startTime = System.nanoTime();
            
            for (int ch = 0; ch < numChannels; ch++) {
                denoisers[ch].denoise(multiChannelData[ch]);
            }
            
            totalTime += System.nanoTime() - startTime;
        }
        
        double avgTimeMs = (totalTime / numBuffers) / 1_000_000.0;
        double samplesPerSecond = (numChannels * bufferSize) / (avgTimeMs / 1000.0);
        
        System.out.printf("\nResults:\n");
        System.out.printf("  Average processing time per buffer: %.2f ms\n", avgTimeMs);
        System.out.printf("  Total throughput: %.0f samples/second\n", samplesPerSecond);
        System.out.printf("  Per-channel throughput: %.0f samples/second\n", samplesPerSecond / numChannels);
        
        // Clean up
        for (MODWTStreamingDenoiser denoiser : denoisers) {
            denoiser.close();
        }
    }
    
    // Helper methods
    
    private static double calculateNoiseReduction(double[] noisy, double[] denoised) {
        double noisyVar = calculateVariance(noisy);
        double denoisedVar = calculateVariance(denoised);
        return 1.0 - (denoisedVar / noisyVar);
    }
    
    private static double calculateVariance(double[] signal) {
        double mean = 0;
        for (double v : signal) mean += v;
        mean /= signal.length;
        
        double variance = 0;
        for (double v : signal) {
            double diff = v - mean;
            variance += diff * diff;
        }
        return variance / signal.length;
    }
}
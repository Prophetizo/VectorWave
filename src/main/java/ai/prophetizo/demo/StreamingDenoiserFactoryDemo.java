package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.Daubechies;
import ai.prophetizo.wavelet.api.Factory;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserConfig;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserFactory;
import ai.prophetizo.wavelet.streaming.StreamingDenoiserStrategy;

/**
 * Demonstrates the use of StreamingDenoiserFactory with dependency injection.
 * <p>
 * Shows how to select between Fast and Quality implementations based on
 * specific use case requirements.
 * <p>
 * <b>Updated:</b> Now also demonstrates the new common Factory interface pattern.
 */
public class StreamingDenoiserFactoryDemo {

    public static void main(String[] args) {
        System.out.println("=== Streaming Denoiser Factory Demo ===\n");

        // Common configuration
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(256)
                .overlapFactor(0.5)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .build();

        demonstrateFastImplementation(config);
        demonstrateQualityImplementation(config);
        demonstrateAutoSelection(config);
        demonstrateNewFactoryInterface(config);
    }

    private static void demonstrateFastImplementation(StreamingDenoiserConfig config) {
        System.out.println("1. Fast Implementation (Real-time Processing)");
        System.out.println("---------------------------------------------");

        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.FAST, config)) {

            StreamingDenoiserStrategy.PerformanceProfile profile = denoiser.getPerformanceProfile();

            System.out.printf("Expected latency: %.2f µs/sample\n", profile.expectedLatencyMicros());
            System.out.printf("Expected SNR improvement: %.1f dB\n", profile.expectedSNRImprovement());
            System.out.printf("Memory usage: %.1f KB\n", profile.memoryUsageBytes() / 1024.0);
            System.out.printf("Real-time capable: %s\n", profile.isRealTimeCapable());

            // Process some samples
            double[] testSignal = generateTestSignal(1024);
            denoiser.process(testSignal);

            System.out.println("\nProcessing complete!");
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateQualityImplementation(StreamingDenoiserConfig config) {
        System.out.println("2. Quality Implementation (Better Denoising)");
        System.out.println("--------------------------------------------");

        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.QUALITY, config)) {

            StreamingDenoiserStrategy.PerformanceProfile profile = denoiser.getPerformanceProfile();

            System.out.printf("Expected latency: %.2f µs/sample\n", profile.expectedLatencyMicros());
            System.out.printf("Expected SNR improvement: %.1f dB\n", profile.expectedSNRImprovement());
            System.out.printf("Memory usage: %.1f KB\n", profile.memoryUsageBytes() / 1024.0);
            System.out.printf("Real-time capable: %s\n", profile.isRealTimeCapable());

            // Process some samples
            double[] testSignal = generateTestSignal(1024);
            denoiser.process(testSignal);

            System.out.println("\nProcessing complete!");
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateAutoSelection(StreamingDenoiserConfig config) {
        System.out.println("3. Automatic Implementation Selection");
        System.out.println("-------------------------------------");

        // Test different configurations
        StreamingDenoiserConfig[] configs = {
                // Small block size -> FAST
                new StreamingDenoiserConfig.Builder()
                        .wavelet(Daubechies.DB4)
                        .blockSize(128)
                        .overlapFactor(0.5)
                        .build(),

                // No overlap -> QUALITY
                new StreamingDenoiserConfig.Builder()
                        .wavelet(Daubechies.DB4)
                        .blockSize(512)
                        .overlapFactor(0.0)
                        .build(),

                // Overlap with adaptive threshold -> FAST
                new StreamingDenoiserConfig.Builder()
                        .wavelet(Daubechies.DB4)
                        .blockSize(256)
                        .overlapFactor(0.5)
                        .adaptiveThreshold(true)
                        .build()
        };

        for (int i = 0; i < configs.length; i++) {
            try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(configs[i])) {
                System.out.printf("Config %d: blockSize=%d, overlap=%.0f%%, adaptive=%s\n",
                        i + 1,
                        configs[i].getBlockSize(),
                        configs[i].getOverlapFactor() * 100,
                        configs[i].isAdaptiveThreshold());

                // Check which implementation was selected
                String implName = denoiser.getClass().getSimpleName();
                System.out.printf("Selected: %s\n\n", implName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void demonstrateNewFactoryInterface(StreamingDenoiserConfig config) {
        System.out.println("4. New Factory Interface Pattern");
        System.out.println("---------------------------------");
        System.out.println("Demonstrates the new standardized Factory interface:\n");
        
        // Get the factory instance that implements Factory<T, C>
        Factory<StreamingDenoiserStrategy, StreamingDenoiserConfig> factory = 
            StreamingDenoiserFactory.getInstance();
        
        System.out.println("Factory Information:");
        System.out.println("  Description: " + factory.getDescription());
        System.out.println("  Config valid: " + factory.isValidConfiguration(config));
        System.out.println();
        
        // Create denoiser using the common interface
        try (StreamingDenoiserStrategy denoiser = factory.create(config)) {
            System.out.println("Created denoiser using Factory interface");
            
            // Show that it works the same way
            StreamingDenoiserStrategy.PerformanceProfile profile = denoiser.getPerformanceProfile();
            System.out.printf("  Latency: %.2f µs/sample\n", profile.expectedLatencyMicros());
            System.out.printf("  Real-time capable: %s\n", profile.isRealTimeCapable());
            
            // Process test signal
            double[] signal = generateTestSignal(256);
            denoiser.process(signal);
            System.out.println("  Processed test signal successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("\nBenefits of using the Factory interface:");
        System.out.println("  - Consistent API across all factories");
        System.out.println("  - Built-in validation support");
        System.out.println("  - Easy integration with dependency injection");
        System.out.println("  - Type-safe factory handling");
    }

    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Clean signal + noise
            signal[i] = Math.sin(2 * Math.PI * i * 5 / length) +
                    0.3 * Math.random();
        }
        return signal;
    }
}
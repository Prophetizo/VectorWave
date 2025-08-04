package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import static ai.prophetizo.wavelet.api.Daubechies.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.modwt.streaming.*;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;

/**
 * Demonstrates the factory pattern implementations in VectorWave with MODWT.
 * 
 * <p>This demo shows:</p>
 * <ul>
 *   <li>How to use the common Factory interface</li>
 *   <li>MODWT factory usage patterns</li>
 *   <li>Different factory implementations</li>
 *   <li>Benefits of the standardized approach</li>
 * </ul>
 */
public class FactoryPatternDemo {
    
    public static void main(String[] args) {
        System.out.println("=== VectorWave Factory Pattern Demo ===\n");
        
        // WaveletOpsFactory removed - using WaveletOperations facade instead
        // demonstrateWaveletOpsFactory();
        demonstrateWaveletTransformFactory();
        demonstrateCWTFactory();
        demonstrateStreamingDenoiserFactory();
        demonstratePolymorphicUsage();
    }
    
    /**
     * Demonstrates WaveletOpsFactory with both old and new patterns.
     * NOTE: WaveletOpsFactory has been removed in favor of the WaveletOperations facade.
     * This method is kept for historical reference but commented out.
     */
    /*
    private static void demonstrateWaveletOpsFactory() {
        System.out.println("1. WaveletOpsFactory Demo");
        System.out.println("-".repeat(50));
        
        // Traditional static approach
        System.out.println("Traditional static approach:");
        WaveletOpsFactory.WaveletOps ops1 = WaveletOpsFactory.createOptimal();
        System.out.println("  Created: " + ops1.getImplementationType());
        
        // New standardized approach using Factory interface
        System.out.println("\nNew Factory interface approach:");
        Factory<WaveletOpsFactory.WaveletOps, TransformConfig> factory = 
            WaveletOpsFactory.getInstance();
        
        // Create with default config
        WaveletOpsFactory.WaveletOps ops2 = factory.create();
        System.out.println("  Default: " + ops2.getImplementationType());
        
        // Create with custom config
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        WaveletOpsFactory.WaveletOps ops3 = factory.create(scalarConfig);
        System.out.println("  Scalar forced: " + ops3.getImplementationType());
        
        // Use factory features
        System.out.println("\nFactory features:");
        System.out.println("  Valid config: " + factory.isValidConfiguration(scalarConfig));
        System.out.println("  Description: " + factory.getDescription());
        
        System.out.println();
    }
    */
    
    /**
     * Demonstrates MODWTTransformFactory which implements Factory interface.
     */
    private static void demonstrateWaveletTransformFactory() {
        System.out.println("2. MODWTTransformFactory Demo");
        System.out.println("-".repeat(50));
        
        // Create MODWT factory instance
        MODWTTransformFactory factory = new MODWTTransformFactory();
        
        // Traditional static approach
        System.out.println("Static factory methods:");
        MODWTTransform transform1 = MODWTTransformFactory.create(new Haar());
        System.out.println("  Created MODWT with Haar wavelet");
        
        MODWTTransform transform2 = MODWTTransformFactory.create(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        System.out.println("  Created MODWT with DB4 and PERIODIC boundary");
        
        // Create from wavelet name
        MODWTTransform transform3 = MODWTTransformFactory.create("sym4");
        System.out.println("  Created MODWT from wavelet name 'sym4'");
        
        // Using Factory interface
        System.out.println("\nFactory interface approach:");
        MODWTTransformFactory.Config config = new MODWTTransformFactory.Config(
            DB4, BoundaryMode.ZERO_PADDING);
        MODWTTransform transform4 = factory.create(config);
        System.out.println("  Created with custom config (DB4, ZERO_PADDING)");
        
        // Multi-level MODWT
        System.out.println("\nMulti-level MODWT:");
        MultiLevelMODWTTransform mlTransform = MODWTTransformFactory.createMultiLevel(
            new Haar(), BoundaryMode.PERIODIC);
        System.out.println("  Created multi-level MODWT transform");
        
        // Validation
        System.out.println("\nValidation:");
        System.out.println("  Valid config: " + factory.isValidConfiguration(config));
        System.out.println("  Null config: " + factory.isValidConfiguration(null));
        
        System.out.println();
    }
    
    /**
     * Demonstrates CWTFactory with its builder pattern.
     */
    private static void demonstrateCWTFactory() {
        System.out.println("3. CWTFactory Demo");
        System.out.println("-".repeat(50));
        
        MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
        
        // Traditional static methods
        System.out.println("Traditional static methods:");
        CWTTransform cwt1 = CWTFactory.create(morlet);
        System.out.println("  Created basic CWT");
        
        CWTTransform cwt2 = CWTFactory.createForRealTime(morlet);
        System.out.println("  Created real-time optimized CWT");
        
        // New Factory interface approach
        System.out.println("\nFactory interface approach:");
        Factory<CWTTransform, ContinuousWavelet> factory = CWTFactory.getInstance();
        
        CWTTransform cwt3 = factory.create(morlet);
        System.out.println("  Created via Factory interface");
        
        // Note: create() without wavelet is not supported
        try {
            factory.create();
        } catch (UnsupportedOperationException e) {
            System.out.println("  create() without wavelet: " + e.getMessage());
        }
        
        // Builder pattern still works
        System.out.println("\nBuilder pattern:");
        CWTTransform cwt4 = CWTFactory.builder()
            .wavelet(morlet)
            .enableFFT(true)
            .normalizeScales(true)
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .build();
        System.out.println("  Created with builder pattern");
        
        System.out.println();
    }
    
    /**
     * Demonstrates MODWT Streaming Denoiser creation and usage.
     */
    private static void demonstrateStreamingDenoiserFactory() {
        System.out.println("4. MODWT Streaming Denoiser Demo");
        System.out.println("-".repeat(50));
        
        // Create basic MODWT streaming denoiser
        System.out.println("Basic MODWT streaming denoiser:");
        MODWTStreamingDenoiser denoiser1 = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(256)
            .build();
        System.out.println("  Created with Haar wavelet, buffer size 256");
        
        // Create with custom configuration
        System.out.println("\nAdvanced configuration:");
        MODWTStreamingDenoiser denoiser2 = new MODWTStreamingDenoiser.Builder()
            .wavelet(DB4)
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .bufferSize(512)
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        System.out.println("  Created with DB4, UNIVERSAL threshold, buffer size 512");
        
        // Subscribe to denoised output
        System.out.println("\nSubscription example:");
        denoiser1.subscribe(new java.util.concurrent.Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                System.out.println("  Subscribed to denoised stream");
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                System.out.println("  Received denoised block of size: " + item.length);
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("  Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("  Stream complete");
            }
        });
        
        // Process some sample data
        System.out.println("\nProcessing sample data:");
        double[] testData = new double[128];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = Math.sin(2 * Math.PI * i / 32) + 0.1 * Math.random();
        }
        double[] denoisedData = denoiser1.denoise(testData);
        System.out.println("  Denoised " + testData.length + " samples");
        
        // Cleanup
        denoiser1.close();
        denoiser2.close();
        
        System.out.println();
    }
    
    /**
     * Demonstrates polymorphic usage of Factory interface.
     */
    private static void demonstratePolymorphicUsage() {
        System.out.println("5. Polymorphic Factory Usage");
        System.out.println("-".repeat(50));
        
        // All factories can be treated uniformly
        System.out.println("Treating different factories polymorphically:\n");
        
        // Create a method that works with any factory
        // WaveletOpsFactory removed - example commented out
        // processWithFactory(WaveletOpsFactory.getInstance(), 
        //     TransformConfig.defaultConfig(), "WaveletOps");
        
        processWithFactory(new MODWTTransformFactory(), 
            new MODWTTransformFactory.Config(new Haar(), BoundaryMode.PERIODIC), "MODWTTransform");
        
        processWithFactory(CWTFactory.getInstance(), 
            new MorletWavelet(), "CWTTransform");
        
        System.out.println("\nBenefits of common interface:");
        System.out.println("  - Consistent API across all factories");
        System.out.println("  - Easy to swap implementations");
        System.out.println("  - Supports dependency injection");
        System.out.println("  - Enables generic factory handling");
        System.out.println("  - Facilitates testing with mock factories");
    }
    
    /**
     * Generic method that works with any Factory implementation.
     */
    private static <T, C> void processWithFactory(Factory<T, C> factory, C config, String type) {
        System.out.println("Processing " + type + " factory:");
        System.out.println("  Description: " + factory.getDescription());
        System.out.println("  Config valid: " + factory.isValidConfiguration(config));
        
        T instance = factory.create(config);
        System.out.println("  Created instance: " + instance.getClass().getSimpleName());
        System.out.println();
    }
}
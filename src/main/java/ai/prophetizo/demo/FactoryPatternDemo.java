package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.cwt.*;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.streaming.*;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;

/**
 * Demonstrates the new factory pattern implementations in VectorWave.
 * 
 * <p>This demo shows:</p>
 * <ul>
 *   <li>How to use the common Factory interface</li>
 *   <li>Comparison between old static methods and new patterns</li>
 *   <li>Different factory implementations</li>
 *   <li>Benefits of the standardized approach</li>
 * </ul>
 */
public class FactoryPatternDemo {
    
    /* TODO: This demo needs to be migrated to MODWT.
     * The demo uses DWT-specific features that need careful adaptation:
     * - Factory patterns (MODWT uses direct instantiation)
     * - FFM features (needs MODWT-specific FFM implementation)
     * - Streaming features (needs MODWT streaming implementation)
     * Temporarily disabled to allow compilation.
     */
    public static void main_disabled(String[] args) {
        System.out.println("This demo is temporarily disabled during DWT to MODWT migration.");
        System.out.println("Please check back later or contribute to the migration effort!");
    }
    
    public static void main_original(String[] args) {
        System.out.println("=== VectorWave Factory Pattern Demo ===\n");
        
        demonstrateWaveletOpsFactory();
        demonstrateWaveletTransformFactory();
        demonstrateCWTFactory();
        demonstrateStreamingDenoiserFactory();
        demonstratePolymorphicUsage();
    }
    
    /**
     * Demonstrates WaveletOpsFactory with both old and new patterns.
     */
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
    
    /**
     * Demonstrates WaveletTransformFactory which directly implements Factory.
     */
    private static void demonstrateWaveletTransformFactory() {
        System.out.println("2. WaveletTransformFactory Demo");
        System.out.println("-".repeat(50));
        
        // Create factory instance (implements Factory<WaveletTransform, Wavelet>)
        WaveletTransformFactory factory = new WaveletTransformFactory();
        
        // Configure factory
        factory.boundaryMode(BoundaryMode.PERIODIC);
        
        // Traditional approach
        System.out.println("Traditional approach:");
        WaveletTransform transform1 = factory.create(new Haar());
        System.out.println("  Created transform with Haar wavelet");
        
        // Using Factory interface methods
        System.out.println("\nFactory interface approach:");
        WaveletTransform transform2 = factory.create(); // Uses default Haar
        System.out.println("  Created with default wavelet");
        
        // Create with different wavelets
        WaveletTransform transform3 = factory.create(Daubechies.DB4);
        System.out.println("  Created with Daubechies DB4");
        
        // Validation
        System.out.println("\nValidation:");
        System.out.println("  Valid wavelet: " + factory.isValidConfiguration(new Haar()));
        System.out.println("  Null wavelet: " + factory.isValidConfiguration(null));
        
        // Static convenience method still works
        WaveletTransform transform4 = WaveletTransformFactory.createDefault(Symlet.SYM4);
        System.out.println("\nStatic method still works: created with Symlet SYM4");
        
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
     * Demonstrates StreamingDenoiserFactory with implementation selection.
     */
    private static void demonstrateStreamingDenoiserFactory() {
        System.out.println("4. StreamingDenoiserFactory Demo");
        System.out.println("-".repeat(50));
        
        // Create configuration
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(new Haar())
            .blockSize(256)
            .overlapFactor(0.5)
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .adaptiveThreshold(true)
            .build();
        
        // Traditional approach with explicit implementation
        System.out.println("Traditional approach:");
        StreamingDenoiserStrategy denoiser1 = StreamingDenoiserFactory.create(
            StreamingDenoiserFactory.Implementation.FAST, config);
        System.out.println("  Created FAST implementation");
        System.out.println("  Performance: " + denoiser1.getPerformanceProfile());
        
        // Auto selection
        StreamingDenoiserStrategy denoiser2 = StreamingDenoiserFactory.create(config);
        System.out.println("\n  Auto-selected implementation based on config");
        
        // New Factory interface approach
        System.out.println("\nFactory interface approach:");
        Factory<StreamingDenoiserStrategy, StreamingDenoiserConfig> factory = 
            StreamingDenoiserFactory.getInstance();
        
        StreamingDenoiserStrategy denoiser3 = factory.create(config);
        System.out.println("  Created via Factory interface");
        System.out.println("  Description: " + factory.getDescription());
        
        // Performance profiling
        System.out.println("\nPerformance profiling:");
        var profile = StreamingDenoiserFactory.getExpectedPerformance(
            StreamingDenoiserFactory.Implementation.QUALITY, config);
        System.out.println("  Quality implementation expected performance:");
        System.out.println("    " + profile);
        
        // Cleanup
        try {
            denoiser1.close();
            denoiser2.close();
            denoiser3.close();
        } catch (Exception e) {
            // Ignore
        }
        
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
        processWithFactory(WaveletOpsFactory.getInstance(), 
            TransformConfig.defaultConfig(), "WaveletOps");
        
        processWithFactory(new WaveletTransformFactory(), 
            new Haar(), "WaveletTransform");
        
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
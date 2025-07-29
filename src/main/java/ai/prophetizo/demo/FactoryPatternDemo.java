package ai.prophetizo.demo;

import ai.prophetizo.wavelet.WaveletTransform;
import ai.prophetizo.wavelet.WaveletTransformFactory;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.cwt.CWTFactory;
import ai.prophetizo.wavelet.cwt.optimization.FFTAcceleratedCWT;
import ai.prophetizo.wavelet.ops.WaveletOpsFactory;
import ai.prophetizo.wavelet.ops.WaveletOpsConfig;

import java.util.Collection;
import java.util.Optional;

/**
 * Demonstration of the Factory Pattern implementation in VectorWave.
 * 
 * This demo showcases the common factory interface, factory registry,
 * and various factory implementations available in the library.
 */
public class FactoryPatternDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave Factory Pattern Demo");
        System.out.println("================================");
        System.out.println();
        
        demonstrateFactoryRegistry();
        System.out.println();
        
        demonstrateWaveletTransformFactory();
        System.out.println();
        
        demonstrateCWTFactory();
        System.out.println();
        
        demonstrateWaveletOpsFactory();
    }
    
    private static void demonstrateFactoryRegistry() {
        System.out.println("FACTORY REGISTRY");
        System.out.println("================");
        
        // Show all registered factories
        System.out.println("Available factories:");
        Collection<Factory<?>> allFactories = FactoryRegistry.getAllFactories();
        for (Factory<?> factory : allFactories) {
            System.out.println("  - " + factory.getClass().getSimpleName() + 
                             ": " + factory.getDescription());
        }
        
        System.out.println("\nFactory names: " + FactoryRegistry.getFactoryNames());
        System.out.println("Total factories: " + FactoryRegistry.getFactoryCount());
        
        // Demonstrate factory retrieval
        Optional<Factory<?>> cwtFactory = FactoryRegistry.getFactory("CWT");
        if (cwtFactory.isPresent()) {
            System.out.println("Found CWT factory: " + cwtFactory.get().getDescription());
        }
        
        // Demonstrate typed factory retrieval
        Optional<Factory<WaveletTransform>> transformFactory = 
            FactoryRegistry.getFactory("WaveletTransform", WaveletTransform.class);
        if (transformFactory.isPresent()) {
            System.out.println("Found typed WaveletTransform factory");
        }
    }
    
    private static void demonstrateWaveletTransformFactory() {
        System.out.println("WAVELET TRANSFORM FACTORY");
        System.out.println("=========================");
        
        // Basic usage with fluent API
        WaveletTransformFactory factory = new WaveletTransformFactory();
        System.out.println("WaveletTransformFactory demonstration");
        System.out.println("Note: This factory requires a wavelet parameter");
        
        // Create transform with PERIODIC boundary
        WaveletTransform transform1 = factory
            .boundaryMode(BoundaryMode.PERIODIC)
            .create(new Haar());
        
        // Create transform with ZERO_PADDING boundary
        WaveletTransform transform2 = factory
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .create(new Haar());
        
        // Static convenience method
        WaveletTransform transform3 = WaveletTransformFactory.createDefault(new Haar());
        
        System.out.println("\nCreated transforms with different boundary modes:");
        System.out.println("  - Transform 1 boundary: " + transform1.getBoundaryMode());
        System.out.println("  - Transform 2 boundary: " + transform2.getBoundaryMode());
        System.out.println("  - Transform 3 boundary: " + transform3.getBoundaryMode());
    }
    
    private static void demonstrateCWTFactory() {
        System.out.println("CWT FACTORY");
        System.out.println("===========");
        
        CWTFactory factory = new CWTFactory();
        System.out.println("Factory description: " + factory.getDescription());
        System.out.println("Product type: " + factory.getProductType().getSimpleName());
        
        // Create with default optimizations
        FFTAcceleratedCWT cwt1 = factory.create();
        System.out.println("Created FFTAcceleratedCWT with optimizations: " + 
                         factory.isOptimizationsEnabled());
        
        // Create with disabled optimizations
        FFTAcceleratedCWT cwt2 = factory
            .withOptimizations(false)
            .create();
        System.out.println("Created FFTAcceleratedCWT with optimizations: " + 
                         factory.isOptimizationsEnabled());
        
        // Static convenience method
        FFTAcceleratedCWT cwt3 = CWTFactory.createDefault();
        System.out.println("Created FFTAcceleratedCWT using createDefault()");
        
        // Verify different instances
        System.out.println("Different instances created: " + 
                         (cwt1 != cwt2 && cwt2 != cwt3 && cwt1 != cwt3));
    }
    
    private static void demonstrateWaveletOpsFactory() {
        System.out.println("WAVELET OPS FACTORY");
        System.out.println("===================");
        
        WaveletOpsFactory factory = new WaveletOpsFactory();
        System.out.println("Factory description: " + factory.getDescription());
        System.out.println("Product type: " + factory.getProductType().getSimpleName());
        
        // Create with default configuration
        WaveletOpsConfig config1 = factory.create();
        System.out.println("Default configuration:");
        System.out.println("  - Boundary mode: " + config1.getBoundaryMode());
        System.out.println("  - Optimization level: " + config1.getOptimizationLevel());
        System.out.println("  - Vectorization enabled: " + config1.isVectorizationEnabled());
        System.out.println("  - Is periodic boundary: " + config1.isPeriodicBoundary());
        System.out.println("  - Is aggressive optimization: " + config1.isAggressiveOptimization());
        
        // Create with custom configuration
        WaveletOpsConfig config2 = factory
            .withBoundaryMode(BoundaryMode.ZERO_PADDING)
            .withOptimizationLevel(WaveletOpsFactory.OptimizationLevel.AGGRESSIVE)
            .withVectorization(false)
            .create();
        
        System.out.println("\nCustom configuration:");
        System.out.println("  - Boundary mode: " + config2.getBoundaryMode());
        System.out.println("  - Optimization level: " + config2.getOptimizationLevel());
        System.out.println("  - Vectorization enabled: " + config2.isVectorizationEnabled());
        System.out.println("  - Is periodic boundary: " + config2.isPeriodicBoundary());
        System.out.println("  - Is aggressive optimization: " + config2.isAggressiveOptimization());
        
        // Static convenience method
        WaveletOpsConfig config3 = WaveletOpsFactory.createDefault();
        System.out.println("\nStatic createDefault() works: " + (config3 != null));
        
        // Show factory current settings
        System.out.println("\nFactory current settings:");
        System.out.println("  - Boundary mode: " + factory.getBoundaryMode());
        System.out.println("  - Optimization level: " + factory.getOptimizationLevel());
        System.out.println("  - Vectorization enabled: " + factory.isVectorizationEnabled());
        
        // Demonstrate immutability - configs are equal but different instances
        System.out.println("\nConfigs 1 and 3 are equal: " + config1.equals(config3));
        System.out.println("But different instances: " + (config1 != config3));
    }
}
package ai.prophetizo.demo;

import ai.prophetizo.wavelet.*;
import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.config.TransformConfig;
import ai.prophetizo.wavelet.cwt.CWTTransform;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import ai.prophetizo.wavelet.streaming.*;
import ai.prophetizo.wavelet.denoising.WaveletDenoiser.ThresholdMethod;

import java.util.Optional;
import java.util.Random;

/**
 * Demonstrates the FactoryRegistry for centralized factory management.
 * 
 * <p>This demo shows:</p>
 * <ul>
 *   <li>How to register and retrieve factories</li>
 *   <li>Benefits of centralized factory management</li>
 *   <li>Dependency injection patterns</li>
 *   <li>Custom factory registration</li>
 * </ul>
 */
public class FactoryRegistryDemo {
    
    public static void main(String[] args) {
        System.out.println("=== VectorWave Factory Registry Demo ===\n");
        
        demonstrateBasicRegistryUsage();
        demonstrateDefaultFactories();
        demonstrateCustomFactoryRegistration();
        demonstrateDependencyInjection();
        demonstratePluginArchitecture();
    }
    
    /**
     * Demonstrates basic registry operations.
     */
    private static void demonstrateBasicRegistryUsage() {
        System.out.println("1. Basic Registry Usage");
        System.out.println("-".repeat(50));
        
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Clear registry to start fresh
        registry.clear();
        System.out.println("Registry cleared");
        
        // Register a factory
        WaveletTransformFactory transformFactory = new WaveletTransformFactory();
        registry.register("myTransformFactory", transformFactory);
        System.out.println("Registered 'myTransformFactory'");
        
        // Check if registered
        System.out.println("Is registered: " + registry.isRegistered("myTransformFactory"));
        
        // Retrieve factory
        Optional<Factory<?, ?>> retrieved = registry.getFactory("myTransformFactory");
        System.out.println("Retrieved: " + retrieved.isPresent());
        
        // Use retrieved factory
        if (retrieved.isPresent()) {
            @SuppressWarnings("unchecked")
            Factory<WaveletTransform, Wavelet> factory = 
                (Factory<WaveletTransform, Wavelet>) retrieved.get();
            WaveletTransform transform = factory.create(new Haar());
            System.out.println("Created transform using retrieved factory");
        }
        
        // List all registered keys
        System.out.println("Registered keys: " + registry.getRegisteredKeys());
        
        System.out.println();
    }
    
    /**
     * Demonstrates usage of default factories.
     */
    private static void demonstrateDefaultFactories() {
        System.out.println("2. Default Factories");
        System.out.println("-".repeat(50));
        
        FactoryRegistry registry = FactoryRegistry.getInstance();
        registry.clear();
        
        // Register all default factories
        FactoryRegistry.registerDefaults();
        System.out.println("Registered default factories");
        
        // List all defaults
        System.out.println("Available factories: " + registry.getRegisteredKeys());
        
        // Use each default factory
        System.out.println("\nUsing default factories:");
        
        // WaveletOps factory
        registry.getFactory("waveletOps", 
                WaveletOpsFactory.WaveletOps.class, 
                TransformConfig.class)
            .ifPresent(factory -> {
                var ops = factory.create();
                System.out.println("  WaveletOps: " + ops.getImplementationType());
            });
        
        // WaveletTransform factory
        registry.getFactory("waveletTransform", 
                WaveletTransform.class, 
                Wavelet.class)
            .ifPresent(factory -> {
                var transform = factory.create(Daubechies.DB4);
                System.out.println("  WaveletTransform: created with DB4");
            });
        
        // CWT factory
        registry.getFactory("cwtTransform", 
                CWTTransform.class, 
                ContinuousWavelet.class)
            .ifPresent(factory -> {
                var cwt = factory.create(new MorletWavelet());
                System.out.println("  CWTTransform: created with Morlet");
            });
        
        // Streaming denoiser factory
        registry.getFactory("streamingDenoiser", 
                StreamingDenoiserStrategy.class, 
                StreamingDenoiserConfig.class)
            .ifPresent(factory -> {
                var config = new StreamingDenoiserConfig.Builder()
                    .wavelet(new Haar())
                    .blockSize(128)
                    .build();
                var denoiser = factory.create(config);
                System.out.println("  StreamingDenoiser: created");
                try { denoiser.close(); } catch (Exception e) {}
            });
        
        System.out.println();
    }
    
    /**
     * Demonstrates registration of custom factories.
     */
    private static void demonstrateCustomFactoryRegistration() {
        System.out.println("3. Custom Factory Registration");
        System.out.println("-".repeat(50));
        
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Create a custom factory for random wavelets
        Factory<Wavelet, Void> randomWaveletFactory = new SimpleFactory<Wavelet>() {
            private final Random random = new Random();
            private final Wavelet[] wavelets = {
                new Haar(),
                Daubechies.DB2,
                Daubechies.DB4,
                Symlet.SYM4,
                Symlet.SYM4,
                Coiflet.COIF1,
                Coiflet.COIF3
            };
            
            @Override
            public Wavelet create() {
                Wavelet selected = wavelets[random.nextInt(wavelets.length)];
                System.out.println("    Random selection: " + selected.getClass().getSimpleName());
                return selected;
            }
            
            @Override
            public String getDescription() {
                return "Random wavelet selector factory";
            }
        };
        
        // Register custom factory
        registry.register("randomWavelet", randomWaveletFactory);
        System.out.println("Registered custom 'randomWavelet' factory");
        System.out.println("Description: " + randomWaveletFactory.getDescription());
        
        // Use custom factory
        System.out.println("\nCreating random wavelets:");
        for (int i = 0; i < 3; i++) {
            registry.getFactory("randomWavelet", Wavelet.class, Void.class)
                .ifPresent(factory -> {
                    Wavelet wavelet = factory.create();
                });
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates dependency injection pattern with registry.
     */
    private static void demonstrateDependencyInjection() {
        System.out.println("4. Dependency Injection Pattern");
        System.out.println("-".repeat(50));
        
        FactoryRegistry registry = FactoryRegistry.getInstance();
        FactoryRegistry.registerDefaults();
        
        // Create a signal processor that uses dependency injection
        SignalProcessor processor = new SignalProcessor(registry);
        
        // Process signal
        double[] signal = generateTestSignal(256);
        processor.processSignal(signal);
        
        System.out.println();
    }
    
    /**
     * Demonstrates plugin architecture possibilities.
     */
    private static void demonstratePluginArchitecture() {
        System.out.println("5. Plugin Architecture Example");
        System.out.println("-".repeat(50));
        
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Simulate loading plugins that register their factories
        System.out.println("Simulating plugin loading...");
        
        // Plugin 1: Custom denoising factory
        loadDenoisingPlugin(registry);
        
        // Plugin 2: Advanced wavelet factory
        loadAdvancedWaveletPlugin(registry);
        
        // List all available factories (including plugins)
        System.out.println("\nAll available factories after plugin loading:");
        for (String key : registry.getRegisteredKeys()) {
            System.out.println("  - " + key);
        }
        
        // Use plugin factories
        System.out.println("\nUsing plugin factories:");
        registry.getFactory("customDenoising")
            .ifPresent(factory -> System.out.println("  Custom denoising factory available"));
        
        registry.getFactory("advancedWavelets")
            .ifPresent(factory -> System.out.println("  Advanced wavelets factory available"));
        
        System.out.println("\nBenefits of registry-based plugin architecture:");
        System.out.println("  - Plugins can register factories without modifying core code");
        System.out.println("  - Dynamic discovery of available implementations");
        System.out.println("  - Clean separation of concerns");
        System.out.println("  - Easy to extend functionality");
    }
    
    /**
     * Example signal processor using dependency injection.
     */
    static class SignalProcessor {
        private final FactoryRegistry registry;
        
        SignalProcessor(FactoryRegistry registry) {
            this.registry = registry;
        }
        
        @SuppressWarnings("try")  // Demo code - InterruptedException handled in catch block
        void processSignal(double[] signal) {
            System.out.println("Processing signal using injected factories:");
            
            // Get transform factory from registry
            registry.getFactory("waveletTransform", WaveletTransform.class, Wavelet.class)
                .ifPresent(factory -> {
                    WaveletTransform transform = factory.create(new Haar());
                    var result = transform.forward(signal);
                    int totalCoeffs = result.approximationCoeffs().length + result.detailCoeffs().length;
                    System.out.println("  - Decomposed signal into " + totalCoeffs + " coefficients");
                });
            
            // Get denoiser factory from registry
            registry.getFactory("streamingDenoiser", 
                    StreamingDenoiserStrategy.class, 
                    StreamingDenoiserConfig.class)
                .ifPresent(factory -> {
                    var config = new StreamingDenoiserConfig.Builder()
                        .wavelet(new Haar())
                        .blockSize(signal.length)
                        .build();
                    try (var denoiser = factory.create(config)) {
                        denoiser.process(signal);
                        System.out.println("  - Applied denoising");
                    } catch (Exception e) {
                        // Handle error
                    }
                });
        }
    }
    
    /**
     * Simulates loading a denoising plugin.
     */
    private static void loadDenoisingPlugin(FactoryRegistry registry) {
        // Plugin registers its custom factory
        registry.register("customDenoising", new SimpleFactory<String>() {
            @Override
            public String create() {
                return "Custom Denoising Implementation";
            }
            
            @Override
            public String getDescription() {
                return "Plugin: Advanced Denoising Factory";
            }
        });
        System.out.println("  Loaded: Denoising Plugin");
    }
    
    /**
     * Simulates loading an advanced wavelets plugin.
     */
    private static void loadAdvancedWaveletPlugin(FactoryRegistry registry) {
        // Plugin registers its custom factory
        registry.register("advancedWavelets", new SimpleFactory<String>() {
            @Override
            public String create() {
                return "Advanced Wavelet Implementation";
            }
            
            @Override
            public String getDescription() {
                return "Plugin: Extended Wavelet Family Factory";
            }
        });
        System.out.println("  Loaded: Advanced Wavelets Plugin");
    }
    
    /**
     * Generates a simple test signal.
     */
    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        return signal;
    }
}
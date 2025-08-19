package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.*;
import ai.prophetizo.wavelet.modwt.*;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.*;

/**
 * Best practices for using the WaveletRegistry with enum-based API.
 * 
 * This demo shows:
 * - How to discover available wavelets using enums
 * - How to check transform compatibility
 * - Type-safe wavelet selection
 * - How to use wavelets in transforms
 * 
 * Run with: mvn exec:java -Dexec.mainClass="ai.prophetizo.demo.WaveletRegistryBestPracticesDemo"
 */
public class WaveletRegistryBestPracticesDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("            WaveletRegistry Best Practices Demo");
        System.out.println("=".repeat(70));
        
        WaveletRegistryBestPracticesDemo demo = new WaveletRegistryBestPracticesDemo();
        demo.demonstrateDiscovery();
        demo.demonstrateSelection();
        demo.demonstrateCompatibility();
        demo.demonstrateTransformUsage();
    }
    
    /**
     * 1. DISCOVERING AVAILABLE WAVELETS
     */
    private void demonstrateDiscovery() {
        System.out.println("\n1. DISCOVERING AVAILABLE WAVELETS");
        System.out.println("-".repeat(40));
        
        // Get all available wavelets using enum-based API
        Set<WaveletName> allWavelets = WaveletRegistry.getAvailableWavelets();
        System.out.println("Total wavelets available: " + allWavelets.size());
        
        // Display first few wavelets
        System.out.println("Sample wavelets:");
        allWavelets.stream().limit(5).forEach(w -> 
            System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        // Get wavelets by type
        List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("\nOrthogonal wavelets (" + orthogonal.size() + " total)");
        
        List<WaveletName> continuous = WaveletRegistry.getContinuousWavelets();
        System.out.println("Continuous wavelets (" + continuous.size() + " total)");
        
        // Check specific wavelet availability (always true with enums!)
        System.out.println("\nEnum-based API guarantees availability:");
        WaveletName[] checkList = {WaveletName.HAAR, WaveletName.DB4, WaveletName.SYM2};
        for (WaveletName name : checkList) {
            boolean available = WaveletRegistry.hasWavelet(name);
            System.out.println("  " + name.getCode() + ": " + 
                (available ? "✓ Available" : "✗ Not available"));
        }
    }
    
    /**
     * 2. SELECTING WAVELETS FOR SPECIFIC TRANSFORMS
     */
    private void demonstrateSelection() {
        System.out.println("\n2. SELECTING WAVELETS FOR TRANSFORMS");
        System.out.println("-".repeat(40));
        
        // Get wavelets compatible with MODWT
        List<WaveletName> modwtWavelets = 
            WaveletRegistry.getWaveletsForTransform(TransformType.MODWT);
        System.out.println("Wavelets compatible with MODWT:");
        modwtWavelets.stream().limit(5).forEach(w -> 
            System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        System.out.println("  ... and " + (modwtWavelets.size() - 5) + " more");
        
        // Get wavelets compatible with CWT
        List<WaveletName> cwtWavelets = 
            WaveletRegistry.getWaveletsForTransform(TransformType.CWT);
        System.out.println("\nWavelets compatible with CWT:");
        cwtWavelets.forEach(w -> 
            System.out.println("  - " + w.getCode() + ": " + w.getDescription()));
        
        // Default selection
        WaveletName defaultWavelet = WaveletName.DB4;
        System.out.println("\nDefault selection: " + defaultWavelet.getCode() + 
            " - " + defaultWavelet.getDescription());
    }
    
    /**
     * 3. TRANSFORM COMPATIBILITY
     */
    private void demonstrateCompatibility() {
        System.out.println("\n3. TRANSFORM COMPATIBILITY");
        System.out.println("-".repeat(40));
        
        // Check what transforms a wavelet supports
        WaveletName wavelet = WaveletName.DB4;
        Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(wavelet);
        System.out.println(wavelet.getCode() + " supports:");
        for (TransformType transform : supported) {
            System.out.println("  ✓ " + transform.getDescription());
        }
        
        // Check specific compatibility
        System.out.println("\nCompatibility checks:");
        System.out.println("  DB4 with MODWT: " + 
            (WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.MODWT) ? "✓" : "✗"));
        System.out.println("  DB4 with CWT: " + 
            (WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.CWT) ? "✓" : "✗"));
        System.out.println("  MORLET with CWT: " + 
            (WaveletRegistry.isCompatible(WaveletName.MORLET, TransformType.CWT) ? "✓" : "✗"));
        
        // Get recommended transform
        TransformType recommended = WaveletRegistry.getRecommendedTransform(WaveletName.DB4);
        System.out.println("\nRecommended transform for DB4: " + recommended.getDescription());
    }
    
    /**
     * 4. USING WAVELETS WITH TRANSFORMS
     */
    private void demonstrateTransformUsage() {
        System.out.println("\n4. USING WAVELETS WITH TRANSFORMS");
        System.out.println("-".repeat(40));
        
        // Use wavelets with different transforms
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);
        
        // Create a simple signal
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(6 * Math.PI * i / 32);
        }
        
        // Use with MODWT
        System.out.println("Using " + db4.name() + " with MODWT:");
        MODWTTransform modwt = new MODWTTransform(db4, BoundaryMode.PERIODIC);
        MODWTResult result = modwt.forward(signal);
        System.out.println("  Signal length: " + signal.length);
        System.out.println("  Approximation length: " + result.getApproximation().length);
        System.out.println("  Details length: " + result.getDetails().length);
        
        // Use with Multi-level MODWT
        System.out.println("\nUsing " + haar.name() + " with Multi-level MODWT:");
        MultiLevelMODWTTransform mlModwt = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult mlResult = mlModwt.forward(signal, 3);
        System.out.println("  Decomposition levels: " + mlResult.getLevels());
        for (int level = 1; level <= mlResult.getLevels(); level++) {
            System.out.println("  Level " + level + " details length: " + 
                mlResult.getDetails(level).length);
        }
        
        // Perfect reconstruction
        double[] reconstructed = modwt.inverse(result);
        double error = 0;
        for (int i = 0; i < signal.length; i++) {
            error += Math.abs(signal[i] - reconstructed[i]);
        }
        System.out.println("\nReconstruction error: " + String.format("%.2e", error));
    }
}
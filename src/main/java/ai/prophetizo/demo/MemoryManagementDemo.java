package ai.prophetizo.demo;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.api.Haar;
import ai.prophetizo.wavelet.memory.*;

import java.util.Arrays;

/**
 * Demonstrates the new Foreign Function & Memory API capabilities in VectorWave.
 * Shows both heap and off-heap memory management with SIMD-aligned allocations.
 */
public class MemoryManagementDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave - Foreign Function & Memory API Demo");
        System.out.println("==============================================");
        System.out.println();
        
        // Test signal
        double[] signal = {10, 12, 15, 18, 20, 17, 14, 11};
        System.out.println("Original Signal: " + Arrays.toString(signal));
        System.out.println();
        
        demonstrateArrayFactories();
        demonstrateMemoryAwareTransforms(signal);
        demonstrateArrayFactoryStrategies(signal);
        demonstratePerformanceConsiderations();
    }
    
    private static void demonstrateArrayFactories() {
        System.out.println("ARRAY FACTORY COMPARISON");
        System.out.println("========================");
        
        // Heap arrays
        System.out.println("\n1. Heap Arrays:");
        try (HeapArrayFactory heapFactory = new HeapArrayFactory()) {
            try (ManagedArray heapArray = heapFactory.create(8)) {
                System.out.println("   Off-heap: " + heapArray.isOffHeap());
                System.out.println("   Alignment: " + heapArray.alignment() + " bytes");
                
                heapArray.fill(42.0);
                System.out.println("   Sample value: " + heapArray.get(0));
            }
        }
        
        // Off-heap arrays
        System.out.println("\n2. Off-heap Arrays:");
        try (OffHeapArrayFactory offHeapFactory = new OffHeapArrayFactory()) {
            try (ManagedArray offHeapArray = offHeapFactory.createAligned(8, 64)) {
                System.out.println("   Off-heap: " + offHeapArray.isOffHeap());
                System.out.println("   Alignment: " + offHeapArray.alignment() + " bytes");
                
                offHeapArray.fill(42.0);
                System.out.println("   Sample value: " + offHeapArray.get(0));
            }
        }
    }
    
    private static void demonstrateMemoryAwareTransforms(double[] signal) {
        System.out.println("\n\nMEMORY-AWARE TRANSFORMS");
        System.out.println("=======================");
        
        // Heap-based transform
        System.out.println("\n1. Heap-based Transform:");
        ArrayFactoryManager heapManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.HEAP_ONLY);
        try (ManagedWaveletTransform heapTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, heapManager)) {
            try (ManagedTransformResult result = heapTransform.forwardManaged(signal)) {
                System.out.println("   Result off-heap: " + result.isOffHeap());
                System.out.println("   Alignment: " + result.alignment() + " bytes");
                System.out.println("   Approximation: " + Arrays.toString(result.approximationCoeffs()));
                
                try (ManagedArray reconstructed = heapTransform.inverseManaged(result)) {
                    double[] reconArray = reconstructed.toArray();
                    double maxError = calculateMaxError(signal, reconArray);
                    System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
                }
            }
        }
        heapManager.close();
        
        // Off-heap transform
        System.out.println("\n2. Off-heap Transform:");
        ArrayFactoryManager offHeapManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.OFF_HEAP_ONLY);
        try (ManagedWaveletTransform offHeapTransform = new ManagedWaveletTransform(new Haar(), BoundaryMode.PERIODIC, offHeapManager)) {
            try (ManagedTransformResult result = offHeapTransform.forwardManaged(signal)) {
                System.out.println("   Result off-heap: " + result.isOffHeap());
                System.out.println("   Alignment: " + result.alignment() + " bytes");
                System.out.println("   Approximation: " + Arrays.toString(result.approximationCoeffs()));
                
                try (ManagedArray reconstructed = offHeapTransform.inverseManaged(result)) {
                    double[] reconArray = reconstructed.toArray();
                    double maxError = calculateMaxError(signal, reconArray);
                    System.out.println("   Max reconstruction error: " + String.format("%.2e", maxError));
                }
            }
        }
        offHeapManager.close();
    }
    
    private static void demonstrateArrayFactoryStrategies(double[] signal) {
        System.out.println("\n\nARRAY FACTORY STRATEGIES");
        System.out.println("========================");
        
        // Size-based strategy
        System.out.println("\n1. Size-based Strategy (threshold=4):");
        ArrayFactoryManager sizeBasedManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.SIZE_BASED, 4);
        
        // Small array - should use heap
        try (ManagedArray smallArray = sizeBasedManager.create(2)) {
            System.out.println("   Small array (2 elements) off-heap: " + smallArray.isOffHeap());
        }
        
        // Large array - should use off-heap
        try (ManagedArray largeArray = sizeBasedManager.create(8)) {
            System.out.println("   Large array (8 elements) off-heap: " + largeArray.isOffHeap());
        }
        
        sizeBasedManager.close();
        
        // System property strategy
        System.out.println("\n2. System Property Strategy:");
        System.setProperty("vectorwave.memory.strategy", "heap_only");
        System.setProperty("vectorwave.memory.threshold", "1");
        
        ArrayFactoryManager systemManager = new ArrayFactoryManager(ArrayFactoryManager.Strategy.SYSTEM_PROPERTY);
        try (ManagedArray array = systemManager.create(8)) {
            System.out.println("   Array with heap_only property: " + array.isOffHeap());
        }
        systemManager.close();
        
        // Clean up system properties
        System.clearProperty("vectorwave.memory.strategy");
        System.clearProperty("vectorwave.memory.threshold");
    }
    
    private static void demonstratePerformanceConsiderations() {
        System.out.println("\n\nPERFORMANCE CONSIDERATIONS");
        System.out.println("==========================");
        
        System.out.println("\n1. Memory Alignment:");
        try (OffHeapArrayFactory factory = new OffHeapArrayFactory()) {
            // Different alignment values
            int[] alignments = {8, 16, 32, 64};
            for (int alignment : alignments) {
                try (ManagedArray array = factory.createAligned(1024, alignment)) {
                    System.out.println("   " + alignment + "-byte aligned array: " + array.alignment() + " bytes");
                }
            }
        }
        
        System.out.println("\n2. Memory Management Benefits:");
        System.out.println("   - Reduced GC pressure with off-heap storage");
        System.out.println("   - SIMD-aligned memory for vectorization potential");
        System.out.println("   - Scoped memory management with Arena lifecycle");
        System.out.println("   - Zero-copy operations between compatible arrays");
        System.out.println("   - Configurable memory strategies for different workloads");
        
        System.out.println("\n3. Backward Compatibility:");
        System.out.println("   - Existing API unchanged");
        System.out.println("   - Automatic fallback to heap arrays when needed");
        System.out.println("   - Transparent memory management");
    }
    
    private static double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0.0;
        for (int i = 0; i < original.length; i++) {
            maxError = Math.max(maxError, Math.abs(original[i] - reconstructed[i]));
        }
        return maxError;
    }
}
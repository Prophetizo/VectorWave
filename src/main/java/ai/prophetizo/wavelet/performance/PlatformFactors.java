package ai.prophetizo.wavelet.performance;

import java.io.Serializable;

/**
 * Platform-specific factors that affect performance.
 * 
 * <p>These factors are used to adjust the base performance model to match
 * the characteristics of the specific hardware platform.</p>
 * 
 * @since 3.1.0
 */
public class PlatformFactors implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * CPU speed factor relative to reference platform (1.0 = reference speed).
     */
    public final double cpuSpeedFactor;
    
    /**
     * Speedup factor when using vector instructions.
     */
    public final double vectorSpeedup;
    
    /**
     * L1 cache size in bytes.
     */
    public final long l1CacheSize;
    
    /**
     * L2 cache size in bytes.
     */
    public final long l2CacheSize;
    
    /**
     * L3 cache size in bytes.
     */
    public final long l3CacheSize;
    
    /**
     * Number of CPU cores available.
     */
    public final int coreCount;
    
    /**
     * Platform architecture (x86, ARM, etc.).
     */
    public final String architecture;
    
    /**
     * Whether the platform supports AVX-512.
     */
    public final boolean hasAVX512;
    
    /**
     * Whether the platform supports ARM NEON.
     */
    public final boolean hasNEON;
    
    /**
     * Memory bandwidth in GB/s.
     */
    public final double memoryBandwidth;
    
    private PlatformFactors(Builder builder) {
        this.cpuSpeedFactor = builder.cpuSpeedFactor;
        this.vectorSpeedup = builder.vectorSpeedup;
        this.l1CacheSize = builder.l1CacheSize;
        this.l2CacheSize = builder.l2CacheSize;
        this.l3CacheSize = builder.l3CacheSize;
        this.coreCount = builder.coreCount;
        this.architecture = builder.architecture;
        this.hasAVX512 = builder.hasAVX512;
        this.hasNEON = builder.hasNEON;
        this.memoryBandwidth = builder.memoryBandwidth;
    }
    
    /**
     * Detects platform factors for the current system.
     * 
     * @return Platform factors for this system
     */
    public static PlatformFactors detectPlatform() {
        Builder builder = new Builder();
        
        // Detect architecture
        String arch = System.getProperty("os.arch").toLowerCase();
        builder.architecture(arch);
        
        // Detect core count
        builder.coreCount(Runtime.getRuntime().availableProcessors());
        
        // Estimate CPU speed factor based on simple benchmark
        builder.cpuSpeedFactor(estimateCpuSpeedFactor());
        
        // Detect vector capabilities
        if (arch.contains("aarch64") || arch.contains("arm")) {
            builder.hasNEON(true);
            builder.vectorSpeedup(2.0); // Conservative estimate for ARM NEON
        } else if (arch.contains("x86") || arch.contains("amd64")) {
            // Check for AVX-512 (simplified check)
            boolean hasAVX512 = checkAVX512Support();
            builder.hasAVX512(hasAVX512);
            builder.vectorSpeedup(hasAVX512 ? 8.0 : 4.0); // AVX-512 vs AVX2
        }
        
        // Estimate cache sizes (platform-specific defaults)
        if (arch.contains("aarch64")) {
            // Apple Silicon typical values
            builder.l1CacheSize(128 * 1024);      // 128 KB per core
            builder.l2CacheSize(4 * 1024 * 1024); // 4 MB shared
            builder.l3CacheSize(16 * 1024 * 1024); // 16 MB shared
            builder.memoryBandwidth(68.0); // GB/s for M1
        } else {
            // x86-64 typical values
            builder.l1CacheSize(32 * 1024);       // 32 KB per core
            builder.l2CacheSize(256 * 1024);      // 256 KB per core
            builder.l3CacheSize(8 * 1024 * 1024); // 8 MB shared
            builder.memoryBandwidth(50.0); // GB/s typical DDR4
        }
        
        return builder.build();
    }
    
    /**
     * Creates platform factors for a reference system.
     * 
     * @return Reference platform factors
     */
    public static PlatformFactors referencePlatform() {
        return new Builder()
            .cpuSpeedFactor(1.0)
            .vectorSpeedup(1.0)
            .l1CacheSize(32 * 1024)
            .l2CacheSize(256 * 1024)
            .l3CacheSize(8 * 1024 * 1024)
            .coreCount(4)
            .architecture("x86_64")
            .hasAVX512(false)
            .hasNEON(false)
            .memoryBandwidth(25.0)
            .build();
    }
    
    private static double estimateCpuSpeedFactor() {
        // Simple CPU speed estimation using array operations
        int size = 1000000;
        double[] data = new double[size];
        
        // Warm up
        for (int i = 0; i < size; i++) {
            data[i] = i * 0.5;
        }
        
        // Measure time for simple operations
        long start = System.nanoTime();
        double sum = 0;
        for (int iter = 0; iter < 10; iter++) {
            for (int i = 0; i < size; i++) {
                sum += data[i] * data[i];
            }
        }
        long elapsed = System.nanoTime() - start;
        
        // Prevent optimization
        if (sum == 0) System.out.println("Prevent optimization");
        
        // Reference time is ~100ms on reference platform
        double referenceTimeNs = 100_000_000;
        return referenceTimeNs / elapsed;
    }
    
    private static boolean checkAVX512Support() {
        // Simplified check - in production, use CPUID or JNI
        try {
            // Check if Vector API reports 512-bit vectors
            Class<?> vectorClass = Class.forName("jdk.incubator.vector.DoubleVector");
            Object species = vectorClass.getField("SPECIES_PREFERRED").get(null);
            int length = (int) species.getClass().getMethod("length").invoke(species);
            return length >= 8; // 8 doubles = 512 bits
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Builder for platform factors.
     */
    public static class Builder {
        private double cpuSpeedFactor = 1.0;
        private double vectorSpeedup = 1.0;
        private long l1CacheSize = 32 * 1024;
        private long l2CacheSize = 256 * 1024;
        private long l3CacheSize = 8 * 1024 * 1024;
        private int coreCount = 4;
        private String architecture = "unknown";
        private boolean hasAVX512 = false;
        private boolean hasNEON = false;
        private double memoryBandwidth = 25.0;
        
        public Builder cpuSpeedFactor(double factor) {
            this.cpuSpeedFactor = factor;
            return this;
        }
        
        public Builder vectorSpeedup(double speedup) {
            this.vectorSpeedup = speedup;
            return this;
        }
        
        public Builder l1CacheSize(long size) {
            this.l1CacheSize = size;
            return this;
        }
        
        public Builder l2CacheSize(long size) {
            this.l2CacheSize = size;
            return this;
        }
        
        public Builder l3CacheSize(long size) {
            this.l3CacheSize = size;
            return this;
        }
        
        public Builder coreCount(int count) {
            this.coreCount = count;
            return this;
        }
        
        public Builder architecture(String arch) {
            this.architecture = arch;
            return this;
        }
        
        public Builder hasAVX512(boolean has) {
            this.hasAVX512 = has;
            return this;
        }
        
        public Builder hasNEON(boolean has) {
            this.hasNEON = has;
            return this;
        }
        
        public Builder memoryBandwidth(double bandwidth) {
            this.memoryBandwidth = bandwidth;
            return this;
        }
        
        public PlatformFactors build() {
            return new PlatformFactors(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "PlatformFactors{arch=%s, cores=%d, cpuSpeed=%.2fx, vectorSpeedup=%.1fx, " +
            "L1=%dKB, L2=%dKB, L3=%dMB, memBW=%.1fGB/s, AVX512=%b, NEON=%b}",
            architecture, coreCount, cpuSpeedFactor, vectorSpeedup,
            l1CacheSize / 1024, l2CacheSize / 1024, l3CacheSize / (1024 * 1024),
            memoryBandwidth, hasAVX512, hasNEON
        );
    }
}
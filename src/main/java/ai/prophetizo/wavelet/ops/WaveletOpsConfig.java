package ai.prophetizo.wavelet.ops;

import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.ops.WaveletOpsFactory.OptimizationLevel;

import java.util.Objects;

/**
 * Configuration class for wavelet operations.
 * 
 * <p>This immutable class encapsulates configuration parameters for
 * wavelet transform operations, including boundary handling, optimization
 * settings, and computational preferences.</p>
 * 
 * <p>Instances of this class are typically created through the
 * {@link WaveletOpsFactory} to ensure consistent configuration and
 * validation of parameters.</p>
 * 
 * @since 1.0.0
 */
public final class WaveletOpsConfig {
    
    private final BoundaryMode boundaryMode;
    private final OptimizationLevel optimizationLevel;
    private final boolean enableVectorization;
    
    /**
     * Creates a new WaveletOpsConfig with specified parameters.
     * 
     * @param boundaryMode the boundary handling mode
     * @param optimizationLevel the optimization level to use
     * @param enableVectorization whether to enable vectorization
     * @throws NullPointerException if any parameter is null
     */
    public WaveletOpsConfig(BoundaryMode boundaryMode, 
                           OptimizationLevel optimizationLevel,
                           boolean enableVectorization) {
        this.boundaryMode = Objects.requireNonNull(boundaryMode, 
                "boundaryMode cannot be null");
        this.optimizationLevel = Objects.requireNonNull(optimizationLevel,
                "optimizationLevel cannot be null");
        this.enableVectorization = enableVectorization;
    }
    
    /**
     * Gets the boundary mode for operations.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Gets the optimization level for operations.
     * 
     * @return the optimization level
     */
    public OptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }
    
    /**
     * Gets whether vectorization is enabled.
     * 
     * @return true if vectorization is enabled
     */
    public boolean isVectorizationEnabled() {
        return enableVectorization;
    }
    
    /**
     * Returns whether this configuration prefers periodic boundary handling.
     * 
     * @return true if boundary mode is PERIODIC
     */
    public boolean isPeriodicBoundary() {
        return boundaryMode == BoundaryMode.PERIODIC;
    }
    
    /**
     * Returns whether this configuration uses aggressive optimizations.
     * 
     * @return true if optimization level is AGGRESSIVE
     */
    public boolean isAggressiveOptimization() {
        return optimizationLevel == OptimizationLevel.AGGRESSIVE;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WaveletOpsConfig that = (WaveletOpsConfig) obj;
        return enableVectorization == that.enableVectorization &&
               boundaryMode == that.boundaryMode &&
               optimizationLevel == that.optimizationLevel;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(boundaryMode, optimizationLevel, enableVectorization);
    }
    
    @Override
    public String toString() {
        return "WaveletOpsConfig{" +
                "boundaryMode=" + boundaryMode +
                ", optimizationLevel=" + optimizationLevel +
                ", enableVectorization=" + enableVectorization +
                '}';
    }
}
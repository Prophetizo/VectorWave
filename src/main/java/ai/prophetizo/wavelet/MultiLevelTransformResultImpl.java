package ai.prophetizo.wavelet;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.BoundaryMode;
import ai.prophetizo.wavelet.util.ValidationUtils;
import java.util.Arrays;

/**
 * Memory-efficient implementation of MultiLevelTransformResult.
 * 
 * <p>This implementation stores only the detail coefficients at each level
 * and the final approximation. Intermediate approximations are reconstructed
 * on demand to minimize memory usage.</p>
 * 
 * <p>For small signals typical in financial analysis (< 1024 samples),
 * this approach provides optimal memory efficiency while maintaining
 * fast access to frequently-used coefficients.</p>
 */
final class MultiLevelTransformResultImpl implements MultiLevelTransformResult {
    
    private final double[] finalApproximation;
    private final double[][] detailsByLevel;
    private final int levels;
    
    // Cache for lazy reconstruction of intermediate approximations
    private final double[][] approximationCache;
    private final Object[] cacheLocks; // For thread-safe lazy initialization
    
    // Keep reference to wavelet for reconstruction
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    
    /**
     * Creates a multi-level transform result.
     * 
     * @param finalApproximation the approximation coefficients at the deepest level
     * @param detailsByLevel array of detail coefficients indexed by level-1
     * @param wavelet the wavelet used for decomposition (needed for reconstruction)
     * @param boundaryMode the boundary mode used
     */
    MultiLevelTransformResultImpl(double[] finalApproximation, 
                                  double[][] detailsByLevel,
                                  Wavelet wavelet,
                                  BoundaryMode boundaryMode) {
        // Validate inputs
        ValidationUtils.validateNotNullOrEmpty(finalApproximation, "finalApproximation");
        
        if (detailsByLevel == null) {
            throw new IllegalArgumentException("detailsByLevel cannot be null");
        }
        if (wavelet == null) {
            throw new IllegalArgumentException("wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new IllegalArgumentException("boundaryMode cannot be null");
        }
        
        if (detailsByLevel.length == 0) {
            throw new IllegalArgumentException("At least one decomposition level required");
        }
        
        // Validate each detail level
        for (int i = 0; i < detailsByLevel.length; i++) {
            ValidationUtils.validateNotNullOrEmpty(detailsByLevel[i], 
                "detailsByLevel[" + i + "]");
        }
        
        // Make defensive copies
        this.finalApproximation = finalApproximation.clone();
        this.levels = detailsByLevel.length;
        this.detailsByLevel = new double[levels][];
        for (int i = 0; i < levels; i++) {
            this.detailsByLevel[i] = detailsByLevel[i].clone();
        }
        
        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        
        // Initialize cache structures
        this.approximationCache = new double[levels + 1][];
        this.cacheLocks = new Object[levels + 1];
        for (int i = 0; i <= levels; i++) {
            cacheLocks[i] = new Object();
        }
        
        // Store final approximation in cache
        this.approximationCache[levels] = this.finalApproximation.clone();
    }
    
    @Override
    public int levels() {
        return levels;
    }
    
    @Override
    public double[] finalApproximation() {
        return finalApproximation.clone();
    }
    
    @Override
    public double[] detailsAtLevel(int level) {
        if (level < 1 || level > levels) {
            throw new IllegalArgumentException(
                "Level must be between 1 and " + levels + ", got: " + level);
        }
        return detailsByLevel[level - 1].clone();
    }
    
    @Override
    public double[] approximationAtLevel(int level) {
        if (level < 0 || level > levels) {
            throw new IllegalArgumentException(
                "Level must be between 0 and " + levels + ", got: " + level);
        }
        
        // Level 0 would be the original signal - reconstruct fully
        if (level == 0) {
            return reconstructToLevel(0);
        }
        
        // Check cache first
        synchronized (cacheLocks[level]) {
            if (approximationCache[level] == null) {
                // Reconstruct approximation at this level
                approximationCache[level] = reconstructToLevel(level);
            }
            return approximationCache[level].clone();
        }
    }
    
    @Override
    public double[][] allDetails() {
        double[][] result = new double[levels][];
        for (int i = 0; i < levels; i++) {
            result[i] = detailsByLevel[i].clone();
        }
        return result;
    }
    
    @Override
    public TransformResult getTransformResultAtLevel(int level) {
        if (level < 1 || level > levels) {
            throw new IllegalArgumentException(
                "Level must be between 1 and " + levels + ", got: " + level);
        }
        
        // For level N, we need to reconstruct the approximation at level N-1
        // that was used to create the details at level N
        if (level == levels) {
            // For the deepest level, return final approximation and its details
            return TransformResultImpl.createFast(finalApproximation, detailsByLevel[level - 1]);
        }
        
        // For other levels, we need to reconstruct the approximation that produced these details
        // This is the approximation one level deeper than what approximationAtLevel returns
        double[] approxForThisLevel = reconstructApproximationForLevel(level);
        double[] details = detailsAtLevel(level);
        
        return TransformResultImpl.createFast(approxForThisLevel, details);
    }
    
    /**
     * Reconstructs the approximation coefficients that were used as input
     * to produce the details at the specified level.
     */
    private double[] reconstructApproximationForLevel(int level) {
        // Start from the final approximation and work backwards
        double[] current = finalApproximation.clone();
        
        // Reconstruct up to the level we need
        WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode);
        
        for (int i = levels; i > level; i--) {
            TransformResult levelResult = TransformResultImpl.createFast(
                current, detailsByLevel[i - 1]);
            current = transform.inverse(levelResult);
        }
        
        return current;
    }
    
    /**
     * Reconstructs the approximation coefficients at the specified level.
     * Works backwards from the final approximation, applying inverse transforms.
     */
    private double[] reconstructToLevel(int targetLevel) {
        // Start with the final approximation
        double[] current = finalApproximation.clone();
        
        // Create transform for reconstruction
        WaveletTransform transform = new WaveletTransform(wavelet, boundaryMode);
        
        // Reconstruct from deepest level up to target level
        for (int level = levels; level > targetLevel; level--) {
            // Create transform result for this level
            TransformResult levelResult = TransformResultImpl.createFast(
                current, detailsByLevel[level - 1]);
            
            // Apply inverse transform
            current = transform.inverse(levelResult);
        }
        
        return current;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MultiLevelTransformResult{\n");
        sb.append("  levels: ").append(levels).append("\n");
        sb.append("  final approximation length: ").append(finalApproximation.length).append("\n");
        
        for (int i = 0; i < levels; i++) {
            sb.append("  level ").append(i + 1).append(" details length: ")
              .append(detailsByLevel[i].length).append("\n");
        }
        
        sb.append("  total coefficients: ");
        int total = finalApproximation.length;
        for (double[] details : detailsByLevel) {
            total += details.length;
        }
        sb.append(total).append("\n");
        sb.append("}");
        
        return sb.toString();
    }
}
package ai.prophetizo.wavelet;

/**
 * Represents the result of a multi-level wavelet decomposition.
 * 
 * <p>In multi-level decomposition, the wavelet transform is applied recursively
 * to the approximation coefficients, creating a hierarchy of detail coefficients
 * at different scales and a final approximation at the coarsest scale.</p>
 * 
 * <p>This interface provides efficient access to coefficients at any decomposition
 * level and supports partial reconstruction for denoising and analysis.</p>
 * 
 * <p>Level indexing convention:</p>
 * <ul>
 *   <li>Level 1: Finest scale details (highest frequencies)</li>
 *   <li>Level 2: Next coarser scale</li>
 *   <li>Level N: Coarsest scale details (lowest frequencies)</li>
 * </ul>
 * 
 * @since 1.1.0
 */
public sealed interface MultiLevelTransformResult permits MultiLevelTransformResultImpl {
    
    /**
     * Returns the number of decomposition levels.
     * 
     * @return the number of levels (always ≥ 1)
     */
    int levels();
    
    /**
     * Returns the final approximation coefficients at the deepest decomposition level.
     * These represent the coarsest scale features of the signal.
     * 
     * @return defensive copy of the final approximation coefficients
     */
    double[] finalApproximation();
    
    /**
     * Returns detail coefficients at the specified level.
     * 
     * @param level the decomposition level (1 to levels(), inclusive)
     * @return defensive copy of the detail coefficients at the specified level
     * @throws IllegalArgumentException if level is out of range
     */
    double[] detailsAtLevel(int level);
    
    /**
     * Returns approximation coefficients at the specified level.
     * 
     * <p>Level 0 returns the original signal, level N returns the final approximation.
     * Intermediate levels are reconstructed on demand for memory efficiency.</p>
     * 
     * @param level the approximation level (0 to levels(), inclusive)
     * @return defensive copy of the approximation coefficients at the specified level
     * @throws IllegalArgumentException if level is out of range
     */
    double[] approximationAtLevel(int level);
    
    /**
     * Returns all detail coefficients as a 2D array.
     * 
     * <p>The returned array has dimensions [levels][coefficients], where:
     * <ul>
     *   <li>result[0] contains level 1 details (finest scale)</li>
     *   <li>result[1] contains level 2 details</li>
     *   <li>result[levels-1] contains level N details (coarsest scale)</li>
     * </ul>
     * </p>
     * 
     * @return defensive copy of all detail coefficients
     */
    double[][] allDetails();
    
    /**
     * Creates a single-level TransformResult for the specified decomposition level.
     * This is useful for level-wise processing or partial reconstruction.
     * 
     * @param level the decomposition level (1 to levels(), inclusive)
     * @return TransformResult containing approximation and details at the specified level
     * @throws IllegalArgumentException if level is out of range
     */
    TransformResult getTransformResultAtLevel(int level);
    
    /**
     * Computes the energy (sum of squares) of detail coefficients at the specified level.
     * Useful for adaptive decomposition and significance testing.
     * 
     * @param level the decomposition level (1 to levels(), inclusive)
     * @return the energy of detail coefficients at the specified level
     * @throws IllegalArgumentException if level is out of range
     */
    default double detailEnergyAtLevel(int level) {
        double[] details = detailsAtLevel(level);
        double energy = 0.0;
        for (double d : details) {
            energy += d * d;
        }
        return energy;
    }
    
    /**
     * Returns the total energy across all detail levels.
     * 
     * @return sum of detail energies at all levels
     */
    default double totalDetailEnergy() {
        double totalEnergy = 0.0;
        for (int level = 1; level <= levels(); level++) {
            totalEnergy += detailEnergyAtLevel(level);
        }
        return totalEnergy;
    }
}
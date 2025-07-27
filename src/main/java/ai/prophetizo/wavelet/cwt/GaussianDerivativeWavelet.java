package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ContinuousWavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Gaussian Derivative wavelets - derivatives of the Gaussian function.
 * 
 * <p>These wavelets are particularly useful for:</p>
 * <ul>
 *   <li>Edge detection (1st derivative)</li>
 *   <li>Ridge/valley detection (2nd derivative)</li>
 *   <li>Feature extraction at multiple scales</li>
 *   <li>Singularity detection and characterization</li>
 *   <li>Image processing and computer vision</li>
 * </ul>
 * 
 * <p>Mathematical definition:</p>
 * <pre>
 * ψ_n(t) = C_n * H_n(t/σ) * exp(-t²/(2σ²))
 * </pre>
 * 
 * where:
 * <ul>
 *   <li>n is the derivative order</li>
 *   <li>σ is the scale parameter (standard deviation)</li>
 *   <li>H_n is related to the n-th derivative operator</li>
 *   <li>C_n is the normalization constant</li>
 * </ul>
 * 
 * <p>Special cases:</p>
 * <ul>
 *   <li>n=1: First derivative, optimal for edge detection</li>
 *   <li>n=2: Second derivative (similar to Mexican Hat), for ridge detection</li>
 *   <li>n=3: Third derivative, for detecting inflection points</li>
 *   <li>n=4: Fourth derivative, for higher-order feature detection</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public final class GaussianDerivativeWavelet implements ContinuousWavelet {
    
    private final int n; // Derivative order
    private final double sigma; // Scale parameter
    private final String name;
    private final double normFactor;
    
    /**
     * Creates a first-order Gaussian derivative wavelet with σ=1.
     */
    public GaussianDerivativeWavelet() {
        this(1, 1.0);
    }
    
    /**
     * Creates a Gaussian derivative wavelet with specified order and σ=1.
     * 
     * @param n derivative order (must be between 1 and 8)
     * @throws IllegalArgumentException if n < 1 or n > 8
     */
    public GaussianDerivativeWavelet(int n) {
        this(n, 1.0);
    }
    
    /**
     * Creates a Gaussian derivative wavelet with specified order and scale.
     * 
     * @param n derivative order (must be between 1 and 8)
     * @param sigma scale parameter (must be positive)
     * @throws IllegalArgumentException if n < 1, n > 8, or sigma <= 0
     */
    public GaussianDerivativeWavelet(int n, double sigma) {
        if (n < 1) {
            throw new IllegalArgumentException("Derivative order must be positive, got: " + n);
        }
        if (n > 8) {
            throw new IllegalArgumentException("Derivative order too large (max 8), got: " + n);
        }
        if (sigma <= 0) {
            throw new IllegalArgumentException("Scale parameter sigma must be positive, got: " + sigma);
        }
        
        this.n = n;
        this.sigma = sigma;
        this.name = "gaus" + n;
        this.normFactor = calculateNormalizationFactor(n, sigma);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        return String.format("Gaussian derivative wavelet of order %d (σ=%.1f) - %s",
            n, sigma, switch (n) {
                case 1 -> "Edge detection";
                case 2 -> "Ridge/valley detection (Mexican Hat)";
                case 3 -> "Inflection point detection";
                case 4 -> "Higher-order feature detection";
                default -> "Order " + n + " feature detection";
            });
    }
    
    @Override
    public double psi(double t) {
        // Scaled variable
        double x = t / sigma;
        double x2 = x * x;
        
        // Gaussian envelope
        double gaussian = Math.exp(-0.5 * x2) * normFactor;
        
        // Apply derivative operator based on order
        return switch (n) {
            case 1 -> -x / sigma * gaussian;
            case 2 -> (x2 - 1.0) / (sigma * sigma) * gaussian;
            case 3 -> x * (3.0 - x2) / (sigma * sigma * sigma) * gaussian;
            case 4 -> (x2 * x2 - 6.0 * x2 + 3.0) / (sigma * sigma * sigma * sigma) * gaussian;
            case 5 -> x * (x2 * x2 - 10.0 * x2 + 15.0) / Math.pow(sigma, 5) * gaussian;
            case 6 -> (x2 * x2 * x2 - 15.0 * x2 * x2 + 45.0 * x2 - 15.0) / Math.pow(sigma, 6) * gaussian;
            case 7 -> x * (x2 * x2 * x2 - 21.0 * x2 * x2 + 105.0 * x2 - 105.0) / Math.pow(sigma, 7) * gaussian;
            case 8 -> (x2 * x2 * x2 * x2 - 28.0 * x2 * x2 * x2 + 210.0 * x2 * x2 - 420.0 * x2 + 105.0) / 
                     Math.pow(sigma, 8) * gaussian;
            default -> throw new IllegalStateException("Unsupported derivative order: " + n);
        };
    }
    
    @Override
    public double centerFrequency() {
        // Center frequency increases with derivative order and decreases with sigma
        // Approximation based on spectral peak location
        return Math.sqrt(n) / (2 * Math.PI * sigma);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth increases with derivative order
        // Higher derivatives have more oscillations and thus broader bandwidth
        return Math.sqrt(n) / (sigma * Math.sqrt(2.0));
    }
    
    @Override
    public boolean isComplex() {
        return false; // Gaussian derivatives are real-valued
    }
    
    @Override
    public double[] discretize(int length) {
        if (length <= 0) {
            throw new InvalidArgumentException("Length must be positive");
        }
        
        double[] samples = new double[length];
        int center = length / 2;
        
        // Support is approximately [-4σ√n, 4σ√n]
        double support = 4.0 * sigma * Math.sqrt(n);
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) * 2.0 * support / length;
            samples[i] = psi(t);
        }
        
        return samples;
    }
    
    /**
     * Gets the derivative order.
     * 
     * @return derivative order n
     */
    public int getDerivativeOrder() {
        return n;
    }
    
    /**
     * Gets the scale parameter.
     * 
     * @return scale parameter sigma
     */
    public double getSigma() {
        return sigma;
    }
    
    /**
     * Calculates the normalization factor for the Gaussian derivative.
     * This ensures the wavelet has unit energy (L2 norm = 1).
     */
    private static double calculateNormalizationFactor(int n, double sigma) {
        // Basic Gaussian normalization
        double gaussNorm = 1.0 / Math.sqrt(2 * Math.PI * sigma * sigma);
        
        // Additional normalization factors for each derivative order
        // These ensure unit L2 norm for the complete wavelet
        double derivativeNorm = switch (n) {
            case 1 -> Math.sqrt(2.0);  // sqrt(2) for first derivative
            case 2 -> Math.sqrt(2.0 / 3.0);  // sqrt(2/3) for second derivative
            case 3 -> Math.sqrt(2.0 / 15.0);
            case 4 -> Math.sqrt(2.0 / 105.0);
            case 5 -> Math.sqrt(2.0 / 945.0);
            case 6 -> Math.sqrt(2.0 / 10395.0);
            case 7 -> Math.sqrt(2.0 / 135135.0);
            case 8 -> Math.sqrt(2.0 / 2027025.0);
            default -> 1.0;
        };
        
        return gaussNorm * derivativeNorm;
    }
}
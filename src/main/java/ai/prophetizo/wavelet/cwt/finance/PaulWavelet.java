package ai.prophetizo.wavelet.cwt.finance;

import ai.prophetizo.wavelet.api.ComplexContinuousWavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Paul wavelet - a complex-valued wavelet particularly useful for financial analysis.
 * 
 * <p>The Paul wavelet is optimal for detecting asymmetric patterns in financial data,
 * such as sharp price movements followed by gradual recoveries (or vice versa).
 * It's particularly effective for:</p>
 * <ul>
 *   <li>Detecting market crashes and recoveries</li>
 *   <li>Identifying asymmetric volatility patterns</li>
 *   <li>Analyzing directional price movements</li>
 *   <li>Capturing phase information in financial cycles</li>
 * </ul>
 * 
 * <p>Mathematical definition:</p>
 * <pre>
 * ψ(t) = (2^m * i^m * m!) / √(π(2m)!) * (1 - it)^(-(m+1))
 * </pre>
 * 
 * where m is the order parameter (typically 4-6 for financial applications).
 * 
 * @since 1.0.0
 */
public final class PaulWavelet implements ComplexContinuousWavelet {
    
    private final int m; // Order parameter
    private final double normFactor;
    private final String name;
    
    /**
     * Creates a Paul wavelet with default order m=4.
     * This is optimal for most financial applications.
     */
    public PaulWavelet() {
        this(4);
    }
    
    /**
     * Creates a Paul wavelet with specified order.
     * 
     * @param m order parameter (must be positive, typically 1-20)
     * @throws IllegalArgumentException if m < 1 or m > 20
     */
    public PaulWavelet(int m) {
        if (m < 1) {
            throw new IllegalArgumentException("Paul wavelet order must be positive, got: " + m);
        }
        if (m > 20) {
            throw new IllegalArgumentException("Paul wavelet order too large (max 20), got: " + m);
        }
        
        this.m = m;
        this.name = "paul" + m;
        
        // Calculate normalization factor: (2^m * m!) / √(π(2m)!)
        this.normFactor = calculateNormalizationFactor(m);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public double psi(double t) {
        // Real part of Paul wavelet
        // ψ(t) = norm * (1 - it)^(-(m+1))
        
        double denom2 = 1 + t * t;
        double denomPow = Math.pow(denom2, (m + 1) / 2.0);
        
        // Real part: norm * cos((m+1) * arctan(t)) / denomPow
        double angle = (m + 1) * Math.atan(t);
        return normFactor * Math.cos(angle) / denomPow;
    }
    
    @Override
    public double psiImaginary(double t) {
        // Imaginary part of Paul wavelet
        
        double denom2 = 1 + t * t;
        double denomPow = Math.pow(denom2, (m + 1) / 2.0);
        
        // Imaginary part: norm * sin((m+1) * arctan(t)) / denomPow
        double angle = (m + 1) * Math.atan(t);
        return normFactor * Math.sin(angle) / denomPow;
    }
    
    @Override
    public double centerFrequency() {
        // Center frequency for Paul wavelet: f_c = (2m + 1) / (4π)
        return (2.0 * m + 1.0) / (4.0 * Math.PI);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth parameter for Paul wavelet
        // Smaller m gives broader bandwidth
        return 1.0 / Math.sqrt(2.0 * m + 1.0);
    }
    
    @Override
    public double[] discretize(int length) {
        if (length <= 0) {
            throw new InvalidArgumentException("Length must be positive");
        }
        
        double[] samples = new double[length];
        int center = length / 2;
        
        // Support is approximately [-4√(m+1), 4√(m+1)]
        double support = 4.0 * Math.sqrt(m + 1);
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) * 2.0 * support / length;
            
            // For discretization, we return the magnitude of the complex wavelet
            double real = psi(t);
            double imag = psiImaginary(t);
            samples[i] = Math.sqrt(real * real + imag * imag);
        }
        
        return samples;
    }
    
    /**
     * Gets the order parameter.
     * 
     * @return order m
     */
    public int getOrder() {
        return m;
    }
    
    /**
     * Calculates the normalization factor for the Paul wavelet.
     */
    private static double calculateNormalizationFactor(int m) {
        // norm = (2^m * m!) / √(π(2m)!)
        
        // Calculate 2^m
        double pow2m = Math.pow(2, m);
        
        // Calculate m!
        double mFactorial = factorial(m);
        
        // Calculate (2m)!
        double factorial2m = factorial(2 * m);
        
        // Combine: (2^m * m!) / √(π * (2m)!)
        return pow2m * mFactorial / Math.sqrt(Math.PI * factorial2m);
    }
    
    /**
     * Computes factorial using logarithms for numerical stability.
     */
    private static double factorial(int n) {
        if (n <= 1) return 1.0;
        
        // Use Stirling's approximation for large n
        if (n > 20) {
            // n! ≈ √(2πn) * (n/e)^n
            return Math.sqrt(2 * Math.PI * n) * Math.pow(n / Math.E, n);
        }
        
        // Direct calculation for small n
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
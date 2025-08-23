package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ComplexContinuousWavelet;
import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Frequency B-Spline wavelet (FBSP).
 * 
 * The FBSP wavelet is defined by B-splines in the frequency domain,
 * providing smooth frequency characteristics with compact support.
 * 
 * ψ̂(ω) = √fb * [sinc(fb*ω/(2m))]^m * exp(i*fc*ω)
 * 
 * where m is the B-spline order.
 * 
 * Properties:
 * - Smooth frequency response (B-spline shaped)
 * - Better time-frequency trade-off than Shannon
 * - Complex-valued for phase analysis
 * - Order m controls smoothness vs localization
 */
public final class FrequencyBSplineWavelet implements ComplexContinuousWavelet {
    
    private final int m;      // B-spline order
    private final double fb;  // Bandwidth parameter  
    private final double fc;  // Center frequency
    private final String name;
    
    /**
     * Create FBSP wavelet with default parameters.
     * Default: m=2 (quadratic), fb=1, fc=1
     */
    public FrequencyBSplineWavelet() {
        this(2, 1.0, 1.0);
    }
    
    public FrequencyBSplineWavelet(int m, double fb, double fc) {
        if (m < 1 || m > 5) {
            throw new IllegalArgumentException(
                "B-spline order must be between 1 and 5, got: " + m);
        }
        if (fb <= 0) {
            throw new IllegalArgumentException(
                "Bandwidth must be positive, got: " + fb);
        }
        if (fc <= 0) {
            throw new IllegalArgumentException(
                "Center frequency must be positive, got: " + fc);
        }
        this.m = m;
        this.fb = fb;
        this.fc = fc;
        this.name = String.format("fbsp%d-%.1f-%.1f", m, fb, fc);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        return String.format("Frequency B-Spline wavelet (m=%d, fb=%.1f, fc=%.1f)", 
                            m, fb, fc);
    }
    
    @Override
    public double psi(double t) {
        // Real part - computed using approximation of inverse Fourier transform
        return computeRealPart(t);
    }
    
    @Override
    public double psiImaginary(double t) {
        // Imaginary part
        return computeImaginaryPart(t);
    }
    
    /**
     * FBSP in frequency domain (easier to define).
     * Returns complex value as [real, imaginary] array.
     */
    public double[] psiHat(double omega) {
        if (Math.abs(omega) < 1e-10) {
            // At ω=0, sinc(0) = 1, so we get √fb
            // Note: This creates a non-zero DC component that affects admissibility
            return new double[]{Math.sqrt(fb), 0.0};
        }
        
        // B-spline in frequency: [sinc(fb*ω/(2m))]^m
        double arg = fb * omega / (2 * m);
        double sinc = Math.sin(Math.PI * arg) / (Math.PI * arg);
        double magnitude = Math.sqrt(fb) * Math.pow(Math.abs(sinc), m);
        
        // Phase modulation
        double phase = fc * omega;
        
        return new double[]{
            magnitude * Math.cos(phase),
            magnitude * Math.sin(phase)
        };
    }
    
    private double computeRealPart(double t) {
        // Numerical inverse Fourier transform with improved accuracy
        double sum = 0;
        double dcComponent = 0; // Track DC for zero mean correction
        int N = 1000; // Increased for better accuracy
        double wMax = 20.0; // Expanded frequency range
        double dw = 2 * wMax / N;
        
        for (int k = 0; k < N; k++) {
            double w = -wMax + k * dw;
            double[] psiW = psiHat(w);
            
            if (Math.abs(w) < 1e-10) {
                dcComponent = psiW[0] / (2 * Math.PI); // DC contribution
            }
            
            sum += psiW[0] * Math.cos(w * t) - psiW[1] * Math.sin(w * t);
        }
        
        // Subtract DC component for zero mean (admissibility)
        return sum * dw / (2 * Math.PI) - dcComponent;
    }
    
    private double computeImaginaryPart(double t) {
        // Improved numerical computation for imaginary part
        double sum = 0;
        int N = 1000; // Increased for consistency 
        double wMax = 20.0; // Expanded frequency range
        double dw = 2 * wMax / N;
        
        for (int k = 0; k < N; k++) {
            double w = -wMax + k * dw;
            double[] psiW = psiHat(w);
            sum += psiW[0] * Math.sin(w * t) + psiW[1] * Math.cos(w * t);
        }
        
        return sum * dw / (2 * Math.PI);
    }
    
    @Override
    public double centerFrequency() {
        return fc;
    }
    
    @Override
    public double bandwidth() {
        return fb;
    }
    
    /**
     * Get B-spline order.
     */
    public int getOrder() {
        return m;
    }
    
    @Override
    public double[] discretize(int length) {
        if (length <= 0) {
            throw new InvalidArgumentException("Length must be positive");
        }
        
        double[] samples = new double[length];
        int center = length / 2;
        
        // Effective support: [-8, 8] for good approximation
        double support = 8.0;
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) * 2.0 * support / length;
            samples[i] = psi(t);
        }
        
        return samples;
    }
}
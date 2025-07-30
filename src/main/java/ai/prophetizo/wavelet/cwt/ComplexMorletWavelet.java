package ai.prophetizo.wavelet.cwt;

import ai.prophetizo.wavelet.api.ComplexContinuousWavelet;

/**
 * Complex Morlet wavelet implementation.
 * 
 * <p>The complex Morlet wavelet is defined as:
 * ψ(t) = (1/π^(1/4)) * (1/√σ) * exp(2πifc*t) * exp(-t²/(2σ²))
 * where σ is the bandwidth parameter and fc is the center frequency.</p>
 * 
 * <p>Note: This implementation uses 'bandwidth' as the parameter name,
 * which corresponds to σ in the standard formula. The canonical Morlet
 * wavelet includes a correction term exp(-σ²/2) to ensure zero mean,
 * but this simplified version omits it for computational efficiency.</p>
 */
public final class ComplexMorletWavelet implements ComplexContinuousWavelet {
    
    private final double bandwidth;
    private final double centerFrequency;
    private final double normFactor;
    
    /**
     * Creates a complex Morlet wavelet.
     * 
     * @param bandwidth bandwidth parameter (typically 1.0)
     * @param centerFrequency center frequency (typically 1.0)
     */
    public ComplexMorletWavelet(double bandwidth, double centerFrequency) {
        if (bandwidth <= 0) {
            throw new IllegalArgumentException("Bandwidth must be positive");
        }
        if (centerFrequency <= 0) {
            throw new IllegalArgumentException("Center frequency must be positive");
        }
        
        this.bandwidth = bandwidth;
        this.centerFrequency = centerFrequency;
        // Normalization factor: 1/(π^(1/4) * √σ)
        this.normFactor = 1.0 / (Math.pow(Math.PI, 0.25) * Math.sqrt(bandwidth));
    }
    
    @Override
    public ComplexNumber psiComplex(double t) {
        double gaussian = Math.exp(-0.5 * t * t / (bandwidth * bandwidth));
        double real = normFactor * gaussian * Math.cos(2 * Math.PI * centerFrequency * t);
        double imag = normFactor * gaussian * Math.sin(2 * Math.PI * centerFrequency * t);
        return new ComplexNumber(real, imag);
    }
    
    @Override
    public double psi(double t) {
        // Real part only
        return normFactor * Math.exp(-0.5 * t * t / (bandwidth * bandwidth)) * 
               Math.cos(2 * Math.PI * centerFrequency * t);
    }
    
    @Override
    public double psiImaginary(double t) {
        // Imaginary part
        return normFactor * Math.exp(-0.5 * t * t / (bandwidth * bandwidth)) * 
               Math.sin(2 * Math.PI * centerFrequency * t);
    }
    
    @Override
    public double bandwidth() {
        return bandwidth;
    }
    
    @Override
    public double centerFrequency() {
        return centerFrequency;
    }
    
    @Override
    public String name() {
        return "Complex Morlet";
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        double[] coeffs = new double[numCoeffs];
        double step = 8.0 / (numCoeffs - 1); // Sample from -4 to 4
        
        for (int i = 0; i < numCoeffs; i++) {
            double t = -4.0 + i * step;
            coeffs[i] = psi(t);
        }
        
        // Normalize
        double sum = 0;
        for (double c : coeffs) {
            sum += c * c;
        }
        double norm = Math.sqrt(sum);
        
        for (int i = 0; i < numCoeffs; i++) {
            coeffs[i] /= norm;
        }
        
        return coeffs;
    }
}
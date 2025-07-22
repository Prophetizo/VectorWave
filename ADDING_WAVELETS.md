# Adding New Wavelets to VectorWave

This guide explains how to add new wavelet types to the VectorWave library.

## Understanding the Wavelet Hierarchy

VectorWave uses a sealed interface hierarchy to ensure type safety:

```
Wavelet (base interface)
├── DiscreteWavelet
│   ├── OrthogonalWavelet
│   └── BiorthogonalWavelet
└── ContinuousWavelet
```

## Steps to Add a New Wavelet

### 1. Choose the Appropriate Interface

- **OrthogonalWavelet**: For wavelets where reconstruction filters are the same as decomposition filters (e.g., Haar, Daubechies, Symlets, Coiflets)
- **BiorthogonalWavelet**: For wavelets with different decomposition and reconstruction filters (e.g., Biorthogonal Splines)
- **ContinuousWavelet**: For wavelets defined by mathematical functions (e.g., Morlet, Mexican Hat)

### 2. Implement the Wavelet

#### Example: Adding an Orthogonal Wavelet

```java
package ai.prophetizo.wavelet.api;

public final class Meyer implements OrthogonalWavelet {
    
    private static final double[] LOW_PASS_COEFFS = {
        // Add Meyer wavelet coefficients here
        0.0, 0.0, // ... actual coefficients
    };
    
    @Override
    public String name() {
        return "meyer";
    }
    
    @Override
    public String description() {
        return "Meyer wavelet";
    }
    
    @Override
    public double[] lowPassDecomposition() {
        return LOW_PASS_COEFFS.clone();
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate using quadrature mirror filter
        double[] h = LOW_PASS_COEFFS;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }
    
    @Override
    public int vanishingMoments() {
        return 4; // Meyer specific value
    }
}
```

#### Example: Adding a Biorthogonal Wavelet

```java
package ai.prophetizo.wavelet.api;

public final class BiorthogonalSpline22 implements BiorthogonalWavelet {
    
    private static final double[] DECOMP_LOW_PASS = {
        // Decomposition low-pass filter coefficients
    };
    
    private static final double[] RECON_LOW_PASS = {
        // Reconstruction low-pass filter coefficients
    };
    
    @Override
    public String name() {
        return "bior2.2";
    }
    
    @Override
    public double[] lowPassDecomposition() {
        return DECOMP_LOW_PASS.clone();
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate from reconstruction low-pass
        return generateHighPass(RECON_LOW_PASS);
    }
    
    @Override
    public double[] lowPassReconstruction() {
        return RECON_LOW_PASS.clone();
    }
    
    @Override
    public double[] highPassReconstruction() {
        // Generate from decomposition low-pass
        return generateHighPass(DECOMP_LOW_PASS);
    }
    
    @Override
    public int vanishingMoments() {
        return 2;
    }
    
    @Override
    public int dualVanishingMoments() {
        return 2;
    }
    
    @Override
    public boolean isSymmetric() {
        return true;
    }
    
    private double[] generateHighPass(double[] lowPass) {
        double[] g = new double[lowPass.length];
        for (int i = 0; i < lowPass.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * lowPass[lowPass.length - 1 - i];
        }
        return g;
    }
}
```

#### Example: Adding a Continuous Wavelet

```java
package ai.prophetizo.wavelet.api;

public final class MexicanHatWavelet implements ContinuousWavelet {
    
    private final double sigma;
    
    public MexicanHatWavelet(double sigma) {
        this.sigma = sigma;
    }
    
    @Override
    public String name() {
        return "mexh";
    }
    
    @Override
    public double psi(double t) {
        double t2 = t * t;
        double sigma2 = sigma * sigma;
        double norm = 2.0 / (Math.sqrt(3 * sigma) * Math.pow(Math.PI, 0.25));
        return norm * (1.0 - t2 / sigma2) * Math.exp(-t2 / (2 * sigma2));
    }
    
    @Override
    public double centerFrequency() {
        return 0.25; // Mexican Hat center frequency
    }
    
    @Override
    public double bandwidth() {
        return sigma;
    }
    
    @Override
    public boolean isComplex() {
        return false;
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        // Discretize the continuous wavelet
        double[] coeffs = new double[numCoeffs];
        double t0 = -5.0 * sigma;
        double dt = 10.0 * sigma / (numCoeffs - 1);
        
        for (int i = 0; i < numCoeffs; i++) {
            coeffs[i] = psi(t0 + i * dt);
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
```

### 3. Register the Wavelet

Add your wavelet to the `WaveletRegistry` in the static initializer:

```java
static {
    // ... existing wavelets ...
    
    // Add your new wavelet
    register(new Meyer());
    register(new BiorthogonalSpline22());
    register(new MexicanHatWavelet(1.0));
}
```

### 4. Add Tests

Create comprehensive tests for your wavelet:

```java
@Test
void testMeyerWaveletProperties() {
    Meyer meyer = new Meyer();
    
    assertEquals("meyer", meyer.name());
    assertNotNull(meyer.lowPassDecomposition());
    assertNotNull(meyer.highPassDecomposition());
    
    // Test perfect reconstruction
    WaveletTransform transform = new WaveletTransform(meyer, BoundaryMode.PERIODIC);
    double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
    TransformResult result = transform.forward(signal);
    double[] reconstructed = transform.inverse(result);
    
    assertArrayEquals(signal, reconstructed, 1e-10);
}
```

### 5. Update Documentation

- Add your wavelet to the README.md features list
- Update any relevant examples
- Document the wavelet's properties and use cases

## Wavelet Coefficient Sources

When implementing wavelets, you'll need the filter coefficients. Good sources include:

1. **PyWavelets**: Python library with extensive wavelet coefficients
2. **MATLAB Wavelet Toolbox**: Comprehensive wavelet documentation
3. **Academic Papers**: Original papers defining the wavelets
4. **"Ten Lectures on Wavelets" by Ingrid Daubechies**: Classic reference

## Best Practices

1. **Coefficient Precision**: Use high-precision coefficients (at least 15 decimal places)
2. **Normalization**: Ensure filters are properly normalized
3. **Perfect Reconstruction**: Test that forward + inverse transform preserves the signal
4. **Memory Efficiency**: Clone arrays when returning to ensure immutability
5. **Documentation**: Include references to the source of coefficients

## Common Pitfalls

1. **Sign Conventions**: Different sources may use different sign conventions for high-pass filters
2. **Normalization**: Some sources use different normalization factors
3. **Filter Length**: Ensure filter lengths are appropriate for the wavelet
4. **Boundary Effects**: Test with different boundary modes

## Testing Your Wavelet

Use the provided test utilities:

```java
// Test perfect reconstruction
WaveletAssertions.assertPerfectReconstruction(signal, reconstructed, tolerance);

// Test energy preservation (Parseval's theorem)
WaveletAssertions.assertEnergyPreserved(signal, result, tolerance);

// Test with standard signals
double[] constant = WaveletTestUtils.createConstantSignal(64, 5.0);
double[] linear = WaveletTestUtils.createLinearSignal(64);
double[] sinusoidal = WaveletTestUtils.createSinusoidalSignal(64, 0.1);
```

## Contributing

When contributing a new wavelet:

1. Ensure all tests pass
2. Include benchmark results
3. Provide references for coefficient sources
4. Update this documentation if needed

For questions or assistance, please open an issue on the project repository.
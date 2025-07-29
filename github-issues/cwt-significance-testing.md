# Statistical Significance Testing for CWT

## Summary
Implement comprehensive statistical significance testing for CWT results, including cone of influence calculation, noise modeling, and bootstrap confidence intervals.

## Motivation
Without significance testing, it's difficult to distinguish between true signal features and random fluctuations. This is critical for scientific applications where false positives must be minimized.

## Detailed Description

### Core Features

1. **Cone of Influence (COI)**
   - Edge effect quantification
   - Scale-dependent COI calculation
   - Automatic masking of unreliable regions

2. **Noise Significance Levels**
   - White noise (χ² distribution)
   - Red noise (AR(1) process)
   - Custom noise models
   - Analytical and Monte Carlo methods

3. **Bootstrap Methods**
   - Wavelet coefficient bootstrapping
   - Block bootstrap for time series
   - Confidence interval estimation
   - Percentile and BCa methods

4. **Multiple Testing Corrections**
   - Bonferroni correction
   - False Discovery Rate (FDR)
   - Wavelet-domain specific corrections

## Proposed API

```java
// Basic significance testing
SignificanceTester tester = new SignificanceTester(cwtResult);

// Test against white noise
WhiteNoiseTest whiteTest = tester.testAgainstWhiteNoise(0.95);
boolean[][] significantCoeffs = whiteTest.getSignificanceMask();

// Test against red noise (AR(1))
RedNoiseParameters redNoise = RedNoiseParameters.estimateFromSignal(signal);
RedNoiseTest redTest = tester.testAgainstRedNoise(redNoise, 0.95);

// Bootstrap confidence intervals
BootstrapConfig bootConfig = BootstrapConfig.builder()
    .numSamples(1000)
    .method(BootstrapMethod.BLOCK)
    .blockSize(10)
    .confidenceLevel(0.95)
    .build();

BootstrapResult bootResult = tester.bootstrap(bootConfig);
double[][] lowerBound = bootResult.getLowerBound();
double[][] upperBound = bootResult.getUpperBound();

// Cone of Influence
ConeOfInfluence coi = new ConeOfInfluence(wavelet, scales);
double[] coiBoundary = coi.calculate(signalLength);
boolean[][] reliableRegion = coi.getMask();

// Multiple testing correction
CorrectedSignificance corrected = tester.applyFDR(0.05);
```

## Implementation Details

### COI Calculation
```java
public double[] calculateCOI(int signalLength) {
    double[] coi = new double[signalLength];
    double e_folding = Math.sqrt(2) * wavelet.effectiveSupport();
    
    for (int i = 0; i < signalLength; i++) {
        // Distance from edge
        int distFromEdge = Math.min(i, signalLength - 1 - i);
        coi[i] = distFromEdge * dt / e_folding;
    }
    return coi;
}
```

### Significance Levels
```java
// White noise significance
public double whiteNoiseThreshold(double scale, double confidence) {
    double dof = 2; // Degrees of freedom for real Morlet
    if (wavelet.isComplex()) dof = 1;
    
    double Pk = wavelet.fourierPeriod(scale);
    double chisquare = ChiSquare.inverseCDF(confidence, dof);
    
    return Math.sqrt(Pk * chisquare / dof);
}

// Red noise (AR1) significance
public double redNoiseThreshold(double scale, double lag1, double confidence) {
    double Pk = wavelet.fourierPeriod(scale);
    double freq = 1.0 / Pk;
    
    // Theoretical AR1 spectrum
    double Pf = (1 - lag1*lag1) / 
                (1 - 2*lag1*Math.cos(2*Math.PI*freq) + lag1*lag1);
    
    return whiteNoiseThreshold(scale, confidence) * Math.sqrt(Pf);
}
```

## Use Cases

1. **Climate Science**: Identifying significant periodic components
2. **Neuroscience**: Detecting true neural oscillations vs noise
3. **Finance**: Distinguishing market cycles from random walk
4. **Quality Control**: Finding significant defects in signals

## Success Criteria
- Accurate implementation verified against published results
- Fast computation for real-time applications
- Clear documentation of statistical assumptions
- Integration with visualization for easy interpretation

## References
- Torrence & Compo (1998) - Significance testing methodology
- Maraun et al. (2007) - Wavelet-based significance testing
- Percival & Walden (2000) - Statistical methods for wavelets

## Labels
`enhancement`, `statistics`, `cwt`, `high-priority`, `scientific`

## Milestone
CWT v1.1

## Estimated Effort
Medium (2-3 weeks)
package ai.prophetizo.financial;

/**
 * Result of volatility analysis containing various volatility measures.
 */
public class VolatilityResult {
    private final double realizedVolatility;
    private final double garchVolatility;
    private final double waveletVolatility;
    private final double[] detailVolatilities;
    private final double[] timeScaleVolatilities;
    private final long timestamp;
    
    public VolatilityResult(double realizedVolatility, double garchVolatility, double waveletVolatility,
                           double[] detailVolatilities, double[] timeScaleVolatilities, long timestamp) {
        this.realizedVolatility = realizedVolatility;
        this.garchVolatility = garchVolatility;
        this.waveletVolatility = waveletVolatility;
        this.detailVolatilities = detailVolatilities.clone();
        this.timeScaleVolatilities = timeScaleVolatilities.clone();
        this.timestamp = timestamp;
    }
    
    public double realizedVolatility() { return realizedVolatility; }
    public double garchVolatility() { return garchVolatility; }
    public double waveletVolatility() { return waveletVolatility; }
    public double[] detailVolatilities() { return detailVolatilities.clone(); }
    public double[] timeScaleVolatilities() { return timeScaleVolatilities.clone(); }
    public long timestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("VolatilityResult{realized=%.4f, garch=%.4f, wavelet=%.4f, timestamp=%d}", 
                           realizedVolatility, garchVolatility, waveletVolatility, timestamp);
    }
}
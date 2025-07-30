package ai.prophetizo.financial;

/**
 * Configuration class for financial analysis parameters.
 */
public class FinancialConfig {
    
    /**
     * Default risk-free rate (annual, as a decimal).
     * Current US Treasury 10-year rate approximation: 4.5%
     */
    public static final double DEFAULT_RISK_FREE_RATE = 0.045;
    
    private final double riskFreeRate;
    
    /**
     * Creates a financial configuration with default risk-free rate.
     */
    public FinancialConfig() {
        this(DEFAULT_RISK_FREE_RATE);
    }
    
    /**
     * Creates a financial configuration with specified risk-free rate.
     * 
     * @param riskFreeRate the risk-free rate as an annual decimal (e.g., 0.045 for 4.5%)
     * @throws IllegalArgumentException if riskFreeRate is negative
     */
    public FinancialConfig(double riskFreeRate) {
        if (riskFreeRate < 0) {
            throw new IllegalArgumentException("Risk-free rate cannot be negative: " + riskFreeRate);
        }
        this.riskFreeRate = riskFreeRate;
    }
    
    /**
     * Returns the configured risk-free rate.
     * 
     * @return the risk-free rate as an annual decimal
     */
    public double getRiskFreeRate() {
        return riskFreeRate;
    }
    
    @Override
    public String toString() {
        return String.format("FinancialConfig{riskFreeRate=%.4f}", riskFreeRate);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FinancialConfig)) return false;
        FinancialConfig other = (FinancialConfig) obj;
        return Double.compare(riskFreeRate, other.riskFreeRate) == 0;
    }
    
    @Override
    public int hashCode() {
        return Double.hashCode(riskFreeRate);
    }
}
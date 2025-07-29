package ai.prophetizo.financial;

/**
 * Represents a trading signal generated from wavelet analysis.
 */
public class TradingSignal {
    public enum Type {
        BUY, SELL, HOLD
    }
    
    public enum Strength {
        WEAK, MODERATE, STRONG
    }
    
    private final Type type;
    private final Strength strength;
    private final double confidence;
    private final double price;
    private final long timestamp;
    private final String reason;
    
    public TradingSignal(Type type, Strength strength, double confidence, double price, long timestamp, String reason) {
        this.type = type;
        this.strength = strength;
        this.confidence = confidence; 
        this.price = price;
        this.timestamp = timestamp;
        this.reason = reason;
    }
    
    public Type type() { return type; }
    public Strength strength() { return strength; }
    public double confidence() { return confidence; }
    public double price() { return price; }
    public long timestamp() { return timestamp; }
    public String reason() { return reason; }
    
    @Override
    public String toString() {
        return String.format("TradingSignal{type=%s, strength=%s, confidence=%.2f, price=%.2f, timestamp=%d, reason='%s'}", 
                           type, strength, confidence, price, timestamp, reason);
    }
}
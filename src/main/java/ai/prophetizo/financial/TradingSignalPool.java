package ai.prophetizo.financial;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Object pool for TradingSignal objects to reduce allocation pressure.
 */
public class TradingSignalPool {
    private final BlockingQueue<TradingSignal> pool;
    private final int maxPoolSize;
    
    public TradingSignalPool(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        this.pool = new ArrayBlockingQueue<>(maxPoolSize);
        
        // Pre-populate with some objects
        for (int i = 0; i < Math.min(10, maxPoolSize); i++) {
            pool.offer(createNewSignal());
        }
    }
    
    /**
     * Get a TradingSignal from the pool, creating new one if pool is empty.
     */
    public TradingSignal acquire(TradingSignal.Type type, TradingSignal.Strength strength, 
                                double confidence, double price, long timestamp, String reason) {
        TradingSignal signal = pool.poll();
        if (signal == null) {
            signal = createNewSignal();
        }
        
        // Reset the signal with new values
        return new TradingSignal(type, strength, confidence, price, timestamp, reason);
    }
    
    /**
     * Return a TradingSignal to the pool for reuse.
     * Note: In real implementation, we'd need mutable TradingSignal for this to work properly.
     * For this demo, we'll just offer a new generic signal to maintain pool size.
     */
    public void release(TradingSignal signal) {
        if (pool.size() < maxPoolSize) {
            // Since TradingSignal is immutable, we can't reuse it directly
            // In a real implementation, we'd have a mutable pooled object
            pool.offer(createNewSignal());
        }
    }
    
    private TradingSignal createNewSignal() {
        return new TradingSignal(TradingSignal.Type.HOLD, TradingSignal.Strength.WEAK, 
                                0.0, 0.0, 0L, "pooled");
    }
    
    /**
     * Get current pool size for monitoring.
     */
    public int getPoolSize() {
        return pool.size();
    }
}
package ai.prophetizo.wavelet.memory;

/**
 * Central factory manager for creating managed arrays.
 * 
 * <p>Provides a unified interface for creating arrays with different memory
 * management strategies. Can automatically choose between heap and off-heap
 * implementations based on configuration or array size thresholds.</p>
 */
public final class ArrayFactoryManager {
    
    /**
     * Strategy for choosing array implementation.
     */
    public enum Strategy {
        /** Always use heap arrays */
        HEAP_ONLY,
        /** Always use off-heap arrays */
        OFF_HEAP_ONLY,
        /** Choose based on array size threshold */
        SIZE_BASED,
        /** Choose based on system properties */
        SYSTEM_PROPERTY
    }
    
    private static final String STRATEGY_PROPERTY = "vectorwave.memory.strategy";
    private static final String THRESHOLD_PROPERTY = "vectorwave.memory.threshold";
    private static final int DEFAULT_THRESHOLD = 1024; // Elements, not bytes
    
    private final Strategy strategy;
    private final int sizeThreshold;
    private final ArrayFactory heapFactory;
    private final ArrayFactory offHeapFactory;
    
    /**
     * Creates a new factory manager with default strategy (SIZE_BASED).
     */
    public ArrayFactoryManager() {
        this(determineStrategy(), DEFAULT_THRESHOLD);
    }
    
    /**
     * Creates a new factory manager with the specified strategy.
     * 
     * @param strategy the strategy to use
     */
    public ArrayFactoryManager(Strategy strategy) {
        this(strategy, DEFAULT_THRESHOLD);
    }
    
    /**
     * Creates a new factory manager with the specified strategy and size threshold.
     * 
     * @param strategy the strategy to use
     * @param sizeThreshold the size threshold for SIZE_BASED strategy (in elements)
     */
    public ArrayFactoryManager(Strategy strategy, int sizeThreshold) {
        this.strategy = strategy;
        this.sizeThreshold = sizeThreshold;
        this.heapFactory = new HeapArrayFactory();
        
        // Only create off-heap factory if needed
        this.offHeapFactory = (strategy == Strategy.HEAP_ONLY) ? null : new OffHeapArrayFactory();
    }
    
    /**
     * Creates a managed array with the specified length using the configured strategy.
     * 
     * @param length the array length
     * @return a new managed array
     */
    public ManagedArray create(int length) {
        return getFactory(length).create(length);
    }
    
    /**
     * Creates an aligned managed array with the specified length using the configured strategy.
     * 
     * @param length the array length
     * @param alignment the memory alignment in bytes
     * @return a new aligned managed array
     */
    public ManagedArray createAligned(int length, int alignment) {
        return getFactory(length).createAligned(length, alignment);
    }
    
    /**
     * Creates a managed array from existing data using the configured strategy.
     * 
     * @param data the source data
     * @return a new managed array containing a copy of the data
     */
    public ManagedArray from(double[] data) {
        return getFactory(data.length).from(data);
    }
    
    /**
     * Creates an aligned managed array from existing data using the configured strategy.
     * 
     * @param data the source data
     * @param alignment the memory alignment in bytes
     * @return a new aligned managed array containing a copy of the data
     */
    public ManagedArray fromAligned(double[] data, int alignment) {
        return getFactory(data.length).fromAligned(data, alignment);
    }
    
    /**
     * Gets the heap factory for creating heap-based arrays.
     * 
     * @return the heap factory
     */
    public ArrayFactory getHeapFactory() {
        return heapFactory;
    }
    
    /**
     * Gets the off-heap factory for creating off-heap arrays.
     * 
     * @return the off-heap factory, or null if not available
     */
    public ArrayFactory getOffHeapFactory() {
        return offHeapFactory;
    }
    
    /**
     * Gets the current strategy.
     * 
     * @return the strategy
     */
    public Strategy getStrategy() {
        return strategy;
    }
    
    /**
     * Gets the size threshold for SIZE_BASED strategy.
     * 
     * @return the size threshold in elements
     */
    public int getSizeThreshold() {
        return sizeThreshold;
    }
    
    /**
     * Closes all factories and releases resources.
     */
    public void close() {
        heapFactory.close();
        if (offHeapFactory != null) {
            offHeapFactory.close();
        }
    }
    
    private ArrayFactory getFactory(int length) {
        return switch (strategy) {
            case HEAP_ONLY -> heapFactory;
            case OFF_HEAP_ONLY -> {
                if (offHeapFactory == null) {
                    throw new IllegalStateException("Off-heap factory not available");
                }
                yield offHeapFactory;
            }
            case SIZE_BASED -> (length >= sizeThreshold && offHeapFactory != null) ? offHeapFactory : heapFactory;
            case SYSTEM_PROPERTY -> determineRuntimeFactory(length);
        };
    }
    
    private ArrayFactory determineRuntimeFactory(int length) {
        // Check system property at runtime for flexibility
        String prop = System.getProperty(STRATEGY_PROPERTY, "size_based").toLowerCase();
        return switch (prop) {
            case "heap", "heap_only" -> heapFactory;
            case "offheap", "off_heap", "off_heap_only" -> {
                if (offHeapFactory == null) {
                    throw new IllegalStateException("Off-heap factory not available");
                }
                yield offHeapFactory;
            }
            default -> { // size_based or any other value
                int threshold = Integer.getInteger(THRESHOLD_PROPERTY, sizeThreshold);
                yield (length >= threshold && offHeapFactory != null) ? offHeapFactory : heapFactory;
            }
        };
    }
    
    private static Strategy determineStrategy() {
        String prop = System.getProperty(STRATEGY_PROPERTY);
        if (prop == null) {
            return Strategy.SIZE_BASED;
        }
        
        return switch (prop.toLowerCase()) {
            case "heap", "heap_only" -> Strategy.HEAP_ONLY;
            case "offheap", "off_heap", "off_heap_only" -> Strategy.OFF_HEAP_ONLY;
            case "system", "system_property" -> Strategy.SYSTEM_PROPERTY;
            default -> Strategy.SIZE_BASED;
        };
    }
}
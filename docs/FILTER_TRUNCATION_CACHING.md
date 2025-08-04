# Filter Truncation Caching Optimization

## Overview
Implemented a caching mechanism in `MultiLevelMODWTTransform` to avoid repeated allocations when truncating filters that exceed signal length, addressing Copilot's performance concern.

## Problem
The original code created new truncated filter arrays every time a filter exceeded the signal length:
```java
if (scaledLowPass.length > signalLength) {
    double[] truncated = new double[signalLength];
    System.arraycopy(scaledLowPass, 0, truncated, 0, signalLength);
    scaledLowPass = truncated;
}
```

This could cause significant memory allocation overhead when:
- Processing many signals of the same length
- Working with high decomposition levels (where filters become very long)
- Performing repeated transformations

## Solution
Implemented a thread-safe caching mechanism using `ConcurrentHashMap`:

1. **Added cache field**:
   ```java
   private final Map<String, double[]> truncatedFilterCache = new ConcurrentHashMap<>();
   ```

2. **Created helper method**:
   ```java
   private double[] getTruncatedFilter(double[] filter, int targetLength, String filterType) {
       String cacheKey = filterType + "_" + targetLength;
       
       return truncatedFilterCache.computeIfAbsent(cacheKey, key -> {
           double[] truncated = new double[targetLength];
           System.arraycopy(filter, 0, truncated, 0, targetLength);
           return truncated;
       });
   }
   ```

3. **Updated all truncation sites** to use the cache:
   - `applyScaledMODWT` method
   - `applyScaledInverseMODWT` method

## Benefits

1. **Memory Efficiency**:
   - Avoids repeated allocations for the same filter/length combinations
   - Especially beneficial when processing multiple signals of the same length

2. **Performance**:
   - Reduces GC pressure from temporary array allocations
   - `computeIfAbsent` is efficient for concurrent access

3. **Thread Safety**:
   - `ConcurrentHashMap` ensures thread-safe caching
   - No synchronization bottlenecks

4. **Automatic Growth**:
   - Cache grows as needed for different signal lengths
   - No pre-allocation or configuration required

## Example Scenario
When processing 1000 signals of length 128 with a 5-level MODWT decomposition:
- **Before**: 5000+ array allocations (5 levels × 2 filters × 1000 signals)
- **After**: ~10 array allocations (5 levels × 2 filter types)

## Testing
All MODWT tests pass, confirming the optimization maintains correctness while improving performance.
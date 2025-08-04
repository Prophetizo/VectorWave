# MODWTStreamingDenoiser Noise Window Update Fix

## Overview
Fixed the noise window update logic in `MODWTStreamingDenoiser` to prevent data loss when detail coefficient arrays are larger than the noise window size, as identified by Copilot.

## Issue
The original code would iterate through ALL detail coefficients but only had a fixed-size noise window:
```java
for (double detail : details) {
    noiseWindow[noiseWindowIndex] = Math.abs(detail);
    noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
}
```

This caused:
- Only the last `noiseWindowSize` coefficients were retained from each transform
- Loss of temporal diversity in noise estimation
- Potentially biased noise estimates

## Solution
Implemented a uniform sampling strategy that:

1. **Small detail arrays** (≤ noise window size): Adds all coefficients to the window
2. **Large detail arrays** (> noise window size): Samples uniformly across the array to maintain temporal diversity

```java
if (details.length <= noiseWindowSize) {
    // Add all coefficients
    for (double detail : details) {
        noiseWindow[noiseWindowIndex] = Math.abs(detail);
        noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
    }
} else {
    // Sample uniformly
    int step = details.length / noiseWindowSize;
    int added = 0;
    
    for (int i = 0; i < details.length && added < noiseWindowSize; i += step) {
        noiseWindow[noiseWindowIndex] = Math.abs(details[i]);
        noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
        added++;
    }
    
    // Handle remaining slots
    int remaining = noiseWindowSize - added;
    if (remaining > 0) {
        int startIdx = details.length - remaining;
        for (int i = startIdx; i < details.length; i++) {
            noiseWindow[noiseWindowIndex] = Math.abs(details[i]);
            noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
        }
    }
}
```

## Benefits

1. **Preserves temporal diversity**: Samples from across the entire detail array
2. **Prevents data loss**: No longer overwrites the buffer multiple times
3. **Better noise estimates**: More representative sampling of noise characteristics
4. **Handles edge cases**: Works correctly when array length is exact multiple of window size

## Test Coverage
Created comprehensive tests that verify:
- Small detail arrays (all coefficients added)
- Large detail arrays (uniform sampling)
- Multiple block processing (window wrapping)
- Consistency between identical denoisers
- Edge case of exact multiples

## Performance Impact
- Minimal overhead for the common case (small arrays)
- Slightly more computation for large arrays, but provides better noise estimation
- Still maintains O(n) complexity where n is the number of coefficients
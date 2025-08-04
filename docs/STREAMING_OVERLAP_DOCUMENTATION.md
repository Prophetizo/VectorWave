# Streaming MODWT Overlap Documentation Improvements

## Overview
Enhanced the documentation in `MODWTStreamingTransformImpl` to clearly explain the sliding window overlap mechanism as requested by Copilot.

## Documentation Improvements

### 1. Inline Implementation Comments
Added detailed explanation at the point where overlap is handled:
```java
// Sliding window overlap mechanism:
// - We processed a full buffer of 'bufferSize' samples
// - The last 'overlapSize' samples need to be included in the next window
// - Therefore, we only consumed (bufferSize - overlapSize) new samples
// 
// Example with bufferSize=1024, overlapSize=256:
// - Window 1: samples [0...1023]     (processes 1024 samples)
// - Window 2: samples [768...1791]   (overlaps last 256 samples)
// - Effective consumption: 1024 - 256 = 768 new samples per window
//
// The circular buffer naturally maintains the overlap samples without copying:
// - Read position stays at the beginning of unprocessed samples
// - The overlap samples remain in the buffer for the next window
```

### 2. Class-Level Documentation
Added comprehensive explanation in the class JavaDoc:
```java
/**
 * <p><strong>Sliding Window Overlap Strategy:</strong></p>
 * <p>To ensure continuity across transform blocks, this implementation uses a sliding
 * window approach with overlap. The overlap size is typically set to the filter length
 * minus 1, which guarantees that boundary effects from one block don't affect the next.</p>
 * 
 * <p>For example, with a buffer size of 1024 and overlap of 256:</p>
 * <ul>
 *   <li>Each transform processes 1024 samples</li>
 *   <li>The last 256 samples are retained for the next window</li>
 *   <li>Only 768 new samples are consumed per transform</li>
 *   <li>This ensures filter state continuity across blocks</li>
 * </ul>
 */
```

## Benefits

1. **Clarity**: The relationship between buffer size, overlap size, and sample consumption is now explicit
2. **Examples**: Concrete numerical examples make the concept easier to understand
3. **Rationale**: Explains why overlap is needed (filter continuity, boundary effects)
4. **Implementation Details**: Clarifies that the circular buffer handles overlap without copying

## Why This Matters

The sliding window overlap mechanism is crucial for streaming wavelet transforms because:
- It ensures filter state continuity across blocks
- It prevents boundary artifacts at block edges
- It leverages MODWT's shift-invariant properties
- It allows seamless real-time processing of continuous data streams

The improved documentation helps future maintainers understand this critical aspect of the implementation.
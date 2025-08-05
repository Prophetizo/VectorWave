# Removal of Unused previousOverlap Array

## Issue
Copilot identified that the `previousOverlap` array was declared and initialized but never actually used in the overlap handling logic.

## Investigation
The streaming MODWT implementation uses a circular buffer approach where:
1. Samples are continuously written to a circular buffer
2. When enough samples are available, a window of `bufferSize` samples is processed
3. After processing, only `(bufferSize - overlapSize)` samples are consumed
4. The remaining `overlapSize` samples stay in the circular buffer for the next window

## Solution
Removed the unused `previousOverlap` array since the circular buffer inherently maintains the overlap:

### Changes Made:
1. Removed the `private double[] previousOverlap;` field declaration
2. Removed the initialization `this.previousOverlap = new double[overlapSize];`
3. Removed the clearing operation `Arrays.fill(previousOverlap, 0.0);`
4. Added clarifying comment explaining why no separate overlap array is needed

### Code Cleanup:
```java
// Before:
private double[] previousOverlap;  // UNUSED
...
this.previousOverlap = new double[overlapSize];  // UNNECESSARY ALLOCATION
...
Arrays.fill(previousOverlap, 0.0);  // UNNECESSARY OPERATION

// After:
// Circular buffer inherently maintains overlap samples
// No separate overlap array needed
```

## Benefits
1. **Memory Efficiency**: Eliminates unnecessary array allocation
2. **Code Clarity**: Removes confusing unused code
3. **Performance**: Eliminates unnecessary array clearing operation
4. **Maintainability**: Clearer implementation without dead code

## Verification
All MODWT tests pass after the removal, confirming that the overlap handling works correctly without the separate array.

The circular buffer approach is actually more elegant because it naturally maintains continuity between windows without any explicit copying of overlap samples.
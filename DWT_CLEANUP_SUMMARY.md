# DWT Cleanup Summary

## Overview
Cleaned up remaining DWT (Discrete Wavelet Transform) remnants in the codebase by clearly marking DWT-specific operations and providing MODWT alternatives.

## Changes Made

### 1. BatchSIMDTransform Deprecation
- **File**: `/src/main/java/ai/prophetizo/wavelet/internal/BatchSIMDTransform.java`
- **Action**: Marked entire class and key methods as `@Deprecated`
- **Reason**: This class implements DWT-specific operations with downsampling (output length = input length / 2)
- **Documentation**: Added clear warnings that this is DWT-specific and outputs are half the input length

### 2. Method Documentation Updates
Updated documentation for all major methods in BatchSIMDTransform:
- `haarBatchTransformSIMD`: Added warning about downsampling
- `blockedBatchTransformSIMD`: Marked as DWT-specific
- `adaptiveBatchTransform`: Clarified DWT nature
- All methods now reference `MODWTTransform.forwardBatch()` as the MODWT alternative

### 3. Created MODWTBatchSIMD
- **File**: `/src/main/java/ai/prophetizo/wavelet/modwt/MODWTBatchSIMD.java`
- **Purpose**: Provide MODWT-specific SIMD utilities without DWT baggage
- **Features**:
  - `getBatchSIMDInfo()`: MODWT-specific SIMD information
  - `isOptimalBatchSize()`: Check if batch size is SIMD-aligned
  - `getOptimalBatchSize()`: Calculate optimal batch size for SIMD
  - Clear documentation about MODWT characteristics (no downsampling, shift-invariant)

### 4. Demo Updates
- **MODWTBatchProcessingDemo**: Now uses `MODWTBatchSIMD.getBatchSIMDInfo()`
- **BatchProcessingDemo**: Added comment clarifying that `BatchSIMDTransform` is DWT-specific

## Key Differences: DWT vs MODWT

| Aspect | DWT (BatchSIMDTransform) | MODWT (MODWTTransform) |
|--------|--------------------------|------------------------|
| Output Length | Input length / 2 | Same as input length |
| Shift-invariant | No | Yes |
| Signal Length | Power of 2 preferred | Any length |
| Downsampling | Yes | No |
| Use Cases | Compression, compact representation | Pattern detection, time series |

## Migration Path

For users still needing DWT functionality:
1. Continue using `BatchSIMDTransform` (now clearly marked as DWT)
2. Be aware of the downsampling behavior
3. Ensure output arrays are sized appropriately (half input length)

For MODWT users:
1. Use `MODWTTransform.forwardBatch()` for batch processing
2. Use `MODWTBatchSIMD` for SIMD information and utilities
3. Enjoy shift-invariance and arbitrary signal lengths

## Future Considerations

1. **Potential Removal**: `BatchSIMDTransform` could be removed entirely in a future major version if DWT support is no longer needed
2. **MODWT Optimization**: The MODWT batch methods could be enhanced with specialized SIMD implementations
3. **Clear Separation**: Consider moving DWT-specific code to a separate package or module

## Testing
All tests continue to pass after the cleanup, confirming that the changes are backward-compatible while providing clear guidance for users.
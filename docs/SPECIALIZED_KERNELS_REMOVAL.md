# Specialized Kernels Removal Summary

## Overview
As per the user's request, all specialized kernel implementations have been removed from the `MODWTOptimizedTransformEngine` class. Performance improvements should be incorporated into the canonical implementations rather than maintained as separate specialized kernels.

## Changes Made

### 1. MODWTOptimizedTransformEngine.java
- Removed the `useSpecializedKernels` field and `ENABLE_EXPERIMENTAL_KERNELS` flag
- Removed the `transformWithSpecializedKernel()` method
- Removed the `hasSpecializedKernel()` method  
- Removed the `modwtHaarOptimized()` method
- Removed the `modwtDB4Optimized()` method
- Updated the `transform()` method to remove specialized kernel logic
- Removed the `withSpecializedKernels()` method from `EngineConfig` class
- Updated all documentation to remove references to specialized kernels

### 2. MODWTOptimizedTransformEngineTest.java
- Removed the `.withSpecializedKernels(true)` call from `testCustomEngineConfig()`
- Converted `testNoSpecializedKernels()` to `testNoSoALayout()` to test a different configuration option

## Rationale
The specialized kernels were:
1. Not properly tested for mathematical correctness
2. Causing test failures when enabled
3. Adding unnecessary complexity to the codebase
4. Not providing clear performance benefits

By removing them, we:
- Simplify the codebase
- Ensure all optimizations go through proper testing in the canonical implementations
- Maintain mathematical correctness as the primary goal
- Follow the principle of having one correct implementation rather than multiple potentially inconsistent ones

## Future Optimizations
Any performance improvements should be:
1. Implemented directly in the canonical transform methods
2. Properly tested for mathematical correctness
3. Benchmarked to prove performance benefits
4. Applied consistently across all wavelets rather than special-cased

## Test Results
All tests pass after the removal:
- 894 tests run
- 0 failures
- 0 errors  
- 3 skipped (unrelated performance tests)

The removal has been completed successfully with no impact on the correctness or functionality of the MODWT implementation.
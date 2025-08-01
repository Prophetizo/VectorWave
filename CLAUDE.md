# CLAUDE.md

Development guide for Claude Code when working with the VectorWave repository.

## Recent Updates (November 2024)

### Foreign Function & Memory API (FFM) Support
- Added comprehensive FFM implementation in `ai.prophetizo.wavelet.memory.ffm` package
- Provides zero-copy operations with SIMD-aligned memory
- Thread-safe memory pooling with automatic cleanup
- Requires Java 23+ with `--enable-native-access=ALL-UNNAMED` flag

### Known Issues
1. **CRITICAL: Biorthogonal Wavelets (#138)**
   - BiorthogonalSpline implementation produces catastrophic reconstruction errors (RMSE > 1.4)
   - Incorrect filter coefficients and violated perfect reconstruction conditions
   - Test `FFMWaveletTransformTest.testBiorthogonalWavelets()` is disabled
   - **Workaround**: Use orthogonal wavelets (Haar, Daubechies, Symlets) instead

2. **Boundary Mode Limitations (#135-137)**
   - SYMMETRIC and CONSTANT modes not implemented for FFM upsampling operations
   - Will throw `UnsupportedOperationException` if used
   - Downsampling operations support all boundary modes

3. **FFM Memory Pool Arena Validation**
   - Memory segments from different arenas now throw `IllegalArgumentException` instead of silent failure
   - Helps catch programming errors during development

## Package Refactoring Tasks

### Complex.java Location Refactoring
- Problem: `Complex.java` is currently located in root wavelet package
- Suggested new locations:
  - `ai.prophetizo.wavelet.util`
  - `ai.prophetizo.wavelet.cwt.util`
  - `ai.prophetizo.wavelet.math`
- Considerations:
  - Used by multiple packages
  - Part of public API
  - Do not maintain backward compatibility

[Rest of the existing file content remains the same...]
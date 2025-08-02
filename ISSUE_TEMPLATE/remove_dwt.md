# Proposed Removal of Discrete Wavelet Transform (DWT) from VectorWave

## Issue Summary
This issue aims to track the removal of Discrete Wavelet Transform (DWT) from the VectorWave library. The proposed plan outlines the rationale, impacted wavelets, steps for removal, and a timeline for the transition. We encourage community involvement and feedback throughout this process.

## Wavelets Impacted
- **Haar**
- **Daubechies** (DB2, DB4, etc.)

## Steps for Removal
1. **Deprecation Phase**:  
   Mark DWT as deprecated in the next release, providing a warning for users.
2. **Documentation Update**:  
   Update all references in the documentation to highlight MODWT and CWT as alternatives.
3. **Codebase Cleanup**:  
   Gradually remove DWT-related code in subsequent releases.
4. **Testing**:  
   Ensure all existing tests for DWT are either adapted for MODWT/CWT or removed if no longer relevant.

## Rationale
- **MODWT**: Provides shift invariance, works with arbitrary signal lengths, and maintains non-decimated outputs.
- **CWT**: Offers continuous and scalable wavelet analysis for applications requiring high resolution.
- **DWT**: Its advantages are largely superseded by MODWT and CWT in practical finance scenarios.

## Timeline
- **Deprecation Warning**: Immediate (next release).
- **Documentation Updates**: Within 1 month.
- **Code Removal**: Within 3 months.

## Feedback Mechanism
We solicit community feedback on the timeline and approach via GitHub Discussions or Issues. Your input is valuable to ensure a smooth transition for all users.

# CRASH_ASYMMETRY_THRESHOLD Clarification

## Issue Description
Issue #91 identified an inconsistency regarding the `CRASH_ASYMMETRY_THRESHOLD` value between the PR #80 description and the actual implementation.

## The Inconsistency
- **PR #80 Description**: Claims the original hardcoded value was `CRASH_ASYMMETRY_THRESHOLD = 10.0`
- **Original Implementation**: Used `DEFAULT_CRASH_ASYMMETRY_THRESHOLD = 0.7`
- **Test Expectations**: Expected the default value to be `0.7`

## Resolution
The **0.7 value was correct** because:

1. The `analyzeCrashAsymmetry()` method returns a ratio between 0 and 1
2. The implementation included a comment: "Crash asymmetry threshold: The analyzeCrashAsymmetry() method returns a ratio between 0 and 1."
3. A threshold of 10.0 would be impossible to exceed for a method that returns values between 0 and 1

## Current Status (August 2025)
- **All defaults have been removed** as part of a broader initiative to require explicit configuration
- Users must now specify all financial thresholds through `FinancialAnalysisConfig.builder()`
- The crash asymmetry threshold must be explicitly set based on:
  - The specific market being analyzed
  - Risk tolerance of the analysis
  - Historical volatility patterns
- Common values range from 0.5 to 0.9, with 0.7 being a reasonable middle ground for many applications

## Impact
This change ensures that users consciously choose appropriate thresholds for their specific use case rather than relying on potentially inappropriate defaults.
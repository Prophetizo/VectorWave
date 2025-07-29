#!/bin/bash

# Script to create GitHub issues from code review
# Usage: ./create-code-review-issues.sh

echo "Creating GitHub issues for VectorWave CWT code review..."

# P0 - Critical Issues (Blockers)

gh issue create \
  --title "[P0] Fix FFT Circular Convolution Artifacts" \
  --label "bug,critical,cwt" \
  --body "## Problem
The FFT implementation in CWTTransform uses circular convolution which can introduce artifacts at signal boundaries.

## Current Implementation
- Location: \`CWTTransform.java\` lines 675-711
- Uses circular wrapping without proper zero-padding
- Can cause incorrect results near signal edges

## Solution
1. Implement proper zero-padding to length \`signal_length + wavelet_support - 1\`
2. Use linear convolution instead of circular
3. Consider implementing real-to-complex FFT for 2x speedup

## Acceptance Criteria
- [ ] No circular convolution artifacts in CWT results
- [ ] Unit tests verify correct edge handling
- [ ] Performance benchmarks show no significant regression

## References
- See CODE_REVIEW_SUMMARY.md for details
- Mathematical validation required against reference implementations"

gh issue create \
  --title "[P0] Standardize Wavelet Normalization" \
  --label "bug,critical,cwt" \
  --body "## Problem
Inconsistent normalization across different wavelet types affects cross-wavelet comparisons.

## Current State
- Some wavelets normalize in constructor
- Others normalize in discretize() method
- Energy preservation not consistently verified

## Solution
1. Define clear normalization strategy (L2 norm = 1)
2. Implement consistent normalization in base class
3. Add validation tests for all wavelets

## Affected Files
- All wavelet implementations in \`api\` and \`cwt.finance\` packages
- Wavelet base interfaces

## Acceptance Criteria
- [ ] All wavelets use same normalization approach
- [ ] Unit tests verify L2 norm = 1 for all wavelets
- [ ] Cross-wavelet comparisons produce consistent results"

gh issue create \
  --title "[P0] Fix Package Organization - Misplaced Demo Files" \
  --label "refactoring,critical" \
  --body "## Problem
Several demo files are in incorrect packages, causing confusion and poor organization.

## Misplaced Files
1. In \`ai.prophetizo\` (should be in \`ai.prophetizo.demo\`):
   - ErrorHandlingDemo.java
   - FinancialOptimizationDemo.java
   - OptimizationDemo.java

2. In \`ai.prophetizo.wavelet.cwt\` (should be in \`ai.prophetizo.demo.cwt\`):
   - CWTPerformanceDemo.java

3. Duplicate files:
   - OptimizationDemo.java exists in multiple locations

## Solution
1. Move all demo files to appropriate demo packages
2. Remove duplicates
3. Update any references

## Acceptance Criteria
- [ ] All demo files in correct packages
- [ ] No duplicate files
- [ ] Build still passes
- [ ] Update CLAUDE.md if needed"

# P1 - High Priority Issues

gh issue create \
  --title "[P1] Reduce Memory Allocations in FinancialWaveletAnalyzer" \
  --label "performance,cwt,enhancement" \
  --body "## Problem
FinancialWaveletAnalyzer creates many temporary arrays and objects in hot paths, causing GC pressure.

## Hot Spots
1. \`analyzeVolatility()\` - lines 209-241: new arrays for each call
2. \`generateTradingSignals()\` - lines 415-449: many TradingSignal objects
3. Helper methods - lines 783-823: temporary array allocations

## Solution
1. Implement object pooling for result objects
2. Pre-allocate and reuse arrays
3. Use primitive collections instead of ArrayList<Double>
4. Consider streaming calculations

## Expected Impact
- 40-60% reduction in GC pressure
- Improved latency for real-time analysis

## Acceptance Criteria
- [ ] Object pool implementation
- [ ] Benchmark shows GC reduction
- [ ] No regression in functionality"

gh issue create \
  --title "[P1] Replace Hardcoded Thresholds with Configurable Parameters" \
  --label "enhancement,cwt,financial" \
  --body "## Problem
Financial analysis uses hardcoded thresholds that limit flexibility for different markets/instruments.

## Hardcoded Values
- CRASH_ASYMMETRY_THRESHOLD = 10.0
- VOLATILITY_LOW_THRESHOLD = 0.5
- REGIME_TREND_THRESHOLD = 0.02
- Various other constants

## Solution
1. Create FinancialAnalysisConfig class
2. Move all thresholds to configuration
3. Provide sensible defaults
4. Allow per-analysis customization

## Acceptance Criteria
- [ ] All hardcoded thresholds moved to config
- [ ] Config can be passed to analyzer
- [ ] Backward compatibility maintained
- [ ] Documentation of all parameters"

gh issue create \
  --title "[P1] Add Configurable Risk-Free Rate to Sharpe Ratio" \
  --label "bug,financial,enhancement" \
  --body "## Problem
Sharpe ratio calculation assumes risk-free rate of 0, which is incorrect for most use cases.

## Current Implementation
- Location: \`FinancialWaveletAnalyzer.calculateSharpeRatio()\`
- Hardcoded risk-free rate = 0
- No way to configure

## Solution
1. Add riskFreeRate parameter to configuration
2. Update Sharpe ratio calculation
3. Default to current treasury rate or allow user input

## Acceptance Criteria
- [ ] Risk-free rate configurable
- [ ] Default value documented
- [ ] Unit tests with various rates
- [ ] Documentation updated"

# P2 - Medium Priority Issues

gh issue create \
  --title "[P2] Implement In-Place FFT with Pre-computed Twiddle Factors" \
  --label "performance,enhancement,cwt" \
  --body "## Opportunity
Current FFT implementation creates many temporary arrays and recalculates twiddle factors.

## Optimization Plan
1. Implement iterative in-place Cooley-Tukey FFT
2. Pre-compute and cache twiddle factors for common sizes
3. Use bit-reversal permutation
4. Consider mixed-radix for non-power-of-2

## Expected Impact
- 30-50% speedup for CWT operations
- Reduced memory allocation
- Better cache efficiency

## Implementation Notes
- Maintain current API
- Add configuration option for algorithm selection
- Benchmark against current implementation"

gh issue create \
  --title "[P2] Add Foreign Function & Memory API Support" \
  --label "enhancement,java23,performance" \
  --body "## Opportunity
Java 23's Foreign Function & Memory API can improve memory management and enable native acceleration.

## Use Cases
1. Direct memory management for large arrays
2. SIMD-aligned memory allocation
3. Native BLAS/LAPACK integration
4. Zero-copy operations

## Implementation Plan
1. Create MemorySegment-based array pools
2. Use Arena for scoped memory management
3. Benchmark against current implementation
4. Maintain backward compatibility

## Benefits
- Reduced GC pressure
- Better SIMD alignment
- Potential for native acceleration"

gh issue create \
  --title "[P2] Implement Ring Buffer for Streaming Components" \
  --label "performance,enhancement,streaming" \
  --body "## Problem
Streaming components use array copying which impacts performance.

## Current Implementation
- StreamingWaveletTransformImpl creates buffer copies
- OverlapBuffer uses array shifting

## Solution
1. Implement lock-free ring buffer
2. Zero-copy processing where possible
3. Optimize for cache efficiency

## Expected Impact
- 50% reduction in memory bandwidth
- Lower latency for streaming
- Better scalability

## Acceptance Criteria
- [ ] Ring buffer implementation
- [ ] Integration with streaming components
- [ ] Performance benchmarks
- [ ] Thread safety maintained"

gh issue create \
  --title "[P2] Create Common Factory Interface" \
  --label "refactoring,enhancement,architecture" \
  --body "## Problem
Multiple factory implementations without common interface makes it harder to maintain consistency.

## Current Factories
- WaveletOpsFactory
- CWTFactory
- StreamingDenoiserFactory
- WaveletTransformFactory

## Solution
1. Define common Factory<T> interface
2. Standardize factory method names
3. Add factory registry if needed
4. Document factory pattern usage

## Benefits
- Better consistency
- Easier to add new factories
- Clearer architecture"

# Performance Optimization Issues

gh issue create \
  --title "[Performance] Implement Complex Number SIMD Vectorization" \
  --label "performance,enhancement,simd" \
  --body "## Opportunity
Complex number operations in CWT are not fully vectorized.

## Current State
- Complex arithmetic done element-by-element
- CWTVectorOps doesn't optimize complex operations

## Solution
1. Pack real/imaginary parts for SIMD
2. Use interleaved or split layout based on operation
3. Optimize common patterns (multiply, conjugate)

## Expected Impact
- 20-30% speedup for complex wavelets
- Better cache utilization

## Implementation
- Extend VectorOps with complex operations
- Benchmark different layouts
- Platform-specific optimizations"

gh issue create \
  --title "[Performance] Implement Real-to-Complex FFT" \
  --label "performance,enhancement,cwt" \
  --body "## Opportunity
For real input signals, specialized real-to-complex FFT is 2x faster.

## Current Implementation
- Treats all signals as complex
- Wastes computation on zero imaginary parts

## Solution
1. Detect real input signals
2. Use specialized real FFT algorithm
3. Pack/unpack results efficiently

## Expected Impact
- 2x speedup for real signal processing
- Reduced memory usage

## Notes
- Common case for financial data
- Maintain API compatibility"

# Documentation Issues

gh issue create \
  --title "[Docs] Add Missing package-info.java Files" \
  --label "documentation,enhancement" \
  --body "## Problem
Several packages missing package-info.java documentation.

## Missing From
- ai.prophetizo.wavelet.cwt
- ai.prophetizo.wavelet.denoising  
- ai.prophetizo.wavelet.concurrent
- ai.prophetizo.wavelet.memory

## Solution
Add comprehensive package documentation including:
- Package purpose
- Key classes
- Usage examples
- Design decisions

## Template
Use existing package-info.java files as templates"

gh issue create \
  --title "[Docs] Document Memory Pool Lifecycle Management" \
  --label "documentation,enhancement" \
  --body "## Problem
No clear guidance on when to create, clear, or dispose of memory pools.

## Need Documentation For
- Pool lifecycle best practices
- When to clear pools
- Thread safety considerations
- Performance implications

## Solution
1. Add lifecycle section to documentation
2. Provide code examples
3. Add to CLAUDE.md
4. Consider automatic lifecycle management"

# Architecture Issues

gh issue create \
  --title "[Architecture] Move Complex.java to Appropriate Package" \
  --label "refactoring,enhancement" \
  --body "## Problem
Complex.java is in root wavelet package but should be in a more specific location.

## Current Location
ai.prophetizo.wavelet.Complex

## Suggested Locations
- ai.prophetizo.wavelet.util
- ai.prophetizo.wavelet.cwt.util
- ai.prophetizo.wavelet.math

## Considerations
- Used by multiple packages
- Part of public API
- Backward compatibility"

gh issue create \
  --title "[Architecture] Consider ServiceLoader for Wavelet Discovery" \
  --label "enhancement,architecture" \
  --body "## Problem
WaveletRegistry uses static initialization which could cause circular dependencies.

## Current Implementation
- Static block registers all wavelets
- Manual registration required

## Solution
1. Use ServiceLoader mechanism
2. Automatic wavelet discovery
3. Plugin architecture support

## Benefits
- No circular dependencies
- Extensible architecture
- Cleaner initialization"

echo "✅ All issues created successfully!"
echo ""
echo "Summary:"
echo "- P0 (Critical): 3 issues"
echo "- P1 (High Priority): 3 issues"  
echo "- P2 (Medium Priority): 4 issues"
echo "- Performance: 2 issues"
echo "- Documentation: 2 issues"
echo "- Architecture: 2 issues"
echo ""
echo "Total: 16 issues created"
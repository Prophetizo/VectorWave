#!/bin/bash

# Script to create GitHub issues for code review recommendations

echo "Creating GitHub issues for code review recommendations..."

# Issue 2: Document FFM Requirements
gh issue create \
  --title "Document FFM (Foreign Function & Memory) API requirements" \
  --body "## Description
The Foreign Function & Memory API implementation requires specific JVM flags but this is not prominently documented.

## Current State
- FFM implementation exists in \`ai.prophetizo.wavelet.memory.ffm\` package
- Requires Java 23+ with \`--enable-native-access=ALL-UNNAMED\` flag
- Not clearly documented in README or getting started guides

## Recommended Actions
1. Add FFM requirements section to README.md
2. Update getting started documentation
3. Add runtime detection with helpful error message when FFM is used without proper flags
4. Consider adding a \`docs/FFM_SETUP.md\` guide with detailed instructions
5. Update demo scripts to include proper JVM flags

## Acceptance Criteria
- [ ] README.md includes FFM requirements in a prominent section
- [ ] Error messages guide users to enable FFM when not configured
- [ ] Demo scripts include proper JVM flags
- [ ] Documentation explains performance benefits of FFM

## References
- FFM API: \`src/main/java/ai/prophetizo/wavelet/memory/ffm/\`
- Related PR: #(current PR number)" \
  --label "documentation" \
  --label "enhancement"

# Issue 3: Integrate JMH benchmarks into CI/CD
gh issue create \
  --title "Integrate JMH benchmarks into CI/CD pipeline" \
  --body "## Description
The codebase includes comprehensive JMH benchmarks but they are not integrated into the CI/CD pipeline for continuous performance monitoring.

## Current State
- Multiple benchmark classes exist in \`src/test/java/ai/prophetizo/wavelet/benchmark/\`
- Benchmarks cover critical paths: SIMD, FFT, CWT, streaming
- No automated performance regression detection

## Recommended Actions
1. Create a GitHub Action workflow for running JMH benchmarks
2. Set up performance baseline storage (could use GitHub Pages or artifacts)
3. Add performance regression detection with configurable thresholds
4. Create performance trend visualization
5. Optional: Add benchmark results as PR comments

## Acceptance Criteria
- [ ] GitHub Action runs JMH benchmarks on PR and main branch
- [ ] Performance regressions are automatically detected
- [ ] Benchmark results are stored and accessible
- [ ] Documentation explains how to run benchmarks locally
- [ ] Performance trends are visible over time

## Technical Considerations
- Consider using \`jmh-maven-plugin\` for integration
- May need to limit benchmark scope for PR builds (time constraints)
- Full benchmark suite could run nightly on main branch

## References
- Benchmark classes: \`src/test/java/ai/prophetizo/wavelet/benchmark/\`
- JMH runner script: \`jmh-runner.sh\`" \
  --label "enhancement" \
  --label "performance" \
  --label "ci/cd"

# Issue 4: Add thread-local cleanup documentation for application servers
gh issue create \
  --title "Document thread-local cleanup requirements for application server deployments" \
  --body "## Description
Several classes use ThreadLocal storage for performance optimization but lack clear documentation about cleanup requirements in application server environments.

## Current State
- \`BatchSIMDTransform\` uses ThreadLocal for temporary arrays
- \`OptimizedFFT\` uses ThreadLocal for vector operations
- Cleanup method exists: \`BatchSIMDTransform.cleanupThreadLocals()\`
- Not documented in deployment guides

## Recommended Actions
1. Create deployment guide for application servers (Tomcat, Jetty, etc.)
2. Document all classes that use ThreadLocal storage
3. Provide example servlet context listener for cleanup
4. Add automatic cleanup in streaming components' close() methods
5. Consider adding a global cleanup utility

## Acceptance Criteria
- [ ] Deployment guide covers thread-local cleanup
- [ ] All ThreadLocal usage is documented
- [ ] Example cleanup code provided
- [ ] Warning added to relevant class JavaDocs
- [ ] Consider adding \`@PreDestroy\` annotations for Spring users

## Code Locations
- \`BatchSIMDTransform.cleanupThreadLocals()\`
- \`OptimizedFFT\` ThreadLocal usage
- Other ThreadLocal usage in the codebase

## Example Cleanup Pattern
\`\`\`java
@WebListener
public class WaveletCleanupListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        BatchSIMDTransform.cleanupThreadLocals();
        // Add other cleanup calls
    }
}
\`\`\`" \
  --label "documentation" \
  --label "enhancement"

# Issue 5: Add comprehensive breaking changes to CHANGELOG.md
gh issue create \
  --title "Update CHANGELOG.md with comprehensive list of breaking changes" \
  --body "## Description
The current feature branch introduces significant breaking changes that need to be documented in CHANGELOG.md for users upgrading from previous versions.

## Breaking Changes to Document

### Removed Features
1. **Default Financial Configurations**
   - Removed \`FinancialConfig\` default constructor
   - Removed \`FinancialAnalysisConfig.defaultConfig()\`
   - Removed \`FinancialAnalyzer.withDefaultConfig()\`
   - Removed default risk-free rate constant

2. **API Changes**
   - \`Complex\` class moved (location TBD based on refactoring)
   - Transform results now use sealed interfaces
   - Some factory methods have changed signatures

### New Requirements
1. Java 23+ for full feature set
2. \`--enable-native-access=ALL-UNNAMED\` for FFM features
3. \`--add-modules jdk.incubator.vector\` for SIMD optimizations

### Migration Required
1. Financial analysis now requires explicit configuration
2. Wavelet selection may need updates for new registry system
3. Streaming API has new interfaces

## Acceptance Criteria
- [ ] CHANGELOG.md includes all breaking changes
- [ ] Migration guide provided for each breaking change
- [ ] Version numbering reflects semantic versioning rules
- [ ] Examples show before/after code
- [ ] Deprecation notices added where applicable

## References
- Current CHANGELOG.md
- CLAUDE.md recent updates section
- Git diff of API changes" \
  --label "documentation" \
  --label "breaking-change"

echo "GitHub issues created successfully!"
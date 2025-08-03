# Contributing to VectorWave

Thank you for your interest in contributing to VectorWave! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

Please be respectful and professional in all interactions. We aim to maintain a welcoming and inclusive environment for all contributors.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/yourusername/VectorWave.git
   cd VectorWave
   ```
3. **Set up development environment**:
   - Install Java 23 or later
   - Install Maven 3.6+
   - Run `mvn clean compile` to verify setup

## Development Process

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-description
```

### 2. Make Your Changes

- **Follow existing code style** - match the patterns in surrounding code
- **Write tests** for new functionality
- **Update documentation** as needed
- **Ensure all tests pass**: `mvn test`

### 3. Code Standards

#### MODWT Focus
- VectorWave uses MODWT (Maximal Overlap Discrete Wavelet Transform) as its primary transform
- Ensure new features work with arbitrary signal lengths (no power-of-2 restrictions)
- Maintain shift-invariance properties where applicable

#### Java Style
- Use meaningful variable and method names
- Keep methods focused and reasonably sized
- Add Javadoc comments for public APIs
- Use final for immutable fields
- Prefer composition over inheritance

#### Performance Considerations
- Consider SIMD optimization opportunities
- Use memory pools for repeated allocations
- Profile performance-critical code
- Document any performance trade-offs

### 4. Testing

All new code should include tests:

```java
@Test
void testNewFeature() {
    // Given - setup test data
    double[] signal = {1, 2, 3, 4, 5, 6, 7}; // MODWT works with any length!
    MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    
    // When - execute operation
    MODWTResult result = transform.forward(signal);
    
    // Then - verify results
    assertNotNull(result);
    assertEquals(signal.length, result.approximationCoeffs().length);
}
```

### 5. Documentation

Update relevant documentation:
- Add/update Javadoc comments
- Update README.md if adding new features
- Update API.md for new public APIs
- Add examples to EXAMPLES.md if appropriate

### 6. Commit Your Changes

Write clear, descriptive commit messages:

```bash
git add .
git commit -m "feat: Add multi-channel MODWT batch processing

- Implement parallel processing for multiple channels
- Add SIMD optimization for channel operations
- Include comprehensive unit tests
- Update documentation with usage examples"
```

Follow conventional commit format:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `test:` Test additions/changes
- `perf:` Performance improvements
- `refactor:` Code refactoring

### 7. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub with:
- Clear title describing the change
- Description of what was changed and why
- Reference to any related issues
- Test results or benchmarks if relevant

## Types of Contributions

### Bug Reports

File issues on GitHub with:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Java version and system information
- Minimal code example if possible

### Feature Requests

Open a discussion or issue with:
- Use case description
- Proposed API or behavior
- Examples of how it would be used
- Any performance considerations

### Code Contributions

We welcome:
- Bug fixes
- Performance improvements
- New wavelet implementations
- Documentation improvements
- Test coverage improvements
- Example demos

### Adding New Wavelets

To add a new wavelet type:

1. Implement the appropriate interface:
   - `OrthogonalWavelet` for orthogonal wavelets
   - `BiorthogonalWavelet` for biorthogonal wavelets
   - `ContinuousWavelet` for continuous wavelets

2. Register in `WaveletRegistry`:
   ```java
   registry.register("myWavelet", new MyWavelet());
   ```

3. Add comprehensive tests
4. Update documentation

See [Adding Wavelets Guide](docs/ADDING_WAVELETS.md) for details.

## Performance Contributions

When optimizing performance:

1. **Benchmark first** - establish baseline
2. **Profile the code** - identify bottlenecks
3. **Implement optimization**
4. **Benchmark again** - verify improvement
5. **Document the optimization**

Use the JMH benchmarking framework:
```bash
./jmh-runner.sh YourBenchmark
```

## Questions?

- Check existing issues and discussions
- Review the documentation
- Ask in GitHub Discussions
- Contact maintainers if needed

## License

By contributing, you agree that your contributions will be licensed under the same GPL-3.0 license as the project.

Thank you for contributing to VectorWave!
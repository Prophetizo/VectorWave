# VectorWave Examples Guide

## Quick Start

```bash
# Simplest possible example - just run this!
mvn compile && mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.basic.GettingStarted"
```

## Organization

All runnable examples are now organized in `src/main/java/ai/prophetizo/examples/`:

```
examples/
├── basic/              # Start here
│   ├── GettingStarted.java        # Minimal example - 5 steps to wavelets
│   └── WaveletRegistryBasics.java # Registry operations and safety
│
├── advanced/           # (Future) Advanced features
│
└── integration/        # External system integration
    └── MotiveWaveIntegration.java # Trading platform example
```

## Why This Organization?

Previously, demos and examples were scattered across:
- `src/main/java/ai/prophetizo/demo/` (40+ files, excluded from compilation)
- `src/test/java/ai/prophetizo/examples/`
- Various other locations

Now everything is:
- ✅ **Organized** - Clear categories (basic/advanced/integration)
- ✅ **Compiled** - Part of main source, not excluded
- ✅ **Runnable** - Simple Maven commands
- ✅ **Documented** - Each example has clear purpose

## Running Examples

```bash
# Compile once
mvn compile

# Run any example
mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.[category].[ClassName]"
```

## Key Examples

### 1. Getting Started (30 seconds)
The absolute minimum to understand VectorWave:
```bash
mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.basic.GettingStarted"
```

### 2. Registry Basics (2 minutes)
Learn the WaveletRegistry API:
```bash
mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.basic.WaveletRegistryBasics"
```

### 3. MotiveWave Integration (5 minutes)
See how to use in trading platforms:
```bash
mvn exec:java -Dexec.mainClass="ai.prophetizo.examples.integration.MotiveWaveIntegration"
```

## Key Points

- **No ServiceLoader** - Simple static registry that works everywhere
- **No Complex Setup** - Just `WaveletRegistry.getWavelet("db4")`
- **MODWT Focus** - Works with ANY signal length
- **Production Ready** - Safe patterns for real applications

## For More Details

See [src/main/java/ai/prophetizo/examples/README.md](src/main/java/ai/prophetizo/examples/README.md) for complete documentation.
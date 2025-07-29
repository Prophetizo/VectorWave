# Factory Pattern Usage in VectorWave

This document describes the factory pattern implementation and usage in the VectorWave wavelet transform library.

## Overview

VectorWave implements a consistent factory pattern across all major components to provide:
- **Consistency**: Standardized creation and configuration APIs
- **Extensibility**: Easy addition of new factory implementations
- **Type Safety**: Compile-time validation of factory products
- **Flexibility**: Support for both simple and complex object creation scenarios

## Common Factory Interface

All factories in VectorWave implement the `Factory<T>` interface:

```java
public interface Factory<T> {
    T create();
    String getDescription();
    Class<T> getProductType();
}
```

### Key Features
- **Generic Type Safety**: `T` represents the product type the factory creates
- **Standard Creation**: `create()` method provides parameterless creation
- **Introspection**: `getDescription()` and `getProductType()` support factory discovery
- **Extensibility**: Factories can add their own specialized creation methods

## Available Factories

### 1. WaveletTransformFactory

Creates `WaveletTransform` instances with configurable boundary modes.

```java
// Basic usage
WaveletTransform transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(new Haar());

// Static convenience method
WaveletTransform transform2 = WaveletTransformFactory.createDefault(Daubechies.DB4);
```

**Configuration Options:**
- `withBoundaryMode(BoundaryMode)`: Set boundary handling (PERIODIC or ZERO_PADDING)

### 2. CWTFactory

Creates `FFTAcceleratedCWT` instances for continuous wavelet transforms.

```java
// Default configuration
FFTAcceleratedCWT cwt = CWTFactory.createDefault();

// With custom settings
FFTAcceleratedCWT cwt2 = new CWTFactory()
    .withOptimizations(true)
    .create();
```

**Configuration Options:**
- `withOptimizations(boolean)`: Enable/disable performance optimizations

### 3. WaveletOpsFactory

Creates `WaveletOpsConfig` instances for operation configuration.

```java
// Default configuration
WaveletOpsConfig config = WaveletOpsFactory.createDefault();

// Custom configuration
WaveletOpsConfig config2 = new WaveletOpsFactory()
    .withBoundaryMode(BoundaryMode.ZERO_PADDING)
    .withOptimizationLevel(OptimizationLevel.AGGRESSIVE)
    .withVectorization(false)
    .create();
```

**Configuration Options:**
- `withBoundaryMode(BoundaryMode)`: Set boundary handling mode
- `withOptimizationLevel(OptimizationLevel)`: Set optimization level (BASIC, STANDARD, AGGRESSIVE)
- `withVectorization(boolean)`: Enable/disable vectorization

### 4. StreamingDenoiserFactory

Placeholder for future streaming denoising functionality.

```java
// This will be available in future versions
StreamingDenoiser denoiser = new StreamingDenoiserFactory()
    .withBufferSize(1024)
    .withThresholdStrategy(ThresholdStrategy.ADAPTIVE)
    .create();
```

## Factory Registry

The `FactoryRegistry` provides centralized access to all factories:

### Discovery

```java
// Get all available factories
Collection<Factory<?>> allFactories = FactoryRegistry.getAllFactories();

// Get factory names
Set<String> names = FactoryRegistry.getFactoryNames();

// Check if factory exists
boolean exists = FactoryRegistry.isRegistered("WaveletTransform");
```

### Retrieval

```java
// Get factory by name
Optional<Factory<?>> factory = FactoryRegistry.getFactory("CWT");

// Get typed factory
Optional<Factory<WaveletTransform>> transformFactory = 
    FactoryRegistry.getFactory("WaveletTransform", WaveletTransform.class);

// Get all factories for a type
List<Factory<FFTAcceleratedCWT>> cwtFactories = 
    FactoryRegistry.getFactoriesForType(FFTAcceleratedCWT.class);
```

### Custom Registration

```java
// Register custom factory
FactoryRegistry.registerFactory("MyCustom", new MyCustomFactory());

// Unregister factory
FactoryRegistry.unregisterFactory("MyCustom");

// Reset to built-in factories only
FactoryRegistry.reset();
```

## Design Patterns

### Fluent Configuration

All factories support method chaining for configuration:

```java
WaveletTransform transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(wavelet);
```

### Static Convenience Methods

Each factory provides static methods for common use cases:

```java
// Instead of: new WaveletTransformFactory().create(wavelet)
WaveletTransform transform = WaveletTransformFactory.createDefault(wavelet);
```

### Immutable Configuration

Factory configurations are captured at creation time:

```java
WaveletTransformFactory factory = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC);

WaveletTransform t1 = factory.create(wavelet1); // Uses PERIODIC
factory.withBoundaryMode(BoundaryMode.ZERO_PADDING);
WaveletTransform t2 = factory.create(wavelet2); // Uses ZERO_PADDING
// t1 is unaffected by the configuration change
```

## Thread Safety

- **FactoryRegistry**: Thread-safe for concurrent registration and retrieval
- **Individual Factories**: Not thread-safe during configuration, but safe for creation
- **Created Objects**: Thread safety depends on the specific product type

## Extending the Factory Pattern

### Creating Custom Factories

```java
public class MyCustomFactory implements Factory<MyProduct> {
    private MyConfig config = DEFAULT_CONFIG;
    
    public MyCustomFactory withConfig(MyConfig config) {
        this.config = Objects.requireNonNull(config);
        return this;
    }
    
    @Override
    public MyProduct create() {
        return new MyProduct(config);
    }
    
    @Override
    public String getDescription() {
        return "Factory for creating MyProduct instances";
    }
    
    @Override
    public Class<MyProduct> getProductType() {
        return MyProduct.class;
    }
    
    public static MyProduct createDefault() {
        return new MyCustomFactory().create();
    }
}
```

### Registration

```java
// Register with the factory registry
FactoryRegistry.registerFactory("MyCustom", new MyCustomFactory());
```

## Best Practices

1. **Parameterless Creation**: Implement `create()` for simple cases, even if it just provides defaults
2. **Validation**: Validate parameters in configuration methods, not in `create()`
3. **Null Safety**: Use `Objects.requireNonNull()` for parameter validation
4. **Documentation**: Provide clear examples in class-level Javadoc
5. **Static Methods**: Offer `createDefault()` convenience methods for common scenarios
6. **Fluent API**: Return `this` from configuration methods for method chaining
7. **Immutability**: Don't modify configuration after creation unless explicitly documented

## Migration Guide

If you're upgrading from a version without the common factory interface:

### Before (old WaveletTransformFactory usage)
```java
WaveletTransform transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(new Haar());
```

### After (still works - no breaking changes)
```java
// Same code continues to work
WaveletTransform transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(new Haar());

// But now you can also use registry
Factory<?> factory = FactoryRegistry.getFactory("WaveletTransform").get();
// (though you'd need to cast for specific methods)
```

The factory pattern implementation is fully backward compatible - existing code will continue to work without changes.
/**
 * Public API interfaces and wavelet implementations for the VectorWave library.
 * 
 * <p>This package provides the type-safe wavelet hierarchy and all available wavelet
 * implementations. The sealed interface design ensures compile-time validation while
 * supporting extensibility for new wavelet types.</p>
 * 
 * <h2>Wavelet Type Hierarchy:</h2>
 * <pre>
 * {@link ai.prophetizo.wavelet.api.Wavelet} (sealed interface)
 * ├── {@link ai.prophetizo.wavelet.api.DiscreteWavelet}
 * │   ├── {@link ai.prophetizo.wavelet.api.OrthogonalWavelet}
 * │   │   ├── {@link ai.prophetizo.wavelet.api.Haar} - Simplest orthogonal wavelet
 * │   │   ├── {@link ai.prophetizo.wavelet.api.Daubechies} - DB2, DB4, DB6, DB8, DB10, DB12, DB14, DB16, DB18, DB20
 * │   │   ├── {@link ai.prophetizo.wavelet.api.Symlet} - Symlets with improved symmetry
 * │   │   └── {@link ai.prophetizo.wavelet.api.Coiflet} - Coiflets with vanishing moments
 * │   └── {@link ai.prophetizo.wavelet.api.BiorthogonalWavelet}
 * │       └── {@link ai.prophetizo.wavelet.api.BiorthogonalSpline} - Biorthogonal spline wavelets
 * └── {@link ai.prophetizo.wavelet.api.ContinuousWavelet}
 *     └── {@link ai.prophetizo.wavelet.api.MorletWavelet} - Morlet wavelet (discretized for DWT)
 * </pre>
 * 
 * <h2>Core Interfaces:</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.api.Wavelet} - Base interface for all wavelets</li>
 *   <li>{@link ai.prophetizo.wavelet.api.DiscreteWavelet} - Base for discrete wavelets with vanishing moments</li>
 *   <li>{@link ai.prophetizo.wavelet.api.OrthogonalWavelet} - Wavelets where reconstruction equals decomposition filters</li>
 *   <li>{@link ai.prophetizo.wavelet.api.BiorthogonalWavelet} - Wavelets with separate reconstruction filters</li>
 *   <li>{@link ai.prophetizo.wavelet.api.ContinuousWavelet} - Wavelets defined by mathematical functions</li>
 * </ul>
 * 
 * <h2>Supporting Classes:</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.wavelet.api.WaveletRegistry} - Central registry for wavelet discovery and lookup</li>
 *   <li>{@link ai.prophetizo.wavelet.api.WaveletName} - Type-safe enum for all supported wavelets</li>
 *   <li>{@link ai.prophetizo.wavelet.api.TransformType} - Enum for available transform types</li>
 *   <li>{@link ai.prophetizo.wavelet.api.WaveletProvider} - Service Provider Interface for custom wavelets</li>
 *   <li>{@link ai.prophetizo.wavelet.api.WaveletType} - Enum for wavelet categorization</li>
 *   <li>{@link ai.prophetizo.wavelet.api.BoundaryMode} - Enum for boundary handling modes</li>
 *   <li>{@link ai.prophetizo.wavelet.api.Factory} - Standard factory interface for component creation</li>
 *   <li>{@link ai.prophetizo.wavelet.api.FactoryRegistry} - Central registry for factory instances</li>
 * </ul>
 * 
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Create wavelets directly
 * Wavelet haar = new Haar();
 * Wavelet db4 = Daubechies.DB4;
 * Wavelet morlet = new MorletWavelet(6.0, 1.0);
 * 
 * // Discover wavelets through registry using enum-based API
 * Set<WaveletName> available = WaveletRegistry.getAvailableWavelets();
 * Wavelet db4ByName = WaveletRegistry.getWavelet(WaveletName.DB4);
 * List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
 * 
 * // Check transform compatibility
 * Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(WaveletName.DB4);
 * boolean canUseMODWT = WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.MODWT);
 * List<WaveletName> modwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.MODWT);
 * 
 * // Check wavelet properties
 * String name = wavelet.name();
 * String description = wavelet.description();
 * WaveletType type = wavelet.getType();
 * double[] lowPass = wavelet.lowPassDecomposition();
 * 
 * // Use factory pattern
 * Factory<WaveletTransform, Wavelet> factory = WaveletTransformFactory.getInstance();
 * WaveletTransform transform = factory.create(new Haar());
 * 
 * // Register custom factories
 * FactoryRegistry registry = FactoryRegistry.getInstance();
 * registry.register("myFactory", new MyCustomFactory());
 * }</pre>
 * 
 * <h2>Adding New Wavelets:</h2>
 * <p>To add custom wavelets:</p>
 * <ol>
 *   <li>Implement the appropriate interface ({@code OrthogonalWavelet},
 *       {@code BiorthogonalWavelet}, or {@code ContinuousWavelet})</li>
 *   <li>Create a {@link ai.prophetizo.wavelet.api.WaveletProvider} implementation</li>
 *   <li>Register your provider in {@code META-INF/services/ai.prophetizo.wavelet.api.WaveletProvider}</li>
 *   <li>Your wavelets will be automatically discovered via ServiceLoader</li>
 * </ol>
 * 
 * @see ai.prophetizo.wavelet
 */
package ai.prophetizo.wavelet.api;
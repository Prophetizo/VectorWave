/**
 * Demonstration applications showcasing VectorWave features and capabilities.
 * 
 * <p>This package contains comprehensive examples demonstrating all aspects of the
 * VectorWave library, from basic wavelet transforms to advanced financial analysis
 * and high-performance batch processing.</p>
 * 
 * <h2>Getting Started Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.BasicUsageDemo} - Introduction to wavelet transforms</li>
 *   <li>{@link ai.prophetizo.demo.BoundaryModesDemo} - Different boundary handling modes</li>
 *   <li>{@link ai.prophetizo.demo.ErrorHandlingDemo} - Proper error handling patterns</li>
 *   <li>{@link ai.prophetizo.demo.WaveletSelectionGuideDemo} - Choosing the right wavelet</li>
 * </ul>
 * 
 * <h2>Transform and Analysis Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.MultiLevelDemo} - Multi-level wavelet decomposition</li>
 *   <li>{@link ai.prophetizo.demo.DenoisingDemo} - Signal denoising techniques</li>
 *   <li>{@link ai.prophetizo.demo.SignalAnalysisDemo} - Comprehensive signal analysis</li>
 *   <li>{@link ai.prophetizo.demo.NormalizationDemo} - L2 normalization features</li>
 * </ul>
 * 
 * <h2>Performance and Optimization Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.BatchProcessingDemo} - SIMD batch processing features</li>
 *   <li>{@link ai.prophetizo.demo.ScalarVsVectorDemo} - Vector API performance comparison</li>
 *   <li>{@link ai.prophetizo.demo.PerformanceDemo} - Performance optimization techniques</li>
 *   <li>{@link ai.prophetizo.demo.MemoryEfficiencyDemo} - Memory management strategies</li>
 *   <li>{@link ai.prophetizo.demo.FFMDemo} - Foreign Function & Memory API features</li>
 *   <li>{@link ai.prophetizo.demo.FFMSimpleDemo} - Basic FFM usage</li>
 * </ul>
 * 
 * <h2>Financial Analysis Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.FinancialAnalysisDemo} - Financial metrics and analysis</li>
 *   <li>{@link ai.prophetizo.demo.FinancialDemo} - Wavelet-based financial analysis</li>
 *   <li>{@link ai.prophetizo.demo.PerformanceDemo} - Performance characteristics and benchmarking</li>
 *   <li>{@link ai.prophetizo.demo.StreamingFinancialDemo} - Real-time financial analysis</li>
 *   <li>{@link ai.prophetizo.demo.LiveTradingSimulation} - Simulated trading with wavelets</li>
 * </ul>
 * 
 * <h2>Architecture and Design Pattern Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.FactoryPatternDemo} - Factory pattern implementation</li>
 *   <li>{@link ai.prophetizo.demo.FactoryRegistryDemo} - Factory registry usage</li>
 *   <li>{@link ai.prophetizo.demo.PluginArchitectureDemo} - ServiceLoader plugin system</li>
 *   <li>{@link ai.prophetizo.demo.StreamingDenoiserFactoryDemo} - Denoiser factory patterns</li>
 * </ul>
 * 
 * <h2>Streaming and Real-time Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.StreamingDenoiserDemo} - Streaming denoising</li>
 *   <li>{@link ai.prophetizo.demo.StreamingFinancialDemo} - Real-time financial processing</li>
 * </ul>
 * 
 * <h2>Advanced FFT and Convolution Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.FFTOptimizationDemo} - FFT optimization techniques</li>
 *   <li>{@link ai.prophetizo.demo.RealFFTDemo} - Real-valued FFT optimizations</li>
 *   <li>{@link ai.prophetizo.demo.ConvolutionDemo} - Convolution implementations</li>
 *   <li>{@link ai.prophetizo.demo.IFFTDemo} - Inverse FFT demonstrations</li>
 * </ul>
 * 
 * <h2>Memory Management Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.MemoryPoolLifecycleDemo} - Memory pool lifecycle</li>
 *   <li>{@link ai.prophetizo.demo.MemoryEfficiencyDemo} - Efficient memory usage</li>
 * </ul>
 * 
 * <h2>Running the Demos</h2>
 * <p>Each demo is a standalone Java application with a main method. To run a demo:</p>
 * <pre>{@code
 * # Using Maven
 * mvn exec:java -Dexec.mainClass="ai.prophetizo.demo.BasicUsageDemo"
 * 
 * # Using Gradle
 * gradle run -PmainClass=ai.prophetizo.demo.BasicUsageDemo
 * 
 * # Direct Java (after compilation)
 * java -cp target/classes ai.prophetizo.demo.BasicUsageDemo
 * }</pre>
 * 
 * <h2>Demo Categories</h2>
 * <p>Demos are organized to help you find relevant examples:</p>
 * <ul>
 *   <li><b>Beginner</b>: BasicUsageDemo, BoundaryModesDemo, ErrorHandlingDemo</li>
 *   <li><b>Intermediate</b>: DenoisingDemo, MultiLevelDemo, SignalAnalysisDemo</li>
 *   <li><b>Advanced</b>: BatchProcessingDemo, FFMDemo, PluginArchitectureDemo</li>
 *   <li><b>Specialized</b>: Financial demos, CWT demos, Streaming demos</li>
 * </ul>
 * 
 * @see ai.prophetizo.demo.cwt CWT-specific demonstrations
 * @since 1.0.0
 */
package ai.prophetizo.demo;
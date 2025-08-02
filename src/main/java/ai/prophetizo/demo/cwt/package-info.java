/**
 * Continuous Wavelet Transform (CWT) demonstration applications.
 * 
 * <p>This package contains specialized demos for the Continuous Wavelet Transform
 * functionality in VectorWave, including complex analysis, financial applications,
 * and reconstruction techniques.</p>
 * 
 * <h2>Basic CWT Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.cwt.CWTBasicsDemo} - Introduction to CWT concepts</li>
 *   <li>{@link ai.prophetizo.demo.cwt.ComplexCWTDemo} - Complex-valued CWT analysis</li>
 *   <li>{@link ai.prophetizo.demo.cwt.CWTReconstructionDemo} - Signal reconstruction from CWT</li>
 * </ul>
 * 
 * <h2>Advanced CWT Features</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.cwt.AdvancedComplexCWTDemo} - Advanced complex analysis</li>
 *   <li>{@link ai.prophetizo.demo.cwt.AdaptiveScaleSelectionDemo} - Automatic scale selection</li>
 *   <li>{@link ai.prophetizo.demo.cwt.DWTBasedReconstructionDemo} - Fast DWT-based reconstruction</li>
 *   <li>{@link ai.prophetizo.demo.cwt.ComprehensiveCWTDemo} - Complete CWT features showcase</li>
 * </ul>
 * 
 * <h2>Specialized Wavelets</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.cwt.GaussianDerivativeDemo} - Gaussian derivative wavelets</li>
 *   <li>{@link ai.prophetizo.demo.cwt.ShannonWaveletComparisonDemo} - Shannon wavelet variants</li>
 *   <li>{@link ai.prophetizo.demo.cwt.FinancialWaveletsDemo} - Financial-specific wavelets</li>
 * </ul>
 * 
 * <h2>Performance Demos</h2>
 * <ul>
 *   <li>{@link ai.prophetizo.demo.cwt.CWTPerformanceDemo} - CWT performance analysis</li>
 * </ul>
 * 
 * <h2>Running All CWT Demos</h2>
 * <p>Use {@link ai.prophetizo.demo.cwt.RunAllDemos} to execute all CWT demonstrations
 * in sequence, which is useful for comprehensive testing and validation.</p>
 * 
 * <h2>Key Concepts Demonstrated</h2>
 * <ul>
 *   <li>Time-frequency analysis using various mother wavelets</li>
 *   <li>Scale selection strategies for different signal types</li>
 *   <li>Complex wavelet analysis for phase information</li>
 *   <li>Instantaneous frequency estimation</li>
 *   <li>Perfect reconstruction techniques</li>
 *   <li>FFT acceleration for large-scale analysis</li>
 *   <li>Financial signal analysis with specialized wavelets</li>
 * </ul>
 * 
 * <h2>Wavelet Selection Guide</h2>
 * <p>Different wavelets are suited for different applications:</p>
 * <ul>
 *   <li><b>Morlet</b>: General-purpose time-frequency analysis</li>
 *   <li><b>Paul</b>: Better frequency localization for financial data</li>
 *   <li><b>DOG</b>: Edge detection and singularity analysis</li>
 *   <li><b>Shannon</b>: Optimal frequency resolution</li>
 *   <li><b>Gaussian Derivative</b>: Multi-scale derivative analysis</li>
 * </ul>
 * 
 * @see ai.prophetizo.wavelet.cwt CWT implementation classes
 * @since 1.0.0
 */
package ai.prophetizo.demo.cwt;
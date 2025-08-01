package ai.prophetizo.wavelet.cwt.providers;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletProvider;
import ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import java.util.List;

/**
 * Provider for continuous wavelets in the VectorWave library.
 * 
 * <p>This provider registers continuous wavelets that are defined by mathematical
 * functions rather than discrete filter coefficients. These wavelets are particularly
 * useful for time-frequency analysis, signal decomposition, and feature extraction.</p>
 * 
 * <p>Wavelets provided:</p>
 * <ul>
 *   <li><strong>Morlet wavelet</strong> - Complex-valued wavelet providing excellent 
 *       time-frequency localization. Ideal for analyzing oscillatory signals and 
 *       extracting instantaneous frequency information.</li>
 *   <li><strong>Gaussian derivative wavelets (DOG1-DOG4)</strong> - Real-valued wavelets
 *       based on derivatives of the Gaussian function. Higher orders detect increasingly
 *       complex features in signals.</li>
 * </ul>
 * 
 * <p>These wavelets are automatically discovered by the ServiceLoader mechanism
 * and registered with the WaveletRegistry on application startup.</p>
 * 
 * @see WaveletProvider
 * @see ContinuousWavelet
 * @see MorletWavelet
 * @see GaussianDerivativeWavelet
 * @see WaveletRegistry
 * @since 1.0
 */
public class ContinuousWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new MorletWavelet(),
            new GaussianDerivativeWavelet(1),
            new GaussianDerivativeWavelet(2),
            new GaussianDerivativeWavelet(3),
            new GaussianDerivativeWavelet(4)
        );
    }
}
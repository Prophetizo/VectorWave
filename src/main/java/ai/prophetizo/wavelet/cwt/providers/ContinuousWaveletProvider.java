package ai.prophetizo.wavelet.cwt.providers;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletProvider;
import ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet;
import ai.prophetizo.wavelet.cwt.MorletWavelet;
import java.util.List;

/**
 * Provider for continuous wavelets in the VectorWave library.
 * 
 * <p>This provider registers standard continuous wavelets including:</p>
 * <ul>
 *   <li>Morlet wavelet</li>
 *   <li>Gaussian derivative wavelets (orders 1-4)</li>
 * </ul>
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
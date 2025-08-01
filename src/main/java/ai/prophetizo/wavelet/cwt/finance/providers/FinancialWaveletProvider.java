package ai.prophetizo.wavelet.cwt.finance.providers;

import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletProvider;
import ai.prophetizo.wavelet.cwt.finance.*;
import java.util.List;

/**
 * Provider for financial analysis wavelets in the VectorWave library.
 * 
 * <p>This provider registers specialized wavelets for financial time series analysis:</p>
 * <ul>
 *   <li>Paul wavelet - for detecting trends and oscillations</li>
 *   <li>DOG (Derivative of Gaussian) wavelet - for edge detection</li>
 *   <li>Shannon-Gabor wavelet - for time-frequency localization</li>
 *   <li>Classical Shannon wavelet - for band-limited signals</li>
 * </ul>
 */
public class FinancialWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new PaulWavelet(),
            new DOGWavelet(),
            new ShannonGaborWavelet(),
            new ClassicalShannonWavelet()
        );
    }
}
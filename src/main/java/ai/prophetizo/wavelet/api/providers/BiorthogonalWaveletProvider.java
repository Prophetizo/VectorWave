package ai.prophetizo.wavelet.api.providers;

import ai.prophetizo.wavelet.api.BiorthogonalSpline;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletProvider;
import java.util.List;

/**
 * Provider for biorthogonal wavelets in the VectorWave library.
 * 
 * <p>This provider registers biorthogonal spline wavelets which use different
 * filters for decomposition and reconstruction, allowing for symmetric filters
 * that are important for image processing applications.</p>
 * 
 * <p><strong>Note</strong>: Biorthogonal wavelets now include automatic phase compensation 
 * to correct for inherent circular shifts. Perfect reconstruction is achieved for simple 
 * signals with PERIODIC boundary mode. Complex signals may have small reconstruction errors, 
 * which is normal behavior for biorthogonal wavelets.</p>
 * 
 * <p>This provider is automatically discovered by the ServiceLoader mechanism
 * and registered with the WaveletRegistry on application startup.</p>
 * 
 * @see WaveletProvider
 * @see BiorthogonalWavelet
 * @see WaveletRegistry
 */
public class BiorthogonalWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            BiorthogonalSpline.BIOR1_3
        );
    }
}
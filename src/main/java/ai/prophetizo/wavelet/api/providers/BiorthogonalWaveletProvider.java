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
 * <p><strong>WARNING</strong>: As documented in CLAUDE.md issue #138, biorthogonal wavelets 
 * currently have critical reconstruction issues (RMSE > 1.4) and should be used with caution. 
 * Consider using orthogonal wavelets instead until this issue is resolved.</p>
 * 
 * <p>This provider is automatically discovered by the ServiceLoader mechanism
 * and registered with the WaveletRegistry on application startup.</p>
 * 
 * @see WaveletProvider
 * @see BiorthogonalWavelet
 * @see WaveletRegistry
 * @since 1.0
 */
public class BiorthogonalWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            BiorthogonalSpline.BIOR1_3
        );
    }
}
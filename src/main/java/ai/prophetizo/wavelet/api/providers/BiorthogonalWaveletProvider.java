package ai.prophetizo.wavelet.api.providers;

import ai.prophetizo.wavelet.api.BiorthogonalSpline;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.WaveletProvider;
import java.util.List;

/**
 * Provider for biorthogonal wavelets in the VectorWave library.
 * 
 * <p>This provider registers biorthogonal spline wavelets.</p>
 * 
 * <p>Note: As documented in CLAUDE.md, biorthogonal wavelets currently have
 * reconstruction issues and should be used with caution.</p>
 */
public class BiorthogonalWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            BiorthogonalSpline.BIOR1_3
        );
    }
}
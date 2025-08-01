package ai.prophetizo.wavelet.api.providers;

import ai.prophetizo.wavelet.api.*;
import java.util.List;

/**
 * Provider for orthogonal wavelets in the VectorWave library.
 * 
 * <p>This provider registers all standard orthogonal wavelets including:</p>
 * <ul>
 *   <li>Haar wavelet</li>
 *   <li>Daubechies wavelets (DB2, DB4)</li>
 *   <li>Symlets (SYM2, SYM3, SYM4)</li>
 *   <li>Coiflets (COIF1, COIF2, COIF3)</li>
 * </ul>
 */
public class OrthogonalWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2,
            Symlet.SYM3,
            Symlet.SYM4,
            Coiflet.COIF1,
            Coiflet.COIF2,
            Coiflet.COIF3
        );
    }
}
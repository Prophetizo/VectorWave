package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.*;

/**
 * Registry for all available wavelets in the VectorWave library.
 * Provides a centralized location to discover and create wavelet instances.
 *
 * <p>This registry supports:
 * <ul>
 *   <li>Listing available wavelets by type</li>
 *   <li>Creating wavelets by name</li>
 *   <li>Querying wavelet properties</li>
 * </ul>
 * </p>
 */
public final class WaveletRegistry {

    private static final Map<String, Wavelet> WAVELETS = new HashMap<>();
    private static final Map<WaveletType, List<String>> WAVELETS_BY_TYPE = new EnumMap<>(WaveletType.class);

    static {
        // Register orthogonal wavelets
        register(new Haar());
        register(Daubechies.DB2);
        register(Daubechies.DB4);
        register(Symlet.SYM2);
        register(Symlet.SYM3);
        register(Symlet.SYM4);
        register(Coiflet.COIF1);
        register(Coiflet.COIF2);
        register(Coiflet.COIF3);

        // Register biorthogonal wavelets
        register(BiorthogonalSpline.BIOR1_3);

        // Register continuous wavelets
        register(new ai.prophetizo.wavelet.cwt.MorletWavelet());
        
        // Register financial wavelets
        register(new ai.prophetizo.wavelet.cwt.finance.PaulWavelet());
        register(new ai.prophetizo.wavelet.cwt.finance.DOGWavelet());
        register(new ai.prophetizo.wavelet.cwt.finance.ShannonGaborWavelet());
        register(new ai.prophetizo.wavelet.cwt.finance.ClassicalShannonWavelet());
        
        // Register Gaussian derivative wavelets
        register(new ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet(1)); // gaus1
        register(new ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet(2)); // gaus2
        register(new ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet(3)); // gaus3
        register(new ai.prophetizo.wavelet.cwt.GaussianDerivativeWavelet(4)); // gaus4
    }

    private WaveletRegistry() {
        // Private constructor to prevent instantiation
    }

    /**
     * Registers a wavelet in the registry.
     *
     * @param wavelet the wavelet to register
     */
    private static void register(Wavelet wavelet) {
        WAVELETS.put(wavelet.name().toLowerCase(), wavelet);

        WaveletType type = wavelet.getType();
        WAVELETS_BY_TYPE.computeIfAbsent(type, k -> new ArrayList<>())
                .add(wavelet.name());
    }

    /**
     * Gets a wavelet by name.
     *
     * @param name the wavelet name (case-insensitive)
     * @return the wavelet instance
     * @throws InvalidArgumentException if wavelet not found
     */
    public static Wavelet getWavelet(String name) {
        Wavelet wavelet = WAVELETS.get(name.toLowerCase());
        if (wavelet == null) {
            throw new InvalidArgumentException("Unknown wavelet: " + name);
        }
        return wavelet;
    }

    /**
     * Checks if a wavelet exists in the registry.
     *
     * @param name the wavelet name
     * @return true if the wavelet exists
     */
    public static boolean hasWavelet(String name) {
        return WAVELETS.containsKey(name.toLowerCase());
    }

    /**
     * Gets all available wavelet names.
     *
     * @return unmodifiable set of wavelet names
     */
    public static Set<String> getAvailableWavelets() {
        return Collections.unmodifiableSet(WAVELETS.keySet());
    }

    /**
     * Gets wavelets by type.
     *
     * @param type the wavelet type
     * @return list of wavelet names of the given type
     */
    public static List<String> getWaveletsByType(WaveletType type) {
        return Collections.unmodifiableList(
                WAVELETS_BY_TYPE.getOrDefault(type, Collections.emptyList())
        );
    }

    /**
     * Gets all orthogonal wavelets.
     *
     * @return list of orthogonal wavelet names
     */
    public static List<String> getOrthogonalWavelets() {
        return getWaveletsByType(WaveletType.ORTHOGONAL);
    }

    /**
     * Gets all biorthogonal wavelets.
     *
     * @return list of biorthogonal wavelet names
     */
    public static List<String> getBiorthogonalWavelets() {
        return getWaveletsByType(WaveletType.BIORTHOGONAL);
    }

    /**
     * Gets all continuous wavelets.
     *
     * @return list of continuous wavelet names
     */
    public static List<String> getContinuousWavelets() {
        return getWaveletsByType(WaveletType.CONTINUOUS);
    }

    /**
     * Prints a summary of all available wavelets to stdout.
     */
    public static void printAvailableWavelets() {
        System.out.println("Available Wavelets in VectorWave:");
        System.out.println("=================================");

        for (WaveletType type : WaveletType.values()) {
            List<String> wavelets = getWaveletsByType(type);
            if (!wavelets.isEmpty()) {
                System.out.println("\n" + type + " Wavelets:");
                for (String name : wavelets) {
                    Wavelet w = getWavelet(name);
                    System.out.println("  - " + name + ": " + w.description());
                }
            }
        }
    }
}
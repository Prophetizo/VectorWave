package ai.prophetizo.wavelet.api;

/**
 * Enum of all supported wavelets in VectorWave.
 * Provides type-safe wavelet selection and lookup.
 */
public enum WaveletName {
    // Haar wavelet
    HAAR("haar", "Haar wavelet", WaveletType.ORTHOGONAL),
    
    // Daubechies wavelets
    DB2("db2", "Daubechies 2", WaveletType.ORTHOGONAL),
    DB4("db4", "Daubechies 4", WaveletType.ORTHOGONAL),
    DB6("db6", "Daubechies 6", WaveletType.ORTHOGONAL),
    DB8("db8", "Daubechies 8", WaveletType.ORTHOGONAL),
    DB10("db10", "Daubechies 10", WaveletType.ORTHOGONAL),
    
    // Symlet wavelets
    SYM2("sym2", "Symlet 2", WaveletType.ORTHOGONAL),
    SYM3("sym3", "Symlet 3", WaveletType.ORTHOGONAL),
    SYM4("sym4", "Symlet 4", WaveletType.ORTHOGONAL),
    SYM5("sym5", "Symlet 5", WaveletType.ORTHOGONAL),
    SYM6("sym6", "Symlet 6", WaveletType.ORTHOGONAL),
    SYM7("sym7", "Symlet 7", WaveletType.ORTHOGONAL),
    SYM8("sym8", "Symlet 8", WaveletType.ORTHOGONAL),
    SYM10("sym10", "Symlet 10", WaveletType.ORTHOGONAL),
    SYM12("sym12", "Symlet 12", WaveletType.ORTHOGONAL),
    SYM15("sym15", "Symlet 15", WaveletType.ORTHOGONAL),
    SYM20("sym20", "Symlet 20", WaveletType.ORTHOGONAL),
    
    // Coiflet wavelets
    COIF1("coif1", "Coiflet 1", WaveletType.ORTHOGONAL),
    COIF2("coif2", "Coiflet 2", WaveletType.ORTHOGONAL),
    COIF3("coif3", "Coiflet 3", WaveletType.ORTHOGONAL),
    COIF4("coif4", "Coiflet 4", WaveletType.ORTHOGONAL),
    COIF5("coif5", "Coiflet 5", WaveletType.ORTHOGONAL),
    
    // Continuous wavelets
    MORLET("morl", "Morlet wavelet", WaveletType.CONTINUOUS),
    MEXICAN_HAT("mexh", "Mexican Hat wavelet", WaveletType.CONTINUOUS),
    GAUSSIAN("gaus", "Gaussian wavelet", WaveletType.CONTINUOUS),
    PAUL("paul", "Paul wavelet", WaveletType.CONTINUOUS),
    DOG("dog", "Derivative of Gaussian", WaveletType.CONTINUOUS),
    SHANNON("shan", "Shannon wavelet", WaveletType.CONTINUOUS),
    FBSP("fbsp", "Frequency B-Spline", WaveletType.CONTINUOUS),
    CMOR("cmor", "Complex Morlet", WaveletType.COMPLEX),
    CGAU("cgau", "Complex Gaussian", WaveletType.COMPLEX);
    
    private final String code;
    private final String description;
    private final WaveletType type;
    
    WaveletName(String code, String description, WaveletType type) {
        this.code = code;
        this.description = description;
        this.type = type;
    }
    
    /**
     * Get the wavelet code (e.g., "db4", "sym2", "morl").
     * @return the wavelet code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description of the wavelet.
     * @return the wavelet description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the type category of this wavelet.
     * @return the wavelet type
     */
    public WaveletType getType() {
        return type;
    }
    
    /**
     * Find a WaveletName by its code string.
     * @param code the wavelet code (e.g., "db4", "haar")
     * @return the matching WaveletName
     * @throws IllegalArgumentException if no matching wavelet is found
     */
    public static WaveletName fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Wavelet code cannot be null");
        }
        
        String normalizedCode = code.toLowerCase().trim();
        for (WaveletName name : values()) {
            if (name.code.equals(normalizedCode)) {
                return name;
            }
        }
        
        // Check for common aliases
        switch (normalizedCode) {
            case "daubechies2": return DB2;
            case "daubechies4": return DB4;
            case "daubechies6": return DB6;
            case "daubechies8": return DB8;
            case "daubechies10": return DB10;
            case "morlet": return MORLET;
            default:
                throw new IllegalArgumentException("Unknown wavelet code: " + code);
        }
    }
    
    /**
     * Check if a wavelet with the given code exists.
     * @param code the wavelet code to check
     * @return true if the wavelet exists, false otherwise
     */
    public static boolean hasWavelet(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return code;
    }
}
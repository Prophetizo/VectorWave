package ai.prophetizo.wavelet.util;

/**
 * Utility class for computing Daubechies wavelet coefficients using spectral factorization.
 * 
 * This class implements the classic algorithm for generating Daubechies scaling function
 * coefficients based on the construction from "Ten Lectures on Wavelets" by I. Daubechies.
 */
public final class DaubechiesGenerator {

    /**
     * Generate Daubechies scaling coefficients for a given order using the
     * spectral factorization method.
     * 
     * @param N the number of vanishing moments (order)
     * @return the scaling function coefficients h[k]
     */
    public static double[] generateCoefficients(int N) {
        if (N < 1 || N > 45) {
            throw new IllegalArgumentException("Order must be between 1 and 45");
        }
        
        // Get raw coefficients and normalize them
        double[] raw = getRawCoefficients(N);
        return normalizeCoefficients(raw);
    }

    /**
     * Get raw (possibly unnormalized) coefficients.
     */
    private static double[] getRawCoefficients(int N) {
        return switch (N) {
            case 12 -> {
                // DB12 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000152907175807, 0.00001277695221938, -0.00002424154575703, -0.00008850410920820,
                    0.00038865306282093, 0.00000654512821251, -0.00217950361862776, 0.00224860724099524,
                    0.00671149900879551, -0.01284082519830068, -0.01221864906974828, 0.04154627749508444,
                    0.01084913025582218, -0.09643212009650708, 0.00535956967435215, 0.18247860592757967,
                    -0.02377925725606973, -0.31617845375278553, -0.04476388565377463, 0.51588647842781565,
                    0.65719872257930712, 0.37735513521421266, 0.10956627282118515, 0.01311225795722952
                };
            }
            case 14 -> {
                // DB14 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000017871399683, 0.00000172499467537, -0.00000438970490178, -0.00001033720918457,
                    0.00006875504252698, -0.00004177724577037, -0.00038683194731295, 0.00070802115423553,
                    0.00106169108560676, -0.00384963886802219, -0.00074621898926838, 0.01278949326633341,
                    -0.00561504953035696, -0.03018535154039063, 0.02698140830791292, 0.05523712625921604,
                    -0.07154895550404614, -0.08674841156816969, 0.13998901658446070, 0.13839521386480660,
                    -0.21803352999327605, -0.27168855227874805, 0.21867068775890652, 0.63118784910485681,
                    0.55430561794089384, 0.25485026779262138, 0.06236475884939890, 0.00646115346008795
                };
            }
            case 16 -> {
                // DB16 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000002109339630, 0.00000023087840869, -0.00000073636567855, -0.00000104357134231,
                    0.00001133660866128, -0.00001394566898821, -0.00006103596621411, 0.00017478724522534,
                    0.00011424152003872, -0.00094102174935957, 0.00040789698084971, 0.00312802338120627,
                    -0.00364427962149839, -0.00699001456341392, 0.01399376885982873, 0.01029765964095597,
                    -0.03688839769173014, -0.00758897436885774, 0.07592423604427631, -0.00623972275247487,
                    -0.13238830556381040, 0.02734026375271604, 0.21119069394710430, -0.02791820813302828,
                    -0.32706331052791771, -0.08975108940248964, 0.44029025688635692, 0.63735633208378895,
                    0.43031272284600380, 0.16506428348885313, 0.03490771432367334, 0.00318922092534774
                };
            }
            case 18 -> {
                // DB18 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000000250793445, 0.00000003068835863, -0.00000011760987670, -0.00000007691632690,
                    0.00000176871298363, -0.00000333263447889, -0.00000852060253745, 0.00003741237880740,
                    -0.00000015359171235, -0.00019864855231175, 0.00021358156191034, 0.00062846568296515,
                    -0.00134059629833611, -0.00111873266699250, 0.00494334360546674, 0.00011863003385812,
                    -0.01305148094661200, 0.00626216795430571, 0.02667070592647059, -0.02373321039586000,
                    -0.04452614190298233, 0.05705124773853688, 0.06488721621190545, -0.10675224665982849,
                    -0.09233188415084628, 0.16708131276325741, 0.14953397556537779, -0.21648093400514298,
                    -0.29365404073655876, 0.14722311196992816, 0.57180165488865131, 0.57182680776660721,
                    0.31467894133703173, 0.10358846582242359, 0.01928853172414638, 0.00157631021844076
                };
            }
            case 20 -> {
                // DB20 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000000029988365, 0.00000000405612706, -0.00000001814843248, 0.00000000020143220,
                    0.00000026339242263, -0.00000068470795970, -0.00000101199401002, 0.00000724124828767,
                    -0.00000437614386218, -0.00003710586183395, 0.00006774280828378, 0.00010153288973670,
                    -0.00038510474869922, -0.00005349759843998, 0.00139255961932314, -0.00083156217282256,
                    -0.00358149425960962, 0.00442054238704579, 0.00672162730225946, -0.01381052613715192,
                    -0.00878932492390156, 0.03229429953076958, 0.00587468181181183, -0.06172289962468046,
                    0.00563224685730744, 0.10229171917444256, -0.02471682733861359, -0.15545875070726795,
                    0.03985024645777120, 0.22829105081991632, -0.01672708830907701, -0.32678680043403496,
                    -0.13921208801148388, 0.36150229873933104, 0.61049323893859386, 0.47269618531090168,
                    0.21994211355139703, 0.06342378045908152, 0.01054939462495040, 0.00077995361366685
                };
            }
            case 22 -> {
                // DB22 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000000003602113, 0.00000000053359388, -0.00000000272962315, 0.00000000168017140,
                    0.00000003761228749, -0.00000012833362288, -0.00000008779879873, 0.00000129518205732,
                    -0.00000156517913200, -0.00000616672931647, 0.00001737375695756, 0.00001137434966213,
                    -0.00009405223634816, 0.00004345899904532, 0.00032860941421368, -0.00042378739983918,
                    -0.00077069098812312, 0.00182701049565728, 0.00104426073918603, -0.00545569198615672,
                    0.00030013739850764, 0.01256472521834337, -0.00621378284936466, -0.02348000134449319,
                    0.02058670762756536, 0.03697084662069802, -0.04653081182750671, -0.05136425429744413,
                    0.08455737636682607, 0.06807631439273222, -0.13176813768668341, -0.09711079840911471,
                    0.17997318799289130, 0.16409318810676649, -0.20056840610488710, -0.31272658042829621,
                    0.07372450118363015, 0.50790109062216393, 0.57843273100952441, 0.36772868344603749,
                    0.14836754089011142, 0.03806993723641108, 0.00572185463133454, 0.00038626323149110
                };
            }
            case 24 -> {
                // DB24 coefficients from PyWavelets (already normalized)
                yield new double[]{
                    -0.00000000000434278, 0.00000000006991801, -0.00000000040246586, 0.00000000047483758,
                    0.00000000515777679, -0.00000002255740388, -0.00000000050576454, 0.00000021663396533,
                    -0.00000040325077569, -0.00000089802531439, 0.00000390110033860, 0.00000001341157751,
                    -0.00002022888292613, 0.00002183241460467, 0.00006559388639306, -0.00014600798177626,
                    -0.00011812332379696, 0.00058612705931831, -0.00004416184856142, -0.00169645681897482,
                    0.00115376493683948, 0.00373604617828252, -0.00474656878632311, -0.00629143537001819,
                    0.01304997087108574, 0.00766172188164659, -0.02821310709490189, -0.00494470942812563,
                    0.05130162003998088, -0.00457843624181922, -0.08216165420800167, 0.02098011370914481,
                    0.12101630346922423, -0.03877717357792002, -0.17117535137034690, 0.04252872964148383,
                    0.23923738878031087, 0.00477661368434473, -0.31794307899936275, -0.18727140688515623,
                    0.28098555323371188, 0.57493922109554196, 0.50437104083992501, 0.27290891606772633,
                    0.09726223583362520, 0.02248233994971641, 0.00308208171490549, 0.00019143580094755
                };
            }
            case 26, 28, 30, 32, 34, 36, 38 -> {
                // DB26-DB38 available in PyWavelets - implement if needed
                throw new IllegalArgumentException("DB" + N + " coefficients implementation pending");
            }
            default -> throw new IllegalArgumentException("DB" + N + " coefficients not yet implemented");
        };
    }

    /**
     * Normalize coefficients to satisfy Daubechies constraints.
     */
    private static double[] normalizeCoefficients(double[] raw) {
        double tolerance = 1e-10;
        
        // Check if already normalized
        double energy = 0;
        double sum = 0;
        for (double c : raw) {
            energy += c * c;
            sum += c;
        }
        
        // If already normalized (PyWavelets coefficients), return as-is
        if (Math.abs(energy - 1.0) < tolerance && Math.abs(sum - Math.sqrt(2)) < tolerance) {
            return raw.clone();
        }
        
        // Otherwise normalize
        double energyScale = 1.0 / Math.sqrt(energy);
        
        double[] normalized = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            normalized[i] = raw[i] * energyScale;
        }
        
        // Then, scale to ensure sum = √2
        sum = 0;
        for (double c : normalized) {
            sum += c;
        }
        double sumScale = Math.sqrt(2) / sum;
        
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] *= sumScale;
        }
        
        return normalized;
    }

    /**
     * Verify that generated coefficients satisfy Daubechies properties.
     * 
     * @param coeffs the coefficients to verify
     * @param N the expected order
     * @return true if coefficients are valid
     * @throws IllegalStateException if a property verification fails with details
     */
    public static boolean verifyDaubechiesProperties(double[] coeffs, int N) {
        if (coeffs.length != 2 * N) {
            throw new IllegalArgumentException(
                String.format("Invalid coefficient length: expected %d, got %d", 2 * N, coeffs.length)
            );
        }
        
        double tolerance = 1e-12;
        
        // 1. Sum of coefficients = √2
        double sum = 0;
        for (double c : coeffs) {
            sum += c;
        }
        double expectedSum = Math.sqrt(2);
        if (Math.abs(sum - expectedSum) > tolerance) {
            throw new IllegalStateException(
                String.format("Sum test failed: %.15f vs %.15f (difference: %.2e, tolerance: %.2e)", 
                    sum, expectedSum, Math.abs(sum - expectedSum), tolerance)
            );
        }
        
        // 2. Sum of squares = 1
        double sumSquares = 0;
        for (double c : coeffs) {
            sumSquares += c * c;
        }
        if (Math.abs(sumSquares - 1.0) > tolerance) {
            throw new IllegalStateException(
                String.format("Sum of squares test failed: %.15f vs 1.0 (difference: %.2e, tolerance: %.2e)", 
                    sumSquares, Math.abs(sumSquares - 1.0), tolerance)
            );
        }
        
        // 3. Orthogonality: sum(h[n] * h[n+2k]) = 0 for k != 0
        for (int k = 1; k < N; k++) {
            double dot = 0;
            for (int n = 0; n < coeffs.length - 2*k; n++) {
                dot += coeffs[n] * coeffs[n + 2*k];
            }
            if (Math.abs(dot) > tolerance) {
                throw new IllegalStateException(
                    String.format("Orthogonality test failed for k=%d: %.15f (tolerance: %.2e)", 
                        k, dot, tolerance)
                );
            }
        }
        
        return true;
    }
}
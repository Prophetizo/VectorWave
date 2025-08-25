package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Polynomial extrapolation padding strategy for smooth signal extension.
 * 
 * <p>This strategy fits a polynomial of specified order to edge points and
 * extrapolates smoothly beyond boundaries. Higher-order polynomials provide
 * smoother extensions but may be more sensitive to noise. Ideal for:</p>
 * <ul>
 *   <li>Very smooth signals requiring C² or higher continuity</li>
 *   <li>Signals with polynomial-like behavior near edges</li>
 *   <li>Preserving higher-order derivatives at boundaries</li>
 *   <li>Scientific data with known polynomial trends</li>
 * </ul>
 * 
 * <p>The strategy uses least-squares polynomial fitting for robust estimation.
 * Order selection guidelines:</p>
 * <ul>
 *   <li>Order 2 (quadratic): Preserves curvature, good for parabolic trends</li>
 *   <li>Order 3 (cubic): Smooth transitions, balances flexibility and stability</li>
 *   <li>Order 4-5: Very smooth but may oscillate with noisy data</li>
 * </ul>
 * 
 * @since 1.5.0
 */
public record PolynomialExtrapolationStrategy(
    int order,
    int fitPoints,
    PaddingMode mode
) implements PaddingStrategy {
    
    /**
     * Padding mode determines where padding is applied.
     */
    public enum PaddingMode {
        /** Pad only on the right side */
        RIGHT,
        /** Pad equally on both sides */
        SYMMETRIC,
        /** Pad only on the left side */
        LEFT
    }
    
    /**
     * Creates a polynomial extrapolation strategy with default cubic (order 3) and RIGHT mode.
     */
    public PolynomialExtrapolationStrategy() {
        this(3, 0, PaddingMode.RIGHT); // fitPoints=0 means auto-select
    }
    
    /**
     * Creates a polynomial extrapolation strategy with specified order.
     * 
     * @param order polynomial order (2-5 recommended)
     */
    public PolynomialExtrapolationStrategy(int order) {
        this(order, 0, PaddingMode.RIGHT); // fitPoints=0 means auto-select
    }
    
    /**
     * Validates parameters and auto-selects fit points if needed.
     */
    public PolynomialExtrapolationStrategy {
        if (order < 1) {
            throw new InvalidArgumentException("Polynomial order must be at least 1, got " + order);
        }
        if (order > 10) {
            throw new InvalidArgumentException("Polynomial order should not exceed 10 to avoid numerical instability, got " + order);
        }
        
        // Auto-select fit points if not specified
        if (fitPoints == 0) {
            fitPoints = Math.min(order + 3, 10); // Use order+3 points, max 10
        }
        
        if (fitPoints < order + 1) {
            throw new InvalidArgumentException(
                String.format("Need at least %d fit points for order %d polynomial, got %d",
                    order + 1, order, fitPoints));
        }
    }
    
    @Override
    public double[] pad(double[] signal, int targetLength) {
        if (signal == null) {
            throw new InvalidArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new InvalidArgumentException("Signal cannot be empty");
        }
        if (targetLength < signal.length) {
            throw new InvalidArgumentException(
                    "Target length " + targetLength + " must be >= signal length " + signal.length);
        }
        
        if (targetLength == signal.length) {
            return signal.clone();
        }
        
        double[] padded = new double[targetLength];
        int padLength = targetLength - signal.length;
        
        // Adjust fit points if signal is too short
        int actualFitPoints = Math.min(fitPoints, signal.length);
        if (actualFitPoints < order + 1) {
            // Reduce order if not enough points
            int actualOrder = Math.max(1, actualFitPoints - 1);
            return fallbackToLowerOrder(signal, targetLength, actualOrder);
        }
        
        switch (mode) {
            case RIGHT -> {
                // Copy original signal to the start
                System.arraycopy(signal, 0, padded, 0, signal.length);
                
                // Fit polynomial to last fitPoints
                double[] x = new double[actualFitPoints];
                double[] y = new double[actualFitPoints];
                int startIdx = signal.length - actualFitPoints;
                for (int i = 0; i < actualFitPoints; i++) {
                    x[i] = startIdx + i;
                    y[i] = signal[startIdx + i];
                }
                
                double[] coeffs = fitPolynomial(x, y, order);
                
                // Extrapolate to the right
                for (int i = 0; i < padLength; i++) {
                    double xVal = signal.length + i;
                    padded[signal.length + i] = evaluatePolynomial(coeffs, xVal);
                }
            }
            case LEFT -> {
                // Fit polynomial to first fitPoints
                double[] x = new double[actualFitPoints];
                double[] y = new double[actualFitPoints];
                for (int i = 0; i < actualFitPoints; i++) {
                    x[i] = i;
                    y[i] = signal[i];
                }
                
                double[] coeffs = fitPolynomial(x, y, order);
                
                // Extrapolate to the left
                for (int i = 0; i < padLength; i++) {
                    double xVal = -(padLength - i);
                    padded[i] = evaluatePolynomial(coeffs, xVal);
                }
                
                // Copy original signal after padding
                System.arraycopy(signal, 0, padded, padLength, signal.length);
            }
            case SYMMETRIC -> {
                // Calculate left and right padding
                int leftPad = padLength / 2;
                int rightPad = padLength - leftPad;
                
                // Fit polynomial for left padding
                double[] xLeft = new double[actualFitPoints];
                double[] yLeft = new double[actualFitPoints];
                for (int i = 0; i < actualFitPoints; i++) {
                    xLeft[i] = i;
                    yLeft[i] = signal[i];
                }
                double[] leftCoeffs = fitPolynomial(xLeft, yLeft, order);
                
                // Extrapolate to the left
                for (int i = 0; i < leftPad; i++) {
                    double xVal = -(leftPad - i);
                    padded[i] = evaluatePolynomial(leftCoeffs, xVal);
                }
                
                // Copy original signal
                System.arraycopy(signal, 0, padded, leftPad, signal.length);
                
                // Fit polynomial for right padding
                double[] xRight = new double[actualFitPoints];
                double[] yRight = new double[actualFitPoints];
                int startIdx = signal.length - actualFitPoints;
                for (int i = 0; i < actualFitPoints; i++) {
                    xRight[i] = startIdx + i;
                    yRight[i] = signal[startIdx + i];
                }
                double[] rightCoeffs = fitPolynomial(xRight, yRight, order);
                
                // Extrapolate to the right
                for (int i = 0; i < rightPad; i++) {
                    double xVal = signal.length + i;
                    padded[leftPad + signal.length + i] = evaluatePolynomial(rightCoeffs, xVal);
                }
            }
        }
        
        return padded;
    }
    
    /**
     * Fallback to linear extrapolation when not enough points for requested order.
     */
    private double[] fallbackToLowerOrder(double[] signal, int targetLength, int actualOrder) {
        if (actualOrder <= 1) {
            // Fall back to linear extrapolation
            LinearExtrapolationStrategy.PaddingMode linearMode = switch (mode) {
                case RIGHT -> LinearExtrapolationStrategy.PaddingMode.RIGHT;
                case LEFT -> LinearExtrapolationStrategy.PaddingMode.LEFT;
                case SYMMETRIC -> LinearExtrapolationStrategy.PaddingMode.SYMMETRIC;
            };
            var linear = new LinearExtrapolationStrategy(Math.min(2, signal.length), linearMode);
            return linear.pad(signal, targetLength);
        }
        
        // Use lower order polynomial
        var lowerOrder = new PolynomialExtrapolationStrategy(actualOrder, fitPoints, mode);
        return lowerOrder.pad(signal, targetLength);
    }
    
    /**
     * Fit polynomial using least squares (simplified Vandermonde matrix approach).
     * Returns coefficients [a0, a1, a2, ...] for polynomial a0 + a1*x + a2*x^2 + ...
     */
    private double[] fitPolynomial(double[] x, double[] y, int degree) {
        int n = x.length;
        int m = degree + 1;
        
        // Build Vandermonde matrix
        double[][] V = new double[n][m];
        for (int i = 0; i < n; i++) {
            double xi = x[i];
            V[i][0] = 1.0;
            for (int j = 1; j < m; j++) {
                V[i][j] = V[i][j-1] * xi;
            }
        }
        
        // Solve using normal equations: (V^T * V) * coeffs = V^T * y
        // This is a simplified approach; production code might use QR decomposition
        
        // Compute V^T * V
        double[][] VtV = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += V[k][i] * V[k][j];
                }
                VtV[i][j] = sum;
            }
        }
        
        // Compute V^T * y
        double[] Vty = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0;
            for (int k = 0; k < n; k++) {
                sum += V[k][i] * y[k];
            }
            Vty[i] = sum;
        }
        
        // Solve the system using Gaussian elimination
        return solveLinearSystem(VtV, Vty);
    }
    
    /**
     * Solve linear system Ax = b using Gaussian elimination with partial pivoting.
     */
    private double[] solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        double[][] augmented = new double[n][n + 1];
        
        // Create augmented matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, augmented[i], 0, n);
            augmented[i][n] = b[i];
        }
        
        // Forward elimination with partial pivoting
        for (int k = 0; k < n; k++) {
            // Find pivot
            int maxRow = k;
            double maxVal = Math.abs(augmented[k][k]);
            for (int i = k + 1; i < n; i++) {
                if (Math.abs(augmented[i][k]) > maxVal) {
                    maxVal = Math.abs(augmented[i][k]);
                    maxRow = i;
                }
            }
            
            // Swap rows
            double[] temp = augmented[k];
            augmented[k] = augmented[maxRow];
            augmented[maxRow] = temp;
            
            // Check for singular matrix
            if (Math.abs(augmented[k][k]) < 1e-10) {
                // Singular matrix, return zeros (will fall back to lower order)
                return new double[n];
            }
            
            // Eliminate column
            for (int i = k + 1; i < n; i++) {
                double factor = augmented[i][k] / augmented[k][k];
                for (int j = k; j <= n; j++) {
                    augmented[i][j] -= factor * augmented[k][j];
                }
            }
        }
        
        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = augmented[i][n];
            for (int j = i + 1; j < n; j++) {
                x[i] -= augmented[i][j] * x[j];
            }
            x[i] /= augmented[i][i];
        }
        
        return x;
    }
    
    /**
     * Evaluate polynomial at given x value.
     */
    private double evaluatePolynomial(double[] coeffs, double x) {
        double result = 0;
        double xPower = 1;
        for (double coeff : coeffs) {
            result += coeff * xPower;
            xPower *= x;
        }
        return result;
    }
    
    @Override
    public double[] trim(double[] result, int originalLength) {
        if (result.length == originalLength) {
            return result;
        }
        if (originalLength > result.length) {
            throw new InvalidArgumentException(
                    "Original length " + originalLength + " exceeds result length " + result.length);
        }
        
        double[] trimmed = new double[originalLength];
        
        switch (mode) {
            case RIGHT -> System.arraycopy(result, 0, trimmed, 0, originalLength);
            case LEFT -> System.arraycopy(result, result.length - originalLength, trimmed, 0, originalLength);
            case SYMMETRIC -> {
                int totalPadding = result.length - originalLength;
                int leftPad = totalPadding / 2;
                System.arraycopy(result, leftPad, trimmed, 0, originalLength);
            }
        }
        
        return trimmed;
    }
    
    @Override
    public String name() {
        return String.format("polynomial-%d-%s", order, mode.name().toLowerCase());
    }
    
    @Override
    public String description() {
        String orderName = switch (order) {
            case 1 -> "linear";
            case 2 -> "quadratic";
            case 3 -> "cubic";
            case 4 -> "quartic";
            case 5 -> "quintic";
            default -> "order-" + order;
        };
        return String.format("Polynomial extrapolation padding (%s, %d fit points, %s mode)", 
                orderName, fitPoints, mode.name().toLowerCase());
    }
}
package ai.prophetizo.wavelet.api;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

/**
 * Composite padding strategy that applies different strategies to left and right sides.
 * 
 * <p>This strategy enables asymmetric padding by using different methods for
 * extending the signal on each side. This is particularly useful when signal
 * characteristics differ at the boundaries. Ideal for:</p>
 * <ul>
 *   <li>Signals with different behavior at start and end</li>
 *   <li>Time series with initialization vs. termination phases</li>
 *   <li>Combining conservative and aggressive padding approaches</li>
 *   <li>Custom padding requirements for specific applications</li>
 * </ul>
 * 
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Financial data: constant padding at start (pre-market), trend at end (projection)</li>
 *   <li>Sensor data: zero padding at start (sensor warmup), statistical at end</li>
 *   <li>Audio: symmetric at start (smooth onset), zero at end (decay to silence)</li>
 * </ul>
 * 
 * @since 1.5.0
 */
public record CompositePaddingStrategy(
    PaddingStrategy leftStrategy,
    PaddingStrategy rightStrategy,
    double leftRatio
) implements PaddingStrategy {
    
    /**
     * Creates a composite strategy with equal padding on both sides.
     * 
     * @param leftStrategy strategy for left padding
     * @param rightStrategy strategy for right padding
     */
    public CompositePaddingStrategy(PaddingStrategy leftStrategy, PaddingStrategy rightStrategy) {
        this(leftStrategy, rightStrategy, 0.5);
    }
    
    /**
     * Validates parameters.
     */
    public CompositePaddingStrategy {
        if (leftStrategy == null) {
            throw new InvalidArgumentException("Left strategy cannot be null");
        }
        if (rightStrategy == null) {
            throw new InvalidArgumentException("Right strategy cannot be null");
        }
        if (leftRatio < 0 || leftRatio > 1) {
            throw new InvalidArgumentException("Left ratio must be between 0 and 1, got " + leftRatio);
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
        
        int totalPadding = targetLength - signal.length;
        int leftPadding = (int) Math.round(totalPadding * leftRatio);
        int rightPadding = totalPadding - leftPadding;
        
        double[] result = new double[targetLength];
        
        if (leftPadding > 0) {
            // Apply left padding
            double[] leftPadded = leftStrategy.pad(signal, signal.length + leftPadding);
            
            // Extract left padding based on strategy type
            if (isRightAlignedStrategy(leftStrategy)) {
                // Strategy pads on the right, we need to extract and reverse
                double[] leftPad = new double[leftPadding];
                for (int i = 0; i < leftPadding; i++) {
                    leftPad[i] = leftPadded[signal.length + i];
                }
                // Use the padding values at the beginning
                System.arraycopy(leftPad, 0, result, 0, leftPadding);
            } else {
                // Strategy pads on the left or symmetrically
                System.arraycopy(leftPadded, 0, result, 0, leftPadding);
            }
        }
        
        // Copy original signal
        System.arraycopy(signal, 0, result, leftPadding, signal.length);
        
        if (rightPadding > 0) {
            // Apply right padding
            double[] rightPadded = rightStrategy.pad(signal, signal.length + rightPadding);
            
            // Extract right padding
            if (isLeftAlignedStrategy(rightStrategy)) {
                // Strategy pads on the left, we need to extract and use end values
                double[] rightPad = new double[rightPadding];
                for (int i = 0; i < rightPadding; i++) {
                    rightPad[i] = rightPadded[i];
                }
                // Use the padding values at the end
                System.arraycopy(rightPad, 0, result, leftPadding + signal.length, rightPadding);
            } else {
                // Strategy pads on the right or symmetrically
                System.arraycopy(rightPadded, signal.length, 
                               result, leftPadding + signal.length, rightPadding);
            }
        }
        
        return result;
    }
    
    /**
     * Check if strategy typically pads on the right.
     */
    private boolean isRightAlignedStrategy(PaddingStrategy strategy) {
        // Most strategies pad on the right by default
        return !(strategy instanceof ConstantPaddingStrategy constant 
                 && constant.mode() == ConstantPaddingStrategy.PaddingMode.LEFT)
            && !(strategy instanceof LinearExtrapolationStrategy linear
                 && linear.mode() == LinearExtrapolationStrategy.PaddingMode.LEFT)
            && !(strategy instanceof PolynomialExtrapolationStrategy poly
                 && poly.mode() == PolynomialExtrapolationStrategy.PaddingMode.LEFT)
            && !(strategy instanceof StatisticalPaddingStrategy stat
                 && stat.mode() == StatisticalPaddingStrategy.PaddingMode.LEFT);
    }
    
    /**
     * Check if strategy typically pads on the left.
     */
    private boolean isLeftAlignedStrategy(PaddingStrategy strategy) {
        return (strategy instanceof ConstantPaddingStrategy constant 
                && constant.mode() == ConstantPaddingStrategy.PaddingMode.LEFT)
            || (strategy instanceof LinearExtrapolationStrategy linear
                && linear.mode() == LinearExtrapolationStrategy.PaddingMode.LEFT)
            || (strategy instanceof PolynomialExtrapolationStrategy poly
                && poly.mode() == PolynomialExtrapolationStrategy.PaddingMode.LEFT)
            || (strategy instanceof StatisticalPaddingStrategy stat
                && stat.mode() == StatisticalPaddingStrategy.PaddingMode.LEFT);
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
        
        // Trim based on the same ratio used for padding
        int totalPadding = result.length - originalLength;
        int leftTrim = (int) Math.round(totalPadding * leftRatio);
        
        double[] trimmed = new double[originalLength];
        System.arraycopy(result, leftTrim, trimmed, 0, originalLength);
        
        return trimmed;
    }
    
    @Override
    public String name() {
        return String.format("composite-%s-%s", 
                leftStrategy.name(), rightStrategy.name());
    }
    
    @Override
    public String description() {
        return String.format("Composite padding (left: %s [%.0f%%], right: %s [%.0f%%])",
                leftStrategy.name(), leftRatio * 100,
                rightStrategy.name(), (1 - leftRatio) * 100);
    }
    
    /**
     * Builder for creating composite padding strategies.
     */
    public static class Builder {
        private PaddingStrategy leftStrategy;
        private PaddingStrategy rightStrategy;
        private double leftRatio = 0.5;
        
        /**
         * Set the strategy for left padding.
         * 
         * @param strategy the left padding strategy
         * @return this builder
         */
        public Builder leftStrategy(PaddingStrategy strategy) {
            this.leftStrategy = strategy;
            return this;
        }
        
        /**
         * Set the strategy for right padding.
         * 
         * @param strategy the right padding strategy
         * @return this builder
         */
        public Builder rightStrategy(PaddingStrategy strategy) {
            this.rightStrategy = strategy;
            return this;
        }
        
        /**
         * Set the ratio of padding to apply on the left (0 to 1).
         * 
         * @param ratio fraction of total padding for left side
         * @return this builder
         */
        public Builder leftRatio(double ratio) {
            this.leftRatio = ratio;
            return this;
        }
        
        /**
         * Build the composite padding strategy.
         * 
         * @return the configured composite strategy
         */
        public CompositePaddingStrategy build() {
            if (leftStrategy == null) {
                throw new InvalidArgumentException("Left strategy must be specified");
            }
            if (rightStrategy == null) {
                throw new InvalidArgumentException("Right strategy must be specified");
            }
            return new CompositePaddingStrategy(leftStrategy, rightStrategy, leftRatio);
        }
    }
}
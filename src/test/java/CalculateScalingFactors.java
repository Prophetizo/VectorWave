import ai.prophetizo.wavelet.cwt.finance.DOGWavelet;
import ai.prophetizo.wavelet.cwt.finance.PaulWavelet;

public class CalculateScalingFactors {
    public static void main(String[] args) {
        // Calculate exact scaling factors needed
        
        System.out.println("=== Mexican Hat (DOG2) Scaling ===");
        DOGWavelet dog2 = new DOGWavelet(2);
        
        // MATLAB reference values
        double[][] matlabValues = {
            {-5.0, -0.0000888178},
            {-3.0, -0.0131550316},
            {-2.0, -0.1711006461},
            {-1.0, -0.3520653268},
            {0.0,   0.8673250706},
            {1.0,  -0.3520653268},
            {2.0,  -0.1711006461},
            {3.0,  -0.0131550316},
            {5.0,  -0.0000888178}
        };
        
        double sumRatio = 0;
        int count = 0;
        
        for (double[] point : matlabValues) {
            double t = point[0];
            double matlabValue = point[1];
            double ourValue = dog2.psi(t);
            
            if (Math.abs(matlabValue) > 1e-10 && Math.abs(ourValue) > 1e-10) {
                double ratio = matlabValue / ourValue;
                System.out.printf("t=%.1f: MATLAB=%.10f, Ours=%.10f, Ratio=%.10f\n", 
                    t, matlabValue, ourValue, ratio);
                sumRatio += ratio;
                count++;
            }
        }
        
        double avgRatio = sumRatio / count;
        System.out.println("\nAverage ratio: " + avgRatio);
        System.out.println("Scaling factor needed: " + avgRatio);
        
        // Test if it's a simple sigma scaling
        double sigma = Math.sqrt(1.0 / avgRatio);
        System.out.println("\nPossible sigma value in MATLAB: " + sigma);
        
        // Check if MATLAB uses σ = √5 instead of σ = 1
        double testSigma = Math.sqrt(5.0);
        double t = -3.0;
        double scaledT = t / testSigma;
        double testValue = 0.8673250706 * (1 - scaledT*scaledT) * Math.exp(-scaledT*scaledT/2) / testSigma;
        System.out.println("\nTest with σ=√5:");
        System.out.println("Value at t=-3: " + testValue);
        System.out.println("MATLAB value: " + matlabValues[1][1]);
        System.out.println("Match? " + (Math.abs(testValue - matlabValues[1][1]) < 1e-6));
    }
}
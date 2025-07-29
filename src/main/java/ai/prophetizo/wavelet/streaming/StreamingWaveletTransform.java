package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.TransformResult;

/**
 * Interface for streaming wavelet transforms that process continuous data streams
 * with overlapping windows.
 * 
 * <p>This interface enables real-time wavelet analysis of streaming signals by:</p>
 * <ul>
 *   <li>Processing overlapping windows to maintain temporal continuity</li>
 *   <li>Providing configurable overlap ratios for different analysis needs</li>
 *   <li>Supporting both sample-by-sample and batch processing modes</li>
 *   <li>Maintaining internal state for seamless streaming operation</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * StreamingWaveletTransform streaming = new StreamingWaveletTransformImpl(
 *     new Haar(), BoundaryMode.PERIODIC, 1024, 512);
 * 
 * // Process streaming data
 * double[] newSamples = acquireNewSamples();
 * streaming.addSamples(newSamples);
 * 
 * if (streaming.isResultReady()) {
 *     TransformResult result = streaming.getNextResult();
 *     // Process result...
 * }
 * }</pre>
 */
public interface StreamingWaveletTransform {
    
    /**
     * Adds new samples to the streaming buffer.
     * 
     * @param samples the samples to add to the processing queue
     * @throws IllegalArgumentException if samples is null
     */
    void addSamples(double[] samples);
    
    /**
     * Adds a single sample to the streaming buffer.
     * 
     * @param sample the sample to add
     * @return true if adding this sample results in a new transform result being ready
     */
    boolean addSample(double sample);
    
    /**
     * Checks if a transform result is ready for retrieval.
     * 
     * @return true if a complete window has been processed and a result is available
     */
    boolean isResultReady();
    
    /**
     * Retrieves the next available transform result.
     * This method advances the internal window to the next overlapping position.
     * 
     * @return the transform result for the current window
     * @throws IllegalStateException if no result is ready
     */
    TransformResult getNextResult();
    
    /**
     * Gets the window size used for transforms.
     * 
     * @return the size of each transform window in samples
     */
    int getWindowSize();
    
    /**
     * Gets the overlap size between consecutive windows.
     * 
     * @return the number of samples that overlap between windows
     */
    int getOverlapSize();
    
    /**
     * Gets the hop size (advance distance) between consecutive windows.
     * 
     * @return the number of samples to advance between windows
     */
    int getHopSize();
    
    /**
     * Gets the number of samples currently buffered.
     * 
     * @return the current buffer size
     */
    int getBufferedSampleCount();
    
    /**
     * Clears all buffered samples and resets the internal state.
     */
    void reset();
    
    /**
     * Checks if the streaming transform is empty (no buffered samples).
     * 
     * @return true if no samples are currently buffered
     */
    boolean isEmpty();
}
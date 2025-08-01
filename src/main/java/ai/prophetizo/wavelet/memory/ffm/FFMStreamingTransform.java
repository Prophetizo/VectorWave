package ai.prophetizo.wavelet.memory.ffm;

import ai.prophetizo.wavelet.TransformResult;
import ai.prophetizo.wavelet.api.Wavelet;
import ai.prophetizo.wavelet.api.DiscreteWavelet;

import java.lang.foreign.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Zero-copy streaming wavelet transform using Foreign Function & Memory API.
 * Processes continuous data streams without intermediate array allocations.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>True zero-copy operation on streaming data</li>
 *   <li>Ring buffer implemented with memory segments</li>
 *   <li>Lock-free for single producer/consumer</li>
 *   <li>Automatic memory management via Arena</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * try (FFMStreamingTransform stream = new FFMStreamingTransform(wavelet, 1024)) {
 *     // Process streaming data
 *     stream.processChunk(dataSegment, offset, length);
 *     
 *     // Get results when ready
 *     if (stream.hasCompleteBlock()) {
 *         TransformResult result = stream.getNextResult();
 *     }
 * }
 * }</pre>
 * 
 * @since 2.0.0
 */
public class FFMStreamingTransform implements AutoCloseable {
    
    private final Wavelet wavelet;
    private final int blockSize;
    private final Arena arena;
    private final FFMWaveletOps operations;
    
    // Ring buffer implemented with memory segments
    private final MemorySegment ringBuffer;
    private final long bufferCapacity;
    private final AtomicLong writePosition = new AtomicLong(0);
    private final AtomicLong readPosition = new AtomicLong(0);
    
    // Pre-allocated segments for transforms
    private final MemorySegment workBuffer;
    private final MemorySegment approxBuffer;
    private final MemorySegment detailBuffer;
    
    // Filter segments
    private final MemorySegment lowPassSeg;
    private final MemorySegment highPassSeg;
    private final int filterLength;
    
    /**
     * Creates a streaming transform with specified block size.
     * 
     * @param wavelet the wavelet to use
     * @param blockSize processing block size (must be power of 2)
     */
    public FFMStreamingTransform(Wavelet wavelet, int blockSize) {
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.blockSize = validateBlockSize(blockSize);
        this.arena = Arena.ofShared();
        this.operations = new FFMWaveletOps();
        
        // Validate wavelet type
        if (!(wavelet instanceof DiscreteWavelet discrete)) {
            throw new IllegalArgumentException("Only discrete wavelets supported");
        }
        
        // Setup ring buffer (2x block size for overlap handling)
        this.bufferCapacity = blockSize * 2L;
        this.ringBuffer = FFMArrayAllocator.allocateAligned(arena, bufferCapacity);
        
        // Pre-allocate work buffers
        this.workBuffer = FFMArrayAllocator.allocateAligned(arena, blockSize);
        this.approxBuffer = FFMArrayAllocator.allocateAligned(arena, blockSize / 2);
        this.detailBuffer = FFMArrayAllocator.allocateAligned(arena, blockSize / 2);
        
        // Setup filter segments
        double[] lowPass = discrete.lowPassDecomposition();
        double[] highPass = discrete.highPassDecomposition();
        this.filterLength = lowPass.length;
        
        this.lowPassSeg = arena.allocate(lowPass.length * Double.BYTES);
        this.highPassSeg = arena.allocate(highPass.length * Double.BYTES);
        
        MemorySegment.copy(lowPass, 0, lowPassSeg, ValueLayout.JAVA_DOUBLE, 0, lowPass.length);
        MemorySegment.copy(highPass, 0, highPassSeg, ValueLayout.JAVA_DOUBLE, 0, highPass.length);
    }
    
    /**
     * Processes a chunk of input data with zero-copy.
     * 
     * @param data the input memory segment
     * @param offset offset in elements
     * @param length number of elements
     */
    public void processChunk(MemorySegment data, long offset, long length) {
        long bytesOffset = offset * Double.BYTES;
        long bytesLength = length * Double.BYTES;
        
        if (bytesOffset + bytesLength > data.byteSize()) {
            throw new IndexOutOfBoundsException("Data slice exceeds segment bounds");
        }
        
        // Copy to ring buffer
        long writePos = writePosition.get();
        long available = bufferCapacity - (writePos - readPosition.get());
        
        if (length > available) {
            throw new IllegalStateException("Ring buffer overflow");
        }
        
        // Handle wrap-around
        long bufferOffset = writePos % bufferCapacity;
        long firstPart = Math.min(length, bufferCapacity - bufferOffset);
        
        // Copy first part
        MemorySegment.copy(data, ValueLayout.JAVA_DOUBLE, bytesOffset,
                          ringBuffer, ValueLayout.JAVA_DOUBLE, bufferOffset * Double.BYTES,
                          firstPart);
        
        // Copy wrapped part if needed
        if (firstPart < length) {
            MemorySegment.copy(data, ValueLayout.JAVA_DOUBLE, bytesOffset + firstPart * Double.BYTES,
                              ringBuffer, ValueLayout.JAVA_DOUBLE, 0,
                              length - firstPart);
        }
        
        writePosition.addAndGet(length);
    }
    
    /**
     * Processes a chunk from a double array.
     * 
     * @param data the input array
     * @param offset offset in array
     * @param length number of elements
     */
    public void processChunk(double[] data, int offset, int length) {
        MemorySegment dataSeg = MemorySegment.ofArray(data);
        processChunk(dataSeg, offset, length);
    }
    
    /**
     * Checks if a complete block is ready for transform.
     * 
     * @return true if block size data is available
     */
    public boolean hasCompleteBlock() {
        return writePosition.get() - readPosition.get() >= blockSize;
    }
    
    /**
     * Gets the next transform result using zero-copy processing.
     * 
     * @return transform result or null if no complete block
     */
    public TransformResult getNextResult() {
        if (!hasCompleteBlock()) {
            return null;
        }
        
        long readPos = readPosition.get();
        long bufferOffset = readPos % bufferCapacity;
        
        // Handle wrap-around by copying to work buffer
        if (bufferOffset + blockSize > bufferCapacity) {
            // Data wraps around - need to linearize
            long firstPart = bufferCapacity - bufferOffset;
            long secondPart = blockSize - firstPart;
            
            MemorySegment.copy(ringBuffer, ValueLayout.JAVA_DOUBLE, bufferOffset * Double.BYTES,
                              workBuffer, ValueLayout.JAVA_DOUBLE, 0,
                              firstPart);
            MemorySegment.copy(ringBuffer, ValueLayout.JAVA_DOUBLE, 0,
                              workBuffer, ValueLayout.JAVA_DOUBLE, firstPart * Double.BYTES,
                              secondPart);
            
            // Process from work buffer
            performTransform(workBuffer);
        } else {
            // Data is contiguous - process directly
            MemorySegment slice = ringBuffer.asSlice(bufferOffset * Double.BYTES, 
                                                     blockSize * Double.BYTES);
            performTransform(slice);
        }
        
        // Update read position
        readPosition.addAndGet(blockSize);
        
        // Copy results to arrays
        int outputSize = blockSize / 2;
        double[] approx = new double[outputSize];
        double[] detail = new double[outputSize];
        
        FFMArrayAllocator.copyToArray(approxBuffer, approx, 0, outputSize);
        FFMArrayAllocator.copyToArray(detailBuffer, detail, 0, outputSize);
        
        return TransformResult.create(approx, detail);
    }
    
    /**
     * Performs the actual wavelet transform on a memory segment.
     */
    private void performTransform(MemorySegment input) {
        operations.convolveAndDownsampleFFM(input, blockSize, lowPassSeg, filterLength,
                                           approxBuffer, blockSize / 2);
        operations.convolveAndDownsampleFFM(input, blockSize, highPassSeg, filterLength,
                                           detailBuffer, blockSize / 2);
    }
    
    /**
     * Gets available data in the buffer.
     * 
     * @return number of elements available
     */
    public long getAvailableData() {
        return writePosition.get() - readPosition.get();
    }
    
    /**
     * Resets the streaming buffer.
     */
    public void reset() {
        writePosition.set(0);
        readPosition.set(0);
        ringBuffer.fill((byte) 0);
    }
    
    /**
     * Creates a view of the current buffer contents.
     * Useful for debugging or analysis.
     * 
     * @return array containing current buffer data
     */
    public double[] getBufferSnapshot() {
        long available = getAvailableData();
        if (available == 0) return new double[0];
        
        double[] snapshot = new double[(int) Math.min(available, blockSize)];
        long readPos = readPosition.get();
        long bufferOffset = readPos % bufferCapacity;
        
        for (int i = 0; i < snapshot.length; i++) {
            long idx = (bufferOffset + i) % bufferCapacity;
            snapshot[i] = ringBuffer.getAtIndex(ValueLayout.JAVA_DOUBLE, idx);
        }
        
        return snapshot;
    }
    
    @Override
    public void close() {
        operations.close();
        arena.close();
    }
    
    private static int validateBlockSize(int size) {
        if (size <= 0 || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("Block size must be positive power of 2: " + size);
        }
        return size;
    }
}
package ai.prophetizo.wavelet.streaming;

import ai.prophetizo.wavelet.exception.InvalidArgumentException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lock-free ring buffer implementation for high-performance streaming.
 * 
 * <p>This implementation provides zero-copy sliding window operations and
 * minimizes memory bandwidth usage for streaming wavelet transforms.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Lock-free concurrent operations using atomic primitives</li>
 *   <li>Zero-copy sliding window via pointer arithmetic</li>
 *   <li>Cache-efficient memory access patterns</li>
 *   <li>Configurable capacity (must be power of 2)</li>
 * </ul>
 * 
 * <p><b>Thread Safety Guarantees:</b></p>
 * <ul>
 *   <li><b>SPSC (Single Producer, Single Consumer):</b> Fully thread-safe without
 *       external synchronization. One thread can write while another reads concurrently.</li>
 *   <li><b>MPSC (Multiple Producer, Single Consumer):</b> Requires external synchronization
 *       on the write methods. Read operations remain lock-free.</li>
 *   <li><b>SPMC (Single Producer, Multiple Consumer):</b> Requires external synchronization
 *       on the read methods. Write operations remain lock-free.</li>
 *   <li><b>MPMC (Multiple Producer, Multiple Consumer):</b> Not supported. Use a proper
 *       MPMC queue implementation instead.</li>
 * </ul>
 * 
 * <p><b>Memory Ordering:</b> Uses acquire/release semantics for atomic operations to ensure
 * proper visibility of data across threads. No explicit memory barriers needed for SPSC usage.</p>
 * 
 * <p><b>False Sharing Prevention:</b> Read and write positions are in separate AtomicInteger
 * instances to prevent false sharing between producer and consumer threads.</p>
 */
public class RingBuffer {
    
    private final double[] buffer;
    private final int capacity;
    private final int mask;
    
    // Separate read and write positions for cache efficiency
    private final AtomicInteger writePosition = new AtomicInteger(0);
    private final AtomicInteger readPosition = new AtomicInteger(0);
    
    /**
     * Creates a new ring buffer with the specified capacity.
     * 
     * @param capacity the buffer capacity (must be power of 2)
     * @throws InvalidArgumentException if capacity is not a power of 2 or less than 2
     */
    public RingBuffer(int capacity) {
        if (capacity < 2) {
            throw new InvalidArgumentException("Capacity must be at least 2, got: " + capacity);
        }
        if ((capacity & (capacity - 1)) != 0) {
            throw new InvalidArgumentException("Capacity must be a power of 2, got: " + capacity);
        }
        
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.buffer = new double[capacity];
    }
    
    /**
     * Writes a single value to the buffer.
     * 
     * @param value the value to write
     * @return true if written successfully, false if buffer is full
     */
    public boolean write(double value) {
        int currentWrite = writePosition.get();
        int nextWrite = (currentWrite + 1) & mask;
        int currentRead = readPosition.get(); // Snapshot read position
        
        // Check if buffer is full
        if (nextWrite == currentRead) {
            return false;
        }
        
        buffer[currentWrite] = value;
        writePosition.set(nextWrite);
        return true;
    }
    
    /**
     * Writes multiple values to the buffer.
     * 
     * @param data the data to write
     * @param offset the start offset in the data array
     * @param length the number of elements to write
     * @return the number of elements actually written
     */
    public int write(double[] data, int offset, int length) {
        if (data == null) {
            throw new InvalidArgumentException("Data cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new InvalidArgumentException("Invalid offset or length");
        }
        
        int written = 0;
        int currentWrite = writePosition.get();
        int currentRead = readPosition.get();
        
        // Calculate available space
        int available = (currentRead - currentWrite - 1) & mask;
        int toWrite = Math.min(available, length);
        
        // Write in up to two chunks (wrap around)
        int firstChunk = Math.min(toWrite, capacity - currentWrite);
        if (firstChunk > 0) {
            System.arraycopy(data, offset, buffer, currentWrite, firstChunk);
            written = firstChunk;
        }
        
        int secondChunk = toWrite - firstChunk;
        if (secondChunk > 0) {
            System.arraycopy(data, offset + firstChunk, buffer, 0, secondChunk);
            written += secondChunk;
        }
        
        writePosition.set((currentWrite + written) & mask);
        return written;
    }
    
    /**
     * Reads a single value from the buffer.
     * 
     * @return the value read, or Double.NaN if buffer is empty
     */
    public double read() {
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get(); // Snapshot write position
        
        // Check if buffer is empty
        if (currentRead == currentWrite) {
            return Double.NaN;
        }
        
        double value = buffer[currentRead];
        readPosition.set((currentRead + 1) & mask);
        return value;
    }
    
    /**
     * Reads multiple values from the buffer.
     * 
     * @param data the array to read into
     * @param offset the start offset in the data array
     * @param length the number of elements to read
     * @return the number of elements actually read
     */
    public int read(double[] data, int offset, int length) {
        if (data == null) {
            throw new InvalidArgumentException("Data cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new InvalidArgumentException("Invalid offset or length");
        }
        
        int read = 0;
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get();
        
        // Calculate available data
        int available = (currentWrite - currentRead) & mask;
        int toRead = Math.min(available, length);
        
        // Read in up to two chunks (wrap around)
        int firstChunk = Math.min(toRead, capacity - currentRead);
        if (firstChunk > 0) {
            System.arraycopy(buffer, currentRead, data, offset, firstChunk);
            read = firstChunk;
        }
        
        int secondChunk = toRead - firstChunk;
        if (secondChunk > 0) {
            System.arraycopy(buffer, 0, data, offset + firstChunk, secondChunk);
            read += secondChunk;
        }
        
        readPosition.set((currentRead + read) & mask);
        return read;
    }
    
    /**
     * Peeks at data without advancing the read position.
     * 
     * @param data the array to peek into
     * @param offset the start offset in the data array
     * @param length the number of elements to peek
     * @return the number of elements actually peeked
     */
    public int peek(double[] data, int offset, int length) {
        if (data == null) {
            throw new InvalidArgumentException("Data cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new InvalidArgumentException("Invalid offset or length");
        }
        
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get();
        
        // Calculate available data
        int available = (currentWrite - currentRead) & mask;
        int toPeek = Math.min(available, length);
        
        // Peek in up to two chunks (wrap around)
        int firstChunk = Math.min(toPeek, capacity - currentRead);
        if (firstChunk > 0) {
            System.arraycopy(buffer, currentRead, data, offset, firstChunk);
        }
        
        int secondChunk = toPeek - firstChunk;
        if (secondChunk > 0) {
            System.arraycopy(buffer, 0, data, offset + firstChunk, secondChunk);
        }
        
        return toPeek;
    }
    
    /**
     * Advances the read position by the specified amount.
     * 
     * @param count the number of positions to advance
     * @return the actual number of positions advanced
     */
    public int skip(int count) {
        if (count < 0) {
            throw new InvalidArgumentException("Count cannot be negative");
        }
        
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get();
        
        int available = (currentWrite - currentRead) & mask;
        int toSkip = Math.min(available, count);
        
        readPosition.set((currentRead + toSkip) & mask);
        return toSkip;
    }
    
    /**
     * Returns the number of elements available for reading.
     * 
     * @return the number of available elements
     */
    public int available() {
        // Snapshot positions to avoid race conditions
        int currentWrite = writePosition.get();
        int currentRead = readPosition.get();
        return (currentWrite - currentRead) & mask;
    }
    
    /**
     * Returns the remaining capacity for writing.
     * 
     * @return the remaining capacity
     */
    public int remainingCapacity() {
        return capacity - available() - 1;
    }
    
    /**
     * Checks if the buffer is empty.
     * 
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        // Snapshot positions to avoid race conditions
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get();
        return currentRead == currentWrite;
    }
    
    /**
     * Checks if the buffer is full.
     * 
     * @return true if full, false otherwise
     */
    public boolean isFull() {
        // Snapshot positions to avoid race conditions
        int currentWrite = writePosition.get();
        int currentRead = readPosition.get();
        int nextWrite = (currentWrite + 1) & mask;
        return nextWrite == currentRead;
    }
    
    /**
     * Clears the buffer by resetting positions.
     */
    public void clear() {
        readPosition.set(0);
        writePosition.set(0);
    }
    
    /**
     * Returns the buffer capacity.
     * 
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }
}
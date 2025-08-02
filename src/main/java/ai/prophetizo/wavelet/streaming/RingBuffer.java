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
    
    // Cache line size constant for false sharing prevention
    private static final int CACHE_LINE_SIZE = 64; // bytes
    private static final int ATOMIC_INT_SIZE = 4;  // bytes (not counting object overhead)
    private static final int PADDING_LONGS = 7;    // 7 * 8 bytes = 56 bytes of padding
    
    private final double[] buffer;
    private final int capacity;
    private final int mask;
    
    // Separate read and write positions with padding to prevent false sharing
    // Cache line is typically 64 bytes. AtomicInteger is 4 bytes + object overhead (~12-16 bytes)
    // We add explicit padding to ensure they're on different cache lines
    
    // Writer's cache line
    private final AtomicInteger writePosition = new AtomicInteger(0);
    
    // PADDING: DO NOT REMOVE OR ACCESS THESE FIELDS
    // These fields exist solely to prevent false sharing between writePosition and readPosition.
    // They must be:
    // - volatile: prevents compiler/JVM reordering or optimization
    // - long: 8 bytes each for predictable size  
    // - never accessed: any access would defeat their purpose
    // Total: PADDING_LONGS * 8 = 56 bytes, ensuring separation on CACHE_LINE_SIZE-byte cache lines
    //
    // @SuppressWarnings("unused") is required because:
    // - IDEs and static analysis tools will flag these fields as unused (which is correct)
    // - We WANT them to be unused - their only purpose is to occupy memory space
    // - Without this annotation, developers might be tempted to remove them as "dead code"
    // - The annotation serves as additional documentation that the unused state is intentional
    @SuppressWarnings("unused")
    private volatile long p1, p2, p3, p4, p5, p6, p7;
    
    // Reader's cache line  
    private final AtomicInteger readPosition = new AtomicInteger(0);
    
    // PADDING: DO NOT REMOVE OR ACCESS THESE FIELDS (see above)
    // @SuppressWarnings("unused") - Same reasoning as p1-p7: these fields must remain unused
    @SuppressWarnings("unused")
    private volatile long p8, p9, p10, p11, p12, p13, p14;
    
    /**
     * Creates a new ring buffer with the specified capacity.
     * 
     * <p>The capacity must be a power of 2 (e.g., 16, 32, 64, 128, 256, 512, 1024).
     * This constraint enables efficient bitwise modulo operations using {@code (index & (capacity - 1))}
     * instead of the more expensive {@code index % capacity}. This optimization is critical
     * for high-performance streaming applications where every CPU cycle matters.</p>
     * 
     * <p>Example: With capacity 256 (0x100), the mask becomes 255 (0xFF), allowing
     * wrap-around via simple bitwise AND instead of integer division.</p>
     * 
     * @param capacity the buffer capacity (must be power of 2, minimum 2)
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
        // In SPSC: producer reads other's position first, then own position
        // If consumer advances readPosition after we read it, we may get a false
        // negative (buffer appears full when it's not), but never data corruption
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get();
        int nextWrite = (currentWrite + 1) & mask;
        
        // Check if buffer is full
        if (nextWrite == currentRead) {
            return false;
        }
        
        buffer[currentWrite] = value;
        
        // Prefetch next cache line for future writes
        prefetchForWrite((currentWrite + CACHE_LINE_SIZE / 8) & mask);
        
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
        int currentRead = readPosition.get(); // Read this first for SPSC consistency
        int currentWrite = writePosition.get();
        
        // Calculate available space
        int available = (currentRead - currentWrite - 1 + capacity) & mask;
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
        
        // Prefetch for next write operation
        if (written > 0) {
            prefetchForWrite(((currentWrite + written) & mask));
        }
        
        return written;
    }
    
    /**
     * Reads a single value from the buffer.
     * 
     * @return the value read, or Double.NaN if buffer is empty
     */
    public double read() {
        int currentWrite = writePosition.get(); // Read this first for SPSC consistency
        int currentRead = readPosition.get();
        
        // Check if buffer is empty
        if (currentRead == currentWrite) {
            return Double.NaN;
        }
        
        double value = buffer[currentRead];
        
        // Prefetch next elements for sequential reads
        prefetchForRead(currentRead, 8);
        
        readPosition.set((currentRead + 1) & mask);
        return value;
    }
    
    /**
     * Writes multiple batches of data to the buffer in a single operation.
     * This method optimizes for multiple small writes by minimizing atomic operations.
     * 
     * @param batches array of data batches to write
     * @return total number of elements written across all batches
     */
    public int writeBatch(double[][] batches) {
        if (batches == null || batches.length == 0) {
            return 0;
        }
        
        // Calculate total size needed
        int totalSize = 0;
        for (double[] batch : batches) {
            if (batch != null) {
                totalSize += batch.length;
            }
        }
        
        if (totalSize == 0) {
            return 0;
        }
        
        // Read positions once for consistency
        int currentRead = readPosition.get();
        int currentWrite = writePosition.get();
        
        // Calculate available space
        int available = (currentRead - currentWrite - 1 + capacity) & mask;
        if (available < totalSize) {
            // Not enough space for all batches - write what we can
            return writeBatchPartial(batches, available);
        }
        
        // Fast path: write all batches in one go
        int written = 0;
        int writePos = currentWrite;
        
        for (double[] batch : batches) {
            if (batch == null || batch.length == 0) {
                continue;
            }
            
            int remaining = batch.length;
            int srcOffset = 0;
            
            while (remaining > 0) {
                int chunkSize = Math.min(remaining, capacity - writePos);
                System.arraycopy(batch, srcOffset, buffer, writePos, chunkSize);
                
                written += chunkSize;
                remaining -= chunkSize;
                srcOffset += chunkSize;
                writePos = (writePos + chunkSize) & mask;
            }
        }
        
        // Single atomic update for all batches
        writePosition.set(writePos);
        return written;
    }
    
    /**
     * Helper method to write partial batches when buffer space is limited.
     */
    private int writeBatchPartial(double[][] batches, int availableSpace) {
        int written = 0;
        int remaining = availableSpace;
        
        for (double[] batch : batches) {
            if (batch == null || batch.length == 0 || remaining == 0) {
                continue;
            }
            
            int toWrite = Math.min(batch.length, remaining);
            int actuallyWritten = write(batch, 0, toWrite);
            written += actuallyWritten;
            remaining -= actuallyWritten;
            
            if (actuallyWritten < toWrite) {
                // Buffer became full
                break;
            }
        }
        
        return written;
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
        int currentWrite = writePosition.get(); // Read this first for SPSC consistency
        int currentRead = readPosition.get();
        
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
        
        int currentWrite = writePosition.get(); // Read this first for SPSC consistency
        int currentRead = readPosition.get();
        
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
        
        int currentWrite = writePosition.get(); // Read this first for SPSC consistency
        int currentRead = readPosition.get();
        
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
        // Read in correct order for SPSC consistency
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
        // Read in correct order for SPSC consistency
        int currentWrite = writePosition.get();
        int currentRead = readPosition.get();
        return currentRead == currentWrite;
    }
    
    /**
     * Checks if the buffer is full.
     * 
     * @return true if full, false otherwise
     */
    public boolean isFull() {
        // Read in correct order for SPSC consistency
        int currentRead = readPosition.get(); // Writer reads other's position first
        int currentWrite = writePosition.get();
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
    
    /**
     * Prefetches data for writing by accessing the memory location.
     * This triggers the CPU's hardware prefetcher to load the cache line.
     * 
     * @param index the buffer index to prefetch
     */
    private void prefetchForWrite(int index) {
        // Touch the memory location to trigger hardware prefetch
        // This is a portable way to hint the CPU without using Unsafe
        // The JIT compiler may optimize this to a prefetch instruction
        if (index >= 0 && index < capacity) {
            @SuppressWarnings("unused")
            double dummy = buffer[index];
        }
    }
    
    /**
     * Prefetches data for reading by accessing future memory locations.
     * This is called during read operations to warm up the cache.
     * 
     * @param startIndex the starting buffer index
     * @param count number of elements that will be read
     */
    private void prefetchForRead(int startIndex, int count) {
        // Prefetch ahead by one cache line
        int prefetchDistance = Math.min(CACHE_LINE_SIZE / 8, count);
        int prefetchIndex = (startIndex + prefetchDistance) & mask;
        
        if (prefetchIndex < capacity) {
            @SuppressWarnings("unused")
            double dummy = buffer[prefetchIndex];
        }
    }
}
import java.net.URL

/**
 * Represents a specific byte range of a file to be downloaded.
 * * @property id A unique identifier for the chunk, typically its zero-based index.
 * @property startByte The inclusive starting byte position.
 * @property endByte The inclusive ending byte position.
 */

data class DownloadChunk(
    val id: Int,
    val startByte: Long,
    val endByte: Long
)

/**
 * Responsible for partitioning a total file size into distinct, non-overlapping byte ranges.
 */
interface ChunkCalculator {
    /**
     * Calculates the exact byte boundaries for parallel downloading.
     *
     * @param totalBytes The total size of the file in bytes.
     * @param threads The number of parallel partitions to create.
     * @return A list of [DownloadChunk] objects representing the byte ranges.
     * @throws IllegalArgumentException if the thread count is zero or negative.
     */
    fun calculateChunks(totalBytes: Long, threads: Int): List<DownloadChunk>
}

/**
 * Abstracts the underlying storage mechanism for writing downloaded bytes.
 * Implementations must manage their own resource lifecycles and ensure thread-safe writes.
 */
interface FileStorage : AutoCloseable {
    /**
     * Pre-allocates the necessary space on the storage medium to prevent
     * out-of-space errors during the download process.
     *
     * @param totalBytes The total expected size of the file.
     */
    fun allocateFile(totalBytes: Long)

    /**
     * Writes a block of data to the storage medium at the specified byte offset.
     * This method must be thread-safe to support concurrent chunk downloads.
     *
     * @param startByte The absolute byte position in the file to begin writing.
     * @param data The byte array containing the downloaded chunk data.
     */
    fun writeChunk(startByte: Long, data: ByteArray)
}

/**
 * Orchestrates the parallel downloading of a file over HTTP.
 * * This client requires the target server to support the `Accept-Ranges: bytes` header
 * to successfully split the file into concurrent chunks.
 */
interface FileDownloader {
    /**
     * Downloads a file to the local filesystem.
     *
     * @param url The target URL of the file.
     * @param destinationPath The local file path where the downloaded file will be saved.
     * @param concurrency The number of parallel network connections to open.
     * @throws UnsupportedOperationException if the server does not support partial downloads.
     */
    suspend fun download(url: URL, destinationPath: String, concurrency: Int)
}

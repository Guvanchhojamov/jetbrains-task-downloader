import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.path.Path

class ParallelFileDownloader(
    private val chunkCalculator: ChunkCalculator,
    private val createStorage: (String) -> FileStorage
) : FileDownloader {

    companion object {
        private const val HEADER_ACCEPT_RANGES = "Accept-Ranges"
        private const val HEADER_CONTENT_LENGTH = "Content-Length"
        private const val HEADER_RANGE = "Range"
        private const val RANGES_BYTES = "bytes"
        private const val REQUEST_METHOD_HEAD = "HEAD"

        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 10000
    }

    override suspend fun download(url: URL, destinationPath: String, concurrency: Int) {
        val totalBytes = fetchContentLength(url)
        require(totalBytes > 0) { "File is empty or server does not support Content-Length" }

        val chunks = chunkCalculator.calculateChunks(totalBytes, concurrency)
        createStorage(destinationPath).use { storage ->
            storage.allocateFile(totalBytes)

            coroutineScope {
                val deferredChunks = chunks.map { chunk ->
                    async(Dispatchers.IO) {
                        downloadChunk(url, chunk, storage)
                    }
                }
                deferredChunks.awaitAll()
            }
        }
    }

    private fun fetchContentLength(url: URL): Long {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = REQUEST_METHOD_HEAD
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Server returned HTTP $responseCode instead of 200 OK. Is the file URL correct?")
            }

            val acceptRanges = connection.getHeaderField(HEADER_ACCEPT_RANGES)
            if (acceptRanges != RANGES_BYTES) {
                throw UnsupportedOperationException("Server does not support partial downloads ($HEADER_ACCEPT_RANGES: $acceptRanges)")
            }
            connection.getHeaderField(HEADER_CONTENT_LENGTH)?.toLong() ?: 0L
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadChunk(url: URL, chunk: DownloadChunk, storage: FileStorage) {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(HEADER_RANGE, "bytes=${chunk.startByte}-${chunk.endByte}")

        try {
            connection.inputStream.use { input ->
                val data = input.readBytes()
                storage.writeChunk(chunk.startByte, data)
            }
        } finally {
            connection.disconnect()
        }
    }
}
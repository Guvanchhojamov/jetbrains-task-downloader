import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ParallelDownloader(private val url: String) {

    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun download(destinationPath: String, chunkSize: Long = 1024 * 1024) = coroutineScope {
        println("Starting download for $url...")

        val totalBytes = fetchMetadata()

        val ranges = calculateRanges(totalBytes, chunkSize)
        println("Split into ${ranges.size} chunks.")

        val file = File(destinationPath)
        RandomAccessFile(file, "rw").use { it.setLength(totalBytes) }

        val deferredChunks = ranges.map { range ->
            async(Dispatchers.IO) {
                downloadChunk(range, file)
            }
        }

        deferredChunks.awaitAll()
        println("Download complete! Saved to $destinationPath")
    }

    fun fetchMetadata(): Long {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .header("User-Agent", "ParallelDownloader/1.0")
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.discarding())

        if (response.statusCode() != 200) {
            throw Exception("Failed to fetch metadata. HTTP Status: ${response.statusCode()}")
        }

        val acceptRanges = response.headers().firstValue("Accept-Ranges").orElse("")
        if (acceptRanges != "bytes") {
            throw Exception("Server does not support parallel downloads.")
        }

        return response.headers().firstValue("Content-Length")
            .orElseThrow { Exception("Content-Length header is missing!") }
            .toLong()
    }

    fun calculateRanges(totalBytes: Long, chunkSize: Long): List<LongRange> {
        val ranges = mutableListOf<LongRange>()
        var start = 0L
        while (start < totalBytes) {
            val end = minOf(start + chunkSize - 1, totalBytes - 1)
            ranges.add(start..end)
            start += chunkSize
        }
        return ranges
    }

    private fun downloadChunk(range: LongRange, file: File) {
        println("Downloading bytes ${range.first}-${range.last}...")

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Range", "bytes=${range.first}-${range.last}")
            .header("User-Agent", "ParallelDownloader/1.0")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() != 206 && response.statusCode() != 200) {
            throw Exception("Chunk ${range.first}-${range.last} failed. Status: ${response.statusCode()}")
        }

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(range.first)

            val inputStream = response.body()
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                raf.write(buffer, 0, bytesRead)
            }
        }
        println("Finished chunk ${range.first}-${range.last}")
    }
}
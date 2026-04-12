import kotlinx.coroutines.runBlocking
import java.net.URI
import kotlin.io.path.Path

private const val TARGET_URL = "http://localhost:8080/test.txt"
private const val DESTINATION_FILE = "downloaded-file.txt"
private const val THREAD_COUNT = 4

fun main() = runBlocking {
    val targetUrl = URI(TARGET_URL).toURL()
    val calculator = DefaultChunkCalculator()

    val downloader = ParallelFileDownloader(
        chunkCalculator = calculator,
        createStorage = { path -> LocalFileStorage(Path(path)) }
    )

    println("Starting download from $targetUrl with $THREAD_COUNT threads...")
    try {
        val startTime = System.currentTimeMillis()

        downloader.download(targetUrl, DESTINATION_FILE, THREAD_COUNT)

        val endTime = System.currentTimeMillis()
        println("Download complete! Saved to $DESTINATION_FILE in ${endTime - startTime}ms")
    } catch (e: Exception) {
        System.err.println("Download failed: ${e.message}")
        e.printStackTrace()
    }
}
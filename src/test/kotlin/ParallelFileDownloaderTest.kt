import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.*

class ParallelFileDownloaderTest {

    private lateinit var mockServer: MockWebServer
    private val testContent = "JetBrains internship tests require covering all edge cases perfectly!"
    private val testFileDestination = "test_output.txt"

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        Path(testFileDestination).deleteIfExists()
    }

    @AfterTest
    fun teardown() {
        mockServer.shutdown()
        Path(testFileDestination).deleteIfExists()
    }

    @Test
    fun `successful parallel download assembles file correctly`() = runBlocking {
        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "HEAD") {
                    return MockResponse()
                        .setResponseCode(200)
                        .addHeader("Accept-Ranges", "bytes")
                        .addHeader("Content-Length", testContent.toByteArray().size.toString())
                }

                if (request.method == "GET") {
                    val rangeHeader = request.getHeader("Range") ?: return MockResponse().setResponseCode(400)
                    val bounds = rangeHeader.removePrefix("bytes=").split("-")
                    val start = bounds[0].toInt()
                    val end = bounds[1].toInt()

                    val chunk = testContent.substring(start, end + 1)
                    return MockResponse()
                        .setResponseCode(206)
                        .setBody(chunk)
                }
                return MockResponse().setResponseCode(404)
            }
        }

        val url = mockServer.url("/testfile.txt").toUrl() // Using OkHttp's safe URL builder
        val downloader = ParallelFileDownloader(
            chunkCalculator = DefaultChunkCalculator(),
            createStorage = { path -> LocalFileStorage(Path(path)) }
        )

        downloader.download(url, testFileDestination, concurrency = 4)

        val downloadedPath = Path(testFileDestination)
        assertTrue(downloadedPath.exists(), "File should have been created on disk")
        assertEquals(testContent, downloadedPath.readText(), "Downloaded content must match exactly")
    }

    @Test
    fun `throws exception when server does not support Accept-Ranges`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Length", "100")
            // Missing Accept-Ranges!
        )

        val url = mockServer.url("/badfile.txt").toUrl()
        val downloader = ParallelFileDownloader(
            chunkCalculator = DefaultChunkCalculator(),
            createStorage = { path -> LocalFileStorage(Path(path)) }
        )

        val exception = assertFailsWith<UnsupportedOperationException> {
            downloader.download(url, testFileDestination, concurrency = 4)
        }

        assertTrue(exception.message!!.contains("Server does not support partial downloads"))
    }
}
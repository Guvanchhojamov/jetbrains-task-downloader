import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue



class ParallelDownloaderTest {

    private lateinit var mockServer: MockWebServer
    private val testContent = "JetBrains is awesome!"
    private val testFileDestination = "test_output.txt"

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "HEAD") {
                    return MockResponse()
                        .setResponseCode(200)
                        .addHeader("Accept-Ranges", "bytes")
                        .setBody(testContent)
                }

                if (request.method == "GET") {
                    val rangeHeader = request.getHeader("Range") ?: ""
                    if (rangeHeader.startsWith("bytes=")) {
                        val bounds = rangeHeader.removePrefix("bytes=").split("-")
                        val start = bounds[0].toInt()
                        val end = bounds[1].toInt()

                        val chunk = testContent.substring(start, end + 1)
                        return MockResponse()
                            .setResponseCode(206) // 206 Partial Content
                            .setBody(chunk)
                    }
                }
                return MockResponse().setResponseCode(400)
            }
        }
        mockServer.start()
    }

    @AfterTest
    fun teardown() {
        mockServer.shutdown()
        val file = File(testFileDestination)
        if (file.exists()) file.delete()
    }

    @Test
    fun `test parallel download assembles file correctly`() = runBlocking {
        val url = mockServer.url("/testfile.txt").toString()
        val downloader = ParallelDownloader(url)
        val chunkSize = 5L


        downloader.download(testFileDestination, chunkSize)

        val downloadedFile = File(testFileDestination)
        assertTrue(downloadedFile.exists(), "File should have been created")

        val downloadedText = downloadedFile.readText()
        assertEquals(testContent, downloadedText, "The downloaded content should match exactly")
    }
}
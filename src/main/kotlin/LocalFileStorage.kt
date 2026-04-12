import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

class LocalFileStorage(private val filePath: Path) : FileStorage {
    private var randomAccessFile: RandomAccessFile? = null

    override fun allocateFile(totalBytes: Long) {
        Files.deleteIfExists(filePath)
        randomAccessFile = RandomAccessFile(filePath.toFile(), "rw").apply {
            setLength(totalBytes)
        }
    }

    override fun writeChunk(startByte: Long, data: ByteArray) {
        val raf = randomAccessFile ?: throw IllegalStateException("File must be allocated before writing")

        synchronized(raf) {
            raf.seek(startByte)
            raf.write(data)
        }
    }

    override fun close() {
        randomAccessFile?.close()
    }
}
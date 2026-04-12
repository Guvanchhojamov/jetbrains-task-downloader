class DefaultChunkCalculator : ChunkCalculator {
    override fun calculateChunks(totalBytes: Long, threads: Int): List<DownloadChunk> {
        if (totalBytes <= 0) return emptyList()
        if (threads <= 0) throw IllegalArgumentException("Thread count must be greater than 0")

        val chunkSize = totalBytes / threads
        return (0 until threads).map { i ->
            val start = i * chunkSize
            val end = if (i == threads - 1) totalBytes - 1 else (start + chunkSize - 1)
            DownloadChunk(id = i, startByte = start, endByte = end)
        }
    }
}


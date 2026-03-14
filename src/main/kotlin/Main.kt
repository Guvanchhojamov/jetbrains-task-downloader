package org.example

import ParallelDownloader

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val testUrl = "http://localhost:8080/test.txt"
    val downloader = ParallelDownloader(testUrl)

    // We will save it locally as "downloaded_test.txt"
    val destinationFile = "downloaded_test.txt"

    try {
        // We use a small chunk size of 10 bytes to force it to split our tiny file
        downloader.download(destinationPath = destinationFile, chunkSize = 10L)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}


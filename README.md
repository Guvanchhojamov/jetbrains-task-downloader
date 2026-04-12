# Parallel File Downloader

A Kotlin-based tool that downloads files in parallel chunks using HTTP `Range` requests.

Built with JetBrains, for JetBrains :)

## Architecture & Design Decisions

To ensure the codebase is maintainable and highly testable, I utilized SOLID principles and separated the core logic into distinct domains using Dependency Injection:

* **`ChunkCalculator`**: Pure logic. Isolates the mathematical partitioning of byte ranges from network and disk I/O, allowing for reliable unit testing without side effects.
* **`FileStorage`**: Abstracts the disk writing mechanism. It implements `AutoCloseable` to guarantee safe resource cleanup and uses synchronized blocks to prevent data corruption during concurrent writes.
* **`FileDownloader`**: The core orchestrator. It expects dependencies to be injected via its constructor, meaning it can easily be mocked or extended in the future (e.g., swapping local storage for a cloud bucket).

## How to Run

### 1. Start the Local Test Server
Create a local folder containing a test file (e.g., `test.txt`). Then, start the test server using Docker:

```
docker run --rm -p 8080:80 -v /path/to/your/local/folder:/usr/local/apache2/htdocs/ httpd:latest
```

### 2. Run the Downloader
Make sure you have a JDK installed (Java 17 or higher). Open your terminal in the project root and run:
bash
```
./gradlew run
```


### 3. Run the  Tests
The test suite includes both pure logic unit tests and integration tests using an embedded `MockWebServer`.
bash
```
./gradlew test
```

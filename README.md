# Parallel File Downloader

A simple Kotlin tool that downloads files in parallel chunks using HTTP `Range` requests. 

Built with JetBrains, for JetBrains SDK Engineering :)

## Features
* Downloads file chunks concurrently using Kotlin Coroutines.
* Uses `RandomAccessFile` to safely write chunks to disk in parallel without high memory usage.
* Includes offline unit tests using `MockWebServer`.

## How to Run

### 1. Start the Local Test Server
Create a local folder containing a test file (e.g., `test.txt`). Then, start the test server using Docker:

```bash
docker run --rm -p 8080:80 -v /absolute/path/to/your/local/folder:/usr/local/apache2/htdocs/ httpd:latest
```

### 2. Run the Downloader
Make sure you have a JDK installed (Java 17 or higher). Open your terminal in the project root and run:
```bash
./gradlew run
```
### 3. Run the Unit Tests
The test suite uses an embedded mock server. 
```bash
./gradlew test
```

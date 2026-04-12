import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultChunkCalculatorTest {
    private val calculator = DefaultChunkCalculator()
    @Test
    fun `calculates chunks evenly when evenly divisible`() {
        val chunks = calculator.calculateChunks(100L, 4)
        assertEquals(4, chunks.size)
        assertEquals(0L, chunks[0].startByte)
        assertEquals(24L, chunks[0].endByte)
        assertEquals(75L, chunks[3].startByte)
        assertEquals(99L, chunks[3].endByte)
    }

    @Test
    fun `calculates chunks correctly with a remainder`() {
        val chunks = calculator.calculateChunks(102L, 4)

        assertEquals(4, chunks.size)
        assertEquals(75L, chunks[3].startByte)
        assertEquals(101L, chunks[3].endByte)
    }
    @Test
    fun `returns empty list for 0 byte file`() {
        val chunks = calculator.calculateChunks(0L, 4)
        assertTrue(chunks.isEmpty(), "0 byte file should result in 0 chunks")
    }
    @Test
    fun `throws exception if thread count is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            calculator.calculateChunks(100L, 0)
        }
    }
}
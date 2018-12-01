package clockvapor.sourcesound

import clockvapor.sourcesound.utils.withExtension
import clockvapor.sourcesound.utils.withoutExtension
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class UtilsTest {
    @Test
    fun testFileWithExtension() {
        val file = File(Paths.get("foo", "bar", "file").toString())
        Assert.assertEquals(Paths.get("foo", "bar", "file.wav").toString(), file.withExtension(".wav").path)
    }

    @Test
    fun testFileWithExtension2() {
        val file = File(Paths.get("foo", "bar", "file").toString())
        Assert.assertEquals(Paths.get("foo", "bar", "file.wav").toString(), file.withExtension("wav").path)
    }

    @Test
    fun testFileWithExtension3() {
        val file = File(Paths.get("foo", "bar", "file.wav").toString())
        Assert.assertEquals(Paths.get("foo", "bar", "file.wav").toString(), file.withExtension(".wav").path)
    }

    @Test
    fun testFileWithExtension4() {
        val file = File(Paths.get("foo", "bar", "file.wav").toString())
        Assert.assertEquals(Paths.get("foo", "bar", "file.wav").toString(), file.withExtension("wav").path)
    }

    @Test
    fun testFileWithExtension5() {
        val file = File(Paths.get("foo", "bar", "file.ogg").toString())
        Assert.assertEquals(Paths.get("foo", "bar", "file.wav").toString(), file.withExtension(".wav").path)
    }

    @Test
    fun testFileWithExtension6() {
        val file = File(Paths.get("foo", "bar", "file.ogg").toString())
        Assert.assertEquals(Paths.get("foo", "bar", "file.wav").toString(), file.withExtension("wav").path)
    }

    @Test
    fun testStringWithoutExtension() {
        Assert.assertEquals("file", "file.wav".withoutExtension)
    }

    @Test
    fun testStringWithoutExtension2() {
        Assert.assertEquals("file", "file".withoutExtension)
    }
}
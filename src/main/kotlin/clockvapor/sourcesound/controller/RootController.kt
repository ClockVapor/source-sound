package clockvapor.sourcesound.controller

import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.view.model.RootModel
import com.github.axet.vget.VGet
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.apache.commons.io.FileUtils
import tornadofx.Controller
import tornadofx.FXTask
import tornadofx.TaskStatus
import tornadofx.get
import java.io.File
import java.net.URL
import java.nio.file.Paths

class RootController(private val model: RootModel) : Controller() {
    val taskStatus: TaskStatus = TaskStatus()

    fun convertSounds(task: FXTask<*>) {
        task.updateMessage("Converting sounds...")
        val game = model.currentGame!!
        val library = model.currentLibrary!!
        val ffmpeg = FFmpeg(model.ffmpegPath)
        val executor = FFmpegExecutor(ffmpeg)
        library.loadSounds()

        val libraryDir = File(library.directory)
        val rateDir = File(libraryDir, game.soundsRate.toString())
        rateDir.mkdirs()

        // find orphaned sounds in the destination dir and remove them (for example, if a base sound is
        // renamed to something else, delete the converted sound with the old name)
        val orphanedConvertedSounds =
            FileUtils.listFiles(rateDir, arrayOf(Sound.FILE_TYPE), true)
                .map { File(rateDir, it.toRelativeString(rateDir)) } -
                FileUtils.listFiles(library.baseDirectoryFile, arrayOf(Sound.FILE_TYPE), true)
                    .map { File(rateDir, it.toRelativeString(library.baseDirectoryFile)) }
        orphanedConvertedSounds.forEach {
            println("Deleting orphaned file ${it.path}")
            val dir = it.parentFile
            it.delete()
            if (FileUtils.listFiles(dir, null, true).isEmpty()) {
                dir.deleteRecursively()
            }
        }

        var done = 0L
        task.updateProgress(0, library.sounds.size.toLong())
        library.sounds.asSequence().forEach { sound ->
            val source = File(sound.path)
            val destination = File(rateDir, sound.relativePath)
            if (!destination.exists() || source.lastModified() > destination.lastModified()) {
                println("Converting ${sound.relativePath} into ${destination.path}")
                val destinationDir = destination.parentFile
                destinationDir.mkdirs()
                val builder = getFfmpegBuilder(game.soundsRate, sound.path, destinationDir.absolutePath)
                executor.createJob(builder).run()
            }
            done++
            task.updateProgress(done, library.sounds.size.toLong())
        }
    }

    fun importSounds(task: FXTask<*>, paths: Collection<String>, destination: String) {
        task.updateMessage("Importing sounds...")
        task.updateProgress(0, paths.size.toLong())
        var done = 0L
        paths.asSequence().forEach {
            FileUtils.copyToDirectory(File(it), File(destination))
            done++
            task.updateProgress(done, paths.size.toLong())
        }
    }

    fun importFromYouTube(task: FXTask<*>, url: String, destination: String) {
        downloadFromYouTube(url) { audioFile ->
            FileUtils.copyToDirectory(audioFile, File(destination))
        }
    }

    private fun downloadFromYouTube(url: String, op: (File) -> Unit) {
        val dir = File(YOUTUBE_DOWNLOAD_PATH).apply { mkdirs() }
        try {
            VGet(URL(url), dir).download()
            op(getDownloadedYouTubeFile())
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun getDownloadedYouTubeFile(): File {
        val files = File(YOUTUBE_DOWNLOAD_PATH).listFiles()
        return files.firstOrNull { it.extension == WEBM_EXTENSION }
            ?: files.firstOrNull { it.extension == MP4_EXTENSION }
            ?: throw RuntimeException(messages["noYouTubeFileFound"])
    }

    private fun getFfmpegBuilder(rate: Int, path: String, destination: String): FFmpegBuilder = FFmpegBuilder()
        .addInput(path)
        .addOutput(Paths.get(destination, "${File(path).nameWithoutExtension}.${Sound.FILE_TYPE}").toString())
        .apply { video_enabled = false }
        .setFormat(Sound.FILE_TYPE)
        .addExtraArgs("-flags", "bitexact", "-map_metadata", "-1")
        .setFormat(Sound.FILE_TYPE)
        .setAudioChannels(1)
        .setAudioCodec("pcm_s16le")
        .setAudioSampleRate(rate)
        .done()

    companion object {
        const val YOUTUBE_DOWNLOAD_PATH = "temp"
        const val MP4_EXTENSION = "mp4"
        const val WEBM_EXTENSION = "webm"
    }
}

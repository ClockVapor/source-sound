package clockvapor.sourcesound.controller

import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.view.model.RootModel
import com.github.axet.vget.VGet
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import tornadofx.*
import java.io.File
import java.net.URL
import java.nio.file.Paths

class RootController(private val model: RootModel) : Controller() {
    fun importSounds(paths: Collection<String>, destination: String) {
        model.currentLibrary!!.let { library ->
            val ffmpeg = FFmpeg(model.ffmpegPath)
            val executor = FFmpegExecutor(ffmpeg)
            paths
                .map {
                    FFmpegBuilder()
                        .addInput(it)
                        .addOutput(
                            Paths.get(destination,
                                "${File(it).nameWithoutExtension}.${Sound.FILE_TYPE}").toString()
                        )
                        .apply {
                            video_enabled = false
                        }
                        .setFormat(Sound.FILE_TYPE)
                        .addExtraArgs("-flags", "bitexact", "-map_metadata", "-1")
                        .setFormat(Sound.FILE_TYPE)
                        .setAudioChannels(1)
                        .setAudioCodec("pcm_s16le")
                        .setAudioSampleRate(library.rate)
                        .done()
                }
                .forEach { executor.createJob(it).run() }
        }
    }

    fun importFromYouTube(url: String, destination: String) {
        downloadFromYouTube(url) { audioFile ->
            val library = model.currentLibrary!!
            val ffmpeg = FFmpeg(model.ffmpegPath)
            val executor = FFmpegExecutor(ffmpeg)
            val builder = FFmpegBuilder()
                .addInput(audioFile.path)
                .addOutput(
                    Paths.get(destination,
                        "${audioFile.nameWithoutExtension}.${Sound.FILE_TYPE}").toString()
                )
                .apply {
                    video_enabled = false
                }
                .setFormat(Sound.FILE_TYPE)
                .addExtraArgs("-flags", "bitexact", "-map_metadata", "-1")
                .setFormat(Sound.FILE_TYPE)
                .setAudioChannels(1)
                .setAudioCodec("pcm_s16le")
                .setAudioSampleRate(library.rate)
                .done()
            executor.createJob(builder).run()
        }
    }

    private fun downloadFromYouTube(url: String, op: (File) -> Unit) {
        val dir = File(YOUTUBE_DOWNLOAD_PATH)
        dir.mkdirs()
        VGet(URL(url), dir).download()
        op(getWebMFileInTemp())
        dir.deleteRecursively()
    }

    private fun getWebMFileInTemp(): File {
        for (file in File(YOUTUBE_DOWNLOAD_PATH).listFiles()) {
            if (file.extension == WEBM_EXTENSION) {
                return file
            }
        }
        throw RuntimeException(messages["noWebmFound"])
    }

    companion object {
        const val YOUTUBE_DOWNLOAD_PATH = "temp"
        const val WEBM_EXTENSION = "webm"
    }
}

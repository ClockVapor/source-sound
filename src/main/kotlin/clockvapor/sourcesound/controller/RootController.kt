package clockvapor.sourcesound.controller

import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.view.model.RootModel
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.nio.file.Paths

class RootController(private val model: RootModel) {
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
}

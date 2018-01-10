package clockvapor.sourcesound.view

import clockvapor.sourcesound.Sound
import clockvapor.sourcesound.controller.EditSoundController
import clockvapor.sourcesound.model.EditSoundModel
import net.bramp.ffmpeg.FFmpeg
import tornadofx.*

class EditSoundView : View() {
    private val controller = EditSoundController()
    val model = EditSoundModel()

    override val root = vbox(8.0)

    init {
        title = messages["title"]
    }

    fun initialize(ffmpegPath: String, sound: Sound) {
        model.sound = sound
        controller.ffmpeg = FFmpeg(ffmpegPath)
    }

    fun dispose() {
        model.sound = null
        controller.ffmpeg = null
    }
}

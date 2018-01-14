package clockvapor.sourcesound.view

import clockvapor.sourcesound.controller.SoundEditorController
import clockvapor.sourcesound.model.Sound
import clockvapor.sourcesound.view.model.SoundEditorModel
import javafx.geometry.Pos
import tornadofx.*

class SoundEditor : View() {
    val model = SoundEditorModel()
    val controller = SoundEditorController(model)

    override val root = vbox(8.0) {
        alignment = Pos.CENTER
        label("TODO")
    }

    init {
        title = messages["title"]
    }

    fun initialize(ffmpegPath: String, sound: Sound) {
        model.ffmpegPath = ffmpegPath
        model.sound = sound
    }

    fun dispose() {
        model.ffmpegPath = null
        model.sound = null
    }
}

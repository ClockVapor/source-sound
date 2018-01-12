package clockvapor.sourcesound.model

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.io.File

data class Sound(val soundsPath: String, val path: String) {
    constructor(soundsPath: String, file: File) : this(soundsPath, file.absolutePath)

    val relativePath: String = File(path).toRelativeString(File(soundsPath).absoluteFile)

    companion object {
        const val FILE_TYPE = "wav"
        val importableExtensions: ObservableList<String> = FXCollections.observableArrayList("*.wav", "*.mp3", "*.ogg",
            "*.wma")
        val rates: ObservableList<Int> = FXCollections.observableArrayList(11025, 22050)
    }
}

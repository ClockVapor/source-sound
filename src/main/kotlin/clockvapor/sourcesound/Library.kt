package clockvapor.sourcesound

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.io.File

class Library(val soundsPath: String, val soundsRate: Int, val cfgPath: String) {
    private val sounds: ObservableList<Sound> = FXCollections.observableArrayList()

    fun createDirectory() {
        File(soundsPath).mkdirs()
    }

    companion object {
        fun fromMap(map: Map<*, *>): Library =
            Library(map["soundsPath"] as String, map["soundsRate"] as Int, map["cfgPath"] as String)
    }
}

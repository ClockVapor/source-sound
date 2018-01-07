package clockvapor.sourcesound

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.commons.io.FileUtils
import java.io.File

class Library(val soundsPath: String, val soundsRate: Int, val cfgPath: String) {
    @JsonIgnore
    val sounds: ObservableList<Sound> = FXCollections.observableArrayList()

    fun createDirectory() {
        File(soundsPath).mkdirs()
    }

    fun loadSounds() {
        sounds.clear()
        FileUtils.listFiles(File(soundsPath), arrayOf("wav"), true).mapTo(sounds) {
            Sound(soundsPath, it)
        }
    }

    fun unloadSounds() {
        sounds.clear()
    }

    companion object {
        fun fromMap(map: Map<*, *>): Library =
            Library(map["soundsPath"] as String, map["soundsRate"] as Int, map["cfgPath"] as String)
    }
}

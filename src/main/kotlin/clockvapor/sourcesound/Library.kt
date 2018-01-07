package clockvapor.sourcesound

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths

@JsonDeserialize(using = Library.Deserializer::class)
class Library(val soundsPath: String, val soundsRate: Int, val cfgPath: String) {
    @JsonIgnore
    val sounds: ObservableList<Sound> = FXCollections.observableArrayList()

    private val mainCfgPath: String = Paths.get(cfgPath, MAIN_CFG_NAME).toString()
    private val listCfgPath: String = Paths.get(cfgPath, LIST_CFG_NAME).toString()
    private val browseCfgPath: String = Paths.get(cfgPath, BROWSE_CFG_NAME).toString()

    private var currentDirectory: String? = null
    private var currentSubdirectories = arrayListOf<String>()
    private var currentSounds = arrayListOf<String>()

    fun createSoundsDirectory() {
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

    fun start(togglePlayKey: String, relayKey: String) {
        currentDirectory = soundsPath
        updateCurrentSoundsAndSubdirectories()
        createMainCfg(togglePlayKey)
        createBrowseCfg(relayKey)
        createListCfg()
    }

    fun stop() {
        deleteCfgs()
    }

    private fun updateCurrentSoundsAndSubdirectories() {
        currentSounds.clear()
        currentSubdirectories.clear()
        val currentDirectoryFile = File(currentDirectory)
        val files = currentDirectoryFile.listFiles()
        files.filter(File::isDirectory)
            .mapTo(currentSubdirectories) { it.toRelativeString(currentDirectoryFile) }
        files.filter(File::isFile)
            .mapTo(currentSounds) { it.toRelativeString(currentDirectoryFile).dropLast(4) }
    }

    private fun createMainCfg(togglePlayKey: String) {
        PrintWriter(mainCfgPath).use {
            it.println("alias $LIST_ALIAS \"exec $LIST_CFG_NAME\"")
            it.println("alias $START_ALIAS \"alias $TOGGLE_ALIAS $STOP_ALIAS; " +
                "voice_inputfromfile 1; voice_loopback 1; +voicerecord\"")
            it.println("alias $STOP_ALIAS \"alias $TOGGLE_ALIAS $START_ALIAS; " +
                "voice_inputfromfile 0; voice_loopback 0; -voicerecord\"")
            it.println("alias $TOGGLE_ALIAS $START_ALIAS")
            it.println("bind $togglePlayKey $TOGGLE_ALIAS")
        }
    }

    private fun createBrowseCfg(relayKey: String) {
        PrintWriter(browseCfgPath).use {
            if (currentDirectory != soundsPath) {
                it.println("alias 0 \"bind $relayKey 0; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: went up one level\"")
            }
            var i = 1
            for (subdirectory in currentSubdirectories) {
                it.println("alias $i \"bind $relayKey $i; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: entered $subdirectory\"")
                i++
            }
            for (sound in currentSounds) {
                it.println("alias $i \"bind $relayKey $i; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: loaded $sound\"")
                i++
            }
        }
    }

    private fun createListCfg() {
        PrintWriter(listCfgPath).use {
            if (currentDirectory != soundsPath) {
                it.println("echo 0. ${File.separator}..")
            }
            var i = 1
            for (subdirectory in currentSubdirectories) {
                it.println("echo ${i++}. ${File.separator}$subdirectory")
            }
            for (sound in currentSounds) {
                it.println("echo ${i++}. $sound")
            }
        }
    }

    private fun deleteCfgs() {
        File(mainCfgPath).delete()
        File(listCfgPath).delete()
        File(browseCfgPath).delete()
    }

    companion object {
        const val MAIN_CFG_NAME = "sourcesound.cfg"
        const val BROWSE_CFG_NAME = "sourcesound_browse.cfg"
        const val LIST_CFG_NAME = "sourcesound_list.cfg"
        const val RELAY_CFG_NAME = "sourcesound_relay.cfg"
        const val LIST_ALIAS = "la"
        const val START_ALIAS = "sourcesound_start"
        const val STOP_ALIAS = "sourcesound_stop"
        const val TOGGLE_ALIAS = "sourcesound_toggle"
    }

    class Deserializer : StdDeserializer<Library>(Library::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Library {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            val soundsPath = rootNode["soundsPath"].asText()
            val cfgPath = rootNode["cfgPath"].asText()
            val soundsRate = rootNode["soundsRate"].asInt()
            return Library(soundsPath, soundsRate, cfgPath)
        }
    }
}

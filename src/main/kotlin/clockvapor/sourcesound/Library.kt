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
    private val listAudioCfgPath: String = Paths.get(cfgPath, LIST_AUDIO_CFG_NAME).toString()

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
        createMainCfg(togglePlayKey, relayKey)
        createListAudioCfg()
    }

    fun stop() {
        deleteCfgs()
    }

    private fun createMainCfg(togglePlayKey: String, relayKey: String) {
        PrintWriter(mainCfgPath).use {
            it.println("alias $LIST_AUDIO_ALIAS \"exec $LIST_AUDIO_CFG_NAME\"")
            it.println("alias $START_ALIAS \"alias $TOGGLE_ALIAS $STOP_ALIAS; " +
                "voice_inputfromfile 1; voice_loopback 1; +voicerecord\"")
            it.println("alias $STOP_ALIAS \"alias $TOGGLE_ALIAS $START_ALIAS; " +
                "voice_inputfromfile 0; voice_loopback 0; -voicerecord\"")
            it.println("alias $TOGGLE_ALIAS $START_ALIAS")
            it.println("bind $togglePlayKey $TOGGLE_ALIAS")
            for (i in sounds.indices) {
                it.println("alias $i \"bind $relayKey $i; host_writeconfig $RELAY_CFG_NAME\"")
            }
        }
    }

    private fun createListAudioCfg() {
        PrintWriter(listAudioCfgPath).use {
            for ((i, sound) in sounds.withIndex()) {
                it.println("${i + 1}")
            }
        }
    }

    private fun deleteCfgs() {
        File(mainCfgPath).delete()
        File(listAudioCfgPath).delete()
    }

    companion object {
        const val MAIN_CFG_NAME = "sourcesound.cfg"
        const val LIST_AUDIO_CFG_NAME = "sourcesound_listaudio.cfg"
        const val RELAY_CFG_NAME = "sourcesound_relay.cfg"
        const val LIST_AUDIO_ALIAS = "la"
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

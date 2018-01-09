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
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import kotlin.concurrent.thread

@JsonDeserialize(using = Library.Deserializer::class)
class Library(var name: String, var rate: Int) {
    @JsonIgnore
    val sounds: ObservableList<Sound> = FXCollections.observableArrayList()

    private val directory get() = Paths.get("libraries", name).toString()
    private val directoryFile get() = File(directory)

    private var currentGame: Game? = null
    private var currentDirectory: String? = null
    private var currentSubdirectories = arrayListOf<String>()
    private var currentSounds = arrayListOf<String>()
    private var watchThread: Thread? = null
    private var watchService: WatchService? = null
    private var watchPath: Path? = null

    fun createSoundsDirectory() {
        directoryFile.mkdirs()
    }

    fun loadSounds() {
        sounds.clear()
        FileUtils.listFiles(directoryFile, arrayOf(Sound.FILE_TYPE), true).mapTo(sounds) {
            Sound(directory, it)
        }
    }

    fun unloadSounds() {
        sounds.clear()
    }

    fun start(game: Game, togglePlayKey: String, relayKey: String) {
        currentGame = game
        currentDirectory = directory
        updateCurrentSoundsAndSubdirectories()
        createMainCfg(togglePlayKey)
        createBrowseCfg(relayKey)
        createListCfg()
        startWatchingCfgDirectory()
    }

    fun stop() {
        stopWatchingCfgDirectory()
        deleteCfgs()
        currentGame = null
        currentSounds.clear()
        currentSubdirectories.clear()
    }

    private fun startWatchingCfgDirectory() {
        watchPath = Paths.get(currentGame!!.cfgPath)
        watchPath!!.let { path ->
            watchService = watchPath!!.fileSystem.newWatchService()
            watchService!!.let { service ->
                path.register(service, StandardWatchEventKinds.ENTRY_MODIFY)
                watchThread = thread {
                    try {
                        while (true) {
                            val key = service.take()
                            for (event in key.pollEvents()) {
                                val kind = event.kind()
                                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    if ((event.context() as Path).toString() == RELAY_CFG_NAME) {
                                        // TODO
                                    }
                                }
                            }
                            if (!key.reset()) {
                                break
                            }
                        }
                    } catch (_: InterruptedException) {
                        // nothing; exit thread gracefully
                    }
                }
            }
        }
    }

    private fun stopWatchingCfgDirectory() {
        watchThread?.let {
            it.interrupt()
            it.join()
        }
        watchThread = null
        watchService?.close()
        watchService = null
        watchPath = null
    }

    private fun updateCurrentSoundsAndSubdirectories() {
        currentSounds.clear()
        currentSubdirectories.clear()
        val currentDirectoryFile = File(currentDirectory)
        val files = currentDirectoryFile.listFiles()
        files.filter(File::isDirectory)
            .mapTo(currentSubdirectories) { it.toRelativeString(currentDirectoryFile) }
        files.filter(File::isFile)
            .mapTo(currentSounds) { it.toRelativeString(currentDirectoryFile).dropLast(Sound.FILE_TYPE.length + 1) }
    }

    private fun createMainCfg(togglePlayKey: String) {
        PrintWriter(Paths.get(currentGame!!.cfgPath, MAIN_CFG_NAME).toString()).use {
            it.println("alias $LIST_ALIAS \"exec $BROWSE_CFG_NAME; exec $LIST_CFG_NAME\"")
            it.println("alias $START_ALIAS \"alias $TOGGLE_ALIAS $STOP_ALIAS; " +
                "voice_inputfromfile 1; voice_loopback 1; +voicerecord\"")
            it.println("alias $STOP_ALIAS \"alias $TOGGLE_ALIAS $START_ALIAS; " +
                "voice_inputfromfile 0; voice_loopback 0; -voicerecord\"")
            it.println("alias $TOGGLE_ALIAS $START_ALIAS")
            it.println("bind $togglePlayKey $TOGGLE_ALIAS")
            it.println("exec $BROWSE_CFG_NAME")
        }
    }

    private fun createBrowseCfg(relayKey: String) {
        PrintWriter(getBrowseCfgPath(currentGame!!.cfgPath)).use {
            if (currentDirectory != directory) {
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
        PrintWriter(getListCfgPath(currentGame!!.cfgPath)).use {
            if (currentDirectory != directory) {
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
        currentGame?.let { game ->
            File(getMainCfgPath(game.cfgPath)).delete()
            File(getListCfgPath(game.cfgPath)).delete()
            File(getBrowseCfgPath(game.cfgPath)).delete()
            File(getRelayCfgPath(game.cfgPath)).delete()
        }
    }

    private fun getMainCfgPath(cfgPath: String): String = Paths.get(cfgPath, MAIN_CFG_NAME).toString()
    private fun getListCfgPath(cfgPath: String): String = Paths.get(cfgPath, LIST_CFG_NAME).toString()
    private fun getBrowseCfgPath(cfgPath: String): String = Paths.get(cfgPath, BROWSE_CFG_NAME).toString()
    private fun getRelayCfgPath(cfgPath: String): String = Paths.get(cfgPath, RELAY_CFG_NAME).toString()

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
            return Library(rootNode["name"].asText(), rootNode["rate"].asInt())
        }
    }
}

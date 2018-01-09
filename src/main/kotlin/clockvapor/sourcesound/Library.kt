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
import tornadofx.*
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.text.MessageFormat
import java.util.regex.Pattern
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
    private var selectionReady: Boolean = true

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

    fun start(game: Game, userdataPath: String, togglePlayKey: String, relayKey: String) {
        selectionReady = true
        currentGame = game
        currentDirectory = directory
        updateCurrentSoundsAndSubdirectories()
        createMainCfg(game, togglePlayKey)
        createBrowseCfg(game, relayKey)
        createListCfg(game)
        startWatchingCfgDirectory(game, userdataPath, relayKey)
    }

    fun stop() {
        stopWatchingCfgDirectory()
        deleteCfgs()
        currentGame = null
        currentSounds.clear()
        currentSubdirectories.clear()
    }

    private fun startWatchingCfgDirectory(game: Game, userdataPath: String, relayKey: String) {
        watchPath = Paths.get(userdataPath, game.id.toString(), "local", "cfg")
        watchPath!!.let { path ->
            watchService = watchPath!!.fileSystem.newWatchService()
            watchService!!.let { service ->
                path.register(service, StandardWatchEventKinds.ENTRY_MODIFY)
                watchThread = thread {
                    try {
                        while (true) {
                            val key = service.take()
                            for (event in key.pollEvents()) {
                                val temp = synchronized(this) { selectionReady }
                                if (temp) {
                                    val kind = event.kind()
                                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                        val modifiedFilename = (event.context() as Path).toString()
                                        if (modifiedFilename == RELAY_CFG_NAME) {
                                            synchronized(this) {
                                                selectionReady = false
                                            }
                                            startSelectionTimerThread()
                                            parseRelayCfg(
                                                game,
                                                relayKey,
                                                File(Paths.get(watchPath.toString(), modifiedFilename).toString())
                                                    .readLines()
                                            )
                                        }
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

    private fun createMainCfg(game: Game, togglePlayKey: String) {
        PrintWriter(Paths.get(game.cfgPath, MAIN_CFG_NAME).toString()).use {
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

    private fun createBrowseCfg(game: Game, relayKey: String) {
        PrintWriter(getBrowseCfgPath(game.cfgPath)).use {
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

    private fun createListCfg(game: Game) {
        PrintWriter(getListCfgPath(game.cfgPath)).use {
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

    private fun parseRelayCfg(game: Game, relayKey: String, lines: List<String>) {
        val pattern = Pattern.compile(
            "^\\s*bind \"(?:$relayKey|${relayKey.toUpperCase()}|${relayKey.toLowerCase()})\" \"(\\d+)\"\\s*$"
        )
        for (line in lines) {
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                doSelection(game, relayKey, matcher.group(1).toInt())
                break
            }
        }
    }

    private fun doSelection(game: Game, relayKey: String, i: Int) {
        when (i) {
            0 -> {
                currentDirectory = File(currentDirectory).parentFile.toRelativeString(File("."))
                updateCurrentSoundsAndSubdirectories()
                createBrowseCfg(game, relayKey)
                createListCfg(game)
                println(SourceSound.resources["upLevel"])
            }

            in 1..currentSubdirectories.size -> {
                currentDirectory = Paths.get(currentDirectory, currentSubdirectories[i - 1]).toFile()
                    .toRelativeString(File("."))
                updateCurrentSoundsAndSubdirectories()
                createBrowseCfg(game, relayKey)
                createListCfg(game)
                println(MessageFormat.format(SourceSound.resources["enteredDirectory"], currentDirectory))
            }

            else -> useSound(game, i - currentSubdirectories.size - 1)
        }
    }

    private fun useSound(game: Game, i: Int) {
        val librarySound = Paths.get(currentDirectory, "${currentSounds[i]}.${Sound.FILE_TYPE}").toFile()
        val targetSound = Paths.get(game.path, TARGET_SOUND_NAME).toFile()
        targetSound.delete()
        librarySound.copyTo(targetSound)
        println(MessageFormat.format(SourceSound.resources["loadedSound"], librarySound.path))
    }

    private fun startSelectionTimerThread() {
        thread {
            Thread.sleep(SELECTION_INTERVAL_MS)
            synchronized(this) {
                selectionReady = true
            }
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
        const val TARGET_SOUND_NAME = "voice_input.wav"
        const val SELECTION_INTERVAL_MS = 250L
    }

    class Deserializer : StdDeserializer<Library>(Library::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Library {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            return Library(rootNode["name"].asText(), rootNode["rate"].asInt())
        }
    }
}

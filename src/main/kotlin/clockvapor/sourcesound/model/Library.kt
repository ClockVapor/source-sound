package clockvapor.sourcesound.model

import clockvapor.sourcesound.SourceSound
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.commons.io.FileUtils
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import tornadofx.get
import tornadofx.getValue
import tornadofx.setValue
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.regex.Pattern
import kotlin.concurrent.thread

@JsonDeserialize(using = Library.Deserializer::class)
class Library(name: String = "") {
    @JsonIgnore
    val nameProperty: StringProperty = SimpleStringProperty(name)
    var name: String by nameProperty

    @JsonIgnore
    val sounds: ObservableList<Sound> = FXCollections.observableArrayList()

    /**
     * Root directory of the library. Example: libraries/libraryName
     */
    val directory: String
        @JsonIgnore
        get() = getDirectory(name)

    /**
     * Directory that imported, non-converted sounds are stored in. Example: libraries/libraryName/base
     */
    val baseDirectory: String
        @JsonIgnore
        get() = Paths.get(directory, "base").toString()

    val baseDirectoryFile: File
        @JsonIgnore
        get() = File(baseDirectory)

    private var currentGame: Game? = null
    private var currentDirectory: String? = null
    private var currentSubdirectories = arrayListOf<String>()
    private var currentSounds = arrayListOf<String>()
    private var monitor: FileAlterationMonitor? = null
    private var selectionReady: Boolean = true
    private var maxAliasToClear: Int? = null

    init {
        nameProperty.addListener { _, oldValue, newValue ->
            val source = File(getDirectory(oldValue))
            source.renameTo(File(getDirectory(newValue)))
        }
    }

    fun createBaseDirectory() {
        baseDirectoryFile.mkdirs()
    }

    fun loadSounds() {
        sounds.clear()
        FileUtils.listFiles(baseDirectoryFile, arrayOf(Sound.FILE_TYPE), true).mapTo(sounds) {
            Sound(baseDirectory, it)
        }
    }

    fun unloadSounds() {
        sounds.clear()
    }

    fun start(game: Game, userdataPath: String, togglePlayKey: String, relayKey: String) {
        selectionReady = true
        currentGame = game
        currentDirectory = getRateDirectory(game.soundsRate)
        updateCurrentSoundsAndSubdirectories()
        createMainCfg(game, togglePlayKey)
        createBrowseCfg(game, relayKey)
        createListCfg(game)
        startWatchingRelayCfg(game, userdataPath, relayKey)
    }

    fun stop() {
        stopWatchingRelayCfg()
        deleteCfgs()
        currentGame = null
        currentSounds.clear()
        currentSubdirectories.clear()
        maxAliasToClear = null
    }

    private fun getDirectory(name: String): String = Paths.get("libraries", name).toString()

    private fun getRateDirectory(rate: Int): String = Paths.get(directory, rate.toString()).toString()

    private fun startWatchingRelayCfg(game: Game, userdataPath: String, relayKey: String) {
        val watchPath = if (game.useUserdata) {
            Paths.get(userdataPath, game.id.toString(), "local", "cfg").toString()
        } else {
            game.cfgPath
        }
        monitor = FileAlterationMonitor(RELAY_POLL_INTERVAL_MS)
        monitor!!.let { mon ->
            val observer = FileAlterationObserver(watchPath)
            val listener = object : FileAlterationListenerAdaptor() {
                override fun onFileCreate(file: File?) {
                    if (file != null) {
                        checkFile(file)
                    }
                }

                override fun onFileChange(file: File?) {
                    if (file != null) {
                        checkFile(file)
                    }
                }

                private fun checkFile(file: File) {
                    val temp = synchronized(this) { selectionReady }
                    if (temp && file.name == RELAY_CFG_NAME) {
                        synchronized(this) { selectionReady = false }
                        startSelectionTimerThread()
                        parseRelayCfg(
                            game,
                            relayKey,
                            Paths.get(watchPath, file.name).toFile().readLines()
                        )
                    }
                }
            }

            observer.addListener(listener)
            mon.addObserver(observer)
            mon.start()
        }
    }

    private fun stopWatchingRelayCfg() {
        monitor?.stop()
        monitor = null
    }

    private fun updateCurrentSoundsAndSubdirectories() {
        currentSounds.clear()
        currentSubdirectories.clear()
        val currentDirectoryFile = File(currentDirectory)
        val files = currentDirectoryFile.listFiles()
        files.asSequence().filter(File::isDirectory).mapTo(currentSubdirectories) {
            it.toRelativeString(currentDirectoryFile)
        }
        files.asSequence().filter(File::isFile).mapTo(currentSounds) {
            it.toRelativeString(currentDirectoryFile).dropLast(Sound.FILE_TYPE.length + 1)
        }
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
        PrintWriter(getBrowseCfgPath(game.cfgPath)).use { writer ->
            // remove previous aliases
            maxAliasToClear?.let {
                for (i in 0..it) {
                    writer.println("alias $i")
                }
            }
            if (currentDirectory != getRateDirectory(game.soundsRate)) {
                writer.println("alias 0 \"bind $relayKey 0; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: went up one level\"")
            }
            var i = 1
            for (subdirectory in currentSubdirectories) {
                writer.println("alias $i \"bind $relayKey $i; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: entered $subdirectory\"")
                i++
            }
            for (sound in currentSounds) {
                writer.println("alias $i \"bind $relayKey $i; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: loaded $sound\"")
                i++
            }
            maxAliasToClear = i
        }
    }

    private fun createListCfg(game: Game) {
        PrintWriter(getListCfgPath(game.cfgPath)).use {
            if (currentDirectory != getRateDirectory(game.soundsRate)) {
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
                if (currentDirectory != getRateDirectory(game.soundsRate)) {
                    currentDirectory = File(currentDirectory).parentFile.toRelativeString(File("."))
                    updateCurrentSoundsAndSubdirectories()
                    createBrowseCfg(game, relayKey)
                    createListCfg(game)
                    println(SourceSound.resources["upLevel"])
                }
            }

            in 1..currentSubdirectories.size -> {
                currentDirectory = Paths.get(currentDirectory, currentSubdirectories[i - 1]).toFile()
                    .toRelativeString(File("."))
                updateCurrentSoundsAndSubdirectories()
                createBrowseCfg(game, relayKey)
                createListCfg(game)
                println(MessageFormat.format(SourceSound.resources["enteredDirectory"], currentDirectory))
            }

            in currentSubdirectories.size + 1..currentSubdirectories.size + currentSounds.size ->
                useSound(game, i - currentSubdirectories.size - 1)
        }
    }

    private fun useSound(game: Game, i: Int) {
        useSound(game, Paths.get(currentDirectory, "${currentSounds[i]}.${Sound.FILE_TYPE}").toFile())
    }

    private fun useSound(game: Game, soundFile: File) {
        val targetSound = Paths.get(game.path, TARGET_SOUND_NAME).toFile()
        targetSound.delete()
        soundFile.copyTo(targetSound)
        println(MessageFormat.format(SourceSound.resources["loadedSound"], soundFile.path))
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
        const val RELAY_POLL_INTERVAL_MS = 250L
    }

    class Deserializer : StdDeserializer<Library>(Library::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Library {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            return Library(rootNode["name"].asText())
        }
    }
}

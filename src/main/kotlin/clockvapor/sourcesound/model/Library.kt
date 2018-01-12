package clockvapor.sourcesound.model

import clockvapor.sourcesound.SourceSound
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import javafx.beans.property.IntegerProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.commons.io.FileUtils
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import tornadofx.*
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.regex.Pattern
import kotlin.concurrent.thread

@JsonDeserialize(using = Library.Deserializer::class)
class Library(name: String = "", rate: Int = Sound.rates[0]) {
    val nameProperty: Property<String> = SimpleStringProperty(name)
    var name: String by nameProperty

    val rateProperty: IntegerProperty = SimpleIntegerProperty(rate)
    var rate: Int by rateProperty

    @JsonIgnore
    val sounds: ObservableList<Sound> = FXCollections.observableArrayList()

    val directory get() = Paths.get("libraries", name).toString()
    val directoryFile get() = File(directory)

    private var currentGame: Game? = null
    private var currentDirectory: String? = null
    private var currentSubdirectories = arrayListOf<String>()
    private var currentSounds = arrayListOf<String>()
    private var monitor: FileAlterationMonitor? = null
    private var selectionReady: Boolean = true
    private var maxAliasToClear: Int? = null

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
        createBrowseFlatCfg(game, relayKey)
        createListCfg(game)
        createListFlatCfg(game)
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
        files.filter(File::isDirectory)
            .mapTo(currentSubdirectories) { it.toRelativeString(currentDirectoryFile) }
        files.filter(File::isFile)
            .mapTo(currentSounds) {
                it.toRelativeString(currentDirectoryFile).dropLast(
                    Sound.FILE_TYPE.length + 1)
            }
    }

    private fun createMainCfg(game: Game, togglePlayKey: String) {
        PrintWriter(Paths.get(game.cfgPath, MAIN_CFG_NAME).toString()).use {
            it.println("alias ${LIST_ALIAS} \"exec ${BROWSE_CFG_NAME}; exec ${LIST_CFG_NAME}\"")
            it.println("alias ${LIST_FLAT_ALIAS} \"exec ${LIST_FLAT_CFG_NAME}\"")
            it.println("alias ${START_ALIAS} \"alias ${TOGGLE_ALIAS} ${STOP_ALIAS}; " +
                "voice_inputfromfile 1; voice_loopback 1; +voicerecord\"")
            it.println("alias ${STOP_ALIAS} \"alias ${TOGGLE_ALIAS} ${START_ALIAS}; " +
                "voice_inputfromfile 0; voice_loopback 0; -voicerecord\"")
            it.println("alias ${TOGGLE_ALIAS} ${START_ALIAS}")
            it.println("bind $togglePlayKey ${TOGGLE_ALIAS}")
            it.println("exec ${BROWSE_CFG_NAME}")
            it.println("exec ${BROWSE_FLAT_CFG_NAME}")
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
            if (currentDirectory != directory) {
                writer.println("alias 0 \"bind $relayKey 0; host_writeconfig ${RELAY_CFG_NAME}; " +
                    "echo ${SourceSound.TITLE}: went up one level\"")
            }
            var i = 1
            for (subdirectory in currentSubdirectories) {
                writer.println("alias $i \"bind $relayKey $i; host_writeconfig ${RELAY_CFG_NAME}; " +
                    "echo ${SourceSound.TITLE}: entered $subdirectory\"")
                i++
            }
            for (sound in currentSounds) {
                writer.println("alias $i \"bind $relayKey $i; host_writeconfig ${RELAY_CFG_NAME}; " +
                    "echo ${SourceSound.TITLE}: loaded $sound\"")
                i++
            }
            maxAliasToClear = i
        }
    }

    private fun createBrowseFlatCfg(game: Game, relayKey: String) {
        PrintWriter(getBrowseFlatCfgPath(game.cfgPath)).use { writer ->
            for ((i, sound) in sounds.withIndex()) {
                val i1 = i + 1
                writer.println("alias ${BROWSE_FLAT_PREFIX}$i1 \"bind $relayKey ${BROWSE_FLAT_PREFIX}$i1; " +
                    "host_writeconfig ${RELAY_CFG_NAME}; " +
                    "echo ${SourceSound.TITLE}: loaded ${sound.relativePath}\"")
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

    private fun createListFlatCfg(game: Game) {
        PrintWriter(getListFlatCfgPath(game.cfgPath)).use {
            for ((i, sound) in sounds.withIndex()) {
                it.println("echo ${BROWSE_FLAT_PREFIX}${i + 1}. ${sound.relativePath}")
            }
        }
    }

    private fun deleteCfgs() {
        currentGame?.let { game ->
            File(getMainCfgPath(game.cfgPath)).delete()
            File(getListCfgPath(game.cfgPath)).delete()
            File(getBrowseCfgPath(game.cfgPath)).delete()
            File(getRelayCfgPath(game.cfgPath)).delete()
            File(getListFlatCfgPath(game.cfgPath)).delete()
            File(getBrowseFlatCfgPath(game.cfgPath)).delete()
        }
    }

    private fun parseRelayCfg(game: Game, relayKey: String, lines: List<String>) {
        val pattern = Pattern.compile(
            "^\\s*bind \"(?:$relayKey|${relayKey.toUpperCase()}|${relayKey.toLowerCase()})\" \"" +
                "((?:${BROWSE_FLAT_PREFIX})?\\d+)\"\\s*$"
        )
        for (line in lines) {
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                var group = matcher.group(1)
                var flat = false
                if (group.startsWith(BROWSE_FLAT_PREFIX)) {
                    group = group.substring(BROWSE_FLAT_PREFIX.length)
                    flat = true
                }
                if (flat) {
                    doSelectionFlat(game, group.toInt())
                } else {
                    doSelection(game, relayKey, group.toInt())
                }
                break
            }
        }
    }

    private fun doSelection(game: Game, relayKey: String, i: Int) {
        when (i) {
            0 -> {
                if (currentDirectory != directory) {
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

    private fun doSelectionFlat(game: Game, i: Int) {
        useSoundFlat(game, i - 1)
    }

    private fun useSound(game: Game, i: Int) {
        useSound(game, Paths.get(currentDirectory, "${currentSounds[i]}.${Sound.FILE_TYPE}").toFile())
    }

    private fun useSoundFlat(game: Game, i: Int) {
        useSound(game, Paths.get(sounds[i].soundsPath, sounds[i].relativePath).toFile())
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

    private fun getMainCfgPath(cfgPath: String): String = Paths.get(cfgPath,
        MAIN_CFG_NAME).toString()

    private fun getListCfgPath(cfgPath: String): String = Paths.get(cfgPath,
        LIST_CFG_NAME).toString()

    private fun getListFlatCfgPath(cfgPath: String): String = Paths.get(cfgPath,
        LIST_FLAT_CFG_NAME).toString()

    private fun getBrowseCfgPath(cfgPath: String): String = Paths.get(cfgPath,
        BROWSE_CFG_NAME).toString()

    private fun getBrowseFlatCfgPath(cfgPath: String): String = Paths.get(cfgPath,
        BROWSE_FLAT_CFG_NAME).toString()

    private fun getRelayCfgPath(cfgPath: String): String = Paths.get(cfgPath,
        RELAY_CFG_NAME).toString()

    companion object {
        const val MAIN_CFG_NAME = "sourcesound.cfg"
        const val BROWSE_CFG_NAME = "sourcesound_browse.cfg"
        const val BROWSE_FLAT_CFG_NAME = "sourcesound_browseflat.cfg"
        const val LIST_CFG_NAME = "sourcesound_list.cfg"
        const val LIST_FLAT_CFG_NAME = "sourcesound_listflat.cfg"
        const val RELAY_CFG_NAME = "sourcesound_relay.cfg"
        const val LIST_ALIAS = "la"
        const val LIST_FLAT_ALIAS = "laf"
        const val START_ALIAS = "sourcesound_start"
        const val STOP_ALIAS = "sourcesound_stop"
        const val TOGGLE_ALIAS = "sourcesound_toggle"
        const val TARGET_SOUND_NAME = "voice_input.wav"
        const val SELECTION_INTERVAL_MS = 250L
        const val RELAY_POLL_INTERVAL_MS = 250L
        const val BROWSE_FLAT_PREFIX = "f"
    }

    class Deserializer : StdDeserializer<Library>(Library::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Library {
            val rootNode = parser.codec.readTree<JsonNode>(parser)
            return Library(rootNode["name"].asText(), rootNode["rate"].asInt())
        }
    }
}

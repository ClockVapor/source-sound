package clockvapor.sourcesound.model

import clockvapor.sourcesound.SourceSound
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.*
import java.util.regex.Pattern
import kotlin.concurrent.thread

@JsonDeserialize(using = Library.Deserializer::class)
class Library(name: String = "", val keywords: MutableMap<String, String> = hashMapOf()) {
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

    @JsonIgnore
    val soundKeywords: MutableMap<String, Sound> = hashMapOf()

    private var currentGame: Game? = null
    private var currentDirectory: String? = null
    private var currentSubdirectories: MutableList<String> = arrayListOf()
    private var currentSounds: MutableList<String> = arrayListOf()
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
        FileUtils.listFiles(baseDirectoryFile, Sound.importableExtensions.map { it.drop(2) }.toTypedArray(), true)
            .mapTo(sounds) { Sound(baseDirectory, it) }
        associateKeywordsToSounds()
    }

    private fun associateKeywordsToSounds() {
        val iterator = keywords.iterator()
        while (iterator.hasNext()) {
            val (keyword, soundRelativePath) = iterator.next()
            val sound = sounds.find { it.relativePath == soundRelativePath }
            if (sound == null) {
                iterator.remove()
            } else {
                soundKeywords[keyword] = sound
                sound.keyword = keyword
            }
        }
    }

    fun unloadSounds() {
        sounds.clear()
        soundKeywords.clear()
    }

    fun start(game: Game, userdataPath: String, togglePlayKey: String, relayKey: String) {
        selectionReady = true
        currentGame = game
        currentDirectory = getRateDirectory(game.soundsRate)
        updateCurrentSoundsAndSubdirectories()
        createMainCfg(game, togglePlayKey)
        createBrowseCfg(game, relayKey)
        createListCfg(game)
        createListKeywordsCfg(game)
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
        PrintWriter(Paths.get(game.cfgPath, MAIN_CFG_NAME).toString()).use { writer ->
            writer.println("alias $LIST_ALIAS \"exec $BROWSE_CFG_NAME; exec $LIST_CFG_NAME\"")
            writer.println("alias $LIST_KEYWORDS_ALIAS \"exec $LIST_KEYWORDS_CFG_NAME\"")
            writer.println("alias $START_ALIAS \"alias $TOGGLE_ALIAS $STOP_ALIAS; " +
                "voice_inputfromfile 1; voice_loopback 1; +voicerecord\"")
            writer.println("alias $STOP_ALIAS \"alias $TOGGLE_ALIAS $START_ALIAS; " +
                "voice_inputfromfile 0; voice_loopback 0; -voicerecord\"")
            writer.println("alias $TOGGLE_ALIAS $START_ALIAS")
            writer.println("bind $togglePlayKey $TOGGLE_ALIAS")
            writer.println("exec $BROWSE_CFG_NAME")
            writer.println("echo ==================================================================================" +
                "=================")
            writer.println("echo Welcome to SourceSound! Use the following console commands to browse and select " +
                "your audio tracks:")
            writer.println("echo $LIST_ALIAS: List audio in current directory. Type a number from the list to " +
                "select it!")
            writer.println("echo $LIST_KEYWORDS_ALIAS: List audio keywords. Type one of these to immediately select " +
                " its associated sound!")
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
            for ((keyword, sound) in soundKeywords) {
                writer.println("alias $keyword \"bind $relayKey $keyword; host_writeconfig $RELAY_CFG_NAME; " +
                    "echo ${SourceSound.TITLE}: loaded ${sound.relativePath}\"")
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
        PrintWriter(getListCfgPath(game.cfgPath)).use { writer ->
            if (currentDirectory != getRateDirectory(game.soundsRate)) {
                writer.println("echo 0. ${File.separator}..")
            }
            var i = 1
            for (subdirectory in currentSubdirectories) {
                writer.println("echo ${i++}. ${File.separator}$subdirectory")
            }
            for (soundName in currentSounds) {
                val relativePath =
                    File(currentDirectory, soundName).toRelativeString(File(getRateDirectory(game.soundsRate)))
                val sound = sounds.find { it.relativePath.replaceAfterLast('.', "").dropLast(1) == relativePath }
                val keyword = sound?.keyword
                if (keyword == null) {
                    writer.println("echo ${i++}. $soundName")
                } else {
                    writer.println("echo ${i++}. $soundName ($keyword)")
                }
            }
        }
    }

    private fun createListKeywordsCfg(game: Game) {
        PrintWriter(getListKeywordsCfgPath(game.cfgPath)).use { writer ->
            for ((keyword, sound) in soundKeywords) {
                writer.println("echo $keyword: ${sound.relativePath}")
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
            "^\\s*bind \"(?:$relayKey|${relayKey.toUpperCase()}|${relayKey.toLowerCase()})\" \"([^\"]+)\"\\s*$"
        )
        for (line in lines) {
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                doSelection(game, relayKey, matcher.group(1))
                break
            }
        }
    }

    private fun doSelection(game: Game, relayKey: String, selection: String) {
        val i = selection.toIntOrNull()
        if (i != null) {
            doSelection(game, relayKey, i)
        } else {
            val sound = soundKeywords[selection.trim().toLowerCase(Locale.ENGLISH)]
            if (sound != null) {
                useSound(game, File(sound.soundsPath, sound.relativePath))
            } else {
                println(MessageFormat.format(SourceSound.resources["keywordNotFound"], selection))
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

    private fun getListKeywordsCfgPath(cfgPath: String): String = Paths.get(cfgPath, LIST_KEYWORDS_CFG_NAME).toString()

    private fun getBrowseCfgPath(cfgPath: String): String = Paths.get(cfgPath, BROWSE_CFG_NAME).toString()

    private fun getRelayCfgPath(cfgPath: String): String = Paths.get(cfgPath, RELAY_CFG_NAME).toString()

    companion object {
        const val MAIN_CFG_NAME = "sourcesound.cfg"
        const val BROWSE_CFG_NAME = "sourcesound_browse.cfg"
        const val LIST_CFG_NAME = "sourcesound_list.cfg"
        const val LIST_KEYWORDS_CFG_NAME = "sourcesound_listkeywords.cfg"
        const val RELAY_CFG_NAME = "sourcesound_relay.cfg"
        const val LIST_ALIAS = "la"
        const val LIST_KEYWORDS_ALIAS = "lk"
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
            return Library(
                rootNode["name"].asText(),
                rootNode["keywords"]?.let {
                    ObjectMapper().convertValue(it, MutableMap::class.java) as MutableMap<String, String>
                } ?: hashMapOf()
            )
        }
    }
}

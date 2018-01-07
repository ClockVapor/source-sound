package clockvapor.sourcesound

import java.io.File

data class Sound(val soundsPath: String, val path: String) {
    constructor(soundsPath: String, file: File) : this(soundsPath, file.absolutePath)

    val relativePath: String = File(path).toRelativeString(File(soundsPath).absoluteFile)
}

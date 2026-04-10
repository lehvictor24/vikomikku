package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.extension
import tachiyomi.source.local.io.Archive.isSupported as isArchiveSupported

sealed interface Format {
    data class Directory(val file: UniFile) : Format
    data class Archive(val file: UniFile) : Format
    data class Epub(val file: UniFile) : Format

    /**
     * A Mokuro volume: [file] points to the `.mokuro` JSON file.
     * Page images live in the same directory as the `.mokuro` file.
     */
    data class Mokuro(val file: UniFile) : Format

    class UnknownFormatException : Exception()

    companion object {

        fun valueOf(file: UniFile) = when {
            file.extension.equals("mokuro", true) -> Mokuro(file)
            file.isDirectory -> Directory(file)
            file.extension.equals("epub", true) -> Epub(file)
            isArchiveSupported(file) -> Archive(file)
            else -> throw UnknownFormatException()
        }
    }
}

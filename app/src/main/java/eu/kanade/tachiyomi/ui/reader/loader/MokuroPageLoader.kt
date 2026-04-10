package eu.kanade.tachiyomi.ui.reader.loader

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.MokuroBlock
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.system.ImageUtil

/**
 * Loader for Mokuro-processed manga volumes.
 *
 * A Mokuro chapter is represented by a single `.mokuro` JSON file that lives in the same directory
 * as the manga's page images. This loader parses that file, resolves each page's image, and
 * attaches the OCR block data to every [ReaderPage] so the reader can show the text overlay.
 */
internal class MokuroPageLoader(private val mokuroFile: UniFile) : PageLoader() {

    override var isLocal: Boolean = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getPages(): List<ReaderPage> {
        val imageDir = mokuroFile.parentFile ?: return emptyList()

        val mokuroData = mokuroFile.openInputStream().use { stream ->
            json.decodeFromString<MokuroJson>(stream.readBytes().decodeToString())
        }

        // Build a name→file map for fast lookup; fall back to sorted-by-name ordering
        val imageMap = buildImageMap(imageDir)

        val defaultWidth = mokuroData.imgWidth ?: 844
        val defaultHeight = mokuroData.imgHeight ?: 1200

        return mokuroData.pages.mapIndexed { index, pageData ->
            val imgWidth = pageData.imgWidth ?: defaultWidth
            val imgHeight = pageData.imgHeight ?: defaultHeight

            val imageFile = resolveImage(pageData.imgPath, imageMap, imageDir)

            ReaderPage(index).apply {
                if (imageFile != null) {
                    stream = { imageFile.openInputStream() }
                    status = Page.State.Ready
                }
                mokuroBlocks = pageData.blocks.map { block ->
                    MokuroBlock(
                        box = block.box.map { it.toFloat() }.toFloatArray(),
                        vertical = block.vertical ?: true,
                        lines = block.lines,
                    )
                }
                mokuroImageWidth = imgWidth
                mokuroImageHeight = imgHeight
            }
        }
    }

    /**
     * Builds a filename→UniFile map for all image files in [dir].
     * Includes both original-case and lower-case keys for case-insensitive lookup.
     */
    private fun buildImageMap(dir: UniFile): Map<String, UniFile> {
        val map = mutableMapOf<String, UniFile>()
        dir.listFiles()
            ?.filter { !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() } }
            ?.forEach { file ->
                val name = file.name ?: return@forEach
                map[name] = file
                map[name.lowercase()] = file
            }
        return map
    }

    /**
     * Resolves [imgPath] (from the .mokuro JSON) to a [UniFile].
     * Tries: exact filename, lowercase filename, then falls back to sorted-by-name order.
     */
    private fun resolveImage(
        imgPath: String?,
        imageMap: Map<String, UniFile>,
        imageDir: UniFile,
    ): UniFile? {
        if (imgPath == null) return null
        val basename = imgPath.replace('\\', '/').substringAfterLast('/')
        return imageMap[basename]
            ?: imageMap[basename.lowercase()]
            ?: imageDir.findFile(basename)
    }

    // ── Mokuro JSON models ────────────────────────────────────────────────────

    @Serializable
    private data class MokuroJson(
        val title: String? = null,
        val volume: String? = null,
        @SerialName("volume_uuid") val volumeUuid: String? = null,
        @SerialName("img_width") val imgWidth: Int? = null,
        @SerialName("img_height") val imgHeight: Int? = null,
        val pages: List<MokuroPage> = emptyList(),
    )

    @Serializable
    private data class MokuroPage(
        @SerialName("img_path") val imgPath: String? = null,
        @SerialName("img") val imgAlt: String? = null,
        @SerialName("img_width") val imgWidth: Int? = null,
        @SerialName("img_height") val imgHeight: Int? = null,
        val blocks: List<MokuroBlockJson> = emptyList(),
    )

    @Serializable
    private data class MokuroBlockJson(
        val box: List<Double> = emptyList(),
        val vertical: Boolean? = null,
        val lines: List<String> = emptyList(),
    )
}

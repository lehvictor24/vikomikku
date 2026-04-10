package eu.kanade.tachiyomi.ui.reader.model

/**
 * A single OCR text block from a Mokuro-processed manga page.
 *
 * @param box Bounding box in original image coordinates: [x1, y1, x2, y2].
 * @param vertical Whether the text runs top-to-bottom (vertical Japanese) or left-to-right.
 * @param lines Individual text lines within the block (from Mokuro OCR output).
 */
data class MokuroBlock(
    val box: FloatArray,
    val vertical: Boolean,
    val lines: List<String>,
) {
    /** Full text of this block, all lines joined. */
    val text: String get() = lines.joinToString("")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MokuroBlock) return false
        return box.contentEquals(other.box) && vertical == other.vertical && lines == other.lines
    }

    override fun hashCode(): Int {
        var result = box.contentHashCode()
        result = 31 * result + vertical.hashCode()
        result = 31 * result + lines.hashCode()
        return result
    }
}

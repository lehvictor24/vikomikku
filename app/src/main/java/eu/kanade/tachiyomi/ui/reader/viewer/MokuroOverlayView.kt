package eu.kanade.tachiyomi.ui.reader.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.ui.reader.model.MokuroBlock

/**
 * A transparent overlay placed on top of a manga page image.
 *
 * It draws invisible hit areas for each Mokuro OCR text block. Tapping inside a block first
 * reveals its text (by darkening the background slightly); tapping the same block a second time
 * — or tapping a word within it — invokes [onWordTapped] with the block and the raw text of the
 * tapped word segment.
 *
 * Block positions are given in *original image coordinates* and are scaled to match whatever
 * dimensions the image is currently drawn at inside the parent [ReaderPageImageView].
 */
@SuppressLint("ViewConstructor")
class MokuroOverlayView(context: Context) : View(context) {

    /** Called when the user taps on a word token within a block. */
    var onWordTapped: ((block: MokuroBlock, word: String) -> Unit)? = null

    private var blocks: List<MokuroBlock> = emptyList()
    private var imgW: Int = 1
    private var imgH: Int = 1

    /** Indices of currently "revealed" blocks (text visible). */
    private val revealedBlocks = mutableSetOf<Int>()

    private val revealPaint = Paint().apply {
        color = Color.argb(120, 10, 17, 30)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 14f * resources.displayMetrics.density
        isAntiAlias = true
        setShadowLayer(2f, 0f, 1f, Color.BLACK)
    }

    /**
     * Provide OCR data for the current page.
     * [imageWidth] / [imageHeight] are the original image dimensions from the .mokuro file.
     */
    fun setBlocks(blocks: List<MokuroBlock>, imageWidth: Int, imageHeight: Int) {
        this.blocks = blocks
        this.imgW = imageWidth.coerceAtLeast(1)
        this.imgH = imageHeight.coerceAtLeast(1)
        revealedBlocks.clear()
        invalidate()
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    /**
     * Returns the pixel offset + scale of the image within this view, assuming CENTER_INSIDE
     * scaling (same as SubsamplingScaleImageView default).
     */
    private fun imageTransform(): FloatArray {
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val scale = minOf(vw / imgW, vh / imgH)
        val left = (vw - imgW * scale) / 2f
        val top = (vh - imgH * scale) / 2f
        return floatArrayOf(scale, left, top)
    }

    /** Maps a box coordinate [v] (in original image space) to view pixels. */
    private fun mapX(v: Float, scale: Float, left: Float) = left + v * scale
    private fun mapY(v: Float, scale: Float, top: Float) = top + v * scale

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (blocks.isEmpty()) return
        val (scale, left, top) = imageTransform()

        blocks.forEachIndexed { idx, block ->
            if (idx !in revealedBlocks) return@forEachIndexed
            val (x1, y1, x2, y2) = block.box.let {
                if (it.size < 4) return@forEachIndexed
                floatArrayOf(it[0], it[1], it[2], it[3])
            }
            val vx1 = mapX(x1, scale, left)
            val vy1 = mapY(y1, scale, top)
            val vx2 = mapX(x2, scale, left)
            val vy2 = mapY(y2, scale, top)

            canvas.drawRect(vx1, vy1, vx2, vy2, revealPaint)

            // Draw text inside the block
            val blockText = block.text
            if (blockText.isNotEmpty()) {
                val maxFontSize = (minOf(vx2 - vx1, vy2 - vy1) / blockText.length.coerceAtLeast(1))
                    .coerceIn(8f * resources.displayMetrics.density, textPaint.textSize)
                textPaint.textSize = maxFontSize
                canvas.save()
                if (block.vertical) {
                    canvas.rotate(90f, (vx1 + vx2) / 2f, (vy1 + vy2) / 2f)
                }
                canvas.drawText(
                    blockText,
                    vx1 + 4f,
                    vy2 - 4f,
                    textPaint,
                )
                canvas.restore()
            }
        }
    }

    // ── Touch ─────────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return false
        val tx = event.x
        val ty = event.y
        val (scale, left, top) = imageTransform()

        val hitIdx = blocks.indexOfFirst { block ->
            if (block.box.size < 4) return@indexOfFirst false
            val vx1 = mapX(block.box[0], scale, left)
            val vy1 = mapY(block.box[1], scale, top)
            val vx2 = mapX(block.box[2], scale, left)
            val vy2 = mapY(block.box[3], scale, top)
            tx in vx1..vx2 && ty in vy1..vy2
        }

        if (hitIdx < 0) return false

        val block = blocks[hitIdx]
        if (hitIdx !in revealedBlocks) {
            // First tap: reveal the block
            revealedBlocks.add(hitIdx)
            invalidate()
        } else {
            // Second tap (on already-revealed block): fire dictionary lookup for the full block text
            onWordTapped?.invoke(block, block.text)
        }
        return true
    }
}

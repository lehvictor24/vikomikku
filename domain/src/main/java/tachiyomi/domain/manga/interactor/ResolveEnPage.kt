package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.SyncAnchor

/**
 * Port of the `resolveEnPage()` JavaScript function from mokuro-reader.html.
 *
 * Given a list of sync anchors (jp→en page mappings) and a JP page number,
 * returns the corresponding EN page number via linear interpolation between
 * the nearest surrounding anchor points.
 */
object ResolveEnPage {

    fun resolve(jpPage: Int, anchors: List<SyncAnchor>): Int {
        if (anchors.isEmpty()) return jpPage

        val sorted = anchors.sortedBy { it.jpPage }

        // Before first anchor: shift by the offset of the first anchor
        if (jpPage <= sorted.first().jpPage) {
            val offset = sorted.first().enPage - sorted.first().jpPage
            return (jpPage + offset).coerceAtLeast(1)
        }

        // After last anchor: shift by the offset of the last anchor
        if (jpPage >= sorted.last().jpPage) {
            val offset = sorted.last().enPage - sorted.last().jpPage
            return (jpPage + offset).coerceAtLeast(1)
        }

        // Find surrounding anchors and interpolate
        val before = sorted.last { it.jpPage <= jpPage }
        val after = sorted.first { it.jpPage > jpPage }

        val ratio = (jpPage - before.jpPage).toDouble() / (after.jpPage - before.jpPage)
        return (before.enPage + ratio * (after.enPage - before.enPage)).toInt().coerceAtLeast(1)
    }
}

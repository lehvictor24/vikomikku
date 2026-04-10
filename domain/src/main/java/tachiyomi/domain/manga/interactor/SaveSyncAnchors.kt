package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.SyncAnchor
import tachiyomi.domain.manga.repository.SyncAnchorRepository

class SaveSyncAnchors(private val repository: SyncAnchorRepository) {

    /** Replace all anchors for [mangaId] with [anchors]. */
    suspend fun await(mangaId: Long, anchors: List<SyncAnchor>) {
        repository.deleteAnchorsByMangaId(mangaId)
        anchors.forEach { repository.insertAnchor(mangaId, it.jpPage, it.enPage) }
    }
}

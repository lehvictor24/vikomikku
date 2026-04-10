package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.SyncAnchor
import tachiyomi.domain.manga.repository.SyncAnchorRepository

class GetSyncAnchors(private val repository: SyncAnchorRepository) {
    suspend fun await(mangaId: Long): List<SyncAnchor> = repository.getAnchorsByMangaId(mangaId)
}

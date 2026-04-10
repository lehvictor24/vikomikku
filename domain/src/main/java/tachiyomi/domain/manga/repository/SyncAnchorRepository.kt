package tachiyomi.domain.manga.repository

import tachiyomi.domain.manga.model.SyncAnchor

interface SyncAnchorRepository {
    suspend fun getAnchorsByMangaId(mangaId: Long): List<SyncAnchor>
    suspend fun insertAnchor(mangaId: Long, jpPage: Int, enPage: Int)
    suspend fun deleteAnchorsByMangaId(mangaId: Long)
    suspend fun deleteAnchor(mangaId: Long, jpPage: Int)
}

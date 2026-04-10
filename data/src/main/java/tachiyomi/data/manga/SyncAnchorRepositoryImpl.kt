package tachiyomi.data.manga

import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.model.SyncAnchor
import tachiyomi.domain.manga.repository.SyncAnchorRepository

class SyncAnchorRepositoryImpl(
    private val handler: DatabaseHandler,
) : SyncAnchorRepository {

    override suspend fun getAnchorsByMangaId(mangaId: Long): List<SyncAnchor> {
        return handler.awaitList {
            sync_anchorsQueries.getAnchorsByMangaId(mangaId) { mId, jpPage, enPage ->
                SyncAnchor(mId, jpPage.toInt(), enPage.toInt())
            }
        }
    }

    override suspend fun insertAnchor(mangaId: Long, jpPage: Int, enPage: Int) {
        handler.await {
            sync_anchorsQueries.insertAnchor(
                mangaId = mangaId,
                jpPage = jpPage.toLong(),
                enPage = enPage.toLong(),
            )
        }
    }

    override suspend fun deleteAnchorsByMangaId(mangaId: Long) {
        handler.await {
            sync_anchorsQueries.deleteAnchorsByMangaId(mangaId)
        }
    }

    override suspend fun deleteAnchor(mangaId: Long, jpPage: Int) {
        handler.await {
            sync_anchorsQueries.deleteAnchor(mangaId, jpPage.toLong())
        }
    }
}

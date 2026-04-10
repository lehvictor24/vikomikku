package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class PairEnVolume(private val mangaRepository: MangaRepository) {

    /** Pair [jpMangaId] with [enMangaId]. Pass null to unpair. */
    suspend fun await(jpMangaId: Long, enMangaId: Long?): Boolean {
        return mangaRepository.update(
            MangaUpdate(id = jpMangaId, pairedEnMangaId = enMangaId),
        )
    }
}

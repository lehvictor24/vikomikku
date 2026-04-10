package tachiyomi.domain.manga.model

data class SyncAnchor(
    val mangaId: Long,
    val jpPage: Int,
    val enPage: Int,
)

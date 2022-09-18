package it.vfsfitvnm.vimusic.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "Album")
data class Album(
    @PrimaryKey val id: String,
    val title: String?,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null,
    val timestamp: Long?,
    val numberItems: String?,
    val length: String?,
)

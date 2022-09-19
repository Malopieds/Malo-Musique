package it.vfsfitvnm.vimusic.models

class ArtistInfo (
        val songs: List<DetailedSong>,
        val albums: List<Album>?=null,
        val seeMoreSongs: String
)
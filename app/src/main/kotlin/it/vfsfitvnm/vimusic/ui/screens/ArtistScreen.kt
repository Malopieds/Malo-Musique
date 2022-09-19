package it.vfsfitvnm.vimusic.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import it.vfsfitvnm.route.RouteHandler
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwarePaddingValues
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness
import it.vfsfitvnm.vimusic.models.*
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.TopAppBar
import it.vfsfitvnm.vimusic.ui.components.themed.InHistoryMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.LoadingOrError
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.ui.styling.shimmer
import it.vfsfitvnm.vimusic.ui.views.BuiltInPlaylistItem
import it.vfsfitvnm.vimusic.ui.views.SongItem
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.thumbnail
import it.vfsfitvnm.youtubemusic.YouTube
import it.vfsfitvnm.youtubemusic.models.NavigationEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.*

@ExperimentalAnimationApi
@Composable
fun ArtistScreen(browseId: String) {
    val lazyListState = rememberLazyListState()
    val lazyHorizontalGridState = rememberLazyGridState()

    RouteHandler(listenToGlobalEmitter = true) {
        playlistRoute { browseId ->
            PlaylistScreen(
                browseId = browseId ?: "browseId cannot be null"
            )
        }
        albumRoute { browseId ->
            AlbumScreen(
                browseId = browseId ?: "browseId cannot be null"
            )
        }
        globalRoutes()

        host {
            val binder = LocalPlayerServiceBinder.current

            val (colorPalette, typography) = LocalAppearance.current

            val artistResult by remember(browseId) {
                Database.artist(browseId).map { artist ->
                    artist
                        ?.takeIf { artist.timestamp != null }
                        ?.let(Result.Companion::success)
                        ?: fetchArtist(browseId)
                }.distinctUntilChanged()
            }.collectAsState(initial = null, context = Dispatchers.IO)

            val artistInfoResult by remember {
                Database.artist(browseId).map { _ ->
                    fetchArtistSongs(browseId)
                }.distinctUntilChanged()
            }.collectAsState(initial = null, context = Dispatchers.IO)

            val songThumbnailSizePx = Dimensions.thumbnails.song.px

            val songs by remember(browseId) {
                Database.artistSongs(browseId)
            }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwarePaddingValues.current,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item {
                    TopAppBar(
                        modifier = Modifier
                            .height(52.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.chevron_back),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier
                                .clickable(onClick = pop)
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 16.dp)
                                .size(24.dp)
                        )
                    }
                }

                item {
                    artistResult?.getOrNull()?.let { artist ->
                        AsyncImage(
                            model = artist.thumbnailUrl?.thumbnail(Dimensions.thumbnails.artist.px),
                            contentDescription = null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(Dimensions.thumbnails.artist)
                        )

                        BasicText(
                            text = artist.name,
                            style = typography.l.semiBold,
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.text),
                                modifier = Modifier
                                    .clickable {
                                        binder?.playRadio(
                                            NavigationEndpoint.Endpoint.Watch(
                                                videoId = artist.shuffleVideoId,
                                                playlistId = artist.shufflePlaylistId
                                            )
                                        )

                                        query {
                                            runBlocking {
                                                fetchArtist(browseId)
                                            }
                                        }
                                    }
                                    .padding(all = 8.dp)
                                    .size(20.dp)
                            )

                            Image(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.text),
                                modifier = Modifier
                                    .clickable {
                                        binder?.playRadio(
                                            NavigationEndpoint.Endpoint.Watch(
                                                videoId = artist.radioVideoId
                                                    ?: artist.shuffleVideoId,
                                                playlistId = artist.radioPlaylistId
                                            )
                                        )

                                        query {
                                            runBlocking {
                                                fetchArtist(browseId)
                                            }
                                        }
                                    }
                                    .padding(all = 8.dp)
                                    .size(20.dp)
                            )
                        }
                    } ?: artistResult?.exceptionOrNull()?.let { throwable ->
                        LoadingOrError(
                            errorMessage = throwable.javaClass.canonicalName,
                            onRetry = {
                                query {
                                    runBlocking {
                                        Database.artist(browseId).first()?.let(Database::update)
                                    }
                                }
                            }
                        )
                    } ?: LoadingOrError()
                }

                item("artistSongs") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .background(colorPalette.background0)
                            .zIndex(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 32.dp)
                    ) {
                        BasicText(
                            text = "Songs",
                            style = typography.m.semiBold,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        )

                        BasicText(
                            text = "Show All",
                            style = typography.s.semiBold.secondary,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .clickable {
                                    playlistRoute("VL${artistInfoResult!!.seeMoreSongs}")
                                }
                        )
                    }
                }

                itemsIndexed(
                    items = artistInfoResult?.songs ?: emptyList(),
                    key = { index, _ -> index},
                    contentType = { _, song -> song },
                ) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSize = songThumbnailSizePx,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(
                                artistInfoResult?.songs!!.map(DetailedSong::asMediaItem), //TODO play it like YTM does (playlist at index)
                                index
                            )
                        },
                        menuContent = {
                            InHistoryMediaItemMenu(song = song)
                        }
                    )
                }

                item("artistAlbum") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .background(colorPalette.background0)
                            .zIndex(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 32.dp, bottom = 20.dp)
                    ) {
                        BasicText(
                            text = "Albums",
                            style = typography.m.semiBold,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                item("albums") {
                    LazyHorizontalGrid(
                        state = lazyHorizontalGridState,
                        rows = GridCells.Fixed(1),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .animateContentSize()
                            .fillMaxWidth()
                            .height(Dimensions.thumbnails.album + 60.dp)
                    ) {
                        itemsIndexed(
                            items = artistInfoResult?.albums ?: emptyList(),
                            key = { index, _ -> index},
                            contentType = { _, song -> song },
                        ) { _, album ->
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    albumRoute(album.id)
                                }) {
                                AlbumItem(
                                    album = album,
                                    thumbnailSizeDp = Dimensions.thumbnails.album)
                            }

                        }
                    }
                }


                item("songs") {
                    if (songs.isEmpty()) return@item

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .background(colorPalette.background0)
                            .zIndex(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 32.dp)
                    ) {
                        BasicText(
                            text = "Local tracks",
                            style = typography.m.semiBold,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        )

                        Image(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier
                                .clickable(enabled = songs.isNotEmpty()) {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlayFromBeginning(
                                        songs
                                            .shuffled()
                                            .map(DetailedSong::asMediaItem)
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .size(20.dp)
                        )
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id },
                    contentType = { _, song -> song },
                ) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSize = songThumbnailSizePx,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(
                                songs.map(DetailedSong::asMediaItem),
                                index
                            )
                        },
                        menuContent = {
                            InHistoryMediaItemMenu(song = song)
                        }
                    )
                }

                artistResult?.getOrNull()?.info?.let { description ->
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(colorPalette.background0)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(top = 32.dp)
                        ) {
                            BasicText(
                                text = "Information",
                                style = typography.m.semiBold,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Max)
                                    .padding(all = 8.dp)
                                    .fillMaxWidth()
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(48.dp)
                                ) {
                                    drawLine(
                                        color = colorPalette.background2,
                                        start = size.center.copy(y = 0f),
                                        end = size.center.copy(y = size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )

                                    drawCircle(
                                        color = colorPalette.background2,
                                        center = size.center.copy(y = size.height),
                                        radius = 4.dp.toPx()
                                    )
                                }

                                BasicText(
                                    text = description,
                                    style = typography.xxs.secondary.medium.copy(
                                        lineHeight = 24.sp,
                                        textAlign = TextAlign.Justify
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOrError(
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val (colorPalette) = LocalAppearance.current

    LoadingOrError(
        errorMessage = errorMessage,
        onRetry = onRetry,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .background(color = colorPalette.shimmer, shape = CircleShape)
                .size(Dimensions.thumbnails.artist)
        )

        TextPlaceholder(
            modifier = Modifier
                .alpha(0.9f)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        )

        repeat(3) {
            TextPlaceholder(
                modifier = Modifier
                    .alpha(0.8f)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

private suspend fun fetchArtist(browseId: String): Result<Artist>? {
    return YouTube.artist(browseId)
        ?.map { youtubeArtist ->
            Artist(
                id = browseId,
                name = youtubeArtist.name,
                thumbnailUrl = youtubeArtist.thumbnail?.url,
                info = youtubeArtist.description,
                shuffleVideoId = youtubeArtist.shuffleEndpoint?.videoId,
                shufflePlaylistId = youtubeArtist.shuffleEndpoint?.playlistId,
                radioVideoId = youtubeArtist.radioEndpoint?.videoId,
                radioPlaylistId = youtubeArtist.radioEndpoint?.playlistId,
                timestamp = System.currentTimeMillis()
            ).also(Database::upsert)
        }
}

private suspend fun fetchArtistSongs(browseId: String): ArtistInfo {
    val songs: MutableList<DetailedSong> = mutableListOf()
    val albums: MutableList<Album> = mutableListOf()
    var seeMoreSongs: String = ""
    YouTube.artist(browseId)
        ?.map { youtubeArtistSong ->
            seeMoreSongs = youtubeArtistSong.songs
                ?.get(0)
                ?.musicResponsiveListItemRenderer
                ?.flexColumns
                ?.get(0)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.get(0)
                ?.navigationEndpoint
                ?.watchEndpoint
                ?.playlistId.toString()
            for (item in youtubeArtistSong.songs!!){
                val e = DetailedSong(
                    id = item.musicResponsiveListItemRenderer
                        .flexColumns
                        .get(0)
                        .musicResponsiveListItemFlexColumnRenderer
                        .text
                        ?.runs
                        ?.get(0)
                        ?.navigationEndpoint
                        ?.watchEndpoint
                        ?.videoId ?: "",
                    title = item.musicResponsiveListItemRenderer
                        .flexColumns
                        .get(0)
                        .musicResponsiveListItemFlexColumnRenderer
                        .text
                        ?.runs
                        ?.get(0)
                        ?.text.toString(),
                    artistsText = youtubeArtistSong.name,
                    durationText = "",//no data about the song length is sent by the api
                    thumbnailUrl = item.musicResponsiveListItemRenderer
                        .thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.get(1)
                        ?.url,
                    albumId = item.musicResponsiveListItemRenderer
                        .flexColumns
                        .get(2)
                        .musicResponsiveListItemFlexColumnRenderer
                        .text
                        ?.runs
                        ?.get(0)
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                    artists = listOf(Info(
                        id = browseId,
                        name = youtubeArtistSong.name
                    ))
                )
            songs.add(e)
            }
            for (album in youtubeArtistSong.albums!!){
                val e = Album(
                    id = album.musicTwoRowItemRenderer
                        ?.title
                        ?.runs
                        ?.get(0)
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId!!,
                    title = album.musicTwoRowItemRenderer
                        ?.title
                        ?.runs
                        ?.get(0)
                        ?.text,
                    thumbnailUrl = album.musicTwoRowItemRenderer
                        ?.thumbnailRenderer
                        ?.musicThumbnailRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.get(1)
                        ?.url,
                    year = album.musicTwoRowItemRenderer
                        ?.subtitle
                        ?.runs
                        ?.get(2)
                        ?.text,
                    authorsText = youtubeArtistSong.name,
                    shareUrl = null,
                    timestamp = null
                )
                albums.add(e)
            }
        }
    return ArtistInfo(
        Collections.unmodifiableList(songs),
        Collections.unmodifiableList(albums),
        seeMoreSongs)
}


@Composable
fun AlbumItem(
    album: Album,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
) {
    val (_, typography) = LocalAppearance.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 30.dp)
    ) {
        AsyncImage(
            model = album.thumbnailUrl?.thumbnail(Dimensions.thumbnails.album.px),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .clip(ThumbnailRoundness.shape)
                .size(Dimensions.thumbnails.album)
        )
        Column(modifier = Modifier.size(thumbnailSizeDp)) {
                BasicText(
                    text = album.title!!,
                    style = typography.xs.semiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                album.year?.let { year ->
                    BasicText(
                        text = year,
                        style = typography.xs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}

package it.vfsfitvnm.vimusic.ui.views.player

import android.annotation.SuppressLint
import android.media.browse.MediaBrowser
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness
import it.vfsfitvnm.vimusic.service.LoginRequiredException
import it.vfsfitvnm.vimusic.service.PlayableFormatNotFoundException
import it.vfsfitvnm.vimusic.service.UnplayableException
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.rememberError
import it.vfsfitvnm.vimusic.utils.rememberMediaItemIndex
import it.vfsfitvnm.vimusic.utils.thumbnail
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@Composable
fun Thumbnail(
    isShowingLyrics: Boolean,
    onShowLyrics: (Boolean) -> Unit,
    isShowingStatsForNerds: Boolean,
    onShowStatsForNerds: (Boolean) -> Unit,
    nestedScrollConnectionProvider: () -> NestedScrollConnection,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val (thumbnailSizeDp, thumbnailSizePx) = Dimensions.thumbnails.player.song.let {
        it to (it - 64.dp).px
    }

    val mediaItemIndex by rememberMediaItemIndex(player)

    var xOffset by remember {
        mutableStateOf(0f)
    }

    val offsetAnimation by animateDpAsState(targetValue = 0.dp)

    val error by rememberError(player)

    //var xOffset = 0f

    AnimatedContent(
        targetState = mediaItemIndex,
        /*transitionSpec = {
            (slideIntoContainer(AnimatedContentScope.SlideDirection.Left, initialOffset = xOffset.toInt()) + fadeIn()).using(

            )
        },*/
        transitionSpec = {
            if (xOffset == 0f){
                val slideDirection =
                    if (targetState > initialState) AnimatedContentScope.SlideDirection.Left else AnimatedContentScope.SlideDirection.Right
                xOffset = 0f
                (slideIntoContainer(slideDirection) + fadeIn() with
                        slideOutOfContainer(slideDirection) + fadeOut()).using(
                    SizeTransform(clip = false))
            }else {
                xOffset = 0f
                ContentTransform(targetContentEnter = fadeIn(), initialContentExit = fadeOut())
            }
        },
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
    ) { currentMediaItemIndex ->
        val mediaItem = remember(currentMediaItemIndex) {
            player.getMediaItemAt(currentMediaItemIndex)
        }

        val nextMediaItem = remember(currentMediaItemIndex + 1) {
            try {
                player.getMediaItemAt(currentMediaItemIndex + 1)
            }catch (e: Exception){
                null
            }
        }

        val prevMediaItem = remember(currentMediaItemIndex - 1) {
            try {
                player.getMediaItemAt(currentMediaItemIndex - 1)
            }catch (e: Exception){
                null
            }
        }

        var currentMediaItem by remember {
            mutableStateOf(mediaItem)
        }


            Box(
                modifier = Modifier
                    .size(thumbnailSizeDp)
            ) {
                if  (prevMediaItem != null) {
                    AsyncImage(
                        model = prevMediaItem.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(ThumbnailRoundness.shape)
                            .offset(x = (-400 + xOffset).dp)
                            .fillMaxSize()
                    )
                }
                AsyncImage(
                    model = currentMediaItem.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .zIndex(0f)
                        .offset(x = xOffset.dp)
                        .absoluteOffset(x = offsetAnimation)
                        .clip(ThumbnailRoundness.shape)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    xOffset = 0f
                                },
                                onDragEnd = {
                                    if (xOffset >= 30) {
                                        currentMediaItem = prevMediaItem!!
                                        binder.player.seekToPreviousMediaItem()
                                    } else if (xOffset <= -30) {
                                        currentMediaItem = nextMediaItem!!
                                        binder.player.seekToNextMediaItem()
                                    }
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                xOffset += dragAmount / 2
                            }
                        }
                        .combinedClickable(
                            onClick = { onShowLyrics(true) },
                            onLongClick = { onShowStatsForNerds(true) }
                        )
                        .fillMaxSize()
                )
                if  (nextMediaItem != null) {
                    AsyncImage(
                        model = nextMediaItem.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = (400 + xOffset).dp)
                            .clip(ThumbnailRoundness.shape)
                            .fillMaxSize()
                    )
                }

                Lyrics(
                    mediaId = mediaItem.mediaId,
                    isDisplayed = isShowingLyrics && error == null,
                    onDismiss = { onShowLyrics(false) },
                    onLyricsUpdate = { areSynchronized, mediaId, lyrics ->
                        if (areSynchronized) {
                            if (Database.updateSynchronizedLyrics(mediaId, lyrics) == 0) {
                                if (mediaId == mediaItem.mediaId) {
                                    Database.insert(mediaItem) { song ->
                                        song.copy(synchronizedLyrics = lyrics)
                                    }
                                }
                            }
                        } else {
                            if (Database.updateLyrics(mediaId, lyrics) == 0) {
                                if (mediaId == mediaItem.mediaId) {
                                    Database.insert(mediaItem) { song ->
                                        song.copy(lyrics = lyrics)
                                    }
                                }
                            }
                        }
                    },
                    size = thumbnailSizeDp,
                    mediaMetadataProvider = mediaItem::mediaMetadata,
                    durationProvider = player::getDuration,
                    nestedScrollConnectionProvider = nestedScrollConnectionProvider,
                )

                StatsForNerds(
                    mediaId = mediaItem.mediaId,
                    isDisplayed = isShowingStatsForNerds && error == null,
                    onDismiss = { onShowStatsForNerds(false) }
                )

                PlaybackError(
                    isDisplayed = error != null,
                    messageProvider = {
                        when (error?.cause?.cause) {
                            is UnresolvedAddressException, is UnknownHostException -> "A network error has occurred"
                            is PlayableFormatNotFoundException -> "Couldn't find a playable audio format"
                            is UnplayableException -> "The original video source of this song has been deleted"
                            is LoginRequiredException -> "This song cannot be played due to server restrictions"
                            else -> "An unknown playback error has occurred"
                        }
                    },
                    onDismiss = player::prepare
                )
            }
        }
    } 

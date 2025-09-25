package github.leavesczy.matisse.internal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState
import github.leavesczy.matisse.internal.logic.MatisseMediaExtend
import github.leavesczy.matisse.internal.logic.MatissePageViewState
import github.leavesczy.matisse.internal.logic.MatisseViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * @Author: leavesCZY
 * @Date: 2022/5/31 16:36
 * @Desc:
 */
@Composable
internal fun MatissePage(
    pageViewState: MatissePageViewState,
    bottomBarViewState: MatisseBottomBarViewState,
    onRequestTakePicture: () -> Unit,
    onClickSure: () -> Unit,
    selectMediaInFastSelectMode: (MediaResource) -> Unit
) {
    var choice by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            MediaTypeTopBar(
                modifier = Modifier,
                onClickMediaType = pageViewState.onClickMediaType,
                onClickChoice = { it -> choice = it },
                onClickDelete = {}
            )
        },
        bottomBar = {
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = pageViewState.lazyGridState,
            columns = GridCells.Fixed(count = pageViewState.gridColumns),
            horizontalArrangement = Arrangement.spacedBy(space = 1.dp),
            verticalArrangement = Arrangement.spacedBy(space = 1.dp),
            contentPadding = PaddingValues(
                top = 1.dp,
                bottom = if (pageViewState.fastSelect) {
                    16.dp
                } else {
                    1.dp
                }
            )
        ) {
            if (pageViewState.selectedBucket.supportCapture) {
                item(
                    key = "CaptureItem",
                    contentType = "CaptureItem"
                ) {
                    CaptureItem(
                        modifier = Modifier
                            .matisseAnimateItem(lazyGridItemScope = this),
                        onClick = onRequestTakePicture
                    )
                }
            }
            items(
                items = pageViewState.selectedBucket.resources,
                key = {
                    it.mediaId
                },
                contentType = {
                    "MediaItem"
                }
            ) {
                if (pageViewState.fastSelect) {
                    MediaItemFastSelect(
                        modifier = Modifier
                            .matisseAnimateItem(lazyGridItemScope = this),
                        mediaResource = it.media,
                        imageEngine = pageViewState.imageEngine,
                        onClickMedia = selectMediaInFastSelectMode
                    )
                } else {
                    MediaItem(
                        modifier = Modifier
                            .matisseAnimateItem(lazyGridItemScope = this),
                        mediaResource = it,
                        imageEngine = pageViewState.imageEngine,
                        onClickMedia = pageViewState.onClickMedia,
                        onClickCheckBox = pageViewState.onMediaCheckChanged,
                        choice = choice
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureItem(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = colorResource(id = R.color.matisse_capture_item_background_color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.5f),
            imageVector = Icons.Filled.PhotoCamera,
            tint = colorResource(id = R.color.matisse_capture_item_icon_color),
            contentDescription = "Capture"
        )
    }
}

@Composable
private fun MediaItem(
    modifier: Modifier,
    mediaResource: MatisseMediaExtend,
    imageEngine: ImageEngine,
    onClickMedia: (MatisseMediaExtend) -> Unit,
    onClickCheckBox: (MatisseMediaExtend) -> Unit,
    choice: Boolean
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clickable {
                onClickMedia(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource.media)
        if (mediaResource.media.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.24f)
            )
        }
        val scrimColor by animateColorAsState(
            targetValue = if (mediaResource.selectState.value.isSelected) {
                colorResource(id = R.color.matisse_media_item_scrim_color_when_selected)
            } else {
                colorResource(id = R.color.matisse_media_item_scrim_color_when_unselected)
            }
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(color = scrimColor)
        )
        if (choice) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .fillMaxSize(fraction = 0.33f)
                    .clickableNoRipple {
                        onClickCheckBox(mediaResource)
                    },
                contentAlignment = Alignment.Center
            ) {
                MatisseCheckbox(
                    modifier = Modifier
                        .fillMaxSize(fraction = 0.68f),
                    selectState = mediaResource.selectState.value,
                    onClick = {
                        onClickCheckBox(mediaResource)
                    }
                )
            }
        }
    }
}

@Composable
private fun MediaItemFastSelect(
    modifier: Modifier,
    mediaResource: MediaResource,
    imageEngine: ImageEngine,
    onClickMedia: (MediaResource) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clickable {
                onClickMedia(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource)
        if (mediaResource.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.24f)
            )
        }
    }
}

@Composable
internal fun VideoIcon(modifier: Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = CircleShape)
            .clip(shape = CircleShape)
            .background(color = colorResource(id = R.color.matisse_video_icon_color)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(fraction = 0.62f),
            imageVector = Icons.Filled.PlayArrow,
            tint = Color.Black,
            contentDescription = null
        )
    }
}

@Stable
private fun Modifier.matisseAnimateItem(lazyGridItemScope: LazyGridItemScope): Modifier {
    return with(receiver = lazyGridItemScope) {
        animateItem(
            fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
            fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
            placementSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        )
    }
}
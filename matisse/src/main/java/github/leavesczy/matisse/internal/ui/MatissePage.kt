package github.leavesczy.matisse.internal.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseBottomBarViewState
import github.leavesczy.matisse.internal.logic.MatisseGridItem
import github.leavesczy.matisse.internal.logic.MatisseMediaExtend
import github.leavesczy.matisse.internal.logic.MatissePageViewState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showDialog by remember { mutableStateOf(false) }
    var shouldContinueDelete by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 用户授权成功（createDeleteRequest 已由系统完成删除），直接刷新列表
            pageViewState.reloadMediaResources()
        } else {
            // 用户拒绝授权，只刷新列表
            pageViewState.reloadMediaResources()
        }
    }

    // Android 10 逐个授权的场景：继续删除剩余文件
    if (shouldContinueDelete) {
        shouldContinueDelete = false
        pageViewState.continuePendingDelete(launcher)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            MediaTypeTopBar(
                modifier = Modifier,
                onClickMediaType = pageViewState.onClickMediaType,
                onClickChoice = { it -> choice = it },
                onClickDelete = { showDialog = true },
                enableSelectAll = pageViewState.enableSelectAll,
                onClickSelectAll = pageViewState.onClickSelectAll
            )
        },
        bottomBar = {
        }
    ) { innerPadding ->
        val gridItems = remember(
            pageViewState.selectedBucket.resources,
            pageViewState.showDateHeaders
        ) {
            buildGridItems(
                resources = pageViewState.selectedBucket.resources,
                showDateHeaders = pageViewState.showDateHeaders
            )
        }
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            state = pageViewState.lazyGridState,
            columns = GridCells.Fixed(count = pageViewState.gridColumns),
            horizontalArrangement = Arrangement.spacedBy(space = pageViewState.gridSpacingDp.dp),
            verticalArrangement = Arrangement.spacedBy(space = pageViewState.gridSpacingDp.dp),
            contentPadding = PaddingValues(
                top = pageViewState.gridSpacingDp.dp,
                bottom = if (pageViewState.fastSelect) {
                    16.dp
                } else {
                    pageViewState.gridSpacingDp.dp
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
                        onClick = onRequestTakePicture,
                        aspectRatio = pageViewState.gridAspectRatio
                    )
                }
            }
            items(
                items = gridItems,
                key = { item ->
                    when (item) {
                        is MatisseGridItem.DateHeader -> "header_${item.dateLabel}"
                        is MatisseGridItem.MediaEntry -> item.media.mediaId
                    }
                },
                span = { item ->
                    when (item) {
                        is MatisseGridItem.DateHeader -> GridItemSpan(maxLineSpan)
                        is MatisseGridItem.MediaEntry -> GridItemSpan(1)
                    }
                },
                contentType = { item ->
                    when (item) {
                        is MatisseGridItem.DateHeader -> "DateHeader"
                        is MatisseGridItem.MediaEntry -> "MediaItem"
                    }
                }
            ) { item ->
                when (item) {
                    is MatisseGridItem.DateHeader -> {
                        DateHeaderItem(dateLabel = item.dateLabel)
                    }

                    is MatisseGridItem.MediaEntry -> {
                        if (pageViewState.fastSelect) {
                            MediaItemFastSelect(
                                modifier = Modifier
                                    .matisseAnimateItem(lazyGridItemScope = this),
                                mediaResource = item.media.media,
                                imageEngine = pageViewState.imageEngine,
                                onClickMedia = selectMediaInFastSelectMode,
                                aspectRatio = pageViewState.gridAspectRatio
                            )
                        } else {
                            MediaItem(
                                modifier = Modifier
                                    .matisseAnimateItem(lazyGridItemScope = this),
                                mediaResource = item.media,
                                imageEngine = pageViewState.imageEngine,
                                onClickMedia = pageViewState.onClickMedia,
                                onClickCheckBox = pageViewState.onMediaCheckChanged,
                                choice = choice,
                                aspectRatio = pageViewState.gridAspectRatio
                            )
                        }
                    }
                }
            }
        }
        // Dialog overlay 在 Scaffold 之上（同一 Window，不会触发导航栏）
        if (showDialog) {
            CustomButtonDialog(
                R.string.matisse_dialog_title,
                onDismissRequest = { showDialog = false },
                onSureClick = {
                    pageViewState.onClickDelete(launcher)
                    showDialog = false
                },
                onCancelClick = { showDialog = false }
            )
        }
    }
}
}

private fun buildGridItems(
    resources: List<MatisseMediaExtend>,
    showDateHeaders: Boolean
): List<MatisseGridItem> {
    if (!showDateHeaders) {
        return resources.map { MatisseGridItem.MediaEntry(it) }
    }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val result = mutableListOf<MatisseGridItem>()
    var lastDate = ""
    for (resource in resources) {
        val dateLabel = dateFormat.format(Date(resource.media.dateModified * 1000L))
        if (dateLabel != lastDate) {
            result.add(MatisseGridItem.DateHeader(dateLabel))
            lastDate = dateLabel
        }
        result.add(MatisseGridItem.MediaEntry(resource))
    }
    return result
}

@Composable
private fun DateHeaderItem(dateLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(id = R.color.matisse_date_header_background_color))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = dateLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.matisse_date_header_text_color)
        )
    }
}

@Composable
private fun CaptureItem(
    modifier: Modifier,
    onClick: () -> Unit,
    aspectRatio: Float = 1f
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = aspectRatio)
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
    choice: Boolean,
    aspectRatio: Float = 1f
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = aspectRatio)
            .clickable {
                onClickMedia(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource.media)
        if (mediaResource.media.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.24f)
                    .aspectRatio(1f)
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
                    .fillMaxWidth(fraction = 0.165f)
                    .aspectRatio(1f)
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
    onClickMedia: (MediaResource) -> Unit,
    aspectRatio: Float = 1f
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = aspectRatio)
            .clickable {
                onClickMedia(mediaResource)
            },
        contentAlignment = Alignment.Center
    ) {
        imageEngine.Thumbnail(mediaResource = mediaResource)
        if (mediaResource.isVideo) {
            VideoIcon(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.24f)
                    .aspectRatio(1f)
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
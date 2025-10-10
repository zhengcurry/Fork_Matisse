package github.leavesczy.matisse.internal.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import github.leavesczy.matisse.ImageEngine
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatissePreviewPageViewState
import kotlin.math.absoluteValue

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:14
 * @Desc:
 */
@Composable
internal fun MatissePreviewPage(
    pageViewState: MatissePreviewPageViewState,
    imageEngine: ImageEngine,
    requestOpenVideo: (MediaResource) -> Unit,
    onClickSure: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 处理返回数据
            pageViewState.reloadMediaResources()
        }
    }
    var deleteIndex by remember { mutableIntStateOf(pageViewState.initialPage) }

    if (showDialog) {
        CustomButtonDialog(
            R.string.matisse_dialog_title,
            onDismissRequest = { showDialog = false },
            onSureClick = {
                pageViewState.deleteMediaResources(
                    pageViewState.previewResources[deleteIndex].media.uri,
                    launcher
                )
                showDialog = false
            },
            onCancelClick = { showDialog = false })
    }
    AnimatedVisibility(
        visible = pageViewState.visible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            targetOffsetX = { it }
        )
    ) {
        BackHandler(
            enabled = pageViewState.visible,
            onBack = pageViewState.onDismissRequest
        )
        val pagerState = rememberPagerState(initialPage = pageViewState.initialPage) {
            pageViewState.previewResources.size
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            contentWindowInsets = WindowInsets(
                left = 0.dp,
                right = 0.dp,
                top = 0.dp,
                bottom = 0.dp
            ),
            containerColor = colorResource(id = R.color.matisse_preview_page_background_color)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .fillMaxSize()
            ) {
                MediaPreviewTopBar(
                    modifier = Modifier,
                    { showDialog = true },
                    backEvent = pageViewState.onDismissRequest
                )
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f),
                    state = pagerState,
                    verticalAlignment = Alignment.CenterVertically,
                    key = { index ->
                        pageViewState.previewResources[index].mediaId
                    }
                ) { pageIndex ->
                    deleteIndex = pageIndex
                    PreviewPage(
                        modifier = Modifier
                            .fillMaxSize(),
                        pagerState = pagerState,
                        pageIndex = pageIndex,
                        imageEngine = imageEngine,
                        mediaResource = pageViewState.previewResources[pageIndex].media,
                        requestOpenVideo = requestOpenVideo
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPage(
    modifier: Modifier,
    pagerState: PagerState,
    pageIndex: Int,
    imageEngine: ImageEngine,
    mediaResource: MediaResource,
    requestOpenVideo: (MediaResource) -> Unit
) {
    val fraction by remember {
        derivedStateOf {
            val pageOffset =
                (pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction).absoluteValue
            val progress = 1f - pageOffset.coerceIn(0f, 1f)
            lerp(
                start = 0.80f,
                stop = 1f,
                fraction = progress
            )
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = fraction
                    scaleY = fraction
                    alpha = fraction
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth(0.3f)
                        .padding(start = 20.dp)
                ) {
                    Text(
                        modifier = Modifier,
                        text = mediaResource.name,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier,
                        text = "大小：${mediaResource.size}",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        modifier = Modifier,
                        text = "时间：${mediaResource.dateModified}",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        modifier = Modifier,
                        text = "位置：${mediaResource.path}",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (mediaResource.isVideo) {
                        val player = exoPlayer(mediaResource.uri)
                        AndroidView(
                            factory = { context: Context ->
                                player
                            },
                            modifier = Modifier
                                .clipToBounds()
                                .fillMaxSize()
                                .align(Alignment.Center),
                            update = { playerView: PlayerView ->
                            }
                        )
                    } else {
                        imageEngine.Image(
                            Modifier
                                .align(Alignment.Center), mediaResource = mediaResource
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun exoPlayer(uri: Uri): PlayerView {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        seekTo(0)
                        setPlayWhenReady(false)
                    }
                }
            })
            setMediaItem(
                androidx.media3.common.MediaItem.fromUri(
                    uri
                )
            )
            prepare()
            playWhenReady = true
        }
    }
    val playerView = PlayerView(context).apply {
        player = exoPlayer
        layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        // 清除播放器的焦点，避免找不到焦点
        setFocusable(false)
        clearFocus()
    }
    // 监听宿主生命周期
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> playerView.onResume()
                Lifecycle.Event.ON_PAUSE -> playerView.onPause()
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    return playerView
}

@Composable
private fun BottomController(
    modifier: Modifier,
    pageViewState: MatissePreviewPageViewState,
    pagerState: PagerState,
    onClickSure: () -> Unit
) {
    val currentResource by remember {
        derivedStateOf {
            pageViewState.previewResources[pagerState.currentPage]
        }
    }
    Box(
        modifier = modifier
            .background(color = colorResource(id = R.color.matisse_preview_page_bottom_navigation_bar_background_color))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(height = 56.dp)
    ) {
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterStart)
                .clip(shape = CircleShape)
                .clickable(onClick = pageViewState.onDismissRequest)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            text = stringResource(id = R.string.matisse_back),
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(id = R.color.matisse_preview_page_back_text_color)
        )
        MatisseCheckbox(
            modifier = Modifier
                .align(alignment = Alignment.Center)
                .size(size = 24.dp),
            selectState = currentResource.selectState.value,
            onClick = {
                pageViewState.onMediaCheckChanged(currentResource)
            }
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .then(
                    other = if (pageViewState.sureButtonClickable) {
                        Modifier
                            .clip(shape = CircleShape)
                            .clickable(onClick = onClickSure)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 6.dp),
            text = pageViewState.sureButtonText,
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal,
            color = colorResource(
                id = if (pageViewState.sureButtonClickable) {
                    R.color.matisse_preview_page_sure_text_color
                } else {
                    R.color.matisse_preview_page_sure_text_color_if_disable
                }
            )
        )
    }
}
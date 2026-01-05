package github.leavesczy.matisse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.parcelize.Parcelize
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * @Author: CZY
 * @Date: 2025/7/23 21:24
 * @Desc:
 */
@Parcelize
class CoilImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        CoilComposeImage(
            modifier = Modifier
                .fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaResource.uri)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Crop
        )
    }

    @Composable
    override fun Image(modifier: Modifier, mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            CoilComposeImage(
                modifier = modifier
                    .fillMaxHeight(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillHeight
            )
        } else {
            val zoomState = rememberZoomState()
            CoilComposeImage(
                modifier = modifier
                    .fillMaxHeight()
                    .clipToBounds()
                    .zoomable(zoomState),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaResource.uri)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.FillHeight
            )
        }
    }

}

@Composable
private fun CoilComposeImage(
    modifier: Modifier,
    model: Any,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    backgroundColor: Color? = colorResource(id = R.color.matisse_media_item_background_color)
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = model,
        alignment = alignment,
        contentScale = contentScale,
        contentDescription = null
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Error -> {
                if (backgroundColor != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = backgroundColor)
                    )
                }
            }

            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}
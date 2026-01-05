package github.leavesczy.matisse

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.parcelize.Parcelize
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * @Author: CZY
 * @Date: 2025/7/23 21:24
 * @Desc:
 */
@Parcelize
class GlideImageEngine : ImageEngine {

    @Composable
    override fun Thumbnail(mediaResource: MediaResource) {
        GlideComposeImage(
            modifier = Modifier
                .fillMaxSize(),
            model = mediaResource.uri,
            contentScale = ContentScale.Crop
        )
    }

    @Composable
    override fun Image(modifier: Modifier, mediaResource: MediaResource) {
        if (mediaResource.isVideo) {
            GlideComposeImage(
                modifier = modifier
                    .fillMaxHeight(),
                model = mediaResource.uri,
                contentScale = ContentScale.FillHeight
            )
        } else {
            val zoomState = rememberZoomState()
            GlideComposeImage(
                modifier = modifier
                    .fillMaxHeight()
                    .clipToBounds()
                    .zoomable(zoomState),
                model = mediaResource.uri,
                contentScale = ContentScale.FillHeight
            )
        }
    }

}

@Composable
private fun GlideComposeImage(
    modifier: Modifier,
    model: Uri,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    backgroundColor: Color? = colorResource(id = R.color.matisse_media_item_background_color)
) {
    GlideImage(
        modifier = modifier,
        model = model,
        contentScale = contentScale,
        alignment = alignment,
        loading = if (backgroundColor != null) {
            placeholder {
                Placeholder(backgroundColor = backgroundColor)
            }
        } else {
            null
        },
        failure = if (backgroundColor != null) {
            placeholder {
                Placeholder(backgroundColor = backgroundColor)
            }
        } else {
            null
        },
        contentDescription = null
    )
}

@Composable
private fun Placeholder(backgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor)
    )
}
package github.leavesczy.matisse

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * @Author: leavesCZY
 * @Date: 2023/6/7 23:11
 * @Desc:
 */
@Stable
interface ImageEngine : Parcelable {

    /**
     * 加载缩略图时调用
     */
    @Composable
    fun Thumbnail(mediaResource: MediaResource)

    /**
     * 加载大图时调用
     */
    @Composable
    fun Image(modifier: Modifier, mediaResource: MediaResource)

}
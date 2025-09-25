package github.leavesczy.matisse.samples

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder

/**
 * @Author: leavesCZY
 * @Date: 2022/5/29 21:10
 * @Desc:
 */
class MatisseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        initCoil()
    }

    private fun initCoil() {
        SingletonImageLoader.setSafe(factory = { context ->
            ImageLoader.Builder(context)
                .crossfade(false)
                .allowHardware(true)
                // 1️⃣ 内存缓存（相册场景建议稍微大一点）
//                .memoryCache {
//                    MemoryCache.Builder()
//                        .maxSizePercent(context,0.25) // 占用应用可用内存的 25%
//                        .build()
//                }
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        })
    }

}
package github.leavesczy.matisse.internal.logic

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import github.leavesczy.matisse.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @Author: leavesCZY
 * @Date: 2022/6/2 11:11
 * @Desc:
 */
internal object MediaProvider {

    data class MediaInfo(
        val mediaId: Long,
        val bucketId: String,
        val bucketName: String,
        val uri: Uri,
        val path: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val dateModified: Long
    )

    suspend fun createImage(
        context: Context,
        imageName: String,
        mimeType: String
    ): Uri? {
        return withContext(context = Dispatchers.Default) {
            try {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                context.contentResolver.insert(imageCollection, contentValues)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteMedia(context: Context, uri: Uri) {
        withContext(context = Dispatchers.Default) {
            val path = queryPath(context, uri)
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
            if (!path.isNullOrBlank()) {
                MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
            }
        }
    }

    suspend fun deleteMoreMedia(context: Context, uris: List<Uri>) {
        withContext(context = Dispatchers.Default) {
            val paths = uris.mapNotNull { queryPath(context, it) }
            for (uri in uris) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            if (paths.isNotEmpty()) {
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
            }
        }
    }

    private fun queryPath(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                } else {
                    null
                }
            }
        } catch (throwable: Throwable) {
            null
        }
    }

    /**
     * 删除媒体文件并返回已成功删除的 URI 列表
     */
    suspend fun deleteMedia(
        launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
        context: Context,
        uris: List<Uri>
    ): List<Uri> {
        Log.d("curry", "deleteMedia: attempting to delete ${uris.size} files")
        val successfullyDeleted = mutableListOf<Uri>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+): 使用 MediaStore.createDeleteRequest 批量删除
            try {
                val deleteRequest = MediaStore.createDeleteRequest(context.contentResolver, uris)
                launcher.launch(
                    IntentSenderRequest.Builder(deleteRequest).build()
                )
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29): 需要捕获 RecoverableSecurityException
            // 注意：不能在 IO 线程中调用 launcher.launch()，必须在主线程
            withContext(context = Dispatchers.IO) {
                for ((index, uri) in uris.withIndex()) {
                    try {
                        val deletedRows = context.contentResolver.delete(uri, null, null)
                        if (deletedRows > 0) {
                            Log.d("curry", "Successfully deleted: $uri")
                            successfullyDeleted.add(uri)
                        } else {
                            // 文件已经被删除或不存在，也算成功（避免重复处理）
                            Log.w("curry", "File already deleted or not found: $uri")
                            successfullyDeleted.add(uri)
                        }
                    } catch (securityException: SecurityException) {
                        // 尝试转换为 RecoverableSecurityException
                        val recoverableSecurityException =
                            securityException as? RecoverableSecurityException

                        if (recoverableSecurityException != null) {
                            // 可以恢复的安全异常，请求用户授权
                            withContext(Dispatchers.Main) {
                                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                                try {
                                    launcher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                    Log.d("curry", "Requesting permission to delete: $uri (${index + 1}/${uris.size})")
                                } catch (e: Exception) {
                                    Log.e("curry", "Failed to launch permission request", e)
                                    e.printStackTrace()
                                }
                            }
                            // 请求授权后立即返回，等待用户确认
                            // 用户授权成功后，调用方会通过 continuePendingDelete() 继续删除剩余文件
                            return@withContext
                        } else {
                            // 不可恢复的安全异常（比如文件已被其他进程删除），跳过该文件
                            Log.w("curry", "Non-recoverable security exception for $uri: ${securityException.message}")
                            successfullyDeleted.add(uri) // 标记为已处理，避免重复尝试
                        }
                    } catch (throwable: Throwable) {
                        // 忽略其他错误（例如文件已被删除、磁盘问题等）
                        Log.w("curry", "Failed to delete $uri: ${throwable.message}")
                        successfullyDeleted.add(uri) // 标记为已处理，避免重复尝试
                    }
                }
                Log.d("curry", "Batch delete completed: ${successfullyDeleted.size}/${uris.size} files processed")
            }
        } else {
            // Android 9 及以下 (API 28-): 可以直接删除
            deleteMoreMedia(context, uris)
            successfullyDeleted.addAll(uris)
        }

        return successfullyDeleted
    }

    private suspend fun loadResources(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<MediaInfo>? {
        return withContext(context = Dispatchers.Default) {
            val idColumn = MediaStore.MediaColumns._ID
            val pathColumn = MediaStore.MediaColumns.DATA
            val sizeColumn = MediaStore.MediaColumns.SIZE
            val displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME
            val mineTypeColumn = MediaStore.MediaColumns.MIME_TYPE
            val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
            val bucketDisplayNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
            val dateModifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
            val projection = arrayOf(
                idColumn,
                pathColumn,
                sizeColumn,
                displayNameColumn,
                mineTypeColumn,
                bucketIdColumn,
                bucketDisplayNameColumn,
                dateModifiedColumn
            )
            val contentUri = MediaStore.Files.getContentUri("external")
            val sortOrder = "$dateModifiedColumn DESC"
            val mediaResourceList = mutableListOf<MediaInfo>()
            try {
                val mediaCursor = context.contentResolver.query(
                    contentUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                ) ?: return@withContext null
                mediaCursor.use { cursor ->
                    while (cursor.moveToNext()) {//&& count < limit
                        val defaultId = Long.MAX_VALUE
                        val id = cursor.getLong(idColumn, defaultId)
                        val path = cursor.getString(pathColumn, "")
                        val size = cursor.getLong(sizeColumn, 0)
                        val dateModified = cursor.getLong(dateModifiedColumn, 0)
                        if (id == defaultId || size <= 0) {
                            continue
                        }
//                        val file = File(path)
//                        if (!file.isFile || !file.exists()) {
//                            continue
//                        }
                        val name = cursor.getString(displayNameColumn, "")
                        val mimeType = cursor.getString(mineTypeColumn, "")
                        val bucketId = cursor.getString(bucketIdColumn, "")
                        val bucketName = cursor.getString(bucketDisplayNameColumn, "")
                        val uri = getUri(id, mimeType)
                        mediaResourceList.add(
                            element = MediaInfo(
                                mediaId = id,
                                bucketId = bucketId,
                                bucketName = bucketName,
                                path = path,
                                uri = uri,
                                name = name,
                                mimeType = mimeType,
                                size = size,
                                dateModified = dateModified
                            )
                        )
                    }
                }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
            mediaResourceList
        }
    }

    // content://media/external/images/media/1000011822 ✔
    // content://media/external/file/1000011826 ❌
    // java.lang.IllegalArgumentException: All requested items must be Media items
    private fun getUri(id: Long, mimeType: String): Uri {
        val contentUri: Uri
        if (mimeType.startsWith("image")) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else if (mimeType.startsWith("video")) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            contentUri = MediaStore.Files.getContentUri("external")
        }

        val uri = ContentUris.withAppendedId(contentUri, id)
        return uri
    }

    suspend fun loadResources(
        context: Context,
        mediaType: MediaType
    ): List<MediaInfo>? {
        return withContext(context = Dispatchers.Default) {
            loadResources(
                context = context,
                selection = generateSqlSelection(mediaType = mediaType),
                selectionArgs = null
            )
        }
    }

    private fun generateSqlSelection(mediaType: MediaType): String {
        val mediaTypeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE
        val mediaTypeImageColumn = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val mediaTypeVideoColumn = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val mimeTypeColumn = MediaStore.Files.FileColumns.MIME_TYPE
        val sizeColumn = MediaStore.MediaColumns.SIZE
        val sizeFilter = "$sizeColumn > 0"
        val queryImageSelection =
            "$mediaTypeColumn = $mediaTypeImageColumn and $mimeTypeColumn like 'image/%'"
        val queryVideoSelection =
            "$mediaTypeColumn = $mediaTypeVideoColumn and $mimeTypeColumn like 'video/%'"
        val typeSelection = when (mediaType) {
            is MediaType.ImageOnly -> {
                queryImageSelection
            }

            MediaType.VideoOnly -> {
                queryVideoSelection
            }

            is MediaType.ImageAndVideo -> {
                buildString {
                    append(queryImageSelection)
                    append(" or ")
                    append(queryVideoSelection)
                }
            }

            is MediaType.MultipleMimeType -> {
                mediaType.mimeTypes.joinToString(
                    prefix = "$mimeTypeColumn in (",
                    postfix = ")",
                    separator = ",",
                    transform = {
                        "'${it}'"
                    }
                )
            }
        }
        return "($typeSelection) and $sizeFilter"
    }

    suspend fun loadResources(context: Context, uri: Uri): MediaInfo? {
        return withContext(context = Dispatchers.Default) {
            val id = ContentUris.parseId(uri)
            val selection = MediaStore.MediaColumns._ID + " = " + id
            val resources = loadResources(
                context = context,
                selection = selection,
                selectionArgs = null
            )
            if (resources.isNullOrEmpty() || resources.size != 1) {
                null
            } else {
                resources[0]
            }
        }
    }

    private fun Cursor.getLong(columnName: String, default: Long): Long {
        return try {
            val columnIndex = getColumnIndexOrThrow(columnName)
            getLong(columnIndex)
        } catch (throwable: IllegalArgumentException) {
            throwable.printStackTrace()
            default
        }
    }

    private fun Cursor.getString(columnName: String, default: String): String {
        return try {
            val columnIndex = getColumnIndexOrThrow(columnName)
            getString(columnIndex) ?: default
        } catch (throwable: IllegalArgumentException) {
            throwable.printStackTrace()
            default
        }
    }

}
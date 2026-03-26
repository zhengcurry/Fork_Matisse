package github.leavesczy.matisse.internal.logic

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
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
     * 删除媒体文件并返回已成功删除的 URI 列表。
     *
     * 策略：先尝试直接 contentResolver.delete()（系统签名/平台签名应用可直接成功），
     * 仅当抛出 SecurityException 时才 fallback 到系统授权对话框。
     * 这样无论应用装在 /system/ 还是 /data/，只要有平台签名就能无感删除。
     */
    suspend fun deleteMedia(
        launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
        context: Context,
        uris: List<Uri>
    ): List<Uri> {
        Log.d("curry", "deleteMedia: attempting to delete ${uris.size} files")
        val successfullyDeleted = mutableListOf<Uri>()
        val needAuthUris = mutableListOf<Uri>() // 需要系统授权的 URI

        // 第一步：逐个尝试直接删除
        withContext(Dispatchers.IO) {
            val paths = uris.mapNotNull { queryPath(context, it) }
            for (uri in uris) {
                try {
                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    Log.d("curry", "Direct delete: $uri, rows=$deletedRows")
                    successfullyDeleted.add(uri)
                } catch (securityException: SecurityException) {
                    // 没有权限直接删除，需要系统授权
                    Log.d("curry", "SecurityException for $uri, need auth")
                    needAuthUris.add(uri)
                } catch (throwable: Throwable) {
                    Log.w("curry", "Delete failed for $uri: ${throwable.message}")
                    successfullyDeleted.add(uri) // 文件可能已不存在，标记为已处理
                }
            }
            // 触发媒体扫描更新
            if (paths.isNotEmpty()) {
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
            }
        }

        // 第二步：直接删除全部成功，无需授权
        if (needAuthUris.isEmpty()) {
            return successfullyDeleted
        }

        // 第三步：有文件需要授权，使用系统对话框
        withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: createDeleteRequest 批量授权
                try {
                    val deleteRequest = MediaStore.createDeleteRequest(
                        context.contentResolver, needAuthUris
                    )
                    launcher.launch(IntentSenderRequest.Builder(deleteRequest).build())
                } catch (e: Exception) {
                    Log.e("curry", "createDeleteRequest failed", e)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10: RecoverableSecurityException 逐个授权
                val firstUri = needAuthUris.first()
                try {
                    context.contentResolver.delete(firstUri, null, null)
                } catch (securityException: SecurityException) {
                    val recoverable = securityException as? RecoverableSecurityException
                    if (recoverable != null) {
                        try {
                            launcher.launch(
                                IntentSenderRequest.Builder(
                                    recoverable.userAction.actionIntent.intentSender
                                ).build()
                            )
                        } catch (e: Exception) {
                            Log.e("curry", "Failed to launch permission request", e)
                        }
                    }
                }
            }
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
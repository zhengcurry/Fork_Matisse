package github.leavesczy.matisse.internal.logic

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.matisse.Matisse
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.MediaType
import github.leavesczy.matisse.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * @Author: leavesCZY
 * @Date: 2022/6/1 19:19
 * @Desc:
 */
internal class MatisseViewModel(application: Application, matisse: Matisse) :
    AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    val maxSelectable = matisse.maxSelectable

    private val imageEngine = matisse.imageEngine

    private val gridColumns = matisse.gridColumns

    private val fastSelect = matisse.fastSelect

    var mediaType = matisse.mediaType

    val singleMediaType = matisse.singleMediaType

    val captureStrategy = matisse.captureStrategy

    private val mediaFilter = matisse.mediaFilter

    val fullScreen = matisse.fullScreen

    private val onlyFolders = matisse.onlyFolders

    // 待删除的文件列表（用于 Android 10 逐个授权删除）
    private val pendingDeleteUris = mutableListOf<Uri>()

    private val defaultBucketId = "&__matisseDefaultBucketId__&"

    private val defaultBucket = MatisseMediaBucket(
        bucketId = defaultBucketId,
        bucketName = getString(id = R.string.matisse_default_bucket_name),
        supportCapture = captureStrategy != null,
        resources = emptyList()
    )

    private val allMediaResources = mutableListOf<MatisseMediaExtend>()

    var pageViewState by mutableStateOf(
        value = MatissePageViewState(
            maxSelectable = maxSelectable,
            fastSelect = fastSelect,
            gridColumns = gridColumns,
            imageEngine = imageEngine,
            captureStrategy = captureStrategy,
            selectedBucket = defaultBucket,
            mediaBucketsInfo = emptyList(),
            onClickBucket = ::onClickBucket,
            lazyGridState = LazyGridState(),
            onClickMedia = ::onClickMedia,
            onMediaCheckChanged = ::onMediaCheckChanged,
            onClickMediaType = ::onClickMediaType,
            onClickDelete = ::deleteMediaResources,
            continuePendingDelete = ::continuePendingDelete,
            reloadMediaResources = ::reloadMediaResources,
            showDateHeaders = matisse.showDateHeaders,
            enableSelectAll = matisse.enableSelectAll,
            onClickSelectAll = ::onClickSelectAll,
            onCancelChoice = ::clearSelection,
            gridAspectRatio = matisse.gridAspectRatio,
            gridSpacingDp = matisse.gridSpacingDp,
        )
    )
        private set

    var bottomBarViewState by mutableStateOf(
        value = buildBottomBarViewState()
    )
        private set

    var previewPageViewState by mutableStateOf(
        value = MatissePreviewPageViewState(
            visible = false,
            initialPage = 0,
            maxSelectable = maxSelectable,
            sureButtonText = "",
            sureButtonClickable = false,
            previewResources = emptyList(),
            onMediaCheckChanged = {},
            onDismissRequest = {},
            deleteMediaResources = ::deleteMediaResources,
            continuePendingDelete = ::continuePendingDelete,
            reloadMediaResources = ::reloadMediaResources,
            showMediaInfo = matisse.showMediaInfo,
        )
    )
        private set

    var loadingDialogVisible by mutableStateOf(value = false)
        private set

    private val unselectedDisabledMediaSelectState = MatisseMediaSelectState(
        isSelected = false,
        isEnabled = false,
        positionIndex = -1
    )

    private val unselectedEnabledMediaSelectState = MatisseMediaSelectState(
        isSelected = false,
        isEnabled = true,
        positionIndex = -1
    )

    fun requestReadMediaPermissionResult(granted: Boolean) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            showLoadingDialog()
            val lastVisible = dismissPreviewPage()
            allMediaResources.clear()
            if (granted) {
                val allResources = loadMediaResources()
                allMediaResources.addAll(elements = allResources)
                val collectBucket = defaultBucket.copy(resources = allResources)
                val allMediaBuckets = buildList {
                    add(
                        element = MatisseMediaBucketInfo(
                            bucketId = collectBucket.bucketId,
                            bucketName = collectBucket.bucketName,
                            size = collectBucket.resources.size,
                            firstMedia = collectBucket.resources.firstOrNull()?.media
                        )
                    )
                    addAll(
                        elements = allResources.groupBy {
                            it.bucketId
                        }.mapNotNull {
                            val bucketId = it.key
                            val resources = it.value
                            val firstResource = resources.firstOrNull()
                            val bucketName = firstResource?.bucketName
                            if (bucketName.isNullOrBlank()) {
                                null
                            } else {
                                MatisseMediaBucketInfo(
                                    bucketId = bucketId,
                                    bucketName = bucketName,
                                    size = resources.size,
                                    firstMedia = firstResource.media
                                )
                            }
                        }
                    )
                }
                pageViewState = pageViewState.copy(
                    mediaBucketsInfo = allMediaBuckets,
                    selectedBucket = collectBucket
                )
                defaultSelectedResources(allMediaResources = allResources)
            } else {
                resetViewState()
                showToast(id = R.string.matisse_read_media_permission_denied)
            }
            bottomBarViewState = buildBottomBarViewState()
            dismissLoadingDialog()

            if (lastVisible) {
                val totalResources = pageViewState.selectedBucket.resources
                previewResource(
                    initialPage = min(previewPageViewState.initialPage + 1, totalResources.size - 1),
                    totalResources = totalResources,
                    selectedResources = filterSelectedMediaResource()
                )
            }
        }
    }

    private fun defaultSelectedResources(allMediaResources: List<MatisseMediaExtend>) {
        val defaultSelectedMediaIds = if (mediaFilter == null || fastSelect) {
            emptyList()
        } else {
            allMediaResources.filter {
                mediaFilter.selectMedia(mediaResource = it.media)
            }.map {
                it.mediaId
            }
        }
        if (defaultSelectedMediaIds.isNotEmpty()) {
            var positionIndex = 0
            allMediaResources.forEach { media ->
                val selectState = media.selectState as MutableState<MatisseMediaSelectState>
                if (defaultSelectedMediaIds.contains(element = media.mediaId)) {
                    selectState.value = MatisseMediaSelectState(
                        isSelected = true,
                        isEnabled = true,
                        positionIndex = positionIndex
                    )
                    positionIndex++
                } else {
                    selectState.value = if (defaultSelectedMediaIds.size >= maxSelectable) {
                        unselectedDisabledMediaSelectState
                    } else {
                        unselectedEnabledMediaSelectState
                    }
                }
            }
        }
    }

    /**
     * preview删除对应的媒体文件
     */
    private fun deleteMediaResources(
        uri: Uri,
        launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
    ) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            pendingDeleteUris.clear()
            pendingDeleteUris.add(uri)
            val deleted = MediaProvider.deleteMedia(launcher, context, listOf(uri))
            // 从待删除列表中移除已成功删除的文件
            pendingDeleteUris.removeAll(deleted.toSet())
            if (pendingDeleteUris.isEmpty()) {
                // 所有文件都已删除，刷新列表
                reloadMediaResources()
            }
        }
    }

    private fun deleteMediaResources(launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) {
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            val selected = filterSelectedMediaResource().map {
                it.media.uri
            }
            if (selected.isNotEmpty()) {
                pendingDeleteUris.clear()
                pendingDeleteUris.addAll(selected)
                val deleted = MediaProvider.deleteMedia(launcher, context, selected)
                // 从待删除列表中移除已成功删除的文件
                pendingDeleteUris.removeAll(deleted.toSet())
                if (pendingDeleteUris.isEmpty()) {
                    // 所有文件都已删除，刷新列表
                    reloadMediaResources()
                }
            }
        }
    }

    /**
     * 授权成功后继续删除剩余文件（仅用于 Android 10）
     * 注意：必须立即调用，避免授权失效
     */
    fun continuePendingDelete(launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) {
        if (pendingDeleteUris.isEmpty()) {
            // 没有待删除文件，只刷新列表
            reloadMediaResources()
            return
        }
        viewModelScope.launch(context = Dispatchers.Main.immediate) {
            // 不等待刷新，立即重新尝试删除剩余文件（利用刚刚获得的授权）
            val urisToDelete = pendingDeleteUris.toList()
            val deleted = MediaProvider.deleteMedia(launcher, context, urisToDelete)
            // 从待删除列表中移除已成功删除的文件
            pendingDeleteUris.removeAll(deleted.toSet())
            if (pendingDeleteUris.isEmpty()) {
                // 所有文件都已删除，刷新列表
                reloadMediaResources()
            }
            // 如果还有待删除的文件，说明又遇到了需要授权的文件
            // 会再次弹出授权对话框，用户授权后会再次调用此方法
        }
    }

    private suspend fun loadMediaResources(): List<MatisseMediaExtend> {
        return withContext(context = Dispatchers.Default) {
            val resourcesInfo = MediaProvider.loadResources(
                context = context,
                mediaType = mediaType
            )
            if (resourcesInfo.isNullOrEmpty()) {
                emptyList()
            } else {
                resourcesInfo.mapNotNull {
                    // 文件夹限定过滤：onlyFolders 非空时仅保留指定 bucketName
                    if (onlyFolders.isNotEmpty() && !onlyFolders.contains(it.bucketName)) {
                        return@mapNotNull null
                    }
                    val media = MediaResource(
                        uri = it.uri,
                        path = it.path,
                        name = it.name,
                        mimeType = it.mimeType,
                        size = it.size,
                        dateModified = it.dateModified
                    )
                    if (mediaFilter?.ignoreMedia(mediaResource = media) == true) {
                        null
                    } else {
                        MatisseMediaExtend(
                            mediaId = it.mediaId,
                            bucketId = it.bucketId,
                            bucketName = it.bucketName,
                            media = media,
                            selectState = mutableStateOf(value = unselectedEnabledMediaSelectState)
                        )
                    }
                }
            }
        }
    }

    private fun resetViewState() {
        pageViewState = pageViewState.copy(
            selectedBucket = defaultBucket,
            mediaBucketsInfo = listOf(
                element = MatisseMediaBucketInfo(
                    bucketId = defaultBucket.bucketId,
                    bucketName = defaultBucket.bucketName,
                    size = defaultBucket.resources.size,
                    firstMedia = defaultBucket.resources.firstOrNull()?.media,
                )
            )
        )
    }

    private fun reloadMediaResources() {
        requestReadMediaPermissionResult(true)
    }

    private fun onClickMediaType(index: Int) {
        this.mediaType = if (index == 1) {
            MediaType.VideoOnly
        } else {
            MediaType.ImageOnly
        }
        requestReadMediaPermissionResult(true)
    }

    private fun onClickSelectAll() {
        val resources = pageViewState.selectedBucket.resources
        val allSelected = resources.isNotEmpty() && resources.all { it.selectState.value.isSelected }
        if (allSelected) {
            // 已全选 → 取消全选
            resetAllMediaSelectState(state = unselectedEnabledMediaSelectState)
        } else {
            // 未全选 → 全选（受 maxSelectable 限制）
            var positionIndex = 0
            allMediaResources.forEach { media ->
                val selectState = media.selectState as MutableState<MatisseMediaSelectState>
                val inCurrentBucket = resources.contains(media)
                if (inCurrentBucket && positionIndex < maxSelectable) {
                    selectState.value = MatisseMediaSelectState(
                        isSelected = true,
                        isEnabled = true,
                        positionIndex = positionIndex
                    )
                    positionIndex++
                } else {
                    selectState.value = if (positionIndex >= maxSelectable) {
                        unselectedDisabledMediaSelectState
                    } else {
                        unselectedEnabledMediaSelectState
                    }
                }
            }
        }
        updatePreviewPageIfNeed()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun clearSelection() {
        resetAllMediaSelectState(state = unselectedEnabledMediaSelectState)
        bottomBarViewState = buildBottomBarViewState()
    }

    private suspend fun onClickBucket(bucketId: String) {
        val viewState = pageViewState
        val isDefaultBucketId = bucketId == defaultBucketId
        val bucketName = viewState.mediaBucketsInfo.first {
            it.bucketId == bucketId
        }.bucketName
        val supportCapture = isDefaultBucketId && defaultBucket.supportCapture
        val resources = if (isDefaultBucketId) {
            allMediaResources
        } else {
            allMediaResources.filter {
                it.bucketId == bucketId
            }
        }
        pageViewState = viewState.copy(
            selectedBucket = MatisseMediaBucket(
                bucketId = bucketId,
                bucketName = bucketName,
                supportCapture = supportCapture,
                resources = resources
            )
        )
        delay(timeMillis = 80)
        pageViewState.lazyGridState.animateScrollToItem(index = 0)
    }

    private fun onMediaCheckChanged(mediaResource: MatisseMediaExtend) {
        val selectState = mediaResource.selectState as MutableState<MatisseMediaSelectState>
        if (selectState.value.isSelected) {
            selectState.value = unselectedEnabledMediaSelectState
        } else {
            if (maxSelectable == 1) {
                resetAllMediaSelectState(state = unselectedEnabledMediaSelectState)
            } else {
                val selectedResources = filterSelectedMediaResource()
                if (selectedResources.size >= maxSelectable) {
                    showToast(text = textWhenTheQuantityExceedsTheLimit())
                    return
                } else if (singleMediaType) {
                    val illegalMediaType = selectedResources.any {
                        it.media.isImage != mediaResource.media.isImage
                    }
                    if (illegalMediaType) {
                        showToast(id = R.string.matisse_cannot_select_both_picture_and_video_at_the_same_time)
                        return
                    }
                }
            }
            selectState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = filterSelectedMediaResource().size
            )
        }
        rearrangeMediaPosition()
        updatePreviewPageIfNeed()
        bottomBarViewState = buildBottomBarViewState()
    }

    private fun rearrangeMediaPosition() {
        val selectedMedia = filterSelectedMediaResource()
        resetAllMediaSelectState(
            state = if (selectedMedia.size >= maxSelectable) {
                unselectedDisabledMediaSelectState
            } else {
                unselectedEnabledMediaSelectState
            }
        )
        selectedMedia.forEachIndexed { index, media ->
            val selectState = media.selectState as MutableState<MatisseMediaSelectState>
            selectState.value = MatisseMediaSelectState(
                isSelected = true,
                isEnabled = true,
                positionIndex = index
            )
        }
    }

    private fun updatePreviewPageIfNeed() {
        val viewState = previewPageViewState
        if (viewState.visible) {
            val selectedResources = filterSelectedMediaResource()
            previewPageViewState = viewState.copy(
                sureButtonText = getString(
                    id = R.string.matisse_sure,
                    selectedResources.size,
                    maxSelectable
                ),
                sureButtonClickable = selectedResources.isNotEmpty() && selectedResources.size <= maxSelectable
            )
        }
    }

    private fun textWhenTheQuantityExceedsTheLimit(): String {
        val includeImage = mediaType.includeImage
        val includeVideo = mediaType.includeVideo
        val stringId = if (includeImage && !includeVideo) {
            R.string.matisse_limit_the_number_of_image
        } else if (!includeImage && includeVideo) {
            R.string.matisse_limit_the_number_of_video
        } else {
            R.string.matisse_limit_the_number_of_media
        }
        return getString(
            id = stringId,
            maxSelectable
        )
    }

    private fun buildBottomBarViewState(): MatisseBottomBarViewState {
        val selected = filterSelectedMediaResource()
        val selectedResourcesIsNotEmpty = selected.isNotEmpty()
        return MatisseBottomBarViewState(
            previewButtonText = getString(id = R.string.matisse_preview),
            previewButtonClickable = selectedResourcesIsNotEmpty,
            onClickPreviewButton = ::onClickPreviewButton,
            sureButtonText = getString(
                id = R.string.matisse_sure,
                selected.size,
                maxSelectable
            ),
            sureButtonClickable = selectedResourcesIsNotEmpty && selected.size <= maxSelectable
        )
    }

    private fun onClickMedia(mediaResource: MatisseMediaExtend) {
        val totalResources = pageViewState.selectedBucket.resources
        previewResource(
            initialPage = totalResources.indexOf(element = mediaResource),
            totalResources = totalResources,
            selectedResources = filterSelectedMediaResource()
        )
    }

    private fun onClickPreviewButton() {
        val selected = filterSelectedMediaResource()
        previewResource(
            initialPage = 0,
            totalResources = selected,
            selectedResources = selected
        )
    }

    private fun previewResource(
        initialPage: Int,
        totalResources: List<MatisseMediaExtend>,
        selectedResources: List<MatisseMediaExtend>
    ) {
        previewPageViewState = previewPageViewState.copy(
            visible = true,
            initialPage = initialPage,
            sureButtonText = getString(
                id = R.string.matisse_sure,
                selectedResources.size,
                maxSelectable
            ),
            sureButtonClickable = selectedResources.isNotEmpty() && selectedResources.size <= maxSelectable,
            previewResources = totalResources,
            onMediaCheckChanged = ::onMediaCheckChanged,
            onDismissRequest = ::dismissPreviewPage
        )
    }

    private fun dismissPreviewPage(): Boolean {
        val viewState = previewPageViewState
        if (viewState.visible) {
            previewPageViewState = viewState.copy(
                visible = false,
                onMediaCheckChanged = {},
                onDismissRequest = {}
            )
        }
        return viewState.visible
    }

    fun filterSelectedMedia(): List<MediaResource> {
        return filterSelectedMediaResource().map {
            it.media
        }
    }

    private fun filterSelectedMediaResource(): List<MatisseMediaExtend> {
        return allMediaResources.filter {
            it.selectState.value.isSelected
        }.sortedBy {
            it.selectState.value.positionIndex
        }
    }

    private fun resetAllMediaSelectState(state: MatisseMediaSelectState) {
        allMediaResources.forEach {
            (it.selectState as MutableState<MatisseMediaSelectState>).value = state
        }
    }

    private fun showLoadingDialog() {
        loadingDialogVisible = true
    }

    private fun dismissLoadingDialog() {
        loadingDialogVisible = false
    }

    private fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }

    private fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
        return context.getString(id, *formatArgs)
    }

    private fun showToast(@StringRes id: Int) {
        showToast(text = getString(id = id))
    }

    private fun showToast(text: String) {
        if (text.isNotBlank()) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

}
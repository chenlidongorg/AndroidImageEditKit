package org.endlessai.androidimageeditkit

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class DragHandle {
    NONE,
    MOVE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

private enum class HorizontalAlignment {
    START,
    CENTER,
    END
}

private enum class VerticalAlignment {
    TOP,
    CENTER,
    BOTTOM
}

private enum class ExportDestination {
    GALLERY,
    FILE
}

private object EditorUiColors {
    val ScreenBg = Color(0xFFF5F5F5)
    val WorkspaceBg = Color(0xFFFFFFFF)
    val WorkspaceBorder = Color(0xFFD5D5D5)
    val ToolbarSurface = Color(0xFFFFFFFF)
    val ToolbarBorder = Color(0xFFDBDBDB)
    val Primary = Color(0xFF111111)
    val PrimarySoft = Color(0x1F111111)
    val SecondaryText = Color(0xFF5F5F5F)
    val CropStroke = Color(0xFFFFFFFF)
    val CropGrid = Color(0xB3FFFFFF)
    val HandleFill = Color(0xFFFFFFFF)
    val HandleStroke = Color(0xFF111111)
    val Mask = Color(0x88000000)
    val MutedBorder = Color(0xFFBEBEBE)
    val SelectionGreen = Color(0xFF1EA954)
    val SelectionGreenBorder = Color(0xFF14833E)
}

private enum class AspectRatioOption(val label: String, val ratio: Float?) {
    FREE("a:b", null),
    RATIO_1_1("1:1", 1f),
    RATIO_5_4("5:4", 5f / 4f),
    RATIO_4_5("4:5", 4f / 5f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f)
}

private data class ExportSettings(
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER
)

private data class RenderInfo(
    val imageRectOnScreen: RectF,
    val scale: Float
)

@Composable
internal fun ImageEditScreen(
    initialUri: Uri?,
    onCancel: () -> Unit,
    onConfirm: (ImageEditResult) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputUriString by rememberSaveable { mutableStateOf(initialUri?.toString()) }

    var workingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropRectOnBitmap by remember { mutableStateOf<RectF?>(null) }
    var workspaceSize by remember { mutableStateOf(IntSize.Zero) }

    var aspectRatioOption by remember { mutableStateOf(AspectRatioOption.FREE) }
    var aspectRatioMenuExpanded by remember { mutableStateOf(false) }
    var addImageMenuExpanded by remember { mutableStateOf(false) }

    var showCustomExportDialog by remember { mutableStateOf(false) }
    var customExportDefaultWidth by remember { mutableIntStateOf(0) }
    var customExportDefaultHeight by remember { mutableIntStateOf(0) }

    var exportSettings by remember { mutableStateOf(ExportSettings()) }
    var isLoadingImage by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var pendingFileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            inputUriString = uri.toString()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not support persistable grants.
            }
            inputUriString = uri.toString()
        }
    }

    val exportToFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bitmap = pendingFileBitmap
        pendingFileBitmap = null

        if (uri == null || bitmap == null) {
            Toast.makeText(context, "已取消导出到文件", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        isExporting = true
        coroutineScope.launch {
            val saved = withContext(Dispatchers.IO) {
                saveBitmapToUri(context = context, bitmap = bitmap, outputUri = uri)
            }
            isExporting = false
            if (saved) {
                Toast.makeText(context, "已导出到文件", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "文件导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val renderInfo = remember(workingBitmap, workspaceSize) {
        val bitmap = workingBitmap
        if (bitmap == null || workspaceSize.width <= 0 || workspaceSize.height <= 0) {
            null
        } else {
            computeRenderInfo(
                workspaceSize = workspaceSize,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
        }
    }

    LaunchedEffect(inputUriString) {
        val uri = inputUriString?.let(Uri::parse)
        if (uri == null) {
            workingBitmap = null
            cropRectOnBitmap = null
            return@LaunchedEffect
        }

        isLoadingImage = true
        val bitmap = withContext(Dispatchers.IO) {
            loadBitmap(context = context, uri = uri)
        }

        if (bitmap == null) {
            Toast.makeText(context, "图片加载失败，请重新选择", Toast.LENGTH_SHORT).show()
            workingBitmap = null
            cropRectOnBitmap = null
        } else {
            workingBitmap = bitmap
            cropRectOnBitmap = createDefaultCropRect(
                width = bitmap.width,
                height = bitmap.height,
                aspectRatio = aspectRatioOption.ratio
            )
        }
        isLoadingImage = false
    }

    fun openImagePicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun openDocumentPicker() {
        openDocumentLauncher.launch(arrayOf("image/*"))
    }

    fun requireEditableImage(block: (Bitmap, RectF) -> Unit) {
        val bitmap = workingBitmap
        val cropRect = cropRectOnBitmap
        if (bitmap == null || cropRect == null) {
            Toast.makeText(context, "请先添加图片", Toast.LENGTH_SHORT).show()
            return
        }
        block(bitmap, cropRect)
    }

    fun applyAspectRatio(option: AspectRatioOption) {
        aspectRatioOption = option
        aspectRatioMenuExpanded = false
        requireEditableImage { bitmap, currentRect ->
            cropRectOnBitmap = if (option.ratio == null) {
                currentRect
            } else {
                coerceRectToAspectRatio(
                    current = currentRect,
                    aspectRatio = option.ratio,
                    imageWidth = bitmap.width.toFloat(),
                    imageHeight = bitmap.height.toFloat(),
                    minSize = 64f
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorUiColors.ScreenBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        TopActionBar(
            onConfirm = {
                requireEditableImage { bitmap, cropRect ->
                    if (isExporting) {
                        return@requireEditableImage
                    }
                    isExporting = true
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            exportEditedImage(
                                context = context,
                                sourceBitmap = bitmap,
                                cropRect = cropRect,
                                exportSettings = exportSettings
                            )
                        }
                        isExporting = false
                        if (result == null) {
                            Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(result)
                        }
                    }
                }
            },
            onSave = {
                requireEditableImage { bitmap, cropRect ->
                    if (isExporting) {
                        return@requireEditableImage
                    }
                    isExporting = true
                    coroutineScope.launch {
                        val savedUri = withContext(Dispatchers.IO) {
                            saveEditedImageToGallery(
                                context = context,
                                sourceBitmap = bitmap,
                                cropRect = cropRect,
                                exportSettings = exportSettings
                            )
                        }
                        isExporting = false
                        if (savedUri == null) {
                            Toast.makeText(context, "保存失败，请检查相册权限", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCustomExport = {
                requireEditableImage { _, cropRect ->
                    customExportDefaultWidth = cropRect.width().roundToInt().coerceAtLeast(1)
                    customExportDefaultHeight = cropRect.height().roundToInt().coerceAtLeast(1)
                    showCustomExportDialog = true
                }
            },
            addImageMenuExpanded = addImageMenuExpanded,
            onAddImageMenuToggle = {
                addImageMenuExpanded = true
            },
            onAddImageMenuDismiss = {
                addImageMenuExpanded = false
            },
            onAddFromGallery = {
                addImageMenuExpanded = false
                openImagePicker()
            },
            onAddFromFiles = {
                addImageMenuExpanded = false
                openDocumentPicker()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 1.5.dp,
                    color = EditorUiColors.WorkspaceBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .background(EditorUiColors.WorkspaceBg, shape = RoundedCornerShape(18.dp))
                .onSizeChanged { size ->
                    workspaceSize = size
                }
        ) {
            if (workingBitmap == null) {
                EmptyWorkspace(
                    isLoadingImage = isLoadingImage,
                    onAddImage = {
                        addImageMenuExpanded = true
                    }
                )
            } else {
                CropWorkspace(
                    bitmap = workingBitmap,
                    cropRectOnBitmap = cropRectOnBitmap,
                    renderInfo = renderInfo,
                    aspectRatio = aspectRatioOption.ratio,
                    onCropRectChange = { rect ->
                        cropRectOnBitmap = rect
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoadingImage || isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        BottomActionBar(
            selectedAspectRatio = aspectRatioOption,
            aspectRatioMenuExpanded = aspectRatioMenuExpanded,
            onAspectRatioMenuToggle = {
                aspectRatioMenuExpanded = !aspectRatioMenuExpanded
            },
            onAspectRatioDismiss = {
                aspectRatioMenuExpanded = false
            },
            onAspectRatioSelect = { option ->
                applyAspectRatio(option)
            },
            onRotate = {
                val bitmap = workingBitmap ?: return@BottomActionBar
                workingBitmap = rotateBitmap90(bitmap)
                cropRectOnBitmap = createDefaultCropRect(
                    width = workingBitmap?.width ?: 0,
                    height = workingBitmap?.height ?: 0,
                    aspectRatio = aspectRatioOption.ratio
                )
            },
            onFlipHorizontally = {
                val bitmap = workingBitmap ?: return@BottomActionBar
                workingBitmap = flipBitmapHorizontally(bitmap)
                cropRectOnBitmap = createDefaultCropRect(
                    width = workingBitmap?.width ?: 0,
                    height = workingBitmap?.height ?: 0,
                    aspectRatio = aspectRatioOption.ratio
                )
            },
            onCancel = onCancel,
        )
    }

    if (showCustomExportDialog) {
        ExportSettingsDialog(
            initialSettings = exportSettings,
            defaultWidthPx = customExportDefaultWidth,
            defaultHeightPx = customExportDefaultHeight,
            onDismiss = {
                showCustomExportDialog = false
            },
            onConfirmExport = { settings, destination ->
                exportSettings = settings
                showCustomExportDialog = false

                requireEditableImage { bitmap, cropRect ->
                    if (isExporting) {
                        return@requireEditableImage
                    }
                    isExporting = true
                    coroutineScope.launch {
                        val editedBitmap = withContext(Dispatchers.IO) {
                            renderEditedBitmap(
                                sourceBitmap = bitmap,
                                cropRect = cropRect,
                                exportSettings = settings
                            )
                        }

                        if (editedBitmap == null) {
                            isExporting = false
                            Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        when (destination) {
                            ExportDestination.GALLERY -> {
                                val uri = withContext(Dispatchers.IO) {
                                    saveBitmapToGallery(context = context, bitmap = editedBitmap)
                                }
                                isExporting = false
                                if (uri == null) {
                                    Toast.makeText(context, "保存到相册失败", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "已导出到相册", Toast.LENGTH_SHORT).show()
                                }
                            }

                            ExportDestination.FILE -> {
                                pendingFileBitmap = editedBitmap
                                isExporting = false
                                exportToFileLauncher.launch(buildExportFileName())
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TopActionBar(
    onConfirm: () -> Unit,
    onSave: () -> Unit,
    onCustomExport: () -> Unit,
    addImageMenuExpanded: Boolean,
    onAddImageMenuToggle: () -> Unit,
    onAddImageMenuDismiss: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAddFromFiles: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorUiColors.ToolbarSurface, RoundedCornerShape(14.dp))
            .border(1.dp, EditorUiColors.ToolbarBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onConfirm) {
                Icon(
                    imageVector = Icons.Filled.CheckCircleOutline,
                    contentDescription = "确认",
                    tint = EditorUiColors.Primary
                )
            }
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "保存",
                    tint = EditorUiColors.Primary
                )
            }
            IconButton(onClick = onCustomExport) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "自定义导出",
                    tint = EditorUiColors.Primary
                )
            }
        }

        Box {
            IconButton(onClick = onAddImageMenuToggle) {
                Icon(
                    imageVector = Icons.Filled.AddCircleOutline,
                    contentDescription = "添加图片",
                    tint = EditorUiColors.Primary
                )
            }

            DropdownMenu(
                expanded = addImageMenuExpanded,
                onDismissRequest = onAddImageMenuDismiss
            ) {
                DropdownMenuItem(
                    text = { Text("从相册导入") },
                    onClick = onAddFromGallery
                )
                DropdownMenuItem(
                    text = { Text("从文件夹导入") },
                    onClick = onAddFromFiles
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    selectedAspectRatio: AspectRatioOption,
    aspectRatioMenuExpanded: Boolean,
    onAspectRatioMenuToggle: () -> Unit,
    onAspectRatioDismiss: () -> Unit,
    onAspectRatioSelect: (AspectRatioOption) -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontally: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorUiColors.ToolbarSurface, RoundedCornerShape(14.dp))
            .border(1.dp, EditorUiColors.ToolbarBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text(text = "取消", color = EditorUiColors.SecondaryText)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(onClick = onAspectRatioMenuToggle) {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = "比例菜单",
                        tint = EditorUiColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = selectedAspectRatio.label, color = EditorUiColors.Primary)
                }

                DropdownMenu(
                    expanded = aspectRatioMenuExpanded,
                    onDismissRequest = onAspectRatioDismiss
                ) {
                    AspectRatioOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(option.label)
                            },
                            onClick = {
                                onAspectRatioSelect(option)
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onRotate) {
                Icon(
                    imageVector = Icons.Filled.CropRotate,
                    contentDescription = "旋转",
                    tint = EditorUiColors.Primary
                )
            }
            IconButton(onClick = onFlipHorizontally) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "水平翻转",
                    tint = EditorUiColors.Primary
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkspace(
    isLoadingImage: Boolean,
    onAddImage: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onAddImage, enabled = !isLoadingImage) {
                Icon(
                    imageVector = Icons.Filled.BlurOn,
                    contentDescription = "添加图片",
                    modifier = Modifier.size(44.dp),
                    tint = EditorUiColors.Primary
                )
            }
            Text(
                text = if (isLoadingImage) "图片加载中..." else "操作台暂无图片，点击添加图片",
                style = MaterialTheme.typography.bodyMedium,
                color = EditorUiColors.SecondaryText
            )
        }
    }
}

@Composable
private fun CropWorkspace(
    bitmap: Bitmap?,
    cropRectOnBitmap: RectF?,
    renderInfo: RenderInfo?,
    aspectRatio: Float?,
    onCropRectChange: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null || cropRectOnBitmap == null || renderInfo == null) {
        return
    }
    val latestCropRectState = rememberUpdatedState(cropRectOnBitmap)
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 28.dp.toPx() }
    val handleRadiusPx = with(density) { 9.dp.toPx() }

    Canvas(
        modifier = modifier
            .pointerInput(bitmap, renderInfo, aspectRatio) {
                var activeHandle = DragHandle.NONE
                var dragRect = RectF(latestCropRectState.value)
                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragRect = RectF(latestCropRectState.value)
                        val cropOnScreen = bitmapToScreen(dragRect, renderInfo)
                        activeHandle = resolveDragHandle(
                            pointer = startOffset,
                            cropRectOnScreen = cropOnScreen,
                            touchRadiusPx = touchRadiusPx
                        )
                    },
                    onDragEnd = {
                        activeHandle = DragHandle.NONE
                    },
                    onDragCancel = {
                        activeHandle = DragHandle.NONE
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (activeHandle == DragHandle.NONE) {
                            return@detectDragGestures
                        }
                        dragRect = updateCropRectByDrag(
                            current = dragRect,
                            dragHandle = activeHandle,
                            deltaX = dragAmount.x / renderInfo.scale,
                            deltaY = dragAmount.y / renderInfo.scale,
                            imageWidth = bitmap.width.toFloat(),
                            imageHeight = bitmap.height.toFloat(),
                            minSize = 64f,
                            aspectRatio = aspectRatio
                        )
                        onCropRectChange(dragRect)
                    }
                )
            }
    ) {
        val imageRect = renderInfo.imageRectOnScreen
        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = androidx.compose.ui.unit.IntOffset(
                x = imageRect.left.roundToInt(),
                y = imageRect.top.roundToInt()
            ),
            dstSize = IntSize(
                width = imageRect.width().roundToInt(),
                height = imageRect.height().roundToInt()
            )
        )

        val cropRect = bitmapToScreen(cropRectOnBitmap, renderInfo)
        drawOutsideMask(imageRect = imageRect, cropRect = cropRect)

        drawRect(
            color = EditorUiColors.CropStroke,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width(), cropRect.height()),
            style = Stroke(width = 4f)
        )

        val thirdWidth = cropRect.width() / 3f
        val thirdHeight = cropRect.height() / 3f
        drawLine(
            color = EditorUiColors.CropGrid,
            start = Offset(cropRect.left + thirdWidth, cropRect.top),
            end = Offset(cropRect.left + thirdWidth, cropRect.bottom),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = EditorUiColors.CropGrid,
            start = Offset(cropRect.left + thirdWidth * 2f, cropRect.top),
            end = Offset(cropRect.left + thirdWidth * 2f, cropRect.bottom),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = EditorUiColors.CropGrid,
            start = Offset(cropRect.left, cropRect.top + thirdHeight),
            end = Offset(cropRect.right, cropRect.top + thirdHeight),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = EditorUiColors.CropGrid,
            start = Offset(cropRect.left, cropRect.top + thirdHeight * 2f),
            end = Offset(cropRect.right, cropRect.top + thirdHeight * 2f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )

        drawHandles(cropRect, handleRadiusPx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutsideMask(
    imageRect: RectF,
    cropRect: RectF
) {
    val maskColor = EditorUiColors.Mask

    val topHeight = max(0f, cropRect.top - imageRect.top)
    if (topHeight > 0f) {
        drawRect(
            color = maskColor,
            topLeft = Offset(imageRect.left, imageRect.top),
            size = Size(imageRect.width(), topHeight)
        )
    }

    val bottomHeight = max(0f, imageRect.bottom - cropRect.bottom)
    if (bottomHeight > 0f) {
        drawRect(
            color = maskColor,
            topLeft = Offset(imageRect.left, cropRect.bottom),
            size = Size(imageRect.width(), bottomHeight)
        )
    }

    val leftWidth = max(0f, cropRect.left - imageRect.left)
    if (leftWidth > 0f) {
        drawRect(
            color = maskColor,
            topLeft = Offset(imageRect.left, cropRect.top),
            size = Size(leftWidth, cropRect.height())
        )
    }

    val rightWidth = max(0f, imageRect.right - cropRect.right)
    if (rightWidth > 0f) {
        drawRect(
            color = maskColor,
            topLeft = Offset(cropRect.right, cropRect.top),
            size = Size(rightWidth, cropRect.height())
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandles(
    cropRect: RectF,
    handleRadius: Float
) {
    val points = listOf(
        Offset(cropRect.left, cropRect.top),
        Offset(cropRect.right, cropRect.top),
        Offset(cropRect.left, cropRect.bottom),
        Offset(cropRect.right, cropRect.bottom),
        Offset((cropRect.left + cropRect.right) / 2f, cropRect.top),
        Offset((cropRect.left + cropRect.right) / 2f, cropRect.bottom),
        Offset(cropRect.left, (cropRect.top + cropRect.bottom) / 2f),
        Offset(cropRect.right, (cropRect.top + cropRect.bottom) / 2f)
    )

    points.forEach { point ->
        drawCircle(
            color = EditorUiColors.HandleFill,
            radius = handleRadius,
            center = point
        )
        drawCircle(
            color = EditorUiColors.HandleStroke,
            radius = handleRadius,
            center = point,
            style = Stroke(width = max(2f, handleRadius * 0.22f))
        )
    }
}

@Composable
private fun ExportSettingsDialog(
    initialSettings: ExportSettings,
    defaultWidthPx: Int,
    defaultHeightPx: Int,
    onDismiss: () -> Unit,
    onConfirmExport: (ExportSettings, ExportDestination) -> Unit
) {
    var widthText by remember(initialSettings.widthPx, defaultWidthPx) {
        mutableStateOf((initialSettings.widthPx ?: defaultWidthPx).toString())
    }
    var heightText by remember(initialSettings.heightPx, defaultHeightPx) {
        mutableStateOf((initialSettings.heightPx ?: defaultHeightPx).toString())
    }
    var horizontalAlignment by remember(initialSettings.horizontalAlignment) {
        mutableStateOf(initialSettings.horizontalAlignment)
    }
    var verticalAlignment by remember(initialSettings.verticalAlignment) {
        mutableStateOf(initialSettings.verticalAlignment)
    }
    var exportDestination by remember { mutableStateOf(ExportDestination.GALLERY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "精确导出")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = widthText,
                    onValueChange = { value ->
                        widthText = value.filter { it.isDigit() }
                    },
                    label = {
                        Text(text = "宽度")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = heightText,
                    onValueChange = { value ->
                        heightText = value.filter { it.isDigit() }
                    },
                    label = {
                        Text(text = "高度")
                    },
                    singleLine = true
                )

                Text(text = "对齐方式")
                AlignmentGridSelector(
                    selectedHorizontal = horizontalAlignment,
                    selectedVertical = verticalAlignment,
                    onSelect = { horizontal, vertical ->
                        horizontalAlignment = horizontal
                        verticalAlignment = vertical
                    }
                )

                Text(text = "导出位置")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ExportDestinationButton(
                        label = "相册",
                        selected = exportDestination == ExportDestination.GALLERY,
                        onClick = {
                            exportDestination = ExportDestination.GALLERY
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ExportDestinationButton(
                        label = "文件夹",
                        selected = exportDestination == ExportDestination.FILE,
                        onClick = {
                            exportDestination = ExportDestination.FILE
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val width = widthText.toIntOrNull()?.takeIf { it > 0 } ?: defaultWidthPx
                    val height = heightText.toIntOrNull()?.takeIf { it > 0 } ?: defaultHeightPx
                    onConfirmExport(
                        ExportSettings(
                            widthPx = width,
                            heightPx = height,
                            horizontalAlignment = horizontalAlignment,
                            verticalAlignment = verticalAlignment
                        ),
                        exportDestination
                    )
                }
            ) {
                Text(text = "确定导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun AlignmentGridSelector(
    selectedHorizontal: HorizontalAlignment,
    selectedVertical: VerticalAlignment,
    onSelect: (HorizontalAlignment, VerticalAlignment) -> Unit
) {
    val rows = listOf(
        VerticalAlignment.TOP,
        VerticalAlignment.CENTER,
        VerticalAlignment.BOTTOM
    )
    val cols = listOf(
        HorizontalAlignment.START,
        HorizontalAlignment.CENTER,
        HorizontalAlignment.END
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cols.forEach { col ->
                    val selected = row == selectedVertical && col == selectedHorizontal
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (selected) EditorUiColors.SelectionGreenBorder else EditorUiColors.MutedBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(
                                color = if (selected) EditorUiColors.SelectionGreen else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                onSelect(col, row)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportDestinationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) EditorUiColors.SelectionGreen else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) EditorUiColors.SelectionGreenBorder else EditorUiColors.MutedBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else EditorUiColors.SecondaryText
        )
    }
}

private fun computeRenderInfo(
    workspaceSize: IntSize,
    imageWidth: Int,
    imageHeight: Int
): RenderInfo {
    val workspaceWidth = workspaceSize.width.toFloat()
    val workspaceHeight = workspaceSize.height.toFloat()

    val imageWidthFloat = imageWidth.toFloat()
    val imageHeightFloat = imageHeight.toFloat()

    val scale = min(workspaceWidth / imageWidthFloat, workspaceHeight / imageHeightFloat)
    val drawnWidth = imageWidthFloat * scale
    val drawnHeight = imageHeightFloat * scale

    val left = (workspaceWidth - drawnWidth) / 2f
    val top = (workspaceHeight - drawnHeight) / 2f

    return RenderInfo(
        imageRectOnScreen = RectF(left, top, left + drawnWidth, top + drawnHeight),
        scale = scale
    )
}

private fun createDefaultCropRect(
    width: Int,
    height: Int,
    aspectRatio: Float?
): RectF {
    val imageWidth = width.toFloat()
    val imageHeight = height.toFloat()

    if (imageWidth <= 0f || imageHeight <= 0f) {
        return RectF(0f, 0f, 0f, 0f)
    }

    if (aspectRatio == null) {
        // a:b（自由模式）默认全图，便于直接导出完整图片。
        return RectF(0f, 0f, imageWidth, imageHeight)
    }

    // 比例模式默认使用“在整图范围内可放下的最大框”，与常见裁剪产品一致。
    val maxWidth = imageWidth
    val maxHeight = imageHeight

    var cropWidth = maxWidth
    var cropHeight = cropWidth / aspectRatio
    if (cropHeight > maxHeight) {
        cropHeight = maxHeight
        cropWidth = cropHeight * aspectRatio
    }

    val left = (imageWidth - cropWidth) / 2f
    val top = (imageHeight - cropHeight) / 2f
    return RectF(left, top, left + cropWidth, top + cropHeight)
}

private fun coerceRectToAspectRatio(
    current: RectF,
    aspectRatio: Float,
    imageWidth: Float,
    imageHeight: Float,
    minSize: Float
): RectF {
    val centerX = current.centerX()
    val centerY = current.centerY()

    val minWidth = max(minSize, minSize * aspectRatio)

    val currentRatio = current.width() / current.height()
    val width = if (currentRatio > aspectRatio) {
        current.height() * aspectRatio
    } else {
        current.width()
    }

    val maxWidthByBounds = min(
        min(centerX, imageWidth - centerX) * 2f,
        min(centerY, imageHeight - centerY) * 2f * aspectRatio
    )

    val adjustedWidth = clampWithMin(width, minWidth, maxWidthByBounds)
    val adjustedHeight = adjustedWidth / aspectRatio

    val left = centerX - adjustedWidth / 2f
    val top = centerY - adjustedHeight / 2f
    return RectF(left, top, left + adjustedWidth, top + adjustedHeight)
}

private fun bitmapToScreen(rectOnBitmap: RectF, renderInfo: RenderInfo): RectF {
    return RectF(
        renderInfo.imageRectOnScreen.left + rectOnBitmap.left * renderInfo.scale,
        renderInfo.imageRectOnScreen.top + rectOnBitmap.top * renderInfo.scale,
        renderInfo.imageRectOnScreen.left + rectOnBitmap.right * renderInfo.scale,
        renderInfo.imageRectOnScreen.top + rectOnBitmap.bottom * renderInfo.scale
    )
}

private fun resolveDragHandle(
    pointer: Offset,
    cropRectOnScreen: RectF,
    touchRadiusPx: Float
): DragHandle {
    val x = pointer.x
    val y = pointer.y

    fun near(px: Float, py: Float): Boolean {
        return abs(x - px) <= touchRadiusPx && abs(y - py) <= touchRadiusPx
    }

    if (near(cropRectOnScreen.left, cropRectOnScreen.top)) return DragHandle.TOP_LEFT
    if (near(cropRectOnScreen.right, cropRectOnScreen.top)) return DragHandle.TOP_RIGHT
    if (near(cropRectOnScreen.left, cropRectOnScreen.bottom)) return DragHandle.BOTTOM_LEFT
    if (near(cropRectOnScreen.right, cropRectOnScreen.bottom)) return DragHandle.BOTTOM_RIGHT

    if (near((cropRectOnScreen.left + cropRectOnScreen.right) / 2f, cropRectOnScreen.top)) {
        return DragHandle.TOP
    }
    if (near((cropRectOnScreen.left + cropRectOnScreen.right) / 2f, cropRectOnScreen.bottom)) {
        return DragHandle.BOTTOM
    }
    if (near(cropRectOnScreen.left, (cropRectOnScreen.top + cropRectOnScreen.bottom) / 2f)) {
        return DragHandle.LEFT
    }
    if (near(cropRectOnScreen.right, (cropRectOnScreen.top + cropRectOnScreen.bottom) / 2f)) {
        return DragHandle.RIGHT
    }

    if (x in cropRectOnScreen.left..cropRectOnScreen.right && y in cropRectOnScreen.top..cropRectOnScreen.bottom) {
        return DragHandle.MOVE
    }

    return DragHandle.NONE
}

private fun updateCropRectByDrag(
    current: RectF,
    dragHandle: DragHandle,
    deltaX: Float,
    deltaY: Float,
    imageWidth: Float,
    imageHeight: Float,
    minSize: Float,
    aspectRatio: Float?
): RectF {
    if (aspectRatio == null) {
        return updateCropRectByFreeDrag(
            current = current,
            dragHandle = dragHandle,
            deltaX = deltaX,
            deltaY = deltaY,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            minSize = minSize
        )
    }

    return updateCropRectByAspectRatioDrag(
        current = current,
        dragHandle = dragHandle,
        deltaX = deltaX,
        deltaY = deltaY,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        minSize = minSize,
        aspectRatio = aspectRatio
    )
}

private fun updateCropRectByFreeDrag(
    current: RectF,
    dragHandle: DragHandle,
    deltaX: Float,
    deltaY: Float,
    imageWidth: Float,
    imageHeight: Float,
    minSize: Float
): RectF {
    val rect = RectF(current)

    when (dragHandle) {
        DragHandle.MOVE -> {
            val newLeft = (rect.left + deltaX).coerceIn(0f, imageWidth - rect.width())
            val newTop = (rect.top + deltaY).coerceIn(0f, imageHeight - rect.height())
            rect.offsetTo(newLeft, newTop)
        }

        DragHandle.LEFT -> {
            rect.left = (rect.left + deltaX).coerceIn(0f, rect.right - minSize)
        }

        DragHandle.RIGHT -> {
            rect.right = (rect.right + deltaX).coerceIn(rect.left + minSize, imageWidth)
        }

        DragHandle.TOP -> {
            rect.top = (rect.top + deltaY).coerceIn(0f, rect.bottom - minSize)
        }

        DragHandle.BOTTOM -> {
            rect.bottom = (rect.bottom + deltaY).coerceIn(rect.top + minSize, imageHeight)
        }

        DragHandle.TOP_LEFT -> {
            rect.left = (rect.left + deltaX).coerceIn(0f, rect.right - minSize)
            rect.top = (rect.top + deltaY).coerceIn(0f, rect.bottom - minSize)
        }

        DragHandle.TOP_RIGHT -> {
            rect.right = (rect.right + deltaX).coerceIn(rect.left + minSize, imageWidth)
            rect.top = (rect.top + deltaY).coerceIn(0f, rect.bottom - minSize)
        }

        DragHandle.BOTTOM_LEFT -> {
            rect.left = (rect.left + deltaX).coerceIn(0f, rect.right - minSize)
            rect.bottom = (rect.bottom + deltaY).coerceIn(rect.top + minSize, imageHeight)
        }

        DragHandle.BOTTOM_RIGHT -> {
            rect.right = (rect.right + deltaX).coerceIn(rect.left + minSize, imageWidth)
            rect.bottom = (rect.bottom + deltaY).coerceIn(rect.top + minSize, imageHeight)
        }

        DragHandle.NONE -> {
            return current
        }
    }

    return rect
}

private fun updateCropRectByAspectRatioDrag(
    current: RectF,
    dragHandle: DragHandle,
    deltaX: Float,
    deltaY: Float,
    imageWidth: Float,
    imageHeight: Float,
    minSize: Float,
    aspectRatio: Float
): RectF {
    if (dragHandle == DragHandle.NONE) {
        return current
    }

    if (dragHandle == DragHandle.MOVE) {
        val newLeft = (current.left + deltaX).coerceIn(0f, imageWidth - current.width())
        val newTop = (current.top + deltaY).coerceIn(0f, imageHeight - current.height())
        return RectF(newLeft, newTop, newLeft + current.width(), newTop + current.height())
    }

    val minWidth = max(minSize, minSize * aspectRatio)
    val minHeight = minWidth / aspectRatio

    return when (dragHandle) {
        DragHandle.LEFT -> resizeHorizontalWithAspectRatio(
            current = current,
            deltaX = deltaX,
            fromLeft = true,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            aspectRatio = aspectRatio,
            minWidth = minWidth
        )

        DragHandle.RIGHT -> resizeHorizontalWithAspectRatio(
            current = current,
            deltaX = deltaX,
            fromLeft = false,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            aspectRatio = aspectRatio,
            minWidth = minWidth
        )

        DragHandle.TOP -> resizeVerticalWithAspectRatio(
            current = current,
            deltaY = deltaY,
            fromTop = true,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            aspectRatio = aspectRatio,
            minHeight = minHeight
        )

        DragHandle.BOTTOM -> resizeVerticalWithAspectRatio(
            current = current,
            deltaY = deltaY,
            fromTop = false,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            aspectRatio = aspectRatio,
            minHeight = minHeight
        )

        DragHandle.TOP_LEFT,
        DragHandle.TOP_RIGHT,
        DragHandle.BOTTOM_LEFT,
        DragHandle.BOTTOM_RIGHT -> resizeCornerWithAspectRatio(
            current = current,
            dragHandle = dragHandle,
            deltaX = deltaX,
            deltaY = deltaY,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            aspectRatio = aspectRatio,
            minWidth = minWidth
        )

        DragHandle.MOVE,
        DragHandle.NONE -> current
    }
}

private fun resizeHorizontalWithAspectRatio(
    current: RectF,
    deltaX: Float,
    fromLeft: Boolean,
    imageWidth: Float,
    imageHeight: Float,
    aspectRatio: Float,
    minWidth: Float
): RectF {
    val anchorX = if (fromLeft) current.right else current.left
    val centerY = current.centerY()

    val requestedWidth = if (fromLeft) {
        anchorX - (current.left + deltaX)
    } else {
        (current.right + deltaX) - anchorX
    }

    val maxWidthByX = if (fromLeft) anchorX else imageWidth - anchorX
    val maxHeightByCenter = min(centerY, imageHeight - centerY) * 2f
    val maxWidthByY = maxHeightByCenter * aspectRatio
    val maxWidth = min(maxWidthByX, maxWidthByY)

    val width = clampWithMin(requestedWidth, minWidth, maxWidth)
    val height = width / aspectRatio

    val top = centerY - height / 2f
    val bottom = centerY + height / 2f

    return if (fromLeft) {
        RectF(anchorX - width, top, anchorX, bottom)
    } else {
        RectF(anchorX, top, anchorX + width, bottom)
    }
}

private fun resizeVerticalWithAspectRatio(
    current: RectF,
    deltaY: Float,
    fromTop: Boolean,
    imageWidth: Float,
    imageHeight: Float,
    aspectRatio: Float,
    minHeight: Float
): RectF {
    val anchorY = if (fromTop) current.bottom else current.top
    val centerX = current.centerX()

    val requestedHeight = if (fromTop) {
        anchorY - (current.top + deltaY)
    } else {
        (current.bottom + deltaY) - anchorY
    }

    val maxHeightByY = if (fromTop) anchorY else imageHeight - anchorY
    val maxWidthByCenter = min(centerX, imageWidth - centerX) * 2f
    val maxHeightByX = maxWidthByCenter / aspectRatio
    val maxHeight = min(maxHeightByY, maxHeightByX)

    val height = clampWithMin(requestedHeight, minHeight, maxHeight)
    val width = height * aspectRatio

    val left = centerX - width / 2f
    val right = centerX + width / 2f

    return if (fromTop) {
        RectF(left, anchorY - height, right, anchorY)
    } else {
        RectF(left, anchorY, right, anchorY + height)
    }
}

private fun resizeCornerWithAspectRatio(
    current: RectF,
    dragHandle: DragHandle,
    deltaX: Float,
    deltaY: Float,
    imageWidth: Float,
    imageHeight: Float,
    aspectRatio: Float,
    minWidth: Float
): RectF {
    val (anchorX, anchorY, signX, signY) = when (dragHandle) {
        DragHandle.TOP_LEFT -> CornerAnchor(current.right, current.bottom, -1f, -1f)
        DragHandle.TOP_RIGHT -> CornerAnchor(current.left, current.bottom, 1f, -1f)
        DragHandle.BOTTOM_LEFT -> CornerAnchor(current.right, current.top, -1f, 1f)
        DragHandle.BOTTOM_RIGHT -> CornerAnchor(current.left, current.top, 1f, 1f)
        else -> return current
    }

    val movingX = when (dragHandle) {
        DragHandle.TOP_LEFT,
        DragHandle.BOTTOM_LEFT -> current.left + deltaX

        DragHandle.TOP_RIGHT,
        DragHandle.BOTTOM_RIGHT -> current.right + deltaX

        else -> anchorX
    }

    val movingY = when (dragHandle) {
        DragHandle.TOP_LEFT,
        DragHandle.TOP_RIGHT -> current.top + deltaY

        DragHandle.BOTTOM_LEFT,
        DragHandle.BOTTOM_RIGHT -> current.bottom + deltaY

        else -> anchorY
    }

    val widthFromX = abs(movingX - anchorX)
    val widthFromY = abs(movingY - anchorY) * aspectRatio
    val requestedWidth = max(widthFromX, widthFromY)

    val maxWidthByX = if (signX < 0f) anchorX else imageWidth - anchorX
    val maxHeightByY = if (signY < 0f) anchorY else imageHeight - anchorY
    val maxWidthByY = maxHeightByY * aspectRatio
    val maxWidth = min(maxWidthByX, maxWidthByY)

    val width = clampWithMin(requestedWidth, minWidth, maxWidth)
    val height = width / aspectRatio

    val left = if (signX < 0f) anchorX - width else anchorX
    val right = if (signX < 0f) anchorX else anchorX + width
    val top = if (signY < 0f) anchorY - height else anchorY
    val bottom = if (signY < 0f) anchorY else anchorY + height

    return RectF(left, top, right, bottom)
}

private data class CornerAnchor(
    val x: Float,
    val y: Float,
    val signX: Float,
    val signY: Float
)

private fun clampWithMin(requested: Float, minValue: Float, maxValue: Float): Float {
    if (maxValue <= 0f) {
        return 0f
    }
    if (maxValue < minValue) {
        return maxValue
    }
    return requested.coerceIn(minValue, maxValue)
}

private fun rotateBitmap90(input: Bitmap): Bitmap {
    val matrix = Matrix().apply {
        postRotate(90f)
    }
    return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
}

private fun flipBitmapHorizontally(input: Bitmap): Bitmap {
    val matrix = Matrix().apply {
        preScale(-1f, 1f)
    }
    return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }
    }.getOrNull()
}

private fun renderEditedBitmap(
    sourceBitmap: Bitmap,
    cropRect: RectF,
    exportSettings: ExportSettings
): Bitmap? {
    val cropped = cropBitmap(
        sourceBitmap = sourceBitmap,
        cropRect = cropRect
    ) ?: return null

    return applyExportSettings(
        croppedBitmap = cropped,
        exportSettings = exportSettings
    )
}

private fun exportEditedImage(
    context: Context,
    sourceBitmap: Bitmap,
    cropRect: RectF,
    exportSettings: ExportSettings
): ImageEditResult? {
    val finalBitmap = renderEditedBitmap(
        sourceBitmap = sourceBitmap,
        cropRect = cropRect,
        exportSettings = exportSettings
    ) ?: return null

    val outputUri = saveToCacheAndGetUri(
        context = context,
        bitmap = finalBitmap
    ) ?: return null

    return ImageEditResult(
        outputUri = outputUri,
        outputWidth = finalBitmap.width,
        outputHeight = finalBitmap.height
    )
}

private fun saveEditedImageToGallery(
    context: Context,
    sourceBitmap: Bitmap,
    cropRect: RectF,
    exportSettings: ExportSettings
): Uri? {
    val finalBitmap = renderEditedBitmap(
        sourceBitmap = sourceBitmap,
        cropRect = cropRect,
        exportSettings = exportSettings
    ) ?: return null

    return saveBitmapToGallery(
        context = context,
        bitmap = finalBitmap
    )
}

private fun cropBitmap(sourceBitmap: Bitmap, cropRect: RectF): Bitmap? {
    val left = cropRect.left.roundToInt().coerceIn(0, sourceBitmap.width - 1)
    val top = cropRect.top.roundToInt().coerceIn(0, sourceBitmap.height - 1)
    val right = cropRect.right.roundToInt().coerceIn(left + 1, sourceBitmap.width)
    val bottom = cropRect.bottom.roundToInt().coerceIn(top + 1, sourceBitmap.height)

    val safeRect = Rect(left, top, right, bottom)
    val width = max(1, safeRect.width())
    val height = max(1, safeRect.height())

    return runCatching {
        Bitmap.createBitmap(sourceBitmap, safeRect.left, safeRect.top, width, height)
    }.getOrNull()
}

private fun applyExportSettings(
    croppedBitmap: Bitmap,
    exportSettings: ExportSettings
): Bitmap {
    val targetWidth = exportSettings.widthPx ?: croppedBitmap.width
    val targetHeight = exportSettings.heightPx ?: croppedBitmap.height

    if (targetWidth == croppedBitmap.width && targetHeight == croppedBitmap.height) {
        return croppedBitmap
    }

    val sourceWidth = croppedBitmap.width.toFloat()
    val sourceHeight = croppedBitmap.height.toFloat()
    val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
    val sourceRatio = sourceWidth / sourceHeight

    // 先在粗裁剪结果上按“等比例铺满”计算，再依据对齐方式做二次精准裁剪。
    val sourceCropRect = if (sourceRatio > targetRatio) {
        val cropWidth = sourceHeight * targetRatio
        val cropLeft = alignedOffset(
            containerSize = sourceWidth,
            contentSize = cropWidth,
            alignment = exportSettings.horizontalAlignment
        )
        RectF(cropLeft, 0f, cropLeft + cropWidth, sourceHeight)
    } else {
        val cropHeight = sourceWidth / targetRatio
        val cropTop = alignedOffset(
            containerSize = sourceHeight,
            contentSize = cropHeight,
            alignment = exportSettings.verticalAlignment
        )
        RectF(0f, cropTop, sourceWidth, cropTop + cropHeight)
    }

    val safeLeft = sourceCropRect.left.roundToInt().coerceIn(0, croppedBitmap.width - 1)
    val safeTop = sourceCropRect.top.roundToInt().coerceIn(0, croppedBitmap.height - 1)
    val safeRight = sourceCropRect.right.roundToInt().coerceIn(safeLeft + 1, croppedBitmap.width)
    val safeBottom = sourceCropRect.bottom.roundToInt().coerceIn(safeTop + 1, croppedBitmap.height)
    val safeCropWidth = max(1, safeRight - safeLeft)
    val safeCropHeight = max(1, safeBottom - safeTop)

    val croppedForTarget = runCatching {
        Bitmap.createBitmap(croppedBitmap, safeLeft, safeTop, safeCropWidth, safeCropHeight)
    }.getOrNull() ?: croppedBitmap

    val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(outputBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val destination = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
    canvas.drawBitmap(croppedForTarget, null, destination, paint)
    return outputBitmap
}

private fun alignedOffset(
    containerSize: Float,
    contentSize: Float,
    alignment: HorizontalAlignment
): Float {
    return when (alignment) {
        HorizontalAlignment.START -> 0f
        HorizontalAlignment.CENTER -> (containerSize - contentSize) / 2f
        HorizontalAlignment.END -> containerSize - contentSize
    }.coerceIn(0f, max(0f, containerSize - contentSize))
}

private fun alignedOffset(
    containerSize: Float,
    contentSize: Float,
    alignment: VerticalAlignment
): Float {
    return when (alignment) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (containerSize - contentSize) / 2f
        VerticalAlignment.BOTTOM -> containerSize - contentSize
    }.coerceIn(0f, max(0f, containerSize - contentSize))
}

private fun saveToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri? {
    val cacheFolder = File(context.cacheDir, "image_edit_kit").apply {
        mkdirs()
    }

    val file = File(cacheFolder, buildExportFileName())

    val saved = runCatching {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }.isSuccess

    if (!saved) {
        return null
    }

    val authority = "${context.packageName}.androidimageeditkit.fileprovider"
    return runCatching {
        FileProvider.getUriForFile(context, authority, file)
    }.getOrNull()
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
    val resolver = context.contentResolver
    val fileName = buildExportFileName()

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AndroidImageEditKit")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val uri = resolver.insert(collection, values) ?: return null

    val saved = runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: return null
    }.isSuccess

    if (!saved) {
        resolver.delete(uri, null, null)
        return null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val done = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, done, null, null)
    }

    return uri
}

private fun saveBitmapToUri(
    context: Context,
    bitmap: Bitmap,
    outputUri: Uri
): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } ?: false
    }.getOrDefault(false)
}

private fun buildExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return "edited_$timestamp.png"
}

package org.photocardlibre.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.AlbumViewModel
import org.photocardlibre.app.R
import org.photocardlibre.app.export.AutomaticSaveResult
import org.photocardlibre.app.export.PdfExport
import org.photocardlibre.app.model.PhotoEntry
import org.photocardlibre.app.model.CropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AlbumScreen(viewModel: AlbumViewModel) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val activity = LocalActivity.current

    BackHandler(enabled = destination != AppDestination.HOME) {
        destination = when (destination) {
            AppDestination.INFO -> AppDestination.SETTINGS
            AppDestination.LANGUAGE -> AppDestination.SETTINGS
            AppDestination.SETTINGS -> AppDestination.HOME
            AppDestination.HOME -> AppDestination.HOME
        }
    }

    when (destination) {
        AppDestination.HOME -> AlbumHomeScreen(
            viewModel = viewModel,
            onOpenSettings = { destination = AppDestination.SETTINGS },
        )
        AppDestination.SETTINGS -> SettingsScreen(
            selectedLanguage = viewModel.state.appLanguage,
            onOpenLanguage = { destination = AppDestination.LANGUAGE },
            selectedImageSize = viewModel.state.pdfImageSize,
            onImageSizeSelected = viewModel::selectPdfImageSize,
            selectedCaptionSize = viewModel.state.pdfCaptionSize,
            onCaptionSizeSelected = viewModel::selectPdfCaptionSize,
            onBack = { destination = AppDestination.HOME },
            onOpenInfo = { destination = AppDestination.INFO },
        )
        AppDestination.INFO -> InfoAppScreen(
            onBack = { destination = AppDestination.SETTINGS },
        )
        AppDestination.LANGUAGE -> LanguageScreen(
            selectedLanguage = viewModel.state.appLanguage,
            onLanguageSelected = { language ->
                viewModel.selectAppLanguage(language) { activity?.recreate() }
            },
            onBack = { destination = AppDestination.SETTINGS },
        )
    }
}

private enum class AppDestination {
    HOME,
    SETTINGS,
    INFO,
    LANGUAGE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumHomeScreen(
    viewModel: AlbumViewModel,
    onOpenSettings: () -> Unit,
) {
    val state = viewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSafSource by rememberSaveable { mutableStateOf<String?>(null) }
    var handledPdfGenerationId by rememberSaveable { mutableLongStateOf(0L) }
    var editingPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPdfName by rememberSaveable { mutableStateOf<String?>(null) }
    var pdfNameError by rememberSaveable { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        viewModel.importUris(uris, context.getString(R.string.default_photo_name))
    }
    val savePdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destination ->
        val source = pendingSafSource
        pendingSafSource = null
        if (destination == null || source == null) {
            if (destination == null) viewModel.onPdfSaveCancelled()
            else viewModel.onPdfSaveFailed()
        } else {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    PdfExport.save(context, source, destination)
                }
                if (result.isSuccess) viewModel.onPdfSaved(destination, inDownloads = false)
                else viewModel.onPdfSaveFailed()
            }
        }
    }
    val addPhotos = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val saveGeneratedPdf: (String, String) -> Unit = { source, fileName ->
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfExport.saveAutomatically(context, source, fileName)
            }
            result.fold(
                onSuccess = { saved ->
                    when (saved) {
                        is AutomaticSaveResult.Saved -> {
                            viewModel.onPdfSaved(saved.uri, inDownloads = true)
                        }
                        AutomaticSaveResult.UseDocumentPicker -> {
                            pendingSafSource = source
                            savePdf.launch(fileName)
                        }
                    }
                },
                onFailure = { viewModel.onPdfSaveFailed() },
            )
        }
    }

    LaunchedEffect(state.pdfGenerationId) {
        if (state.pdfGenerationId == 0L || state.pdfGenerationId == handledPdfGenerationId) {
            return@LaunchedEffect
        }
        handledPdfGenerationId = state.pdfGenerationId
        if (state.pdfPath == null) return@LaunchedEffect
        pendingPdfName = PdfExport.automaticFileName()
        pdfNameError = null
    }

    val editingPhoto = editingPhotoId?.let { photoId ->
        state.album.photos.firstOrNull { it.id == photoId }
    }
    if (editingPhoto != null) {
        CropEditorScreen(
            photo = editingPhoto,
            onBack = { editingPhotoId = null },
            onConfirm = { crop ->
                viewModel.select(editingPhoto.id)
                viewModel.updateCrop(crop)
                editingPhotoId = null
            },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("apri_impostazioni"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.open_settings),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                state.userMessage?.let { message ->
                    Text(
                        stringResource(message.resource, *message.formatArgs.toTypedArray()),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (state.hasSavedPdfActions) {
                    val savedUri = Uri.parse(state.savedPdfUri)
                    SavedPdfActions(
                        onOpen = {
                            val result = PdfExport.open(context, savedUri)
                            if (result.isFailure) {
                                viewModel.showMessage(R.string.message_pdf_open_failed)
                            }
                        },
                        onShare = {
                            val result = PdfExport.share(context, savedUri)
                            if (result.isFailure) {
                                viewModel.showMessage(R.string.message_pdf_share_failed)
                            }
                        },
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("azioni_principali"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val hasPhotos = state.album.photos.isNotEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = addPhotos,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_add_photo), contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_add_photos), maxLines = 2,
                                textAlign = TextAlign.Center)
                        }
                        TextButton(
                            onClick = viewModel::preview,
                            enabled = hasPhotos,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_visibility), contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_preview), maxLines = 2,
                                textAlign = TextAlign.Center)
                        }
                    }
                    Button(
                        onClick = viewModel::createPdf,
                        enabled = hasPhotos,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_picture_as_pdf), contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_create_pdf))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PhotoCardWordmark()
                Spacer(Modifier.height(4.dp))
                Text(
                    pluralStringResource(
                        R.plurals.photo_count,
                        state.album.photos.size,
                        state.album.photos.size,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.album.photos.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_add_photo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                stringResource(R.string.empty_album_hint),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        items(state.album.photos.size) { index ->
                            val photo = state.album.photos[index]
                            PhotoThumbnail(
                                photo = photo,
                                selected = photo.id == state.album.selectedId,
                                onClick = { viewModel.select(photo.id) },
                                modifier = Modifier.width(132.dp),
                            )
                        }
                    }
                }
            }
            state.album.selectedPhoto?.let { selected ->
                item {
                    SelectedPhotoEditor(
                        photo = selected,
                        canMoveBefore = state.album.photos.first().id != selected.id,
                        canMoveAfter = state.album.photos.last().id != selected.id,
                        onCaptionChange = viewModel::updateCaption,
                        onMoveBefore = { viewModel.move(-1) },
                        onMoveAfter = { viewModel.move(1) },
                        onDelete = viewModel::deleteSelected,
                        onEdit = { editingPhotoId = selected.id },
                    )
                }
            }
            state.pdfPath?.let {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        ),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                pluralStringResource(
                                    R.plurals.pdf_ready,
                                    state.pdfPageCount,
                                    state.pdfPageCount,
                                ),
                                fontWeight = FontWeight.Bold,
                            )
                            if (state.pdfSaving) {
                                Text(stringResource(R.string.pdf_saving))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (state.busyMessage != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(state.busyMessage)) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
        )
    }
    if (state.previewVisible) {
        PreviewDialog(state.previewPaths, viewModel::closePreview)
    }
    pendingPdfName?.let { proposedName ->
        PdfNameDialog(
            name = proposedName,
            error = pdfNameError,
            onNameChange = {
                pendingPdfName = it
                pdfNameError = null
            },
            onCancel = {
                pendingPdfName = null
                pdfNameError = null
                viewModel.onPdfSaveCancelled()
            },
            onSave = {
                val normalizedName = PdfExport.normalizeFileName(proposedName)
                val source = state.pdfPath
                if (normalizedName == null) {
                    pdfNameError = context.getString(R.string.pdf_name_invalid)
                } else if (source == null) {
                    pendingPdfName = null
                    viewModel.onPdfSaveFailed()
                } else {
                    pendingPdfName = null
                    pdfNameError = null
                    saveGeneratedPdf(source, normalizedName)
                }
            },
        )
    }
}

@Composable
internal fun PdfNameDialog(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.pdf_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.file_name_label)) },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth().testTag("nome_file_pdf"),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(R.string.save)) } },
    )
}

@Composable
internal fun SavedPdfActions(onOpen: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .testTag("azioni_pdf_salvato"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onOpen, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.open_pdf))
        }
        TextButton(onClick = onShare, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.share_pdf))
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: PhotoEntry, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface,
        ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        },
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                LocalImage(
                    photo.localPath,
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    stringResource(R.string.photo_thumbnail_description, photo.displayName),
                    photo.crop,
                    maximumSide = 512,
                )
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = stringResource(R.string.selected_photo_description),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                    )
                }
            }
            Text(
                photo.caption.uppercase(Locale.ROOT),
                maxLines = 2,
                minLines = 2,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun SelectedPhotoEditor(
    photo: PhotoEntry,
    canMoveBefore: Boolean = true,
    canMoveAfter: Boolean = true,
    onCaptionChange: (String) -> Unit,
    onMoveBefore: () -> Unit,
    onMoveAfter: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.selected_photo_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LocalImage(
                photo.localPath,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                stringResource(R.string.selected_photo_description),
                photo.crop,
            )
            FilledTonalButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_photo))
            }
            OutlinedTextField(
                value = photo.caption,
                onValueChange = onCaptionChange,
                label = { Text(stringResource(R.string.caption_label)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary,
                ),
                supportingText = { Text(stringResource(R.string.caption_support)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMoveBefore,
                    enabled = canMoveBefore,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.move_before))
                }
                OutlinedButton(
                    onClick = onMoveAfter,
                    enabled = canMoveAfter,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.move_after))
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun LocalImage(
    path: String,
    modifier: Modifier,
    description: String,
    crop: CropRect? = null,
    maximumSide: Int = 1024,
) {
    val image by key(path, crop, maximumSide) {
        produceState<ImageLoadState>(ImageLoadState.Loading) {
            val bitmap = withContext(Dispatchers.IO) {
                decodeOrientedBitmap(path, maximumSide)?.let { cropBitmap(it, crop) }
            }
            value = if (bitmap == null) ImageLoadState.Failed
            else ImageLoadState.Ready(bitmap.asImageBitmap())
        }
    }
    when (val current = image) {
        is ImageLoadState.Ready ->
            Image(current.bitmap, description, modifier, contentScale = ContentScale.Fit)
        ImageLoadState.Loading, ImageLoadState.Failed -> {
            Box(
                modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (current == ImageLoadState.Failed) {
                    Text(stringResource(R.string.image_unavailable))
                }
            }
        }
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data object Failed : ImageLoadState
    data class Ready(val bitmap: androidx.compose.ui.graphics.ImageBitmap) : ImageLoadState
}

@Composable
private fun PreviewDialog(paths: List<String>, onClose: () -> Unit) {
    val configuration = LocalConfiguration.current
    val previewWidth = minOf(
        260.dp,
        (configuration.screenWidthDp - 96).coerceAtLeast(120).dp,
        (configuration.screenHeightDp - 220).coerceAtLeast(120).dp * (210f / 297f),
    )
    AlertDialog(
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        confirmButton = {
            Button(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
        title = {
            Text(
                stringResource(R.string.preview_a4_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(paths.size) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            LocalImage(
                                paths[index],
                                Modifier.width(previewWidth).aspectRatio(210f / 297f),
                                stringResource(R.string.preview_page_description, index + 1),
                            )
                        }
                        Text(
                            stringResource(R.string.page_position, index + 1, paths.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        },
    )
}

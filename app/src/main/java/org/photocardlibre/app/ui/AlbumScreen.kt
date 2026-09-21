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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.photocardlibre.app.AlbumViewModel
import org.photocardlibre.app.R
import org.photocardlibre.app.export.AutomaticSaveResult
import org.photocardlibre.app.export.PdfExport
import org.photocardlibre.app.model.CropRect
import org.photocardlibre.app.model.PhotoEntry
import java.util.Locale

private enum class AppDestination { ALBUM, SETTINGS, INFO, LANGUAGE }
private enum class WorkspacePage { ALBUM, PHOTO, PREVIEW, RESULT }

@Composable
fun AlbumScreen(viewModel: AlbumViewModel) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.ALBUM) }
    val activity = LocalActivity.current
    BackHandler(enabled = destination != AppDestination.ALBUM) {
        destination = when (destination) {
            AppDestination.INFO, AppDestination.LANGUAGE -> AppDestination.SETTINGS
            else -> AppDestination.ALBUM
        }
    }
    when (destination) {
        AppDestination.ALBUM -> AlbumWorkspace(viewModel) { destination = AppDestination.SETTINGS }
        AppDestination.SETTINGS -> SettingsScreen(
            selectedLanguage = viewModel.state.appLanguage,
            onOpenLanguage = { destination = AppDestination.LANGUAGE },
            selectedImageSize = viewModel.state.pdfImageSize,
            onImageSizeSelected = viewModel::selectPdfImageSize,
            selectedCaptionSize = viewModel.state.pdfCaptionSize,
            onCaptionSizeSelected = viewModel::selectPdfCaptionSize,
            onBack = { destination = AppDestination.ALBUM },
            onOpenInfo = { destination = AppDestination.INFO },
        )
        AppDestination.INFO -> InfoAppScreen(onBack = { destination = AppDestination.SETTINGS })
        AppDestination.LANGUAGE -> LanguageScreen(
            selectedLanguage = viewModel.state.appLanguage,
            onLanguageSelected = { viewModel.selectAppLanguage(it) { activity?.recreate() } },
            onBack = { destination = AppDestination.SETTINGS },
        )
    }
}

@Composable
private fun AlbumWorkspace(viewModel: AlbumViewModel, onOpenSettings: () -> Unit) {
    val state = viewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(WorkspacePage.ALBUM) }
    var cropPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSafSource by rememberSaveable { mutableStateOf<String?>(null) }
    var handledPdfGenerationId by rememberSaveable { mutableLongStateOf(state.pdfGenerationId) }
    var handledSavedUri by rememberSaveable { mutableStateOf(state.savedPdfUri) }
    var pendingPdfName by rememberSaveable { mutableStateOf<String?>(null) }
    var pdfNameError by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris -> viewModel.importUris(uris, context.getString(R.string.default_photo_name)) }
    val addPhotos = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val savePdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destination ->
        val source = pendingSafSource
        pendingSafSource = null
        if (destination == null || source == null) {
            if (destination == null) viewModel.onPdfSaveCancelled() else viewModel.onPdfSaveFailed()
        } else {
            scope.launch {
                val result = withContext(Dispatchers.IO) { PdfExport.save(context, source, destination) }
                if (result.isSuccess) viewModel.onPdfSaved(destination, inDownloads = false)
                else viewModel.onPdfSaveFailed()
            }
        }
    }
    val saveGeneratedPdf: (String, String) -> Unit = { source, fileName ->
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfExport.saveAutomatically(context, source, fileName)
            }
            result.fold(
                onSuccess = { saved ->
                    when (saved) {
                        is AutomaticSaveResult.Saved -> viewModel.onPdfSaved(saved.uri, inDownloads = true)
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

    LaunchedEffect(state.previewVisible) {
        if (state.previewVisible) page = WorkspacePage.PREVIEW
    }
    LaunchedEffect(state.pdfGenerationId) {
        if (state.pdfGenerationId != 0L && state.pdfGenerationId != handledPdfGenerationId) {
            handledPdfGenerationId = state.pdfGenerationId
            if (state.pdfPath != null) {
                pendingPdfName = PdfExport.automaticFileName()
                pdfNameError = null
            }
        }
    }
    LaunchedEffect(state.savedPdfUri) {
        if (state.hasSavedPdfActions && state.savedPdfUri != handledSavedUri) {
            handledSavedUri = state.savedPdfUri
            page = WorkspacePage.RESULT
        }
    }

    val cropPhoto = cropPhotoId?.let { id -> state.album.photos.firstOrNull { it.id == id } }
    if (cropPhoto != null) {
        CropEditorScreen(
            photo = cropPhoto,
            onBack = { cropPhotoId = null },
            onConfirm = { crop ->
                viewModel.select(cropPhoto.id)
                viewModel.updateCrop(crop)
                cropPhotoId = null
            },
        )
    } else {
        BackHandler(enabled = page != WorkspacePage.ALBUM && pendingPdfName == null) {
            if (page == WorkspacePage.PREVIEW) viewModel.closePreview()
            page = WorkspacePage.ALBUM
        }
        when (page) {
            WorkspacePage.ALBUM -> AlbumGallery(
                photos = state.album.photos,
                message = state.userMessage?.let { context.getString(it.resource, *it.formatArgs.toTypedArray()) },
                onAdd = addPhotos,
                onSettings = onOpenSettings,
                onPhoto = { id -> viewModel.select(id); page = WorkspacePage.PHOTO },
                onMove = viewModel::move,
                onPreview = viewModel::preview,
            )
            WorkspacePage.PHOTO -> {
                val selected = state.album.selectedPhoto
                if (selected == null) {
                    LaunchedEffect(Unit) { page = WorkspacePage.ALBUM }
                } else {
                    val selectedIndex = state.album.photos.indexOfFirst { it.id == selected.id }
                    PhotoDetailScreen(
                        photo = selected,
                        position = selectedIndex + 1,
                        total = state.album.photos.size,
                        onBack = { page = WorkspacePage.ALBUM },
                        onCaptionChange = viewModel::updateCaption,
                        onCrop = { cropPhotoId = selected.id },
                        onDelete = { viewModel.deleteSelected(); page = WorkspacePage.ALBUM },
                        message = state.userMessage?.let { context.getString(it.resource, *it.formatArgs.toTypedArray()) },
                    )
                }
            }
            WorkspacePage.PREVIEW -> PreviewScreen(
                paths = state.previewPaths,
                onBack = { viewModel.closePreview(); page = WorkspacePage.ALBUM },
                onCreatePdf = viewModel::createPdf,
                saving = state.pdfSaving,
                message = state.userMessage?.let { context.getString(it.resource, *it.formatArgs.toTypedArray()) },
            )
            WorkspacePage.RESULT -> PdfResultScreen(
                pageCount = state.pdfPageCount,
                message = state.userMessage?.let { context.getString(it.resource, *it.formatArgs.toTypedArray()) },
                onBack = { viewModel.closePreview(); page = WorkspacePage.ALBUM },
                onOpen = {
                    val uri = state.savedPdfUri?.let(Uri::parse) ?: return@PdfResultScreen
                    if (PdfExport.open(context, uri).isFailure) viewModel.showMessage(R.string.message_pdf_open_failed)
                },
                onShare = {
                    val uri = state.savedPdfUri?.let(Uri::parse) ?: return@PdfResultScreen
                    if (PdfExport.share(context, uri).isFailure) viewModel.showMessage(R.string.message_pdf_share_failed)
                },
            )
        }
    }

    if (state.busyMessage != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(state.busyMessage)) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
        )
    }
    pendingPdfName?.let { proposedName ->
        PdfNameDialog(
            name = proposedName,
            error = pdfNameError,
            onNameChange = { pendingPdfName = it; pdfNameError = null },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumGallery(
    photos: List<PhotoEntry>, message: String?,
    onAdd: () -> Unit, onSettings: () -> Unit, onPhoto: (String) -> Unit,
    onMove: (String, Int) -> Unit, onPreview: () -> Unit,
) {
    val hasPhotos = photos.isNotEmpty()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { PhotoCardWordmark(style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSettings, modifier = Modifier.testTag("apri_impostazioni")) {
                        Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.open_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (hasPhotos) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        Button(onClick = onPreview, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("anteprima_album")) {
                            Icon(painterResource(R.drawable.ic_visibility), null)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.action_preview))
                        }
                    }
                }
            }
        },
    ) { insets ->
        if (!hasPhotos) {
            Column(
                Modifier.fillMaxSize().padding(insets).verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(24.dp).testTag("album_vuoto"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(144.dp).clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_add_photo), null,
                        Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(28.dp))
                Text(stringResource(R.string.empty_album_title), style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.app_description), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("aggiungi_foto")) {
                    Icon(painterResource(R.drawable.ic_add_photo), null)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.action_add_photos))
                }
                MessageText(message)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.fillMaxSize().padding(insets).testTag("griglia_album"),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(stringResource(R.string.album_heading), style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(pluralStringResource(R.plurals.photo_count, photos.size, photos.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_edit), null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Text(stringResource(R.string.album_tap_to_edit),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (photos.size > 1) {
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.reorder_heading),
                                style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.album_reorder_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp).testTag("aggiungi_foto")) {
                            Icon(painterResource(R.drawable.ic_add_photo), null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_add_photos))
                        }
                        MessageText(message)
                    }
                }
                gridItemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                    AlbumTile(
                        photo = photo,
                        position = index + 1,
                        total = photos.size,
                        previousPhoto = photos.getOrNull(index - 1),
                        nextPhoto = photos.getOrNull(index + 1),
                        onClick = { onPhoto(photo.id) },
                        onMoveBefore = { onMove(photo.id, -1) },
                        onMoveAfter = { onMove(photo.id, 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumTile(
    photo: PhotoEntry, position: Int, total: Int,
    previousPhoto: PhotoEntry?, nextPhoto: PhotoEntry?,
    onClick: () -> Unit, onMoveBefore: () -> Unit, onMoveAfter: () -> Unit,
) {
    val beforeDescription = previousPhoto?.let {
        stringResource(R.string.move_before_target, position - 1, it.caption.ifBlank { it.displayName })
    } ?: stringResource(R.string.move_before_short)
    val afterDescription = nextPhoto?.let {
        stringResource(R.string.move_after_target, position + 1, it.caption.ifBlank { it.displayName })
    } ?: stringResource(R.string.move_after_short)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("foto_${photo.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                LocalImage(photo.localPath, Modifier.fillMaxSize(),
                    stringResource(R.string.photo_thumbnail_description, photo.displayName), photo.crop, 512)
                Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface) {
                    Text(position.toString(), Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(photo.caption.ifBlank { photo.displayName }.uppercase(Locale.ROOT),
                    Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    maxLines = 2, minLines = 2, textAlign = TextAlign.Center)
                Icon(painterResource(R.drawable.ic_edit), null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary)
            }
            if (total > 1) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = onMoveBefore, enabled = previousPhoto != null,
                        modifier = Modifier.size(48.dp)
                            .testTag("sposta_${photo.id}_prima")
                            .semantics { contentDescription = beforeDescription }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), null)
                    }
                    IconButton(onClick = onMoveAfter, enabled = nextPhoto != null,
                        modifier = Modifier.size(48.dp)
                            .testTag("sposta_${photo.id}_dopo")
                            .semantics { contentDescription = afterDescription }) {
                        Icon(painterResource(R.drawable.ic_arrow_forward), null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoDetailScreen(
    photo: PhotoEntry, position: Int, total: Int,
    onBack: () -> Unit, onCaptionChange: (String) -> Unit,
    onCrop: () -> Unit, onDelete: () -> Unit, message: String?,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WorkspaceTopBar(photo.caption.ifBlank { photo.displayName }, onBack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Button(onClick = onBack, Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(16.dp).heightIn(min = 56.dp)) { Text(stringResource(R.string.done)) }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(stringResource(R.string.photo_position, position, total),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            LocalImage(photo.localPath, Modifier.fillMaxWidth().heightIn(min = 200.dp).aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant),
                stringResource(R.string.selected_photo_description), photo.crop)
            SelectedPhotoEditor(photo, onCaptionChange, onDelete, onCrop)
            MessageText(message)
        }
    }
}

@Composable
internal fun SelectedPhotoEditor(
    photo: PhotoEntry, onCaptionChange: (String) -> Unit,
    onDelete: () -> Unit, onEdit: () -> Unit,
) {
    var confirmDeletion by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = photo.caption, onValueChange = onCaptionChange,
            label = { Text(stringResource(R.string.caption_label)) },
            supportingText = { Text(stringResource(R.string.caption_support)) },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("didascalia_foto"),
        )
        FilledTonalButton(onClick = onEdit, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(painterResource(R.drawable.ic_edit), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.aspect_ratio))
        }
        TextButton(onClick = { confirmDeletion = true },
            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).testTag("elimina_foto")) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmDeletion) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
            title = { Text(stringResource(R.string.delete_photo_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_photo_confirmation_message,
                photo.caption.ifBlank { photo.displayName })) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeletion = false
                    onDelete()
                }, modifier = Modifier.testTag("conferma_elimina_foto")) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletion = false },
                    modifier = Modifier.testTag("annulla_elimina_foto")) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewScreen(paths: List<String>, onBack: () -> Unit, onCreatePdf: () -> Unit,
    saving: Boolean, message: String?) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        topBar = { WorkspaceTopBar(stringResource(R.string.preview_a4_title), onBack,
            MaterialTheme.colorScheme.surfaceVariant) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    MessageText(message)
                    if (saving) {
                        Text(stringResource(R.string.pdf_saving), modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            textAlign = TextAlign.Center)
                    }
                    Button(onClick = onCreatePdf, enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .testTag("crea_pdf_anteprima")) {
                        Icon(painterResource(R.drawable.ic_picture_as_pdf), null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_create_pdf))
                    }
                }
            }
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
            val previewWidth = minOf(maxWidth - 64.dp,
                (maxHeight - 72.dp).coerceAtLeast(48.dp) * (210f / 297f), 420.dp)
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                itemsIndexed(paths) { index, path ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            LocalImage(path, Modifier.width(previewWidth).aspectRatio(210f / 297f),
                                stringResource(R.string.preview_page_description, index + 1))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.page_position, index + 1, paths.size),
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfResultScreen(pageCount: Int, message: String?, onBack: () -> Unit,
    onOpen: () -> Unit, onShare: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WorkspaceTopBar(stringResource(R.string.pdf_ready_title), onBack) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(136.dp).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check_circle), null, Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(24.dp))
            Text(pluralStringResource(R.plurals.pdf_ready, pageCount, pageCount),
                style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            MessageText(message)
            Spacer(Modifier.height(24.dp))
            SavedPdfActions(onOpen, onShare)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.back_to_album))
            }
        }
    }
}

@Composable
internal fun SavedPdfActions(onOpen: () -> Unit, onShare: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("azioni_pdf_salvato"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.open_pdf))
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.share_pdf))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceTopBar(title: String, onBack: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
    )
}

@Composable
private fun MessageText(message: String?) {
    if (message != null) {
        Text(message, Modifier.fillMaxWidth().padding(top = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun PdfNameDialog(name: String, error: String?, onNameChange: (String) -> Unit,
    onCancel: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.pdf_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = onNameChange, singleLine = true,
                    label = { Text(stringResource(R.string.file_name_label)) }, isError = error != null,
                    modifier = Modifier.fillMaxWidth().testTag("nome_file_pdf"))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(R.string.save)) } },
    )
}

@Composable
private fun LocalImage(path: String, modifier: Modifier, description: String,
    crop: CropRect? = null, maximumSide: Int = 1024) {
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
        is ImageLoadState.Ready -> Image(current.bitmap, description, modifier,
            contentScale = ContentScale.Fit)
        ImageLoadState.Loading, ImageLoadState.Failed -> Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { if (current == ImageLoadState.Failed) Text(stringResource(R.string.image_unavailable)) }
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data object Failed : ImageLoadState
    data class Ready(val bitmap: androidx.compose.ui.graphics.ImageBitmap) : ImageLoadState
}

package org.photocardlibre.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    BackHandler(enabled = destination != AppDestination.HOME) {
        destination = when (destination) {
            AppDestination.INFO -> AppDestination.SETTINGS
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
            onBack = { destination = AppDestination.HOME },
            onOpenInfo = { destination = AppDestination.INFO },
        )
        AppDestination.INFO -> InfoAppScreen(
            onBack = { destination = AppDestination.SETTINGS },
        )
    }
}

private enum class AppDestination {
    HOME,
    SETTINGS,
    INFO,
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
    var handledPdfGenerationId by rememberSaveable { mutableStateOf(0L) }
    var editingPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPdfName by rememberSaveable { mutableStateOf<String?>(null) }
    var pdfNameError by rememberSaveable { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
        viewModel::importUris,
    )
    val savePdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destination ->
        val source = pendingSafSource
        pendingSafSource = null
        if (destination == null || source == null) {
            viewModel.onPdfSaveFailed()
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
                if (state.hasSavedPdfActions) {
                    val savedUri = Uri.parse(state.savedPdfUri)
                    SavedPdfActions(
                        onOpen = {
                            val result = PdfExport.open(context, savedUri)
                            if (result.isFailure) {
                                viewModel.showMessage("Non è stato possibile aprire il PDF.")
                            }
                        },
                        onShare = {
                            val result = PdfExport.share(context, savedUri)
                            if (result.isFailure) {
                                viewModel.showMessage("Non è stato possibile condividere il PDF.")
                            }
                        },
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("azioni_principali"),
                ) {
                    val compactActions = maxWidth < 390.dp
                    val actionIconSize = if (compactActions) 18.dp else 20.dp
                    val actionSpacing = if (compactActions) 4.dp else 6.dp
                    val actionPadding = PaddingValues(
                        horizontal = if (compactActions) 4.dp else 8.dp,
                        vertical = 8.dp,
                    )
                    val actionTextStyle = MaterialTheme.typography.labelLarge.copy(
                        fontSize = if (compactActions) 12.sp else 14.sp,
                        lineHeight = if (compactActions) 14.sp else 18.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = addPhotos,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            contentPadding = actionPadding,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_add_photo),
                                contentDescription = null,
                                modifier = Modifier.size(actionIconSize),
                            )
                            Spacer(Modifier.width(actionSpacing))
                            Text(
                                "AGGIUNGI FOTO",
                                style = actionTextStyle,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::preview,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = actionPadding,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_visibility),
                                contentDescription = null,
                                modifier = Modifier.size(actionIconSize),
                            )
                            Spacer(Modifier.width(actionSpacing))
                            Text(
                                "ANTEPRIMA",
                                style = actionTextStyle,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                        Button(
                            onClick = viewModel::createPdf,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            contentPadding = actionPadding,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_picture_as_pdf),
                                contentDescription = null,
                                modifier = Modifier.size(actionIconSize),
                            )
                            Spacer(Modifier.width(actionSpacing))
                            Text(
                                "CREA PDF",
                                style = actionTextStyle,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
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
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onPrimaryContainer)) {
                            append("PhotoCard")
                        }
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                            append("Libre")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    if (state.album.photos.size == 1) "1 fotografia"
                    else "${state.album.photos.size} fotografie",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.album.photos.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Text("Premi AGGIUNGI FOTO per iniziare.", modifier = Modifier.padding(24.dp))
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        modifier = Modifier.fillMaxWidth().height(276.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.album.photos, key = { it.id }) { photo ->
                            PhotoThumbnail(
                                photo = photo,
                                selected = photo.id == state.album.selectedId,
                                onClick = { viewModel.select(photo.id) },
                            )
                        }
                    }
                }
            }
            state.album.selectedPhoto?.let { selected ->
                item {
                    SelectedPhotoEditor(
                        photo = selected,
                        onCaptionChange = viewModel::updateCaption,
                        onMoveBefore = { viewModel.move(-1) },
                        onMoveAfter = { viewModel.move(1) },
                        onDelete = viewModel::deleteSelected,
                        onEdit = { editingPhotoId = selected.id },
                    )
                }
            }
            state.userMessage?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            state.pdfPath?.let {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("PDF pronto (${state.pdfPageCount} pagine)", fontWeight = FontWeight.Bold)
                            if (state.pdfSaving) {
                                Text("Salvataggio PDF…")
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
            title = { Text(state.busyMessage) },
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
                    pdfNameError = "Inserisci un nome valido per il PDF."
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
        title = { Text("NOME FILE PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text("NOME FILE") },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth().testTag("nome_file_pdf"),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("ANNULLA") } },
        confirmButton = { Button(onClick = onSave) { Text("SALVA") } },
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
        Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
            Text("APRI PDF")
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Text("CONDIVIDI PDF")
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: PhotoEntry, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (selected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        },
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                LocalImage(
                    photo.localPath,
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    "Miniatura ${photo.displayName}",
                    photo.crop,
                )
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = "Fotografia selezionata",
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
    onCaptionChange: (String) -> Unit,
    onMoveBefore: () -> Unit,
    onMoveAfter: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "FOTOGRAFIA SELEZIONATA",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LocalImage(
                photo.localPath,
                Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                "Fotografia selezionata",
                photo.crop,
            )
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text("MODIFICA FOTO")
            }
            OutlinedTextField(
                value = photo.caption,
                onValueChange = onCaptionChange,
                label = { Text("DIDASCALIA (massimo 4 parole)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMoveBefore,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text("PRIMA")
                }
                OutlinedButton(
                    onClick = onMoveAfter,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text("DOPO")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text("ELIMINA")
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
) {
    val bitmap = remember(path, crop) {
        decodeOrientedBitmap(path, maximumSide = 1024)
            ?.let { cropBitmap(it, crop) }
            ?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap, description, modifier, contentScale = ContentScale.Fit)
    } else {
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("Immagine non disponibile")
        }
    }
}

@Composable
private fun PreviewDialog(paths: List<String>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { Button(onClick = onClose) { Text("CHIUDI") } },
        title = {
            Text(
                "ANTEPRIMA A4",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(paths.size) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LocalImage(
                            paths[index],
                            Modifier.width(260.dp).height(368.dp),
                            "Anteprima pagina ${index + 1}",
                        )
                        Text("Pagina ${index + 1} di ${paths.size}")
                    }
                }
            }
        },
    )
}

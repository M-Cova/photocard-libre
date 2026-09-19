package it.fotoalbum.spike.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.fotoalbum.spike.AlbumViewModel
import it.fotoalbum.spike.export.AutomaticSaveResult
import it.fotoalbum.spike.export.PdfExport
import it.fotoalbum.spike.model.PhotoEntry
import it.fotoalbum.spike.model.CropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlbumScreen(viewModel: AlbumViewModel) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("azioni_principali"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = addPhotos, modifier = Modifier.weight(1f)) {
                        Text("AGGIUNGI FOTO")
                    }
                    OutlinedButton(onClick = viewModel::preview, modifier = Modifier.weight(1f)) {
                        Text("ANTEPRIMA")
                    }
                    Button(onClick = viewModel::createPdf, modifier = Modifier.weight(1f)) {
                        Text("CREA PDF")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "FOTO ALBUM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("${state.album.photos.size} fotografie")
            }
            if (state.album.photos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("Premi AGGIUNGI FOTO per iniziare.", modifier = Modifier.padding(24.dp))
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    Card(modifier = Modifier.fillMaxWidth()) {
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
    Column(
        modifier = Modifier
            .width(110.dp)
            .then(
                if (selected) Modifier.border(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp),
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        LocalImage(
            photo.localPath,
            Modifier.fillMaxWidth().height(82.dp),
            "Miniatura ${photo.displayName}",
            photo.crop,
        )
        Text(
            photo.caption.ifEmpty { "SENZA DIDASCALIA" },
            maxLines = 2,
            style = MaterialTheme.typography.labelSmall,
        )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FOTOGRAFIA SELEZIONATA", fontWeight = FontWeight.Bold)
            LocalImage(
                photo.localPath,
                Modifier.fillMaxWidth().height(220.dp),
                "Fotografia selezionata",
                photo.crop,
            )
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("MODIFICA FOTO")
            }
            OutlinedTextField(
                value = photo.caption,
                onValueChange = onCaptionChange,
                label = { Text("DIDASCALIA (massimo 4 parole)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMoveBefore, modifier = Modifier.weight(1f)) {
                    Text("PRIMA")
                }
                OutlinedButton(onClick = onMoveAfter, modifier = Modifier.weight(1f)) {
                    Text("DOPO")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
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
        Box(modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Text("Immagine non disponibile")
        }
    }
}

@Composable
private fun PreviewDialog(paths: List<String>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("CHIUDI") } },
        title = { Text("ANTEPRIMA A4") },
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

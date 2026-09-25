package org.photocardlibre.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.PdfCaptionSize
import org.photocardlibre.app.settings.PdfImageSize
import org.photocardlibre.app.settings.MeasurementUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    selectedLanguage: AppLanguage,
    onOpenLanguage: () -> Unit,
    selectedMeasurementUnit: MeasurementUnit,
    onOpenMeasurementUnit: () -> Unit,
    selectedImageSize: PdfImageSize,
    onImageSizeSelected: (PdfImageSize) -> Unit,
    selectedCaptionSize: PdfCaptionSize,
    onCaptionSizeSelected: (PdfCaptionSize) -> Unit,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val displayLocale = LocalConfiguration.current.locales[0]

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("schermata_impostazioni"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            AppPanel {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language_section)) },
                    supportingContent = {
                        Text(
                            stringResource(selectedLanguage.labelResource),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Text(
                            "›",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(onClick = onOpenLanguage)
                        .testTag("apri_lingua"),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.measurement_unit)) },
                    supportingContent = {
                        Text(
                            stringResource(selectedMeasurementUnit.labelResource),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Text(
                            "›",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(onClick = onOpenMeasurementUnit)
                        .testTag("apri_unita_misura"),
                )
            }

            Spacer(Modifier.height(12.dp))
            SettingsSectionTitle(R.string.pdf_image_size_section)
            Text(
                text = stringResource(R.string.pdf_image_size_support),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            AppPanel {
                listOf(
                    PdfImageSize.CM_3,
                    PdfImageSize.CM_5,
                    PdfImageSize.CM_7,
                    PdfImageSize.CM_10,
                ).forEach { size ->
                    RadioSettingsOption(
                        label = selectedMeasurementUnit.format(size.centimeters, displayLocale),
                        selected = selectedImageSize == size,
                        onSelect = { onImageSizeSelected(size) },
                        radioTestTag = "pdf_size_${size.centimeters}",
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsSectionTitle(R.string.pdf_caption_size_section)
            AppPanel {
                listOf(
                    PdfCaptionSize.SMALL to R.string.caption_size_small,
                    PdfCaptionSize.MEDIUM to R.string.caption_size_medium,
                    PdfCaptionSize.LARGE to R.string.caption_size_large,
                ).forEach { (size, label) ->
                    RadioSettingsOption(
                        label = stringResource(label),
                        selected = selectedCaptionSize == size,
                        onSelect = { onCaptionSizeSelected(size) },
                        radioTestTag = "caption_size_${size.storageValue}",
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsSectionTitle(R.string.app_info_section)
            AppPanel {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_info_entry)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(onClick = onOpenInfo)
                        .testTag("apri_info_app"),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
    )
}

@Composable
private fun SettingsSectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioSettingsOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    radioTestTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surface,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = if (radioTestTag == null) Modifier else Modifier.testTag(radioTestTag),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun AppPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(content = content)
    }
}

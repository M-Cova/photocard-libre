package org.photocardlibre.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.PdfImageSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    selectedImageSize: PdfImageSize,
    onImageSizeSelected: (PdfImageSize) -> Unit,
    cuttingBorderEnabled: Boolean,
    onCuttingBorderToggled: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLanguage by rememberSaveable { mutableIntStateOf(R.string.language_system) }
    var selectedCaptionSize by rememberSaveable { mutableIntStateOf(R.string.caption_size_medium) }

    BackHandler(onBack = onBack)

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
            SettingsSectionTitle(R.string.language_section)
            RadioSettingsOption(
                label = R.string.language_system,
                selected = selectedLanguage == R.string.language_system,
                onSelect = { selectedLanguage = R.string.language_system },
            )
            RadioSettingsOption(
                label = R.string.language_italian,
                selected = selectedLanguage == R.string.language_italian,
                onSelect = { selectedLanguage = R.string.language_italian },
            )
            RadioSettingsOption(
                label = R.string.language_english,
                selected = selectedLanguage == R.string.language_english,
                onSelect = { selectedLanguage = R.string.language_english },
            )

            SettingsDivider()
            SettingsSectionTitle(R.string.pdf_image_size_section)
            Text(
                text = stringResource(R.string.pdf_image_size_support),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            listOf(
                PdfImageSize.CM_5 to R.string.pdf_size_5_cm,
                PdfImageSize.CM_7 to R.string.pdf_size_7_cm,
                PdfImageSize.CM_10 to R.string.pdf_size_10_cm,
            ).forEach { (size, label) ->
                RadioSettingsOption(
                    label = label,
                    selected = selectedImageSize == size,
                    onSelect = { onImageSizeSelected(size) },
                    radioTestTag = "pdf_size_${size.centimeters}",
                )
            }

            SettingsDivider()
            SettingsSectionTitle(R.string.pdf_caption_size_section)
            listOf(
                R.string.caption_size_small,
                R.string.caption_size_medium,
                R.string.caption_size_large,
            ).forEach { option ->
                RadioSettingsOption(
                    label = option,
                    selected = selectedCaptionSize == option,
                    onSelect = { selectedCaptionSize = option },
                )
            }

            SettingsDivider()
            SettingsSectionTitle(R.string.cutting_border_section)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(
                        role = Role.Switch,
                        onClick = { onCuttingBorderToggled(!cuttingBorderEnabled) },
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cutting_border_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = cuttingBorderEnabled,
                    onCheckedChange = onCuttingBorderToggled,
                )
            }

            SettingsDivider()
            SettingsSectionTitle(R.string.app_info_section)
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_info_entry)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onOpenInfo)
                    .testTag("apri_info_app"),
            )
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
    @StringRes label: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    radioTestTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = if (radioTestTag == null) Modifier else Modifier.testTag(radioTestTag),
        )
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

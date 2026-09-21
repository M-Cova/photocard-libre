package org.photocardlibre.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.AppLanguage

internal val AppLanguage.labelResource: Int
    @StringRes get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ITALIAN -> R.string.language_italian
        AppLanguage.ENGLISH -> R.string.language_english
        AppLanguage.SPANISH -> R.string.language_spanish
        AppLanguage.GERMAN -> R.string.language_german
        AppLanguage.FRENCH -> R.string.language_french
        AppLanguage.PORTUGUESE -> R.string.language_portuguese
    }

@Composable
internal fun LanguageScreen(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("schermata_lingua"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(title = stringResource(R.string.language_section), onBack = onBack)
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            AppLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.RadioButton) { onLanguageSelected(language) }
                        .semantics { selected = selectedLanguage == language }
                        .testTag("language_${language.storageValue}")
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selectedLanguage == language, onClick = null)
                    Text(
                        text = stringResource(language.labelResource),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

package org.photocardlibre.app.ui

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.AppLanguage
import java.util.Locale

internal val AppLanguage.labelResource: Int
    @StringRes get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ITALIAN -> R.string.language_italian
        AppLanguage.ENGLISH -> R.string.language_english
        AppLanguage.SPANISH -> R.string.language_spanish
        AppLanguage.GERMAN -> R.string.language_german
        AppLanguage.FRENCH -> R.string.language_french
        AppLanguage.PORTUGUESE -> R.string.language_portuguese
        AppLanguage.ARABIC -> R.string.language_arabic
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.language_simplified_chinese
        AppLanguage.JAPANESE -> R.string.language_japanese
        AppLanguage.HINDI -> R.string.language_hindi
        AppLanguage.INDONESIAN -> R.string.language_indonesian
    }

@Composable
internal fun AppLanguage.displayLabel(systemLocale: Locale): String =
    if (this == AppLanguage.SYSTEM) {
        "${stringResource(R.string.language_system)} · ${AppLanguage.systemLanguageNativeName(systemLocale)}"
    } else {
        stringResource(labelResource)
    }

@Composable
internal fun LanguageScreen(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val systemLocale = Resources.getSystem().configuration.locales[0]
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("schermata_lingua"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.language_section),
                onBack = onBack,
                titleIcon = R.drawable.ic_language,
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
                AppLanguage.entries.forEach { language ->
                    val selected = selectedLanguage == language
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surface,
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onLanguageSelected(language) },
                            )
                            .testTag("language_${language.storageValue}")
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        if (language == AppLanguage.SYSTEM) {
                            Icon(
                                painter = painterResource(R.drawable.ic_phone_android),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .testTag("system_language_icon"),
                            )
                        }
                        Text(
                            text = language.displayLabel(systemLocale),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(
                                start = if (language == AppLanguage.SYSTEM) 8.dp else 12.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

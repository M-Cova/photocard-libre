package org.photocardlibre.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.BuildConfig
import org.photocardlibre.app.R

@Composable
internal fun InfoAppScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("schermata_info_app"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.app_info_title),
                onBack = onBack,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(Modifier.padding(24.dp)) {
                    PhotoCardWordmark()
                    androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Text(
                text = stringResource(R.string.open_source_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            AppPanel(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                InfoItem(R.string.privacy_label, stringResource(R.string.privacy_description))
            }
            AppPanel(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                InfoItem(R.string.version_label, BuildConfig.VERSION_NAME)
                InfoDivider()
                InfoItem(R.string.software_license_label, stringResource(R.string.value_to_be_defined))
                InfoDivider()
                InfoItem(
                    R.string.graphic_resources_license_label,
                    stringResource(R.string.value_to_be_defined),
                )
                InfoDivider()
                InfoItem(R.string.source_repository_label, stringResource(R.string.value_to_be_defined))
                InfoDivider()
                InfoItem(R.string.report_issue_label, stringResource(R.string.value_to_be_defined))
                InfoDivider()
                InfoItem(R.string.credits_label, stringResource(R.string.value_to_be_defined))
            }
        }
    }
}

@Composable
private fun InfoItem(
    @StringRes label: Int,
    value: String,
) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        supportingContent = { Text(value) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    )
}

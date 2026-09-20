package org.photocardlibre.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            InfoItem(R.string.version_label, BuildConfig.VERSION_NAME)
            InfoItem(R.string.software_license_label, stringResource(R.string.value_to_be_defined))
            InfoItem(
                R.string.graphic_resources_license_label,
                stringResource(R.string.value_to_be_defined),
            )
            InfoItem(R.string.source_repository_label, stringResource(R.string.value_to_be_defined))
            InfoItem(R.string.report_issue_label, stringResource(R.string.value_to_be_defined))
            InfoItem(R.string.privacy_label, stringResource(R.string.privacy_description))
            InfoItem(R.string.credits_label, stringResource(R.string.value_to_be_defined))
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

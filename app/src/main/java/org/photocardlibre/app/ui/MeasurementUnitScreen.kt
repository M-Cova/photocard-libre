package org.photocardlibre.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.MeasurementUnit

internal val MeasurementUnit.labelResource: Int
    @StringRes get() = when (this) {
        MeasurementUnit.CENTIMETERS -> R.string.measurement_unit_centimeters
        MeasurementUnit.INCHES -> R.string.measurement_unit_inches
    }

@Composable
internal fun MeasurementUnitScreen(
    selectedUnit: MeasurementUnit,
    onUnitSelected: (MeasurementUnit) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("schermata_unita_misura"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(title = stringResource(R.string.measurement_unit), onBack = onBack)
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            AppPanel {
                MeasurementUnit.entries.forEach { unit ->
                    val selected = selectedUnit == unit
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
                                onClick = { onUnitSelected(unit) },
                            )
                            .testTag("measurement_unit_${unit.storageValue}")
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(
                            text = stringResource(unit.labelResource),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

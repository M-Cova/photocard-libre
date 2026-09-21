package org.photocardlibre.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.photocardlibre.app.R

@Composable
internal fun PhotoCardWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
) {
    val appName = stringResource(R.string.app_name)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onPrimaryContainer)) {
                append(appName.substringBeforeLast(' '))
            }
            append(" ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                append(appName.substringAfterLast(' '))
            }
        },
        style = style,
        modifier = modifier.testTag("photocard_libre_wordmark"),
    )
}

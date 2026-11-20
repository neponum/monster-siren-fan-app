package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite

/**
 * Shown once on first launch so the fan-made / non-official status is impossible to miss.
 */
@Composable
fun FanDisclaimerDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Must be acknowledged explicitly. */ },
        containerColor = MsrSurfaceElevated,
        title = {
            Text(
                text = stringResource(R.string.fan_disclaimer_title),
                style = MaterialTheme.typography.titleLarge,
                color = MsrWhite
            )
        },
        text = {
            Text(
                text = stringResource(R.string.fan_disclaimer_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MsrGrayLight
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(
                    text = stringResource(R.string.fan_disclaimer_understood),
                    style = MaterialTheme.typography.labelLarge,
                    color = MsrPureWhite
                )
            }
        }
    )
}

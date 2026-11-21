package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite

@Composable
fun WhatsNewDialog(
    version: String,
    notes: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MsrSurfaceElevated,
        title = {
            Text(
                text = stringResource(R.string.whats_new_title, version),
                style = MaterialTheme.typography.titleLarge,
                color = MsrWhite
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MsrGrayLight
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.whats_new_ok),
                    style = MaterialTheme.typography.labelLarge,
                    color = MsrPureWhite
                )
            }
        }
    )
}

package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.update.UpdateManager
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite
import com.example.ui.viewmodel.UpdateUiState

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val context = LocalContext.current
    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { onDownload() }

    val update = state.update
    when {
        update != null -> {
            AlertDialog(
                onDismissRequest = { if (!state.isDownloading) onDismiss() },
                containerColor = MsrSurfaceElevated,
                title = {
                    Text(
                        text = stringResource(R.string.update_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MsrWhite
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = stringResource(R.string.update_available, update.versionName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MsrGrayLight
                        )
                        Text(
                            text = stringResource(R.string.update_current, com.example.BuildConfig.VERSION_NAME),
                            style = MsrMonoMicro,
                            color = MsrGrayMuted
                        )
                        if (!update.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.update_release_notes),
                                style = MsrMonoMicro,
                                color = MsrGrayMuted
                            )
                            Text(
                                text = update.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MsrGrayLight
                            )
                        }
                        if (state.isDownloading) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.update_downloading, state.progress),
                                style = MsrMonoMicro,
                                color = MsrPureWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(color = MsrPureWhite)
                        }
                        if (state.error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !state.isDownloading,
                        onClick = {
                            if (!UpdateManager.canInstall(context)) {
                                installPermissionLauncher.launch(
                                    UpdateManager.unknownSourcesIntent(context)
                                )
                            } else {
                                onDownload()
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.update_download),
                            style = MaterialTheme.typography.labelLarge,
                            color = MsrPureWhite
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !state.isDownloading,
                        onClick = onDismiss
                    ) {
                        Text(
                            text = stringResource(R.string.update_later),
                            style = MaterialTheme.typography.labelLarge,
                            color = MsrGrayLight
                        )
                    }
                }
            )
        }

        state.infoMessage != null || state.error != null -> {
            AlertDialog(
                onDismissRequest = onDismissMessage,
                containerColor = MsrSurfaceElevated,
                title = {
                    Text(
                        text = stringResource(R.string.update_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MsrWhite
                    )
                },
                text = {
                    Text(
                        text = state.infoMessage ?: state.error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MsrGrayLight
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismissMessage) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge,
                            color = MsrPureWhite
                        )
                    }
                }
            )
        }
    }
}

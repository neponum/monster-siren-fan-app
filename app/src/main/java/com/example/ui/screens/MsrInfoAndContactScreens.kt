package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.MsrHalftoneBackground
import com.example.ui.components.MsrInfoNodesIcon
import com.example.ui.components.MsrSectionDividerWithHatches
import com.example.ui.components.MsrWaveNodeDivider
import com.example.ui.theme.MsrBackgroundDeep
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSectionHeading
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.CatalogUiState
import com.example.ui.viewmodel.NewsUiState
import com.example.ui.viewmodel.OfflineVaultUiState
import com.example.ui.viewmodel.UpdateUiState

/**
 * Official INFO screen ("INFO / Инфо"): live Monster Siren announcements feed + catalog sync & offline stats.
 */
@Composable
fun MsrInfoScreen(
    catalogState: CatalogUiState,
    offlineState: OfflineVaultUiState,
    newsState: NewsUiState,
    isMenuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onSyncCatalog: () -> Unit,
    onToggleOfflineOnly: () -> Unit,
    onRefreshNews: () -> Unit,
    onOpenNewsDetail: (String) -> Unit,
    onCloseNewsDetail: () -> Unit,
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDetail = newsState.selectedDetail
    if (selectedDetail != null) {
        val plainBody = remember(selectedDetail.content) {
            HtmlCompat.fromHtml(
                selectedDetail.content.orEmpty(),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            ).toString().trim()
        }
        AlertDialog(
            onDismissRequest = onCloseNewsDetail,
            containerColor = MsrSurfaceElevated,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedDetail.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MsrWhite
                    )
                    val meta = listOfNotNull(
                        selectedDetail.author?.takeIf { it.isNotBlank() },
                        selectedDetail.date.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MsrMonoMicro,
                            color = MsrGrayMuted
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = plainBody.ifEmpty { "Monster Siren Records" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MsrGrayLight
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onCloseNewsDetail) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge,
                        color = MsrPureWhite
                    )
                }
            }
        )
    }

    MsrHalftoneBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MsrSimpleSectionTopBar(
                isMenuOpen = isMenuOpen,
                onOpenMenu = onOpenMenu,
                tag = "info_open_menu_button"
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("catalog_info_list")
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            MsrInfoNodesIcon()
                            Text(
                                text = "INFO",
                                style = MsrSectionHeading,
                                color = MsrWhite
                            )
                        }
                        MsrSectionDividerWithHatches()
                    }
                }

                // Fan-made disclaimer: this app is not affiliated with Hypergryph / MSR
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MsrHairline.copy(alpha = 0.7f))
                            .background(MsrBackgroundDeep.copy(alpha = 0.65f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.fan_disclaimer_title),
                            style = MsrMonoMicro,
                            color = MsrPureWhite
                        )
                        Text(
                            text = stringResource(R.string.fan_disclaimer_short),
                            style = MaterialTheme.typography.bodySmall,
                            color = MsrGrayLight
                        )
                    }
                }

                // App version + self-updater
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MsrHairline.copy(alpha = 0.6f))
                            .background(MsrBackgroundDeep.copy(alpha = 0.45f))
                            .padding(14.dp)
                    ) {
                        MsrStatLine(
                            label = "APP VERSION",
                            value = BuildConfig.VERSION_NAME
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MsrHairline)
                                .background(MsrBackgroundDeep.copy(alpha = 0.55f))
                                .clickable(enabled = !updateState.isChecking) { onCheckUpdates() }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("info_check_updates_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = MsrWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.update_check),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MsrWhite
                                )
                            }
                            Text(
                                text = when {
                                    updateState.isChecking -> stringResource(R.string.update_checking)
                                    else -> updateState.update?.let { "NEW: ${it.versionName}" } ?: "OK"
                                },
                                style = MsrMonoMicro,
                                color = if (updateState.update != null) MsrPureWhite else MsrGrayMuted
                            )
                        }
                    }
                }

                // Catalog Sync & Offline Mode Controls
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MsrHairline)
                                .background(MsrBackgroundDeep.copy(alpha = 0.55f))
                                .clickable {
                                    onSyncCatalog()
                                    onRefreshNews()
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("info_sync_catalog_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = MsrWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "SYNC MONSTER SIREN CATALOG",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MsrWhite
                                )
                            }
                            Text(
                                text = when {
                                    catalogState.isSyncing -> "SYNCING…"
                                    catalogState.isNetworkConnected -> "ONLINE"
                                    else -> "OFFLINE"
                                },
                                style = MsrMonoMicro,
                                color = MsrGrayMuted
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (catalogState.offlineOnlyMode) MsrPureWhite else MsrHairline)
                                .background(MsrBackgroundDeep.copy(alpha = 0.55f))
                                .clickable(onClick = onToggleOfflineOnly)
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("info_offline_only_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "OFFLINE-ONLY PLAYBACK MODE",
                                style = MaterialTheme.typography.labelLarge,
                                color = MsrWhite
                            )
                            Text(
                                text = if (catalogState.offlineOnlyMode) "ACTIVE" else "OFF",
                                style = MsrMonoMicro,
                                color = if (catalogState.offlineOnlyMode) MsrPureWhite else MsrGrayMuted
                            )
                        }
                    }
                }

                // Local Library & Offline Storage Telemetry
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MsrHairline.copy(alpha = 0.6f))
                            .background(MsrBackgroundDeep.copy(alpha = 0.45f))
                            .padding(16.dp)
                    ) {
                        MsrStatLine(label = "ALBUMS INDEXED", value = "${catalogState.albums.size}")
                        MsrStatLine(label = "TRACKS INDEXED", value = "${catalogState.songs.size}")
                        MsrStatLine(label = "DOWNLOADED OFFLINE", value = "${offlineState.downloadedSongs.size}")
                        MsrStatLine(label = "FAVORITES", value = "${offlineState.favoriteSongs.size}")
                        MsrStatLine(
                            label = "OFFLINE STORAGE",
                            value = Formatters.formatFileSize(offlineState.totalOfflineBytes)
                        )
                    }
                }

                // Official Monster Siren News / Announcements (`动向`)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ANNOUNCEMENTS // 动向",
                                style = MaterialTheme.typography.titleMedium,
                                color = MsrWhite
                            )
                            if (newsState.isLoading || newsState.isLoadingDetail) {
                                CircularProgressIndicator(
                                    color = MsrWhite,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        MsrWaveNodeDivider()
                    }
                }

                if (newsState.items.isEmpty() && !newsState.isLoading) {
                    item {
                        Text(
                            text = "Подключитесь к интернету для загрузки новостей Monster Siren Records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MsrGrayMuted
                        )
                    }
                } else {
                    items(
                        items = newsState.items.take(20),
                        key = { "news_${it.cid}" }
                    ) { news ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MsrHairline.copy(alpha = 0.4f))
                                .background(MsrBackgroundDeep.copy(alpha = 0.45f))
                                .clickable { onOpenNewsDetail(news.cid) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("news_item_${news.cid}")
                        ) {
                            Text(
                                text = news.date,
                                style = MsrMonoMicro,
                                color = MsrGrayMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = news.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MsrWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Official CONTACT screen ("CONTACT / Контакты") matching Monster Siren Records official channels.
 */
@Composable
fun MsrContactScreen(
    isMenuOpen: Boolean,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val officialEmail = "monstersirenrecords@hypergryph.com"
    val copiedToastText = stringResource(R.string.contact_copied_email)

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    val sendEmail: () -> Unit = {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$officialEmail"))
            context.startActivity(intent)
        }
    }

    val copyEmail: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("MSR Email", officialEmail))
        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
    }

    MsrHalftoneBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MsrSimpleSectionTopBar(
                isMenuOpen = isMenuOpen,
                onOpenMenu = onOpenMenu,
                tag = "contact_open_menu_button"
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("contact_screen_list")
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MsrWhite,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "CONTACT",
                                style = MsrSectionHeading,
                                color = MsrWhite
                            )
                        }
                        MsrSectionDividerWithHatches()
                    }
                }

                // Official Brand Header Block
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MsrHairline.copy(alpha = 0.6f))
                            .background(MsrBackgroundDeep.copy(alpha = 0.55f))
                            .padding(18.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_msr_logo_mark),
                            contentDescription = "Monster Siren Records",
                            tint = MsrWhite,
                            modifier = Modifier.size(width = 72.dp, height = 36.dp)
                        )
                        Text(
                            text = "MONSTER SIREN RECORDS",
                            style = MaterialTheme.typography.titleLarge,
                            color = MsrWhite
                        )
                        Text(
                            text = "塞壬唱片-MSR · HYPERGRYPH",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MsrGrayLight
                        )
                        MsrWaveNodeDivider()
                        Text(
                            text = "A WORLD FAMILIARLY UNKNOWN / 一个已知或未知的世界",
                            style = MsrMonoMicro,
                            color = MsrGrayMuted
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.fan_disclaimer_short),
                        style = MsrMonoMicro,
                        color = MsrGrayMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Official Contact Email with Send + Copy actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "OFFICIAL EMAIL",
                            style = MsrMonoMicro,
                            color = MsrGrayMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MsrHairline)
                                .background(MsrBackgroundDeep.copy(alpha = 0.55f))
                                .clickable(onClick = sendEmail)
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("contact_email_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = officialEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MsrWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clickable(onClick = copyEmail)
                                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                    .testTag("contact_copy_email_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy email",
                                    tint = MsrGrayLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Official Monster Siren Platforms
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "OFFICIAL CHANNELS",
                            style = MsrMonoMicro,
                            color = MsrGrayMuted
                        )

                        MsrContactLinkRow(
                            title = "MONSTER SIREN WEBSITE",
                            subtitle = "monster-siren.hypergryph.com",
                            onClick = { openUrl("https://monster-siren.hypergryph.com") },
                            tag = "contact_link_website"
                        )
                        MsrContactLinkRow(
                            title = "NETEASE CLOUD MUSIC // 网易云音乐",
                            subtitle = "塞壬唱片-MSR",
                            onClick = { openUrl("https://music.163.com/#/artist?id=32540734") },
                            tag = "contact_link_netease"
                        )
                        MsrContactLinkRow(
                            title = "BILIBILI // 哔哩哔哩",
                            subtitle = "明日方舟 / 塞壬唱片-MSR",
                            onClick = { openUrl("https://space.bilibili.com/161775300") },
                            tag = "contact_link_bilibili"
                        )
                        MsrContactLinkRow(
                            title = "WEIBO // 微博",
                            subtitle = "@明日方舟Arknights",
                            onClick = { openUrl("https://www.weibo.com/arknights") },
                            tag = "contact_link_weibo"
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MsrContactLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MsrHairline.copy(alpha = 0.65f))
            .background(MsrBackgroundDeep.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = MsrGrayLight,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MsrWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MsrMonoMicro,
                    color = MsrGrayMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MsrGrayMuted,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun MsrStatLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MsrMonoMicro,
            color = MsrGrayMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MsrWhite
        )
    }
}

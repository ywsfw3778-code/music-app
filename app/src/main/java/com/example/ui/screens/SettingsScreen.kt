package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppStrings
import com.example.model.ArtworkBgMode
import com.example.model.ExclusionReason
import com.example.model.FolderItem
import com.example.model.Track
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDuration
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard
import com.example.ui.theme.waterGlassContent
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    strings: AppStrings,
    settings: AppSettings,
    activeCount: Int,
    excludedCount: Int,
    excludedTracks: List<Track>,
    folderItems: List<FolderItem> = emptyList(),
    hasNotificationPermission: Boolean,
    accentColor: Color,
    onLanguageChange: (AppLanguage) -> Unit,
    onCrossfadeChange: (Boolean, Int) -> Unit,
    onAutoExcludeRecordingsChange: (Boolean) -> Unit,
    onMinDurationChange: (Int) -> Unit,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onToggleExcludedFolder: (String) -> Unit = {},
    onSetAllFoldersExcluded: (Set<String>, Boolean) -> Unit = { _, _ -> },
    onDynamicAccentChange: (Boolean) -> Unit,
    onArtworkBgModeChange: (ArtworkBgMode) -> Unit,
    onGlassBlurChange: (Boolean) -> Unit,
    onRestoreTrack: (Track) -> Unit,
    onRescan: () -> Unit,
    onClearData: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean = true
) {
    var keywordInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExcludedPage by remember { mutableStateOf(false) }
    var showFoldersPage by remember { mutableStateOf(false) }
    val waterGlass = LocalWaterGlassState.current

    val hasSubPage = showExcludedPage || showFoldersPage

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .waterGlassContent(if (isPageActive && !hasSubPage) waterGlass else null)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 14.dp,
                    bottom = bottomPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.settingsTitle,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = strings.settingsSubtitle,
                            color = NocturneColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                item {
                    SettingsCard(
                        title = strings.languageSection,
                        icon = Icons.Rounded.Translate,
                        accentColor = accentColor
                    ) {
                        Text(strings.languageDesc, color = NocturneColors.TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        GlassSegmentedControl(
                            options = listOf(AppLanguage.AR to "العربية", AppLanguage.EN to "English"),
                            selected = settings.language,
                            accentColor = accentColor,
                            onSelect = onLanguageChange
                        )
                    }
                }

                item {
                    SettingsCard(
                        title = strings.crossfadeSection,
                        icon = Icons.Rounded.GraphicEq,
                        accentColor = accentColor
                    ) {
                        SettingsToggleRow(
                            title = strings.crossfadeToggle,
                            subtitle = strings.crossfadeDesc,
                            checked = settings.crossfadeEnabled,
                            accentColor = accentColor,
                            onCheckedChange = { onCrossfadeChange(it, settings.crossfadeSec) }
                        )
                        if (settings.crossfadeEnabled) {
                            GlassDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.crossfadeDuration, color = NocturneColors.TextSecondary, fontSize = 12.5.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(accentColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "${settings.crossfadeSec} ${strings.secondsUnit}",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            GlassSlider(
                                value = settings.crossfadeSec.toFloat(),
                                valueRange = 1f..12f,
                                accentColor = accentColor,
                                onValueChange = { onCrossfadeChange(true, it.roundToInt()) }
                            )
                        }
                    }
                }

                item {
                    SettingsCard(
                        title = strings.filterSection,
                        icon = Icons.Rounded.FilterAlt,
                        accentColor = accentColor
                    ) {
                        SettingsToggleRow(
                            title = strings.autoExcludeRecordings,
                            subtitle = strings.autoExcludeRecordingsDesc,
                            checked = settings.autoExcludeRecordings,
                            accentColor = accentColor,
                            onCheckedChange = onAutoExcludeRecordingsChange
                        )
                        GlassDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.minDurationLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(accentColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "${settings.minDurationSec} ${strings.secondsUnit}",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(strings.minDurationDesc, color = NocturneColors.TextSecondary, fontSize = 11.5.sp)
                        Spacer(Modifier.height(4.dp))
                        GlassSlider(
                            value = settings.minDurationSec.toFloat(),
                            valueRange = 0f..120f,
                            accentColor = accentColor,
                            onValueChange = { onMinDurationChange(it.roundToInt()) }
                        )

                        GlassDivider()
                        Text(strings.customKeywordsLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = keywordInput,
                                    onValueChange = { keywordInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = AppFontFamily),
                                    cursorBrush = SolidColor(accentColor),
                                    decorationBox = { inner ->
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                            if (keywordInput.isEmpty()) {
                                                Text(strings.customKeywordsPlaceholder, color = NocturneColors.TextMuted, fontSize = 12.sp)
                                            }
                                            inner()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .clickable {
                                        if (keywordInput.isNotBlank()) {
                                            onAddKeyword(keywordInput)
                                            keywordInput = ""
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (settings.customKeywords.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                settings.customKeywords.forEach { kw ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(kw, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Rounded.Close,
                                            null,
                                            tint = NocturneColors.TextSecondary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onRemoveKeyword(kw) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsCard(
                        title = strings.tabQuarantine,
                        icon = Icons.Rounded.Block,
                        accentColor = accentColor
                    ) {
                        Text(
                            text = strings.noQuarantineDesc,
                            color = NocturneColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                                .testTag("settings_open_excluded")
                                .clickable { showExcludedPage = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Block, null, tint = accentColor, modifier = Modifier.size(17.dp))
                                }
                                Column {
                                    Text(
                                        text = if (strings.isRtl) "فتح صفحة المستبعدات" else "Open Excluded Tracks",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${excludedTracks.size} ${strings.songsCount}",
                                        color = NocturneColors.TextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    SettingsCard(
                        title = strings.musicFoldersSection,
                        icon = Icons.Rounded.Folder,
                        accentColor = accentColor
                    ) {
                        Text(
                            text = strings.musicFoldersDesc,
                            color = NocturneColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                                .testTag("settings_open_folders")
                                .clickable { showFoldersPage = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Folder, null, tint = accentColor, modifier = Modifier.size(17.dp))
                                }
                                Column {
                                    Text(
                                        text = if (strings.isRtl) "إدارة مجلدات الموسيقى" else "Manage Music Folders",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (strings.isRtl) "${folderItems.count { !it.isExcluded }} مجلد مفعل من أصل ${folderItems.size}"
                                        else "${folderItems.count { !it.isExcluded }} active of ${folderItems.size} folders",
                                        color = NocturneColors.TextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    SettingsCard(
                        title = strings.appearanceSection,
                        icon = Icons.Rounded.Palette,
                        accentColor = accentColor
                    ) {
                        SettingsToggleRow(
                            title = strings.dynamicAccentLabel,
                            subtitle = strings.dynamicAccentDesc,
                            checked = settings.dynamicAccent,
                            accentColor = accentColor,
                            onCheckedChange = onDynamicAccentChange
                        )
                        GlassDivider()
                        SettingsToggleRow(
                            title = strings.glassBlurLabel,
                            subtitle = strings.glassBlurDesc,
                            checked = settings.glassBlurEnabled,
                            accentColor = accentColor,
                            onCheckedChange = onGlassBlurChange
                        )
                        GlassDivider()
                        Text(strings.artworkBgLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(strings.artworkBgDesc, color = NocturneColors.TextSecondary, fontSize = 11.5.sp)
                        Spacer(Modifier.height(10.dp))
                        GlassSegmentedControl(
                            options = listOf(
                                ArtworkBgMode.FULL to strings.artworkBgFull,
                                ArtworkBgMode.OFF to strings.artworkBgOff
                            ),
                            selected = if (settings.artworkBgMode == ArtworkBgMode.OFF) ArtworkBgMode.OFF else ArtworkBgMode.FULL,
                            accentColor = accentColor,
                            onSelect = onArtworkBgModeChange
                        )
                    }
                }

                item {
                    SettingsCard(
                        title = strings.storageSection,
                        icon = Icons.Rounded.Storage,
                        accentColor = accentColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Text("$activeCount", color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(strings.songsCount, color = NocturneColors.TextSecondary, fontSize = 11.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Text("$excludedCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(strings.filteredCount, color = NocturneColors.TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .testTag("settings_rescan_button")
                                    .clickable(onClick = onRescan),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.rescanFilters, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(NocturneColors.Danger.copy(alpha = 0.14f))
                                    .border(1.dp, NocturneColors.Danger.copy(alpha = 0.30f), RoundedCornerShape(50))
                                    .testTag("settings_clear_button")
                                    .clickable { showClearDialog = true },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null, tint = NocturneColors.Danger, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.clearLibrary, color = NocturneColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    SettingsCard(
                        title = strings.permissionsSection,
                        icon = Icons.Rounded.Notifications,
                        accentColor = accentColor
                    ) {
                        Text(strings.permissionsDesc, color = NocturneColors.TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        val statusColor = if (hasNotificationPermission) NocturneColors.Emerald else Color(0xFFF59E0B)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(statusColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (hasNotificationPermission) strings.permissionGranted else strings.permissionPending,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(accentColor.copy(alpha = 0.14f))
                                    .clickable(onClick = onOpenNotificationSettings)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(strings.openNotificationSettings, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showExcludedPage,
                enter = slideInHorizontally { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally { it } + fadeOut(tween(180))
            ) {
                ExcludedTracksPage(
                    excludedTracks = excludedTracks,
                    strings = strings,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onRestoreTrack = onRestoreTrack,
                    onClose = { showExcludedPage = false }
                )
            }

            AnimatedVisibility(
                visible = showFoldersPage,
                enter = slideInHorizontally { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally { it } + fadeOut(tween(180))
            ) {
                MusicFoldersPage(
                    folderItems = folderItems,
                    strings = strings,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onToggleExcludedFolder = onToggleExcludedFolder,
                    onSetAllFoldersExcluded = onSetAllFoldersExcluded,
                    onClose = { showFoldersPage = false }
                )
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = NocturneColors.MatteElevatedBg,
                title = { Text(strings.clearLibrary, color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text(strings.clearLibraryConfirm, color = NocturneColors.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearData()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NocturneColors.Danger, contentColor = Color.White)
                    ) {
                        Text(strings.confirmDelete, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(strings.cancel, color = NocturneColors.TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Text(title, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun GlassDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 1.dp,
        color = Color.White.copy(alpha = 0.07f)
    )
}

@Composable
private fun <T> GlassSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    accentColor: Color,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val active = selected == value
            val bgColor by animateColorAsState(
                targetValue = if (active) accentColor else Color.Transparent,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "segBg"
            )
            val txtColor by animateColorAsState(
                targetValue = if (active) Color.Black else NocturneColors.TextSecondary,
                animationSpec = tween(200),
                label = "segTxt"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = txtColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = NocturneColors.TextSecondary, fontSize = 11.5.sp)
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(checked = checked, accentColor = accentColor, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GlassSwitch(
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.White.copy(alpha = 0.14f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "switchThumb"
    )
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color.White)
        )
    }
}

@Composable
private fun GlassSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val span = (max - min).coerceAtLeast(1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(((value - min) / span).coerceIn(0f, 1f)) }
    val currentFraction = if (isDragging) dragFraction else ((value - min) / span).coerceIn(0f, 1f)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(min, max, isRtl) {
                detectTapGestures { offset ->
                    val raw = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val frac = if (isRtl) 1f - raw else raw
                    onValueChange(min + frac * span)
                }
            }
            .pointerInput(min, max, isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val raw = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = if (isRtl) 1f - raw else raw
                        onValueChange(min + dragFraction * span)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val raw = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = if (isRtl) 1f - raw else raw
                        onValueChange(min + dragFraction * span)
                    },
                    onDragEnd = {
                        onValueChange(min + dragFraction * span)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val thumbSizePx = with(LocalDensity.current) { 18.dp.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.65f), accentColor)
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .offset {
                    val x = (currentFraction * (trackWidthPx - thumbSizePx)).roundToInt()
                    IntOffset(x, 0)
                }
                .size(18.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun ExcludedTracksPage(
    excludedTracks: List<Track>,
    strings: AppStrings,
    accentColor: Color,
    bottomPadding: PaddingValues,
    onRestoreTrack: (Track) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(enabled = true, onBack = onClose)

    var searchQuery by remember { mutableStateOf("") }
    val filteredTracks = remember(excludedTracks, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) excludedTracks
        else excludedTracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }

    val glass = LocalWaterGlassState.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (glass != null) {
                    drawLayer(if (glass.blurEnabled) glass.blurredBgLayer else glass.bgLayer)
                    drawRect(Color.Black.copy(alpha = 0.45f))
                } else {
                    drawRect(NocturneColors.Black)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.tabQuarantine,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${excludedTracks.size} ${strings.filteredCount}",
                        color = NocturneColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = NocturneColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontFamily = AppFontFamily),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchPlaceholder,
                                    color = NocturneColors.TextSecondary,
                                    fontSize = 12.5.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = NocturneColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (excludedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding.calculateBottomPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .matteCard(RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(20.dp))
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = strings.noQuarantineTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.noQuarantineDesc,
                            color = NocturneColors.TextSecondary,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noSearchResults,
                        color = NocturneColors.TextSecondary,
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTracks, key = { it.id }, contentType = { "excluded_track" }) { track ->
                        val reasonLabel = when (track.exclusionReason) {
                            ExclusionReason.RECORDING -> strings.reasonRecording
                            ExclusionReason.SHORT_CLIP -> strings.reasonShortClip
                            ExclusionReason.KEYWORD -> strings.reasonKeyword
                            else -> strings.tabQuarantine
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matteCard(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(
                                artUri = track.artUri,
                                size = 50.dp,
                                cornerRadius = 12.dp,
                                accentColor = accentColor
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = track.artist.ifBlank { strings.unknownArtist },
                                    color = NocturneColors.TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = formatDuration(track.durationSec),
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = reasonLabel,
                                            color = accentColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = { onRestoreTrack(track) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.18f),
                                    contentColor = accentColor
                                ),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = strings.restoreTrack,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicFoldersPage(
    folderItems: List<FolderItem>,
    strings: AppStrings,
    accentColor: Color,
    bottomPadding: PaddingValues,
    onToggleExcludedFolder: (String) -> Unit,
    onSetAllFoldersExcluded: (Set<String>, Boolean) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(enabled = true, onBack = onClose)

    var searchQuery by remember { mutableStateOf("") }
    val filteredFolders = remember(folderItems, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) folderItems
        else folderItems.filter {
            it.name.contains(q, ignoreCase = true) || it.path.contains(q, ignoreCase = true)
        }
    }

    val allPaths = remember(folderItems) { folderItems.map { it.path }.toSet() }
    val allIncluded = folderItems.isNotEmpty() && folderItems.none { it.isExcluded }

    val glass = LocalWaterGlassState.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (glass != null) {
                    drawLayer(if (glass.blurEnabled) glass.blurredBgLayer else glass.bgLayer)
                    drawRect(Color.Black.copy(alpha = 0.45f))
                } else {
                    drawRect(NocturneColors.Black)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.musicFoldersSection,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (strings.isRtl) "${folderItems.count { !it.isExcluded }} مجلد مفعل من أصل ${folderItems.size}"
                        else "${folderItems.count { !it.isExcluded }} active of ${folderItems.size} folders",
                        color = NocturneColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (folderItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accentColor.copy(alpha = 0.14f))
                            .clickable {
                                onSetAllFoldersExcluded(allPaths, allIncluded)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (allIncluded) strings.deselectAll else strings.selectAll,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = NocturneColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontFamily = AppFontFamily),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (strings.isRtl) "ابحث في المجلدات..." else "Search folders...",
                                    color = NocturneColors.TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = NocturneColors.TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFolders, key = { it.path }, contentType = { "folder_row" }) { folder ->
                    val isIncluded = !folder.isExcluded
                    val relPath = folder.path.removePrefix("/storage/emulated/0/").ifBlank { folder.name }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isIncluded) accentColor.copy(alpha = 0.18f)
                                        else Color.White.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = if (isIncluded) accentColor else NocturneColors.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = folder.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${folder.trackCount}",
                                            color = accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = relPath,
                                    color = NocturneColors.TextMuted,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        GlassSwitch(
                            checked = isIncluded,
                            accentColor = accentColor,
                            onCheckedChange = { onToggleExcludedFolder(folder.path) }
                        )
                    }
                }
            }
        }
    }
}

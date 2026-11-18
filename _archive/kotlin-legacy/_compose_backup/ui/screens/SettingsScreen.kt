package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppStrings
import com.example.model.ArtworkBgMode
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    strings: AppStrings,
    settings: AppSettings,
    activeCount: Int,
    excludedCount: Int,
    hasNotificationPermission: Boolean,
    accentColor: Color,
    onLanguageChange: (AppLanguage) -> Unit,
    onCrossfadeChange: (Boolean, Int) -> Unit,
    onAutoExcludeRecordingsChange: (Boolean) -> Unit,
    onMinDurationChange: (Int) -> Unit,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onDynamicAccentChange: (Boolean) -> Unit,
    onArtworkBgModeChange: (ArtworkBgMode) -> Unit,
    onRescan: () -> Unit,
    onClearData: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    bottomPadding: PaddingValues
) {
    var keywordInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = bottomPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(strings.settingsTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(strings.settingsSubtitle, color = NocturneColors.TextSecondary, fontSize = 12.5.sp)
            }
        }

        // Language Section
        item {
            SettingsCard(title = strings.languageSection) {
                Text(strings.languageDesc, color = NocturneColors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(AppLanguage.AR to "العربية", AppLanguage.EN to "English").forEach { (lang, label) ->
                        val active = settings.language == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accentColor else Color.Transparent)
                                .clickable { onLanguageChange(lang) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.Black else NocturneColors.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Crossfade Section
        item {
            SettingsCard(title = strings.crossfadeSection) {
                SettingsToggleRow(
                    title = strings.crossfadeToggle,
                    subtitle = strings.crossfadeDesc,
                    checked = settings.crossfadeEnabled,
                    accentColor = accentColor,
                    onCheckedChange = { onCrossfadeChange(it, settings.crossfadeSec) }
                )
                if (settings.crossfadeEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.crossfadeDuration, color = NocturneColors.TextSecondary, fontSize = 12.5.sp)
                        Text("${settings.crossfadeSec} ${strings.secondsUnit}", color = accentColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.crossfadeSec.toFloat(),
                        onValueChange = { onCrossfadeChange(true, it.roundToInt()) },
                        valueRange = 1f..12f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                }
            }
        }

        // Smart Filtering Section
        item {
            SettingsCard(title = strings.filterSection) {
                SettingsToggleRow(
                    title = strings.autoExcludeRecordings,
                    subtitle = strings.autoExcludeRecordingsDesc,
                    checked = settings.autoExcludeRecordings,
                    accentColor = accentColor,
                    onCheckedChange = onAutoExcludeRecordingsChange
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.minDurationLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("${settings.minDurationSec} ${strings.secondsUnit}", color = accentColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(strings.minDurationDesc, color = NocturneColors.TextSecondary, fontSize = 12.sp)
                Slider(
                    value = settings.minDurationSec.toFloat(),
                    onValueChange = { onMinDurationChange(it.roundToInt()) },
                    valueRange = 0f..90f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )

                Spacer(Modifier.height(6.dp))
                Text(strings.customKeywordsLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        placeholder = { Text(strings.customKeywordsPlaceholder, color = NocturneColors.TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                        )
                    )
                    Button(
                        onClick = {
                            if (keywordInput.isNotBlank()) {
                                onAddKeyword(keywordInput)
                                keywordInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    }
                }
                if (settings.customKeywords.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settings.customKeywords.forEach { kw ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(kw, color = Color.White, fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Rounded.Close,
                                    null,
                                    tint = NocturneColors.TextMuted,
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

        // Super AMOLED Appearance Section
        item {
            SettingsCard(title = strings.appearanceSection) {
                SettingsToggleRow(
                    title = strings.dynamicAccentLabel,
                    subtitle = strings.dynamicAccentDesc,
                    checked = settings.dynamicAccent,
                    accentColor = accentColor,
                    onCheckedChange = onDynamicAccentChange
                )
                Spacer(Modifier.height(12.dp))
                Text(strings.artworkBgLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(strings.artworkBgDesc, color = NocturneColors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ArtworkBgMode.FULL to strings.artworkBgFull,
                        ArtworkBgMode.PARTIAL to strings.artworkBgPartial,
                        ArtworkBgMode.OFF to strings.artworkBgOff
                    ).forEach { (mode, label) ->
                        val active = settings.artworkBgMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accentColor else Color.Transparent)
                                .clickable { onArtworkBgModeChange(mode) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.Black else NocturneColors.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Local Library Storage Section
        item {
            SettingsCard(title = strings.storageSection) {
                Text(
                    text = "$activeCount ${strings.songsCount} · $excludedCount ${strings.filteredCount}",
                    color = NocturneColors.TextSecondary,
                    fontSize = 12.5.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRescan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.rescanFilters, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocturneColors.Danger.copy(alpha = 0.16f),
                            contentColor = NocturneColors.Danger
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.clearLibrary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // System Permissions & Live Capsule Section
        item {
            SettingsCard(title = strings.permissionsSection) {
                Text(strings.permissionsDesc, color = NocturneColors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Notifications,
                            null,
                            tint = if (hasNotificationPermission) NocturneColors.Emerald else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (hasNotificationPermission) strings.permissionGranted else strings.permissionPending,
                            color = if (hasNotificationPermission) NocturneColors.Emerald else Color(0xFFF59E0B),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text(strings.openNotificationSettings, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
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

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        content()
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = NocturneColors.TextSecondary, fontSize = 11.5.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}

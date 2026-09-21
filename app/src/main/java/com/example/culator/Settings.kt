package com.example.culator

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class AppSettings(val darkMode: Boolean = true, val accent: String = "System", val offsetRange: Int = 20, val customAccent: Int = 0xFFCAEFAC.toInt(), val followSystemTheme: Boolean = true, val keepScreenOn: Boolean = true, val historyRetentionDays: Int = 7)

internal class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val defaults = AppSettings()
    fun load() = AppSettings(
        preferences.getBoolean("darkMode", defaults.darkMode),
        preferences.getString("accent", defaults.accent)?.takeIf { it in accents } ?: defaults.accent,
        preferences.getInt("offsetRange", defaults.offsetRange).coerceIn(1, 100),
        preferences.getInt("customAccent", defaults.customAccent),
        preferences.getBoolean("followSystemTheme", defaults.followSystemTheme),
        preferences.getBoolean("keepScreenOn", defaults.keepScreenOn),
        preferences.getInt("historyRetentionDays", defaults.historyRetentionDays).takeIf { it in historyRetentionOptions } ?: defaults.historyRetentionDays
    )
    fun save(settings: AppSettings) {
        preferences.edit().putBoolean("darkMode", settings.darkMode)
            .putBoolean("followSystemTheme", settings.followSystemTheme)
            .putBoolean("keepScreenOn", settings.keepScreenOn)
            .putInt("historyRetentionDays", settings.historyRetentionDays)
            .putString("accent", settings.accent).putInt("offsetRange", settings.offsetRange)
            .putInt("customAccent", settings.customAccent).apply()
    }
}

internal val historyRetentionOptions = listOf(1, 7, 30, 90, 365)

internal val accents = listOf("Mint", "Blue", "Purple", "System", "Custom")

@Composable
internal fun SettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, onClose: () -> Unit) {
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    if (showColorPicker) CustomColorDialog(settings.customAccent,
        onConfirm = { onChange(settings.copy(accent = "Custom", customAccent = it)); showColorPicker = false },
        onDismiss = { showColorPicker = false })
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = !settings.followSystemTheme && settings.darkMode,
                    onClick = { onChange(settings.copy(darkMode = true, followSystemTheme = false)) }, label = { Text("Dark") })
                FilterChip(selected = !settings.followSystemTheme && !settings.darkMode,
                    onClick = { onChange(settings.copy(darkMode = false, followSystemTheme = false)) }, label = { Text("Light") })
                FilterChip(selected = settings.followSystemTheme,
                    onClick = { onChange(settings.copy(followSystemTheme = true)) }, label = { Text("System") })
            }
            Text("Accent color", style = MaterialTheme.typography.titleMedium)
            accents.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { accent ->
                        FilterChip(selected = settings.accent == accent,
                            onClick = { if (accent == "Custom") showColorPicker = true else onChange(settings.copy(accent = accent)) }, label = { Text(accent) })
                    }
                }
            }
            if (settings.accent == "System" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text("System accent requires Android 12 or later. Using Mint on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = settings.keepScreenOn,
                    role = Role.Switch,
                    onValueChange = { onChange(settings.copy(keepScreenOn = it)) }
                ).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Keep screen on", style = MaterialTheme.typography.titleMedium)
                    Text("Prevent the screen from sleeping while using the app.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.keepScreenOn, onCheckedChange = null)
            }
            HorizontalDivider()
            Text("Keep history for", style = MaterialTheme.typography.titleMedium)
            historyRetentionOptions.chunked(3).forEach { daysRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    daysRow.forEach { days ->
                        FilterChip(selected = settings.historyRetentionDays == days,
                            onClick = { onChange(settings.copy(historyRetentionDays = days)) },
                            label = { Text(if (days == 1) "1 day" else "$days days") })
                    }
                }
            }
            Text("Older entries are deleted. Increasing this period won't restore deleted history.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Random average offsets", style = MaterialTheme.typography.titleMedium)
            Text("Up to ±${settings.offsetRange}")
            Slider(value = settings.offsetRange.toFloat(), onValueChange = { onChange(settings.copy(offsetRange = it.roundToInt())) },
                valueRange = 1f..100f, steps = 98)
            Text("Each whole-number result stays within this distance of the average. The total stays exact.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { onChange(AppSettings()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore defaults")
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@Composable
private fun CustomColorDialog(initialColor: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var hex by rememberSaveable { mutableStateOf("%06X".format(initialColor and 0xFFFFFF)) }
    val rgb = hex.takeIf { it.length == 6 }?.toIntOrNull(16)
    val color = (rgb ?: (initialColor and 0xFFFFFF)) or 0xFF000000.toInt()
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Custom accent") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(64.dp).background(Color(color)))
                OutlinedTextField(value = hex, onValueChange = { input ->
                    val value = input.removePrefix("#")
                    if (value.length <= 6 && value.all { it in "0123456789abcdefABCDEF" }) hex = value.uppercase()
                }, label = { Text("Hex color") }, prefix = { Text("#") }, singleLine = true,
                    isError = rgb == null, supportingText = { Text("Enter 6 hexadecimal digits") })
                listOf("Red" to 16, "Green" to 8, "Blue" to 0).forEach { (name, shift) ->
                    val channel = (color shr shift) and 255
                    Text("$name: $channel")
                    Slider(value = channel.toFloat(), onValueChange = {
                        val next = (color and (255 shl shift).inv()) or (it.roundToInt() shl shift)
                        hex = "%06X".format(next and 0xFFFFFF)
                    }, valueRange = 0f..255f, steps = 254,
                        modifier = Modifier.semantics { contentDescription = name })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(color) }, enabled = rgb != null) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

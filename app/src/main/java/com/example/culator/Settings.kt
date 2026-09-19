package com.example.culator

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class AppSettings(val darkMode: Boolean = true, val accent: String = "Mint", val offsetRange: Int = 20, val customAccent: Int = 0xFFCAEFAC.toInt())

internal class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun load() = AppSettings(
        preferences.getBoolean("darkMode", true),
        preferences.getString("accent", "Mint")?.takeIf { it in accents } ?: "Mint",
        preferences.getInt("offsetRange", 20).coerceIn(1, 100),
        preferences.getInt("customAccent", 0xFFCAEFAC.toInt())
    )
    fun save(settings: AppSettings) {
        preferences.edit().putBoolean("darkMode", settings.darkMode)
            .putString("accent", settings.accent).putInt("offsetRange", settings.offsetRange)
            .putInt("customAccent", settings.customAccent).apply()
    }
}

internal val accents = listOf("Mint", "Blue", "Purple", "Custom")

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
                FilterChip(selected = settings.darkMode, onClick = { onChange(settings.copy(darkMode = true)) }, label = { Text("Dark") })
                FilterChip(selected = !settings.darkMode, onClick = { onChange(settings.copy(darkMode = false)) }, label = { Text("Light") })
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
            HorizontalDivider()
            Text("Random average offsets", style = MaterialTheme.typography.titleMedium)
            Text("Up to ±${settings.offsetRange}")
            Slider(value = settings.offsetRange.toFloat(), onValueChange = { onChange(settings.copy(offsetRange = it.roundToInt())) },
                valueRange = 1f..100f, steps = 98)
            Text("Each whole-number result stays within this distance of the average. The total stays exact.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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

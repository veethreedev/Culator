package com.example.culator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.math.BigDecimal
import com.example.culator.ui.theme.CulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { SettingsStore(this) }
            var settings by remember { mutableStateOf(store.load()) }
            SideEffect {
                val barStyle = if (settings.darkMode) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            CulatorTheme(darkTheme = settings.darkMode, accent = settings.accent, customAccent = settings.customAccent) {
                CalculatorScreen(settings) { settings = it; store.save(it) }
            }
        }
    }
}

@Composable
private fun CalculatorScreen(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }
    val context = LocalContext.current
    val historyStore = remember(context) { HistoryStore(context) }
    var history by remember { mutableStateOf(historyStore.load()) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    // Remove expired entries on resume and as they expire while the app is open.
    DisposableEffect(context, historyStore) {
        val lifecycle = (context as ComponentActivity).lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) history = historyStore.load()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(historyStore, history) {
        val oldest = history.minOfOrNull { it.timestamp }
        if (oldest != null) {
            delay((oldest + HISTORY_RETENTION_MS + 1 - System.currentTimeMillis()).coerceIn(1L, 60_000L))
            history = historyStore.load()
            // Continue checking even when no entries have expired yet.
            while (true) {
                delay(60_000L)
                history = historyStore.load()
            }
        }
    }
    var equation by rememberSaveable { mutableStateOf("") }
    var completed by rememberSaveable { mutableStateOf(false) }
    var splitTotal by rememberSaveable { mutableStateOf<String?>(null) }
    var itemCount by rememberSaveable { mutableStateOf("") }
    var results by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val enteringCount = splitTotal != null && results == null
    val validCount = itemCount.toIntOrNull()?.let { it in 1..1000 } == true
    var averageError by remember(equation) { mutableStateOf(false) }
    val answer = evaluate(equation)
    fun startAverage() {
        answer?.let {
            if (it.stripTrailingZeros().scale() > 0) averageError = true
            else { averageError = false; splitTotal = it.display(); itemCount = "" }
        }
    }
    BackHandler(enabled = enteringCount && !showSettings) { splitTotal = null }
    if (showSettings) {
        SettingsScreen(settings, onSettingsChange) { showSettings = false }
    } else Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            val keySize = minOf((maxWidth - 30.dp) / 4, (maxHeight - 200.dp) / 5, 100.dp).coerceAtLeast(40.dp)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showSettings = true }) { Text("Settings") }
                    TextButton(onClick = { history = historyStore.load(); showHistory = true }) { Text("History") }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                    if (enteringCount) Text("Number of items", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    EquationText(if (enteringCount) itemCount.ifEmpty { "0" } else equation.ifEmpty { "0" })
                    Text(if (enteringCount) "press = to confirm" else if (answer != null) "= ${answer.display()}" else "",
                        color = if (answer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 23.sp, maxLines = 1,
                        modifier = Modifier.padding(top = 8.dp).clickable(enabled = answer != null && !enteringCount) { startAverage() }.horizontalScroll(rememberScrollState()))
                }
                if (averageError) Text("Random average requires a whole-number total", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                Row(Modifier.padding(vertical = 20.dp).width(keySize * 4 + 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { if (enteringCount) splitTotal = null else startAverage() }, enabled = answer != null,
                        modifier = Modifier.weight(2f), shape = RoundedCornerShape(14.dp)) { Text(if (enteringCount) "Cancel" else "Random average", maxLines = 1) }
                }
                val rows = listOf(listOf("C", "⌫", "−5", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "-"), listOf("1", "2", "3", "+"), listOf("0", ".", "00", "="))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { label ->
                                val enabled = if (label == "−5") answer != null && !enteringCount
                                    else !enteringCount || label in listOf("C", "⌫") ||
                                        label.all(Char::isDigit) || (label == "=" && validCount)
                                val accent = label == "="
                                val operator = label in listOf("÷", "×", "-", "+")
                                Surface(color = if (accent) MaterialTheme.colorScheme.primary else if (operator) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp), modifier = Modifier.size(keySize)
                                        .semantics { contentDescription = when (label) { "⌫" -> "Backspace"; "C" -> "Clear"; "−5" -> "Subtract five"; else -> label } }
                                        .clickable(enabled = enabled) {
                                            if (enteringCount) {
                                                when (label) {
                                                    "C" -> itemCount = ""
                                                    "⌫" -> itemCount = itemCount.dropLast(1)
                                                    "=" -> if (validCount) results = randomAverage(splitTotal!!.toBigDecimal(), itemCount.toInt(), offsetRange = settings.offsetRange).map { it.display() }
                                                    else -> if (label.all(Char::isDigit)) {
                                                        val next = (itemCount + label).trimStart('0').ifEmpty { "0" }
                                                        if (next.length <= 4) itemCount = next
                                                    }
                                                }
                                                return@clickable
                                            }
                                            when (label) {
                                                "C" -> { equation = ""; completed = false }
                                                "⌫" -> { equation = equation.dropLast(1); completed = false }
                                                "=" -> answer?.let {
                                                    history = historyStore.add(equation)
                                                    equation = it.display()
                                                    completed = true
                                                }
                                                "−5" -> answer?.let { equation = (it - BigDecimal(5)).display(); completed = true }
                                                "+", "-", "×", "÷" -> {
                                                    if (equation.isEmpty() || equation == "-") { if (label == "-") equation = "-" }
                                                    else if (equation.last() in "+-×÷") equation = equation.dropLast(1) + label
                                                    else equation += label
                                                    completed = false
                                                }
                                                else -> {
                                                    if (completed) equation = ""
                                                    completed = false
                                                    val current = equation.takeLastWhile { it !in "+-×÷" }
                                                    if (equation.length < 100 && (label != "." || !current.contains('.'))) {
                                                        equation += if (label == "." && current.isEmpty()) "0." else label
                                                    }
                                                }
                                            }
                                        }) {
                                    Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 27.sp,
                                        color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) else if (accent) MaterialTheme.colorScheme.onPrimary else if (operator || label == "C") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showHistory) HistoryDialog(history,
        onDelete = { history = historyStore.delete(it) },
        onClear = { history = historyStore.clear() },
        onRestore = {
            equation = it.equation
            completed = false
            splitTotal = null
            results = null
            itemCount = ""
            averageError = false
            showHistory = false
        },
        onDismiss = { showHistory = false })
    results?.let { values ->
        AverageDialog(splitTotal!!.toBigDecimal(), values, settings.offsetRange,
            onShuffle = { results = randomAverage(splitTotal!!.toBigDecimal(), values.size, offsetRange = settings.offsetRange).map { it.display() } },
            onDismiss = { results = null; splitTotal = null; itemCount = ""; equation = ""; completed = false; averageError = false })
    }
}

@Composable
private fun AverageDialog(total: BigDecimal, results: List<String>, offsetRange: Int, onShuffle: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Random average", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("Total ${total.display()} · offsets up to ±$offsetRange", color = MaterialTheme.colorScheme.primary)
                    Text("${results.size} items · total exactly ${total.display()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(results) { index, value ->
                            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp)).padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}".padStart(2, '0'), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Text(value, fontSize = 22.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    OutlinedButton(onClick = onShuffle, modifier = Modifier.fillMaxWidth()) { Text("Shuffle again") }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@Composable
private fun EquationText(equation: String) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.roundToPx() }
        val style = LocalTextStyle.current
        val size = (44 downTo 24 step 2).firstOrNull { candidate ->
            measurer.measure(equation, style = style.merge(TextStyle(fontSize = candidate.sp)),
                softWrap = false).size.width <= width
        } ?: 24
        Text(equation, fontSize = size.sp, lineHeight = (size + 6).sp, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End, softWrap = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HistoryDialog(
    entries: List<HistoryEntry>, onDelete: (HistoryEntry) -> Unit,
    onClear: () -> Unit, onRestore: (HistoryEntry) -> Unit, onDismiss: () -> Unit
) {
    var selectedEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("History", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("Last 7 days · Hold an equation for actions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entries.isEmpty()) Text("No saved equations. Press = to save a result.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(entries) { _, entry ->
                        Box {
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                            .combinedClickable(onClick = {}, onLongClickLabel = "Equation actions", onLongClick = { selectedEntry = entry })
                            .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(dateFormat.format(Date(entry.timestamp)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(entry.equation, fontSize = 20.sp)
                            Text("= ${entry.result}", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
                        }
                        DropdownMenu(expanded = selectedEntry == entry, onDismissRequest = { selectedEntry = null }) {
                            DropdownMenuItem(text = { Text("Use in calculator") }, onClick = { selectedEntry = null; onRestore(entry) })
                            DropdownMenuItem(text = { Text("Delete equation") }, onClick = { selectedEntry = null; onDelete(entry) })
                        }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { selectedEntry = null; onClear() }, enabled = entries.isNotEmpty()) { Text("Clear history") }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

package com.akira.simpleplayer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.simpleplayer.playback.EqBand
import com.akira.simpleplayer.playback.EqFormat
import com.akira.simpleplayer.playback.EqLimits
import com.akira.simpleplayer.playback.EqMath
import com.akira.simpleplayer.playback.EqPreset
import com.akira.simpleplayer.playback.EqStore
import com.akira.simpleplayer.playback.EqType
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Equalizer settings: presets (select / new / rename / delete / import / export), the
 * on/off switch, preamp and up to 10 parametric bands. Changes are heard while sliders
 * move and saved when they are released.
 *
 * Drawn inside PlayerApp's root Box like CustomPlaylistScreen.
 */

private enum class EqDialog { NEW, RENAME, DELETE }

private const val CURVE_RANGE_DB = 15.0

// frequency and Q sliders are logarithmic: position 0..1
private fun posToFreq(p: Float) = EqLimits.MIN_FREQ * (EqLimits.MAX_FREQ / EqLimits.MIN_FREQ).pow(p)
private fun freqToPos(f: Float) = (ln(f / EqLimits.MIN_FREQ) / ln(EqLimits.MAX_FREQ / EqLimits.MIN_FREQ)).coerceIn(0f, 1f)
private fun posToQ(p: Float) = EqLimits.MIN_Q * (EqLimits.MAX_Q / EqLimits.MIN_Q).pow(p)
private fun qToPos(q: Float) = (ln(q / EqLimits.MIN_Q) / ln(EqLimits.MAX_Q / EqLimits.MIN_Q)).coerceIn(0f, 1f)

private fun roundFreq(f: Float): Float = when {
    f < 100f -> f.roundToInt().toFloat()
    f < 1000f -> (f / 5f).roundToInt() * 5f
    else -> (f / 50f).roundToInt() * 50f
}

private fun fmtFreq(f: Float): String =
    if (f >= 1000f) "%.2f kHz".format(f / 1000f).replace(Regex("""\.?0+ kHz$"""), " kHz")
    else "${f.roundToInt()} Hz"

private fun fmtDb(v: Float) = "%+.1f dB".format(v)

private fun displayName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

@Composable
fun EqualizerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var enabled by remember { mutableStateOf(EqStore.enabled) }
    var presets by remember { mutableStateOf(EqStore.presets()) }
    var edit by remember { mutableStateOf(EqStore.current()) }
    // the name `edit` is stored under (it changes when renamed)
    var savedName by remember { mutableStateOf(edit.name) }
    var dialog by remember { mutableStateOf<EqDialog?>(null) }

    fun reloadFromStore() {
        presets = EqStore.presets()
        edit = EqStore.current()
        savedName = edit.name
    }

    /** Heard right away, not saved yet (slider being dragged). */
    fun live(p: EqPreset) {
        edit = p
        EqStore.preview(p)
    }

    /** Save the edit. */
    fun commit(p: EqPreset = edit) {
        EqStore.save(p, savedName)
        reloadFromStore()
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = displayName(context, uri)?.substringBeforeLast('.')?.ifBlank { null } ?: "匯入的設定"
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("讀不到檔案")
            EqFormat.import(text, name)
        }.onSuccess { list ->
            EqStore.addAll(list)
            reloadFromStore()
            toast("匯入了 ${list.size} 組設定")
        }.onFailure {
            toast("匯入失敗：${it.message}")
        }
    }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(EqFormat.toPoweramp(EqStore.presets()).toByteArray(Charsets.UTF_8))
            } ?: error("寫不進檔案")
        }.onSuccess { toast("已匯出 ${presets.size} 組設定") }
            .onFailure { toast("匯出失敗：${it.message}") }
    }

    BackHandler(onBack = onClose)

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize()) {
            // top bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "等化器",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (enabled) "開" else "關",
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        EqStore.setEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary
                    )
                )
                Spacer(Modifier.width(12.dp))
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // presets
                item {
                    Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets, key = { it.name }) { p ->
                            FilterChip(
                                selected = p.name == savedName,
                                onClick = {
                                    EqStore.select(p.name)
                                    reloadFromStore()
                                },
                                label = { Text(p.name) }
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TextButton(onClick = { dialog = EqDialog.NEW }) { Text("新增", color = colors.onSurface) }
                        TextButton(onClick = { dialog = EqDialog.RENAME }) { Text("重新命名", color = colors.onSurface) }
                        TextButton(onClick = { dialog = EqDialog.DELETE }) { Text("刪除", color = colors.onSurface) }
                        TextButton(onClick = { importer.launch(arrayOf("*/*")) }) { Text("匯入", color = colors.onSurface) }
                        TextButton(onClick = { exporter.launch("SimplePlayer-EQ.json") }) { Text("匯出", color = colors.onSurface) }
                    }
                    if (!enabled) {
                        Text(
                            "等化器目前關閉，打開右上角的開關才會套用。",
                            color = colors.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    }
                }

                // response curve
                item {
                    ResponseCurve(edit)
                }

                // preamp
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceContainer)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("前級", fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
                            Text(fmtDb(edit.preamp), color = colors.onSurfaceVariant, fontSize = 13.sp)
                            TextButton(onClick = { commit(edit.copy(preamp = EqMath.autoPreamp(edit))) }) {
                                Text("自動", color = colors.onSurface)
                            }
                        }
                        EqSlider(
                            value = edit.preamp,
                            range = EqLimits.MIN_PREAMP..EqLimits.MAX_PREAMP,
                            onChange = { live(edit.copy(preamp = (it * 2).roundToInt() / 2f)) },
                            onDone = { commit() }
                        )
                        Text(
                            "有頻段加了增益時，前級要往下調差不多一樣多，才不會破音。按「自動」會幫你算。",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // bands
                itemsIndexed(edit.bands) { i, band ->
                    fun replace(b: EqBand) = edit.copy(bands = edit.bands.toMutableList().also { it[i] = b })
                    BandCard(
                        index = i,
                        band = band,
                        onLive = { live(replace(it)) },
                        onCommit = { commit(replace(it)) },
                        onDone = { commit() },
                        onDelete = { commit(edit.copy(bands = edit.bands.filterIndexed { j, _ -> j != i })) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            commit(edit.copy(bands = edit.bands + EqBand(EqType.PEAK, 1000f, 0f, 1f)))
                        },
                        enabled = edit.bands.size < EqLimits.MAX_BANDS,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.onSurface)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (edit.bands.size < EqLimits.MAX_BANDS) "新增頻段" else "最多 ${EqLimits.MAX_BANDS} 段",
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }

    when (dialog) {
        EqDialog.NEW -> NameDialog(
            title = "新增設定",
            initial = "",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                EqStore.save(EqFormat.defaultPreset(name))
                reloadFromStore()
            }
        )
        EqDialog.RENAME -> NameDialog(
            title = "重新命名",
            initial = savedName,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                dialog = null
                commit(edit.copy(name = name))
            }
        )
        EqDialog.DELETE -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("刪除「$savedName」？", color = colors.onSurface) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    EqStore.delete(savedName)
                    reloadFromStore()
                }) { Text("刪除", color = colors.onSurface) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("取消", color = colors.onSurfaceVariant) }
            },
            containerColor = colors.surface
        )
        null -> Unit
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                label = { Text("名稱") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("確定", color = colors.onSurface) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.onSurfaceVariant) }
        },
        containerColor = colors.surface
    )
}

@Composable
private fun EqSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        onValueChangeFinished = onDone,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = colors.onSurface,
            activeTrackColor = colors.onSurface,
            inactiveTrackColor = colors.outlineVariant
        )
    )
}

@Composable
private fun BandCard(
    index: Int,
    band: EqBand,
    onLive: (EqBand) -> Unit,
    onCommit: (EqBand) -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("頻段 ${index + 1}", fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "刪除頻段", tint = colors.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EqType.entries.forEach { t ->
                FilterChip(
                    selected = band.type == t,
                    onClick = { if (band.type != t) onCommit(band.copy(type = t)) },
                    label = { Text(t.label, fontSize = 13.sp) }
                )
            }
        }

        LabeledRow("頻率", fmtFreq(band.freq))
        EqSlider(
            value = freqToPos(band.freq),
            range = 0f..1f,
            onChange = { onLive(band.copy(freq = roundFreq(posToFreq(it)))) },
            onDone = onDone
        )

        LabeledRow("增益", fmtDb(band.gain))
        EqSlider(
            value = band.gain,
            range = EqLimits.MIN_GAIN..EqLimits.MAX_GAIN,
            onChange = { onLive(band.copy(gain = (it * 10).roundToInt() / 10f)) },
            onDone = onDone
        )

        LabeledRow("Q", "%.2f".format(band.q))
        EqSlider(
            value = qToPos(band.q),
            range = 0f..1f,
            onChange = { onLive(band.copy(q = (posToQ(it) * 100).roundToInt() / 100f)) },
            onDone = onDone
        )
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(top = 4.dp)) {
        Text(label, color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = colors.onSurfaceVariant, fontSize = 13.sp)
    }
}

/** The preset's frequency response, 20 Hz – 20 kHz, ±15 dB. */
@Composable
private fun ResponseCurve(preset: EqPreset) {
    val colors = MaterialTheme.colorScheme
    val line = colors.onSurface
    val grid = colors.outlineVariant
    val zero = colors.outline
    val points = remember(preset) {
        val n = 160
        DoubleArray(n + 1) { i ->
            val f = 20.0 * 1000.0.pow(i / n.toDouble())
            EqMath.response(preset, f).coerceIn(-CURVE_RANGE_DB, CURVE_RANGE_DB)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainer)
            .padding(12.dp)
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            fun y(db: Double) = (h / 2 - db / CURVE_RANGE_DB * (h / 2)).toFloat()
            fun x(f: Double) = (log10(f / 20.0) / 3.0 * w).toFloat()

            for (db in listOf(-12.0, -6.0, 6.0, 12.0)) {
                drawLine(grid, Offset(0f, y(db)), Offset(w, y(db)), strokeWidth = 1f)
            }
            drawLine(zero, Offset(0f, y(0.0)), Offset(w, y(0.0)), strokeWidth = 1.5f)
            for (f in listOf(50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0)) {
                drawLine(grid, Offset(x(f), 0f), Offset(x(f), h), strokeWidth = 1f)
            }

            val path = Path()
            points.forEachIndexed { i, db ->
                val px = w * i / (points.size - 1)
                if (i == 0) path.moveTo(px, y(db)) else path.lineTo(px, y(db))
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx()))

            for (b in preset.bands) {
                val db = EqMath.response(preset, b.freq.toDouble()).coerceIn(-CURVE_RANGE_DB, CURVE_RANGE_DB)
                drawCircle(line, radius = 4.dp.toPx(), center = Offset(x(b.freq.toDouble()), y(db)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("20", "100", "1k", "10k", "20k").forEachIndexed { i, label ->
                if (i > 0) Spacer(Modifier.weight(1f))
                Text(label, color = colors.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Text(
            "±15 dB，含前級",
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

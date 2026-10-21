package com.akira.simpleplayer.playback

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Parametric equalizer: the data model, the filter maths (RBJ "Audio EQ Cookbook"
 * biquads) and preset import/export. No Android dependencies, so it is shared by the
 * audio processor, the settings screen and the preset store.
 */

enum class EqType(val label: String) {
    LOW_SHELF("低頻擱架"),
    PEAK("峰值"),
    HIGH_SHELF("高頻擱架")
}

data class EqBand(
    val type: EqType,
    /** Hz */
    val freq: Float,
    /** dB */
    val gain: Float,
    val q: Float
)

data class EqPreset(
    val name: String,
    /** dB, applied before the filters (negative = headroom against clipping) */
    val preamp: Float,
    val bands: List<EqBand>
) {
    /** True when it would not change the sound at all. */
    val isFlat: Boolean
        get() = preamp == 0f && bands.all { it.gain == 0f }
}

object EqLimits {
    const val MAX_BANDS = 10
    const val MIN_FREQ = 20f
    const val MAX_FREQ = 20_000f
    const val MIN_GAIN = -15f
    const val MAX_GAIN = 15f
    const val MIN_Q = 0.1f
    const val MAX_Q = 10f
    const val MIN_PREAMP = -20f
    const val MAX_PREAMP = 6f

    fun band(b: EqBand) = EqBand(
        b.type,
        b.freq.coerceIn(MIN_FREQ, MAX_FREQ),
        b.gain.coerceIn(MIN_GAIN, MAX_GAIN),
        b.q.coerceIn(MIN_Q, MAX_Q)
    )

    fun preset(p: EqPreset) = EqPreset(
        p.name.trim().ifEmpty { "未命名" }.take(40),
        p.preamp.coerceIn(MIN_PREAMP, MAX_PREAMP),
        p.bands.take(MAX_BANDS).map(::band)
    )
}

object EqMath {

    /** Normalised biquad coefficients b0, b1, b2, a1, a2 (a0 = 1). */
    fun coefficients(b: EqBand, sampleRate: Int, out: DoubleArray, offset: Int) {
        // keep the centre frequency below Nyquist (a 20 kHz shelf at 44.1 kHz is fine,
        // a 20 kHz peak at 32 kHz would not be)
        val f = b.freq.toDouble().coerceAtMost(sampleRate * 0.45)
        val a = 10.0.pow(b.gain / 40.0)
        val w0 = 2.0 * PI * f / sampleRate
        val cw = cos(w0)
        val alpha = sin(w0) / (2.0 * b.q.toDouble())
        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double
        when (b.type) {
            EqType.PEAK -> {
                b0 = 1 + alpha * a
                b1 = -2 * cw
                b2 = 1 - alpha * a
                a0 = 1 + alpha / a
                a1 = -2 * cw
                a2 = 1 - alpha / a
            }
            EqType.LOW_SHELF -> {
                val s = 2 * sqrt(a) * alpha
                b0 = a * ((a + 1) - (a - 1) * cw + s)
                b1 = 2 * a * ((a - 1) - (a + 1) * cw)
                b2 = a * ((a + 1) - (a - 1) * cw - s)
                a0 = (a + 1) + (a - 1) * cw + s
                a1 = -2 * ((a - 1) + (a + 1) * cw)
                a2 = (a + 1) + (a - 1) * cw - s
            }
            EqType.HIGH_SHELF -> {
                val s = 2 * sqrt(a) * alpha
                b0 = a * ((a + 1) + (a - 1) * cw + s)
                b1 = -2 * a * ((a - 1) + (a + 1) * cw)
                b2 = a * ((a + 1) + (a - 1) * cw - s)
                a0 = (a + 1) - (a - 1) * cw + s
                a1 = 2 * ((a - 1) - (a + 1) * cw)
                a2 = (a + 1) - (a - 1) * cw - s
            }
        }
        out[offset] = b0 / a0
        out[offset + 1] = b1 / a0
        out[offset + 2] = b2 / a0
        out[offset + 3] = a1 / a0
        out[offset + 4] = a2 / a0
    }

    /** Gain in dB of the whole preset (preamp included) at [freq] Hz. */
    fun response(p: EqPreset, freq: Double, sampleRate: Int = 48_000): Double {
        val c = DoubleArray(5)
        val w = 2.0 * PI * freq / sampleRate
        val c1 = cos(w)
        val s1 = sin(w)
        val c2 = cos(2 * w)
        val s2 = sin(2 * w)
        var db = p.preamp.toDouble()
        for (b in p.bands) {
            coefficients(b, sampleRate, c, 0)
            // H(e^jw) = (b0 + b1 e^-jw + b2 e^-2jw) / (1 + a1 e^-jw + a2 e^-2jw)
            val nr = c[0] + c[1] * c1 + c[2] * c2
            val ni = -(c[1] * s1 + c[2] * s2)
            val dr = 1 + c[3] * c1 + c[4] * c2
            val di = -(c[3] * s1 + c[4] * s2)
            val mag2 = (nr * nr + ni * ni) / (dr * dr + di * di)
            db += 10 * log10(max(mag2, 1e-12))
        }
        return db
    }

    /** Preamp that keeps the loudest point of the curve at 0 dB (never above 0). */
    fun autoPreamp(p: EqPreset): Float {
        val flat = p.copy(preamp = 0f)
        var peak = 0.0
        var f = 20.0
        while (f <= 20_000.0) {
            peak = max(peak, response(flat, f))
            f *= 1.02
        }
        // round down to 0.5 dB steps
        return (-(kotlin.math.ceil(peak * 2) / 2)).toFloat().coerceAtLeast(EqLimits.MIN_PREAMP)
    }
}

object EqFormat {

    /* ---------- the app's own storage format ---------- */

    fun toJson(p: EqPreset): JSONObject = JSONObject()
        .put("name", p.name)
        .put("preamp", p.preamp.toDouble())
        .put("bands", JSONArray().apply {
            for (b in p.bands) {
                put(
                    JSONObject()
                        .put("type", b.type.name)
                        .put("freq", b.freq.toDouble())
                        .put("gain", b.gain.toDouble())
                        .put("q", b.q.toDouble())
                )
            }
        })

    fun fromJson(o: JSONObject): EqPreset? {
        val arr = o.optJSONArray("bands") ?: return null
        val bands = (0 until arr.length()).mapNotNull { i ->
            val b = arr.optJSONObject(i) ?: return@mapNotNull null
            val type = runCatching { EqType.valueOf(b.optString("type")) }.getOrNull()
                ?: return@mapNotNull null
            EqBand(
                type,
                b.optDouble("freq", 1000.0).toFloat(),
                b.optDouble("gain", 0.0).toFloat(),
                b.optDouble("q", 0.7).toFloat()
            )
        }
        return EqLimits.preset(EqPreset(o.optString("name"), o.optDouble("preamp", 0.0).toFloat(), bands))
    }

    /* ---------- import: Poweramp JSON / AutoEq ParametricEQ.txt ---------- */

    /**
     * Reads presets from a file's text. [fallbackName] names presets that have no name
     * of their own (AutoEq files). Throws IllegalArgumentException when nothing is found.
     */
    fun import(text: String, fallbackName: String): List<EqPreset> {
        val t = text.trim().removePrefix("﻿")
        val found = if (t.startsWith("[") || t.startsWith("{")) poweramp(t, fallbackName) else autoEq(t, fallbackName)
        require(found.isNotEmpty()) { "檔案裡沒有看得懂的等化器設定" }
        return found.map(EqLimits::preset)
    }

    /**
     * Poweramp's exported presets: an array (or one object) of
     * {name, preamp, parametric, bands: [{type, frequency, q, gain, channels, color}]}.
     * type 0 = low shelf, 1 = high shelf; anything else is read as a peak filter.
     */
    private fun poweramp(t: String, fallbackName: String): List<EqPreset> {
        val list = if (t.startsWith("[")) JSONArray(t) else JSONArray().put(JSONObject(t))
        return (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            // our own export format reads the same way
            if (o.optJSONArray("bands")?.optJSONObject(0)?.has("freq") == true) return@mapNotNull fromJson(o)
            val arr = o.optJSONArray("bands") ?: return@mapNotNull null
            val parametric = o.optBoolean("parametric", true)
            val bands = (0 until arr.length()).mapNotNull { j ->
                val b = arr.optJSONObject(j) ?: return@mapNotNull null
                val type = if (!parametric) EqType.PEAK else when (b.optInt("type", 2)) {
                    0 -> EqType.LOW_SHELF
                    1 -> EqType.HIGH_SHELF
                    else -> EqType.PEAK
                }
                EqBand(
                    type,
                    b.optDouble("frequency", b.optDouble("freq", 1000.0)).toFloat(),
                    b.optDouble("gain", 0.0).toFloat(),
                    b.optDouble("q", 1.41).toFloat()
                )
            }
            if (bands.isEmpty()) null
            else EqPreset(o.optString("name").ifBlank { fallbackName }, o.optDouble("preamp", 0.0).toFloat(), bands)
        }
    }

    private val PREAMP = Regex("""(?i)^\s*preamp\s*:\s*(-?[\d.]+)\s*dB""")
    private val FILTER = Regex(
        """(?i)^\s*filter\s*\d*\s*:\s*(ON|OFF)\s+([A-Z]+)\s+Fc\s+([\d.]+)\s*Hz\s+Gain\s+(-?[\d.]+)\s*dB(?:\s+Q\s+([\d.]+))?"""
    )

    /**
     * AutoEq / Equalizer APO "ParametricEQ.txt":
     *   Preamp: -6.2 dB
     *   Filter 1: ON LSC Fc 105 Hz Gain 6.5 dB Q 0.70
     *   Filter 2: ON PK Fc 220 Hz Gain -2.1 dB Q 1.20
     */
    private fun autoEq(t: String, fallbackName: String): List<EqPreset> {
        var preamp = 0f
        val bands = mutableListOf<EqBand>()
        for (line in t.lineSequence()) {
            PREAMP.find(line)?.let { preamp = it.groupValues[1].toFloat() }
            val m = FILTER.find(line) ?: continue
            if (m.groupValues[1].equals("OFF", true)) continue
            val type = when (m.groupValues[2].uppercase()) {
                "PK", "PEQ", "PEAK" -> EqType.PEAK
                "LS", "LSC", "LSQ" -> EqType.LOW_SHELF
                "HS", "HSC", "HSQ" -> EqType.HIGH_SHELF
                else -> continue
            }
            bands += EqBand(
                type,
                m.groupValues[3].toFloat(),
                m.groupValues[4].toFloat(),
                m.groupValues[5].toFloatOrNull() ?: if (type == EqType.PEAK) 1.41f else 0.7f
            )
        }
        return if (bands.isEmpty()) emptyList() else listOf(EqPreset(fallbackName, preamp, bands))
    }

    /** Export in Poweramp's format (so a preset can go back into Poweramp). */
    fun toPoweramp(presets: List<EqPreset>): String {
        val arr = JSONArray()
        for (p in presets) {
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("preamp", p.preamp.toDouble())
                    .put("parametric", true)
                    .put("bands", JSONArray().apply {
                        for (b in p.bands) {
                            put(
                                JSONObject()
                                    .put("type", when (b.type) {
                                        EqType.LOW_SHELF -> 0
                                        EqType.HIGH_SHELF -> 1
                                        EqType.PEAK -> 2
                                    })
                                    .put("channels", 0)
                                    .put("frequency", b.freq.toInt())
                                    .put("q", b.q.toDouble())
                                    .put("gain", b.gain.toDouble())
                                    .put("color", 0)
                            )
                        }
                    })
            )
        }
        return arr.toString(2)
    }

    /** A starting point: three bands at 0 dB to drag around. */
    fun defaultPreset(name: String = "預設") = EqPreset(
        name, 0f, listOf(
            EqBand(EqType.LOW_SHELF, 105f, 0f, 0.7f),
            EqBand(EqType.PEAK, 1000f, 0f, 1.0f),
            EqBand(EqType.HIGH_SHELF, 10_000f, 0f, 0.7f)
        )
    )
}

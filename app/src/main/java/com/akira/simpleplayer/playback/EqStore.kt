package com.akira.simpleplayer.playback

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Equalizer settings: the saved presets, which one is in use and the on/off switch.
 *
 * The audio thread reads [enabled], [active] and [version] (all volatile) on every
 * buffer; the settings screen changes them. [preview] changes the sound right away
 * without saving (while a slider is being dragged); [save] also writes it to disk.
 */
object EqStore {

    private const val PREFS = "equalizer"
    private const val KEY = "state"

    private var prefs: SharedPreferences? = null
    private val presets = mutableListOf<EqPreset>()
    private var currentName = ""

    @Volatile
    var enabled = false
        private set

    @Volatile
    var active: EqPreset = EqFormat.defaultPreset()
        private set

    /** Bumped on every change, so the audio processor knows to recompute. */
    @Volatile
    var version = 0
        private set

    /** Safe to call more than once (the activity and the playback service both do). */
    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        runCatching {
            val o = JSONObject(p.getString(KEY, null) ?: return@runCatching)
            enabled = o.optBoolean("enabled", false)
            currentName = o.optString("current")
            val arr = o.optJSONArray("presets") ?: JSONArray()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let(EqFormat::fromJson)?.let(presets::add)
            }
        }
        if (presets.isEmpty()) presets += EqFormat.defaultPreset()
        active = presets.firstOrNull { it.name == currentName } ?: presets.first()
        currentName = active.name
        version++
    }

    @Synchronized
    fun presets(): List<EqPreset> = presets.toList()

    @Synchronized
    fun current(): EqPreset = presets.firstOrNull { it.name == currentName } ?: presets.first()

    @Synchronized
    fun setEnabled(on: Boolean) {
        enabled = on
        version++
        persist()
    }

    @Synchronized
    fun select(name: String) {
        val p = presets.firstOrNull { it.name == name } ?: return
        currentName = p.name
        active = p
        version++
        persist()
    }

    /** Hear an edit right away without saving it. */
    fun preview(p: EqPreset) {
        active = EqLimits.preset(p)
        version++
    }

    /**
     * Saves [p] in place of the preset called [replacing] (or adds it) and makes it the
     * current one. Returns the name it was saved under (made unique if needed).
     */
    @Synchronized
    fun save(p: EqPreset, replacing: String? = null): String {
        var clean = EqLimits.preset(p)
        val index = presets.indexOfFirst { it.name == (replacing ?: clean.name) }
        val taken = presets.filterIndexed { i, _ -> i != index }.map { it.name }.toSet()
        clean = clean.copy(name = uniqueName(clean.name, taken))
        if (index >= 0) presets[index] = clean else presets += clean
        currentName = clean.name
        active = clean
        version++
        persist()
        return clean.name
    }

    /** Adds imported presets (renamed when the name is taken); selects the first. */
    @Synchronized
    fun addAll(list: List<EqPreset>): Int {
        var first: String? = null
        for (p in list) {
            val clean = EqLimits.preset(p)
            val name = uniqueName(clean.name, presets.map { it.name }.toSet())
            presets += clean.copy(name = name)
            if (first == null) first = name
        }
        first?.let { select(it) } ?: persist()
        return list.size
    }

    @Synchronized
    fun delete(name: String) {
        presets.removeAll { it.name == name }
        if (presets.isEmpty()) presets += EqFormat.defaultPreset()
        if (currentName == name) {
            currentName = presets.first().name
            active = presets.first()
        }
        version++
        persist()
    }

    private fun uniqueName(name: String, taken: Set<String>): String {
        if (name !in taken) return name
        var n = 2
        while ("$name ($n)" in taken) n++
        return "$name ($n)"
    }

    private fun persist() {
        val p = prefs ?: return
        val o = JSONObject()
            .put("enabled", enabled)
            .put("current", currentName)
            .put("presets", JSONArray().apply { presets.forEach { put(EqFormat.toJson(it)) } })
        p.edit().putString(KEY, o.toString()).apply()
    }
}

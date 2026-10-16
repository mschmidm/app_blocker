package com.khanhtq.app_blocker.debug

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists debug events to SharedPreferences so they survive when the Flutter
 * engine is not listening (background alarms, accessibility service lifecycle,
 * device boot). The Flutter app drains the buffer on startup via [getAndClear].
 *
 * Thread-safe via [synchronized] on the [prefs] object.
 */
object DebugEventBuffer {

    private const val TAG = "DebugEventBuffer"
    private const val PREF_NAME = "app_blocker_debug"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 400

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /** Appends an event. [type] should be a camelCase string identifying the event. */
    fun append(context: Context, type: String, details: Map<String, Any?> = emptyMap()) {
        init(context)
        val p = prefs ?: return
        try {
            synchronized(p) {
                val existing = parseArray(p.getString(KEY_EVENTS, "[]") ?: "[]")
                val entry = JSONObject().apply {
                    put("type", type)
                    put("timestamp", System.currentTimeMillis())
                    for ((k, v) in details) {
                        when (v) {
                            null -> put(k, JSONObject.NULL)
                            is Boolean -> put(k, v)
                            is Int -> put(k, v)
                            is Long -> put(k, v)
                            is Double -> put(k, v)
                            is String -> put(k, v)
                            is List<*> -> put(k, JSONArray(v))
                            else -> put(k, v.toString())
                        }
                    }
                }
                existing.put(entry)
                // Trim to MAX_EVENTS (keep newest)
                val trimmed = if (existing.length() > MAX_EVENTS) {
                    val start = existing.length() - MAX_EVENTS
                    JSONArray().also { arr ->
                        for (i in start until existing.length()) arr.put(existing.get(i))
                    }
                } else existing
                p.edit().putString(KEY_EVENTS, trimmed.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append debug event: $e")
        }
    }

    /**
     * Returns all buffered events as a list of maps and clears the buffer.
     * Each map has at least "type" (String) and "timestamp" (Long).
     */
    fun getAndClear(context: Context): List<Map<String, Any?>> {
        init(context)
        val p = prefs ?: return emptyList()
        return try {
            synchronized(p) {
                val json = p.getString(KEY_EVENTS, "[]") ?: "[]"
                val array = parseArray(json)
                p.edit().putString(KEY_EVENTS, "[]").apply()
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    for (key in obj.keys()) {
                        map[key] = when (val v = obj.get(key)) {
                            JSONObject.NULL -> null
                            // JSONArray is not serializable over the method channel
                            is JSONArray -> (0 until v.length()).map { j -> v.get(j) }
                            else -> v
                        }
                    }
                    map
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get debug events: $e")
            emptyList()
        }
    }

    private fun parseArray(json: String): JSONArray {
        return try {
            JSONArray(json)
        } catch (_: Exception) {
            JSONArray()
        }
    }
}

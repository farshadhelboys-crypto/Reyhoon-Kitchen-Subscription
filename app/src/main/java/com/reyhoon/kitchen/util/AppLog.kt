package com.reyhoon.kitchen.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * لاگ مرکزی اپ — Logcat + حافظه درون‌برنامه برای دیباگ ذخیره منو و API
 */
object AppLog {
    private const val TAG = "Reyhoon"
    private const val MAX = 400
    private val buffer = CopyOnWriteArrayList<String>()
    private val df = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun d(tag: String, msg: String) = add("D", tag, msg)
    fun i(tag: String, msg: String) = add("I", tag, msg)
    fun w(tag: String, msg: String) = add("W", tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) {
        val full = if (t != null) "$msg | ${t.javaClass.simpleName}: ${t.message}" else msg
        add("E", tag, full)
        if (t != null) Log.e(TAG, "[$tag] $msg", t) else Log.e(TAG, "[$tag] $msg")
    }

    private fun add(level: String, tag: String, msg: String) {
        val line = "${df.format(Date())} $level/$tag: $msg"
        buffer.add(0, line)
        while (buffer.size > MAX) buffer.removeAt(buffer.size - 1)
        when (level) {
            "E" -> { /* already logged */ }
            "W" -> Log.w(TAG, "[$tag] $msg")
            "I" -> Log.i(TAG, "[$tag] $msg")
            else -> Log.d(TAG, "[$tag] $msg")
        }
    }

    fun recent(limit: Int = 80): List<String> = buffer.take(limit)

    fun clear() = buffer.clear()

    fun dump(): String = recent(200).joinToString("\n")
}

package com.kivopass.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import com.kivopass.app.crypto.Crypto

object Clip {
    private val handler = Handler(Looper.getMainLooper())
    private var clearTask: Runnable? = null

    /** Copia marcando el contenido como sensible y lo borra a los 30 s. */
    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clip.description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= 33) putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            else putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        cm.setPrimaryClip(clip)
        clearTask?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            try {
                if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip()
                else cm.setPrimaryClip(ClipData.newPlainText("", ""))
            } catch (_: Exception) {
            }
        }
        clearTask = task
        handler.postDelayed(task, 30_000)
    }
}

object PasswordGen {
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#%^&*-_=+?"

    fun generate(length: Int = 20): String {
        val all = LOWER + UPPER + DIGITS + SYMBOLS
        while (true) {
            val sb = StringBuilder()
            repeat(length) { sb.append(all[Crypto.randomInt(all.length)]) }
            val s = sb.toString()
            if (s.any { it in LOWER } && s.any { it in UPPER } &&
                s.any { it in DIGITS } && s.any { it in SYMBOLS }
            ) return s
        }
    }
}

fun hostOf(url: String): String {
    val u = url.trim()
    if (u.isEmpty()) return ""
    return u.substringAfter("://").substringBefore('/').substringBefore('?').removePrefix("www.")
}

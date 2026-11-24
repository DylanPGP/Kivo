package com.kivopass.app.util

import android.net.Uri
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** TOTP (RFC 6238): HMAC-SHA1, 30 s, 6 dígitos. La semilla se guarda en Base32. */
object Totp {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun base32Decode(input: String): ByteArray? {
        val clean = input.uppercase().filter { it != ' ' && it != '-' && it != '=' }
        if (clean.isEmpty()) return null
        var buffer = 0
        var bits = 0
        val out = ArrayList<Byte>()
        for (c in clean) {
            val v = ALPHABET.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return if (out.isEmpty()) null else out.toByteArray()
    }

    /** Acepta una semilla Base32 o una URI otpauth://. Devuelve null si no es válida. */
    fun keyFrom(input: String): ByteArray? {
        val raw = input.trim()
        val secret = if (raw.startsWith("otpauth://", ignoreCase = true)) {
            Uri.parse(raw).getQueryParameter("secret") ?: return null
        } else raw
        return base32Decode(secret)
    }

    fun code(key: ByteArray, timeMs: Long, stepSec: Long = 30, digits: Int = 6): String {
        val counter = timeMs / 1000 / stepSec
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val bin = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        var mod = 1
        repeat(digits) { mod *= 10 }
        return (bin % mod).toString().padStart(digits, '0')
    }
}

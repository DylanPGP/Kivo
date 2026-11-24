package com.kivopass.app.data

import android.content.Context
import android.util.Base64
import com.kivopass.app.crypto.Crypto
import com.kivopass.app.crypto.KeystoreHelper
import org.json.JSONArray
import java.io.File
import java.security.GeneralSecurityException
import javax.crypto.Cipher

/**
 * Diseño de claves:
 *
 *   secreto (PIN/contraseña) --Argon2id(salt16)--> KEK
 *   VK (32 bytes SecureRandom)  --AES-256-GCM(KEK)-->  inner
 *   inner --AES-256-GCM(clave Android Keystore)-->  wrapped   (guardado en prefs)
 *   entradas (JSON) --AES-256-GCM(VK)--> vault.kivo   (nonce 12 aleatorio nuevo en cada guardado)
 *
 * La huella dactilar guarda otra copia de VK cifrada con una clave del Keystore
 * que exige biometría en cada uso.
 */
class VaultStore(context: Context) {
    private val prefs = context.getSharedPreferences("kivo_meta", Context.MODE_PRIVATE)
    private val dir = context.filesDir
    private val vaultFile = File(dir, "vault.kivo")

    private val aadVk = "kivopass:vk:v1".toByteArray()
    private val aadVault = "kivopass:vault:v1".toByteArray()

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    // ---------- Preferencias ----------

    fun isSetup() = prefs.contains("wrapped")

    fun authType(): AuthType =
        if (prefs.getString("auth", "pin") == "password") AuthType.PASSWORD else AuthType.PIN

    var lang: String?
        get() = prefs.getString("lang", null)
        set(v) { prefs.edit().putString("lang", v).apply() }

    var autoLockSec: Int
        get() = prefs.getInt("autolock", 60)
        set(v) { prefs.edit().putInt("autolock", v).apply() }

    // ---------- Creación / desbloqueo ----------

    fun createVault(type: AuthType, secret: CharArray): ByteArray {
        val vk = Crypto.randomBytes(Crypto.KEY_LEN)
        wrap(vk, type, secret)
        saveEntries(vk, emptyList())
        prefs.edit().putInt("fails", 0).putLong("lockUntil", 0L).apply()
        return vk
    }

    /** Vuelve a envolver VK con un secreto nuevo (cambio de PIN/contraseña). */
    fun rewrap(vk: ByteArray, type: AuthType, secret: CharArray) = wrap(vk, type, secret)

    private fun wrap(vk: ByteArray, type: AuthType, secret: CharArray) {
        val salt = Crypto.randomBytes(Crypto.SALT_LEN)
        val kek = Crypto.argon2id(secret, salt)
        val inner = try { Crypto.encrypt(kek, vk, aadVk) } finally { kek.fill(0) }
        val outer = KeystoreHelper.encrypt(inner)
        prefs.edit()
            .putString("salt", b64(salt))
            .putString("wrapped", b64(outer))
            .putString("auth", if (type == AuthType.PIN) "pin" else "password")
            .putString("kdf", "argon2id;v=19;m=32768;t=3;p=2")
            .commit()
    }

    /** Devuelve VK o null si el secreto es incorrecto. */
    fun unlock(secret: CharArray): ByteArray? {
        return try {
            val salt = unb64(prefs.getString("salt", null) ?: return null)
            val outer = unb64(prefs.getString("wrapped", null) ?: return null)
            val inner = KeystoreHelper.decrypt(outer)
            val kek = Crypto.argon2id(secret, salt)
            try { Crypto.decrypt(kek, inner, aadVk) } finally { kek.fill(0) }
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // ---------- Bloqueo por intentos fallidos ----------

    fun lockRemainingMs(): Long =
        (prefs.getLong("lockUntil", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun registerFailure() {
        val fails = prefs.getInt("fails", 0) + 1
        var until = 0L
        if (fails >= 5) {
            val sec = minOf(30L shl minOf(fails - 5, 7), 3600L)
            until = System.currentTimeMillis() + sec * 1000
        }
        prefs.edit().putInt("fails", fails).putLong("lockUntil", until).apply()
    }

    fun resetFailures() {
        prefs.edit().putInt("fails", 0).putLong("lockUntil", 0L).apply()
    }

    // ---------- Biometría ----------

    fun bioEnabled() = prefs.contains("bio_ct") && prefs.contains("bio_iv")

    fun bioEnableCipher(): Cipher = KeystoreHelper.newBioEncryptCipher()

    fun bioFinishEnable(vk: ByteArray, cipher: Cipher) {
        val ct = cipher.doFinal(vk)
        prefs.edit().putString("bio_iv", b64(cipher.iv)).putString("bio_ct", b64(ct)).commit()
    }

    /** null si la clave quedó invalidada (cambio de huellas). */
    fun bioDecryptCipher(): Cipher? {
        return try {
            val iv = prefs.getString("bio_iv", null) ?: return null
            KeystoreHelper.bioDecryptCipher(unb64(iv))
        } catch (e: Exception) {
            bioDisable()
            null
        }
    }

    fun bioFinishUnlock(cipher: Cipher): ByteArray =
        cipher.doFinal(unb64(prefs.getString("bio_ct", null)!!))

    fun bioDisable() {
        prefs.edit().remove("bio_iv").remove("bio_ct").apply()
        KeystoreHelper.deleteBioKey()
    }

    // ---------- Entradas ----------

    fun loadEntries(vk: ByteArray): List<Entry> {
        if (!vaultFile.exists()) return emptyList()
        val plain = Crypto.decrypt(vk, vaultFile.readBytes(), aadVault)
        return try {
            val arr = JSONArray(String(plain, Charsets.UTF_8))
            (0 until arr.length()).map { Entry.fromJson(arr.getJSONObject(it)) }
        } finally {
            plain.fill(0)
        }
    }

    fun saveEntries(vk: ByteArray, list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val plain = arr.toString().toByteArray(Charsets.UTF_8)
        val blob = try { Crypto.encrypt(vk, plain, aadVault) } finally { plain.fill(0) }
        val tmp = File(dir, "vault.kivo.tmp")
        tmp.writeBytes(blob)
        if (!tmp.renameTo(vaultFile)) {
            vaultFile.delete()
            tmp.renameTo(vaultFile)
        }
    }
}

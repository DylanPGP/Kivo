package com.kivopass.app

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.kivopass.app.data.AuthType
import com.kivopass.app.data.Entry
import com.kivopass.app.data.EntryType
import com.kivopass.app.data.VaultStore
import com.kivopass.app.util.Bio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface Nav {
    data object Home : Nav
    data class Detail(val id: String) : Nav
    data class Edit(val id: String?) : Nav
    data object Settings : Nav
}

class AppModel(context: Context) {
    val store = VaultStore(context)

    var lang by mutableStateOf(store.lang ?: if (Locale.getDefault().language == "es") "es" else "en")
        private set
    var needsSetup by mutableStateOf(!store.isSetup())
    var unlocked by mutableStateOf(false)
        private set
    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set
    var nav by mutableStateOf<Nav>(Nav.Home)
    var bioOn by mutableStateOf(store.bioEnabled())
        private set
    var autoLockSec by mutableStateOf(store.autoLockSec)
        private set

    private var vk: ByteArray? = null
    private var backgroundAt = 0L

    private fun s(es: String, en: String) = if (lang == "es") es else en

    fun setLang(value: String) {
        lang = value
        store.lang = value
    }

    fun setAutoLock(sec: Int) {
        autoLockSec = sec
        store.autoLockSec = sec
    }

    // ---------- Sesión ----------

    suspend fun setup(type: AuthType, secret: CharArray) {
        val key = withContext(Dispatchers.Default) { store.createVault(type, secret) }
        enter(key, emptyList())
        needsSetup = false
    }

    /** true si el secreto fue correcto. */
    suspend fun unlock(secret: CharArray): Boolean {
        val key = withContext(Dispatchers.Default) { store.unlock(secret) }
        if (key == null) {
            store.registerFailure()
            return false
        }
        return try {
            enter(key, store.loadEntries(key))
            true
        } catch (e: Exception) {
            key.fill(0)
            false
        }
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        val cipher = store.bioDecryptCipher()
        if (cipher == null) {
            bioOn = false
            return
        }
        Bio.prompt(activity, s("Desbloquear Kivo Pass", "Unlock Kivo Pass"), s("Usar PIN / contraseña", "Use PIN / password"), cipher) { c ->
            try {
                val key = store.bioFinishUnlock(c)
                enter(key, store.loadEntries(key))
            } catch (_: Exception) {
            }
        }
    }

    private fun enter(key: ByteArray, list: List<Entry>) {
        vk = key
        entries = list.sortedBy { it.title.lowercase() }
        store.resetFailures()
        nav = Nav.Home
        unlocked = true
    }

    fun lock() {
        vk?.fill(0)
        vk = null
        entries = emptyList()
        unlocked = false
        nav = Nav.Home
    }

    fun onBackgrounded() {
        backgroundAt = SystemClock.elapsedRealtime()
        if (unlocked && autoLockSec == 0) lock()
    }

    fun onForegrounded() {
        if (unlocked && backgroundAt > 0 &&
            SystemClock.elapsedRealtime() - backgroundAt > autoLockSec * 1000L
        ) lock()
        backgroundAt = 0
    }

    // ---------- Biometría ----------

    fun enableBiometric(activity: FragmentActivity) {
        val key = vk ?: return
        val cipher = try { store.bioEnableCipher() } catch (e: Exception) { return }
        Bio.prompt(activity, s("Activar huella dactilar", "Enable fingerprint"), s("Cancelar", "Cancel"), cipher) { c ->
            try {
                store.bioFinishEnable(key, c)
                bioOn = true
            } catch (_: Exception) {
                store.bioDisable()
                bioOn = false
            }
        }
    }

    fun disableBiometric() {
        store.bioDisable()
        bioOn = false
    }

    suspend fun changeSecret(type: AuthType, secret: CharArray) {
        val key = vk ?: return
        withContext(Dispatchers.Default) { store.rewrap(key, type, secret) }
    }

    // ---------- Entradas ----------

    fun save(entry: Entry) {
        val key = vk ?: return
        entries = (entries.filter { it.id != entry.id } + entry).sortedBy { it.title.lowercase() }
        store.saveEntries(key, entries)
    }

    fun delete(id: String) {
        val key = vk ?: return
        entries = entries.filter { it.id != id }
        store.saveEntries(key, entries)
    }

    fun typeLabel(t: EntryType) = when (t) {
        EntryType.LOGIN -> s("Cuenta", "Account")
        EntryType.CARD -> s("Tarjeta", "Card")
        EntryType.SEED -> s("Frase semilla", "Seed phrase")
    }
}

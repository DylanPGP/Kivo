package com.kivopass.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivopass.app.AppModel
import com.kivopass.app.Nav
import com.kivopass.app.data.Entry
import com.kivopass.app.data.EntryType
import com.kivopass.app.data.K
import com.kivopass.app.util.PasswordGen
import com.kivopass.app.util.Totp
import com.kivopass.app.util.hostOf

private fun allowedKeys(t: EntryType): Set<String> = when (t) {
    EntryType.LOGIN -> setOf(K.URL, K.USER, K.TOTP, K.PASS, K.NOTES)
    EntryType.CARD -> setOf(K.HOLDER, K.NUMBER, K.EXPIRY, K.CVV, K.PIN, K.NOTES)
    EntryType.SEED -> setOf(K.PHRASE, K.PASSPHRASE, K.NOTES)
}

@Composable
fun EditScreen(model: AppModel, id: String?) {
    val lang = model.lang
    val existing = remember(id) { model.entries.firstOrNull { it.id == id } }
    var type by remember { mutableStateOf(existing?.type ?: EntryType.LOGIN) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    val f = remember { mutableStateMapOf<String, String>().also { m -> existing?.fields?.let { m.putAll(it) } } }
    var error by remember { mutableStateOf<String?>(null) }

    val goBack = { model.nav = if (existing != null) Nav.Detail(existing.id) else Nav.Home }
    BackHandler { goBack() }

    val errName = tx(lang, "Escribe un nombre", "Enter a name")
    val errTotp = tx(lang, "El código TOTP no es Base32 válido", "The TOTP secret is not valid Base32")
    val errPhrase = tx(lang, "Escribe la frase semilla", "Enter the seed phrase")
    val errNumber = tx(lang, "Escribe el número de la tarjeta", "Enter the card number")

    fun save() {
        val keys = allowedKeys(type)
        val clean = HashMap<String, String>()
        f.forEach { (k, v) ->
            if (k in keys && v.isNotBlank()) clean[k] = if (k == K.PASS) v else v.trim()
        }
        val finalTitle = title.trim()
            .ifBlank { hostOf(clean[K.URL].orEmpty()) }
            .ifBlank { clean[K.USER].orEmpty() }
            .ifBlank { clean[K.HOLDER].orEmpty() }
        when {
            finalTitle.isBlank() -> { error = errName; return }
            type == EntryType.LOGIN && clean[K.TOTP] != null && Totp.keyFrom(clean[K.TOTP]!!) == null -> {
                error = errTotp; return
            }
            type == EntryType.SEED && clean[K.PHRASE] == null -> { error = errPhrase; return }
            type == EntryType.CARD && clean[K.NUMBER] == null -> { error = errNumber; return }
        }
        val now = System.currentTimeMillis()
        val entry = Entry(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            type = type,
            title = finalTitle,
            fields = clean,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        model.save(entry)
        model.nav = Nav.Detail(entry.id)
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        BackBar(onBack = { goBack() }) {
            Text(
                if (existing == null) tr("Nuevo", "New") else tr("Editar", "Edit"),
                fontSize = 18.sp, color = KText, modifier = Modifier.padding(end = 16.dp)
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (existing == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryType.values().forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t; error = null },
                            label = { Text(model.typeLabel(t)) })
                    }
                }
            }
            KField(
                when (type) {
                    EntryType.LOGIN -> tr("Nombre (opcional si pones enlace)", "Name (optional if you add a link)")
                    EntryType.CARD -> tr("Nombre (ej. Visa personal)", "Name (e.g. Personal Visa)")
                    EntryType.SEED -> tr("Nombre (ej. Billetera principal)", "Name (e.g. Main wallet)")
                },
                title, { title = it; error = null }
            )

            when (type) {
                EntryType.LOGIN -> {
                    KField(tr("Enlace de la plataforma (opcional)", "Platform link (optional)"),
                        f[K.URL].orEmpty(), { f[K.URL] = it }, keyboardType = KeyboardType.Uri)
                    KField(tr("Usuario o correo", "Username or email"),
                        f[K.USER].orEmpty(), { f[K.USER] = it }, keyboardType = KeyboardType.Email)
                    KField(
                        tr("Contraseña", "Password"), f[K.PASS].orEmpty(), { f[K.PASS] = it },
                        hidden = true,
                        extra = {
                            IconButton(onClick = { f[K.PASS] = PasswordGen.generate() }) {
                                Icon(Icons.Filled.AutoFixHigh, tr("Generar", "Generate"))
                            }
                        }
                    )
                    KField(tr("Código TOTP en Base32 (opcional)", "TOTP secret in Base32 (optional)"),
                        f[K.TOTP].orEmpty(), { f[K.TOTP] = it; error = null }, hidden = true)
                }
                EntryType.CARD -> {
                    KField(tr("Titular", "Cardholder"), f[K.HOLDER].orEmpty(), { f[K.HOLDER] = it })
                    KField(tr("Número de tarjeta", "Card number"), f[K.NUMBER].orEmpty(),
                        { f[K.NUMBER] = it.filter { c -> c.isDigit() || c == ' ' }.replace(" ", "") },
                        keyboardType = KeyboardType.Number, hidden = true)
                    KField(tr("Vencimiento (MM/AA)", "Expiry (MM/YY)"), f[K.EXPIRY].orEmpty(), { f[K.EXPIRY] = it })
                    KField("CVV", f[K.CVV].orEmpty(), { f[K.CVV] = it }, keyboardType = KeyboardType.NumberPassword, hidden = true)
                    KField(tr("PIN (opcional)", "PIN (optional)"), f[K.PIN].orEmpty(), { f[K.PIN] = it },
                        keyboardType = KeyboardType.NumberPassword, hidden = true)
                }
                EntryType.SEED -> {
                    KField(tr("Frase semilla (palabras separadas por espacios)", "Seed phrase (words separated by spaces)"),
                        f[K.PHRASE].orEmpty(), { f[K.PHRASE] = it; error = null }, minLines = 3, hidden = true)
                    KField(tr("Passphrase (opcional)", "Passphrase (optional)"),
                        f[K.PASSPHRASE].orEmpty(), { f[K.PASSPHRASE] = it }, hidden = true)
                }
            }
            KField(tr("Notas (opcional)", "Notes (optional)"), f[K.NOTES].orEmpty(), { f[K.NOTES] = it }, minLines = 2)
            error?.let { Text(it, color = KDanger) }
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = { save() },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
        ) { Text(tr("Guardar", "Save"), fontSize = 16.sp) }
    }
}

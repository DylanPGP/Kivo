package com.kivopass.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.kivopass.app.AppModel
import com.kivopass.app.Nav
import com.kivopass.app.data.AuthType
import com.kivopass.app.util.Bio
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(model: AppModel, activity: FragmentActivity) {
    val lang = model.lang
    val bioAvailable = remember { Bio.available(activity) }
    var showChange by remember { mutableStateOf(false) }
    BackHandler { model.nav = Nav.Home }

    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
        BackBar(onBack = { model.nav = Nav.Home }) {
            Text(tr("Ajustes", "Settings"), fontSize = 18.sp, color = KText, modifier = Modifier.padding(end = 16.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionTitle(tr("Idioma", "Language"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = lang == "es", onClick = { model.changeLang("es") }, label = { Text("Español") })
                FilterChip(selected = lang == "en", onClick = { model.changeLang("en") }, label = { Text("English") })
            }

            SectionTitle(tr("Seguridad", "Security"))
            if (bioAvailable) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Huella dactilar", "Fingerprint"), color = KText, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Switch(checked = model.bioOn, onCheckedChange = {
                        if (it) model.enableBiometric(activity) else model.disableBiometric()
                    })
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = { showChange = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Cambiar PIN o contraseña", "Change PIN or password"))
            }
            Spacer(Modifier.height(16.dp))
            Text(tr("Bloqueo automático", "Auto-lock"), color = KText, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to tr("Inmediato", "Immediate"), 60 to "1 min", 300 to "5 min").forEach { (sec, label) ->
                    FilterChip(selected = model.autoLockSec == sec, onClick = { model.setAutoLock(sec) }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { model.lock() }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Bloquear ahora", "Lock now"))
            }

            SectionTitle(tr("Acerca de", "About"))
            Text("Kivo Pass 0.1.0 beta", color = KText)
            Spacer(Modifier.height(6.dp))
            Text(
                "Argon2id · AES-256-GCM · salt 16 B · nonce 12 B · tag 16 B · SecureRandom · Android Keystore",
                color = KDim, fontSize = 13.sp
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showChange) ChangeSecretDialog(model) { showChange = false }
}

@Composable
private fun ChangeSecretDialog(model: AppModel, onDismiss: () -> Unit) {
    val lang = model.lang
    var type by remember { mutableStateOf(AuthType.PIN) }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(tr("Nuevo bloqueo", "New lock")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == AuthType.PIN, onClick = { type = AuthType.PIN; a = ""; b = "" },
                        label = { Text("PIN (5)") })
                    FilterChip(selected = type == AuthType.PASSWORD, onClick = { type = AuthType.PASSWORD; a = ""; b = "" },
                        label = { Text(tr("Contraseña", "Password")) })
                }
                SecretField(a, { a = it; error = null }, tr("Nuevo", "New"), numeric = type == AuthType.PIN)
                SecretField(b, { b = it; error = null }, tr("Confirmar", "Confirm"), numeric = type == AuthType.PIN)
                error?.let { Text(it, color = KDanger) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                val err = secretError(lang, type, a, b)
                if (err != null) {
                    error = err
                } else {
                    busy = true
                    val chars = a.toCharArray()
                    scope.launch {
                        model.changeSecret(type, chars)
                        chars.fill('0')
                        onDismiss()
                    }
                }
            }) { Text(tr("Guardar", "Save")) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(tr("Cancelar", "Cancel")) } }
    )
}

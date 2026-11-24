package com.kivopass.app.ui

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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.kivopass.app.AppModel
import com.kivopass.app.data.AuthType
import com.kivopass.app.util.Bio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(model: AppModel, activity: FragmentActivity) {
    val lang = model.lang
    var type by remember { mutableStateOf(AuthType.PIN) }
    var s1 by remember { mutableStateOf("") }
    var s2 by remember { mutableStateOf("") }
    var wantBio by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val bioAvailable = remember { Bio.available(activity) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Logo(110.dp)
        Text("Kivo Pass", fontSize = 28.sp, color = KText)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Crea el bloqueo de tu bóveda. No se puede recuperar si lo olvidas.",
                "Create your vault lock. It cannot be recovered if you forget it."),
            color = KDim, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == AuthType.PIN,
                onClick = { type = AuthType.PIN; s1 = ""; s2 = ""; error = null },
                label = { Text(tr("PIN de 5 dígitos", "5-digit PIN")) }
            )
            FilterChip(
                selected = type == AuthType.PASSWORD,
                onClick = { type = AuthType.PASSWORD; s1 = ""; s2 = ""; error = null },
                label = { Text(tr("Contraseña", "Password")) }
            )
        }
        Spacer(Modifier.height(14.dp))
        SecretField(
            s1, { s1 = it; error = null },
            if (type == AuthType.PIN) tr("PIN", "PIN") else tr("Contraseña", "Password"),
            numeric = type == AuthType.PIN, enabled = !busy
        )
        Spacer(Modifier.height(10.dp))
        SecretField(
            s2, { s2 = it; error = null }, tr("Confirmar", "Confirm"),
            numeric = type == AuthType.PIN, enabled = !busy
        )
        if (bioAvailable) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = wantBio, onCheckedChange = { wantBio = it })
                Text(tr("Activar también huella dactilar", "Also enable fingerprint"), color = KText)
            }
        }
        error?.let { Text(it, color = KDanger, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                val err = secretError(lang, type, s1, s2)
                if (err != null) {
                    error = err
                } else {
                    busy = true
                    val chars = s1.toCharArray()
                    val bio = wantBio
                    scope.launch {
                        model.setup(type, chars)
                        chars.fill('0')
                        if (bio) model.enableBiometric(activity)
                    }
                }
            }
        ) {
            if (busy) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
            else Text(tr("Crear bóveda", "Create vault"), fontSize = 16.sp)
        }
    }
}

@Composable
fun LockScreen(model: AppModel, activity: FragmentActivity) {
    val lang = model.lang
    val type = remember { model.store.authType() }
    val numeric = type == AuthType.PIN
    var secret by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var remaining by remember { mutableLongStateOf(model.store.lockRemainingMs()) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val wrong = tx(lang, "Incorrecto", "Incorrect")

    LaunchedEffect(remaining) {
        while (remaining > 0) {
            delay(1000)
            remaining = model.store.lockRemainingMs()
        }
    }
    LaunchedEffect(Unit) {
        if (model.bioOn) model.unlockWithBiometric(activity)
        else runCatching { focus.requestFocus() }
    }

    fun attempt() {
        if (busy || remaining > 0 || secret.isEmpty()) return
        busy = true
        val chars = secret.toCharArray()
        scope.launch {
            val ok = model.unlock(chars)
            chars.fill('0')
            busy = false
            secret = ""
            if (!ok) {
                remaining = model.store.lockRemainingMs()
                error = wrong
            }
        }
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Logo(120.dp)
        Text("Kivo Pass", fontSize = 30.sp, color = KText)
        Spacer(Modifier.height(24.dp))
        SecretField(
            value = secret,
            onChange = {
                secret = it
                error = null
                if (numeric && it.length == 5) attempt()
            },
            label = if (numeric) tr("PIN", "PIN") else tr("Contraseña", "Password"),
            numeric = numeric,
            enabled = !busy && remaining == 0L,
            modifier = Modifier.focusRequester(focus),
            onDone = { attempt() }
        )
        if (remaining > 0) {
            Text(
                tr("Demasiados intentos. Espera ${(remaining + 999) / 1000} s",
                    "Too many attempts. Wait ${(remaining + 999) / 1000} s"),
                color = KDanger, modifier = Modifier.padding(top = 10.dp)
            )
        } else {
            error?.let { Text(it, color = KDanger, modifier = Modifier.padding(top = 10.dp)) }
        }
        if (!numeric) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { attempt() },
                enabled = !busy && remaining == 0L,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
                else Text(tr("Desbloquear", "Unlock"), fontSize = 16.sp)
            }
        } else if (busy) {
            CircularProgressIndicator(Modifier.padding(top = 16.dp).height(28.dp), strokeWidth = 2.dp)
        }
        if (model.bioOn) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { model.unlockWithBiometric(activity) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Fingerprint, null)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(tr("Usar huella dactilar", "Use fingerprint"))
            }
        }
    }
}

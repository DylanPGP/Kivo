package com.kivopass.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivopass.app.AppModel
import com.kivopass.app.Nav
import com.kivopass.app.data.EntryType
import com.kivopass.app.data.K
import com.kivopass.app.util.Clip
import com.kivopass.app.util.Totp
import com.kivopass.app.util.hostOf
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(model: AppModel, id: String) {
    val entry = model.entries.firstOrNull { it.id == id }
    BackHandler { model.nav = Nav.Home }
    if (entry == null) {
        LaunchedEffect(Unit) { model.nav = Nav.Home }
        return
    }
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
        BackBar(onBack = { model.nav = Nav.Home }) {
            IconButton(onClick = { model.nav = Nav.Edit(entry.id) }) {
                Icon(Icons.Filled.Edit, tr("Editar", "Edit"), tint = KAccent)
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF26233F))
                    .border(1.dp, KBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(typeIcon(entry.type), null, tint = KAccent, modifier = Modifier.size(38.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(entry.title, fontSize = 32.sp, color = KText, textAlign = TextAlign.Center)
            val sub = if (entry.type == EntryType.LOGIN) entry.f(K.URL) else model.typeLabel(entry.type)
            if (sub.isNotBlank()) Text(sub, fontSize = 15.sp, color = KDim)
            Spacer(Modifier.height(20.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (entry.type) {
                    EntryType.LOGIN -> {
                        if (entry.f(K.USER).isNotBlank()) FieldCard(tr("Usuario / correo", "Username / email"), entry.f(K.USER))
                        if (entry.f(K.PASS).isNotEmpty()) FieldCard(tr("Contraseña", "Password"), entry.f(K.PASS), secret = true)
                        if (entry.f(K.URL).isNotBlank()) {
                            FieldCard("URL", entry.f(K.URL), onOpen = { openUrl(ctx, entry.f(K.URL)) })
                        }
                        if (entry.f(K.TOTP).isNotBlank()) TotpCard(entry.f(K.TOTP))
                    }
                    EntryType.CARD -> {
                        if (entry.f(K.HOLDER).isNotBlank()) FieldCard(tr("Titular", "Holder"), entry.f(K.HOLDER))
                        if (entry.f(K.NUMBER).isNotBlank()) {
                            FieldCard(tr("Número", "Number"), entry.f(K.NUMBER), secret = true, display = { v ->
                                if (v.all { it.isDigit() }) v.chunked(4).joinToString(" ") else v
                            })
                        }
                        if (entry.f(K.EXPIRY).isNotBlank()) FieldCard(tr("Vencimiento", "Expiry"), entry.f(K.EXPIRY))
                        if (entry.f(K.CVV).isNotBlank()) FieldCard("CVV", entry.f(K.CVV), secret = true)
                        if (entry.f(K.PIN).isNotBlank()) FieldCard("PIN", entry.f(K.PIN), secret = true)
                    }
                    EntryType.SEED -> {
                        if (entry.f(K.PHRASE).isNotBlank()) FieldCard(tr("Frase semilla", "Seed phrase"), entry.f(K.PHRASE), secret = true)
                        if (entry.f(K.PASSPHRASE).isNotBlank()) FieldCard(tr("Passphrase", "Passphrase"), entry.f(K.PASSPHRASE), secret = true)
                    }
                }
                if (entry.f(K.NOTES).isNotBlank()) FieldCard(tr("Notas", "Notes"), entry.f(K.NOTES))
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.Delete, null, tint = KDanger)
                Spacer(Modifier.size(8.dp))
                Text(tr("Eliminar", "Delete"), color = KDanger)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("¿Eliminar?", "Delete?")) },
            text = { Text(tr("Esta acción no se puede deshacer.", "This cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    model.delete(entry.id)
                    model.nav = Nav.Home
                }) { Text(tr("Eliminar", "Delete"), color = KDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancelar", "Cancel")) } }
        )
    }
}

@Composable
private fun TotpCard(secret: String) {
    val ctx = LocalContext.current
    val key = remember(secret) { Totp.keyFrom(secret) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val shape = RoundedCornerShape(16.dp)
    val copied = tr("Copiado (se borra en 30 s)", "Copied (clears in 30 s)")
    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, KBorder, shape).background(KCard).padding(16.dp)
    ) {
        Text("TOTP", fontSize = 12.sp, letterSpacing = 2.sp, color = KDim)
        Spacer(Modifier.height(6.dp))
        if (key == null) {
            Text(tr("Semilla Base32 no válida", "Invalid Base32 secret"), color = KDanger)
        } else {
            val code = Totp.code(key, now)
            val left = 30 - ((now / 1000) % 30)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    code.substring(0, 3) + " " + code.substring(3),
                    fontSize = 30.sp, color = KText, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text("${left}s", color = KDim, fontSize = 15.sp)
                IconButton(onClick = {
                    Clip.copy(ctx, "TOTP", code)
                    android.widget.Toast.makeText(ctx, copied, android.widget.Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Filled.ContentCopy, null, tint = KAccent) }
            }
            LinearProgressIndicator(
                progress = { left / 30f },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                color = KAccent, trackColor = KBorder
            )
        }
    }
}

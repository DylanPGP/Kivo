package com.kivopass.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivopass.app.R
import com.kivopass.app.data.AuthType
import com.kivopass.app.data.EntryType
import com.kivopass.app.util.Clip

@Composable
fun Logo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = "Kivo Pass",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

fun typeIcon(t: EntryType): ImageVector = when (t) {
    EntryType.LOGIN -> Icons.Filled.Person
    EntryType.CARD -> Icons.Filled.CreditCard
    EntryType.SEED -> Icons.Filled.VpnKey
}

fun secretError(lang: String, type: AuthType, a: String, b: String): String? = when {
    type == AuthType.PIN && !(a.length == 5 && a.all { it.isDigit() }) ->
        tx(lang, "El PIN debe tener 5 dígitos", "The PIN must be 5 digits")
    type == AuthType.PASSWORD && a.length < 8 ->
        tx(lang, "La contraseña debe tener al menos 8 caracteres", "The password must be at least 8 characters")
    a != b -> tx(lang, "Los campos no coinciden", "The fields do not match")
    else -> null
}

@Composable
fun BackBar(onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = KAccent)
            Spacer(Modifier.width(6.dp))
            Text(tr("Atrás", "Back"), color = KAccent, fontSize = 17.sp)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** Tarjeta de campo con etiqueta en mayúsculas, como en la pantalla de referencia. */
@Composable
fun FieldCard(
    label: String,
    value: String,
    secret: Boolean = false,
    display: (String) -> String = { it },
    onOpen: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    val copied = tr("Copiado (se borra en 30 s)", "Copied (clears in 30 s)")
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(BorderStroke(1.dp, KBorder), shape)
            .background(KCard).padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), fontSize = 12.sp, letterSpacing = 2.sp, color = KDim)
            Spacer(Modifier.height(6.dp))
            Text(
                if (secret && !shown) "•".repeat(12) else display(value),
                fontSize = 18.sp, color = KText
            )
        }
        if (secret) {
            IconButton(onClick = { shown = !shown }) {
                Icon(if (shown) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = KAccent)
            }
        }
        if (onOpen != null) {
            IconButton(onClick = onOpen) { Icon(Icons.Filled.OpenInNew, null, tint = KAccent) }
        }
        IconButton(onClick = {
            Clip.copy(ctx, label, value)
            Toast.makeText(ctx, copied, Toast.LENGTH_SHORT).show()
        }) { Icon(Icons.Filled.ContentCopy, null, tint = KAccent) }
    }
}

fun openUrl(ctx: android.content.Context, raw: String) {
    val url = if (raw.contains("://")) raw else "https://$raw"
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

/** Campo de texto con opción de ocultar y acciones extra. */
@Composable
fun KField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    hidden: Boolean = false,
    extra: (@Composable () -> Unit)? = null
) {
    var shown by remember { mutableStateOf(false) }
    val trailing: (@Composable () -> Unit)? = if (hidden || extra != null) {
        @Composable {
            Row {
                extra?.invoke()
                if (hidden) {
                    IconButton(onClick = { shown = !shown }) {
                        Icon(if (shown) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                    }
                }
            }
        }
    } else null
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        visualTransformation = if (hidden && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

/** Campo para PIN o contraseña maestra. */
@Composable
fun SecretField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    numeric: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null
) {
    var shown by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { v ->
            if (numeric) {
                if (v.length <= 5 && v.all { it.isDigit() }) onChange(v)
            } else onChange(v)
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        trailingIcon = {
            IconButton(onClick = { shown = !shown }) {
                Icon(if (shown) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(), fontSize = 12.sp, letterSpacing = 2.sp, color = KDim,
        fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun Spacing(h: Dp) = Box(Modifier.height(h))

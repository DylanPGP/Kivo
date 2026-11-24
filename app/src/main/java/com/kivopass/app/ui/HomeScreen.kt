package com.kivopass.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivopass.app.AppModel
import com.kivopass.app.Nav
import com.kivopass.app.data.Entry
import com.kivopass.app.data.EntryType
import com.kivopass.app.data.K
import com.kivopass.app.util.hostOf

@Composable
fun HomeScreen(model: AppModel) {
    var filter by remember { mutableStateOf<EntryType?>(null) }
    val shown = model.entries.filter { filter == null || it.type == filter }

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Logo(38.dp)
                Spacer(Modifier.width(10.dp))
                Text("Kivo Pass", fontSize = 22.sp, color = KText)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { model.nav = Nav.Settings }) {
                    Icon(Icons.Filled.Settings, tr("Ajustes", "Settings"), tint = KText)
                }
            }

            if (model.entries.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Logo(120.dp, Modifier.alpha(0.35f))
                    Spacer(Modifier.size(16.dp))
                    Text(tr("Todavía no hay nada guardado", "Nothing saved yet"), fontSize = 18.sp, color = KText)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        tr("Toca + para agregar una cuenta, una tarjeta o una frase semilla.",
                            "Tap + to add an account, a card or a seed phrase."),
                        color = KDim, textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filter == null, onClick = { filter = null },
                        label = { Text(tr("Todo", "All")) })
                    EntryType.values().forEach { t ->
                        FilterChip(selected = filter == t, onClick = { filter = if (filter == t) null else t },
                            label = { Text(model.typeLabel(t)) })
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { e -> EntryRow(e) { model.nav = Nav.Detail(e.id) } }
                }
            }
        }

        FloatingActionButton(
            onClick = { model.nav = Nav.Edit(null) },
            containerColor = KAccent,
            contentColor = androidx.compose.ui.graphics.Color(0xFF14102E),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, tr("Agregar", "Add")) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(e: Entry, onOpen: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    val words = tr("palabras", "words")
    val subtitle = when (e.type) {
        EntryType.LOGIN -> e.f(K.USER).ifBlank { hostOf(e.f(K.URL)) }
        EntryType.CARD -> "•••• " + e.f(K.NUMBER).filter { it.isDigit() }.takeLast(4)
        EntryType.SEED -> "${e.f(K.PHRASE).trim().split(Regex("\\s+")).size} $words"
    }
    Row(
        Modifier.fillMaxWidth().clip(shape).border(BorderStroke(1.dp, KBorder), shape).background(KCard)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpen()
                }
            ).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color(0xFF26233F)),
            contentAlignment = Alignment.Center
        ) { Icon(typeIcon(e.type), null, tint = KAccent) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, fontSize = 17.sp, color = KText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 14.sp, color = KDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

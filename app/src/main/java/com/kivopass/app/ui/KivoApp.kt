package com.kivopass.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.kivopass.app.AppModel
import com.kivopass.app.Nav

@Composable
fun KivoApp(model: AppModel, activity: FragmentActivity) {
    CompositionLocalProvider(LocalLang provides model.lang) {
        Surface(modifier = Modifier.fillMaxSize(), color = KBg) {
            when {
                model.needsSetup -> SetupScreen(model, activity)
                !model.unlocked -> LockScreen(model, activity)
                else -> when (val n = model.nav) {
                    is Nav.Home -> HomeScreen(model)
                    is Nav.Detail -> DetailScreen(model, n.id)
                    is Nav.Edit -> EditScreen(model, n.id)
                    is Nav.Settings -> SettingsScreen(model, activity)
                }
            }
        }
    }
}

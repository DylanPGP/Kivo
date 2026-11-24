package com.kivopass.app

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.kivopass.app.ui.KivoApp
import com.kivopass.app.ui.KivoTheme

class MainActivity : FragmentActivity() {
    private lateinit var model: AppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bloquea capturas de pantalla y la vista previa en recientes.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        model = AppModel(applicationContext)
        setContent {
            KivoTheme {
                KivoApp(model, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        model.onForegrounded()
    }

    override fun onStop() {
        super.onStop()
        model.onBackgrounded()
    }
}

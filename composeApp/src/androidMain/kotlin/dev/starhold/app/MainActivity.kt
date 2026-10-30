package dev.starhold.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.starhold.app.platform.AndroidPlatform
import dev.starhold.core.store.AndroidKeyValueStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark chrome with light status/navigation icons: the app is always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        AndroidPlatform.appContext = applicationContext
        val store = AndroidKeyValueStore(applicationContext)
        setContent { StarholdApp(store) }
    }
}

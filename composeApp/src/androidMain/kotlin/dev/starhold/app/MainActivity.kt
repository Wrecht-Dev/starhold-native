package dev.starhold.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.starhold.app.platform.AndroidPlatform
import dev.starhold.core.store.AndroidKeyValueStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidPlatform.appContext = applicationContext
        val store = AndroidKeyValueStore(applicationContext)
        setContent { StarholdApp(store) }
    }
}

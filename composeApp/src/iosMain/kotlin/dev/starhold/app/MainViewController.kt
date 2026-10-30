package dev.starhold.app

import androidx.compose.ui.window.ComposeUIViewController
import dev.starhold.core.store.NsUserDefaultsKeyValueStore
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val store = NsUserDefaultsKeyValueStore()
    return ComposeUIViewController { StarholdApp(store) }
}

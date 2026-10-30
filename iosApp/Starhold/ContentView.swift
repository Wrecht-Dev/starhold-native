import SwiftUI
import UIKit
import StarholdApp

/// Hosts the shared Compose UI. All navigation, playback and theming live in
/// Kotlin; this file only bridges UIKit lifecycle.
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

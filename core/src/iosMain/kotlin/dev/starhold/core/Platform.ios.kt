package dev.starhold.core

import platform.UIKit.UIDevice

actual val platformName: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

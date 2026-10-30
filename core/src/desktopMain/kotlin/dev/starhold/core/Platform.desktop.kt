package dev.starhold.core

actual val platformName: String =
    System.getProperty("os.name", "Desktop")

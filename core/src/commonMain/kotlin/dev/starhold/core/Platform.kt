package dev.starhold.core

/** Identifies the host so UI and diagnostics can adapt without guessing. */
expect val platformName: String

/** Version of the shared Starhold core. */
const val CORE_VERSION: String = "0.1.0"

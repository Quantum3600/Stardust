package io.github.quantum3600.stardust

import android.os.Build

actual fun platformInfo(): String =
    if (Build.VERSION.SDK_INT >= 33) "Android ${Build.VERSION.SDK_INT} · AGSL"
    else "Android ${Build.VERSION.SDK_INT} · OpenGL ES 3.0 fallback"

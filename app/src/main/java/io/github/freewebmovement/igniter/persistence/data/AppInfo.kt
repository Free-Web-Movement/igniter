package io.github.freewebmovement.igniter.persistence.data

import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppInfo : Cloneable {
    var appName: String? = null
    var icon: Drawable? = null
    var packageName: String? = null
    var isSystemApp: Boolean = false

    /**
     * Backed by Compose state so the proxy-apps list reacts immediately when a
     * row's switch is toggled, even though the AppInfo object itself is reused.
     */
    var enabled: Boolean by mutableStateOf(false)

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        val appInfo = super.clone() as AppInfo
        appInfo.appName = appName
        appInfo.icon = icon
        appInfo.packageName = packageName
        appInfo.enabled = enabled
        appInfo.isSystemApp = isSystemApp
        return appInfo
    }
}

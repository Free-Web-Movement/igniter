package io.github.freewebmovement.igniter.persistence.data

import android.graphics.drawable.Drawable

class AppInfo : Cloneable {
    var appName: String? = null
    var icon: Drawable? = null
    var packageName: String? = null
    var enabled: Boolean = false
    var isSystemApp: Boolean = false

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

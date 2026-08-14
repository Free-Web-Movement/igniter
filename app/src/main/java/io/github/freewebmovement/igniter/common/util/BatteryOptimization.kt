package io.github.freewebmovement.igniter.common.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for keeping Igniter alive in the background. Aggressive battery
 * managers (Samsung, Xiaomi, etc.) kill even foreground services unless the
 * app is whitelisted, so Igniter guides the user to grant that whitelist.
 */
object BatteryOptimization {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Shows the system dialog that asks to add Igniter to the battery
     * optimization whitelist. Falls back to the whitelist settings page when
     * the dialog activity is unavailable.
     */
    fun requestIgnoreOptimizations(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openBatteryOptimizationSettings(context)
        }
    }

    /** Opens the "battery optimization" settings list for all apps. */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppDetailsSettings(context)
        }
    }

    /**
     * Opens the OEM-specific "background usage limits" page (autostart / app
     * protection / battery management) for the current device, so the user can
     * exempt Igniter from background killing. Returns true when a matching
     * system page was opened, false when the device has no known page.
     */
    fun openOemBackgroundLimits(context: Context): Boolean {
        val oem = oemKey() ?: return false
        val candidates = OEM_BACKGROUND_LIMITS[oem].orEmpty()
        for (candidate in candidates) {
            try {
                val (pkg, cls) = candidate.split('/')
                val intent = Intent().apply {
                    component = ComponentName(pkg, cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                // try the next candidate
            }
        }
        return false
    }

    /**
     * Maps the current device to a known OEM battery-management brand, or null
     * when it has no dedicated page in [OEM_BACKGROUND_LIMITS].
     */
    private fun oemKey(): String? {
        val value = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return when {
            "samsung" in value -> "samsung"
            "xiaomi" in value || "redmi" in value || "poco" in value -> "xiaomi"
            "huawei" in value || "honor" in value -> "huawei"
            "oppo" in value || "realme" in value || "oneplus" in value -> "oppo"
            "vivo" in value || "iqoo" in value -> "vivo"
            "meizu" in value -> "meizu"
            "lenovo" in value -> "lenovo"
            "asus" in value -> "asus"
            else -> null
        }
    }

    private val OEM_BACKGROUND_LIMITS: Map<String, List<String>> = mapOf(
        "samsung" to listOf(
            "com.samsung.android.lool/.ui.battery.BatteryActivity",
            "com.samsung.android.sm/.ui.battery.BatteryActivity",
            "com.samsung.android.sm.battery.ui.BatteryActivity"
        ),
        "xiaomi" to listOf(
            "com.miui.securitycenter/.com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.miui.powerkeeper/.ui.HiddenAppsConfigActivity",
            "com.miui.powerkeeper/.ui.PowerKeeperActivity"
        ),
        "huawei" to listOf(
            "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity",
            "com.huawei.systemmanager/.optimize.process.ProtectActivity"
        ),
        "oppo" to listOf(
            "com.oplus.safecenter/.startupapp.StartupAppListActivity",
            "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter/.startupapp.StartupAppListActivity",
            "com.coloros.oppoguardelf/.permission.startup.StartupAppListActivity",
            "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity"
        ),
        "vivo" to listOf(
            "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
            "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManagerActivity"
        ),
        "meizu" to listOf(
            "com.meizu.safe/.permission.SmartBGActivity"
        ),
        "lenovo" to listOf(
            "com.lenovo.safecenter/.permission.smartcontrol.MainActivity"
        ),
        "asus" to listOf(
            "com.asus.mobilemanager/.autostart.AutoStartActivity"
        )
    )

    private fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

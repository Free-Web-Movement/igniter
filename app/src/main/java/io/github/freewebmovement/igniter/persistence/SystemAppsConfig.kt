package io.github.freewebmovement.igniter.persistence

import android.content.pm.ApplicationInfo
import io.github.freewebmovement.igniter.IgniterApplication

class SystemAppsConfig(private val app: IgniterApplication) {
    private var records: Array<String>? = null

    init {
        records = Storage.readLines(app.storage.path.systemApps!!)
    }

    fun getRecords(): Array<String>? {
        return records
    }

    fun isSystemApps(packageName: String): Boolean {
        records?.forEach { filter ->
            if (filter.isNotEmpty() && packageName.startsWith(filter)) {
                return true
            }
        }
        return false
    }

    /**
     * Scans the installed applications, detects the ones the platform flags as
     * system apps (FLAG_SYSTEM / FLAG_UPDATED_SYSTEM_APP), and appends the
     * package names that are not already covered by an existing filter to
     * `config_system_apps.txt`. This keeps the list in sync with the device
     * automatically, so newly installed/updated system apps are classified
     * without a manual edit.
     */
    @Synchronized
    fun updateFromInstalledApps(installed: List<ApplicationInfo>) {
        val detected = ArrayList<String>()
        for (info in installed) {
            val flags = info.flags
            val isSystem = (flags and
                (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
            if (isSystem && !isSystemApps(info.packageName)) {
                detected.add(info.packageName)
            }
        }
        if (detected.isEmpty()) {
            return
        }
        val merged = (records?.toMutableList() ?: mutableListOf())
        merged.addAll(detected)
        merged.sort()
        records = merged.toTypedArray()
        Storage.write(app.storage.path.systemApps!!, merged.joinToString("\n").toByteArray())
    }
}

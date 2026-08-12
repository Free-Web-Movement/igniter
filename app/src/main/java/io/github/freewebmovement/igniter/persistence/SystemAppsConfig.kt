package io.github.freewebmovement.igniter.persistence

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
            if (packageName.startsWith(filter)) {
                return true
            }
        }
        return false
    }
}

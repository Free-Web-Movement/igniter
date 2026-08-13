package io.github.freewebmovement.igniter.persistence.data

import androidx.annotation.WorkerThread

interface ExemptAppDataSource {
    /**
     * Load exempt applications' package names.
     *
     * @return exempt applications' package names..
     */
    @WorkerThread
    fun loadExemptAppPackageNameSet(): Set<String>

    /**
     * Save exempt applications' package names.
     *
     * @param exemptAppPackageNames exempt app package name set
     */
    @WorkerThread
    fun saveExemptAppInfoSet(exemptAppPackageNames: Set<String>)

    /**
     * Load all application info list, including exempt apps and non-exempt apps.
     * @return all application info list
     */
    @WorkerThread
    fun getAllAppInfoList(): List<AppInfo>

    /**
     * Load all currently installed applications' package names.
     *
     * @return all installed package names
     */
    @WorkerThread
    fun getAllInstalledPackageNames(): Set<String>
}

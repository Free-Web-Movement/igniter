package io.github.freewebmovement.igniter.persistence.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.persistence.Storage

/**
 * Implementation of [ExemptAppDataSource]. This class reads and writes exempted app list in a
 * file. The exempted app package names will be written line by line in the file.
 */
class ExemptAppDataManager(private val app: IgniterApplication) : ExemptAppDataSource {
    private val mPackageManager: PackageManager = app.packageManager

    override fun saveExemptAppInfoSet(exemptAppPackageNames: Set<String>) {
        if (exemptAppPackageNames.isEmpty()) {
            return
        }
        val exemptApps = StringBuilder()
        for (name in exemptAppPackageNames) {
            exemptApps.append(name).append("\n")
        }
        Storage.write(app.storage.path.exemptedAppList!!, exemptApps.toString().toByteArray())
    }

    private fun readExemptAppListConfig(): Array<String> {
        val config = Storage.read(app.storage.path.exemptedAppList!!)
            ?: return arrayOf()
        val exemptApps = String(config)
        return exemptApps.split("\\r?\\n".toRegex()).toTypedArray()
    }

    override fun loadExemptAppPackageNameSet(): Set<String> {
        val exemptAppPackageNames = readExemptAppListConfig()
        // filter uninstalled apps
        val applicationInfoList = queryCurrentInstalledApps()
        val installedAppPackageNames = HashSet<String>()
        for (applicationInfo in applicationInfoList) {
            installedAppPackageNames.add(applicationInfo.packageName)
        }
        val ret = HashSet<String>()
        if (exemptAppPackageNames.isEmpty()) {
            //exempt all packages by default!
            saveExemptAppInfoSet(installedAppPackageNames)
            return installedAppPackageNames
        }
        for (packageName in exemptAppPackageNames) {
            if (installedAppPackageNames.contains(packageName)) {
                ret.add(packageName)
            }
        }
        return ret
    }

    private fun queryCurrentInstalledApps(): List<ApplicationInfo> {
        var flags = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags or (PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS)
        } else {
            flags = flags or (PackageManager.GET_UNINSTALLED_PACKAGES or PackageManager.GET_DISABLED_COMPONENTS)
        }
        return mPackageManager.getInstalledApplications(flags)
    }

    fun enableAll(isEnable: Boolean) {
        if (isEnable) {
            Storage.write(app.storage.path.exemptedAppList!!, byteArrayOf())
        } else {
            val applicationInfoList = queryCurrentInstalledApps()
            val installedAppPackageNames = HashSet<String>()
            for (applicationInfo in applicationInfoList) {
                installedAppPackageNames.add(applicationInfo.packageName)
            }
            saveExemptAppInfoSet(installedAppPackageNames)
        }
    }

    override fun getAllAppInfoList(): List<AppInfo> {
        val applicationInfoList = queryCurrentInstalledApps()
        val showSystemApps = IgniterApplication.getApplication().trojanPreferences.getShowSystemApps()

        val appInfoList = ArrayList<AppInfo>()
        for (applicationInfo in applicationInfoList) {
            if (!showSystemApps) {
                if (app.systemAppsConfig.isSystemApps(applicationInfo.packageName)) {
                    continue
                }
            }
            val appInfo = AppInfo()
            appInfo.appName = mPackageManager.getApplicationLabel(applicationInfo).toString()
            appInfo.packageName = applicationInfo.packageName
            appInfo.icon = mPackageManager.getApplicationIcon(applicationInfo)
            appInfoList.add(appInfo)
        }
        return appInfoList
    }
}

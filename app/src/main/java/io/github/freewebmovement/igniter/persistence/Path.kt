package io.github.freewebmovement.igniter.persistence

import android.content.Context
import io.github.freewebmovement.igniter.R
import java.io.File

class Path internal constructor(val context: Context) {
    companion object {
        const val CACHE = 0
        const val FILES = 1
    }

    @JvmField
    val dirs = arrayOfNulls<File>(2)
    @JvmField
    var countryMmdb: String? = null
    @JvmField
    var clashConfig: String? = null
    @JvmField
    var trojanConfig: String? = null
    @JvmField
    var trojanConfigList: String? = null
    @JvmField
    var exemptedAppList: String? = null
    @JvmField
    var caCert: String? = null
    @JvmField
    var systemApps: String? = null

    init {
        dirs[CACHE] = context.cacheDir
        dirs[FILES] = context.filesDir
        countryMmdb = get(FILES, context.getString(R.string.country_mmdb_config))
        clashConfig = get(FILES, context.getString(R.string.clash_config))
        trojanConfig = get(FILES, context.getString(R.string.trojan_config))
        trojanConfigList = get(FILES, context.getString(R.string.trojan_list_config))
        exemptedAppList = get(FILES, context.getString(R.string.exempted_app_list_config))
        caCert = get(FILES, context.getString(R.string.ca_cert_config))
        systemApps = get(FILES, context.getString(R.string.system_apps_config))
    }

    fun get(type: Int, filename: String): String? {
        return when (type) {
            CACHE, FILES -> File(dirs[type]!!, filename).path
            else -> null
        }
    }
}

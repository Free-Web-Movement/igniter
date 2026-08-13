package io.github.freewebmovement.igniter

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.persistence.ClashConfig
import io.github.freewebmovement.igniter.persistence.Storage
import io.github.freewebmovement.igniter.persistence.SystemAppsConfig
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.TrojanPreferences
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataManager
import io.github.freewebmovement.igniter.services.ProxyService

class IgniterApplication : Application() {

    // Sharable Singletons, assigned in init() before anything else runs.
    lateinit var storage: Storage
    lateinit var clashConfig: ClashConfig
    lateinit var trojanConfig: TrojanConfig
    lateinit var trojanPreferences: TrojanPreferences
    lateinit var systemAppsConfig: SystemAppsConfig
    lateinit var exemptAppDataManager: ExemptAppDataManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        init()
    }

    fun init() {
        trojanPreferences = TrojanPreferences(this)

        storage = Storage(this)
        storage.check()
        // Make sure the CA file exists;
        trojanConfig = TrojanConfig.getInstance(storage)
        clashConfig = ClashConfig(storage.path.clashConfig!!)
        systemAppsConfig = SystemAppsConfig(this)
        exemptAppDataManager = ExemptAppDataManager(this)
    }

    fun startProxyService() {
        val intent = Intent(this, ProxyService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    fun stopProxyService() {
        val intent = Intent(getString(R.string.stop_service))
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    fun restartProxyService() {
        val intent = Intent(getString(R.string.restart_service))
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    fun startLauncherActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    companion object {
        @JvmField
        var instance: IgniterApplication? = null

        @JvmStatic
        fun getApplication(): IgniterApplication = instance!!
    }
}

package io.github.freewebmovement.igniter.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import androidx.annotation.IntDef
import androidx.core.app.NotificationCompat
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.connection.TestConnection
import io.github.freewebmovement.igniter.persistence.NetWorkConfig
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataManager
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataSource
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanServiceCallback
import io.github.freewebmovement.igniter.receivers.WatchdogManager

class ProxyService : VpnService(), TestConnection.OnResultListener {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ProxyService.STATE_NONE, ProxyService.STARTING, ProxyService.STARTED, ProxyService.STOPPING, ProxyService.STOPPED)
    annotation class ProxyState

    private val mHandler = Handler(Looper.getMainLooper())
    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    @Volatile
    private var state: Int = STATE_NONE

    @Volatile
    private var pfd: ParcelFileDescriptor? = null
    private var mExemptAppDataSource: ExemptAppDataSource? = null
    /**
     * Receives stop/restart events.
     */
    private val mStopBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.stop_service) -> stop()
                getString(R.string.restart_service) -> restart()
            }
        }
    }
    /**
     * Callback list for remote processes or services.
     */
    private val mCallbackList = RemoteCallbackList<ITrojanServiceCallback>()
    /**
     * Binder implementation of [ITrojanService], which provides access of connection state,
     * connection test and callback registration.
     */
    private val mBinder: IBinder = object : ITrojanService.Stub() {
        override fun getState(): Int {
            Log.i(TAG, "IBinder getState# : ${this@ProxyService.state}")
            return this@ProxyService.state
        }

        override fun testConnection(testUrl: String) {
            if (this@ProxyService.state != STARTED) {
                onResult(TUN2SOCKS5_SERVER_HOST, false, 0L, "ProxyService not yet connected.")
                return
            }
            val port = app.trojanConfig.getLocalPort()
            TestConnection(TUN2SOCKS5_SERVER_HOST, port.toLong(), this@ProxyService).execute(testUrl)
        }

        override fun showDevelopInfoInLogcat() {
            // Log.showDevelopInfoInLogcat()
        }

        override fun registerCallback(callback: ITrojanServiceCallback) {
            Log.i(TAG, "IBinder registerCallback#")
            mCallbackList.register(callback)
        }

        override fun unregisterCallback(callback: ITrojanServiceCallback) {
            Log.i(TAG, "IBinder unregisterCallback#")
            mCallbackList.unregister(callback)
        }
    }

    private fun setState(state: Int) {
        Log.i(TAG, "setState: $state")
        this.state = state
        notifyStateChange()
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        val filter = IntentFilter()
        filter.addAction(getString(R.string.stop_service))
        filter.addAction(getString(R.string.restart_service))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // only for gingerbread and newer versions
            registerReceiver(mStopBroadcastReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(mStopBroadcastReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        mCallbackList.kill()
        setState(STOPPED)
        app.trojanPreferences.setVpnActive(false)
        WatchdogManager.cancel(this)
        unregisterReceiver(mStopBroadcastReceiver)
        pfd = null
    }

    /**
     * Broadcast the state change event by invoking callbacks from other processes or services.
     */
    private fun notifyStateChange() {
        val state = this.state
        for (i in mCallbackList.beginBroadcast() - 1 downTo 0) {
            try {
                // the second String parameter is currently useless. Might be the url of the profile.
                mCallbackList.getBroadcastItem(i).onStateChanged(state, "state changed")
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        mCallbackList.finishBroadcast()
    }

    override fun onRevoke() {
        // Calls to this method may not happen on the main thread
        // of the process.
        stop()
    }

    override fun onResult(testUrl: String, connected: Boolean, delay: Long, error: String) {
        // broadcast test result by invoking callbacks from other processes or services.
        for (i in mCallbackList.beginBroadcast() - 1 downTo 0) {
            try {
                mCallbackList.getBroadcastItem(i).onTestResult(testUrl, connected, delay, error)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        mCallbackList.finishBroadcast()
    }

    override fun onBind(intent: Intent): IBinder? {
        val bindServiceAction = getString(R.string.bind_service)
        if (bindServiceAction == intent.action) {
            return mBinder
        }
        return super.onBind(intent)
    }

    /**
     * Apps the user manually set to bypass the tunnel (不代理). They are added
     * to the VPN's disallowed-app list so their traffic never enters the
     * tunnel and is never auto-tested by the Socks5Gate.
     */
    private fun getBypassAppPackageNames(): Set<String> {
        if (mExemptAppDataSource == null) {
            mExemptAppDataSource = ExemptAppDataManager(app)
        }
        return mExemptAppDataSource!!.loadExemptAppPackageNameSet()
    }

    /**
     * Start foreground notification to avoid ANR and crash, as Android requires that Service which
     * is started by calling [Context.startForegroundService] must
     * invoke [android.app.Service.startForeground] within 5 seconds.
     */
    private fun startForegroundNotification(channelId: String) {
        val openMainActivityIntent = Intent(this, MainActivity::class.java)
        openMainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingOpenMainActivityIntent = PendingIntent.getActivity(this,
            0, openMainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_starting_service))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingOpenMainActivityIntent)
            .setAutoCancel(false)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setShowWhen(true)
        }
        builder.setWhen(0L)

        // it's required to create a notification channel before startForeground on SDK >= Android O
        createNotificationChannel(channelId)
        Log.i(TAG, "Start foreground notification")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // only for gingerbread and newer versions
            startForeground(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, builder.build())
        }
    }

    fun getVPNBuilder(): VpnService.Builder {
        // The VPN interface (addresses, routes, DNS, MTU) is fully configured in
        // NetWorkConfig.establish() to keep a single source of truth.
        val vpnService: VpnService = this
        return vpnService.Builder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")
        if (state == STARTED || state == STARTING) {
            return START_NOT_STICKY
        }
        // In order to keep the service long-lived, starting the service by Context.startForegroundService()
        // might be the easiest way. According to the official indication, a service which is started
        // by Context.startForegroundService() must call Service.startForeground() within 5 seconds.
        // Otherwise the process will be shutdown and user will get an ANR notification.
        startForegroundNotification(getString(R.string.notification_channel_id))
        setState(STARTING)

        // All apps go through the tunnel by default; the Socks5Gate
        // auto-detect decides per domain whether to connect directly or via
        // Clash. Only Igniter itself (to avoid loops: it must reach Clash and
        // the physical DNS directly) and the apps the user manually set to
        // 不代理 bypass the VPN.
        val directPackageNames = mutableSetOf(getPackageName())
        directPackageNames.addAll(getBypassAppPackageNames())

        // VPN setup performs blocking I/O (port probing, native startup) and must
        // not run on the main thread.
        Thread({ startVpn(directPackageNames) }, "igniter-vpn-start").start()
        return START_STICKY
    }

    private fun startVpn(packageNames: Set<String>) {
        val statusStr: String
        try {
            pfd = NetWorkConfig.establish(
                app,
                getVPNBuilder(),
                getString(R.string.app_name),
                packageNames
            )
            Log.i("VPN", "pfd established")
            if (pfd == null) {
                throw IllegalStateException(getString(R.string.error_establish_vpn))
            }
            statusStr = NetWorkConfig.startService(app, pfd!!.detachFd())
        } catch (e: SecurityException) {
            Log.e(TAG, "VPN permission revoked", e)
            failStart(getString(R.string.error_vpn_permission_revoked))
            return
        } catch (e: Exception) {
            Log.e(TAG, "failed to start proxy", e)
            val message = e.message
            failStart(if (message.isNullOrEmpty()) e.toString() else message)
            return
        }
        mHandler.post {
            setState(STARTED)
            app.trojanPreferences.setVpnActive(true)
            WatchdogManager.schedule(this)
            val openMainActivityIntent = Intent(this, MainActivity::class.java)
            openMainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingOpenMainActivityIntent = PendingIntent.getActivity(this, 0, openMainActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            val channelId = getString(R.string.notification_channel_id)
            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.service_is_running))
                .setContentText(statusStr)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(statusStr))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingOpenMainActivityIntent)
                .setAutoCancel(false)
                .setOngoing(true)
            startForeground(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, builder.build())
        }
    }

    /**
     * Stops the VPN after a startup failure, showing the reason briefly so the
     * user can tell why the connection did not come up.
     */
    private fun failStart(message: String) {
        Log.e(TAG, "failStart: $message")
        try {
            val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.service_failed))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setState(STOPPED)
        NetWorkConfig.stop(app)
        pfd = null
        app.trojanPreferences.setVpnActive(false)
        WatchdogManager.cancel(this)
        // Give the error notification a moment to be seen before the process is recycled.
        Handler(Looper.getMainLooper()).postDelayed({ stop() }, 1500)
    }

    private fun shutdown() {
        Log.i(TAG, "shutdown")
        setState(STOPPING)
        app.trojanPreferences.setVpnActive(false)
        WatchdogManager.cancel(this)

        NetWorkConfig.stop(app)
        stopSelf()

        setState(STOPPED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(true)
        }
        destroyNotificationChannel(getString(R.string.notification_channel_id))
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId,
                getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun destroyNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.deleteNotificationChannel(channelId)
        }
    }

    fun stop() {
        shutdown()
        // this is essential for goMobile aar
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    /**
     * Restarts the proxy so the freshly saved exempt-app list takes effect on
     * the VPN interface. The native (go) stack cannot be re-initialized within
     * the same process, so the activity is relaunched (which auto-starts the
     * proxy in the fresh process) before tearing down this process.
     */
    fun restart() {
        Log.i(TAG, "restarting proxy to apply new exempt app configuration")
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(MainActivity.EXTRA_AUTO_START_PROXY, true)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "failed to relaunch activity on restart", e)
        }
        stop()
    }

    companion object {
        private const val TAG = "ProxyService"

        const val STATE_NONE = -1
        const val STARTING = 0
        const val STARTED = 1
        const val STOPPING = 2
        const val STOPPED = 3
        const val PROXY_SERVICE_STATUS_NOTIFY_MSG_ID = 114514

        private         const val TUN2SOCKS5_SERVER_HOST = "127.0.0.1"
    }
}

package io.github.freewebmovement.igniter.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import io.github.freewebmovement.igniter.IgniterApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.v("On Receiver", "Message!")
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.v("On Receiver", "Boot Message!")
            val app = IgniterApplication.getApplication()
            if (app.trojanPreferences.isEnableBootStart()) {
                // Skip when the VPN permission was revoked (e.g. by aggressive
                // battery managers); starting without it would crash the process.
                if (VpnService.prepare(context) == null) {
                    Log.v("On Receiver", "Boot Start!")
                    app.startProxyService()
                }
            }
        }
    }
}

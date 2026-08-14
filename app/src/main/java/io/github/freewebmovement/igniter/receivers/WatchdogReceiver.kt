package io.github.freewebmovement.igniter.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.freewebmovement.igniter.IgniterApplication

/**
 * Watchdog: while the tunnel is supposed to be running, periodically verify the
 * ":proxy" process is actually alive and restart it if the system killed it.
 * Aggressive battery managers (Samsung/Xiaomi...) kill even foreground services
 * when the app is not whitelisted; START_STICKY alone is not enough because
 * those managers also refuse the service restart.
 *
 * The alarm is only active while the VPN runs: ProxyService schedules it when
 * the tunnel is established and cancels it when the user stops. Each tick
 * reschedules the next one.
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WATCHDOG) {
            return
        }
        Log.i(TAG, "watchdog tick")
        val app = IgniterApplication.getApplication()
        app.trojanPreferences.reload()
        if (!app.trojanPreferences.isVpnActive()) {
            // The tunnel is not supposed to run (the user stopped it, or this
            // is a stale alarm left over from an earlier session): terminate
            // the watchdog instead of rescheduling, so it never keeps waking
            // the device up while the VPN is off.
            WatchdogManager.cancel(context)
            return
        }
        // No-op when the service is still running; revives the tunnel when the
        // :proxy process was killed behind our back.
        WatchdogManager.schedule(context)
        Log.i(TAG, "vpn should be running, ensure proxy service alive")
        app.startProxyService()
    }

    companion object {
        private const val TAG = "WatchdogReceiver"
        const val ACTION_WATCHDOG = "io.github.freewebmovement.igniter.WATCHDOG"
    }
}

object WatchdogManager {
    private const val INTERVAL_MS = 10 * 60 * 1000L

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + INTERVAL_MS,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WatchdogReceiver::class.java)
            .setAction(WatchdogReceiver.ACTION_WATCHDOG)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

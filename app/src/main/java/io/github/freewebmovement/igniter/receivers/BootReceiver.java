package io.github.freewebmovement.igniter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import io.github.freewebmovement.igniter.IgniterApplication;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("On Receiver", "Message!");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.v("On Receiver", "Boot Message!");
            IgniterApplication app = IgniterApplication.getApplication();
            if (app.trojanPreferences.isEnableBootStart()) {
                // Skip when the VPN permission was revoked (e.g. by aggressive
                // battery managers); starting without it would crash the process.
                if (VpnService.prepare(context) == null) {
                    Log.v("On Receiver", "Boot Start!");
                    app.startProxyService();
                }
            }
        }
    }
}

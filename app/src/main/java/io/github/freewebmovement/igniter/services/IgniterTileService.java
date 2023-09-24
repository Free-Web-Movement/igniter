package io.github.freewebmovement.igniter.services;

import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.RemoteException;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.connection.TrojanConnection;
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService;
import io.github.freewebmovement.igniter.activities.MainActivity;

@RequiresApi(api = Build.VERSION_CODES.N)
public class IgniterTileService extends TileService implements TrojanConnection.Callback {
    private static final String TAG = "IgniterTile";
    private final TrojanConnection mConnection = new TrojanConnection(false);
    /**
     * Indicates that user had tapped the tile before {@link TrojanConnection} connects {@link ProxyService}.
     * Generally speaking, when the connection is built, we should call {@link #onClick()} again if
     * the value is <code>true</code>.
     */
    private boolean mTapPending;

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.i(TAG, "onStartListening");
        mConnection.connect(this, this);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.i(TAG, "onStopListening");
        mConnection.disconnect(this);
    }

    @Override
    public void onServiceConnected(ITrojanService service) {
        Log.i(TAG, "onServiceConnected");
        try {
            int state = service.getState();
            updateTile(state);
            if (mTapPending) {
                mTapPending = false;
                onClick();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected() {
        Log.i(TAG, "onServiceDisconnected");
    }

    @Override
    public void onStateChanged(int state, String msg) {
        Log.i(TAG, "onStateChanged# state: " + state + ", msg: " + msg);
        updateTile(state);
    }

    @Override
    public void onTestResult(String testUrl, boolean connected, long delay, @NonNull String error) {
        // Do nothing, since TileService will not submit test request.
    }

    @Override
    public void onBinderDied() {
        Log.i(TAG, "onBinderDied");
    }

    private void updateTile(final @ProxyService.ProxyState int state) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        Log.i(TAG, "updateTile with state: " + state);
        switch (state) {
            case ProxyService.STATE_NONE:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.app_name));
                break;
            case ProxyService.STARTED:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getString(R.string.tile_on));
                break;
            case ProxyService.STARTING:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getString(R.string.tile_starting));
                break;
            case ProxyService.STOPPED:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_off));
                break;
            case ProxyService.STOPPING:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_stopping));
                break;
            default:
                Log.e(TAG, "Unknown state: " + state);
                break;
        }
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.i(TAG, "onClick");
        IgniterApplication app = IgniterApplication.getApplication();

        if (app.trojanPreferences.isEverStarted()) {
            // if user never open Igniter before, when he/she clicks the tile, it is necessary
            // to start the launcher activity for resource preparation.
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        ITrojanService service = mConnection.getService();
        if (service == null) {
            mTapPending = true;
        } else {
            try {
                @ProxyService.ProxyState int state = service.getState();
                switch (state) {
                    case ProxyService.STARTED:
                        stopProxyService();
                        break;
                    case ProxyService.STARTING:
                    case ProxyService.STOPPING:
                        break;
                    case ProxyService.STATE_NONE:
                    case ProxyService.STOPPED:
                        startProxyService();
                        break;
                    default:
                        Log.e(TAG, "Unknown state: " + state);
                        break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start ProxyService if everything is ready. Otherwise start the launcher Activity.
     */
    private void startProxyService() {
        IgniterApplication app = IgniterApplication.getApplication();
        if (app.trojanConfig.isValidRunningConfig() && VpnService.prepare(this.getApplicationContext()) == null) {
            app.startProxyService();
        } else {
            app.startLauncherActivity();
        }
    }

    private void stopProxyService() {
        IgniterApplication app = IgniterApplication.getApplication();
        app.stopProxyService();
    }
}

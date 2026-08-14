package io.github.freewebmovement.igniter.services

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.RemoteException
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.connection.TrojanConnection
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService

@RequiresApi(Build.VERSION_CODES.N)
class IgniterTileService : TileService(), TrojanConnection.Callback {
    private val mConnection = TrojanConnection(false)
    /**
     * Indicates that user had tapped the tile before [TrojanConnection] connects [ProxyService].
     * Generally speaking, when the connection is built, we should call [onClick] again if
     * the value is `true`.
     */
    private var mTapPending = false

    override fun onStartListening() {
        super.onStartListening()
        Log.i(TAG, "onStartListening")
        // Only bind while the tunnel is actually running. Binding to a stopped
        // service keeps a dead :proxy process alive as an idle zombie.
        if (IgniterApplication.getApplication().trojanPreferences.isVpnActive()) {
            mConnection.connect(this, this)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.i(TAG, "onStopListening")
        mConnection.disconnect(this)
    }

    override fun onServiceConnected(service: ITrojanService) {
        Log.i(TAG, "onServiceConnected")
        try {
            val state = service.getState()
            updateTile(state)
            if (mTapPending) {
                mTapPending = false
                onClick()
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onServiceDisconnected() {
        Log.i(TAG, "onServiceDisconnected")
    }

    override fun onStateChanged(state: Int, msg: String?) {
        Log.i(TAG, "onStateChanged# state: $state, msg: $msg")
        updateTile(state)
        if (state == ProxyService.STOPPED && mConnection.isConnected()) {
            mConnection.disconnect(this)
        }
    }

    override fun onTestResult(testUrl: String?, connected: Boolean, delay: Long, @NonNull error: String) {
        // Do nothing, since TileService will not submit test request.
    }

    override fun onBinderDied() {
        Log.i(TAG, "onBinderDied")
    }

    private fun updateTile(@ProxyService.ProxyState state: Int) {
        val tile = qsTile
        if (tile == null) {
            return
        }
        Log.i(TAG, "updateTile with state: $state")
        when (state) {
            ProxyService.STATE_NONE -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.app_name)
            }
            ProxyService.STARTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_on)
            }
            ProxyService.STARTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_starting)
            }
            ProxyService.STOPPED -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_off)
            }
            ProxyService.STOPPING -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_stopping)
            }
            else -> Log.e(TAG, "Unknown state: $state")
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.i(TAG, "onClick")
        val app = IgniterApplication.getApplication()

        if (app.trojanPreferences.isEverStarted()) {
            // if user never open Igniter before, when he/she clicks the tile, it is necessary
            // to start the launcher activity for resource preparation.
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            return
        }
        val service = mConnection.getService()
        if (service == null) {
            mTapPending = true
            if (!mConnection.isConnected()) {
                // The tile only binds while the tunnel is running; connect on
                // demand so a tap can start a stopped tunnel.
                mConnection.connect(this, this)
            }
        } else {
            try {
                val state = service.getState()
                when (state) {
                    ProxyService.STARTED -> stopProxyService()
                    ProxyService.STARTING, ProxyService.STOPPING -> {
                    }
                    ProxyService.STATE_NONE, ProxyService.STOPPED -> startProxyService()
                    else -> Log.e(TAG, "Unknown state: $state")
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start ProxyService if everything is ready. Otherwise start the launcher Activity.
     */
    private fun startProxyService() {
        val app = IgniterApplication.getApplication()
        if (app.trojanConfig.isValidRunningConfig() && VpnService.prepare(applicationContext) == null) {
            app.startProxyService()
        } else {
            app.startLauncherActivity()
        }
    }

    private fun stopProxyService() {
        val app = IgniterApplication.getApplication()
        app.stopProxyService()
    }

    companion object {
        private const val TAG = "IgniterTile"
    }
}

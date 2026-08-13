package io.github.freewebmovement.igniter.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.github.freewebmovement.igniter.connection.ServerPingManager
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Continuously pings every server in the list so the home page can show each
 * server's current/ average latency and connectivity. The loop reads the
 * server list from the database on every round, so newly added servers are
 * picked up automatically.
 */
class ServerPingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob?.isActive != true) {
            loopJob = scope.launch {
                while (isActive) {
                    val servers = withContext(Dispatchers.IO) {
                        AccessDatabase.getDatabase().serverDao().all()
                    }
                    for (server in servers) {
                        ServerPingManager.ping(server.hostname, server.port)
                    }
                    delay(PING_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PING_INTERVAL_MS = 10_000L

        fun start(context: Context) {
            context.startService(Intent(context, ServerPingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ServerPingService::class.java))
        }
    }
}

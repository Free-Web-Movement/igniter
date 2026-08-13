package io.github.freewebmovement.igniter.connection

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayDeque
import java.util.concurrent.Executors
import kotlin.math.roundToLong

/**
 * Measures per-server TCP round-trip time (host:port) and exposes the latest
 * ping, the running average and a connectivity ranking so the UI can show how
 * reachable each server currently is.
 */
object ServerPingManager {

    private const val MAX_SAMPLES = 10
    private const val TIMEOUT_MS = 5000L

    /** Connectivity ranked by the latest ping, from fast to unreachable. */
    enum class Connectivity { EXCELLENT, GOOD, FAIR, POOR, UNREACHABLE }

    data class PingInfo(
        val currentMs: Long?,
        val avgMs: Long?,
        val connectivity: Connectivity
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val history = HashMap<String, ArrayDeque<Long>>()

    private val _pingData = MutableLiveData<Map<String, PingInfo>>(emptyMap())
    val pingData: LiveData<Map<String, PingInfo>> = _pingData

    /** Pings one server (host:port) and publishes the result to [pingData]. */
    fun ping(host: String, port: Int) {
        executor.execute {
            val info = measure(host, port)
            mainHandler.post {
                val next = HashMap(_pingData.value.orEmpty())
                next["$host:$port"] = info
                _pingData.value = next
            }
        }
    }

    private fun measure(host: String, port: Int): PingInfo {
        val key = "$host:$port"
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS.toInt())
            }
            val rtt = System.currentTimeMillis() - start
            val samples = history.getOrPut(key) { ArrayDeque() }
            samples.addLast(rtt)
            while (samples.size > MAX_SAMPLES) {
                samples.removeFirst()
            }
            val avg = samples.average().roundToLong()
            PingInfo(rtt, avg, rank(rtt))
        } catch (e: Exception) {
            history.remove(key)
            PingInfo(null, null, Connectivity.UNREACHABLE)
        }
    }

    private fun rank(rtt: Long): Connectivity {
        return when {
            rtt <= 100 -> Connectivity.EXCELLENT
            rtt <= 200 -> Connectivity.GOOD
            rtt <= 400 -> Connectivity.FAIR
            else -> Connectivity.POOR
        }
    }
}

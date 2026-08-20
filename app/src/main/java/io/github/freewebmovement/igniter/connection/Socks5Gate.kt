package io.github.freewebmovement.igniter.connection

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.NewDomainPromptActivity
import io.github.freewebmovement.igniter.constants.Net
import io.github.freewebmovement.igniter.persistence.ClashRouteBuilder
import io.github.freewebmovement.igniter.persistence.DomainRulesManager
import io.github.freewebmovement.igniter.persistence.Storage
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread

/** Per-domain routing outcome of the automatic connectivity detection. */
enum class AutoRoute { DIRECT, PROXY, DNS_POLLUTED, UNREACHABLE }

/**
 * Local SOCKS5 gateway sitting between go-tun2socks and Clash.
 *
 * For every CONNECT the destination is routed by live connectivity detection
 * (see [decide]):
 *  - a manual rule always wins and is honoured without testing;
 *  - known-CN sites go DIRECT if the direct probe passes, else fall back to
 *    the proxy;
 *  - known-foreign sites go PROXY if the proxy probe passes, else fall back
 *    to direct;
 *  - unknown domains are probed DIRECT first then PROXY; a direct resolution
 *    returning a known poisoned IP is reported as DNS-polluted; domains where
 *    both paths fail are persisted to the unreachable list and refused.
 * Decisions are cached per registrable domain.
 *
 * IP-based connections and UDP traffic are relayed to Clash transparently,
 * keeping the existing routing behaviour untouched.
 */
class Socks5Gate(
    private val app: IgniterApplication,
    private val clashPort: Int,
    private val enableIpv6: Boolean
) {

    companion object {
        private const val TAG = "Socks5Gate"

        const val POLICY_PROXY = "Proxy"
        const val POLICY_DIRECT = "DIRECT"

        private const val SOCKS5 = 0x05
        private const val ATYP_IPV4 = 0x01
        private const val ATYP_DOMAIN = 0x03
        private const val ATYP_IPV6 = 0x04
        private const val CMD_CONNECT = 0x01
        private const val CMD_UDP_ASSOCIATE = 0x03
        private const val REP_SUCCEEDED = 0x00
        private const val REP_REFUSED = 0x05
        private const val REP_UNSUPPORTED = 0x07

        private const val PENDING_PREF = "domain_pending"
        private const val PENDING_KEY = "pending"
        const val PENDING_NOTIFY_ID = 115000

        private const val DNS_QUERY_TIMEOUT_MS = 2000
        private const val DNS_TYPE_A = 1
        private const val DNS_TYPE_AAAA = 28

        private const val PROBE_CONNECT_TIMEOUT_MS = 2000
        private const val PROBE_READ_TIMEOUT_MS = 1500
        private const val PROBE_TLS_TIMEOUT_MS = 2000

        private const val UNREACHABLE_PREF = "unreachable_domains"
        private const val UNREACHABLE_KEY = "domains"

        /** Unreachable domains expire after 24 hours and are retried. */
        private const val UNREACHABLE_TTL_MS = 24 * 60 * 60 * 1000L

        /** Well-known IPs returned by DNS poisoning (GFW fake answers). */
        private val KNOWN_POLLUTED_IPS = setOf(
            "0.0.0.0",
            "23.23.43.98",
            "31.13.69.193",
            "31.13.79.193",
            "46.229.164.99",
            "59.24.3.173",
            "61.144.62.19",
            "66.220.242.14",
            "71.6.146.185",
            "71.6.167.142",
            "81.177.139.39",
            "91.218.115.197",
            "118.193.43.209",
            "197.79.3.9",
            "197.254.68.194",
            "198.74.102.69",
            "203.98.7.65",
            "203.99.198.78",
            "203.99.206.17",
            "203.195.170.178"
        )

        /** Compound public suffixes whose registrable domain spans three labels. */
        private val COMPOUND_SUFFIXES = setOf(
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "ac.cn",
            "com.hk", "com.tw", "org.tw", "com.sg", "com.my", "com.au",
            "net.au", "co.uk", "org.uk", "ac.uk", "gov.uk", "co.jp",
            "or.jp", "ne.jp", "ac.jp", "co.kr", "or.kr", "co.nz",
            "com.br", "com.mx", "com.tr", "com.ar", "com.vn", "com.ph",
            "com.th", "co.id", "com.id", "co.in"
        )

        /** Collapses subdomains to their registrable domain (a.t.com -> t.com). */
        private fun rootDomain(host: String): String {
            if (TunnelLogParser.isIpLike(host)) {
                return host
            }
            val labels = host.split('.')
            if (labels.size <= 2) {
                return host
            }
            val suffix = labels.takeLast(2).joinToString(".")
            return labels.takeLast(if (suffix in COMPOUND_SUFFIXES) 3 else 2).joinToString(".")
        }

        @Suppress("DEPRECATION")
        private fun unreachablePrefs(context: Context): SharedPreferences =
            context.getSharedPreferences(UNREACHABLE_PREF, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)

        @JvmStatic
        fun getUnreachable(context: Context): List<String> {
            val now = System.currentTimeMillis()
            val raw = unreachablePrefs(context).getString(UNREACHABLE_KEY, "") ?: ""
            if (raw.isEmpty()) return emptyList()
            val entries = mutableListOf<String>()
            val alive = mutableListOf<String>()
            for (entry in raw.split("||")) {
                if (entry.isEmpty()) continue
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2) {
                    val domain = parts[0]
                    val ts = parts[1].toLongOrNull() ?: 0L
                    if (now - ts < UNREACHABLE_TTL_MS) {
                        entries.add(domain)
                        alive.add(entry)
                    }
                } else {
                    entries.add(entry)
                    alive.add(entry)
                }
            }
            if (alive.size != raw.split("||").filter { it.isNotEmpty() }.size) {
                unreachablePrefs(context).edit().putString(UNREACHABLE_KEY, alive.joinToString("||")).apply()
            }
            return entries
        }

        @JvmStatic
        @Synchronized
        fun addUnreachable(context: Context, domain: String) {
            val entries = getUnreachableRawEntries(context).toMutableMap()
            entries[domain] = System.currentTimeMillis()
            unreachablePrefs(context).edit().putString(
                UNREACHABLE_KEY,
                entries.entries.joinToString("||") { "${it.key}:${it.value}" }
            ).apply()
        }

        @JvmStatic
        @Synchronized
        fun removeUnreachable(context: Context, domain: String) {
            val entries = getUnreachableRawEntries(context).toMutableMap()
            entries.remove(domain)
            unreachablePrefs(context).edit().putString(
                UNREACHABLE_KEY,
                entries.entries.joinToString("||") { "${it.key}:${it.value}" }
            ).apply()
        }

        @JvmStatic
        @Synchronized
        fun clearUnreachable(context: Context) {
            unreachablePrefs(context).edit().remove(UNREACHABLE_KEY).apply()
        }

        @JvmStatic
        private fun getUnreachableRawEntries(context: Context): Map<String, Long> {
            val now = System.currentTimeMillis()
            val raw = unreachablePrefs(context).getString(UNREACHABLE_KEY, "") ?: ""
            if (raw.isEmpty()) return emptyMap()
            val result = mutableMapOf<String, Long>()
            for (entry in raw.split("||")) {
                if (entry.isEmpty()) continue
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2) {
                    val domain = parts[0]
                    val ts = parts[1].toLongOrNull() ?: 0L
                    if (now - ts < UNREACHABLE_TTL_MS) {
                        result[domain] = ts
                    }
                }
            }
            return result
        }

        /**
         * Pending-list access. The gate (":proxy" process) writes it, the
         * in-app choice page (main process) reads/removes entries. Both sides
         * go through multi-process SharedPreferences.
         */
        @Suppress("DEPRECATION")
        private fun pendingPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PENDING_PREF, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)

        @JvmStatic
        fun getPending(context: Context): List<String> {
            val raw = pendingPrefs(context).getString(PENDING_KEY, "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split("||").filter { it.isNotEmpty() }
        }

        @JvmStatic
        @Synchronized
        fun removePending(context: Context, domain: String) {
            val list = getPending(context).toMutableSet()
            if (list.remove(domain)) {
                pendingPrefs(context).edit().putString(PENDING_KEY, list.joinToString("||")).apply()
                updatePendingNotification(context)
            }
        }

        @JvmStatic
        fun updatePendingNotification(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val list = getPending(context)
            if (list.isEmpty()) {
                nm.cancel(PENDING_NOTIFY_ID)
                return
            }
            val pi = PendingIntent.getActivity(
                context, 0,
                Intent(context, NewDomainPromptActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.domain_prompt_title))
                .setContentText(context.getString(R.string.domain_pending_count, list.size))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            try {
                nm.notify(PENDING_NOTIFY_ID, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val rulesManager: DomainRulesManager = DomainRulesManager(app)

    /** Cached auto-detection results, keyed by registrable domain. */
    private val autoCache = ConcurrentHashMap<String, AutoRoute>()

    @Volatile
    private var running = false
    private var server: ServerSocket? = null

    /** Static domain rules parsed from the built-in clash config, excluding injected ones. */
    private val builtinRules: List<BuiltinRule> by lazy { loadBuiltinRules() }

    private data class BuiltinRule(val type: String, val value: String, val policy: String)

    private class Request(val cmd: Int, val host: String, val port: Int)

    fun start(): Int {
        if (running) {
            return server?.localPort ?: 0
        }
        running = true
        val srv = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
        server = srv
        thread(name = "igniter-gate-accept", isDaemon = true) {
            while (running) {
                try {
                    val client = srv.accept()
                    thread(name = "igniter-gate-conn", isDaemon = true) { handle(client) }
                } catch (e: IOException) {
                    if (running) {
                        Log.w(TAG, "accept failed", e)
                    }
                }
            }
        }
        Log.i(TAG, "gate listening on ${srv.localPort} -> clash $clashPort")
        return srv.localPort
    }

    fun stop() {
        running = false
        try {
            server?.close()
        } catch (e: IOException) {
        }
        server = null
        autoCache.clear()
        // Held connections die with the tunnel; do not leave stale domains behind.
        pendingPrefs().edit().remove(PENDING_KEY).apply()
        getSystemService().cancel(PENDING_NOTIFY_ID)
    }

    @Suppress("DEPRECATION")
    private fun pendingPrefs(): SharedPreferences =
        app.getSharedPreferences(PENDING_PREF, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)

    private fun getSystemService(): NotificationManager = app.getSystemService(NotificationManager::class.java)

    private fun handle(client: Socket) {
        Log.d(TAG, "handle: new client ${client.remoteSocketAddress}")
        try {
            client.soTimeout = 20000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())

            val version = input.read()
            val nmethods = input.read()
            if (version != SOCKS5 || nmethods < 0) {
                Log.d(TAG, "handle: bad greeting version=$version nmethods=$nmethods, closing")
                client.close()
                return
            }
            // tun2socks only offers no-auth; accept whatever the client wants.
            repeat(nmethods) { input.read() }
            output.write(SOCKS5)
            output.write(0x00)
            output.flush()
            Log.d(TAG, "handle: greeting replied")

            val req = readRequest(input) ?: run {
                Log.d(TAG, "handle: no request, closing")
                client.close()
                return
            }
            when (req.cmd) {
                CMD_CONNECT -> handleConnect(client, input, output, req)
                CMD_UDP_ASSOCIATE -> handleUdpAssociate(client, input, output)
                else -> {
                    writeReply(output, REP_UNSUPPORTED)
                    client.close()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "connection error", e)
            try {
                client.close()
            } catch (e2: IOException) {
            }
        }
    }

    private fun readRequest(input: InputStream): Request? {
        val ver = input.read()
        if (ver != SOCKS5) return null
        val cmd = input.read()
        if (cmd < 0) return null
        input.read() // rsv
        val atyp = input.read()
        if (atyp < 0) return null
        val host = when (atyp) {
            ATYP_IPV4 -> {
                val b = ByteArray(4)
                if (!readFully(input, b)) return null
                InetAddress.getByAddress(b).hostAddress
            }
            ATYP_DOMAIN -> {
                val len = input.read()
                if (len <= 0) return null
                val b = ByteArray(len)
                if (!readFully(input, b)) return null
                String(b)
            }
            ATYP_IPV6 -> {
                val b = ByteArray(16)
                if (!readFully(input, b)) return null
                InetAddress.getByAddress(b).hostAddress
            }
            else -> return null
        }
        val hi = input.read()
        val lo = input.read()
        if (hi < 0 || lo < 0) return null
        return Request(cmd, host, (hi shl 8) or lo)
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var read = 0
        while (read < buffer.size) {
            val n = input.read(buffer, read, buffer.size - read)
            if (n < 0) return false
            read += n
        }
        return true
    }

    private fun writeReply(output: OutputStream, rep: Int, bindHost: String = "127.0.0.1", bindPort: Int = 0) {
        output.write(SOCKS5)
        output.write(rep)
        output.write(0x00)
        output.write(ATYP_IPV4)
        output.write(InetAddress.getByName(bindHost).address)
        output.write((bindPort ushr 8) and 0xFF)
        output.write(bindPort and 0xFF)
        output.flush()
    }

    private fun handleConnect(client: Socket, input: InputStream, output: OutputStream, req: Request) {
        val host = req.host.lowercase(Locale.US)
        if (!isIpLike(host)) {
            when (decide(host, req.port)) {
                AutoRoute.DIRECT -> route(client, input, output, host, req.port, POLICY_DIRECT)
                AutoRoute.PROXY, AutoRoute.DNS_POLLUTED ->
                    route(client, input, output, host, req.port, POLICY_PROXY)
                AutoRoute.UNREACHABLE -> {
                    addUnreachable(app, host)
                    Log.w(TAG, "$host unreachable both direct and proxy, refusing")
                    try {
                        writeReply(output, REP_REFUSED)
                        client.close()
                    } catch (e: IOException) {
                    }
                }
            }
            return
        }
        // Raw IP: let Clash decide (GeoIP etc.) exactly as before.
        route(client, input, output, host, req.port, POLICY_PROXY)
    }

    /**
     * Decides the routing policy for a domain by live connectivity detection.
     *  - a manual rule always wins and is honoured without testing;
     *  - known-CN (builtin DIRECT) sites go DIRECT if the direct probe passes,
     *    otherwise they fall back to the proxy;
     *  - known-foreign (curated / builtin Proxy) sites go PROXY if the proxy
     *    probe passes, otherwise they fall back to direct;
     *  - unknown domains are probed DIRECT first, then PROXY; when the direct
     *    resolution came back from a known poisoned IP the result is reported
     *    as [AutoRoute.DNS_POLLUTED] instead of plain [AutoRoute.PROXY].
     * Results are cached per registrable domain; a domain where both paths
     * fail is persisted to the unreachable list and refused.
     */
    private fun decide(host: String, port: Int): AutoRoute {
        val root = rootDomain(host)
        val manual = rulesManager.getPolicy(root) ?: rulesManager.getPolicy(host)
        if (manual != null) {
            return if (manual == POLICY_DIRECT) AutoRoute.DIRECT else AutoRoute.PROXY
        }
        autoCache[root]?.let { return it }
        if (getUnreachable(app).contains(root)) {
            return AutoRoute.UNREACHABLE
        }
        val policy = lookupPolicy(host)
        val isMajorForeign = rulesManager.lookupMajorForeignPolicy(host) != null
        val decision = when (policy) {
            POLICY_DIRECT -> {
                val ip = resolveRealIp(host)
                when {
                    ip != null && probeDirect(ip, host, port) -> AutoRoute.DIRECT
                    probeProxy(host, port) -> AutoRoute.PROXY
                    else -> AutoRoute.UNREACHABLE
                }
            }
            POLICY_PROXY -> {
                if (probeProxy(host, port)) {
                    AutoRoute.PROXY
                } else if (isMajorForeign) {
                    AutoRoute.PROXY
                } else {
                    val ip = resolveRealIp(host)
                    when {
                        ip != null && probeDirect(ip, host, port) -> AutoRoute.DIRECT
                        else -> AutoRoute.UNREACHABLE
                    }
                }
            }
            else -> {
                val ip = resolveRealIp(host)
                when {
                    ip != null && probeDirect(ip, host, port) -> AutoRoute.DIRECT
                    probeProxy(host, port) ->
                        if (ip != null && isPollutedIp(ip)) AutoRoute.DNS_POLLUTED else AutoRoute.PROXY
                    else -> AutoRoute.UNREACHABLE
                }
            }
        }
        if (decision != AutoRoute.UNREACHABLE) {
            if (autoCache.size > 4096) {
                autoCache.clear()
            }
            autoCache[root] = decision
        }
        Log.i(TAG, "auto-decision $host -> $decision")
        return decision
    }

    /** TCP connect + data-response probe against the resolved real IP. */
    private fun probeDirect(ip: InetAddress, host: String, port: Int): Boolean {
        var socket: Socket? = null
        var ssl: SSLSocket? = null
        return try {
            socket = Socket()
            socket!!.connect(InetSocketAddress(ip, port), PROBE_CONNECT_TIMEOUT_MS)
            if (port == 443) {
                ssl = SSLContext.getDefault().socketFactory
                    .createSocket(socket!!, host, port, true) as SSLSocket
                ssl.soTimeout = PROBE_TLS_TIMEOUT_MS
                ssl.startHandshake()
                true
            } else {
                socket!!.soTimeout = PROBE_READ_TIMEOUT_MS
                socket!!.getOutputStream().write(
                    "GET / HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n\r\n"
                        .toByteArray(Charsets.US_ASCII))
                socket!!.getOutputStream().flush()
                socket!!.getInputStream().read() >= 0
            }
        } catch (e: Exception) {
            false
        } finally {
            runCatching { ssl?.close() }
            runCatching { socket?.close() }
        }
    }

    /** CONNECT through Clash (which dials trojan -> server) and check the reply. */
    /**
     * TCP connect + data-response probe through Clash. Clash answers the
     * CONNECT with 0x00 before dialing, so success is only trusted once the
     * tunnel actually delivers data (TLS handshake for 443, HTTP body byte
     * otherwise); a read timeout is treated as reachable-but-slow because a
     * failed dial is closed by Clash immediately (EOF, never a timeout).
     */
    private fun probeProxy(host: String, port: Int): Boolean {
        var socket: Socket? = null
        var ssl: SSLSocket? = null
        return try {
            socket = Socket()
            socket!!.connect(InetSocketAddress("127.0.0.1", clashPort), PROBE_CONNECT_TIMEOUT_MS)
            socket!!.soTimeout = PROBE_READ_TIMEOUT_MS
            socks5Connect(socket!!, host, port)
            if (port == 443) {
                ssl = SSLContext.getDefault().socketFactory
                    .createSocket(socket!!, host, port, true) as SSLSocket
                ssl.soTimeout = PROBE_TLS_TIMEOUT_MS
                ssl.startHandshake()
                true
            } else {
                socket!!.getOutputStream().write(
                    "GET / HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n\r\n"
                        .toByteArray(Charsets.US_ASCII))
                socket!!.getOutputStream().flush()
                socket!!.getInputStream().read() >= 0
            }
        } catch (e: SocketTimeoutException) {
            true
        } catch (e: Exception) {
            false
        } finally {
            runCatching { ssl?.close() }
            runCatching { socket?.close() }
        }
    }

    private fun isPollutedIp(ip: InetAddress): Boolean {
        return ip.hostAddress in KNOWN_POLLUTED_IPS
    }

    /**
     * Opens the upstream (direct socket or Clash), performs the proxy/direct
     * handshake and relays bytes both ways until either side closes.
     */
    private fun route(client: Socket, input: InputStream, output: OutputStream, host: String, port: Int, policy: String) {
        val upstream = try {
            val s = if (POLICY_DIRECT == policy) Socket() else Socket("127.0.0.1", clashPort)
            s.soTimeout = 15000
            s
        } catch (e: IOException) {
            Log.w(TAG, "upstream connect failed", e)
            null
        }
        if (upstream == null) {
            try {
                writeReply(output, REP_REFUSED)
                client.close()
            } catch (e: IOException) {
            }
            return
        }
        try {
            if (POLICY_DIRECT == policy) {
                // The fake-ip system DNS must not be used: a real A/AAAA lookup
                // gives a real IP that this (VPN-owner) process can dial straight
                // out, keeping the connection truly direct.
                val ip = resolveRealIp(host)
                if (ip == null) {
                    Log.w(TAG, "no real address for direct target $host")
                    writeReply(output, REP_REFUSED)
                    client.close()
                    return
                }
                upstream.connect(InetSocketAddress(ip, port), 15000)
            } else {
                if (!socks5Connect(upstream, host, port)) {
                    writeReply(output, REP_REFUSED)
                    client.close()
                    return
                }
            }
            writeReply(output, REP_SUCCEEDED)
            val upInput = BufferedInputStream(upstream.getInputStream())
            val upOutput = BufferedOutputStream(upstream.getOutputStream())
            val t1 = thread(name = "igniter-gate-relay", isDaemon = true) { pump(input, upOutput) }
            val t2 = thread(name = "igniter-gate-relay", isDaemon = true) { pump(upInput, output) }
            t1.join()
            t2.join()
        } catch (e: Exception) {
            Log.w(TAG, "route failed", e)
            try {
                writeReply(output, REP_REFUSED)
            } catch (e2: IOException) {
            }
        } finally {
            try {
                upstream.close()
            } catch (e: IOException) {
            }
            try {
                client.close()
            } catch (e: IOException) {
            }
        }
    }

    private fun socks5Connect(socket: Socket, host: String, port: Int): Boolean {
        val out = BufferedOutputStream(socket.getOutputStream())
        val input = BufferedInputStream(socket.getInputStream())
        val hostBytes: ByteArray
        val atyp: Int
        if (isIpLike(host)) {
            // Literal IP: parsed locally, no DNS lookup involved.
            val addr = InetAddress.getByName(host)
            hostBytes = addr.address
            atyp = if (hostBytes.size == 4) ATYP_IPV4 else ATYP_IPV6
        } else {
            // Domain: send as-is. The fake-ip system DNS would block on
            // resolution here, so Clash must do the DNS itself.
            hostBytes = host.toByteArray(Charsets.US_ASCII)
            atyp = ATYP_DOMAIN
        }
        val request = ByteArrayOutputStream()
        request.write(SOCKS5)
        request.write(CMD_CONNECT)
        request.write(0x00)
        request.write(atyp)
        if (atyp == ATYP_DOMAIN) {
            request.write(hostBytes.size)
        }
        request.write(hostBytes)
        request.write((port ushr 8) and 0xFF)
        request.write(port and 0xFF)
        // Greeting and request in a single write. The domain CONNECT must
        // carry the one-byte length prefix (written above), otherwise Clash
        // reads the first domain byte as the length and waits forever.
        out.write(SOCKS5)
        out.write(0x01)
        out.write(0x00)
        out.write(request.toByteArray())
        out.flush()
        if (input.read() != SOCKS5 || input.read() != 0x00) {
            Log.w(TAG, "socks5Connect: bad greeting reply from clash for $host")
            return false
        }

        if (input.read() != SOCKS5) {
            Log.w(TAG, "socks5Connect: no SOCKS version from clash for $host")
            return false
        }
        val rep = input.read()
        input.read() // rsv
        val rAtyp = input.read()
        if (rAtyp == ATYP_IPV4) {
            input.skip(4)
        } else if (rAtyp == ATYP_DOMAIN) {
            val len = input.read()
            if (len < 0) return false
            input.skip(len.toLong())
        } else if (rAtyp == ATYP_IPV6) {
            input.skip(16)
        } else {
            return false
        }
        input.skip(2)
        return rep == 0x00
    }

    /**
     * Relays a SOCKS5 UDP ASSOCIATE to Clash transparently: the client's TCP
     * control channel and UDP datagrams are piped through untouched, so Clash
     * keeps doing all the UDP routing. UDP domains are not prompted.
     */
    private fun handleUdpAssociate(client: Socket, input: InputStream, output: OutputStream) {
        val clientUdp = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        var up: Socket? = null
        try {
            up = Socket("127.0.0.1", clashPort)
            val upInput = BufferedInputStream(up.getInputStream())
            val upOutput = BufferedOutputStream(up.getOutputStream())
            upOutput.write(SOCKS5)
            upOutput.write(0x01)
            upOutput.write(0x00)
            upOutput.flush()
            if (upInput.read() != SOCKS5 || upInput.read() != 0x00) {
                writeReply(output, REP_REFUSED)
                client.close()
                return
            }
            upOutput.write(SOCKS5)
            upOutput.write(CMD_UDP_ASSOCIATE)
            upOutput.write(0x00)
            upOutput.write(ATYP_IPV4)
            upOutput.write(clientUdp.localAddress.address)
            upOutput.write((clientUdp.localPort ushr 8) and 0xFF)
            upOutput.write(clientUdp.localPort and 0xFF)
            upOutput.flush()

            if (upInput.read() != SOCKS5) {
                writeReply(output, REP_REFUSED)
                client.close()
                return
            }
            val rep = upInput.read()
            upInput.read() // rsv
            val atyp = upInput.read()
            val relayIp: ByteArray = when (atyp) {
                ATYP_IPV4 -> {
                    val b = ByteArray(4)
                    if (!readFully(upInput, b)) return
                    b
                }
                ATYP_IPV6 -> {
                    val b = ByteArray(16)
                    if (!readFully(upInput, b)) return
                    b
                }
                else -> {
                    writeReply(output, REP_REFUSED)
                    client.close()
                    return
                }
            }
            val hi = upInput.read()
            val lo = upInput.read()
            if (hi < 0 || lo < 0) {
                writeReply(output, REP_REFUSED)
                client.close()
                return
            }
            if (rep != 0x00) {
                writeReply(output, REP_REFUSED)
                client.close()
                return
            }
            val relayAddr = InetSocketAddress(InetAddress.getByAddress(relayIp), (hi shl 8) or lo)

            writeReply(output, REP_SUCCEEDED, "127.0.0.1", clientUdp.localPort)

            val lastClientAddr = AtomicReference<SocketAddress>(null)
            thread(name = "igniter-gate-udp-c2s", isDaemon = true) {
                try {
                    val buf = ByteArray(65536)
                    while (running) {
                        val p = DatagramPacket(buf, buf.size)
                        clientUdp.receive(p)
                        lastClientAddr.set(p.socketAddress)
                        clientUdp.send(DatagramPacket(p.data, p.length, relayAddr))
                    }
                } catch (e: IOException) {
                }
            }
            thread(name = "igniter-gate-udp-s2c", isDaemon = true) {
                try {
                    val buf = ByteArray(65536)
                    while (running) {
                        val p = DatagramPacket(buf, buf.size)
                        clientUdp.receive(p)
                        val dest = lastClientAddr.get() ?: continue
                        clientUdp.send(DatagramPacket(p.data, p.length, dest))
                    }
                } catch (e: IOException) {
                }
            }
            val t1 = thread(name = "igniter-gate-udp-tcp1", isDaemon = true) { pump(input, upOutput) }
            val t2 = thread(name = "igniter-gate-udp-tcp2", isDaemon = true) { pump(upInput, output) }
            t1.join()
            t2.join()
        } catch (e: Exception) {
            Log.w(TAG, "udp associate failed", e)
        } finally {
            clientUdp.close()
            try {
                up?.close()
            } catch (e: IOException) {
            }
            try {
                client.close()
            } catch (e: IOException) {
            }
        }
    }

    private fun pump(from: InputStream, to: OutputStream) {
        try {
            val buf = ByteArray(16384)
            while (true) {
                val n = from.read(buf)
                if (n < 0) break
                to.write(buf, 0, n)
                to.flush()
            }
        } catch (e: IOException) {
        } finally {
            try {
                to.close()
            } catch (e: IOException) {
            }
        }
    }

    /**
     * First matching rule wins. Manual rules have the highest priority, then the
     * curated foreign-website list, then the built-in clash config rules.
     */
    private fun lookupPolicy(host: String): String? {
        for ((rule, policy) in rulesManager.getRules()) {
            if (suffixMatch(rule, host)) return policy
        }
        rulesManager.lookupMajorForeignPolicy(host)?.let { return it }
        for (r in builtinRules) {
            when (r.type) {
                "DOMAIN" -> if (host == r.value) return r.policy
                "DOMAIN-SUFFIX" -> if (suffixMatch(r.value, host)) return r.policy
                "DOMAIN-KEYWORD" -> if (host.contains(r.value)) return r.policy
            }
        }
        return null
    }

    private fun suffixMatch(rule: String, host: String): Boolean {
        return host == rule || host.endsWith(".$rule")
    }

    private fun isIpLike(host: String): Boolean {
        return host.matches(Regex("[0-9a-fA-F:.]+"))
    }

    /**
     * Resolves a domain to a real address, bypassing the fake-ip system DNS.
     * The VPN-owner process's own traffic bypasses the tunnel, so plain UDP
     * queries to public nameservers are answered directly. The physical
     * network's DNS servers are tried as a fallback for networks where the
     * public list is unreachable. IPv4 is preferred; IPv6 is only attempted
     * when enabled and the A lookup came up empty.
     */
    private fun resolveRealIp(host: String): InetAddress? {
        val servers = (Net.DNS_SERVERS + physicalDnsServers()).distinct()
        for (server in servers) {
            val a = dnsQuery(server, host, DNS_TYPE_A)?.let { runCatching { InetAddress.getByAddress(it) }.getOrNull() }
            if (a != null) {
                return a
            }
        }
        if (enableIpv6) {
            for (server in servers) {
                val aaaa = dnsQuery(server, host, DNS_TYPE_AAAA)?.let { runCatching { InetAddress.getByAddress(it) }.getOrNull() }
                if (aaaa != null) {
                    return aaaa
                }
            }
        }
        return null
    }

    /** DNS servers of the physical network, in query order. The active
     *  network is the VPN's own tun0 (fake-ip DNS), so scan all networks and
     *  skip VPN ones to reach the real upstream resolver. */
    private fun physicalDnsServers(): Array<String> {
        return try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val servers = mutableListOf<String>()
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                val lp: LinkProperties = cm.getLinkProperties(network) ?: continue
                for (dns in lp.dnsServers) {
                    servers.add(dns.hostAddress)
                }
            }
            servers.toTypedArray()
        } catch (e: Exception) {
            emptyArray()
        }
    }

    private fun dnsQuery(server: String, host: String, type: Int): ByteArray? {
        val socket = DatagramSocket()
        return try {
            socket.soTimeout = DNS_QUERY_TIMEOUT_MS
            socket.connect(InetSocketAddress(server, 53))
            val query = buildDnsQuery(host, type)
            socket.send(DatagramPacket(query, query.size))
            val buf = ByteArray(512)
            val response = DatagramPacket(buf, buf.size)
            socket.receive(response)
            parseDnsAnswer(buf, response.length, type)
        } catch (e: Exception) {
            null
        } finally {
            socket.close()
        }
    }

    private fun buildDnsQuery(host: String, type: Int): ByteArray {
        val qname = ByteArrayOutputStream()
        for (label in host.split('.')) {
            qname.write(label.length)
            qname.write(label.toByteArray(Charsets.US_ASCII))
        }
        qname.write(0)
        val out = ByteArrayOutputStream()
        out.write(0xAB) // transaction id
        out.write(0xCD)
        out.write(0x01) // flags: recursion desired
        out.write(0x00)
        out.write(0x00) // qdcount
        out.write(0x01)
        out.write(0x00) // ancount
        out.write(0x00)
        out.write(0x00) // nscount
        out.write(0x00)
        out.write(0x00) // arcount
        out.write(0x00)
        out.write(qname.toByteArray())
        out.write((type ushr 8) and 0xFF)
        out.write(type and 0xFF)
        out.write(0x00) // qclass
        out.write(0x01)
        return out.toByteArray()
    }

    private fun parseDnsAnswer(buf: ByteArray, len: Int, wanted: Int): ByteArray? {
        if (len < 12) return null
        val qdcount = ((buf[4].toInt() and 0xFF) shl 8) or (buf[5].toInt() and 0xFF)
        val ancount = ((buf[6].toInt() and 0xFF) shl 8) or (buf[7].toInt() and 0xFF)
        if (ancount == 0) return null
        var pos = 12
        for (i in 0 until qdcount) {
            pos = skipDnsName(buf, pos, len) ?: return null
            pos += 4
        }
        for (i in 0 until ancount) {
            pos = skipDnsName(buf, pos, len) ?: return null
            if (pos + 10 > len) return null
            val type = ((buf[pos].toInt() and 0xFF) shl 8) or (buf[pos + 1].toInt() and 0xFF)
            val rdlength = ((buf[pos + 8].toInt() and 0xFF) shl 8) or (buf[pos + 9].toInt() and 0xFF)
            val rdata = pos + 10
            if (rdata + rdlength > len) return null
            if (type == wanted) {
                return buf.copyOfRange(rdata, rdata + rdlength)
            }
            pos = rdata + rdlength
        }
        return null
    }

    private fun skipDnsName(buf: ByteArray, start: Int, len: Int): Int? {
        var pos = start
        while (true) {
            if (pos >= len) return null
            val c = buf[pos].toInt() and 0xFF
            if (c == 0) return pos + 1
            if ((c and 0xC0) == 0xC0) return pos + 2
            pos += c + 1
            if (pos > len) return null
        }
    }

    private fun loadBuiltinRules(): List<BuiltinRule> {
        val path = app.storage.path.clashConfig ?: return emptyList()
        val content = Storage.read(path) ?: return emptyList()
        val out = mutableListOf<BuiltinRule>()
        for (line in String(content).split('\n')) {
            val trimmed = line.trim()
            if (trimmed.endsWith(ClashRouteBuilder.RULE_MARKER)) continue
            if (!trimmed.startsWith("- ")) continue
            val parts = trimmed.removePrefix("- ").split(',')
            if (parts.size < 3) continue
            val type = parts[0].trim()
            val value = parts[1].trim()
            val policy = parts[2].trim()
            if (type == "DOMAIN" || type == "DOMAIN-SUFFIX" || type == "DOMAIN-KEYWORD") {
                out.add(BuiltinRule(type, value.lowercase(Locale.US), policy))
            }
        }
        Log.i(TAG, "loaded ${out.size} built-in domain rules")
        return out
    }
}

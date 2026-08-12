package io.github.freewebmovement.igniter.persistence

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import clash.Clash
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.JNIHelper
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.constants.Net
import io.github.freewebmovement.igniter.constants.Trojan
import tun2socks.Tun2socks
import tun2socks.Tun2socksStartOptions
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

object NetWorkConfig {

    private var tun2socksStarted = false
    private var clashStarted = false

    @JvmStatic
    fun isPortTaken(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            true
        } catch (ce: IOException) {
            false
        }
    }

    /**
     * Picks a random free TCP port, preferring the given port when it is free.
     */
    @JvmStatic
    fun findFreePort(preferred: Int): Int {
        if (preferred in 1..65535 && !isPortTaken("127.0.0.1", preferred, 100)) {
            return preferred
        }
        return findFreePort()
    }

    /**
     * Picks a random free TCP port by binding an ephemeral socket.
     */
    @JvmStatic
    fun findFreePort(): Int {
        for (i in 0 until 20) {
            var port = -1
            try {
                ServerSocket(0).use { socket ->
                    port = socket.localPort
                }
            } catch (e: IOException) {
                e.printStackTrace()
                continue
            }
            if (port > 0 && !isPortTaken("127.0.0.1", port, 100)) {
                return port
            }
        }
        return 0
    }

    /**
     * Polls until a TCP listener is reachable at the given address, or the timeout elapses.
     */
    @JvmStatic
    fun waitForPort(ip: String, port: Int, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isPortTaken(ip, port, 200)) {
                return true
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    @JvmStatic
    fun tunnelProxy(fd: Int, port: Int, enableIPV6: Boolean, enableClash: Boolean) {
        val tun2socksStartOptions = Tun2socksStartOptions()
        tun2socksStartOptions.tunFd = fd.toLong()
        tun2socksStartOptions.socks5Server = Net.TUN2SOCKS5_SERVER_HOST + ":" + port
        tun2socksStartOptions.enableIPv6 = enableIPV6
        tun2socksStartOptions.mtu = Net.VPN_MTU.toLong()

        Tun2socks.setLoglevel(Net.TUNNEL_TO_SOCKS_LOG_LEVEL)
        if (enableClash) {
            tun2socksStartOptions.fakeIPRange = Net.FAKE_IP_RANGE
        } else {
            // Disable go-tun2socks fake ip
            tun2socksStartOptions.fakeIPRange = ""
        }
        Tun2socks.start(tun2socksStartOptions)
    }

    @JvmStatic
    fun startService(app: IgniterApplication, fd: Int): String {
        val enableClash = app.trojanPreferences.getEnableClash()
        val enableIPV6 = app.trojanPreferences.getEnableIPV6()
        val enableLan = app.trojanPreferences.isEnableLan()

        var clashSocksPort = 0
        var trojanPort: Int = 0
        if (enableClash) {
            clashSocksPort = app.clashConfig.getPort()
            val configError = app.clashConfig.validateConfig()
            if (configError != null) {
                throw IllegalStateException("invalid clash config: $configError")
            }
            // The embedded Clash library kills the whole process on startup errors
            // (log.Fatalf -> os.Exit). Avoid the most common one: a busy SOCKS port.
            clashSocksPort = findFreePort(clashSocksPort)
            var trojanCandidate = app.trojanConfig.getLocalPort()
            if (trojanCandidate == clashSocksPort) {
                trojanCandidate = findFreePort(clashSocksPort)
            }
            trojanPort = applyTrojanPort(app, findFreePort(trojanCandidate))
            if (trojanPort == clashSocksPort) {
                trojanPort = applyTrojanPort(app, findFreePort())
            }
        } else {
            trojanPort = applyTrojanPort(app, findFreePort(app.trojanConfig.getLocalPort()))
        }

        JNIHelper.start(app.storage.path.trojanConfig!!)
        if (!waitForPort("127.0.0.1", trojanPort, 3000)) {
            throw IllegalStateException("trojan failed to listen on $trojanPort")
        }

        val tun2socksPort: Int
        if (enableClash) {
            injectUserDomainRules(app)
            if (!ClashConfig.startClash(
                    app.filesDir.toString(),
                    clashSocksPort, trojanPort,
                    enableLan
                )
            ) {
                throw IllegalStateException("clash failed to start on $clashSocksPort")
            }
            clashStarted = true
            tun2socksPort = clashSocksPort
        } else {
            tun2socksPort = trojanPort
        }

        tun2socksStarted = true
        tunnelProxy(fd, tun2socksPort, enableIPV6, enableClash)
        var str = String.format(app.getString(R.string.network_ports), trojanPort, tun2socksPort)
        if (enableClash) {
            str += String.format(app.getString(R.string.clash_port), clashSocksPort)
        }
        return str
    }

    /**
     * Updates the trojan local port both in memory and in config.json so the
     * native trojan client (which reads the file at startup) binds the new port.
     */
    private fun applyTrojanPort(app: IgniterApplication, port: Int): Int {
        if (app.trojanConfig.getLocalPort() != port) {
            app.trojanConfig.setLocalPort(port)
            TrojanConfig.update(app.storage.path.trojanConfig!!, Trojan.KEY_LOCAL_PORT, port)
        }
        return port
    }

    /**
     * Prepends the effective domain rules (the curated major-foreign-website
     * list defaulting to Proxy, overridden by the user's manual rules) to the
     * `rules:` section of the Clash config before Clash starts. Stale
     * injected lines from previous runs are removed first, so no duplicates
     * accumulate.
     */
    @JvmStatic
    fun injectUserDomainRules(app: IgniterApplication) {
        try {
            val manager = DomainRulesManager(app)
            val rules = manager.getEffectiveRules()
            val path = app.storage.path.clashConfig!!
            val content = String(Storage.read(path)!!)
            if (content == null) {
                return
            }
            val keep = ArrayList<String>()
            for (line in content.split("\n", limit = -1)) {
                if (!line.trim().endsWith("# user rule")) {
                    keep.add(line)
                }
            }
            val sb = StringBuilder(content.length + rules.size * 48)
            var inserted = false
            for (line in keep) {
                if (!inserted && line.trim().startsWith("rules:")) {
                    sb.append(line).append('\n')
                    for ((key, value) in rules) {
                        sb.append("  - DOMAIN-SUFFIX,")
                            .append(key)
                            .append(',')
                            .append(value)
                            .append("  # user rule\n")
                    }
                    inserted = true
                } else {
                    sb.append(line).append('\n')
                }
            }
            Storage.write(path, sb.toString().toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun establish(
        app: IgniterApplication,
        b: VpnService.Builder,
        sessionName: String,
        packages: Set<String>
    ): ParcelFileDescriptor {
        val enableClash = app.trojanPreferences.getEnableClash()
        val enableIPV6 = app.trojanPreferences.getEnableIPV6()
        for (packageName in packages) {
            try {
                b.addDisallowedApplication(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
        b.setSession(sessionName)
        b.setMtu(Net.VPN_MTU)
        b.addAddress(Net.PRIVATE_VLAN4_CLIENT, 30)
        if (enableClash) {
            for (route in app.resources.getStringArray(R.array.bypass_private_route)) {
                val parts = route.split("/", limit = 2)
                b.addRoute(parts[0], Integer.parseInt(parts[1]))
            }
            // fake ip range for go-tun2socks
            // should match clash configuration
            b.addRoute("198.18.0.0", 16)
        } else {
            b.addRoute("0.0.0.0", 0)
        }
        if (enableClash) {
            // In clash mode the device must never be handed a real DNS address.
            // A public address lets Android's Private DNS (DoT, TCP/853) bypass
            // tun2socks' fake-ip DNS interception (UDP/53 only), so apps would
            // receive real and possibly polluted addresses and domain-based
            // routing would never match. Advertising a fake-ip address makes
            // DoT fail fast and fall back to plain UDP/53, which tun2socks
            // answers locally with fake ips - no system DNS is ever used.
            b.addDnsServer(Net.DNS_SERVER_FAKE_IP)
        } else {
            for (server in Net.DNS_SERVERS) {
                b.addDnsServer(server)
            }
        }
        // Route the physical network's DNS servers through the tunnel too.
        // Plain-DNS queries to those servers would otherwise use the LAN
        // bypass route (direct to the router) and be answered by a polluted
        // upstream DNS. Once routed through tun0 they are intercepted by
        // tun2socks' fake-ip DNS, so domain routing stays intact.
        addPhysicalDnsRoutes(app, b)
        if (enableIPV6) {
            b.addAddress(Net.PRIVATE_VLAN6_CLIENT, 126)
            b.addRoute("::", 0)

            for (server in Net.IPV6_DNS_SERVERS) {
                b.addDnsServer(server)
            }
        }
        return b.establish()!!
    }

    private fun addPhysicalDnsRoutes(app: IgniterApplication, b: VpnService.Builder) {
        try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network: Network = cm.activeNetwork ?: return
            val lp: LinkProperties = cm.getLinkProperties(network) ?: return
            for (dns in lp.dnsServers) {
                if (dns is Inet4Address) {
                    b.addRoute(dns.hostAddress, 32)
                } else if (dns is Inet6Address && app.trojanPreferences.getEnableIPV6()) {
                    b.addRoute(dns.hostAddress, 128)
                }
            }
        } catch (e: Exception) {
            Log.w("NetWorkConfig", "addPhysicalDnsRoutes failed", e)
        }
    }

    @JvmStatic
    fun stop(app: IgniterApplication) {
        JNIHelper.terminate()
        if (clashStarted) {
            Clash.stop()
            clashStarted = false
        }
        if (tun2socksStarted) {
            Tun2socks.stop()
            tun2socksStarted = false
        }
    }

    @JvmStatic
    fun setPort(app: IgniterApplication, port: Int) {
        if (app.trojanPreferences.getEnableClash()) {
            app.clashConfig.setPort(port)
            app.clashConfig.setTrojanPort(port + 1)
            app.trojanConfig.setLocalPort(port + 1)
        } else {
            app.trojanConfig.setLocalPort(port)
        }
    }
}

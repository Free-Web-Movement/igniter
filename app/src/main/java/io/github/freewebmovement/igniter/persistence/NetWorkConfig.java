package io.github.freewebmovement.igniter.persistence;

import static io.github.freewebmovement.igniter.constants.Net.DNS_SERVER_FAKE_IP;
import static io.github.freewebmovement.igniter.constants.Net.DNS_SERVERS;
import static io.github.freewebmovement.igniter.constants.Net.FAKE_IP_RANGE;
import static io.github.freewebmovement.igniter.constants.Net.IPV6_DNS_SERVERS;
import static io.github.freewebmovement.igniter.constants.Net.PRIVATE_VLAN4_CLIENT;
import static io.github.freewebmovement.igniter.constants.Net.PRIVATE_VLAN6_CLIENT;
import static io.github.freewebmovement.igniter.constants.Net.TUN2SOCKS5_SERVER_HOST;
import static io.github.freewebmovement.igniter.constants.Net.TUNNEL_TO_SOCKS_LOG_LEVEL;
import static io.github.freewebmovement.igniter.constants.Net.VPN_MTU;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

import clash.Clash;
import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.JNIHelper;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.constants.Trojan;
import tun2socks.Tun2socks;
import tun2socks.Tun2socksStartOptions;

public class NetWorkConfig {

    private static volatile boolean tun2socksStarted = false;
    private static volatile boolean clashStarted = false;

    public static boolean isPortTaken(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch (IOException ce) {
            return false;
        }
    }

    /**
     * Picks a random free TCP port, preferring the given port when it is free.
     */
    public static int findFreePort(int preferred) {
        if (preferred >= 1 && preferred <= 65535 && !isPortTaken("127.0.0.1", preferred, 100)) {
            return preferred;
        }
        return findFreePort();
    }

    /**
     * Picks a random free TCP port by binding an ephemeral socket.
     */
    public static int findFreePort() {
        for (int i = 0; i < 20; i++) {
            int port = -1;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (port > 0 && !isPortTaken("127.0.0.1", port, 100)) {
                return port;
            }
        }
        return 0;
    }

    /**
     * Polls until a TCP listener is reachable at the given address, or the timeout elapses.
     */
    public static boolean waitForPort(final String ip, final int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isPortTaken(ip, port, 200)) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static void tunnelProxy(int fd, int port, boolean enableIPV6, boolean enableClash) {
        Tun2socksStartOptions tun2socksStartOptions = new Tun2socksStartOptions();
        tun2socksStartOptions.setTunFd(fd);
        tun2socksStartOptions.setSocks5Server(TUN2SOCKS5_SERVER_HOST + ":" + port);
        tun2socksStartOptions.setEnableIPv6(enableIPV6);
        tun2socksStartOptions.setMTU(VPN_MTU);

        Tun2socks.setLoglevel(TUNNEL_TO_SOCKS_LOG_LEVEL);
        if (enableClash) {
            tun2socksStartOptions.setFakeIPRange(FAKE_IP_RANGE);
        } else {
            // Disable go-tun2socks fake ip
            tun2socksStartOptions.setFakeIPRange("");
        }
        Tun2socks.start(tun2socksStartOptions);
    }

    public static String startService(IgniterApplication app, int fd) {
        boolean enableClash = app.trojanPreferences.enableClash;
        boolean enableIPV6 = app.trojanPreferences.enableIPV6;
        boolean enableLan = app.trojanPreferences.enableLan;

        int clashSocksPort = 0;
        int trojanPort;
        if (enableClash) {
            clashSocksPort = app.clashConfig.getPort();
            String configError = app.clashConfig.validateConfig();
            if (configError != null) {
                throw new IllegalStateException("invalid clash config: " + configError);
            }
            // The embedded Clash library kills the whole process on startup errors
            // (log.Fatalf -> os.Exit). Avoid the most common one: a busy SOCKS port.
            clashSocksPort = findFreePort(clashSocksPort);
            int trojanCandidate = app.trojanConfig.getLocalPort();
            if (trojanCandidate == clashSocksPort) {
                trojanCandidate = findFreePort(clashSocksPort);
            }
            trojanPort = applyTrojanPort(app, findFreePort(trojanCandidate));
            if (trojanPort == clashSocksPort) {
                trojanPort = applyTrojanPort(app, findFreePort());
            }
        } else {
            trojanPort = applyTrojanPort(app, findFreePort(app.trojanConfig.getLocalPort()));
        }

        JNIHelper.start(app.storage.path.trojanConfig);
        if (!waitForPort("127.0.0.1", trojanPort, 3000)) {
            throw new IllegalStateException("trojan failed to listen on " + trojanPort);
        }

        int tun2socksPort;
        if (enableClash) {
            injectUserDomainRules(app);
            if (!ClashConfig.startClash(app.getFilesDir().toString(),
                    clashSocksPort, trojanPort,
                    enableLan)) {
                throw new IllegalStateException("clash failed to start on " + clashSocksPort);
            }
            clashStarted = true;
            tun2socksPort = clashSocksPort;
        } else {
            tun2socksPort = trojanPort;
        }

        tun2socksStarted = true;
        tunnelProxy(fd, tun2socksPort, enableIPV6, enableClash);
        String str = String.format(app.getString(R.string.network_ports), trojanPort, tun2socksPort);
        if (enableClash) {
            str += String.format(app.getString(R.string.clash_port), clashSocksPort);
        }
        return str;
    }

    /**
     * Updates the trojan local port both in memory and in config.json so the
     * native trojan client (which reads the file at startup) binds the new port.
     */
    private static int applyTrojanPort(IgniterApplication app, int port) {
        if (app.trojanConfig.getLocalPort() != port) {
            app.trojanConfig.setLocalPort(port);
            TrojanConfig.update(app.storage.path.trojanConfig, Trojan.KEY_LOCAL_PORT, port);
        }
        return port;
    }

    /**
     * Prepends the effective domain rules (the curated major-foreign-website
     * list defaulting to Proxy, overridden by the user's manual rules) to the
     * {@code rules:} section of the Clash config before Clash starts. Stale
     * injected lines from previous runs are removed first, so no duplicates
     * accumulate.
     */
    public static void injectUserDomainRules(IgniterApplication app) {
        try {
            DomainRulesManager manager = new DomainRulesManager(app);
            java.util.Map<String, String> rules = manager.getEffectiveRules();
            String path = app.storage.path.clashConfig;
            String content = new String(Storage.read(path));
            if (content == null) {
                return;
            }
            java.util.List<String> keep = new java.util.ArrayList<>();
            for (String line : content.split("\n", -1)) {
                if (!line.trim().endsWith("# user rule")) {
                    keep.add(line);
                }
            }
            StringBuilder sb = new StringBuilder(content.length() + rules.size() * 48);
            boolean inserted = false;
            for (String line : keep) {
                if (!inserted && line.trim().startsWith("rules:")) {
                    sb.append(line).append('\n');
                    for (java.util.Map.Entry<String, String> e : rules.entrySet()) {
                        sb.append("  - DOMAIN-SUFFIX,")
                                .append(e.getKey())
                                .append(',')
                                .append(e.getValue())
                                .append("  # user rule\n");
                    }
                    inserted = true;
                } else {
                    sb.append(line).append('\n');
                }
            }
            Storage.write(path, sb.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ParcelFileDescriptor establish(IgniterApplication app, VpnService.Builder b, String sessionName, Set<String> packages) {
        boolean enableClash = app.trojanPreferences.enableClash;
        boolean enableIPV6 = app.trojanPreferences.enableIPV6;
        for (String packageName : packages) {
            try {
                b.addDisallowedApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        b.setSession(sessionName);
        b.setMtu(VPN_MTU);
        b.addAddress(PRIVATE_VLAN4_CLIENT, 30);
        if (enableClash) {
            for (String route : app.getResources().getStringArray(R.array.bypass_private_route)) {
                String[] parts = route.split("/", 2);
                b.addRoute(parts[0], Integer.parseInt(parts[1]));
            }
            // fake ip range for go-tun2socks
            // should match clash configuration
            b.addRoute("198.18.0.0", 16);
        } else {
            b.addRoute("0.0.0.0", 0);
        }
        if (enableClash) {
            // In clash mode the device must never be handed a real DNS address.
            // A public address lets Android's Private DNS (DoT, TCP/853) bypass
            // tun2socks' fake-ip DNS interception (UDP/53 only), so apps would
            // receive real and possibly polluted addresses and domain-based
            // routing would never match. Advertising a fake-ip address makes
            // DoT fail fast and fall back to plain UDP/53, which tun2socks
            // answers locally with fake ips - no system DNS is ever used.
            b.addDnsServer(DNS_SERVER_FAKE_IP);
        } else {
            for (String server : DNS_SERVERS) {
                b.addDnsServer(server);
            }
        }
        // Route the physical network's DNS servers through the tunnel too.
        // Plain-DNS queries to those servers would otherwise use the LAN
        // bypass route (direct to the router) and be answered by a polluted
        // upstream DNS. Once routed through tun0 they are intercepted by
        // tun2socks' fake-ip DNS, so domain routing stays intact.
        addPhysicalDnsRoutes(app, b);
        if (enableIPV6) {
            b.addAddress(PRIVATE_VLAN6_CLIENT, 126);
            b.addRoute("::", 0);

            for (String server : IPV6_DNS_SERVERS) {
                b.addDnsServer(server);
            }
        }
        return b.establish();
    }

    private static void addPhysicalDnsRoutes(IgniterApplication app, VpnService.Builder b) {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return;
            }
            LinkProperties lp = cm.getLinkProperties(network);
            if (lp == null) {
                return;
            }
            for (InetAddress dns : lp.getDnsServers()) {
                if (dns instanceof Inet4Address) {
                    b.addRoute(dns.getHostAddress(), 32);
                } else if (dns instanceof Inet6Address && app.trojanPreferences.enableIPV6) {
                    b.addRoute(dns.getHostAddress(), 128);
                }
            }
        } catch (Exception e) {
            Log.w("NetWorkConfig", "addPhysicalDnsRoutes failed", e);
        }
    }

    public static void stop(IgniterApplication app) {
        JNIHelper.terminate();
        if (clashStarted) {
            Clash.stop();
            clashStarted = false;
        }
        if (tun2socksStarted) {
            Tun2socks.stop();
            tun2socksStarted = false;
        }
    }

    public static void setPort(IgniterApplication app, int port) {
        if (app.trojanPreferences.getEnableClash()) {
            app.clashConfig.setPort(port);
            app.clashConfig.setTrojanPort(port + 1);
            app.trojanConfig.setLocalPort(port + 1);
        } else {
            app.trojanConfig.setLocalPort(port);
        }
    }
}

package io.github.freewebmovement.igniter.persistence;

import static io.github.freewebmovement.igniter.constants.Net.DNS_SERVERS;
import static io.github.freewebmovement.igniter.constants.Net.FAKE_IP_RANGE;
import static io.github.freewebmovement.igniter.constants.Net.IPV6_DNS_SERVERS;
import static io.github.freewebmovement.igniter.constants.Net.PRIVATE_VLAN4_CLIENT;
import static io.github.freewebmovement.igniter.constants.Net.PRIVATE_VLAN6_CLIENT;
import static io.github.freewebmovement.igniter.constants.Net.TUN2SOCKS5_SERVER_HOST;
import static io.github.freewebmovement.igniter.constants.Net.TUNNEL_TO_SOCKS_LOG_LEVEL;
import static io.github.freewebmovement.igniter.constants.Net.VPN_MTU;

import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

import clash.Clash;
import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.JNIHelper;
import io.github.freewebmovement.igniter.R;
import tun2socks.Tun2socks;
import tun2socks.Tun2socksStartOptions;

public class NetWorkConfig {

    public static boolean isPortTaken(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        }
        catch(IOException ce){
            ce.printStackTrace();
            return false;
        }
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
        JNIHelper.start(app.storage.getTrojanConfigPath());

        boolean enableClash = app.trojanPreferences.enableClash;
        boolean enableIPV6 = app.trojanPreferences.enableIPV6;
        boolean enableLan= app.trojanPreferences.enableLan;
        long trojanPort = app.trojanConfig.getLocalPort();
        long clashSocksPort = 0;
        long tun2socksPort;
        if (enableClash) {
            clashSocksPort = app.clashConfig.getPort();
            ClashConfig.startClash(app.getFilesDir().toString(),
                    (int)clashSocksPort, (int)trojanPort,
                    enableLan);
            tun2socksPort = clashSocksPort;
        } else {
            tun2socksPort = trojanPort;
        }
        tunnelProxy(fd, (int)tun2socksPort, enableIPV6, enableClash);
        String str = String.format(app.getString(R.string.network_ports), trojanPort, tun2socksPort);
        if (enableClash) {
            str += String.format(app.getString(R.string.clash_port), clashSocksPort);
        }
        return str;
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
        for (String server : DNS_SERVERS) {
            b.addDnsServer(server);
        }
        if (enableIPV6) {
            b.addAddress(PRIVATE_VLAN6_CLIENT, 126);
            b.addRoute("::", 0);

            for (String server : IPV6_DNS_SERVERS) {
                b.addDnsServer(server);
            }
        }
        return b.establish();
    }

    public static void stop(IgniterApplication app) {
        boolean enableClash = app.trojanPreferences.enableClash;
        JNIHelper.terminate();
        if (enableClash) {
            Clash.stop();
        }
        Tun2socks.stop();
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

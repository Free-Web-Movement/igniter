package io.github.freewebmovement.igniter.constants;

public class Net {
    //    public static final String LOCAL_HOST = "127.0.0.1";
    public static final int VPN_MTU = 1500;
    public static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    public static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    public static final String TUN2SOCKS5_SERVER_HOST = "127.0.0.1";
    public static final String FAKE_IP_RANGE = "198.18.0.1/16";
    public static final String DNS_SERVER_FAKE_IP = "198.18.0.1";
    public static final String TUNNEL_TO_SOCKS_LOG_LEVEL = "info";

    public static final String[] DNS_SERVERS = {
            "223.5.5.5",
            "119.29.29.29",
            "114.114.114.114"
    };

    public static final String[] IPV6_DNS_SERVERS = {
            "2400:3200::1",
            "2400:3200:baba::1"
    };
}

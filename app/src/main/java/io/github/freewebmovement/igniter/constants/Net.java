package io.github.freewebmovement.igniter.constants;

public class Net {
    //    public static final String LOCAL_HOST = "127.0.0.1";
    public static final int VPN_MTU = 1500;
    public static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    public static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    public static final String TUN2SOCKS5_SERVER_HOST = "127.0.0.1";
    public static final String FAKE_IP_RANGE = "198.18.0.1/16";
    public static final String TUNNEL_TO_SOCKS_LOG_LEVEL = "info";

    public static final String[] DNS_SERVERS = {
            "8.8.8.8",
            "8.8.4.4",
            "1.1.1.1",
            "1.0.0.1"
    };

    public static final String[] IPV6_DNS_SERVERS = {
            "2001:4860:4860::8888",
            "2001:4860:4860::8844"
    };
}

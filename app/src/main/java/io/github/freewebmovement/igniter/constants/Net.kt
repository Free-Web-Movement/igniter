package io.github.freewebmovement.igniter.constants

object Net {
    //    const val LOCAL_HOST = "127.0.0.1"
    const val VPN_MTU = 1500
    const val PRIVATE_VLAN4_CLIENT = "172.19.0.1"
    const val PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1"
    const val TUN2SOCKS5_SERVER_HOST = "127.0.0.1"
    const val FAKE_IP_RANGE = "198.18.0.1/16"
    const val DNS_SERVER_FAKE_IP = "198.18.0.1"
    const val TUNNEL_TO_SOCKS_LOG_LEVEL = "info"

    @JvmField
    val DNS_SERVERS = arrayOf(
        "223.5.5.5",
        "119.29.29.29",
        "114.114.114.114"
    )

    @JvmField
    val IPV6_DNS_SERVERS = arrayOf(
        "2400:3200::1",
        "2400:3200:baba::1"
    )
}

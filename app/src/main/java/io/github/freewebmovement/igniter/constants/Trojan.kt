package io.github.freewebmovement.igniter.constants

object Trojan {
    // Class Scoped Static Definitions for Configuration

    // URI
    const val SCHEMA = "trojan"

    // Top Level Keys
    const val KEY_LOCAL_ADDR = "local_addr"
    const val KEY_LOCAL_PORT = "local_port"
    const val KEY_REMOTE_ADDR = "remote_addr"
    const val KEY_REMOTE_IP = "remote_ip"
    const val KEY_REMOTE_PORT = "remote_port"
    const val KEY_PASSWORD = "password"

    // SSL Sub Keys
    const val KEY_SSL = "ssl"
    const val KEY_VERIFY_CERT = "verify"
    const val KEY_CA_CERT_PATH = "cert"
    const val KEY_CIPHER_LIST = "cipher"
    const val KEY_TLS13_CIPHER_LIST = "cipher_tls13"

    // User Data Preferences

    // Preferences Names
    const val TROJAN_PREFERENCE_NAME = "TROJAN_PREFERENCE"

    // Multi Process shared keys
    const val KEY_EVER_STARTED = "ever_started"
    const val KEY_ENABLE_CLASH = "enable_clash"
    const val KEY_ENABLE_LAN = "enable_lan"
    const val KEY_ENABLE_IPV6 = "enable_ipv6"
    const val KEY_ENABLE_AUTO_START = "enable_auto_start"
    const val KEY_ENABLE_BOOT_START = "enable_boot_start"
    const val KEY_SELECTED_INDEX = "selected_index"

    const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
}

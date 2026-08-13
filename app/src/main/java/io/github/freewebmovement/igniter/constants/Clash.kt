package io.github.freewebmovement.igniter.constants

object Clash {
    // KEYS
    const val KEY_SOCKS_PORT = "socks-port"
    const val KEY_PROXIES = "proxies"
    const val KEY_NAME = "name"
    const val KEY_PORT = "port"
    const val KEY_TROJAN_NAME = "trojan"
    const val KEY_MODE = "mode"

    const val DEFAULT_PORT = 1080
    const val DEFAULT_TROJAN_PORT = 1081

    // Modes
    const val MODE_RULE = "Rule"
    const val MODE_GLOBAL = "Global"
    const val MODE_DIRECT = "Direct"
    val MODES = listOf(MODE_RULE, MODE_GLOBAL, MODE_DIRECT)
}

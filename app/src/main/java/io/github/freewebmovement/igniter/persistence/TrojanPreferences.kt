package io.github.freewebmovement.igniter.persistence

import android.content.Context
import android.content.SharedPreferences
import io.github.freewebmovement.igniter.constants.Trojan

class TrojanPreferences(context: Context) {

    private val context: Context
    private var sharedPreferences: SharedPreferences
    private var everStarted: Boolean = false
    private var enableIPV6: Boolean = false
    private var enableClash: Boolean = false
    private var enableLan: Boolean = false
    private var enableAutoStart: Boolean = false
    private var enableBootStart: Boolean = false
    private var selectedIndex: Int = 0
    private var showSystemApps: Boolean = false
    private var vpnActive: Boolean = false

    init {
        this.context = context
        sharedPreferences = context.getSharedPreferences(Trojan.TROJAN_PREFERENCE_NAME, Context.MODE_PRIVATE)
        enableIPV6 = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_IPV6, false)
        everStarted = sharedPreferences.getBoolean(Trojan.KEY_EVER_STARTED, false)
        enableClash = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_CLASH, true)
        enableLan = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_LAN, false)
        enableAutoStart = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_AUTO_START, false)
        enableBootStart = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_BOOT_START, false)
        selectedIndex = sharedPreferences.getInt(Trojan.KEY_SELECTED_INDEX, 0)
        showSystemApps = sharedPreferences.getBoolean(Trojan.KEY_SHOW_SYSTEM_APPS, false)
        vpnActive = sharedPreferences.getBoolean(Trojan.KEY_VPN_ACTIVE, false)
    }

    /**
     * Re-reads every cached field from disk. The UI runs in the main process
     * and the proxy service in the ":proxy" process; each process caches a
     * snapshot of this file, so a toggled setting (e.g. IPv6 / LAN / Clash)
     * is invisible to an already-running proxy process until it reloads.
     * MODE_MULTI_PROCESS makes SharedPreferences check the file's mtime on
     * every access, which is what the VPN start path relies on here.
     */
    fun reload() {
        val sp = context.getSharedPreferences(
            Trojan.TROJAN_PREFERENCE_NAME,
            Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS
        )
        enableIPV6 = sp.getBoolean(Trojan.KEY_ENABLE_IPV6, false)
        everStarted = sp.getBoolean(Trojan.KEY_EVER_STARTED, false)
        enableClash = sp.getBoolean(Trojan.KEY_ENABLE_CLASH, true)
        enableLan = sp.getBoolean(Trojan.KEY_ENABLE_LAN, false)
        enableAutoStart = sp.getBoolean(Trojan.KEY_ENABLE_AUTO_START, false)
        enableBootStart = sp.getBoolean(Trojan.KEY_ENABLE_BOOT_START, false)
        selectedIndex = sp.getInt(Trojan.KEY_SELECTED_INDEX, 0)
        showSystemApps = sp.getBoolean(Trojan.KEY_SHOW_SYSTEM_APPS, false)
        vpnActive = sp.getBoolean(Trojan.KEY_VPN_ACTIVE, false)
    }

    fun setEnableIPV6(enableIPV6: Boolean) {
        this.enableIPV6 = enableIPV6
        setBoolean(Trojan.KEY_ENABLE_IPV6, enableIPV6)
    }

    fun getEnableIPV6(): Boolean {
        return enableIPV6
    }

    private fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun getString(key: String, value: String): String? {
        return sharedPreferences.getString(key, value)
    }

    private fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun getBoolean(key: String, fallback: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, fallback)
    }

    private fun setInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    private fun getInt(key: String, fallback: Int): Int {
        return sharedPreferences.getInt(key, fallback)
    }

    fun isEverStarted(): Boolean {
        return everStarted
    }

    fun setEverStarted(everStarted: Boolean) {
        this.everStarted = everStarted
        setBoolean(Trojan.KEY_EVER_STARTED, everStarted)
    }

    fun getEnableClash(): Boolean {
        return enableClash
    }

    fun setEnableClash(enableClash: Boolean) {
        this.enableClash = enableClash
        setBoolean(Trojan.KEY_ENABLE_CLASH, enableClash)
    }

    fun isEnableLan(): Boolean {
        return enableLan
    }

    fun setEnableLan(enableLan: Boolean) {
        this.enableLan = enableLan
        setBoolean(Trojan.KEY_ENABLE_LAN, enableLan)
    }

    fun isEnableAutoStart(): Boolean {
        return enableAutoStart
    }

    fun setEnableAutoStart(enableAutoStart: Boolean) {
        this.enableAutoStart = enableAutoStart
        setBoolean(Trojan.KEY_ENABLE_AUTO_START, enableAutoStart)
    }

    fun isEnableBootStart(): Boolean {
        return enableBootStart
    }

    fun setEnableBootStart(enableBootStart: Boolean) {
        this.enableBootStart = enableBootStart
        setBoolean(Trojan.KEY_ENABLE_BOOT_START, enableBootStart)
    }

    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    fun setSelectedIndex(index: Int) {
        this.selectedIndex = index
        setInt(Trojan.KEY_SELECTED_INDEX, index)
    }

    fun getShowSystemApps(): Boolean {
        return showSystemApps
    }

    fun setShowSystemApps(showSystemApps: Boolean) {
        this.showSystemApps = showSystemApps
        setBoolean(Trojan.KEY_SHOW_SYSTEM_APPS, showSystemApps)
    }

    fun isVpnActive(): Boolean {
        return vpnActive
    }

    /**
     * Records whether the tunnel is currently established. Written from the
     * :proxy process, read by the main-process watchdog. Uses commit() instead
     * of apply() because ProxyService may kill its own process right after a
     * stop, and an async apply() could be lost before it reaches disk - which
     * would leave a stale "active" flag and make the watchdog restart a tunnel
     * the user just stopped.
     */
    fun setVpnActive(active: Boolean) {
        vpnActive = active
        sharedPreferences.edit().putBoolean(Trojan.KEY_VPN_ACTIVE, active).commit()
    }
}

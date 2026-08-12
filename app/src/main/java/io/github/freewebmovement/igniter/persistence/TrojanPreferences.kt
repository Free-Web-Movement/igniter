package io.github.freewebmovement.igniter.persistence

import android.content.Context
import android.content.SharedPreferences
import io.github.freewebmovement.igniter.constants.Trojan

class TrojanPreferences(context: Context) {

    private var sharedPreferences: SharedPreferences
    private var everStarted: Boolean = false
    private var enableIPV6: Boolean = false
    private var enableClash: Boolean = false
    private var enableLan: Boolean = false
    private var enableAutoStart: Boolean = false
    private var enableBootStart: Boolean = false
    private var selectedIndex: Int = 0
    private var showSystemApps: Boolean = false

    init {
        sharedPreferences = context.getSharedPreferences(Trojan.TROJAN_PREFERENCE_NAME, Context.MODE_PRIVATE)
        enableIPV6 = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_IPV6, false)
        everStarted = sharedPreferences.getBoolean(Trojan.KEY_EVER_STARTED, false)
        enableClash = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_CLASH, true)
        enableLan = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_LAN, false)
        enableAutoStart = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_AUTO_START, false)
        enableBootStart = sharedPreferences.getBoolean(Trojan.KEY_ENABLE_BOOT_START, false)
        selectedIndex = sharedPreferences.getInt(Trojan.KEY_SELECTED_INDEX, 0)
        showSystemApps = sharedPreferences.getBoolean(Trojan.KEY_SHOW_SYSTEM_APPS, false)
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
}

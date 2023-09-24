package io.github.freewebmovement.igniter.persistence;

import static io.github.freewebmovement.igniter.constants.Trojan.KEY_ENABLE_AUTO_START;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_ENABLE_BOOT_START;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_ENABLE_CLASH;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_ENABLE_IPV6;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_ENABLE_LAN;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_EVER_STARTED;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_SELECTED_INDEX;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_SHOW_SYSTEM_APPS;
import static io.github.freewebmovement.igniter.constants.Trojan.TROJAN_PREFERENCE_NAME;

import android.content.Context;
import android.content.SharedPreferences;

public class TrojanPreferences {

    // Multi Process shared values

    // Application Status
    boolean everStarted;

    // Network Settings
    boolean enableIPV6;
    boolean enableClash;
    boolean enableLan;

    // Application Switches
    boolean enableAutoStart;
    boolean enableBootStart;
    int selectedIndex;
    boolean showSystemApps;

    // Android System Objects
    Context context;
    SharedPreferences sharedPreferences;

    public TrojanPreferences(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(TROJAN_PREFERENCE_NAME, Context.MODE_PRIVATE);
        enableIPV6 = sharedPreferences.getBoolean(KEY_ENABLE_IPV6, false);
        everStarted = sharedPreferences.getBoolean(KEY_EVER_STARTED, false);
        enableClash = sharedPreferences.getBoolean(KEY_ENABLE_CLASH, false);
        enableLan = sharedPreferences.getBoolean(KEY_ENABLE_LAN, false);
        enableAutoStart = sharedPreferences.getBoolean(KEY_ENABLE_AUTO_START, false);
        enableBootStart = sharedPreferences.getBoolean(KEY_ENABLE_BOOT_START, false);
        selectedIndex = sharedPreferences.getInt(KEY_SELECTED_INDEX, 0);
        showSystemApps = sharedPreferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false);
    }

    public void setEnableIPV6(boolean enableIPV6) {
        this.enableIPV6 = enableIPV6;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_ENABLE_IPV6, enableIPV6);
        editor.apply();
    }

    public boolean getEnableIPV6() {
        return enableIPV6;
    }

    private void setString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    private String getString(String key, String value) {
        return sharedPreferences.getString(key, value);
    }

    private void setBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private boolean getBoolean(String key, boolean fallback) {
        return sharedPreferences.getBoolean(key, fallback);
    }

    private void setInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    private int getInt(String key, int fallback) {
        return sharedPreferences.getInt(key, fallback);
    }

    public boolean isEverStarted() {
        return everStarted;
    }

    public void setEverStarted(boolean everStarted) {
        this.everStarted = everStarted;
        setBoolean(KEY_EVER_STARTED, everStarted);
    }

    public boolean getEnableClash() {
        return enableClash;
    }
    public void setEnableClash(boolean enableClash) {
        this.enableClash = enableClash;
        setBoolean(KEY_ENABLE_CLASH, enableClash);
    }

    public boolean isEnableLan() {
        return enableLan;
    }

    public void setEnableLan(boolean enableLan) {
        this.enableLan = enableLan;
        setBoolean(KEY_ENABLE_LAN, enableLan);
    }

    public boolean isEnableAutoStart() {
        return enableAutoStart;
    }

    public void setEnableAutoStart(boolean enableAutoStart) {
        this.enableAutoStart = enableAutoStart;
        setBoolean(KEY_ENABLE_AUTO_START, enableAutoStart);
    }

    public boolean isEnableBootStart() {
        return enableBootStart;
    }

    public void setEnableBootStart(boolean enableBootStart) {
        this.enableBootStart = enableBootStart;
        setBoolean(KEY_ENABLE_BOOT_START, enableBootStart);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        setInt(KEY_SELECTED_INDEX, index);
    }

    public boolean getShowSystemApps() {
        return showSystemApps;
    }

    public void setShowSystemApps(boolean showSystemApps) {
        this.showSystemApps = showSystemApps;
        setBoolean(KEY_SHOW_SYSTEM_APPS, showSystemApps);
    }
}

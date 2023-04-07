package io.github.freewebmovement.igniter.persistence;

import android.content.Context;
import android.content.SharedPreferences;

public class TrojanPreferences {

    // Preferences Names
    public static final String TROJAN_PREFERENCE_NAME = "TROJAN_PREFERENCE";

    // Private keys
    public static final String KEY_ENABLE_IPV6 = "enable_ipv6";

    // Multi Process shared keys
    public static final String KEY_EVER_STARTED = "ever_started";
    public static final String KEY_ENABLE_CLASH = "enable_clash";
    public static final String KEY_ENABLE_LAN = "enable_lan";
    public static final String KEY_ENABLE_AUTO_START = "enable_auto_start";
    public static final String KEY_ENABLE_BOOT_START = "enable_boot_start";
    public static final String KEY_SELECTED_INDEX = "selected_index";

    boolean enableIPV6;
    boolean enableClash;
    boolean everStarted;
    boolean enableLan;
    boolean enableAutoStart;
    boolean enableBootStart;

    int selectedIndex;


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
}

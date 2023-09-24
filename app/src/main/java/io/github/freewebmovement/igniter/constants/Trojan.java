package io.github.freewebmovement.igniter.constants;

public class Trojan {
    // Class Scoped Static Definitions for Configuration

    // URI
    public static final String SCHEMA = "trojan";

    // Tags
//    public static final String SINGLE_CONFIG_TAG = "TrojanConfig";

    // Top Level Keys
    public static final String KEY_LOCAL_ADDR = "local_addr";
    public static final String KEY_LOCAL_PORT = "local_port";
    public static final String KEY_REMOTE_ADDR = "remote_addr";
    public static final String KEY_REMOTE_PORT = "remote_port";
    public static final String KEY_PASSWORD = "password";

    // SSL Sub Keys
    public static final String KEY_SSL = "ssl";
    public static final String KEY_VERIFY_CERT = "verify";
    public static final String KEY_CA_CERT_PATH = "cert";
    public static final String KEY_CIPHER_LIST = "cipher";
    public static final String KEY_TLS13_CIPHER_LIST = "cipher_tls13";

    // User Data Preferences

    // Preferences Names
    public static final String TROJAN_PREFERENCE_NAME = "TROJAN_PREFERENCE";

    // Private keys

    // Multi Process shared keys
    public static final String KEY_EVER_STARTED = "ever_started";
    public static final String KEY_ENABLE_CLASH = "enable_clash";
    public static final String KEY_ENABLE_LAN = "enable_lan";
    public static final String KEY_ENABLE_IPV6 = "enable_ipv6";
    public static final String KEY_ENABLE_AUTO_START = "enable_auto_start";
    public static final String KEY_ENABLE_BOOT_START = "enable_boot_start";
    public static final String KEY_SELECTED_INDEX = "selected_index";

    public static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";
}

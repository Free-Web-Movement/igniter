package io.github.freewebmovement.igniter

/**
 * JNI bridge to the trojan native library.
 *
 * Replaces the old C++ jni-helper. [JNIHelper] keeps the legacy
 * [start]/[terminate] API so existing call sites are untouched; the new
 * [TrojanNative] object exposes the full trojan capability.
 */
object JNIHelper {
    private var isStarted = false

    init {
        System.loadLibrary("trojan")
    }

    @JvmStatic
    fun start(filename: String) {
        if (isStarted) {
            return
        }
        isStarted = true
        if (!TrojanNative.startClient(filename)) {
            isStarted = false
        }
    }

    @JvmStatic
    fun terminate() {
        if (isStarted) {
            TrojanNative.stopAll()
            isStarted = false
        }
    }
}

/**
 * trojan native API. The client runs a local SOCKS5 listener and tunnels
 * through a remote trojan server (same behaviour as the old C++ client).
 */
object TrojanNative {
    @JvmStatic
    external fun startClient(config: String): Boolean

    @JvmStatic
    external fun stopClient(): Boolean

    @JvmStatic
    external fun stopAll()

    @JvmStatic
    external fun getState(): Int

    @JvmStatic
    external fun getVersion(): String
}

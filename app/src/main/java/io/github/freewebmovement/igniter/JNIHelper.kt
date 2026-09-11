package io.github.freewebmovement.igniter

/**
 * JNI bridge to the trojan native library (client side).
 *
 * The same `libtrojan.so` is exported in two Kotlin classes to match the
 * native symbol names:
 *
 * - [JNIHelper] keeps the legacy C++ API (`start`/`terminate`) unchanged so
 *   existing call sites keep working.
 * - [TrojanNative] exposes the full trojan API. Client is fully
 *   supported today; server start/stop are exported and ready, to be
 *   verified against a live deployment later.
 */
object JNIHelper {
    private var isStarted = false

    init {
        System.loadLibrary("trojan")
    }

    private external fun trojan(config: String)

    private external fun stop()

    @JvmStatic
    fun start(filename: String) {
        if (isStarted) {
            return
        }
        isStarted = true
        trojan(filename)
    }

    @JvmStatic
    fun terminate() {
        if (isStarted) {
            stop()
            isStarted = false
        }
    }
}

/**
 * New trojan API. A phone running a client listens on a local SOCKS5
 * port and tunnels through a remote trojan server. Server entry points are
 * also exported (a phone can act as a server for the peer-to-peer mesh) but
 * are not yet exercised on device.
 */
object TrojanNative {
    /** Start the client (local SOCKS5 -> TLS -> remote server). */
    @JvmStatic
    external fun startClient(config: String): Boolean

    /** Start a server on this device (exported; verification deferred). */
    @JvmStatic
    external fun startServer(config: String): Boolean

    /** Stop the client instance. */
    @JvmStatic
    external fun stopClient(): Boolean

    /** Stop the server instance. */
    @JvmStatic
    external fun stopServer(): Boolean

    /** Stop both. */
    @JvmStatic
    external fun stopAll()

    /** Run state: 0 idle, 101/102 client starting/running, 201/202 server. */
    @JvmStatic
    external fun getState(): Int

    @JvmStatic
    external fun getVersion(): String

    @JvmStatic
    fun isClientRunning(): Boolean {
        val s = getState()
        return s == 101 || s == 102
    }

    @JvmStatic
    fun isServerRunning(): Boolean {
        val s = getState()
        return s == 201 || s == 202
    }
}

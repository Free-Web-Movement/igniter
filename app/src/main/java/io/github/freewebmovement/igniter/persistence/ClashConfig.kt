package io.github.freewebmovement.igniter.persistence

import android.util.Log
import clash.Clash
import clash.ClashStartOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.util.Objects
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.constants.Clash as ClashConstants

class ClashConfig(private val filename: String) {
    companion object {
        const val TAG = "ClashConfig"

        @JvmStatic
        fun startClash(path: String, port: Int, proxy: Int, enableLan: Boolean): Boolean {
            val clashStartOptions = ClashStartOptions()
            clashStartOptions.homeDir = path
            clashStartOptions.trojanProxyServer = "127.0.0.1:$proxy"
            if (enableLan) {
                clashStartOptions.socksListener = "*:$port"
            } else {
                clashStartOptions.socksListener = "127.0.0.1:$port"
            }
            clashStartOptions.trojanProxyServerUdpEnabled = true
            Clash.start(clashStartOptions)
            // Clash.start() does not report failures back to Java; the embedded
            // library logs and exits the process on fatal errors. Verify the SOCKS
            // listener is actually up so we do not silently blackhole traffic.
            return NetWorkConfig.waitForPort("127.0.0.1", port, 5000)
        }
    }

    @JvmField
    var data: MutableMap<String, Any?> = HashMap()
    private var yaml: Yaml = Yaml()

    init {
        try {
            loadFromFile(filename)
        } catch (e: Exception) {
            // A corrupt or partial config must not crash startup; restore the bundled default.
            Log.e(TAG, "Failed to parse clash config, restoring default", e)
            restoreDefault()
        }
    }

    @Throws(IOException::class)
    private fun loadFromFile(file: String) {
        val fileInputStream = FileInputStream(file)
        yaml = Yaml()
        @Suppress("UNCHECKED_CAST")
        data = yaml.load<Any?>(fileInputStream) as MutableMap<String, Any?>
        fileInputStream.close()
        if (data == null) {
            throw IOException("empty clash config")
        }
    }

    /**
     * Re-reads the config from disk into memory. Call after editing the config
     * file so the in-memory data (used at connect time) matches what was saved.
     */
    @Throws(IOException::class)
    fun reload() {
        loadFromFile(filename)
    }

    private fun restoreDefault() {
        try {
            val storage = Storage(IgniterApplication.getApplication())
            storage.reset(storage.path.clashConfig!!, R.raw.clash_config)
            loadFromFile(storage.path.clashConfig!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load default clash config", e)
            data = HashMap()
        }
    }

    fun <T> update(key: String, value: T) {
        update(data, key, value)
    }

    fun <T> update(data: MutableMap<String, Any?>, key: String, value: T) {
        data[key] = value
    }

    @Throws(IOException::class)
    fun save(filename: String) {
        val file = File(filename)
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = PrintWriter(filename)
        yaml.dump(data, writer)
        writer.close()
    }

    fun setPort(port: Int) {
        try {
            data[ClashConstants.KEY_SOCKS_PORT] = port
            save(filename)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setTrojanPort(port: Int) {
        @Suppress("UNCHECKED_CAST")
        val proxies = data[ClashConstants.KEY_PROXIES] as MutableList<MutableMap<String, Any?>>?
        try {
            if (proxies == null || proxies.isEmpty()) {
                return
            }
            for (i in proxies.indices) {
                val map = proxies[i]
                if (Objects.equals(map[ClashConstants.KEY_NAME], ClashConstants.KEY_TROJAN_NAME)) {
                    map[ClashConstants.KEY_PORT] = port
                    proxies[i] = map
                    break
                }
            }
            data[ClashConstants.KEY_PROXIES] = proxies
            save(filename)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getPort(): Int {
        return data[ClashConstants.KEY_SOCKS_PORT] as Int
    }

    /**
     * The Clash routing mode. This project only uses rule-based routing (the
     * domain rules in the `rules:` section), so the mode is always Rule and
     * Global/Direct are never offered.
     */
    fun getMode(): String = ClashConstants.MODE_RULE

    /**
     * Rewrites the `mode:` field of the Clash config to Rule. Called before
     * starting Clash so a stale Global/Direct value left by an old build can
     * never bypass the domain rules.
     */
    fun ensureRuleMode() {
        if (data[ClashConstants.KEY_MODE] != ClashConstants.MODE_RULE) {
            data[ClashConstants.KEY_MODE] = ClashConstants.MODE_RULE
            try {
                save(filename)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getTrojanPort(): Int {
        @Suppress("UNCHECKED_CAST")
        val proxies = data[ClashConstants.KEY_PROXIES] as MutableList<MutableMap<String, Any?>>?
        if (proxies != null) {
            for (i in proxies.indices) {
                val map = proxies[i]
                if (Objects.equals(map[ClashConstants.KEY_NAME], ClashConstants.KEY_TROJAN_NAME)) {
                    return map[ClashConstants.KEY_PORT] as Int
                }
            }
        }
        return ClashConstants.DEFAULT_TROJAN_PORT
    }

    /**
     * Validates that this config is compatible with the embedded Clash library.
     * The library hard-requires that the first proxy entry is a "trojan" socks5
     * proxy, otherwise it calls log.Fatalf (which terminates the whole process).
     *
     * @return an error message if the config is unusable, or `null` if it is valid.
     */
    fun validateConfig(): String? {
        if (data == null) {
            return "clash config is empty or invalid"
        }
        val proxies = data[ClashConstants.KEY_PROXIES]
        if (proxies !is List<*> || proxies.isEmpty()) {
            return "clash config has no proxies entry"
        }
        val first = proxies[0]
        if (first is Map<*, *>) {
            if ("socks5" == first["type"] && "trojan" == first["name"]) {
                return null
            }
        }
        return "the first clash proxy entry must be type=socks5, name=trojan"
    }
}

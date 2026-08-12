package io.github.freewebmovement.igniter.persistence

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.constants.Trojan

class TrojanConfig : Parcelable {

    companion object {
        @JvmStatic
        var instance: TrojanConfig? = null
            private set

        private var defaultJSON: JSONObject? = null

        @JvmStatic
        fun getInstance(storage: Storage): TrojanConfig {
            instance?.let { return it }
            defaultJSON = storage.readRawJSON(R.raw.config)

            val filename = storage.path.trojanConfig!!
            var trojanConfig = read(filename)

            if (trojanConfig == null) {
                trojanConfig = TrojanConfig().fromJSON(defaultJSON)
                write(trojanConfig!!, filename)
            }

            trojanConfig!!.setCaCertPath(storage.path.caCert!!)
            instance = trojanConfig
            return trojanConfig
        }

        @JvmStatic
        fun getDefaultJSON(storage: Storage): JSONObject? {
            return storage.readRawJSON(R.raw.config)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<TrojanConfig> = object : Parcelable.Creator<TrojanConfig> {
            override fun createFromParcel(source: Parcel): TrojanConfig {
                return TrojanConfig().readFromParcel(source)
            }

            override fun newArray(size: Int): Array<TrojanConfig?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun read(filename: String): TrojanConfig? {
            val json = Storage.readJSON(filename)
            return TrojanConfig().fromJSON(json)
        }

        @JvmStatic
        fun write(trojanConfig: TrojanConfig, filename: String) {
            try {
                val config = trojanConfig.toJSONString()
                val file = File(filename)
                FileOutputStream(file).use { fos ->
                    fos.write(config.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun <T> update(trojanConfigPath: String, key: String, v: T) {
            val file = File(trojanConfigPath)
            if (file.exists()) {
                try {
                    val str = FileInputStream(file).use { fis ->
                        val content = ByteArray(file.length().toInt())
                        fis.read(content)
                        String(content)
                    }
                    val json = JSONObject(str)
                    json.put(key, v)
                    FileOutputStream(file).use { fos ->
                        fos.write(json.toString().toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun toURIString(trojanConfig: TrojanConfig): String? {
            return try {
                val trojanUri = URI(
                    Trojan.SCHEMA,
                    trojanConfig.getPassword(),
                    trojanConfig.getRemoteAddr(),
                    trojanConfig.getRemotePort(),
                    null, null, null
                )
                trojanUri.toString()
            } catch (e: java.net.URISyntaxException) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun fromURIString(URIString: String): TrojanConfig? {
            val trojanUri = try {
                URI(URIString)
            } catch (e: java.net.URISyntaxException) {
                e.printStackTrace()
                return null
            }
            val scheme = trojanUri.scheme
            if (scheme == null) {
                return null
            }
            if (scheme != Trojan.SCHEMA) {
                return null
            }
            val host = trojanUri.host
            val port = trojanUri.port
            val userInfo = trojanUri.userInfo

            val retConfig = TrojanConfig()
            retConfig.setRemoteAddr(host)
            retConfig.setRemotePort(port)
            retConfig.setPassword(userInfo)
            return retConfig
        }

        private fun paramEquals(a: Any?, b: Any?): Boolean {
            if (a === b) {
                return true
            }
            if (a == null || b == null) {
                return false
            }
            return a == b
        }
    }

    // Object Scoped members
    private var localAddr: String? = null
    private var localPort: Int = 0
    private var remoteAddr: String? = null
    private var remoteIP: String? = null
    private var remotePort: Int = 0
    private var password: String? = null
    private var verifyCert: Boolean = false
    private var caCertPath: String? = null
    private var cipherList: String? = null
    private var tls13CipherList: String? = null

    init {
        fromJSON(defaultJSON)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(localAddr)
        dest.writeInt(localPort)
        dest.writeString(remoteAddr)
        dest.writeString(remoteIP)
        dest.writeInt(remotePort)
        dest.writeString(password)
        dest.writeByte((if (verifyCert) 1 else 0).toByte())
        dest.writeString(caCertPath)
        dest.writeString(cipherList)
        dest.writeString(tls13CipherList)
    }

    fun readFromParcel(`in`: Parcel): TrojanConfig {
        localAddr = `in`.readString()
        localPort = `in`.readInt()
        remoteAddr = `in`.readString()
        remoteIP = `in`.readString()
        remotePort = `in`.readInt()
        password = `in`.readString()
        verifyCert = `in`.readByte().toInt() != 0
        caCertPath = `in`.readString()
        cipherList = `in`.readString()
        tls13CipherList = `in`.readString()
        return this
    }

    fun toJSON(): JSONObject? {
        return try {
            val json = JSONObject()
            json.put(Trojan.KEY_LOCAL_ADDR, localAddr)
            json.put(Trojan.KEY_LOCAL_PORT, localPort)
            json.put(Trojan.KEY_REMOTE_ADDR, remoteAddr)
            json.put(Trojan.KEY_REMOTE_IP, remoteIP)
            json.put(Trojan.KEY_REMOTE_PORT, remotePort)
            json.put(Trojan.KEY_PASSWORD, JSONArray().put(password))
            val ssl = JSONObject()
            ssl.put(Trojan.KEY_VERIFY_CERT, verifyCert)
            ssl.put(Trojan.KEY_CA_CERT_PATH, caCertPath)
            ssl.put(Trojan.KEY_CIPHER_LIST, cipherList)
            ssl.put(Trojan.KEY_TLS13_CIPHER_LIST, tls13CipherList)
            assert(File(caCertPath).exists())
            json.put(Trojan.KEY_SSL, ssl)
            json
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    fun toJSONString(): String {
        return toJSON().toString()
    }

    fun fromJSON(json: JSONObject?): TrojanConfig? {
        return try {
            this.localAddr = json!!.getString(Trojan.KEY_LOCAL_ADDR)
            this.localPort = json.getInt(Trojan.KEY_LOCAL_PORT)
            this.remoteAddr = json.getString(Trojan.KEY_REMOTE_ADDR)
            this.remoteIP = json.getString(Trojan.KEY_REMOTE_IP)
            this.remotePort = json.getInt(Trojan.KEY_REMOTE_PORT)
            this.password = json.getJSONArray(Trojan.KEY_PASSWORD).getString(0)
            val ssl = json.getJSONObject(Trojan.KEY_SSL)
            this.verifyCert = ssl.getBoolean(Trojan.KEY_VERIFY_CERT)
            this.caCertPath = ssl.getString(Trojan.KEY_CA_CERT_PATH)
            this.cipherList = ssl.getString(Trojan.KEY_CIPHER_LIST)
            this.tls13CipherList = ssl.getString(Trojan.KEY_TLS13_CIPHER_LIST)
            this
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    fun isValidRunningConfig(): Boolean {
        return !TextUtils.isEmpty(caCertPath) &&
                !TextUtils.isEmpty(remoteAddr) &&
                !TextUtils.isEmpty(password)
    }

    fun getLocalPort(): Int {
        return localPort
    }

    fun setLocalPort(localPort: Int) {
        this.localPort = localPort
    }

    fun getRemoteAddr(): String {
        val addr = remoteAddr
        return if (addr.isNullOrEmpty()) {
            "0.0.0.0"
        } else {
            addr
        }
    }

    fun setRemoteAddr(remoteAddr: String) {
        this.remoteAddr = remoteAddr
    }

    fun getRemoteIP(): String {
        val ip = remoteIP
        return if (ip.isNullOrEmpty()) {
            ""
        } else {
            ip
        }
    }

    fun setRemoteIP(remoteIP: String) {
        this.remoteIP = remoteIP
    }

    fun getRemotePort(): Int {
        return remotePort
    }

    fun setRemotePort(remotePort: Int) {
        this.remotePort = remotePort
    }

    fun getPassword(): String? {
        return password
    }

    fun setPassword(password: String): TrojanConfig {
        this.password = password
        return this
    }

    fun getVerifyCert(): Boolean {
        return verifyCert
    }

    fun setVerifyCert(verifyCert: Boolean) {
        this.verifyCert = verifyCert
    }

    fun setCaCertPath(caCertPath: String) {
        this.caCertPath = caCertPath
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TrojanConfig) {
            return false
        }
        return paramEquals(remoteAddr, other.remoteAddr) &&
                paramEquals(remoteIP, other.remoteIP) &&
                paramEquals(remotePort, other.remotePort) &&
                paramEquals(localAddr, other.localAddr) &&
                paramEquals(localPort, other.localPort) &&
                paramEquals(password, other.password) &&
                paramEquals(verifyCert, other.verifyCert) &&
                paramEquals(caCertPath, other.caCertPath) &&
                paramEquals(cipherList, other.cipherList) &&
                paramEquals(tls13CipherList, other.tls13CipherList)
    }
}

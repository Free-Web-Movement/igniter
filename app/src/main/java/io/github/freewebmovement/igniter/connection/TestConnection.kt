package io.github.freewebmovement.igniter.connection

import android.os.AsyncTask
import android.util.Log
import androidx.annotation.NonNull
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class TestConnection(
    private val mProxyHost: String,
    private val mProxyPort: Long,
    mOnResultListener: OnResultListener
) : AsyncTask<String, Void, TestResult>() {
    private val mOnResultListenerRef: WeakReference<OnResultListener> = WeakReference(mOnResultListener)

    interface OnResultListener {
        fun onResult(testUrl: String, connected: Boolean, delay: Long, error: String)
    }

    override fun doInBackground(vararg strings: String): TestResult {
        val testUrl = strings[0]
        Log.i("TestConnection", "Connecting to $testUrl via SOCKS5 $mProxyHost:$mProxyPort")
        var connection: java.net.HttpURLConnection? = null
        return try {
            val startTime = System.currentTimeMillis()
            val proxyAddress = InetSocketAddress(mProxyHost, mProxyPort.toInt())
            val proxy = Proxy(Proxy.Type.SOCKS, proxyAddress)
            connection = URL(testUrl).openConnection(proxy) as java.net.HttpURLConnection
            connection.connectTimeout = DEFAULT_TIMEOUT
            connection.readTimeout = DEFAULT_TIMEOUT
            connection.connect()
            try {
                connection.inputStream.close()
            } catch (_: Exception) {}
            val delay = System.currentTimeMillis() - startTime
            Log.i("TestConnection", "Connected to $testUrl in ${delay}ms")
            TestResult(testUrl, true, "", delay)
        } catch (e: Exception) {
            Log.e("TestConnection", "Failed to connect to $testUrl: ${e.javaClass.simpleName}: ${e.message}")
            TestResult(testUrl, false, e.message ?: "", 0)
        } finally {
            connection?.disconnect()
        }
    }

    override fun onPostExecute(testResult: TestResult) {
        val listener = mOnResultListenerRef.get()
        if (listener != null) {
            listener.onResult(testResult.url, testResult.connected, testResult.delay, testResult.error)
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 10 * 1000 // 10 seconds
    }
}

class TestResult(
    var url: String,
    var connected: Boolean,
    @param:NonNull var error: String,
    var delay: Long
)
